import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CleanOrderCreateRspDto {
    public static void main(String[] args) throws Exception {
        File dtoFile = new File("src/main/java/com/airlines/go7api/responsedto/OrderCreateRspDto.java");
        if (dtoFile.exists()) {
            String content = new String(Files.readAllBytes(dtoFile.toPath()), StandardCharsets.UTF_8);
            
            // Remove field declarations
            content = content.replaceAll("private List<BookingReference> bookingReferences;\\s*", "");
            content = content.replaceAll("private List<OrderItemDTO> orderItems;\\s*", "");
            
            // Remove inner classes
            content = content.replaceAll("(?s)public static class BookingReference \\{.*?\\}", "");
            content = content.replaceAll("(?s)public static class OrderItemDTO \\{.*?    \\}", "");
            
            // Just to be safe, I'll remove any remaining annotations hanging around because of regex replace
            content = content.replaceAll("(?s)@Data\\s*@NoArgsConstructor\\s*@AllArgsConstructor\\s*@JsonInclude\\(JsonInclude\\.Include\\.NON_NULL\\)\\s*// This will exclude null fields\\s*", "");
            content = content.replaceAll("(?s)@Data\\s*@NoArgsConstructor\\s*@AllArgsConstructor\\s*@JsonInclude\\(JsonInclude\\.Include\\.NON_NULL\\)\\s*", "");
            
            Files.write(dtoFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
            System.out.println("Cleaned OrderCreateRspDto.java fields and inner classes.");
        }
        
        File mapperFile = new File("src/main/java/com/airlines/go7api/response/OrderCreateResponse.java");
        if (mapperFile.exists()) {
            String content = new String(Files.readAllBytes(mapperFile.toPath()), StandardCharsets.UTF_8);
            
            content = content.replace("OrderCreateRspDto.BookingReference", "BookingReferences");
            content = content.replace("OrderCreateRspDto.OrderItemDTO", "OrderItemsDTO");
            
            Files.write(mapperFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
            System.out.println("Updated references in OrderCreateResponse.java");
        }
    }
}
