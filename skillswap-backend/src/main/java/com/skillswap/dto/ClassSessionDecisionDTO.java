package com.skillswap.dto;

import com.skillswap.enums.SessionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClassSessionDecisionDTO {

    @NotNull(message = "Session status is required")
    private SessionStatus status;
}