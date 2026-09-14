import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Go7Purger {
    public static void main(String[] args) throws Exception {
        Path responseGo7Dir = Paths.get("src/main/java/com/airlines/go7api/responsego7");
        Path responseDir = Paths.get("src/main/java/com/airlines/go7api/response");
        
        // 1. Purge DTOs
        Files.walk(responseGo7Dir)
             .filter(p -> p.toString().endsWith("RspGo7Dto.java") && !p.toString().contains("common"))
             .forEach(file -> {
                 try {
                     String content = new String(Files.readAllBytes(file));
                     // We will delete everything from the first "public static class " onwards, except the last closing brace
                     // Wait, some classes might have other nested classes that aren't shared?
                     // We extracted: Details, Aerocrs, Booking, Items, Flights, Flight, Taxes, Passengers, Passenger, Remarks, Remark, BalanceInformation, Seat
                     // Let's just remove the specific classes we know are shared.
                     
                     String[] classesToRemove = {
                         "Details", "Aerocrs", "Booking", "Items", "Flights", "Flight", 
                         "Taxes", "Passengers", "Passenger", "Remarks", "Remark", 
                         "BalanceInformation", "Seat"
                     };
                     
                     for (String cls : classesToRemove) {
                         while (true) {
                             String startMarker = "public static class " + cls + " {";
                             int startIndex = content.indexOf(startMarker);
                             if (startIndex == -1) break;
                             
                             int annotationsStart = content.lastIndexOf("@Data", startIndex);
                             if (annotationsStart == -1) annotationsStart = startIndex;
                             
                             int braceCount = 0;
                             int endIndex = -1;
                             boolean started = false;
                             for (int i = startIndex; i < content.length(); i++) {
                                 if (content.charAt(i) == '{') { braceCount++; started = true; }
                                 else if (content.charAt(i) == '}') {
                                     braceCount--;
                                     if (started && braceCount == 0) {
                                         endIndex = i + 1;
                                         break;
                                     }
                                 }
                             }
                             if (endIndex != -1) {
                                 content = content.substring(0, annotationsStart) + content.substring(endIndex);
                             } else {
                                 break;
                             }
                         }
                     }
                     
                     // Add import
                     if (!content.contains("import com.airlines.go7api.responsego7.common.*;")) {
                         content = content.replace("public class", "import com.airlines.go7api.responsego7.common.*;\n\npublic class");
                     }
                     
                     Files.write(file, content.getBytes());
                     System.out.println("Purged DTO: " + file.getFileName());
                 } catch (Exception e) { e.printStackTrace(); }
             });
             
        // 2. Update Mappers
        String[] dtoNames = {
            "ChangePaymentRspGo7Dto", "OrderCreateRspGo7Dto", "OrderRetrieveRspGo7Dto", 
            "UnpaidCancelRspGo7Dto", "AirshopRspGo7Dto", "ChangeSeatRspGo7Dto", 
            "ChangeServiceRspGo7Dto", "OfferPriceRspGo7Dto", "SeatAvailabilityRspGo7Dto", "ServiceListRspGo7Dto"
        };
        
        Files.walk(responseDir)
             .filter(p -> p.toString().endsWith(".java"))
             .forEach(file -> {
                 try {
                     String content = new String(Files.readAllBytes(file));
                     boolean changed = false;
                     
                     for (String dtoName : dtoNames) {
                         // Replace DtoName.InnerClass with just InnerClass
                         // e.g., OrderCreateRspGo7Dto.Flight -> Flight
                         String[] innerClasses = {
                             "Details", "Aerocrs", "Booking", "Items", "Flights", "Flight", 
                             "Taxes", "Passengers", "Passenger", "Remarks", "Remark", 
                             "BalanceInformation", "Seat"
                         };
                         
                         for (String inner : innerClasses) {
                             String searchFor = dtoName + "." + inner;
                             if (content.contains(searchFor)) {
                                 content = content.replace(searchFor, inner);
                                 changed = true;
                             }
                         }
                     }
                     
                     if (changed) {
                         if (!content.contains("import com.airlines.go7api.responsego7.common.*;")) {
                             content = content.replace("public class", "import com.airlines.go7api.responsego7.common.*;\n\npublic class");
                         }
                         Files.write(file, content.getBytes());
                         System.out.println("Updated mapper: " + file.getFileName());
                     }
                 } catch (Exception e) { e.printStackTrace(); }
             });
    }
}
