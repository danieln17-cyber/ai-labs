Contratos preservados: CUMPLIDO - mvn verify pasó con 22 pruebas; el cambio no altera POST /auth/login. Las pruebas siguen verificando debug y login sin client (AuthResourceTest).
Política aprobada de cuentas iniciales: BLOQUEADO - la decisión fue “solo desarrollo”, pero application.properties también habilita la siembra en %test. Falta confirmar si test está autorizado como excepción o ajustar la configuración.
CI en verde: PENDIENTE DE OTRO GATE - hay evidencia de mvn verify local exitoso, pero no de una ejecución de CI.
Cobertura crítica ≥85 %: CUMPLIDO - JaCoCo reporta LegacyDatabase: 83/91 líneas cubiertas, 91,2 % (jacoco.csv).
Dependencias controladas: NO APLICA AL ALCANCE - no se modificaron dependencias ni pom.xml.
Secure coding, secretos fuera del código: FALTA EVIDENCIA - las credenciales de las cuentas de muestra siguen definidas en LegacyDatabase; falta confirmar que son exclusivamente ficticias y seguras para los perfiles donde se habilitan.
Secure coding, SQL parametrizado: NO APLICA AL ALCANCE - el cambio no modifica consultas SQL.
PR auditable: PENDIENTE DE OTRO GATE - no hay PR abierto ni revisión; el estado de Git aún contiene cambios ajenos al cambio de auth.
Guardrails, ejecución de laboratorio y sin pruebas externas: CUMPLIDO - se ejecutó mvn verify localmente desde auth; no hay evidencia de pruebas contra sistemas externos.
Guardrails, gates no desactivados: CUMPLIDO - mvn verify pasó; no se modificó pom.xml ni se desactivaron pruebas o gates.
Guardrails, revisión humana: PENDIENTE DE OTRO GATE - falta revisar el PR y resolver la discrepancia entre la política “solo desarrollo” y la configuración %test.