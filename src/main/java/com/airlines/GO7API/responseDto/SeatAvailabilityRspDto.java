package com.airlines.go7api.responsedto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeatAvailabilityRspDto {
    private List<String> warnings;
    private String offerExpiration;
    private String responseId;
    private  String orderId;
    private String validatingCarrier;
    private String apiOwner;
    private String offerId;
    private List<OfferItem> offerItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OfferItem {
        private String offerItemId;
        private List<String> segmentRefs;
        private List<String> paxref;
        private List<String> givenName;
        private String currency;
        List<Compartment> compartmentList;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Compartment {
            private String cabinType;
            private BigInteger firstRow;
            private BigInteger lastRow;
            private BigInteger totalRow;
            private List<Seat> seat;


            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class Seat {
                private BaseFare baseFare;
                private TotalTax totalTax;
                private List<Tax> taxes;
                private TotalFare totalFare;
                private String occupancyCode;
                private String columId;
                private String rowNumber;
                private List<SeatCharacteristic> seatCharacteristics;

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
                    private String description;
                }

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public static class SeatCharacteristic {
                    private String code;
                    private String description;
                }
            }
        }
    }
}

