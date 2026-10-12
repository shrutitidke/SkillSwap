package com.skillswap.dto;

import com.skillswap.enums.SkillType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SkillDiscoveryResponse {

    private Long id;
    private String name;
    private String description;
    private SkillType type;
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
