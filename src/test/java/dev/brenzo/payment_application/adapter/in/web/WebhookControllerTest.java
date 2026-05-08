package dev.brenzo.payment_application.adapter.in.web;

import dev.brenzo.payment_application.application.service.WebhookService;
import dev.brenzo.payment_application.domain.webhook.WebhookSubscription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookController.class)
@Import(ApiExceptionHandler.class)
class WebhookControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean WebhookService webhookService;

    @Test
    void post_registers_webhook() throws Exception {
        UUID id = UUID.randomUUID();
        WebhookSubscription sub = new WebhookSubscription(id,
                "https://example.com/webhooks/payments", true, Instant.parse("2026-05-07T19:53:00Z"));
        when(webhookService.register(any())).thenReturn(sub);

        mvc.perform(post("/api/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "url": "https://example.com/webhooks/payments" }"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.url").value("https://example.com/webhooks/payments"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void post_returns_400_for_blank_url() throws Exception {
        mvc.perform(post("/api/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "url": "" }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"));
    }

    @Test
    void post_returns_400_for_non_http_scheme() throws Exception {
        when(webhookService.register(any()))
                .thenThrow(new IllegalArgumentException("webhook url must use http or https"));

        mvc.perform(post("/api/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "url": "ftp://example.com/x" }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_argument"));
    }

    @Test
    void list_returns_webhooks() throws Exception {
        UUID id = UUID.randomUUID();
        WebhookSubscription sub = new WebhookSubscription(id,
                "https://a.test/h", true, Instant.parse("2026-05-07T19:53:00Z"));
        when(webhookService.listActive()).thenReturn(List.of(sub));

        mvc.perform(get("/api/webhooks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].url").value("https://a.test/h"));
    }

    @Test
    void delete_returns_204_when_present() throws Exception {
        UUID id = UUID.randomUUID();
        when(webhookService.delete(id)).thenReturn(true);

        mvc.perform(delete("/api/webhooks/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns_404_when_missing() throws Exception {
        UUID id = UUID.randomUUID();
        when(webhookService.delete(id)).thenReturn(false);

        mvc.perform(delete("/api/webhooks/{id}", id))
                .andExpect(status().isNotFound());
    }
}
