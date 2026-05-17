# Decisions Taken While Implementation

1. **Money is a plain `BigDecimal`.** Single currency, no need for a Value Object for simplicity and since the system is small in scope.

2. **Added `GET /carts/{id}` and `GET /orders/{id}`.** it is not required, but tests and Postman need to read state to verify the system is working correctly.

3. **Cart total updated on add, not recomputed.** recalcuting total every time item added to cart is resource heavy, instead we will accumulate the total amount every time an item is added to the cart.

4. **`Cart.lock()` throws if already locked.** instead of do nothing when the cart is already locked, we throw an exception to indicate that the cart is already locked to avoid any undefined behavior.

5. **Mock provider endpoint is `/mock-provider/{id}/trigger`, not `/payments/{id}/confirmed`.** "Trigger" fits both `CONFIRMED` and `FAILED`, and the path is clearly separate from the real `/payments/...` surface.
