import java.io.File;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FindStrayDTOsController {
    public static void main(String[] args) throws Exception {
        File file = new File("src/main/java/com/airlines/go7api/controller/Go7Controller.java");
        Pattern p = Pattern.compile("com\\.airlines\\.go7api\\.responsego7\\.([A-Za-z0-9_]+)\\.([A-Za-z0-9_]+)");

        String content = new String(Files.readAllBytes(file.toPath()));
        Matcher m = p.matcher(content);
        while (m.find()) {
            System.out.println("Found stray DTO reference: " + m.group(0) + " in " + file.getName());
        }
    }
}
