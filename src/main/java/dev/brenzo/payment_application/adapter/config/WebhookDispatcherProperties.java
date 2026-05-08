package dev.brenzo.payment_application.adapter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.webhooks.dispatcher")
public class WebhookDispatcherProperties {

    private long pollIntervalMs = 2_000L;
    private int batchSize = 50;
    private int maxAttempts = 8;
    private int backoffCapSeconds = 3_600;
    private int connectTimeoutMs = 5_000;
    private int readTimeoutMs = 10_000;

    public long getPollIntervalMs() { return pollIntervalMs; }
    public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public int getBackoffCapSeconds() { return backoffCapSeconds; }
    public void setBackoffCapSeconds(int backoffCapSeconds) { this.backoffCapSeconds = backoffCapSeconds; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
}
