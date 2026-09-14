import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FixControllerLoops {
    public static void main(String[] args) throws Exception {
        File file = new File("src/main/java/com/airlines/go7api/controller/Go7Controller.java");
        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

        // Find .getFlights() in for loops
        Pattern p1 = Pattern.compile("for\\s*\\([^:]+:\\s*[^)]+\\.getFlights\\(\\)\\s*\\)");
        Matcher m1 = p1.matcher(content);
        StringBuffer sb1 = new StringBuffer();
        while (m1.find()) {
            String match = m1.group();
            String replacement = match.substring(0, match.lastIndexOf(")")) + ".getFlight())";
            m1.appendReplacement(sb1, Matcher.quoteReplacement(replacement));
        }
        m1.appendTail(sb1);
        content = sb1.toString();

        // Find .getPassengers() in for loops
        Pattern p2 = Pattern.compile("for\\s*\\([^:]+:\\s*[^)]+\\.getPassengers\\(\\)\\s*\\)");
        Matcher m2 = p2.matcher(content);
        StringBuffer sb2 = new StringBuffer();
        while (m2.find()) {
            String match = m2.group();
            String replacement = match.substring(0, match.lastIndexOf(")")) + ".getPassenger())";
            m2.appendReplacement(sb2, Matcher.quoteReplacement(replacement));
        }
        m2.appendTail(sb2);
        content = sb2.toString();
        
        // Find .getItems() in for loops ?
        Pattern p3 = Pattern.compile("for\\s*\\([^:]+:\\s*[^)]+\\.getItems\\(\\)\\s*\\)");
        Matcher m3 = p3.matcher(content);
        StringBuffer sb3 = new StringBuffer();
        while (m3.find()) {
            String match = m3.group();
            String replacement = match.substring(0, match.lastIndexOf(")")) + ".getFlight())";
            m3.appendReplacement(sb3, Matcher.quoteReplacement(replacement));
        }
        m3.appendTail(sb3);
        content = sb3.toString();

        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        System.out.println("Go7Controller loops updated.");
    }
}
