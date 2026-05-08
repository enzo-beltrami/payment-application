package dev.brenzo.payment_application.adapter.in.web;

import dev.brenzo.payment_application.adapter.in.web.dto.CreatePaymentRequest;
import dev.brenzo.payment_application.adapter.in.web.dto.ErrorResponse;
import dev.brenzo.payment_application.adapter.in.web.dto.PaymentResponse;
import dev.brenzo.payment_application.application.service.PaymentService;
import dev.brenzo.payment_application.domain.payment.Payment;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Create and retrieve payments. Card numbers are encrypted at rest and never returned in full.")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Create a payment",
            description = "Validates the card number with the Mod 10 (Luhn) algorithm, encrypts it with AES-GCM, and persists the payment. After persistence every active webhook receives a `payment.created` event; delivery is asynchronous and resilient to receiver failures.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreatePaymentRequest.class),
                            examples = @ExampleObject(name = "valid", value = """
                                    {
                                      "firstName": "Ada",
                                      "lastName": "Lovelace",
                                      "zipCode": "10001",
                                      "cardNumber": "4242 4242 4242 4242"
                                    }
                                    """))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment created.",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class),
                            examples = @ExampleObject(name = "created", value = """
                                    {
                                      "id": "f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
                                      "firstName": "Ada",
                                      "lastName": "Lovelace",
                                      "zipCode": "10001",
                                      "cardLast4": "4242",
                                      "maskedCardNumber": "**** **** **** 4242",
                                      "createdAt": "2026-05-07T19:53:00Z"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Validation failure (missing field or invalid card number).",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "invalidCard", value = """
                                    {
                                      "error": "invalid_card_number",
                                      "message": "card number failed Mod 10 (Luhn) check",
                                      "fieldErrors": []
                                    }
                                    """)))
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody CreatePaymentRequest request) {

        Payment created = paymentService.create(new PaymentService.CreatePaymentCommand(
                request.firstName(),
                request.lastName(),
                request.zipCode(),
                request.cardNumber()));

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(PaymentResponse.from(created));
    }

    @Operation(summary = "Get a payment by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found.",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "404", description = "No payment with that id.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> get(@PathVariable UUID id) {
        return paymentService.findById(id)
                .map(p -> ResponseEntity.ok(PaymentResponse.from(p)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
