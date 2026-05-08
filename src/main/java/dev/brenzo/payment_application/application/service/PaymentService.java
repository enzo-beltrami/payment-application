package dev.brenzo.payment_application.application.service;

import dev.brenzo.payment_application.application.port.out.PaymentRepository;
import dev.brenzo.payment_application.application.port.out.WebhookDeliveryRepository;
import dev.brenzo.payment_application.application.port.out.WebhookSubscriptionRepository;
import dev.brenzo.payment_application.domain.payment.CardNumber;
import dev.brenzo.payment_application.domain.payment.Payment;
import dev.brenzo.payment_application.domain.webhook.PaymentCreatedEvent;
import dev.brenzo.payment_application.domain.webhook.WebhookDelivery;
import dev.brenzo.payment_application.domain.webhook.WebhookSubscription;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class PaymentService {

    private final PaymentRepository payments;
    private final WebhookSubscriptionRepository subscriptions;
    private final WebhookDeliveryRepository deliveries;
    private final ObjectMapper objectMapper;
    private final Supplier<UUID> ids;
    private final Clock clock;

    public PaymentService(PaymentRepository payments,
                          WebhookSubscriptionRepository subscriptions,
                          WebhookDeliveryRepository deliveries,
                          ObjectMapper objectMapper,
                          Supplier<UUID> ids,
                          Clock clock) {
        this.payments = payments;
        this.subscriptions = subscriptions;
        this.deliveries = deliveries;
        this.objectMapper = objectMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Transactional
    public Payment create(CreatePaymentCommand cmd) {
        Instant now = clock.instant();
        Payment payment = new Payment(
                ids.get(),
                cmd.firstName(),
                cmd.lastName(),
                cmd.zipCode(),
                new CardNumber(cmd.cardNumber()),
                now);
        Payment saved = payments.save(payment);

        List<WebhookSubscription> activeSubs = subscriptions.findAllActive();
        for (WebhookSubscription sub : activeSubs) {
            UUID deliveryId = ids.get();
            PaymentCreatedEvent event = new PaymentCreatedEvent(
                    deliveryId,
                    saved.id(),
                    saved.firstName(),
                    saved.lastName(),
                    saved.zipCode(),
                    saved.cardLast4().value(),
                    saved.createdAt());
            String payload = serialize(event);
            WebhookDelivery delivery = WebhookDelivery.newPending(
                    deliveryId, saved.id(), sub.id(), sub.url(), payload, now);
            deliveries.save(delivery);
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findById(UUID id) {
        return payments.findById(id);
    }

    private String serialize(PaymentCreatedEvent event) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("event", PaymentCreatedEvent.EVENT_TYPE);
        root.put("deliveryId", event.deliveryId().toString());
        ObjectNode payment = root.putObject("payment");
        payment.put("id", event.paymentId().toString());
        payment.put("firstName", event.firstName());
        payment.put("lastName", event.lastName());
        payment.put("zipCode", event.zipCode());
        payment.put("cardLast4", event.cardLast4());
        payment.put("createdAt", event.createdAt().toString());
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JacksonException e) {
            throw new IllegalStateException("failed to serialize PaymentCreatedEvent", e);
        }
    }

    public record CreatePaymentCommand(String firstName, String lastName, String zipCode, String cardNumber) {
    }
}
