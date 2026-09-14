import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class Splitter {
    public static void main(String[] args) throws Exception {
        String sourcePath = "src/main/java/com/airlines/go7api/responsedto/ChangeSeatRspDto.java";
        String content = new String(Files.readAllBytes(Paths.get(sourcePath)));
        
        String commonPackageDir = "src/main/java/com/airlines/go7api/responsedto/common";
        Files.createDirectories(Paths.get(commonPackageDir));
        
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
        
        for (String cls : classes) {
            String regex = "(?s)(    @(Data|NoArgsConstructor|AllArgsConstructor|JsonInclude).*?public static class " + cls + " \\{.*?    \\})\\s*(?=\\n    @|\\n\\})";
            Matcher matcher = Pattern.compile(regex).matcher(content);
            if (matcher.find()) {
                String classContent = matcher.group(1);
                
                // Adjust indentations
                classContent = classContent.replaceAll("(?m)^    ", "");
                
                // Make class public not static
                classContent = classContent.replace("public static class", "public class");
                
                String fileContent = "package com.airlines.go7api.responsedto.common;\n\n" +
                                     "import com.fasterxml.jackson.annotation.JsonIgnoreProperties;\n" +
                                     "import com.fasterxml.jackson.annotation.JsonInclude;\n" +
                                     "import lombok.AllArgsConstructor;\n" +
                                     "import lombok.Data;\n" +
                                     "import lombok.NoArgsConstructor;\n\n" +
                                     "import java.math.BigDecimal;\n" +
                                     "import java.math.BigInteger;\n" +
                                     "import java.util.List;\n\n" +
                                     classContent + "\n";
                
                Files.write(Paths.get(commonPackageDir, cls + ".java"), fileContent.getBytes());
                System.out.println("Extracted " + cls);
                
                // Replace in source content to verify
                content = content.replace(matcher.group(0), "");
            }
        }
        
        // Write the stripped DTO back to a test file
        // content = content.replaceAll("import .*?;\n", ""); 
        // We will do imports manually.
    }
}
