import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class FindOverreach {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java/com/airlines/go7api");
        processDirectory(dir);
    }

    private static void processDirectory(File dir) throws Exception {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                processDirectory(file);
            } else if (file.getName().endsWith(".java")) {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                
                // Looking for common.Something that shouldn't be there
                String[] words = content.split("[^A-Za-z0-9_.]");
                for (String w : words) {
                    if (w.contains("common.")) {
                        int idx = w.indexOf("common.");
                        String after = w.substring(idx + 7);
                        if (!after.equals("Flight") &&
                            !after.equals("Flights") &&
                            !after.equals("Passenger") &&
                            !after.equals("Passengers") &&
                            !after.equals("Booking") &&
                            !after.equals("Aerocrs") &&
                            !after.equals("Seat") &&
                            !after.equals("BalanceInformation") &&
                            !after.equals("Taxes") &&
                            !after.equals("Items") &&
                            !after.equals("Detail")) {
                            if (!after.isEmpty()) {
                                System.out.println("Possible overreach in " + file.getName() + ": " + w);
                            }
                        }
                    }
                }
            }
        }
    }
}
