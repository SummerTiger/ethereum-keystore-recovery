/**
 * Interface for validating keystore passwords.
 *
 * <p>Implementations of this interface test whether a given password
 * can successfully decrypt an Ethereum keystore file.
 *
 * @author KeystoreRecovery Team
 * @version 1.1.0
 */
public interface KeystoreValidator {

    /**
     * Tests if the given password can decrypt the keystore.
     *
     * <p>This method should:
     * <ul>
     *   <li>Return true if password is correct</li>
     *   <li>Return false if password is incorrect</li>
     *   <li>Handle exceptions gracefully</li>
     * </ul>
     *
     * @param password the password to test (must not be null)
     * @return true if password is correct, false otherwise
     * @throws IllegalArgumentException if password is null
     */
    boolean validate(String password);

    /**
     * Gets a description of this validator.
     *
     * @return human-readable description
     */
    String getDescription();
}
