# Arquitectura de TaskFlow

Documento dirigido a un desarrollador nuevo en el proyecto TaskFlow. Explica las capas, el flujo de `POST /projects/{projectId}/tasks`, dónde viven las reglas de negocio, cómo funciona la seguridad con JWT y la organización de los tests.

---

## 1. Capas y paquetes

La aplicación sigue una arquitectura en capas con el paquete raíz `com.taskflow`:

- Capa de presentación (HTTP): controladores que reciben peticiones y devuelven DTOs.
  - Ejemplos: `ProjectController`, `TaskController`.
  - Rutas y archivos: `src/main/java/com/taskflow/controller/ProjectController.java`, `src/main/java/com/taskflow/controller/TaskController.java`, `src/main/java/com/taskflow/controller/AuthController.java`.

- Capa de servicio (casos de uso): orquesta la lógica del negocio usando repositorios y entidades.
  - Ejemplo clave: `TaskService`.
  - Archivo: `src/main/java/com/taskflow/service/TaskService.java`.

- Capa de persistencia (repositorios): interfaces Spring Data JPA que acceden a la base de datos.
  - Ejemplos: `TaskRepository`, `ProjectRepository`, `UserRepository`.
  - Rutas: `src/main/java/com/taskflow/repository/TaskRepository.java`, `src/main/java/com/taskflow/repository/ProjectRepository.java`.

- Capa de modelo (entidades y reglas de dominio): entidades JPA con comportamiento y validaciones de dominio.
  - Ejemplo: `Task` (modelo con lógica de creación, vencimiento y estado).
  - Archivo: `src/main/java/com/taskflow/model/Task.java`.

- Mappers y DTOs: convierten entre entidades y DTOs expuestos por los controladores.
  - Ejemplo: `TaskMapper` con método `aResponse`.
  - Archivo: `src/main/java/com/taskflow/mapper/TaskMapper.java`.

- Configuración y seguridad: filtros, configuración de Spring Security y clases de seguridad específicas.
  - Ejemplos: `SecurityConfig`, `JwtAuthenticationFilter`, `ProjectSecurity`.
  - Rutas: `src/main/java/com/taskflow/config/SecurityConfig.java`, `src/main/java/com/taskflow/security/JwtAuthenticationFilter.java`, `src/main/java/com/taskflow/security/ProjectSecurity.java`.

- Manejo de errores y arranque de datos:
  - `GlobalExceptionHandler` centraliza respuestas de error.
  - `DataSeeder` poblado inicial (perfil `h2`).
- Rutas: `src/main/java/com/taskflow/advice/GlobalExceptionHandler.java`, `src/main/java/com/taskflow/config/DataSeeder.java`.

---

## 2. Recorrido de POST /projects/{projectId}/tasks (de arriba abajo)

Se describe el flujo típico cuando un cliente hace `POST /projects/{projectId}/tasks` para crear una tarea.

1. Petición HTTP llega al controlador:
   - Endpoint en `TaskController`: `src/main/java/com/taskflow/controller/TaskController.java`.
   - El método recibe un `@RequestBody` con `TaskRequest` y está anotado con `@Valid`.

2. Comprobación de existencia del proyecto:
   - `TaskController.createTask` llama a `ProjectService.buscarPorId(projectId)` y, si no existe, lanza `ProjectNotFoundException` (mapped a 404 por el advice).

3. Conversión y creación:
   - `TaskController` delega en `TaskService.crear(request, projectId)`.
   - `TaskService.crear` convierte el DTO en entidad nueva llamando a `TaskMapper.aEntidadNueva(request, projectId)`. Ese método pasa por la factory `Task.crear` que aplica las reglas de dominio (por ejemplo, dueDate no en el pasado) y fija `status=TODO`.

4. Persistencia:
   - `TaskService.crear` guarda la entidad con `TaskRepository.save(...)`, que persiste la fila y asigna el `id`.

5. Respuesta:
   - `TaskController` construye la cabecera `Location` con `/tasks/{id}` y responde `201 Created` con el cuerpo obtenido por `TaskMapper.aResponse(creada)`.

6. Errores y excepciones:
   - Errores de validación del DTO producen `MethodArgumentNotValidException` (400 por el advice).
   - Las violaciones de reglas de dominio lanzadas por `Task.crear` suben como `TaskValidationException` y son manejadas por el advice (400). 404 y 422 se mapean según la excepción lanzada.

7. Errores y excepciones:
   - Si hay errores de validación se lanza `MethodArgumentNotValidException` → procesado por `GlobalExceptionHandler` (`src/main/java/com/taskflow/advice/GlobalExceptionHandler.java`) que devuelve 400.
   - Si no se encuentra `projectId` → 404; si la acción viola reglas de dominio → 422; duplicados → 409. Los mapeos están centralizados en `GlobalExceptionHandler`.

---

## 3. Dónde viven las reglas de negocio

- Reglas de invariantes y comportamiento (reglas ricas de dominio) viven dentro de las entidades del paquete `model`.
  - Ejemplo: `Task` contiene `crear(...)`, `estaVencida()`, y las reglas para cambiar de estado.

- Reglas orquestadas (coordinación entre repositorios, validaciones de existencia, envío de eventos) viven en `service`.
  - Ejemplo: `TaskService` comprueba que el proyecto exista, aplica reglas del `Task` y persiste.

- Reglas de seguridad y permisos específicos (por ejemplo, quién puede borrar un proyecto) están en `security` y se usan desde controladores con `@PreAuthorize` y `ProjectSecurity`.
  - Ejemplo: `ProjectSecurity` contiene lógica que decide si el usuario autenticado es dueño o tiene rol `ADMIN`.

Regla general: reutilizar código del modelo (`Task`) desde los servicios; no duplicar reglas en controladores.

---

## 4. Seguridad con JWT

- La aplicación usa JWT sin estado (stateless). La configuración principal está en `SecurityConfig` (`src/main/java/com/taskflow/config/SecurityConfig.java`).

- Flujo básico:
  - El cliente obtiene un token mediante `POST /auth/login` (controlador `AuthController`).
  - Para peticiones autenticadas, el token se envía en `Authorization: Bearer <token>`.
  - Un filtro de seguridad (`JwtAuthenticationFilter` o clase similar en `src/main/java/com/taskflow/security/JwtAuthenticationFilter.java`) intercepta la petición, valida la firma y la expiración, y construye un `Authentication` con los detalles del usuario.
  - El `SecurityContext` se popula con el usuario autenticado para que `@PreAuthorize` y `ProjectSecurity` puedan evaluar permisos.

- Endpoints públicos: `/auth/**`, `/info`, la consola H2 y los recursos de Swagger/UI están abiertos.

- Autorización fina:
  - Uso de `@PreAuthorize` en controladores con llamadas a `ProjectSecurity` para decisiones de propietarios/roles.
  - Ejemplo: borrar proyecto sólo para el dueño o `ADMIN` (controlado por `ProjectSecurity`).

---

## 5. Organización de tests

Estructura de tests bajo `src/test/java/com/taskflow/...` con tres niveles principales:

- Unit tests (sin Spring): prueban clases del dominio y servicios puros con Mockito.
- Ejemplo: `src/test/java/com/taskflow/unit/TaskServiceTest.java`.
  - Ejecutar: `mvn -q test "-Dtest=TaskServiceTest"`.

- Slice tests (pruebas de capa): `@WebMvcTest` para controladores o `@DataJpaTest` para repositorios.
- Ejemplo: `src/test/java/com/taskflow/slice/ProjectControllerTest.java`.

- Integration tests: `@SpringBootTest` con perfil `test` y (opcional) Testcontainers para bases externas.
  - Notas: los tests `*IT.java` usan Testcontainers y por defecto no se ejecutan con `mvn test` salvo que se pase `-Ddocker.tests=true`.
  - Ejemplo: `src/test/java/com/taskflow/integration/AuthControllerTest.java`.

- Convenciones y comandos:
  - Ejecutar toda la suite: `mvn -q test`.
  - Ejecutar una clase específica: `mvn -q test "-Dtest=TaskServiceTest"`.
  - Ejecutar la aplicación con H2 (datos de ejemplo): `mvn spring-boot:run "-Dspring-boot.run.profiles=h2"`.

---

## 6. Buenas prácticas al trabajar en TaskFlow

- Reutilizar las reglas del modelo (`Task`) desde los servicios; no duplicar lógica.
- Los controladores solo deben transformar DTOs y orquestar llamadas a servicios.
- No añadir un GlobalExceptionHandler genérico para `Exception` (sigue la convención del proyecto).
- Tests: preferir unitarios para lógica de dominio; las integraciones funcionan con el perfil `test` y Testcontainers solo cuando está habilitado.

---

Si hay que ampliar ejemplos de clases concretas o añadir diagramas, indicarlo y se agrega material complementario.
Las fechas límite se validan en `Task.crear` (`src/main/java/com/taskflow/model/Task.java`).
