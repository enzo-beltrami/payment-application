package dev.brenzo.payment_application.integration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.brenzo.payment_application.adapter.out.persistence.PaymentJpaEntity;
import dev.brenzo.payment_application.adapter.out.persistence.PaymentJpaRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.webhooks.dispatcher.poll-interval-ms=200",
                "app.webhooks.dispatcher.connect-timeout-ms=2000",
                "app.webhooks.dispatcher.read-timeout-ms=2000",
                "app.encryption.key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        })
@Testcontainers
class PaymentEndToEndTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort int port;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired PaymentJpaRepository paymentJpa;

    static FlakyReceiver receiver;
    static HttpClient http;

    @BeforeAll
    static void startReceiver() throws IOException {
        receiver = new FlakyReceiver();
        receiver.start();
        http = HttpClient.newHttpClient();
    }

    @AfterAll
    static void stopReceiver() {
        if (receiver != null) receiver.stop();
    }

    @Test
    void posts_payment_then_webhook_delivers_resiliently_after_first_500() throws Exception {
        // 1. register webhook
        HttpResponse<String> webhookResp = postJson("/api/webhooks", """
                { "url": "%s" }""".formatted(receiver.url()));
        assertThat(webhookResp.statusCode()).isEqualTo(201);

        // 2. create payment
        HttpResponse<String> paymentResp = postJson("/api/payments", """
                {
                  "firstName": "Ada",
                  "lastName": "Lovelace",
                  "zipCode": "10001",
                  "cardNumber": "4242 4242 4242 4242"
                }""");
        assertThat(paymentResp.statusCode()).isEqualTo(201);
        JsonNode body = objectMapper.readTree(paymentResp.body());
        UUID paymentId = UUID.fromString(body.get("id").asString());
        assertThat(body.get("cardLast4").asString()).isEqualTo("4242");
        assertThat(paymentResp.body()).doesNotContain("4242424242424242");

        // 3. wait until receiver gets the second (successful) call
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(receiver.deliveriesReceived()).isGreaterThanOrEqualTo(2));

        assertThat(receiver.failuresIssued()).isGreaterThanOrEqualTo(1);

        // 4. inspect the second (successful) payload
        String payload = receiver.bodies().get(receiver.bodies().size() - 1);
        JsonNode payloadNode = objectMapper.readTree(payload);
        assertThat(payloadNode.get("event").asString()).isEqualTo("payment.created");
        assertThat(payloadNode.get("payment").get("id").asString()).isEqualTo(paymentId.toString());
        assertThat(payloadNode.get("payment").get("cardLast4").asString()).isEqualTo("4242");
        assertThat(payload).doesNotContain("4242424242424242");

        // 5. db column holds ciphertext, not plaintext
        assertCiphertextInDatabase(paymentId);
    }

    private HttpResponse<String> postJson(String path, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void assertCiphertextInDatabase(UUID paymentId) {
        // Raw JDBC bypasses the JPA AttributeConverter, so we see the actual ciphertext.
        String stored = jdbc.queryForObject(
                "SELECT card_number_encrypted FROM payment WHERE id = ?",
                String.class, paymentId);
        assertThat(stored).isNotNull();
        assertThat(stored).doesNotContain("4242424242424242");

        // Round-trip via the JPA entity yields the plaintext (proves the converter is wired).
        PaymentJpaEntity entity = paymentJpa.findById(paymentId).orElseThrow();
        assertThat(entity.getCardNumber()).isEqualTo("4242424242424242");
    }

    /**
     * Tiny in-process HTTP receiver: returns 503 on the first call, 200 thereafter.
     * Records every body received.
     */
    static class FlakyReceiver {
        private HttpServer server;
        private final List<String> bodies = new CopyOnWriteArrayList<>();
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicInteger failures = new AtomicInteger();

        void start() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/hook", this::handle);
            server.setExecutor(java.util.concurrent.Executors.newSingleThreadExecutor());
            server.start();
        }

        private void handle(HttpExchange exchange) throws IOException {
            byte[] body = exchange.getRequestBody().readAllBytes();
            String json = new String(body, StandardCharsets.UTF_8);
            int n = calls.incrementAndGet();
            bodies.add(json);
            int status;
            if (n == 1) {
                status = 503;
                failures.incrementAndGet();
            } else {
                status = 200;
            }
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        }

        void stop() {
            if (server != null) server.stop(0);
        }

        String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort() + "/hook";
        }

        int deliveriesReceived() { return calls.get(); }
        int failuresIssued() { return failures.get(); }
        List<String> bodies() { return bodies; }
    }
}
