package com.skillswap.controller;

import com.skillswap.dto.SkillDiscoveryResponse;
import com.skillswap.dto.ReciprocalMatchResponse;
import com.skillswap.dto.SkillRequest;
import com.skillswap.dto.SkillResponse;
import com.skillswap.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @PostMapping
    public ResponseEntity<String> addSkill(
            @Valid @RequestBody SkillRequest request,
            Authentication authentication) {

        skillService.addSkill(request, authentication);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Skill added successfully");
    }

    @GetMapping("/my-skills")
    public ResponseEntity<List<SkillResponse>> getMySkills(
            Authentication authentication) {

        List<SkillResponse> skills =
                skillService.getMySkills(authentication);

        return ResponseEntity.ok(skills);
    }

    @PutMapping("/{skillId}")
    public ResponseEntity<String> updateSkill(
            @PathVariable Long skillId,
            @Valid @RequestBody SkillRequest request,
            Authentication authentication) {

        skillService.updateSkill(skillId, request, authentication);

        return ResponseEntity.ok("Skill updated successfully");
    }

    @DeleteMapping("/{skillId}")
    public ResponseEntity<String> deleteSkill(
            @PathVariable Long skillId,
            Authentication authentication) {

        skillService.deleteSkill(skillId, authentication);

        return ResponseEntity.ok("Skill deleted successfully");
    }

    @GetMapping("/discover")
    public ResponseEntity<List<SkillDiscoveryResponse>> discoverSkills(
            Authentication authentication) {

        List<SkillDiscoveryResponse> skills =
                skillService.discoverSkills(authentication);

        return ResponseEntity.ok(skills);
    }

    @GetMapping("/matches")
    public ResponseEntity<List<ReciprocalMatchResponse>> getReciprocalMatches(
            Authentication authentication) {
        return ResponseEntity.ok(
                skillService.getReciprocalMatches(authentication));
    }

}
