package dev.brenzo.payment_application.application.port.out;

import dev.brenzo.payment_application.domain.payment.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);
}
