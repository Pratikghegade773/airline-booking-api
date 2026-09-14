import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemoveUnusedImports {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java/com/airlines/go7api");
        processDirectory(dir);
        System.out.println("Unused imports removed.");
    }

    private static void processDirectory(File dir) throws Exception {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                processDirectory(file);
            } else if (file.getName().endsWith(".java")) {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                String[] lines = content.split("\\r?\\n");
                
                List<String> newLines = new ArrayList<>();
                boolean modified = false;
                
                Pattern importPattern = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w\\.]+)\\s*;");
                
                for (String line : lines) {
                    Matcher m = importPattern.matcher(line);
                    if (m.find()) {
                        String fullImport = m.group(1);
                        // skip wildcard imports
                        if (fullImport.endsWith(".*")) {
                            newLines.add(line);
                            continue;
                        }
                        
                        // Extract class name
                        int lastDot = fullImport.lastIndexOf('.');
                        String className = lastDot == -1 ? fullImport : fullImport.substring(lastDot + 1);
                        
                        // Check if className is used in the file outside of the import statement
                        // We use a regex with word boundaries
                        Pattern usagePattern = Pattern.compile("\\b" + className + "\\b");
                        Matcher usageMatcher = usagePattern.matcher(content);
                        
                        int count = 0;
                        while (usageMatcher.find()) {
                            count++;
                        }
                        
                        // count will be at least 1 because of the import statement itself
                        // If it's used elsewhere, count > 1 (or > 0 if we stripped imports first, but we didn't)
                        // Actually, wait, if the class name appears in another import, count could be > 1.
                        // A safer way is to check if count > 1. 
                        if (count <= 1) {
                            // Unused import!
                            modified = true;
                            continue; // skip adding this line
                        }
                    }
                    newLines.add(line);
                }
                
                if (modified) {
                    String newContent = String.join(System.lineSeparator(), newLines);
                    Files.write(file.toPath(), newContent.getBytes(StandardCharsets.UTF_8));
                    System.out.println("Cleaned imports in: " + file.getName());
                }
            }
        }
    }
}
