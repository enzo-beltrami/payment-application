package dev.brenzo.payment_application.application.port.out;

import dev.brenzo.payment_application.domain.webhook.WebhookSubscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookSubscriptionRepository {

    WebhookSubscription save(WebhookSubscription subscription);

    Optional<WebhookSubscription> findById(UUID id);

    List<WebhookSubscription> findAllActive();
}
