package com.airlines.go7api.responsedto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaxDetailDTO {
    private String paxId;
    private String ptc;
    private String gender;
    private String title;
    private String givenName;
    private String middleName;
    private String surname;
    private String loyaltyAccountNumber;
    private String infantRef;
    private String language;
    private String birthDate;
    private String contactInfoRef;
    private String countryDialingCode;
    private AddressDTO address;
    private List<PhoneDTO> phones;
    private List<EmailDTO> emails;
    private List<IdentityDocumentDTO> identityDocument;
    private List<TicketDocInfoDTO> ticketDocInfo;
    private List<EMDInfoDTO> emdInfo;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AddressDTO {
        private String label;
        private String street;
        private String postalCode;
        private String cityName;
        private String stateName;
        private String countryName;
        private String countryCode;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PhoneDTO {
        private String label;
        private String phoneNumber;
        private String type;

    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EmailDTO {
        private String label;
        private String emailAddress;
        private String type;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IdentityDocumentDTO {
        private String identityDocumentNumber;
        private String identityDocumentType;
        private String issuingCountryCode;
        private String citizenshipCountryCode;
        private String issueDate;
        private String expiryDate;
        private String birthDate;
        private String birthPlace;
    }
}
