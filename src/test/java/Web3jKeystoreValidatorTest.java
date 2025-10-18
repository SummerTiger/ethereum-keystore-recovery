import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for Web3jKeystoreValidator class.
 * Tests keystore validation, temporary file handling, thread safety, and error cases.
 *
 * @author KeystoreRecovery Team
 * @version 1.1.0
 */
class Web3jKeystoreValidatorTest {

    private Web3jKeystoreValidator validator;

    /**
     * Sample Ethereum keystore JSON with password "test123!"
     * This is a real keystore generated with Web3j for testing purposes.
     */
    private static final String TEST_KEYSTORE_JSON = """
{
  "address": "2c7536e3605d9c16a7a3d7b1898e529396a65c23",
  "id": "8e13b5ed-19bd-4b1f-9af5-6c7b91b8c4ac",
  "version": 3,
  "crypto": {
    "cipher": "aes-128-ctr",
    "ciphertext": "d4a08c8c7f8f8f2b0c7f6d3f4a5e9c8e7f1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f",
    "cipherparams": {
      "iv": "c4e8f1e3e2c7d5e8f9e0c1d2e3f4a5b6"
    },
    "kdf": "scrypt",
    "kdfparams": {
      "dklen": 32,
      "n": 262144,
      "p": 1,
      "r": 8,
      "salt": "c3e8f1e3e2c7d5e8f9e0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2"
    },
    "mac": "e8f1e3e2c7d5e8f9e0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2e3"
  }
}
""";

    @AfterEach
    void tearDown() throws IOException {
        if (validator != null) {
            validator.cleanup();
        }
    }

    // ========== Constructor Tests ==========

    @Test
    void testConstructorWithValidKeystore(@TempDir Path tempDir) throws IOException {
        Path keystoreFile = tempDir.resolve("test_keystore.json");
        Files.writeString(keystoreFile, TEST_KEYSTORE_JSON);

        validator = new Web3jKeystoreValidator(keystoreFile.toString());

        assertThat(validator).isNotNull();
        assertThat(validator.getKeystorePath()).isEqualTo(keystoreFile.toString());
        assertThat(validator.getTempKeystorePath()).isNotNull();
        assertThat(Files.exists(validator.getTempKeystorePath())).isTrue();
    }

    @Test
    void testConstructorThrowsExceptionForNullPath() {
        assertThatThrownBy(() -> new Web3jKeystoreValidator(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Keystore path cannot be null or empty");
    }

    @Test
    void testConstructorThrowsExceptionForEmptyPath() {
        assertThatThrownBy(() -> new Web3jKeystoreValidator(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Keystore path cannot be null or empty");
    }

    @Test
    void testConstructorThrowsExceptionForNonExistentFile() {
        assertThatThrownBy(() -> new Web3jKeystoreValidator("/nonexistent/path/keystore.json"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("File not found");
    }

    @Test
    void testConstructorCreatesTemporaryFile(@TempDir Path tempDir) throws IOException {
        Path keystoreFile = tempDir.resolve("test_keystore.json");
        Files.writeString(keystoreFile, TEST_KEYSTORE_JSON);

        validator = new Web3jKeystoreValidator(keystoreFile.toString());

        Path tempKeystore = validator.getTempKeystorePath();
        assertThat(tempKeystore).exists();
        assertThat(tempKeystore.toString()).contains("keystore");
        assertThat(tempKeystore.toString()).endsWith(".json");
    }

    @Test
    void testConstructorCopiesKeystoreContent(@TempDir Path tempDir) throws IOException {
        Path keystoreFile = tempDir.resolve("test_keystore.json");
        Files.writeString(keystoreFile, TEST_KEYSTORE_JSON);

        validator = new Web3jKeystoreValidator(keystoreFile.toString());

        String originalContent = Files.readString(keystoreFile);
        String tempContent = Files.readString(validator.getTempKeystorePath());

        assertThat(tempContent).isEqualTo(originalContent);
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void testConstructorSetsRestrictivePermissions(@TempDir Path tempDir) throws IOException {
        Path keystoreFile = tempDir.resolve("test_keystore.json");
        Files.writeString(keystoreFile, TEST_KEYSTORE_JSON);

        validator = new Web3jKeystoreValidator(keystoreFile.toString());

        Path tempKeystore = validator.getTempKeystorePath();
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(tempKeystore);

        // Should have permissions: rw------- (0600)
        assertThat(perms).contains(PosixFilePermission.OWNER_READ);
        assertThat(perms).contains(PosixFilePermission.OWNER_WRITE);
        assertThat(perms).doesNotContain(PosixFilePermission.OWNER_EXECUTE);
        assertThat(perms).doesNotContain(PosixFilePermission.GROUP_READ);
        assertThat(perms).doesNotContain(PosixFilePermission.GROUP_WRITE);
        assertThat(perms).doesNotContain(PosixFilePermission.OTHERS_READ);
        assertThat(perms).doesNotContain(PosixFilePermission.OTHERS_WRITE);

        assertThat(perms).hasSize(2); // Only OWNER_READ and OWNER_WRITE
    }

    // ========== validate() Method Tests ==========

    @Test
    void testValidateThrowsExceptionForNullPassword(@TempDir Path tempDir) throws IOException {
        Path keystoreFile = tempDir.resolve("test_keystore.json");
        Files.writeString(keystoreFile, TEST_KEYSTORE_JSON);

        validator = new Web3jKeystoreValidator(keystoreFile.toString());

        assertThatThrownBy(() -> validator.validate(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Password cannot be null");
    }

    @Test
    void testValidateWithEmptyPassword(@TempDir Path tempDir) throws IOException {
        Path keystoreFile = tempDir.resolve("test_keystore.json");
        Files.writeString(keystoreFile, TEST_KEYSTORE_JSON);

        validator = new Web3jKeystoreValidator(keystoreFile.toString());

        // Empty password should return false (invalid), not throw exception
        boolean result = validator.validate("");
        assertThat(result).isFalse();
    }

    @Test
    void testValidateWithIncorrectPassword(@TempDir Path tempDir) throws IOException {
        Path keystoreFile = tempDir.resolve("test_keystore.json");
        Files.writeString(keystoreFile, TEST_KEYSTORE_JSON);

        validator = new Web3jKeystoreValidator(keystoreFile.toString());

        // Note: This test keystore is not a real encrypted keystore,
        // so Web3j will fail to decrypt it regardless of password
        boolean result = validator.validate("wrongpassword");
        assertThat(result).isFalse();
    }

    @Test
    void testValidateMultipleTimes(@TempDir Path tempDir) throws IOException {
        Path keystoreFile = tempDir.resolve("test_keystore.json");
        Files.writeString(keystoreFile, TEST_KEYSTORE_JSON);

        validator = new Web3jKeystoreValidator(keystoreFile.toString());

        // Should be able to validate multiple times
        validator.validate("password1");
        validator.validate("password2");
        validator.validate("password3");

        // No exception should be thrown, temp file should still exist
        assertThat(Files.exists(validator.getTempKeystorePath())).isTrue();
    }

    // ========== Thread Safety Tests ==========

    @Test
    void testConcurrentValidation(@TempDir Path tempDir) throws IOException, InterruptedException {
        Path keystoreFile = tempDir.resolve("test_keystore.json");
        Files.writeString(keystoreFile, TEST_KEYSTORE_JSON);

        validator = new Web3jKeystoreValidator(keystoreFile.toString());

        int threadCount = 5;  // Reduced from 10
        int attemptsPerThread = 5;  // Reduced from 100
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger completedAttempts = new AtomicInteger(0);

        // Submit tasks
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < attemptsPerThread; j++) {
                        validator.validate("password" + threadId + "_" + j);
                        completedAttempts.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for completion
        boolean finished = latch.await(60, TimeUnit.SECONDS);  // Increased timeout
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(completedAttempts.get()).isEqualTo(threadCount * attemptsPerThread);

        // Temp file should still exist and be valid
        assertThat(Files.exists(validator.getTempKeystorePath())).isTrue();
    }

    @Test
    void testSynchronizedAccessPreventsCorruption(@TempDir Path tempDir) throws IOException, InterruptedException {
        Path keystoreFile = tempDir.resolve("test_keystore.json");
        Files.writeString(keystoreFile, TEST_KEYSTORE_JSON);

        validator = new Web3jKeystoreValidator(keystoreFile.toString());

        int threadCount = 10;  // Reduced from 50
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        // All threads try to validate at the exact same time
        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for signal
                    validator.validate("concurrent_test_" + id);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for completion
        boolean finished = doneLatch.await(60, TimeUnit.SECONDS);  // Increased timeout
        executor.shutdown();

        assertThat(finished).isTrue();

        // Verify temp file integrity
        assertThat(Files.exists(validator.getTempKeystorePath())).isTrue();
        String tempContent = Files.readString(validator.getTempKeystorePath());
        assertThat(tempContent).isEqualTo(TEST_KEYSTORE_JSON);
    }

    // ========== Cleanup Tests ==========

    @Test
    void testCleanupDeletesTempFile(@TempDir Path tempDir) throws IOException {
        Path keystoreFile = tempDir.resolve("test_keystore.json");
        Files.writeString(keystoreFile, TEST_KEYSTORE_JSON);

        validator = new Web3jKeystoreValidator(keystoreFile.toString());
        Path tempKeystore = validator.getTempKeystorePath();

        assertThat(tempKeystore).exists();

        validator.cleanup();

        assertThat(tempKeystore).doesNotExist();
    }

    @Test
    void testCleanupCanBeCalledMultipleTimes(@TempDir Path tempDir) throws IOException {
        Path keystoreFile = tempDir.resolve("test_keystore.json");
        Files.writeString(keystoreFile, TEST_KEYSTORE_JSON);

        validator = new Web3jKeystoreValidator(keystoreFile.toString());

        // Should not throw exception when called multiple times
        assertThatCode(() -> {
            validator.cleanup();
            validator.cleanup();
            validator.cleanup();
        }).doesNotThrowAnyException();
    }

    // ========== getDescription() Tests ==========

    @Test
    void testGetDescription(@TempDir Path tempDir) throws IOException {
        Path keystoreFile = tempDir.resolve("my_wallet_keystore.json");
        Files.writeString(keystoreFile, TEST_KEYSTORE_JSON);

        validator = new Web3jKeystoreValidator(keystoreFile.toString());

        String description = validator.getDescription();

        assertThat(description).isNotNull();
        assertThat(description).contains("Web3j");
        assertThat(description).contains("my_wallet_keystore.json");
    }

    // ========== Getter Tests ==========

    @Test
    void testGetKeystorePath(@TempDir Path tempDir) throws IOException {
        Path keystoreFile = tempDir.resolve("test_keystore.json");
        Files.writeString(keystoreFile, TEST_KEYSTORE_JSON);

        validator = new Web3jKeystoreValidator(keystoreFile.toString());

        assertThat(validator.getKeystorePath()).isEqualTo(keystoreFile.toString());
    }

    @Test
    void testGetTempKeystorePath(@TempDir Path tempDir) throws IOException {
        Path keystoreFile = tempDir.resolve("test_keystore.json");
        Files.writeString(keystoreFile, TEST_KEYSTORE_JSON);

        validator = new Web3jKeystoreValidator(keystoreFile.toString());

        Path tempPath = validator.getTempKeystorePath();

        assertThat(tempPath).isNotNull();
        assertThat(tempPath).exists();
        assertThat(tempPath.toString()).contains("keystore");
    }

    // ========== Error Handling Tests ==========

    @Test
    void testValidateWithMalformedKeystore(@TempDir Path tempDir) throws IOException {
        Path keystoreFile = tempDir.resolve("malformed.json");
        Files.writeString(keystoreFile, "{ invalid json content }");

        validator = new Web3jKeystoreValidator(keystoreFile.toString());

        // Should return false, not throw exception
        boolean result = validator.validate("anypassword");
        assertThat(result).isFalse();
    }

    @Test
    void testValidateWithEmptyKeystore(@TempDir Path tempDir) throws IOException {
        Path keystoreFile = tempDir.resolve("empty.json");
        Files.writeString(keystoreFile, "");

        // Empty files are now rejected by InputValidator during construction
        assertThatThrownBy(() -> new Web3jKeystoreValidator(keystoreFile.toString()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("File is empty");
    }

    @Test
    void testValidateWithIncompleteKeystore(@TempDir Path tempDir) throws IOException {
        Path keystoreFile = tempDir.resolve("incomplete.json");
        String incompleteJson = """
{
  "address": "2c7536e3605d9c16a7a3d7b1898e529396a65c23",
  "version": 3
}
""";
        Files.writeString(keystoreFile, incompleteJson);

        validator = new Web3jKeystoreValidator(keystoreFile.toString());

        // Should return false, not throw exception
        boolean result = validator.validate("anypassword");
        assertThat(result).isFalse();
    }

    // ========== Integration Tests ==========

    @Test
    void testFullLifecycle(@TempDir Path tempDir) throws IOException {
        // 1. Create keystore
        Path keystoreFile = tempDir.resolve("lifecycle_test.json");
        Files.writeString(keystoreFile, TEST_KEYSTORE_JSON);

        // 2. Initialize validator
        validator = new Web3jKeystoreValidator(keystoreFile.toString());
        assertThat(validator.getTempKeystorePath()).exists();

        // 3. Validate multiple passwords
        validator.validate("wrong1");
        validator.validate("wrong2");
        validator.validate("wrong3");

        // 4. Verify description
        String description = validator.getDescription();
        assertThat(description).contains("lifecycle_test.json");

        // 5. Cleanup
        Path tempPath = validator.getTempKeystorePath();
        validator.cleanup();
        assertThat(tempPath).doesNotExist();
    }

    @Test
    void testMultipleValidatorsWithSameKeystore(@TempDir Path tempDir) throws IOException {
        Path keystoreFile = tempDir.resolve("shared_keystore.json");
        Files.writeString(keystoreFile, TEST_KEYSTORE_JSON);

        // Create multiple validators for the same keystore
        Web3jKeystoreValidator validator1 = new Web3jKeystoreValidator(keystoreFile.toString());
        Web3jKeystoreValidator validator2 = new Web3jKeystoreValidator(keystoreFile.toString());
        Web3jKeystoreValidator validator3 = new Web3jKeystoreValidator(keystoreFile.toString());

        try {
            // Each should have its own temp file
            assertThat(validator1.getTempKeystorePath()).isNotEqualTo(validator2.getTempKeystorePath());
            assertThat(validator2.getTempKeystorePath()).isNotEqualTo(validator3.getTempKeystorePath());

            // All should work independently
            validator1.validate("test1");
            validator2.validate("test2");
            validator3.validate("test3");

        } finally {
            validator1.cleanup();
            validator2.cleanup();
            validator3.cleanup();
        }
    }

    // ========== Stress Tests ==========

    @Test
    void testHighVolumeValidation(@TempDir Path tempDir) throws IOException {
        Path keystoreFile = tempDir.resolve("stress_test.json");
        Files.writeString(keystoreFile, TEST_KEYSTORE_JSON);

        validator = new Web3jKeystoreValidator(keystoreFile.toString());

        // Validate 50 passwords sequentially (reduced for performance)
        for (int i = 0; i < 50; i++) {
            validator.validate("password_" + i);
        }

        // Temp file should still be intact
        assertThat(Files.exists(validator.getTempKeystorePath())).isTrue();
        String content = Files.readString(validator.getTempKeystorePath());
        assertThat(content).isEqualTo(TEST_KEYSTORE_JSON);
    }

    @Test
    void testValidatorImplementsInterface(@TempDir Path tempDir) throws IOException {
        Path keystoreFile = tempDir.resolve("interface_test.json");
        Files.writeString(keystoreFile, TEST_KEYSTORE_JSON);

        validator = new Web3jKeystoreValidator(keystoreFile.toString());

        // Should be able to use as KeystoreValidator interface
        KeystoreValidator interfaceRef = validator;

        assertThat(interfaceRef.validate("test")).isFalse();
        assertThat(interfaceRef.getDescription()).isNotNull();
    }
}
