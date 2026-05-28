package com.airlines.go7api.requestdto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaxReqDto {
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
    private IdentityDocument identityDocument;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdentityDocument {
        private String identityDocumentType;
        private String identityDocumentNumber;
        private String expiryDate;
        private String issuingCountryCode;
        private String citizenshipCountryCode;
    }

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
