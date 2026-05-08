package dev.brenzo.payment_application.domain.payment;

import java.util.Objects;

public final class CardLast4 {

    private final String value;

    public CardLast4(String value) {
        if (value == null || value.length() != 4 || !value.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("CardLast4 must be exactly 4 digits");
        }
        this.value = value;
    }

    public String value() {
        return value;
    }

    public String masked() {
        return "**** **** **** " + value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CardLast4 other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return masked();
    }
}
