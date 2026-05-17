# Cart Checkout + Mock Payment System

Single Spring Boot service implementing the [hiring-quest](requirements.md)

The system is small in scope but operates under strict correctness constraints  **it handles money**.  
Order state must always be correct, no double payments, no invalid transitions, and webhooks must be safe to receive twice.

---

## Quick start

```bash
./mvnw clean package           # build
./mvnw spring-boot:run         # run
./mvnw test                    # test
```

App: <http://localhost:8080> ·  
H2 console: <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:checkout`, user `sa`, no password).

Postman: import [Cart-Checkout.postman_collection.json](Cart-Checkout.postman_collection.json).  
The collection walks all three required user flows (happy path, retry, duplicate webhook) + edge cases.

---

## Endpoints

| Method | Path | Controller |
|---|---|---|
| `POST` | `/carts` | [CartController.java](src/main/java/com/cart/checkout/api/CartController.java) |
| `GET`  | `/carts/{cartId}` | [CartController.java](src/main/java/com/cart/checkout/api/CartController.java) |
| `POST` | `/carts/{cartId}/items` | [CartController.java](src/main/java/com/cart/checkout/api/CartController.java) |
| `POST` | `/carts/{cartId}/checkout` | [CheckoutController.java](src/main/java/com/cart/checkout/api/CheckoutController.java) |
| `GET`  | `/orders/{orderId}` | [OrderController.java](src/main/java/com/cart/checkout/api/OrderController.java) |
| `POST` | `/orders/{orderId}/payment/start` | [PaymentController.java](src/main/java/com/cart/checkout/api/PaymentController.java) |
| `POST` | `/payments/webhook` | [WebhookController.java](src/main/java/com/cart/checkout/api/WebhookController.java) |
| `POST` | `/mock-provider/{providerPaymentId}/trigger?outcome=CONFIRMED\|FAILED` | [MockProviderController.java](src/main/java/com/cart/checkout/infrastructure/MockProviderController.java) |

The mock-provider `trigger` endpoint simulates the provider deciding the outcome; it then POSTs a webhook back to `/payments/webhook`. That's the demo loop.

---

## Unit & Integration tests

- [Unit Tests](src/test/java/com/cart/checkout): 42 tests covering all state machines and domain logic
- [Integration Tests](src/test/java/com/cart/checkout): 6 tests covering the happy path and duplicate webhook idempotency

---

## Project structure

```
com.cart.checkout
├── CheckoutApplication.java           // entry point
├── api/                               // controllers, DTOs, global exception handler
├── domain/
│   ├── cart/      Cart, CartItem, CartStatus
│   ├── order/     Order, OrderItem, OrderStatus
│   └── payment/   Payment, PaymentStatus + port/PaymentProvider
├── service/                           // orchestration (CartService, CheckoutService, ...)
├── repository/                        // JPA repositories
├── infrastructure/                    // MockPaymentProvider, MockProviderController
└── exceptions/                        // domain exceptions
```

Each context layers as **controller → service → repository → aggregate**. Cross-context references are by ID only  no direct aggregate references. Full diagrams and rationale in [ARCHITECTURE.md](ARCHITECTURE.md).

---



## AI usage disclosure

Per the assignment's AI usage policy:

- **[ai_usage_policy/AI_USAGE.md](ai_usage_policy/AI_USAGE.md)**  primary disclosure: what was AI-assisted vs. what I owned, where I corrected the AI, and a planning-session summary.
- **[ai_usage_policy/BRAIN_STORMING_CONVERSATION.md](ai_usage_policy/BRAIN_STORMING_CONVERSATION.md)**  verbatim transcript of the design session.

Design rationale: [DESIGN.md](ai_usage_policy/DESIGN.md), [DECISIONS.md](ai_usage_policy/DECISIONS.md), [DESIGN_ARCH_TRADEOFFS.md](ai_usage_policy/DESIGN_ARCH_TRADEOFFS.md), [IMPLEMENTATION_DECISIONS.md](ai_usage_policy/IMPLEMENTATION_DECISIONS.md).
