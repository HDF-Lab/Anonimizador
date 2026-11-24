
# UC-001 — Anonymize Content

## 1. Overview

| Attribute | Description |
|---|---|
| Use Case ID | UC-001 |
| Name | Anonymize Content |
| Primary Actor | User |
| Goal | Replace detected personal information in text |
| Priority | High |
| Related Component | `AnonymizeContentUseCase` |

## 2. Preconditions

- The user has provided text directly or obtained text from document extraction.
- Masking configuration is available.

## 3. Trigger

The user enters or changes text, or changes an enabled PII type, masking mode, or WhatsApp cleanup setting.

## 4. Main Success Scenario

1. The application receives the original text and current configuration.
2. If the text is blank, the application returns an empty result.
3. If the text matches the supported WhatsApp format, the application parses it when cleanup is enabled or participant anonymization is enabled.
4. When cleanup is enabled, the application removes supported system noise and prepares normalized text. When cleanup is disabled, it preserves the original text.
5. If participant anonymization is enabled, the parser identifies participant names in message headers and marks every occurrence of those names in the prepared text, including message bodies.
6. The application detects information from the PII types enabled in the request.
7. Detected matches and participant matches are combined.
8. The application validates and consolidates overlaps using the longest valid interval policy.
9. The application applies the selected masking mode.
10. The application returns the anonymized text, entity counts, optional reversal vault, and removed WhatsApp system line count.

## 5. Alternative Flows

### A1 — No PII Detected

The application returns the prepared text unchanged and empty entity counts.

### A2 — No PII Types Enabled

The application performs no detection and leaves the text unchanged.

### A3 — WhatsApp Cleanup Disabled

The application preserves the original text without removing WhatsApp system noise. If participant anonymization is enabled and the text matches the supported WhatsApp format, the parser still identifies participant names and marks their occurrences in message headers and bodies for masking.

### A4 — Reversible Masking

The application replaces matches with consistent identifiers and includes a tag-to-original-value vault in `AnonymizationOutput`. The vault is not currently stored or restored by the presentation layer.

### A5 — Irreversible Masking

The application replaces matches with identifiers and returns no reversal vault.

### A6 — Invalid Detection Match

The span consolidator ignores matches that do not represent a valid interval of the exact prepared text.

### A7 — Configuration Changed During Processing

The ViewModel cancels the previous processing job and presents only the result from the latest input and configuration.

## 6. Exception Flows

### E1 — Processing Failure

The ViewModel stops the affected operation and presents a processing error without displaying raw personal information.

### E2 — Processing Cancelled

The use case propagates cancellation and does not publish a cancelled result.

## 7. Business Rules

| ID | Rule |
|---|---|
| BR-001 | The original text remains unchanged. |
| BR-002 | Only enabled PII types are sent to the corresponding detectors. |
| BR-003 | Match indexes refer to the exact prepared text. |
| BR-004 | Invalid matches are discarded by the consolidator. |
| BR-005 | Overlaps are resolved before masking. |
| BR-006 | The longest valid interval has priority; adjacent intervals remain valid. |
| BR-007 | Reversible masking generates a reversal vault in the output. |
| BR-008 | Irreversible masking produces an empty reversal vault. |
| BR-009 | Processing is local and supports coroutine cancellation. |
| BR-010 | The latest ViewModel operation prevents superseded work from publishing a result. |
| BR-011 | When participant anonymization is enabled for a supported WhatsApp export, every occurrence of identified participant names in the prepared text, including message bodies, is eligible for masking regardless of the noise-cleanup setting. |
| BR-012 | Disabling WhatsApp noise cleanup preserves the original text and prevents system-noise removal; it does not disable participant identification or anonymization. |

## 8. Postconditions

### Success

- The anonymized text is available to the presentation layer.
- Entity counts reflect consolidated matches.
- The original text remains unchanged.
- WhatsApp system line removal is reported when applicable.

### Failure or Cancellation

- No cancelled operation is reported as successful.
- Processing errors are presented without raw personal information.

## 9. Acceptance Criteria

- [ ] Accepts text input.
- [ ] Processes only enabled PII types.
- [ ] Handles empty text.
- [ ] Handles content without detected PII.
- [ ] Parses supported WhatsApp text when cleanup is enabled.
- [ ] Identifies and masks participant-name occurrences in message bodies when participant anonymization is enabled.
- [ ] Preserves original WhatsApp text and still masks participant names when noise cleanup is disabled.
- [ ] Rejects invalid detection intervals.
- [ ] Resolves overlaps deterministically.
- [ ] Supports irreversible masking.
- [ ] Generates a reversible masking map in the output.
- [ ] Supports cancellation.
- [ ] Prevents superseded processing from overwriting newer state.
- [ ] Returns output based on the active request configuration.

## 10. Related Components

- `AnonymizeContentUseCase`
- `WhatsAppParser`
- `PiiDetector`
- `SpanConsolidator`
- `TextMasker`
- `SafeTextMasker`
- `PiiMatch`
- `AnonymizationOutput`
