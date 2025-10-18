import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class DebugParser {
    public static void main(String[] args) throws Exception {
        String content = Files.readString(Paths.get("/tmp/simple_test.md"));
        System.out.println("=== FILE CONTENT ===");
        System.out.println(content);
        System.out.println("=== SPLITTING ===");

        String[] sections = content.split("(?=^##\\s)", Pattern.MULTILINE);
        System.out.println("Found " + sections.length + " sections");

        for (int i = 0; i < sections.length; i++) {
            System.out.println("\n=== SECTION " + i + " ===");
            System.out.println("'" + sections[i] + "'");
            System.out.println("---");

            String[] lines = sections[i].split("\n");
            if (lines.length > 0) {
                String header = lines[0].toLowerCase();
                System.out.println("Header: '" + header + "'");
                System.out.println("Contains 'number': " + header.contains("number"));
                System.out.println("Contains 'digit': " + header.contains("digit"));
            }
        }
    }
}
