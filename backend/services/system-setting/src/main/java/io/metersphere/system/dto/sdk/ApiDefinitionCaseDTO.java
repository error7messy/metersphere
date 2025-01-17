package io.metersphere.system.dto.sdk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ApiDefinitionCaseDTO {

    @Schema(description = "message.domain.name")
    private String name;

    @Schema(description = "message.domain.protocol")
    private String protocol;

    @Schema(description = "message.domain.method")
    private String method;

    @Schema(description = "message.domain.path")
    private String path;

    @Schema(description = "message.domain.status")
    private String status;

    @Schema(description = "message.domain.description")
    private String description;

    @Schema(description = "message.domain.create_time")
    private Long createTime;

    @Schema(description = "message.domain.create_user")
    private String createUser;

    @Schema(description = "message.domain.update_time")
    private Long updateTime;

    @Schema(description = "message.domain.update_user")
    private String updateUser;

    @Schema(description = "message.domain.delete_user")
    private String deleteUser;

    @Schema(description = "message.domain.delete_time")
    private Long deleteTime;

    @Schema(description = "message.domain.case_name")
    private String caseName;

    @Schema(description = "message.domain.priority")
    private String priority;

    @Schema(description = "message.domain.case_status")
    private String caseStatus;

    @Schema(description = "message.domain.last_report_status")
    private String lastReportStatus;

    @Schema(description = "message.domain.principal")
    private String principal;

    @Schema(description = "message.domain.case_create_time")
    private Long caseCreateTime;

    @Schema(description = "message.domain.case_create_user")
    private String caseCreateUser;

    @Schema(description = "message.domain.case_update_time")
    private Long caseUpdateTime;

    @Schema(description = "message.domain.case_update_user")
    private String caseUpdateUser;

    @Schema(description = "message.domain.case_delete_time")
    private Long caseDeleteTime;

    @Schema(description = "message.domain.case_delete_user")
    private String caseDeleteUser;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "自定义字段的值")
    private List<OptionDTO> fields;

    @Schema(description = "message.domain.mock_name")
    private String mockName;

    @Schema(description = "message.domain.scenario_report_url")
    private String reportUrl;

    @Schema(description = "message.domain.scenario_report_share_url")
    private String shareUrl;

    @Schema(description = "message.domain.scenario_report_name")
    private String reportName;

    @Schema(description = "message.domain.scenario_report_start_time")
    private Long startTime;

    @Schema(description = "message.domain.scenario_report_end_time")
    private Long endTime;

    @Schema(description = "message.domain.scenario_report_request_duration")
    private Long requestDuration;

    @Schema(description = "message.domain.scenario_report_status")
    private String reportStatus;

    @Schema(description = "message.domain.scenario_report_environment")
    private String environment;

    @Schema(description = "message.domain.scenario_report_error_count")
    private Long errorCount;

    @Schema(description = "message.domain.scenario_report_fake_error_count")
    private Long fakeErrorCount;

    @Schema(description = "message.domain.scenario_report_pending_count")
    private Long pendingCount;

    @Schema(description = "message.domain.scenario_report_success_count")
    private Long successCount;

    @Schema(description = "message.domain.scenario_report_assertion_count")
    private Long assertionCount;

    @Schema(description = "message.domain.scenario_report_assertion_success_count")
    private Long assertionSuccessCount;

    @Schema(description = "message.domain.scenario_report_request_error_rate")
    private String requestErrorRate;

    @Schema(description = "message.domain.scenario_report_request_pending_rate")
    private String requestPendingRate;

    @Schema(description = "message.domain.scenario_report_request_fake_error_rate")
    private String requestFakeErrorRate;

    @Schema(description = "message.domain.scenario_report_request_pass_rate")
    private String requestPassRate;

    @Schema(description = "message.domain.scenario_report_assertion_pass_rate")
    private String assertionPassRate;

}
