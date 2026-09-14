import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class CheckImports {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java/com/airlines/go7api");
        processDirectory(dir);
        System.out.println("Done checking imports.");
    }

    private static void processDirectory(File dir) throws Exception {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                processDirectory(file);
            } else if (file.getName().endsWith(".java")) {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                String[] lines = content.split("\\r?\\n");
                boolean modified = false;
                for (int i = 0; i < lines.length; i++) {
                    if (lines[i].startsWith("import ") && lines[i].contains("BaggageAllowance")) {
                        if (lines[i].contains("TicketDocInfoDTO") || lines[i].contains("OrderItemsDTO")) {
                            lines[i] = "import com.airlines.go7api.responsedto.common.BaggageAllowance;";
                            modified = true;
                        }
                    }
                }
                if (modified) {
                    Files.write(file.toPath(), String.join(System.lineSeparator(), lines).getBytes(StandardCharsets.UTF_8));
                    System.out.println("Fixed import in: " + file.getName());
                }
            }
        }
    }
}
