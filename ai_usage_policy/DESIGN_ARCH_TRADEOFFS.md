# Architectural Trade-offs

Companion to [DESIGN.md](DESIGN.md). Catalogues the deliberate trade-offs behind the design and sketches what near-term extensions (CANCELLED, refunds) would look like without breaking existing invariants.

## 1. Why a single Payment per attempt, not one per Order

Each retry creates a fresh Payment aggregate rather than transitioning the existing one back to `INITIATED`. Trade-offs:

| Pro | Con |
|---|---|
| Immutable history  failed attempts are preserved for audit | Slightly more rows in the DB per retried order |
| Simpler state machine  no back-edges on Payment | Need to make sure queries scope to "current" payment per order |
| Cleanly extends to partial payments later (multiple Payments coexisting) |  |
| Webhook idempotency is trivially safe  terminal payments can never be reactivated |  |

Net: the right call for "real money + extensibility".

## 2. Why optimistic locking, not pessimistic

| Reason |
|---|
| Low contention  most carts/orders/payments are touched by one actor at a time |
| Webhook + start races are real but rare; an OptimisticLockingFailureException is acceptable for the loser (retry once, then the order is in a different state and the duplicate-check kicks in) |
| `SELECT FOR UPDATE` would tie us to specific lock semantics that don't add value at this scale |
| `@Version` is a one-line annotation; pessimistic locking is more invasive |

## 3. Why service-layer idempotency, not an event-id table

| Pro | Con |
|---|---|
| Zero schema overhead | Doesn't generalize to providers that send multiple legitimate events for one payment |
| Self-evidently correct: terminal status is the truth |  |
| Sufficient for the spec  each payment has exactly one outcome |  |

The brief explicitly says "just enough to demonstrate full flow; no complex simulation needed." Adding an event-id table for this spec would be over-engineering. The hook for adding it later is clean: it's a service-layer concern, not a domain concern, so it can be slipped in without touching the aggregates.

## 4. Why no merge on adding the same productId twice

Two adds for the same productId produce two separate `CartItem` rows. The brief doesn't require merging, and the simpler version is easier to reason about and test. A real product would almost certainly merge  when that requirement appears, `cart.addItem()` is the only method that changes.

## 5. Why one Cart aggregate, not a separate `CartHeader` + `CartLine`

Cart is small enough to be a single aggregate with the items as part of the consistency boundary. This makes the "lock blocks modification" invariant trivial to enforce: it's just a check at the top of `addItem()`. Splitting them would require a separate locking concern.

## 6. What CANCELLED would look like (not implemented yet)

- Add `CANCELLED` to `OrderStatus`.
- Add `order.cancel()` behavior method  legal from `CREATED` or `PAYMENT_FAILED`; rejected from `PENDING_PAYMENT` (because there's an outstanding payment intent) and from `PAID` (refund flow, not cancel flow).
- New endpoint: `POST /orders/{id}/cancel`.

No existing transitions change. The state machine extends without breaking.

## 7. What refunds would look like (not implemented yet)

- Refunds are *new Payment aggregates* with a different role (e.g. `PaymentKind = CHARGE | REFUND`), not transitions on existing ones.
- This is why Payment is a separate aggregate from Order: many payments per order is already the model.
- Order would gain a `REFUNDED` state  `PAID → REFUNDED` (full)  possibly with an intermediate `PARTIALLY_REFUNDED` for partials.

The current Payment design has the necessary shape; we add the new state(s) and the new aggregate kind. No existing invariants are violated.
