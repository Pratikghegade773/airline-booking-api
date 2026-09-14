import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class PrintGetItems {
    public static void main(String[] args) throws Exception {
        File file = new File("src/main/java/com/airlines/go7api/controller/Go7Controller.java");
        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

        String[] lines = content.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("getItems()")) {
                System.out.println("Line " + (i + 1) + ": " + lines[i].trim());
            }
        }
    }
}
