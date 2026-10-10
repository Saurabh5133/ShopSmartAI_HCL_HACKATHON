package com.shopsmart.controller;

import com.shopsmart.dto.Dtos.*;
import com.shopsmart.entity.User;
import com.shopsmart.service.CartService;
import com.shopsmart.service.OrderService;
import com.shopsmart.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/** Wishlist, preferences, cart and orders. Every method works on the authenticated user only. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CustomerController {
    private final WishlistService wishlist;
    private final CartService cart;
    private final OrderService orders;

    // ----- wishlist -----
    @GetMapping("/wishlist")
    public List<ProductResponse> wishlist(@AuthenticationPrincipal User u) { return wishlist.list(u); }

    @PostMapping("/wishlist/items") @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse addWishlist(@AuthenticationPrincipal User u, @Valid @RequestBody WishlistRequest r) {
        return wishlist.add(u, r.productId());
    }

    @DeleteMapping("/wishlist/items/{productId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeWishlist(@AuthenticationPrincipal User u, @PathVariable Long productId) { wishlist.remove(u, productId); }

    // ----- preferences -----
    @GetMapping("/users/me/preferences")
    public Map<String, Object> prefs(@AuthenticationPrincipal User u) { return wishlist.getPreferences(u); }

    @PutMapping("/users/me/preferences")
    public Map<String, Object> savePrefs(@AuthenticationPrincipal User u, @Valid @RequestBody PreferencesRequest r) {
        return wishlist.savePreferences(u, r);
    }

    // ----- cart -----
    @GetMapping("/cart")
    public CartResponse cart(@AuthenticationPrincipal User u) { return cart.get(u); }

    @PostMapping("/cart/items") @ResponseStatus(HttpStatus.CREATED)
    public CartResponse addItem(@AuthenticationPrincipal User u, @Valid @RequestBody CartItemRequest r) { return cart.add(u, r); }

    @PatchMapping("/cart/items/{itemId}")
    public CartResponse updateItem(@AuthenticationPrincipal User u, @PathVariable Long itemId,
                                   @Valid @RequestBody CartItemUpdate r) { return cart.update(u, itemId, r); }

    @DeleteMapping("/cart/items/{itemId}")
    public CartResponse removeItem(@AuthenticationPrincipal User u, @PathVariable Long itemId) { return cart.remove(u, itemId); }

    @DeleteMapping("/cart") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@AuthenticationPrincipal User u) { cart.clear(u); }

    // ----- orders -----
    @PostMapping("/orders") @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(@AuthenticationPrincipal User u, @Valid @RequestBody OrderRequest r) { return orders.place(u, r); }

    @GetMapping("/orders")
    public List<OrderResponse> myOrders(@AuthenticationPrincipal User u) { return orders.list(u); }

    @GetMapping("/orders/{id}")
    public OrderResponse order(@AuthenticationPrincipal User u, @PathVariable Long id) { return orders.get(u, id); }

    @GetMapping("/orders/{id}/tracking")
    public TrackingResponse tracking(@AuthenticationPrincipal User u, @PathVariable Long id) { return orders.tracking(u, id); }

    @PostMapping("/orders/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal User u, @PathVariable Long id) { return orders.cancel(u, id); }
}
