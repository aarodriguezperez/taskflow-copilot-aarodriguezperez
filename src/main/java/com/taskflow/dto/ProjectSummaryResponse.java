package com.taskflow.dto;

import java.util.Map;

/**
 * ProjectSummaryResponse — contrato de salida de GET /projects/{id}/summary.
 */
public record ProjectSummaryResponse(
        Long projectId,
        String projectName,
        long totalTasks,
        Map<String, Long> byStatus,
        long overdue
) {
}
