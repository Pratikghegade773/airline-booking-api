import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class FindClasses2 {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java/com/airlines/go7api/responsedto/common");
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                if (f.getName().endsWith(".java")) {
                    String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                    if (content.contains("class PhoneDTO")) {
                        System.out.println("Found PhoneDTO in " + f.getName());
                    }
                }
            }
        }
    }
}
