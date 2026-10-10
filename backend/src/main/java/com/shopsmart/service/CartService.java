package com.shopsmart.service;

import com.shopsmart.dto.Dtos.*;
import com.shopsmart.entity.*;
import com.shopsmart.exception.ApiException;
import com.shopsmart.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    private final CartRepository carts;
    private final ProductRepository products;
    private final UserRepository users;

    public Cart cartFor(User u) {
        return carts.findByUserId(u.getId()).orElseGet(() -> {
            Cart c = new Cart();
            c.setUser(users.getReferenceById(u.getId()));
            return carts.saveAndFlush(c);
        });
    }

    public CartResponse get(User u) { return toResponse(cartFor(u)); }

    public CartResponse add(User u, CartItemRequest r) {
        Cart cart = cartFor(u);
        Product p = products.findById(r.productId()).filter(Product::isActive)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(p.getId())).findFirst().orElse(null);
        int newQty = (item == null ? 0 : item.getQuantity()) + r.quantity();
        checkStock(p, newQty);
        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setProduct(p);
            cart.getItems().add(item);
        }
        item.setQuantity(newQty);
        return toResponse(carts.saveAndFlush(cart));
    }

    public CartResponse update(User u, Long itemId, CartItemUpdate r) {
        Cart cart = cartFor(u);
        CartItem item = find(cart, itemId);
        checkStock(item.getProduct(), r.quantity());
        item.setQuantity(r.quantity());
        return toResponse(carts.saveAndFlush(cart));
    }

    public CartResponse remove(User u, Long itemId) {
        Cart cart = cartFor(u);
        cart.getItems().remove(find(cart, itemId)); // orphanRemoval deletes the row
        return toResponse(carts.saveAndFlush(cart));
    }

    public void clear(User u) {
        Cart cart = cartFor(u);
        cart.getItems().clear();
        carts.saveAndFlush(cart);
    }

    // ownership is enforced by only searching the caller's own cart
    private CartItem find(Cart cart, Long itemId) {
        return cart.getItems().stream().filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("Cart item not found"));
    }

    private void checkStock(Product p, int qty) {
        if (qty > p.getStockQuantity()) {
            throw ApiException.conflict("INSUFFICIENT_STOCK",
                    "Only " + p.getStockQuantity() + " units are currently available");
        }
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream().map(i -> {
            BigDecimal unit = i.getProduct().getPrice(); // always the current DB price
            return new CartItemResponse(i.getId(), ProductResponse.from(i.getProduct()), unit, i.getQuantity(),
                    unit.multiply(BigDecimal.valueOf(i.getQuantity())));
        }).toList();
        BigDecimal total = items.stream().map(CartItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), items, total);
    }
}
