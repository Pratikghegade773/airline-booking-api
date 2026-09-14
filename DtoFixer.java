import java.io.*;
import java.nio.file.*;
import java.util.*;

public class DtoFixer {
    public static void main(String[] args) throws Exception {
        // Fix mappers for PhoneDTO and EmailDTO
        String[] mapperFiles = {
            "src/main/java/com/airlines/go7api/response/OrderCreateResponse.java",
            "src/main/java/com/airlines/go7api/response/OrderRetrieveResponse.java",
            "src/main/java/com/airlines/go7api/response/UnpaidCancelResponse.java",
            "src/main/java/com/airlines/go7api/response/OfferPriceResponse.java",
            "src/main/java/com/airlines/go7api/response/ServiceListResponse.java"
        };
        for (String filePath : mapperFiles) {
            Path file = Paths.get(filePath);
            if (!Files.exists(file)) continue;
            String content = new String(Files.readAllBytes(file));
            
            content = content.replace("OrderCreateRspDto.PhoneDTO", "PaxDetailDTO.PhoneDTO")
                             .replace("OrderCreateRspDto.EmailDTO", "PaxDetailDTO.EmailDTO")
                             .replace("OrderRetrieveRspDto.PhoneDTO", "PaxDetailDTO.PhoneDTO")
                             .replace("OrderRetrieveRspDto.EmailDTO", "PaxDetailDTO.EmailDTO")
                             .replace("UnpaidCancelRspDto.PhoneDTO", "PaxDetailDTO.PhoneDTO")
                             .replace("UnpaidCancelRspDto.EmailDTO", "PaxDetailDTO.EmailDTO")
                             .replace("OfferPriceRspDto.OD", "OD")
                             .replace("OfferPriceRspDto.PriceClassList.Description", "PriceClassList.Description") // just in case
                             .replace("ServiceListRspDto.OfferItem.Baggage.Service", "Service")
                             .replace("ServiceListRspDto.OfferItem.OtherService.Service", "Service");
                             
            Files.write(file, content.getBytes());
        }
        
        // Strip duplicate classes from OfferPriceRspDto, AirshopRspDto, OrderReshopRspDto
        String[] dtoFiles = {
            "src/main/java/com/airlines/go7api/responsedto/OfferPriceRspDto.java",
            "src/main/java/com/airlines/go7api/responsedto/AirshopRspDto.java",
            "src/main/java/com/airlines/go7api/responsedto/OrderReshopRspDto.java"
        };
        
        String[] classesToRemove = {
            "OD", "PaxDetailDTO", "TicketDocInfoDTO", "EMDInfoDTO", "OrderItemsDTO", "Service", "PaymentsDTO"
        };
        
        for (String filePath : dtoFiles) {
            Path file = Paths.get(filePath);
            if (!Files.exists(file)) continue;
            String content = new String(Files.readAllBytes(file));
            boolean changed = false;
            
            for (String cls : classesToRemove) {
                String signature = "public static class " + cls + " \\{";
                // find index
                int start = content.indexOf("public static class " + cls + " {");
                if (start == -1) start = content.indexOf("public static class " + cls + "\r\n");
                if (start == -1) start = content.indexOf("public static class " + cls + "\n");
                
                if (start != -1) {
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
                    if (braceStart != -1) {
                        int braces = 1;
                        int i = braceStart + 1;
                        for (; i < content.length(); i++) {
                            if (content.charAt(i) == '{') braces++;
                            else if (content.charAt(i) == '}') braces--;
                            if (braces == 0) break;
                        }
                        if (braces == 0) {
                            content = content.substring(0, annotationStart) + content.substring(i + 1);
                            changed = true;
                        }
                    }
                }
            }
            if (changed) {
                if (!content.contains("import com.airlines.go7api.responsedto.common.*;")) {
                    content = content.replace("package com.airlines.go7api.responsedto;", "package com.airlines.go7api.responsedto;\n\nimport com.airlines.go7api.responsedto.common.*;");
                }
                Files.write(file, content.getBytes());
                System.out.println("Stripped nested classes from " + file.getFileName());
            }
        }
    }
}
