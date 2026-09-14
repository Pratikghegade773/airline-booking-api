import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class FixRequestMappers {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java/com/airlines/go7api/request");
        
        String[] targets = {"ChangeSeatReq.java", "ChangeServiceReq.java", "OrderChangeReq.java", "OrderCreateReq.java"};
        
        for (String t : targets) {
            File f = new File(dir, t);
            if (!f.exists()) continue;
            
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            
            // Add import if not present
            if (!content.contains("import com.airlines.go7api.requestdto.common.*;")) {
                content = content.replace("package com.airlines.go7api.request;", "package com.airlines.go7api.request;\n\nimport com.airlines.go7api.requestdto.common.*;");
            }
            
            // Fully qualified replacements
            content = content.replace("com.airlines.go7api.requestdto.ChangeSeatReqDto.Offer.OfferItemDto", "ChangeOfferReqDto.OfferItemDto");
            content = content.replace("com.airlines.go7api.requestdto.ChangeSeatReqDto.Offer", "ChangeOfferReqDto");
            
            content = content.replace("com.airlines.go7api.requestdto.ChangeServiceReqDto.Offer.OfferItemDto", "ChangeOfferReqDto.OfferItemDto");
            content = content.replace("com.airlines.go7api.requestdto.ChangeServiceReqDto.Offer", "ChangeOfferReqDto");
            
            content = content.replace("com.airlines.go7api.requestdto.ChangeSeatReqDto.OrderItemDto", "ChangeOrderItemReqDto");
            content = content.replace("com.airlines.go7api.requestdto.ChangeServiceReqDto.OrderItemDto", "ChangeOrderItemReqDto");
            
            content = content.replace("com.airlines.go7api.requestdto.ChangeSeatReqDto.Pax", "PaxReqDto");
            content = content.replace("com.airlines.go7api.requestdto.ChangeServiceReqDto.Pax", "PaxReqDto");
            content = content.replace("com.airlines.go7api.requestdto.OrderChangeReqDto.Pax", "PaxReqDto");
            content = content.replace("com.airlines.go7api.requestdto.OrderCreateReqDto.Pax", "PaxReqDto");
            
            content = content.replace("com.airlines.go7api.requestdto.ChangeSeatReqDto.PaymentInformation", "PaymentInformationReqDto");
            content = content.replace("com.airlines.go7api.requestdto.ChangeServiceReqDto.PaymentInformation", "PaymentInformationReqDto");
            content = content.replace("com.airlines.go7api.requestdto.OrderChangeReqDto.PaymentInformation", "PaymentInformationReqDto");
            content = content.replace("com.airlines.go7api.requestdto.OrderCreateReqDto.PaymentInformation", "PaymentInformationReqDto");
            
            // Non-fully qualified replacements
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
