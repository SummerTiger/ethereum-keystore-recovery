import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Multi-threaded password recovery engine.
 *
 * <p>This class coordinates the password recovery process using multiple threads
 * to test passwords in parallel. It provides real-time progress monitoring and
 * graceful shutdown capabilities.
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe and designed for
 * concurrent execution.
 *
 * @author KeystoreRecovery Team
 * @version 1.1.0
 */
public class RecoveryEngine {

    private static final Logger logger = LoggerFactory.getLogger(RecoveryEngine.class);

    /** Minimum allowed thread count */
    public static final int MIN_THREADS = 1;

    /** Maximum allowed thread count */
    public static final int MAX_THREADS = 100;

    /** Progress update interval in milliseconds */
    public static final long PROGRESS_UPDATE_INTERVAL_MS = 1000;

    private final KeystoreValidator validator;
    private final PasswordGenerator generator;
    private final int threadCount;

    private final AtomicLong attemptCounter = new AtomicLong(0);
    private final AtomicBoolean passwordFound = new AtomicBoolean(false);
    private volatile String foundPassword = null;

    /**
     * Creates a new recovery engine.
     *
     * @param validator the keystore validator to use
     * @param generator the password generator to use
     * @param threadCount number of threads for parallel processing (1-100)
     * @throws IllegalArgumentException if parameters are invalid
     */
    public RecoveryEngine(KeystoreValidator validator,
                          PasswordGenerator generator,
                          int threadCount) {
        if (validator == null) {
            throw new IllegalArgumentException("validator cannot be null");
        }
        if (generator == null) {
            throw new IllegalArgumentException("generator cannot be null");
        }
        InputValidator.validateThreadCount(threadCount, MIN_THREADS, MAX_THREADS);

        this.validator = validator;
        this.generator = generator;
        this.threadCount = threadCount;
    }

    /**
     * Attempts to recover the password using the given configuration.
     *
     * <p>This method:
     * <ol>
     *   <li>Generates all password combinations</li>
     *   <li>Distributes work across threads</li>
     *   <li>Monitors progress in real-time</li>
     *   <li>Returns immediately when password is found</li>
     * </ol>
     *
     * @param config the password configuration
     * @return the recovered password, or null if not found
     * @throws IllegalArgumentException if config is invalid
     * @throws InterruptedException if recovery is interrupted
     */
    public RecoveryResult recover(PasswordConfig config) throws InterruptedException {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        if (!config.isValid()) {
            throw new IllegalArgumentException("config must be valid");
        }

        // Reset state
        attemptCounter.set(0);
        passwordFound.set(false);
        foundPassword = null;

        // Generate password combinations
        Set<String> baseCombinations = generator.generateBaseCombinations(config.getBaseWords());
        long totalCombinations = (long) baseCombinations.size() *
                                config.getNumberCombinations().size() *
                                config.getSpecialCharacters().size();

        logger.info("Starting password recovery");
        logger.info("Pattern: [5-12 chars] + [1-5 digits] + [1 special char]");
        logger.info("Total combinations: {}", String.format("%,d", totalCombinations));
        logger.info("Using {} threads for parallel processing", threadCount);

        System.out.println("\n🔍 Starting password recovery...");
        System.out.println("Pattern: [5-12 chars] + [1-5 digits] + [1 special char]");
        System.out.println("Total combinations: " + String.format("%,d", totalCombinations));
        System.out.println("Using " + threadCount + " threads for parallel processing");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<String>> futures = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        // Start progress monitoring
        Thread progressThread = startProgressMonitoring(startTime);

        try {
            // Create password generation tasks
            List<String> baseList = new ArrayList<>(baseCombinations);
            int chunkSize = Math.max(1, baseList.size() / threadCount);

            for (int i = 0; i < threadCount; i++) {
                int start = i * chunkSize;
                int end = (i == threadCount - 1) ? baseList.size() : (i + 1) * chunkSize;
                List<String> chunk = baseList.subList(start, end);

                futures.add(executor.submit(() -> processChunk(chunk, config)));
            }

            // Wait for completion
            for (Future<String> future : futures) {
                try {
                    String result = future.get();
                    if (result != null) {
                        foundPassword = result;
                        passwordFound.set(true);
                        break;
                    }
                } catch (ExecutionException e) {
                    logger.error("Task execution error", e);
                    System.err.println("⚠️  Task execution error: " + e.getMessage());
                }
            }

        } finally {
            // Cleanup
            executor.shutdownNow();
            progressThread.interrupt();
        }

        long totalTime = System.currentTimeMillis() - startTime;

        // Display results
        if (foundPassword != null) {
            logger.info("Password recovery successful - attempts: {}, time: {}ms",
                       attemptCounter.get(), totalTime);
            System.out.println("\n\n✅ SUCCESS! Password found!");
            System.out.println("Total attempts: " + String.format("%,d", attemptCounter.get()));
            System.out.println("Time elapsed: " + (totalTime / 1000.0) + " seconds");
        } else {
            logger.info("Password not found - attempts: {}, time: {}ms",
                       attemptCounter.get(), totalTime);
            System.out.println("\n\n❌ Password not found after " +
                             String.format("%,d", attemptCounter.get()) + " attempts");
        }

        return new RecoveryResult(
            foundPassword,
            attemptCounter.get(),
            totalTime,
            foundPassword != null
        );
    }

    /**
     * Processes a chunk of base combinations.
     *
     * @param bases list of base combinations to process
     * @param config the password configuration
     * @return the found password, or null
     */
    private String processChunk(List<String> bases, PasswordConfig config) {
        for (String base : bases) {
            if (passwordFound.get()) return null;

            for (String numbers : config.getNumberCombinations()) {
                if (passwordFound.get()) return null;

                for (String special : config.getSpecialCharacters()) {
                    if (passwordFound.get()) return null;

                    String password = base + numbers + special;
                    attemptCounter.incrementAndGet();

                    if (validator.validate(password)) {
                        passwordFound.set(true);
                        return password;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Starts a background thread for progress monitoring.
     *
     * @param startTime the recovery start time in milliseconds
     * @return the progress monitoring thread
     */
    private Thread startProgressMonitoring(long startTime) {
        Thread progressThread = new Thread(() -> {
            while (!passwordFound.get() && !Thread.currentThread().isInterrupted()) {
                long attempts = attemptCounter.get();
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > 0) {
                    long rate = (attempts * 1000) / elapsed;
                    System.out.print("\rProgress: " + String.format("%,d", attempts) +
                                   " attempts | " + rate + " passwords/sec");
                }
                try {
                    Thread.sleep(PROGRESS_UPDATE_INTERVAL_MS);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        progressThread.start();
        return progressThread;
    }

    /**
     * Gets the current attempt count.
     *
     * @return number of passwords tested so far
     */
    public long getAttemptCount() {
        return attemptCounter.get();
    }

    /**
     * Checks if a password has been found.
     *
     * @return true if password found
     */
    public boolean isPasswordFound() {
        return passwordFound.get();
    }

    /**
     * Result of a password recovery operation.
     */
    public static class RecoveryResult {
        private final String password;
        private final long attempts;
        private final long timeMs;
        private final boolean success;

        public RecoveryResult(String password, long attempts, long timeMs, boolean success) {
            this.password = password;
            this.attempts = attempts;
            this.timeMs = timeMs;
            this.success = success;
        }

        public String getPassword() {
            return password;
        }

        public long getAttempts() {
            return attempts;
        }

        public long getTimeMs() {
            return timeMs;
        }

        public double getTimeSec() {
            return timeMs / 1000.0;
        }

        public boolean isSuccess() {
            return success;
        }

        @Override
        public String toString() {
            return String.format("RecoveryResult{success=%s, attempts=%,d, time=%.2fs}",
                success, attempts, getTimeSec());
        }
    }
}
