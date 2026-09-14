import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FixEqualsAndHashCode {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java/com/airlines/go7api/responsedto");
        
        List<String> targetFiles = Arrays.asList(
            "OrderRetrieveRspDto.java",
            "ChangePaymentRspDto.java",
            "ChangeSeatRspDto.java",
            "ChangeServiceRspDto.java",
            "UnpaidCancelRspDto.java",
            "OrderCreateRspDto.java"
        );
        
        for (String target : targetFiles) {
            File f = new File(dir, target);
            if (!f.exists()) continue;
            
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            
            // Remove all occurrences
            content = content.replace("@lombok.EqualsAndHashCode(callSuper = true)\n", "");
            content = content.replace("@lombok.EqualsAndHashCode(callSuper = true)", "");
            
            // Add it EXACTLY once after the first @Data
            content = content.replaceFirst("@Data", "@Data\n@lombok.EqualsAndHashCode(callSuper = true)");
            
            Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
            System.out.println("Fixed EqualsAndHashCode in " + target);
        }
    }
}
