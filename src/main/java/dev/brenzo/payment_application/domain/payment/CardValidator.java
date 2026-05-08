package dev.brenzo.payment_application.domain.payment;

public final class CardValidator {

    private CardValidator() {
    }

    public static boolean isValid(String digits) {
        if (digits == null || digits.isEmpty()) {
            return false;
        }
        int sum = 0;
        boolean doubleNext = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            char c = digits.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
            int n = c - '0';
            if (doubleNext) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            doubleNext = !doubleNext;
        }
        return sum % 10 == 0;
    }
}
