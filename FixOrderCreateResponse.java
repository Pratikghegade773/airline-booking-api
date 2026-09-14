import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class FixOrderCreateResponse {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java/com/airlines/go7api/response");
        
        String[] targets = {"OrderCreateResponse.java", "OrderChangeResponse.java", "ChangeSeatResponse.java", "ChangeServiceResponse.java"};
        
        for (String t : targets) {
            File f = new File(dir, t);
            if (!f.exists()) continue;
            
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            
            // Add import if not present
            if (!content.contains("import com.airlines.go7api.requestdto.common.*;")) {
                content = content.replace("package com.airlines.go7api.response;", "package com.airlines.go7api.response;\n\nimport com.airlines.go7api.requestdto.common.*;");
            }
            
            // Replace non-fully qualified references
            content = content.replace("OrderCreateReqDto.Pax", "PaxReqDto");
            content = content.replace("OrderCreateReqDto.PaymentInformation", "PaymentInformationReqDto");
            content = content.replace("OrderChangeReqDto.Pax", "PaxReqDto");
            content = content.replace("OrderChangeReqDto.PaymentInformation", "PaymentInformationReqDto");
            content = content.replace("ChangeSeatReqDto.Pax", "PaxReqDto");
            content = content.replace("ChangeSeatReqDto.PaymentInformation", "PaymentInformationReqDto");
            content = content.replace("ChangeServiceReqDto.Pax", "PaxReqDto");
            content = content.replace("ChangeServiceReqDto.PaymentInformation", "PaymentInformationReqDto");
            
            content = content.replace("ChangeSeatReqDto.Offer.OfferItemDto", "ChangeOfferReqDto.OfferItemDto");
            content = content.replace("ChangeSeatReqDto.Offer", "ChangeOfferReqDto");
            content = content.replace("ChangeServiceReqDto.Offer.OfferItemDto", "ChangeOfferReqDto.OfferItemDto");
            content = content.replace("ChangeServiceReqDto.Offer", "ChangeOfferReqDto");
            
            content = content.replace("ChangeSeatReqDto.OrderItemDto", "ChangeOrderItemReqDto");
            content = content.replace("ChangeServiceReqDto.OrderItemDto", "ChangeOrderItemReqDto");
            
            Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
            System.out.println("Fixed " + t);
        }
    }
}
