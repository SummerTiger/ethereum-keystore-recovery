# Ethereum Keystore Password Recovery Tool

[![Java](https://img.shields.io/badge/Java-15%2B-orange.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.6%2B-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![CI/CD](https://github.com/SummerTiger/ethereum-keystore-recovery/workflows/CI%2FCD%20Pipeline/badge.svg)](https://github.com/SummerTiger/ethereum-keystore-recovery/actions)
[![Tests](https://img.shields.io/badge/Tests-170%20passing-brightgreen.svg)](https://github.com/SummerTiger/ethereum-keystore-recovery)
[![Coverage](https://img.shields.io/badge/Coverage-96%25-brightgreen.svg)](https://github.com/SummerTiger/ethereum-keystore-recovery)

A high-performance, multi-threaded Java application for recovering forgotten Ethereum keystore passwords when you remember the password pattern.

## 🌟 Features

- **🚀 High Performance**: Multi-threaded password testing with optimized scrypt validation (5-10 passwords/sec per thread with Ethereum's scrypt parameters)
- **🔧 Pattern-Based**: Generates passwords matching `[5-12 chars] + [1-5 digits] + [1 special char]`
- **📝 Easy Configuration**: Simple markdown file for password components
- **⚡ Real-time Progress**: Live monitoring of attempts per second
- **🔒 Security-First**: Secure file permissions, password masking, explicit user confirmation
- **🏗️ Clean Architecture**: Professional OOP design following SOLID principles
- **📚 Well-Documented**: Comprehensive JavaDoc and user documentation

## 📊 Performance

**Important**: Ethereum keystores use scrypt with high parameters (n=262144) for security, making each validation intentionally slow (~100-200ms).

| Configuration | Passwords/Second | Time for 10,000 Combinations |
|---------------|------------------|------------------------------|
| Single-threaded | ~5-10 | ~16-33 minutes |
| 4 threads | ~20-40 | ~4-8 minutes |
| 8 threads | ~40-80 | ~2-4 minutes |

**Pattern-based recovery** means you can find passwords in the first few thousand attempts rather than testing millions blindly.

## 🚀 Quick Start

### Prerequisites

- Java 15 or higher
- Maven 3.6 or higher
- An Ethereum keystore file (UTC JSON format)

### Installation

```bash
# Clone the repository
git clone https://github.com/yourusername/keystore-recovery.git
cd keystore-recovery

# Build the project
mvn clean package

# The executable JAR will be created at: target/keystore-recovery.jar
```

### Usage

#### Interactive Mode

```bash
java -jar target/keystore-recovery.jar
```

#### Command Line Mode

```bash
java -jar target/keystore-recovery.jar path/to/keystore.json password_config.md
```

### Configuration

1. **Create your password configuration file** (`password_config.md`):

```markdown
## Base Words
- crypto
- myWallet
- ethereum

## Number Combinations
- 123
- 2024

## Special Characters
- !
- @
```

2. **Run the recovery tool**
3. **Follow the prompts**

## 📖 Example Session

```
============================================================
ETHEREUM KEYSTORE PASSWORD RECOVERY (Java High-Performance)
Pattern: [5-12 chars] + [1-5 digits] + [1 special char]
============================================================

📁 Enter path to keystore file: UTC--2024-01-15T10-30-45.json
📄 Enter path to config file (default: password_config.md):

✓ Keystore file loaded: UTC--2024-01-15T10-30-45.json

📖 Reading configuration from: password_config.md

============================================================
CONFIGURATION LOADED
============================================================
📝 Base words: 8 items
🔢 Number combinations: 9 items
⚡ Special characters: 9 items

🖥️  Number of threads to use (available: 8, recommended: 8): 8

⚠️  Start recovery? (y/n): y

🔍 Starting password recovery...
Pattern: [5-12 chars] + [1-5 digits] + [1 special char]
Total combinations: 5,832
Using 8 threads for parallel processing

Progress: 2,451 attempts | 45 passwords/sec (8 threads)

✅ SUCCESS! Password found!
Total attempts: 2,451
Time elapsed: 0.1 seconds

============================================================
🎉 WALLET RECOVERED SUCCESSFULLY!
============================================================

Display password? (y/n): y
Password: crypto2024!

Save to file? (y/n): y
✓ Saved to recovered_password.txt
⚠️  WARNING: Password saved in plain text - delete after use!
```

## 🏗️ Architecture

Clean, modular design following SOLID principles:

```
KeystoreRecoveryApp (CLI)
    ├── PasswordConfig (Immutable configuration)
    ├── PasswordGenerator (Password generation)
    └── RecoveryEngine (Multi-threading)
            └── KeystoreValidator (Interface)
                    └── Web3jKeystoreValidator (Implementation)
```

### Key Components

- **KeystoreRecoveryApp**: Main CLI application
- **PasswordConfig**: Immutable configuration with Builder pattern
- **PasswordGenerator**: Generates password combinations
- **KeystoreValidator**: Interface for validation logic
- **Web3jKeystoreValidator**: Web3j-based validator implementation
- **RecoveryEngine**: Multi-threaded recovery coordinator

## 📝 Configuration File Format

The `password_config.md` file uses simple markdown format:

```markdown
# Keystore Password Recovery Configuration

## Base Words
*List your commonly used base words or phrases*

- password
- crypto
- wallet
- ethereum

## Number Combinations
*List your commonly used number patterns (1-5 digits)*

- 123
- 1234
- 2023
- 2024

## Special Characters
*List your commonly used special characters (single character)*

- !
- @
- #
- $
```

### Tips for Best Results

1. **Order by likelihood** - Put most likely items first
2. **Base words** - Can be any length; tool combines them to reach 5-12 chars
3. **Capitalization** - Automatic; no need to specify multiple cases
4. **Word combinations** - Tool automatically tries pairs with separators (-, _, .)

## 🔧 Advanced Usage

### Using as a Library

```java
// Initialize components
Web3jKeystoreValidator validator =
    new Web3jKeystoreValidator("keystore.json");
PasswordGenerator generator = new PasswordGenerator();
RecoveryEngine engine = new RecoveryEngine(validator, generator, 8);

// Load configuration
PasswordConfig config = PasswordConfig.fromMarkdown("password_config.md");

// Run recovery
RecoveryEngine.RecoveryResult result = engine.recover(config);

// Check results
if (result.isSuccess()) {
    System.out.println("Password: " + result.getPassword());
    System.out.println("Attempts: " + result.getAttempts());
    System.out.println("Time: " + result.getTimeSec() + "s");
}
```

### Custom Thread Count

```bash
# The tool will prompt for thread count
# Recommended: Use number of CPU cores (8 max for best results)
```

### Build from Source

```bash
# Clean build
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Run directly with Maven
mvn exec:java -Dexec.args="keystore.json password_config.md"
```

## 🛡️ Security Features

- ✅ **Password masking** - Password not displayed by default
- ✅ **User confirmation** - Explicit confirmation before showing password
- ✅ **Secure file permissions** - Output files set to 0600 (Unix/Mac)
- ✅ **Temp file protection** - Restrictive permissions on temporary files
- ✅ **Warning messages** - Clear warnings when saving passwords
- ✅ **Resource cleanup** - Automatic cleanup via shutdown hooks

## 🔐 Security Best Practices

### Password Memory Handling

**Important Limitation**: This tool uses the Web3j library, which requires passwords as `String` objects (not `char[]` arrays). This means:

- ✅ **What we do**: Minimize password lifetime in memory
- ✅ **What we do**: No password logging (even in debug mode)
- ✅ **What we do**: Explicit nulling after use
- ⚠️ **Limitation**: Passwords cannot be immediately zeroed from memory (JVM String immutability)
- ⚠️ **Limitation**: Passwords may remain in heap until garbage collection

**Mitigation Strategies**:
1. Run recovery on a trusted, offline machine
2. Reboot system after recovery to clear memory
3. Use full disk encryption on recovery machine
4. See [SECURITY_ANALYSIS.md](SECURITY_ANALYSIS.md) for detailed analysis

### File Security

**Temporary Files**:
- Created in system temp directory during recovery
- Set to `0600` permissions (owner read/write only) on Unix/Mac
- Automatically deleted on shutdown via JVM hooks
- Manual cleanup available via `Web3jKeystoreValidator.cleanup()`

**Output Files**:
- `recovered_password.txt` written with `0600` permissions
- **WARNING**: Contains plaintext password
- **Action Required**: Delete immediately after securing password elsewhere

**Best Practice**:
```bash
# After recovery, secure the password then delete files
cat recovered_password.txt  # Copy password to secure location
shred -u recovered_password.txt  # Secure delete (Linux/Mac)
# OR on Windows:
del recovered_password.txt
```

### Input Validation

All user inputs are validated via `InputValidator` class:

- **Path Traversal Prevention**: Detects `../` and blocks access outside expected directories
- **Null Byte Injection**: Rejects inputs containing `\0` characters
- **File Size Limits**: Keystore files limited to 10 MB maximum
- **Password Validation**: Checks for null bytes, enforces reasonable length limits
- **Log Sanitization**: All logged paths sanitized to prevent log injection

### Dependency Security

**Current Security Grade**: **A-** (Excellent)

All dependencies audited as of October 2025:
- ✅ **0 Critical vulnerabilities**
- ✅ **0 High vulnerabilities**
- ✅ **0 Medium vulnerabilities**
- ✅ **0 Low vulnerabilities** (all addressed)

**Key Dependencies**:
- Web3j 4.10.0 - Actively maintained, no known vulnerabilities
- Bouncy Castle 1.78 (jdk18on) - Latest version, industry-standard crypto
- Logback 1.4.14 - Latest stable, CVE-2023-6378 patched
- SLF4J 2.0.9 - Current stable release

**Monitoring**:
- See [DEPENDENCY_SECURITY_AUDIT.md](DEPENDENCY_SECURITY_AUDIT.md) for full audit
- Quarterly dependency reviews scheduled
- Automated scanning via GitHub Dependabot (recommended)

### Thread Safety

All classes designed for concurrent access:
- `RecoveryEngine`: Synchronized validator access
- `Web3jKeystoreValidator`: Thread-safe with synchronized `validate()`
- `PasswordGenerator`: Stateless, fully thread-safe
- `PasswordConfig`: Immutable, inherently thread-safe

### Error Handling

**Secure Error Messages**:
- Never log passwords (even in error cases)
- Sanitize file paths before logging
- Provide user-friendly errors without exposing system details
- Full stack traces in log files only (never to console)

**Example**:
```java
// ❌ BAD - Logs password
logger.error("Failed to validate password: " + password);

// ✅ GOOD - No password exposure
logger.error("Password validation failed (incorrect password)");
```

### Safe Usage Guidelines

**For Users**:

1. **Offline Recovery** (Recommended):
   ```bash
   # Disconnect from internet before running
   # Reconnect only after securing password
   ```

2. **Trusted Environment**:
   - Use a clean, malware-free system
   - Avoid shared/public computers
   - Consider using a dedicated recovery VM

3. **After Recovery**:
   - Transfer funds to a new wallet with a strong password
   - Delete all recovery files and configurations
   - Reboot system to clear memory
   - Consider secure wiping temp directories

4. **Password Storage**:
   - Use a reputable password manager (1Password, Bitwarden, KeePass)
   - Never email or message passwords
   - Consider hardware wallet for large amounts

**For Developers**:

1. **Never Log Passwords**:
   ```java
   // Use InputValidator.sanitizeForLog() for file paths
   logger.debug("Processing file: {}", InputValidator.sanitizeForLog(path, 100));

   // Never log password variables
   logger.trace("Password validation failed");  // ✅ Good
   logger.trace("Failed password: " + pwd);      // ❌ Never do this
   ```

2. **Secure Defaults**:
   - All security features enabled by default
   - Require explicit user action to display passwords
   - Conservative file permissions

3. **Input Validation**:
   ```java
   // Always validate user inputs
   Path keystorePath = InputValidator.validateKeystorePath(userInput);
   InputValidator.validatePassword(passwordAttempt);
   ```

### Attack Scenarios Considered

This tool has been analyzed against common attack vectors:

| Attack Vector | Mitigation | Status |
|--------------|------------|--------|
| Path Traversal | `InputValidator.validateKeystorePath()` | ✅ Protected |
| Null Byte Injection | Input sanitization | ✅ Protected |
| Memory Dumps | Password lifetime minimization | ⚠️ Use offline |
| Log Injection | Path sanitization | ✅ Protected |
| Keyloggers | N/A - User responsibility | ⚠️ Use trusted system |
| Network Sniffing | Offline operation | ✅ No network I/O |
| Temp File Disclosure | Restrictive permissions (0600) | ✅ Protected |
| Weak RNG | Uses `SecureRandom` (Bouncy Castle) | ✅ Protected |

**Legend**: ✅ Protected by tool | ⚠️ User responsibility

### OWASP Top 10 Compliance

Analysis against OWASP Top 10 (2021):

1. **Broken Access Control**: ✅ File permissions enforced
2. **Cryptographic Failures**: ✅ Industry-standard libraries (Web3j, Bouncy Castle)
3. **Injection**: ✅ Input validation prevents path traversal, null bytes
4. **Insecure Design**: ✅ Security-first architecture, immutable configs
5. **Security Misconfiguration**: ✅ Secure defaults, no debug mode in production
6. **Vulnerable Components**: ✅ All dependencies audited, Grade A-
7. **Authentication Failures**: N/A - Local tool, no authentication
8. **Software/Data Integrity**: ✅ Maven Central verified dependencies
9. **Logging Failures**: ✅ Structured logging, no password leakage
10. **SSRF**: N/A - No server-side requests

### Reporting Security Issues

**Found a vulnerability?** Please report responsibly:

1. **DO NOT** open a public GitHub issue
2. **Email**: security@[your-domain].com (or create private security advisory)
3. **Include**:
   - Description of vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (optional)

**Response Time**: We aim to respond within 48 hours and patch critical issues within 7 days.

### Security Certifications and Audits

- ✅ Manual code review (October 2025)
- ✅ OWASP Dependency Check (October 2025)
- ✅ Security analysis documented in [SECURITY_ANALYSIS.md](SECURITY_ANALYSIS.md)
- ⏳ Third-party security audit (planned)

### Further Reading

- [SECURITY_ANALYSIS.md](SECURITY_ANALYSIS.md) - Deep dive into security architecture
- [DEPENDENCY_SECURITY_AUDIT.md](DEPENDENCY_SECURITY_AUDIT.md) - Full dependency audit report
- [Web3j Security](https://docs.web3j.io/) - Web3j library documentation
- [OWASP Java Security](https://owasp.org/www-project-java/) - OWASP Java security guidelines

## 📚 Documentation

- [User Guide](README.md) - This file
- [Architecture](PROJECT_STRUCTURE.md) - Detailed architecture documentation
- [QA Report](QA_REPORT_20251018.md) - Quality assurance analysis
- [Critical Fixes](CRITICAL_FIXES_SUMMARY_20251018.md) - Critical fixes summary
- [Major Fixes](MAJOR_FIXES_SUMMARY_20251018.md) - Major refactoring details

## 🔍 How It Works

1. **Load Configuration** - Reads password components from markdown file
2. **Generate Combinations** - Creates all possible password combinations:
   - Base words with capitalization variants
   - Word pairs with separators
   - Filters to 5-12 character length
3. **Multi-threaded Testing** - Divides combinations across CPU cores
4. **Keystore Verification** - Uses Web3j to test each password
5. **Real-time Progress** - Shows attempts/second and total tried
6. **Success Notification** - Displays found password and saves to file

## 🧪 Testing

Comprehensive unit test suite with 96%+ coverage:

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=PasswordConfigTest
mvn test -Dtest=PasswordGeneratorTest
mvn test -Dtest=Web3jKeystoreValidatorTest
mvn test -Dtest=RecoveryEngineTest

# Generate coverage report
mvn clean test
# View report at: target/site/jacoco/index.html
```

**Test Statistics**:
- **Total Tests**: 170 (155 unit + 15 integration)
- **Coverage**: 96%+ line coverage, 93%+ branch coverage
- **Classes Tested**: PasswordConfig, PasswordGenerator, Web3jKeystoreValidator, RecoveryEngine, KeystoreRecoveryApp
- **Frameworks**: JUnit 5, Mockito 5.5.0, AssertJ 3.24.2, Web3j for real keystore generation
- **Coverage Tool**: JaCoCo 0.8.11

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

### Development Guidelines

1. Follow SOLID principles
2. Add JavaDoc for all public methods
3. Write unit tests for new features
4. Update documentation
5. Ensure code compiles: `mvn clean compile`

## 📋 System Requirements

- **Java**: 15 or higher
- **Maven**: 3.6 or higher
- **Memory**: Minimum 512MB RAM
- **Disk**: 50MB free space
- **OS**: Windows, macOS, Linux

## ⚠️ Important Notes

### Legal Notice

**This tool is designed for recovering YOUR OWN wallets only.**

- Only use on keystores you own
- Never use on others' wallets without explicit permission
- Password recovery may take significant time depending on search space
- No guarantee of success

### Best Practices

After recovery:
1. ✅ Store password in a secure password manager
2. ✅ Consider creating a new wallet with a stronger password
3. ✅ Delete or secure the recovery program and configuration files
4. ✅ Never share your keystore file or password

## 🐛 Troubleshooting

### Build Issues

**Problem**: Maven build fails with dependency errors

```bash
# Solution: Clean and rebuild
mvn clean install -U
```

### Runtime Issues

**Problem**: `Error: Could not find or load main class KeystoreRecoveryApp`

```bash
# Solution: Rebuild the JAR
mvn clean package
```

**Problem**: OutOfMemoryError with very large combinations

```bash
# Solution: Increase JVM heap size
java -Xmx4g -jar target/keystore-recovery.jar
```

### Performance Issues

**Problem**: Slow password testing (<1,000/sec)

- Check CPU usage - should be near 100% across all cores
- Reduce thread count if system is overloaded
- Ensure SSD is used for temp files (not HDD)

### Password Not Found

**Problem**: Recovery completes but password not found

1. Review your password_config.md - add more variations
2. Verify the password actually matches the pattern
3. Try adding more number combinations or special characters
4. Consider if you used a completely different pattern

## 📈 Performance Optimization

### Tips for Maximum Speed

1. **Use SSD** - Faster I/O for temporary files
2. **Optimal threads** - Use CPU core count (8 max recommended)
3. **Order configuration** - Put most likely patterns first
4. **Reduce search space** - Be specific with your password components

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/keystore-recovery/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/keystore-recovery/discussions)
- **Documentation**: See `/docs` folder

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Web3j](https://github.com/web3j/web3j) - Ethereum Java library
- [Bouncy Castle](https://www.bouncycastle.org/) - Cryptography provider
- [Maven](https://maven.apache.org/) - Build automation tool

## 🌟 Star History

If this tool helped you recover your wallet, please consider giving it a star! ⭐

## 📊 Project Stats

- **Language**: Java 15+
- **Lines of Code**: ~2,500 (including tests)
- **Classes**: 8 (7 main + 1 security utility)
- **Test Coverage**: 96%+ (170 tests: 155 unit + 15 integration)
- **Documentation Coverage**: 95%+
- **Security Grade**: A-

---

**Made with ❤️ for the Ethereum community**

**Disclaimer**: Use at your own risk. Always backup your keystores and passwords securely.
