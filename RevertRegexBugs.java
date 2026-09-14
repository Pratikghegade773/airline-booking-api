import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class RevertRegexBugs {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java/com/airlines/go7api");
        processDirectory(dir);
        System.out.println("Reverts completed.");
    }

    private static void processDirectory(File dir) throws Exception {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                processDirectory(file);
            } else if (file.getName().endsWith(".java")) {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                boolean modified = false;

                // SeatClass
                if (content.contains("com.airlines.go7api.responsego7.common.SeatClass")) {
                    content = content.replace("com.airlines.go7api.responsego7.common.SeatClass", "com.airlines.go7api.responsego7.SeatAvailabilityRspGo7Dto.SeatClass");
                    modified = true;
                }
                
                // SeatMapFare
                if (content.contains("com.airlines.go7api.responsego7.common.SeatMapFare")) {
                    content = content.replace("com.airlines.go7api.responsego7.common.SeatMapFare", "com.airlines.go7api.responsego7.SeatAvailabilityRspGo7Dto.SeatMapFare");
                    modified = true;
                }
                
                // FlightClass
                if (content.contains("com.airlines.go7api.responsego7.common.FlightClass")) {
                    content = content.replace("com.airlines.go7api.responsego7.common.FlightClass", "com.airlines.go7api.responsego7.AirshopRspGo7Dto.FlightClass");
                    modified = true;
                }

                if (modified) {
                    Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
                    System.out.println("Reverted in: " + file.getAbsolutePath());
                }
            }
        }
    }
}
