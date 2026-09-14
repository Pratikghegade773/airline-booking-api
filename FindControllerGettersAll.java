import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.TreeSet;

public class FindControllerGettersAll {
    public static void main(String[] args) throws Exception {
        File file = new File("src/main/java/com/airlines/go7api/controller/Go7Controller.java");
        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

        Pattern p = Pattern.compile("\\.get([A-Z][a-zA-Z0-9_]*)\\(\\)");
        Matcher m = p.matcher(content);
        Set<String> uniqueGetters = new TreeSet<>();
        while (m.find()) {
            uniqueGetters.add(m.group(1));
        }
        for (String g : uniqueGetters) {
            System.out.println(g);
        }
    }
}
