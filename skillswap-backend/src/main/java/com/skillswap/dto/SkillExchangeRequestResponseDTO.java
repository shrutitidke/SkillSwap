
package com.skillswap.dto;

import com.skillswap.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SkillExchangeRequestResponseDTO {

    private String learnerName;
    private String teacherName;
    private String message;
    private String skillName;
    private RequestStatus status;
}
