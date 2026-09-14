import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class AddJsonIgnoreToCommon {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java/com/airlines/go7api/responsego7/common");
        for (File file : dir.listFiles()) {
            if (file.getName().endsWith(".java")) {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                if (!content.contains("@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)")) {
                    // insert before public class
                    int classIdx = content.indexOf("public class ");
                    if (classIdx != -1) {
                        content = content.substring(0, classIdx) + 
                                  "@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)\n" + 
                                  content.substring(classIdx);
                        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
                        System.out.println("Added to: " + file.getName());
                    }
                }
            }
        }
    }
}
