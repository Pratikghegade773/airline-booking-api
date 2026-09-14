import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class RenameSeatField {
    public static void main(String[] args) throws Exception {
        File dir = new File("src/main/java/com/airlines/go7api");
        processDirectory(dir);
        System.out.println("Seat get/set renamed.");
    }

    private static void processDirectory(File dir) throws Exception {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                processDirectory(file);
            } else if (file.getName().endsWith(".java")) {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                boolean modified = false;

                if (content.contains("s.getSeat()")) {
                    content = content.replace("s.getSeat()", "s.getSeatNumber()");
                    modified = true;
                }
                if (content.contains("matchedSeat.getSeat()")) {
                    content = content.replace("matchedSeat.getSeat()", "matchedSeat.getSeatNumber()");
                    modified = true;
                }
                if (content.contains("s.setSeat(")) {
                    content = content.replace("s.setSeat(", "s.setSeatNumber(");
                    modified = true;
                }
                if (content.contains("matchedSeat.setSeat(")) {
                    content = content.replace("matchedSeat.setSeat(", "matchedSeat.setSeatNumber(");
                    modified = true;
                }

                if (modified) {
                    Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
                    System.out.println("Renamed in: " + file.getName());
                }
            }
        }
    }
}
