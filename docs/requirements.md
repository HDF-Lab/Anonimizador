
# System Requirements

## 1. Introduction

### 1.1 Purpose

Anonimizador Offline is an Android application designed to identify and anonymize sensitive personal information in text, PDF documents, and exported WhatsApp conversations.

The application allows users to prepare content before sharing it with third parties, reducing the exposure of personal information.

### 1.2 Scope

The application provides:

- Text input and editing.
- Importing supported documents.
- PII detection.
- Configurable anonymization.
- Reversible and irreversible masking modes.
- WhatsApp export processing.
- Local content processing.
- Copying and exporting anonymized content.

### 1.3 Target Platform

- Android.
- Kotlin.
- Jetpack Compose.
- Offline-first operation.

---

## 2. Terminology

| Term | Description |
|---|---|
| PII | Personally Identifiable Information. |
| Anonymization | Transformation of personal information to reduce or prevent identification. |
| Masking | Replacement of detected information with a placeholder or another representation. |
| Reversible masking | Masking that allows original values to be restored during the supported session lifecycle. |
| Irreversible masking | Masking that does not retain a mapping for restoring original values. |
| Detector | Component responsible for identifying a specific category of sensitive information. |
| Match | A detected text interval associated with a PII type. |
| Span | A range of character positions in the input text. |
| Noise | Automatically generated content that may be removed during WhatsApp processing. |

---

# 3. Functional Requirements

## FR-001 — Text Input

The application shall allow users to enter or paste text into the editor.

### Acceptance criteria

- The user can enter text manually.
- The user can paste text.
- The application updates the anonymized result when the input changes.
- Empty input must not produce invalid anonymization results.
- Input changes must not allow outdated processing results to overwrite newer results.

## FR-002 — Text Editing

The application shall allow users to modify previously entered or imported content.

### Acceptance criteria

- The user can edit the input text.
- Changes must trigger processing according to the application's processing policy.
- The result must correspond to the latest input.
- Clearing the input must clear the corresponding output and session state.

## FR-003 — PII Detection

The application shall identify supported categories of personal information.

Supported categories may include:

- CPF.
- CNPJ.
- PIX UUID/random key.
- Email addresses.
- Brazilian telephone numbers.
- IPv4 addresses.
- Credit card numbers.
- Supported Brazilian vehicle license plates.

### Acceptance criteria

- Each detector must identify only its configured PII categories.
- Detection must respect enabled and disabled categories.
- Detected positions must correspond to the original text supplied to the detector.
- Invalid matches must not be applied to the output.
- Overlapping matches must be resolved before masking.
- Detection must not modify the original input.

The application does not guarantee detection of every possible occurrence of personal information.

## FR-004 — PII Configuration

The application shall allow users to configure which PII categories are anonymized.

### Acceptance criteria

- The user can enable individual categories.
- The user can disable individual categories.
- The user can enable or disable all available categories, if this control is provided.
- Configuration changes must affect subsequent processing.
- If content is already present, relevant configuration changes must reprocess that content.
- The displayed output must reflect the latest configuration.

## FR-005 — Masking Modes

The application shall provide the supported masking modes.

### Acceptance criteria

- The user can select a masking mode.
- The selected mode must be applied consistently to detected matches.
- Reversible masking must maintain the required mapping during its supported lifecycle.
- Irreversible masking must not retain a mapping intended for restoration.
- Switching modes must reprocess existing content when applicable.

## FR-006 — Match Consolidation

The application shall consolidate detected matches before applying replacements.

### Acceptance criteria

- Invalid intervals must be rejected.
- Overlapping intervals must not be applied simultaneously.
- Adjacent intervals must be preserved.
- The longest valid interval must have priority under the current consolidation policy.
- Tie-breaking must be deterministic.
- Final matches must be ordered by their position in the text.

## FR-007 — WhatsApp Export Processing

The application shall support processing exported WhatsApp conversations in supported formats.

### Acceptance criteria

- The application can process supported TXT exports.
- The application can process ZIP archives containing TXT exports.
- Participant identification must follow the supported export formats.
- Participant names must be anonymized when the corresponding functionality is enabled.
- Message content must be preserved unless a configured processing rule explicitly changes it.
- Known automatic WhatsApp messages may be treated according to the noise-cleanup configuration.
- Unsupported or malformed exports must produce an appropriate error.

## FR-008 — WhatsApp Noise Cleanup

The application shall provide configurable cleanup of supported WhatsApp-generated content.

Examples include:

- Automatic system notifications.
- Encryption notices.
- Media placeholders.
- Deleted-message notifications.
- Other explicitly supported system-generated entries.

### Acceptance criteria

- Cleanup must follow documented parser rules.
- User messages must not be removed merely because they resemble system messages.
- Disabling noise cleanup must preserve supported content that would otherwise be removed.
- Changes to the cleanup setting must affect existing content when reprocessing is supported.

## FR-009 — TXT Import

The application shall allow users to import supported text files.

### Acceptance criteria

- The application must use Android document access mechanisms.
- File access must not depend on hardcoded filesystem paths.
- Supported encoding must be documented.
- File size limits must be enforced.
- Invalid or unsupported files must produce an appropriate error.

## FR-010 — PDF Import

The application shall allow users to extract text from supported PDF documents.

### Acceptance criteria

- The application must extract available text from supported PDFs.
- The application must enforce configured file and extracted-text limits.
- Invalid or encrypted PDFs that cannot be processed must produce an appropriate error.
- Scanned documents without an available text layer are not required to be processed through OCR.
- PDF extraction must not block the main thread.

## FR-011 — ZIP Import

The application shall allow users to import supported ZIP archives containing text exports.

### Acceptance criteria

- The application must inspect archive entries safely.
- The application must enforce configured entry-count and extraction limits.
- Unsupported entries must not be interpreted as text documents.
- The behavior when multiple TXT files exist must be documented.
- Malformed archives must produce an appropriate error.

## FR-012 — Copy Anonymized Content

The application shall allow users to copy the anonymized result.

### Acceptance criteria

- Copying must use the anonymized output, not the original input.
- Empty results must not be copied as valid content.
- Configured clipboard size restrictions must be respected.
- The user must receive appropriate feedback.

## FR-013 — Export Anonymized Content

The application shall allow users to save the anonymized result as a supported text file.

### Acceptance criteria

- Exported content must contain the anonymized result.
- Original sensitive values must not be included unless they are intentionally preserved by the selected mode.
- The application must handle file creation and access errors.
- The user must receive appropriate feedback.

## FR-014 — Session Management

The application shall allow users to clear the current anonymization session.

### Acceptance criteria

- The input text must be cleared.
- The anonymized output must be cleared.
- Temporary reversible mappings must be cleared.
- Pending processing must be cancelled or invalidated.
- Results from obsolete operations must not reappear after clearing.

## FR-015 — Processing Feedback

The application shall provide feedback while content is being processed.

### Acceptance criteria

- The user can identify when processing is in progress.
- Processing errors must be communicated.
- The UI must remain responsive.
- Obsolete operations must not publish outdated results.
- Configuration controls must remain usable during processing unless a specific operation requires otherwise.

---

# 4. Non-Functional Requirements

## NFR-001 — Offline Operation

The application shall perform its core anonymization functionality locally.

### Acceptance criteria

- Core processing must not require internet access.
- User content must not be uploaded to external services.
- No remote API may be required for detection, parsing, or masking.

## NFR-002 — Privacy

The application shall minimize exposure and retention of sensitive information.

### Acceptance criteria

- Original content must not be logged.
- Sensitive content must not be sent to analytics services.
- Temporary mappings must follow the session lifecycle.
- Content must not be persisted unnecessarily.
- Clipboard and export operations must use the anonymized output.

## NFR-003 — Security

The application shall implement appropriate safeguards for sensitive content.

### Acceptance criteria

- Sensitive screens must use the configured secure-screen behavior.
- File access must follow Android permission and document-provider mechanisms.
- Input limits must be enforced.
- Dependencies must be reviewed for unnecessary permissions and network behavior.
- Error messages must not expose sensitive values.

## NFR-004 — Performance

The application shall process supported inputs without blocking the user interface.

### Acceptance criteria

- Long-running operations must execute outside the main thread.
- Processing must support cancellation.
- Obsolete jobs must not overwrite current results.
- File and text limits must be respected.
- Performance must be evaluated using representative input sizes.

## NFR-005 — Reliability

The application shall produce predictable results for supported inputs.

### Acceptance criteria

- The same input and configuration should produce deterministic results when the underlying rules are unchanged.
- Invalid matches must not corrupt the output.
- Processing errors must not be reported as successful anonymization.
- Partial or outdated results must not replace the latest valid state.

## NFR-006 — Maintainability

The application shall preserve separation of responsibilities.

### Acceptance criteria

- Business rules must remain independent of the UI.
- Components must have clear responsibilities.
- Interfaces must be used where they provide meaningful decoupling.
- Business rules must be covered by automated tests.
- Documentation must reflect relevant behavioral changes.

## NFR-007 — Accessibility

The application shall provide an accessible user interface.

### Acceptance criteria

- Interactive controls must expose appropriate accessibility semantics.
- Controls must have meaningful labels.
- Touch targets must be usable.
- Processing states and errors must be communicated.
- UI behavior must remain understandable when accessibility services are enabled.

## NFR-008 — Compatibility

The application shall operate within its declared Android compatibility range.

### Acceptance criteria

- Supported Android versions must be documented.
- File access must follow platform APIs.
- Dependencies must be compatible with the configured Android SDK.
- Platform-specific behavior must be tested on supported versions.

---

# 5. Business Rules

## BR-001 — Original Input Preservation

The original input must remain separate from the anonymized output.

## BR-002 — Enabled Categories

Only enabled PII categories may be anonymized.

## BR-003 — Match Validity

A match must have valid boundaries and correspond to the exact text being processed.

## BR-004 — Overlap Resolution

Overlapping matches must be consolidated before masking.

The current policy prioritizes the longest valid interval, followed by deterministic tie-breaking rules.

## BR-005 — Index Integrity

All match indexes must refer to the exact text version from which they were generated.

If text preparation changes the content, indexes must be calculated against the prepared text.

## BR-006 — Reversible Mapping

Reversible masking must use a consistent mapping between original values and replacement identifiers during the supported session lifecycle.

## BR-007 — Irreversible Mapping

Irreversible masking must not preserve a restoration mapping.

## BR-008 — WhatsApp Noise

Only explicitly supported noise patterns may be removed.

## BR-009 — Processing Consistency

The displayed result must correspond to the latest input and active configuration.

## BR-010 — Local Processing

Sensitive content must remain within local application processing.

---

# 6. Constraints and Limitations

The application has the following constraints:

- PII detection is based on implemented detection rules and does not guarantee complete identification.
- Regex-based detection may produce false positives and false negatives.
- Scanned PDFs require OCR, which is outside the current extraction scope unless explicitly implemented.
- ZIP processing is limited by configured archive and extraction limits.
- PDF libraries may consume additional memory beyond extracted-text limits.
- WhatsApp parsing depends on supported export formats and patterns.
- Reversible masking depends on the lifetime and availability of the in-memory mapping.
- Offline processing does not automatically guarantee complete device-level security.

Actual limits and supported categories must be verified against the current implementation.

---

# 7. Out of Scope

Unless explicitly added to the product scope, the following are not required:

- Cloud-based anonymization.
- Server-side processing.
- Automatic upload of user documents.
- OCR for scanned PDFs.
- Guaranteed detection of every possible PII format.
- Automatic anonymization of arbitrary document formats.
- Synchronization of sessions across devices.
- Permanent storage of reversible mappings.

---

# 8. Requirement Traceability

Each requirement should be traceable to its implementation and tests.

Suggested relationship:

| Requirement | Implementation area | Test area |
|---|---|---|
| FR-003 | `domain.detector`, `data.detector` | Detector tests |
| FR-004 | `presentation`, ViewModel | ViewModel tests |
| FR-005 | `domain.masking` | Masker tests |
| FR-006 | `domain.masking` | Span consolidator tests |
| FR-007 | `domain.parser` | WhatsApp parser tests |
| FR-009 | `data.ingestion` | TXT extraction tests |
| FR-010 | `data.ingestion` | PDF extraction tests |
| FR-011 | `data.ingestion` | ZIP extraction tests |
| FR-014 | `presentation`, ViewModel | Session lifecycle tests |
| NFR-001 | Manifest and application architecture | Permission and integration checks |
| NFR-002 | Application-wide | Privacy and security review |
| NFR-004 | Use cases and data layer | Performance tests |

Update this table when requirements or implementation responsibilities change.

---

# 9. Requirement Change Policy

Changes to requirements must be evaluated for their impact on:

- Business rules.
- Architecture.
- Existing interfaces.
- User experience.
- Privacy and security.
- Automated tests.
- Documentation.

A requirement must not be considered implemented solely because a corresponding class or method exists.

Implementation must satisfy the associated acceptance criteria.

When a requirement is not yet implemented, document its status explicitly rather than presenting it as an existing capability.
