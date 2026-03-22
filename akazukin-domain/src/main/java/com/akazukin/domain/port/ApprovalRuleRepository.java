package com.akazukin.domain.port;

import com.akazukin.domain.model.ApprovalRule;
import com.akazukin.domain.model.RiskLevelFlow;

import java.util.List;
import java.util.UUID;

public interface ApprovalRuleRepository {

    List<ApprovalRule> findByTeamId(UUID teamId);

    ApprovalRule save(ApprovalRule rule);

    void deleteByTeamId(UUID teamId);

    List<RiskLevelFlow> findRiskFlowsByTeamId(UUID teamId);

    RiskLevelFlow saveRiskFlow(RiskLevelFlow flow);

    void deleteRiskFlowsByTeamId(UUID teamId);
}
