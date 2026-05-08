package dev.brenzo.payment_application.support;

import dev.brenzo.payment_application.application.port.out.WebhookDeliveryRepository;
import dev.brenzo.payment_application.domain.webhook.DeliveryStatus;
import dev.brenzo.payment_application.domain.webhook.WebhookDelivery;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class InMemoryWebhookDeliveryRepository implements WebhookDeliveryRepository {

    public final Map<UUID, WebhookDelivery> store = new LinkedHashMap<>();
    private final Clock clock;

    public InMemoryWebhookDeliveryRepository(Clock clock) {
        this.clock = clock;
    }

    @Override
    public WebhookDelivery save(WebhookDelivery delivery) {
        store.put(delivery.id(), delivery);
        return delivery;
    }

    @Override
    public int claimAndProcessDuePending(int batchSize, Consumer<WebhookDelivery> processor) {
        var now = clock.instant();
        List<WebhookDelivery> due = store.values().stream()
                .filter(d -> d.status() == DeliveryStatus.PENDING)
                .filter(d -> !d.nextAttemptAt().isAfter(now))
                .sorted((a, b) -> a.nextAttemptAt().compareTo(b.nextAttemptAt()))
                .limit(batchSize)
                .toList();
        for (WebhookDelivery d : due) {
            processor.accept(d);
            store.put(d.id(), d);
        }
        return due.size();
    }

    @Override
    public List<WebhookDelivery> findByPaymentId(UUID paymentId) {
        List<WebhookDelivery> result = new ArrayList<>();
        for (WebhookDelivery d : store.values()) {
            if (d.paymentId().equals(paymentId)) result.add(d);
        }
        return result;
    }
}
