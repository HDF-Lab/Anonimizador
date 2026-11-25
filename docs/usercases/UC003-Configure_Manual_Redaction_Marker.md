# UC-003 — Configure Manual Redaction Marker

## 1. Overview

| Attribute | Description |
|---|---|
| Use Case ID | UC-003 |
| Name | Configure Manual Redaction Marker |
| Primary Actor | User |
| Goal | Choose the marker used for future manually redacted passages |
| Priority | Medium |
| Related Components | Settings UI, `AnonymizerViewModel`, UC-002 |

## 2. Preconditions

- The application is open.
- The settings panel is available.

## 3. Trigger

The user opens Settings and edits the **Marcador de veto manual** field.

## 4. Main Success Scenario

1. Settings show the current session's marker, defaulting to `[REMOVIDO]`.
2. The user enters a non-blank marker of up to 40 characters.
3. When settings are applied, the ViewModel trims surrounding whitespace and stores the marker in the current session.
4. Future manual redactions use the configured marker for short selections and long-result searches.
5. The result is not reprocessed solely because the marker changed.

## 5. Alternative Flows

### A1 — Empty Marker

The settings field reports that the marker cannot be blank. If settings are applied while it is blank, the ViewModel retains the previously configured marker while applying any other valid settings.

### A2 — Existing Manual Redactions

Changing the marker affects future redactions only. Existing redactions, including their undo/redo history, remain unchanged.

### A3 — Marker Length Limit

Input beyond 40 characters is not accepted.

## 6. Business Rules

| ID | Rule |
|---|---|
| BR-001 | The default manual redaction marker is `[REMOVIDO]`. |
| BR-002 | The marker is trimmed before it is stored. |
| BR-003 | A blank marker does not replace the current valid marker. |
| BR-004 | The marker is limited to 40 characters. |
| BR-005 | Changing the marker does not modify existing redactions or trigger anonymization again. |
| BR-006 | The marker is held in memory for the current ViewModel session and is not persisted. |

## 7. Acceptance Criteria

- [ ] Settings display the current marker and allow the user to edit it.
- [ ] Future short-text and long-text redactions use the configured marker.
- [ ] Blank values are rejected without clearing the existing marker.
- [ ] Inputs are limited to 40 characters, including values supplied outside the UI.
- [ ] Existing redactions and history remain unchanged when the marker changes.
- [ ] Clearing the session restores the default marker.
