package dev.brenzo.payment_application.adapter.out.persistence;

import dev.brenzo.payment_application.application.port.out.PaymentRepository;
import dev.brenzo.payment_application.domain.payment.CardNumber;
import dev.brenzo.payment_application.domain.payment.Payment;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpa;

    public PaymentRepositoryAdapter(PaymentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Payment save(Payment payment) {
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.setId(payment.id());
        entity.setFirstName(payment.firstName());
        entity.setLastName(payment.lastName());
        entity.setZipCode(payment.zipCode());
        entity.setCardNumber(payment.cardNumber().value());
        entity.setCardLastFour(payment.cardLast4().value());
        entity.setCreatedAt(payment.createdAt());
        PaymentJpaEntity saved = jpa.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpa.findById(id).map(PaymentRepositoryAdapter::toDomain);
    }

    private static Payment toDomain(PaymentJpaEntity e) {
        return new Payment(
                e.getId(),
                e.getFirstName(),
                e.getLastName(),
                e.getZipCode(),
                new CardNumber(e.getCardNumber()),
                e.getCreatedAt());
    }
}
