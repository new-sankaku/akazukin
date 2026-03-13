package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.Team;
import com.akazukin.domain.model.TeamMember;
import com.akazukin.domain.port.TeamRepository;
import com.akazukin.infrastructure.persistence.entity.TeamEntity;
import com.akazukin.infrastructure.persistence.entity.TeamMemberEntity;
import com.akazukin.infrastructure.persistence.mapper.TeamMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class TeamRepositoryImpl implements TeamRepository, PanacheRepository<TeamEntity> {

    @Inject
    EntityManager entityManager;

    @Override
    public Optional<Team> findById(UUID id) {
        return find("id", id).firstResultOptional().map(TeamMapper::toDomain);
    }

    @Override
    public List<Team> findByUserId(UUID userId) {
        List<TeamMemberEntity> memberships = entityManager
                .createQuery("SELECT m FROM TeamMemberEntity m WHERE m.userId = :userId",
                        TeamMemberEntity.class)
                .setParameter("userId", userId)
                .getResultList();

        List<UUID> teamIds = memberships.stream()
                .map(m -> m.teamId)
                .toList();

        if (teamIds.isEmpty()) {
            return List.of();
        }

        return find("id IN ?1", teamIds)
                .list()
                .stream()
                .map(TeamMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Team save(Team team) {
        TeamEntity entity = TeamMapper.toEntity(team);
        if (entity.id != null && find("id", entity.id).firstResult() != null) {
            entity = getEntityManager().merge(entity);
        } else {
            persist(entity);
        }
        return TeamMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        entityManager.createQuery("DELETE FROM TeamMemberEntity m WHERE m.teamId = :teamId")
                .setParameter("teamId", id)
                .executeUpdate();
        delete("id", id);
    }

    @Override
    @Transactional
    public TeamMember addMember(TeamMember member) {
        TeamMemberEntity entity = TeamMapper.memberToEntity(member);
        entityManager.persist(entity);
        return TeamMapper.memberToDomain(entity);
    }

    @Override
    @Transactional
    public void removeMember(UUID teamId, UUID userId) {
        entityManager.createQuery(
                        "DELETE FROM TeamMemberEntity m WHERE m.teamId = :teamId AND m.userId = :userId")
                .setParameter("teamId", teamId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    @Override
    public List<TeamMember> findMembersByTeamId(UUID teamId) {
        return entityManager
                .createQuery("SELECT m FROM TeamMemberEntity m WHERE m.teamId = :teamId",
                        TeamMemberEntity.class)
                .setParameter("teamId", teamId)
                .getResultList()
                .stream()
                .map(TeamMapper::memberToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<TeamMember> findMember(UUID teamId, UUID userId) {
        return entityManager
                .createQuery(
                        "SELECT m FROM TeamMemberEntity m WHERE m.teamId = :teamId AND m.userId = :userId",
                        TeamMemberEntity.class)
                .setParameter("teamId", teamId)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst()
                .map(TeamMapper::memberToDomain);
    }
}
