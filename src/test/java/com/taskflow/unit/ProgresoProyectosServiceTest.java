package com.taskflow.unit;

import com.taskflow.dto.ProjectProgressResponse;
import com.taskflow.model.Project;
import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgresoProyectosServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService service;

    private Project p1 = new Project(1L, "Plataforma TaskFlow", "d", 1L, null);
    private Project p2 = new Project(2L, "App Móvil", "d", 1L, null);
    private Project p3 = new Project(3L, "Migración Legacy", "d", 1L, null);

    @Test
    void progreso_listaTodosLosProyectos_yLosOrdenaYCalculaPorcentaje() throws Exception {
        // repositorio devuelve en orden 3,1,2 (mezcla)
        when(projectRepository.findAll()).thenReturn(List.of(p3, p1, p2));

        // proyecto 1: 5 tareas, 1 DONE -> 20.0
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(
                tarea(1L, TaskStatus.TODO),
                tarea(2L, TaskStatus.TODO),
                tarea(3L, TaskStatus.TODO),
                tarea(4L, TaskStatus.TODO),
                tarea(5L, TaskStatus.DONE)
        ));

        // proyecto 2: 3 tareas, 1 DONE -> 33.3
        when(taskRepository.findByProjectId(2L)).thenReturn(List.of(
                tarea(6L, TaskStatus.TODO),
                tarea(7L, TaskStatus.DONE),
                tarea(8L, TaskStatus.TODO)
        ));

        // proyecto 3: sin tareas -> 0.0
        when(taskRepository.findByProjectId(3L)).thenReturn(List.of());

        List<com.taskflow.dto.ProjectProgressResponse> resultado = service.progresoPorProyecto();

        assertEquals(3, resultado.size());
        assertEquals(new ProjectProgressResponse(1L, "Plataforma TaskFlow", 5, 1, 20.0), resultado.get(0));
        assertEquals(new ProjectProgressResponse(2L, "App Móvil", 3, 1, 33.3), resultado.get(1));
        assertEquals(new ProjectProgressResponse(3L, "Migración Legacy", 0, 0, 0.0), resultado.get(2));
    }

    private Task tarea(Long id, TaskStatus status) throws Exception {
        return new Task(id, "Tarea " + id, "d", status, Priority.MED, 1L, null, null);
    }
}
