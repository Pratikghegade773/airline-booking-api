import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class FindTaxFields {
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
                if (content.contains("tax_4")) {
                    System.out.println("Found tax_4 in: " + file.getAbsolutePath());
                }
            }
        }
    }
}
