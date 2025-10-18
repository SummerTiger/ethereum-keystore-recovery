import java.util.*;
import java.util.stream.Collectors;

/**
 * Generator for password combinations based on pattern rules.
 *
 * <p>This class generates password combinations following the pattern:
 * [5-12 character base] + [1-5 digit number] + [1 special character]
 *
 * <p>The generator applies various capitalization strategies and word
 * combinations to maximize recovery chances.
 *
 * @author KeystoreRecovery Team
 * @version 1.1.0
 */
public class PasswordGenerator {

    /** Minimum length for base word combinations */
    public static final int MIN_BASE_LENGTH = 5;

    /** Maximum length for base word combinations */
    public static final int MAX_BASE_LENGTH = 12;

    /** Word separators used for combining base words */
    public static final String[] WORD_SEPARATORS = {"", "-", "_", "."};

    /**
     * Generates all password combinations from the given configuration.
     *
     * <p>The generation process:
     * <ol>
     *   <li>Generate base combinations (5-12 chars) with capitalization variants</li>
     *   <li>Combine bases with number patterns</li>
     *   <li>Append special characters</li>
     * </ol>
     *
     * @param config the password configuration
     * @return set of all generated password combinations
     * @throws IllegalArgumentException if config is null or invalid
     */
    public Set<String> generateAll(PasswordConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        if (!config.isValid()) {
            throw new IllegalArgumentException("config must be valid");
        }

        Set<String> baseCombinations = generateBaseCombinations(config.getBaseWords());
        Set<String> passwords = new HashSet<>();

        for (String base : baseCombinations) {
            for (String numbers : config.getNumberCombinations()) {
                for (String special : config.getSpecialCharacters()) {
                    passwords.add(base + numbers + special);
                }
            }
        }

        return passwords;
    }

    /**
     * Generates base word combinations with length constraints.
     *
     * <p>This method creates:
     * <ul>
     *   <li>Single words with various capitalizations</li>
     *   <li>Two-word combinations with separators</li>
     * </ul>
     *
     * @param words list of base words
     * @return set of base combinations (5-12 characters)
     */
    public Set<String> generateBaseCombinations(List<String> words) {
        if (words == null || words.isEmpty()) {
            throw new IllegalArgumentException("words list cannot be null or empty");
        }

        Set<String> bases = new HashSet<>();

        // Single words with capitalizations
        for (String word : words) {
            if (word.length() >= MIN_BASE_LENGTH && word.length() <= MAX_BASE_LENGTH) {
                bases.add(word);
                bases.add(word.toLowerCase());
                bases.add(word.toUpperCase());
                bases.add(capitalize(word));
                bases.add(titleCase(word));
            }
        }

        // Two-word combinations
        for (String w1 : words) {
            for (String w2 : words) {
                if (w1.equals(w2)) continue;

                for (String sep : WORD_SEPARATORS) {
                    String combined = w1 + sep + w2;
                    if (combined.length() >= MIN_BASE_LENGTH && combined.length() <= MAX_BASE_LENGTH) {
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
     * Capitalizes the first letter of a string.
     *
     * @param str the string to capitalize
     * @return capitalized string, or original if null/empty
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Converts a string to title case (capitalizes each word).
     *
     * @param str the string to convert
     * @return title-cased string, or original if null/empty
     */
    public static String titleCase(String str) {
        if (str == null || str.isEmpty()) return str;
        return Arrays.stream(str.split("\\s+"))
                     .map(PasswordGenerator::capitalize)
                     .collect(Collectors.joining(" "));
    }

    /**
     * Estimates the total number of passwords that will be generated.
     *
     * @param config the password configuration
     * @return estimated number of password combinations
     */
    public long estimateCount(PasswordConfig config) {
        if (config == null || !config.isValid()) {
            return 0;
        }

        Set<String> bases = generateBaseCombinations(config.getBaseWords());
        return (long) bases.size() *
               config.getNumberCombinations().size() *
               config.getSpecialCharacters().size();
    }
}
