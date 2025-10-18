# Project Structure (v1.1.0)

## Overview
Multi-class, well-architected Ethereum keystore password recovery tool following SOLID principles.

## Architecture

```
┌─────────────────────────┐
│ KeystoreRecoveryApp     │ ← Entry point (CLI)
└──────────┬──────────────┘
           │
           ├── Uses ───────────────────┐
           │                           │
           ▼                           ▼
    ┌─────────────┐           ┌──────────────┐
    │ Password    │           │ Password     │
    │ Config      │           │ Generator    │
    └─────────────┘           └──────────────┘
           │                           │
           └────────┬──────────────────┘
                    ▼
           ┌─────────────────┐
           │ Recovery        │
           │ Engine          │
           └────────┬────────┘
                    │
                    ▼
           ┌─────────────────────┐
           │ KeystoreValidator   │ ← Interface
           └────────┬────────────┘
                    │
                    ▼
           ┌──────────────────────┐
           │ Web3jKeystore        │ ← Implementation
           │ Validator            │
           └──────────────────────┘
```

## Class Descriptions

### KeystoreRecoveryApp
- **Role**: Main entry point and CLI
- **Responsibilities**:
  - User interaction
  - Input validation
  - Component orchestration
  - Result handling
- **Lines**: 220
- **Package**: Default

### PasswordConfig
- **Role**: Immutable configuration holder
- **Responsibilities**:
  - Store password components
  - Parse markdown config files
  - Validate configuration
  - Generate sample configs
- **Lines**: 300
- **Package**: Default
- **Key Features**:
  - Immutable with Builder pattern
  - Unmodifiable collections
  - Thread-safe

### PasswordGenerator
- **Role**: Password combination generator
- **Responsibilities**:
  - Generate base combinations
  - Apply capitalization variants
  - Create word combinations
  - Estimate total combinations
- **Lines**: 145
- **Package**: Default
- **Key Features**:
  - Stateless
  - All constants extracted
  - Highly reusable

### KeystoreValidator (Interface)
- **Role**: Define validation contract
- **Responsibilities**:
  - Specify validation interface
  - Enable dependency injection
- **Lines**: 30
- **Package**: Default
- **Key Features**:
  - Small, focused interface
  - Enables testing
  - Loose coupling

### Web3jKeystoreValidator
- **Role**: Keystore password validation
- **Responsibilities**:
  - Test passwords against keystore
  - Manage temp files
  - Handle Web3j interactions
- **Lines**: 140
- **Package**: Default
- **Key Features**:
  - Thread-safe (synchronized)
  - Resource management
  - Implements interface

### RecoveryEngine
- **Role**: Multi-threaded recovery coordinator
- **Responsibilities**:
  - Distribute work across threads
  - Monitor progress
  - Coordinate validation
  - Return results
- **Lines**: 250
- **Package**: Default
- **Key Features**:
  - Thread-safe
  - Progress monitoring
  - Result object pattern
  - Graceful shutdown

## Constants Defined

### PasswordGenerator
```java
MIN_BASE_LENGTH = 5
MAX_BASE_LENGTH = 12
WORD_SEPARATORS = {"", "-", "_", "."}
```

### RecoveryEngine
```java
MIN_THREADS = 1
MAX_THREADS = 100
PROGRESS_UPDATE_INTERVAL_MS = 1000
```

### KeystoreRecoveryApp
```java
BANNER_WIDTH = 60
DEFAULT_CONFIG_FILE = "password_config.md"
OUTPUT_FILE = "recovered_password.txt"
```

## Design Patterns Used

1. **Builder Pattern** - PasswordConfig construction
2. **Strategy Pattern** - KeystoreValidator interface
3. **Result Object Pattern** - RecoveryResult
4. **Dependency Injection** - RecoveryEngine constructor
5. **Singleton Pattern** - Constants as static final

## Thread Safety

- **PasswordConfig**: Immutable (thread-safe)
- **PasswordGenerator**: Stateless (thread-safe)
- **KeystoreValidator**: Interface (implementation-dependent)
- **Web3jKeystoreValidator**: Synchronized validate() (thread-safe)
- **RecoveryEngine**: AtomicLong, AtomicBoolean (thread-safe)

## SOLID Principles

| Principle | Compliance | Implementation |
|-----------|------------|----------------|
| **S**ingle Responsibility | ✅ Yes | Each class has one clear purpose |
| **O**pen/Closed | ✅ Yes | Extendable via interfaces |
| **L**iskov Substitution | ✅ Yes | KeystoreValidator implementations |
| **I**nterface Segregation | ✅ Yes | Small, focused interfaces |
| **D**ependency Inversion | ✅ Yes | Depends on abstractions |

## Testing Strategy

### Unit Tests (Future)
- PasswordConfig builder
- PasswordGenerator combinations
- PasswordConfig immutability
- Mock validator testing

### Integration Tests (Future)
- Full recovery workflow
- Error handling
- File operations

### Current Test Coverage
- **0%** - No tests yet (architecture now supports testing!)

## Usage Example

```java
// Initialize components
Web3jKeystoreValidator validator =
    new Web3jKeystoreValidator("keystore.json");
PasswordGenerator generator = new PasswordGenerator();
RecoveryEngine engine = new RecoveryEngine(validator, generator, 8);

// Load configuration
PasswordConfig config = PasswordConfig.fromMarkdown("password_config.md");

// Run recovery
RecoveryResult result = engine.recover(config);

// Check results
if (result.isSuccess()) {
    System.out.println("Password: " + result.getPassword());
    System.out.println("Attempts: " + result.getAttempts());
    System.out.println("Time: " + result.getTimeSec() + "s");
}
```

## Build Commands

```bash
# Compile
mvn clean compile

# Package
mvn clean package

# Run
java -jar target/keystore-recovery.jar

# Run with Maven
mvn exec:java -Dexec.args="keystore.json password_config.md"
```

## Dependencies

- Web3j 4.10.0
- Bouncy Castle 1.70
- SLF4J 2.0.9
- Java 15+

## Version History

- **v1.0.0** - Initial monolithic implementation
- **v1.1.0** - Refactored OOP architecture (current)

---

*Last Updated: 2025-10-18*
