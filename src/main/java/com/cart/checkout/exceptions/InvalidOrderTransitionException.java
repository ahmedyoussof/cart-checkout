package com.cart.checkout.exceptions;

public class InvalidOrderTransitionException extends RuntimeException {
    public InvalidOrderTransitionException(String message) {
        super(message);
    }
}
