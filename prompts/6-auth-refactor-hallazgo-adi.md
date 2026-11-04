## PLAN

AMBITO
Workspace: ai-labs, abierto en su raiz. Todas las rutas son relativas
a esa raiz. Lee el codigo solo de auth/; escribe codigo, configuracion y
pruebas solo en auth/. No leas ni modifiques codigo de otros componentes.
Lee README.md, RETO.md y docs/GOOD_PRACTICES.md. Lee
HALLAZGOS.md, PLAN.md y los demas documentos de docs/ solo si existen.
Escribe evidencias solo en HALLAZGOS.md, PLAN.md, prompts/,
docs/ENDPOINTS.md, docs/TESTING.md y docs/GOOD_PRACTICES.md de la raiz.
Cuando este paso pida un artefacto ausente, crealo. Si ya existe, conserva
las filas y secciones de otros componentes. No las sobrescribas.
Cada hallazgo lleva componente auth e ID H-AUTH-NN, por ejemplo H-AUTH-01.
Cita codigo como auth/ruta:linea. No renumeres IDs existentes.
Ejecuta Git desde ai-labs/; ejecuta pruebas con cwd auth/.
Coordina dependencias externas sin ampliar AMBITO. No inventes aprobaciones
humanas ni resultados de pruebas. Si falta evidencia, registra el pendiente.

CONTEXTO
Trabaja solo las filas y secciones de auth en los documentos generales.
Conserva contenido previo de otros componentes y el historial de este componente.
Si docs/ENDPOINTS.md existe, distingue contrato observado, contrato acordado y
riesgos pendientes. Si docs/TESTING.md existe, relaciona cada ejecucion con
componente e IDs.
HALLAZGOS.md tiene el backlog general; selecciona solo items del componente elegido (pueden
ser hallazgos de seguridad, bugs, deuda técnica o sugerencias de review). Antes
de proponer cualquier cambio, consulta docs/ENDPOINTS.md si existe. Si no existe,
deriva el contrato observado del código. Consulta docs/GOOD_PRACTICES.md para las
convenciones a las que debe converger el código.

OBJETIVO
Crear o actualizar PLAN.md con la propuesta de corrección para [indica aquí los items:
por ejemplo "H-AUTH-02 y H-AUTH-04, si existen en HALLAZGOS.md", o "los 3 de mayor severidad"].
Un apartado por item, cada uno con su propio alcance.

RESTRICCIONES
No modifiques código todavía. No combines el alcance de dos items en un mismo
apartado, aunque los resuelvas en la misma tanda. No cambies un contrato de
docs/ENDPOINTS.md salvo que el item lo pida explícitamente.

CRITERIOS DE ACEPTACIÓN
Por cada item incluido, PLAN.md dice: qué item de HALLAZGOS.md es, el cambio
propuesto, los archivos que se van a tocar, y qué prueba hay que escribir antes
de tocarlos.

FORMATO DE RESPUESTA
PLAN.md creado o actualizado con un apartado por item (## Item: <hallazgo>).
Conserva los apartados de otros componentes. No modifiques código todavía.

## EJECUCION DE PLAN
AMBITO
Workspace: ai-labs, abierto en su raiz. Todas las rutas son relativas
a esa raiz. Lee el codigo solo de auth/; escribe codigo, configuracion y
pruebas solo en auth/. No leas ni modifiques codigo de otros componentes.
Lee README.md, RETO.md, HALLAZGOS.md, PLAN.md y
docs/GOOD_PRACTICES.md. Consulta los demas documentos de docs/ si existen.
Escribe evidencias solo en HALLAZGOS.md, PLAN.md, prompts/,
docs/ENDPOINTS.md, docs/TESTING.md y docs/GOOD_PRACTICES.md de la raiz,
conservando las filas y secciones de otros componentes. No los sobrescribas.
Cada hallazgo lleva componente auth e ID H-AUTH-NN, por ejemplo H-AUTH-01.
Cita codigo como auth/ruta:linea. No renumeres IDs existentes.
Ejecuta Git desde ai-labs/; ejecuta pruebas con cwd auth/.
Coordina dependencias externas sin ampliar AMBITO. No inventes aprobaciones
humanas ni resultados de pruebas. Si falta evidencia, registra el pendiente.

CONTEXTO
Trabaja solo las filas y secciones de auth en los documentos generales.
Conserva contenido previo de otros componentes y el historial de este componente.
Si docs/ENDPOINTS.md existe, distingue contrato observado, contrato acordado y
riesgos pendientes. Si docs/TESTING.md existe, relaciona cada ejecucion con
componente e IDs; si no, registra la evidencia en PLAN.md.
Comprueba la aprobacion humana real registrada para estos IDs en PLAN.md.
Si falta, detente antes de editar codigo. Úsalo como guía única.

OBJETIVO
Implementar cada item de PLAN.md, uno a la vez y en el orden en que aparecen,
con su prueba antes y después. Después de cada uno, actualizar su fila en
HALLAZGOS.md antes de pasar al siguiente.

RESTRICCIONES
No implementes items que no estén en PLAN.md. No reduzcas quality gates. No
avances al siguiente item si el anterior no quedó con su prueba en verde.

CRITERIOS DE ACEPTACIÓN
Cada item de PLAN.md queda con su propio diff y su propia prueba. Cada fila
correspondiente en HALLAZGOS.md pasa a estado "Resuelto", con una nota corta
(1-2 líneas) de cómo se resolvió y qué test y diff local lo respaldan. Los contratos
existentes de docs/ENDPOINTS.md siguen funcionando, salvo que algún item pidiera
cambiarlos a propósito.

FORMATO DE RESPUESTA
Por cada item: el diff, la caracterizacion antes y despues del cambio y la prueba de correccion
con sus resultados reales, y la fila de HALLAZGOS.md actualizada con la nota de resolución.