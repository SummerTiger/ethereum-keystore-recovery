public class TestParser {
    public static void main(String[] args) {
        try {
            PasswordConfig config = PasswordConfig.fromMarkdown("/tmp/simple_test.md");
            System.out.println("Base words: " + config.getBaseWords());
            System.out.println("Numbers: " + config.getNumberCombinations());
            System.out.println("Specials: " + config.getSpecialCharacters());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
