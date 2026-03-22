package com.akazukin.application.usecase;

import com.akazukin.application.dto.AiReviewResultDto;
import com.akazukin.application.dto.ApprovalDashboardDto;
import com.akazukin.application.dto.ApprovalRuleDto;
import com.akazukin.application.dto.ApprovalRuleUpdateDto;
import com.akazukin.application.dto.RiskLevelFlowDto;
import com.akazukin.application.dto.RoleApprovalSettingDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.AgentTask;
import com.akazukin.domain.model.AgentType;
import com.akazukin.domain.model.ApprovalAction;
import com.akazukin.domain.model.ApprovalRequest;
import com.akazukin.domain.model.ApprovalRule;
import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.model.RiskLevelFlow;
import com.akazukin.domain.model.Role;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.Team;
import com.akazukin.domain.model.TeamMember;
import com.akazukin.domain.port.AgentOrchestrator;
import com.akazukin.domain.port.ApprovalRequestRepository;
import com.akazukin.domain.port.ApprovalRuleRepository;
import com.akazukin.domain.port.PostRepository;
import com.akazukin.domain.port.TeamRepository;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamApprovalUseCaseTest {

    private InMemoryTeamRepository teamRepository;
    private InMemoryApprovalRuleRepository approvalRuleRepository;
    private InMemoryApprovalRequestRepository approvalRequestRepository;
    private InMemoryPostRepository postRepository;
    private StubAgentOrchestrator agentOrchestrator;
    private TeamApprovalUseCase teamApprovalUseCase;

    private UUID userId;
    private UUID teamId;

    @BeforeEach
    void setUp() {
        teamRepository = new InMemoryTeamRepository();
        approvalRuleRepository = new InMemoryApprovalRuleRepository();
        approvalRequestRepository = new InMemoryApprovalRequestRepository();
        postRepository = new InMemoryPostRepository();
        agentOrchestrator = new StubAgentOrchestrator();

        teamApprovalUseCase = new TeamApprovalUseCase(
                teamRepository, approvalRuleRepository, approvalRequestRepository,
                postRepository, agentOrchestrator);

        userId = UUID.randomUUID();
        teamId = UUID.randomUUID();

        Instant now = Instant.now();
        Team team = new Team(teamId, "Test Team", userId, now, now);
        teamRepository.save(team);

        TeamMember member = new TeamMember(UUID.randomUUID(), teamId, userId, Role.ADMIN, now);
        teamRepository.addMember(member);
    }

    @Test
    void getApprovalRules_returnsRulesForTeamMember() {
        Instant now = Instant.now();
        ApprovalRule rule = new ApprovalRule(UUID.randomUUID(), teamId, Role.USER,
                true, false, true, true, false, 2, 24, now, now);
        approvalRuleRepository.save(rule);

        List<ApprovalRuleDto> rules = teamApprovalUseCase.getApprovalRules(teamId, userId);

        assertEquals(1, rules.size());
        assertEquals("USER", rules.get(0).role());
        assertTrue(rules.get(0).postApprovalRequired());
        assertFalse(rules.get(0).scheduleApprovalRequired());
        assertTrue(rules.get(0).mediaApprovalRequired());
    }

    @Test
    void getApprovalRules_returnsEmptyWhenNoRules() {
        List<ApprovalRuleDto> rules = teamApprovalUseCase.getApprovalRules(teamId, userId);

        assertTrue(rules.isEmpty());
    }

    @Test
    void getApprovalRules_throwsTeamNotFoundForNonExistentTeam() {
        UUID nonExistentTeamId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> teamApprovalUseCase.getApprovalRules(nonExistentTeamId, userId));
        assertEquals("TEAM_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void getApprovalRules_throwsForbiddenForNonMember() {
        UUID nonMemberId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> teamApprovalUseCase.getApprovalRules(teamId, nonMemberId));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void updateApprovalRules_savesRoleSettingsAndReturnsRules() {
        List<RoleApprovalSettingDto> roleSettings = List.of(
                new RoleApprovalSettingDto("USER", true, true, false),
                new RoleApprovalSettingDto("VIEWER", false, false, false));

        ApprovalRuleUpdateDto request = new ApprovalRuleUpdateDto(
                roleSettings, true, false, 2, 48, null);

        List<ApprovalRuleDto> result = teamApprovalUseCase.updateApprovalRules(teamId, userId, request);

        assertEquals(2, result.size());
    }

    @Test
    void updateApprovalRules_savesRiskLevelFlows() {
        List<RiskLevelFlowDto> riskFlows = List.of(
                new RiskLevelFlowDto("HIGH", 3, true, true),
                new RiskLevelFlowDto("LOW", 1, false, false));

        ApprovalRuleUpdateDto request = new ApprovalRuleUpdateDto(
                List.of(), false, false, 1, 24, riskFlows);

        teamApprovalUseCase.updateApprovalRules(teamId, userId, request);

        List<com.akazukin.domain.model.RiskLevelFlow> savedFlows =
                approvalRuleRepository.findRiskFlowsByTeamId(teamId);
        assertEquals(2, savedFlows.size());
    }

    @Test
    void updateApprovalRules_deletesExistingRulesBeforeSaving() {
        Instant now = Instant.now();
        approvalRuleRepository.save(new ApprovalRule(UUID.randomUUID(), teamId, Role.USER,
                true, true, true, true, true, 1, 12, now, now));

        List<RoleApprovalSettingDto> newSettings = List.of(
                new RoleApprovalSettingDto("ADMIN", false, false, false));
        ApprovalRuleUpdateDto request = new ApprovalRuleUpdateDto(
                newSettings, false, false, 1, 24, null);

        List<ApprovalRuleDto> result = teamApprovalUseCase.updateApprovalRules(teamId, userId, request);

        assertEquals(1, result.size());
        assertEquals("ADMIN", result.get(0).role());
    }

    @Test
    void updateApprovalRules_throwsTeamNotFoundForNonExistentTeam() {
        UUID nonExistentTeamId = UUID.randomUUID();
        ApprovalRuleUpdateDto request = new ApprovalRuleUpdateDto(
                List.of(), false, false, 1, 24, null);

        DomainException exception = assertThrows(DomainException.class,
                () -> teamApprovalUseCase.updateApprovalRules(nonExistentTeamId, userId, request));
        assertEquals("TEAM_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void updateApprovalRules_throwsForbiddenForNonAdminMember() {
        UUID viewerUserId = UUID.randomUUID();
        Instant now = Instant.now();
        TeamMember viewer = new TeamMember(UUID.randomUUID(), teamId, viewerUserId, Role.VIEWER, now);
        teamRepository.addMember(viewer);

        ApprovalRuleUpdateDto request = new ApprovalRuleUpdateDto(
                List.of(), false, false, 1, 24, null);

        DomainException exception = assertThrows(DomainException.class,
                () -> teamApprovalUseCase.updateApprovalRules(teamId, viewerUserId, request));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void updateApprovalRules_throwsForbiddenForNonMember() {
        UUID nonMemberId = UUID.randomUUID();
        ApprovalRuleUpdateDto request = new ApprovalRuleUpdateDto(
                List.of(), false, false, 1, 24, null);

        DomainException exception = assertThrows(DomainException.class,
                () -> teamApprovalUseCase.updateApprovalRules(teamId, nonMemberId, request));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void getDashboard_returnsZeroCountsWhenNoApprovals() {
        ApprovalDashboardDto dashboard = teamApprovalUseCase.getDashboard(userId);

        assertNotNull(dashboard);
        assertEquals(0, dashboard.pendingCount());
        assertEquals(0, dashboard.approvedTodayCount());
        assertEquals(0, dashboard.rejectedCount());
        assertEquals(0.0, dashboard.aiFailRate());
        assertTrue(dashboard.rejectionTrends().isEmpty());
        assertEquals(1, dashboard.teamStatuses().size());
    }

    @Test
    void getDashboard_countsPendingApprovals() {
        Instant now = Instant.now();
        approvalRequestRepository.save(new ApprovalRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), userId, teamId,
                null, null, now, null));
        approvalRequestRepository.save(new ApprovalRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), userId, teamId,
                null, null, now, null));

        ApprovalDashboardDto dashboard = teamApprovalUseCase.getDashboard(userId);

        assertEquals(2, dashboard.pendingCount());
    }

    @Test
    void getDashboard_calculatesAiFailRateFromRejections() {
        Instant now = Instant.now();
        approvalRequestRepository.save(new ApprovalRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), userId, teamId,
                ApprovalAction.APPROVE, null, now, now));
        approvalRequestRepository.save(new ApprovalRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), userId, teamId,
                ApprovalAction.REJECT, "bad content", now, now));

        ApprovalDashboardDto dashboard = teamApprovalUseCase.getDashboard(userId);

        assertEquals(1, dashboard.rejectedCount());
        assertTrue(dashboard.aiFailRate() > 0);
    }

    @Test
    void getDashboard_returnsEmptyForUserWithNoTeams() {
        UUID noTeamUserId = UUID.randomUUID();

        ApprovalDashboardDto dashboard = teamApprovalUseCase.getDashboard(noTeamUserId);

        assertEquals(0, dashboard.pendingCount());
        assertTrue(dashboard.teamStatuses().isEmpty());
    }

    @Test
    void getDashboard_aggregatesRejectionTrends() {
        Instant now = Instant.now();
        approvalRequestRepository.save(new ApprovalRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), userId, teamId,
                ApprovalAction.REJECT, "content too long", now, now));
        approvalRequestRepository.save(new ApprovalRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), userId, teamId,
                ApprovalAction.REJECT, "content too long", now, now));
        approvalRequestRepository.save(new ApprovalRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), userId, teamId,
                ApprovalAction.REJECT, "inappropriate", now, now));

        ApprovalDashboardDto dashboard = teamApprovalUseCase.getDashboard(userId);

        assertFalse(dashboard.rejectionTrends().isEmpty());
        assertEquals(3, dashboard.rejectedCount());
    }

    @Test
    void getAiReview_returnsReviewResultForApproval() {
        Instant now = Instant.now();
        UUID postId = UUID.randomUUID();
        UUID approvalId = UUID.randomUUID();

        postRepository.save(new Post(postId, userId, "Test post content", List.of(),
                PostStatus.PENDING_APPROVAL, null, now, now));
        approvalRequestRepository.save(new ApprovalRequest(
                approvalId, postId, userId, UUID.randomUUID(), teamId,
                null, null, now, null));

        agentOrchestrator.setOutput("");

        AiReviewResultDto result = teamApprovalUseCase.getAiReview(approvalId, userId);

        assertNotNull(result);
        assertEquals(100, result.score());
        assertEquals("pass", result.verdict());
        assertTrue(result.findings().isEmpty());
    }

    @Test
    void getAiReview_returnsLowScoreForCriticalFindings() {
        Instant now = Instant.now();
        UUID postId = UUID.randomUUID();
        UUID approvalId = UUID.randomUUID();

        postRepository.save(new Post(postId, userId, "Test post", List.of(),
                PostStatus.PENDING_APPROVAL, null, now, now));
        approvalRequestRepository.save(new ApprovalRequest(
                approvalId, postId, userId, UUID.randomUUID(), teamId,
                null, null, now, null));

        agentOrchestrator.setOutput("critical|Copyright Issue|Potential copyright violation|著作権法|Case123");

        AiReviewResultDto result = teamApprovalUseCase.getAiReview(approvalId, userId);

        assertNotNull(result);
        assertTrue(result.score() < 100);
        assertFalse(result.findings().isEmpty());
    }

    @Test
    void getAiReview_throwsApprovalNotFoundForNonExistentApproval() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> teamApprovalUseCase.getAiReview(nonExistentId, userId));
        assertEquals("APPROVAL_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void recheckAi_returnsNewReviewResult() {
        Instant now = Instant.now();
        UUID postId = UUID.randomUUID();
        UUID approvalId = UUID.randomUUID();

        postRepository.save(new Post(postId, userId, "Review content", List.of(),
                PostStatus.PENDING_APPROVAL, null, now, now));
        approvalRequestRepository.save(new ApprovalRequest(
                approvalId, postId, userId, UUID.randomUUID(), teamId,
                null, null, now, null));

        agentOrchestrator.setOutput("warning|Tone Issue|Content tone may be inappropriate|社内規定|");

        AiReviewResultDto result = teamApprovalUseCase.recheckAi(approvalId, userId);

        assertNotNull(result);
        assertTrue(result.score() < 100);
    }

    @Test
    void recheckAi_throwsApprovalNotFoundForNonExistentApproval() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> teamApprovalUseCase.recheckAi(nonExistentId, userId));
        assertEquals("APPROVAL_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void getAiReview_handlesPostNotFoundGracefully() {
        Instant now = Instant.now();
        UUID approvalId = UUID.randomUUID();
        UUID orphanPostId = UUID.randomUUID();

        approvalRequestRepository.save(new ApprovalRequest(
                approvalId, orphanPostId, userId, UUID.randomUUID(), teamId,
                null, null, now, null));

        agentOrchestrator.setOutput("");

        AiReviewResultDto result = teamApprovalUseCase.getAiReview(approvalId, userId);

        assertNotNull(result);
        assertEquals(100, result.score());
    }

    @Test
    void updateApprovalRules_handlesNullRoleSettings() {
        ApprovalRuleUpdateDto request = new ApprovalRuleUpdateDto(
                null, false, false, 1, 24, null);

        List<ApprovalRuleDto> result = teamApprovalUseCase.updateApprovalRules(teamId, userId, request);

        assertTrue(result.isEmpty());
    }

    @Test
    void updateApprovalRules_handlesNullRiskLevelFlows() {
        List<RoleApprovalSettingDto> settings = List.of(
                new RoleApprovalSettingDto("USER", true, false, false));

        ApprovalRuleUpdateDto request = new ApprovalRuleUpdateDto(
                settings, true, true, 1, 24, null);

        List<ApprovalRuleDto> result = teamApprovalUseCase.updateApprovalRules(teamId, userId, request);

        assertEquals(1, result.size());
        assertTrue(result.get(0).aiCheckRequired());
        assertTrue(result.get(0).aiAutoReject());
    }

    private static class InMemoryTeamRepository implements TeamRepository {

        private final Map<UUID, Team> teamStore = new HashMap<>();
        private final Map<UUID, List<TeamMember>> memberStore = new HashMap<>();

        @Override
        public Optional<Team> findById(UUID id) {
            return Optional.ofNullable(teamStore.get(id));
        }

        @Override
        public List<Team> findByUserId(UUID userId) {
            List<Team> result = new ArrayList<>();
            for (Map.Entry<UUID, List<TeamMember>> entry : memberStore.entrySet()) {
                boolean isMember = entry.getValue().stream()
                        .anyMatch(m -> m.getUserId().equals(userId));
                if (isMember) {
                    Team team = teamStore.get(entry.getKey());
                    if (team != null) {
                        result.add(team);
                    }
                }
            }
            return result;
        }

        @Override
        public Team save(Team team) {
            teamStore.put(team.getId(), team);
            return team;
        }

        @Override
        public void deleteById(UUID id) {
            teamStore.remove(id);
            memberStore.remove(id);
        }

        @Override
        public TeamMember addMember(TeamMember member) {
            memberStore.computeIfAbsent(member.getTeamId(), k -> new ArrayList<>()).add(member);
            return member;
        }

        @Override
        public void removeMember(UUID teamId, UUID userId) {
            List<TeamMember> members = memberStore.get(teamId);
            if (members != null) {
                members.removeIf(m -> m.getUserId().equals(userId));
            }
        }

        @Override
        public List<TeamMember> findMembersByTeamId(UUID teamId) {
            return memberStore.getOrDefault(teamId, List.of());
        }

        @Override
        public Map<UUID, List<TeamMember>> findMembersByTeamIds(Collection<UUID> teamIds) {
            Map<UUID, List<TeamMember>> result = new HashMap<>();
            for (UUID id : teamIds) {
                result.put(id, memberStore.getOrDefault(id, List.of()));
            }
            return result;
        }

        @Override
        public Optional<TeamMember> findMember(UUID teamId, UUID userId) {
            return memberStore.getOrDefault(teamId, List.of()).stream()
                    .filter(m -> m.getUserId().equals(userId))
                    .findFirst();
        }
    }

    private static class InMemoryApprovalRuleRepository implements ApprovalRuleRepository {

        private final Map<UUID, ApprovalRule> ruleStore = new HashMap<>();
        private final Map<UUID, List<RiskLevelFlow>> flowStore = new HashMap<>();

        @Override
        public List<ApprovalRule> findByTeamId(UUID teamId) {
            return ruleStore.values().stream()
                    .filter(r -> r.getTeamId().equals(teamId))
                    .toList();
        }

        @Override
        public ApprovalRule save(ApprovalRule rule) {
            ruleStore.put(rule.getId(), rule);
            return rule;
        }

        @Override
        public void deleteByTeamId(UUID teamId) {
            List<UUID> toRemove = ruleStore.values().stream()
                    .filter(r -> r.getTeamId().equals(teamId))
                    .map(ApprovalRule::getId)
                    .toList();
            toRemove.forEach(ruleStore::remove);
        }

        @Override
        public List<RiskLevelFlow> findRiskFlowsByTeamId(UUID teamId) {
            return flowStore.getOrDefault(teamId, List.of());
        }

        @Override
        public RiskLevelFlow saveRiskFlow(RiskLevelFlow flow) {
            flowStore.computeIfAbsent(flow.getTeamId(), k -> new ArrayList<>()).add(flow);
            return flow;
        }

        @Override
        public void deleteRiskFlowsByTeamId(UUID teamId) {
            flowStore.remove(teamId);
        }
    }

    private static class InMemoryApprovalRequestRepository implements ApprovalRequestRepository {

        private final Map<UUID, ApprovalRequest> store = new HashMap<>();

        @Override
        public Optional<ApprovalRequest> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<ApprovalRequest> findByPostId(UUID postId) {
            return store.values().stream()
                    .filter(r -> r.getPostId().equals(postId))
                    .findFirst();
        }

        @Override
        public List<ApprovalRequest> findPendingByApproverId(UUID approverId, int offset, int limit) {
            List<ApprovalRequest> pending = store.values().stream()
                    .filter(r -> r.getApproverId().equals(approverId))
                    .filter(r -> r.getStatus() == null)
                    .toList();
            int end = Math.min(offset + limit, pending.size());
            if (offset >= pending.size()) return List.of();
            return new ArrayList<>(pending.subList(offset, end));
        }

        @Override
        public List<ApprovalRequest> findPendingByTeamId(UUID teamId, int offset, int limit) {
            List<ApprovalRequest> pending = store.values().stream()
                    .filter(r -> r.getTeamId().equals(teamId))
                    .filter(r -> r.getStatus() == null)
                    .toList();
            int end = Math.min(offset + limit, pending.size());
            if (offset >= pending.size()) return List.of();
            return new ArrayList<>(pending.subList(offset, end));
        }

        @Override
        public ApprovalRequest save(ApprovalRequest approvalRequest) {
            store.put(approvalRequest.getId(), approvalRequest);
            return approvalRequest;
        }

        @Override
        public long countPendingByApproverId(UUID approverId) {
            return store.values().stream()
                    .filter(r -> r.getApproverId().equals(approverId))
                    .filter(r -> r.getStatus() == null)
                    .count();
        }

        @Override
        public long countByTeamIdAndStatus(UUID teamId, ApprovalAction status) {
            return store.values().stream()
                    .filter(r -> r.getTeamId().equals(teamId))
                    .filter(r -> r.getStatus() == status)
                    .count();
        }

        @Override
        public long countByTeamIdAndStatusAndDecidedAfter(UUID teamId, ApprovalAction status, Instant after) {
            return store.values().stream()
                    .filter(r -> r.getTeamId().equals(teamId))
                    .filter(r -> r.getStatus() == status)
                    .filter(r -> r.getDecidedAt() != null && r.getDecidedAt().isAfter(after))
                    .count();
        }

        @Override
        public List<ApprovalRequest> findByTeamIdAndStatus(UUID teamId, ApprovalAction status) {
            return store.values().stream()
                    .filter(r -> r.getTeamId().equals(teamId))
                    .filter(r -> r.getStatus() == status)
                    .toList();
        }
    }

    private static class InMemoryPostRepository implements PostRepository {

        private final Map<UUID, Post> store = new HashMap<>();

        @Override
        public Optional<Post> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Post> findByUserId(UUID userId, int offset, int limit) {
            return List.of();
        }

        @Override
        public List<Post> findScheduledBefore(Instant before) {
            return List.of();
        }

        @Override
        public Post save(Post post) {
            store.put(post.getId(), post);
            return post;
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }

        @Override
        public long countByUserId(UUID userId) {
            return 0;
        }

        @Override
        public long countByUserIdAndStatus(UUID userId, PostStatus status) {
            return 0;
        }

        @Override
        public Map<SnsPlatform, Long> countByUserIdGroupByPlatform(UUID userId) {
            return Map.of();
        }
    }

    private static class StubAgentOrchestrator implements AgentOrchestrator {

        private String output = "";

        void setOutput(String output) {
            this.output = output;
        }

        @Override
        public AgentTask submitTask(UUID userId, AgentType agentType, String input) {
            return new AgentTask(UUID.randomUUID(), userId, agentType, input, output,
                    "COMPLETED", null, Instant.now(), Instant.now());
        }

        @Override
        public AgentTask submitTask(UUID userId, AgentType agentType, String input, UUID parentTaskId) {
            return new AgentTask(UUID.randomUUID(), userId, agentType, input, output,
                    "COMPLETED", parentTaskId, Instant.now(), Instant.now());
        }

        @Override
        public AgentTask getTaskResult(UUID taskId) {
            return null;
        }
    }
}
