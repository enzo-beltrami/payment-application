package dev.brenzo.payment_application.domain.payment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardNumberTest {

    @Test
    void strips_spaces_and_hyphens() {
        CardNumber c = new CardNumber("4242 4242-4242 4242");
        assertThat(c.value()).isEqualTo("4242424242424242");
        assertThat(c.last4().value()).isEqualTo("4242");
    }

    @Test
    void rejects_null() {
        assertThatThrownBy(() -> new CardNumber(null))
                .isInstanceOf(InvalidCardNumberException.class);
    }

    @Test
    void rejects_blank() {
        assertThatThrownBy(() -> new CardNumber("   "))
                .isInstanceOf(InvalidCardNumberException.class);
    }

    @Test
    void rejects_non_digits() {
        assertThatThrownBy(() -> new CardNumber("abcd-efgh-ijkl-mnop"))
                .isInstanceOf(InvalidCardNumberException.class)
                .hasMessageContaining("digits");
    }

    @Test
    void rejects_too_short() {
        assertThatThrownBy(() -> new CardNumber("123456789012"))
                .isInstanceOf(InvalidCardNumberException.class)
                .hasMessageContaining("length");
    }

    @Test
    void rejects_too_long() {
        assertThatThrownBy(() -> new CardNumber("12345678901234567890"))
                .isInstanceOf(InvalidCardNumberException.class)
                .hasMessageContaining("length");
    }

    @Test
    void rejects_failing_luhn() {
        assertThatThrownBy(() -> new CardNumber("4242424242424241"))
                .isInstanceOf(InvalidCardNumberException.class)
                .hasMessageContaining("Mod 10");
    }

    @Test
    void accepts_valid_pan() {
        CardNumber c = new CardNumber("4242424242424242");
        assertThat(c.last4().masked()).isEqualTo("**** **** **** 4242");
    }

    @Test
    void toString_does_not_leak_pan() {
        CardNumber c = new CardNumber("4242424242424242");
        assertThat(c.toString()).doesNotContain("4242424242424242");
        assertThat(c.toString()).contains("****4242");
    }
}
