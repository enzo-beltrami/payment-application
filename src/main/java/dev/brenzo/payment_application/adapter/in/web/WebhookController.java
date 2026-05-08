package dev.brenzo.payment_application.adapter.in.web;

import dev.brenzo.payment_application.adapter.in.web.dto.ErrorResponse;
import dev.brenzo.payment_application.adapter.in.web.dto.RegisterWebhookRequest;
import dev.brenzo.payment_application.adapter.in.web.dto.WebhookResponse;
import dev.brenzo.payment_application.application.service.WebhookService;
import dev.brenzo.payment_application.domain.webhook.WebhookSubscription;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/webhooks")
@Tag(name = "Webhooks", description = "Register endpoints to receive `payment.created` events.")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @Operation(summary = "Register a webhook subscription",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RegisterWebhookRequest.class),
                            examples = @ExampleObject(name = "register", value = """
                                    {
                                      "url": "https://example.com/webhooks/payments"
                                    }
                                    """))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Webhook registered.",
                    content = @Content(schema = @Schema(implementation = WebhookResponse.class),
                            examples = @ExampleObject(name = "registered", value = """
                                    {
                                      "id": "550e8400-e29b-41d4-a716-446655440000",
                                      "url": "https://example.com/webhooks/payments",
                                      "active": true,
                                      "createdAt": "2026-05-07T19:53:00Z"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid URL.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<WebhookResponse> register(@Valid @RequestBody RegisterWebhookRequest request) {
        WebhookSubscription created = webhookService.register(request.url());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(WebhookResponse.from(created));
    }

    @Operation(summary = "List active webhook subscriptions")
    @ApiResponse(responseCode = "200", description = "Active webhooks.",
            content = @Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                    schema = @Schema(implementation = WebhookResponse.class))))
    @GetMapping
    public List<WebhookResponse> list() {
        return webhookService.listActive().stream().map(WebhookResponse::from).toList();
    }

    @Operation(summary = "Deactivate a webhook subscription",
            description = "Soft-delete: marks the subscription inactive. In-flight delivery retries continue to completion or final failure.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deactivated."),
            @ApiResponse(responseCode = "404", description = "No active webhook with that id.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        boolean removed = webhookService.delete(id);
        if (!removed) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.noContent().build();
    }
}
