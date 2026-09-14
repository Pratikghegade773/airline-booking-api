import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

public class Fixer {
    public static void main(String[] args) throws Exception {
        // Fix common directory static classes
        String commonDir = "src/main/java/com/airlines/go7api/responsedto/common";
        List<Path> commonFiles = Files.walk(Paths.get(commonDir))
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".java"))
            .collect(Collectors.toList());
            
        for (Path file : commonFiles) {
            String className = file.getFileName().toString().replace(".java", "");
            String content = new String(Files.readAllBytes(file));
            
            // First, make everything static just in case
            content = content.replace("public class", "public static class");
            
            // Then make ONLY the top-level class non-static
            content = content.replaceFirst("public static class " + className, "public class " + className);
            
            Files.write(file, content.getBytes());
        }
        System.out.println("Fixed common package statics");
        
        // Fix mappers
        String responseDir = "src/main/java/com/airlines/go7api/response";
        List<Path> responseFiles = Files.walk(Paths.get(responseDir))
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".java"))
            .collect(Collectors.toList());
            
        String regex = "[A-Za-z]+RspDto\\.(PaxDetailDTO|OrderItemsDTO|Service|EMDInfoDTO|TicketDocInfoDTO|OD|BookingReferences|SpecialServiceRequest|PaymentsDTO|PriceClass|OtherServiceInformation)";
        Pattern pattern = Pattern.compile(regex);
        
        for (Path file : responseFiles) {
            String content = new String(Files.readAllBytes(file));
            String newContent = pattern.matcher(content).replaceAll("$1");
            
            if (!newContent.equals(content)) {
                Files.write(file, newContent.getBytes());
                System.out.println("Fixed references in " + file.getFileName());
            }
        }
        
        // Also strip the nested classes correctly from AirshopRspDto, OrderReshopRspDto, OfferPriceRspDto
        // We will just do a simple replacement for them using EXACT matched string blocks to be safe
        // Or actually, just let them be! If they don't have those nested classes, they don't have them!
        // Wait, AirshopRspDto DOES have some nested classes that might be duplicates?
        // Let's just leave the DTOs alone for now and see if the compile passes. 
        // Stripper already stripped most of them.
    }
}
