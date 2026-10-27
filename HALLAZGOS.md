# Hallazgos

## auth — Discovery

### Flujo y contrato observado

`docs/ENDPOINTS.md` no existe. Por ello, este contrato se deriva del código y queda pendiente de acuerdo:

1. `POST /auth/login` consume JSON y deserializa `username`, `password` y `client` en `LoginRequest` (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:13-25`; `auth/src/main/java/com/bcp/workshop/auth/LoginRequest.java:3-6`).
2. El recurso valida que el request, el usuario y la contraseña no sean nulos o vacíos; responde `400` en esos casos (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:26-35`).
3. El recurso delega en `AuthService`, que consulta `AuthRepository`, verifica el hash y genera el token firmado (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:38-40`; `auth/src/main/java/com/bcp/workshop/auth/AuthService.java:24-37`).
4. Si no encuentra usuario responde `401`; si lo encuentra responde `200` con `token`, `expiresIn`, `user` y `debug` (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:38-40,56-60`; `auth/src/main/java/com/bcp/workshop/auth/LoginResponse.java:3`).
5. La configuración HTTP, CORS y secreto externo está en `application.properties`; SQLite conserva una ruta local configurable por `AUTH_DB_URL` (`auth/src/main/resources/application.properties:1-9`; `auth/src/main/java/com/bcp/workshop/auth/SqliteAuthRepository.java:34-39`).

### Matriz de seguimiento

| id | componente | hallazgo | tipo | explicación | evidencia (archivo:línea) | severidad | estado |
|---|---|---|---|---|---|---|---|
| H-AUTH-01 | auth | Credenciales concatenadas en una consulta SQL | Vulnerabilidad | La entrada de usuario y contraseña se incorporaba al SQL mediante concatenación. La búsqueda actual usa SQL constante y `PreparedStatement`. | `auth/src/main/java/com/bcp/workshop/auth/SqliteAuthRepository.java:27,60-70` | alta | Resuelto |
| H-AUTH-02 | auth | Contraseñas almacenadas y comparadas en texto plano | Vulnerabilidad | Las contraseñas se migran y persisten como PBKDF2 con salt; la verificación ocurre fuera del SQL. | `auth/src/main/java/com/bcp/workshop/auth/PasswordHasher.java:13-52`; `auth/src/main/java/com/bcp/workshop/auth/SqliteAuthRepository.java:84-119`; `auth/src/main/java/com/bcp/workshop/auth/AuthService.java:24-27` | alta | Resuelto |
| H-AUTH-03 | auth | Token reversible en Base64 con secreto embebido | Vulnerabilidad | El token ahora está firmado con HMAC-SHA-256, contiene claims y expiración, y rechaza alteraciones o tokens vencidos. | `auth/src/main/java/com/bcp/workshop/auth/TokenService.java:19-105` | alta | Resuelto |
| H-AUTH-04 | auth | Secreto embebido y CORS permitido para cualquier origen | Vulnerabilidad | La clave se exige mediante `AUTH_TOKEN_SECRET`; CORS acepta por defecto solo el origen local y puede configurarse por ambiente. | `auth/src/main/resources/application.properties:4-9`; `auth/src/test/resources/application.properties:1` | alta | Resuelto |
| H-AUTH-05 | auth | Respuestas de error exponen detalles internos | Vulnerabilidad | El cliente recibe un mensaje `500` genérico y el detalle queda en logging controlado del servidor. | `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:18-19,41-46`; `auth/src/test/java/com/bcp/workshop/auth/AuthErrorResponseTest.java:12-38` | media | Resuelto |
| H-AUTH-06 | auth | El recurso concentra validación, negocio, persistencia y construcción de respuesta | Refactor | El flujo quedó separado en Resource, Service, Repository y DTOs, con guard clauses y responsabilidades acotadas. | `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:17-61`; `auth/src/main/java/com/bcp/workshop/auth/AuthService.java:10-38`; `auth/src/main/java/com/bcp/workshop/auth/AuthRepository.java:5-7`; `auth/src/main/java/com/bcp/workshop/auth/SqliteAuthRepository.java:21-124` | media | Resuelto |
| H-AUTH-07 | auth | Versiones de Quarkus y Commons Text requieren análisis SCA | Sugerencia | Quarkus migró a la LTS `3.40.1.1`, Commons Text permanece en `1.15.0` y la regresión pasó. El SCA local no pudo actualizar NVD sin API key y los controles remotos no se ejecutaron, por lo que falta evidencia para cerrar el riesgo. | `auth/pom.xml:11-14,47-55`; `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:29-44`; `docs/TESTING.md` | baja | Pendiente |

### Notas de resolución

- H-AUTH-01: consulta parametrizada y prueba con comilla sin error SQL.
- H-AUTH-02: PBKDF2 con salt, migración local y pruebas de verificación y no persistencia en claro.
- H-AUTH-03: token JWT compacto firmado, claims explícitos, expiración y pruebas de alteración.
- H-AUTH-04: secreto externo, fixture ficticio de prueba y CORS restringido/configurable.
- H-AUTH-05: error genérico al cliente y detalle únicamente en el log del servidor.
- H-AUTH-06: separación Resource → Service → Repository con DTOs propios.
- H-AUTH-07: migración local a Quarkus `3.40.1.1` y caracterización antes/después correctas; `mvn verify` pasó 26/26. Sigue pendiente porque OWASP Dependency-Check falló al actualizar NVD sin API key y CodeQL/Dependency Review no se ejecutaron.
