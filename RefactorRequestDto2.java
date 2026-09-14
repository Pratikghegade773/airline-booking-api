import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RefactorRequestDto2 {
    public static void main(String[] args) throws Exception {
        File commonDir = new File("src/main/java/com/airlines/go7api/requestdto/common");
        
        // 1. Create ChangeOfferReqDto.java
        String offerContent = "package com.airlines.go7api.requestdto.common;\n\n" +
            "import lombok.AllArgsConstructor;\n" +
            "import lombok.Data;\n" +
            "import lombok.NoArgsConstructor;\n" +
            "import java.math.BigDecimal;\n" +
            "import java.math.BigInteger;\n" +
            "import java.util.List;\n\n" +
            "@Data\n" +
            "@NoArgsConstructor\n" +
            "@AllArgsConstructor\n" +
            "public class ChangeOfferReqDto {\n" +
            "    private String offerId;\n" +
            "    private List<OfferItemDto> offerItems;\n\n" +
            "    @Data\n" +
            "    @NoArgsConstructor\n" +
            "    @AllArgsConstructor\n" +
            "    public static class OfferItemDto {\n" +
            "        private String offerItemId;\n" +
            "        private String ptc;\n" +
            "        private List<String> paxRefs;\n" +
            "        private BigInteger row;\n" +
            "        private String column;\n" +
            "        private SpecialServices specialServices;\n\n" +
            "        @Data\n" +
            "        @NoArgsConstructor\n" +
            "        @AllArgsConstructor\n" +
            "        public static class SpecialServices {\n" +
            "            private BigDecimal qty;\n" +
            "            private String text;\n" +
            "            private String name;\n" +
            "            private String type;\n" +
            "            private String serviceDefId;\n" +
            "            private List<String> segId;\n" +
            "        }\n" +
            "    }\n" +
            "}\n";
        Files.write(new File(commonDir, "ChangeOfferReqDto.java").toPath(), offerContent.getBytes(StandardCharsets.UTF_8));
        
        // 2. Create ChangeOrderItemReqDto.java
        String orderItemContent = "package com.airlines.go7api.requestdto.common;\n\n" +
            "import lombok.AllArgsConstructor;\n" +
            "import lombok.Data;\n" +
            "import lombok.NoArgsConstructor;\n\n" +
            "@Data\n" +
            "@NoArgsConstructor\n" +
            "@AllArgsConstructor\n" +
            "public class ChangeOrderItemReqDto {\n" +
            "    private String orderItemId;\n" +
            "}\n";
        Files.write(new File(commonDir, "ChangeOrderItemReqDto.java").toPath(), orderItemContent.getBytes(StandardCharsets.UTF_8));
        
        // 3. Update the DTOs
        List<String> targetFiles = Arrays.asList(
            "ChangeSeatReqDto.java",
            "ChangeServiceReqDto.java"
        );
        File dir = new File("src/main/java/com/airlines/go7api/requestdto");
        for (String target : targetFiles) {
            File f = new File(dir, target);
            if (!f.exists()) continue;
            
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            
            // Replace fields
            content = content.replace("private List<Offer> offers;", "private List<ChangeOfferReqDto> offers;");
            content = content.replace("private List<OrderItemDto> deleteOrderItems;", "private List<ChangeOrderItemReqDto> deleteOrderItems;");
            
            // Remove inner classes using precise counting
            content = removeInnerClass(content, "public static class Offer ");
            content = removeInnerClass(content, "public static class OrderItemDto ");
            
            Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
            System.out.println("Cleaned " + target);
        }
        
        System.out.println("Extraction 2 complete!");
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
