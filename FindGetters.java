import java.io.File;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FindGetters {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java/com/airlines/go7api/response");
        Pattern aerocrsPattern = Pattern.compile("getAerocrs\\(\\)\\.get([A-Z]\\w+)\\(\\)");
        Pattern flightPattern = Pattern.compile("flight\\.get([A-Z]\\w+)\\(\\)");
        Pattern bookingPattern = Pattern.compile("getBooking\\(\\)\\.get([A-Z]\\w+)\\(\\)");
        Pattern booking2Pattern = Pattern.compile("booking\\.get([A-Z]\\w+)\\(\\)");

        for (File file : dir.listFiles()) {
            if (file.getName().endsWith(".java")) {
                String content = new String(Files.readAllBytes(file.toPath()));
                
                Matcher m = aerocrsPattern.matcher(content);
                while (m.find()) {
                    System.out.println("Aerocrs." + m.group(1) + " in " + file.getName());
                }
                
                m = flightPattern.matcher(content);
                while (m.find()) {
                    System.out.println("Flight." + m.group(1) + " in " + file.getName());
                }
                
                m = bookingPattern.matcher(content);
                while (m.find()) {
                    System.out.println("Booking." + m.group(1) + " in " + file.getName());
                }
                
                m = booking2Pattern.matcher(content);
                while (m.find()) {
                    System.out.println("Booking." + m.group(1) + " in " + file.getName());
                }
            }
        }
    }
}
