import java.io.File;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FindAllGetters {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java/com/airlines/go7api/response");
        Pattern p = Pattern.compile("(?:flight|f|booking|pax|detail|seat)\\.get([A-Z]\\w+)\\(\\)");

        for (File file : dir.listFiles()) {
            if (file.getName().endsWith(".java")) {
                String content = new String(Files.readAllBytes(file.toPath()));
                Matcher m = p.matcher(content);
                while (m.find()) {
                    System.out.println(m.group(1) + " in " + file.getName());
                }
            }
        }
    }
}
