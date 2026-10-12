package com.skillswap.dto;

import com.skillswap.enums.RequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SkillExchangeRequestDecisionDTO {

    @NotNull(message = "Status is required")
    private RequestStatus status;
}
