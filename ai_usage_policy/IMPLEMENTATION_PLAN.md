# Implementation Plan — Cart Checkout + Mock Payment System

## Context

The repo is a Spring Boot 4 / Java 25 scaffold ([pom.xml](pom.xml), [CheckoutApplication.java](src/main/java/com/cart/checkout/CheckoutApplication.java)). The full design — bounded contexts, aggregates, state machines, idempotency strategy, exception mapping — is authoritative in [ai_usage_policy/DESIGN.md](ai_usage_policy/DESIGN.md) and the trade-offs companion [ai_usage_policy/DESIGN_ARCH_TRADEOFFS.md](ai_usage_policy/DESIGN_ARCH_TRADEOFFS.md). This plan turns that design into concrete files and a build order. Nothing in the design needs to be re-litigated; the goal is a faithful, testable implementation that wins the evaluation on the four weighted criteria (state-machine correctness 25%, idempotency 25%, code clarity 25%, testing 20%).

The submission deadline is 2026-05-17 (today is 2026-05-15), so the plan favors shipping a complete, correct implementation over polish.

---

## Package Layout

All under `com.cart.checkout`. Top-level packages are technical **layers** (`domain`, `repository`, `service`, `api`, `infrastructure`); each layer's bounded contexts live as sub-packages. This makes layer boundaries grep-able and keeps each layer's surface visible at a glance. Cross-context rules (Cart never imports Order; Order never imports Payment; Payment references Order by ID only) are conventions enforced by code review and the domain types themselves (e.g. `Order` stores a `UUID cartId`, not a `Cart` field).

```
com.cart.checkout
├── CheckoutApplication.java                 
│
├── domain
│   ├── cart
│   │   ├── Cart.java                        aggregate root (@Entity, @Version)
│   │   ├── CartItem.java                    entity
│   │   └── CartStatus.java                  OPEN | LOCKED
│   ├── order
│   │   ├── Order.java                       aggregate root, owns state machine, @Version
│   │   ├── OrderItem.java
│   │   └── OrderStatus.java                 CREATED | PENDING_PAYMENT | PAYMENT_FAILED | PAID
│   └── payment
│       ├── Payment.java                     aggregate root, @Version
│       ├── PaymentStatus.java               INITIATED | CONFIRMED | FAILED
│       └── port
│           ├── PaymentProvider.java         outbound port — initiatePayment(paymentId, amount)
│           └── ProviderInitiationResult.java
│
├── repository
│   ├── CartRepository.java                  JpaRepository<Cart, UUID>
│   ├── OrderRepository.java                 JpaRepository<Order, UUID>
│   └── PaymentRepository.java               JpaRepository<Payment, UUID>
│
├── service
│   ├── CartService.java                     create, addItem (loads aggregate, calls behavior, saves)
│   ├── CheckoutService.java                 checkout(cartId) — loads Cart, locks, snapshots into Order
│   ├── PaymentService.java                  startPayment(orderId) — one @Transactional
│   └── WebhookService.java                  handle(payload) — one @Transactional, idempotent
│
├── api
│   ├── CartController.java                  POST /carts, POST /carts/{id}/items
│   ├── CheckoutController.java              POST /carts/{cartId}/checkout
│   ├── PaymentController.java               POST /orders/{orderId}/payment/start
│   ├── WebhookController.java               POST /payments/webhook
│   ├── GlobalExceptionHandler.java          @RestControllerAdvice — domain exception → HTTP
│   ├── ErrorResponse.java
│   └── dto
│       ├── AddItemRequest.java
│       ├── CartResponse.java
│       ├── CartItemResponse.java
│       ├── OrderResponse.java
│       ├── PaymentStartResponse.java
│       └── WebhookPayload.java              { paymentId, providerPaymentId, outcome }
│
├── exceptions
│   ├── CartLockedException.java             thrown by Cart.addItem on LOCKED → 409
│   ├── InvalidOrderTransitionException.java thrown by Order.mark* on illegal source state → 409
│   ├── InvalidPaymentTransitionException.java thrown by Payment.mark* on terminal status → 409
│   └── ResourceNotFoundException.java       thrown by services on repo miss → 404
│
└── infrastructure
    ├── MockPaymentProvider.java             implements PaymentProvider — generates providerPaymentId,
    │                                         stores pending in-memory map keyed by providerPaymentId
    ├── MockProviderController.java          POST /mock-provider/{providerPaymentId}/trigger?outcome=…
    └── MockProviderClient.java              wraps Spring RestClient; POSTs to /payments/webhook
```

Notes:
- **All exceptions in one place**: `exceptions/` sits at the top level. Domain aggregates and services import from it, the `GlobalExceptionHandler` references them by type for HTTP mapping. Trade-off: domain code now imports a sibling package instead of throwing from its own package — accepted for centralization.
- **Ports stay with their domain**: `domain.payment.port.PaymentProvider` is owned by the Payment domain (it's the consumer that defines the contract). The implementation in `infrastructure` plugs in from outside; nothing in `domain.*` imports `infrastructure.*`.
- **No shared kernel / no `Money` value object.** Monetary fields are plain `BigDecimal` columns with `precision = 19, scale = 2`. Currency is implicit (single-currency system) and not stored. Trade-off: we lose compile-time cross-currency safety and a single home for monetary arithmetic; we accept it because the brief is single-currency and the persisted shape is simpler (one column per amount, no `@Embeddable`).
- **Trade-off vs. context-first layout**: this layout loses package-private enforcement of the "Cart never imports Order" rule — anything in `domain.order` *could* reach into `domain.cart`. We accept this; the rules are simple enough to enforce by review, and the persistence model (`Order.cartId : UUID` rather than `Order.cart : Cart`) makes accidental violations visible.

---

## Logging Convention

Every service class logs via SLF4J using the pattern already established in [CartService.java](src/main/java/com/cart/checkout/service/CartService.java):

```java
private static final Logger log = LoggerFactory.getLogger(<Class>.class);
...
log.info("Created a new cart {}", cart.getId());
```

Rules — apply to every service added in the build order below:

- **One logger per service**, declared `private static final Logger log = LoggerFactory.getLogger(<Class>.class);`. Field name is always `log`.
- **Always use the parameterized form** (`log.info("... {}", arg)`) — never string concatenation, never `String.format`. This is what `CartService.createCart()` does and what reviewers will look for elsewhere.
- **INFO** on every successful state transition or aggregate persistence, with the aggregate id in the message:
  - `CartService`: cart created, item added (`cartId`, `productId`, new total).
  - `CheckoutService`: cart locked + order created (`cartId`, `orderId`).
  - `PaymentService`: payment initiated (`orderId`, `paymentId`, `providerPaymentId`); order → PENDING_PAYMENT.
  - `WebhookService`: webhook received (`paymentId`, outcome); payment → CONFIRMED/FAILED; order → PAID/PAYMENT_FAILED; **and** duplicate-webhook fast-path hit (so idempotency is observable in logs).
  - `MockPaymentProvider` / `MockProviderController`: provider id generated, trigger received, webhook dispatched.
- **WARN** when a domain exception is thrown out of a service path — illegal state transition, locked cart, unknown providerPaymentId. Log before re-throwing so the cause is visible even when the advice maps it to 409/404.
- **ERROR** only for unexpected infrastructure failures (RestClient call to `/payments/webhook` fails, DB I/O fails, optimistic-lock retry exhausted). Reserve for things that warrant on-call attention.
- **Domain aggregates (`Cart`, `Order`, `Payment`) do NOT log.** Keep them pure and free of framework dependencies. Logging lives in services, controllers' exception advice, and infrastructure adapters.
- **Never log sensitive data.** IDs and statuses only. No full webhook payloads beyond ids/outcome, no monetary amounts in WARN/ERROR paths that might be replayed in shared logs.
- `GlobalExceptionHandler` logs each mapped exception once at the appropriate level (WARN for 4xx domain exceptions, ERROR for 5xx) so failures aren't silently swallowed.

---

## Build Order — Use-Case Slices

Each step is a **vertical slice** delivering one use case end-to-end: aggregate behavior + repository + service + controller + DTOs + exceptions, plus the test that proves it works. After each step you should be able to `./mvnw spring-boot:run` and hit the new endpoint with curl/Postman before moving on.

### Step 0 — Foundation (prerequisite, not a use case)

Pure plumbing needed before any endpoint can be wired. No HTTP surface added.

**Files**
- `api/ErrorResponse.java` — `{ String code, String message, Instant timestamp }`.
- `api/GlobalExceptionHandler.java` — `@RestControllerAdvice`. Maps `CartLockedException`, `InvalidOrderTransitionException`, `InvalidPaymentTransitionException`, `OptimisticLockingFailureException` → 409; `ResourceNotFoundException` → 404; `MethodArgumentNotValidException` → 400. (Exception classes themselves are stubs at this point — created here so the advice compiles, behavior added in their respective slices.)
- `exceptions/CartLockedException.java`, `InvalidOrderTransitionException.java`, `InvalidPaymentTransitionException.java`, `ResourceNotFoundException.java` — empty stub bodies extending `RuntimeException` with a `(String message)` constructor.
- `src/main/resources/application.properties` — `spring.jpa.hibernate.ddl-auto=create-drop`, `spring.h2.console.enabled=true`, `spring.jpa.open-in-view=false`, `mock.provider.callback-base-url=http://localhost:8080`.

**Verify**: `./mvnw spring-boot:run` boots cleanly with H2 console at `http://localhost:8080/h2-console`.

---

### Step 1 — Use case: Create empty cart

**Endpoint**: `POST /carts` → 201 + `CartResponse`

**Files (new)**
- `domain/cart/CartStatus.java` — enum `OPEN | LOCKED`.
- `domain/cart/Cart.java` — `@Entity`. `UUID id @Id`, `CartStatus status` (enum, stored as STRING), `@OneToMany(cascade = ALL, orphanRemoval = true) @JoinColumn("cart_id") List<CartItem> items`, `BigDecimal total` (column `total_amount`, `precision = 19, scale = 2`), `@Version Long version`. Factory `Cart.create()` returns a new OPEN cart with empty items and `BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY)`. `lock()` no-op if already LOCKED. Getters; `getItems()` returns `Collections.unmodifiableList(items)`; no setter for `status`.
- `domain/cart/CartItem.java` — minimal entity stub (fields, no behavior yet — populated in Step 2). Defined now so the `@OneToMany` compiles.
- `repository/CartRepository.java` — `extends JpaRepository<Cart, UUID>`.
- `service/CartService.java` — `@Service`. Method `createCart()` returns a saved `Cart`.
- `api/dto/CartResponse.java`, `api/dto/CartItemResponse.java` — record DTOs. `CartResponse.from(Cart)` static mapper.
- `api/CartController.java` — `@RestController @RequestMapping("/carts")`. `POST` returns 201 + `CartResponse`.

**Verify**: `curl -X POST localhost:8080/carts` → 201 with `{ "id": "…", "status": "OPEN", "items": [], "total": "0.00" }`.

---

### Step 2 — Use case: Add item to cart

**Endpoint**: `POST /carts/{cartId}/items` → 200 + `CartResponse`. 409 if cart is LOCKED.

**Files (new / modified)**
- `domain/cart/CartItem.java` (fill in) — fields `id (UUID)`, `productId (UUID)`, `int quantity`, `BigDecimal unitPrice` (`precision = 19, scale = 2`), `BigDecimal lineTotal` (`precision = 19, scale = 2`). Package-private constructor `CartItem(UUID productId, int quantity, BigDecimal unitPrice)` validates `quantity > 0` and `unitPrice.signum() >= 0`, computes `lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.UNNECESSARY)`.
- `domain/cart/Cart.java` (modify) — add `addItem(UUID productId, int quantity, BigDecimal unitPrice)` behavior method. Throws `CartLockedException` if `status == LOCKED`. Constructs `CartItem`, appends, recomputes `total = items.stream().map(CartItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.UNNECESSARY)`.
- `exceptions/CartLockedException.java` (flesh out) — message includes cartId.
- `service/CartService.java` (modify) — `addItem(UUID cartId, AddItemRequest req)`: load cart (or `ResourceNotFoundException`), call `cart.addItem(...)`, save, return.
- `api/dto/AddItemRequest.java` — record `{ @NotNull UUID productId, @Positive int quantity, @NotNull @DecimalMin("0.00") BigDecimal unitPrice }`.
- `api/CartController.java` (modify) — add `@PostMapping("/{cartId}/items")` with `@Valid @RequestBody AddItemRequest`.

**Unit test (mandatory candidate)**: `CartTest` — `addItem` on LOCKED throws `CartLockedException`; `lock()` is idempotent; total recomputes correctly after multiple items.

> ↪ *Superseded during implementation — see [IMPLEMENTATION_DECISIONS.md §4](IMPLEMENTATION_DECISIONS.md). The test now asserts `lock()` throws `CartLockedException` on a second invocation (`CartTest.lock_whenAlreadyLocked_shouldThrowCartLockedException`), not that it is a no-op.*

**Verify**: add an item to the cart from Step 1; observe `total` updates and items appear in the response.

---

### Step 3 — Use case: Checkout (cart → order)

**Endpoint**: `POST /carts/{cartId}/checkout` → 201 + `OrderResponse`. 409 if cart already LOCKED.

**Files (new / modified)**
- `domain/order/OrderStatus.java` — enum `CREATED | PENDING_PAYMENT | PAYMENT_FAILED | PAID`.
- `domain/order/OrderItem.java` — `@Entity`. Same shape as `CartItem` but FK to Order; constructor takes a CartItem-like input and copies fields.
- `domain/order/Order.java` — `@Entity`. `UUID id`, `UUID cartId` (not a Cart reference), `OrderStatus status`, `@OneToMany items`, `BigDecimal totalAmount` (`precision = 19, scale = 2`), `@Version`. Factory `Order.fromCart(Cart)` snapshots items into fresh `OrderItem`s, copies `cart.getTotal()` into `totalAmount`, initializes `status = CREATED`. State-machine methods (`markPendingPayment`, `markPaid`, `markPaymentFailed`) stubbed but throw `UnsupportedOperationException` for now — fleshed out in Step 4. **Important**: only `markPendingPayment`'s acceptance from `CREATED` is needed by Step 4; the others are added with their slice.
- `repository/OrderRepository.java` — `JpaRepository<Order, UUID>`.
- `service/CheckoutService.java` — `@Service @Transactional`. `checkout(UUID cartId)`: load cart; if `status == LOCKED` throw `CartLockedException`; `Order order = Order.fromCart(cart)`; `cart.lock()`; save both; return order.
  - ↪ *Superseded during implementation — see [IMPLEMENTATION_DECISIONS.md §4](IMPLEMENTATION_DECISIONS.md). `CheckoutService` no longer pre-checks the cart status; the `LOCKED` guard moved into `Cart.lock()` itself, so the service just calls `cart.lock()` and lets the aggregate throw.*
- `api/dto/OrderResponse.java` — record with `from(Order)`.
- `api/CheckoutController.java` — `POST /carts/{cartId}/checkout`.

**Verify**: full Step 1 → 2 → 3 flow returns an order with status `CREATED` and items copied; calling checkout twice on the same cart → 409.

---

### Step 4 — Use case: Start payment

**Endpoint**: `POST /orders/{orderId}/payment/start` → 200 + `PaymentStartResponse { paymentId, providerPaymentId }`. 409 if order is in `PENDING_PAYMENT` or `PAID`.

**Files (new / modified)**
- `domain/order/Order.java` (modify) — finalize state-machine methods. `markPendingPayment()` legal from `CREATED` or `PAYMENT_FAILED`, else `InvalidOrderTransitionException`. `markPaid()` and `markPaymentFailed()` legal only from `PENDING_PAYMENT` (used in Step 5).
- `exceptions/InvalidOrderTransitionException.java` (flesh out) — message includes source state and attempted target.
- `domain/payment/PaymentStatus.java` — enum `INITIATED | CONFIRMED | FAILED`.
- `domain/payment/Payment.java` — `@Entity`. `UUID id`, `UUID orderId`, `BigDecimal amount` (`precision = 19, scale = 2`), `PaymentStatus status`, `String providerPaymentId` (nullable, immutable once set), `@Version`. Factory `Payment.initiate(UUID orderId, BigDecimal amount)` returns INITIATED with null providerPaymentId. `attachProviderId(String)` throws `InvalidPaymentTransitionException` if already set. `markConfirmed()` / `markFailed()` legal only from INITIATED with non-null providerPaymentId — used in Step 5.
- `exceptions/InvalidPaymentTransitionException.java` (flesh out).
- `domain/payment/port/PaymentProvider.java` — interface with `ProviderInitiationResult initiatePayment(UUID paymentId, BigDecimal amount)`.
- `domain/payment/port/ProviderInitiationResult.java` — record `{ String providerPaymentId }`.
- `infrastructure/MockPaymentProvider.java` — `@Component` implementing the port. Generates `UUID.randomUUID().toString()` as `providerPaymentId`, stores `paymentId → providerPaymentId` in `ConcurrentHashMap` (and reverse map for Step 6 lookup), returns result synchronously. **No webhook callback yet — that's Step 6.**
- `repository/PaymentRepository.java` — `JpaRepository<Payment, UUID>`.
- `service/PaymentService.java` — `@Service @Transactional`. `startPayment(UUID orderId)`: load order; `order.markPendingPayment()`; `Payment p = Payment.initiate(orderId, order.getTotalAmount())`; save (materializes id); `var res = provider.initiatePayment(p.getId(), p.getAmount())`; `p.attachProviderId(res.providerPaymentId())`; save; save order; return.
- `api/dto/PaymentStartResponse.java`.
- `api/PaymentController.java` — `POST /orders/{orderId}/payment/start`.

**Unit test (mandatory by spec)**: `OrderTest` — `markPaid()` from `PENDING_PAYMENT` succeeds; from `CREATED` throws `InvalidOrderTransitionException`. Cover `PAYMENT_FAILED → PENDING_PAYMENT` retry edge.

**Verify**: start payment after checkout — get back ids; order is now PENDING_PAYMENT; calling start again → 409.

---

### Step 5 — Use case: Receive webhook

**Endpoint**: `POST /payments/webhook` → always 200 on the idempotent path. Provider posts here.

**Files (new / modified)**
- `api/dto/WebhookPayload.java` — record `{ UUID paymentId, String providerPaymentId, Outcome outcome }` where `Outcome = CONFIRMED | FAILED`.
- `service/WebhookService.java` — `@Service @Transactional`. `handle(WebhookPayload)`:
  1. Load `Payment` by `paymentId` (404 if missing — provider misconfig).
  2. **Service-layer idempotency fast path**: if `payment.status` is terminal (CONFIRMED or FAILED), return — no mutation, no exception. Controller responds 200.
  3. Otherwise call `payment.markConfirmed()` or `payment.markFailed()` per outcome. (Aggregate is the safety net for races: if a concurrent webhook already transitioned it, the second call's `OptimisticLockingFailureException` propagates → 409; provider retries; the retry hits the fast path.)
  4. Load `Order`, call `markPaid()` / `markPaymentFailed()`.
  5. Save both.
- `api/WebhookController.java` — `POST /payments/webhook`, `@Valid @RequestBody`. Catch-nothing — let the advice translate.

**Verify (manual, without mock loop yet)**: with an INITIATED payment from Step 4, `curl -X POST -H 'Content-Type: application/json' -d '{"paymentId":"…","providerPaymentId":"…","outcome":"CONFIRMED"}' localhost:8080/payments/webhook` → 200. Order is PAID. Replay same curl → 200, no change. This proves idempotency without needing the mock to fire it.

---

### Step 6 — Use case: Trigger mock outcome (closes the loop)

**Endpoint**: `POST /mock-provider/{providerPaymentId}/trigger?outcome=CONFIRMED|FAILED` → 200. Mock posts to our own `/payments/webhook`.

**Files (new / modified)**
- `infrastructure/MockProviderClient.java` — wraps Spring's `RestClient`. Reads `mock.provider.callback-base-url` from `application.properties`. Method `sendWebhook(WebhookPayload)` POSTs to `${base}/payments/webhook`.
- `infrastructure/MockProviderController.java` — `POST /mock-provider/{providerPaymentId}/trigger?outcome=…`. Looks up `paymentId` from `MockPaymentProvider`'s reverse map (add accessor method); 404 if unknown. Builds `WebhookPayload` and calls `MockProviderClient.sendWebhook(...)`. Returns 200.

**Verify**: full end-to-end happy path via Postman/curl — create cart → add items → checkout → start payment → trigger mock CONFIRMED → order is PAID.

---

### Step 7 — Integration tests


- `HappyPathIT` — `@SpringBootTest(webEnvironment = RANDOM_PORT)`. Override `mock.provider.callback-base-url` to `http://localhost:${local.server.port}` via `@DynamicPropertySource`. Walk all six requests with `TestRestClient`. Assert final order PAID, payment CONFIRMED.
- `DuplicateWebhookIT` — same setup; POST `/payments/webhook` twice with the same payload after payment is INITIATED. Second call returns 200 and statuses are unchanged.
- `RetryFlowIT` (bonus) — trigger FAILED, then `payment/start` again, then trigger CONFIRMED. Asserts the original Payment₁ stays FAILED while Payment₂ is CONFIRMED — proves "new Payment per attempt".

---

### Step 8 — Postman collection

[checkout.postman_collection.json](../checkout.postman_collection.json) at the repo root (no separate `postman/` directory). Four folders — the three required user flows plus an invariants folder so the negative paths are demonstrated alongside the happy ones:

1. **Flow 1 — Happy Path**: create cart → add item → checkout → start payment → trigger mock provider CONFIRMED. Uses the mock-provider trigger endpoint to exercise the full provider-→-webhook loop end-to-end.
2. **Flow 2 — Payment Failure + Retry**: fresh cart/order → start payment → direct webhook FAILED → start payment again → webhook CONFIRMED. Asserts the retry's `paymentId` differs from attempt 1 (new Payment per attempt) and that a late webhook against the now-terminal first payment returns 200 (idempotent no-op).
3. **Flow 3 — Duplicate Webhook**: fresh cart/order → start payment → webhook CONFIRMED → replay the same CONFIRMED webhook → also a replay with `FAILED` outcome on the now-terminal payment. All three webhook calls must return 200; the conflicting-outcome replay specifically proves the terminal-status fast-path absorbs malformed/late deliveries.
4. **Edge Cases — State-Machine Invariants**: depends on Flow 1's artifacts. Covers `CART_LOCKED` (re-checkout, add-item-after-checkout), `INVALID_ORDER_TRANSITION` (start payment on a PAID order), and `NOT_FOUND` (unknown order, unknown payment in webhook, unknown providerPaymentId on the mock trigger).

**Portability**: collection-level variables `baseUrl`, `cartId`, `orderId`, `paymentId`, `providerPaymentId`, `paymentIdRetry`, `providerPaymentIdRetry`. Override `baseUrl` per environment; the rest are populated by the `pm.collectionVariables.set(...)` test scripts on each request response. Path variables are bound at the URL level (`/carts/:cartId/items` with `variable: [{ key: "cartId", value: "{{cartId}}" }]`) so the request shape mirrors the spec instead of hard-coding a UUID — same pattern the inspiration collection uses.

**Inline assertions**: every request has a `test` event that asserts the HTTP status plus at least one domain assertion (e.g. `order.status === "CREATED"`, `cartId` is preserved on the order, the retry's `paymentId` differs from the first). Running the collection in the Postman Runner is therefore a green/red signal for the four evaluation criteria (state machine, idempotency, code clarity surfaced via API shape, and a covering test bed).

---

### Step 9 — README & AI usage

- `README.md` — setup, endpoint table, curl snippets, assumptions (single implicit currency, no auth, no merging of duplicate productIds), pointers to [DESIGN.md](ai_usage_policy/DESIGN.md) and [DESIGN_ARCH_TRADEOFFS.md](ai_usage_policy/DESIGN_ARCH_TRADEOFFS.md), how to run tests, how to import the Postman collection.
- `ai_usage_policy/AI_USAGE.md` and `ai_usage_policy/CONVERSATION.md` — what was AI-assisted vs. user-driven, per the brief and CLAUDE.md.

---

## Critical Files (most invariant-sensitive — read carefully)

These are the spots where a wrong line breaks the evaluation:

- **`domain/order/Order.java`** — the state machine. All four `mark…` methods must validate source state and throw `InvalidOrderTransitionException` on illegal transitions. No public setter for `status`. This is 25% of the grade.
- **`service/WebhookService.java`** — two-layer idempotency: service-layer terminal-status check + aggregate safety net. Single `@Transactional`. Always returns 200 on the idempotent duplicate path. This is 25% of the grade.
- **`api/GlobalExceptionHandler.java`** — domain exceptions → 409 (not 500). Without this, the API surface looks broken even though the domain is correct.

---

## Reusable Pieces Already in the Repo

- Maven wrapper and `pom.xml` are configured for Spring Boot 4 with starters for web-mvc, validation, data-jpa, h2, h2console, and their test counterparts ([pom.xml](pom.xml)).
- `CheckoutApplication` is the entry point — no changes needed ([CheckoutApplication.java](src/main/java/com/cart/checkout/CheckoutApplication.java)).
- `application.properties` exists ([application.properties](src/main/resources/application.properties)) and just needs the JPA/H2 settings added.

Nothing in `src/` to refactor — the slate is clean.

---
