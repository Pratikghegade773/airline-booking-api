import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

public class Stripper {
    public static void main(String[] args) throws Exception {
        String dir = "src/main/java/com/airlines/go7api/responsedto";
        
        List<Path> files = Files.list(Paths.get(dir))
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".java"))
            .collect(Collectors.toList());
            
        String[] classes = {
            "OtherServiceInformation",
            "OD",
            "BookingReferences",
            "PaxDetailDTO",
            "SpecialServiceRequest",
            "TicketDocInfoDTO",
            "EMDInfoDTO",
            "Service",
            "OrderItemsDTO",
            "PaymentsDTO",
            "PriceClass"
        };
        
        for (Path file : files) {
            String content = new String(Files.readAllBytes(file));
            boolean changed = false;
            
            for (String cls : classes) {
                // More permissive regex to catch any spacing and annotations
                String regex = "(?s)\\s*@[A-Za-z]+(?:\\([^)]*\\))?\\s*(?:@[A-Za-z]+(?:\\([^)]*\\))?\\s*)*public static class " + cls + " \\{.*?(?=\\n\\s*(?:@[A-Za-z]|public|\\z|\\}\\s*\\z|\\}\\s*\\n\\s*\\}*))";
                // Even simpler: find "public static class ClassName {" and balance the braces.
                // But balancing braces with regex is hard. 
                // Let's write a simple brace balancer string matching.
            }
            
            // Let's implement a manual brace matching to remove the static nested classes.
            for (String cls : classes) {
                String signature = "public static class " + cls;
                int start = content.indexOf(signature);
                while (start != -1) {
                    // find start of annotations
                    int annotationStart = start;
                    while (annotationStart > 0) {
                        int prevLineEnd = content.lastIndexOf('\n', annotationStart - 1);
                        String line = content.substring(prevLineEnd + 1, annotationStart).trim();
                        if (line.startsWith("@") || line.isEmpty()) {
                            annotationStart = prevLineEnd;
                        } else {
                            break;
                        }
                    }
                    if (annotationStart < 0) annotationStart = 0;
                    
                    int braceStart = content.indexOf('{', start);
                    if (braceStart == -1) break;
                    
                    int braces = 1;
                    int i = braceStart + 1;
                    for (; i < content.length(); i++) {
                        if (content.charAt(i) == '{') braces++;
                        else if (content.charAt(i) == '}') braces--;
                        
                        if (braces == 0) {
                            break;
                        }
                    }
                    
                    if (braces == 0) {
                        content = content.substring(0, annotationStart) + content.substring(i + 1);
                        changed = true;
                        start = content.indexOf(signature); // check again
                    } else {
                        break; // malformed
                    }
                }
            }
            
            if (changed) {
                // insert import
                if (!content.contains("import com.airlines.go7api.responsedto.common.*;")) {
                    content = content.replace("package com.airlines.go7api.responsedto;", "package com.airlines.go7api.responsedto;\n\nimport com.airlines.go7api.responsedto.common.*;");
                }
                Files.write(file, content.getBytes());
                System.out.println("Stripped nested classes from " + file.getFileName());
            }
        }
    }
}
