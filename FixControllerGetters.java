import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FixControllerGetters {
    public static void main(String[] args) throws Exception {
        File file = new File("src/main/java/com/airlines/go7api/controller/Go7Controller.java");
        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

        content = content.replace(".getFlightnumber()", ".getNumber()");
        content = content.replace(".getFromLocation()", ".getFrom()");
        content = content.replace(".getToLocation()", ".getTo()");
        content = content.replace(".getGetFlight()", ".getFlights()");

        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        System.out.println("Go7Controller getters updated.");
    }
}
