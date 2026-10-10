package com.shopsmart.repository;

import com.shopsmart.entity.CustomerPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerPreferenceRepository extends JpaRepository<CustomerPreference, Long> {
    Optional<CustomerPreference> findByUserId(Long userId);
}
