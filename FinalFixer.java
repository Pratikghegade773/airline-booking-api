import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FinalFixer {
    public static void main(String[] args) throws Exception {
        // 1. Fix Go7Controller.java
        Path controllerFile = Paths.get("src/main/java/com/airlines/go7api/controller/Go7Controller.java");
        if (Files.exists(controllerFile)) {
            String content = new String(Files.readAllBytes(controllerFile));
            content = content.replace("ChangeSeatRspDto.OrderItemsDTO", "OrderItemsDTO")
                             .replace("ChangeSeatRspDto.Service", "Service")
                             .replace("ChangeServiceRspDto.OrderItemsDTO", "OrderItemsDTO")
                             .replace("ChangeServiceRspDto.Service", "Service");
                             
            if (!content.contains("import com.airlines.go7api.responsedto.common.*;")) {
                content = content.replace("package com.airlines.go7api.controller;", "package com.airlines.go7api.controller;\n\nimport com.airlines.go7api.responsedto.common.*;");
            }
            Files.write(controllerFile, content.getBytes());
            System.out.println("Fixed Go7Controller");
        }

        // 2. Add missing fields to common/Service.java
        Path serviceFile = Paths.get("src/main/java/com/airlines/go7api/responsedto/common/Service.java");
        if (Files.exists(serviceFile)) {
            String content = new String(Files.readAllBytes(serviceFile));
            if (!content.contains("private String odKey;")) {
                content = content.replace("private String segmentId;", "private String segmentId;\n    private String odKey;\n    private ServiceDefinationRef serviceDefinitionRef;\n\n    @Data\n    @NoArgsConstructor\n    @AllArgsConstructor\n    @JsonInclude(JsonInclude.Include.NON_NULL)\n    public static class ServiceDefinationRef {\n        private String serviceDefinitionRefId;\n        private String name;\n    }");
                Files.write(serviceFile, content.getBytes());
                System.out.println("Fixed common/Service.java");
            }
        }
        
        // 3. Fix OfferPriceResponse.java PriceClassList
        Path offerPriceResp = Paths.get("src/main/java/com/airlines/go7api/response/OfferPriceResponse.java");
        if (Files.exists(offerPriceResp)) {
            String content = new String(Files.readAllBytes(offerPriceResp));
            content = content.replace("PriceClassList", "OfferPriceRspDto.PriceClassList")
                             .replace("OfferPriceRspDto.OfferPriceRspDto.PriceClassList", "OfferPriceRspDto.PriceClassList");
            Files.write(offerPriceResp, content.getBytes());
            System.out.println("Fixed OfferPriceResponse.java");
        }
        
        // 4. Fix ServiceListResponse.java ServiceDefinitionRef typos (if any)
        Path serviceListResp = Paths.get("src/main/java/com/airlines/go7api/response/ServiceListResponse.java");
        if (Files.exists(serviceListResp)) {
            String content = new String(Files.readAllBytes(serviceListResp));
            content = content.replace("ServiceDefinationRef", "Service.ServiceDefinationRef");
            Files.write(serviceListResp, content.getBytes());
            System.out.println("Fixed ServiceListResponse.java");
        }
    }
}
