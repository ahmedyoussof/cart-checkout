## Architectural Decisions I Made

### Tech & scope decisions
- **Persistence**: JPA + H2 in-memory
- **No authentication** for this iteration
- **No webhook signature verification** (tight schedule, conscious trade-off)
- **Mock provider lives in the same project**, behind an interface for swappability

### Bounded context decisions
- **Four bounded contexts**: Cart, Order, Payment, Provider
- **Service and Repository are layers**, not contexts (corrected the AI when it proposed Service as a context)
- **Dependency direction is one-way only**: Cart ← Order ← Payment → Provider (interface)
- **Order knows about Cart by ID only**, not by aggregate reference
- **Payment knows about Order by ID only**
- **Order does NOT know about Payment** — held this line when AI offered a bidirectional option

### Cart domain decisions
- **Cart as aggregate root** containing CartItems and Money
- **Currency fixed to EGP** for this iteration (label-invariant on amount, not stored independently)
- **Cart status**: OPEN → LOCKED, one-way
- **`lock()` is idempotent** — no-op on already-locked cart (not an error)
  - ↪ *Superseded during implementation — see [IMPLEMENTATION_DECISIONS.md §4](IMPLEMENTATION_DECISIONS.md). `lock()` now throws `CartLockedException` on a second invocation.*
- **Locked cart rejects mutations with 409 Conflict**
- **No item merging on duplicate productId** — each add appends a new CartItem (simplification)
- **No update/remove item endpoints** — only `addItem` as per the brief

### Order domain decisions
- **Order is built by snapshotting cart items at checkout** (not by reference to live cart)
- **State machine lives inside the Order aggregate** as behavior methods (not external)
- **No timestamps on Order** — time isn't a concern for this design
- **`CANCELLED` is excluded** from this iteration (design must accommodate it later without breaking invariants)
- **Currency is a label-invariant on Money**, must match across all items and totalAmount
- **`@Version` on Order** for optimistic concurrency control

### Payment domain decisions
- **Payment is a separate aggregate** from Order (one-way: Payment → Order)
- **`paymentId` is the idempotency key sent to the provider** (no separate field needed)
- **`outcome` belongs to the Provider context** (webhook payload field), not the Payment aggregate — corrected the AI's initial structuring
- **Payment status: INITIATED, CONFIRMED, FAILED** — both outcomes terminal
- **Retries create a new Payment aggregate**, not transitions on the old one (preserves audit history)
- **`providerPaymentId` is set once and immutable** — separate `attachProviderId()` method
- **`@Version` on Payment** for optimistic concurrency control

### Idempotency & concurrency decisions
- **Webhook idempotency via terminal-status check** in the service layer (no separate processed-events table — sufficient for one-outcome-per-payment, can be added later if needed)
- **No partial unique index** on Payment.orderId — "no concurrent payments" is enforced naturally by the Order state machine (since `markPendingPayment()` only accepts CREATED or PAYMENT_FAILED as source states)
- **`payment/start` rejected when order already in PENDING_PAYMENT** — returns 409

### Cart-to-order lifecycle decision
- **Cart is kept after checkout, marked LOCKED, linked to Order by ID** — provides audit trail, no deletion logic to coordinate, future-proof for refunds
- **Order snapshots items independently** — price changes on products don't retroactively affect orders

### Money representation decision
- **`Money` as a shared Value Object** — immutable, value equality, cross-currency arithmetic throws. Currency fixed to EGP for this iteration (label-invariant on the amount, not carried independently). Provides `add` / `multiply` helpers and a type-level guard against mixing currencies at the aggregate boundary.
  - ↪ *Superseded during implementation — see [IMPLEMENTATION_DECISIONS.md §1](IMPLEMENTATION_DECISIONS.md). `Money` was dropped; amounts are stored as raw `BigDecimal` (precision 19, scale 2) on aggregates and DTOs.*

---
