package com.shopsmart.repository;

import com.shopsmart.entity.Membership;
import com.shopsmart.entity.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    List<Membership> findByUserIdAndStatus(Long userId, MembershipStatus status);
    List<Membership> findByUserIdOrderByStartDateDescIdDesc(Long userId);
    Optional<Membership> findFirstByUserIdAndStatusOrderByStartDateDesc(Long userId, MembershipStatus status);
}
