import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.crypto.CipherException;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;
import java.util.regex.*;

/**
 * High-Performance Ethereum Keystore Password Recovery Tool
 * Pattern: [5-12 chars] + [1-5 digits] + [1 special char]
 */
public class KeystoreRecovery {

    private final String keystorePath;
    private final String keystoreContent;
    private final AtomicLong attemptCounter = new AtomicLong(0);
    private final AtomicBoolean passwordFound = new AtomicBoolean(false);
    private volatile String foundPassword = null;
    private final Path tempKeystore;

    public KeystoreRecovery(String keystorePath) throws IOException {
        this.keystorePath = keystorePath;
        this.keystoreContent = Files.readString(Paths.get(keystorePath));

        // Create temp file once and reuse for all password attempts
        this.tempKeystore = Files.createTempFile("keystore", ".json");
        Files.writeString(tempKeystore, keystoreContent);

        // Set restrictive permissions on Unix systems
        try {
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
            Files.setPosixFilePermissions(tempKeystore, perms);
        } catch (UnsupportedOperationException e) {
            // Windows doesn't support POSIX permissions, skip
        }

        // Ensure cleanup on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.deleteIfExists(tempKeystore);
            } catch (IOException ignored) {
                // Best effort cleanup
            }
        }));

        System.out.println("✓ Keystore file loaded: " + Paths.get(keystorePath).getFileName());
    }

    /**
     * Configuration class to hold password components
     */
    static class PasswordConfig {
        List<String> baseWords;
        List<String> numberCombinations;
        List<String> specialCharacters;

        PasswordConfig(List<String> baseWords, List<String> numbers, List<String> specials) {
            this.baseWords = baseWords;
            this.numberCombinations = numbers;
            this.specialCharacters = specials;
        }

        static PasswordConfig fromMarkdown(String filePath) throws IOException {
            String content = Files.readString(Paths.get(filePath));

            List<String> baseWords = new ArrayList<>();
            List<String> numbers = new ArrayList<>();
            List<String> specials = new ArrayList<>();

            String[] sections = content.split("(?=^##\\s)", Pattern.MULTILINE);

            for (String section : sections) {
                if (section.trim().isEmpty()) continue;

                String[] lines = section.split("\n");
                String header = lines[0].toLowerCase();

                List<String> currentList = null;
                if (header.contains("base") || header.contains("word")) {
                    currentList = baseWords;
                } else if (header.contains("number") || header.contains("digit")) {
                    currentList = numbers;
                } else if (header.contains("special") || header.contains("character")) {
                    currentList = specials;
                }

                if (currentList != null) {
                    for (int i = 1; i < lines.length; i++) {
                        String line = lines[i].trim();
                        // Match bullet points or plain lines
                        Pattern pattern = Pattern.compile("^[-*+\\d.]\\s+(.+)");
                        Matcher matcher = pattern.matcher(line);

                        if (matcher.find()) {
                            String item = matcher.group(1).trim();
                            if (validateItem(item, currentList)) {
                                currentList.add(item);
                            }
                        } else if (!line.isEmpty() && !line.startsWith("#")) {
                            if (validateItem(line, currentList)) {
                                currentList.add(line);
                            }
                        }
                    }
                }
            }

            return new PasswordConfig(baseWords, numbers, specials);
        }

        private static boolean validateItem(String item, List<String> targetList) {
            if (targetList == null) return false;

            // Validation based on list type (simplified check)
            if (item.matches("\\d{1,5}")) {
                return true; // Valid number
            } else if (item.length() == 1 && !Character.isLetterOrDigit(item.charAt(0))) {
                return true; // Valid special character
            } else if (item.length() >= 1 && item.length() <= 20) {
                return true; // Valid word
            }
            return false;
        }

        static void createSampleConfig(String filePath) throws IOException {
            String sample = """
# Keystore Password Recovery Configuration

## Base Words
*List your commonly used base words or phrases (5-12 characters)*

- password
- crypto
- wallet
- ethereum
- mytoken
- secure
- private
- blockchain

## Number Combinations
*List your commonly used number patterns (1-5 digits)*

- 123
- 1234
- 2023
- 2024
- 99
- 00
- 777
- 111
- 2025

## Special Characters
*List your commonly used special characters (single character)*

- !
- @
- #
- $
- %
- &
- *
- _
- .

## Notes
- Order items by likelihood for faster recovery
- Base words will be tried with different capitalizations
- Words can be combined to reach the 5-12 character requirement
            """;

            Files.writeString(Paths.get(filePath), sample);
            System.out.println("✓ Sample configuration created: " + filePath);
        }
    }

    /**
     * Generate base word combinations (5-12 characters)
     */
    private Set<String> generateBaseCombinations(List<String> words) {
        Set<String> bases = new HashSet<>();
        int minLen = 5, maxLen = 12;

        // Single words with capitalizations
        for (String word : words) {
            if (word.length() >= minLen && word.length() <= maxLen) {
                bases.add(word);
                bases.add(word.toLowerCase());
                bases.add(word.toUpperCase());
                bases.add(capitalize(word));
                bases.add(titleCase(word));
            }
        }

        // Two-word combinations
        String[] separators = {"", "-", "_", "."};
        for (String w1 : words) {
            for (String w2 : words) {
                if (w1.equals(w2)) continue;

                for (String sep : separators) {
                    String combined = w1 + sep + w2;
                    if (combined.length() >= minLen && combined.length() <= maxLen) {
                        bases.add(combined.toLowerCase());
                        bases.add(combined.toUpperCase());
                        bases.add(capitalize(w1) + sep + capitalize(w2));
                    }
                }
            }
        }

        return bases;
    }

    /**
     * Try to decrypt keystore with password.
     * Thread-safe: Uses synchronized access to shared temp file.
     *
     * @param password the password to test
     * @return true if password is correct, false otherwise
     */
    private synchronized boolean tryPassword(String password) {
        try {
            // Try to load credentials with the password
            Credentials credentials = WalletUtils.loadCredentials(password, tempKeystore.toString());
            return credentials != null;
        } catch (CipherException e) {
            // Wrong password - this is expected
            return false;
        } catch (IOException e) {
            System.err.println("\n⚠️  I/O error testing password: " + e.getMessage());
            return false;
        } catch (Exception e) {
            // Log unexpected errors but continue
            System.err.println("\n⚠️  Unexpected error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cleanup resources (temp files).
     * Should be called when recovery is complete or interrupted.
     */
    private void cleanup() {
        try {
            if (tempKeystore != null) {
                Files.deleteIfExists(tempKeystore);
            }
        } catch (IOException e) {
            System.err.println("⚠️  Failed to cleanup temp file: " + e.getMessage());
        }
    }

    /**
     * Multi-threaded password recovery with input validation.
     *
     * @param config the password configuration
     * @param threadCount number of threads to use (1-100)
     * @return the recovered password, or null if not found
     * @throws IllegalArgumentException if parameters are invalid
     * @throws InterruptedException if recovery is interrupted
     */
    public String recoverPassword(PasswordConfig config, int threadCount) throws InterruptedException {
        // Input validation
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        if (threadCount < 1 || threadCount > 100) {
            throw new IllegalArgumentException("threadCount must be 1-100, got: " + threadCount);
        }
        if (config.baseWords.isEmpty()) {
            throw new IllegalArgumentException("Base words list cannot be empty");
        }
        if (config.numberCombinations.isEmpty()) {
            throw new IllegalArgumentException("Number combinations list cannot be empty");
        }
        if (config.specialCharacters.isEmpty()) {
            throw new IllegalArgumentException("Special characters list cannot be empty");
        }

        Set<String> baseCombinations = generateBaseCombinations(config.baseWords);

        long totalCombinations = (long) baseCombinations.size() *
                                config.numberCombinations.size() *
                                config.specialCharacters.size();

        System.out.println("\n🔍 Starting password recovery...");
        System.out.println("Pattern: [5-12 chars] + [1-5 digits] + [1 special char]");
        System.out.println("Total combinations: " + String.format("%,d", totalCombinations));
        System.out.println("Using " + threadCount + " threads for parallel processing");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<String>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // Progress monitoring thread
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
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        progressThread.start();

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
                e.printStackTrace();
            }
        }

        // Cleanup
        executor.shutdownNow();
        progressThread.interrupt();

        long totalTime = System.currentTimeMillis() - startTime;

        if (foundPassword != null) {
            System.out.println("\n\n✅ SUCCESS! Password found!");
            System.out.println("Total attempts: " + String.format("%,d", attemptCounter.get()));
            System.out.println("Time elapsed: " + (totalTime / 1000.0) + " seconds");
        } else {
            System.out.println("\n\n❌ Password not found after " +
                             String.format("%,d", attemptCounter.get()) + " attempts");
        }

        return foundPassword;
    }

    /**
     * Process a chunk of base combinations
     */
    private String processChunk(List<String> bases, PasswordConfig config) {
        for (String base : bases) {
            if (passwordFound.get()) return null;

            for (String numbers : config.numberCombinations) {
                if (passwordFound.get()) return null;

                for (String special : config.specialCharacters) {
                    if (passwordFound.get()) return null;

                    String password = base + numbers + special;
                    attemptCounter.incrementAndGet();

                    if (tryPassword(password)) {
                        passwordFound.set(true);
                        return password;
                    }
                }
            }
        }
        return null;
    }

    // Utility methods
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private static String titleCase(String str) {
        if (str == null || str.isEmpty()) return str;
        return Arrays.stream(str.split("\\s+"))
                     .map(KeystoreRecovery::capitalize)
                     .collect(Collectors.joining(" "));
    }

    /**
     * Main method with proper resource management and error handling.
     */
    public static void main(String[] args) {
        KeystoreRecovery recovery = null;

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("=" + "=".repeat(59));
            System.out.println("ETHEREUM KEYSTORE PASSWORD RECOVERY (Java High-Performance)");
            System.out.println("Pattern: [5-12 chars] + [1-5 digits] + [1 special char]");
            System.out.println("=" + "=".repeat(59));

            // Get file paths
            String keystorePath, configPath;
            if (args.length >= 1) {
                keystorePath = args[0];
                configPath = args.length >= 2 ? args[1] : "password_config.md";
            } else {
                System.out.print("\n📁 Enter path to keystore file: ");
                keystorePath = scanner.nextLine().trim();

                System.out.print("📄 Enter path to config file (default: password_config.md): ");
                configPath = scanner.nextLine().trim();
                if (configPath.isEmpty()) configPath = "password_config.md";
            }

            // Validate keystore file exists
            if (!Files.exists(Paths.get(keystorePath))) {
                System.err.println("\n❌ Keystore file not found: " + keystorePath);
                System.exit(1);
            }

            // Check if config exists
            if (!Files.exists(Paths.get(configPath))) {
                System.out.println("\n⚠️  Configuration file not found: " + configPath);
                System.out.print("Create sample configuration? (y/n): ");
                if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                    PasswordConfig.createSampleConfig(configPath);
                    System.out.println("\n📝 Edit '" + configPath + "' and run again.");
                    return;
                } else {
                    System.err.println("Cannot proceed without configuration file.");
                    System.exit(1);
                }
            }

            // Load configuration
            System.out.println("\n📖 Reading configuration from: " + configPath);
            PasswordConfig config = PasswordConfig.fromMarkdown(configPath);

            // Validate configuration is not empty
            if (config.baseWords.isEmpty() || config.numberCombinations.isEmpty() ||
                config.specialCharacters.isEmpty()) {
                System.err.println("\n❌ Configuration file is incomplete. Please add:");
                if (config.baseWords.isEmpty()) System.err.println("  - Base words");
                if (config.numberCombinations.isEmpty()) System.err.println("  - Number combinations");
                if (config.specialCharacters.isEmpty()) System.err.println("  - Special characters");
                System.exit(1);
            }

            // Display summary
            System.out.println("\n" + "=".repeat(60));
            System.out.println("CONFIGURATION LOADED");
            System.out.println("=".repeat(60));
            System.out.println("📝 Base words: " + config.baseWords.size() + " items");
            System.out.println("🔢 Number combinations: " + config.numberCombinations.size() + " items");
            System.out.println("⚡ Special characters: " + config.specialCharacters.size() + " items");

            // Ask for thread count with validation
            int availableProcessors = Runtime.getRuntime().availableProcessors();
            int threads = Math.min(8, availableProcessors);

            System.out.print("\n🖥️  Number of threads to use (available: " +
                           availableProcessors + ", recommended: " + threads + "): ");
            String threadInput = scanner.nextLine().trim();

            if (!threadInput.isEmpty()) {
                try {
                    threads = Integer.parseInt(threadInput);
                    if (threads < 1 || threads > 100) {
                        System.err.println("⚠️  Thread count must be 1-100. Using default: " +
                                         Math.min(8, availableProcessors));
                        threads = Math.min(8, availableProcessors);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("⚠️  Invalid number. Using default: " +
                                     Math.min(8, availableProcessors));
                    threads = Math.min(8, availableProcessors);
                }
            }

            // Confirm
            System.out.print("\n⚠️  Start recovery? (y/n): ");
            if (!scanner.nextLine().trim().equalsIgnoreCase("y")) {
                System.out.println("Recovery cancelled.");
                return;
            }

            // Start recovery
            recovery = new KeystoreRecovery(keystorePath);
            String password = recovery.recoverPassword(config, threads);

            if (password != null) {
                System.out.println("\n" + "=".repeat(60));
                System.out.println("🎉 WALLET RECOVERED SUCCESSFULLY!");
                System.out.println("=".repeat(60));

                // Ask before displaying password
                System.out.print("\nDisplay password? (y/n): ");
                if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                    System.out.println("Password: " + password);
                }

                System.out.print("\nSave to file? (y/n): ");
                if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                    Path outputFile = Paths.get("recovered_password.txt");
                    Files.writeString(outputFile,
                        "Password: " + password + "\n" +
                        "Keystore: " + keystorePath + "\n" +
                        "Time: " + new Date() + "\n");

                    // Set restrictive permissions on Unix systems
                    try {
                        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
                        Files.setPosixFilePermissions(outputFile, perms);
                    } catch (UnsupportedOperationException e) {
                        // Windows doesn't support POSIX permissions
                    }

                    System.out.println("✓ Saved to recovered_password.txt");
                    System.out.println("⚠️  WARNING: Password saved in plain text - delete after use!");
                }
            }

        } catch (FileNotFoundException e) {
            System.err.println("\n❌ File not found: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("\n❌ I/O error: " + e.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("\n❌ Invalid input: " + e.getMessage());
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("\n⚠️  Recovery interrupted by user");
            System.exit(130);
        } catch (Exception e) {
            System.err.println("\n❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            // Ensure cleanup happens
            if (recovery != null) {
                recovery.cleanup();
            }
        }
    }
}
