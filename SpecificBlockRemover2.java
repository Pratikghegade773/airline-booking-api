import java.io.*;
import java.nio.file.*;

public class SpecificBlockRemover2 {
    public static void main(String[] args) throws Exception {
        Path projectRoot = Paths.get("src/main/java/com/airlines/go7api");
        Files.walk(projectRoot)
             .filter(p -> p.toString().endsWith(".java"))
             .forEach(file -> {
                 try {
                     String content = new String(Files.readAllBytes(file));
                     if (content.contains("// Remarks (Removed as per request)")) {
                         System.out.println("Found block in: " + file);
                         String normalizedContent = content.replace("\r\n", "\n");
                         
                         int start = normalizedContent.indexOf("// Remarks (Removed as per request)");
                         int end = normalizedContent.indexOf("// response.setPriceClassList(priceClasses); // Removed");
                         
                         if (start != -1 && end != -1) {
                             int endOfLine = normalizedContent.indexOf("\n", end);
                             if (endOfLine != -1) {
                                 String sub = normalizedContent.substring(0, start) + normalizedContent.substring(endOfLine + 1);
                                 Files.write(file, sub.getBytes());
                                 System.out.println("Removed second block from " + file);
                             }
                         }
                     }
                 } catch (Exception e) {
                     e.printStackTrace();
                 }
             });
    }
}
