# Summary
- Update `POST /auth/login` tokens to include the required claims, validate and include the request's `client`, and report an expiry consistent with the token.
- Preserve the existing response fields: `token`, `expiresIn`, and `user`.

# Scope
- **In scope**
  - Require `sub`, `client`, `iat`, and `exp` claims.
  - Validate the request's `client` and include it in the token.
  - Ensure `expiresIn` is consistent with `exp - now` within a documented tolerance.
  - Address the default-secret handling outside the laboratory and prevent logging tokens or secrets.
  - Document the signing approach decision and add the specified authentication tests and evidence.
- **Out of scope**
  - No additional scope is specified by the HU.

# Assumptions
- None confirmed by the HU. Decisions not specified there are listed under Open questions.

# Technical decisions to make
- **Open:** retain HMAC or adopt a JWT library.
- **Open:** define the documented time tolerance for comparing `expiresIn` with `exp - now`.
- **Open:** determine how to remove the default secret outside the laboratory or document it as laboratory-only.

# Risks and mitigations
- Tokens or secrets could be exposed through logs. **Mitigation:** verify logs do not include tokens, secrets, or sensitive values.
- A default signing secret could be used outside the laboratory. **Mitigation:** remove the default there or explicitly document its laboratory-only use.
- Expiry reporting could disagree with the token's `exp`. **Mitigation:** define and document a tolerance, then test the relationship.

# Acceptance criteria
- Every valid token contains `sub`, `client`, `iat`, and `exp`.
- The request's `client` is validated and its value is reflected in the token.
- `expiresIn` matches `exp - now` within the documented tolerance.
- Tests cover valid login, invalid credentials, and expired tokens.
- Logs contain no tokens, secrets, or sensitive values.
- The HMAC-versus-JWT decision is documented.

# Validation plan
- **Unit tests:** verify required claims, `client` validation and propagation, expiry consistency within the documented tolerance, and applicable invalid cases.
- **Functional tests:** exercise valid login, invalid credentials, and expired-token behavior through the authentication flow.
- **Security (minimum checks):** inspect relevant logging to confirm tokens, secrets, and sensitive values are not recorded; verify default-secret handling is documented or removed outside the laboratory.
- **Required evidence:** this generated document, test results for claims, expiry, and authentication errors, documented secret configuration, and evidence of redacted logs.

# Suggested tasks by role
- **Chapter/PO**
  - Confirm the acceptance criteria and expected evidence.
  - Resolve whether the default secret is removed outside the laboratory or documented as laboratory-only.
- **Tech Lead**
  - Decide and document HMAC versus a JWT library.
  - Define and document the expiry-comparison tolerance.
  - Review the authentication-flow changes and evidence against the acceptance criteria.
- **Backend**
  - Implement the required token claims and request `client` validation/propagation.
  - Ensure `expiresIn` is consistent with `exp - now` within the agreed tolerance.
  - Add tests for claims, expiry, valid login, invalid credentials, and expired tokens; ensure sensitive values are not logged.
- **Frontend**
  - No frontend change is specified by the HU; confirm whether any client-side change is needed after the backend contract is reviewed.
- **QA**
  - Verify valid login returns a token with all required claims.
  - Verify invalid credentials and expired tokens are covered by tests.
  - Capture test results and inspect the relevant logs for sensitive values.
- **Cyber**
  - Review logging evidence for token, secret, and sensitive-value exposure.
  - Review the documented handling of the default secret outside the laboratory.

# Open questions
- Should signing continue to use HMAC, or should a JWT library be adopted?
- What documented tolerance applies to the `expiresIn` and `exp - now` comparison?
- What validation rules define an acceptable `client` value?
- Should the default secret be removed outside the laboratory, or documented as laboratory-only?
- Which configuration and deployment evidence will demonstrate the chosen secret handling without disclosing secret values?