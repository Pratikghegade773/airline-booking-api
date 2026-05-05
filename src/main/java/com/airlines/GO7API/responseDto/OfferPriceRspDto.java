package com.airlines.GO7API.responseDto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OfferPriceRspDto {

    private List<String> warning;

    private String responseId;
    private String apiOwner;
    private String pricedOfferId;
    private String validatingCarrier;
    private BigDecimal totalPrice;
    private BigDecimal totalTaxes;
    private String currency;
    private String offerPriceExpiration;
    private String paymentTimeLimit;
    private String ticketedByTimeLimit;
    private List<OD> ods;
    private List<OfferItemDto> offerItems;
    private List<PriceClassList> priceClassList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OD {
        private String classType;
        private String cabinType;
        private String fareBasisCode;
        private String priceClassId;
        private String segmentId;
        private String negotiatedCode;
        private String rbdCode;
        private String odKey;
        private String origin;
        private String destination;
        private String originAirportName;
        private String destinationAirportName;
        private String departureDate;
        private String arrivalDate;
        private String departureTime;
        private String arrivalTime;
        private String journeyTime;
        private String equipment;
        private String flightNumber;
        private String marketingCarrierName;
        private Object marketingCarrierCode;
        private String marketingCarrierLogo;
        private String operatingCarrierName;
        private Object operatingCarrierCode;
        private String operatingCarrierLogo;
        private String arrivalTerminal;
        private String departureTerminal;
        private String operatingCarrierFlightNumber;
        private int changeOfDay;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OfferItemDto {
        private String offerItemId;
        private String ptc;
        private List<String> paxref;
        private BigDecimal totalPrice;
        private List<String> passengerIds;
        private TotalFare totalFare;
        private BaseFare baseFare;
        private TotalTax totalTax;
        private List<Tax> taxes;
        private List<BaggageAllowance> baggageAllowances;
        private List<String> segmentRefs;
        private List<String> givenName;
        private String currency;

        // private List<PriceClassReference> priceClassReferences;

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
        public static class TotalTax {
            private BigDecimal amount;
            private String currency;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Tax {
            private String description;
            private BigDecimal amount;
            private String currency;
            private String code;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class BaggageAllowance {
            private String baggageAllowanceId;
            private String ptc;
            private List<String> passengerId;
            private String category;
            private String quantity;
            private List<Weight> weight;
            // private List<Dimension> dimension;
            private List<DescriptionDTO> descriptions;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class Weight {
                private BigDecimal value;
                private String uom;
            }

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class DescriptionDTO {
                private String description;
            }
        }

        private String cabinTypeCode;
        private String cabinType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PriceClassList {
        private String priceClassId;
        private String className;
        private String cabinTypeCode;
        private List<Description> descriptions;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Description {
            private String odKey;
            private String text;
        }
    }
}
