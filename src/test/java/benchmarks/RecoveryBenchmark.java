package benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.web3j.crypto.WalletUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

// Import main classes from default package
import PasswordConfig;
import PasswordGenerator;
import Web3jKeystoreValidator;
import RecoveryEngine;

/**
 * JMH Performance Benchmarks for Ethereum Keystore Password Recovery Tool.
 *
 * <p>This class measures:
 * <ul>
 *   <li>Password generation throughput</li>
 *   <li>Keystore validation performance</li>
 *   <li>Multi-threading scaling</li>
 *   <li>Memory allocation rates</li>
 * </ul>
 *
 * <p>Run benchmarks with:
 * <pre>
 * mvn clean test-compile exec:java -Dexec.classpathScope=test \
 *     -Dexec.mainClass=benchmarks.RecoveryBenchmark
 * </pre>
 *
 * @author KeystoreRecovery Team
 * @version 1.0.0
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 2)
@Measurement(iterations = 3, time = 3)
@Fork(1)
public class RecoveryBenchmark {

    private PasswordConfig config;
    private PasswordGenerator generator;
    private Path testKeystore;
    private Web3jKeystoreValidator validator;
    private static final String TEST_PASSWORD = "password123!";

    /**
     * Set up benchmark state.
     */
    @Setup(Level.Trial)
    public void setup() throws Exception {
        // Create temp directory
        Path tempDir = Files.createTempDirectory("benchmark");

        // Generate test keystore with known password
        String fileName = WalletUtils.generateLightNewWalletFile(TEST_PASSWORD, tempDir.toFile());
        testKeystore = tempDir.resolve(fileName);

        // Create test configuration
        Path configPath = tempDir.resolve("config.md");
        String configContent = """
            ## Base Words
            - password
            - secret
            - wallet

            ## Number Combinations
            - 123
            - 456
            - 789

            ## Special Characters
            - !
            - @
            - #
            """;
        Files.writeString(configPath, configContent);

        // Initialize components
        config = PasswordConfig.fromMarkdown(configPath.toString());
        generator = new PasswordGenerator();
        validator = new Web3jKeystoreValidator(testKeystore.toString());
    }

    /**
     * Tear down benchmark state.
     */
    @TearDown(Level.Trial)
    public void tearDown() throws IOException {
        if (validator != null) {
            validator.cleanup();
        }
        if (testKeystore != null && Files.exists(testKeystore)) {
            Files.deleteIfExists(testKeystore);
        }
    }

    /**
     * Benchmark: Password generation throughput.
     */
    @Benchmark
    public Set<String> benchmarkPasswordGeneration() {
        return generator.generateAll(config);
    }

    /**
     * Benchmark: Base combination generation.
     */
    @Benchmark
    public Set<String> benchmarkBaseCombinations() {
        return generator.generateBaseCombinations(config.getBaseWords());
    }

    /**
     * Benchmark: Single keystore validation (scrypt operation).
     * Note: This is expected to be slow (~100-200ms per validation with scrypt n=262144).
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public boolean benchmarkKeystoreValidation() {
        return validator.validatePassword(TEST_PASSWORD);
    }

    /**
     * Benchmark: Multi-threaded recovery with 1 thread.
     */
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public RecoveryEngine.RecoveryResult benchmarkRecoverySingleThread() {
        RecoveryEngine engine = new RecoveryEngine(validator, generator, 1);
        return engine.recover(config);
    }

    /**
     * Benchmark: Multi-threaded recovery with 4 threads.
     */
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public RecoveryEngine.RecoveryResult benchmarkRecoveryFourThreads() {
        RecoveryEngine engine = new RecoveryEngine(validator, generator, 4);
        return engine.recover(config);
    }

    /**
     * Benchmark: Multi-threaded recovery with 8 threads.
     */
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public RecoveryEngine.RecoveryResult benchmarkRecoveryEightThreads() {
        RecoveryEngine engine = new RecoveryEngine(validator, generator, 8);
        return engine.recover(config);
    }

    /**
     * Main method to run benchmarks.
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(RecoveryBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}
