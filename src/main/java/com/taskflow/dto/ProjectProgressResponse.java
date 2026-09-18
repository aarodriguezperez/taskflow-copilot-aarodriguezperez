package com.taskflow.dto;

/**
 * ProjectProgressResponse — contrato de salida de GET /reports/progress.
 * Contiene, por proyecto, el total de tareas, cuántas están en DONE y el porcentaje completado
 * redondeado a un decimal.
 */
public record ProjectProgressResponse(
        Long projectId,
        String projectName,
        long totalTasks,
        long doneTasks,
        double percentDone
) {
}
