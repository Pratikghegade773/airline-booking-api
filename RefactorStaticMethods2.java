import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class RefactorStaticMethods2 {
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
            
            // It replaced `mapPaxType(` with `OrderMappingUtil.mapPaxType(` in the declaration!
            // Let's delete the declarations manually.
            
            content = content.replaceAll("(?s)private static String(?: OrderMappingUtil\\.)?mapPaxType\\(.*?\\} *(?=\\n|private|public|\\}$)", "");
            content = content.replaceAll("(?s)private static String(?: OrderMappingUtil\\.)?formatDate\\(.*?\\} *(?=\\n|private|public|\\}$)", "");
            content = content.replaceAll("(?s)private static String(?: OrderMappingUtil\\.)?adjustDateByDays\\(.*?\\} *(?=\\n|private|public|\\}$)", "");
            content = content.replaceAll("(?s)private static String(?: OrderMappingUtil\\.)?calculateJourneyTime\\(.*?\\} *(?=\\n|private|public|\\}$)", "");
            content = content.replaceAll("(?s)private static String(?: OrderMappingUtil\\.)?formatCurrentDate\\(.*?\\} *(?=\\n|private|public|\\}$)", "");
            
            Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
            System.out.println("Cleaned " + target);
        }
    }
}
