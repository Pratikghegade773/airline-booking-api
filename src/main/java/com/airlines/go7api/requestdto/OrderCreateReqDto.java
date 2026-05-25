package com.airlines.go7api.requestdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCreateReqDto {

    private String agentId;
    private String agencyId;
    private String agencyName;


    private String apiKey;
    private String clientKey;
    private String orderCreateUrl;
    private String soapAction;

    private int httpResponseCode;

    private String owner;
    private String responseId;
    private String offerId;
    private String serviceResponseId;
    private String serviceOfferId;
    private String seatResponseId;
    private String seatOfferId;
    private List<OfferItemDto> offerItems;
    private PaymentInformation paymentInformation;
    private int httpResponse;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfferItemDto {
        private String offerItemId;
        private String ptc;
        private List<Object> paxRefs;
        private SpecialServices specialServices;
        private String column;
        private BigInteger row;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SpecialServices {
            private BigDecimal qty;
            private String text;
            private String name;
            private String serviceDefId;
            private List<String> segId;
        }
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
        private String currency;
    }

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
        private String language;
        private String loyaltyAccountNumber;
        private String infantRef;
        private String countryDialingCode;
        private BigDecimal areaCode;
        private BigDecimal phoneNumber;
        private IdentityDocument identityDocument;
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

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class IdentityDocument {
            private String identityDocumentNumber;
            private String identityDocumentType;
            private String issuingCountryCode;
            private String citizenshipCountryCode;
            private String residenceCountryCode;
            private String issueDate;
            private String expiryDate;
            private String birthDate;
            private String birthPlace;
            private String gender;
        }
    }

}
