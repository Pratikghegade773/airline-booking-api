import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FixAllStrayDTOs {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java/com/airlines/go7api");
        processDirectory(dir);
        System.out.println("Global DTO fix completed.");
    }

    private static void processDirectory(File dir) throws Exception {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                processDirectory(file);
            } else if (file.getName().endsWith(".java")) {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                boolean modified = false;

                // Known purged inner classes to replace
                String[] targets = {
                    "OrderRetrieveRspGo7Dto.Passenger",
                    "OrderRetrieveRspGo7Dto.Passengers",
                    "OrderCreateRspGo7Dto.Passenger",
                    "OrderCreateRspGo7Dto.Passengers",
                    "ChangePaymentRspGo7Dto.BalanceInformation",
                    "ChangeSeatRspGo7Dto.Flight",
                    "ChangeSeatRspGo7Dto.Seat",
                    "ChangeServiceRspGo7Dto.Aerocrs",
                    "AirshopRspGo7Dto.Flight",
                    "SeatAvailabilityRspGo7Dto.Flight",
                    "ServiceListRspGo7Dto.Flight"
                };

                for (String target : targets) {
                    String fullTarget = "com.airlines.go7api.responsego7." + target;
                    String shortClass = target.substring(target.indexOf('.') + 1);
                    String replacement = "com.airlines.go7api.responsego7.common." + shortClass;
                    
                    if (content.contains(fullTarget)) {
                        content = content.replace(fullTarget, replacement);
                        modified = true;
                    }

                    // Also check for direct usages if imported, but usually fully qualified when nested
                    // To be safe, we'll just handle fully qualified ones since that's what the error showed.
                }
                
                // Let's also find ANY "com.airlines.go7api.responsego7.[DTO].Passenger" using regex
                Pattern p = Pattern.compile("com\\.airlines\\.go7api\\.responsego7\\.[A-Za-z0-9_]+\\.(Passenger|Passengers|Flight|Flights|Booking|Aerocrs|BalanceInformation|Seat)");
                Matcher m = p.matcher(content);
                StringBuffer sb = new StringBuffer();
                while (m.find()) {
                    m.appendReplacement(sb, "com.airlines.go7api.responsego7.common." + m.group(1));
                    modified = true;
                }
                m.appendTail(sb);
                content = sb.toString();

                if (modified) {
                    Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
                    System.out.println("Fixed stray DTOs in: " + file.getAbsolutePath());
                }
            }
        }
    }
}
