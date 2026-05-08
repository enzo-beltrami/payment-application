package dev.brenzo.payment_application.adapter.out.scheduling;

import dev.brenzo.payment_application.application.service.WebhookDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WebhookDispatcherScheduler {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcherScheduler.class);

    private final WebhookDispatchService dispatcher;

    public WebhookDispatcherScheduler(WebhookDispatchService dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelayString = "${app.webhooks.dispatcher.poll-interval-ms:2000}")
    public void runOnce() {
        try {
            WebhookDispatchService.DispatchSummary summary = dispatcher.dispatchBatch();
            if (summary.claimed() > 0) {
                log.info("webhook dispatch batch: claimed={} delivered={} retried={} failed={}",
                        summary.claimed(), summary.delivered(), summary.retried(), summary.failed());
            }
        } catch (Exception e) {
            log.error("webhook dispatcher batch crashed", e);
        }
    }
}
