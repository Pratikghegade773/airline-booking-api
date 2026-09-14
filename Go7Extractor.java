import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Go7Extractor {
    public static void main(String[] args) throws Exception {
        Path sourceFile = Paths.get("src/main/java/com/airlines/go7api/responsego7/ChangePaymentRspGo7Dto.java");
        Path commonDir = Paths.get("src/main/java/com/airlines/go7api/responsego7/common");
        Files.createDirectories(commonDir);
        
        String content = new String(Files.readAllBytes(sourceFile));
        
        // Map of class names to their exact contents, we will extract them manually or via script.
        // It's safer to extract them via a quick script.
        
        String[] classesToExtract = {
            "Details", "Aerocrs", "Booking", "Items", "Flights", "Flight", 
            "Taxes", "Passengers", "Passenger", "Remarks", "Remark", 
            "BalanceInformation", "Seat"
        };
        
        for (String cls : classesToExtract) {
            // Find the start of the class definition
            String startMarker = "    public static class " + cls + " {";
            int startIndex = content.indexOf(startMarker);
            if (startIndex == -1) {
                System.out.println("Could not find class " + cls);
                continue;
            }
            
            // Find annotations before it
            int annotationsStart = content.lastIndexOf("    @Data", startIndex);
            if (annotationsStart == -1) annotationsStart = startIndex;
            
            // Find the end of the class. Count braces.
            int braceCount = 0;
            int endIndex = -1;
            boolean started = false;
            
            for (int i = startIndex; i < content.length(); i++) {
                if (content.charAt(i) == '{') {
                    braceCount++;
                    started = true;
                } else if (content.charAt(i) == '}') {
                    braceCount--;
                    if (started && braceCount == 0) {
                        endIndex = i + 1;
                        break;
                    }
                }
            }
            
            if (endIndex != -1) {
                String classContent = content.substring(annotationsStart, endIndex);
                
                // Adjust class string to be public top-level
                classContent = classContent.replace("    public static class " + cls, "public class " + cls);
                
                // Clean up leading spaces
                classContent = classContent.replace("\n    ", "\n");
                if (classContent.startsWith("    @")) {
                    classContent = classContent.substring(4);
                }
                
                StringBuilder newFile = new StringBuilder();
                newFile.append("package com.airlines.go7api.responsego7.common;\n\n");
                
                // Add common imports
                newFile.append("import com.fasterxml.jackson.annotation.JsonInclude;\n");
                newFile.append("import com.fasterxml.jackson.annotation.JsonProperty;\n");
                newFile.append("import lombok.AllArgsConstructor;\n");
                newFile.append("import lombok.Data;\n");
                newFile.append("import lombok.NoArgsConstructor;\n");
                newFile.append("import java.math.BigDecimal;\n");
                newFile.append("import java.util.List;\n\n");
                
                newFile.append(classContent);
                
                Path targetFile = commonDir.resolve(cls + ".java");
                Files.write(targetFile, newFile.toString().getBytes());
                System.out.println("Extracted: " + cls + ".java");
            }
        }
    }
}
