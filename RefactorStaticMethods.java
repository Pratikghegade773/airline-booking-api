import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RefactorStaticMethods {
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
        
        for (String target : targetFiles) {
            File f = new File(dir, target);
            if (!f.exists()) continue;
            
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            boolean modified = false;
            
            // Remove the definitions using regex.
            // mapPaxType
            if (content.contains("private static String mapPaxType")) {
                content = content.replaceAll("(?s)private static String mapPaxType\\(.*?\\} *(?=\\n|private|public)", "");
                modified = true;
            }
            
            // formatDate
            if (content.contains("private static String formatDate")) {
                content = content.replaceAll("(?s)private static String formatDate\\(.*?\\} *(?=\\n|private|public)", "");
                modified = true;
            }
            
            // adjustDateByDays
            if (content.contains("private static String adjustDateByDays")) {
                content = content.replaceAll("(?s)private static String adjustDateByDays\\(.*?\\} *(?=\\n|private|public)", "");
                modified = true;
            }
            
            // calculateJourneyTime
            if (content.contains("private static String calculateJourneyTime")) {
                content = content.replaceAll("(?s)private static String calculateJourneyTime\\(.*?\\} *(?=\\n|private|public)", "");
                modified = true;
            }
            
            // formatCurrentDate
            if (content.contains("private static String formatCurrentDate")) {
                content = content.replaceAll("(?s)private static String formatCurrentDate\\(.*?\\} *(?=\\n|private|public)", "");
                modified = true;
            }
            
            if (modified) {
                // Now replace the usages.
                content = content.replaceAll("(?<!\\w)mapPaxType\\(", "OrderMappingUtil.mapPaxType(");
                content = content.replaceAll("(?<!\\w)formatDate\\(", "OrderMappingUtil.formatDate(");
                content = content.replaceAll("(?<!\\w)adjustDateByDays\\(", "OrderMappingUtil.adjustDateByDays(");
                content = content.replaceAll("(?<!\\w)calculateJourneyTime\\(", "OrderMappingUtil.calculateJourneyTime(");
                content = content.replaceAll("(?<!\\w)formatCurrentDate\\(", "OrderMappingUtil.formatCurrentDate(");
                
                Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
                System.out.println("Refactored static helpers in " + target);
            }
        }
    }
}
