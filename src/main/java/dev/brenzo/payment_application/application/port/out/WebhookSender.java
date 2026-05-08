package dev.brenzo.payment_application.application.port.out;

public interface WebhookSender {

    SendResult send(String url, String jsonBody);

    record SendResult(boolean success, Integer statusCode, String errorMessage) {

        public static SendResult ok(int statusCode) {
            return new SendResult(true, statusCode, null);
        }

        public static SendResult httpError(int statusCode, String message) {
            return new SendResult(false, statusCode, message);
        }

        public static SendResult transportError(String message) {
            return new SendResult(false, null, message);
        }
    }
}
