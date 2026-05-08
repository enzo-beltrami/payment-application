package dev.brenzo.payment_application.application.service;

import dev.brenzo.payment_application.adapter.config.WebhookDispatcherProperties;
import dev.brenzo.payment_application.application.port.out.WebhookDeliveryRepository;
import dev.brenzo.payment_application.application.port.out.WebhookSender;
import dev.brenzo.payment_application.domain.webhook.DeliveryStatus;
import dev.brenzo.payment_application.domain.webhook.WebhookDelivery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class WebhookDispatchService {

    private final WebhookDeliveryRepository deliveries;
    private final WebhookSender sender;
    private final Clock clock;
    private final int batchSize;
    private final int maxAttempts;
    private final int backoffCapSeconds;

    public WebhookDispatchService(WebhookDeliveryRepository deliveries,
                                  WebhookSender sender,
                                  Clock clock,
                                  WebhookDispatcherProperties props) {
        this.deliveries = deliveries;
        this.sender = sender;
        this.clock = clock;
        this.batchSize = props.getBatchSize();
        this.maxAttempts = props.getMaxAttempts();
        this.backoffCapSeconds = props.getBackoffCapSeconds();
    }

    public DispatchSummary dispatchBatch() {
        AtomicInteger delivered = new AtomicInteger();
        AtomicInteger retried = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        int claimed = deliveries.claimAndProcessDuePending(batchSize, delivery -> {
            WebhookSender.SendResult result;
            try {
                result = sender.send(delivery.url(), delivery.payloadJson());
            } catch (RuntimeException ex) {
                result = WebhookSender.SendResult.transportError(ex.getMessage());
            }
            applyResult(delivery, result, delivered, retried, failed);
        });

        return new DispatchSummary(claimed, delivered.get(), retried.get(), failed.get());
    }

    private void applyResult(WebhookDelivery delivery,
                             WebhookSender.SendResult result,
                             AtomicInteger delivered,
                             AtomicInteger retried,
                             AtomicInteger failed) {
        if (result.success()) {
            delivery.markDelivered(clock.instant(), result.statusCode());
            delivered.incrementAndGet();
            return;
        }
        delivery.markFailedAndScheduleRetry(
                clock.instant(),
                result.statusCode(),
                result.errorMessage(),
                maxAttempts,
                backoffCapSeconds);
        if (delivery.status() == DeliveryStatus.FAILED) {
            log.warn("Webhook delivery failed permanently: id={} url={} attempts={} status={} error={}",
                    delivery.id(), delivery.url(), delivery.attempts(),
                    result.statusCode(), result.errorMessage());
            failed.incrementAndGet();
        } else {
            log.warn("Webhook delivery failed, scheduled retry: id={} url={} attempts={} status={} error={} nextAttemptAt={}",
                    delivery.id(), delivery.url(), delivery.attempts(),
                    result.statusCode(), result.errorMessage(), delivery.nextAttemptAt());
            retried.incrementAndGet();
        }
    }

    public record DispatchSummary(int claimed, int delivered, int retried, int failed) {
    }
}
