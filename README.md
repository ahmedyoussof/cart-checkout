# Getting Started

## Project Structure

The project follows a layer-first architectural style with nested bounded contexts under `com.cart.checkout.domain`:

```
com.cart.checkout
├── CheckoutApplication.java                    // application entry point
├── api/                                      // REST controllers, DTOs, ErrorResponse
│   ├── CartController.java
│   ├── CheckoutController.java
│   ├── PaymentController.java
│   ├── WebhookController.java
│   ├── GlobalExceptionHandler.java           // exception → HTTP status mapping
│   └── dto/
│       └── *
├── domain/
│   ├── shared/
│   │   └── Money.java                        // value object with currency invariant
│   ├── cart/
│   │   ├── Cart.java                         // aggregate + state machine
│   │   ├── CartItem.java
│   │   └── CartStatus.java (OPEN, LOCKED)
│   ├── order/
│   │   ├── Order.java                        // aggregate + state machine
│   │   ├── OrderItem.java
│   │   └── OrderStatus (CREATED, PENDING_PAYMENT, PAYMENT_FAILED, PAID)
│   └── payment/
│       ├── Payment.java                        // aggregate + state machine
│       ├── PaymentStatus (PENDING, SUCCEEDED, FAILED)
│       └── port/                               // port for external payment provider
│           ├── PaymentProvider.java
│           └── ProviderInitiationResult.java
├── exceptions/                               // domain exceptions
│   ├── CartLockedException.java
│   ├── InvalidOrderTransitionException.java
│   ├── InvalidPaymentTransitionException.java
│   └── ResourceNotFoundException.java
├── infrastructure/
│   ├── MockPaymentProvider.java              // mock provider implementation
│   └── MockProviderClient.java               // client to trigger outcomes
└── repository/                               // JPA repositories
    ├── CartRepository.java
    ├── OrderRepository.java
    └── PaymentRepository.java
```

## Build & Run

```bash
# Build the project with Maven
./mvnw clean package

# Run the application
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`. The H2 console is available at `http://localhost:8080/h2-console`.
