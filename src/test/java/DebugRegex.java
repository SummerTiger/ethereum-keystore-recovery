import java.util.regex.Pattern;

public class DebugRegex {
    public static void main(String[] args) {
        String content = "## Base Words\n- password\n\n## Number Combinations\n- 123\n\n## Special Characters\n- !";

        System.out.println("=== TEST 1: Original Pattern ===");
        String[] sections1 = content.split("(?=^##\\s)", Pattern.MULTILINE);
        System.out.println("Sections: " + sections1.length);
        for (int i = 0; i < sections1.length; i++) {
            System.out.println("Section " + i + ": '" + sections1[i].substring(0, Math.min(20, sections1[i].length())) + "...'");
        }

        System.out.println("\n=== TEST 2: Without MULTILINE flag ===");
        String[] sections2 = content.split("(?m)(?=^##\\s)");
        System.out.println("Sections: " + sections2.length);
        for (int i = 0; i < sections2.length; i++) {
            System.out.println("Section " + i + ": '" + sections2[i].substring(0, Math.min(20, sections2[i].length())) + "...'");
        }

        System.out.println("\n=== TEST 3: Using embedded flag ===");
        String[] sections3 = content.split("(?m)(?=^## )");
        System.out.println("Sections: " + sections3.length);
        for (int i = 0; i < sections3.length; i++) {
            System.out.println("Section " + i + ": '" + sections3[i].substring(0, Math.min(20, sections3[i].length())) + "...'");
        }
    }
}
