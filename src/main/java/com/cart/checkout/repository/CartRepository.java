package com.cart.checkout.repository;

import com.cart.checkout.domain.cart.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {
}
