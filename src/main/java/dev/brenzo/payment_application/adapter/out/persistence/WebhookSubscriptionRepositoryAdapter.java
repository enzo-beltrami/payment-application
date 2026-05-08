package dev.brenzo.payment_application.adapter.out.persistence;

import dev.brenzo.payment_application.application.port.out.WebhookSubscriptionRepository;
import dev.brenzo.payment_application.domain.webhook.WebhookSubscription;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class WebhookSubscriptionRepositoryAdapter implements WebhookSubscriptionRepository {

    private final WebhookSubscriptionJpaRepository jpa;

    public WebhookSubscriptionRepositoryAdapter(WebhookSubscriptionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public WebhookSubscription save(WebhookSubscription subscription) {
        WebhookSubscriptionJpaEntity entity = jpa.findById(subscription.id())
                .orElseGet(WebhookSubscriptionJpaEntity::new);
        entity.setId(subscription.id());
        entity.setUrl(subscription.url());
        entity.setActive(subscription.active());
        entity.setCreatedAt(subscription.createdAt());
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<WebhookSubscription> findById(UUID id) {
        return jpa.findById(id).map(WebhookSubscriptionRepositoryAdapter::toDomain);
    }

    @Override
    public List<WebhookSubscription> findAllActive() {
        return jpa.findAllByActiveTrue().stream()
                .map(WebhookSubscriptionRepositoryAdapter::toDomain)
                .toList();
    }

    private static WebhookSubscription toDomain(WebhookSubscriptionJpaEntity e) {
        return new WebhookSubscription(e.getId(), e.getUrl(), e.isActive(), e.getCreatedAt());
    }
}
