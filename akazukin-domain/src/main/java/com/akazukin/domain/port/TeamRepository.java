package com.akazukin.domain.port;

import com.akazukin.domain.model.Team;
import com.akazukin.domain.model.TeamMember;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository {

    Optional<Team> findById(UUID id);

    List<Team> findByUserId(UUID userId);

    Team save(Team team);

    void deleteById(UUID id);

    TeamMember addMember(TeamMember member);

    void removeMember(UUID teamId, UUID userId);

    List<TeamMember> findMembersByTeamId(UUID teamId);

    Map<UUID, List<TeamMember>> findMembersByTeamIds(Collection<UUID> teamIds);

    Optional<TeamMember> findMember(UUID teamId, UUID userId);
}
