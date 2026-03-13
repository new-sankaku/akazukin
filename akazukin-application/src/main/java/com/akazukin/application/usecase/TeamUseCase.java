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


import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TeamUseCase {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    @Inject
    public TeamUseCase(TeamRepository teamRepository, UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    public TeamResponseDto createTeam(UUID userId, String name) {
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
    }

    public TeamResponseDto addMember(UUID teamId, UUID userId, UUID targetUserId, Role role) {
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
    }

    public void removeMember(UUID teamId, UUID userId, UUID targetUserId) {
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
    }

    public List<TeamResponseDto> listTeams(UUID userId) {
        List<Team> teams = teamRepository.findByUserId(userId);

        return teams.stream()
                .map(team -> {
                    List<TeamMember> members = teamRepository.findMembersByTeamId(team.getId());
                    return toResponseDto(team, members);
                })
                .toList();
    }

    public TeamResponseDto getTeam(UUID teamId, UUID userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new DomainException("TEAM_NOT_FOUND",
                        "Team not found: " + teamId));

        teamRepository.findMember(teamId, userId)
                .orElseThrow(() -> new DomainException("FORBIDDEN",
                        "You are not a member of this team"));

        List<TeamMember> members = teamRepository.findMembersByTeamId(teamId);
        return toResponseDto(team, members);
    }

    public void deleteTeam(UUID teamId, UUID userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new DomainException("TEAM_NOT_FOUND",
                        "Team not found: " + teamId));

        if (!team.getOwnerUserId().equals(userId)) {
            throw new DomainException("FORBIDDEN", "Only the team owner can delete the team");
        }

        teamRepository.deleteById(teamId);
    }

    private TeamResponseDto toResponseDto(Team team, List<TeamMember> members) {
        List<TeamMemberDto> memberDtos = members.stream()
                .map(member -> {
                    String username = userRepository.findById(member.getUserId())
                            .map(User::getUsername)
                            .orElse("unknown");
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
