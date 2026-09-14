import java.io.File;
import java.nio.file.Files;

public class PrintStrayLines {
    public static void main(String[] args) throws Exception {
        File f1 = new File("src/main/java/com/airlines/go7api/response/ChangeSeatResponse.java");
        File f2 = new File("src/main/java/com/airlines/go7api/response/SeatAvailabilityResponse.java");
        
        String[] lines1 = new String(Files.readAllBytes(f1.toPath())).split("\\n");
        for (int i=0; i<lines1.length; i++) {
            if (lines1[i].contains("com.airlines.go7api.responsego7.")) {
                System.out.println("ChangeSeatResponse:" + (i+1) + " -> " + lines1[i].trim());
            }
        }
        
        String[] lines2 = new String(Files.readAllBytes(f2.toPath())).split("\\n");
        for (int i=0; i<lines2.length; i++) {
            if (lines2[i].contains("com.airlines.go7api.responsego7.")) {
                System.out.println("SeatAvailabilityResponse:" + (i+1) + " -> " + lines2[i].trim());
            }
        }
    }
}
