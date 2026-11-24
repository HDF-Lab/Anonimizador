
# Anonimizador Offline — Architecture

## 1. Overview

Anonimizador Offline follows an architecture based on Clean Architecture, MVVM (Model-View-ViewModel), and UDF (Unidirectional Data Flow).

The architecture aims to keep business rules independent of the Android user interface, simplify testing, and support the evolution of detection and anonymization mechanisms.

The main architectural goals are:

- Separation of concerns.
- Low coupling between layers.
- Independence of business rules from Android-specific components.
- Maintainability and testability.
- Local data processing.
- Extensibility of detection mechanisms without changing the main processing flow.

## 2. Layer Organization

The project is organized into the following layers:

```text
br.com.hdf.anonimizador
├── core/
├── data/
├── di/
├── domain/
└── presentation/
```

### 2.1 Core

Contains shared utilities and functionality used across different parts of the application.

Responsibilities:

- Specific data validators.
- Utilities independent of application features.
- Application security configurations.
- Shared technical resources.

Examples:

- `BrazilianDocumentValidator`
- Window security utilities.

### 2.2 Domain

Represents the core of the application's business logic.

It should not depend on Android UI components, presentation libraries, or concrete data-layer implementations.

Responsibilities:

- Define domain models.
- Define contracts and interfaces.
- Implement business rules.
- Orchestrate anonymization processes.
- Define detection and masking policies.

Main components:

- `PiiDetector`
- `TextMasker`
- `SpanConsolidator`
- `WhatsAppParser`
- `AnonymizeContentUseCase`
- `PiiMatch`
- `PiiType`
- `AnonymizationOutput`

### 2.3 Data

Contains concrete implementations of the contracts defined by the domain layer.

Responsibilities:

- Detect personally identifiable information.
- Extract content from files.
- Parse exported conversations.
- Apply text substitutions.
- Integrate external document-processing libraries.

Examples:

- `BrazilRegexPiiDetector`
- `SafeTextMasker`
- `DefaultSpanConsolidator`
- `AndroidDocumentExtractor`

### 2.4 Presentation

Responsible for user interaction.

Jetpack Compose is used to build the user interface, while ViewModel manages screen state and user actions.

Responsibilities:

- Display anonymization results.
- Receive text and files.
- Manage user settings.
- Expose UI state.
- Handle user events.
- Communicate with use cases.

Main components:

- `AnonymizerScreen`
- `AnonymizerViewModel`
- `AnonymizerUiState`
- `SettingsBottomSheet`
- `SettingsMenu`

### 2.5 Dependency Injection (DI)

Responsible for configuring dependency injection using Hilt.

Responsibilities:

- Register contract implementations.
- Configure dependencies between components.
- Provide dispatchers for asynchronous operations.
- Define dependency scopes.

---

## 3. Dependency Flow

Dependencies should point inward, toward the domain layer.

The Domain layer does not depend on concrete implementations from the Data layer or components from the Presentation layer.

```text
             PRESENTATION
                  |
                  v
                DOMAIN
                  ^
                  |
                 DATA

                  DI
       Configures dependencies
```

The Data layer implements contracts defined by the Domain layer.

The Presentation layer uses Domain use cases to execute operations.

The DI layer connects concrete implementations to their respective contracts.

---

## 4. Anonymization Flow

The anonymization process is orchestrated by `AnonymizeContentUseCase`.

The general processing flow is:

```text
          User Input
              |
              v
        Text Preparation
              |
              v
         WhatsApp Parser
              |
              v
          PII Detection
              |
              v
        Span Consolidation
              |
              v
          Text Masking
              |
              v
       Anonymized Output
              |
              v
           User Interface
```

### 4.1 Text Preparation

The input content is prepared before detection.

When the input matches a supported WhatsApp export, the parser identifies participants if cleanup or participant anonymization is enabled. With cleanup enabled, it removes supported automatic message artifacts and prepares normalized text. With cleanup disabled, it preserves the original text while still identifying participant names when participant anonymization is enabled. All occurrences of identified names in the prepared text, including message bodies, are provided as participant matches for masking.

The prepared text becomes the input for subsequent detection stages.

### 4.2 PII Detection

Enabled detectors analyze the prepared text and identify potential personal information.

Each detector implements the `PiiDetector` contract.

Detection results are represented by `PiiMatch`, which contains:

- Start and end positions.
- Original matched value.
- PII category.
- Confidence score.
- Detector priority.

Multiple detectors can operate independently and their results are combined.

### 4.3 Span Consolidation

Different detectors may identify overlapping text ranges.

`SpanConsolidator` validates detection results and resolves overlapping intervals before masking.

The default implementation prioritizes:

1. Longer intervals.
2. Higher confidence.
3. Higher detector priority.
4. Deterministic tie-breaking criteria.

Only valid, non-overlapping matches are forwarded to the masking stage.

### 4.4 Text Masking

`SafeTextMasker` applies substitutions to the validated matches.

It supports two masking modes:

- **Irreversible:** replaces detected values without maintaining a recovery mapping.
- **Reversible:** maintains a temporary mapping between original values and generated identifiers.

Replacements are applied from the end of the text toward the beginning to preserve the original positions of remaining matches.

### 4.5 Anonymization Output

The process produces an `AnonymizationOutput` containing the processed text and relevant detection information.

The Presentation layer receives the result and updates the user interface.

---

## 5. State Management

The Presentation layer follows the MVVM pattern and uses unidirectional data flow.

The main components are:

- **UI:** renders the current state and forwards user actions.
- **ViewModel:** processes user events and coordinates use case execution.
- **UiState:** represents the current screen state.

The general interaction flow is:

```text
       User Interaction
              |
              v
         UI Event
              |
              v
          ViewModel
              |
              v
          Use Case
              |
              v
        Processing Result
              |
              v
          UI State
              |
              v
        Compose Recomposition
```

The UI observes state exposed by the ViewModel.

User actions are forwarded to the ViewModel, which updates the state and triggers the required operations.

---

## 6. Asynchronous Processing

Kotlin Coroutines are used for asynchronous operations.

The application uses dispatchers to keep expensive operations away from the main thread.

Examples include:

- File extraction.
- PII detection.
- Text processing.
- Anonymization.

Cancellation should be propagated through processing stages to prevent outdated operations from publishing results.

---

## 7. Design Principles

The architecture follows these principles:

- **Single Responsibility:** each component has a clearly defined responsibility.
- **Dependency Inversion:** high-level business rules depend on abstractions rather than concrete implementations.
- **Separation of Concerns:** presentation, business logic, and data processing remain independent.
- **Testability:** business rules can be tested independently of Android UI components.
- **Extensibility:** new detectors and processing strategies can be introduced through existing contracts.
- **Deterministic Processing:** overlapping detections are resolved using explicit and predictable rules.
- **Privacy by Design:** personal data processing is designed to occur locally on the device.

---

## 8. Technology Stack

| Technology | Purpose |
|---|---|
| Kotlin | Primary programming language |
| Jetpack Compose | Declarative user interface |
| Material 3 | UI design system |
| Hilt | Dependency injection |
| Kotlin Coroutines | Asynchronous processing |
| StateFlow | Reactive state management |
| Apache PDFBox Android | PDF text extraction |
| JUnit | Unit testing |

---

## 9. Architectural Considerations

The architecture is designed to support future improvements without requiring changes to the main application flow.

Potential extensions include:

- Additional PII detectors.
- On-device Named Entity Recognition (NER).
- New document formats.
- Additional anonymization strategies.
- Improved validation and detection policies.

New implementations should follow the existing domain contracts whenever possible, preserving separation of concerns and minimizing changes to existing components.


## 10. Application User Cases

| Use Case | Responsibility | Documentation |
|---|---|---|
| AnonymizeContentUseCase | Coordinates content anonymization | [UC-001](usercases/UC001-Anonymize_Content.md) |