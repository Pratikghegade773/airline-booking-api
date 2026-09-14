import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

public class CodeCleaner {

    public static void main(String[] args) throws Exception {
        Path projectRoot = Paths.get("src/main/java/com/airlines/go7api");
        List<Path> javaFiles = Files.walk(projectRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .collect(Collectors.toList());

        for (Path file : javaFiles) {
            cleanFile(file);
        }
        System.out.println("Code cleanup completed.");
    }

    private static void cleanFile(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        List<String> cleanedLines = new ArrayList<>();
        
        // Find all imports
        List<String> imports = new ArrayList<>();
        for (String line : lines) {
            if (line.trim().startsWith("import ") && !line.trim().contains(".*")) {
                imports.add(line.trim());
            }
        }
        
        // Combine all lines to easily check for usage
        String fullContent = String.join("\n", lines);
        // Strip out imports from the content string so we don't match the import itself
        String contentWithoutImports = fullContent.replaceAll("import\\s+[\\w\\.]+;", "");
        
        Set<String> unusedImports = new HashSet<>();
        for (String imp : imports) {
            String className = imp.substring(imp.lastIndexOf('.') + 1, imp.length() - 1);
            // Search for the class name as a whole word
            Pattern p = Pattern.compile("\\b" + className + "\\b");
            Matcher m = p.matcher(contentWithoutImports);
            if (!m.find()) {
                unusedImports.add(imp);
            }
        }

        boolean changed = false;
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Remove unused imports
            if (trimmed.startsWith("import ") && unusedImports.contains(trimmed)) {
                changed = true;
                continue; // Skip this line
            }
            
            // Remove commented out code (heuristic)
            if (trimmed.startsWith("//")) {
                String commentContent = trimmed.substring(2).trim();
                // If it ends with semicolon or has braces, it's likely commented code
                if ((commentContent.endsWith(";") && !commentContent.toLowerCase().contains("note:") && !commentContent.toLowerCase().contains("todo")) ||
                    commentContent.startsWith("public ") || commentContent.startsWith("private ") || 
                    commentContent.startsWith("import ") || commentContent.startsWith("class ") ||
                    commentContent.startsWith("if (") || commentContent.startsWith("for (")) {
                    changed = true;
                    continue; // Skip this line
                }
            }

            cleanedLines.add(line);
        }

        if (changed) {
            Files.write(file, cleanedLines);
            System.out.println("Cleaned: " + file.getFileName());
        }
    }
}
