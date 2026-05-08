package dev.brenzo.payment_application.adapter.out.http;

import dev.brenzo.payment_application.adapter.config.WebhookDispatcherProperties;
import dev.brenzo.payment_application.application.port.out.WebhookSender;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class RestClientWebhookSender implements WebhookSender {

    private final RestClient restClient;

    public RestClientWebhookSender(WebhookDispatcherProperties props) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(props.getReadTimeoutMs()));
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Override
    public SendResult send(String url, String jsonBody) {
        try {
            int status = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonBody)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode()
                    .value();
            return SendResult.ok(status);
        } catch (RestClientResponseException e) {
            return SendResult.httpError(e.getStatusCode().value(), e.getMessage());
        } catch (ResourceAccessException e) {
            return SendResult.transportError(e.getMessage());
        } catch (Exception e) {
            return SendResult.transportError(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
