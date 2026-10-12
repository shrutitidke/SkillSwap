package com.skillswap.dto;

import com.skillswap.enums.SkillType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SkillResponse {

    private Long id;
    private String name;
    private String description;
    private SkillType type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
