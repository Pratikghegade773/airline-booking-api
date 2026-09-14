import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

public class ExtractBaseOrderResponse {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java/com/airlines/go7api/responsedto");
        
        List<String> targetFiles = Arrays.asList(
            "OrderRetrieveRspDto.java",
            "ChangePaymentRspDto.java",
            "ChangeSeatRspDto.java",
            "ChangeServiceRspDto.java",
            "UnpaidCancelRspDto.java",
            "OrderCreateRspDto.java"
        );
        
        // Define common fields to extract
        List<String> commonFields = Arrays.asList(
            "private List<String> warnings;",
            "private String responseId;",
            "private String pnr;",
            "private String apiOwner;",
            "private String orderId;",
            "private String validatingCarrier;",
            "private BigDecimal totalOrderPrice;",
            "private String currency;",
            "private String paymentTimeLimit;",
            "private List<OD> ods;",
            "private String statusCode;",
            "private List<String> remarks;",
            "private List<BookingReferences> bookingReferences;",
            "private List<PaxDetailDTO> paxDetailList;",
            "private List<SpecialServiceRequest> specialServiceRequests;",
            "private List<PriceClass> priceClassList;",
            "private List<TicketDocInfoDTO> ticketDocInfoList;",
            "private List<EMDInfoDTO> emdInfoList;",
            "private List<PaymentsDTO> payments;",
            "private List<OrderItemsDTO> orderItems;"
        );
        
        // Generate BaseOrderResponseDTO.java
        String baseClassContent = "package com.airlines.go7api.responsedto.common;\n\n" +
            "import com.fasterxml.jackson.annotation.JsonInclude;\n" +
            "import lombok.AllArgsConstructor;\n" +
            "import lombok.Data;\n" +
            "import lombok.NoArgsConstructor;\n" +
            "import java.math.BigDecimal;\n" +
            "import java.util.List;\n\n" +
            "@Data\n" +
            "@NoArgsConstructor\n" +
            "@AllArgsConstructor\n" +
            "@JsonInclude(JsonInclude.Include.NON_NULL)\n" +
            "public class BaseOrderResponseDTO {\n";
        
        for (String field : commonFields) {
            baseClassContent += "    " + field + "\n";
        }
        baseClassContent += "}\n";
        
        File baseFile = new File("src/main/java/com/airlines/go7api/responsedto/common/BaseOrderResponseDTO.java");
        Files.write(baseFile.toPath(), baseClassContent.getBytes(StandardCharsets.UTF_8));
        System.out.println("Created BaseOrderResponseDTO.java");
        
        // Update each target file
        for (String target : targetFiles) {
            File f = new File(dir, target);
            if (!f.exists()) continue;
            
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            
            // Add EqualsAndHashCode
            if (!content.contains("@EqualsAndHashCode")) {
                content = content.replace("@Data", "@Data\n@lombok.EqualsAndHashCode(callSuper = true)");
            }
            
            // Add extends
            content = content.replaceFirst("public class " + target.replace(".java", "") + "\\s*\\{",
                    "public class " + target.replace(".java", "") + " extends BaseOrderResponseDTO {");
            
            // Remove common fields
            for (String field : commonFields) {
                // handle spaces and newlines
                String fieldRegex = "\\s*" + Pattern.quote(field);
                content = content.replaceAll(fieldRegex, "");
            }
            
            Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
            System.out.println("Updated " + target);
        }
        
    }
}
