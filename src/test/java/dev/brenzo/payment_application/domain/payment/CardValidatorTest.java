package dev.brenzo.payment_application.domain.payment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CardValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "4242424242424242", // Visa test
            "4111111111111111", // Visa test
            "5555555555554444", // Mastercard test
            "378282246310005",  // Amex test
            "6011111111111117", // Discover test
            "4222222222222"     // 13-digit Visa
    })
    void accepts_known_valid_pans(String pan) {
        assertThat(CardValidator.isValid(pan)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "4242424242424241",
            "4111111111111112",
            "1234567812345678"
    })
    void rejects_known_invalid_pans(String pan) {
        assertThat(CardValidator.isValid(pan)).isFalse();
    }

    @Test
    void rejects_null() {
        assertThat(CardValidator.isValid(null)).isFalse();
    }

    @Test
    void rejects_empty() {
        assertThat(CardValidator.isValid("")).isFalse();
    }

    @Test
    void rejects_non_digits() {
        assertThat(CardValidator.isValid("4242-4242-4242-4242")).isFalse();
        assertThat(CardValidator.isValid("4242abc4242424242")).isFalse();
    }

    @Test
    void single_zero_is_valid() {
        // sum = 0, divisible by 10
        assertThat(CardValidator.isValid("0")).isTrue();
    }
}
