package dev.brenzo.payment_application.application.service;

import dev.brenzo.payment_application.application.port.out.WebhookSubscriptionRepository;
import dev.brenzo.payment_application.domain.webhook.WebhookSubscription;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class WebhookService {

    private final WebhookSubscriptionRepository subscriptions;
    private final Supplier<UUID> ids;
    private final Clock clock;

    public WebhookService(WebhookSubscriptionRepository subscriptions, Supplier<UUID> ids, Clock clock) {
        this.subscriptions = subscriptions;
        this.ids = ids;
        this.clock = clock;
    }

    @Transactional
    public WebhookSubscription register(String url) {
        WebhookSubscription sub = new WebhookSubscription(ids.get(), url, true, clock.instant());
        return subscriptions.save(sub);
    }

    @Transactional(readOnly = true)
    public List<WebhookSubscription> listActive() {
        return subscriptions.findAllActive();
    }

    @Transactional
    public boolean delete(UUID id) {
        Optional<WebhookSubscription> existing = subscriptions.findById(id);
        if (existing.isEmpty() || !existing.get().active()) {
            return false;
        }
        subscriptions.save(existing.get().deactivate());
        return true;
    }
}
