package dev.brenzo.payment_application.application.service;

import dev.brenzo.payment_application.application.service.PaymentService.CreatePaymentCommand;
import dev.brenzo.payment_application.domain.payment.InvalidCardNumberException;
import dev.brenzo.payment_application.domain.payment.Payment;
import dev.brenzo.payment_application.domain.webhook.DeliveryStatus;
import dev.brenzo.payment_application.domain.webhook.PaymentCreatedEvent;
import dev.brenzo.payment_application.domain.webhook.WebhookDelivery;
import dev.brenzo.payment_application.domain.webhook.WebhookSubscription;
import dev.brenzo.payment_application.support.FixedClock;
import dev.brenzo.payment_application.support.InMemoryPaymentRepository;
import dev.brenzo.payment_application.support.InMemoryWebhookDeliveryRepository;
import dev.brenzo.payment_application.support.InMemoryWebhookSubscriptionRepository;
import dev.brenzo.payment_application.support.SequentialIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceTest {

    private static final Instant T0 = Instant.parse("2026-05-07T19:53:00Z");
    private static final String VALID_CARD = "4242 4242 4242 4242";

    InMemoryPaymentRepository payments;
    InMemoryWebhookSubscriptionRepository subs;
    InMemoryWebhookDeliveryRepository deliveries;
    SequentialIdGenerator ids;
    FixedClock clock;
    ObjectMapper objectMapper;
    PaymentService service;

    @BeforeEach
    void setUp() {
        payments = new InMemoryPaymentRepository();
        subs = new InMemoryWebhookSubscriptionRepository();
        clock = new FixedClock(T0);
        deliveries = new InMemoryWebhookDeliveryRepository(clock);
        ids = new SequentialIdGenerator();
        objectMapper = new ObjectMapper();
        service = new PaymentService(payments, subs, deliveries, objectMapper, ids, clock);
    }

    @Test
    void persists_payment_and_records_last4() {
        Payment p = service.create(new CreatePaymentCommand("Ada", "Lovelace", "10001", VALID_CARD));

        assertThat(payments.store).hasSize(1);
        assertThat(p.cardLast4().value()).isEqualTo("4242");
        assertThat(p.firstName()).isEqualTo("Ada");
        assertThat(p.createdAt()).isEqualTo(T0);
    }

    @Test
    void invalid_card_throws_and_persists_nothing() {
        assertThatThrownBy(() -> service.create(
                new CreatePaymentCommand("Ada", "Lovelace", "10001", "4242424242424241")))
                .isInstanceOf(InvalidCardNumberException.class);

        assertThat(payments.store).isEmpty();
        assertThat(deliveries.store).isEmpty();
    }

    @Test
    void creates_one_pending_delivery_per_active_webhook() {
        UUID s1 = ids.get();
        UUID s2 = ids.get();
        UUID s3 = ids.get();
        subs.save(new WebhookSubscription(s1, "https://a.test/hook", true, T0));
        subs.save(new WebhookSubscription(s2, "https://b.test/hook", true, T0));
        subs.save(new WebhookSubscription(s3, "https://inactive.test/hook", false, T0));

        Payment p = service.create(new CreatePaymentCommand("Ada", "Lovelace", "10001", VALID_CARD));

        List<WebhookDelivery> created = deliveries.findByPaymentId(p.id());
        assertThat(created).hasSize(2);
        assertThat(created).allSatisfy(d -> {
            assertThat(d.status()).isEqualTo(DeliveryStatus.PENDING);
            assertThat(d.attempts()).isZero();
            assertThat(d.nextAttemptAt()).isEqualTo(T0);
            assertThat(d.paymentId()).isEqualTo(p.id());
            assertThat(d.payloadJson()).contains("\"cardLast4\":\"4242\"");
            assertThat(d.payloadJson()).doesNotContain("4242424242424242");
        });
        assertThat(created.stream().map(WebhookDelivery::url).toList())
                .containsExactlyInAnyOrder("https://a.test/hook", "https://b.test/hook");
    }

    @Test
    void no_subscriptions_means_no_deliveries() {
        Payment p = service.create(new CreatePaymentCommand("Ada", "Lovelace", "10001", VALID_CARD));
        assertThat(deliveries.findByPaymentId(p.id())).isEmpty();
    }

    @Test
    void payload_uses_PaymentCreatedEvent_event_type() {
        subs.save(new WebhookSubscription(ids.get(), "https://x.test/h", true, T0));

        Payment p = service.create(new CreatePaymentCommand("Ada", "Lovelace", "10001", VALID_CARD));

        List<WebhookDelivery> created = deliveries.findByPaymentId(p.id());
        assertThat(created).hasSize(1);
        JsonNode payload = objectMapper.readTree(created.get(0).payloadJson());
        assertThat(payload.get("event").asString()).isEqualTo(PaymentCreatedEvent.EVENT_TYPE);
        assertThat(payload.get("event").asString()).isEqualTo("payment.created");
        assertThat(payload.get("payment").get("id").asString()).isEqualTo(p.id().toString());
        assertThat(payload.get("payment").get("cardLast4").asString()).isEqualTo("4242");
        assertThat(payload.get("payment").get("firstName").asString()).isEqualTo("Ada");
    }
}
