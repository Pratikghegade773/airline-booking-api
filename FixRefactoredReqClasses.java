import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class FixRefactoredReqClasses {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java/com/airlines/go7api/request");
        
        String[] targets = {
            "AirshopReq.java", "ChangePaymentReq.java", "ChangeSeatReq.java", 
            "ChangeServiceReq.java", "OfferPriceReq.java", "OrderReshopReq.java", 
            "OrderRetrieveReq.java", "SeatAvailabilityReq.java", "ServiceListReq.java", 
            "UnpaidCancelReq.java"
        };
        
        for (String t : targets) {
            File f = new File(dir, t);
            if (!f.exists()) continue;
            
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            
            // Fix the syntax error:
            // Remove "}\n    @Override\n    protected String getApiUrl()"
            // and replace it with "    @Override\n    protected String getApiUrl()"
            content = content.replace("}\n    @Override\n    protected String getApiUrl()", "    @Override\n    protected String getApiUrl()");
            
            // Fix wrong field names:
            // "seatAvailabilityReqUrl" -> "seatAvailabilityUrl"
            content = content.replace("seatAvailabilityReqUrl", "seatAvailabilityUrl");
            content = content.replace("serviceListReqUrl", "serviceListUrl");
            content = content.replace("orderRetrieveReqUrl", "orderRetrieveUrl");
            
            Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
            System.out.println("Fixed syntax in " + t);
        }
    }
}
