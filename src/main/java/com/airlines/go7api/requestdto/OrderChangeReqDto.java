package com.airlines.go7api.requestdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.airlines.go7api.requestdto.common.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderChangeReqDto {
    private String owner;
    private String responseId;
    private String orderId;
    private String agencyId;
    private String agentId;
    private String apiKey;
    private String orderChangeUrl;
    private String agencyName;


    private List<DeleteOrderItemDto> deleteOrderItems;
    private PaymentInformationReqDto paymentInformation;
    private List<Offer> offers;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Offer {
        private String offerId;
        private List<OfferItemDto> offerItems;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OfferItemDto {
            private String offerItemId;
            private String ptc;
            private List<String> paxRefs;
            private Long row;
            private String column;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfferItemDto {
        private String offerItemId;
        private String ptc;
        private List<String> paxRefs;
        private List<String> paxRef;
        private Long row;
        private String column;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteOrderItemDto {
        private String orderItemId;
        private List<String> paxRefs;
    }

    private String paymentType;

    

    private List<PaxReqDto> passengers;

    

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SSR {
        private String ssrCode;
        private String countryCode;
        private String freeText;
        private String email;
        private String gstNumber;
        private String phoneNumber;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String zipCode;
        private String actionCode;
        private String passengerReference;
        private String segmentReference;
        private String registeredCompanyName;
        private BigInteger numberOfServices;
    }
}