package com.cart.checkout.exceptions;

public class CartLockedException extends RuntimeException {
    public CartLockedException(String message) {
        super(message);
    }
}
