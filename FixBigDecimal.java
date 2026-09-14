import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class FixBigDecimal {
    public static void main(String[] args) throws Exception {
        File f1 = new File("src/main/java/com/airlines/go7api/response/OrderRetrieveResponse.java");
        fix(f1);
        File f2 = new File("src/main/java/com/airlines/go7api/response/ChangePaymentResponse.java");
        fix(f2);
        System.out.println("Done fixing BigDecimal.");
    }

    private static void fix(File file) throws Exception {
        if (!file.exists()) return;
        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        String oldC = content;
        content = content.replace("w.setValue(new BigDecimal(30));", "w.setValue(\"30\");");
        content = content.replace("w.setValue(new BigDecimal(\"30\"));", "w.setValue(\"30\");");
        if (!content.equals(oldC)) {
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
            System.out.println("Fixed BigDecimal in " + file.getName());
        }
    }
}
