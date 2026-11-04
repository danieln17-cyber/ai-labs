# Contrato HTTP

## auth

Base local observada: `http://localhost:8080` (README.md). En el perfil de
desarrollo la aplicacion enlaza a `127.0.0.1`; en produccion el host se
configura a `0.0.0.0` (`auth/src/main/resources/application.properties:1-3`).
El despliegue puede cambiar host y puerto.

### GET /q/health

1. **Que hace:** permite a un integrador u operador comprobar si el servicio de
   autenticacion esta disponible. La ruta pertenece a SmallRye Health, incluida
   en `auth/pom.xml:35-38`, y se publica en el README como
   `http://localhost:8080/q/health`.
2. **Autenticacion:** `auth/` no declara una politica ni un header
   `Authorization` requerido para esta ruta. El acceso efectivo puede depender
   de configuracion externa o de un proxy; confirmarlo es una pregunta abierta.
3. **Request:**

   `auth/` no define parametros ni un cuerpo para esta ruta. Al ser `GET`, no se
   envia ejemplo JSON.

   | campo | tipo | obligatorio | descripcion |
   |---|---|---|---|
   | Ninguno definido por `auth/` | — | No | El componente no declara campos de request. |

4. **Response exitosa:** `200 OK` con el estado agregado `UP`, de acuerdo con
   el contrato de SmallRye Health. La extension publica la ruta; `auth/` no
   contiene checks de salud propios. Referencia del formato:
   [guia SmallRye Health de Quarkus 3.27](https://quarkus.io/version/3.27/guides/smallrye-health/).

   | campo | tipo | descripcion |
   |---|---|---|
   | `status` | string | Estado agregado; `UP` para la respuesta exitosa. |
   | `checks` | array | Checks aportados por la extension o la aplicacion; `auth/` no declara checks propios. |

   Ejemplo del formato de la extension cuando no hay checks registrados por la
   aplicacion:

   ```json
   {
     "status": "UP",
     "checks": []
   }
   ```

5. **Respuestas de error:** `auth/` no implementa respuestas de error para
   esta ruta ni define sus status/cuerpos. **Pregunta abierta:** confirmar los
   status y cuerpos emitidos por la extension y por cada configuracion de
   despliegue. No hay un ejemplo de error verificable contra codigo propio.

6. **Notas de seguridad:** `auth/` no declara checks propios ni define datos
   de base de datos, configuracion o excepciones para esta respuesta.
   Confirmar si el despliegue expone o protege esta ruta.

### POST /auth/login

1. **Que hace:** autentica una cuenta contra el servicio y, si las credenciales
   son validas, entrega la identidad basica y un token para el cliente
   (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:25-42`,
   `auth/src/main/java/com/bcp/workshop/auth/AuthService.java:19-47`).
2. **Autenticacion:** no requiere un token previo. Envia
   `Content-Type: application/json`. No se define header `Authorization` para
   esta operacion (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:13-14`).
3. **Request:**

   | campo | tipo | obligatorio | descripcion |
   |---|---|---|---|
   | `username` | string | Si | Nombre de usuario. Nulo o vacio/solo espacios produce 400. |
   | `password` | string | Si | Password de la cuenta. Nulo o vacio/solo espacios produce 400. |
   | `client` | string | No | Campo aceptado por el DTO; no se consulta en el flujo de autenticacion. |

   Tipos y campos: `auth/src/main/java/com/bcp/workshop/auth/LoginRequest.java:3-7`.
   Validaciones: `auth/src/main/java/com/bcp/workshop/auth/AuthService.java:20-25`.

   Ejemplo:

   ```json
   {
     "username": "student",
     "password": "Workshop123!",
     "client": "angular-workshop"
   }
   ```

   Es una cuenta ficticia de desarrollo documentada en `README.md`; no usar
   credenciales reales en ejemplos o pruebas.

4. **Response exitosa:** `200 OK`. `expiresIn` es 3600 segundos. El valor de
   `token` se genera en runtime a partir de la clave privada configurada.

   | campo | tipo | descripcion |
   |---|---|---|
   | `token` | string | JWT firmado, con `sub` igual al username, `groups` con el rol e `iat`/`exp`; expira una hora despues de su emision. |
   | `expiresIn` | number | Duracion del token en segundos: `3600`. |
   | `user` | object | Datos basicos de la cuenta autenticada. |
   | `user.username` | string | Nombre de usuario. |
   | `user.displayName` | string | Nombre de presentacion, escapado para HTML por el servicio. |
   | `user.role` | string | Rol de la cuenta, por ejemplo `STUDENT` o `TRAINER`. |
   | `debug` | string | Valor fijo actual: `authenticated-with-legacy-flow`. No usar como dato de autorizacion. |

   DTO: `auth/src/main/java/com/bcp/workshop/auth/LoginResponse.java:3-5`.
   Claims y duracion: `auth/src/main/java/com/bcp/workshop/auth/TokenService.java:14-34`.
   Mapeo de usuario y respuesta: `auth/src/main/java/com/bcp/workshop/auth/AuthService.java:36-47`.

   Ejemplo del esquema JSON. El marcador de `token` no es un valor devuelto por
   el servidor ni un token utilizable; una respuesta real contiene un JWT
   firmado para la clave y el instante de emision de ese despliegue.

   ```json
   {
     "token": "<JWT firmado generado en runtime>",
     "expiresIn": 3600,
     "user": {
       "username": "student",
       "displayName": "Workshop Student",
       "role": "STUDENT"
     },
     "debug": "authenticated-with-legacy-flow"
   }
   ```

5. **Respuestas de error:**

   - **400 Bad Request:** request JSON `null`.

     ```json
     {
       "message": "Request is null"
     }
     ```

   - **400 Bad Request:** `username` nulo, vacio o compuesto solo por espacios.

     ```json
     {
       "message": "Username is required"
     }
     ```

   - **400 Bad Request:** `password` nulo, vacio o compuesto solo por espacios.

     ```json
     {
       "message": "Password is required"
     }
     ```

   - **401 Unauthorized:** username inexistente o password incorrecto. El
     mensaje incluye el username recibido.

     ```json
     {
       "message": "Invalid user student or password"
     }
     ```

   - **500 Internal Server Error:** una excepcion durante la autenticacion.
     El cuerpo es generico; el detalle se registra en el servidor.

     ```json
     {
       "message": "Authentication failed"
     }
     ```

     Respuestas y status: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:28-51`;
     resultados de validacion/credenciales: `auth/src/main/java/com/bcp/workshop/auth/AuthService.java:20-32`.

   **Pregunta abierta:** el codigo de la aplicacion no define el status ni el
   cuerpo para JSON malformado, tipo de contenido no soportado u otros errores
   generados antes de entrar al recurso por Quarkus/HTTP; por tanto, no se
   documentan como respuestas confirmadas ni se inventan ejemplos.

6. **Notas de seguridad:**

   - El token es JWT firmado con clave privada cuya ubicacion se configura
     mediante `AUTH_JWT_PRIVATE_KEY_LOCATION`; la aplicacion falla al iniciar
     si no se configura (`auth/src/main/resources/application.properties:9`,
     `auth/src/main/java/com/bcp/workshop/auth/TokenService.java:18-25`).
     Configura una clave privada segura por entorno; no la incluyas en Git.
   - El token incluye identidad, grupo/rol y expiracion real de una hora.
     `expiresIn` informa esa duracion. El cliente debe tratar el token como
     credencial y no confiar en `debug` para autorizar.
   - La respuesta 401 refleja el username en el mensaje; los clientes no deben
     asumir que ese texto es un mensaje neutral respecto a la existencia de
     cuentas.
   - CORS permite por defecto `http://localhost:4200`; configura los origenes
     del despliegue con `AUTH_CORS_ORIGINS`
     (`auth/src/main/resources/application.properties:4-8`).
   - El request transporta un password. Usa HTTPS en despliegues fuera del
     entorno local y no registres credenciales ni tokens.

### Contrato y preguntas abiertas de auth

- **Observado:** el codigo actual define el endpoint de login y SmallRye Health
  expone la ruta de salud. La respuesta de health no tiene checks propios
  declarados en el componente.
- **Acordado:** login conserva ruta, validaciones, status 400/401/200 y campos
  de respuesta; el token ahora es JWT firmado y un error interno responde 500
  con mensaje generico, segun `PLAN.md` y `HALLAZGOS.md`.
- **Pendiente:** coordinar la migracion del formato del token con consumidores
  externos a `auth/`; configurar clave JWT y origenes CORS en cada despliegue.
  Confirmar el cuerpo de errores HTTP de framework y el comportamiento exacto
  de health cuando hay checks adicionales o fallidos.
