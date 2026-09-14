import java.io.*;
import java.util.*;
import javax.tools.*;

public class CompilerCheck {
    public static void main(String[] args) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        
        List<File> files = new ArrayList<>();
        collectFiles(new File("src/main/java/com/airlines/go7api/response"), files);
        collectFiles(new File("src/main/java/com/airlines/go7api/responsego7"), files);
        
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(files);
        
        List<String> options = Arrays.asList("-cp", ".;lib/*");
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);
        
        boolean success = task.call();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                System.out.println(diagnostic.getSource().getName() + ":" + diagnostic.getLineNumber());
                System.out.println(diagnostic.getMessage(null));
            }
        }
        System.out.println("Success: " + success);
    }
    
    private static void collectFiles(File dir, List<File> files) {
        if (dir.isDirectory()) {
            for (File child : dir.listFiles()) {
                collectFiles(child, files);
            }
        } else if (dir.getName().endsWith(".java")) {
            files.add(dir);
        }
    }
}
