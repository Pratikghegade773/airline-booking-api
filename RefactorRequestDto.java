import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RefactorRequestDto {
    public static void main(String[] args) throws Exception {
        File commonDir = new File("src/main/java/com/airlines/go7api/requestdto/common");
        commonDir.mkdirs();
        
        // 1. Create PaymentInformationReqDto.java
        String paymentInfoContent = "package com.airlines.go7api.requestdto.common;\n\n" +
            "import lombok.AllArgsConstructor;\n" +
            "import lombok.Data;\n" +
            "import lombok.NoArgsConstructor;\n" +
            "import java.math.BigDecimal;\n\n" +
            "@Data\n" +
            "@NoArgsConstructor\n" +
            "@AllArgsConstructor\n" +
            "public class PaymentInformationReqDto {\n" +
            "    private String cardCode;\n" +
            "    private String cardNumber;\n" +
            "    private String seriesCode;\n" +
            "    private String cardHolderName;\n" +
            "    private String expiration;\n" +
            "    private Address address;\n" +
            "    private String currencyCode;\n" +
            "    private BigDecimal amount;\n\n" +
            "    @Data\n" +
            "    @NoArgsConstructor\n" +
            "    @AllArgsConstructor\n" +
            "    public static class Address {\n" +
            "        private String street;\n" +
            "        private String city;\n" +
            "        private String state;\n" +
            "        private String postalCode;\n" +
            "        private String countryCode;\n" +
            "        private String country;\n" +
            "    }\n" +
            "}\n";
        Files.write(new File(commonDir, "PaymentInformationReqDto.java").toPath(), paymentInfoContent.getBytes(StandardCharsets.UTF_8));
        
        // 2. Create PaxReqDto.java
        String paxContent = "package com.airlines.go7api.requestdto.common;\n\n" +
            "import lombok.AllArgsConstructor;\n" +
            "import lombok.Data;\n" +
            "import lombok.NoArgsConstructor;\n" +
            "import java.math.BigDecimal;\n\n" +
            "@Data\n" +
            "@NoArgsConstructor\n" +
            "@AllArgsConstructor\n" +
            "public class PaxReqDto {\n" +
            "    private String paxId;\n" +
            "    private String ptc;\n" +
            "    private String dob;\n" +
            "    private String gender;\n" +
            "    private String title;\n" +
            "    private String firstName;\n" +
            "    private String middleName;\n" +
            "    private String lastName;\n" +
            "    private String infantRef;\n" +
            "    private String countryDialingCode;\n" +
            "    private BigDecimal areaCode;\n" +
            "    private BigDecimal phoneNumber;\n" +
            "    private String email;\n" +
            "    private Address1 address;\n\n" +
            "    @Data\n" +
            "    @NoArgsConstructor\n" +
            "    @AllArgsConstructor\n" +
            "    public static class Address1 {\n" +
            "        private String street;\n" +
            "        private String city;\n" +
            "        private String state;\n" +
            "        private String postalCode;\n" +
            "        private String countryCode;\n" +
            "        private String country;\n" +
            "    }\n" +
            "}\n";
        Files.write(new File(commonDir, "PaxReqDto.java").toPath(), paxContent.getBytes(StandardCharsets.UTF_8));
        
        // 3. Update the DTOs
        List<String> targetFiles = Arrays.asList(
            "ChangeSeatReqDto.java",
            "ChangeServiceReqDto.java",
            "OrderChangeReqDto.java",
            "OrderCreateReqDto.java"
        );
        File dir = new File("src/main/java/com/airlines/go7api/requestdto");
        for (String target : targetFiles) {
            File f = new File(dir, target);
            if (!f.exists()) continue;
            
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            
            // Add import
            content = content.replaceFirst("import lombok.AllArgsConstructor;", "import com.airlines.go7api.requestdto.common.*;\nimport lombok.AllArgsConstructor;");
            
            // Replace fields
            content = content.replace("private PaymentInformation paymentInformation;", "private PaymentInformationReqDto paymentInformation;");
            content = content.replace("private List<Pax> passengers;", "private List<PaxReqDto> passengers;");
            
            // Remove inner classes using precise counting
            content = removeInnerClass(content, "public static class PaymentInformation ");
            content = removeInnerClass(content, "public static class Pax ");
            
            Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
            System.out.println("Cleaned " + target);
        }
        
        System.out.println("Extraction complete!");
    }
    
    private static String removeInnerClass(String content, String signature) {
        int idx = content.indexOf(signature);
        if (idx == -1) return content;
        
        // Walk backwards to find the annotations
        int startIdx = idx;
        while (startIdx > 0) {
            char c = content.charAt(startIdx - 1);
            if (Character.isWhitespace(c) || c == '@' || Character.isLetter(c) || c == '(' || c == ')' || c == '=' || c == '"') {
                startIdx--;
                // stop if we went past @Data
                if (content.substring(startIdx).startsWith("@Data")) {
                    break;
                }
            } else {
                break;
            }
        }
        
        int braceStart = content.indexOf("{", idx);
        if (braceStart == -1) return content;
        
        int braceCount = 1;
        int endIdx = braceStart + 1;
        while (braceCount > 0 && endIdx < content.length()) {
            char c = content.charAt(endIdx);
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            endIdx++;
        }
        
        return content.substring(0, startIdx) + content.substring(endIdx);
    }
}
