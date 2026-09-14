import java.io.File;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FindStrayDTOs {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java/com/airlines/go7api/response");
        Pattern p = Pattern.compile("com\\.airlines\\.go7api\\.responsego7\\.([A-Z]\\w+)");

        for (File file : dir.listFiles()) {
            if (file.getName().endsWith(".java")) {
                String content = new String(Files.readAllBytes(file.toPath()));
                Matcher m = p.matcher(content);
                while (m.find()) {
                    String className = m.group(1);
                    // Ignore the root DTOs themselves
                    if (!className.endsWith("Dto")) {
                        System.out.println("Found stray DTO reference: " + m.group(0) + " in " + file.getName());
                    }
                }
            }
        }
    }
}
