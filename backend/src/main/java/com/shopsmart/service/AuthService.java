package com.shopsmart.service;

import com.shopsmart.dto.Dtos.*;
import com.shopsmart.entity.*;
import com.shopsmart.exception.ApiException;
import com.shopsmart.repository.MembershipRepository;
import com.shopsmart.repository.UserRepository;
import com.shopsmart.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository users;
    private final MembershipRepository memberships;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final MembershipService membershipService;

    public UserResponse register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("EMAIL_ALREADY_REGISTERED", "An account with this email already exists");
        }
        User u = new User();
        u.setName(r.name().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setRole(Role.CUSTOMER); // role is never taken from the client
        users.save(u);
        createBasicMembership(u);
        return toResponse(u);
    }

    public LoginResponse login(LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password"));
        return new LoginResponse(jwt.generate(u), "Bearer", toResponse(u));
    }

    public void createBasicMembership(User u) {
        Membership m = new Membership();
        m.setUser(u);
        m.setTier(MembershipTier.BASIC);
        m.setStatus(MembershipStatus.ACTIVE);
        m.setStartDate(Instant.now());
        memberships.save(m); // BASIC has no expiry
    }

    public UserResponse toResponse(User u) {
        MembershipTier tier = membershipService.current(u.getId()).getTier(); // applies expiry first
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), tier);
    }
}
