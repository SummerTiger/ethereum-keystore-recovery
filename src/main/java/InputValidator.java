import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for validating and sanitizing user inputs.
 *
 * <p>This class provides security-focused validation for file paths,
 * passwords, and other user-provided data to prevent common attacks
 * like path traversal, injection, and malformed input.
 *
 * @author KeystoreRecovery Team
 * @version 1.1.0
 */
public final class InputValidator {

    /** Maximum allowed password length to prevent DOS attacks */
    public static final int MAX_PASSWORD_LENGTH = 1000;

    /** Maximum allowed file path length */
    public static final int MAX_PATH_LENGTH = 4096;

    /** Maximum allowed keystore file size (10 MB) */
    public static final long MAX_KEYSTORE_SIZE_BYTES = 10 * 1024 * 1024;

    private InputValidator() {
        // Utility class - prevent instantiation
    }

    /**
     * Validates a keystore file path.
     *
     * <p>This method checks:
     * <ul>
     *   <li>Path is not null or empty</li>
     *   <li>Path length is reasonable</li>
     *   <li>No path traversal attempts (../, .\, etc.)</li>
     *   <li>File exists and is readable</li>
     *   <li>File has .json extension</li>
     *   <li>File size is within reasonable limits</li>
     * </ul>
     *
     * @param pathString the file path to validate
     * @return validated Path object
     * @throws IllegalArgumentException if validation fails
     */
    public static Path validateKeystorePath(String pathString) {
        if (pathString == null || pathString.trim().isEmpty()) {
            throw new IllegalArgumentException("Keystore path cannot be null or empty");
        }

        if (pathString.length() > MAX_PATH_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Path too long: %d chars (max: %d)", pathString.length(), MAX_PATH_LENGTH));
        }

        // Check for path traversal attempts
        String normalized = pathString.trim().replace('\\', '/');
        if (normalized.contains("../") || normalized.contains("/..") ||
            normalized.contains("./")  || normalized.equals("..") || normalized.equals(".")) {
            throw new IllegalArgumentException("Path traversal detected: " + pathString);
        }

        // Check for null bytes
        if (pathString.contains("\0")) {
            throw new IllegalArgumentException("Null byte detected in path");
        }

        // Convert to Path and check existence
        Path path;
        try {
            path = Paths.get(pathString).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Invalid path: " + e.getMessage(), e);
        }

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File not found: " + pathString);
        }

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Not a regular file: " + pathString);
        }

        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("File not readable: " + pathString);
        }

        // Check file extension
        String fileName = path.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".json")) {
            throw new IllegalArgumentException("Keystore file must have .json extension: " + fileName);
        }

        // Check file size
        try {
            long fileSize = Files.size(path);
            if (fileSize > MAX_KEYSTORE_SIZE_BYTES) {
                throw new IllegalArgumentException(
                    String.format("File too large: %d bytes (max: %d)", fileSize, MAX_KEYSTORE_SIZE_BYTES));
            }
            if (fileSize == 0) {
                throw new IllegalArgumentException("File is empty: " + pathString);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot check file size: " + e.getMessage(), e);
        }

        return path;
    }

    /**
     * Validates a password for basic security checks.
     *
     * <p>This method checks:
     * <ul>
     *   <li>Password is not null</li>
     *   <li>Password length is within reasonable bounds</li>
     *   <li>No null bytes in password</li>
     * </ul>
     *
     * <p><strong>Note:</strong> Empty passwords are allowed (some keystores use them).
     *
     * @param password the password to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validatePassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }

        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Password too long: %d chars (max: %d)",
                    password.length(), MAX_PASSWORD_LENGTH));
        }

        // Check for null bytes (can cause issues with C libraries)
        if (password.contains("\0")) {
            throw new IllegalArgumentException("Null byte detected in password");
        }

        // Note: We allow control characters and special characters
        // because users may have used them in their original passwords
    }

    /**
     * Validates a thread count parameter.
     *
     * @param threadCount the thread count to validate
     * @param min minimum allowed value (inclusive)
     * @param max maximum allowed value (inclusive)
     * @return the validated thread count
     * @throws IllegalArgumentException if out of bounds
     */
    public static int validateThreadCount(int threadCount, int min, int max) {
        if (threadCount < min || threadCount > max) {
            throw new IllegalArgumentException(
                String.format("threadCount must be %d-%d, got: %d", min, max, threadCount));
        }
        return threadCount;
    }

    /**
     * Sanitizes a string for safe display in logs or error messages.
     *
     * <p>This method truncates long strings and removes control characters
     * to prevent log injection attacks.
     *
     * @param input the string to sanitize
     * @param maxLength maximum length to display
     * @return sanitized string safe for logging
     */
    public static String sanitizeForLog(String input, int maxLength) {
        if (input == null) {
            return "null";
        }

        // Remove control characters except newline and tab
        String sanitized = input.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "?");

        // Truncate if too long
        if (sanitized.length() > maxLength) {
            return sanitized.substring(0, maxLength) + "... [truncated]";
        }

        return sanitized;
    }

    /**
     * Checks if a string contains only printable ASCII characters.
     *
     * @param input the string to check
     * @return true if all characters are printable ASCII
     */
    public static boolean isPrintableAscii(String input) {
        if (input == null) {
            return false;
        }

        for (char c : input.toCharArray()) {
            if (c < 32 || c > 126) {
                return false;
            }
        }
        return true;
    }
}
