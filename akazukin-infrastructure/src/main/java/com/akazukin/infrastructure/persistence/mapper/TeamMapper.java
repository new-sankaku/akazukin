package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.Role;
import com.akazukin.domain.model.Team;
import com.akazukin.domain.model.TeamMember;
import com.akazukin.infrastructure.persistence.entity.TeamEntity;
import com.akazukin.infrastructure.persistence.entity.TeamMemberEntity;

public final class TeamMapper {

    private TeamMapper() {}

    public static Team toDomain(TeamEntity entity) {
        return new Team(
                entity.id,
                entity.name,
                entity.ownerUserId,
                entity.createdAt,
                entity.updatedAt
        );
    }

    public static TeamEntity toEntity(Team domain) {
        TeamEntity entity = new TeamEntity();
        entity.id = domain.getId();
        entity.name = domain.getName();
        entity.ownerUserId = domain.getOwnerUserId();
        entity.createdAt = domain.getCreatedAt();
        entity.updatedAt = domain.getUpdatedAt();
        return entity;
    }

    public static TeamMember memberToDomain(TeamMemberEntity entity) {
        return new TeamMember(
                entity.id,
                entity.teamId,
                entity.userId,
                Role.valueOf(entity.role),
                entity.joinedAt
        );
    }

    public static TeamMemberEntity memberToEntity(TeamMember domain) {
        TeamMemberEntity entity = new TeamMemberEntity();
        entity.id = domain.getId();
        entity.teamId = domain.getTeamId();
        entity.userId = domain.getUserId();
        entity.role = domain.getRole().name();
        entity.joinedAt = domain.getJoinedAt();
        return entity;
    }
}
