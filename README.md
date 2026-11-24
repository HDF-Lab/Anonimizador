[![Android CI](https://github.com/HDF-Lab/Anonimizador/actions/workflows/android.yml/badge.svg)](https://github.com/HDF-Lab/Anonimizador/actions/workflows/android.yml)
# 🛡️ Anonimizador Offline — LGPD & Privacy

An **offline, open-source Android application** for cleaning and anonymizing personal data (PII — Personally Identifiable Information) in text, PDF documents, and exported WhatsApp conversations.

The goal is to allow users to remove or replace personal information before sharing content with third parties or Artificial Intelligence tools.

---

## 🔒 Security and Privacy

Privacy is a core principle of this project.

- **No Internet permission:** the application is designed not to communicate directly with external servers. The `AndroidManifest.xml` must remain free of the `android.permission.INTERNET` permission.
- **Local processing:** extraction, detection, and anonymization are performed directly on the device.
- **No telemetry:** the project must not include analytics, advertising, or tracking SDKs.
- **Screen protection:** `FLAG_SECURE` is used to restrict screenshots and screen recordings on protected screens when enabled.
- **Temporary data:** the reversible anonymization mapping is intended for the current session and does not require transmission to servers.

> The absence of the Internet permission reduces the application's network communication surface, but does not replace an audit of dependencies, permissions, storage, and Android sharing flows.

---

## ✨ Features

### 📄 Document Import

- **TXT:** reads UTF-8 text files.
- **PDF:** local text extraction using Apache PDFBox Android.
- **ZIP:** reads `.txt` files contained in WhatsApp exports.
- **Android sharing:** integration with the system sharing mechanism, according to the intent filters configured in the application.

### 💬 WhatsApp Conversation Processing

- Identification and removal of automatic noise.
- Handling of system notices and unavailable-content messages according to parser rules.
- Participant identification.
- Replacement of participant names with identifiers such as `[PARTICIPANT_1]`, `[PARTICIPANT_2]`, and so on.

### 🔎 Personal Information Detection

Detectors operate on the PII types enabled by the user.

- CPF and CNPJ, when enabled as independent types.
- PIX UUID (random key).
- Brazilian phone numbers.
- Email addresses.
- IPv4 addresses.
- Credit card numbers, validated using the Luhn algorithm.
- Vehicle license plates in the formats supported by the detector.

Detection uses regular expressions and specific validators for applicable formats.

> Detection is based on known patterns and does not guarantee that all personal information in a document will be identified.

### 🎭 Masking Modes

**Irreversible**

- Replaces detected values with labels.
- Does not generate a restoration mapping.
- Intended for sharing anonymized content.

**Reversible (session-based)**

- Replaces values with labels.
- Maintains a mapping between labels and original values.
- The mapping is intended to remain restricted to the current session.
- Restoration depends on the mapping being safely preserved.

---

## 🛡️ File Processing Protection

The document extractor implements limits to reduce the risk of excessive resource consumption.

| Resource | Configured limit |
|---|---:|
| Input file | 20 MB |
| Extracted TXT text | 10 MB |
| Extracted PDF text | 10 MB |
| ZIP entries | 1,000 |
| Uncompressed content inspection | 50 MB |

ZIP processing includes size and entry-count checks to reduce risks associated with excessively large or maliciously compressed files.

PDFBox may use additional memory to load internal document structures. Therefore, input and extracted-text limits do not represent an absolute memory limit.

---

## 🏗️ Architecture and Technologies

The project follows **Clean Architecture**, **MVVM**, and **UDF (Unidirectional Data Flow)** principles.

### Layer Organization

| Layer | Responsibility |
|---|---|
| `core` | Shared validators and utilities |
| `data` | Detectors, extractors, and masking implementations |
| `di` | Dependency injection modules |
| `domain` | Models, contracts, policies, and use cases |
| `presentation` | ViewModels, UI state, and Compose screens |

### Technology Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Design System:** Material Design 3
- **Dependency Injection:** Hilt (Dagger)
- **Asynchronous Processing:** Kotlin Coroutines and StateFlow
- **PDF Processing:** Apache PDFBox Android
- **Testing:** JUnit and testing tools compatible with the project's dependencies

### Package Structure

```text
br.com.hdf.anonimizador
├── core/
│   ├── util/           # Validators and utilities
│   └── security/       # Window security
├── data/
│   ├── detector/       # PII detectors
│   ├── ingestion/      # TXT, PDF, and ZIP extraction
│   ├── masking/        # Text masking
│   └── parser/         # Format-specific parser implementations (currently WhatsApp)
├── di/                 # Hilt modules and dispatchers
├── domain/
│   ├── detector/       # Detection contracts
│   ├── ingestion/      # Extraction contracts
│   ├── masking/        # Masking and consolidation contracts
│   ├── model/          # Domain models
│   ├── parser/         # Generic parsing contracts and models
│   └── usecase/        # Use cases
└── presentation/
    └── editor/         # ViewModel and Compose UI
```

---

## 🔄 Anonymization Flow

1. The user enters text or imports a document.
2. The content is prepared by the parser, when applicable.
3. Detectors identify potential personal information.
4. `SpanConsolidator` validates intervals and resolves overlaps.
5. `SafeTextMasker` replaces the identified values.
6. The result is presented to the user for review and sharing.

When intervals overlap, consolidation prioritizes the longest interval, using confidence and detector priority as tie-breaking criteria.

---

## 🚀 Building the Project

1. Clone the repository:

```bash
git clone <REPOSITORY_URL>
```

2. Open the project in Android Studio.
3. Use the JDK and Android Gradle Plugin versions configured in the project.
4. Sync the Gradle dependencies.
5. Run the tests:

```bash
./gradlew test
```

6. Build the application:

```bash
./gradlew assembleDebug
```

---

## 🧪 Known Limitations

- Pattern-based detectors cannot guarantee complete identification of personal information.
- Scanned PDFs containing images may not produce extractable text without OCR.
- ZIP extraction processes the first `.txt` file found.
- Files using an encoding other than UTF-8 may be interpreted incorrectly.
- Masking quality depends on the accuracy of the detection results.
- Users should review the anonymized content before sharing it.

---

## 📄 License

Distributed under the **MIT License**. See the [LICENSE](LICENSE) file for the terms of use.
