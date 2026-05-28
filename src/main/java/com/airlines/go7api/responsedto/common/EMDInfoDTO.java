package com.airlines.go7api.responsedto.common;

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
public class EMDInfoDTO {
    private String ptc;
    private String validatingCarrier;
    private List<String> paxId;
    private String agentIdType;
    private String agentId;
    private String issuingAirlineName;
    private String issuingPlace;
    private String issuingCountry;
    private List<TicketDocumentDTO> ticketDocument;
    private String paymentRefId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TicketDocumentDTO {
        private String connectedDocNbr;
        private String ticketDocNbr;
        private String primaryDocInd;
        private String exchTicketNbrInd;
        private String type;
        private int numberOfBooklets;
        private String dateOfIssue;
        private String timeOfIssue;
        private String ticketingLocation;
        private List<String> endorsements;
        private List<CouponInfoDTO> couponInfo;
        private String reportingType;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class CouponInfoDTO {
            private String validatingCarrier;
            private String classType;
            private String rbdCode;
            private int couponNumber;
            private String couponReference;
            private List<String> serviceRefs;
            private String fareBasisCode;
            private String rbd;
            private String stp;
            private String name;
            private String rfic;
            private String rfisc;
            private String couponMedia;
            private String status;
            private String nvb;
            private String nva;
            private String column;
            private BigInteger row;
            private List<SeatCharacteristic> seatCharacteristics;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class SeatCharacteristic {
                private String code;
                private String description;
            }

            private List<CurrentAirlineInfoDTO> currentAirlineInfo;
            private List<AllowableBagDTO> allowableBags;
            private List<BaggageAllowance> baggageAllowances;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class BaggageAllowance {
                private String baggageAllowanceId;
                private String ptc;
                private String passengerId;
                private String name;
                private String category;
                private String quantity;
                private List<Weight> weight;
                private List<Dimension> dimensions;
                private List<DescriptionDTO> descriptions;

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public static class Weight {
                    private BigDecimal value;
                    private String unit;
                    private String uom;
                }

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public static class Dimension {
                    private String unit;
                    private String category;
                    private BigDecimal value;
                    private String uom;
                }

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public static class DescriptionDTO {
                    private String description;
                    private String text;
                    private String label;
                }
            }

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class CurrentAirlineInfoDTO {
                private String departureAirportCode;
                private String arrivalAirportCode;
                private String departureDate;
                private String departureTime;
                private String departureAirportName;
                private String departureTerminal;
                private String arrivalDate;
                private String arrivalTime;
                private String arrivalAirportName;
                private String arrivalTerminal;
                private String changeOfDay;
                private String marketingCarrierAirlineId;
                private String marketingCarrierName;
                private String operatingCarrierAirlineId;
                private String operatingCarrierName;
                private String flightNumber;
                private String equipmentAircraftCode;
                private String equipmentName;
                private String status;
            }

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class AllowableBagDTO {
                private int number;
                private String type;
            }
        }
    }
}
