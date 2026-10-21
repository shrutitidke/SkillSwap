package com.skillswap.dto;

import com.skillswap.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SkillExchangeRequestResponseDTO {

    private Long id;
    private String learnerName;
    private String teacherName;
    private String message;
    private String skillName;
    private RequestStatus status;
    private Long teacherSkillId;
    private Long learnerSkillId;
    private String learnerSkillName;
    private LocalDateTime acceptedAt;
    private boolean skillsChangedSinceAcceptance;
}
