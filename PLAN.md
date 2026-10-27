# Plan de corrección

## auth

### Base contractual y condiciones de ejecución

`docs/ENDPOINTS.md` no existe. Se preservará el comportamiento observado en `POST /auth/login` y documentado en `HALLAZGOS.md`: validaciones `400`, rechazo `401` y respuesta exitosa `200` con `token`, `expiresIn`, `user` y `debug` (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:15-49`). Las excepciones son los comportamientos inseguros que cada hallazgo pide corregir explícitamente, como la exposición de detalles internos y el CORS abierto.

Antes de implementar cualquier apartado se requiere revisión y aprobación humana de su alcance. Las pruebas se escribirán primero en `auth/`, se ejecutarán con `cwd auth/` y sus resultados reales se registrarán después; este plan no afirma ejecuciones ni cobertura.

### Estado de aprobación y ejecución

- Alcance histórico aprobado por Daniel el 26/10/2026: H-AUTH-01 a H-AUTH-07 en su plan original.
- Resultado histórico: H-AUTH-01 a H-AUTH-06 quedaron verificados; H-AUTH-07 tuvo una remediación parcial y fue reabierto en la revisión local del 27/10/2026 porque Quarkus `3.27.6.1` está EOL.
- Alcance seleccionado en esta revisión del plan: únicamente H-AUTH-07.
- Aprobación del nuevo alcance de migración a una LTS soportada: `Aprobado` por Daniel el 27/10/2026 mediante confirmación explícita en la conversación. La aprobación se limita al apartado revisado de H-AUTH-07.
- Implementación del nuevo alcance: migración y regresión completadas; cierre pendiente por falta de un SCA completo.
- Última regresión documentada: `mvn verify`, 26 pruebas, 0 fallos, 0 errores y 0 omitidas con Quarkus `3.40.1.1`; cobertura JaCoCo 145/168 líneas = 86.31 %.
- CodeQL remoto: no ejecutado en este recorrido local.
- Dependency Review remoto: no ejecutado en este recorrido local.
- Condición de cierre de H-AUTH-07: la migración, regresión y cobertura están completas; falta ejecutar SCA con una fuente de vulnerabilidades disponible y obtener revisión humana del diff y de la evidencia.

### Registro de ejecuciones de auth

| orden | IDs | comando o comprobación | resultado real |
|---|---|---|---|
| 1 | H-AUTH-01 a H-AUTH-07 | `mvn test` | No inició: Maven intentó crear `C:\.m2\repository` y recibió acceso denegado. |
| 2 | H-AUTH-01 a H-AUTH-07 | `mvn -Dmaven.repo.local=target/m2-cache test` | No inició por interpretación del argumento sin comillas; con ruta absoluta entre comillas llegó a resolución, pero la red del sandbox bloqueó Maven Central. |
| 3 | H-AUTH-01 a H-AUTH-07 | `mvn test` fuera de la restricción de red | Caracterización inicial: 6 pruebas, todas correctas. La entrada con comilla produjo stacktrace SQL, confirmando H-AUTH-01. |
| 4 | H-AUTH-01 | `mvn test` | Después de parametrizar: 6 pruebas correctas y sin error SQL para la entrada con comilla. |
| 5 | H-AUTH-02 | `mvn test -Dtest=PasswordHasherTest` | Rojo esperado: compilación falló porque `PasswordHasher` aún no existía. |
| 6 | H-AUTH-02 | `mvn test -Dtest=PasswordHasherTest,AuthResourceTest` | Primer intento no inició por la coma interpretada por PowerShell; reintento citado: 8 pruebas correctas. |
| 7 | H-AUTH-03 | `mvn test -Dtest=TokenServiceTest` | Rojo esperado: compilación falló porque `TokenService` aún no existía. |
| 8 | H-AUTH-03 | `mvn test -Dtest=TokenServiceTest,AuthResourceTest` | 9 pruebas correctas. |
| 9 | H-AUTH-04 | `mvn test -Dtest=AuthResourceTest` | Rojo esperado: 1 de 7 falló porque el origen ficticio recibió cabecera CORS. |
| 10 | H-AUTH-04 | `mvn test -Dtest=AuthResourceTest` | Después de restringir CORS: 7 pruebas correctas. |
| 11 | H-AUTH-05 | `mvn test -Dtest=AuthErrorResponseTest` | Rojo esperado: 1 fallo; el cuerpo devolvía `database-details`. |
| 12 | H-AUTH-05 | `mvn test -Dtest=AuthErrorResponseTest,AuthResourceTest` | 8 pruebas correctas; detalle ausente del cuerpo y presente solo en log. |
| 13 | H-AUTH-06 | `mvn test -Dtest=AuthServiceTest` | Rojo esperado: compilación falló porque Service, Repository y DTOs aún no existían. |
| 14 | H-AUTH-06 | `mvn test` | Después del refactor: 15 pruebas correctas. |
| 15 | H-AUTH-07 | `mvn verify` | Después de actualizar dependencias: 15 pruebas correctas, build y reporte JaCoCo correctos. |
| 16 | H-AUTH-01, H-AUTH-02, H-AUTH-03, H-AUTH-06 | `mvn verify` con pruebas de cobertura nuevas | 18 pruebas correctas y 1 error de fixture: el test directo no había registrado el driver SQLite. |
| 17 | H-AUTH-01 a H-AUTH-07 | `mvn verify` final | 19 pruebas, 0 fallos, 0 errores, build correcto con Quarkus `3.27.6.1`. |
| 18 | H-AUTH-01 a H-AUTH-07 | Lectura de `auth/target/site/jacoco/jacoco.csv` | 146/166 líneas cubiertas = 87.95%; 28/38 ramas = 73.68%. |
| 19 | H-AUTH-07 | `mvn "-Dtest=AuthResourceTest#ignoresUnknownJsonFieldsWithoutChangingSuccessfulLogin" test` antes del BOM | Código 0; 1 prueba correcta con Quarkus `3.27.6.1`. |
| 20 | H-AUTH-07 | `mvn dependency:tree "-Dincludes=io.quarkus:*,org.apache.commons:commons-text"` antes del BOM | Código 0; Quarkus `3.27.6.1` y Commons Text `1.15.0` confirmados. |
| 21 | H-AUTH-07 | La misma caracterización después de cambiar el BOM | Código 0; 1 prueba correcta con Quarkus `3.40.1.1`, sin cambiar expectativas. |
| 22 | H-AUTH-07 | `mvn verify` después del BOM | Código 0; 26 pruebas correctas y JaCoCo generado. Maven advirtió que `quarkus-junit5` fue reubicado. |
| 23 | H-AUTH-07 | `mvn verify` después de usar `quarkus-junit` | Código 0; 26 pruebas, 0 fallos, 0 errores, 0 omitidas y sin la advertencia de relocation. |
| 24 | H-AUTH-07 | `mvn org.owasp:dependency-check-maven:13.0.0:check "-DfailBuildOnCVSS=7"` | Código 1; el análisis no inició porque NVD rechazó la actualización sin API key y no existía caché local. No se obtuvo un resultado de vulnerabilidades. |
| 25 | H-AUTH-07 | Comprobación local de `osv-scanner`, `trivy` y `grype` | Los tres comandos están ausentes; Sonatype Guide requiere credenciales. |
| 26 | H-AUTH-07 | Árbol efectivo final con `mvn dependency:tree` | Código 0; extensiones Quarkus y `quarkus-junit` resueltas en `3.40.1.1`; Commons Text permanece en `1.15.0`. |
| 27 | H-AUTH-07 | Lectura final de `auth/target/site/jacoco/jacoco.csv` | 145/168 líneas = 86.31%; 33/42 ramas = 78.57%. |
## Item: H-AUTH-01 — Credenciales concatenadas en una consulta SQL

**Hallazgo.** H-AUTH-01, componente auth, estado inicial `Pendiente`, resultado `Resuelto`, severidad alta. La consulta incorporaba directamente `username` y `password` y se ejecutaba con `Statement` (`auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:31-35`).

**Problema.** Una entrada puede modificar la estructura de la consulta. Además, un error SQL se captura y se transforma silenciosamente en ausencia de usuario (`auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:39-42`).

**Cambio propuesto.** Mantener una única responsabilidad para la búsqueda de credenciales y reemplazar la concatenación por SQL constante con parámetros `?` enlazados mediante `PreparedStatement`. El método seguirá devolviendo el mismo usuario para credenciales válidas y ausencia para credenciales inválidas. No se modificará el endpoint ni su cuerpo de respuesta. La posterior separación de capas de H-AUTH-06 deberá trasladar esta consulta ya parametrizada sin reintroducir concatenación.

**Archivos previstos.**

- Modificar `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java`.
- Ampliar primero `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`.
- Crear, si hace falta aislar SQLite, `auth/src/test/java/com/bcp/workshop/auth/LegacyDatabaseTest.java`.

**Prueba de caracterización previa.** Antes de tocar `LegacyDatabase`, cubrir: credenciales `student`/`Workshop123!` → `200`; credenciales ficticias inválidas → `401`; usuario ficticio con una comilla y contraseña ficticia → `401`, sin autenticación ni `500`. Tras el cambio, repetir exactamente esos casos y añadir una prueba directa que compruebe que usuario y contraseña se tratan como valores, no como fragmentos SQL.

## Item: H-AUTH-02 — Contraseñas almacenadas y comparadas en texto plano

**Hallazgo.** H-AUTH-02, componente auth, estado inicial `Pendiente`, resultado `Resuelto`, severidad alta. Las contraseñas se insertaban literalmente y formaban parte de la consulta de autenticación (`auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:22-24,31-35`).

**Problema.** Una copia o lectura no autorizada de SQLite revela contraseñas inmediatamente utilizables. La comparación dentro del SQL también impide aplicar de forma clara un algoritmo lento con salt.

**Cambio propuesto.** Introducir un componente de propósito único para generar y verificar hashes lentos con salt. Preferir un algoritmo disponible en el JDK, como PBKDF2 con HMAC-SHA-256, para evitar una dependencia nueva; iteraciones, tamaño de salt y tamaño de clave serán constantes con nombre. El repositorio buscará primero por `username` mediante consulta parametrizada y la verificación del hash se hará fuera del SQL. Los usuarios ficticios se inicializarán con hashes, y cualquier migración de la base local existente deberá convertir los valores legados una sola vez y eliminar el texto plano, sin registrar contraseñas. El contrato HTTP y las credenciales ficticias documentadas se conservarán.

**Archivos previstos.**

- Modificar `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java`.
- Crear `auth/src/main/java/com/bcp/workshop/auth/PasswordHasher.java`.
- Crear `auth/src/test/java/com/bcp/workshop/auth/PasswordHasherTest.java`.
- Ampliar primero `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`.

**Prueba de caracterización previa.** Antes de cambiar el esquema o la comparación, fijar el comportamiento de login válido (`200` y usuario `student`) y contraseña incorrecta (`401`). Después, verificar unitariamente que la contraseña correcta valida, una incorrecta falla, dos hashes de la misma contraseña usan salts distintos y el valor persistido no contiene `Workshop123!` ni `Trainer123!` en texto plano.

## Item: H-AUTH-03 — Token reversible en Base64 con secreto embebido

**Hallazgo.** H-AUTH-03, componente auth, estado inicial `Pendiente`, resultado `Resuelto`, severidad alta. El token codificaba en Base64 usuario, rol, expiración y un secreto constante, sin firma verificable (`auth/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java:5,8,12-15`).

**Problema.** Base64 es reversible y no demuestra integridad. Un consumidor no puede validar de forma estándar que los claims no fueron alterados ni que el token procede del emisor esperado.

**Cambio propuesto.** Sustituir `LegacyTokenUtil` por un servicio de tokens firmado que emita claims explícitos de usuario, rol, emisión y expiración. Se evaluará primero la capacidad JWT de Quarkus; si requiere una dependencia, se añadirá solo la extensión oficial mínima y se documentará su necesidad en el cambio. La duración de 3600 segundos será una constante o propiedad con nombre y seguirá correspondiendo al `expiresIn` de la respuesta. La clave no será parte de este alcance: el servicio la recibirá desde la configuración definida por H-AUTH-04. H-AUTH-03 y H-AUTH-04 conservarán apartados, pruebas y revisión independientes aunque deban coordinarse para integrar el token.

**Archivos previstos.**

- Modificar `auth/pom.xml` únicamente si la solución JWT elegida necesita una extensión justificada.
- Sustituir o retirar `auth/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java`.
- Crear `auth/src/main/java/com/bcp/workshop/auth/TokenService.java`.
- Modificar `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java` para delegar la emisión.
- Crear `auth/src/test/java/com/bcp/workshop/auth/TokenServiceTest.java`.
- Ampliar primero `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`.

**Prueba de caracterización previa.** Antes de cambiar el token, fijar que el login válido responde `200`, entrega un `token` no vacío, `expiresIn` igual a `3600` y conserva los datos de `user` y `debug`. Tras la implementación, probar firma válida, rechazo de token alterado, claims de usuario y rol, y expiración controlada mediante un reloj inyectable para evitar tiempos mágicos o pruebas inestables.

## Item: H-AUTH-04 — Secreto embebido y CORS permitido para cualquier origen

**Hallazgo.** H-AUTH-04, componente auth, estado inicial `Pendiente`, resultado `Resuelto`, severidad alta. El secreto estaba en Java y CORS aceptaba cualquier origen (`auth/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java:8`; `auth/src/main/resources/application.properties:4-7`).

**Problema.** El secreto queda expuesto junto con el código y no puede rotarse por ambiente. El patrón CORS abierto permite que cualquier origen intente consumir el servicio desde un navegador.

**Cambio propuesto.** Eliminar el secreto del código e inyectar la clave o referencia de firma mediante configuración externa, sin valor real ni valor de producción en el repositorio. Proporcionar únicamente un valor ficticio aislado para pruebas. Reemplazar el patrón CORS universal por una propiedad de orígenes permitidos configurable por ambiente, con el origen local del laboratorio como valor de desarrollo. Mantener métodos y cabeceras necesarios para `POST /auth/login`. La reducción de orígenes es el cambio de seguridad intencional; la forma y estados de la respuesta del endpoint no cambian.

**Archivos previstos.**

- Modificar `auth/src/main/resources/application.properties`.
- Modificar el proveedor de token resultante de H-AUTH-03; mientras exista, retirar el secreto de `auth/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java`.
- Crear `auth/src/test/resources/application.properties` con valores ficticios exclusivos de prueba, si el perfil de prueba lo requiere.
- Crear `auth/src/test/java/com/bcp/workshop/auth/AuthConfigurationTest.java` o ampliar primero `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java` para CORS.

**Prueba de caracterización previa.** Antes de cambiar configuración, comprobar que el login válido funciona con la configuración de prueba y registrar la respuesta CORS actual para el origen local. Después, probar que el origen local configurado recibe las cabeceras CORS esperadas, que un origen ficticio no autorizado no las recibe y que la aplicación de prueba arranca solo con una clave ficticia configurada. No se guardará ni imprimirá la clave.

## Item: H-AUTH-05 — Respuestas de error exponen detalles internos

**Hallazgo.** H-AUTH-05, componente auth, estado inicial `Pendiente`, resultado `Resuelto`, severidad media. La respuesta `500` incluía el mensaje real y la clase de la excepción (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:54-58`).

**Problema.** El cliente puede recibir nombres de clases, detalles del controlador JDBC u otra información interna útil para reconocer la implementación. En persistencia, las excepciones se imprimen directamente (`auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:26-27,39-40`).

**Cambio propuesto.** Devolver un mensaje genérico y estable para errores internos, sin clase ni detalle técnico. Registrar el error completo mediante logging controlado del servidor, sin credenciales. Extraer la construcción de respuestas de error a métodos pequeños o a un mapper de excepciones si evita duplicación; no añadir exclusiones de scanner ni ocultar alertas. Los estados `400`, `401` y `500` observados se conservarán, salvo que una prueba demuestre otro contrato.

**Archivos previstos.**

- Modificar `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java` o crear `auth/src/main/java/com/bcp/workshop/auth/AuthExceptionMapper.java` si centraliza realmente la responsabilidad.
- Modificar `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java` para logging controlado.
- Crear `auth/src/test/java/com/bcp/workshop/auth/AuthErrorResponseTest.java` o ampliar primero `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`.

**Prueba de caracterización previa.** Antes de cambiar el manejo, provocar una excepción local controlada mediante un doble de persistencia y fijar el estado `500` y el formato JSON. La prueba de regresión de seguridad deberá exigir un mensaje genérico y ausencia de las claves o valores `exception`, nombre de clase, SQL y credenciales; inicialmente documentará la diferencia esperada respecto del comportamiento inseguro.

## Item: H-AUTH-06 — El recurso concentra múltiples responsabilidades

**Hallazgo.** H-AUTH-06, componente auth, estado inicial `Pendiente`, resultado `Resuelto`, severidad media. `AuthResource.login` validaba, accedía a persistencia, decidía el resultado, generaba el token y construía la respuesta (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:24-60`).

**Problema.** El anidamiento y acoplamiento hacen que una modificación de persistencia, seguridad o respuesta afecte al mismo método, dificultando revisión y pruebas unitarias.

**Cambio propuesto.** Refactor incremental hacia Resource → Service → Repository. `AuthResource` conservará HTTP, validación superficial y mapeo de respuestas; `AuthService` coordinará autenticación y token; `AuthRepository` expresará la búsqueda de usuario; una implementación SQLite contendrá exclusivamente SQL parametrizado. Introducir DTOs de respuesta propios para no devolver modelos de persistencia. Usar guard clauses, métodos de una sola responsabilidad, nombres de propósito y constantes para `3600`, mensajes y claves repetidas. No duplicar lógica ya corregida por H-AUTH-01 a H-AUTH-05.

**Archivos previstos.**

- Modificar `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java`.
- Sustituir gradualmente `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java` por `auth/src/main/java/com/bcp/workshop/auth/AuthRepository.java` y `auth/src/main/java/com/bcp/workshop/auth/SqliteAuthRepository.java`.
- Crear `auth/src/main/java/com/bcp/workshop/auth/AuthService.java`.
- Crear DTOs de salida bajo `auth/src/main/java/com/bcp/workshop/auth/`, con nombres basados en su propósito.
- Conservar o adaptar `auth/src/main/java/com/bcp/workshop/auth/LoginRequest.java`, `UserRecord.java` y el proveedor de token sin cambiar el JSON observado.
- Ampliar primero `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java` y crear pruebas unitarias de servicio y repositorio.

**Prueba de caracterización previa.** Antes del movimiento entre capas, cubrir la matriz HTTP: request nulo, usuario ausente, contraseña ausente, credenciales inválidas y credenciales válidas. Para cada caso fijar estado, mensaje o campos de éxito observados, incluidos `token`, `expiresIn`, `user.username`, `user.displayName`, `user.role` y `debug`. Tras el refactor, las mismas pruebas deben pasar sin cambios; las pruebas unitarias nuevas comprobarán cada capa por separado.

## Item: H-AUTH-07 — Versiones de Quarkus y Commons Text requieren análisis SCA

**Hallazgo.** H-AUTH-07, componente auth, severidad baja y estado actual `Pendiente`. Commons Text ya está en `1.15.0`, pero el BOM de Quarkus sigue en `3.27.6.1`, perteneciente a la línea identificada como EOL durante la revisión local. Falta migrar a una LTS soportada y repetir SCA y regresión (`HALLAZGOS.md:25,35`; `auth/pom.xml:11-14,47-50`).

**Problema.** Permanecer en una línea EOL impide recibir mantenimiento comunitario ordinario, aunque la compilación y las pruebas actuales pasen. Elegir una versión solo por ser la más reciente también puede introducir cambios incompatibles. La migración debe mantener el contrato observado de `POST /auth/login` y `GET /q/health`, incluidos serialización JSON, estados de error y respuestas de framework (`docs/ENDPOINTS.md:7-137`).

**Cambio propuesto.**

1. Antes de editar dependencias, consultar la matriz oficial de releases de Quarkus y seleccionar una línea LTS que siga soportada en la fecha de implementación, sea compatible con JDK 21 y tenga disponibles las extensiones `quarkus-rest-jackson`, `quarkus-smallrye-health` y `quarkus-junit5`. Registrar la versión exacta y la fuente; este plan no congela una versión sin esa evidencia temporal.
2. Actualizar únicamente `quarkus.platform.version` en el BOM para mantener alineadas las extensiones administradas (`auth/pom.xml:11-25,30-41,52-60`). No cambiar Commons Text `1.15.0` salvo que el SCA aporte un advisory aplicable o una incompatibilidad demostrada.
3. Adaptar código o configuración de `auth/` solo si la guía oficial de migración o una prueba roja demuestra una incompatibilidad. Cada adaptación debe conservar los contratos actuales y quedar en un diff separado y justificable; no se aceptan exclusiones de scanner ni reducciones de JaCoCo.
4. Obtener evidencia SCA de las coordenadas resueltas. Localmente, registrar el árbol efectivo de dependencias; CodeQL y Dependency Review se registrarán como no ejecutados hasta que realmente corran en el control remoto correspondiente. H-AUTH-07 no se marcará `Resuelto` solo porque `mvn verify` pase.
5. Después del cambio, ejecutar la nueva caracterización, toda la suite con `mvn verify`, comparar el contrato de ambos endpoints y medir JaCoCo con el mismo método de `docs/TESTING.md`. Exigir al menos 85 % para el código de producción que eventualmente deba modificarse.

**Archivos previstos.**

- `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`: añadir primero la caracterización del request con un campo JSON desconocido.
- `auth/pom.xml`: cambiar el BOM de Quarkus después de elegir y documentar la LTS soportada.
- `auth/src/main/**`: no se prevén cambios; tocar únicamente los archivos que una incompatibilidad reproducida obligue a adaptar, sin cambiar el contrato.
- `HALLAZGOS.md`: actualizar solo la fila y nota de H-AUTH-07 después de contar con evidencia real.
- `PLAN.md`: registrar aprobación, comandos y resultados de la futura implementación sin alterar el historial.
- `docs/TESTING.md`: añadir las ejecuciones, versiones resueltas, código de salida, cobertura, fallos y controles no ejecutados.
- `docs/ENDPOINTS.md`: no modificar; solo actualizarlo si una desviación contractual fuera acordada explícitamente, lo cual queda fuera de este alcance.

**Prueba que debe escribirse antes del cambio.** Añadir a `AuthResourceTest` un caso de caracterización que envíe credenciales válidas junto con un campo JSON desconocido y verifique `200` y el cuerpo completo de éxito. Este caso fija el comportamiento observado `quarkus.jackson.fail-on-unknown-properties=false` (`auth/src/main/resources/application.properties:8`; `docs/ENDPOINTS.md:37`) frente a posibles cambios de Jackson o Quarkus. Debe pasar con `3.27.6.1` antes de editar el BOM y volver a pasar sin adaptar su expectativa después de la migración.

**Regresión y evidencia posteriores.** Reutilizar sin cambios `AuthResourceTest`, `HealthEndpointTest`, `AuthErrorResponseTest` y las pruebas unitarias de servicio, token, hash y repositorio. Ejecutar desde `auth/` la caracterización aislada y luego `mvn verify`; generar `auth/target/site/jacoco/index.html`, registrar numerador, denominador y método de cobertura, y conservar visibles los fallos. Registrar aparte el comando y resultado del SCA elegido y no afirmar que CodeQL o Dependency Review pasaron si no se ejecutaron.

## Resultados de implementación por hallazgo

### H-AUTH-01

- **Diff:** `LegacyDatabase` fue sustituido por `SqliteAuthRepository`; la búsqueda usa SQL constante, `PreparedStatement` y parámetro de usuario (`auth/src/main/java/com/bcp/workshop/auth/SqliteAuthRepository.java:27,60-70`).
- **Caracterización antes/después:** 6/6 pruebas correctas en ambos lados; antes apareció `SQLiteException` con la comilla y después no apareció.
- **Corrección antes/después:** antes, la comilla alcanzaba la sintaxis SQL; después se trata como valor y retorna `401`. Resultado final incluido en 19/19.

### H-AUTH-02

- **Diff:** se añadió `PasswordHasher` con PBKDF2 y salt; el repositorio migra valores legados y `AuthService` verifica el hash (`auth/src/main/java/com/bcp/workshop/auth/PasswordHasher.java:13-52`; `auth/src/main/java/com/bcp/workshop/auth/SqliteAuthRepository.java:84-119`).
- **Caracterización antes/después:** el login `student`/`Workshop123!` conservó `200` y una contraseña incorrecta conservó `401`.
- **Corrección antes/después:** la prueba fue roja por clase ausente; después validó contraseña correcta, rechazo de incorrecta, salts distintos y ausencia del texto plano. Resultado focalizado: 2/2; final: 19/19.

### H-AUTH-03

- **Diff:** se retiró `LegacyTokenUtil` y se añadió `TokenService` con HMAC-SHA-256, claims y expiración (`auth/src/main/java/com/bcp/workshop/auth/TokenService.java:19-105`).
- **Caracterización antes/después:** la respuesta mantiene token no vacío, `expiresIn=3600`, `user` y `debug`.
- **Corrección antes/después:** la prueba fue roja por clase ausente; después cubrió firma, claims, token alterado, expirado y malformado. Resultado focalizado: 5/5; final: 19/19.

### H-AUTH-04

- **Diff:** la clave proviene de `AUTH_TOKEN_SECRET`, el test usa un valor ficticio y CORS proviene de `AUTH_CORS_ORIGINS` con localhost como valor de desarrollo (`auth/src/main/resources/application.properties:4-9`; `auth/src/test/resources/application.properties:1`).
- **Caracterización antes/después:** el origen `http://localhost:4200` siguió permitido y el login conservó su contrato.
- **Corrección antes/después:** antes, la prueba del origen ficticio falló porque recibía la cabecera CORS; después dejó de recibirla. Resultado: 7/7.

### H-AUTH-05

- **Diff:** `AuthResource` devuelve un mensaje genérico y registra la excepción solo en servidor (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:18-19,41-46`).
- **Caracterización antes/después:** el estado interno continúa siendo `500`; los estados `400`, `401` y `200` no cambiaron.
- **Corrección antes/después:** antes, 1/1 falló porque el cuerpo contenía `database-details`; después, el test pasó y confirmó ausencia de `exception`, clase y detalle. Resultado combinado: 8/8.

### H-AUTH-06

- **Diff:** se separaron `AuthResource`, `AuthService`, `AuthRepository`, `SqliteAuthRepository`, `LoginResponse` y `UserResponse`; se reemplazó el anidamiento por guard clauses.
- **Caracterización antes/después:** las 7 pruebas HTTP del recurso pasaron sin cambios antes y después.
- **Corrección antes/después:** la prueba de servicio fue roja por capas ausentes; después validó autenticación y rechazo unitarios. Resultado del refactor: 15/15; final: 19/19.

### H-AUTH-07

- **Diff:** Quarkus pasó de `3.27.4` a `3.27.6.1` y Commons Text de `1.9` a `1.15.0` (`auth/pom.xml:13,47-50`).
- **Caracterización antes/después:** 15/15 pruebas pasaron con las versiones anteriores y nuevamente tras actualizar; la corrida final amplió la suite a 19/19.
- **Corrección antes/después:** la revisión oficial identificó Commons Text anterior a 1.10.0 en el rango de CVE-2022-42889, aunque el uso local no alcanza `StringSubstitutor`, y un parche posterior para la línea Quarkus 3.27. Se actualizaron ambas dependencias y `mvn verify` terminó correctamente. Fuentes: `https://commons.apache.org/proper/commons-text/security.html`, `https://commons.apache.org/proper/commons-text/changes.html`, `https://github.com/quarkusio/quarkus/security/advisories/GHSA-qcxp-gm7m-4j5v`, `https://quarkus.io/releases/`.
- **Nuevo diff aprobado:** el BOM migró de Quarkus `3.27.6.1` a la LTS `3.40.1.1` y el artefacto de prueba migró de `quarkus-junit5` a `quarkus-junit` según la guía oficial 3.31 (`auth/pom.xml:11-14,52-55`). No cambió código de producción ni Commons Text.
- **Nueva caracterización antes/después:** se añadió el caso de campo JSON desconocido (`auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:29-44`); pasó 1/1 con `3.27.6.1` y 1/1 con `3.40.1.1`, ambos con código 0 y la misma expectativa.
- **Nueva corrección y regresión:** `mvn verify` pasó 26/26 después del cambio del BOM y nuevamente tras renombrar el artefacto de JUnit. El árbol final confirmó Quarkus `3.40.1.1` y Commons Text `1.15.0`. Fuentes oficiales: `https://quarkus.io/releases/`, `https://quarkus.io/blog/quarkus-3-40-released/` y `https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.31`.
- **Revisión de cierre:** parcial. OWASP Dependency-Check 13.0.0 terminó con código 1 antes del análisis porque NVD requiere una API key y no había caché; no se usaron secretos reales. H-AUTH-07 permanece `Pendiente` hasta obtener un SCA completo y la revisión humana final.

### Comprobaciones finales

- Cobertura de líneas tras la migración: 145/168 = 86.31%, superior a la meta de 85%.
- Cobertura de ramas informativa tras la migración: 33/42 = 78.57%; AGENTS.md no establece un gate separado de ramas.
- CodeQL remoto: no ejecutado.
- Dependency Review remoto: no ejecutado.
- No se usaron secretos reales ni se modificaron `api/` o `web/`.
