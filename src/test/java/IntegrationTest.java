import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.web3j.crypto.WalletUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the Ethereum Keystore Password Recovery Tool.
 *
 * <p>These tests validate end-to-end workflows including:
 * <ul>
 *   <li>Full password recovery scenarios</li>
 *   <li>Multi-threaded performance</li>
 *   <li>Large configuration file handling</li>
 *   <li>Error recovery and edge cases</li>
 *   <li>Resource cleanup verification</li>
 * </ul>
 *
 * <p>Note: These tests use real keystores with known passwords for validation.
 *
 * @author KeystoreRecovery Team
 * @version 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Integration Tests - End-to-End Scenarios")
public class IntegrationTest {

    @TempDir
    Path tempDir;

    private Path testKeystore;
    private Path testConfig;
    private static final String TEST_PASSWORD = "password123!"; // Must match pattern: [5-12 chars] + digits + special

    @BeforeEach
    public void setUp() throws Exception {
        // Generate a real Ethereum keystore with known password using Web3j
        String fileName = WalletUtils.generateLightNewWalletFile(TEST_PASSWORD, tempDir.toFile());
        testKeystore = tempDir.resolve(fileName);

        // Create configuration file that will find the password
        testConfig = tempDir.resolve("test-config.md");
        String configContent = """
            ## Base Words
            - password

            ## Number Combinations
            - 123

            ## Special Characters
            - !
            """;
        Files.writeString(testConfig, configContent);
    }

    // ==========================================
    // Scenario 1: Successful Password Recovery
    // ==========================================

    @Test
    @Order(1)
    @DisplayName("Integration: Full recovery workflow - password found")
    public void testSuccessfulPasswordRecovery() throws Exception {
        // Load configuration
        PasswordConfig config = PasswordConfig.fromMarkdown(testConfig.toString());
        assertThat(config).isNotNull();
        assertThat(config.isValid()).isTrue();

        // Initialize validator
        Web3jKeystoreValidator validator = new Web3jKeystoreValidator(testKeystore.toString());
        assertThat(validator).isNotNull();

        // Initialize generator and engine
        PasswordGenerator generator = new PasswordGenerator();
        RecoveryEngine engine = new RecoveryEngine(validator, generator, 4);

        // Run recovery
        RecoveryEngine.RecoveryResult result = engine.recover(config);

        // Verify success
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPassword()).isEqualTo(TEST_PASSWORD);
        assertThat(result.getAttempts()).isGreaterThan(0);
        assertThat(result.getTimeMs()).isGreaterThan(0);

        // Cleanup
        validator.cleanup();
    }

    // ==========================================
    // Scenario 2: Password Not Found
    // ==========================================

    @Test
    @Order(2)
    @DisplayName("Integration: Full recovery workflow - password not found")
    public void testPasswordNotFound() throws Exception {
        // Create config that won't find the password
        Path wrongConfig = tempDir.resolve("wrong-config.md");
        String wrongConfigContent = """
            ## Base Words
            - wrong

            ## Number Combinations
            - 999

            ## Special Characters
            - ?
            """;
        Files.writeString(wrongConfig, wrongConfigContent);

        PasswordConfig config = PasswordConfig.fromMarkdown(wrongConfig.toString());
        Web3jKeystoreValidator validator = new Web3jKeystoreValidator(testKeystore.toString());
        PasswordGenerator generator = new PasswordGenerator();
        RecoveryEngine engine = new RecoveryEngine(validator, generator, 2);

        RecoveryEngine.RecoveryResult result = engine.recover(config);

        // Verify failure
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getPassword()).isNull();
        assertThat(result.getAttempts()).isGreaterThan(0);

        validator.cleanup();
    }

    // ==========================================
    // Scenario 3: Multi-threaded Recovery
    // ==========================================

    @Test
    @Order(3)
    @DisplayName("Integration: Multi-threaded recovery (1, 2, 4, 8 threads)")
    public void testMultiThreadedRecovery() throws Exception {
        int[] threadCounts = {1, 2, 4, 8};

        for (int threads : threadCounts) {
            PasswordConfig config = PasswordConfig.fromMarkdown(testConfig.toString());
            Web3jKeystoreValidator validator = new Web3jKeystoreValidator(testKeystore.toString());
            PasswordGenerator generator = new PasswordGenerator();
            RecoveryEngine engine = new RecoveryEngine(validator, generator, threads);

            RecoveryEngine.RecoveryResult result = engine.recover(config);

            assertThat(result.isSuccess())
                .as("Recovery should succeed with %d threads", threads)
                .isTrue();
            assertThat(result.getPassword()).isEqualTo(TEST_PASSWORD);

            validator.cleanup();
        }
    }

    // ==========================================
    // Scenario 4: Large Configuration File
    // ==========================================

    @Test
    @Order(4)
    @DisplayName("Integration: Large configuration file (100+ combinations)")
    @Timeout(value = 120, unit = TimeUnit.SECONDS)  // Increased to 2 minutes for scrypt performance
    public void testLargeConfigurationFile() throws Exception {
        Path largeConfig = tempDir.resolve("large-config.md");

        // Create config with many combinations but includes the correct password
        StringBuilder configBuilder = new StringBuilder();
        configBuilder.append("## Base Words\n");
        for (int i = 0; i < 20; i++) {  // 20 wrong words
            configBuilder.append("- wrongword").append(i).append("\n");
        }
        configBuilder.append("- password\n"); // Include the correct base word

        configBuilder.append("\n## Number Combinations\n");
        for (int i = 0; i < 5; i++) {  // 5 wrong numbers
            configBuilder.append("- ").append(i * 111).append("\n");
        }
        configBuilder.append("- 123\n"); // Include the correct number

        configBuilder.append("\n## Special Characters\n");
        configBuilder.append("- @\n- #\n- $\n- !\n"); // Include the correct special char

        Files.writeString(largeConfig, configBuilder.toString());

        PasswordConfig config = PasswordConfig.fromMarkdown(largeConfig.toString());
        assertThat(config.getBaseWords().size()).isGreaterThan(10);

        Web3jKeystoreValidator validator = new Web3jKeystoreValidator(testKeystore.toString());
        PasswordGenerator generator = new PasswordGenerator();
        RecoveryEngine engine = new RecoveryEngine(validator, generator, 8);

        long startTime = System.currentTimeMillis();
        RecoveryEngine.RecoveryResult result = engine.recover(config);
        long duration = System.currentTimeMillis() - startTime;

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPassword()).isEqualTo(TEST_PASSWORD);
        assertThat(duration).isLessThan(120000); // Should complete within 2 minutes (scrypt is slow)

        validator.cleanup();
    }

    // ==========================================
    // Scenario 5: Performance Verification
    // ==========================================

    @Test
    @Order(5)
    @DisplayName("Integration: Performance verification (passwords/sec)")
    public void testPerformanceMetrics() throws Exception {
        // Create a config with enough combinations to measure performance
        Path perfConfig = tempDir.resolve("perf-config.md");
        String perfConfigContent = """
            ## Base Words
            - pass
            - word
            - key
            - test

            ## Number Combinations
            - 1
            - 12
            - 123
            - 1234
            - 12345

            ## Special Characters
            - !
            - @
            - #
            - $
            - %
            """;
        Files.writeString(perfConfig, perfConfigContent);

        PasswordConfig config = PasswordConfig.fromMarkdown(perfConfig.toString());
        Web3jKeystoreValidator validator = new Web3jKeystoreValidator(testKeystore.toString());
        PasswordGenerator generator = new PasswordGenerator();
        RecoveryEngine engine = new RecoveryEngine(validator, generator, 8);

        RecoveryEngine.RecoveryResult result = engine.recover(config);

        assertThat(result).isNotNull();
        long attempts = result.getAttempts();
        double timeSec = result.getTimeSec();
        long passwordsPerSec = timeSec > 0 ? (long) (attempts / timeSec) : 0;

        // Verify reasonable performance
        // Note: Scrypt KDF with n=262144 is intentionally slow for security (~100-200ms per attempt)
        // Realistic performance: 5-10 passwords/sec per thread with scrypt
        assertThat(passwordsPerSec)
            .as("Performance should be at least 1 password/sec (scrypt is intentionally slow)")
            .isGreaterThan(0);

        System.out.printf("Performance: %d passwords/sec (%d attempts in %.3f seconds)%n",
            passwordsPerSec, attempts, timeSec);

        validator.cleanup();
    }

    // ==========================================
    // Scenario 6: Resource Cleanup Verification
    // ==========================================

    @Test
    @Order(6)
    @DisplayName("Integration: Resource cleanup verification")
    public void testResourceCleanup() throws Exception {
        PasswordConfig config = PasswordConfig.fromMarkdown(testConfig.toString());
        Web3jKeystoreValidator validator = new Web3jKeystoreValidator(testKeystore.toString());

        // Get temp file path before cleanup
        Path tempFile = validator.getTempKeystorePath();
        assertThat(Files.exists(tempFile)).isTrue();

        // Run recovery
        PasswordGenerator generator = new PasswordGenerator();
        RecoveryEngine engine = new RecoveryEngine(validator, generator, 2);
        engine.recover(config);

        // Cleanup
        validator.cleanup();

        // Verify temp file is deleted
        assertThat(Files.exists(tempFile)).isFalse();
    }

    // ==========================================
    // Scenario 7: Interrupted Recovery
    // ==========================================

    @Test
    @Order(7)
    @DisplayName("Integration: Interrupted recovery (graceful shutdown)")
    public void testInterruptedRecovery() throws Exception {
        // Create a large config that will take time
        Path slowConfig = tempDir.resolve("slow-config.md");
        StringBuilder slowConfigBuilder = new StringBuilder();
        slowConfigBuilder.append("## Base Words\n");
        for (int i = 0; i < 20; i++) {
            slowConfigBuilder.append("- word").append(i).append("\n");
        }

        slowConfigBuilder.append("\n## Number Combinations\n");
        for (int i = 0; i < 20; i++) {
            slowConfigBuilder.append("- ").append(i).append("\n");
        }

        slowConfigBuilder.append("\n## Special Characters\n");
        slowConfigBuilder.append("- !\n- @\n- #\n");

        Files.writeString(slowConfig, slowConfigBuilder.toString());

        PasswordConfig config = PasswordConfig.fromMarkdown(slowConfig.toString());
        Web3jKeystoreValidator validator = new Web3jKeystoreValidator(testKeystore.toString());
        PasswordGenerator generator = new PasswordGenerator();
        RecoveryEngine engine = new RecoveryEngine(validator, generator, 2);

        // Run in separate thread and interrupt it
        Thread recoveryThread = new Thread(() -> {
            try {
                engine.recover(config);
            } catch (InterruptedException e) {
                // Expected
                Thread.currentThread().interrupt();
            }
        });

        recoveryThread.start();
        Thread.sleep(100); // Let it start
        recoveryThread.interrupt();
        recoveryThread.join(5000); // Wait up to 5 seconds

        assertThat(recoveryThread.isAlive()).isFalse();

        validator.cleanup();
    }

    // ==========================================
    // Scenario 8: Configuration Validation
    // ==========================================

    @Test
    @Order(8)
    @DisplayName("Integration: Configuration file validation")
    public void testConfigurationValidation() throws Exception {
        // Valid config
        PasswordConfig validConfig = PasswordConfig.fromMarkdown(testConfig.toString());
        assertThat(validConfig.isValid()).isTrue();

        // Empty config (should fail)
        Path emptyConfig = tempDir.resolve("empty-config.md");
        Files.writeString(emptyConfig, "");

        assertThatThrownBy(() -> PasswordConfig.fromMarkdown(emptyConfig.toString()))
            .isInstanceOf(IllegalStateException.class);

        // Partial config (missing special characters)
        Path partialConfig = tempDir.resolve("partial-config.md");
        String partialContent = """
            ## Base Words
            - test

            ## Number Combinations
            - 123
            """;
        Files.writeString(partialConfig, partialContent);

        assertThatThrownBy(() -> PasswordConfig.fromMarkdown(partialConfig.toString()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cannot be empty");
    }

    // ==========================================
    // Scenario 9: Multiple Validators (Parallel Use)
    // ==========================================

    @Test
    @Order(9)
    @DisplayName("Integration: Multiple validators running in parallel")
    public void testMultipleValidatorsParallel() throws Exception {
        PasswordConfig config = PasswordConfig.fromMarkdown(testConfig.toString());

        // Create 3 validators in parallel
        Web3jKeystoreValidator validator1 = new Web3jKeystoreValidator(testKeystore.toString());
        Web3jKeystoreValidator validator2 = new Web3jKeystoreValidator(testKeystore.toString());
        Web3jKeystoreValidator validator3 = new Web3jKeystoreValidator(testKeystore.toString());

        PasswordGenerator generator = new PasswordGenerator();
        RecoveryEngine engine1 = new RecoveryEngine(validator1, generator, 2);
        RecoveryEngine engine2 = new RecoveryEngine(validator2, generator, 2);
        RecoveryEngine engine3 = new RecoveryEngine(validator3, generator, 2);

        // Run all in parallel
        Thread t1 = new Thread(() -> {
            try {
                RecoveryEngine.RecoveryResult result = engine1.recover(config);
                assertThat(result.isSuccess()).isTrue();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                RecoveryEngine.RecoveryResult result = engine2.recover(config);
                assertThat(result.isSuccess()).isTrue();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread t3 = new Thread(() -> {
            try {
                RecoveryEngine.RecoveryResult result = engine3.recover(config);
                assertThat(result.isSuccess()).isTrue();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        t1.start();
        t2.start();
        t3.start();

        t1.join(10000);
        t2.join(10000);
        t3.join(10000);

        assertThat(t1.isAlive()).isFalse();
        assertThat(t2.isAlive()).isFalse();
        assertThat(t3.isAlive()).isFalse();

        validator1.cleanup();
        validator2.cleanup();
        validator3.cleanup();
    }

    // ==========================================
    // Scenario 10: Sample Config Creation
    // ==========================================

    @Test
    @Order(10)
    @DisplayName("Integration: Sample configuration file creation")
    public void testSampleConfigCreation() throws Exception {
        Path sampleConfig = tempDir.resolve("sample.md");

        // Create sample
        PasswordConfig.createSampleConfig(sampleConfig.toString());

        // Verify it exists and is valid
        assertThat(Files.exists(sampleConfig)).isTrue();
        String content = Files.readString(sampleConfig);
        assertThat(content).contains("## Base Words");
        assertThat(content).contains("## Number Combinations");
        assertThat(content).contains("## Special Characters");

        // Verify it can be loaded
        PasswordConfig config = PasswordConfig.fromMarkdown(sampleConfig.toString());
        assertThat(config.isValid()).isTrue();
    }

    // ==========================================
    // Scenario 11: Thread Count Boundaries
    // ==========================================

    @Test
    @Order(11)
    @DisplayName("Integration: Thread count boundary testing")
    public void testThreadCountBoundaries() throws Exception {
        PasswordConfig config = PasswordConfig.fromMarkdown(testConfig.toString());

        // Test minimum threads (1)
        Web3jKeystoreValidator validator1 = new Web3jKeystoreValidator(testKeystore.toString());
        PasswordGenerator generator = new PasswordGenerator();
        RecoveryEngine engine1 = new RecoveryEngine(validator1, generator, RecoveryEngine.MIN_THREADS);
        RecoveryEngine.RecoveryResult result1 = engine1.recover(config);
        assertThat(result1.isSuccess()).isTrue();
        validator1.cleanup();

        // Test maximum threads (100)
        Web3jKeystoreValidator validator2 = new Web3jKeystoreValidator(testKeystore.toString());
        RecoveryEngine engine2 = new RecoveryEngine(validator2, generator, RecoveryEngine.MAX_THREADS);
        RecoveryEngine.RecoveryResult result2 = engine2.recover(config);
        assertThat(result2.isSuccess()).isTrue();
        validator2.cleanup();

        // Test invalid thread count (should throw)
        Web3jKeystoreValidator validator3 = new Web3jKeystoreValidator(testKeystore.toString());
        assertThatThrownBy(() -> new RecoveryEngine(validator3, generator, 0))
            .isInstanceOf(IllegalArgumentException.class);
        validator3.cleanup();

        assertThatThrownBy(() -> new RecoveryEngine(validator3, generator, 101))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ==========================================
    // Scenario 12: Password Found Early
    // ==========================================

    @Test
    @Order(12)
    @DisplayName("Integration: Password found in first few attempts")
    public void testPasswordFoundEarly() throws Exception {
        // Config where password is likely first
        Path earlyConfig = tempDir.resolve("early-config.md");
        String earlyConfigContent = """
            ## Base Words
            - password

            ## Number Combinations
            - 123

            ## Special Characters
            - !
            """;
        Files.writeString(earlyConfig, earlyConfigContent);

        PasswordConfig config = PasswordConfig.fromMarkdown(earlyConfig.toString());
        Web3jKeystoreValidator validator = new Web3jKeystoreValidator(testKeystore.toString());
        PasswordGenerator generator = new PasswordGenerator();
        RecoveryEngine engine = new RecoveryEngine(validator, generator, 1);

        RecoveryEngine.RecoveryResult result = engine.recover(config);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAttempts()).isLessThan(10); // Should find quickly

        validator.cleanup();
    }

    // ==========================================
    // Scenario 13: Memory and Resource Usage
    // ==========================================

    @Test
    @Order(13)
    @DisplayName("Integration: Memory and resource usage monitoring")
    public void testMemoryAndResourceUsage() throws Exception {
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        PasswordConfig config = PasswordConfig.fromMarkdown(testConfig.toString());
        Web3jKeystoreValidator validator = new Web3jKeystoreValidator(testKeystore.toString());
        PasswordGenerator generator = new PasswordGenerator();
        RecoveryEngine engine = new RecoveryEngine(validator, generator, 4);

        RecoveryEngine.RecoveryResult result = engine.recover(config);
        assertThat(result.isSuccess()).isTrue();

        validator.cleanup();
        System.gc(); // Suggest garbage collection

        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = (memoryAfter - memoryBefore) / (1024 * 1024); // Convert to MB

        System.out.printf("Memory used: %d MB%n", memoryUsed);

        // Verify reasonable memory usage (less than 100 MB for this test)
        assertThat(memoryUsed).isLessThan(100);
    }

    // ==========================================
    // Scenario 14: File Permissions
    // ==========================================

    @Test
    @Order(14)
    @DisplayName("Integration: File permissions verification")
    public void testFilePermissions() throws Exception {
        Web3jKeystoreValidator validator = new Web3jKeystoreValidator(testKeystore.toString());
        Path tempFile = validator.getTempKeystorePath();

        assertThat(Files.exists(tempFile)).isTrue();

        // Check permissions on Unix/Mac
        try {
            var perms = Files.getPosixFilePermissions(tempFile);
            assertThat(perms).containsExactlyInAnyOrder(
                java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
            );
        } catch (UnsupportedOperationException e) {
            // Windows - test passes
            System.out.println("POSIX permissions not supported on this platform");
        }

        validator.cleanup();
    }

    // ==========================================
    // Scenario 15: End-to-End Full Workflow
    // ==========================================

    @Test
    @Order(15)
    @DisplayName("Integration: Complete end-to-end workflow")
    public void testCompleteEndToEndWorkflow() throws Exception {
        // 1. Load configuration
        PasswordConfig config = PasswordConfig.fromMarkdown(testConfig.toString());
        assertThat(config.isValid()).isTrue();
        System.out.println("✓ Configuration loaded");

        // 2. Initialize validator
        Web3jKeystoreValidator validator = new Web3jKeystoreValidator(testKeystore.toString());
        System.out.println("✓ Validator initialized");

        // 3. Create generator
        PasswordGenerator generator = new PasswordGenerator();
        var baseCombinations = generator.generateBaseCombinations(config.getBaseWords());
        assertThat(baseCombinations).isNotEmpty();
        System.out.println("✓ Generator created");

        // 4. Create engine
        RecoveryEngine engine = new RecoveryEngine(validator, generator, 8);
        System.out.println("✓ Recovery engine created");

        // 5. Run recovery
        RecoveryEngine.RecoveryResult result = engine.recover(config);
        System.out.printf("✓ Recovery completed: %s%n", result.isSuccess() ? "SUCCESS" : "FAILED");

        // 6. Verify result
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPassword()).isEqualTo(TEST_PASSWORD);
        System.out.printf("✓ Password found: %s%n", result.getPassword());

        // 7. Check metrics
        System.out.printf("  - Attempts: %d%n", result.getAttempts());
        System.out.printf("  - Time: %.3f seconds%n", result.getTimeSec());
        assertThat(result.getAttempts()).isGreaterThan(0);
        assertThat(result.getTimeMs()).isGreaterThan(0);

        // 8. Cleanup
        validator.cleanup();
        System.out.println("✓ Resources cleaned up");
    }
}
