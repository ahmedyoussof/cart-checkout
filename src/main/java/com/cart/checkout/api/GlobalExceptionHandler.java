package com.cart.checkout.api;

import com.cart.checkout.exceptions.CartLockedException;
import com.cart.checkout.exceptions.InvalidOrderTransitionException;
import com.cart.checkout.exceptions.InvalidPaymentTransitionException;
import com.cart.checkout.exceptions.ResourceNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CartLockedException.class)
    public ResponseEntity<ErrorResponse> cartLocked(CartLockedException ex) {
        return conflict("CART_LOCKED", ex.getMessage());
    }

    @ExceptionHandler(InvalidOrderTransitionException.class)
    public ResponseEntity<ErrorResponse> invalidOrderTransition(InvalidOrderTransitionException ex) {
        return conflict("INVALID_ORDER_TRANSITION", ex.getMessage());
    }

    @ExceptionHandler(InvalidPaymentTransitionException.class)
    public ResponseEntity<ErrorResponse> invalidPaymentTransition(InvalidPaymentTransitionException ex) {
        return conflict("INVALID_PAYMENT_TRANSITION", ex.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> concurrentModification(OptimisticLockingFailureException ex) {
        return conflict("CONCURRENT_MODIFICATION", "Resource was modified concurrently; retry");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("VALIDATION_ERROR", msg));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> illegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("BAD_REQUEST", ex.getMessage()));
    }

    private ResponseEntity<ErrorResponse> conflict(String code, String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(code, message));
    }
}
