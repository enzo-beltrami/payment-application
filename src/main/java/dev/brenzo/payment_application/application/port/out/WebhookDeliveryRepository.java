package dev.brenzo.payment_application.application.port.out;

import dev.brenzo.payment_application.domain.webhook.WebhookDelivery;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public interface WebhookDeliveryRepository {

    WebhookDelivery save(WebhookDelivery delivery);

    /**
     * Atomically claim up to {@code batchSize} due-pending deliveries and process each within
     * its own transaction. Implementations should use {@code SELECT ... FOR UPDATE SKIP LOCKED}
     * (or equivalent) so concurrent dispatcher instances do not double-deliver.
     *
     * <p>The processor receives the rehydrated domain {@link WebhookDelivery}; any mutations it
     * performs are persisted when the per-row transaction commits.
     *
     * @return the number of deliveries claimed and handed to the processor.
     */
    int claimAndProcessDuePending(int batchSize, Consumer<WebhookDelivery> processor);

    List<WebhookDelivery> findByPaymentId(UUID paymentId);
}
