import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class ImportAdder {
    public static void main(String[] args) throws Exception {
        String responseDir = "src/main/java/com/airlines/go7api/response";
        List<Path> responseFiles = Files.walk(Paths.get(responseDir))
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".java"))
            .collect(Collectors.toList());
            
        for (Path file : responseFiles) {
            String content = new String(Files.readAllBytes(file));
            
            if (!content.contains("import com.airlines.go7api.responsedto.common.*;")) {
                content = content.replace("package com.airlines.go7api.response;", "package com.airlines.go7api.response;\n\nimport com.airlines.go7api.responsedto.common.*;");
                Files.write(file, content.getBytes());
                System.out.println("Added import to " + file.getFileName());
            }
        }
    }
}
