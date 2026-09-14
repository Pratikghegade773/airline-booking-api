import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FindBrokenReferences {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java");
        search(dir);
    }
    
    private static void search(File f) throws Exception {
        if (f.isDirectory()) {
            for (File child : f.listFiles()) {
                search(child);
            }
        } else if (f.getName().endsWith(".java")) {
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            
            Pattern p = Pattern.compile("com\\.airlines\\.go7api\\.requestdto\\.(?:ChangeSeatReqDto|ChangeServiceReqDto|OrderChangeReqDto|OrderCreateReqDto)\\.(?:Offer|Pax|PaymentInformation|OrderItemDto)");
            Matcher m = p.matcher(content);
            if (m.find()) {
                System.out.println("Found broken reference in " + f.getAbsolutePath() + ": " + m.group());
                
                // Fix it
                content = content.replace("com.airlines.go7api.requestdto.ChangeSeatReqDto.Offer.OfferItemDto", "com.airlines.go7api.requestdto.common.ChangeOfferReqDto.OfferItemDto");
                content = content.replace("com.airlines.go7api.requestdto.ChangeSeatReqDto.Offer", "com.airlines.go7api.requestdto.common.ChangeOfferReqDto");
                
                content = content.replace("com.airlines.go7api.requestdto.ChangeServiceReqDto.Offer.OfferItemDto", "com.airlines.go7api.requestdto.common.ChangeOfferReqDto.OfferItemDto");
                content = content.replace("com.airlines.go7api.requestdto.ChangeServiceReqDto.Offer", "com.airlines.go7api.requestdto.common.ChangeOfferReqDto");
                
                content = content.replace("com.airlines.go7api.requestdto.ChangeSeatReqDto.OrderItemDto", "com.airlines.go7api.requestdto.common.ChangeOrderItemReqDto");
                content = content.replace("com.airlines.go7api.requestdto.ChangeServiceReqDto.OrderItemDto", "com.airlines.go7api.requestdto.common.ChangeOrderItemReqDto");
                
                content = content.replace("com.airlines.go7api.requestdto.ChangeSeatReqDto.Pax", "com.airlines.go7api.requestdto.common.PaxReqDto");
                content = content.replace("com.airlines.go7api.requestdto.ChangeServiceReqDto.Pax", "com.airlines.go7api.requestdto.common.PaxReqDto");
                content = content.replace("com.airlines.go7api.requestdto.OrderChangeReqDto.Pax", "com.airlines.go7api.requestdto.common.PaxReqDto");
                content = content.replace("com.airlines.go7api.requestdto.OrderCreateReqDto.Pax", "com.airlines.go7api.requestdto.common.PaxReqDto");
                
                content = content.replace("com.airlines.go7api.requestdto.ChangeSeatReqDto.PaymentInformation", "com.airlines.go7api.requestdto.common.PaymentInformationReqDto");
                content = content.replace("com.airlines.go7api.requestdto.ChangeServiceReqDto.PaymentInformation", "com.airlines.go7api.requestdto.common.PaymentInformationReqDto");
                content = content.replace("com.airlines.go7api.requestdto.OrderChangeReqDto.PaymentInformation", "com.airlines.go7api.requestdto.common.PaymentInformationReqDto");
                content = content.replace("com.airlines.go7api.requestdto.OrderCreateReqDto.PaymentInformation", "com.airlines.go7api.requestdto.common.PaymentInformationReqDto");
                
                Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
                System.out.println("Auto-fixed " + f.getName());
            }
        }
    }
}
