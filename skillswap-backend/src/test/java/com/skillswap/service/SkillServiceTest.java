package com.skillswap.service;

import com.skillswap.dto.SkillRequest;
import com.skillswap.dto.SkillResponse;
import com.skillswap.dto.ReciprocalMatchResponse;
import com.skillswap.entity.Skill;
import com.skillswap.entity.User;
import com.skillswap.enums.Role;
import com.skillswap.enums.SkillType;
import com.skillswap.exception.SkillNotFoundException;
import com.skillswap.repository.SkillRepository;
import com.skillswap.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    private SkillService skillService;
    private User user;

    @BeforeEach
    void setUp() {
        skillService = new SkillService(skillRepository, userRepository);
        user = new User();
        user.setId(21L);
        user.setName("Member");
        user.setEmail("member@example.com");
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
    }

    @Test
    void addSkillTrimsAndNormalizesNameAndDescription() {
        SkillRequest request = new SkillRequest();
        request.setName("  Java   Script  ");
        request.setDescription("  Build browser apps.  ");
        request.setType(SkillType.TEACH);
        when(skillRepository.save(any(Skill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SkillResponse response =
                skillService.addSkill(request, authentication);

        assertEquals("Java Script", response.getName());
        assertEquals("Build browser apps.", response.getDescription());
        assertEquals(SkillType.TEACH, response.getType());
        verify(skillRepository).save(argThat(skill ->
                skill.getUser().getId().equals(user.getId())));
    }

    @Test
    void updateCannotAccessAnotherUsersSkill() {
        SkillRequest request = new SkillRequest();
        request.setName("Java");
        request.setType(SkillType.LEARN);
        when(skillRepository.findByIdAndUserId(99L, user.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                SkillNotFoundException.class,
                () -> skillService.updateSkill(99L, request, authentication));
        verify(skillRepository, never()).save(any(Skill.class));
    }

    @Test
    void deleteCannotAccessAnotherUsersSkill() {
        when(skillRepository.findByIdAndUserId(99L, user.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                SkillNotFoundException.class,
                () -> skillService.deleteSkill(99L, authentication));
        verify(skillRepository, never()).delete(any(Skill.class));
    }

    @Test
    void reciprocalMatchesRequireBothDirectionsAndExcludeAdminsAndDuplicates() {
        Skill myTeachJavaScript = skill(
                1L, user, "JavaScript", SkillType.TEACH, "Frontend development");
        Skill myLearnJava = skill(
                2L, user, " Java ", SkillType.LEARN, null);

        User reciprocalUser = user(31L, "Alex", Role.USER);
        Skill alexTeachesJava = skill(
                3L, reciprocalUser, " java ", SkillType.TEACH, "Core Java");
        Skill duplicateJava = skill(
                4L, reciprocalUser, "JAVA", SkillType.TEACH, "Duplicate entry");
        Skill alexLearnsJavaScript = skill(
                5L, reciprocalUser, " JAVASCRIPT ", SkillType.LEARN, null);

        User oneWayUser = user(32L, "One Way", Role.USER);
        Skill oneWayJava = skill(
                6L, oneWayUser, "Java", SkillType.TEACH, null);

        User admin = user(33L, "Admin", Role.ADMIN);
        Skill adminTeachesJava = skill(
                7L, admin, "Java", SkillType.TEACH, null);
        Skill adminLearnsJavaScript = skill(
                8L, admin, "JavaScript", SkillType.LEARN, null);

        when(skillRepository.findByUserId(user.getId()))
                .thenReturn(List.of(myTeachJavaScript, myLearnJava));
        when(skillRepository.findDiscoverableSkills(
                null, user.getId(), Role.ADMIN))
                .thenReturn(List.of(
                        alexTeachesJava,
                        duplicateJava,
                        alexLearnsJavaScript,
                        oneWayJava,
                        adminTeachesJava,
                        adminLearnsJavaScript,
                        myTeachJavaScript,
                        myLearnJava));

        List<ReciprocalMatchResponse> matches =
                skillService.getReciprocalMatches(authentication);

        assertEquals(1, matches.size());
        ReciprocalMatchResponse match = matches.get(0);
        assertEquals(reciprocalUser.getId(), match.userId());
        assertEquals("Alex", match.userName());
        assertEquals(alexTeachesJava.getId(), match.skillTheyTeachId());
        assertEquals("java", match.skillTheyTeach());
        assertEquals("Frontend development", match.skillYouTeachDescription());
    }

    private Skill skill(
            Long id,
            User owner,
            String name,
            SkillType type,
            String description) {
        Skill skill = new Skill();
        skill.setId(id);
        skill.setUser(owner);
        skill.setName(name);
        skill.setType(type);
        skill.setDescription(description);
        return skill;
    }

    private User user(Long id, String name, Role role) {
        User other = new User();
        other.setId(id);
        other.setName(name);
        other.setEmail(name.toLowerCase() + "@example.com");
        other.setRole(role);
        return other;
    }
}
