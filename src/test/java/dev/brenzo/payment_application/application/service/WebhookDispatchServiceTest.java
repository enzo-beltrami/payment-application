package dev.brenzo.payment_application.application.service;

import dev.brenzo.payment_application.adapter.config.WebhookDispatcherProperties;
import dev.brenzo.payment_application.application.port.out.WebhookSender;
import dev.brenzo.payment_application.application.service.WebhookDispatchService.DispatchSummary;
import dev.brenzo.payment_application.domain.webhook.DeliveryStatus;
import dev.brenzo.payment_application.domain.webhook.WebhookDelivery;
import dev.brenzo.payment_application.support.FixedClock;
import dev.brenzo.payment_application.support.InMemoryWebhookDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookDispatchServiceTest {

    private static final Instant T0 = Instant.parse("2026-05-07T19:53:00Z");

    FixedClock clock;
    InMemoryWebhookDeliveryRepository deliveries;
    ProgrammableSender sender;
    WebhookDispatchService dispatcher;

    @BeforeEach
    void setUp() {
        clock = new FixedClock(T0);
        deliveries = new InMemoryWebhookDeliveryRepository(clock);
        sender = new ProgrammableSender();
        WebhookDispatcherProperties props = new WebhookDispatcherProperties();
        props.setBatchSize(50);
        props.setMaxAttempts(5);
        props.setBackoffCapSeconds(3600);
        dispatcher = new WebhookDispatchService(deliveries, sender, clock, props);
    }

    private WebhookDelivery enqueue(String url) {
        WebhookDelivery d = WebhookDelivery.newPending(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                url, "{\"hello\":1}", T0);
        return deliveries.save(d);
    }

    @Test
    void successful_send_marks_delivered() {
        WebhookDelivery d = enqueue("https://ok.test/h");
        sender.queue(WebhookSender.SendResult.ok(200));

        DispatchSummary summary = dispatcher.dispatchBatch();

        assertThat(summary.claimed()).isEqualTo(1);
        assertThat(summary.delivered()).isEqualTo(1);
        assertThat(summary.retried()).isZero();
        assertThat(summary.failed()).isZero();
        WebhookDelivery reloaded = deliveries.store.get(d.id());
        assertThat(reloaded.status()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(reloaded.lastResponseCode()).isEqualTo(200);
    }

    @Test
    void transient_failure_schedules_retry() {
        WebhookDelivery d = enqueue("https://flaky.test/h");
        sender.queue(WebhookSender.SendResult.httpError(503, "Service Unavailable"));

        DispatchSummary summary = dispatcher.dispatchBatch();

        assertThat(summary.delivered()).isZero();
        assertThat(summary.retried()).isEqualTo(1);
        assertThat(summary.failed()).isZero();
        WebhookDelivery reloaded = deliveries.store.get(d.id());
        assertThat(reloaded.status()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(reloaded.attempts()).isEqualTo(1);
        assertThat(reloaded.nextAttemptAt()).isEqualTo(T0.plusSeconds(2));
        assertThat(reloaded.lastError()).isEqualTo("Service Unavailable");
    }

    @Test
    void transport_error_records_no_status_code_and_retries() {
        WebhookDelivery d = enqueue("https://gone.test/h");
        sender.queue(WebhookSender.SendResult.transportError("connection refused"));

        DispatchSummary summary = dispatcher.dispatchBatch();

        assertThat(summary.retried()).isEqualTo(1);
        WebhookDelivery reloaded = deliveries.store.get(d.id());
        assertThat(reloaded.status()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(reloaded.lastResponseCode()).isNull();
        assertThat(reloaded.lastError()).isEqualTo("connection refused");
    }

    @Test
    void exhausted_retries_become_FAILED() {
        WebhookDelivery d = enqueue("https://dead.test/h");
        for (int i = 0; i < 5; i++) {
            sender.queue(WebhookSender.SendResult.httpError(500, "boom"));
        }

        // simulate dispatcher waking each time the delivery becomes due
        for (int i = 0; i < 5; i++) {
            clock.set(deliveries.store.get(d.id()).nextAttemptAt());
            dispatcher.dispatchBatch();
        }

        WebhookDelivery reloaded = deliveries.store.get(d.id());
        assertThat(reloaded.status()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(reloaded.attempts()).isEqualTo(5);
    }

    @Test
    void sender_throws_is_treated_as_transport_error() {
        WebhookDelivery d = enqueue("https://throws.test/h");
        sender.throwOnNext(new RuntimeException("kaboom"));

        DispatchSummary summary = dispatcher.dispatchBatch();

        assertThat(summary.retried()).isEqualTo(1);
        WebhookDelivery reloaded = deliveries.store.get(d.id());
        assertThat(reloaded.status()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(reloaded.lastError()).contains("kaboom");
    }

    @Test
    void batch_isolates_failures_between_rows() {
        WebhookDelivery good = enqueue("https://ok.test/h");
        WebhookDelivery bad = enqueue("https://bad.test/h");
        sender.queue(WebhookSender.SendResult.ok(200));
        sender.queue(WebhookSender.SendResult.httpError(500, "err"));

        DispatchSummary summary = dispatcher.dispatchBatch();

        assertThat(summary.claimed()).isEqualTo(2);
        assertThat(summary.delivered()).isEqualTo(1);
        assertThat(summary.retried()).isEqualTo(1);
        assertThat(deliveries.store.get(good.id()).status()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(deliveries.store.get(bad.id()).status()).isEqualTo(DeliveryStatus.PENDING);
    }

    @Test
    void only_due_pending_rows_are_picked_up() {
        WebhookDelivery now = enqueue("https://now.test/h");

        // queue a delivery that's not yet due
        WebhookDelivery later = WebhookDelivery.newPending(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "https://later.test/h", "{}", T0.plusSeconds(60));
        deliveries.save(later);

        sender.queue(WebhookSender.SendResult.ok(200));
        DispatchSummary summary = dispatcher.dispatchBatch();

        assertThat(summary.claimed()).isEqualTo(1);
        assertThat(deliveries.store.get(now.id()).status()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(deliveries.store.get(later.id()).status()).isEqualTo(DeliveryStatus.PENDING);
    }

    static class ProgrammableSender implements WebhookSender {
        private final Deque<SendResult> queue = new ArrayDeque<>();
        private final List<RuntimeException> throwOn = new ArrayList<>();

        void queue(SendResult r) { queue.add(r); }
        void throwOnNext(RuntimeException ex) { throwOn.add(ex); }

        @Override
        public SendResult send(String url, String jsonBody) {
            if (!throwOn.isEmpty()) throw throwOn.removeFirst();
            if (queue.isEmpty()) throw new IllegalStateException("no canned result for " + url);
            return queue.removeFirst();
        }
    }
}
