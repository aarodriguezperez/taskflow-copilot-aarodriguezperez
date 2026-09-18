# Proyecto final · Semana 6 · GitHub Copilot

**Alumno:** `Alberto Alejandro Rodríguez Pérez` · **Usuario de GitHub:** `aarodriguezperez`

## 1. Qué construí

| | Feature | Especificación |
|---|---|---|
| [X] | `GET /reports/progress` — avance por proyecto | [`specs/progress.md`](../specs/progress.md) |

## 2. El pull request

- **URL del PR (mergeado):** `https://github.com/aarodriguezperez/taskflow-copilot-aarodriguezperez/pull/5`
- **Commit del merge en `main`:** `49bd9ad (HEAD -> main, origin/main) Merge pull request #5 from aarodriguezperez/feature/progress`
- **Comentarios de Copilot code review:** `3 comentarios: 2 aplicados y 1 descartado después de comprobarlo.`

## 3. Cómo lo hice

| Paso | Qué hice | Evidencia |
|---|---|---|
| Rama y spec | `git switch -c feature/progress` y copié la spec a `specs/progress.md` | `git log --oneline main..feature/progress` (antes del merge) |
| Implementación | `copilot -p "/crear-endpoint-taskflow …"` con `gpt-5-mini` | `semana6/sesion-implementacion.md` (tiene la línea `Skill "crear-endpoint-taskflow" loaded successfully`) |
| Revisión | agente `revisor` sobre `semana6/proyecto-final.diff` | `semana6/revision.md` (termina con `Veredicto: APROBADO`) |
| Tests | `mvn test` en verde | `Tests run: 79, Failures: 0, Errors: 0, Skipped: 0` |
| Comprobación REST | `verificar.ps1` con `casos-progress.ps1` | sección 5 de este documento |
| Code review | Copilot en el PR | `https://github.com/aarodriguezperez/taskflow-copilot-aarodriguezperez/pull/5/changes` |

## 4. Qué hizo el agente y qué corregí yo

| # | Qué hizo mal el agente (archivo) | Quién lo detectó | Cómo quedó corregido |
|---|---|---|---|
| 1 | Dejó el import no utilizado `com.taskflow.model.Project` en `ReportController.java` | Copilot code review | Se eliminó el import mediante la corrección de PF-6; la suite quedó en 79 tests y `BUILD SUCCESS`. |
| 2 | `ProgresoProyectosControllerTest.java` no verificaba todos los campos de `ProjectProgressResponse`; faltaban, entre otros, `projectName` y `doneTasks` | Copilot code review | Se ampliaron los `jsonPath` para comprobar `projectId`, `projectName`, `totalTasks`, `doneTasks` y `percentDone`. |
| 3 | Copilot recomendó agregar `/reports/progress` a `SecurityRulesTest.java` para comprobar el 401 | Copilot code review | No se aplicó: `SecurityRulesTest.java` ya existía en `main` y la práctica prohíbe modificar tests preexistentes. El 401 se comprobó mediante `casos-progress.ps1` contra la aplicación real. |

**Lo que el agente hizo bien a la primera** (una o dos líneas): `La implementación inicial creó correctamente el DTO, mapper, service y controller de `GET /reports/progress`, además de las dos clases de test nuevas exigidas por la spec. El agente revisor terminó con `Veredicto: APROBADO`.`

## 5. Comprobaciones REST

```text
PS C:\Users\User\taskflow-copilot-aarodriguezperez> pwsh -NoProfile -File .github\skills\verificar-taskflow\verificar.ps1
Repositorio: C:\Users\User\taskflow-copilot-aarodriguezperez
URL de la app: http://127.0.0.1:8080
Empaquetando con Maven (mvn -q package -DskipTests), tarda unos segundos...
App arrancando (PID 11896). Esperando a que /info responda...
App lista en 9 s.
[OK]    GET /tasks/overdue devuelve solo la tarea 7
[OK]    GET /tasks/unassigned devuelve las tareas 4 y 6
[OK]    GET /projects/1/summary
[OK]    GET /projects/2/summary
[OK]    GET /projects/3/summary
[OK]    GET /projects/99/summary responde 404
[OK]    GET /projects/1/summary sin token responde 401
[OK]    GET /reports/progress devuelve los 3 proyectos con la semilla
[OK]    El proyecto 1 se llama «Plataforma TaskFlow»
[OK]    Tras pasar la tarea 1 a DONE, el proyecto 1 queda en 2/5 = 40
[OK]    GET /reports/progress sin token responde 401
App detenida (PID 11896).
[OK]    App apagada: el puerto 8080 ya no responde
RESULTADO: 12/12 OK
```

## 6. Créditos de la semana

| Qué | AI credits |
|---|---|
| Usados en septiembre según github.com (incluye semanas anteriores si usaste Copilot antes) | `147` |
| Implementación con la skill (`AI Credits` del PF-2) | `2.99` |
| Revisión del `revisor` (`AI Credits` del PF-3) | `2.47` |
| Correcciones del PF-4 y del PF-6, si hubo (`AI Credits`) | `1.99` |
