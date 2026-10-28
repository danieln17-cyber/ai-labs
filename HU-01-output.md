# Feature Intake -> Spec Checklist — HU-01

## 1. Summary

- Fortalecer el token devuelto por `POST /auth/login` para incluir los claims mínimos `sub`, `client`, `iat` y `exp`.
- Obtener y validar `client` desde la solicitud, y reflejarlo en el token.
- Mantener coherencia verificable entre `expiresIn` y `exp - now`.
- Proteger el token, el secreto y demás valores sensibles en configuración y logs.
- Documentar la decisión técnica entre conservar HMAC o adoptar una librería JWT.

## 2. Scope

### Dentro del alcance

- Claims `sub`, `client`, `iat` y `exp`.
- Lectura y validación de `client` desde el request de login.
- Coherencia entre `expiresIn` y la expiración del token.
- Configuración de `AUTH_TOKEN_SECRET` para laboratorio y otros entornos.
- Prevención de tokens, secretos y valores sensibles en logs.
- Pruebas de login, claims, expiración y errores de autenticación.
- Registro de la decisión sobre HMAC frente a una librería JWT.

### Fuera del alcance

- La HU no confirma exclusiones adicionales.

## 3. Assumptions

- El alcance se limita al servicio de autenticación y a `POST /auth/login`.
- La respuesta debe continuar exponiendo `token`, `expiresIn` y `user`; cualquier cambio adicional del contrato requiere confirmación.
- Las pruebas usarán secretos, tokens y credenciales ficticios.
- No se presupone una tolerancia temporal, una regla de validación de `client` ni una tecnología JWT hasta resolverlas explícitamente.

## 4. Technical decisions to make (pending or resolved)

- **Resuelta:** los claims mínimos requeridos son `sub`, `client`, `iat` y `exp`.
- **Resuelta:** `client` proviene de la solicitud y debe validarse antes de incluirse en el token.
- **Pendiente:** tolerancia permitida al comparar `expiresIn` con `exp - now`.
- **Pendiente:** reglas de obligatoriedad, formato y valores válidos para `client`.
- **Pendiente:** conservar la implementación HMAC o adoptar una librería JWT.
- **Pendiente:** política exacta para el secreto por defecto en el laboratorio y su ausencia fuera de él.
- **Pendiente:** lista completa de valores considerados sensibles para las comprobaciones de logging.

## 5. Risks and mitigations

- **Un `client` ausente o inválido puede producir tokens ambiguos.**
  - Mitigación: definir reglas explícitas, validar antes de emitir el token y probar casos válidos e inválidos.
- **Una diferencia entre `expiresIn` y `exp` puede provocar expiraciones inconsistentes.**
  - Mitigación: derivar ambos valores de la misma referencia temporal y verificar la tolerancia acordada.
- **Tokens o secretos pueden quedar expuestos en logs.**
  - Mitigación: evitar su registro, usar datos ficticios en pruebas y revisar evidencia de logs redactados.
- **Un secreto por defecto fuera del laboratorio debilita la firma.**
  - Mitigación: eliminarlo fuera del laboratorio o limitarlo y documentarlo explícitamente como valor exclusivo de laboratorio.
- **Una implementación HMAC propia puede aumentar el riesgo de mantenimiento.**
  - Mitigación: documentar la evaluación frente a una librería JWT antes de decidir.

## 6. Acceptance criteria (measurable)

- Cada token emitido por un login válido contiene valores no nulos para `sub`, `client`, `iat` y `exp`.
- El claim `client` coincide con el valor validado recibido en la solicitud.
- `expiresIn` coincide con `exp - now` dentro de la tolerancia documentada.
- Existen pruebas automatizadas verdes para login válido, credenciales inválidas y token expirado.
- Las pruebas de claims verifican explícitamente `sub`, `client`, `iat` y `exp`.
- La revisión de logs encuentra cero tokens, secretos o valores clasificados como sensibles.
- Fuera del laboratorio no existe un secreto por defecto; si se conserva uno para laboratorio, queda identificado como exclusivo de ese entorno.
- La decisión HMAC frente a librería JWT queda documentada con su justificación.

## 7. Validation plan

### Unit tests

- Verificar creación y lectura de los cuatro claims mínimos.
- Verificar aceptación y rechazo de `client` según las reglas acordadas.
- Verificar expiración válida, vencida y coherencia de `expiresIn` con una referencia temporal controlada.
- Verificar firma válida y rechazo de tokens alterados.

### Functional tests

- Login válido: respuesta exitosa y token con claims esperados.
- Credenciales inválidas: respuesta contractual sin emisión de token.
- Request con `client` inválido o ausente: resultado definido por la decisión pendiente.
- Token expirado: rechazo comprobado.

### Security (minimum checks)

- Inspeccionar logs del flujo válido y de error para confirmar ausencia de tokens y secretos.
- Comprobar que la aplicación no arranca fuera del laboratorio con un secreto por defecto no autorizado.
- Verificar que los errores no devuelvan el secreto, el token ni detalles sensibles.
- Revisar la decisión documentada sobre HMAC o librería JWT.

### Required evidence (attachments)

- Salida de las pruebas unitarias y funcionales.
- Casos de claims y expiración con datos ficticios.
- Configuración documentada de `AUTH_TOKEN_SECRET` sin incluir su valor.
- Evidencia sanitizada de logs redactados.
- Documento de decisión técnica HMAC frente a librería JWT.

## 8. Suggested tasks by role

### Chapter/PO

- Confirmar el significado funcional de `client`.
- Acordar el comportamiento esperado cuando `client` falta o es inválido.
- Validar que el contrato resultante satisface la HU.

### Tech Lead

- Definir la tolerancia temporal y la referencia usada para `now`.
- Liderar y documentar la decisión HMAC frente a librería JWT.
- Definir la política del secreto para laboratorio y otros entornos.
- Revisar que el contrato de login permanezca trazable.

### Backend

- Incorporar y validar `client` en el flujo de autenticación.
- Emitir y verificar `sub`, `client`, `iat` y `exp`.
- Mantener coherentes `expiresIn` y la expiración del token.
- Añadir las pruebas unitarias y funcionales acordadas.

### Frontend

- Confirmar si el consumidor actual ya envía `client`.
- Adaptar el request solo si el contrato acordado lo requiere.
- Verificar que no se registren ni muestren tokens en diagnósticos del cliente.

### QA

- Preparar casos válidos, inválidos, ausentes y de límite para `client`.
- Verificar claims, expiración y coherencia de `expiresIn`.
- Ejecutar login válido, credenciales inválidas y token expirado.
- Adjuntar resultados y evidencia sanitizada.

### Cyber

- Revisar la gestión de `AUTH_TOKEN_SECRET` por entorno.
- Revisar los claims mínimos, la firma y la expiración.
- Comprobar que logs y errores no expongan valores sensibles.
- Participar en la decisión HMAC frente a librería JWT.

## 9. Open questions

- ¿`client` es obligatorio? ¿Qué formato, longitud y valores se aceptan?
- ¿Qué respuesta debe producir un `client` ausente o inválido?
- ¿Cuál es la tolerancia exacta para comparar `expiresIn` con `exp - now`?
- ¿Qué reloj o referencia temporal define `now`?
- ¿Se conserva HMAC o se adopta una librería JWT? ¿Con qué criterios se decidirá?
- ¿Qué entornos se consideran laboratorio?
- ¿Existe actualmente un secreto por defecto y dónde está permitido?
- ¿Qué valores, además del token y el secreto, se consideran sensibles?
- ¿Algún consumidor requiere cambios para enviar `client`?
