package com.shopsmart.controller;

import com.shopsmart.dto.Dtos.*;
import com.shopsmart.entity.User;
import com.shopsmart.service.MembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/memberships")
@RequiredArgsConstructor
public class MembershipController {
    private final MembershipService service;

    @GetMapping("/me")
    public MembershipResponse me(@AuthenticationPrincipal User u) { return service.me(u); }

    @GetMapping("/me/history")
    public List<MembershipResponse> history(@AuthenticationPrincipal User u) { return service.history(u); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public MembershipResponse activate(@AuthenticationPrincipal User u, @Valid @RequestBody MembershipRequest r) {
        return service.activate(u, r.tier());
    }

    @PostMapping("/me/renew")
    public MembershipResponse renew(@AuthenticationPrincipal User u, @Valid @RequestBody RenewRequest r) {
        return service.renew(u, r.tier(), r.durationDays());
    }
}
