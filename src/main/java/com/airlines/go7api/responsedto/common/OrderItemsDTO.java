package com.airlines.go7api.responsedto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderItemsDTO {
    private String orderItemId;
    private String ptc;
    private BigDecimal totalPrice;
    private List<String> passengerIds;
    private String className;
    private String timeStamp;
    private TotalFare totalFare;
    private BaseFare baseFare;
    private TotalTax totalTax;
    private List<Tax> taxes;
    private List<Qsurcharges> qSurCharges;
    private List<String> endorsements;
    private List<Service> serviceList;
    private List<BaggageAllowance> baggageAllowances;
    private List<FareDetail> fareDetail;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FareDetail {
        private DiffPrice diffPrice;
        private NewPrice newPrice;
        private OldPrice oldPrice;
        private Penalty penalty;
        private DifferentialDueByAirline airlineDue;
        private DifferentialDueByPassenger passengerDue;
        private TotalAmount totalAmount;
        private List<String> paxrefId;

        @Data
        public static class DiffPrice {
            private TotalFare totalFare;
            private TotalTax totalTax;
            private BaseFare baseFare;
            private List<Tax> taxes;
        }

        @Data
        public static class OldPrice {
            private TotalFare totalFare;
            private TotalTax totalTax;
            private BaseFare baseFare;
            private List<Tax> taxes;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Penalty {
            private BigDecimal amount;
            private String currency;
            private String amountType;
            private String purpose;
        }

        @Data
        public static class DifferentialDueByAirline {
            private BigDecimal amount;
            private String currency;
        }

        @Data
        public static class DifferentialDueByPassenger {
            private BigDecimal amount;
            private String currency;
        }

        @Data
        public static class TotalAmount {
            private BigDecimal amount;
            private String currency;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class NewPrice {
            private TotalFare totalFare;
            private TotalTax totalTax;
            private BaseFare baseFare;
            private List<Tax> taxes;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TotalTax {
        private BigDecimal amount;
        private String currency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TotalFare {
        private BigDecimal amount;
        private String currency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BaseFare {
        private BigDecimal amount;
        private String currency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Tax {
        private String code;
        private BigDecimal amount;
        private String currency;
        private String nation;
        private String taxCode;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Qsurcharges {
        private BigDecimal amount;
        private String currency;
        private List<Fee> fee;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Fee {
            private BigDecimal amount;
            private String currency;
            private String designator;
            private String description;
        }
    }

}
