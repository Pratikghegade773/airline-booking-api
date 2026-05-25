package com.airlines.go7api.responsedto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderReshopRspDto {

    private List<String> warnings;
    private String responseId;
    private String apiOwner;
    private String roundTripType;
    private List<Offer> offers;
    private String xmlRQ;
    private String xmlRS;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Offer {
        private String offerId;
        private String classType;
        private String cabinTypeCode;
        private String validatingCarrier;
        private String offerExpiration;
        private String paymentTimeLimit;
        private String ticketedByTimeLimit;
        private List<OD> ods;
        private BigDecimal totalPrice;
        private String currency;
        private List<OfferItemDto> offerItems;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class OD {
            private String fareBasisCode;
            private String rbdCode;
            private String cabinType;
            private String odKey;
            private String priceClassId;
            private String origin;
            private String segmentId;
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
            private String marketingCarrierCode;
            private String marketingCarrierLogo;
            private String operatingCarrierName;
            private String operatingCarrierCode;
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
            private List<String> passengerRefs;
            private String ptc;
            private BigDecimal totalPrice;
            private List<BaggageAllowance> baggageAllowances;
            private List<PriceClassReference> priceClassReferences;
            private List<FareDetail> fareDetail;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class FareDetail {
                private Price price;

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public static class Price {
                    private TotalFare totalFare;
                    private BaseFare baseFare;
                    private TotalTax totalTax;
                    private List<Taxes> taxes;
                    private List<Qsurcharges> qSurCharges;

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
                    public static class Taxes {
                        private BigDecimal total;
                        private String code;
                        private BigDecimal amount;
                        private String currency;
                        private String description;
                    }
                }
            }

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class PriceClassReference {
                private String priceClassId;
                private String className;
                private String cabinTypeCode;
                private List<Description> descriptions;

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public static class Description {
                    private String text;
                }
            }

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class BaggageAllowance {
                private String baggageAllowanceId;
                private String ptc;
                private List<String> segmentrefId;
                private List<String> passengerId;
                private String category;
                private List<Weight> weight;
                private String quantity;
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
        }
    }
}
