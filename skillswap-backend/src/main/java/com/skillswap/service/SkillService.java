package com.skillswap.service;

import com.skillswap.dto.SkillRequest;
import com.skillswap.dto.SkillResponse;
import com.skillswap.entity.Skill;
import com.skillswap.entity.User;
import com.skillswap.exception.SkillNotFoundException;
import com.skillswap.exception.UserNotFoundException;
import com.skillswap.repository.SkillRepository;
import com.skillswap.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import com.skillswap.enums.Role;
import com.skillswap.enums.SkillType;
import com.skillswap.dto.SkillDiscoveryResponse;

@Service
public class SkillService {

    private final SkillRepository skillRepository;
    private final UserRepository userRepository;

    public SkillService(
            SkillRepository skillRepository,
            UserRepository userRepository) {
        this.skillRepository = skillRepository;
        this.userRepository = userRepository;
    }

    //@Transactional as a safety arrangement for database work
    //Runs the database operation within a transaction. If a database operation fails, the transaction can be rolled back.
    //It helps ensure that related database operations succeed together or roll back together when a failure requires it.
    @Transactional
    public SkillResponse addSkill(
            SkillRequest request,
            Authentication authentication) {

        // Find the currently logged-in user
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        // Create a new skill
        Skill skill = new Skill();
        skill.setName(request.getName());
        skill.setDescription(request.getDescription());
        skill.setType(request.getType());
        skill.setUser(user);

        // Save the skill in the database
        Skill savedSkill = skillRepository.save(skill);

        // Return the response
        return new SkillResponse(
                savedSkill.getId(),
                savedSkill.getName(),
                savedSkill.getDescription(),
                savedSkill.getType(),
                savedSkill.getCreatedAt(),
                savedSkill.getUpdatedAt()
        );
    }

    public List<SkillResponse> getMySkills(Authentication authentication) {

        // Get the logged-in user's email
        String email = authentication.getName();

        // Find the user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        // Find all skills belonging to this user
        List<Skill> skills = skillRepository.findByUserId(user.getId());

        // Convert Skill entities into SkillResponse objects
        return skills.stream()
                .map(skill -> new SkillResponse(
                        skill.getId(),
                        skill.getName(),
                        skill.getDescription(),
                        skill.getType(),
                        skill.getCreatedAt(),
                        skill.getUpdatedAt()
                ))
                .toList();
    }

    @Transactional
    public SkillResponse updateSkill(
            Long skillId,
            SkillRequest request,
            Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        Skill skill = skillRepository
                .findByIdAndUserId(skillId, user.getId())
                .orElseThrow(() ->
                        new SkillNotFoundException("Skill not found"));

        skill.setName(request.getName());
        skill.setDescription(request.getDescription());
        skill.setType(request.getType());

        Skill updatedSkill = skillRepository.save(skill);

        return new SkillResponse(
                updatedSkill.getId(),
                updatedSkill.getName(),
                updatedSkill.getDescription(),
                updatedSkill.getType(),
                updatedSkill.getCreatedAt(),
                updatedSkill.getUpdatedAt()
        );
    }


    @Transactional
    public void deleteSkill(Long skillId, Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Skill skill = skillRepository.findByIdAndUserId(skillId, user.getId())
                .orElseThrow(() -> new SkillNotFoundException("Skill not found"));

        skillRepository.delete(skill);
    }


    public List<SkillDiscoveryResponse> discoverSkills(
            Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        List<Skill> skills = skillRepository.findDiscoverableSkills(
                SkillType.TEACH,
                user.getId(),
                Role.ADMIN
        );

        return skills.stream()
                .map(skill -> new SkillDiscoveryResponse(
                        skill.getId(),
                        skill.getName(),
                        skill.getDescription(),
                        skill.getType(),
                        skill.getUser().getId(),
                        skill.getUser().getName(),
                        skill.getCreatedAt(),
                        skill.getUpdatedAt()
                ))
                .toList();
    }



}

