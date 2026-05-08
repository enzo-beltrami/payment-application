package dev.brenzo.payment_application.support;

import dev.brenzo.payment_application.application.port.out.PaymentRepository;
import dev.brenzo.payment_application.domain.payment.Payment;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryPaymentRepository implements PaymentRepository {

    public final Map<UUID, Payment> store = new HashMap<>();

    @Override
    public Payment save(Payment payment) {
        store.put(payment.id(), payment);
        return payment;
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }
}
