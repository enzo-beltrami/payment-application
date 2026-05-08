package dev.brenzo.payment_application;

import dev.brenzo.payment_application.adapter.config.WebhookDispatcherProperties;
import dev.brenzo.payment_application.adapter.out.crypto.EncryptionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({EncryptionProperties.class, WebhookDispatcherProperties.class})
public class PaymentApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentApplication.class, args);
	}

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	public Supplier<UUID> ids() {
		return UUID::randomUUID;
	}

}
