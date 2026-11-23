
# Development Guide

## 1. Requirements

The development environment requires:

- Android Studio compatible with the project's Gradle configuration.
- JDK version configured by the project.
- Android SDK.
- Gradle Wrapper included in the repository.

Use the versions specified in the project configuration rather than assuming the latest available versions.

## 2. Clone the Repository

```bash
git clone <REPOSITORY_URL>
cd anonimizador-offline
```

Replace `<REPOSITORY_URL>` with the actual repository address.

## 3. Open the Project

1. Open Android Studio.
2. Select the project directory.
3. Allow Gradle synchronization to complete.
4. Resolve any SDK or dependency configuration issues.

## 4. Build the Application

Run the following command to build the debug application:

```bash
./gradlew assembleDebug
```

## 5. Run Unit Tests

Execute the unit test suite:

```bash
./gradlew test
```

## 6. Useful Gradle Commands

```bash
# Clean build outputs
./gradlew clean

# Compile debug application
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run Android lint checks
./gradlew lint

# List available tasks
./gradlew tasks
```

Some tasks may depend on the project's configured Android modules and plugins.

## 7. Project Structure

```text
app/src/main/java/br/com/hdf/anonimizador/
├── core/
├── data/
├── di/
├── domain/
└── presentation/
```

See [Architecture](architecture.md) for layer responsibilities.

## 8. Development Guidelines

### Architecture

- Keep business rules in the Domain layer.
- Keep platform-specific implementations in the Data layer.
- Keep UI logic in the Presentation layer.
- Use dependency injection instead of manually constructing complex dependencies.

### Kotlin

- Prefer immutable data structures when possible.
- Use descriptive names.
- Keep functions focused on a single responsibility.
- Propagate coroutine cancellation.
- Avoid blocking the main thread.

### Privacy

- Never log original sensitive content.
- Avoid unnecessary persistence of user data.
- Keep reversible mappings temporary.
- Review new dependencies before adding them.

### Testing

- Add unit tests for new business rules.
- Test boundary conditions.
- Test invalid and malformed input.
- Update existing tests when contracts change.

## 9. Pull Requests

Before submitting changes:

1. Run relevant unit tests.
2. Verify compilation.
3. Review the affected architecture layers.
4. Update documentation when behavior changes.
5. Check that sensitive data is not exposed through logs.
