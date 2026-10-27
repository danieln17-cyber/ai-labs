# Evidencia de pruebas

## Componente auth

### Alcance contractual

Esta sección verifica únicamente los endpoints documentados para `auth` en `docs/ENDPOINTS.md`: `POST /auth/login` y `GET /q/health`. Las ejecuciones se relacionan con H-AUTH-01 a H-AUTH-07. Para H-AUTH-07, la migración y las pruebas están completas, pero el cierre sigue pendiente por falta de un SCA completo.

### Matriz de casos

| endpoint | caso | tipo | resultado esperado |
|---|---|---|---|
| `POST /auth/login` | Credenciales ficticias válidas y `client` informado | Camino feliz | `200`; token no nulo, `expiresIn: 3600`, usuario `student` y marcador `authenticated-with-legacy-flow`. |
| `POST /auth/login` | Credenciales válidas con un campo JSON desconocido | Caracterización / H-AUTH-07 | `200` con el mismo cuerpo exitoso; el campo adicional se ignora antes y después de migrar Quarkus. |
| `POST /auth/login` | Usuario ficticio que contiene una comilla | Caso borde / H-AUTH-01 | `401` con el mensaje contractual; no se produce error SQL ni `500`. |
| `POST /auth/login` | `username` compuesto solo por espacios | Caso borde | `400` con `{"message":"Username is required"}`. |
| `POST /auth/login` | Cuerpo JSON `null` | Error `400` | `400` con `{"message":"Request is null"}`. |
| `POST /auth/login` | Falta `username` | Error `400` | `400` con `{"message":"Username is required"}`. |
| `POST /auth/login` | Falta `password` | Error `400` | `400` con `{"message":"Password is required"}`. |
| `POST /auth/login` | JSON malformado | Error `400` | `400` con cuerpo vacío. |
| `POST /auth/login` | Usuario o contraseña inválidos | Error `401` | `401` con `{"message":"Invalid user unknown-user or password"}`. |
| `POST /auth/login` | `Content-Type: text/plain` | Error `415` | `415` con cuerpo vacío. |
| `POST /auth/login` | El servicio de autenticación lanza una excepción | Error `500` / H-AUTH-05 | `500` con `{"message":"Authentication service unavailable"}`, sin detalle interno. |
| `GET /q/health` | Request con `Accept: application/json` | Camino feliz | `200`, media type JSON, `status: UP` y `checks: []`. |
| `GET /q/health` | Request mínimo, sin headers explícitos ni cuerpo | Caso borde | `200`, `status: UP` y `checks: []`; el contrato no exige headers ni campos. |

`GET /q/health` no tiene respuestas de error propias documentadas en el contrato actual, por lo que no se inventó un caso `DOWN`. Si se incorpora un health check capaz de fallar, primero debe caracterizarse y actualizarse `docs/ENDPOINTS.md`.

### Código de los tests

Los casos ya existentes permanecen en:

- `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:13-86`: éxito, `401`, comilla, usuario ausente y contraseña ausente.
- `auth/src/test/java/com/bcp/workshop/auth/AuthErrorResponseTest.java:12-28`: `500` genérico sin fuga de detalles.
- `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:133-154`: CORS permitido y rechazado para H-AUTH-04.

Para H-AUTH-07 se escribió antes de cambiar el BOM esta caracterización (`auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:29-44`):

```java
@Test
void ignoresUnknownJsonFieldsWithoutChangingSuccessfulLogin() {
    given()
            .contentType("application/json")
            .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\",\"futureField\":\"ignored\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(200)
            .body("user.username", is("student"))
            .body("user.displayName", is("Workshop Student"))
            .body("user.role", is("STUDENT"))
            .body("token", notNullValue())
            .body("expiresIn", is(3600))
            .body("debug", is("authenticated-with-legacy-flow"));
}
```

Se añadieron al test HTTP de login estos casos que faltaban (`auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:89-131`):

```java
@Test
void rejectsNullRequest() {
    given()
            .contentType("application/json")
            .body("null")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("message", is("Request is null"));
}

@Test
void rejectsBlankUsername() {
    given()
            .contentType("application/json")
            .body("{\"username\":\"   \",\"password\":\"Workshop123!\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("message", is("Username is required"));
}

@Test
void rejectsMalformedJsonWithEmptyBody() {
    given()
            .contentType("application/json")
            .body("{\"username\":")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body(is(""));
}

@Test
void rejectsUnsupportedMediaTypeWithEmptyBody() {
    given()
            .contentType("text/plain")
            .body("student")
            .when().post("/auth/login")
            .then()
            .statusCode(415)
            .body(is(""));
}
```

Se creó el test HTTP de salud (`auth/src/test/java/com/bcp/workshop/auth/HealthEndpointTest.java:1-34`):

```java
@QuarkusTest
class HealthEndpointTest {
    @Test
    void reportsServiceAsUp() {
        given()
                .accept("application/json")
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("status", is("UP"))
                .body("checks", empty());
    }

    @Test
    void acceptsMinimalRequestWithoutHeadersOrBody() {
        when()
                .get("/q/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks", empty());
    }
}
```

### Ejecuciones reales

| fecha | componente / IDs | directorio | comando | código de salida | resultado |
|---|---|---|---|---:|---|
| 2026-10-27 | auth / H-AUTH-01 a H-AUTH-06 | `auth/` | `mvn verify` | `1` | Fallo de infraestructura antes de compilar: dentro del sandbox Maven intentó crear `C:\.m2\repository` y recibió acceso denegado. No es un resultado de las pruebas. |
| 2026-10-27 | auth / H-AUTH-01 a H-AUTH-06 | `auth/` | `mvn verify` | `0` | `BUILD SUCCESS`: 25 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas. Incluye 11 de `AuthResourceTest`, 2 de `HealthEndpointTest` y 12 pruebas de servicio, seguridad y repositorio. |
| 2026-10-27 | auth / H-AUTH-07 | `auth/` | `mvn "-Dtest=AuthResourceTest#ignoresUnknownJsonFieldsWithoutChangingSuccessfulLogin" test` con Quarkus `3.27.6.1` | `0` | Caracterización previa: 1 prueba, 0 fallos; campos desconocidos ignorados y contrato exitoso conservado. |
| 2026-10-27 | auth / H-AUTH-07 | `auth/` | `mvn dependency:tree "-Dincludes=io.quarkus:*,org.apache.commons:commons-text"` antes del cambio | `0` | Confirmó Quarkus `3.27.6.1` y Commons Text `1.15.0`. |
| 2026-10-27 | auth / H-AUTH-07 | `auth/` | La misma caracterización con Quarkus `3.40.1.1` | `0` | Caracterización posterior: 1 prueba, 0 fallos y expectativas sin cambios. |
| 2026-10-27 | auth / H-AUTH-07 | `auth/` | `mvn verify` después del BOM | `0` | 26 pruebas correctas y reporte JaCoCo; reveló relocation de `quarkus-junit5`. |
| 2026-10-27 | auth / H-AUTH-07 | `auth/` | `mvn verify` después de cambiar a `quarkus-junit` | `0` | `BUILD SUCCESS`: 26 pruebas, 0 fallos, 0 errores, 0 omitidas y sin advertencia de relocation. |
| 2026-10-27 | auth / H-AUTH-07 | `auth/` | `mvn org.owasp:dependency-check-maven:13.0.0:check "-DfailBuildOnCVSS=7"` | `1` | SCA no ejecutado: NVD rechazó la actualización sin API key y no había datos en caché; no produjo un resultado de vulnerabilidades. |
| 2026-10-27 | auth / H-AUTH-07 | `auth/` | Árbol efectivo final con `mvn dependency:tree` | `0` | Confirmó extensiones Quarkus y `quarkus-junit` `3.40.1.1`; Commons Text `1.15.0`. |

La segunda ejecución histórica usó el repositorio Maven normal del usuario fuera del sandbox después de la autorización correspondiente. Durante `AuthErrorResponseTest` aparece en el log la excepción ficticia `database-details`; es la entrada deliberada del test y la prueba pasó al confirmar que esa cadena no llega al response.

### Cobertura

- Reporte: `auth/target/site/jacoco/index.html`.
- Datos para el cálculo: `auth/target/site/jacoco/jacoco.csv` generado por JaCoCo `0.8.12` durante `mvn verify` (`auth/pom.xml:90-107`).
- Método: sumar `LINE_COVERED` y `LINE_MISSED` de las 13 clases de producción del bundle `workshop-auth-backend`; porcentaje = `LINE_COVERED / (LINE_COVERED + LINE_MISSED) × 100`.
- Resultado real después de migrar Quarkus: `145 / (145 + 23) = 145 / 168 = 86.31 %` de líneas.
- Comprobaciones adicionales: `739 / 835 = 88.50 %` de instrucciones y `33 / 42 = 78.57 %` de ramas.
- Umbral: el resultado de líneas supera la meta de 85 %. H-AUTH-07 modificó dependencias y pruebas, no líneas de producción; la métrica de todo el módulo es más amplia y confirma que el working tree refactorizado conserva la meta.

El `pom.xml` genera el reporte, pero no contiene una regla `jacoco:check` que haga fallar el build bajo 85 % (`auth/pom.xml:90-108`). No se redujo ni se eliminó ningún quality gate existente.

### Fallos

- Fallo de infraestructura inicial de `mvn verify`: código `1` por acceso denegado a `C:\.m2\repository` dentro del sandbox. La repetición autorizada con el mismo comando terminó con código `0`.
- SCA de H-AUTH-07: OWASP Dependency-Check terminó con código `1` antes de analizar porque NVD requiere API key y no existía caché local. No se interpreta como hallazgos cero ni como aprobación.
- Fallos funcionales en la ejecución válida: ninguno.

### Pendientes

- H-AUTH-07: la migración a Quarkus `3.40.1.1`, la regresión y la cobertura están completas; falta un SCA que pueda acceder a una fuente de vulnerabilidades.
- CodeQL y Dependency Review: no ejecutados; son controles remotos fuera de este recorrido local.
- El contrato no define una respuesta no saludable para `GET /q/health`; no hay un test de error hasta que exista ese comportamiento acordado y documentado.
- Falta la revisión humana posterior porque este paso añadió pruebas después de la revisión local previa.

### Pruebas propuestas no ejecutadas

- Repetir OWASP Dependency-Check con una API key de NVD aportada mediante el entorno o ejecutar el control SCA remoto acordado; no guardar la credencial en el repositorio.
- Ninguna prueba funcional adicional dentro del contrato actual. Una futura respuesta `DOWN` de salud deberá convertirse en prueba solo después de incorporar un health check y documentar su contrato.
