import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class StripBaggageAllowance {
    public static void main(String[] args) throws Exception {
        File orderItems = new File("src/main/java/com/airlines/go7api/responsedto/common/OrderItemsDTO.java");
        stripClass(orderItems, "BaggageAllowance");

        File ticketDoc = new File("src/main/java/com/airlines/go7api/responsedto/common/TicketDocInfoDTO.java");
        stripClass(ticketDoc, "BaggageAllowance");
        
        System.out.println("Stripped BaggageAllowance");
    }

    private static void stripClass(File file, String className) throws Exception {
        if (!file.exists()) return;
        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        String declaration = "public static class " + className;
        int idx = content.indexOf(declaration);
        if (idx == -1) return;

        // find the preceding annotations
        int startIdx = idx;
        while (startIdx > 0 && (content.charAt(startIdx - 1) == '@' || Character.isWhitespace(content.charAt(startIdx - 1)) || Character.isLetter(content.charAt(startIdx - 1)))) {
            startIdx--;
            if (content.substring(startIdx, idx).contains("public static class")) break; // safety
        }
        
        // Find the start of annotations accurately
        int annotationStart = content.lastIndexOf("@Data", idx);
        if (annotationStart != -1 && annotationStart > idx - 100) {
            startIdx = annotationStart;
        }

        // find the closing brace
        int openBraces = 0;
        int endIdx = idx;
        boolean started = false;
        for (int i = idx; i < content.length(); i++) {
            if (content.charAt(i) == '{') {
                openBraces++;
                started = true;
            } else if (content.charAt(i) == '}') {
                openBraces--;
            }
            if (started && openBraces == 0) {
                endIdx = i + 1;
                break;
            }
        }

        String newContent = content.substring(0, startIdx) + content.substring(endIdx);
        Files.write(file.toPath(), newContent.getBytes(StandardCharsets.UTF_8));
    }
}
