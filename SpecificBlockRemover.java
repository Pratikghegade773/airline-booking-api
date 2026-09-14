import java.io.*;
import java.nio.file.*;
import java.util.*;

public class SpecificBlockRemover {

    public static void main(String[] args) throws Exception {
        Path projectRoot = Paths.get("src/main/java/com/airlines/go7api");
        Files.walk(projectRoot)
             .filter(p -> p.toString().endsWith(".java"))
             .forEach(file -> {
                 try {
                     String content = new String(Files.readAllBytes(file));
                     if (content.contains("// Pricing (Removed)")) {
                         System.out.println("Found block in: " + file);
                         // Define regex or literal string to replace.
                         // We can remove it line by line based on what the user gave.
                         String replacementTarget = 
                             "        response.setApiOwner(\"G7\");\n" +
                             "        // Pricing (Removed)\n" +
                             "        // } else if (booking.getTotalprice() != null) {\n" +
                             "        // \n" +
                             "        // try {\n" +
                             "        // \n" +
                             "        // } catch (NumberFormatException e) {\n" +
                             "        // \n" +
                             "        // }\n" +
                             "        // \n" +
                             "        // }\n" +
                             "        // response.setCurrency(booking.getCurrency() != null ? booking.getCurrency() :\n" +
                             "        // \"USD\"); // Removed\n" +
                             "        // response.setPaymentTimeLimit(formatDateTime(booking.getPnrttl())); // Removed";

                         // Normalize line endings to avoid \r\n vs \n issues
                         String normalizedContent = content.replace("\r\n", "\n");
                         
                         // Try to replace the exact block if found
                         if (normalizedContent.contains(replacementTarget)) {
                             normalizedContent = normalizedContent.replace(replacementTarget, "        response.setApiOwner(\"G7\");");
                             Files.write(file, normalizedContent.getBytes());
                             System.out.println("Removed block from " + file);
                         } else {
                             // Let's just remove anything from "// Pricing (Removed)" up to "// Removed"
                             int start = normalizedContent.indexOf("// Pricing (Removed)");
                             int end = normalizedContent.indexOf("// Removed", start);
                             if (start != -1 && end != -1) {
                                 // include the rest of the line
                                 int endOfLine = normalizedContent.indexOf("\n", end + 10);
                                 if (endOfLine != -1) {
                                     String sub = normalizedContent.substring(0, start) + normalizedContent.substring(endOfLine + 1);
                                     Files.write(file, sub.getBytes());
                                     System.out.println("Fuzzy removed block from " + file);
                                 }
                             }
                         }
                     }
                 } catch (Exception e) {
                     e.printStackTrace();
                 }
             });
    }
}
