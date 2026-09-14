import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public class ReadGetters {
    public static void main(String[] args) throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get("getters_output.txt"));
        String content = new String(bytes, StandardCharsets.UTF_16LE);
        String[] lines = content.split("\\r?\\n");
        java.util.Set<String> unique = new java.util.TreeSet<>();
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                unique.add(line.trim());
            }
        }
        for (String u : unique) {
            System.out.println(u);
        }
    }
}
