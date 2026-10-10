package com.shopsmart.service;

import com.shopsmart.dto.Dtos.*;
import com.shopsmart.entity.*;
import com.shopsmart.exception.ApiException;
import com.shopsmart.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Wishlist + customer preferences (both small, both per-user). */
@Service
@RequiredArgsConstructor
@Transactional
public class WishlistService {
    private final WishlistRepository wishlist;
    private final ProductRepository products;
    private final UserRepository users;
    private final CustomerPreferenceRepository prefs;

    @Transactional(readOnly = true)
    public List<ProductResponse> list(User u) {
        return wishlist.findByUserIdOrderByCreatedAtDesc(u.getId()).stream()
                .map(w -> ProductResponse.from(w.getProduct())).toList();
    }

    public ProductResponse add(User u, Long productId) {
        Product p = products.findById(productId).filter(Product::isActive)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        if (wishlist.findByUserIdAndProductId(u.getId(), productId).isEmpty()) {
            WishlistItem w = new WishlistItem();
            w.setUser(users.getReferenceById(u.getId()));
            w.setProduct(p);
            wishlist.save(w);
        }
        return ProductResponse.from(p);
    }

    public void remove(User u, Long productId) {
        WishlistItem w = wishlist.findByUserIdAndProductId(u.getId(), productId)
                .orElseThrow(() -> ApiException.notFound("Item not in wishlist"));
        wishlist.delete(w);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPreferences(User u) {
        return prefs.findByUserId(u.getId()).map(CustomerPreference::getPreferences).orElse(Map.of());
    }

    public Map<String, Object> savePreferences(User u, PreferencesRequest r) {
        if (r.budgetMin() != null && r.budgetMax() != null && r.budgetMin().compareTo(r.budgetMax()) > 0) {
            throw ApiException.badRequest("INVALID_BUDGET", "budgetMin cannot be greater than budgetMax");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("preferredCategories", r.preferredCategories() == null ? List.of() : r.preferredCategories());
        m.put("preferredBrands", r.preferredBrands() == null ? List.of() : r.preferredBrands());
        m.put("budgetMin", r.budgetMin());
        m.put("budgetMax", r.budgetMax());
        m.put("useCases", r.useCases() == null ? List.of() : r.useCases());
        CustomerPreference cp = prefs.findByUserId(u.getId()).orElseGet(() -> {
            CustomerPreference n = new CustomerPreference();
            n.setUser(users.getReferenceById(u.getId()));
            return n;
        });
        cp.setPreferences(m);
        return prefs.save(cp).getPreferences();
    }
}
