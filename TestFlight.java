import java.lang.reflect.Method;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

public class TestFlight {
    public static void main(String[] args) throws Exception {
        // Compile Flight.java manually to see if it succeeds
        Process p = Runtime.getRuntime().exec("javac -cp \".;lib/*\" src/main/java/com/airlines/go7api/responsego7/common/Flight.java");
        p.waitFor();
        
        File classesDir = new File("src/main/java");
        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{classesDir.toURI().toURL()});
        Class<?> flightClass = Class.forName("com.airlines.go7api.responsego7.common.Flight", true, classLoader);
        
        System.out.println("Methods:");
        for (Method m : flightClass.getDeclaredMethods()) {
            if (m.getName().toLowerCase().contains("airline")) {
                System.out.println(m.getName());
            }
        }
    }
}
