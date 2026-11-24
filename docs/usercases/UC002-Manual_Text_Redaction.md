# UC-002 — Manual Text Redaction

## 1. Overview

| Attribute | Description |
|---|---|
| Use Case ID | UC-002 |
| Name | Manual Text Redaction |
| Primary Actor | User |
| Goal | Redact selected or located text in the anonymized result |
| Priority | Medium |
| Related Component | `ApplyManualTextReplacementsUseCase` using the Command pattern |

## 2. Preconditions

- An anonymized result is available.
- The result is not currently being processed.

## 3. Trigger

The user selects a passage in a short result or searches for text in a long result and requests redaction.

## 4. Main Success Scenario

1. For a short result, the user selects a passage and uses the floating redaction action. For a long result, the user locates text and chooses to redact its first occurrence or all occurrences.
2. The presentation layer creates a replacement command using the fixed `[REMOVIDO]` marker and the selected occurrence scope.
3. The ViewModel invokes `ApplyManualTextReplacementsUseCase` on the computation dispatcher.
4. The use case executes each command and returns the updated text with applied command records.
5. Each applied command record stores the changed spans and provides an inverse operation for undo.
6. The ViewModel pushes the applied command onto the undo stack and clears the redo stack.
7. The ViewModel updates the count of occurrences whose text changed.
8. The presentation layer updates the result and enables the applicable **Desfazer** and **Refazer** actions.

## 5. Alternative Flows

### A1 — Short Result

For results of up to 15,000 characters, the user selects a range and redacts that occurrence using the fixed `[REMOVIDO]` marker.

### A2 — Long Result

For results longer than 15,000 characters, the UI shows a limited preview and provides a search field. The user can redact the first matching occurrence or all matching occurrences in the full result using `[REMOVIDO]`.

### A3 — Target Not Found

The use case leaves the text unchanged and reports no applied operation. The ViewModel presents a user-facing message.

### A4 — Configuration Reprocessing

When anonymization is repeated after configuration changes, stored operations are reapplied in order. Only operations whose targets still match are retained.

### A5 — Source Text Changed

Changing the original input clears both history stacks so commands do not carry over to another document.

### A6 — Undo and Redo

Undo applies the inverse of the most recently executed redaction and moves its command to the redo stack. Redo executes the most recently undone command and moves the new applied command record back to the undo stack. A new redaction clears the redo stack.

### A7 — Configuration Reprocessing

Applied commands are replayed in order against the newly anonymized result. Commands whose targets still match are retained with updated spans; commands that no longer match are discarded. Redo commands are rebased against the result after active commands.

### A8 — Manual Removal Count

The result summary shows automatically detected entities and manually redacted occurrences separately to avoid implying they are unique items. The format is “N dados detectados · M trechos vetados”. The manual count includes occurrences whose original text was changed by applied manual commands; commands that replace text with itself do not count. Undo, redo, source changes, and configuration reprocessing update the count with the active command history.
When all result text is removed, the result card remains visible and explains that the text was manually removed, with undo available to restore it.
The result card also remains visible while undo or redo history exists, including when the current result is empty or contains only whitespace.

## 6. Exception Flows

### E1 — Invalid Operation

An empty target or a negative occurrence index is rejected with `IllegalArgumentException`. UI input prevents empty targets, while the domain use case validates its contract independently.

### E2 — Processing Cancelled

The ViewModel propagates coroutine cancellation and does not publish an obsolete replacement result.

## 7. Business Rules

| ID | Rule |
|---|---|
| BR-001 | An empty target is invalid; users cannot provide a custom replacement value. |
| BR-002 | A selected occurrence is replaced with `[REMOVIDO]`. |
| BR-003 | First-occurrence scope redacts only the first matching occurrence with `[REMOVIDO]`. |
| BR-004 | All-occurrences scope redacts every non-overlapping match with `[REMOVIDO]`. |
| BR-005 | The Command pattern represents each manual replacement as an executable command with a reversible execution record. |
| BR-006 | Undo reverses the most recently applied command; redo executes the most recently undone command. |
| BR-007 | A new replacement clears the redo stack. |
| BR-008 | Manual commands are replayed in order after configuration changes; unmatched commands are discarded. |
| BR-009 | Changing source text clears both history stacks. |
| BR-010 | Command history exists only for the current ViewModel session and is not persisted to disk. |
| BR-011 | Manual redaction changes the anonymized result only; the user must review the result before sharing. |
| BR-012 | The original input remains unchanged by manual redaction. |
| BR-013 | The manual removal count includes applied occurrences whose original and replacement text differ; it excludes no-op replacements and undone commands. |

## 8. Postconditions

### Success

- The anonymized result reflects the requested manual replacements.
- Undo and redo history remains available during the current session.
- The result displays the number of occurrences redacted by active manual commands.

### No Match

- The result remains unchanged and the user is informed that the target was not found.

## 9. Acceptance Criteria

- [ ] Redacts a selected occurrence in a result up to 15,000 characters with `[REMOVIDO]`.
- [ ] Redacts the first or all located matches in the complete long result with `[REMOVIDO]`.
- [ ] Undoes and redoes replacement commands in last-in-first-out order.
- [ ] Clears redo history after a new command is executed.
- [ ] Preserves commands whose targets still match after configuration reprocessing and drops unmatched commands.
- [ ] Clears undo and redo history when the original input changes.
- [ ] Keeps command history in memory only for the current session.
- [ ] Counts active manual changes, including replacements with non-empty values, updates on undo/redo and reprocessing, and excludes no-op replacements.
- [ ] Rejects empty targets and negative occurrence indexes.
- [ ] Does not ask the user to enter a replacement value.
- [ ] Does not alter the original input text.

## 10. Related Components

- `ApplyManualTextReplacementsUseCase`
- `ManualTextReplacementCommand`
- `ManualTextReplacementResult`
- `AppliedManualTextReplacementCommand`
- `AnonymizerViewModel`
- `OutputCard`
