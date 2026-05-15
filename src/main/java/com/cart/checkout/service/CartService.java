package com.cart.checkout.service;

import com.cart.checkout.domain.cart.Cart;
import com.cart.checkout.exceptions.ResourceNotFoundException;
import com.cart.checkout.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;

    public CartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @Transactional
    public Cart createCart() {
        Cart cart = Cart.create();
        log.info("Created a new cart {}", cart.getId());
        return cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public Cart find(UUID cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found: " + cartId));
    }
}
