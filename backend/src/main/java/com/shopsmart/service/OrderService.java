package com.shopsmart.service;

import com.shopsmart.dto.Dtos.*;
import com.shopsmart.entity.*;
import com.shopsmart.exception.ApiException;
import com.shopsmart.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepository orders;
    private final ProductRepository products;
    private final CartService cartService;
    private final CartRepository carts;
    private final UserRepository users;

    /** Reads the server-side cart, re-validates stock under row locks, decrements stock, clears the cart. */
    public OrderResponse place(User u, OrderRequest r) {
        Cart cart = cartService.cartFor(u);
        if (cart.getItems().isEmpty()) throw ApiException.badRequest("EMPTY_CART", "Your cart is empty");

        Order order = new Order();
        order.setUser(users.getReferenceById(u.getId()));
        order.setDeliveryAddress(r.deliveryAddress());
        order.setDeliveryMethod(r.deliveryMethod());

        BigDecimal total = BigDecimal.ZERO;
        // lock products in id order to avoid deadlocks between concurrent orders
        List<CartItem> sorted = cart.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getProduct().getId())).toList();
        for (CartItem ci : sorted) {
            Product p = products.findByIdForUpdate(ci.getProduct().getId())
                    .orElseThrow(() -> ApiException.notFound("Product not found"));
            if (!p.isActive()) {
                throw ApiException.conflict("PRODUCT_UNAVAILABLE", p.getName() + " is no longer available");
            }
            if (ci.getQuantity() > p.getStockQuantity()) {
                throw ApiException.conflict("INSUFFICIENT_STOCK", "Only " + p.getStockQuantity()
                        + " units of " + p.getName() + " are currently available");
            }
            p.setStockQuantity(p.getStockQuantity() - ci.getQuantity());
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(p);
            oi.setQuantity(ci.getQuantity());
            oi.setPriceAtPurchase(p.getPrice());
            order.getItems().add(oi);
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PLACED);
        addEvent(order, OrderStatus.PLACED, "Order placed successfully (payment simulated)");
        orders.save(order);

        cart.getItems().clear();
        carts.save(cart);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list(User u) {
        return orders.findByUserIdOrderByCreatedAtDesc(u.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse get(User u, Long id) { return toResponse(authorized(u, id)); }

    @Transactional(readOnly = true)
    public TrackingResponse tracking(User u, Long id) {
        Order o = authorized(u, id);
        return new TrackingResponse(o.getId(), o.getStatus(), o.getEvents().stream()
                .map(e -> new OrderEventResponse(e.getStatus(), e.getDescription(), e.getCreatedAt())).toList());
    }

    public OrderResponse cancel(User u, Long id) {
        Order o = authorized(u, id);
        if (o.getStatus() != OrderStatus.PLACED && o.getStatus() != OrderStatus.PROCESSING) {
            throw ApiException.conflict("CANNOT_CANCEL", "Orders in status " + o.getStatus() + " cannot be cancelled");
        }
        cancelAndRestock(o, "Order cancelled by customer");
        return toResponse(o);
    }

    /** Admin: simulate fulfilment progress. */
    public OrderResponse adminSetStatus(Long id, OrderStatus next) {
        Order o = orders.findById(id).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.CANCELLED) {
            throw ApiException.conflict("FINAL_STATUS", "Order is already " + o.getStatus());
        }
        if (next == OrderStatus.CANCELLED) {
            cancelAndRestock(o, "Order cancelled by admin");
        } else if (next.ordinal() <= o.getStatus().ordinal()) {
            throw ApiException.badRequest("INVALID_TRANSITION", "Status can only move forward");
        } else {
            o.setStatus(next);
            addEvent(o, next, "Order status updated to " + next);
        }
        return toResponse(o);
    }

    private void cancelAndRestock(Order o, String note) {
        o.getItems().stream().sorted(Comparator.comparing(i -> i.getProduct().getId())).forEach(i -> {
            Product p = products.findByIdForUpdate(i.getProduct().getId()).orElseThrow();
            p.setStockQuantity(p.getStockQuantity() + i.getQuantity());
        });
        o.setStatus(OrderStatus.CANCELLED);
        addEvent(o, OrderStatus.CANCELLED, note);
    }

    private void addEvent(Order o, OrderStatus s, String description) {
        OrderEvent e = new OrderEvent();
        e.setOrder(o);
        e.setStatus(s);
        e.setDescription(description);
        o.getEvents().add(e);
    }

    private Order authorized(User u, Long id) {
        Order o = orders.findById(id).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (u.getRole() != Role.ADMIN && !o.getUser().getId().equals(u.getId())) {
            throw ApiException.forbidden("You do not have access to this order");
        }
        return o;
    }

    private OrderResponse toResponse(Order o) {
        return new OrderResponse(o.getId(), o.getStatus(), o.getTotalAmount(), o.getDeliveryAddress(),
                o.getDeliveryMethod(), o.getCreatedAt(), o.getItems().stream()
                .map(i -> new OrderItemResponse(i.getProduct().getId(), i.getProduct().getName(),
                        i.getQuantity(), i.getPriceAtPurchase())).toList());
    }
}
