import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable configuration class for password recovery components.
 *
 * <p>This class holds the base words, number combinations, and special characters
 * used to generate password candidates. All lists are immutable once constructed.
 *
 * @author KeystoreRecovery Team
 * @version 1.1.0
 */
public final class PasswordConfig {

    private final List<String> baseWords;
    private final List<String> numberCombinations;
    private final List<String> specialCharacters;

    /**
     * Private constructor - use Builder to create instances.
     *
     * @param baseWords list of base words
     * @param numbers list of number combinations
     * @param specials list of special characters
     */
    private PasswordConfig(List<String> baseWords, List<String> numbers, List<String> specials) {
        this.baseWords = Collections.unmodifiableList(new ArrayList<>(baseWords));
        this.numberCombinations = Collections.unmodifiableList(new ArrayList<>(numbers));
        this.specialCharacters = Collections.unmodifiableList(new ArrayList<>(specials));
    }

    /**
     * Gets immutable list of base words.
     *
     * @return unmodifiable list of base words
     */
    public List<String> getBaseWords() {
        return baseWords;
    }

    /**
     * Gets immutable list of number combinations.
     *
     * @return unmodifiable list of number combinations
     */
    public List<String> getNumberCombinations() {
        return numberCombinations;
    }

    /**
     * Gets immutable list of special characters.
     *
     * @return unmodifiable list of special characters
     */
    public List<String> getSpecialCharacters() {
        return specialCharacters;
    }

    /**
     * Checks if the configuration is valid (all lists non-empty).
     *
     * @return true if all lists contain at least one item
     */
    public boolean isValid() {
        return !baseWords.isEmpty() &&
               !numberCombinations.isEmpty() &&
               !specialCharacters.isEmpty();
    }

    /**
     * Loads password configuration from a markdown file.
     *
     * <p>The markdown file should contain three sections:
     * <ul>
     *   <li>## Base Words - containing base word entries</li>
     *   <li>## Number Combinations - containing number patterns</li>
     *   <li>## Special Characters - containing special characters</li>
     * </ul>
     *
     * @param filePath path to the markdown configuration file
     * @return a new PasswordConfig instance
     * @throws IOException if file cannot be read
     * @throws IllegalArgumentException if configuration is invalid
     */
    public static PasswordConfig fromMarkdown(String filePath) throws IOException {
        String content = Files.readString(Paths.get(filePath));

        List<String> baseWords = new ArrayList<>();
        List<String> numbers = new ArrayList<>();
        List<String> specials = new ArrayList<>();

        String[] sections = content.split("(?m)(?=^## )");

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
                    Pattern pattern = Pattern.compile("^[-*+\\d.]\\s+(.+)");
                    Matcher matcher = pattern.matcher(line);

                    if (matcher.find()) {
                        String item = matcher.group(1).trim();
                        if (validateItem(item)) {
                            currentList.add(item);
                        }
                    } else if (!line.isEmpty() && !line.startsWith("#") && !line.startsWith("*")) {
                        if (validateItem(line)) {
                            currentList.add(line);
                        }
                    }
                }
            }
        }

        PasswordConfig config = new Builder()
            .setBaseWords(baseWords)
            .setNumberCombinations(numbers)
            .setSpecialCharacters(specials)
            .build();

        if (!config.isValid()) {
            throw new IllegalArgumentException("Configuration is incomplete - all sections must have at least one entry");
        }

        return config;
    }

    /**
     * Validates an item from the configuration file.
     *
     * @param item the item to validate
     * @return true if item is valid
     */
    private static boolean validateItem(String item) {
        if (item == null || item.isEmpty()) return false;

        // Valid number (1-5 digits)
        if (item.matches("\\d{1,5}")) {
            return true;
        }

        // Valid special character
        if (item.length() == 1 && !Character.isLetterOrDigit(item.charAt(0))) {
            return true;
        }

        // Valid word (1-20 characters)
        if (item.length() >= 1 && item.length() <= 20) {
            return true;
        }

        return false;
    }

    /**
     * Creates a sample configuration file at the specified path.
     *
     * @param filePath path where the sample file will be created
     * @throws IOException if file cannot be written
     */
    public static void createSampleConfig(String filePath) throws IOException {
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

    @Override
    public String toString() {
        return String.format("PasswordConfig{baseWords=%d, numbers=%d, specials=%d}",
            baseWords.size(), numberCombinations.size(), specialCharacters.size());
    }

    /**
     * Builder class for constructing PasswordConfig instances.
     */
    public static class Builder {
        private List<String> baseWords = new ArrayList<>();
        private List<String> numberCombinations = new ArrayList<>();
        private List<String> specialCharacters = new ArrayList<>();

        /**
         * Sets the base words list.
         *
         * @param words list of base words
         * @return this builder
         */
        public Builder setBaseWords(List<String> words) {
            this.baseWords = new ArrayList<>(words);
            return this;
        }

        /**
         * Sets the number combinations list.
         *
         * @param numbers list of number combinations
         * @return this builder
         */
        public Builder setNumberCombinations(List<String> numbers) {
            this.numberCombinations = new ArrayList<>(numbers);
            return this;
        }

        /**
         * Sets the special characters list.
         *
         * @param specials list of special characters
         * @return this builder
         */
        public Builder setSpecialCharacters(List<String> specials) {
            this.specialCharacters = new ArrayList<>(specials);
            return this;
        }

        /**
         * Adds a single base word.
         *
         * @param word the base word to add
         * @return this builder
         */
        public Builder addBaseWord(String word) {
            if (word != null && !word.isEmpty()) {
                this.baseWords.add(word);
            }
            return this;
        }

        /**
         * Adds a single number combination.
         *
         * @param number the number combination to add
         * @return this builder
         */
        public Builder addNumberCombination(String number) {
            if (number != null && !number.isEmpty()) {
                this.numberCombinations.add(number);
            }
            return this;
        }

        /**
         * Adds a single special character.
         *
         * @param special the special character to add
         * @return this builder
         */
        public Builder addSpecialCharacter(String special) {
            if (special != null && !special.isEmpty()) {
                this.specialCharacters.add(special);
            }
            return this;
        }

        /**
         * Builds the PasswordConfig instance.
         *
         * @return a new immutable PasswordConfig
         * @throws IllegalStateException if any list is empty
         */
        public PasswordConfig build() {
            if (baseWords.isEmpty()) {
                throw new IllegalStateException("Base words list cannot be empty");
            }
            if (numberCombinations.isEmpty()) {
                throw new IllegalStateException("Number combinations list cannot be empty");
            }
            if (specialCharacters.isEmpty()) {
                throw new IllegalStateException("Special characters list cannot be empty");
            }

            return new PasswordConfig(baseWords, numberCombinations, specialCharacters);
        }
    }
}
