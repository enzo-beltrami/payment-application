package dev.brenzo.payment_application.domain.payment;

public class InvalidCardNumberException extends RuntimeException {

    public InvalidCardNumberException(String message) {
        super(message);
    }
}
