import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FixImports2 {
    public static void main(String[] args) throws Exception {
        Path responseDir = Paths.get("src/main/java/com/airlines/go7api/response");
        Path responseGo7Dir = Paths.get("src/main/java/com/airlines/go7api/responsego7");
        
        List<Path> dirs = Arrays.asList(responseDir, responseGo7Dir);
        
        for (Path dir : dirs) {
            Files.walk(dir)
                 .filter(p -> p.toString().endsWith(".java"))
                 .forEach(file -> {
                     try {
                         List<String> lines = Files.readAllLines(file);
                         boolean changed = false;
                         List<String> newLines = new ArrayList<>();
                         boolean importAdded = false;
                         
                         for (String line : lines) {
                             if (line.trim().equals("import com.airlines.go7api.responsego7.common.*;")) {
                                 changed = true;
                                 continue;
                             }
                             newLines.add(line);
                             
                             if (!importAdded && line.trim().startsWith("package ")) {
                                 newLines.add("");
                                 newLines.add("import com.airlines.go7api.responsego7.common.*;");
                                 importAdded = true;
                             }
                         }
                         
                         if (changed) {
                             Files.write(file, newLines);
                             System.out.println("Fixed imports in: " + file.getFileName());
                         }
                     } catch (Exception e) { e.printStackTrace(); }
                 });
        }
    }
}
