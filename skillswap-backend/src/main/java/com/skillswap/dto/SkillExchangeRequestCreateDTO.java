package com.skillswap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SkillExchangeRequestCreateDTO {

    @NotNull(message = "Skill ID is required")
    private Long skillId;

    @Size(max = 500, message = "Message cannot exceed 500 characters")
    private String message;
}
