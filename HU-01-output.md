# Feature Intake -> Spec Checklist

## 1. Summary
Add a pre-send transfer confirmation with a summary and data validation to help users avoid duplicate transfers. Target impact: reduce complaints caused by duplicate transfers by 20% in one month.

## 2. Scope
- **Dentro del alcance**
  - Confirm the transfer before sending.
  - Display a transfer summary.
  - Validate transfer data.
- **Fuera del alcance**
  - Infrastructure changes.
  - Complete UI redesign.

## 3. Supuestos
- The feature will use the existing REST API, web frontend, and standardized logging.
- The accounts service and transfers service are dependencies.

## 4. Decisiones técnicas por tomar (pendientes o resueltas)
- **Resueltas:** use the existing REST API and web frontend; follow standardized logging.
- **Pendientes:** determine the summary fields and confirm the existing amount-validation rules.

## 5. Riesgos y mitigaciones
- **Sensitive data exposure in logs:** review confirmation-related logs and ensure they do not contain sensitive data.
- **Amount-validation errors:** confirm the existing validation rules and test valid and invalid amounts.
- **Dependency on accounts and transfers services:** verify their contracts and integration requirements before implementation.

## 6. Criterios de aceptación (medibles)
- Every transfer is presented for confirmation with a summary before it is sent.
- Canceling the confirmation results in no transfer being sent.
- Data that fails the agreed validation rules is blocked from being sent and produces a validation message.
- Duplicate-transfer complaints decrease by 20% within one month, measured against an agreed baseline.
- Confirmation-related logs contain no sensitive values.

## 7. Plan de validación
- **Pruebas unitarias**
  - Test valid and invalid amounts against the existing validation rules.
  - Verify confirmation allows submission and cancellation prevents it.
- **Pruebas funcionales**
  - Test the summary, confirmation, and submission flow with the accounts and transfers services.
  - Measure duplicate-transfer complaints against the agreed baseline after one month.
- **Seguridad (comprobaciones mínimas)**
  - Review confirmation-related logs for sensitive data.
  - Verify invalid transfer data cannot be submitted.
- **Evidencia requerida (qué adjuntar)**
  - Unit and functional test results.
  - Completed logging review checklist.
  - Versioned `SKILL.md`, `examples/input.sample.md`, and `examples/output.sample.md`, plus the Skill version reference `skill-feature-intake-v0.1.0`.
  - Brief documentation and reviewed PR, per the team DoD.

## 8. Tareas sugeridas por rol
- **Chapter/PO**
  - Agree on the baseline and measurement method for duplicate-transfer complaints.
  - Confirm which transfer details the summary should display.
- **Tech Lead**
  - Review the existing transfer flow and REST API contracts.
  - Confirm integration requirements for the accounts and transfers services.
  - Define how confirmation and validation fit into the existing flow.
- **Backend**
  - Review the existing amount-validation rules.
  - Implement or adjust pre-submission validation according to the agreed design.
  - Verify that standardized logs do not expose sensitive values.
- **Frontend**
  - Implement the transfer summary and confirmation/cancellation actions.
  - Connect confirmation and cancellation to the existing submission flow.
- **QA**
  - Create test cases for confirmation, cancellation, and data validation.
  - Verify that cancellation and validation failures prevent submission.
  - Attach test results and document the agreed metric measurement.
- **Cyber**
  - Review confirmation-related logs for sensitive data.
  - Verify that invalid data is blocked before submission.
  - Review the feature’s security risks within the stated scope.

## 9. Preguntas abiertas
- What baseline and data source will measure the 20% reduction in complaints?
- Which transfer fields should the summary display?
- What are the existing amount-validation rules?