import java.io.*;
import java.nio.file.*;

public class QuickFixer {
    public static void main(String[] args) throws Exception {
        // Fix Go7Controller
        Path controllerFile = Paths.get("src/main/java/com/airlines/go7api/controller/Go7Controller.java");
        if (Files.exists(controllerFile)) {
            String content = new String(Files.readAllBytes(controllerFile));
            content = content.replace("com.airlines.go7api.responsedto.OrderItemsDTO", "com.airlines.go7api.responsedto.common.OrderItemsDTO")
                             .replace("com.airlines.go7api.responsedto.Service", "com.airlines.go7api.responsedto.common.Service");
            Files.write(controllerFile, content.getBytes());
            System.out.println("Fixed Go7Controller");
        }

        // Fix ServiceListResponse
        Path serviceListResp = Paths.get("src/main/java/com/airlines/go7api/response/ServiceListResponse.java");
        if (Files.exists(serviceListResp)) {
            String content = new String(Files.readAllBytes(serviceListResp));
            content = content.replace("Service.Service.ServiceDefinationRef", "Service.ServiceDefinationRef")
                             .replace("setService.ServiceDefinationRef", "setServiceDefinitionRef")
                             .replace("setServiceDefinationRef", "setServiceDefinitionRef");
            Files.write(serviceListResp, content.getBytes());
            System.out.println("Fixed ServiceListResponse");
        }
        
        // Fix common.Service to have the Encoding and Description classes, and setServiceDefinitionRef instead of setServiceDefinationRef
        Path commonService = Paths.get("src/main/java/com/airlines/go7api/responsedto/common/Service.java");
        if (Files.exists(commonService)) {
            String content = new String(Files.readAllBytes(commonService));
            // Let's just redefine ServiceDefinitionRef correctly with Encoding and Description
            String oldServiceDef = "    @Data\n    @NoArgsConstructor\n    @AllArgsConstructor\n    @JsonInclude(JsonInclude.Include.NON_NULL)\n    public static class ServiceDefinationRef {\n        private String serviceDefinitionRefId;\n        private String name;\n    }";
            String newServiceDef = "    @Data\n    @NoArgsConstructor\n    @AllArgsConstructor\n    @JsonInclude(JsonInclude.Include.NON_NULL)\n    public static class ServiceDefinationRef {\n        private String serviceDefId;\n        private String name;\n        private Encoding encoding;\n        private java.util.List<Description> descriptions;\n\n        @Data\n        public static class Encoding {\n            private String rfic;\n            private String type;\n            private String code;\n            private String subCode;\n        }\n\n        @Data\n        @AllArgsConstructor\n        @NoArgsConstructor\n        public static class Description {\n            private String text;\n            private String type;\n        }\n    }";
            content = content.replace(oldServiceDef, newServiceDef);
            Files.write(commonService, content.getBytes());
            System.out.println("Fixed common Service");
        }
        
        // OfferPriceResponse variable name setOfferPriceRspDto
        Path offerPriceResp = Paths.get("src/main/java/com/airlines/go7api/response/OfferPriceResponse.java");
        if (Files.exists(offerPriceResp)) {
            String content = new String(Files.readAllBytes(offerPriceResp));
            content = content.replace("response.setOfferPriceRspDto.PriceClassList(", "response.setPriceClassList("); 
            Files.write(offerPriceResp, content.getBytes());
            System.out.println("Fixed OfferPriceResponse.java");
        }
    }
}
