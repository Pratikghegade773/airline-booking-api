import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RemoveSpecificImport {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java/com/airlines/go7api");
        processDirectory(dir);
        System.out.println("Done.");
    }

    private static void processDirectory(File dir) throws Exception {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                processDirectory(file);
            } else if (file.getName().endsWith(".java")) {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                if (content.contains("import com.airlines.go7api.requestdto.common.*;")) {
                    
                    // Check if they actually use it
                    boolean actuallyUses = content.contains("PaxReqDto") || 
                                           content.contains("PaymentInformationReqDto") || 
                                           content.contains("ChangeOfferReqDto") || 
                                           content.contains("ChangeOrderItemReqDto");
                                           
                    if (!actuallyUses) {
                        String[] lines = content.split("\\r?\\n");
                        List<String> newLines = new ArrayList<>();
                        for (String line : lines) {
                            if (!line.trim().equals("import com.airlines.go7api.requestdto.common.*;")) {
                                newLines.add(line);
                            }
                        }
                        String newContent = String.join(System.lineSeparator(), newLines);
                        Files.write(file.toPath(), newContent.getBytes(StandardCharsets.UTF_8));
                        System.out.println("Removed unused wildcard import from: " + file.getName());
                    }
                }
            }
        }
    }
}
