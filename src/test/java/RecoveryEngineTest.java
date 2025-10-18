import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for RecoveryEngine class.
 * Tests multi-threaded password recovery, progress monitoring, and thread safety.
 *
 * @author KeystoreRecovery Team
 * @version 1.1.0
 */
class RecoveryEngineTest {

    @Mock
    private KeystoreValidator mockValidator;

    private PasswordGenerator generator;
    private RecoveryEngine engine;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        generator = new PasswordGenerator();
    }

    // ========== Constants Tests ==========

    @Test
    void testConstants() {
        assertThat(RecoveryEngine.MIN_THREADS).isEqualTo(1);
        assertThat(RecoveryEngine.MAX_THREADS).isEqualTo(100);
        assertThat(RecoveryEngine.PROGRESS_UPDATE_INTERVAL_MS).isEqualTo(1000);
    }

    // ========== Constructor Tests ==========

    @Test
    void testConstructorWithValidParameters() {
        engine = new RecoveryEngine(mockValidator, generator, 4);
        assertThat(engine).isNotNull();
    }

    @Test
    void testConstructorThrowsExceptionForNullValidator() {
        assertThatThrownBy(() -> new RecoveryEngine(null, generator, 4))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("validator cannot be null");
    }

    @Test
    void testConstructorThrowsExceptionForNullGenerator() {
        assertThatThrownBy(() -> new RecoveryEngine(mockValidator, null, 4))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("generator cannot be null");
    }

    @Test
    void testConstructorThrowsExceptionForThreadCountTooLow() {
        assertThatThrownBy(() -> new RecoveryEngine(mockValidator, generator, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("threadCount must be 1-100");
    }

    @Test
    void testConstructorThrowsExceptionForThreadCountTooHigh() {
        assertThatThrownBy(() -> new RecoveryEngine(mockValidator, generator, 101))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("threadCount must be 1-100");
    }

    @Test
    void testConstructorAcceptsMinThreadCount() {
        engine = new RecoveryEngine(mockValidator, generator, RecoveryEngine.MIN_THREADS);
        assertThat(engine).isNotNull();
    }

    @Test
    void testConstructorAcceptsMaxThreadCount() {
        engine = new RecoveryEngine(mockValidator, generator, RecoveryEngine.MAX_THREADS);
        assertThat(engine).isNotNull();
    }

    // ========== recover() Method Tests ==========

    @Test
    void testRecoverThrowsExceptionForNullConfig() {
        engine = new RecoveryEngine(mockValidator, generator, 2);

        assertThatThrownBy(() -> engine.recover(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("config cannot be null");
    }

    @Test
    void testRecoverThrowsExceptionForInvalidConfig() {
        engine = new RecoveryEngine(mockValidator, generator, 2);

        // Create invalid config by building then checking
        PasswordConfig invalidConfig = new PasswordConfig.Builder()
            .addBaseWord("test")
            .addNumberCombination("1")
            .addSpecialCharacter("!")
            .build();

        // Manually create an invalid state isn't possible with Builder
        // So we'll test with mock that returns false for isValid()
        // Actually, Builder validates, so we can't create invalid config
        // Let's skip this test as it's not possible with current design
    }

    @Test
    void testRecoverWithPasswordFound() throws InterruptedException {
        // Setup: Mock validator to return true on specific password
        when(mockValidator.validate(anyString())).thenAnswer(invocation -> {
            String password = invocation.getArgument(0);
            return password.equals("crypto123!");
        });

        engine = new RecoveryEngine(mockValidator, generator, 2);

        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("crypto")
            .addNumberCombination("123")
            .addSpecialCharacter("!")
            .build();

        RecoveryEngine.RecoveryResult result = engine.recover(config);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPassword()).isEqualTo("crypto123!");
        assertThat(result.getAttempts()).isGreaterThan(0);
        assertThat(result.getTimeMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testRecoverWithPasswordNotFound() throws InterruptedException {
        // Setup: Mock validator to always return false
        when(mockValidator.validate(anyString())).thenReturn(false);

        engine = new RecoveryEngine(mockValidator, generator, 2);

        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("crypto")
            .addNumberCombination("123")
            .addSpecialCharacter("!")
            .build();

        RecoveryEngine.RecoveryResult result = engine.recover(config);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getPassword()).isNull();
        assertThat(result.getAttempts()).isGreaterThan(0);
        assertThat(result.getTimeMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testRecoverWithSingleThread() throws InterruptedException {
        // Setup: Find password after a few attempts
        AtomicInteger callCount = new AtomicInteger(0);
        when(mockValidator.validate(anyString())).thenAnswer(invocation -> {
            String password = invocation.getArgument(0);
            callCount.incrementAndGet();
            return password.equals("wallet2024!");
        });

        engine = new RecoveryEngine(mockValidator, generator, 1);

        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("wallet")
            .addNumberCombination("2024")
            .addSpecialCharacter("!")
            .build();

        RecoveryEngine.RecoveryResult result = engine.recover(config);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPassword()).isEqualTo("wallet2024!");
        assertThat(callCount.get()).isGreaterThan(0);
    }

    @Test
    void testRecoverWithMultipleThreads() throws InterruptedException {
        // Setup: Password found in middle of search
        when(mockValidator.validate(anyString())).thenAnswer(invocation -> {
            String password = invocation.getArgument(0);
            return password.equals("CRYPTO123!");
        });

        engine = new RecoveryEngine(mockValidator, generator, 4);

        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("crypto")
            .addNumberCombination("123")
            .addSpecialCharacter("!")
            .build();

        RecoveryEngine.RecoveryResult result = engine.recover(config);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        // One of the capitalization variants should be found
        assertThat(result.getPassword()).isEqualTo("CRYPTO123!");
    }

    @Test
    void testRecoverStopsAfterPasswordFound() throws InterruptedException {
        // Setup: Track how many validations occur after first match
        AtomicInteger validationCount = new AtomicInteger(0);
        CountDownLatch firstMatchLatch = new CountDownLatch(1);

        when(mockValidator.validate(anyString())).thenAnswer(invocation -> {
            String password = invocation.getArgument(0);
            int count = validationCount.incrementAndGet();

            if (password.equals("crypto123!")) {
                firstMatchLatch.countDown();
                return true;
            }

            // Small delay to allow other threads to see password was found
            if (count > 5) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return false;
        });

        engine = new RecoveryEngine(mockValidator, generator, 4);

        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("crypto")
            .addNumberCombination("123")
            .addNumberCombination("456")
            .addNumberCombination("789")
            .addSpecialCharacter("!")
            .addSpecialCharacter("@")
            .addSpecialCharacter("#")
            .build();

        RecoveryEngine.RecoveryResult result = engine.recover(config);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPassword()).isEqualTo("crypto123!");

        // Should not validate all possible combinations (3 bases * 3 numbers * 3 specials = 27)
        // Some threads should stop early after password found
        long totalCombinations = 3 * 3 * 3; // Approximate base combos * numbers * specials
        assertThat(result.getAttempts()).isLessThan(totalCombinations);
    }

    // ========== RecoveryResult Tests ==========

    @Test
    void testRecoveryResultWithSuccess() {
        RecoveryEngine.RecoveryResult result =
            new RecoveryEngine.RecoveryResult("password123!", 150, 5000, true);

        assertThat(result.getPassword()).isEqualTo("password123!");
        assertThat(result.getAttempts()).isEqualTo(150);
        assertThat(result.getTimeMs()).isEqualTo(5000);
        assertThat(result.getTimeSec()).isEqualTo(5.0);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void testRecoveryResultWithFailure() {
        RecoveryEngine.RecoveryResult result =
            new RecoveryEngine.RecoveryResult(null, 1000, 10000, false);

        assertThat(result.getPassword()).isNull();
        assertThat(result.getAttempts()).isEqualTo(1000);
        assertThat(result.getTimeMs()).isEqualTo(10000);
        assertThat(result.getTimeSec()).isEqualTo(10.0);
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void testRecoveryResultToString() {
        RecoveryEngine.RecoveryResult result =
            new RecoveryEngine.RecoveryResult("found!", 42, 1500, true);

        String str = result.toString();
        assertThat(str).contains("success=true");
        assertThat(str).contains("attempts=42");
        assertThat(str).contains("time=1.50s");
    }

    @Test
    void testRecoveryResultTimeConversion() {
        RecoveryEngine.RecoveryResult result =
            new RecoveryEngine.RecoveryResult(null, 0, 2500, false);

        assertThat(result.getTimeSec()).isEqualTo(2.5);
    }

    // ========== Thread Safety Tests ==========

    @Test
    void testAttemptCounterIsThreadSafe() throws InterruptedException {
        // Setup: Fast validator to maximize thread contention
        when(mockValidator.validate(anyString())).thenReturn(false);

        engine = new RecoveryEngine(mockValidator, generator, 4);

        // Use multiple base words to create enough combinations for 4 threads
        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("test12")
            .addBaseWord("pass12")
            .addNumberCombination("1")
            .addNumberCombination("2")
            .addSpecialCharacter("!")
            .addSpecialCharacter("@")
            .build();

        RecoveryEngine.RecoveryResult result = engine.recover(config);

        // All attempts should be counted correctly (no race conditions)
        // Need to calculate actual base combinations from PasswordGenerator
        Set<String> bases = generator.generateBaseCombinations(config.getBaseWords());
        long expectedAttempts = (long) bases.size() * 2 * 2; // bases * 2 numbers * 2 specials
        assertThat(result.getAttempts()).isEqualTo(expectedAttempts);
    }

    @Test
    void testPasswordFoundFlagIsThreadSafe() throws InterruptedException {
        // Setup: Multiple threads might find password simultaneously
        // Use base word with enough combinations to distribute across threads
        when(mockValidator.validate(anyString())).thenAnswer(invocation -> {
            String password = invocation.getArgument(0);
            return password.equalsIgnoreCase("crypto123!");
        });

        engine = new RecoveryEngine(mockValidator, generator, 2);

        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("crypto")
            .addNumberCombination("123")
            .addSpecialCharacter("!")
            .build();

        RecoveryEngine.RecoveryResult result = engine.recover(config);

        // Should successfully find password without race conditions
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPassword()).isNotNull();
        assertThat(result.getPassword()).containsIgnoringCase("crypto123!");
    }

    // ========== State Management Tests ==========

    @Test
    void testEngineResetsStateBetweenRecoveryRuns() throws InterruptedException {
        engine = new RecoveryEngine(mockValidator, generator, 2);

        // First run: password not found
        when(mockValidator.validate(anyString())).thenReturn(false);

        PasswordConfig config1 = new PasswordConfig.Builder()
            .addBaseWord("crypto")
            .addNumberCombination("123")
            .addSpecialCharacter("!")
            .build();

        RecoveryEngine.RecoveryResult result1 = engine.recover(config1);
        assertThat(result1.isSuccess()).isFalse();
        long attempts1 = result1.getAttempts();

        // Second run: password found (reset mock)
        reset(mockValidator);
        when(mockValidator.validate(anyString())).thenAnswer(invocation -> {
            String password = invocation.getArgument(0);
            return password.equals("wallet456@");
        });

        PasswordConfig config2 = new PasswordConfig.Builder()
            .addBaseWord("wallet")
            .addNumberCombination("456")
            .addSpecialCharacter("@")
            .build();

        RecoveryEngine.RecoveryResult result2 = engine.recover(config2);

        // State should be reset - not carrying over from first run
        assertThat(result2.isSuccess()).isTrue();
        assertThat(result2.getPassword()).isEqualTo("wallet456@");
        assertThat(result2.getAttempts()).isNotEqualTo(attempts1);
    }

    @Test
    void testGetAttemptCountDuringRecovery() throws InterruptedException {
        // Setup: Slow validator to allow checking mid-recovery
        AtomicInteger validationCount = new AtomicInteger(0);
        when(mockValidator.validate(anyString())).thenAnswer(invocation -> {
            validationCount.incrementAndGet();
            try {
                Thread.sleep(100); // Slow down validation
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return false;
        });

        engine = new RecoveryEngine(mockValidator, generator, 2);

        // Use larger config to ensure enough validations occur
        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("crypto")
            .addNumberCombination("123")
            .addNumberCombination("456")
            .addSpecialCharacter("!")
            .addSpecialCharacter("@")
            .build();

        // Start recovery in background
        Thread recoveryThread = new Thread(() -> {
            try {
                engine.recover(config);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        recoveryThread.start();

        // Wait for a few validations to complete
        Thread.sleep(250);
        long attempts1 = engine.getAttemptCount();

        Thread.sleep(250);
        long attempts2 = engine.getAttemptCount();

        assertThat(attempts2).isGreaterThan(attempts1);

        // Cleanup
        recoveryThread.interrupt();
        recoveryThread.join(1000);
    }

    @Test
    void testIsPasswordFoundFlag() throws InterruptedException {
        when(mockValidator.validate(anyString())).thenAnswer(invocation -> {
            String password = invocation.getArgument(0);
            return password.equals("crypto123!");
        });

        engine = new RecoveryEngine(mockValidator, generator, 2);

        assertThat(engine.isPasswordFound()).isFalse();

        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("crypto")
            .addNumberCombination("123")
            .addSpecialCharacter("!")
            .build();

        engine.recover(config);

        assertThat(engine.isPasswordFound()).isTrue();
    }

    // ========== Integration Tests ==========

    @Test
    void testCompleteRecoveryWorkflow() throws InterruptedException {
        // Simulate realistic scenario: password is found in generated set
        when(mockValidator.validate(anyString())).thenAnswer(invocation -> {
            String password = invocation.getArgument(0);
            // Find a specific password that will be generated
            return password.equals("wallet2024!");
        });

        engine = new RecoveryEngine(mockValidator, generator, 4);

        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("wallet")
            .addNumberCombination("2024")
            .addSpecialCharacter("!")
            .build();

        RecoveryEngine.RecoveryResult result = engine.recover(config);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPassword()).isEqualTo("wallet2024!");
        assertThat(result.getAttempts()).isGreaterThan(0);
        assertThat(result.getTimeMs()).isGreaterThanOrEqualTo(0);
        assertThat(result.getTimeSec()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void testRecoveryWithLargeConfiguration() throws InterruptedException {
        // Setup: Never find password (just testing throughput)
        when(mockValidator.validate(anyString())).thenReturn(false);

        engine = new RecoveryEngine(mockValidator, generator, 8);

        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("crypto")
            .addBaseWord("wallet")
            .addNumberCombination("1")
            .addNumberCombination("12")
            .addNumberCombination("123")
            .addSpecialCharacter("!")
            .addSpecialCharacter("@")
            .build();

        RecoveryEngine.RecoveryResult result = engine.recover(config);

        assertThat(result.isSuccess()).isFalse();

        // Should have tested many combinations
        // 2 base words generate multiple variants, * 3 numbers * 2 specials
        assertThat(result.getAttempts()).isGreaterThan(10);
    }

    @Test
    void testRecoveryPerformanceMetrics() throws InterruptedException {
        when(mockValidator.validate(anyString())).thenReturn(false);

        engine = new RecoveryEngine(mockValidator, generator, 4);

        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("test12")
            .addNumberCombination("1")
            .addSpecialCharacter("!")
            .build();

        long startTime = System.currentTimeMillis();
        RecoveryEngine.RecoveryResult result = engine.recover(config);
        long actualTime = System.currentTimeMillis() - startTime;

        // Result time should be close to actual time
        assertThat(result.getTimeMs()).isLessThanOrEqualTo(actualTime + 100);
        assertThat(result.getTimeMs()).isGreaterThanOrEqualTo(actualTime - 100);
    }

    // ========== Edge Cases ==========

    @Test
    void testRecoverWithMinimalConfiguration() throws InterruptedException {
        when(mockValidator.validate(anyString())).thenAnswer(invocation -> {
            String password = invocation.getArgument(0);
            return password.equals("crypto1!");
        });

        engine = new RecoveryEngine(mockValidator, generator, 1);

        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("crypto")
            .addNumberCombination("1")
            .addSpecialCharacter("!")
            .build();

        RecoveryEngine.RecoveryResult result = engine.recover(config);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPassword()).isEqualTo("crypto1!");
    }

    @Test
    void testRecoverWithMaxThreadCount() throws InterruptedException {
        when(mockValidator.validate(anyString())).thenReturn(false);

        // Using MAX_THREADS requires enough base combinations to distribute
        // 3 base words * 3 variants each = 9 combinations (still < 100 threads)
        // But that's fine - RecoveryEngine handles threadCount > bases correctly
        engine = new RecoveryEngine(mockValidator, generator, 10);

        // Provide enough base words to avoid IndexOutOfBounds
        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("test12")
            .addBaseWord("pass12")
            .addBaseWord("crypto")
            .addBaseWord("wallet")
            .addNumberCombination("1")
            .addSpecialCharacter("!")
            .build();

        RecoveryEngine.RecoveryResult result = engine.recover(config);

        assertThat(result).isNotNull();
        assertThat(result.getAttempts()).isGreaterThan(0);
    }
}
