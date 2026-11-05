# Política de cuentas iniciales de auth

## Fuente
H-AUTH-08, aprobado para especificación: «Credenciales predefinidas para cuentas iniciales». `LegacyDatabase.java:25` contiene credenciales predefinidas; `initialize` inserta las cuentas iniciales sin condición de perfil (`LegacyDatabase.java:42,49`). El impacto depende de que este arranque se use fuera de un entorno aislado.

## Contexto actual
El servicio inicializa las cuentas al arrancar. No está confirmado qué entornos reales usan este mecanismo.

## Problema confirmado
El código mantiene cuentas iniciales con credenciales predefinidas. Su exposición fuera de un entorno aislado podría permitir autenticación no autorizada.

## Comportamiento a preservar
- Mantener `POST /auth/login`, sus estados 400/401/200 y el esquema de respuesta acordado mientras no se apruebe un cambio explícito de contrato.
- No exponer valores de credenciales en la spec ni en la evidencia.

## Alcance
- Definir en qué entornos se permite aprovisionar cuentas iniciales.
- Especificar y probar el comportamiento de inicialización según la política aprobada.

## Fuera de alcance
- Cambiar la ruta o el formato de `POST /auth/login`.
- Rediseñar la persistencia de usuarios.

## Criterios de aceptación
- La política de aprovisionamiento por entorno queda documentada y aprobada.
- Pruebas verifican el comportamiento de inicialización para cada entorno definido.
- Se verifica que el flujo conserva el contrato protegido de login.

## Contratos afectados/protegidos
- `POST /auth/login`: ruta, estados y esquema JSON protegidos. Un cambio requiere aprobación explícita.

## Riesgos, supuestos y preguntas abiertas
- Riesgo: si las cuentas iniciales están habilitadas fuera del entorno aislado previsto, podrían permitir accesos no autorizados.
- Supuesto: ninguno sobre los entornos desplegados.
- Pregunta abierta: ¿en qué entornos deben existir cuentas iniciales?

# Semántica del campo `client`

## Fuente
H-AUTH-09, aprobado para especificación: «El campo `client` de la solicitud no se usa en la autenticación». El campo está declarado en `LoginRequest.java:6`, pero `AuthResource.java:33` pasa solo username y password; el token se construye sin `client` (`TokenService.java:30`). El impacto depende del contrato esperado para ese campo.

## Contexto actual
La solicitud admite `client`, pero su valor no influye en el flujo de autenticación ni en el token.

## Problema confirmado
El comportamiento del campo `client` no está definido por el contrato observado; actualmente se ignora.

## Comportamiento a preservar
- Mantener `POST /auth/login` y su esquema de respuesta protegido.
- No agregar claims ni reglas de validación sin una decisión aprobada.

## Alcance
- Definir si `client` es obligatorio, qué valores acepta y qué efecto tiene en la autenticación o el token.
- Probar el comportamiento que se acuerde.

## Fuera de alcance
- Cambiar otros campos o estados de la respuesta de login.
- Prescribir claims o una estrategia técnica antes de decidir el contrato.

## Criterios de aceptación
- La semántica de `client` queda documentada y aprobada.
- Pruebas cubren los casos válidos e inválidos definidos por esa decisión.
- El endpoint conserva los contratos protegidos que no se hayan aprobado cambiar.

## Contratos afectados/protegidos
- Cuerpo de solicitud y posibles claims del token de `POST /auth/login`.
- Ruta, estados y esquema de respuesta de login permanecen protegidos salvo aprobación explícita.

## Riesgos, supuestos y preguntas abiertas
- Riesgo: si el contrato requiere distinguir o limitar tokens por cliente, ignorar el valor podría incumplirlo.
- Supuesto: ninguno sobre la intención original del campo.
- Pregunta abierta: ¿qué semántica y reglas de validación deben tener los valores de `client`?

# Campo `debug` en la respuesta de login

## Fuente
H-AUTH-10, aprobado para especificación: «La respuesta exitosa expone un marcador interno `debug`». `AuthService.java:11,47` define e incluye el marcador; `LoginResponse.java:3` lo declara. El impacto potencial es exponer un detalle interno y convertirlo en parte observable del contrato.

## Contexto actual
La respuesta exitosa incluye el campo `debug`. Su presencia está cubierta por una prueba de integración (`AuthResourceTest.java:54`).

## Problema confirmado
Un marcador interno se entrega al cliente como parte de la respuesta exitosa. No está confirmado si el campo es intencional ni si algún consumidor depende de él.

## Comportamiento a preservar
- Mantener `POST /auth/login`, sus estados y el esquema de respuesta aprobado.
- No retirar ni cambiar `debug` sin revisar y aprobar el cambio contractual.

## Alcance
- Confirmar si `debug` pertenece al contrato público.
- Registrar la decisión y alinear las pruebas de API con el contrato aprobado.

## Fuera de alcance
- Cambiar ruta, autenticación, token o campos no relacionados.
- Asumir que el campo debe eliminarse antes de revisar consumidores y aprobar el cambio.

## Criterios de aceptación
- La presencia o ausencia de `debug` queda definida en el contrato aprobado.
- Las pruebas de API verifican la respuesta conforme a ese contrato.
- Cualquier cambio respecto del contrato actual cuenta con aprobación explícita.

## Contratos afectados/protegidos
- Esquema JSON de éxito de `POST /auth/login`, incluido actualmente `debug`.
- Ruta y estados de login protegidos; no se modifican implícitamente.

## Riesgos, supuestos y preguntas abiertas
- Riesgo: el campo puede revelar información interna o generar dependencia de clientes.
- Supuesto: ninguno sobre el uso de `debug` por consumidores.
- Pregunta abierta: ¿se conserva `debug` como campo público o se aprueba su retirada con la correspondiente revisión de consumidores?