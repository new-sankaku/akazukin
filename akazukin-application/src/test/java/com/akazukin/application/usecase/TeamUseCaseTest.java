package com.akazukin.application.usecase;

import com.akazukin.application.dto.TeamResponseDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.Role;
import com.akazukin.domain.model.Team;
import com.akazukin.domain.model.TeamMember;
import com.akazukin.domain.model.User;
import com.akazukin.domain.port.TeamRepository;
import com.akazukin.domain.port.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamUseCaseTest {

    private InMemoryTeamRepository teamRepository;
    private InMemoryUserRepository userRepository;
    private TeamUseCase teamUseCase;

    private UUID ownerUserId;
    private UUID memberUserId;

    @BeforeEach
    void setUp() {
        teamRepository = new InMemoryTeamRepository();
        userRepository = new InMemoryUserRepository();
        teamUseCase = new TeamUseCase(teamRepository, userRepository);

        ownerUserId = UUID.randomUUID();
        memberUserId = UUID.randomUUID();

        Instant now = Instant.now();
        userRepository.save(new User(ownerUserId, "owner", "owner@example.com",
                "hashed:pass", Role.ADMIN, now, now));
        userRepository.save(new User(memberUserId, "member", "member@example.com",
                "hashed:pass", Role.USER, now, now));
    }

    @Test
    void createTeam_createsTeamWithOwnerAsMember() {
        TeamResponseDto result = teamUseCase.createTeam(ownerUserId, "Dev Team");

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("Dev Team", result.name());
        assertEquals(ownerUserId, result.ownerUserId());
        assertEquals(1, result.members().size());
        assertEquals(ownerUserId, result.members().get(0).userId());
        assertEquals("ADMIN", result.members().get(0).role());
    }

    @Test
    void createTeam_throwsWhenNameIsNull() {
        DomainException exception = assertThrows(DomainException.class,
                () -> teamUseCase.createTeam(ownerUserId, null));
        assertEquals("INVALID_TEAM_NAME", exception.getErrorCode());
    }

    @Test
    void createTeam_throwsWhenNameIsBlank() {
        DomainException exception = assertThrows(DomainException.class,
                () -> teamUseCase.createTeam(ownerUserId, "  "));
        assertEquals("INVALID_TEAM_NAME", exception.getErrorCode());
    }

    @Test
    void addMember_addsUserToTeam() {
        TeamResponseDto created = teamUseCase.createTeam(ownerUserId, "Team A");

        TeamResponseDto result = teamUseCase.addMember(
                created.id(), ownerUserId, memberUserId, Role.USER);

        assertEquals(2, result.members().size());
    }

    @Test
    void addMember_throwsWhenTeamNotFound() {
        UUID nonExistentTeamId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> teamUseCase.addMember(nonExistentTeamId, ownerUserId, memberUserId, Role.USER));
        assertEquals("TEAM_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void addMember_throwsWhenRequesterIsNotMember() {
        TeamResponseDto created = teamUseCase.createTeam(ownerUserId, "Team A");
        UUID outsiderUserId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> teamUseCase.addMember(created.id(), outsiderUserId, memberUserId, Role.USER));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void addMember_throwsWhenRequesterIsNotAdmin() {
        TeamResponseDto created = teamUseCase.createTeam(ownerUserId, "Team A");
        teamUseCase.addMember(created.id(), ownerUserId, memberUserId, Role.USER);

        UUID anotherUserId = UUID.randomUUID();
        Instant now = Instant.now();
        userRepository.save(new User(anotherUserId, "another", "another@example.com",
                "hashed:pass", Role.USER, now, now));

        DomainException exception = assertThrows(DomainException.class,
                () -> teamUseCase.addMember(created.id(), memberUserId, anotherUserId, Role.USER));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void addMember_throwsWhenTargetUserNotFound() {
        TeamResponseDto created = teamUseCase.createTeam(ownerUserId, "Team A");
        UUID nonExistentUserId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> teamUseCase.addMember(created.id(), ownerUserId, nonExistentUserId, Role.USER));
        assertEquals("USER_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void addMember_throwsWhenUserAlreadyMember() {
        TeamResponseDto created = teamUseCase.createTeam(ownerUserId, "Team A");
        teamUseCase.addMember(created.id(), ownerUserId, memberUserId, Role.USER);

        DomainException exception = assertThrows(DomainException.class,
                () -> teamUseCase.addMember(created.id(), ownerUserId, memberUserId, Role.USER));
        assertEquals("ALREADY_MEMBER", exception.getErrorCode());
    }

    @Test
    void removeMember_removesMemberFromTeam() {
        TeamResponseDto created = teamUseCase.createTeam(ownerUserId, "Team A");
        teamUseCase.addMember(created.id(), ownerUserId, memberUserId, Role.USER);

        teamUseCase.removeMember(created.id(), ownerUserId, memberUserId);

        TeamResponseDto afterRemoval = teamUseCase.getTeam(created.id(), ownerUserId);
        assertEquals(1, afterRemoval.members().size());
    }

    @Test
    void removeMember_allowsSelfRemoval() {
        TeamResponseDto created = teamUseCase.createTeam(ownerUserId, "Team A");
        teamUseCase.addMember(created.id(), ownerUserId, memberUserId, Role.USER);

        teamUseCase.removeMember(created.id(), memberUserId, memberUserId);

        TeamResponseDto afterRemoval = teamUseCase.getTeam(created.id(), ownerUserId);
        assertEquals(1, afterRemoval.members().size());
    }

    @Test
    void removeMember_throwsWhenTeamNotFound() {
        UUID nonExistentTeamId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> teamUseCase.removeMember(nonExistentTeamId, ownerUserId, memberUserId));
        assertEquals("TEAM_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void removeMember_throwsWhenRemovingOwner() {
        TeamResponseDto created = teamUseCase.createTeam(ownerUserId, "Team A");

        DomainException exception = assertThrows(DomainException.class,
                () -> teamUseCase.removeMember(created.id(), ownerUserId, ownerUserId));
        assertEquals("CANNOT_REMOVE_OWNER", exception.getErrorCode());
    }

    @Test
    void removeMember_throwsWhenNonAdminRemovesOther() {
        TeamResponseDto created = teamUseCase.createTeam(ownerUserId, "Team A");
        teamUseCase.addMember(created.id(), ownerUserId, memberUserId, Role.USER);

        UUID anotherUserId = UUID.randomUUID();
        Instant now = Instant.now();
        userRepository.save(new User(anotherUserId, "another", "another@example.com",
                "hashed:pass", Role.USER, now, now));
        teamUseCase.addMember(created.id(), ownerUserId, anotherUserId, Role.USER);

        DomainException exception = assertThrows(DomainException.class,
                () -> teamUseCase.removeMember(created.id(), memberUserId, anotherUserId));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void removeMember_throwsWhenTargetNotMember() {
        TeamResponseDto created = teamUseCase.createTeam(ownerUserId, "Team A");

        DomainException exception = assertThrows(DomainException.class,
                () -> teamUseCase.removeMember(created.id(), ownerUserId, memberUserId));
        assertEquals("MEMBER_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void getTeam_returnsTeamWithMembers() {
        TeamResponseDto created = teamUseCase.createTeam(ownerUserId, "Team A");

        TeamResponseDto result = teamUseCase.getTeam(created.id(), ownerUserId);

        assertNotNull(result);
        assertEquals(created.id(), result.id());
        assertEquals("Team A", result.name());
        assertEquals(1, result.members().size());
    }

    @Test
    void getTeam_throwsWhenTeamNotFound() {
        UUID nonExistentTeamId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> teamUseCase.getTeam(nonExistentTeamId, ownerUserId));
        assertEquals("TEAM_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void getTeam_throwsWhenUserIsNotMember() {
        TeamResponseDto created = teamUseCase.createTeam(ownerUserId, "Team A");
        UUID outsiderUserId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> teamUseCase.getTeam(created.id(), outsiderUserId));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void listTeams_returnsTeamsForUser() {
        teamUseCase.createTeam(ownerUserId, "Team A");
        teamUseCase.createTeam(ownerUserId, "Team B");

        List<TeamResponseDto> result = teamUseCase.listTeams(ownerUserId);

        assertEquals(2, result.size());
    }

    @Test
    void listTeams_returnsEmptyForUserWithNoTeams() {
        UUID otherUserId = UUID.randomUUID();

        List<TeamResponseDto> result = teamUseCase.listTeams(otherUserId);

        assertTrue(result.isEmpty());
    }

    @Test
    void deleteTeam_removesTeam() {
        TeamResponseDto created = teamUseCase.createTeam(ownerUserId, "Team A");

        teamUseCase.deleteTeam(created.id(), ownerUserId);

        DomainException exception = assertThrows(DomainException.class,
                () -> teamUseCase.getTeam(created.id(), ownerUserId));
        assertEquals("TEAM_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void deleteTeam_throwsWhenTeamNotFound() {
        UUID nonExistentTeamId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> teamUseCase.deleteTeam(nonExistentTeamId, ownerUserId));
        assertEquals("TEAM_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void deleteTeam_throwsWhenNotOwner() {
        TeamResponseDto created = teamUseCase.createTeam(ownerUserId, "Team A");
        teamUseCase.addMember(created.id(), ownerUserId, memberUserId, Role.ADMIN);

        DomainException exception = assertThrows(DomainException.class,
                () -> teamUseCase.deleteTeam(created.id(), memberUserId));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    private static class InMemoryTeamRepository implements TeamRepository {

        private final Map<UUID, Team> teamStore = new HashMap<>();
        private final List<TeamMember> memberStore = new ArrayList<>();

        @Override
        public Optional<Team> findById(UUID id) {
            return Optional.ofNullable(teamStore.get(id));
        }

        @Override
        public List<Team> findByUserId(UUID userId) {
            List<UUID> memberTeamIds = memberStore.stream()
                    .filter(m -> m.getUserId().equals(userId))
                    .map(TeamMember::getTeamId)
                    .toList();
            return teamStore.values().stream()
                    .filter(t -> memberTeamIds.contains(t.getId()))
                    .toList();
        }

        @Override
        public Team save(Team team) {
            if (team.getId() == null) {
                team.setId(UUID.randomUUID());
            }
            teamStore.put(team.getId(), team);
            return team;
        }

        @Override
        public void deleteById(UUID id) {
            teamStore.remove(id);
            memberStore.removeIf(m -> m.getTeamId().equals(id));
        }

        @Override
        public TeamMember addMember(TeamMember member) {
            if (member.getId() == null) {
                member.setId(UUID.randomUUID());
            }
            memberStore.add(member);
            return member;
        }

        @Override
        public void removeMember(UUID teamId, UUID userId) {
            memberStore.removeIf(m -> m.getTeamId().equals(teamId) && m.getUserId().equals(userId));
        }

        @Override
        public List<TeamMember> findMembersByTeamId(UUID teamId) {
            return memberStore.stream()
                    .filter(m -> m.getTeamId().equals(teamId))
                    .toList();
        }

        @Override
        public Map<UUID, List<TeamMember>> findMembersByTeamIds(Collection<UUID> teamIds) {
            Map<UUID, List<TeamMember>> result = new HashMap<>();
            for (UUID teamId : teamIds) {
                result.put(teamId, findMembersByTeamId(teamId));
            }
            return result;
        }

        @Override
        public Optional<TeamMember> findMember(UUID teamId, UUID userId) {
            return memberStore.stream()
                    .filter(m -> m.getTeamId().equals(teamId) && m.getUserId().equals(userId))
                    .findFirst();
        }
    }

    private static class InMemoryUserRepository implements UserRepository {

        private final Map<UUID, User> store = new HashMap<>();

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return store.values().stream()
                    .filter(user -> user.getUsername().equals(username))
                    .findFirst();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return store.values().stream()
                    .filter(user -> user.getEmail().equals(email))
                    .findFirst();
        }

        @Override
        public List<User> findAllByIds(Collection<UUID> ids) {
            return ids.stream()
                    .map(store::get)
                    .filter(user -> user != null)
                    .toList();
        }

        @Override
        public User save(User user) {
            store.put(user.getId(), user);
            return user;
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }
    }
}
