package com.cart.checkout.service;

import com.cart.checkout.domain.cart.Cart;
import com.cart.checkout.domain.order.Order;
import com.cart.checkout.exceptions.ResourceNotFoundException;
import com.cart.checkout.repository.CartRepository;
import com.cart.checkout.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;

    public CheckoutService(CartRepository cartRepository, OrderRepository orderRepository) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order checkout(UUID cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found: " + cartId));
        cart.lock();
        Order order = Order.fromCart(cart);
        cartRepository.save(cart);
        Order savedOrder = orderRepository.save(order);
        log.info("Checked out cart {} into order {}", cart.getId(), savedOrder.getId());
        return savedOrder;
    }
}
