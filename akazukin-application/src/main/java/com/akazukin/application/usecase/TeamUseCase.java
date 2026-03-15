package com.akazukin.application.usecase;

import com.akazukin.application.dto.TeamMemberDto;
import com.akazukin.application.dto.TeamResponseDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.Role;
import com.akazukin.domain.model.Team;
import com.akazukin.domain.model.TeamMember;
import com.akazukin.domain.model.User;
import com.akazukin.domain.port.TeamRepository;
import com.akazukin.domain.port.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;


import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class TeamUseCase {

    private static final Logger LOG = Logger.getLogger(TeamUseCase.class.getName());

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    @Inject
    public TeamUseCase(TeamRepository teamRepository, UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TeamResponseDto createTeam(UUID userId, String name) {
        long perfStart = System.nanoTime();
        try {
            if (name == null || name.isBlank()) {
                throw new DomainException("INVALID_TEAM_NAME", "Team name must not be empty");
            }

            Instant now = Instant.now();
            Team team = new Team(null, name, userId, now, now);
            Team savedTeam = teamRepository.save(team);

            TeamMember ownerMember = new TeamMember(
                    null, savedTeam.getId(), userId, Role.ADMIN, now
            );
            teamRepository.addMember(ownerMember);

            return toResponseDto(savedTeam, List.of(ownerMember));
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"TeamUseCase.createTeam", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"TeamUseCase.createTeam", perfMs});
            }
        }
    }

    @Transactional
    public TeamResponseDto addMember(UUID teamId, UUID userId, UUID targetUserId, Role role) {
        long perfStart = System.nanoTime();
        try {
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new DomainException("TEAM_NOT_FOUND",
                            "Team not found: " + teamId));

            TeamMember requester = teamRepository.findMember(teamId, userId)
                    .orElseThrow(() -> new DomainException("FORBIDDEN",
                            "You are not a member of this team"));

            if (requester.getRole() != Role.ADMIN) {
                throw new DomainException("FORBIDDEN", "Only ADMIN members can add new members");
            }

            userRepository.findById(targetUserId)
                    .orElseThrow(() -> new DomainException("USER_NOT_FOUND",
                            "User not found: " + targetUserId));

            if (teamRepository.findMember(teamId, targetUserId).isPresent()) {
                throw new DomainException("ALREADY_MEMBER",
                        "User is already a member of this team");
            }

            TeamMember newMember = new TeamMember(
                    null, teamId, targetUserId, role, Instant.now()
            );
            teamRepository.addMember(newMember);

            List<TeamMember> members = teamRepository.findMembersByTeamId(teamId);
            return toResponseDto(team, members);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"TeamUseCase.addMember", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"TeamUseCase.addMember", perfMs});
            }
        }
    }

    @Transactional
    public void removeMember(UUID teamId, UUID userId, UUID targetUserId) {
        long perfStart = System.nanoTime();
        try {
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new DomainException("TEAM_NOT_FOUND",
                            "Team not found: " + teamId));

            if (team.getOwnerUserId().equals(targetUserId)) {
                throw new DomainException("CANNOT_REMOVE_OWNER",
                        "Cannot remove the team owner");
            }

            boolean isSelfRemoval = userId.equals(targetUserId);
            if (!isSelfRemoval) {
                TeamMember requester = teamRepository.findMember(teamId, userId)
                        .orElseThrow(() -> new DomainException("FORBIDDEN",
                                "You are not a member of this team"));

                if (requester.getRole() != Role.ADMIN) {
                    throw new DomainException("FORBIDDEN",
                            "Only ADMIN members can remove other members");
                }
            }

            teamRepository.findMember(teamId, targetUserId)
                    .orElseThrow(() -> new DomainException("MEMBER_NOT_FOUND",
                            "User is not a member of this team"));

            teamRepository.removeMember(teamId, targetUserId);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"TeamUseCase.removeMember", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"TeamUseCase.removeMember", perfMs});
            }
        }
    }

    public List<TeamResponseDto> listTeams(UUID userId) {
        long perfStart = System.nanoTime();
        try {
            List<Team> teams = teamRepository.findByUserId(userId);

            if (teams.isEmpty()) {
                return List.of();
            }

            List<UUID> teamIds = teams.stream()
                    .map(Team::getId)
                    .toList();

            Map<UUID, List<TeamMember>> membersByTeamId = teamRepository.findMembersByTeamIds(teamIds);

            List<TeamMember> allMembers = membersByTeamId.values().stream()
                    .flatMap(List::stream)
                    .toList();

            Map<UUID, User> usersById = fetchUsersById(allMembers);

            return teams.stream()
                    .map(team -> {
                        List<TeamMember> members = membersByTeamId.getOrDefault(team.getId(), List.of());
                        return toResponseDto(team, members, usersById);
                    })
                    .toList();
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"TeamUseCase.listTeams", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"TeamUseCase.listTeams", perfMs});
            }
        }
    }

    public TeamResponseDto getTeam(UUID teamId, UUID userId) {
        long perfStart = System.nanoTime();
        try {
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new DomainException("TEAM_NOT_FOUND",
                            "Team not found: " + teamId));

            teamRepository.findMember(teamId, userId)
                    .orElseThrow(() -> new DomainException("FORBIDDEN",
                            "You are not a member of this team"));

            List<TeamMember> members = teamRepository.findMembersByTeamId(teamId);
            return toResponseDto(team, members);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"TeamUseCase.getTeam", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"TeamUseCase.getTeam", perfMs});
            }
        }
    }

    @Transactional
    public void deleteTeam(UUID teamId, UUID userId) {
        long perfStart = System.nanoTime();
        try {
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new DomainException("TEAM_NOT_FOUND",
                            "Team not found: " + teamId));

            if (!team.getOwnerUserId().equals(userId)) {
                throw new DomainException("FORBIDDEN", "Only the team owner can delete the team");
            }

            teamRepository.deleteById(teamId);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"TeamUseCase.deleteTeam", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"TeamUseCase.deleteTeam", perfMs});
            }
        }
    }

    private Map<UUID, User> fetchUsersById(List<TeamMember> members) {
        List<UUID> userIds = members.stream()
                .map(TeamMember::getUserId)
                .distinct()
                .toList();

        return userRepository.findAllByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private TeamResponseDto toResponseDto(Team team, List<TeamMember> members) {
        Map<UUID, User> usersById = fetchUsersById(members);
        return toResponseDto(team, members, usersById);
    }

    private TeamResponseDto toResponseDto(Team team, List<TeamMember> members, Map<UUID, User> usersById) {
        List<TeamMemberDto> memberDtos = members.stream()
                .map(member -> {
                    User user = usersById.get(member.getUserId());
                    String username = user != null ? user.getUsername() : "unknown";
                    return new TeamMemberDto(
                            member.getUserId(),
                            username,
                            member.getRole().name(),
                            member.getJoinedAt()
                    );
                })
                .toList();

        return new TeamResponseDto(
                team.getId(),
                team.getName(),
                team.getOwnerUserId(),
                memberDtos,
                team.getCreatedAt()
        );
    }
}
