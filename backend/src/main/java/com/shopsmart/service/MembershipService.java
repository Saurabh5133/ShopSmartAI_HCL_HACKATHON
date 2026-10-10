package com.shopsmart.service;

import com.shopsmart.dto.Dtos.*;
import com.shopsmart.entity.*;
import com.shopsmart.exception.ApiException;
import com.shopsmart.repository.MembershipRepository;
import com.shopsmart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Rules (payment is out of scope, nothing is charged):
 *  - exactly one ACTIVE membership per user (also enforced by a partial unique index);
 *  - BASIC never expires; PRIME/PREMIUM last 30 days when activated;
 *  - changing tier takes effect immediately: the old ACTIVE record becomes CANCELLED;
 *  - renewal extends the end date by 30/90/365 days (same tier only);
 *  - expiry is applied lazily on every read: expired paid tiers become EXPIRED and the user falls back to BASIC.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MembershipService {
    private static final Set<Integer> ALLOWED_DAYS = Set.of(30, 90, 365);
    private static final int ACTIVATION_DAYS = 30;

    private final MembershipRepository memberships;
    private final UserRepository users;

    /** Returns the user's current ACTIVE membership after applying expiry. */
    public Membership current(Long userId) {
        Instant now = Instant.now();
        List<Membership> active = memberships.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE);
        for (Membership m : active) {
            if (m.getEndDate() != null && m.getEndDate().isBefore(now)) {
                m.setStatus(MembershipStatus.EXPIRED);
                memberships.saveAndFlush(m);
            }
        }
        Optional<Membership> stillActive = active.stream()
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE).findFirst();
        return stillActive.orElseGet(() -> startNew(userId, MembershipTier.BASIC, null));
    }

    public MembershipResponse me(User u) { return MembershipResponse.from(current(u.getId())); }

    public List<MembershipResponse> history(User u) {
        current(u.getId()); // apply expiry before listing
        return memberships.findByUserIdOrderByStartDateDescIdDesc(u.getId()).stream()
                .map(MembershipResponse::from).toList();
    }

    public MembershipResponse activate(User u, MembershipTier tier) {
        Membership cur = current(u.getId());
        if (cur.getTier() == tier) {
            throw ApiException.conflict("ALREADY_ACTIVE", tier == MembershipTier.BASIC
                    ? "You are already on the BASIC tier"
                    : "Your " + tier + " membership is already active. Use /memberships/me/renew to extend it");
        }
        cur.setStatus(MembershipStatus.CANCELLED); // upgrade / downgrade
        cur.setEndDate(Instant.now());
        memberships.saveAndFlush(cur); // flush before inserting so the unique "one ACTIVE" index is respected
        return MembershipResponse.from(startNew(u.getId(), tier, tier == MembershipTier.BASIC ? null : ACTIVATION_DAYS));
    }

    public MembershipResponse renew(User u, MembershipTier tier, int durationDays) {
        if (tier == MembershipTier.BASIC) {
            throw ApiException.badRequest("INVALID_TIER", "BASIC membership does not expire and cannot be renewed");
        }
        if (!ALLOWED_DAYS.contains(durationDays)) {
            throw ApiException.badRequest("INVALID_DURATION", "durationDays must be one of " + ALLOWED_DAYS);
        }
        Membership cur = current(u.getId());
        if (cur.getTier() == tier) {
            cur.setEndDate(cur.getEndDate().plus(durationDays, ChronoUnit.DAYS)); // extend from current end date
            return MembershipResponse.from(memberships.saveAndFlush(cur));
        }
        if (cur.getTier() == MembershipTier.BASIC) { // previous paid tier expired (or never bought): start fresh
            return MembershipResponse.from(startNew(u.getId(), tier, durationDays));
        }
        throw ApiException.conflict("TIER_MISMATCH", "Your active tier is " + cur.getTier()
                + ". Activate " + tier + " via POST /memberships instead");
    }

    private Membership startNew(Long userId, MembershipTier tier, Integer days) {
        Instant now = Instant.now();
        Membership m = new Membership();
        m.setUser(users.getReferenceById(userId));
        m.setTier(tier);
        m.setStatus(MembershipStatus.ACTIVE);
        m.setStartDate(now);
        m.setEndDate(days == null ? null : now.plus(days, ChronoUnit.DAYS));
        return memberships.saveAndFlush(m);
    }
}
