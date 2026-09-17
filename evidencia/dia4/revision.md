# Copilot CLI Session

> [!NOTE]
> - **Session ID:** `0434f62e-0238-49fd-8e74-cb1558973d3f`  
> - **Started:** 17/9/2026, 12:22:55 p.m.  
> - **Duration:** 1m 38s  
> - **Exported:** 17/9/2026, 12:24:33 p.m.  

---

<sub>1s</sub>

### User

Revisa evidencia/dia4/summary.diff contra la especificación specs/summary.md.

---

<sub>8s</sub>

### Copilot

Abriré en paralelo el diff (evidencia/dia4/summary.diff) y la especificación (specs/summary.md) para comparar alcance, contrato y tests según la checklist de revisor. Leer ambos archivos ayuda a revisar cada cambio en contexto. Ejecutando lectura de ambos archivos ahora.

---

<sub>8s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-aarodriguezperez\evidencia\dia4\summary.diff.**

C:\Users\User\taskflow-copilot-aarodriguezperez\evidencia\dia4\summary.diff

<details>
<summary>242 lines</summary>

```diff
diff --git a/src/main/java/com/taskflow/controller/ProjectController.java b/src/main/java/com/taskflow/controller/ProjectController.java
index 56ccccf..85174eb 100644
--- a/src/main/java/com/taskflow/controller/ProjectController.java
+++ b/src/main/java/com/taskflow/controller/ProjectController.java
@@ -2,6 +2,7 @@ package com.taskflow.controller;
 
 import com.taskflow.dto.ProjectRequest;
 import com.taskflow.dto.ProjectResponse;
+import com.taskflow.dto.ProjectSummaryResponse;
 import com.taskflow.dto.TaskResponse;
 import com.taskflow.exception.ProjectNotFoundException;
 import com.taskflow.mapper.ProjectMapper;
@@ -131,4 +132,15 @@ public class ProjectController {
         projectService.eliminar(id);
         return ResponseEntity.noContent().build();
     }
+
+    /** GET /projects/{id}/summary — resumen del proyecto (conteos por estado y vencidas). */
+    @Operation(summary = "Resumen de un proyecto",
+            description = "Devuelve el número de tareas por estado y cuántas están vencidas. 404 si el proyecto no existe.")
+    @GetMapping("/projects/{id}/summary")
+    public ProjectSummaryResponse getSummary(@PathVariable("id") Long id) {
+        Project proyecto = projectService.buscarPorId(id)
+                .orElseThrow(() -> new ProjectNotFoundException(id));
+        return projectService.resumen(proyecto);
+    }
 }
+
diff --git a/src/main/java/com/taskflow/dto/ProjectSummaryResponse.java b/src/main/java/com/taskflow/dto/ProjectSummaryResponse.java
new file mode 100644
index 0000000..eaa6c6e
--- /dev/null
+++ b/src/main/java/com/taskflow/dto/ProjectSummaryResponse.java
@@ -0,0 +1,15 @@
+package com.taskflow.dto;
+
+import java.util.Map;
+
+/**
+ * ProjectSummaryResponse — contrato de salida de GET /projects/{id}/summary.
+ */
+public record ProjectSummaryResponse(
+        Long projectId,
+        String projectName,
+        long totalTasks,
+        Map<String, Long> byStatus,
+        long overdue
+) {
+}
diff --git a/src/main/java/com/taskflow/mapper/ProjectMapper.java b/src/main/java/com/taskflow/mapper/ProjectMapper.java
index a33a95b..2d68b6a 100644
--- a/src/main/java/com/taskflow/mapper/ProjectMapper.java
+++ b/src/main/java/com/taskflow/mapper/ProjectMapper.java
@@ -22,4 +22,11 @@ public final class ProjectMapper {
         return new ProjectResponse(p.getId(), p.getName(), p.getDescription(),
                 p.getOwnerId(), p.getCreatedAt());
     }
+
+    /** DTO de salida de GET /projects/{id}/summary. */
+    public static com.taskflow.dto.ProjectSummaryResponse aSummary(Long projectId, String projectName,
+                                                  long totalTasks, java.util.Map<String, Long> byStatus,
+                                                  long overdue) {
+        return new com.taskflow.dto.ProjectSummaryResponse(projectId, projectName, totalTasks, byStatus, overdue);
+    }
 }
diff --git a/src/main/java/com/taskflow/service/ProjectService.java b/src/main/java/com/taskflow/service/ProjectService.java
index d315e3f..d6b901b 100644
--- a/src/main/java/com/taskflow/service/ProjectService.java
+++ b/src/main/java/com/taskflow/service/ProjectService.java
@@ -13,7 +13,10 @@ import org.springframework.stereotype.Service;
 
 import java.time.LocalDate;
 import java.util.List;
+import java.util.Map;
 import java.util.Optional;
+import com.taskflow.dto.ProjectSummaryResponse;
+import com.taskflow.mapper.ProjectMapper;
 
 /**
  * ProjectService — la capa de negocio del lado Project. Cero HTTP aquí (el "no existe" se traduce con
@@ -101,4 +104,23 @@ public class ProjectService {
         tareasDe(id).forEach(t -> taskRepository.deleteById(t.getId()));   // cascada manual (la FK obliga el orden)
         projectRepository.deleteById(id);
     }
+
+    /**
+     * Resumen de un proyecto: cuántas tareas por estado y cuántas están vencidas.
+     * Reutiliza Task.estaVencida() para el conteo de vencidas.
+     */
+    public ProjectSummaryResponse resumen(Project proyecto) {
+        List<Task> tareas = taskRepository.findByProjectId(proyecto.getId());
+        long total = tareas.size();
+        long todo = tareas.stream().filter(t -> t.getStatus() == com.taskflow.model.TaskStatus.TODO).count();
+        long inProgress = tareas.stream().filter(t -> t.getStatus() == com.taskflow.model.TaskStatus.IN_PROGRESS).count();
+        long done = tareas.stream().filter(t -> t.getStatus() == com.taskflow.model.TaskStatus.DONE).count();
+        long overdue = tareas.stream().filter(Task::estaVencida).count();
+        Map<String, Long> byStatus = Map.of(
+                "TODO", todo,
+                "IN_PROGRESS", inProgress,
+                "DONE", done
+        );
+        return ProjectMapper.aSummary(proyecto.getId(), proyecto.getName(), total, byStatus, overdue);
+    }
 }
diff --git a/src/test/java/com/taskflow/slice/ProjectSummaryControllerTest.java b/src/test/java/com/taskflow/slice/ProjectSummaryControllerTest.java
new file mode 100644
index 0000000..814b289
--- /dev/null
+++ b/src/test/java/com/taskflow/slice/ProjectSummaryControllerTest.java
@@ -0,0 +1,63 @@
+package com.taskflow.slice;
+
+import com.taskflow.controller.ProjectController;
+import com.taskflow.dto.ProjectSummaryResponse;
+import com.taskflow.model.Project;
+import com.taskflow.security.JwtAuthenticationFilter;
+import com.taskflow.service.ProjectService;
+import org.junit.jupiter.api.Test;
+import org.springframework.beans.factory.annotation.Autowired;
+import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
+import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
+import org.springframework.test.context.bean.override.mockito.MockitoBean;
+import org.springframework.test.web.servlet.MockMvc;
+
+import java.util.Map;
+import java.util.Optional;
+
+import static org.mockito.ArgumentMatchers.any;
+import static org.mockito.Mockito.when;
+import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
+
+@WebMvcTest(ProjectController.class)
+@AutoConfigureMockMvc(addFilters = false)
+class ProjectSummaryControllerTest {
+
+    @Autowired
+    private MockMvc mockMvc;
+
+    @MockitoBean
+    private ProjectService projectService;
+
+    @MockitoBean
+    private JwtAuthenticationFilter jwtAuthenticationFilter;
+
+    @Test
+    void getSummary_existente_devuelve200ConCadaCampo() throws Exception {
+        when(projectService.buscarPorId(2L)).thenReturn(Optional.of(new Project(2L, "App Móvil", "d", 1L, null)));
+        when(projectService.resumen(any(Project.class))).thenReturn(
+                new ProjectSummaryResponse(2L, "App Móvil", 4, Map.of("TODO", 1L, "IN_PROGRESS", 2L, "DONE", 1L), 1)
+        );
+
+        mockMvc.perform(get("/projects/2/summary"))
+                .andExpect(status().isOk())
+                .andExpect(jsonPath("$.projectId").value(2))
+                .andExpect(jsonPath("$.projectName").value("App Móvil"))
+                .andExpect(jsonPath("$.totalTasks").value(4))
+                .andExpect(jsonPath("$.byStatus.TODO").value(1))
+                .andExpect(jsonPath("$.byStatus.IN_PROGRESS").value(2))
+                .andExpect(jsonPath("$.byStatus.DONE").value(1))
+                .andExpect(jsonPath("$.overdue").value(1));
+    }
+
+    @Test
+    void getSummary_proyectoInexistente_devuelve404() throws Exception {
+        when(projectService.buscarPorId(99L)).thenReturn(Optional.empty());
+
+        mockMvc.perform(get("/projects/99/summary"))
+                .andExpect(status().isNotFound())
+                .andExpect(jsonPath("$.status").value(404));
+    }
+}
diff --git a/src/test/java/com/taskflow/unit/ProjectSummaryServiceTest.java b/src/test/java/com/taskflow/unit/ProjectSummaryServiceTest.java
new file mode 100644
index 0000000..db4e222
--- /dev/null
+++ b/src/test/java/com/taskflow/unit/ProjectSummaryServiceTest.java
@@ -0,0 +1,66 @@
+package com.taskflow.unit;
+
+import com.taskflow.dto.ProjectSummaryResponse;
+import com.taskflow.model.Priority;
+import com.taskflow.model.Project;
+import com.taskflow.model.Task;
+import com.taskflow.model.TaskStatus;
+import com.taskflow.repository.ProjectRepository;
+import com.taskflow.repository.TaskRepository;
+import com.taskflow.repository.UserRepository;
+import com.taskflow.service.ProjectService;
+import org.junit.jupiter.api.Test;
+import org.junit.jupiter.api.extension.ExtendWith;
+import org.mockito.InjectMocks;
+import org.mockito.Mock;
+import org.mockito.junit.jupiter.MockitoExtension;
+
+import java.time.LocalDate;
+import java.util.List;
+import java.util.Map;
+
+import static org.junit.jupiter.api.Assertions.assertEquals;
+import static org.mockito.Mockito.when;
+
+@ExtendWith(MockitoExtension.class)
+class ProjectSummaryServiceTest {
+
+    @Mock
+    private ProjectRepository projectRepository;
+
+    @Mock
+    private TaskRepository taskRepository;
+
+    @Mock
+    private UserRepository userRepository;
+
+    @InjectMocks
+    private ProjectService service;
+
+    private final Project proyecto = new Project(2L, "App Móvil", "d", 1L, null);
+
+    @Test
+    void resumen_cuentaPorEstadoYVencidas() throws Exception {
+        when(taskRepository.findByProjectId(2L)).thenReturn(List.of(
+                tarea(5L, TaskStatus.TODO, null),
+                tarea(6L, TaskStatus.IN_PROGRESS, null),
+                tarea(7L, TaskStatus.IN_PROGRESS, LocalDate.now().minusDays(2)), // vencida
+                tarea(8L, TaskStatus.DONE, LocalDate.now().minusDays(5))
+        ));
+
+        Map<String, Long> expectedMap = Map.of("TODO", 1L, "IN_PROGRESS", 2L, "DONE", 1L);
+        assertEquals(new ProjectSummaryResponse(2L, "App Móvil", 4, expectedMap, 1), service.resumen(proyecto));
+    }
+
+    @Test
+    void resumen_proyectoSinTareas_devuelveCeros() {
+        when(taskRepository.findByProjectId(2L)).thenReturn(List.of());
+
+        Map<String, Long> expectedMap = Map.of("TODO", 0L, "IN_PROGRESS", 0L, "DONE", 0L);
+        assertEquals(new ProjectSummaryResponse(2L, "App Móvil", 0, expectedMap, 0), service.resumen(proyecto));
+    }
+
+    private Task tarea(Long id, TaskStatus status, LocalDate due) throws Exception {
+        return new Task(id, "Tarea " + id, "d", status, Priority.MED, 1L, 1L, due);
+    }
+}
```

</details>


---

<sub>8s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-aarodriguezperez\specs\summary.md.**

C:\Users\User\taskflow-copilot-aarodriguezperez\specs\summary.md

<details>
<summary>52 lines</summary>

````
## Qué se pide

Un endpoint nuevo, `GET /projects/{id}/summary`, que resume un proyecto: **cuántas tareas tiene en
cada estado y cuántas están vencidas**. Sirve para pintar un tablero sin descargar todas las tareas del
proyecto. Pide el token de `POST /auth/login`, como todo lo que no es público.

## Respuesta 200

```json
{
  "projectId": 2,
  "projectName": "App Móvil",
  "totalTasks": 4,
  "byStatus": { "TODO": 1, "IN_PROGRESS": 2, "DONE": 1 },
  "overdue": 1
}
```

| Campo | Tipo | Qué es |
|---|---|---|
| `projectId` | número | el `id` del proyecto pedido |
| `projectName` | texto | el `name` del proyecto |
| `totalTasks` | número | cuántas tareas tiene el proyecto |
| `byStatus` | objeto | una clave por cada valor de `TaskStatus` (`TODO`, `IN_PROGRESS`, `DONE`), **siempre las tres**, aunque valgan 0 |
| `overdue` | número | cuántas tareas del proyecto cumplen `Task.estaVencida()` |

## Reglas

1. **Vencida** es exactamente lo que ya dice `Task.estaVencida()`: tiene fecha límite, la fecha ya pasó y
   el estado no es `DONE`. Se reutiliza ese método; no se reescribe la regla en el servicio.
2. Un proyecto **sin tareas** responde 200 con `totalTasks` 0, las tres claves de `byStatus` en 0 y `overdue` 0.
3. Un proyecto **que no existe** responde **404** con el `ErrorResponse` uniforme de siempre
   (`ProjectNotFoundException`, igual que `GET /projects/{id}`).
4. **Sin token** responde **401**, como todo lo que no es público.
5. Cualquier usuario autenticado puede pedir el resumen de cualquier proyecto (igual que `GET /projects/{id}/tasks`).

## Resultado esperado con la semilla del perfil `h2`

| Petición | `totalTasks` | `byStatus` | `overdue` |
|---|---|---|---|
| `GET /projects/1/summary` | 5 | `TODO` 3 · `IN_PROGRESS` 1 · `DONE` 1 | 0 (la tarea 2 tiene fecha pasada, pero está `DONE`) |
| `GET /projects/2/summary` | 4 | `TODO` 1 · `IN_PROGRESS` 2 · `DONE` 1 | 1 (la tarea 7) |
| `GET /projects/3/summary` | 0 | `TODO` 0 · `IN_PROGRESS` 0 · `DONE` 0 | 0 |
| `GET /projects/99/summary` | — | — | 404 |

Si antes de probar creaste tareas en un proyecto (por la UI o con el servidor MCP), `totalTasks` y
`TODO` de ese proyecto suben en la misma cantidad. Al reiniciar la app con el perfil `h2` vuelven a
ser los de la tabla.

## Cómo se construye (convenciones de TaskFlow)

- DTO de salida: un `record` en `com.taskflow.dto`, `ProjectSummaryResponse`.
- La lógica de conteo va en el servicio (`ProjectService`), no en el controller.
- El controller solo traduce HTTP: busca el proyecto (404 si no existe) y delega.
- Tests:
  - **unit** del servicio con Mockito: un proyecto con tareas en los tres estados y una vencida; un proyecto sin tareas.
  - **slice** `@WebMvcTest` del controller: 200 con el JSON de arriba y 404 con un proyecto inexistente.
- `mvn -q test` en verde al terminar, sin modificar tests existentes.

## Criterios de aceptación

- [ ] `GET /projects/2/summary` con el token de `ana` devuelve el JSON del ejemplo.
- [ ] `GET /projects/3/summary` devuelve ceros en los cinco números.
- [ ] `GET /projects/99/summary` devuelve 404.
- [ ] `mvn -q test` en verde: los tests de antes más los nuevos.
````

</details>


---

<sub>1m 38s</sub>

### Copilot

Resumen de la revisión: abrir diff y spec, comprobar checklist y tests. Hallazgos abajo.

| # | Gravedad | Archivo:línea | Punto checklist | Qué pasa | Qué harías |
|---|---|---|---|---|---|
| 1 | Bloquea | src/test/java/com/taskflow/slice/ProjectSummaryControllerTest.java (imports / anotaciones, ~líneas 8-20) | Tests existentes / Convenciones de tests | Usa `@MockitoBean` e importa `org.springframework.test.context.bean.override.mockito.MockitoBean` (no existe en Spring Boot). Los tests no compilarán. | Reemplazar por `@MockBean` (import `org.springframework.boot.test.mock.mockito.MockBean`) en las dos declaraciones (`projectService`, `jwtAuthenticationFilter`). |
| 2 | Corregir | src/main/java/com/taskflow/service/ProjectService.java (método resumen) | Reglas del dominio | Reusa correctamente `Task::estaVencida()` y cuenta por estado. | Ninguno, está bien. |
| 3 | Sugerencia | src/main/java/com/taskflow/dto/ProjectSummaryResponse.java | Contrato / Convenciones | DTO es `record` y contiene los campos requeridos; `byStatus` siempre creado con las tres claves. | Ninguno. |
| 4 | Sugerencia | src/main/java/com/taskflow/controller/ProjectController.java (nuevo método) | Contrato / Seguridad | Endpoint y 404 coinciden con la spec. No hay test de seguridad (slice desactiva filtros). | Considerar añadir test que verifique 401 (ver abajo). |
| 5 | Sugerencia | varios (ProjectMapper, tests unitarios) | Tests que prueban algo | Unit y slice cubren los casos básicos (con tareas, sin tareas, 404). | Corregir el punto 1 para poder ejecutar la suite. |

Casos sin test
- 401 cuando se llama sin token (security) — ninguno de los nuevos tests lo comprueba.
- Prueba de integración contra la semilla H2 (GET /projects/1,2,3) — no incluida.
- No hay test que verifique que byStatus siempre tiene exactamente las tres claves ante cambios futuros.

Veredicto: CAMBIOS PEDIDOS — la anotación errónea en ProjectSummaryControllerTest bloquea la suite. Después de corregir eso, ejecutar mvn -q test para verificar todo.

---

<sub>Generated by [GitHub Copilot CLI](https://github.com/features/copilot/cli)</sub>
