import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class FixResponseMappers {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java/com/airlines/go7api/response");
        
        String[] targets = {"ChangeSeatResponse.java", "ChangeServiceResponse.java"};
        
        for (String t : targets) {
            File f = new File(dir, t);
            if (!f.exists()) continue;
            
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            
            // Fix ChangeSeatReqDto
            content = content.replace("com.airlines.go7api.requestdto.ChangeSeatReqDto.Offer.OfferItemDto", 
                                      "com.airlines.go7api.requestdto.common.ChangeOfferReqDto.OfferItemDto");
            content = content.replace("com.airlines.go7api.requestdto.ChangeSeatReqDto.Offer", 
                                      "com.airlines.go7api.requestdto.common.ChangeOfferReqDto");
            
            // Fix ChangeServiceReqDto
            content = content.replace("com.airlines.go7api.requestdto.ChangeServiceReqDto.Offer.OfferItemDto", 
                                      "com.airlines.go7api.requestdto.common.ChangeOfferReqDto.OfferItemDto");
            content = content.replace("com.airlines.go7api.requestdto.ChangeServiceReqDto.Offer", 
                                      "com.airlines.go7api.requestdto.common.ChangeOfferReqDto");
            
            Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
            System.out.println("Fixed " + t);
        }
    }
}
