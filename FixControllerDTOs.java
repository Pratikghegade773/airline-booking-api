import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class FixControllerDTOs {
    public static void main(String[] args) throws Exception {
        File file = new File("src/main/java/com/airlines/go7api/controller/Go7Controller.java");
        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

        content = content.replace("com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto.BalanceInformation", "com.airlines.go7api.responsego7.common.BalanceInformation");
        content = content.replace("com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto.Passengers", "com.airlines.go7api.responsego7.common.Passengers");
        content = content.replace("com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto.Passenger", "com.airlines.go7api.responsego7.common.Passenger");
        content = content.replace("com.airlines.go7api.responsego7.ChangeSeatRspGo7Dto.Flight", "com.airlines.go7api.responsego7.common.Flight");
        content = content.replace("com.airlines.go7api.responsego7.ChangeSeatRspGo7Dto.Seat", "com.airlines.go7api.responsego7.common.Seat");
        content = content.replace("com.airlines.go7api.responsego7.ChangeServiceRspGo7Dto.Aerocrs", "com.airlines.go7api.responsego7.common.Aerocrs");

        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        System.out.println("Go7Controller updated.");
    }
}
