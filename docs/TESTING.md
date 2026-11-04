# Pruebas de endpoints

## auth

La tabla describe los casos del contrato de auth en `docs/ENDPOINTS.md`.
Los resultados observados para `/q/health` corresponden al perfil Quarkus de
pruebas; no aseguran la configuracion de un proxy o despliegue externo.

| endpoint | caso | tipo | resultado esperado |
|---|---|---|---|
| `GET /q/health` | Consulta JSON sin header `Authorization` (`reportsHealthStatusWithoutAuthorizationHeader`, `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:32`) | Camino feliz y limite de autenticacion observado | `200`; `status` igual a `UP`. No se envia autenticacion en la prueba. |
| `GET /q/health` | Request repetida / limite del servicio | Pendiente | El contrato no define limites, frecuencia ni otro comportamiento de borde. No se inventa una expectativa. |
| `GET /q/health` | Error | Pendiente | `docs/ENDPOINTS.md` no documenta respuestas de error; los status y cuerpos emitidos por la extension o el despliegue son pregunta abierta. |
| `POST /auth/login` | Credenciales validas de student (`logsInWithWorkshopUser`, `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:42`) | Camino feliz | `200`; respuesta con `user.username=student`, `user.role=STUDENT`, `expiresIn=3600` y token no nulo. |
| `POST /auth/login` | Credenciales validas de trainer (`logsInWithWorkshopTrainer`, `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:69`) | Camino feliz | `200`; `user.username=trainer`, `user.role=TRAINER`, `expiresIn=3600` y token no nulo. |
| `POST /auth/login` | `username` de espacios (`rejectsEmptyUsername`, `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:94`) | Caso borde | `400`; `{"message":"Username is required"}`. |
| `POST /auth/login` | `password` de espacios (`rejectsEmptyPassword`, `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:105`) | Caso borde | `400`; `{"message":"Password is required"}`. |
| `POST /auth/login` | Request JSON `null` (`rejectsNullRequest`, `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:116`) | Error documentado | `400`; `{"message":"Request is null"}`. |
| `POST /auth/login` | `username` null (`rejectsNullUsername`, `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:127`) | Error documentado | `400`; `{"message":"Username is required"}`. |
| `POST /auth/login` | `password` null (`rejectsNullPassword`, `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:138`) | Error documentado | `400`; `{"message":"Password is required"}`. |
| `POST /auth/login` | Password incorrecto (`rejectsInvalidCredentials`, `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:58`) | Error documentado | `401`; `{"message":"Invalid user student or password"}`. |
| `POST /auth/login` | Usuario inexistente (`rejectsUnknownUsername`, `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:83`) | Error documentado | `401`; `{"message":"Invalid user unknown-user or password"}`. |
| `POST /auth/login` | Excepcion de persistencia simulada (`doesNotExposeInternalErrorDetails`, `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:195`) | Error documentado; prueba unitaria del recurso | `500`; `{"message":"Authentication failed"}` y sin detalle interno. El test invoca `AuthResource.login` directamente, no induce la excepcion a traves de HTTP. |

No se prueba JSON malformado ni `Content-Type` no soportado: `docs/ENDPOINTS.md`
los deja como preguntas abiertas, sin status ni cuerpo confirmados. Para
`/q/health`, el caso sin `Authorization` es el unico limite de request
comprobable con el contrato actual; no se definen campos que puedan vaciarse,
duplicarse o llevarse a un limite. Los errores y otros casos de borde quedan
pendientes de un contrato confirmado.

## Codigo de los tests

Los tests viven en
`auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`.
Estos extractos son los metodos de prueba actuales para los casos de la tabla.

```java
@Test
void reportsHealthStatusWithoutAuthorizationHeader() {
    given()
            .accept("application/json")
            .when().get("/q/health")
            .then()
            .statusCode(200)
            .body("status", is("UP"));
}

@Test
void logsInWithWorkshopUser() {
    given()
            .contentType("application/json")
            .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(200)
            .body("user.username", is("student"))
            .body("user.displayName", is("Workshop Student"))
            .body("user.role", is("STUDENT"))
            .body("expiresIn", is(3600))
            .body("debug", is("authenticated-with-legacy-flow"))
            .body("token", notNullValue());
}

@Test
void logsInWithWorkshopTrainer() {
    given()
            .contentType("application/json")
            .body("{\"username\":\"trainer\",\"password\":\"Trainer123!\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(200)
            .body("user.username", is("trainer"))
            .body("user.role", is("TRAINER"))
            .body("expiresIn", is(3600))
            .body("token", notNullValue());
}

@Test
void rejectsEmptyUsername() {
    given()
            .contentType("application/json")
            .body("{\"username\":\" \",\"password\":\"Workshop123!\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("message", is("Username is required"));
}

@Test
void rejectsEmptyPassword() {
    given()
            .contentType("application/json")
            .body("{\"username\":\"student\",\"password\":\" \"}")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("message", is("Password is required"));
}

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
void rejectsNullUsername() {
    given()
            .contentType("application/json")
            .body("{\"username\":null,\"password\":\"Workshop123!\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("message", is("Username is required"));
}

@Test
void rejectsNullPassword() {
    given()
            .contentType("application/json")
            .body("{\"username\":\"student\",\"password\":null}")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("message", is("Password is required"));
}

@Test
void rejectsInvalidCredentials() {
    given()
            .contentType("application/json")
            .body("{\"username\":\"student\",\"password\":\"Incorrecta123!\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(401)
            .body("message", is("Invalid user student or password"));
}

@Test
void rejectsUnknownUsername() {
    given()
            .contentType("application/json")
            .body("{\"username\":\"unknown-user\",\"password\":\"AnyPassword123!\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(401)
            .body("message", is("Invalid user unknown-user or password"));
}

@Test
void doesNotExposeInternalErrorDetails() {
    AuthResource resource = new AuthResource();
    resource.authService = new AuthService();
    resource.authService.database = new LegacyDatabase() {
        @Override
        public UserRecord findByCredentials(String username, String password) {
            throw new IllegalStateException("fixture database failure");
        }
    };
    LoginRequest request = new LoginRequest();
    request.username = "student";
    request.password = "Workshop123!";

    Response response = resource.login(request);
    Map<?, ?> body = (Map<?, ?>) response.getEntity();

    assertEquals(500, response.getStatus());
    assertEquals("Authentication failed", body.get("message"));
    assertFalse(body.containsKey("exception"));
    assertFalse(body.get("message").toString().contains("fixture database failure"));
}
```

## Ejecuciones y cobertura

El fixture de integracion usa SQLite bajo `auth/target/` y las cuentas ficticias
`student` y `trainer`; la clave JWT de integracion se genera para la prueba.
`AUTH_DB_URL` apunta a una base aislada con el esquema legacy y esas cuentas.
No se usan credenciales reales. Los comandos Maven se ejecutaron con directorio
de trabajo `auth/`.

La prueba `migratesLegacyPlainTextPasswordStorage` ahora crea su propio SQLite
temporal con el esquema legacy, ejecuta la migracion y comprueba el hash y la
eliminacion de la columna de texto plano. No requiere `AUTH_DB_URL`; los valores
de esa variable en la tabla siguiente pertenecen a las ejecuciones historicas.

| componente | IDs / alcance | comando y directorio | codigo de salida | resultado |
|---|---|---|---|---|
| auth | H-AUTH-01 a H-AUTH-06; regresion de auth y endpoints documentados | Antes de agregar el test directo de health: `mvn -B verify`, cwd `auth/`, `AUTH_DB_URL=jdbc:sqlite:target/h-auth-20261104-testing-baseline.db` | 0 | `BUILD SUCCESS`; 20 pruebas, 0 fallos, 0 errores. No habia una asercion directa de `GET /q/health`. |
| auth | H-AUTH-01 a H-AUTH-06; regresion de auth y endpoints documentados | Primera ejecucion despues de agregar el test HTTP de health: `mvn -B verify`, cwd `auth/`, `AUTH_DB_URL=jdbc:sqlite:target/h-auth-20261104-testing-post.db` | 0 | `BUILD SUCCESS`; 21 pruebas, 0 fallos, 0 errores. La unica diferencia de la prueba HTTP con la ejecucion final fue renombrar el metodo para explicitar que no envia `Authorization`. |
| auth | H-AUTH-01 a H-AUTH-06; regresion de auth y endpoints documentados | Despues de agregar el test de health: `mvn -B verify`, cwd `auth/`, `AUTH_DB_URL=jdbc:sqlite:target/h-auth-20261104-testing-final.db` | 0 | `BUILD SUCCESS`; 21 pruebas, 0 fallos, 0 errores. `AuthResourceTest`: 16; `LegacyDatabaseTest`: 1; `PasswordHashServiceTest`: 2; `TokenServiceTest`: 2. |

**Cobertura:** JaCoCo Maven Plugin 0.8.12; reporte generado por `mvn -B
verify` en `auth/target/site/jacoco/`. Metodo: sumar `LINE_COVERED` y
`LINE_MISSED` de las clases de produccion del paquete
`com.bcp.workshop.auth` en `jacoco.csv`. Resultado de esta ejecucion: 171/180
lineas cubiertas, **95,0 %**, superior al 85 %. En esta tarea solo se modifico
una prueba; JaCoCo no mide cobertura del codigo de test. No se modifico codigo
de produccion.

### Fallos

- Ninguno en las ejecuciones registradas.

### Pendientes

- Confirmar con el contrato/despliegue los errores posibles de `GET /q/health`;
  por ahora no existe respuesta de error documentada que se pueda probar.
- Confirmar si el despliegue o un proxy exige autenticacion para health; el test
  sin `Authorization` solo refleja el perfil de pruebas local.

### Pruebas propuestas

- Agregar pruebas de error y de borde para health solo despues de acordar los
  status/cuerpos y los limites que debe garantizar el endpoint.
- Agregar pruebas HTTP para la falla 500 cuando exista un mecanismo soportado
  para simular una falla interna; el test actual comprueba el recurso aislado.

## Documentos y fuentes consultados

- `docs/ENDPOINTS.md`: contrato auth, errores documentados y preguntas abiertas.
- `README.md`: herramientas y comandos documentados para auth.
- `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`: casos y
  codigo de prueba.
- `auth/pom.xml`: configuracion JaCoCo.
- `PLAN.md`: IDs y registro historico de ejecuciones.
