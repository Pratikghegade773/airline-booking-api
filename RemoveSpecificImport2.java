import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RemoveSpecificImport2 {
    public static void main(String[] args) throws Exception {
        String[] targets = {
            "src/main/java/com/airlines/go7api/response/ChangeSeatResponse.java",
            "src/main/java/com/airlines/go7api/response/ChangeServiceResponse.java"
        };
        
        for (String t : targets) {
            File f = new File(t);
            if (!f.exists()) continue;
            
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            String[] lines = content.split("\\r?\\n");
            List<String> newLines = new ArrayList<>();
            for (String line : lines) {
                if (!line.trim().equals("import com.airlines.go7api.requestdto.common.*;")) {
                    newLines.add(line);
                }
            }
            String newContent = String.join(System.lineSeparator(), newLines);
            Files.write(f.toPath(), newContent.getBytes(StandardCharsets.UTF_8));
            System.out.println("Removed from " + f.getName());
        }
    }
}
