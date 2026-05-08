package dev.brenzo.payment_application.adapter.in.web;

import dev.brenzo.payment_application.application.service.PaymentService;
import dev.brenzo.payment_application.domain.payment.CardNumber;
import dev.brenzo.payment_application.domain.payment.InvalidCardNumberException;
import dev.brenzo.payment_application.domain.payment.Payment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(ApiExceptionHandler.class)
class PaymentControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean PaymentService paymentService;

    @Test
    void post_creates_payment_and_returns_201_with_masked_card() throws Exception {
        UUID id = UUID.fromString("f81d4fae-7dec-11d0-a765-00a0c91e6bf6");
        Payment p = new Payment(id, "Ada", "Lovelace", "10001",
                new CardNumber("4242424242424242"),
                Instant.parse("2026-05-07T19:53:00Z"));
        when(paymentService.create(any())).thenReturn(p);

        mvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Ada",
                                  "lastName": "Lovelace",
                                  "zipCode": "10001",
                                  "cardNumber": "4242 4242 4242 4242"
                                }"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/payments/" + id)))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.cardLast4").value("4242"))
                .andExpect(jsonPath("$.maskedCardNumber").value("**** **** **** 4242"))
                .andExpect(jsonPath("$.firstName").value("Ada"));
    }

    @Test
    void post_returns_400_with_validation_errors_for_blank_fields() throws Exception {
        mvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "firstName": "", "lastName": "", "zipCode": "", "cardNumber": "" }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void post_returns_400_when_card_fails_luhn() throws Exception {
        when(paymentService.create(any()))
                .thenThrow(new InvalidCardNumberException("card number failed Mod 10 (Luhn) check"));

        mvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Ada", "lastName": "Lovelace",
                                  "zipCode": "10001", "cardNumber": "4242424242424241"
                                }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_card_number"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Mod 10")));
    }

    @Test
    void get_returns_404_when_missing() throws Exception {
        when(paymentService.findById(any())).thenReturn(Optional.empty());

        mvc.perform(get("/api/payments/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_returns_payment_when_found() throws Exception {
        UUID id = UUID.randomUUID();
        Payment p = new Payment(id, "Ada", "Lovelace", "10001",
                new CardNumber("4242424242424242"), Instant.parse("2026-05-07T19:53:00Z"));
        when(paymentService.findById(id)).thenReturn(Optional.of(p));

        mvc.perform(get("/api/payments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardLast4").value("4242"));
    }

    @Test
    void post_returns_400_for_malformed_json() throws Exception {
        mvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("malformed_request"));
    }
}
