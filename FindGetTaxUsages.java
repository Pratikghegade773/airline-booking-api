import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class FindGetTaxUsages {
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
                boolean modified = false;
                
                for (int i = 0; i < lines.length; i++) {
                    if (lines[i].contains("getTax_4()")) {
                        lines[i] = lines[i].replace("getTax_4()", "getTax4()");
                        modified = true;
                    }
                    if (lines[i].contains("getTax_1()")) {
                        lines[i] = lines[i].replace("getTax_1()", "getTax1()");
                        modified = true;
                    }
                    if (lines[i].contains("setTax_4(")) {
                        lines[i] = lines[i].replace("setTax_4(", "setTax4(");
                        modified = true;
                    }
                    if (lines[i].contains("setTax_1(")) {
                        lines[i] = lines[i].replace("setTax_1(", "setTax1(");
                        modified = true;
                    }
                }
                
                if (modified) {
                    Files.write(file.toPath(), String.join(System.lineSeparator(), lines).getBytes(StandardCharsets.UTF_8));
                    System.out.println("Renamed tax getters/setters in: " + file.getName());
                }
            }
        }
    }
}
