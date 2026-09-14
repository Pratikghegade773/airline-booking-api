import java.nio.file.*;
import java.util.regex.*;
import java.util.*;

public class ExtractRefactor {
    public static void main(String[] args) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get("C:\Users\prati\.gemini\antigravity\brain\3478f26f-06a8-4cb6-ac0f-bd8f05dd258e\.system_generated\logs\transcript.jsonl")));
        
        String[] files = {"OfferPriceReq.java", "ChangeServiceReq.java", "AirshopReq.java"};
        for (String f : files) {
            System.out.println("Looking for " + f);
        }
    }
}
