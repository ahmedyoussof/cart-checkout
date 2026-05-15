# Hiring Quest - Senior Java @ ExeQut


### META INFO
* **CATEGORY:** Registration
* **STATUS:** Registration
* **REGISTRATION DEADLINE:** May 13, 2026
* **SUBMISSION DEADLINE:** May 17, 2026
* **PRIZE:** Top performers get hired with a paid contract and the opportunity to work on real-world projects.

---

### QUEST BRIEF
**Cart Checkout + Mock Payment System** *Single Service | Java / Spring Boot*

#### Business Context
You are designing the backend for a small e-commerce product. The system allows users to:
1.  Create a cart
2.  Add items
3.  Checkout
4.  Pay for their order

This system is not expected to scale. However:
1.  It handles real money
2.  Order state must always be correct
3.  No double payments
4.  No invalid state transitions
5.  Webhooks may arrive twice

#### Your Responsibility (Tech Lead)
1.  Defining the domain model and designing clear boundaries inside a single service.
2.  Designing the order state machine and protecting business invariants.
3.  Ensuring data consistency and correctness in all state transitions.
4.  Handling payment safely, including webhook processing and idempotency.
5.  Defining a safe payment handling strategy that prevents double charges.
6.  Designing failure handling for duplicates, retries, and race conditions.
7.  Making conscious architectural trade-offs (simplicity vs robustness).
8.  Enabling future extensibility (refunds, partial payments, cancellations) without breaking core invariants.

---

### USER FLOWS

**Flow 1 - Happy Path**
1.  Create cart
2.  Add items
3.  Checkout → Order created
4.  Start payment
5.  Mock provider confirms payment (webhook)
6.  Order becomes PAID

**Flow 2 - Payment Failure + Retry**
1.  Start payment
2.  Provider sends FAILED
3.  Order becomes PAYMENT_FAILED
4.  Start payment again
5.  Provider confirms → Order becomes PAID

**Flow 3 - Duplicate Event Handling**
1.  Provider may send the same webhook twice
2.  System must remain correct
3.  No duplicate transitions
4.  No corrupted state

---

### FUNCTIONAL REQUIREMENTS

#### Cart
**Endpoints:**
* `POST /carts` → create cart
* `POST /carts/{cartId}/items` → add item (productId, quantity, price)

**Cart Rules:**
1.  Items can be modified before checkout
2.  Checkout locks the cart

#### Checkout/Order
**Endpoint:**
* `POST /carts/{cartId}/checkout`

**Behavior:**
1.  Creates an Order from cart
2.  Locks cart
3.  Initializes order state

**Required Order States:**
You must define and implement a state machine with these minimum states:
1.  CREATED
2.  PENDING_PAYMENT
3.  PAYMENT_FAILED
4.  PAID
5.  *(Optional: CANCELLED if you can support it cleanly)*

*Note: You must enforce valid transitions.*

#### Payment
**Endpoints:**
* `POST /orders/{orderId}/payment/start`
* `POST /payments/webhook`

**Required Behavior:**
* **When starting payment:** Create a payment intent/attempt, move Order to `PENDING_PAYMENT`, and prevent duplicate active payments.
* **When webhook is received:** Update order state safely and handle duplicate webhook calls idempotently.

---

### MOCK PAYMENT PROVIDER
You must implement a minimal mock provider inside the same project that:
1.  Simulates payment start.
2.  Allows triggering a payment result (CONFIRMED/FAILED).
3.  Calls your `/payments/webhook` endpoint.
*(Just enough to demonstrate full flow; no complex simulation needed.)*

---

### TESTING & DOCUMENTATION

#### Testing Requirements
* **Minimum:** 1 unit test validating domain state transition.
* **Optional but strong:** 1 integration test covering happy path and 1 test for duplicate webhook.

#### Postman Collection (Required)
Provide a complete Postman Collection demonstrating how you validated system correctness.

#### Deliverables
1.  Complete Java / Spring Boot codebase.
2.  Clear domain model & order state machine.
3.  Payment handling (including idempotency & webhook logic).
4.  Mock payment provider.
5.  Required endpoints implemented.
6.  At least 1 unit test for state transitions.
7.  README (setup + assumptions + key decisions).
8.  **10-15 min Video (Required)** covering:
    * Architecture & key decisions.
    * State machine & payment flow.
    * Webhook handling & idempotency.
    * Edge cases (failures, retries, duplicates).
    * Trade-offs & improvements.

---

### AI USAGE POLICY
Do NOT use AI tools to generate the solution blindly. If AI tools are used, you MUST include:
* The prompts used.
* Relevant conversation excerpts.
* What parts were AI-assisted.
* What architectural decisions were your own.

*We are evaluating your engineering thinking and trade-off decisions, not just the final code. Submissions appearing heavily AI-generated without understanding may be rejected.*

---

### EVALUATION CRITERIA
| Area | Weight |
| :--- | :--- |
| State machine & invariants | 25% |
| Idempotency & payment safety | 25% |
| Code clarity & structure | 25% |
| Testing | 20% |
| Documentation clarity | 5% |