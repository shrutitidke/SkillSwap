package com.skillswap.dto;

import com.skillswap.enums.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ClassSessionResponseDTO {

    private Long id;
    private Long requestId;
    private Long proposerId;
    private String proposerName;
    private String teachingSkillName;
    private String learnerName;
    private String teacherName;
    private String skillName;
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private String meetingLink;
    private String description;
    private SessionStatus status;
}
