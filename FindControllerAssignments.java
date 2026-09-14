import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FindControllerAssignments {
    public static void main(String[] args) throws Exception {
        File file = new File("src/main/java/com/airlines/go7api/controller/Go7Controller.java");
        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

        Pattern p = Pattern.compile("([^;\\n]+=\\s*[^;\\n]+(?:getFlights\\(\\)|getPassengers\\(\\)|getItems\\(\\))[^;\\n]*)");
        Matcher m = p.matcher(content);
        while (m.find()) {
            System.out.println(m.group(1).trim());
        }
    }
}
