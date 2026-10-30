# Summary
- La historia solicita exigir los claims `sub`, `client`, `iat` y `exp` en los tokens de `POST /auth/login`, obtener y validar `client` desde la solicitud, y comprobar la coherencia de `expiresIn` con `exp - now`.
- También requiere resolver el tratamiento del secreto por defecto, evitar registrar tokens o secretos y documentar la decisión entre HMAC y una librería JWT.

# Scope
- **Dentro del alcance**
  - Claims mínimos del token y origen de `client`.
  - Coherencia entre `expiresIn` y `exp - now`, con tolerancia documentada.
  - Tratamiento del secreto por defecto y prevención de logs sensibles.
  - Pruebas y documentación indicadas en la HU.
- **Fuera del alcance**
  - Requisitos o cambios funcionales no descritos en la HU.

# Assumptions
- Se toma como contexto de la HU que el servicio ya expone `POST /auth/login`, devuelve `token`, `expiresIn` y `user`, firma con HMAC y lee `AUTH_TOKEN_SECRET`.
- No se presupone qué formato, reglas de validación o valores admitidos debe tener `client`; queda por definir.

# Technical decisions to make
- Resolver si se conserva HMAC o se adopta una librería JWT; documentar la decisión.
- Resolver si se elimina el secreto por defecto fuera del laboratorio o se documenta como exclusivo del laboratorio.
- Definir y documentar la tolerancia para comparar `expiresIn` con `exp - now`.
- Definir las reglas de validación de `client`.

# Risks and mitigations
- Un secreto por defecto fuera del laboratorio podría permitir firmar tokens con una configuración no deseada. Mitigación indicada por la HU: eliminarlo fuera del laboratorio o documentarlo como exclusivo de laboratorio.
- Registrar tokens o secretos podría exponer valores sensibles. Mitigación: evitar esos registros y aportar evidencia de logs redactados.
- Sin una tolerancia acordada, la comprobación de expiración puede ser ambigua. Mitigación: documentar la tolerancia antes de validar ese criterio.

# Acceptance criteria
- Un token válido contiene `sub`, `client`, `iat` y `exp`.
- `client` se obtiene de la solicitud, se valida y se refleja en el token.
- `expiresIn` coincide con `exp - now` dentro de una tolerancia documentada.
- Hay pruebas para login válido, credenciales inválidas y token expirado.
- No se registran tokens, secretos ni valores sensibles.
- Queda documentada la decisión sobre HMAC frente a una librería JWT y el tratamiento del secreto por defecto.

# Validation plan
- **Pruebas unitarias**
  - Comprobar los cuatro claims requeridos y el valor de `client`.
  - Comprobar la coherencia de `expiresIn` con `exp - now` según la tolerancia documentada.
  - Cubrir los casos de autenticación y expiración enumerados en los criterios.
- **Pruebas funcionales**
  - Verificar en `POST /auth/login` la respuesta para login válido y credenciales inválidas.
- **Seguridad (comprobaciones mínimas)**
  - Revisar que los logs no contengan tokens, secretos ni otros valores sensibles.
  - Comprobar que la configuración del secreto cumple la decisión documentada.
- **Evidencia requerida**
  - Documento de decisión y configuración de `AUTH_TOKEN_SECRET`.
  - Salida de las pruebas de claims, expiración y errores de autenticación.
  - Evidencia de logs redactados.

# Suggested tasks by role
- **Chapter/PO**
  - Confirmar el alcance de la historia y sus criterios de aceptación.
  - Resolver las preguntas abiertas sobre `client` y tolerancia.
- **Tech Lead**
  - Evaluar HMAC frente a una librería JWT y documentar la decisión.
  - Resolver y documentar la política del secreto por defecto.
  - Acordar y documentar la tolerancia de expiración.
- **Backend**
  - Implementar los claims requeridos y obtener y validar `client` desde la solicitud.
  - Asegurar la coherencia de `expiresIn` con `exp - now` según la tolerancia acordada.
  - Añadir las pruebas de login, claims y expiración, y evitar logs sensibles.
- **Frontend**
  - No se identifica trabajo de frontend en la HU; no aplica salvo que la validación de `client` requiera cambios en la solicitud enviada.
- **QA**
  - Verificar login válido, credenciales inválidas y token expirado.
  - Verificar claims y coherencia de expiración conforme a la tolerancia documentada.
  - Adjuntar resultados de pruebas y evidencia de revisión de logs.
- **Cyber**
  - Revisar que no se registren tokens, secretos ni valores sensibles.
  - Revisar que el tratamiento del secreto por defecto quede documentado.

# Open questions
- ¿Qué valores y reglas de validación se aceptan para `client`?
- ¿Qué tolerancia se documentará para comparar `expiresIn` con `exp - now`?
- ¿Se conserva HMAC o se adopta una librería JWT?
- ¿Se elimina el secreto por defecto fuera del laboratorio o se documenta como exclusivo de laboratorio?
