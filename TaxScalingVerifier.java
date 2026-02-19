import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class TaxScalingVerifier {
    public static void main(String[] args) {
        // Inputs
        BigDecimal unitTax = new BigDecimal("25.00");
        int count = 2;
        BigDecimal totalTaxReported = unitTax.multiply(new BigDecimal(count)); // 50.00

        Map<String, String> breakdown = new HashMap<>();
        breakdown.put("YQ", "12.00");
        breakdown.put("I2", "8.00");
        // Sum is 20.00, but unitTax is 25.00. Scale factor should be 1.25.

        BigDecimal breakdownSum = BigDecimal.ZERO;
        for (String val : breakdown.values()) {
            breakdownSum = breakdownSum.add(new BigDecimal(val));
        }

        BigDecimal scaleFactor = BigDecimal.ONE;
        if (breakdownSum.compareTo(BigDecimal.ZERO) > 0) {
            // This is the logic from AirshopResponse.java
            BigDecimal unitTaxFromTotal = totalTaxReported.divide(new BigDecimal(count), 10, RoundingMode.HALF_UP);
            scaleFactor = unitTaxFromTotal.divide(breakdownSum, 10, RoundingMode.HALF_UP);
        }

        System.out.println("Scale Factor: " + scaleFactor);

        BigDecimal finalSum = BigDecimal.ZERO;
        for (Map.Entry<String, String> entry : breakdown.entrySet()) {
            BigDecimal rawUnitTax = new BigDecimal(entry.getValue());
            BigDecimal scaledUnitTax = rawUnitTax.multiply(scaleFactor).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalTaxAmount = scaledUnitTax.multiply(new BigDecimal(count));

            System.out.println("Tax " + entry.getKey() + ": Raw=" + entry.getValue() + ", ScaledUnit=" + scaledUnitTax
                    + ", Total=" + totalTaxAmount);
            finalSum = finalSum.add(totalTaxAmount);
        }

        System.out.println("Reported Total Tax: " + totalTaxReported);
        System.out.println("Sum of Scaled Taxes: " + finalSum);

        if (totalTaxReported.compareTo(finalSum) == 0) {
            System.out.println("SUCCESS: Breakdown sums up to total tax.");
        } else {
            System.out.println("FAILURE: Mismatch detected!");
        }
    }
}
