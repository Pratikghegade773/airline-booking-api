import java.io.*;
import java.nio.file.*;

public class DtoFixerTaxes {
    public static void main(String[] args) throws Exception {
        String[] mappers = {
            "src/main/java/com/airlines/go7api/response/OrderRetrieveResponse.java",
            "src/main/java/com/airlines/go7api/response/UnpaidCancelResponse.java"
        };
        for (String mapperPath : mappers) {
            Path p = Paths.get(mapperPath);
            String content = new String(Files.readAllBytes(p));
            
            content = content.replace("OrderItemsDTO.Taxes", "OrderItemsDTO.Tax");
            content = content.replace("OrderRetrieveRspDto.Taxes", "OrderItemsDTO.Tax");
            content = content.replace("UnpaidCancelRspDto.Taxes", "OrderItemsDTO.Tax");
            
            // Just in case it's named something else
            content = content.replace("new OrderItemsDTO.Taxes()", "new OrderItemsDTO.Tax()");
            
            Files.write(p, content.getBytes());
            System.out.println("Updated mapper taxes in: " + mapperPath);
        }
    }
}
