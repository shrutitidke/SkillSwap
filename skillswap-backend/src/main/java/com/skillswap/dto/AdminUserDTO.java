package com.skillswap.dto;

import com.skillswap.enums.Role;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserDTO(
        Long id,
        String name,
        String email,
        Role role,
        LocalDateTime createdAt,
        List<AdminSkillDTO> skills) {
}
