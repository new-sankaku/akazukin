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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class TeamRepositoryImpl implements TeamRepository, PanacheRepository<TeamEntity> {

    private static final Logger LOG = Logger.getLogger(TeamRepositoryImpl.class.getName());

    @Inject
    EntityManager entityManager;

    @Override
    public Optional<Team> findById(UUID id) {
        long perfStart = System.nanoTime();
        try {
            return find("id", id).firstResultOptional().map(TeamMapper::toDomain);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"TeamRepositoryImpl.findById", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"TeamRepositoryImpl.findById", perfMs});
            }
        }
    }

    @Override
    public List<Team> findByUserId(UUID userId) {
        long perfStart = System.nanoTime();
        try {
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
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"TeamRepositoryImpl.findByUserId", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"TeamRepositoryImpl.findByUserId", perfMs});
            }
        }
    }

    @Override
    @Transactional
    public Team save(Team team) {
        long perfStart = System.nanoTime();
        try {
            TeamEntity entity = TeamMapper.toEntity(team);
            if (entity.id == null) {
                entity.id = UUID.randomUUID();
                persist(entity);
            } else {
                entity = getEntityManager().merge(entity);
            }
            return TeamMapper.toDomain(entity);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"TeamRepositoryImpl.save", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"TeamRepositoryImpl.save", perfMs});
            }
        }
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        long perfStart = System.nanoTime();
        try {
            entityManager.createQuery("DELETE FROM TeamMemberEntity m WHERE m.teamId = :teamId")
                    .setParameter("teamId", id)
                    .executeUpdate();
            delete("id", id);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"TeamRepositoryImpl.deleteById", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"TeamRepositoryImpl.deleteById", perfMs});
            }
        }
    }

    @Override
    @Transactional
    public TeamMember addMember(TeamMember member) {
        long perfStart = System.nanoTime();
        try {
            TeamMemberEntity entity = TeamMapper.memberToEntity(member);
            if (entity.id == null) {
                entity.id = UUID.randomUUID();
            }
            entityManager.persist(entity);
            return TeamMapper.memberToDomain(entity);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"TeamRepositoryImpl.addMember", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"TeamRepositoryImpl.addMember", perfMs});
            }
        }
    }

    @Override
    @Transactional
    public void removeMember(UUID teamId, UUID userId) {
        long perfStart = System.nanoTime();
        try {
            entityManager.createQuery(
                            "DELETE FROM TeamMemberEntity m WHERE m.teamId = :teamId AND m.userId = :userId")
                    .setParameter("teamId", teamId)
                    .setParameter("userId", userId)
                    .executeUpdate();
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"TeamRepositoryImpl.removeMember", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"TeamRepositoryImpl.removeMember", perfMs});
            }
        }
    }

    @Override
    public List<TeamMember> findMembersByTeamId(UUID teamId) {
        long perfStart = System.nanoTime();
        try {
            return entityManager
                    .createQuery("SELECT m FROM TeamMemberEntity m WHERE m.teamId = :teamId",
                            TeamMemberEntity.class)
                    .setParameter("teamId", teamId)
                    .getResultList()
                    .stream()
                    .map(TeamMapper::memberToDomain)
                    .collect(Collectors.toList());
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"TeamRepositoryImpl.findMembersByTeamId", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"TeamRepositoryImpl.findMembersByTeamId", perfMs});
            }
        }
    }

    @Override
    public Map<UUID, List<TeamMember>> findMembersByTeamIds(Collection<UUID> teamIds) {
        long perfStart = System.nanoTime();
        try {
            if (teamIds.isEmpty()) {
                return Map.of();
            }

            List<TeamMemberEntity> entities = entityManager
                    .createQuery("SELECT m FROM TeamMemberEntity m WHERE m.teamId IN :teamIds",
                            TeamMemberEntity.class)
                    .setParameter("teamIds", new ArrayList<>(teamIds))
                    .getResultList();

            return entities.stream()
                    .map(TeamMapper::memberToDomain)
                    .collect(Collectors.groupingBy(TeamMember::getTeamId));
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"TeamRepositoryImpl.findMembersByTeamIds", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"TeamRepositoryImpl.findMembersByTeamIds", perfMs});
            }
        }
    }

    @Override
    public Optional<TeamMember> findMember(UUID teamId, UUID userId) {
        long perfStart = System.nanoTime();
        try {
            return entityManager
                    .createQuery(
                            "SELECT m FROM TeamMemberEntity m WHERE m.teamId = :teamId AND m.userId = :userId",
                            TeamMemberEntity.class)
                    .setParameter("teamId", teamId)
                    .setParameter("userId", userId)
                    .getResultStream()
                    .findFirst()
                    .map(TeamMapper::memberToDomain);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"TeamRepositoryImpl.findMember", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"TeamRepositoryImpl.findMember", perfMs});
            }
        }
    }
}
