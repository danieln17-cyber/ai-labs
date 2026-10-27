# Contratos HTTP

Este documento registra el contrato observado del componente `auth`. Los ejemplos se verificaron contra el código actual y, donde se indica, mediante solicitudes al servicio local con datos ficticios. Los errores genéricos por usar una ruta o un método inexistentes no forman parte del contrato de estos endpoints.

## Componente auth

### `POST /auth/login`

#### 1. Qué hace

Autentica a una persona mediante usuario y contraseña y, si las credenciales son válidas, entrega un token temporal junto con los datos de su usuario (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:24-40`; `auth/src/main/java/com/bcp/workshop/auth/AuthService.java:24-36`).

#### 2. Autenticación y headers

No requiere autenticación previa y no requiere header `Authorization`. El request debe usar exactamente `Content-Type: application/json`; la respuesta usa JSON porque el recurso declara `application/json` (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:14-16`).

Para llamadas desde navegador, el origen permitido por defecto es `http://localhost:4200`; puede configurarse con `AUTH_CORS_ORIGINS` (`auth/src/main/resources/application.properties:4-7`).

#### 3. Request

| campo | tipo | obligatorio | descripción |
|---|---|---|---|
| `username` | string | Sí | Identificador del usuario. No puede ser nulo, vacío ni contener solo espacios (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:30-31,49-50`). |
| `password` | string | Sí | Contraseña en texto para esta solicitud. No puede ser nula, vacía ni contener solo espacios (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:33-34,49-50`). |
| `client` | string | No | Identificador informativo del cliente. El modelo lo acepta, pero el flujo actual no lo utiliza para autenticar (`auth/src/main/java/com/bcp/workshop/auth/LoginRequest.java:3-6`; `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:38`). |

Ejemplo JSON completo:

```json
{
  "username": "student",
  "password": "Workshop123!",
  "client": "angular-workshop"
}
```

Las credenciales del ejemplo son datos ficticios del taller y están ejercitadas por la prueba de caracterización (`README.md:44`; `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:13-26`). Los campos JSON desconocidos se ignoran (`auth/src/main/resources/application.properties:8`).

#### 4. Response exitosa

Código de estado: `200 OK`.

| campo | tipo | descripción |
|---|---|---|
| `token` | string | JWT compacto firmado con HMAC-SHA256. Contiene las claims `sub`, `role`, `iat` y `exp` (`auth/src/main/java/com/bcp/workshop/auth/TokenService.java:20-22,42-46,109`). |
| `expiresIn` | integer | Vigencia del token en segundos; actualmente `3600` (`auth/src/main/java/com/bcp/workshop/auth/TokenService.java:20`; `auth/src/main/java/com/bcp/workshop/auth/AuthService.java:36`). |
| `user` | object | Usuario autenticado (`auth/src/main/java/com/bcp/workshop/auth/LoginResponse.java:3`). |
| `user.username` | string | Identificador del usuario autenticado (`auth/src/main/java/com/bcp/workshop/auth/UserResponse.java:3`). |
| `user.displayName` | string | Nombre visible, escapado para HTML antes de incluirse en la respuesta (`auth/src/main/java/com/bcp/workshop/auth/AuthService.java:30-35`). |
| `user.role` | string | Rol del usuario autenticado (`auth/src/main/java/com/bcp/workshop/auth/UserResponse.java:3`). |
| `debug` | string | Indicador de compatibilidad del flujo; actualmente `authenticated-with-legacy-flow` (`auth/src/main/java/com/bcp/workshop/auth/AuthService.java:11,36`). |

Ejemplo JSON completo capturado localmente con el usuario ficticio y una clave de firma descartable:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzdHVkZW50Iiwicm9sZSI6IlNUVURFTlQiLCJpYXQiOjE3OTMwNzg3MDIsImV4cCI6MTc5MzA4MjMwMn0.6oR9WPnSg0cawfV9P2hq2YXrl09dXIex0VGq7BFSl54",
  "expiresIn": 3600,
  "user": {
    "username": "student",
    "displayName": "Workshop Student",
    "role": "STUDENT"
  },
  "debug": "authenticated-with-legacy-flow"
}
```

El token del ejemplo es solo evidencia de formato: expira y no debe reutilizarse como credencial.

#### 5. Respuestas de error

| código de estado | cuándo ocurre | ejemplo de cuerpo |
|---|---|---|
| `400 Bad Request` | El cuerpo JSON es `null` o no produce un objeto de request. | `{"message":"Request is null"}` |
| `400 Bad Request` | `username` falta, es `null`, está vacío o contiene solo espacios. | `{"message":"Username is required"}` |
| `400 Bad Request` | `password` falta, es `null`, está vacío o contiene solo espacios. | `{"message":"Password is required"}` |
| `400 Bad Request` | El cuerpo no es JSON válido. Esta respuesta la genera el framework antes de entrar al método y el cuerpo observado está vacío. | *(cuerpo vacío)* |
| `401 Unauthorized` | El usuario no existe o la contraseña no coincide. | `{"message":"Invalid user unknown-user or password"}` |
| `415 Unsupported Media Type` | El request usa un tipo distinto de `application/json`. La respuesta observada tiene el cuerpo vacío. | *(cuerpo vacío)* |
| `500 Internal Server Error` | Ocurre una excepción inesperada durante la autenticación, por ejemplo al consultar el repositorio o generar el token. | `{"message":"Authentication service unavailable"}` |

Los cuerpos controlados `400`, `401` y `500` están definidos en `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:26-45,53-60`. Las pruebas automatizadas existentes verifican el éxito, credenciales inválidas y campos obligatorios en `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:13-70`. En la comprobación local de documentación se observaron además el `400` sin cuerpo para JSON malformado y el `415` sin cuerpo para `text/plain`; no se forzó una falla interna para producir el `500`.

#### 6. Notas de seguridad

- La contraseña se compara mediante el verificador de hashes; no se incluye en la respuesta (`auth/src/main/java/com/bcp/workshop/auth/AuthService.java:24-27`).
- El token está firmado con HMAC-SHA256, valida la firma en tiempo constante y rechaza tokens vencidos (`auth/src/main/java/com/bcp/workshop/auth/TokenService.java:49-64,67-74`).
- La clave de firma se obtiene de `AUTH_TOKEN_SECRET` y debe tener al menos 32 caracteres; no existe un valor por defecto en la configuración de producción (`auth/src/main/resources/application.properties:9`; `auth/src/main/java/com/bcp/workshop/auth/TokenService.java:28-37`).
- El token vence una hora después de su emisión. El consumidor debe tratar `expiresIn` como segundos y no confiar en un token una vez alcanzada su claim `exp` (`auth/src/main/java/com/bcp/workshop/auth/TokenService.java:20,42-46,60-64`).
- El mensaje `401` reproduce el `username` recibido. Los consumidores no deben insertar `message` como HTML sin escape (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:57-60`).

### `GET /q/health`

#### 1. Qué hace

Informa si el servicio de autenticación está disponible para recibir tráfico. El endpoint lo aporta la extensión SmallRye Health incluida por el componente (`auth/pom.xml:38-41`) y README lo publica como comprobación de disponibilidad (`README.md:44`).

#### 2. Autenticación y headers

No requiere autenticación ni header `Authorization`. No requiere `Content-Type` porque el request no tiene cuerpo. El response usa `Content-Type: application/json; charset=UTF-8`, verificado en la ejecución local.

#### 3. Request

| campo | tipo | obligatorio | descripción |
|---|---|---|---|
| — | — | — | Este request `GET` no tiene cuerpo ni campos. |

No existe un ejemplo JSON de request: la llamada correcta se envía sin cuerpo.

#### 4. Response exitosa

Código de estado: `200 OK`.

| campo | tipo | descripción |
|---|---|---|
| `status` | string | Estado global de salud; en la ejecución local fue `UP`. |
| `checks` | array | Resultados de comprobaciones de salud registradas. Actualmente el componente no define comprobaciones propias y el array observado está vacío. |

Ejemplo JSON completo observado localmente:

```json
{
  "status": "UP",
  "checks": []
}
```

#### 5. Respuestas de error

El componente `auth` no define una respuesta de error propia para este endpoint ni registra comprobaciones de salud que puedan devolver un detalle `DOWN`: la única respuesta comprobada en el estado actual fue `200` con `status: UP` y `checks: []`. Por tanto, no existe un cuerpo JSON de error del componente que pueda documentarse sin inventarlo.

Pregunta abierta: si en el futuro se registra una comprobación de salud que pueda fallar, habrá que caracterizar y documentar el código de estado y el cuerpo `DOWN` concretos antes de considerarlos parte del contrato.

#### 6. Notas de seguridad

- El endpoint es público y revela que el proceso está activo, pero el cuerpo actual no expone secretos, credenciales, rutas internas ni detalles de la base de datos.
- `UP` solo acredita la salud reportada por las comprobaciones registradas. Como `checks` está vacío, no demuestra por sí mismo conectividad con SQLite ni que unas credenciales puedan autenticarse.

## Preguntas abiertas

- ¿Debe conservarse a largo plazo el campo público `debug` del login? El código y las pruebas actuales lo hacen parte del contrato observado, pero no hay un contrato acordado adicional que defina su propósito para consumidores.
- Si se agregan comprobaciones de salud reales, falta acordar y caracterizar el contrato exacto de la respuesta no saludable de `GET /q/health`.

## Evidencia de comprobación local

El 27 de octubre de 2026 se inició el artefacto local de `auth` con un secreto ficticio y se consultaron ambos endpoints en `localhost:8080`. Se observaron los estados y cuerpos descritos arriba. El primer intento dentro del sandbox no pudo abrir `auth/target/quarkus-app/app/workshop-auth-backend-0.1.0.jar` por `AccessDeniedException`; al ejecutar el mismo artefacto con autorización fuera del sandbox, el servicio inició correctamente. No se modificó código ni configuración y no se ejecutaron pruebas automatizadas en este paso de documentación.
