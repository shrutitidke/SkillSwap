package com.skillswap.repository;

import com.skillswap.entity.User;
import com.skillswap.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findAllByRole(Role role);
}
