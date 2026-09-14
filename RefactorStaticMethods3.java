import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RefactorStaticMethods3 {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java/com/airlines/go7api/response");
        List<String> targetFiles = Arrays.asList(
            "ChangePaymentResponse.java",
            "ChangeSeatResponse.java",
            "ChangeServiceResponse.java",
            "OrderCreateResponse.java",
            "OrderRetrieveResponse.java",
            "UnpaidCancelResponse.java",
            "AirshopResponse.java",
            "OfferPriceResponse.java",
            "SeatAvailabilityResponse.java",
            "ServiceListResponse.java"
        );
        
        String[] prefixes = {
            "private static String OrderMappingUtil.mapPaxType",
            "private static String OrderMappingUtil.formatDate",
            "private static String OrderMappingUtil.adjustDateByDays",
            "private static String OrderMappingUtil.calculateJourneyTime",
            "private static String OrderMappingUtil.formatCurrentDate"
        };
        
        for (String target : targetFiles) {
            File f = new File(dir, target);
            if (!f.exists()) continue;
            
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            
            for (String prefix : prefixes) {
                while (content.contains(prefix)) {
                    int startIdx = content.indexOf(prefix);
                    // Find the opening brace
                    int braceStart = content.indexOf("{", startIdx);
                    if (braceStart == -1) break;
                    
                    int braceCount = 1;
                    int endIdx = braceStart + 1;
                    while (braceCount > 0 && endIdx < content.length()) {
                        char c = content.charAt(endIdx);
                        if (c == '{') braceCount++;
                        else if (c == '}') braceCount--;
                        endIdx++;
                    }
                    // Remove from startIdx to endIdx
                    content = content.substring(0, startIdx) + content.substring(endIdx);
                }
            }
            
            Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
            System.out.println("Cleaned braces in " + target);
        }
    }
}
