# HALLAZGOS

## auth

### Contrato observado de `POST /auth/login`

- Ruta JSON observada: `POST /auth/login`; recibe `username`, `password` y `client` (`client` no se usa en el flujo). Evidencia actual: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:14`, `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:26`, `auth/src/main/java/com/bcp/workshop/auth/LoginRequest.java:3`.
- Validacion observada y conservada: request nulo, username nulo/vacio o password nulo/vacio devuelve 400. Evidencia actual: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:28`, `auth/src/main/java/com/bcp/workshop/auth/AuthService.java:20`, `auth/src/main/java/com/bcp/workshop/auth/AuthService.java:23`.
- Persistencia: valida username con `PreparedStatement` y compara el hash; sin coincidencia, devuelve 401. Evidencia actual: `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:155`, `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:158`, `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:161`.
- Exito observado y conservado en el sobre HTTP: status 200 con `token`, `expiresIn`, `user` y `debug`. Evidencia actual: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:36`, `auth/src/main/java/com/bcp/workshop/auth/AuthService.java:42`, `auth/src/main/java/com/bcp/workshop/auth/LoginResponse.java:3`.
- Error 500 antes del cambio: el cuerpo exponia mensaje y clase de excepcion; evidencia historica del codigo previo: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:54`. El comportamiento acordado ahora conserva status/JSON y devuelve solo `Authentication failed`; evidencia actual: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:41`.
- Contrato acordado tras aprobacion humana: ruta, status 400/401/200, campos de exito y valores observables se conservan; el token interno ahora es JWT y el cuerpo 500 es generico. `docs/ENDPOINTS.md` no existe; detalles y riesgos pendientes registrados en `PLAN.md`. El Discovery original no ejecuto pruebas.

### Matriz de seguimiento

| id | componente | hallazgo | tipo | explicacion | evidencia (archivo:linea) | severidad | estado |
|---|---|---|---|---|---|---|---|
| H-AUTH-01 | auth | Passwords almacenadas en texto plano | Vulnerabilidad | Resuelto: la columna legacy se migra transaccionalmente a `password_hash` con PBKDF2 y salt aleatorio; las cuentas iniciales tambien se guardan con hash. Se verifico migracion, login, base limpia e inicializacion repetida. | `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:23`, `auth/src/main/java/com/bcp/workshop/auth/PasswordHashService.java:21` | Alta | Resuelto |
| H-AUTH-02 | auth | Token Base64 con secreto estatico y sin firma | Vulnerabilidad | Resuelto: el emisor ahora genera JWT firmado con `sub`, grupo/rol y expiracion de una hora. La clave privada se carga desde `AUTH_JWT_PRIVATE_KEY_LOCATION`; la firma y claims se verifican con una clave efimera de prueba. No se inspeccionaron consumidores fuera de `auth/`, por lo que su migracion sigue siendo una coordinacion externa. | `auth/src/main/java/com/bcp/workshop/auth/TokenService.java:10`, `auth/src/main/java/com/bcp/workshop/auth/TokenService.java:15`, `auth/src/main/resources/application.properties:9` | Alta | Resuelto |
| H-AUTH-03 | auth | Configuracion de secreto y CORS demasiado amplia | Vulnerabilidad | Resuelto: origenes CORS configurables mediante `AUTH_CORS_ORIGINS` con `http://localhost:4200` como baseline local; la clave JWT es externa y `TokenService` falla al iniciar si falta su ubicacion. Los despliegues deben configurar sus origenes y clave propios; no se inspeccionaron consumidores externos. | `auth/src/main/resources/application.properties:5`, `auth/src/main/resources/application.properties:9`, `auth/src/main/java/com/bcp/workshop/auth/TokenService.java:21` | Media | Resuelto |
| H-AUTH-04 | auth | Respuesta de error expone detalles internos | Vulnerabilidad | Resuelto: el 500 mantiene JSON y status, devuelve solo `Authentication failed` y registra el detalle en el log del servidor; la prueba confirma que el cuerpo no incluye el mensaje ni la clase de excepcion ficticia. | `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:40`, `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:41` | Media | Resuelto |
| H-AUTH-05 | auth | Evaluacion de advisories de dependencias pendiente; versiones candidatas presentes, CVE no confirmado | Vulnerabilidad | Resuelto con evidencia: Quarkus 3.27.4 se actualizo a 3.27.5.1, parche indicado por GHSA-xrmg-9xc7-83fp (CVE-2026-16308); Commons Text 1.9 se actualizo a 1.10.0, version que corrige CVE-2022-42889. El arbol local confirma las versiones resueltas. CodeQL y Dependency Review remotos no ejecutados. Fuentes: https://github.com/quarkusio/quarkus/security/advisories/GHSA-xrmg-9xc7-83fp y https://commons.apache.org/proper/commons-text/security.html. | `auth/pom.xml:13`, `auth/pom.xml:54` | Media | Resuelto |
| H-AUTH-06 | auth | AuthResource concentra validacion, acceso a persistencia, creacion del token y respuesta | Refactor | Resuelto: `AuthResource` queda como adaptador HTTP; `AuthService` valida y coordina persistencia/token y devuelve un DTO tipado, mientras `LegacyDatabase` conserva el acceso SQLite parametrizado. El contrato de status y campos JSON se cubre en pruebas. | `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:33`, `auth/src/main/java/com/bcp/workshop/auth/AuthService.java:19`, `auth/src/main/java/com/bcp/workshop/auth/LoginResponse.java:3` | Baja | Resuelto |
| H-AUTH-07 | auth | Concatenacion de credenciales en SQL: no encontrada en el estado actual | Sugerencia | El candidato de RETO no aparece en la consulta revisada: usa placeholders y enlaza username mediante `PreparedStatement`; la verificacion del hash ocurre despues de leer la fila. No se observa concatenacion de credenciales en SQL. | `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:158`, `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:161` | Baja | No encontrado |

### Candidatos de discovery

#### H-AUTH-08 - Credenciales predefinidas para cuentas iniciales
- Estado: Implementado; pendiente de revisión del PR.
- Evidencia: `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:25` contiene una credencial predefinida; el metodo `@PostConstruct initialize` inserta las cuentas iniciales sin una condicion de perfil en `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:42` y `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:49`.
- Impacto: si este arranque se usa fuera de un entorno aislado, las cuentas conocidas por estar definidas en el repositorio podrian permitir autenticacion no autorizada.
- Prioridad propuesta: Alta, pendiente de confirmar el uso en despliegues.
- Contrato potencialmente afectado: politica de autenticacion de `POST /auth/login` y ciclo de vida de cuentas iniciales.
- Como validarlo: en una base efimera y una configuracion equivalente a produccion, verificar si el arranque crea las cuentas y si pueden autenticarse; confirmar separadamente que los despliegues reales no dependen de esas cuentas. No usar credenciales reales.
- Evidencia de implementacion: `LegacyDatabase` solo siembra cuentas cuando `auth.seed-initial-users` esta habilitado; la opcion esta desactivada por defecto y habilitada para los perfiles dev y test. `mvn verify` desde `auth/`: 22 pruebas, 0 fallos/errores; JaCoCo para `LegacyDatabase`: 83/91 lineas cubiertas (91.2%).

#### H-AUTH-09 - El campo `client` de la solicitud no se usa en la autenticacion
- Estado: CANDIDATO
- Evidencia: `auth/src/main/java/com/bcp/workshop/auth/LoginRequest.java:6` declara `client`, pero `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:33` solo pasa username y password; el JWT se construye con subject, grupo, emision y expiracion en `auth/src/main/java/com/bcp/workshop/auth/TokenService.java:30`.
- Impacto: si el contrato requiere distinguir o limitar tokens por cliente, el valor enviado no influye en la autenticacion ni en el token emitido.
- Prioridad propuesta: Media, condicionada a confirmar el contrato esperado para `client`.
- Contrato potencialmente afectado: cuerpo de solicitud y claims del token de `POST /auth/login`.
- Como validarlo: confirmar el contrato con el responsable y, en pruebas aisladas, enviar dos valores de cliente y comparar los claims resultantes; verificar el rechazo de valores invalidos si el contrato define validacion.

#### H-AUTH-10 - La respuesta exitosa expone un marcador interno `debug`
- Estado: CANDIDATO
- Evidencia: `auth/src/main/java/com/bcp/workshop/auth/AuthService.java:11` define el marcador `DEBUG_FLOW` y `auth/src/main/java/com/bcp/workshop/auth/AuthService.java:47` lo incluye en la respuesta; el DTO publico lo declara en `auth/src/main/java/com/bcp/workshop/auth/LoginResponse.java:3`.
- Impacto: el endpoint expone un detalle de implementacion y convierte el marcador en parte observable del contrato, lo que puede revelar informacion interna y condicionar clientes al campo.
- Prioridad propuesta: Baja, pendiente de confirmar si el campo es intencional y esta documentado.
- Contrato potencialmente afectado: esquema JSON de exito de `POST /auth/login`.
- Como validarlo: comparar el esquema de respuesta con el contrato aprobado y revisar consumidores del endpoint antes de decidir si conservar o retirar el campo.
