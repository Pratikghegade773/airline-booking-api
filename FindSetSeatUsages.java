import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class FindSetSeatUsages {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java/com/airlines/go7api");
        processDirectory(dir);
    }

    private static void processDirectory(File dir) throws Exception {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                processDirectory(file);
            } else if (file.getName().endsWith(".java")) {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                String[] lines = content.split("\\r?\\n");
                for (int i = 0; i < lines.length; i++) {
                    if (lines[i].contains("setSeat(")) {
                        System.out.println(file.getName() + ":" + (i + 1) + ": " + lines[i].trim());
                    }
                }
            }
        }
    }
}
