package dev.brenzo.payment_application.support;

import dev.brenzo.payment_application.application.port.out.WebhookSubscriptionRepository;
import dev.brenzo.payment_application.domain.webhook.WebhookSubscription;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryWebhookSubscriptionRepository implements WebhookSubscriptionRepository {

    public final Map<UUID, WebhookSubscription> store = new LinkedHashMap<>();

    @Override
    public WebhookSubscription save(WebhookSubscription subscription) {
        store.put(subscription.id(), subscription);
        return subscription;
    }

    @Override
    public Optional<WebhookSubscription> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<WebhookSubscription> findAllActive() {
        return store.values().stream().filter(WebhookSubscription::active).toList();
    }
}
