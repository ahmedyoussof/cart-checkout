## 1. Business Flow (top-level)

High-level user journey from cart creation to a paid order.

```mermaid
flowchart LR
    A[Create Cart] --> B[Add Items]
    B --> C[Checkout → Order]
    C --> D[Start Payment]
    D --> E[Webhook → PAID]

    style E fill:#0e3a4d,stroke:#38bdf8,color:#7dd3fc
```

---

## 2. Bounded Contexts (Architecture)

Four contexts inside one service · one-way dependencies · references by ID only · dashed arrow = async webhook callback.

```mermaid
flowchart LR
    Cart[Cart<br/><i>items, lock on checkout</i>]
    Order[Order<br/><i>state machine, totals</i>]
    Payment[Payment<br/><i>per-attempt aggregate</i>]
    Provider[Provider<br/><i>mock external system</i>]
    Webhook{{/payments/webhook}}

    Order -- depends on --> Cart
    Payment -- depends on --> Order
    Payment -- calls --> Provider
    Provider -. async callback .-> Webhook
    Webhook --> Payment

    style Cart fill:#1e293b,stroke:#38bdf8,color:#7dd3fc
    style Order fill:#1e293b,stroke:#38bdf8,color:#7dd3fc
    style Payment fill:#1e293b,stroke:#38bdf8,color:#7dd3fc
    style Provider fill:#3b2a0e,stroke:#fbbf24,color:#fde68a
    style Webhook fill:#0e3a4d,stroke:#f59e0b,color:#fde68a
```

---

## 3. Layering Inside Each Context

Same shape per context: HTTP on top, domain at the bottom, invariants owned by the aggregate.

```mermaid
flowchart TB
    Controller["<b>Controller</b><br/>HTTP translation"]
    Service["<b>Service</b><br/>orchestration + @Transactional"]
    Repository["<b>Repository</b><br/>Spring Data JPA"]
    Aggregate["<b>Aggregate (Domain)</b><br/>enforces every invariant<br/>no public setters on state"]

    Controller --> Service
    Service --> Repository
    Repository --> Aggregate

    style Controller fill:#1e293b,stroke:#334155,color:#e2e8f0
    style Service fill:#1e293b,stroke:#334155,color:#e2e8f0
    style Repository fill:#1e293b,stroke:#334155,color:#e2e8f0
    style Aggregate fill:#0e3a4d,stroke:#38bdf8,color:#7dd3fc
```

---

## 4. Domain Model & By-ID References

Three independent aggregates. Cross-context relationships are by **ID only** — no JPA association across boundaries.

```mermaid
classDiagram
    class Cart {
        +UUID cartId
        +CartStatus status
        +Currency currency
        +List~CartItem~ items
        +addItem()
        +lockForCheckout()
    }

    class Order {
        +UUID orderId
        +UUID cartId
        +OrderStatus status
        +Money total
        +List~OrderItem~ items
        +@Version
        +markPendingPayment()
        +markPaid()
        +markPaymentFailed()
    }

    class Payment {
        +UUID paymentId
        +UUID orderId
        +PaymentStatus status
        +String providerPaymentId
        +Money amount
        +@Version
        +markConfirmed()
        +markFailed()
    }

    Order ..> Cart : cartId (by ID)
    Payment ..> Order : orderId (by ID)
```

---

## 5. State Machines — Cart · Order · Payment

The Order state machine is the heart of the system. PAID, LOCKED, and CONFIRMED/FAILED are terminal.

### 5a. Cart

```mermaid
stateDiagram-v2
    direction LR
    [*] --> OPEN
    OPEN --> LOCKED : lockForCheckout()
    LOCKED --> [*]

    note right of LOCKED
        terminal — no more addItem
        or checkout after this
    end note
```

### 5b. Payment (per attempt)

```mermaid
stateDiagram-v2
    direction LR
    [*] --> INITIATED
    INITIATED --> CONFIRMED : markConfirmed()
    INITIATED --> FAILED    : markFailed()
    CONFIRMED --> [*]
    FAILED --> [*]

    note right of CONFIRMED
        terminal · immutable
        basis of webhook idempotency
    end note
```

### 5c. Order — the heart of the system

```mermaid
stateDiagram-v2
    direction LR
    [*] --> CREATED
    CREATED --> PENDING_PAYMENT : markPendingPayment()
    PENDING_PAYMENT --> PAID            : markPaid() · webhook CONFIRMED
    PENDING_PAYMENT --> PAYMENT_FAILED  : markPaymentFailed() · webhook FAILED
    PAYMENT_FAILED  --> PENDING_PAYMENT : retry → new Payment attempt
    PAID --> [*]

    note right of PAID
        terminal · double markPaid() throws
        InvalidOrderTransitionException
    end note
```

---

## 6. Payment-per-Attempt

Every retry creates a **new** Payment aggregate. Old attempts stay as immutable audit history.

```mermaid
flowchart LR
    Order[Order #42<br/>PAID ✓]

    P1[Payment #1 — FAILED<br/><i>card declined</i>]
    P2[Payment #2 — FAILED<br/><i>network timeout</i>]
    P3[Payment #3 — CONFIRMED<br/><i>webhook arrived</i>]

    Order -.-> P1
    Order -.-> P2
    Order -.-> P3

    Notes["✓ Auditable<br/>✓ Debuggable<br/>✓ No mutation of terminal state<br/>✓ Idempotency comes free"]

    style Order fill:#1e293b,stroke:#38bdf8,color:#7dd3fc
    style P1 fill:#3a0f0f,stroke:#f87171,color:#fca5a5
    style P2 fill:#3a0f0f,stroke:#f87171,color:#fca5a5
    style P3 fill:#0f3a1e,stroke:#4ade80,color:#86efac
    style Notes fill:#111827,stroke:#334155,color:#7dd3fc
```

---

## 7. Flow 1 — Happy Path (Sequence)

Cart → checkout → start payment → provider initiates → mock provider triggers webhook → Order PAID.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Cart as Cart API
    participant OP as Order/Payment
    participant Provider as Mock Provider
    participant Webhook as /payments/webhook

    User->>Cart: POST /carts + items
    User->>OP: POST /carts/{id}/checkout
    Note over OP: Order CREATED · Cart LOCKED
    User->>OP: POST /orders/{id}/payment/start
    Note over OP: Order PENDING_PAYMENT · Payment INITIATED
    OP->>Provider: initiate(amount) → providerPaymentId
    User-->>Provider: trigger CONFIRMED (dev endpoint)
    Provider->>Webhook: POST /payments/webhook
    Webhook->>OP: markConfirmed() · markPaid()
    Note over OP: Order → PAID · Payment → CONFIRMED
```

---

## 8. Flow 2 — Failure & Retry (Sequence)

First payment fails → Order `PAYMENT_FAILED` → retry creates a brand-new Payment #2 → eventually PAID.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant OP as Order + Payment
    participant Provider as Mock Provider

    User->>OP: start payment
    Note over OP: Payment #1 INITIATED · Order PENDING_PAYMENT
    User-->>Provider: trigger FAILED (dev endpoint)
    Provider->>OP: webhook FAILED
    Note over OP: Payment #1 → FAILED · Order → PAYMENT_FAILED

    User->>OP: retry: POST /orders/{id}/payment/start
    Note over OP: new Payment #2 INITIATED · Order PENDING_PAYMENT
    OP->>Provider: initiate again
    Provider->>OP: webhook CONFIRMED
    Note over OP: Order → PAID · Payment #1 stays as audit record
```

---

## 9. Flow 3 — Duplicate Webhook (Sequence)

First webhook lands and transitions state · second webhook hits Layer 1 fast-path and is a no-op. Always 200 OK.

```mermaid
sequenceDiagram
    autonumber
    participant Provider as Mock Provider
    participant WS as WebhookService
    participant Pay as Payment aggregate
    participant Order

    Provider->>WS: webhook(CONFIRMED) #1
    WS->>Pay: markConfirmed() — INITIATED → CONFIRMED ✓
    WS->>Order: markPaid() → Order PAID ✓
    WS-->>Provider: 200 OK

    Provider->>WS: webhook(CONFIRMED) #2 (duplicate)
    Note over WS: Layer 1 check:<br/>payment already CONFIRMED → fast path
    WS-->>Provider: 200 OK — no state change
```

---

## 10. Idempotency — Two Layers (Defense in Depth)

Service-layer fast path catches duplicates cheaply · the aggregate is a safety net if anything slips through.

```mermaid
flowchart LR
    Dup[Duplicate webhook]
    L1["<b>Layer 1: Service</b><br/>terminal? → 200 no-op"]
    L2["<b>Layer 2: Aggregate</b><br/>illegal transition → throw"]
    OK["<b>State stays correct</b><br/>no double-paid order"]

    Dup --> L1 --> L2 --> OK

    style Dup fill:#3b2a0e,stroke:#fbbf24,color:#fde68a
    style L1 fill:#1e293b,stroke:#38bdf8,color:#7dd3fc
    style L2 fill:#1e293b,stroke:#38bdf8,color:#7dd3fc
    style OK fill:#0f3a1e,stroke:#4ade80,color:#86efac
```

---

## 11. Concurrency — Optimistic Locking

Two transactions read the same Order version · TXN A commits and bumps the version · TXN B fails with `OptimisticLockException` and rolls back cleanly.

```mermaid
sequenceDiagram
    autonumber
    participant A as TXN A (webhook)
    participant Order as Order #42<br/>version = 7
    participant B as TXN B (retry start)

    A->>Order: read v=7
    B->>Order: read v=7
    A->>Order: markPaid()
    Note over A,Order: TXN A commits<br/>version 7 → 8 · Order PAID
    B->>Order: markPendingPayment() · expects v=7
    Note over B,Order: TXN B fails<br/>OptimisticLockException<br/>rollback · clean error
```

**Result:** no silent overwrite, no lost update. The caller can retry against the correct state.


---

### 12. Future extensibility (refunds cancellations)

```mermaid
stateDiagram-v2
    [*] --> CREATED: checkout
    CREATED --> PENDING_PAYMENT: start payment
    CREATED --> CANCELLED: cancel<br/>(pre-payment  no refund needed)
    PENDING_PAYMENT --> PAID: webhook CONFIRMED
    PENDING_PAYMENT --> PAYMENT_FAILED: webhook FAILED
    PAYMENT_FAILED --> PENDING_PAYMENT: retry start payment
    PAYMENT_FAILED --> CANCELLED: give up<br/>(no money moved  no refund)
    PAID --> CANCELLED: cancel paid order<br/>(refund required next)
    CANCELLED --> REFUNDED: refund issued<br/>(only legal if entered from PAID)
    CANCELLED --> [*]
    REFUNDED --> [*]
```