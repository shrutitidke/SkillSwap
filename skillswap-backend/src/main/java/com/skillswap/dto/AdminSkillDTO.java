package com.skillswap.dto;

import com.skillswap.enums.SkillType;

public record AdminSkillDTO(
        Long id,
        String name,
        String description,
        SkillType type) {
}
