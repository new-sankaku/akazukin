package com.akazukin.application.usecase;

import com.akazukin.application.dto.AiReviewFindingDto;
import com.akazukin.application.dto.AiReviewResultDto;
import com.akazukin.application.dto.ApprovalDashboardDto;
import com.akazukin.application.dto.ApprovalRuleDto;
import com.akazukin.application.dto.ApprovalRuleUpdateDto;
import com.akazukin.application.dto.RejectionTrendDto;
import com.akazukin.application.dto.RiskLevelFlowDto;
import com.akazukin.application.dto.RoleApprovalSettingDto;
import com.akazukin.application.dto.TeamApprovalStatusDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.AgentType;
import com.akazukin.domain.model.ApprovalAction;
import com.akazukin.domain.model.ApprovalRequest;
import com.akazukin.domain.model.ApprovalRule;
import com.akazukin.domain.model.RiskLevel;
import com.akazukin.domain.model.RiskLevelFlow;
import com.akazukin.domain.model.Role;
import com.akazukin.domain.model.Team;
import com.akazukin.domain.model.TeamMember;
import com.akazukin.domain.port.AgentOrchestrator;
import com.akazukin.domain.port.ApprovalRequestRepository;
import com.akazukin.domain.port.ApprovalRuleRepository;
import com.akazukin.domain.port.PostRepository;
import com.akazukin.domain.port.TeamRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class TeamApprovalUseCase {

    private static final Logger LOG = Logger.getLogger(TeamApprovalUseCase.class.getName());

    private final TeamRepository teamRepository;
    private final ApprovalRuleRepository approvalRuleRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final PostRepository postRepository;
    private final AgentOrchestrator agentOrchestrator;

    @Inject
    public TeamApprovalUseCase(TeamRepository teamRepository,
                               ApprovalRuleRepository approvalRuleRepository,
                               ApprovalRequestRepository approvalRequestRepository,
                               PostRepository postRepository,
                               AgentOrchestrator agentOrchestrator) {
        this.teamRepository = teamRepository;
        this.approvalRuleRepository = approvalRuleRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.postRepository = postRepository;
        this.agentOrchestrator = agentOrchestrator;
    }

    public List<ApprovalRuleDto> getApprovalRules(UUID teamId, UUID userId) {
        long perfStart = System.nanoTime();
        try {
            validateTeamAccess(teamId, userId);
            List<ApprovalRule> rules = approvalRuleRepository.findByTeamId(teamId);
            return rules.stream().map(this::toApprovalRuleDto).toList();
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"TeamApprovalUseCase.getApprovalRules", perfMs});
            }
        }
    }

    @Transactional
    public List<ApprovalRuleDto> updateApprovalRules(UUID teamId, UUID userId, ApprovalRuleUpdateDto request) {
        long perfStart = System.nanoTime();
        try {
            validateTeamAdminAccess(teamId, userId);

            approvalRuleRepository.deleteByTeamId(teamId);
            approvalRuleRepository.deleteRiskFlowsByTeamId(teamId);

            Instant now = Instant.now();
            List<ApprovalRule> savedRules = new ArrayList<>();

            if (request.roleSettings() != null) {
                for (RoleApprovalSettingDto setting : request.roleSettings()) {
                    ApprovalRule rule = new ApprovalRule(
                            UUID.randomUUID(), teamId, Role.valueOf(setting.role()),
                            setting.postApprovalRequired(),
                            setting.scheduleApprovalRequired(),
                            setting.mediaApprovalRequired(),
                            request.aiCheckRequired(),
                            request.aiAutoReject(),
                            request.minApprovers(),
                            request.approvalDeadlineHours(),
                            now, now
                    );
                    savedRules.add(approvalRuleRepository.save(rule));
                }
            }

            if (request.riskLevelFlows() != null) {
                for (RiskLevelFlowDto flowDto : request.riskLevelFlows()) {
                    RiskLevelFlow flow = new RiskLevelFlow(
                            UUID.randomUUID(), teamId,
                            RiskLevel.valueOf(flowDto.riskLevel()),
                            flowDto.requiredApprovers(),
                            flowDto.adminRequired(),
                            flowDto.legalReviewRequired()
                    );
                    approvalRuleRepository.saveRiskFlow(flow);
                }
            }

            return savedRules.stream().map(this::toApprovalRuleDto).toList();
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"TeamApprovalUseCase.updateApprovalRules", perfMs});
            }
        }
    }

    public ApprovalDashboardDto getDashboard(UUID userId) {
        long perfStart = System.nanoTime();
        try {
            List<Team> teams = teamRepository.findByUserId(userId);
            Instant todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();

            long pendingCount = 0;
            long approvedTodayCount = 0;
            long rejectedCount = 0;
            Map<String, Integer> rejectionReasonCounts = new LinkedHashMap<>();

            for (Team team : teams) {
                List<ApprovalRequest> pending = approvalRequestRepository.findPendingByTeamId(team.getId(), 0, 1000);
                pendingCount += pending.size();
                approvedTodayCount += approvalRequestRepository.countByTeamIdAndStatusAndDecidedAfter(
                        team.getId(), ApprovalAction.APPROVE, todayStart);
                rejectedCount += approvalRequestRepository.countByTeamIdAndStatus(
                        team.getId(), ApprovalAction.REJECT);

                List<ApprovalRequest> rejected = approvalRequestRepository.findByTeamIdAndStatus(
                        team.getId(), ApprovalAction.REJECT);
                for (ApprovalRequest req : rejected) {
                    String reason = (req.getComment() != null && !req.getComment().isBlank())
                            ? req.getComment().trim() : "other";
                    rejectionReasonCounts.merge(reason, 1, Integer::sum);
                }
            }

            long totalDecided = approvedTodayCount + rejectedCount;
            double aiFailRate = totalDecided > 0 ? (double) rejectedCount / totalDecided * 100 : 0;

            int totalRejections = rejectionReasonCounts.values().stream().mapToInt(Integer::intValue).sum();
            List<RejectionTrendDto> trends = rejectionReasonCounts.entrySet().stream()
                    .map(e -> new RejectionTrendDto(e.getKey(),
                            totalRejections > 0 ? (int) Math.round((double) e.getValue() / totalRejections * 100) : 0))
                    .sorted((a, b) -> Integer.compare(b.percentage(), a.percentage()))
                    .toList();

            List<TeamApprovalStatusDto> teamStatuses = new ArrayList<>();
            for (Team team : teams) {
                List<ApprovalRequest> teamPending = approvalRequestRepository.findPendingByTeamId(team.getId(), 0, 1000);
                long teamApproved = approvalRequestRepository.countByTeamIdAndStatus(
                        team.getId(), ApprovalAction.APPROVE);
                long teamRejected = approvalRequestRepository.countByTeamIdAndStatus(
                        team.getId(), ApprovalAction.REJECT);
                long teamDecided = teamApproved + teamRejected;
                double teamFailRate = teamDecided > 0 ? (double) teamRejected / teamDecided * 100 : 0;
                teamStatuses.add(new TeamApprovalStatusDto(
                        team.getId(), team.getName(),
                        teamPending.size(), teamApproved, teamRejected, teamFailRate));
            }

            return new ApprovalDashboardDto(
                    pendingCount, approvedTodayCount, rejectedCount,
                    aiFailRate, trends, teamStatuses);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"TeamApprovalUseCase.getDashboard", perfMs});
            }
        }
    }

    public AiReviewResultDto getAiReview(UUID approvalId, UUID userId) {
        long perfStart = System.nanoTime();
        try {
            ApprovalRequest approval = approvalRequestRepository.findById(approvalId)
                    .orElseThrow(() -> new DomainException("APPROVAL_NOT_FOUND",
                            "Approval request not found: " + approvalId));

            String postContent = postRepository.findById(approval.getPostId())
                    .map(p -> p.getContent())
                    .orElse("");

            return runAiReview(userId, postContent);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"TeamApprovalUseCase.getAiReview", perfMs});
            }
        }
    }

    public AiReviewResultDto recheckAi(UUID approvalId, UUID userId) {
        long perfStart = System.nanoTime();
        try {
            ApprovalRequest approval = approvalRequestRepository.findById(approvalId)
                    .orElseThrow(() -> new DomainException("APPROVAL_NOT_FOUND",
                            "Approval request not found: " + approvalId));

            String postContent = postRepository.findById(approval.getPostId())
                    .map(p -> p.getContent())
                    .orElse("");

            return runAiReview(userId, postContent);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"TeamApprovalUseCase.recheckAi", perfMs});
            }
        }
    }

    private AiReviewResultDto runAiReview(UUID userId, String postContent) {
        var sentinelTask = agentOrchestrator.submitTask(userId, AgentType.SENTINEL,
                "ai-review:" + postContent);
        var complianceTask = agentOrchestrator.submitTask(userId, AgentType.COMPLIANCE,
                "ai-review:" + postContent);

        String sentinelOutput = sentinelTask.getOutput() != null ? sentinelTask.getOutput() : "";
        String complianceOutput = complianceTask.getOutput() != null ? complianceTask.getOutput() : "";

        List<AiReviewFindingDto> findings = parseFindings(sentinelOutput + "\n" + complianceOutput);

        int score = 100;
        for (AiReviewFindingDto finding : findings) {
            if ("critical".equals(finding.severity())) {
                score -= 30;
            } else if ("warning".equals(finding.severity())) {
                score -= 15;
            } else {
                score -= 5;
            }
        }
        score = Math.max(0, score);

        String verdict;
        if (score >= 80) {
            verdict = "pass";
        } else if (score >= 50) {
            verdict = "warn";
        } else {
            verdict = "fail";
        }

        return new AiReviewResultDto(score, verdict, findings);
    }

    private List<AiReviewFindingDto> parseFindings(String output) {
        List<AiReviewFindingDto> findings = new ArrayList<>();
        String[] lines = output.split("\n");
        for (String line : lines) {
            String[] parts = line.split("\\|");
            if (parts.length >= 3) {
                List<String> laws = new ArrayList<>();
                if (parts.length >= 4) {
                    String[] lawParts = parts[3].trim().split(",");
                    for (String law : lawParts) {
                        if (!law.isBlank()) {
                            laws.add(law.trim());
                        }
                    }
                }
                String pastCase = parts.length >= 5 ? parts[4].trim() : "";
                findings.add(new AiReviewFindingDto(
                        parts[0].trim(), parts[1].trim(), parts[2].trim(), laws, pastCase));
            }
        }
        return findings;
    }

    private void validateTeamAccess(UUID teamId, UUID userId) {
        teamRepository.findById(teamId)
                .orElseThrow(() -> new DomainException("TEAM_NOT_FOUND", "Team not found: " + teamId));
        teamRepository.findMember(teamId, userId)
                .orElseThrow(() -> new DomainException("FORBIDDEN", "You are not a member of this team"));
    }

    private void validateTeamAdminAccess(UUID teamId, UUID userId) {
        teamRepository.findById(teamId)
                .orElseThrow(() -> new DomainException("TEAM_NOT_FOUND", "Team not found: " + teamId));
        TeamMember member = teamRepository.findMember(teamId, userId)
                .orElseThrow(() -> new DomainException("FORBIDDEN", "You are not a member of this team"));
        if (member.getRole() != Role.ADMIN) {
            throw new DomainException("FORBIDDEN", "Only ADMIN members can modify approval rules");
        }
    }

    private ApprovalRuleDto toApprovalRuleDto(ApprovalRule rule) {
        return new ApprovalRuleDto(
                rule.getId(), rule.getTeamId(), rule.getRole().name(),
                rule.isPostApprovalRequired(), rule.isScheduleApprovalRequired(),
                rule.isMediaApprovalRequired(), rule.isAiCheckRequired(),
                rule.isAiAutoReject(), rule.getMinApprovers(), rule.getApprovalDeadlineHours());
    }
}
