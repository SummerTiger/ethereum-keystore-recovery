import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DebugMarkdownParsing {
    public static void main(String[] args) {
        String[] testLines = {
            "- password",
            "- 123",
            "- !",
            "* password",
            "+ wallet",
            "1. ethereum"
        };

        Pattern pattern = Pattern.compile("^[-*+\\d.]\\s+(.+)");

        for (String line : testLines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String extracted = matcher.group(1).trim();
                System.out.println("Line: '" + line + "' => Extracted: '" + extracted + "'");
            } else {
                System.out.println("Line: '" + line + "' => NO MATCH");
            }
        }
    }
}
