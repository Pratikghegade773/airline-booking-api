import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class FixBaggageReferences {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java/com/airlines/go7api");
        processDirectory(dir);
        System.out.println("References fixed.");
    }

    private static void processDirectory(File dir) throws Exception {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                processDirectory(file);
            } else if (file.getName().endsWith(".java")) {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                boolean modified = false;

                String[] targets = {
                    "TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.BaggageAllowance",
                    "TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.BaggageAllowance.Weight",
                    "TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.BaggageAllowance.Dimension",
                    "TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.BaggageAllowance.DescriptionDTO",
                    "OrderItemsDTO.BaggageAllowance",
                    "OrderItemsDTO.BaggageAllowance.Weight",
                    "OrderItemsDTO.BaggageAllowance.Dimension",
                    "OrderItemsDTO.BaggageAllowance.DescriptionDTO"
                };

                for (String target : targets) {
                    if (content.contains(target)) {
                        String replacement = "BaggageAllowance";
                        if (target.contains("Weight")) replacement = "BaggageAllowance.Weight";
                        else if (target.contains("Dimension")) replacement = "BaggageAllowance.Dimension";
                        else if (target.contains("DescriptionDTO")) replacement = "BaggageAllowance.DescriptionDTO";
                        
                        content = content.replace(target, replacement);
                        modified = true;
                    }
                }

                if (modified) {
                    Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
                    System.out.println("Fixed in: " + file.getName());
                }
            }
        }
    }
}
