package com.airlines.GO7API.requestDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderChangeReqDto {
    private String owner;
    private String responseId;
    private String orderId;
    // private String offerId;
    private String agencyId;
    private String agentId;
    private String apiKey;
    private String orderChangeUrl;
    private String agencyName;


    private List<DeleteOrderItemDto> deleteOrderItems;
    private PaymentInformation paymentInformation;
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
        private String dob;
        private String gender;
        private String title;
        private String firstName;
        private String middleName;
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
