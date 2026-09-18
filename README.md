# TaskFlow · GitHub Copilot · Semana 6

Repositorio de trabajo de la **Semana 6 de la Academia Backend / QE**, enfocado en el uso de **GitHub Copilot** dentro de un proyecto Java existente.

Durante la semana se trabajó con GitHub Copilot CLI, VS Code, instrucciones de repositorio, skills, agentes personalizados, Model Context Protocol (MCP), revisión de código y un proyecto final integrado mediante Pull Request.

---

## Alumno

**Alberto Alejandro Rodríguez Pérez**  
GitHub: [@aarodriguezperez](https://github.com/aarodriguezperez)

---

## Evidencia principal

El reporte general de la semana se encuentra en:

### [EVIDENCIA-SEMANA-6.md](EVIDENCIA-SEMANA-6.md)

Este documento resume los cinco días e incluye:

- qué se construyó;
- dónde se encuentra en el repositorio;
- cómo se comprobó;
- qué problemas aparecieron y cómo se resolvieron;
- capturas representativas;
- referencias a evidencia reproducible.

---

## Proyecto final

La feature elegida para el proyecto final fue:

```text
GET /reports/progress
```

El endpoint devuelve el avance de cada proyecto a partir del porcentaje de tareas en estado `DONE`.

### Resultado final

- Pull Request: [#5 · feat: progress (proyecto final)](https://github.com/aarodriguezperez/taskflow-copilot-aarodriguezperez/pull/5)
- Verificación REST: `12/12 OK`
- Suite final: `79 tests`
- Failures: `0`
- Errors: `0`
- Estado del PR: `Merged`

La documentación específica del proyecto final se encuentra en:

### [semana6/README.md](semana6/README.md)

---

## Trabajo realizado

| Día | Tema principal | Resultado |
|---|---|---|
| Día 1 | GitHub Copilot CLI | Configuración del repositorio, instrucciones y documentación de arquitectura |
| Día 2 | Especificar, implementar y revisar | `GET /tasks/overdue` y `GET /tasks/unassigned` |
| Día 3 | Model Context Protocol | Servidor MCP propio de TaskFlow, GitHub MCP, AWS Knowledge y Playwright |
| Día 4 | Skills y agentes | Skills reutilizables, agentes `revisor` y `tester`, `GET /projects/{id}/summary` |
| Día 5 | VS Code y proyecto final | MCP en VS Code y `GET /reports/progress` |

---

## Documentación paso a paso

El recorrido detallado de cada día se conserva en:

- [Día 1 · GitHub Copilot CLI](docs/dia1.md)
- [Día 2 · Especificar, implementar y revisar](docs/dia2.md)
- [Día 3 · MCP](docs/dia3.md)
- [Día 4 · Skills y agentes](docs/dia4.md)
- [Día 5 · VS Code y proyecto final](docs/dia5.md)

La documentación de arquitectura generada y posteriormente verificada se encuentra en:

- [Arquitectura de TaskFlow](docs/ARQUITECTURA.md)

---

## Evidencia

La evidencia técnica y las capturas están organizadas por día:

```text
evidencia/
├── dia1/
├── dia2/
├── dia3/
├── dia4/
└── dia5/
```

Cada carpeta contiene archivos que permiten comprobar los resultados sin depender únicamente de la respuesta del modelo.

Entre ellos se encuentran:

- resultados de tests;
- verificaciones REST;
- transcripts;
- revisiones;
- resultados de scripts;
- conteos;
- capturas de pantalla.

---

## Componentes principales

```text
.github/
├── copilot-instructions.md
├── agents/
│   ├── revisor.agent.md
│   └── tester.agent.md
└── skills/
    ├── crear-endpoint-taskflow/
    └── verificar-taskflow/

docs/
├── ARQUITECTURA.md
├── dia1.md
├── dia2.md
├── dia3.md
├── dia4.md
└── dia5.md

evidencia/
├── dia1/
├── dia2/
├── dia3/
├── dia4/
└── dia5/

issues/
└── summary.md

specs/
├── overdue.md
├── unassigned.md
├── summary.md
└── progress.md

taskflow-mcp/
└── servidor MCP de TaskFlow en Java

semana6/
├── README.md
├── code-review.md
├── proyecto-final.diff
├── revision.md
└── sesion-implementacion.md
```

---

## Principales aprendizajes

Durante la semana se aplicó un flujo de trabajo en el que GitHub Copilot se utilizó como herramienta de apoyo, manteniendo la verificación y la decisión final del lado del desarrollador.

Entre los principales aprendizajes estuvieron:

- especificar el comportamiento antes de pedir una implementación;
- revisar el alcance real de los cambios del agente;
- comprobar que un test falle cuando desaparece la regla que pretende proteger;
- utilizar permisos mínimos para ejecutar herramientas;
- diferenciar datos externos de instrucciones confiables;
- auditar transcripts cuando se utilizan servidores MCP;
- separar responsabilidades mediante skills y agentes personalizados;
- comprobar el comportamiento real mediante REST además de los tests;
- revisar cada sugerencia de Code Review antes de aplicarla.

---

## Estado final

Al finalizar la semana:

- las features desarrolladas quedaron integradas a `main`;
- el proyecto final `GET /reports/progress` quedó mergeado mediante el PR `#5`;
- la suite final quedó en `79 tests` con `0` fallos;
- la verificación REST del proyecto final terminó en `12/12 OK`;
- la evidencia de los cinco días quedó organizada dentro de `evidencia/`;
- la documentación detallada quedó disponible dentro de `docs/`;
- el reporte principal de la semana quedó en `EVIDENCIA-SEMANA-6.md`.

---

## Repositorio

**GitHub:**  
https://github.com/aarodriguezperez/taskflow-copilot-aarodriguezperez
