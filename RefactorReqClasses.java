import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class RefactorReqClasses {
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
            
            // 1. Make class extend BaseGo7Req
            content = content.replace("public class " + t.replace(".java", ""), "public class " + t.replace(".java", "") + " extends BaseGo7Req");
            
            // 2. Remove unmarshal() and makeApiCall() completely
            int unmarshalIdx = content.indexOf("public Object unmarshal()");
            if (unmarshalIdx != -1) {
                // Find the end of the class
                int lastBrace = content.lastIndexOf("}");
                if (lastBrace != -1) {
                    // Extract the part to remove
                    String toRemove = content.substring(unmarshalIdx, lastBrace);
                    content = content.replace(toRemove, "");
                }
            }
            
            // 3. Add getApiUrl() and getRequestName()
            String className = t.replace(".java", "");
            String urlVar = Character.toLowerCase(className.charAt(0)) + className.substring(1) + "Url";
            if (t.equals("AirshopReq.java")) urlVar = "airshoppingUrl";
            if (t.equals("ChangePaymentReq.java")) urlVar = "changePaymentUrl";
            if (t.equals("OfferPriceReq.java")) urlVar = "offerPriceUrl";
            if (t.equals("OrderReshopReq.java")) urlVar = "orderReshopUrl";
            if (t.equals("UnpaidCancelReq.java")) urlVar = "unpaidCancelUrl";
            if (t.equals("ChangeSeatReq.java")) urlVar = "changeAncillariesUrl";
            if (t.equals("ChangeServiceReq.java")) urlVar = "changeAncillariesUrl";
            
            // Check what the urlVar actually is by regex or hardcode
            // Actually, I can just use a simple string replace for the class end
            String methods = "\n    @Override\n    protected String getApiUrl() {\n        return " + urlVar + " != null ? " + urlVar + " : \"\";\n    }\n\n    @Override\n    protected String getRequestName() {\n        return \"" + className.replace("Req", "") + "\";\n    }\n}\n";
            content = content.trim() + methods;
            
            // Add missing imports
            if (content.contains("import javax.xml.datatype.DatatypeConfigurationException;")) {
                content = content.replace("import javax.xml.datatype.DatatypeConfigurationException;\n", "");
            }
            if (content.contains("import java.io.IOException;\n")) {
                content = content.replace("import java.io.IOException;\n", "");
            }
            
            Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
            System.out.println("Refactored " + t);
        }
    }
}
