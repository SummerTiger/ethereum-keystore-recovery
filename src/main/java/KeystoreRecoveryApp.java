import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Date;
import java.util.Scanner;
import java.util.Set;

/**
 * Main application class for Ethereum Keystore Password Recovery.
 *
 * <p>This is the entry point for the command-line interface. It handles:
 * <ul>
 *   <li>User interaction and input validation</li>
 *   <li>Configuration loading and validation</li>
 *   <li>Recovery engine orchestration</li>
 *   <li>Result display and secure password handling</li>
 * </ul>
 *
 * <p><strong>Usage:</strong>
 * <pre>
 * # Interactive mode
 * java KeystoreRecoveryApp
 *
 * # With arguments
 * java KeystoreRecoveryApp keystore.json password_config.md
 * </pre>
 *
 * @author KeystoreRecovery Team
 * @version 1.1.0
 */
public class KeystoreRecoveryApp {

    /** Banner width for display formatting */
    private static final int BANNER_WIDTH = 60;

    /** Default configuration file name */
    private static final String DEFAULT_CONFIG_FILE = "password_config.md";

    /** Output file for recovered passwords */
    private static final String OUTPUT_FILE = "recovered_password.txt";

    /**
     * Main entry point for the application.
     *
     * @param args command line arguments: [keystorePath] [configPath]
     */
    public static void main(String[] args) {
        Web3jKeystoreValidator validator = null;

        try (Scanner scanner = new Scanner(System.in)) {
            printBanner("ETHEREUM KEYSTORE PASSWORD RECOVERY (Java High-Performance)",
                       "Pattern: [5-12 chars] + [1-5 digits] + [1 special char]");

            // Get file paths
            String keystorePath = getKeystorePath(args, scanner);
            String configPath = getConfigPath(args, scanner);

            // Validate keystore file exists
            validateKeystoreExists(keystorePath);

            // Handle configuration file
            ensureConfigExists(configPath, scanner);

            // Load and validate configuration
            System.out.println("\n📖 Reading configuration from: " + configPath);
            PasswordConfig config = PasswordConfig.fromMarkdown(configPath);
            displayConfigSummary(config);

            // Get thread count
            int threads = getThreadCount(scanner);

            // Confirm start
            if (!confirmStart(scanner)) {
                System.out.println("Recovery cancelled.");
                return;
            }

            // Initialize components
            validator = new Web3jKeystoreValidator(keystorePath);
            PasswordGenerator generator = new PasswordGenerator();
            RecoveryEngine engine = new RecoveryEngine(validator, generator, threads);

            // Run recovery
            RecoveryEngine.RecoveryResult result = engine.recover(config);

            // Handle results
            if (result.isSuccess()) {
                handleSuccess(result.getPassword(), keystorePath, scanner);
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
            if (validator != null) {
                try {
                    validator.cleanup();
                } catch (IOException e) {
                    System.err.println("⚠️  Failed to cleanup: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Gets the keystore file path from arguments or user input.
     */
    private static String getKeystorePath(String[] args, Scanner scanner) {
        if (args.length >= 1) {
            return args[0];
        }

        System.out.print("\n📁 Enter path to keystore file: ");
        return scanner.nextLine().trim();
    }

    /**
     * Gets the configuration file path from arguments or user input.
     */
    private static String getConfigPath(String[] args, Scanner scanner) {
        if (args.length >= 2) {
            return args[1];
        }

        if (args.length == 1) {
            return DEFAULT_CONFIG_FILE;
        }

        System.out.print("📄 Enter path to config file (default: " + DEFAULT_CONFIG_FILE + "): ");
        String path = scanner.nextLine().trim();
        return path.isEmpty() ? DEFAULT_CONFIG_FILE : path;
    }

    /**
     * Validates that the keystore file exists.
     */
    private static void validateKeystoreExists(String keystorePath) {
        if (!Files.exists(Paths.get(keystorePath))) {
            System.err.println("\n❌ Keystore file not found: " + keystorePath);
            System.exit(1);
        }
    }

    /**
     * Ensures configuration file exists, offers to create sample if not.
     */
    private static void ensureConfigExists(String configPath, Scanner scanner) throws IOException {
        if (!Files.exists(Paths.get(configPath))) {
            System.out.println("\n⚠️  Configuration file not found: " + configPath);
            System.out.print("Create sample configuration? (y/n): ");

            if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                PasswordConfig.createSampleConfig(configPath);
                System.out.println("\n📝 Edit '" + configPath + "' and run again.");
                System.exit(0);
            } else {
                System.err.println("Cannot proceed without configuration file.");
                System.exit(1);
            }
        }
    }

    /**
     * Displays configuration summary.
     */
    private static void displayConfigSummary(PasswordConfig config) {
        printBanner("CONFIGURATION LOADED", null);
        System.out.println("📝 Base words: " + config.getBaseWords().size() + " items");
        System.out.println("🔢 Number combinations: " + config.getNumberCombinations().size() + " items");
        System.out.println("⚡ Special characters: " + config.getSpecialCharacters().size() + " items");
    }

    /**
     * Gets thread count from user with validation.
     */
    private static int getThreadCount(Scanner scanner) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int defaultThreads = Math.min(8, availableProcessors);

        System.out.print("\n🖥️  Number of threads to use (available: " +
                       availableProcessors + ", recommended: " + defaultThreads + "): ");
        String threadInput = scanner.nextLine().trim();

        if (threadInput.isEmpty()) {
            return defaultThreads;
        }

        try {
            int threads = Integer.parseInt(threadInput);
            if (threads < RecoveryEngine.MIN_THREADS || threads > RecoveryEngine.MAX_THREADS) {
                System.err.println("⚠️  Thread count must be " + RecoveryEngine.MIN_THREADS +
                                 "-" + RecoveryEngine.MAX_THREADS + ". Using default: " + defaultThreads);
                return defaultThreads;
            }
            return threads;
        } catch (NumberFormatException e) {
            System.err.println("⚠️  Invalid number. Using default: " + defaultThreads);
            return defaultThreads;
        }
    }

    /**
     * Confirms with user before starting recovery.
     */
    private static boolean confirmStart(Scanner scanner) {
        System.out.print("\n⚠️  Start recovery? (y/n): ");
        return scanner.nextLine().trim().equalsIgnoreCase("y");
    }

    /**
     * Handles successful password recovery.
     */
    private static void handleSuccess(String password, String keystorePath, Scanner scanner)
            throws IOException {
        printBanner("🎉 WALLET RECOVERED SUCCESSFULLY!", null);

        // Ask before displaying password
        System.out.print("\nDisplay password? (y/n): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
            System.out.println("Password: " + password);
        }

        // Ask to save to file
        System.out.print("\nSave to file? (y/n): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
            savePasswordToFile(password, keystorePath);
        }
    }

    /**
     * Saves password to file with secure permissions.
     */
    private static void savePasswordToFile(String password, String keystorePath) throws IOException {
        Path outputFile = Paths.get(OUTPUT_FILE);
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

        System.out.println("✓ Saved to " + OUTPUT_FILE);
        System.out.println("⚠️  WARNING: Password saved in plain text - delete after use!");
    }

    /**
     * Prints a formatted banner.
     */
    private static void printBanner(String title, String subtitle) {
        String border = "=".repeat(BANNER_WIDTH);
        System.out.println("\n" + border);
        System.out.println(title);
        if (subtitle != null) {
            System.out.println(subtitle);
        }
        System.out.println(border);
    }
}
