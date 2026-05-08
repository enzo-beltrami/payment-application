package dev.brenzo.payment_application.domain.webhook;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class WebhookSubscription {

    private final UUID id;
    private final String url;
    private final boolean active;
    private final Instant createdAt;

    public WebhookSubscription(UUID id, String url, boolean active, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.url = validateUrl(url);
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    private static String validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("webhook url must not be blank");
        }
        try {
            URI parsed = new URI(url);
            String scheme = parsed.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("webhook url must use http or https");
            }
            if (parsed.getHost() == null) {
                throw new IllegalArgumentException("webhook url must include a host");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("webhook url is not a valid URI: " + e.getMessage());
        }
        return url;
    }

    public WebhookSubscription deactivate() {
        return new WebhookSubscription(id, url, false, createdAt);
    }

    public UUID id() { return id; }
    public String url() { return url; }
    public boolean active() { return active; }
    public Instant createdAt() { return createdAt; }
}
