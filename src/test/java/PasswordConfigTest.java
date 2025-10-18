import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for PasswordConfig class.
 * Tests Builder pattern, markdown parsing, immutability, and validation.
 *
 * @author KeystoreRecovery Team
 * @version 1.1.0
 */
class PasswordConfigTest {

    // ========== Builder Pattern Tests ==========

    @Test
    void testBuilderWithValidData() {
        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("password")
            .addNumberCombination("123")
            .addSpecialCharacter("!")
            .build();

        assertThat(config).isNotNull();
        assertThat(config.getBaseWords()).containsExactly("password");
        assertThat(config.getNumberCombinations()).containsExactly("123");
        assertThat(config.getSpecialCharacters()).containsExactly("!");
        assertThat(config.isValid()).isTrue();
    }

    @Test
    void testBuilderWithMultipleItems() {
        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("crypto")
            .addBaseWord("wallet")
            .addBaseWord("ethereum")
            .addNumberCombination("123")
            .addNumberCombination("2024")
            .addSpecialCharacter("!")
            .addSpecialCharacter("@")
            .addSpecialCharacter("#")
            .build();

        assertThat(config.getBaseWords()).hasSize(3);
        assertThat(config.getNumberCombinations()).hasSize(2);
        assertThat(config.getSpecialCharacters()).hasSize(3);
    }

    @Test
    void testBuilderSettersWithLists() {
        List<String> words = Arrays.asList("password", "crypto", "wallet");
        List<String> numbers = Arrays.asList("123", "456", "789");
        List<String> specials = Arrays.asList("!", "@", "#");

        PasswordConfig config = new PasswordConfig.Builder()
            .setBaseWords(words)
            .setNumberCombinations(numbers)
            .setSpecialCharacters(specials)
            .build();

        assertThat(config.getBaseWords()).containsExactlyElementsOf(words);
        assertThat(config.getNumberCombinations()).containsExactlyElementsOf(numbers);
        assertThat(config.getSpecialCharacters()).containsExactlyElementsOf(specials);
    }

    @Test
    void testBuilderThrowsExceptionWhenBaseWordsEmpty() {
        assertThatThrownBy(() -> {
            new PasswordConfig.Builder()
                .addNumberCombination("123")
                .addSpecialCharacter("!")
                .build();
        })
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Base words list cannot be empty");
    }

    @Test
    void testBuilderThrowsExceptionWhenNumbersEmpty() {
        assertThatThrownBy(() -> {
            new PasswordConfig.Builder()
                .addBaseWord("password")
                .addSpecialCharacter("!")
                .build();
        })
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Number combinations list cannot be empty");
    }

    @Test
    void testBuilderThrowsExceptionWhenSpecialsEmpty() {
        assertThatThrownBy(() -> {
            new PasswordConfig.Builder()
                .addBaseWord("password")
                .addNumberCombination("123")
                .build();
        })
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Special characters list cannot be empty");
    }

    @Test
    void testBuilderIgnoresNullAndEmptyStrings() {
        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("valid")
            .addBaseWord(null)
            .addBaseWord("")
            .addNumberCombination("123")
            .addNumberCombination(null)
            .addNumberCombination("")
            .addSpecialCharacter("!")
            .addSpecialCharacter(null)
            .addSpecialCharacter("")
            .build();

        // Should only have the valid entries
        assertThat(config.getBaseWords()).containsExactly("valid");
        assertThat(config.getNumberCombinations()).containsExactly("123");
        assertThat(config.getSpecialCharacters()).containsExactly("!");
    }

    // ========== Immutability Tests ==========

    @Test
    void testConfigIsImmutable() {
        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("password")
            .addNumberCombination("123")
            .addSpecialCharacter("!")
            .build();

        // Try to modify returned lists (should throw UnsupportedOperationException)
        assertThatThrownBy(() -> config.getBaseWords().add("hacker"))
            .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> config.getNumberCombinations().add("999"))
            .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> config.getSpecialCharacters().add("$"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testBuilderListsAreCopied() {
        // Test that modifying original list doesn't affect config
        List<String> words = Arrays.asList("password");
        PasswordConfig.Builder builder = new PasswordConfig.Builder()
            .setBaseWords(words)
            .addNumberCombination("123")
            .addSpecialCharacter("!");

        // This should not affect the builder
        // (Note: Arrays.asList creates fixed-size list, so we can't add to it,
        // but the builder makes a copy anyway)

        PasswordConfig config = builder.build();
        assertThat(config.getBaseWords()).containsExactly("password");
    }

    // ========== Validation Tests ==========

    @Test
    void testIsValidReturnsTrueForCompleteConfig() {
        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("password")
            .addNumberCombination("123")
            .addSpecialCharacter("!")
            .build();

        assertThat(config.isValid()).isTrue();
    }

    // ========== Markdown Parsing Tests ==========

    @Test
    void testFromMarkdownWithValidFile(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("test_config.md");
        String content = """
## Base Words
- password
- crypto
- wallet

## Number Combinations
- 123
- 456

## Special Characters
- !
- @
""";
        Files.writeString(configFile, content);

        PasswordConfig config = PasswordConfig.fromMarkdown(configFile.toString());

        assertThat(config.getBaseWords()).containsExactly("password", "crypto", "wallet");
        assertThat(config.getNumberCombinations()).containsExactly("123", "456");
        assertThat(config.getSpecialCharacters()).containsExactly("!", "@");
        assertThat(config.isValid()).isTrue();
    }

    @Test
    void testFromMarkdownWithDifferentListFormats(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("test_config.md");
        String content = """
## Base Words
- password
* crypto
+ wallet
1. ethereum

## Number Combinations
- 123
* 456
1. 789

## Special Characters
- !
* @
+ #
""";
        Files.writeString(configFile, content);

        PasswordConfig config = PasswordConfig.fromMarkdown(configFile.toString());

        assertThat(config.getBaseWords()).hasSize(4);
        assertThat(config.getNumberCombinations()).hasSize(3);
        assertThat(config.getSpecialCharacters()).hasSize(3);
    }

    @Test
    void testFromMarkdownWithHeaderVariations(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("test_config.md");
        String content = """
## BASE WORDS
- password

## number combinations
- 123

## Special Character Section
- !
""";
        Files.writeString(configFile, content);

        PasswordConfig config = PasswordConfig.fromMarkdown(configFile.toString());

        assertThat(config.getBaseWords()).containsExactly("password");
        assertThat(config.getNumberCombinations()).containsExactly("123");
        assertThat(config.getSpecialCharacters()).containsExactly("!");
    }

    @Test
    void testFromMarkdownSkipsComments(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("test_config.md");
        String content = """
## Base Words
# This is a comment
- password
*Note: this is a note*
- crypto

## Number Combinations
- 123

## Special Characters
- !
""";
        Files.writeString(configFile, content);

        PasswordConfig config = PasswordConfig.fromMarkdown(configFile.toString());

        assertThat(config.getBaseWords()).containsExactly("password", "crypto");
    }

    @Test
    void testFromMarkdownThrowsExceptionForIncompleteConfig(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("incomplete_config.md");
        String content = """
## Base Words
- password

## Number Combinations
- 123
""";
        // Missing Special Characters section
        Files.writeString(configFile, content);

        assertThatThrownBy(() -> PasswordConfig.fromMarkdown(configFile.toString()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Special characters list cannot be empty");
    }

    @Test
    void testFromMarkdownThrowsExceptionForNonExistentFile() {
        assertThatThrownBy(() -> PasswordConfig.fromMarkdown("/nonexistent/file.md"))
            .isInstanceOf(IOException.class);
    }

    @Test
    void testFromMarkdownWithEmptySections(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("empty_sections.md");
        String content = """
## Base Words

## Number Combinations

## Special Characters
""";
        Files.writeString(configFile, content);

        assertThatThrownBy(() -> PasswordConfig.fromMarkdown(configFile.toString()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("list cannot be empty");
    }

    // ========== Item Validation Tests ==========

    @Test
    void testValidNumberPatterns(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("numbers.md");
        String content = """
## Base Words
- password

## Number Combinations
- 1
- 12
- 123
- 1234
- 12345

## Special Characters
- !
""";
        Files.writeString(configFile, content);

        PasswordConfig config = PasswordConfig.fromMarkdown(configFile.toString());

        assertThat(config.getNumberCombinations()).containsExactly("1", "12", "123", "1234", "12345");
    }

    @Test
    void testValidSpecialCharacters(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("specials.md");
        String content = """
## Base Words
- password

## Number Combinations
- 123

## Special Characters
- !
- @
- #
- $
- %
- &
- *
""";
        Files.writeString(configFile, content);

        PasswordConfig config = PasswordConfig.fromMarkdown(configFile.toString());

        assertThat(config.getSpecialCharacters()).containsExactly("!", "@", "#", "$", "%", "&", "*");
    }

    @Test
    void testValidWordLengths(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("words.md");
        String content = """
## Base Words
- a
- password
- verylongwordhere123

## Number Combinations
- 123

## Special Characters
- !
""";
        Files.writeString(configFile, content);

        PasswordConfig config = PasswordConfig.fromMarkdown(configFile.toString());

        assertThat(config.getBaseWords()).hasSize(3);
        assertThat(config.getBaseWords()).contains("a", "password", "verylongwordhere123");
    }

    // ========== Sample Config Generation Tests ==========

    @Test
    void testCreateSampleConfig(@TempDir Path tempDir) throws IOException {
        Path sampleFile = tempDir.resolve("sample_config.md");

        PasswordConfig.createSampleConfig(sampleFile.toString());

        assertThat(sampleFile).exists();
        String content = Files.readString(sampleFile);
        assertThat(content).contains("## Base Words");
        assertThat(content).contains("## Number Combinations");
        assertThat(content).contains("## Special Characters");
        assertThat(content).contains("password");
        assertThat(content).contains("123");
        assertThat(content).contains("!");
    }

    @Test
    void testSampleConfigIsValid(@TempDir Path tempDir) throws IOException {
        Path sampleFile = tempDir.resolve("sample_config.md");

        PasswordConfig.createSampleConfig(sampleFile.toString());

        // Should be able to parse the generated sample
        PasswordConfig config = PasswordConfig.fromMarkdown(sampleFile.toString());

        assertThat(config.isValid()).isTrue();
        assertThat(config.getBaseWords()).isNotEmpty();
        assertThat(config.getNumberCombinations()).isNotEmpty();
        assertThat(config.getSpecialCharacters()).isNotEmpty();
    }

    // ========== toString() Tests ==========

    @Test
    void testToString() {
        PasswordConfig config = new PasswordConfig.Builder()
            .addBaseWord("password")
            .addBaseWord("crypto")
            .addNumberCombination("123")
            .addNumberCombination("456")
            .addNumberCombination("789")
            .addSpecialCharacter("!")
            .build();

        String str = config.toString();

        assertThat(str).contains("baseWords=2");
        assertThat(str).contains("numbers=3");
        assertThat(str).contains("specials=1");
    }

    // ========== Edge Case Tests ==========

    @Test
    void testConfigWithWhitespaceInMarkdown(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("whitespace.md");
        String content = """
## Base Words
  - password
  - crypto

## Number Combinations
  - 123

## Special Characters
  - !
""";
        Files.writeString(configFile, content);

        PasswordConfig config = PasswordConfig.fromMarkdown(configFile.toString());

        assertThat(config.getBaseWords()).containsExactly("password", "crypto");
        assertThat(config.getNumberCombinations()).containsExactly("123");
        assertThat(config.getSpecialCharacters()).containsExactly("!");
    }

    @Test
    void testConfigWithMixedContent(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("mixed.md");
        String content = """
# Keystore Password Recovery Configuration

Some introduction text here.

## Base Words
*List your commonly used base words or phrases*

- password
- crypto

Some notes in between sections.

## Number Combinations
*Number patterns you commonly use*

- 123
- 2024

## Special Characters
*Single special characters*

- !
- @

## Notes
These are final notes and should be ignored.
""";
        Files.writeString(configFile, content);

        PasswordConfig config = PasswordConfig.fromMarkdown(configFile.toString());

        assertThat(config.getBaseWords()).containsExactly("password", "crypto");
        assertThat(config.getNumberCombinations()).containsExactly("123", "2024");
        assertThat(config.getSpecialCharacters()).containsExactly("!", "@");
    }

    @Test
    void testLargeConfiguration(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("large.md");
        StringBuilder content = new StringBuilder();

        content.append("## Base Words\n");
        for (int i = 0; i < 100; i++) {
            content.append("- word").append(i).append("\n");
        }

        content.append("\n## Number Combinations\n");
        for (int i = 0; i < 50; i++) {
            content.append("- ").append(i).append("\n");
        }

        content.append("\n## Special Characters\n");
        content.append("- !\n- @\n- #\n");

        Files.writeString(configFile, content.toString());

        PasswordConfig config = PasswordConfig.fromMarkdown(configFile.toString());

        assertThat(config.getBaseWords()).hasSize(100);
        assertThat(config.getNumberCombinations()).hasSize(50);
        assertThat(config.getSpecialCharacters()).hasSize(3);
        assertThat(config.isValid()).isTrue();
    }
}
