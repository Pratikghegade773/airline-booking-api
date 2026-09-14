import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class FixSyntax {
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
            
            // Fix the syntax error by removing the extra closing brace that got stuck before @Override
            content = content.replaceAll("\\}\\s*@Override", "@Override");
            
            // But wait, the class needs a closing brace at the very end!
            // Let's ensure the file ends with }\n
            content = content.trim() + "\n}\n";
            
            Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
            System.out.println("Fixed syntax in " + t);
        }
    }
}
