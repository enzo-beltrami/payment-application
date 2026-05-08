# Payment Application

A small payment service that:

- Exposes a REST API to create payments. Card numbers are validated with the **Mod 10 (Luhn)** algorithm and stored **encrypted** with AES-GCM.
- Allows registering **dynamic webhook subscriptions**. Every active webhook is POSTed a `payment.created` event after each successful payment.
- Delivers webhooks via a **transactional outbox + scheduled dispatcher** with exponential backoff. Delivery state lives in Postgres so retries survive restarts.
- Documents itself with **Springdoc OpenAPI** — both Swagger UI and a downloadable spec are exposed at runtime.

The codebase is organised around a **hexagonal architecture**: a Spring/JPA-free domain core, an application layer of services that depend on ports for the boundaries that warrant abstracting (persistence, encryption, outbound HTTP), and adapters that plug Postgres, AES-GCM, the JDK HTTP client, and Spring MVC into those ports. Trivial collaborators (the system clock, UUID generation, JSON serialization) use JDK/library types directly rather than bespoke ports.

---

## Prerequisites

- JDK **21** (the `./gradlew` wrapper will download Gradle itself).
- **Docker** and **Docker compose**(Postgres is provisioned by the bundled `compose.yaml` for local dev, and by Testcontainers for integration tests).

## Configuration

We have the required APP_ENCRYPTION_KEY:

```bash
export APP_ENCRYPTION_KEY=$(openssl rand -base64 32)
```

The key must be a Base64-encoded **16, 24, or 32-byte AES key**.

In case you have issues running the openssl command here is a sample encryption_key:\
```bash
export APP_ENCRYPTION_KEY=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=
```

Other knobs live in `application.properties` under `app.webhooks.dispatcher.*`: poll interval, batch size, max attempts, backoff cap, connect/read timeouts.

## Run it

```bash
./gradlew bootRun
```

The Spring Boot dev-tools docker-compose integration auto-starts the Postgres in `compose.yaml`. Flyway then applies `V1__init.sql` on startup.

Once it's up:

- API base: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

Save the spec to disk if you want to share it:

```bash
curl -s http://localhost:8080/v3/api-docs.yaml -o openapi.yaml
```

## Try it

Register a webhook receiver:

```bash
curl -X POST http://localhost:8080/api/webhooks \
  -H 'Content-Type: application/json' \
  -d '{ "url": "https://webhook.site/<your-token>" }'
```

Create a payment:

```bash
curl -X POST http://localhost:8080/api/payments \
  -H 'Content-Type: application/json' \
  -d '{
        "firstName": "Ada",
        "lastName": "Lovelace",
        "zipCode": "10001",
        "cardNumber": "4242 4242 4242 4242"
      }'
```

The response includes only the **last four** digits and a masked representation — the raw PAN is never echoed. Within a couple of seconds your webhook receiver will get a POST like:

```json
{
  "event": "payment.created",
  "deliveryId": "…",
  "payment": {
    "id": "…",
    "firstName": "Ada",
    "lastName": "Lovelace",
    "zipCode": "10001",
    "cardLast4": "4242",
    "createdAt": "…"
  }
}
```

If the receiver is down or returns a non-2xx status, the dispatcher retries with exponential backoff (2 s, 4 s, 8 s, … capped) for up to 8 attempts before marking the delivery as `FAILED`. Retries continue across application restarts because the queue lives in Postgres.

## Endpoints

Full request/response examples are embedded in the OpenAPI spec.

## Tests

```bash
./gradlew test
```

Three layers:

- **Unit tests** (no Spring, no DB) cover the domain core: Luhn validator, `CardNumber` value object, `WebhookDelivery` retry/backoff transitions, plus the application services with in-memory port fakes.
- **Slice tests** (`@WebMvcTest`) cover the controllers and the `@RestControllerAdvice` error mapping. They use `@MockitoBean` against the concrete service classes.
- An **end-to-end integration test** boots the full Spring context against a Testcontainers Postgres, registers a webhook pointing at an in-process HTTP server that fails the first call and succeeds the second, and asserts the payload shape, that retries are observed, and that the database column holds ciphertext rather than the PAN.

## Architecture

```
domain/                 pure Java — entities, value objects, domain services
application/
  port/out/             ports for boundaries worth abstracting:
                          PaymentRepository, WebhookSubscriptionRepository,
                          WebhookDeliveryRepository, CardEncryptor, WebhookSender
  service/              @Service classes (PaymentService, WebhookService,
                          WebhookDispatchService); controllers depend on these directly
adapter/
  in/web/               REST controllers, DTOs, exception handler
  out/persistence/      JPA entities + Spring Data + port adapters + AttributeConverter
  out/crypto/           AES-GCM card encryptor
  out/http/             RestClient webhook sender
  out/scheduling/       @Scheduled trigger that calls the dispatch service
  config/               @ConfigurationProperties beans
PaymentApplication      composition root: @SpringBootApplication, @EnableScheduling,
                          @EnableConfigurationProperties, @Bean Clock + Supplier<UUID>
```

The domain has no Spring or JPA imports. The application layer uses Spring's `@Service` and `@Transactional` for wiring and tx boundaries; everything else flows through constructor-injected ports.

## Known limitations & next steps

Items I deliberately scoped out to keep within the time budget. Each is something I'd address before exposing this to real traffic:

- **Dispatcher transaction scope.** `claimAndProcessDuePending` runs the outbound HTTP send inside the same transaction that holds `FOR UPDATE` on every claimed row. At low volume this is fine; under a slow receiver and a large batch, row locks are held for the duration of the sends. The right shape is to claim-and-mark `IN_FLIGHT` in a short transaction, send outside the lock, then update the result in a second transaction.
- **No protection on webhook URLs.** `RegisterWebhookRequest` validates URL syntax but does not block malicious URL's. A caller could register `http://localhost:8080/...` or cloud metadata endpoints. Before this is publicly reachable I'd add a denylist on resolution and reject non-`https` in production.
- **No signing of webhook payloads.** Receivers have no way to verify a POST originated from us. Standard next step is a per-subscription signing secret and a signature header.
- **No idempotency on `POST /api/payments`.** A client retry creates a duplicate payment plus duplicate webhook deliveries. An `Idempotency-Key` header backed by a short-TTL store is the conventional fix.
- **No authentication on the API itself.** Out of scope for this exercise but obviously required before any real deployment.
- **Webhook subscriptions are unowned.** No tenant/user model — anyone who can call the API can register or delete any subscription.
- **Synchronous fanout on payment creation.** `PaymentService.create` writes one `webhook_delivery` row per active subscription inside the payment's transaction. With a handful of subscribers this is negligible, but the cost (and transaction size) grows linearly with the subscription count and adds latency to the user-facing write. At higher volume I'd decouple the fanout: persist a single `payment.created` event row in the payment's transaction, then have a separate worker materialise per-subscription delivery rows asynchronously. Same outbox guarantees, but payment latency becomes O(1) regardless of subscriber count.
