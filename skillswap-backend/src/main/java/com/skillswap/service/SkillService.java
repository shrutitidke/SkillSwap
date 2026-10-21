package com.skillswap.service;

import com.skillswap.dto.SkillRequest;
import com.skillswap.dto.SkillResponse;
import com.skillswap.dto.ReciprocalMatchResponse;
import com.skillswap.dto.SkillDiscoveryResponse;
import com.skillswap.entity.Skill;
import com.skillswap.entity.User;
import com.skillswap.enums.Role;
import com.skillswap.enums.SkillType;
import com.skillswap.exception.SkillNotFoundException;
import com.skillswap.exception.UserNotFoundException;
import com.skillswap.repository.SkillRepository;
import com.skillswap.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        skill.setName(normalizeSkillName(request.getName()));
        skill.setDescription(normalizeDescription(request.getDescription()));
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

        skill.setName(normalizeSkillName(request.getName()));
        skill.setDescription(normalizeDescription(request.getDescription()));
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

    @Transactional(readOnly = true)
    public List<ReciprocalMatchResponse> getReciprocalMatches(
            Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        if (currentUser.getRole() == Role.ADMIN) {
            return List.of();
        }

        List<Skill> currentSkills =
                skillRepository.findByUserId(currentUser.getId());
        Map<String, Skill> currentTeachSkills =
                skillsByNormalizedName(currentSkills, SkillType.TEACH);
        Map<String, Skill> currentLearnSkills =
                skillsByNormalizedName(currentSkills, SkillType.LEARN);

        if (currentTeachSkills.isEmpty() || currentLearnSkills.isEmpty()) {
            return List.of();
        }

        List<Skill> discoverableSkills =
                skillRepository.findDiscoverableSkills(
                        null,
                        currentUser.getId(),
                        Role.ADMIN);
        Map<Long, List<Skill>> skillsByUser = new LinkedHashMap<>();
        for (Skill skill : discoverableSkills) {
            User owner = skill.getUser();
            if (owner.getRole() != Role.ADMIN
                    && !owner.getId().equals(currentUser.getId())) {
                skillsByUser.computeIfAbsent(
                        owner.getId(),
                        ignored -> new ArrayList<>()
                ).add(skill);
            }
        }

        Map<MatchKey, ReciprocalMatchResponse> matches =
                new LinkedHashMap<>();

        for (List<Skill> otherSkills : skillsByUser.values()) {
            Map<String, Skill> otherTeachSkills =
                    skillsByNormalizedName(otherSkills, SkillType.TEACH);
            Map<String, Skill> otherLearnSkills =
                    skillsByNormalizedName(otherSkills, SkillType.LEARN);

            for (Map.Entry<String, Skill> currentTeach
                    : currentTeachSkills.entrySet()) {
                Skill otherLearn = otherLearnSkills.get(currentTeach.getKey());
                if (otherLearn == null) {
                    continue;
                }

                for (Map.Entry<String, Skill> currentLearn
                        : currentLearnSkills.entrySet()) {
                    Skill otherTeach = otherTeachSkills.get(
                            currentLearn.getKey());
                    if (otherTeach == null) {
                        continue;
                    }

                    User otherUser = otherTeach.getUser();
                    MatchKey key = new MatchKey(
                            otherUser.getId(),
                            currentLearn.getKey(),
                            currentTeach.getKey());
                    matches.putIfAbsent(
                            key,
                            new ReciprocalMatchResponse(
                                    otherUser.getId(),
                                    otherUser.getName(),
                                    otherTeach.getId(),
                                    currentTeach.getValue().getId(),
                                    normalizeSkillName(otherTeach.getName()),
                                    otherTeach.getDescription(),
                                    normalizeSkillName(
                                            currentTeach.getValue().getName()),
                                    currentTeach.getValue().getDescription(),
                                    "You can teach "
                                            + normalizeSkillName(
                                                    currentTeach.getValue()
                                                            .getName())
                                            + " while "
                                            + otherUser.getName()
                                            + " teaches you "
                                            + normalizeSkillName(
                                                    otherTeach.getName())
                                            + "."));
                }
            }
        }

        return matches.values().stream()
                .sorted(Comparator
                        .comparing(
                                ReciprocalMatchResponse::userName,
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(
                                ReciprocalMatchResponse::skillYouTeach,
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(
                                ReciprocalMatchResponse::skillTheyTeach,
                                String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Map<String, Skill> skillsByNormalizedName(
            List<Skill> skills,
            SkillType type) {
        Map<String, Skill> skillsByName = new LinkedHashMap<>();
        skills.stream()
                .filter(skill -> skill.getType() == type)
                .sorted(Comparator.comparing(
                        Skill::getId,
                        Comparator.nullsLast(Long::compareTo)))
                .forEach(skill -> skillsByName.putIfAbsent(
                        normalizedSkillKey(skill.getName()),
                        skill));
        return skillsByName;
    }

    private String normalizedSkillKey(String name) {
        return normalizeSkillName(name).toLowerCase(Locale.ROOT);
    }

    private record MatchKey(
            Long otherUserId,
            String theyTeach,
            String youTeach) {
    }

    private String normalizeSkillName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }

        String normalized = description.trim();
        return normalized.isEmpty() ? null : normalized;
    }

}
