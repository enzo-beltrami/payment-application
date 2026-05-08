package dev.brenzo.payment_application.domain.webhook;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookDeliveryTest {

    private static final Instant T0 = Instant.parse("2026-05-07T19:53:00Z");

    private WebhookDelivery newDelivery() {
        return WebhookDelivery.newPending(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "https://example.test/hook", "{}", T0);
    }

    @Test
    void newPending_starts_at_zero_attempts_and_due_now() {
        WebhookDelivery d = newDelivery();
        assertThat(d.status()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(d.attempts()).isZero();
        assertThat(d.nextAttemptAt()).isEqualTo(T0);
        assertThat(d.lastResponseCode()).isNull();
        assertThat(d.lastError()).isNull();
    }

    @Test
    void markDelivered_records_status_and_response_code() {
        WebhookDelivery d = newDelivery();
        Instant t1 = T0.plusSeconds(1);
        d.markDelivered(t1, 204);
        assertThat(d.status()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(d.attempts()).isEqualTo(1);
        assertThat(d.lastResponseCode()).isEqualTo(204);
        assertThat(d.lastError()).isNull();
        assertThat(d.updatedAt()).isEqualTo(t1);
    }

    @Test
    void retries_use_exponential_backoff() {
        WebhookDelivery d = newDelivery();

        d.markFailedAndScheduleRetry(T0, 500, "boom", 8, 3600);
        assertThat(d.status()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(d.attempts()).isEqualTo(1);
        assertThat(d.nextAttemptAt()).isEqualTo(T0.plusSeconds(2)); // 2^1

        d.markFailedAndScheduleRetry(T0, 500, "boom", 8, 3600);
        assertThat(d.attempts()).isEqualTo(2);
        assertThat(d.nextAttemptAt()).isEqualTo(T0.plusSeconds(4)); // 2^2

        d.markFailedAndScheduleRetry(T0, 500, "boom", 8, 3600);
        assertThat(d.attempts()).isEqualTo(3);
        assertThat(d.nextAttemptAt()).isEqualTo(T0.plusSeconds(8)); // 2^3
    }

    @Test
    void backoff_is_capped() {
        WebhookDelivery d = newDelivery();
        // attempts=1..5 with cap=10 → 2,4,8,10,10
        d.markFailedAndScheduleRetry(T0, 500, "x", 10, 10);
        assertThat(d.nextAttemptAt()).isEqualTo(T0.plusSeconds(2));
        d.markFailedAndScheduleRetry(T0, 500, "x", 10, 10);
        assertThat(d.nextAttemptAt()).isEqualTo(T0.plusSeconds(4));
        d.markFailedAndScheduleRetry(T0, 500, "x", 10, 10);
        assertThat(d.nextAttemptAt()).isEqualTo(T0.plusSeconds(8));
        d.markFailedAndScheduleRetry(T0, 500, "x", 10, 10);
        assertThat(d.nextAttemptAt()).isEqualTo(T0.plusSeconds(10));
        d.markFailedAndScheduleRetry(T0, 500, "x", 10, 10);
        assertThat(d.nextAttemptAt()).isEqualTo(T0.plusSeconds(10));
    }

    @Test
    void transitions_to_FAILED_after_max_attempts() {
        WebhookDelivery d = newDelivery();
        for (int i = 0; i < 2; i++) {
            d.markFailedAndScheduleRetry(T0, 503, "down", 3, 3600);
            assertThat(d.status()).isEqualTo(DeliveryStatus.PENDING);
        }
        d.markFailedAndScheduleRetry(T0, 503, "down", 3, 3600);
        assertThat(d.status()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(d.attempts()).isEqualTo(3);
        assertThat(d.lastError()).isEqualTo("down");
        assertThat(d.lastResponseCode()).isEqualTo(503);
    }

    @Test
    void markDelivered_after_failures_clears_error_and_marks_delivered() {
        WebhookDelivery d = newDelivery();
        d.markFailedAndScheduleRetry(T0, 503, "down", 8, 3600);
        d.markDelivered(T0.plusSeconds(10), 200);
        assertThat(d.status()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(d.lastError()).isNull();
        assertThat(d.lastResponseCode()).isEqualTo(200);
        assertThat(d.attempts()).isEqualTo(2);
    }
}
