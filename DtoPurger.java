import java.io.*;
import java.nio.file.*;
import java.util.*;

public class DtoPurger {
    public static void main(String[] args) throws Exception {
        String[] dtos = {
            "src/main/java/com/airlines/go7api/responsedto/OrderRetrieveRspDto.java",
            "src/main/java/com/airlines/go7api/responsedto/UnpaidCancelRspDto.java"
        };
        
        for (String dtoPath : dtos) {
            Path p = Paths.get(dtoPath);
            String content = new String(Files.readAllBytes(p));
            // We want to delete everything from the first "    public static class BookingReference {"
            // Actually, we'll just find the first "@Data\n    @NoArgsConstructor\n    @AllArgsConstructor\n    @JsonInclude(JsonInclude.Include.NON_NULL)\n    public static class BookingReference"
            int index = content.indexOf("public static class BookingReference");
            if (index != -1) {
                // Find the previous @Data to ensure we catch the annotations
                int start = content.lastIndexOf("@Data", index);
                if (start != -1) {
                    content = content.substring(0, start) + "\n}\n";
                    
                    // Replace usages in this file
                    content = content.replace("private List<BookingReference> bookingReferences;", "private List<BookingReferences> bookingReferences;");
                    content = content.replace("private List<OrderItemDTO> orderItems;", "private List<OrderItemsDTO> orderItems;");
                    
                    Files.write(p, content.getBytes());
                    System.out.println("Purged nested classes from: " + dtoPath);
                }
            }
        }
        
        // Update Mappers
        String[] mappers = {
            "src/main/java/com/airlines/go7api/response/OrderRetrieveResponse.java",
            "src/main/java/com/airlines/go7api/response/UnpaidCancelResponse.java"
        };
        for (String mapperPath : mappers) {
            Path p = Paths.get(mapperPath);
            String content = new String(Files.readAllBytes(p));
            
            content = content.replace("OrderRetrieveRspDto.BookingReference", "BookingReferences");
            content = content.replace("UnpaidCancelRspDto.BookingReference", "BookingReferences");
            content = content.replace("OrderRetrieveRspDto.OrderItemDTO", "OrderItemsDTO");
            content = content.replace("UnpaidCancelRspDto.OrderItemDTO", "OrderItemsDTO");
            content = content.replace("new BookingReference()", "new BookingReferences()");
            
            Files.write(p, content.getBytes());
            System.out.println("Updated mapper: " + mapperPath);
        }
    }
}
