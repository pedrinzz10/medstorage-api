package com.saas.MedStorage_api.user.repository;

import com.saas.MedStorage_api.user.entity.User;
import com.saas.MedStorage_api.user.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRoleInAndAtivoTrue(List<UserRole> roles);
}
