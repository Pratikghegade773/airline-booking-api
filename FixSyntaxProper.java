import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class FixSyntaxProper {
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
            
            // 1. Put back the missing } before @Override
            content = content.replace("    @Override", "    }\n\n    @Override");
            
            // 2. Remove any extra } at the end of the file.
            // The file currently ends with }\n} or similar.
            // We want it to end with EXACTLY:
            //     protected String getRequestName() {
            //         return "XYZ";
            //     }
            // }
            int lastOverride = content.lastIndexOf("@Override");
            if (lastOverride != -1) {
                // Find the getRequestName method
                int endOfMethod = content.indexOf("\"", lastOverride);
                endOfMethod = content.indexOf("\"", endOfMethod + 1); // end of "XYZ"
                endOfMethod = content.indexOf(";", endOfMethod); // ;
                endOfMethod = content.indexOf("}", endOfMethod); // } closing getRequestName
                
                String before = content.substring(0, endOfMethod + 1);
                content = before + "\n}\n";
            }
            
            Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
            System.out.println("Properly fixed syntax in " + t);
        }
    }
}
