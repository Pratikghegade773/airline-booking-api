import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class Replacer {
    public static void main(String[] args) throws Exception {
        String[] dirs = {
            "src/main/java/com/airlines/go7api/response",
            "src/main/java/com/airlines/go7api/responsedto"
        };
        
        for (String dir : dirs) {
            List<Path> files = Files.walk(Paths.get(dir))
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .collect(Collectors.toList());
                
            for (Path file : files) {
                String content = new String(Files.readAllBytes(file));
                String newContent = content
                    .replace("ChangeSeatRspDto.OrderItemsDTO", "OrderItemsDTO")
                    .replace("ChangeServiceRspDto.OrderItemsDTO", "OrderItemsDTO")
                    .replace("ChangePaymentRspDto.OrderItemsDTO", "OrderItemsDTO")
                    .replace("OrderRetrieveRspDto.OrderItemsDTO", "OrderItemsDTO")
                    .replace("UnpaidCancelRspDto.OrderItemsDTO", "OrderItemsDTO")
                    .replace("OrderCreateRspDto.OrderItemsDTO", "OrderItemsDTO")
                    .replace("AirshopRspDto.Offer.OD", "OD")
                    .replace("OrderReshopRspDto.Offer.OD", "OD")
                    .replace("AirshopRspDto.Offer.OfferItemDto.PriceClassReference", "PriceClassReference")
                    .replace("OrderReshopRspDto.Offer.OfferItemDto.PriceClassReference", "PriceClassReference")
                    .replace("OfferPriceRspDto.PriceClassList", "PriceClassList");
                
                // Add common imports for Response classes
                if (!newContent.equals(content)) {
                    if (file.toString().contains("\\response\\") && !newContent.contains("import com.airlines.go7api.responsedto.common.*;")) {
                        newContent = newContent.replace("package com.airlines.go7api.response;", "package com.airlines.go7api.response;\n\nimport com.airlines.go7api.responsedto.common.*;");
                    }
                    Files.write(file, newContent.getBytes());
                    System.out.println("Updated " + file.getFileName());
                }
            }
        }
    }
}
