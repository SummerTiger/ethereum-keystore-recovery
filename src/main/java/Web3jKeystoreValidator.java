import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.crypto.CipherException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * Web3j-based implementation of KeystoreValidator.
 *
 * <p>This validator uses the Web3j library to test passwords against
 * Ethereum keystore files. It creates a temporary file on initialization
 * and reuses it for all validation attempts for optimal performance.
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe. The validate
 * method is synchronized to prevent concurrent access to the shared temp file.
 *
 * @author KeystoreRecovery Team
 * @version 1.1.0
 */
public class Web3jKeystoreValidator implements KeystoreValidator {

    private final String keystorePath;
    private final Path tempKeystore;

    /**
     * Creates a new validator for the specified keystore file.
     *
     * <p>This constructor:
     * <ul>
     *   <li>Loads the keystore content</li>
     *   <li>Creates a temporary file for validation</li>
     *   <li>Sets restrictive permissions (Unix/Mac only)</li>
     *   <li>Registers a shutdown hook for cleanup</li>
     * </ul>
     *
     * @param keystorePath path to the keystore JSON file
     * @throws IOException if keystore cannot be read or temp file cannot be created
     * @throws IllegalArgumentException if keystorePath is null or empty
     */
    public Web3jKeystoreValidator(String keystorePath) throws IOException {
        if (keystorePath == null || keystorePath.isEmpty()) {
            throw new IllegalArgumentException("keystorePath cannot be null or empty");
        }

        if (!Files.exists(Paths.get(keystorePath))) {
            throw new IOException("Keystore file not found: " + keystorePath);
        }

        this.keystorePath = keystorePath;
        String keystoreContent = Files.readString(Paths.get(keystorePath));

        // Create temp file once and reuse for all password attempts
        this.tempKeystore = Files.createTempFile("keystore", ".json");
        Files.writeString(tempKeystore, keystoreContent);

        // Set restrictive permissions on Unix systems (0600)
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
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Performance Note:</strong> This method is synchronized to ensure
     * thread-safe access to the shared temp file. This adds a small overhead but
     * is necessary for correctness.
     *
     * @throws IllegalArgumentException if password is null
     */
    @Override
    public synchronized boolean validate(String password) {
        if (password == null) {
            throw new IllegalArgumentException("password cannot be null");
        }

        try {
            // Try to load credentials with the password
            Credentials credentials = WalletUtils.loadCredentials(password, tempKeystore.toString());
            return credentials != null;

        } catch (CipherException e) {
            // Wrong password - this is expected
            return false;

        } catch (IOException e) {
            // I/O error - log but continue
            System.err.println("\n⚠️  I/O error testing password: " + e.getMessage());
            return false;

        } catch (Exception e) {
            // Unexpected error - log but continue
            System.err.println("\n⚠️  Unexpected error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String getDescription() {
        return "Web3j Keystore Validator for " + Paths.get(keystorePath).getFileName();
    }

    /**
     * Cleans up resources (temp files).
     *
     * <p>This method should be called when the validator is no longer needed.
     * Note that cleanup also happens automatically via shutdown hook.
     *
     * @throws IOException if cleanup fails
     */
    public void cleanup() throws IOException {
        if (tempKeystore != null) {
            Files.deleteIfExists(tempKeystore);
        }
    }

    /**
     * Gets the path to the keystore file.
     *
     * @return keystore file path
     */
    public String getKeystorePath() {
        return keystorePath;
    }

    /**
     * Gets the path to the temporary keystore file used for validation.
     *
     * @return temp keystore path
     */
    public Path getTempKeystorePath() {
        return tempKeystore;
    }
}
