# PLAN

## auth

### Alcance y contrato

Este plan cubre solo H-AUTH-01 a H-AUTH-06, que figuran como `Pendiente` en `HALLAZGOS.md`. H-AUTH-07 esta `No encontrado` y queda fuera. No existe `docs/ENDPOINTS.md`; el contrato disponible es el observado en `HALLAZGOS.md`, no uno acordado. No se modificara codigo hasta revisar y aprobar los cambios de comportamiento indicados abajo.

La caracterizacion debe conservar `POST /auth/login`, los status 400/401/200 observados, los campos de la respuesta exitosa (`token`, `expiresIn`, `user`, `debug`) y sus valores observables. Dos correcciones propuestas plantean decisiones de compatibilidad: firmar el contenido del token y dejar de devolver detalles de excepciones. El cuerpo 500 actual y el formato interno del token no se cambiaran silenciosamente; hace falta acordar sus contratos antes de implementar esas correcciones. No se inspeccionaron consumidores fuera de `auth/`.

### H-AUTH-01 - Passwords almacenadas en texto plano

- **Hallazgo:** H-AUTH-01, vulnerabilidad alta, `Pendiente` en `HALLAZGOS.md`.
- **Problema:** `legacy_users.password` almacena credenciales directamente y el lookup compara el password recibido contra ese valor. Una copia de la base expone las credenciales; cambiar solo la consulta no corrige el almacenamiento. Evidencia: `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:23-25`, `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:32-37`.
- **Cambio propuesto:** guardar un hash lento con salt y verificarlo en el flujo de login. Migrar las filas existentes de forma transaccional e idempotente, retirar la columna en texto plano y generar los hashes de las cuentas ficticias al inicializar una base nueva. Validar primero una API mantenida del ecosistema Quarkus (por ejemplo, `BcryptUtil` del artefacto `quarkus-elytron-security-common`); justificar y revisar el artefacto/version administrada por el BOM antes de agregarlo. No implementar un algoritmo criptografico propio ni registrar passwords o hashes.
- **Archivos a tocar:** `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java`, `auth/src/main/java/com/bcp/workshop/auth/UserRecord.java`, `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java`; nuevo `auth/src/main/java/com/bcp/workshop/auth/PasswordHashService.java` si la verificacion queda encapsulada; `auth/pom.xml` solo si se confirma la dependencia; `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java` y pruebas de persistencia en `auth/src/test/java/com/bcp/workshop/auth/`.
- **Caracterizacion antes de tocar produccion:** ampliar las pruebas HTTP para login valido de student y trainer, password incorrecto (401), username inexistente (401), username/password vacios (400) y conservar campos/status de exito. En una base SQLite temporal bajo `auth/target`, crear una fila con el esquema legacy y verificar que la migracion preserve el login, elimine el valor plano, sea idempotente y rechace un password incorrecto. No usar la base de desarrollo ni credenciales reales.
- **Verificacion posterior propuesta:** repetir casos HTTP; comprobar que la columna/hash persistido no contiene el password; probar upgrade desde esquema legacy, inicializacion limpia y segunda inicializacion; ejecutar `mvn -B verify` desde `auth/` y registrar cobertura del codigo modificado, no solo cobertura global.
- **Cambio Aprobado:** Si
- **Implementacion ejecutada:** se agrego `PasswordHashService` con PBKDF2/HMAC-SHA256 del JDK, salt aleatorio y comparacion constante; `LegacyDatabase` migra la tabla legacy en transaccion, elimina la columna `password`, guarda `password_hash` y verifica el password recibido. No se agrego dependencia.
- **Caracterizacion antes del cambio:** `H-AUTH-01` / auth: `mvn -B -Dtest=AuthResourceTest test`, con SQLite aislada en `auth/target/h-auth-20261104-h01.db`; 14 pruebas, 0 fallos, 0 errores. Incluye student/trainer validos, credenciales incorrectas, campos nulos/vacios y una asercion de password legacy en claro.
- **Prueba despues del cambio:** misma suite y misma DB legacy; 14 pruebas, 0 fallos, 0 errores. Confirma login compatible y migracion a `password_hash` sin columna `password`. Se repetira para verificar idempotencia.
- **Desviacion:** se uso PBKDF2/HMAC-SHA256 de la JDK en lugar de agregar BcryptUtil; evita dependencia adicional. Se conserva hash lento con salt.

### H-AUTH-02 - Token Base64 con secreto estatico y sin firma

- **Hallazgo:** H-AUTH-02, vulnerabilidad alta, `Pendiente` en `HALLAZGOS.md`.
- **Problema:** el token concatena username, role, expiracion y secreto antes de codificar en Base64; Base64 no firma ni protege integridad. Evidencia: `auth/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java:8-15`; emision desde `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:39-43`.
- **Cambio propuesto:** sustituir el formato casero por un JWT firmado y verificable, con `sub`, role/grupo y `exp` explicitos; mantener el sobre HTTP y `expiresIn` en 3600 segundos. Evaluar el builder SmallRye JWT administrado por Quarkus (`quarkus-smallrye-jwt-build`) y revisar advisories/versiones antes de agregarlo. La clave privada debe venir de configuracion externa y no almacenarse en Git. La representacion del token cambia; detener la implementacion hasta acordar que no hay consumidor dependiente del formato Base64 actual o hasta coordinar su migracion.
- **Archivos a tocar:** `auth/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java` o un nuevo servicio de tokens en `auth/src/main/java/com/bcp/workshop/auth/`; `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java`; `auth/pom.xml` si se justifica la extension; `auth/src/main/resources/application.properties`; `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java` y una prueba de token en `auth/src/test/java/com/bcp/workshop/auth/`.
- **Caracterizacion antes de tocar produccion:** fijar las aserciones HTTP existentes: token no nulo, `expiresIn=3600`, usuario y role; registrar en una prueba aislada el formato/claims del token actual para que el impacto de compatibilidad sea explicito. No guardar claves reales ni convertir la prueba legacy en el contrato final sin acuerdo.
- **Verificacion posterior propuesta:** verificar firma con clave publica de prueba efimera, `sub`, role/grupo y expiracion; probar que token manipulado o expirado no valida con el verificador de prueba; comprobar que la respuesta conserva campos/status. Las claves de prueba se generan o almacenan solo en fixtures de prueba aislados.
- **Cambio Aprobado:** Si

### H-AUTH-03 - Secreto fijo y CORS demasiado amplio

- **Hallazgo:** H-AUTH-03, vulnerabilidad media, `Pendiente` en `HALLAZGOS.md`.
- **Problema:** el secreto esta embebido en Java y CORS acepta todos los origenes (`/.*/`), lo que impide rotacion por entorno y amplia origenes web que pueden interactuar con el servicio. Evidencia: `auth/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java:8`, `auth/src/main/resources/application.properties:4-5`.
- **Cambio propuesto:** mover la clave de firma a una fuente externa configurada (sin incluirla en propiedades versionadas) y definir una lista de origenes CORS configurable y acotada por entorno. Como baseline local, evaluar el origen de la UI documentado en `README.md`; no asumir origenes de produccion. La reduccion desde cualquier origen es un cambio observable y requiere confirmar los consumidores permitidos.
- **Archivos a tocar:** `auth/src/main/resources/application.properties`, `auth/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java` o el servicio de tokens acordado en H-AUTH-02, y pruebas de configuracion en `auth/src/test/java/com/bcp/workshop/auth/`. `auth/pom.xml` solo si la solucion de configuracion requiere una extension justificada.
- **Caracterizacion antes de tocar produccion:** probar el comportamiento CORS actual con un origen ficticio no sensible y el origen local documentado; registrar los headers actuales y comprobar que el test corre contra perfil de prueba. Verificar que la configuracion actual no permite probar rotacion del secreto sin cambiar codigo.
- **Verificacion posterior propuesta:** origen local permitido recibe los headers necesarios; un origen fuera de la lista no recibe `Access-Control-Allow-Origin`; cambiar la clave por variables/configuracion de prueba rota la firma sin recompilar. No imprimir ni registrar claves. Coordinar con consumidores antes de restringir CORS.
- **Cambio Aprobado:** Si

### H-AUTH-04 - Respuesta de error expone detalles internos

- **Hallazgo:** H-AUTH-04, vulnerabilidad media, `Pendiente` en `HALLAZGOS.md`.
- **Problema:** el catch del recurso copia el mensaje y el nombre de clase de la excepcion a la respuesta 500. Evidencia: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:54-58`.
- **Cambio propuesto:** conservar status 500 y respuesta JSON, enviar un mensaje generico y dejar el detalle solo en logging controlado del servidor; no devolver mensaje, stack trace ni clase real de excepcion. La forma exacta del cuerpo cambia respecto del comportamiento observado y debe acordarse al no existir `docs/ENDPOINTS.md`.
- **Archivos a tocar:** `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java` (o el mapper de excepciones definido en H-AUTH-06) y `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`.
- **Caracterizacion antes de tocar produccion:** provocar una excepcion ficticia desde un `LegacyDatabase` de prueba que falle en `findByCredentials`; comprobar status 500 y capturar la forma actual del cuerpo, sin usar fallos ni secretos externos. La prueba puede usar una subclase fake en el mismo paquete para evitar agregar una dependencia de mocking.
- **Verificacion posterior propuesta:** el mismo caso mantiene 500 y JSON, devuelve solo el mensaje generico acordado y no contiene texto o clase de la excepcion ficticia; verificar que el detalle queda en el mecanismo de logging, sin password ni token.
- **Cambio Aprobado:** Si

### H-AUTH-05 - Evaluacion SCA pendiente

- **Hallazgo:** H-AUTH-05, sugerencia de severidad media provisional, `Pendiente` en `HALLAZGOS.md`; no hay CVE confirmado.
- **Problema:** `auth/pom.xml` declara Quarkus 3.27.4 y Commons Text 1.9, pero las versiones por si solas no prueban una vulnerabilidad. Evidencia: `auth/pom.xml:13`, `auth/pom.xml:49-50`.
- **Cambio propuesto:** primero obtener evidencia de advisory/CodeQL/Dependency Review, version afectada, alcance transitivo y version corregida compatible. No actualizar ni eliminar dependencias por conjetura. Si existe hallazgo aplicable, actualizar con el BOM de Quarkus y una version corregida justificada; no silenciar alertas ni relajar JaCoCo. Coordinar la ejecucion de controles externos con su responsable; no se presume aprobacion ni resultado.
- **Archivos a tocar:** `auth/pom.xml` unicamente si la evidencia exige cambio; pruebas existentes en `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`. Informes de escaneo se adjuntan al proceso de revision, no se inventan ni se sustituyen con una exclusion.
- **Caracterizacion antes de tocar dependencias:** guardar el arbol actual con `mvn -B dependency:tree` y ejecutar el baseline de `mvn -B -Dtest=AuthResourceTest verify` desde `auth/`; registrar las versiones resueltas y resultados de escaneo disponibles. No ejecutar/atribuir CodeQL o Dependency Review hasta tener el control externo realmente disponible.
- **Verificacion posterior propuesta:** repetir arbol y analisis SCA, confirmar que el advisory aplicable queda corregido sin exclusiones y correr `mvn -B verify` con JaCoCo intacto; registrar resultados reales. Si no hay advisory aplicable, documentar evidencia y proponer cerrar el candidato sin cambiar dependencias.
- **Cambio Aprobado:** Si

### H-AUTH-06 - Responsabilidades concentradas en AuthResource

- **Hallazgo:** H-AUTH-06, refactor de severidad baja, `Pendiente` en `HALLAZGOS.md`.
- **Problema:** `login` valida, consulta `LegacyDatabase`, crea token, escapa datos y construye la respuesta en un metodo con condicionales anidados. Evidencia: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:26-50`.
- **Cambio propuesto:** separar HTTP (request/response y status), servicio de autenticacion (reglas y coordinacion), repositorio (SQLite) y seguridad (hash/token), con metodos de una responsabilidad, retornos anticipados, respuestas tipadas y constantes nombradas para expiracion y mensajes. Mantener el recurso como adaptador HTTP; no devolver `UserRecord` como DTO. Mantener consultas parametrizadas. Realizar esta extraccion como cambio revisable independiente de migracion de password y cambio de token para que los diffs sean atribuibles.
- **Archivos a tocar:** `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java`, `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java`, `auth/src/main/java/com/bcp/workshop/auth/UserRecord.java`, `auth/src/main/java/com/bcp/workshop/auth/LoginRequest.java`; nuevos `AuthService.java`, `UserRepository.java` o equivalente, DTO de respuesta y servicios de seguridad bajo `auth/src/main/java/com/bcp/workshop/auth/`; `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java` y pruebas unitarias nuevas en `auth/src/test/java/com/bcp/workshop/auth/`.
- **Caracterizacion antes de tocar produccion:** completar pruebas de contrato para request nulo, username/password nulo o vacio, login valido, credenciales incorrectas, username con comilla y error interno; comprobar status y campos JSON actuales. Las pruebas actuales cubren login valido, password incorrecto y username con comilla en `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:13-47`; extenderlas antes de extraer capas.
- **Verificacion posterior propuesta:** repetir todos los casos HTTP; pruebas unitarias por servicio/repositorio/seguridad con SQLite temporal; asegurar que SQL siga parametrizado y no haya secretos o passwords en logs/respuestas; ejecutar `mvn -B verify` desde `auth/` y reportar por separado cobertura global y del codigo modificado con numerador, denominador y metodo.
- **Cambio Aprobado:** Si

### Dependencias, orden y pendientes de aprobacion

1. Completar y ejecutar las caracterizaciones de H-AUTH-01, H-AUTH-02, H-AUTH-04 y H-AUTH-06 antes de cambiar produccion. Usar SQLite temporal bajo `auth/target` y fixtures ficticios.
2. Obtener resultados SCA de H-AUTH-05 antes de actualizar versiones o incorporar `quarkus-elytron-security-common`/`quarkus-smallrye-jwt-build`; validar BOM, advisories y compatibilidad. Ninguna dependencia nueva queda aprobada por este plan.
3. Definir/configurar el origen local permitido y la fuente de clave de prueba de H-AUTH-03 antes de emitir JWT en H-AUTH-02.
4. Acordar el nuevo formato de token con los consumidores y el cuerpo generico 500, dado que `docs/ENDPOINTS.md` falta. No se inspeccionan ni modifican otros componentes dentro de este alcance.
5. Implementar cada hallazgo en cambios revisables, repetir sus pruebas enfocadas y la regresion `mvn -B verify`; registrar solo resultados reales y cobertura medida. Los apartados H-AUTH-01 a H-AUTH-06 tienen `Cambio Aprobado: Si`; no se registra nombre/fecha de aprobador.

### Registro de ejecuciones

| componente | IDs | comando/accion | resultado |
|---|---|---|---|
| auth | H-AUTH-01, H-AUTH-02, H-AUTH-03, H-AUTH-04, H-AUTH-06 | Caracterizacion previa: `mvn -B -Dtest=AuthResourceTest test`, desde `auth/`, DB temporal `target/h-auth-20261104-h01.db` | 14 pruebas; 0 fallos; 0 errores. Captura contrato HTTP, payload legacy, CORS permisivo y error 500 actual antes de produccion. |
| auth | H-AUTH-01 | Misma suite tras migracion PBKDF2, desde `auth/`, misma DB | 14 pruebas; 0 fallos; 0 errores. CodeQL y Dependency Review remotos no ejecutados. |
| auth | H-AUTH-05 | `mvn -B dependency:tree`, desde `auth/` | Arbol local generado; declara Quarkus 3.27.4 y Commons Text 1.9. No es analisis de CVE. SCA, CodeQL y Dependency Review no ejecutados; sin resultado para resolver H-AUTH-05. |

### Aprobaciones, contrato y riesgos actualizados (2026-11-04)

- El usuario confirmo explicitamente en esta sesion la implementacion de H-AUTH-02 a H-AUTH-06 segun este plan. H-AUTH-01 ya figuraba `Resuelto` al iniciar este recorrido.
- **Contrato observado antes del cambio:** `POST /auth/login` conserva request `username`, `password`, `client`; status 400/401/200; para exito, `token`, `expiresIn=3600`, `user` y `debug`. El 500 observado incluia mensaje y clase de excepcion. La prueba previa especifica del H-AUTH-04 verifico este ultimo comportamiento.
- **Contrato acordado con la aprobacion recibida:** se mantienen ruta, validaciones, status 400/401/200, valores de usuario, `expiresIn`, `debug` y sobre JSON de exito; cambia la representacion interna de `token` a JWT firmado. Los errores 500 siguen siendo JSON/status 500, pero el cuerpo se limita al mensaje generico `Authentication failed`.
- **Configuracion aprobada:** `AUTH_CORS_ORIGINS` tiene como baseline local `http://localhost:4200`; los despliegues deben proveer sus origenes permitidos y `AUTH_JWT_PRIVATE_KEY_LOCATION`. No se inspeccionaron consumidores fuera de `auth/`; su migracion a JWT y configuracion de despliegue quedan como coordinacion externa pendiente.
- No existe `docs/ENDPOINTS.md` ni se creo en este paso. No se creo `docs/TESTING.md`; ambos se reservan para sus pasos de documentacion.

### Resultado por hallazgo

| componente | ID | caracterizacion antes del cambio | correccion y prueba despues del cambio | resultado |
|---|---|---|---|---|
| auth | H-AUTH-01 | El registro historico arriba consigna 14 pruebas; la base temporal legacy contenia passwords en claro. | `mvn -B verify` migro filas legacy, mantuvo login y elimino la columna en claro; `LegacyDatabaseTest` prueba inicializacion limpia y repetida; `PasswordHashServiceTest` prueba salt distinto, password correcto/incorrecto y formatos invalidos. | Antes: 14 pruebas, 0 fallos. Despues: suite total 18 pruebas, 0 fallos; JaCoCo cubre la ruta de migracion y la inicializacion limpia/repetida. |
| auth | H-AUTH-02 | El registro historico indica 14 pruebas y captura de payload legacy. Esa asercion ya no esta en la prueba actual y no se puede reproducir desde el arbol actual; se conserva como evidencia historica, no como prueba repetible. | `AuthResourceTest.issuesSignedJwtWithExpectedClaims` valida formato JWT, `sub`, grupo, expiracion, firma y rechazo de firma alterada; los tests HTTP conservan campos/status. | Antes: 14 pruebas segun el registro historico. Despues: suite total 18 pruebas, 0 fallos. Riesgo de consumidores fuera de alcance sigue abierto. |
| auth | H-AUTH-03 | La caracterizacion historica reporta CORS permisivo para origen ficticio y local. | `AuthResourceTest` comprueba origen local permitido y origen ajeno sin `Access-Control-Allow-Origin`; la clave se configura por ruta externa y la prueba usa clave efimera. | Antes: 14 pruebas segun el registro historico. Despues: suite total 18 pruebas, 0 fallos. La configuracion real de cada despliegue sigue pendiente. |
| auth | H-AUTH-04 | `mvn -B -Dtest=AuthResourceTest test` sobre el recurso previo, con error ficticio: status 500, mensaje `fixture database failure` y campo `exception`. | La misma prueba exige status 500, mensaje generico y ausencia de mensaje/clase interna. El detalle se escribe en el log del servidor. | Antes: 15 pruebas, 0 fallos. Despues: 15 pruebas enfocadas, 0 fallos; suite final 18 pruebas, 0 fallos. |
| auth | H-AUTH-05 | El arbol previo de esta ejecucion resolvia Quarkus/REST y JWT builder 3.27.4 y Commons Text 1.10.0 (este ultimo ya estaba actualizado en el arbol de trabajo al iniciar). La base original historica del plan declara Commons Text 1.9. | Apache Commons Text indica 1.10.0 como version que corrige CVE-2022-42889. GHSA-xrmg-9xc7-83fp indica versiones afectadas `<=3.38` y parche `3.27.5.1` para la rama 3.27; se actualizo el BOM. Arbol posterior confirma REST/JWT builder 3.27.5.1 y Commons Text 1.10.0. | Antes: Quarkus 3.27.4; despues: 3.27.5.1. `mvn -B verify` final: 18 pruebas, 0 fallos. El arbol no es SCA; CodeQL y Dependency Review remotos no ejecutados. |
| auth | H-AUTH-06 | La caracterizacion previa de `AuthResourceTest` comprobo 400/401/200, usuarios validos e inexistentes, campos nulos/vacios y error interno antes de extraer capas. | `AuthResource` delega a `AuthService`; este coordina validacion, persistencia y token y retorna `LoginResponse`; el repositorio preserva SQL parametrizado. Se repitieron los casos HTTP y la prueba de 500. | Antes: 15 pruebas, 0 fallos. Despues: 15 pruebas enfocadas y 18 de regresion total, 0 fallos; contrato de status y campos JSON conservado segun las aserciones. |

### Registro adicional de ejecuciones

| componente | IDs | comando/accion | resultado |
|---|---|---|---|
| auth | H-AUTH-02, H-AUTH-03, H-AUTH-04, H-AUTH-06 | Antes de refactorizar: `mvn -B -Dtest=AuthResourceTest test`, desde `auth/`, DB aislada `target/h-auth-20261104-pre-h02-h06.db` | 15 pruebas; 0 fallos; 0 errores. El test de error confirmo el cuerpo 500 observado antes del cambio. |
| auth | H-AUTH-04, H-AUTH-06 | Despues de cambiar manejo de errores y capas: `mvn -B -Dtest=AuthResourceTest test`, desde `auth/`, DB aislada `target/h-auth-20261104-post-h04-h06.db` | 15 pruebas; 0 fallos; 0 errores. El error ficticio solo aparece en el log del servidor; la respuesta es generica. |
| auth | H-AUTH-01 | Intento `mvn -B -Dtest=AuthResourceTest,LegacyDatabaseTest,PasswordHashServiceTest test`, desde `auth/` | 18 pruebas, 1 error: la prueba unitaria de SQLite no habia cargado el driver en su classloader. Se agrego la carga explicita del driver en la prueba; no fue un fallo funcional del producto. |
| auth | H-AUTH-01, H-AUTH-02, H-AUTH-03, H-AUTH-04, H-AUTH-06 | Reintento `mvn -B -Dtest=AuthResourceTest,LegacyDatabaseTest,PasswordHashServiceTest test`, desde `auth/`, DB temporal bajo `target/` | 18 pruebas; 0 fallos; 0 errores. Incluye DB legacy aislada, DB limpia, inicializacion repetida, JWT, CORS y error generico. |
| auth | H-AUTH-01, H-AUTH-02, H-AUTH-03, H-AUTH-04, H-AUTH-06 | `mvn -B verify`, desde `auth/`, antes del cambio de BOM, DB legacy aislada | 15 pruebas; 0 fallos; 0 errores; BUILD SUCCESS con Quarkus 3.27.4. |
| auth | H-AUTH-05 | `mvn -B dependency:tree -Dincludes=io.quarkus:quarkus-smallrye-jwt-build,io.quarkus.resteasy.reactive:*,org.apache.commons:commons-text`, desde `auth/`, antes de actualizar Quarkus | BUILD SUCCESS; REST/JWT builder 3.27.4, Commons Text 1.10.0. |
| auth | H-AUTH-01, H-AUTH-02, H-AUTH-03, H-AUTH-04, H-AUTH-05, H-AUTH-06 | `mvn -B verify`, desde `auth/`, DB legacy aislada `target/h-auth-20261104-final-verify.db` | BUILD SUCCESS; 18 pruebas, 0 fallos, 0 errores. JaCoCo: 166/175 lineas cubiertas (94.9%) en las clases de produccion modificadas: `AuthResource`, `AuthService` y nested records, `LoginResponse`, `LegacyDatabase`, `PasswordHashService` y `TokenService`. |
| auth | H-AUTH-05 | Mismo `mvn -B dependency:tree ...` posterior al BOM actualizado | BUILD SUCCESS; REST/JWT builder 3.27.5.1, Commons Text 1.10.0. |
| auth | H-AUTH-05 | Revision de advisory Apache Commons Text y GHSA-xrmg-9xc7-83fp | Apache fija Commons Text 1.10.0 para CVE-2022-42889; el advisory Quarkus fija la rama 3.27 en 3.27.5.1 para CVE-2026-16308. CodeQL y Dependency Review remotos no ejecutados; el arbol local no se presenta como analisis SCA. |
| auth | H-AUTH-03 | Seguimiento de revision local: agregar validacion al iniciar si falta `AUTH_JWT_PRIVATE_KEY_LOCATION`; `mvn -B verify`, desde `auth/`, DB temporal `target/h-auth-20261104-jwt-config.db` | BUILD SUCCESS; 20 pruebas, 0 fallos, 0 errores. `TokenServiceTest` cubre ubicacion vacia y configurada; JaCoCo actualizado: 169/178 lineas (94.9%) en las clases de produccion modificadas. El fixture de integracion provee una clave RSA efimera. |
| auth | H-AUTH-01, H-AUTH-02, H-AUTH-03, H-AUTH-04, H-AUTH-06 | Documentacion del contrato comprobado: crear `docs/ENDPOINTS.md` para `GET /q/health` y `POST /auth/login` | Documentacion actualizada desde codigo y propiedades; los cuerpos dependientes de framework no definidos por auth quedan como preguntas abiertas. No se ejecutaron pruebas: cambio solo documental. |
| auth | H-AUTH-01 a H-AUTH-06 | Antes de agregar el test directo de health: `mvn -B verify`, cwd `auth/`, DB aislada `target/h-auth-20261104-testing-baseline.db` | Codigo de salida 0; BUILD SUCCESS; 20 pruebas, 0 fallos, 0 errores. La suite no tenia asercion directa para `GET /q/health`. |
| auth | H-AUTH-01 a H-AUTH-06 | Primera ejecucion tras agregar el test HTTP de health: `mvn -B verify`, cwd `auth/`, DB aislada `target/h-auth-20261104-testing-post.db` | Codigo de salida 0; BUILD SUCCESS; 21 pruebas, 0 fallos, 0 errores. Luego se renombro el metodo para explicitar que no envia `Authorization`; la asercion no cambio. |
| auth | H-AUTH-01 a H-AUTH-06 | Agregar `AuthResourceTest.reportsHealthStatusWithoutAuthorizationHeader`; `mvn -B verify`, cwd `auth/`, DB aislada `target/h-auth-20261104-testing-final.db`; crear/actualizar `docs/TESTING.md` | Codigo de salida 0; BUILD SUCCESS; 21 pruebas, 0 fallos, 0 errores. JaCoCo: 171/180 lineas (95.0%) en el paquete de produccion `com.bcp.workshop.auth`. Health no tiene errores ni bordes definidos en el contrato; quedan pendientes sin inventar respuestas. |
