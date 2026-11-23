
# Architecture and Technical Decisions

This document records important architectural and technical decisions made during the development of Anonimizador Offline.

The purpose is to preserve the reasoning behind significant choices and provide context for future development.

Decisions should be updated when the project requirements or implementation change.

---

## Decision Status

| Status | Description |
|---|---|
| Proposed | Under consideration. |
| Accepted | Approved and currently applicable. |
| Superseded | Replaced by a newer decision. |
| Deprecated | No longer recommended, but not necessarily replaced. |

---

## Decision Format

Each decision should contain:

- ID
- Title
- Status
- Context
- Decision
- Consequences

---

## ADR-001 — Use Clean Architecture

**Status:** Accepted

### Context

The application contains several distinct responsibilities:

- PII detection.
- Text parsing.
- Anonymization.
- File extraction.
- User interface.
- Android-specific integration.

Combining these responsibilities would make the application difficult to test and maintain.

### Decision

Use Clean Architecture principles to separate the application into layers:

- `core`
- `domain`
- `data`
- `presentation`
- `di`

The domain layer contains business rules and contracts.

The data layer provides implementations of domain contracts.

The presentation layer handles user interaction and application state.

Dependency injection connects abstractions to their implementations.

### Consequences

Positive:

- Business rules can be tested independently.
- Android-specific code is isolated.
- Implementations can be replaced without rewriting business logic.
- Responsibilities are easier to identify.

Negative:

- Additional interfaces and classes may be required.
- The architecture requires discipline to avoid unnecessary abstractions.

---

## ADR-002 — Use Kotlin and Jetpack Compose

**Status:** Accepted

### Context

The application targets Android and requires a modern UI framework with support for declarative state-driven interfaces.

### Decision

Use Kotlin as the primary programming language and Jetpack Compose for the user interface.

Use Material 3 components where appropriate.

### Consequences

Positive:

- UI can be described declaratively.
- State changes can update the interface.
- Kotlin provides null safety and coroutine support.
- Compose supports reusable UI components.

Negative:

- Compose requires careful state management.
- UI behavior may need additional testing for recomposition and lifecycle changes.

---

## ADR-003 — Use MVVM and Unidirectional Data Flow

**Status:** Accepted

### Context

The application performs asynchronous operations and must keep the UI synchronized with the latest input and configuration.

### Decision

Use ViewModels to manage presentation state and user actions.

The UI emits actions, the ViewModel processes them, and observable state is rendered by Compose.

Use `StateFlow` for observable UI state where appropriate.

### Consequences

Positive:

- UI state is explicit.
- Business operations are easier to coordinate.
- UI components remain focused on presentation.
- State transitions can be tested.

Negative:

- ViewModels must carefully manage concurrent operations.
- State models may become large if responsibilities are not separated.

---

## ADR-004 — Keep Core Processing Offline

**Status:** Accepted

### Context

The application's purpose is to reduce exposure of sensitive information before content is shared.

Sending user content to a remote service would contradict this purpose.

### Decision

Perform core anonymization, parsing, detection, and masking locally on the device.

The application must not require an internet connection for its core functionality.

### Consequences

Positive:

- User content does not need to be uploaded to a processing server.
- Core functionality can operate without network access.
- The application reduces dependence on external services.

Negative:

- Detection capabilities are limited to local implementations.
- Large documents may be constrained by device resources.
- The application must manage processing performance locally.

This decision does not by itself guarantee complete device-level security.

---

## ADR-005 — Separate PII Detection from Masking

**Status:** Accepted

### Context

Detection and masking are different responsibilities.

A detector identifies sensitive information, while a masker transforms the identified text.

Combining them would make it difficult to support multiple detection strategies or masking modes.

### Decision

Use separate contracts for detection and masking.

The detection process returns `PiiMatch` objects.

The masking process receives validated matches and applies replacements.

### Consequences

Positive:

- Detectors can be tested independently.
- Masking logic can be reused.
- New detector implementations can be added.
- Detection results can be consolidated before replacement.

Negative:

- Additional orchestration is required.
- Match validity must be enforced between stages.

---

## ADR-006 — Use PiiMatch as the Detection Result

**Status:** Accepted

### Context

The application needs a consistent representation of detected sensitive information.

Detection results must include the matched text, its position, and its category.

### Decision

Represent detections using `PiiMatch`.

A match contains:

- Start index.
- End index.
- Original value.
- PII type.
- Confidence.
- Detector priority.

Indexes must refer to the exact text supplied to the detector.

### Consequences

Positive:

- Detection results have a consistent structure.
- Matches can be validated before masking.
- Consolidation can operate independently of detector implementation.

Negative:

- Matches become invalid if the corresponding text changes.
- All processing stages must preserve index consistency.

---

## ADR-007 — Consolidate Overlapping Matches Before Masking

**Status:** Accepted

### Context

Different detectors may identify overlapping intervals.

Applying overlapping replacements could corrupt the output or produce inconsistent masking.

### Decision

Use `SpanConsolidator` to resolve conflicts before masking.

The current policy is:

1. Prefer the longest valid interval.
2. Prefer higher confidence when lengths are equal.
3. Prefer higher detector priority when previous criteria are equal.
4. Apply deterministic tie-breaking.
5. Return accepted matches ordered by their position in the text.

Adjacent intervals are allowed because they do not overlap.

### Consequences

Positive:

- Replacement intervals are consistent.
- The same input produces deterministic consolidation.
- Longer matches can preserve complete identifiers.

Negative:

- Some shorter valid matches may be rejected.
- The greedy policy does not guarantee the maximum possible number of accepted intervals.
- Changing the policy may alter anonymization output and requires updated tests.

---

## ADR-008 — Use Reversible and Irreversible Masking Modes

**Status:** Accepted

### Context

Different users and situations require different levels of anonymization.

Some workflows need consistent placeholders that can be restored during the current session.

Other workflows require replacements that cannot be restored through the application.

### Decision

Support the masking modes implemented by the application:

- Reversible masking.
- Irreversible masking.

Reversible masking uses a mapping between original values and replacement identifiers.

Irreversible masking must not retain a restoration mapping.

### Consequences

Positive:

- Users can choose the required masking behavior.
- Reversible mode can support consistent references.
- Irreversible mode reduces the risk of restoration through application state.

Negative:

- Reversible mode requires careful lifecycle management.
- Mapping storage must be cleared when the session ends.
- Reversible masking is not equivalent to cryptographic encryption.

---

## ADR-009 — Use Coroutine-Based Processing

**Status:** Accepted

### Context

Text detection, file extraction, and anonymization may require significant processing time.

Blocking the main thread would make the interface unresponsive.

### Decision

Use Kotlin Coroutines for asynchronous processing.

Use appropriate dispatchers for CPU-intensive and I/O-intensive operations.

Long-running operations must support cancellation where practical.

### Consequences

Positive:

- The UI can remain responsive.
- Processing can be cancelled.
- Asynchronous operations can be coordinated.

Negative:

- Concurrent operations require explicit lifecycle management.
- Cancellation must be propagated correctly.
- Outdated jobs must not overwrite newer results.

---

## ADR-010 — Use Local PDF and ZIP Extraction

**Status:** Accepted

### Context

The application must process documents without uploading them to external services.

TXT, PDF, and ZIP are useful input formats for the intended workflows.

### Decision

Use Android document access APIs for file selection.

Use local extraction implementations for supported formats.

Use PDFBox Android for PDF text extraction.

ZIP processing must enforce configured input and extraction limits.

### Consequences

Positive:

- Files can be processed locally.
- The application can support common document formats.
- File access can use Android's document-provider mechanisms.

Negative:

- PDF extraction may consume substantial memory.
- Scanned PDFs may not contain extractable text.
- ZIP archives require defensive processing.
- Extraction limits do not guarantee a strict memory limit.

---

## ADR-011 — Keep Temporary Sensitive Data in Memory

**Status:** Accepted

### Context

The application processes sensitive text and may maintain mappings for reversible masking.

Persisting original content unnecessarily would increase exposure.

### Decision

Avoid persistent storage of original content and reversible mappings unless a future requirement explicitly justifies it.

Temporary sensitive data should follow the current session lifecycle.

### Consequences

Positive:

- Reduces unnecessary persistence.
- Limits the lifetime of reversible mappings.
- Supports the offline privacy model.

Negative:

- Session data may be lost when the application process is terminated.
- In-memory data cannot be guaranteed to be securely erased from all memory locations.

---

## ADR-012 — Use Configurable PII Categories

**Status:** Accepted

### Context

Users may need to anonymize different categories of information depending on the content being shared.

Applying every detector unconditionally may produce unnecessary replacements.

### Decision

Allow users to enable and disable supported PII categories.

Detectors must respect the enabled category set.

Configuration changes that affect existing content should trigger reprocessing.

### Consequences

Positive:

- Users have control over anonymization.
- Unnecessary replacements can be reduced.
- The behavior is configurable.

Negative:

- Configuration changes must be synchronized with processing.
- Results must be invalidated when settings change.
- The UI must clearly communicate which categories are active.

---

## ADR-013 — Treat WhatsApp Parsing as a Separate Responsibility

**Status:** Accepted

### Context

WhatsApp exports contain message content, timestamps, participant names, and system-generated entries.

These structures differ from ordinary text and require specialized parsing.

### Decision

Keep WhatsApp parsing separate from generic PII detection.

The parser is responsible for recognizing supported export structures and applying configured cleanup rules.

Generic PII detectors remain responsible for identifying supported sensitive values.

### Consequences

Positive:

- WhatsApp-specific rules remain isolated.
- Parser behavior can be tested independently.
- Generic detectors can be reused for other input types.

Negative:

- Export format variations require additional parser rules.
- Parser changes may affect participant detection and noise cleanup.

---

## ADR-014 — Prefer Deterministic Processing

**Status:** Accepted

### Context

Anonymization results must be predictable and testable.

Nondeterministic replacement order could produce confusing or inconsistent output.

### Decision

Use deterministic ordering for:

- Detector results where ordering affects behavior.
- Match consolidation.
- Replacement identifiers.
- Final masking operations.

### Consequences

Positive:

- Results are easier to test.
- Repeated processing is more predictable.
- Debugging is simpler.

Negative:

- Tie-breaking rules must be maintained.
- Changes to ordering may affect existing outputs.

---

## ADR-015 — Document Security Claims Conservatively

**Status:** Accepted

### Context

The application is privacy-oriented, but local processing does not automatically guarantee complete security.

File libraries, operating-system behavior, clipboard access, and memory management may introduce limitations.

### Decision

Documentation must describe verified behavior and avoid absolute security claims.

Claims such as guaranteed secure memory erasure, complete PII detection, or absolute ZIP bomb protection must not be made unless supported by the implementation and evidence.

### Consequences

Positive:

- Documentation remains technically accurate.
- Users can understand the application's limitations.
- Security claims are easier to verify.

Negative:

- Documentation may need more explicit caveats.
- Security guarantees must be reviewed as the implementation evolves.

---

# 10. Updating Decisions

When a decision changes:

1. Identify the existing decision.
2. Explain why the previous decision is no longer appropriate.
3. Create a new decision entry.
4. Mark the previous decision as `Superseded`.
5. Update affected architecture and feature documentation.
6. Update tests where behavior changes.

Do not silently rewrite accepted decisions when the change affects architecture, privacy, or business rules.

---

# 11. Related Documentation

- `README.pt-br.md`
- `AGENTS.md`
- `docs/ai-context.md`
- `docs/architecture.md`
- `docs/requirements.md`
- `docs/anonymization.md`
- `docs/features.md`
- `docs/file-processing.md`
- `docs/privacy-and-security.md`
- `docs/testing.md`
