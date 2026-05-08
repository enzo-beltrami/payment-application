package dev.brenzo.payment_application.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {
}
