
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
3. The application selects the single registered chat export parser that recognizes the text. If none recognizes it, the text is treated as ordinary content; if multiple parsers recognize it, processing fails explicitly.
4. If the selected parser's platform has noise cleanup enabled or its participant PII type is enabled, the application parses the content.
5. When cleanup is enabled for the selected platform, the application removes supported noise and prepares normalized text. When cleanup is disabled, it preserves the original text.
6. If the selected parser provides a participant PII type and that type is enabled, the parser identifies participant names and marks every occurrence in the prepared text.
7. The application detects information from the PII types enabled in the request. This includes supported URLs, context-labeled names and CNH numbers, and addresses with a recognized street type when their corresponding types are enabled.
8. Detected matches and participant matches are combined.
9. The application validates and consolidates overlaps using the longest valid interval policy.
10. The application replaces detected matches, including enabled URLs, with consistent markers such as `[LINK_1]` according to the selected masking mode.
11. The application returns the anonymized text, entity counts, optional reversal vault, and removed-noise count.

## 5. Alternative Flows

### A1 — No PII Detected

The application returns the prepared text unchanged and empty entity counts.

### A2 — No PII Types Enabled

The application performs no detection and leaves the text unchanged.

### A3 — Link and WhatsApp Live Location Detection

The application preserves URLs and live-location text when the `LINK` type is disabled. When enabled, supported URLs and `geo:` coordinate URIs are replaced with markers. If a line contains the phrase `Localização ao vivo` (or `Localização ao vivo compartilhada`) and a recognized Google Maps link or `geo:` URI, both the phrase and the link are replaced. Bare domains without a supported prefix are not recognized by this detector.

### A4 — Contextual Name, Address, and CNH Detection

Name detection is limited to values explicitly labeled `Nome`, `Nome completo`, or `Titular`. Address detection requires an `Endereço` or `Residência` label followed by a recognized street type such as `Rua`, `Avenida`, or `Travessa`. CNH detection requires a value of 11 digits (allowing spaces, periods, or hyphens) explicitly labeled `CNH` or `Carteira Nacional de Habilitação`; it does not validate the official CNH check digits. These patterns can miss unlabeled data and may still produce false positives, so the result must be reviewed.

### A5 — WhatsApp Cleanup Disabled

The application preserves the original text without removing WhatsApp system noise. If participant anonymization is enabled and the text matches the supported WhatsApp format, the parser still identifies participant names and marks their occurrences in message headers and bodies for masking.

### A6 — Reversible Masking

The application replaces matches with consistent identifiers and includes a tag-to-original-value vault in `AnonymizationOutput`. The vault is not currently stored or restored by the presentation layer.

### A7 — Irreversible Masking

The application replaces matches with identifiers and returns no reversal vault.

### A8 — Invalid Detection Match

The span consolidator ignores matches that do not represent a valid interval of the exact prepared text.

### A9 — Configuration Changed During Processing

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
| BR-013 | Link and location detection is controlled by the `LINK` type and recognizes URLs beginning with `http://`, `https://`, or `www.`, as well as `geo:` coordinate URIs. |
| BR-014 | Recognized URLs and paired live-location phrases are replaced with consistent `[LINK_n]` markers; bare domains without a supported prefix may remain visible. |
| BR-015 | Name detection is limited to values in explicitly labeled `Nome`, `Nome completo`, or `Titular` fields. |
| BR-016 | Address detection requires an `Endereço` or `Residência` label followed by a recognized Brazilian street-type prefix. |
| BR-017 | CNH detection requires an explicit CNH label and an 11-digit number, but does not validate CNH check digits. |

## 8. Postconditions

### Success

- The anonymized text is available to the presentation layer.
- Entity counts reflect consolidated matches.
- The original text remains unchanged.
- Chat-export noise removal is reported when applicable.

### Failure or Cancellation

- No cancelled operation is reported as successful.
- Processing errors are presented without raw personal information.

## 9. Acceptance Criteria

- [ ] Accepts text input.
- [ ] Processes only enabled PII types.
- [ ] Replaces enabled supported URLs and `geo:` coordinate URIs with `[LINK_n]` markers.
- [ ] Replaces the WhatsApp live-location phrase and associated map link when both are on the same line.
- [ ] Detects names only in supported labeled fields.
- [ ] Detects labeled addresses with recognized street types.
- [ ] Detects an 11-digit CNH number only when explicitly labeled.
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
- `ChatExportParser`
- `PiiDetector`
- `SpanConsolidator`
- `TextMasker`
- `SafeTextMasker`
- `PiiMatch`
- `AnonymizationOutput`
- `ApplyManualTextReplacementsUseCase` (see [UC-002 — Manual Text Redaction](./UC002-Manual_Text_Redaction.md))
