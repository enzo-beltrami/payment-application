package dev.brenzo.payment_application.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryJpaRepository extends JpaRepository<WebhookDeliveryJpaEntity, UUID> {

    @Query(value = """
            SELECT * FROM webhook_delivery
            WHERE status = 'PENDING' AND next_attempt_at <= now()
            ORDER BY next_attempt_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<WebhookDeliveryJpaEntity> claimDuePending(int batchSize);

    List<WebhookDeliveryJpaEntity> findAllByPaymentId(UUID paymentId);
}
