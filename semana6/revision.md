# Copilot CLI Session

> [!NOTE]
> - **Session ID:** `08f5bd15-e6f5-4132-a094-9ce19b217492`  
> - **Started:** 18/9/2026, 12:03:04 p.m.  
> - **Duration:** 1m 49s  
> - **Exported:** 18/9/2026, 12:04:53 p.m.  

---

<sub>2s</sub>

### User

Revisa semana6/proyecto-final.diff contra la especificación specs/progress.md.

---

<sub>8s</sub>

### Copilot

Leeré el diff y la especificación para identificar los archivos cambiados y las reglas a revisar, luego abriré cada archivo afectado para compararlos con la especificación. Ejecutando lecturas en paralelo de semana6/proyecto-final.diff y specs/progress.md.

---

<sub>8s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-aarodriguezperez\semana6\proyecto-final.diff.**

C:\Users\User\taskflow-copilot-aarodriguezperez\semana6\proyecto-final.diff

<details>
<summary>252 lines</summary>

```diff
diff --git a/src/main/java/com/taskflow/controller/ReportController.java b/src/main/java/com/taskflow/controller/ReportController.java
new file mode 100644
index 0000000..f634286
--- /dev/null
+++ b/src/main/java/com/taskflow/controller/ReportController.java
@@ -0,0 +1,35 @@
+package com.taskflow.controller;
+
+import com.taskflow.dto.ProjectProgressResponse;
+import com.taskflow.model.Project;
+import com.taskflow.service.ProjectService;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.tags.Tag;
+import org.springframework.web.bind.annotation.GetMapping;
+import org.springframework.web.bind.annotation.RestController;
+
+import java.util.List;
+
+/**
+ * ReportController — endpoints de reportes agregados en la API.
+ */
+@RestController
+@Tag(name = "Reports", description = "Informes agregados: progreso de proyectos y otros.")
+public class ReportController {
+
+    private final ProjectService projectService;
+
+    public ReportController(ProjectService projectService) {
+        this.projectService = projectService;
+    }
+
+    /**
+     * GET /reports/progress — devuelve una lista con el progreso de cada proyecto.
+     */
+    @Operation(summary = "Progreso de proyectos",
+            description = "Lista para cada proyecto el total de tareas, cuántas están en DONE y el porcentaje completado (un decimal).")
+    @GetMapping("/reports/progress")
+    public List<ProjectProgressResponse> getProgress() {
+        return projectService.progresoPorProyecto();
+    }
+}
diff --git a/src/main/java/com/taskflow/dto/ProjectProgressResponse.java b/src/main/java/com/taskflow/dto/ProjectProgressResponse.java
new file mode 100644
index 0000000..10ec459
--- /dev/null
+++ b/src/main/java/com/taskflow/dto/ProjectProgressResponse.java
@@ -0,0 +1,15 @@
+package com.taskflow.dto;
+
+/**
+ * ProjectProgressResponse — contrato de salida de GET /reports/progress.
+ * Contiene, por proyecto, el total de tareas, cuántas están en DONE y el porcentaje completado
+ * redondeado a un decimal.
+ */
+public record ProjectProgressResponse(
+        Long projectId,
+        String projectName,
+        long totalTasks,
+        long doneTasks,
+        double percentDone
+) {
+}
diff --git a/src/main/java/com/taskflow/mapper/ProjectMapper.java b/src/main/java/com/taskflow/mapper/ProjectMapper.java
index 2d68b6a..5c10187 100644
--- a/src/main/java/com/taskflow/mapper/ProjectMapper.java
+++ b/src/main/java/com/taskflow/mapper/ProjectMapper.java
@@ -1,6 +1,7 @@
 package com.taskflow.mapper;
 
 import com.taskflow.dto.ProjectResponse;
+import com.taskflow.dto.ProjectProgressResponse;
 import com.taskflow.model.Project;
 
 /**
@@ -29,4 +30,11 @@ public final class ProjectMapper {
                                                   long overdue) {
         return new com.taskflow.dto.ProjectSummaryResponse(projectId, projectName, totalTasks, byStatus, overdue);
     }
+
+    /**
+     * DTO de salida de GET /reports/progress: puente desde el service con los conteos ya calculados.
+     */
+    public static ProjectProgressResponse aProgreso(Project proyecto, long totalTasks, long doneTasks, double percentDone) {
+        return new ProjectProgressResponse(proyecto.getId(), proyecto.getName(), totalTasks, doneTasks, percentDone);
+    }
 }
diff --git a/src/main/java/com/taskflow/service/ProjectService.java b/src/main/java/com/taskflow/service/ProjectService.java
index d6b901b..67ecc17 100644
--- a/src/main/java/com/taskflow/service/ProjectService.java
+++ b/src/main/java/com/taskflow/service/ProjectService.java
@@ -123,4 +123,27 @@ public class ProjectService {
         );
         return ProjectMapper.aSummary(proyecto.getId(), proyecto.getName(), total, byStatus, overdue);
     }
+
+    /**
+     * Progreso por proyecto: lista todos los proyectos con conteos y porcentaje redondeado a 1 decimal.
+     * Reutiliza taskRepository.findByProjectId(...) y ProjectMapper.aProgreso.
+     */
+    public java.util.List<com.taskflow.dto.ProjectProgressResponse> progresoPorProyecto() {
+        java.util.List<Project> proyectos = projectRepository.findAll();
+        return proyectos.stream()
+                .sorted(java.util.Comparator.comparing(Project::getId))
+                .map(p -> {
+                    java.util.List<Task> tareas = taskRepository.findByProjectId(p.getId());
+                    long total = tareas.size();
+                    long done = tareas.stream().filter(t -> t.getStatus() == com.taskflow.model.TaskStatus.DONE).count();
+                    double percentDone;
+                    if (total == 0) {
+                        percentDone = 0.0;
+                    } else {
+                        percentDone = Math.round((done * 100.0 / total) * 10.0) / 10.0;
+                    }
+                    return ProjectMapper.aProgreso(p, total, done, percentDone);
+                })
+                .toList();
+    }
 }
diff --git a/src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java b/src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java
new file mode 100644
index 0000000..708099a
--- /dev/null
+++ b/src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java
@@ -0,0 +1,47 @@
+package com.taskflow.slice;
+
+import com.taskflow.controller.ReportController;
+import com.taskflow.dto.ProjectProgressResponse;
+import com.taskflow.security.JwtAuthenticationFilter;
+import com.taskflow.service.ProjectService;
+import org.junit.jupiter.api.Test;
+import org.springframework.beans.factory.annotation.Autowired;
+import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
+import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
+import org.springframework.test.context.bean.override.mockito.MockitoBean;
+import org.springframework.test.web.servlet.MockMvc;
+
+import java.util.List;
+
+import static org.mockito.Mockito.when;
+import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
+
+@WebMvcTest(ReportController.class)
+@AutoConfigureMockMvc(addFilters = false)
+class ProgresoProyectosControllerTest {
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
+    void getProgress_devuelve200ConCadaCampo() throws Exception {
+        when(projectService.progresoPorProyecto()).thenReturn(List.of(
+                new ProjectProgressResponse(1L, "Plataforma TaskFlow", 5, 1, 20.0),
+                new ProjectProgressResponse(2L, "App Móvil", 3, 1, 33.3)
+        ));
+
+        mockMvc.perform(get("/reports/progress"))
+                .andExpect(status().isOk())
+                .andExpect(jsonPath("$[0].projectId").value(1))
+                .andExpect(jsonPath("$[0].percentDone").value(20.0))
+                .andExpect(jsonPath("$[1].totalTasks").value(3));
+    }
+}
diff --git a/src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java b/src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java
new file mode 100644
index 0000000..3f1ae2f
--- /dev/null
+++ b/src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java
@@ -0,0 +1,77 @@
+package com.taskflow.unit;
+
+import com.taskflow.dto.ProjectProgressResponse;
+import com.taskflow.model.Project;
+import com.taskflow.model.Priority;
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
+import java.util.List;
+
+import static org.junit.jupiter.api.Assertions.assertEquals;
+import static org.mockito.Mockito.when;
+
+@ExtendWith(MockitoExtension.class)
+class ProgresoProyectosServiceTest {
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
+    private Project p1 = new Project(1L, "Plataforma TaskFlow", "d", 1L, null);
+    private Project p2 = new Project(2L, "App Móvil", "d", 1L, null);
+    private Project p3 = new Project(3L, "Migración Legacy", "d", 1L, null);
+
+    @Test
+    void progreso_listaTodosLosProyectos_yLosOrdenaYCalculaPorcentaje() throws Exception {
+        // repositorio devuelve en orden 3,1,2 (mezcla)
+        when(projectRepository.findAll()).thenReturn(List.of(p3, p1, p2));
+
+        // proyecto 1: 5 tareas, 1 DONE -> 20.0
+        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(
+                tarea(1L, TaskStatus.TODO),
+                tarea(2L, TaskStatus.TODO),
+                tarea(3L, TaskStatus.TODO),
+                tarea(4L, TaskStatus.TODO),
+                tarea(5L, TaskStatus.DONE)
+        ));
+
+        // proyecto 2: 3 tareas, 1 DONE -> 33.3
+        when(taskRepository.findByProjectId(2L)).thenReturn(List.of(
+                tarea(6L, TaskStatus.TODO),
+                tarea(7L, TaskStatus.DONE),
+                tarea(8L, TaskStatus.TODO)
+        ));
+
+        // proyecto 3: sin tareas -> 0.0
+        when(taskRepository.findByProjectId(3L)).thenReturn(List.of());
+
+        List<com.taskflow.dto.ProjectProgressResponse> resultado = service.progresoPorProyecto();
+
+        assertEquals(3, resultado.size());
+        assertEquals(new ProjectProgressResponse(1L, "Plataforma TaskFlow", 5, 1, 20.0), resultado.get(0));
+        assertEquals(new ProjectProgressResponse(2L, "App Móvil", 3, 1, 33.3), resultado.get(1));
+        assertEquals(new ProjectProgressResponse(3L, "Migración Legacy", 0, 0, 0.0), resultado.get(2));
+    }
+
+    private Task tarea(Long id, TaskStatus status) throws Exception {
+        return new Task(id, "Tarea " + id, "d", status, Priority.MED, 1L, null, null);
+    }
+}
```

</details>


---

<sub>8s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-aarodriguezperez\specs\progress.md.**

C:\Users\User\taskflow-copilot-aarodriguezperez\specs\progress.md

<details>
<summary>77 lines</summary>

````
# Especificación — `GET /reports/progress`

## Qué

Un reporte con el avance de **cada** proyecto: cuántas tareas tiene, cuántas están terminadas y qué
porcentaje representan. Sirve para una pantalla de «cómo vamos» sin pedir las tareas proyecto por
proyecto.

```
GET /reports/progress
Authorization: ******
```

## Respuesta 200

```json
[
  { "projectId": 1, "projectName": "Plataforma TaskFlow", "totalTasks": 5, "doneTasks": 1, "percentDone": 20.0 },
  { "projectId": 2, "projectName": "App Móvil",           "totalTasks": 4, "doneTasks": 1, "percentDone": 25.0 },
  { "projectId": 3, "projectName": "Migración Legacy",    "totalTasks": 0, "doneTasks": 0, "percentDone": 0.0 }
]
```

| Campo | Tipo | Qué es |
|---|---|---|
| `projectId` | número | el `id` del proyecto |
| `projectName` | texto | el `name` del proyecto |
| `totalTasks` | número | cuántas tareas tiene el proyecto |
| `doneTasks` | número | cuántas de esas están en `DONE` |
| `percentDone` | número decimal | `doneTasks` entre `totalTasks`, por 100, redondeado a **un decimal** |

## Reglas

1. Aparecen **todos** los proyectos, también los que no tienen tareas.
2. Proyecto sin tareas: `totalTasks` 0, `doneTasks` 0 y `percentDone` **`0.0`**. Nunca `NaN` ni un
   error por dividir entre cero.
3. El porcentaje se calcula en decimal **antes** de dividir (`doneTasks * 100.0 / totalTasks`) y se
   redondea a un decimal: 1 de 3 → `33.3`; 2 de 3 → `66.7`; 1 de 8 → `12.5`. Es la trampa `long/long`
   que ya documenta `ReportService.porcentajeCompletadas()`: con división entera, 1 de 4 da `0`.
4. Orden: por `projectId` ascendente, aunque el repositorio los devuelva en otro orden.
5. Seguridad: cualquier usuario autenticado puede pedirlo (sin token → `401`). No cambies
   `SecurityConfig`.

## Dónde

- DTO de salida nuevo en `com.taskflow.dto`:
  `public record ProjectProgressResponse(Long projectId, String projectName, long totalTasks, long doneTasks, double percentDone)`,
  con su Javadoc.
- Mapper: método nuevo `public static ProjectProgressResponse aProgreso(Project proyecto, long totalTasks, long doneTasks, double percentDone)`
  en `ProjectMapper`.
- Método nuevo en `ProjectService`: `public List<ProjectProgressResponse> progresoPorProyecto()`.
  `ProjectService` ya tiene los dos repositorios que hacen falta (`projectRepository` y
  `taskRepository`): usa `projectRepository.findAll()` y `taskRepository.findByProjectId(Long)`.
  **No agregues métodos a los repositorios.** La cuenta y el redondeo van aquí, no en el controller.
- Controller **nuevo** `ReportController` en `com.taskflow.controller`, con
  `@Tag(name = "Reports", description = …)`, `@GetMapping("/reports/progress")` y `@Operation` en
  español. Recibe `ProjectService` por constructor. Es un controller nuevo, así que ningún slice test
  existente lo conoce y no rompe nada.

## Tests que deben existir al terminar

Los dos en **clases nuevas**:

- **Unit** `src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java`, con `@Mock` de
  `ProjectRepository`, `TaskRepository` y `UserRepository` (los tres del constructor de
  `ProjectService`) e `@InjectMocks ProjectService`. Un caso con tres proyectos que el mock devuelve
  en orden `3, 1, 2`:
  - el 1 con 5 tareas y 1 `DONE` → `20.0`;
  - el 2 con 3 tareas y 1 `DONE` → `33.3` (prueba el redondeo);
  - el 3 sin tareas → `0.0`;
  - y la lista sale en orden `1, 2, 3`.
- **Slice** `src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java`, con
  `@WebMvcTest(ReportController.class)`, `@AutoConfigureMockMvc(addFilters = false)` y `@MockitoBean`
  de `ProjectService` y `JwtAuthenticationFilter`. Caso: `200` y cada campo del JSON de los dos
  primeros elementos que devuelve el mock (`$[0].projectId`, `$[0].percentDone`, `$[1].totalTasks`…).

## Resultado esperado con la semilla

Con la app recién arrancada con el perfil `h2` y el token de `ana`:

| Petición | Resultado |
|---|---|
| `GET /reports/progress` | `200` con los tres objetos del ejemplo de arriba, en ese orden |
| `PATCH /tasks/1/status` con `{"status": "DONE"}` y luego `GET /reports/progress` | el proyecto 1 pasa a `doneTasks` 2 y `percentDone` `40.0` (la tarea 1 tiene responsable, así que el PATCH responde 200) |
| `GET /reports/progress` sin token | `401` |

## Restricciones para el agente

- No modifiques, borres ni desactives ningún test existente.
- No toques archivos fuera de `ProjectProgressResponse.java` (nuevo), `ProjectMapper.java`,
  `ProjectService.java`, `ReportController.java` (nuevo) y las dos clases de test nuevas.
- Al terminar, `mvn -q test` tiene que pasar completo.

El pull request del proyecto final lleva además `semana6/`, `.github/skills/verificar-taskflow/casos-progress.ps1`
y una línea nueva en `verificar.ps1`. Esos archivos los agregas tú en el proceso (PF-2 a PF-7 de la guía),
no el agente: no cuentan contra esta restricción.
````

</details>


---

<sub>26s</sub>

### Copilot

Abriendo en paralelo los archivos clave para comparar: ProjectMapper, ProjectService, Project, Task y TaskRepository. Esto permite verificar firmas de constructores y existencia de findByProjectId. Ejecutando las lecturas.

---

<sub>26s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-aarodriguezperez\src\main\java\com\taskflow\mapper\ProjectMapper.java.**

C:\Users\User\taskflow-copilot-aarodriguezperez\src\main\java\com\taskflow\mapper\ProjectMapper.java

<details>
<summary>34 lines</summary>

```
package com.taskflow.mapper;

import com.taskflow.dto.ProjectResponse;
import com.taskflow.dto.ProjectProgressResponse;
import com.taskflow.model.Project;

/**
 * ProjectMapper — puente DTO &lt;-&gt; dominio del lado Project. Estático, a mano, sin MapStruct.
 *
 * HOY se SIMPLIFICÓ (lo prometía D3): la entidad ya guarda 'ownerId' directo (se aplanó el 'User
 * owner' en MP-4), así que aResponse ya no deriva el id desde un objeto (p.getOwner().id()) — lee
 * p.getOwnerId() tal cual. El contrato de salida (ProjectResponse con ownerId Long) no cambió; el
 * mapeo se volvió trivial porque el dominio por fin coincide con la forma canónica.
 */
public final class ProjectMapper {

    private ProjectMapper() {
        // no instanciable
    }

    /** Entidad -> DTO de salida. Ahora ownerId sale directo del campo (sin puente por objeto). */
    public static ProjectResponse aResponse(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(),
                p.getOwnerId(), p.getCreatedAt());
    }

    /** DTO de salida de GET /projects/{id}/summary. */
    public static com.taskflow.dto.ProjectSummaryResponse aSummary(Long projectId, String projectName,
                                                  long totalTasks, java.util.Map<String, Long> byStatus,
                                                  long overdue) {
        return new com.taskflow.dto.ProjectSummaryResponse(projectId, projectName, totalTasks, byStatus, overdue);
    }

    /**
     * DTO de salida de GET /reports/progress: puente desde el service con los conteos ya calculados.
     */
    public static ProjectProgressResponse aProgreso(Project proyecto, long totalTasks, long doneTasks, double percentDone) {
        return new ProjectProgressResponse(proyecto.getId(), proyecto.getName(), totalTasks, doneTasks, percentDone);
    }
}
```

</details>


---

<sub>27s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-aarodriguezperez\src\main\java\com\taskflow\service\ProjectService.java.**

C:\Users\User\taskflow-copilot-aarodriguezperez\src\main\java\com\taskflow\service\ProjectService.java (lines 1-260)

<details>
<summary>136 lines</summary>

```
package com.taskflow.service;

import com.taskflow.dto.ProjectRequest;
import com.taskflow.exception.ProjectNotFoundException;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.User;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.mapper.ProjectMapper;

/**
 * ProjectService — la capa de negocio del lado Project. Cero HTTP aquí (el "no existe" se traduce con
 * orElseThrow -> ProjectNotFoundException; el advice le pone el 404).
 *
 * Cambios de HOY (S2D4) — y por qué la promesa del día NO lo cubre:
 *   - MP-4: el modelo Project se aplanó (User owner -> Long ownerId), así que 'crear' y 'reemplazar'
 *     manejan un ownerId (Long), no un objeto User. Antes 'crear' construía con 0L (convención del
 *     InMemory de "aún sin id"); ahora construye con null y la BD asigna el id (IDENTITY).
 *   - MP-6: 'tareasDe' cambió su stream-filter de findAll() por taskRepository.findByProjectId(id)
 *     — la nota de D3 ("en D4 esto se vuelve un query method") se paga AQUÍ. La promesa "no tocar el
 *     servicio" protege a TaskService (que no se tocó), no a ProjectService (que ya se tocó en MP-4).
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;   // D5: para resolver el owner desde el username del JWT

    public ProjectService(ProjectRepository projectRepository, TaskRepository taskRepository,
                          UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    /** Todos los proyectos. */
    public List<Project> listar() {
        return projectRepository.findAll();
    }

    /** Un proyecto por id (el Optional sube tal cual: el controller decide 404 vs 200). */
    public Optional<Project> buscarPorId(Long id) {
        return projectRepository.findById(id);
    }

    /**
     * Las tareas de un proyecto: MP-6 sustituye el stream-filter de D2 por la derived query
     * taskRepository.findByProjectId(projectId) — el LIKE... perdón, el WHERE project_id = ? lo hace
     * la BD (viajan solo las filas del proyecto). "no existe el proyecto" (404) lo decide el
     * controller con buscarPorId; ESTE método, si el proyecto existe pero no tiene tareas, devuelve
     * [] (-> 200 con []). No son lo mismo.
     */
    public List<Task> tareasDe(Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    /**
     * Crea un proyecto (POST): el request trae name y description; el ownerId ya NO es una constante
     * (murió el 1L fijo de D3) — se resuelve del USERNAME AUTENTICADO (el que puso el JWT en el
     * Authentication). El dueño no lo decide el cliente ni una semilla: es QUIEN llama. createdAt = hoy;
     * id == null -> la BD lo asigna (IDENTITY).
     */
    public Project crear(ProjectRequest request, String ownerUsername) {
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new UsernameNotFoundException(ownerUsername));
        Project nuevo = new Project(null, request.name(), request.description(),
                owner.getId(), LocalDate.now());
        return projectRepository.save(nuevo);
    }

    /**
     * Reemplazo COMPLETO (PUT): existe -> reconstruye con el MISMO id, conservando ownerId y createdAt
     * (el cliente no los toca); name y description vienen del request. save con id presente = update.
     * No existe -> 404.
     */
    public Project reemplazar(Long id, ProjectRequest request) {
        Project actual = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        Project reemplazo = new Project(actual.getId(), request.name(), request.description(),
                actual.getOwnerId(), actual.getCreatedAt());
        return projectRepository.save(reemplazo);
    }

    /**
     * Elimina un proyecto (DELETE) con CASCADA MANUAL: borra primero sus tareas (regla "no Task sin
     * Project" — y además la FK task.project_id lo EXIGE: borrar el proyecto con tareas colgando daría
     * una violación de integridad) y luego el proyecto. No existe -> 404. La regla owner/ADMIN llega
     * en D5 (hoy queda abierto, documentado en el @Operation del controller).
     */
    public void eliminar(Long id) {
        projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        tareasDe(id).forEach(t -> taskRepository.deleteById(t.getId()));   // cascada manual (la FK obliga el orden)
        projectRepository.deleteById(id);
    }

    /**
     * Resumen de un proyecto: cuántas tareas por estado y cuántas están vencidas.
     * Reutiliza Task.estaVencida() para el conteo de vencidas.
     */
    public ProjectSummaryResponse resumen(Project proyecto) {
        List<Task> tareas = taskRepository.findByProjectId(proyecto.getId());
        long total = tareas.size();
        long todo = tareas.stream().filter(t -> t.getStatus() == com.taskflow.model.TaskStatus.TODO).count();
        long inProgress = tareas.stream().filter(t -> t.getStatus() == com.taskflow.model.TaskStatus.IN_PROGRESS).count();
        long done = tareas.stream().filter(t -> t.getStatus() == com.taskflow.model.TaskStatus.DONE).count();
        long overdue = tareas.stream().filter(Task::estaVencida).count();
        Map<String, Long> byStatus = Map.of(
                "TODO", todo,
                "IN_PROGRESS", inProgress,
                "DONE", done
        );
        return ProjectMapper.aSummary(proyecto.getId(), proyecto.getName(), total, byStatus, overdue);
    }

    /**
     * Progreso por proyecto: lista todos los proyectos con conteos y porcentaje redondeado a 1 decimal.
     * Reutiliza taskRepository.findByProjectId(...) y ProjectMapper.aProgreso.
     */
    public java.util.List<com.taskflow.dto.ProjectProgressResponse> progresoPorProyecto() {
        java.util.List<Project> proyectos = projectRepository.findAll();
        return proyectos.stream()
                .sorted(java.util.Comparator.comparing(Project::getId))
                .map(p -> {
                    java.util.List<Task> tareas = taskRepository.findByProjectId(p.getId());
                    long total = tareas.size();
                    long done = tareas.stream().filter(t -> t.getStatus() == com.taskflow.model.TaskStatus.DONE).count();
                    double percentDone;
                    if (total == 0) {
                        percentDone = 0.0;
                    } else {
                        percentDone = Math.round((done * 100.0 / total) * 10.0) / 10.0;
                    }
                    return ProjectMapper.aProgreso(p, total, done, percentDone);
                })
                .toList();
    }
}
```

</details>


---

<sub>53s</sub>

### Copilot

Abriendo en paralelo los archivos nuevos para obtener líneas y contexto: ReportController.java, ProjectProgressResponse.java, ProgresoProyectosControllerTest.java, ProgresoProyectosServiceTest.java.

---

<sub>53s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-aarodriguezperez\src\main\java\com\taskflow\controller\ReportController.java.**

C:\Users\User\taskflow-copilot-aarodriguezperez\src\main\java\com\taskflow\controller\ReportController.java (lines 1-200)

<details>
<summary>29 lines</summary>

```
package com.taskflow.controller;

import com.taskflow.dto.ProjectProgressResponse;
import com.taskflow.model.Project;
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
```

</details>


---

<sub>55s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-aarodriguezperez\src\main\java\com\taskflow\dto\ProjectProgressResponse.java.**

C:\Users\User\taskflow-copilot-aarodriguezperez\src\main\java\com\taskflow\dto\ProjectProgressResponse.java (lines 1-200)

<details>
<summary>14 lines</summary>

```
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
```

</details>


---

<sub>57s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-aarodriguezperez\src\test\java\com\taskflow\slice\ProgresoProyectosControllerTest.java.**

C:\Users\User\taskflow-copilot-aarodriguezperez\src\test\java\com\taskflow\slice\ProgresoProyectosControllerTest.java (lines 1-200)

<details>
<summary>38 lines</summary>

```
package com.taskflow.slice;

import com.taskflow.controller.ReportController;
import com.taskflow.dto.ProjectProgressResponse;
import com.taskflow.security.JwtAuthenticationFilter;
import com.taskflow.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProgresoProyectosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getProgress_devuelve200ConCadaCampo() throws Exception {
        when(projectService.progresoPorProyecto()).thenReturn(List.of(
                new ProjectProgressResponse(1L, "Plataforma TaskFlow", 5, 1, 20.0),
                new ProjectProgressResponse(2L, "App Móvil", 3, 1, 33.3)
        ));

        mockMvc.perform(get("/reports/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectId").value(1))
                .andExpect(jsonPath("$[0].percentDone").value(20.0))
                .andExpect(jsonPath("$[1].totalTasks").value(3));
    }
}
```

</details>


---

<sub>59s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-aarodriguezperez\src\test\java\com\taskflow\unit\ProgresoProyectosServiceTest.java.**

C:\Users\User\taskflow-copilot-aarodriguezperez\src\test\java\com\taskflow\unit\ProgresoProyectosServiceTest.java (lines 1-300)

<details>
<summary>61 lines</summary>

```
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
```

</details>


---

<sub>1m 9s</sub>

### Copilot

Abriendo en paralelo TaskRepository, Task y Project para comprobar firmas y métodos referenciados. Ejecutando lecturas.

---

<sub>1m 9s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-aarodriguezperez\src\main\java\com\taskflow\repository\TaskRepository.java.**

C:\Users\User\taskflow-copilot-aarodriguezperez\src\main\java\com\taskflow\repository\TaskRepository.java

<details>
<summary>36 lines</summary>

```
package com.taskflow.repository;

import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * TaskRepository — EL MOMENTO NARRATIVO DEL DÍA (MP-6). Ayer era una interfaz que redeclaraba
 * save/findById/findAll/deleteById, implementada por InMemoryTaskRepository. HOY extiende
 * JpaRepository&lt;Task, Long&gt; y esos 4 métodos VIENEN GRATIS (+ count/existsById/etc.): NO se
 * implementa nadie, un PROXY de runtime lo hace (la misma "magia" de DI de S2D1).
 *
 * La paga de la interfaz extraída en S1D5: TaskService/ReportService dependían de ESTE contrato desde
 * entonces, y sus firmas (Optional&lt;Task&gt; findById, void deleteById) se alinearon a Spring Data
 * desde S2D1 — por eso hoy el swap memoria->BD NO toca el servicio. "git diff de TaskService = vacío".
 * InMemoryTaskRepository dejó de compilar y SE ELIMINÓ (git lo recuerda; borrar código es sano
 * cuando la interfaz sobrevive).
 *
 * DERIVED QUERIES: el NOMBRE del método es la query. Gramática findBy + propiedad + And/Not/
 * Containing/IgnoreCase/OrderBy, y countBy. Se validan AL ARRANCAR: un nombre con una propiedad que
 * no existe (p.ej. findByEstado) lanza QueryCreationException y la app NO levanta — error de arranque,
 * no de runtime. Estas seis se prueban en TaskRepositoryTest (@DataJpaTest).
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    /** Tareas en un estado dado. La consume ProjectService/ReportService y las vistas por estado. */
    List<Task> findByStatus(TaskStatus status);

    /** Tareas de un proyecto (por la columna project_id). La consume ProjectService.tareasDe (MP-6). */
    List<Task> findByProjectId(Long projectId);

    /** Combina dos criterios con And: tareas de un asignado en un estado. La consume el stretch de D5. */
    List<Task> findByAssigneeIdAndStatus(Long assigneeId, TaskStatus status);

    /** LIKE case-insensitive: título que CONTIENE el fragmento (la BD hace el filtrado). */
    List<Task> findByTitleContainingIgnoreCase(String fragment);

    /** El complemento de findByStatus: todas MENOS las de ese estado (Not). Sirve a "pendientes". */
    List<Task> findByStatusNot(TaskStatus status);

    /** Cuenta en la BD (no trae filas para contarlas): conteo por estado. */
    long countByStatus(TaskStatus status);
}
```

</details>


---

<sub>1m 9s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-aarodriguezperez\src\main\java\com\taskflow\model\Task.java.**

C:\Users\User\taskflow-copilot-aarodriguezperez\src\main\java\com\taskflow\model\Task.java

<details>
<summary>225 lines</summary>

```
package com.taskflow.model;

import com.taskflow.exception.TaskValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Task — entidad central del dominio TaskFlow. ESTADO FINAL del Día 4 (persistida con JPA).
 *
 * La entidad ES la clase que YA teníamos (canónica S1, 8 campos): NO se escribió un "modelo nuevo",
 * se ANOTÓ el Task de D3. Novedades de HOY, todas por anotación:
 *   - @Entity + @Table(name = "tasks"): esta clase mapea a la tabla TASKS.
 *   - @Id + @GeneratedValue(IDENTITY): el id lo asigna la BD (columna autoincremental). La secuencia
 *     manual del InMemoryTaskRepository (el Math.max de S1D3) MURIÓ: el id lo pone la BD y nadie más.
 *   - @Enumerated(STRING) en status y priority: se guardan como TEXTO ('TODO'), NUNCA como ORDINAL
 *     (0,1,2): reordenar o insertar un enum corrompería datos históricos. El orden LOW,MED,HIGH de
 *     S1D3 es contrato del SORT (compareTo), no del almacenamiento.
 *   - relación @ManyToOne Task->Project (patrón "asociación de solo lectura"): la COLUMNA la sigue
 *     mandando el escalar 'projectId' (por eso Task.crear, TaskService, mappers y DTOs NO cambian);
 *     el objeto 'project' solo NAVEGA. Frase ancla: "la columna manda; el objeto navega".
 *
 * Tensión JPA vs validaciones (paga de S1D2 MP-8, fijada por el apéndice del CAPSTONE): JPA rehidrata
 * por reflexión con un constructor sin args protegido, SIN pasar por las validaciones. Está bien:
 * Task.crear(...) sigue siendo el ÚNICO camino de CREACIÓN de negocio; JPA solo REHIDRATA datos que
 * ya existían (la misma distinción crear/rehidratar de siempre).
 *
 * OJO: las columnas persistentes NO pueden ser 'final' (JPA las escribe por reflexión tras el no-arg);
 * por eso title/description/projectId/dueDate perdieron el 'final' que traían de D3. La inmutabilidad
 * de negocio la sigue garantizando la ausencia de setters públicos para esos campos.
 */
@Entity
@Table(name = "tasks")
public class Task implements Comparable<Task> {

    // Reglas de longitud del título (del capstone).
    private static final int TITULO_MIN = 3;
    private static final int TITULO_MAX = 120;

    // id nace null; lo asigna la BD (IDENTITY). Ver setId (uso interno de JPA/repo).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    private String description;                 // por convención -> columna DESCRIPTION

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;                  // texto 'TODO'/'IN_PROGRESS'/'DONE' (nunca 0/1/2)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;                  // texto 'LOW'/'MED'/'HIGH'

    // La COLUMNA project_id la posee este escalar (insertable/updatable por default). Sigue siendo
    // el dueño: Task.crear, el mapper y el servicio hablan con projectId, no con el objeto.
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    // assigneeId se queda Long a secas (juicio explícito): es nullable, NUNCA navegamos al User
    // completo al leer tareas, y las derived queries operan igual. No toda FK necesita ser objeto:
    // mapea objeto cuando NAVEGAS, deja el id cuando solo REFERENCIAS.
    @Column(name = "assignee_id")
    private Long assigneeId;

    private LocalDate dueDate;                  // por convención -> columna DUE_DATE (camelCase->snake_case)

    /**
     * Asociación de SOLO LECTURA hacia el proyecto (MP-8). Comparte la MISMA columna project_id con
     * el escalar de arriba, pero marcada insertable=false/updatable=false: no escribe la columna (el
     * escalar lo hace), solo permite NAVEGAR (task.getProject().getName()). LAZY: no se carga hasta
     * que se toca — y fuera de una sesión abierta, tocarla lanza LazyInitializationException (por eso
     * la API responde con DTOs, nunca con la entidad).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    /**
     * Constructor NO-ARG protegido: EXCLUSIVO de JPA (rehidratación por reflexión). Nunca es un camino
     * de creación de negocio. Su existencia es obligatoria para cualquier @Entity: sin él, al arrancar
     * Hibernate lanza "No default constructor for entity: com.taskflow.model.Task".
     */
    protected Task() {
        // solo para JPA: rehidratación, nunca creación de negocio
    }

    /**
     * Constructor de REHIDRATACIÓN: reconstruye una tarea que YA existía (semilla del DataSeeder,
     * datos releídos). Valida las INVARIANTES que siempre deben cumplirse (título y proyecto), pero
     * NO la regla temporal de dueDate: una tarea vencida es un dato válido que hay que poder releer.
     */
    public Task(Long id, String title, String description, TaskStatus status,
                Priority priority, Long projectId, Long assigneeId, LocalDate dueDate)
            throws TaskValidationException {
        // Invariante 1: título obligatorio, 3-120 chars.
        if (title == null || title.isBlank()) {
            throw new TaskValidationException("El título es obligatorio (no puede ir vacío).");
        }
        if (title.length() < TITULO_MIN || title.length() > TITULO_MAX) {
            throw new TaskValidationException(
                    "El título debe tener entre " + TITULO_MIN + " y " + TITULO_MAX
                            + " caracteres; recibí " + title.length() + ": \"" + title + "\".");
        }
        // Invariante 2: una tarea no puede existir sin proyecto (regla del capstone).
        if (projectId == null) {
            throw new TaskValidationException(
                    "Una tarea no puede existir sin proyecto (projectId == null).");
        }
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.projectId = projectId;
        this.assigneeId = assigneeId;
        this.dueDate = dueDate;
    }

    /**
     * Factory de CREACIÓN de negocio: añade la regla temporal (dueDate no en el pasado) y delega en
     * el constructor. id nace null, status nace TODO. Firma canónica S1 (apéndice del CAPSTONE-SPEC):
     * la usan el mapper y el servicio tal cual — por eso NO cambia hoy pese a que Task ya sea @Entity.
     */
    public static Task crear(String title, String description, Priority priority,
                             LocalDate dueDate, Long projectId, Long assigneeId)
            throws TaskValidationException {
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            throw new TaskValidationException(
                    "La fecha límite no puede estar en el pasado: " + dueDate + ".");
        }
        return new Task(null, title, description, TaskStatus.TODO, priority, projectId, assigneeId, dueDate);
    }

    // ---- Comportamiento: vive CON los datos ----

    /** true si tiene fecha, ya pasó, y la tarea aún no está terminada. */
    public boolean estaVencida() {
        return dueDate != null && dueDate.isBefore(LocalDate.now()) && status != TaskStatus.DONE;
    }

    // ---- Getters ----

    public Long getId() {
        return id;
    }

    /** Uso interno (JPA/repo) para el id autoincremental. Nadie más lo llama a mano. */
    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Priority getPriority() {
        return priority;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    /**
     * Navega al proyecto asociado (asociación de solo lectura). Fuera de una sesión JPA abierta puede
     * lanzar LazyInitializationException (por eso la API expone DTOs, no la entidad). En el runtime
     * nadie del camino principal lo usa; existe para NAVEGAR cuando haga falta.
     */
    public Project getProject() {
        return project;
    }

    // ---- Setters SOLO donde el dominio muta ----

    /**
     * Cambia el estado aplicando la regla del capstone: no se puede pasar a DONE una tarea sin
     * responsable (assigneeId). La regla vive AQUÍ, no en el servicio ni en el menú.
     */
    public void setStatus(TaskStatus status) throws TaskValidationException {
        if (status == TaskStatus.DONE && assigneeId == null) {
            throw new TaskValidationException(
                    "No se puede marcar como " + TaskStatus.DONE
                            + " una tarea sin responsable: \"" + title + "\".");
        }
        this.status = status;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    // ---- Identidad de ENTIDAD por id (S1D3 MP-6 = literalmente lo que hace JPA) ----

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Task otra = (Task) o;
        return Objects.equals(id, otra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // ---- Orden natural por prioridad (Comparable) ----

    @Override
    public int compareTo(Task otra) {
        return this.priority.compareTo(otra.priority);
    }

    @Override
    public String toString() {
        return "Task{id=" + id
                + ", title='" + title + '\''
                + ", status=" + status
                + ", priority=" + priority
                + ", assigneeId=" + (assigneeId == null ? "sin asignar" : assigneeId)
                + ", dueDate=" + (dueDate == null ? "sin fecha" : dueDate)
                + (estaVencida() ? " *VENCIDA*" : "")
                + '}';
    }
}
```

</details>


---

<sub>1m 9s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-aarodriguezperez\src\main\java\com\taskflow\model\Project.java.**

C:\Users\User\taskflow-copilot-aarodriguezperez\src\main\java\com\taskflow\model\Project.java

<details>
<summary>66 lines</summary>

```
package com.taskflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Project — un proyecto dueño de tareas, ESTADO FINAL del Día 4 (entidad JPA).
 *
 * PAGA DE LA PROMESA DE S1D2 (MP-4): el 'User owner' (objeto) de D1/D3 se APLANÓ a 'Long ownerId'
 * — la forma CANÓNICA del capstone (id, name, description, ownerId, createdAt). "El repositorio/BD
 * guarda ids": lo que en el dominio de consola era un objeto, en la BD es una columna Long.
 *
 * Como Task: @Entity + @Table(name = "projects"), @Id @GeneratedValue(IDENTITY), constructor no-arg
 * protegido para JPA. El id pasó de 'long' primitivo a 'Long' (envoltorio): un id nuevo es null hasta
 * que la BD lo asigna (el 0L de "aún sin id" del InMemory murió con el repositorio en memoria).
 *
 * ownerId NO gana @ManyToOne hoy (juicio, como assigneeId en Task): no navegamos al User dueño al
 * listar proyectos; con el id basta. Se mapea objeto cuando se NAVEGA, no cuando solo se REFERENCIA.
 */
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    private Long ownerId;

    private LocalDate createdAt;

    /** Constructor no-arg protegido: EXCLUSIVO de JPA (rehidratación por reflexión). */
    protected Project() {
        // solo para JPA
    }

    public Project(Long id, String name, String description, Long ownerId, LocalDate createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Project{id=" + id + ", name='" + name + '\''
                + ", ownerId=" + ownerId
                + ", createdAt=" + createdAt + '}';
    }
}
```

</details>


---

<sub>1m 49s</sub>

### Copilot

Resumen breve: comparé el diff con specs/progress.md y abrí todos los archivos afectados. No hay fallos que bloqueen; las reglas, contratos, DTOs y tests exigidos están implementados.

| # | Grav. | Archivo:línea | Punto checklist | Qué pasa | Qué harías |
|---|---|---|---|---|---|
| 1 | Sugerencia | src/main/java/com/taskflow/controller/ReportController.java:4 | Convenciones / limpieza | Import innecesario: import com.taskflow.model.Project no se usa. | Eliminar el import para limpiar el código. |
| 2 | Sugerencia | src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java:18 | Tests slice | @MockitoBean JwtAuthenticationFilter está presente aunque addFilters = false (innecesario). | Quitar el @MockitoBean del filtro o dejar comentario; no afecta ejecución. |

Casos sin test:
- ninguno (los dos tests requeridos están presentes y cubren los casos especificados: ordenamiento, redondeo para 1/3, proyecto sin tareas, y slice que valida campos JSON).

Veredicto: APROBADO.

---

<sub>Generated by [GitHub Copilot CLI](https://github.com/features/copilot/cli)</sub>
