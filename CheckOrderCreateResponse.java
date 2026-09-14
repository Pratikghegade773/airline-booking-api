import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CheckOrderCreateResponse {
    public static void main(String[] args) throws Exception {
        File mapperFile = new File("src/main/java/com/airlines/go7api/response/OrderCreateResponse.java");
        if (mapperFile.exists()) {
            String content = new String(Files.readAllBytes(mapperFile.toPath()), StandardCharsets.UTF_8);
            
            // Check for any leftover "OrderCreateRspDto.OrderItemDTO" or "OrderItemDTO"
            String[] lines = content.split("\n");
            for(int i = 0; i < lines.length; i++) {
                if (lines[i].contains("OrderItemDTO")) {
                    System.out.println("Line " + (i+1) + ": " + lines[i].trim());
                }
            }
        }
    }
}
