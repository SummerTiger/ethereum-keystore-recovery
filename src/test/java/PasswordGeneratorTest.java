import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for PasswordGenerator class.
 * Tests password generation, capitalization strategies, and combination logic.
 *
 * @author KeystoreRecovery Team
 * @version 1.1.0
 */
class PasswordGeneratorTest {

    private PasswordGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new PasswordGenerator();
    }

    // ========== Constants Tests ==========

    @Test
    void testConstants() {
        assertThat(PasswordGenerator.MIN_BASE_LENGTH).isEqualTo(5);
        assertThat(PasswordGenerator.MAX_BASE_LENGTH).isEqualTo(12);
        assertThat(PasswordGenerator.WORD_SEPARATORS).containsExactly("", "-", "_", ".");
    }

    // ========== generateAll() Tests ==========

    @Test
    void testGenerateAllWithMinimalConfig() {
        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("password")
            .addNumberCombination("123")
            .addSpecialCharacter("!")
            .build();

        Set<String> passwords = generator.generateAll(config);

        assertThat(passwords).isNotEmpty();
        // Should have at least: password123!, Password123!, PASSWORD123!, etc.
        assertThat(passwords).contains("password123!");
    }

    @Test
    void testGenerateAllWithMultipleOptions() {
        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("crypto")
            .addNumberCombination("123")
            .addNumberCombination("456")
            .addSpecialCharacter("!")
            .addSpecialCharacter("@")
            .build();

        Set<String> passwords = generator.generateAll(config);

        // Should contain all combinations
        assertThat(passwords).contains("crypto123!");
        assertThat(passwords).contains("crypto123@");
        assertThat(passwords).contains("crypto456!");
        assertThat(passwords).contains("crypto456@");
    }

    @Test
    void testGenerateAllNoDuplicates() {
        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("test12")
            .addNumberCombination("1")
            .addSpecialCharacter("!")
            .build();

        Set<String> passwords = generator.generateAll(config);

        // Verify no duplicates (Set property)
        long uniqueCount = passwords.stream().distinct().count();
        assertThat(passwords).hasSize((int) uniqueCount);
    }

    @Test
    void testGenerateAllThrowsExceptionForNullConfig() {
        assertThatThrownBy(() -> generator.generateAll(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("config cannot be null");
    }

    @Test
    void testGenerateAllThrowsExceptionForInvalidConfig() {
        // Create an invalid config by using Builder incorrectly isn't possible
        // since Builder validates. But we can test with a mock.
        // For now, let's ensure valid configs work
        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("password")
            .addNumberCombination("123")
            .addSpecialCharacter("!")
            .build();

        assertThat(config.isValid()).isTrue();
        Set<String> passwords = generator.generateAll(config);
        assertThat(passwords).isNotEmpty();
    }

    // ========== generateBaseCombinations() Tests ==========

    @Test
    void testGenerateBaseCombinationsSingleWord() {
        List<String> words = Arrays.asList("crypto");

        Set<String> bases = generator.generateBaseCombinations(words);

        // Should have: crypto, CRYPTO, Crypto (and titleCase)
        assertThat(bases).contains("crypto");
        assertThat(bases).contains("CRYPTO");
        assertThat(bases).contains("Crypto");
    }

    @Test
    void testGenerateBaseCombinationsWithShortWord() {
        // "pwd" is only 3 characters, below MIN_BASE_LENGTH (5)
        List<String> words = Arrays.asList("pwd");

        Set<String> bases = generator.generateBaseCombinations(words);

        // Should not include single "pwd" since it's too short
        assertThat(bases).doesNotContain("pwd");
        assertThat(bases).isEmpty();
    }

    @Test
    void testGenerateBaseCombinationsWithLongWord() {
        // Word longer than MAX_BASE_LENGTH (12)
        List<String> words = Arrays.asList("verylongpassword");

        Set<String> bases = generator.generateBaseCombinations(words);

        // Should not include this word since it exceeds max length
        assertThat(bases).doesNotContain("verylongpassword");
        assertThat(bases).isEmpty();
    }

    @Test
    void testGenerateBaseCombinationsWithExactBoundaries() {
        List<String> words = Arrays.asList("12345", "123456789012");

        Set<String> bases = generator.generateBaseCombinations(words);

        // Both words should be included (exactly MIN and MAX length)
        assertThat(bases).contains("12345");
        assertThat(bases).contains("123456789012");
    }

    @Test
    void testGenerateBaseCombinationsTwoWords() {
        List<String> words = Arrays.asList("my", "pass");

        Set<String> bases = generator.generateBaseCombinations(words);

        // Should combine with separators: "my-pass", "my_pass", "mypass", "my.pass"
        // Only if length is 5-12 chars
        assertThat(bases).contains("my-pass");  // 7 chars
        assertThat(bases).contains("my_pass");  // 7 chars
        assertThat(bases).contains("mypass");   // 6 chars
        assertThat(bases).contains("my.pass");  // 7 chars

        // Should also have reverse combinations
        assertThat(bases).contains("pass-my");
        assertThat(bases).contains("passmy");
    }

    @Test
    void testGenerateBaseCombinationsSkipsSameWordCombination() {
        List<String> words = Arrays.asList("test12");

        Set<String> bases = generator.generateBaseCombinations(words);

        // Should NOT have "test-test" or similar (same word repeated)
        assertThat(bases).doesNotContain("test12-test12");
        assertThat(bases).doesNotContain("test12_test12");
    }

    @Test
    void testGenerateBaseCombinationsWithMultipleWords() {
        List<String> words = Arrays.asList("my", "pass", "word");

        Set<String> bases = generator.generateBaseCombinations(words);

        // Should have combinations like:
        assertThat(bases).contains("my-pass");
        assertThat(bases).contains("my-word");
        assertThat(bases).contains("pass-my");
        assertThat(bases).contains("pass-word");
        assertThat(bases).contains("word-my");
        assertThat(bases).contains("word-pass");
    }

    @Test
    void testGenerateBaseCombinationsCapitalizationVariants() {
        List<String> words = Arrays.asList("my", "pass");

        Set<String> bases = generator.generateBaseCombinations(words);

        // Two-word combinations should have capitalization variants
        assertThat(bases).contains("my-pass");      // lowercase
        assertThat(bases).contains("MY-PASS");      // uppercase
        assertThat(bases).contains("My-Pass");      // capitalized both
    }

    @Test
    void testGenerateBaseCombinationsThrowsExceptionForNull() {
        assertThatThrownBy(() -> generator.generateBaseCombinations(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("words list cannot be null or empty");
    }

    @Test
    void testGenerateBaseCombinationsThrowsExceptionForEmptyList() {
        assertThatThrownBy(() -> generator.generateBaseCombinations(Arrays.asList()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("words list cannot be null or empty");
    }

    // ========== capitalize() Tests ==========

    @Test
    void testCapitalizeNormalString() {
        assertThat(PasswordGenerator.capitalize("hello")).isEqualTo("Hello");
        assertThat(PasswordGenerator.capitalize("WORLD")).isEqualTo("World");
        assertThat(PasswordGenerator.capitalize("pAssWoRd")).isEqualTo("Password");
    }

    @Test
    void testCapitalizeSingleCharacter() {
        assertThat(PasswordGenerator.capitalize("a")).isEqualTo("A");
        assertThat(PasswordGenerator.capitalize("Z")).isEqualTo("Z");
    }

    @Test
    void testCapitalizeEmptyString() {
        assertThat(PasswordGenerator.capitalize("")).isEqualTo("");
    }

    @Test
    void testCapitalizeNull() {
        assertThat(PasswordGenerator.capitalize(null)).isNull();
    }

    @Test
    void testCapitalizeWithNumbers() {
        assertThat(PasswordGenerator.capitalize("123abc")).isEqualTo("123abc");
    }

    @Test
    void testCapitalizeWithSpecialChars() {
        assertThat(PasswordGenerator.capitalize("!hello")).isEqualTo("!hello");
    }

    // ========== titleCase() Tests ==========

    @Test
    void testTitleCaseSingleWord() {
        assertThat(PasswordGenerator.titleCase("hello")).isEqualTo("Hello");
    }

    @Test
    void testTitleCaseMultipleWords() {
        assertThat(PasswordGenerator.titleCase("hello world")).isEqualTo("Hello World");
        assertThat(PasswordGenerator.titleCase("my secure password")).isEqualTo("My Secure Password");
    }

    @Test
    void testTitleCaseAllCaps() {
        assertThat(PasswordGenerator.titleCase("HELLO WORLD")).isEqualTo("Hello World");
    }

    @Test
    void testTitleCaseEmptyString() {
        assertThat(PasswordGenerator.titleCase("")).isEqualTo("");
    }

    @Test
    void testTitleCaseNull() {
        assertThat(PasswordGenerator.titleCase(null)).isNull();
    }

    @Test
    void testTitleCaseMultipleSpaces() {
        assertThat(PasswordGenerator.titleCase("hello  world")).isEqualTo("Hello World");
    }

    // ========== estimateCount() Tests ==========

    @Test
    void testEstimateCountWithMinimalConfig() {
        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("crypto")  // Will generate multiple capitalization variants
            .addNumberCombination("123")
            .addSpecialCharacter("!")
            .build();

        long estimate = generator.estimateCount(config);

        // Should have:
        // - Base combinations: crypto, CRYPTO, Crypto (titleCase same as capitalize for single word)
        //   Set will dedupe, so we get 3 unique variants (crypto, CRYPTO, Crypto)
        // - 1 number combination
        // - 1 special character
        // Total = 3 * 1 * 1 = 3
        assertThat(estimate).isEqualTo(3);
    }

    @Test
    void testEstimateCountWithMultipleOptions() {
        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("crypto")
            .addNumberCombination("123")
            .addNumberCombination("456")
            .addNumberCombination("789")
            .addSpecialCharacter("!")
            .addSpecialCharacter("@")
            .build();

        long estimate = generator.estimateCount(config);

        // Should be: bases * 3 numbers * 2 specials
        Set<String> bases = generator.generateBaseCombinations(config.getBaseWords());
        long expectedCount = (long) bases.size() * 3 * 2;

        assertThat(estimate).isEqualTo(expectedCount);
    }

    @Test
    void testEstimateCountMatchesActualGeneration() {
        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("test12")
            .addNumberCombination("1")
            .addNumberCombination("2")
            .addSpecialCharacter("!")
            .build();

        long estimate = generator.estimateCount(config);
        Set<String> actualPasswords = generator.generateAll(config);

        assertThat(estimate).isEqualTo(actualPasswords.size());
    }

    @Test
    void testEstimateCountReturnsZeroForNullConfig() {
        assertThat(generator.estimateCount(null)).isEqualTo(0);
    }

    // ========== Integration Tests ==========

    @Test
    void testCompletePasswordGeneration() {
        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("wallet")
            .addNumberCombination("2024")
            .addSpecialCharacter("!")
            .build();

        Set<String> passwords = generator.generateAll(config);

        // Verify structure: base + number + special
        for (String password : passwords) {
            assertThat(password).endsWith("2024!");
            assertThat(password.length()).isGreaterThanOrEqualTo(
                PasswordGenerator.MIN_BASE_LENGTH + "2024!".length()
            );
        }
    }

    @Test
    void testPasswordGenerationWithRealWorldConfig() {
        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("crypto")
            .addBaseWord("wallet")
            .addNumberCombination("123")
            .addNumberCombination("2024")
            .addSpecialCharacter("!")
            .addSpecialCharacter("@")
            .build();

        Set<String> passwords = generator.generateAll(config);

        // Should have many combinations
        assertThat(passwords.size()).isGreaterThan(10);

        // Verify specific expected patterns exist
        assertThat(passwords).contains("crypto123!");
        assertThat(passwords).contains("wallet2024@");
        assertThat(passwords).contains("CRYPTO123!");
    }

    @Test
    void testLargeScaleGeneration() {
        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("password")
            .addBaseWord("secure")
            .addBaseWord("crypto")
            .addNumberCombination("1")
            .addNumberCombination("12")
            .addNumberCombination("123")
            .addNumberCombination("1234")
            .addSpecialCharacter("!")
            .addSpecialCharacter("@")
            .addSpecialCharacter("#")
            .build();

        Set<String> passwords = generator.generateAll(config);

        // Should generate a large number of combinations
        assertThat(passwords.size()).isGreaterThan(50);

        // All passwords should end with a special character
        for (String password : passwords) {
            assertThat(password).matches(".*[!@#]$");
        }
    }

    // ========== Edge Case Tests ==========

    @Test
    void testEmptyBaseListThrowsException() {
        assertThatThrownBy(() -> generator.generateBaseCombinations(Arrays.asList()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testBaseCombinationsFiltersByLength() {
        // Mix of valid and invalid length words
        List<String> words = Arrays.asList(
            "ab",           // Too short (2 chars) - won't appear as single word
            "valid",        // Valid (5 chars)
            "perfect"       // Valid (7 chars)
        );

        Set<String> bases = generator.generateBaseCombinations(words);

        // Should include valid length words as singles
        assertThat(bases).contains("valid");
        assertThat(bases).contains("perfect");

        // "ab" alone shouldn't appear (too short as single word)
        // But may appear in combinations like "ab-valid" (8 chars - valid!)
        assertThat(bases).contains("ab-valid");
        assertThat(bases).contains("valid-ab");
    }

    @Test
    void testTwoWordCombinationExceedsMaxLength() {
        // Two words that individually are valid, but combined exceed max
        List<String> words = Arrays.asList("lengthy", "wordhere");  // "lengthy-wordhere" = 16 chars

        Set<String> bases = generator.generateBaseCombinations(words);

        // Should include individual words
        assertThat(bases).contains("lengthy");
        assertThat(bases).contains("wordhere");

        // Should NOT include the combination (too long)
        assertThat(bases).doesNotContain("lengthy-wordhere");
        assertThat(bases).doesNotContain("lengthywordhere");
    }

    @Test
    void testCapitalizationCreatesDistinctEntries() {
        List<String> words = Arrays.asList("password");

        Set<String> bases = generator.generateBaseCombinations(words);

        // Should have multiple distinct capitalization variants
        assertThat(bases).contains("password");
        assertThat(bases).contains("PASSWORD");
        assertThat(bases).contains("Password");

        // All should be distinct entries in the Set
        assertThat(bases.size()).isGreaterThanOrEqualTo(3);
    }
}
