package com.airlines.go7api.requestdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangeSeatReqDto {

    /// Config Variables
    private String subscriptionKey;
    private String apiUrl;
    private String agencyId;
    private String agentId;
    private String apiKey;
    private String changeAncillariesUrl;
    private String agencyName;


    private String owner;
    private String responseId;
    private String orderId;
    private List<Offer> offers;
    private List<OrderItemDto> deleteOrderItems;
    private PaymentInformation paymentInformation;


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
            private BigInteger row;
            private String column;
            private SpecialServices specialServices;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class SpecialServices {
                private BigDecimal qty;
                private String text;
                private String name;
                private String type;
                private String serviceDefId;
                private List<String> segId;
            }
        }
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDto {
        private String orderItemId;
    }


    private String paymentType;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInformation {
        private String cardCode;
        private String cardNumber;
        private String seriesCode;
        private String cardHolderName;
        private String expiration;
        private Address address;
        private String currencyCode;
        private BigDecimal amount;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Address {
            private String street;
            private String city;
            private String state;
            private String postalCode;
            private String countryCode;
            private String country;
        }


    }

    private List<Pax> passengers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pax {
        private String paxId;
        private String ptc;
        private String title;
        private String dob;
        private String gender;
        private String firstName;
        private String lastName;
        private String infantRef;
        private String countryDialingCode;
        private BigDecimal areaCode;
        private BigDecimal phoneNumber;
        private String email;
        private Address1 address;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Address1 {
            private String street;
            private String city;
            private String state;
            private String postalCode;
            private String countryCode;
            private String country;
        }


    }
}
