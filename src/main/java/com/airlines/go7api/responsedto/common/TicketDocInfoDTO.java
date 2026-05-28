package com.airlines.go7api.responsedto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketDocInfoDTO {
    private List<String> paxId;
    private String validatingCarrier;
    private String ptc;
    private String agentIdType;
    private String agentId;
    private String issuingAirlineName;
    private String issuingPlace;
    private String issuingCountry;
    private List<TicketDocumentDTO> ticketDocument;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TicketDocumentDTO {

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
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class CouponInfoDTO {
            private int couponNumber;
            private String classType;
            private String rbdCode;
            private String couponReference;
            private String fareBasisCode;
            private String rbd;
            private String stp;
            private String couponMedia;
            private String status;
            private String nvb;
            private String nva;
            private List<CurrentAirlineInfoDTO> currentAirlineInfo;
            private String validatingCarrier;
            private String validatingArline;
            private List<AllowableBagDTO> allowableBags;
            private List<BaggageAllowance> baggageAllowances;

            @Data
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
                private String operatingCarrierAirlineId;
                private String marketingCarrierName;
                private String operatingCarrierName;
                private String flightNumber;
                private String equipmentAircraftCode;
                private String equipmentName;
                private String status;
            }

            @Data
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class AllowableBagDTO {
                private int number;
                private String type;
            }
        }
    }
}