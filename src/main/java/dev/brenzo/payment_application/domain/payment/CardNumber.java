package dev.brenzo.payment_application.domain.payment;

import java.util.Objects;

public final class CardNumber {

    private static final int MIN_LENGTH = 13;
    private static final int MAX_LENGTH = 19;

    private final String digits;

    public CardNumber(String raw) {
        if (raw == null) {
            throw new InvalidCardNumberException("card number is required");
        }
        String stripped = raw.replaceAll("[\\s-]", "");
        if (stripped.isEmpty()) {
            throw new InvalidCardNumberException("card number is required");
        }
        if (!stripped.chars().allMatch(Character::isDigit)) {
            throw new InvalidCardNumberException("card number must contain only digits, spaces, or hyphens");
        }
        if (stripped.length() < MIN_LENGTH || stripped.length() > MAX_LENGTH) {
            throw new InvalidCardNumberException(
                    "card number length must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " digits");
        }
        if (!CardValidator.isValid(stripped)) {
            throw new InvalidCardNumberException("card number failed Mod 10 (Luhn) check");
        }
        this.digits = stripped;
    }

    public String value() {
        return digits;
    }

    public CardLast4 last4() {
        return new CardLast4(digits.substring(digits.length() - 4));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CardNumber other)) return false;
        return digits.equals(other.digits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(digits);
    }

    @Override
    public String toString() {
        return "CardNumber[****" + digits.substring(digits.length() - 4) + "]";
    }
}
