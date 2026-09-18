package com.taskflow.controller;

import com.taskflow.dto.ProjectProgressResponse;
import com.taskflow.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ReportController — endpoints de reportes agregados en la API.
 */
@RestController
@Tag(name = "Reports", description = "Informes agregados: progreso de proyectos y otros.")
public class ReportController {

    private final ProjectService projectService;

    public ReportController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * GET /reports/progress — devuelve una lista con el progreso de cada proyecto.
     */
    @Operation(summary = "Progreso de proyectos",
            description = "Lista para cada proyecto el total de tareas, cuántas están en DONE y el porcentaje completado (un decimal).")
    @GetMapping("/reports/progress")
    public List<ProjectProgressResponse> getProgress() {
        return projectService.progresoPorProyecto();
    }
}
