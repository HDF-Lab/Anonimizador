## 1. Project Overview

Anonimizador Offline is an Android application designed to anonymize sensitive personal information in text, PDF documents, and exported WhatsApp conversations.

The application processes content locally on the device, without requiring an internet connection.

Its main purpose is to help users protect personal information before sharing content with third parties.

### Core principles

- Privacy by design.
- Offline-first processing.
- Separation of responsibilities.
- Explicit and predictable behavior.
- Accessibility and usability.
- Maintainable and testable code.
- Minimal and justified dependencies.

---

## 2. Technology Stack

The project uses the following technologies:

- Kotlin
- Android SDK
- Jetpack Compose
- Material 3
- Coroutines
- StateFlow
- Hilt
- Gradle
- PDFBox Android

Before introducing a new library or framework, check whether the existing stack already provides the required functionality.

Do not add dependencies without a clear technical justification.

---

## 3. Architecture

The project follows Clean Architecture principles combined with MVVM and unidirectional data flow.

Main packages:

- `core`: shared utilities, validators, and platform-independent infrastructure.
- `domain`: business models, contracts, policies, and use cases.
- `data`: implementations of domain contracts, detectors, parsers, and data extraction.
- `presentation`: UI components, screens, ViewModels, and UI state.
- `di`: dependency injection configuration.

### Dependency rules

- Domain must not depend on Android framework APIs.
- Domain must not depend on presentation or data implementations.
- Presentation may depend on domain.
- Data may depend on domain contracts.
- Dependency injection connects abstractions to their implementations.
- Avoid circular dependencies.

Do not move business rules into Compose components or Android-specific classes.

---

## 4. General Coding Guidelines

When modifying the code:

1. Inspect the existing implementation before making changes.
2. Understand the responsibilities of the affected classes.
3. Follow existing naming and package conventions.
4. Prefer small, focused changes.
5. Preserve public interfaces unless a change is necessary.
6. Avoid unrelated refactoring.
7. Avoid duplicated business rules.
8. Prefer explicit behavior over implicit side effects.
9. Keep functions focused on a single responsibility.
10. Use meaningful names for classes, functions, and variables.

Do not create abstractions solely to reduce the number of lines of code.

Do not introduce generic frameworks or unnecessary layers for simple operations.

Do not silently change existing application behavior.

---

## 5. Kotlin Guidelines

- Prefer immutable data with `val`.
- Use data classes for models when appropriate.
- Use sealed classes or sealed interfaces for well-defined state variations.
- Handle nullable values explicitly.
- Avoid `!!` unless there is a documented and justified invariant.
- Avoid unnecessary scope functions and nested lambdas.
- Prefer extension functions only when they improve readability.
- Use structured concurrency.
- Propagate coroutine cancellation.
- Avoid blocking the main thread.
- Use dispatcher abstractions where appropriate.

Long-running operations must not execute on the main thread.

Do not swallow exceptions with empty `catch` blocks.

Cancellation exceptions must be propagated instead of being treated as ordinary failures.

---

## 6. Domain Layer

The domain layer contains the application's business rules.

Keep the following responsibilities separated:

- PII detection.
- WhatsApp parsing.
- Text preparation.
- Match consolidation.
- Text masking.
- Reversible mapping.
- Anonymization orchestration.

### Use cases

Use cases should coordinate business operations without depending on Android UI components.

Avoid placing business logic inside ViewModels or Compose screens when it belongs in the domain layer.

### PII detection

Detectors must:

- Respect the enabled PII types.
- Return indexes relative to the exact input text.
- Preserve the original matched value.
- Provide meaningful confidence and priority values when applicable.
- Support coroutine cancellation.
- Avoid modifying the input text.

Do not claim that regex-based detection guarantees identification of every personal information occurrence.

### Span consolidation

The current consolidation contract is:

`SpanConsolidator.consolidate(text, matches)`

The implementation must validate matches against the exact text being processed.

The current conflict-resolution policy is:

**Longest interval first, preserving non-overlapping matches whenever possible.**

Tie-breaking rules must remain deterministic.

Adjacent intervals are valid and must not be considered overlapping.

Do not change this policy without reviewing its impact on anonymization and tests.

### Text masking

The masking implementation must:

- Validate match boundaries.
- Reject overlapping intervals.
- Preserve text outside matched intervals.
- Apply replacements without corrupting indexes.
- Maintain consistent replacement identifiers.
- Respect the selected masking mode.
- Support cancellation during long operations.

Never silently discard invalid matches without a documented reason.

---

## 7. WhatsApp Processing

WhatsApp parsing must remain separate from generic PII detection.

The parser may identify:

- Message boundaries.
- Timestamps.
- Participants.
- System-generated messages.
- Automatic WhatsApp notifications.
- Known media placeholders.

Noise cleanup and participant anonymization must follow their respective configuration settings.

Do not assume every line follows the same WhatsApp export format.

Support format variations through explicit parsing rules and tests.

Do not remove message content merely because it resembles a system notification.

When changing parser rules, include examples of valid and invalid inputs in tests.

---

## 8. File Processing

Supported input formats may include:

- TXT
- PDF
- ZIP containing TXT files

File extraction must:

- Use Android content URIs where applicable.
- Avoid assuming filesystem paths.
- Validate supported MIME types and extensions.
- Enforce configured input and extraction limits.
- Handle malformed and unsupported files.
- Close streams and document resources.
- Propagate coroutine cancellation.
- Avoid blocking the main thread.

ZIP processing must consider entry count, compressed input size, and uncompressed extraction limits.

PDF processing must consider that extracted text limits do not guarantee a strict memory limit for the PDF library.

Do not claim absolute protection against malicious files.

When changing extraction limits, update tests and documentation.

---

## 9. Presentation Layer

The presentation layer uses Jetpack Compose and ViewModels.

Follow unidirectional data flow:

- UI emits user actions.
- ViewModel handles actions.
- ViewModel updates observable UI state.
- Compose renders the current state.

### Compose guidelines

- Prefer small, reusable composables with clear responsibilities.
- Keep business logic out of composables.
- Use Material 3 components consistently.
- Support accessibility semantics.
- Provide meaningful content descriptions where necessary.
- Ensure controls have appropriate touch targets.
- Handle loading, empty, success, and error states.
- Avoid unnecessary recompositions.
- Do not use UI state as a substitute for domain state.

### ViewModel guidelines

- Expose observable UI state.
- Keep state transitions predictable.
- Cancel obsolete processing jobs.
- Prevent outdated jobs from overwriting newer results.
- Handle repeated input values correctly.
- Keep configuration changes consistent with the current input.
- Avoid launching unmanaged coroutines.

Settings changes that are intended to affect existing content must trigger reprocessing according to the current application behavior.

Do not disable configuration controls during processing unless there is a documented functional reason.

---

## 10. Privacy and Security

Privacy is a central requirement.

Any modification must preserve the offline-first design.

### Mandatory considerations

- Do not introduce network communication.
- Do not upload user content.
- Do not add telemetry or analytics that collect user content.
- Do not log raw personal information.
- Do not expose sensitive content in exception messages.
- Avoid unnecessary persistence of original text.
- Clear temporary sensitive data when its lifecycle ends.
- Review clipboard and file-sharing behavior.
- Preserve secure-screen behavior where configured.

Before adding a dependency, inspect its permissions and potential network behavior.

Do not claim that sensitive data is securely erased from memory unless the implementation can guarantee that property.

---

## 11. Error Handling

Errors should be explicit and meaningful.

Distinguish between:

- Invalid input.
- Unsupported file format.
- File access failure.
- File size limit exceeded.
- Extraction failure.
- Processing failure.
- Cancellation.

User-facing messages should be understandable and should not expose internal implementation details.

Do not convert every exception into a generic success or empty result.

Use the existing error-handling conventions before introducing new mechanisms.

---

## 12. Testing Requirements

Every meaningful business-rule change should include or update tests.

Prioritize unit tests for:

- PII detectors.
- PII match validation.
- Span consolidation.
- Text masking.
- WhatsApp parsing.
- File extraction limits.
- Use case orchestration.
- Configuration changes.
- Cancellation and concurrent processing.

Tests should cover:

- Normal cases.
- Empty input.
- Invalid input.
- Boundary conditions.
- Overlapping matches.
- Adjacent matches.
- Malformed files.
- Large inputs within and beyond configured limits.
- Cancellation where applicable.

Avoid tests that only verify implementation details.

Prefer tests that verify observable behavior and business rules.

Do not claim tests passed unless they were actually executed successfully.

---

## 13. Documentation

Update documentation whenever a change affects:

- Architecture.
- Business rules.
- Supported file formats.
- PII detection behavior.
- Masking behavior.
- Privacy or security guarantees.
- Configuration behavior.
- Build or development procedures.

Relevant documentation is maintained in `docs/`.

If documentation conflicts with the actual implementation, inspect the source code and tests, then update the documentation.

Do not invent features or security guarantees.

---

## 14. AI-Assisted Development Workflow

When working on a task:

1. Identify the requested behavior.
2. Inspect the relevant source files.
3. Identify affected interfaces and implementations.
4. Check dependencies and call sites.
5. Review existing tests.
6. Propose the smallest appropriate change.
7. Implement the change consistently with the architecture.
8. Update affected tests.
9. Update documentation when necessary.
10. Report the files changed and any remaining limitations.

### Before modifying code

Do not assume that a class, method, or interface has the same implementation described in documentation.

Inspect the current source.

Do not invent package names, method signatures, dependencies, or enum values.

### When changing interfaces

Check all affected:

- Implementations.
- Call sites.
- Dependency injection bindings.
- Tests.
- Mocks and fakes.

Explain breaking changes explicitly.

### When reviewing code

Prioritize:

1. Functional correctness.
2. Privacy and security.
3. Coroutine and concurrency safety.
4. Data integrity.
5. Architecture and separation of concerns.
6. Accessibility and usability.
7. Performance.
8. Readability and maintainability.

Do not recommend refactoring merely for stylistic preference.

---

## 15. Expected Response Format for Code Changes

When proposing code modifications:

- Explain the problem briefly.
- Identify the affected files.
- Provide complete code when requested.
- Explain important design decisions.
- Identify signature changes and affected call sites.
- Include relevant tests or test scenarios.
- Mention assumptions and limitations.
- Distinguish verified results from recommendations.

Do not claim that code compiles or tests pass without executing the relevant commands.

---

## 16. Important Rule

Preserve the project's fundamental objective:

**Anonymize sensitive information locally, predictably, and transparently, without compromising user privacy.**

Every architectural or implementation decision should be evaluated against this objective.


## 17. Command Execution Policy

**Never execute any command without explicit user approval.**

- Always explain the command and its purpose before execution.
- Show the exact command to the user.
- Wait for explicit confirmation before running it.
- Do not assume approval from previous commands or instructions.
- Each new command or group of commands requires separate approval.
- Never execute commands automatically as part of code changes.
- Do not run build, test, lint, formatting, installation, Git, or cleanup commands without approval.
- Never execute destructive or potentially irreversible commands without explicit approval.
- If a command is necessary, suggest it and explain its expected impact instead of executing it.

This policy applies to all execution environments, including terminals, shells, IDE terminals, scripts, and automated tasks.

**Default behavior: propose commands, do not execute them.**
