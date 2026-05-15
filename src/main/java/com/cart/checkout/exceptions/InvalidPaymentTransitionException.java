package com.cart.checkout.exceptions;

public class InvalidPaymentTransitionException extends RuntimeException {
    public InvalidPaymentTransitionException(String message) {
        super(message);
    }
}
