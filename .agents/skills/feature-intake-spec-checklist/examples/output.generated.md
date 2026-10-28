# Feature Intake -> Spec Checklist

## 1. Summary

- Add a confirmation step before a transfer is sent.
- Show a concise transfer summary and validate the transfer data.
- Target outcome: reduce duplicate-transfer complaints by 20% within one month.
- Use the existing REST API, web frontend, and standardized logging.

## 2. Scope

### Within scope

- Pre-send transfer confirmation.
- Transfer summary message.
- Transfer-data validation.
- Unit tests, brief documentation, and PR review.

### Out of scope

- Infrastructure changes.
- A complete UI redesign.
- Unrelated changes to the accounts or transfers services.

## 3. Assumptions

- The confirmation is presented by the web frontend before it calls the transfer operation.
- The existing REST API remains responsible for executing the transfer.
- The accounts and transfers services are available in the validation environment.
- Validation rules and summary fields will be agreed before implementation.

## 4. Technical decisions to make (pending or resolved)

- **Pending:** Define the fields displayed in the confirmation summary.
- **Pending:** Define amount-validation rules and boundary values.
- **Pending:** Assign validation responsibilities between frontend and backend.
- **Pending:** Identify the API version that owns the transfer contract.
- **Pending:** Define the allowlist and masking rules for confirmation logs.
- **Resolved:** Reuse the existing REST API, web frontend, and standardized logging.

## 5. Risks and mitigations

- **Sensitive data could be exposed in logs.**
  - Mitigation: log only allowlisted fields, mask sensitive values, and review sanitized log evidence.
- **Incorrect amount validation could accept or reject transfers improperly.**
  - Mitigation: agree on validation rules, keep one contractual source, and test valid, invalid, and boundary values.
- **Frontend and backend validation could diverge.**
  - Mitigation: document shared rules and exercise the same validation cases in both layers.
- **A confirmation screen alone may not prevent every duplicate transfer.**
  - Mitigation: measure the outcome and decide separately whether idempotency is required.

## 6. Acceptance criteria (measurable)

- Every valid transfer attempt displays confirmation before the transfer request is sent.
- Cancelling confirmation results in zero requests to the transfers service.
- Every agreed invalid-data case blocks submission and displays a validation message.
- The confirmation summary contains every agreed field with the values sent in the request.
- Unit and functional test suites finish with zero failures.
- Confirmation-flow logs contain none of the values classified as sensitive.
- Duplicate-transfer complaints decrease by at least 20% during the first month against the agreed baseline.
- Brief documentation is updated and the PR has a recorded review.

## 7. Validation plan

### Unit tests

- Test valid, invalid, and boundary transfer amounts.
- Test confirmation-summary construction.
- Test confirm, cancel, and edit state transitions.
- Verify that confirmation enables only one transfer submission.

### Functional tests

- Confirm and send a valid transfer.
- Cancel a valid transfer without calling the transfers service.
- Block an invalid transfer before submission.
- Verify handling of errors from dependent services.
- Compare the confirmed summary with the request sent.

### Security (minimum checks)

- Verify that logs and error messages expose no sensitive values.
- Verify that the backend validates received transfer data.
- Verify that edited data requires validation and confirmation again.

### Required evidence (attachments)

- Unit and functional test output.
- Completed validation and security checklist.
- Sanitized evidence of the request sent after confirmation.
- Sanitized sample of confirmation-flow logs.
- Updated brief documentation and recorded PR review.
- Complaint-metric report including baseline and measurement period.

## 8. Suggested tasks by role

### Chapter/PO

- Define the complaint baseline and measurement period.
- Approve summary fields and user-facing messages.
- Validate scope and acceptance criteria.

### Tech Lead

- Define the frontend-to-API confirmation flow.
- Assign validation responsibilities to each layer.
- Review accounts and transfers service contracts.
- Approve the secure logging approach.

### Backend

- Implement or confirm the agreed server-side validations.
- Preserve the existing REST transfer contract.
- Add unit tests for valid, invalid, and boundary data.
- Ensure logs exclude sensitive values.

### Frontend

- Implement the summary and confirmation step.
- Prevent submission when validation fails.
- Ensure cancellation makes no transfer request.
- Add tests for confirm, cancel, and edit flows.

### QA

- Prepare happy-path, boundary, and error cases.
- Verify the sent request against the confirmed summary.
- Verify that cancel and invalid-data flows make no transfer request.
- Attach test results and the regression checklist.

### Cyber

- Define which transfer fields are sensitive.
- Review logging, masking, and error messages.
- Verify that server-side validation does not trust the client alone.

## 9. Open questions

- What baseline will be used to measure the 20% reduction?
- Which fields must appear in the confirmation summary?
- What amount rules and boundary values apply?
- Which API version owns the transfer contract?
- Which values are classified as sensitive?
- Is idempotency required in addition to visual confirmation?
- Which environments and fictitious data are available for validation?
