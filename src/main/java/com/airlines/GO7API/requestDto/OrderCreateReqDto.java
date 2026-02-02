package com.airlines.GO7API.requestDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreateReqDto {

    private String agentId;
    private String agencyId;

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

    private String paymentType;
    private List<Pax> passengers;

    // Confirmation / Hold parameters
    private String agentConfirmation;
    private String remarks;
    private String confirmationEmail;
    private Boolean holdBooking;
    private Boolean sendEmail;

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    public String getOrderCreateUrl() {
        return orderCreateUrl;
    }

    public void setOrderCreateUrl(String orderCreateUrl) {
        this.orderCreateUrl = orderCreateUrl;
    }

    public String getSoapAction() {
        return soapAction;
    }

    public void setSoapAction(String soapAction) {
        this.soapAction = soapAction;
    }

    public int getHttpResponseCode() {
        return httpResponseCode;
    }

    public void setHttpResponseCode(int httpResponseCode) {
        this.httpResponseCode = httpResponseCode;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    public String getOfferId() {
        return offerId;
    }

    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }

    public String getServiceResponseId() {
        return serviceResponseId;
    }

    public void setServiceResponseId(String serviceResponseId) {
        this.serviceResponseId = serviceResponseId;
    }

    public String getServiceOfferId() {
        return serviceOfferId;
    }

    public void setServiceOfferId(String serviceOfferId) {
        this.serviceOfferId = serviceOfferId;
    }

    public String getSeatResponseId() {
        return seatResponseId;
    }

    public void setSeatResponseId(String seatResponseId) {
        this.seatResponseId = seatResponseId;
    }

    public String getSeatOfferId() {
        return seatOfferId;
    }

    public void setSeatOfferId(String seatOfferId) {
        this.seatOfferId = seatOfferId;
    }

    public List<OfferItemDto> getOfferItems() {
        return offerItems;
    }

    public void setOfferItems(List<OfferItemDto> offerItems) {
        this.offerItems = offerItems;
    }

    public PaymentInformation getPaymentInformation() {
        return paymentInformation;
    }

    public void setPaymentInformation(PaymentInformation paymentInformation) {
        this.paymentInformation = paymentInformation;
    }

    public int getHttpResponse() {
        return httpResponse;
    }

    public void setHttpResponse(int httpResponse) {
        this.httpResponse = httpResponse;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public List<Pax> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<Pax> passengers) {
        this.passengers = passengers;
    }

    public String getAgentConfirmation() {
        return agentConfirmation;
    }

    public void setAgentConfirmation(String agentConfirmation) {
        this.agentConfirmation = agentConfirmation;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getConfirmationEmail() {
        return confirmationEmail;
    }

    public void setConfirmationEmail(String confirmationEmail) {
        this.confirmationEmail = confirmationEmail;
    }

    public Boolean getHoldBooking() {
        return holdBooking;
    }

    public void setHoldBooking(Boolean holdBooking) {
        this.holdBooking = holdBooking;
    }

    public Boolean getSendEmail() {
        return sendEmail;
    }

    public void setSendEmail(Boolean sendEmail) {
        this.sendEmail = sendEmail;
    }

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

        public String getOfferItemId() {
            return offerItemId;
        }

        public void setOfferItemId(String offerItemId) {
            this.offerItemId = offerItemId;
        }

        public String getPtc() {
            return ptc;
        }

        public void setPtc(String ptc) {
            this.ptc = ptc;
        }

        public List<Object> getPaxRefs() {
            return paxRefs;
        }

        public void setPaxRefs(List<Object> paxRefs) {
            this.paxRefs = paxRefs;
        }

        public SpecialServices getSpecialServices() {
            return specialServices;
        }

        public void setSpecialServices(SpecialServices specialServices) {
            this.specialServices = specialServices;
        }

        public String getColumn() {
            return column;
        }

        public void setColumn(String column) {
            this.column = column;
        }

        public BigInteger getRow() {
            return row;
        }

        public void setRow(BigInteger row) {
            this.row = row;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SpecialServices {
            private BigDecimal qty;
            private String text;
            private String name;
            private String serviceDefId;
            private List<String> segId;

            public BigDecimal getQty() {
                return qty;
            }

            public void setQty(BigDecimal qty) {
                this.qty = qty;
            }

            public String getText() {
                return text;
            }

            public void setText(String text) {
                this.text = text;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getServiceDefId() {
                return serviceDefId;
            }

            public void setServiceDefId(String serviceDefId) {
                this.serviceDefId = serviceDefId;
            }

            public List<String> getSegId() {
                return segId;
            }

            public void setSegId(List<String> segId) {
                this.segId = segId;
            }
        }
    }

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

        public String getCardCode() {
            return cardCode;
        }

        public void setCardCode(String cardCode) {
            this.cardCode = cardCode;
        }

        public String getCardNumber() {
            return cardNumber;
        }

        public void setCardNumber(String cardNumber) {
            this.cardNumber = cardNumber;
        }

        public String getSeriesCode() {
            return seriesCode;
        }

        public void setSeriesCode(String seriesCode) {
            this.seriesCode = seriesCode;
        }

        public String getCardHolderName() {
            return cardHolderName;
        }

        public void setCardHolderName(String cardHolderName) {
            this.cardHolderName = cardHolderName;
        }

        public String getExpiration() {
            return expiration;
        }

        public void setExpiration(String expiration) {
            this.expiration = expiration;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }

        public String getCurrencyCode() {
            return currencyCode;
        }

        public void setCurrencyCode(String currencyCode) {
            this.currencyCode = currencyCode;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
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

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getPostalCode() {
            return postalCode;
        }

        public void setPostalCode(String postalCode) {
            this.postalCode = postalCode;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }
    }

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

        public String getPaxId() {
            return paxId;
        }

        public void setPaxId(String paxId) {
            this.paxId = paxId;
        }

        public String getPtc() {
            return ptc;
        }

        public void setPtc(String ptc) {
            this.ptc = ptc;
        }

        public String getDob() {
            return dob;
        }

        public void setDob(String dob) {
            this.dob = dob;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getMiddleName() {
            return middleName;
        }

        public void setMiddleName(String middleName) {
            this.middleName = middleName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getLoyaltyAccountNumber() {
            return loyaltyAccountNumber;
        }

        public void setLoyaltyAccountNumber(String loyaltyAccountNumber) {
            this.loyaltyAccountNumber = loyaltyAccountNumber;
        }

        public String getInfantRef() {
            return infantRef;
        }

        public void setInfantRef(String infantRef) {
            this.infantRef = infantRef;
        }

        public String getCountryDialingCode() {
            return countryDialingCode;
        }

        public void setCountryDialingCode(String countryDialingCode) {
            this.countryDialingCode = countryDialingCode;
        }

        public BigDecimal getAreaCode() {
            return areaCode;
        }

        public void setAreaCode(BigDecimal areaCode) {
            this.areaCode = areaCode;
        }

        public BigDecimal getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(BigDecimal phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public IdentityDocument getIdentityDocument() {
            return identityDocument;
        }

        public void setIdentityDocument(IdentityDocument identityDocument) {
            this.identityDocument = identityDocument;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Address1 getAddress() {
            return address;
        }

        public void setAddress(Address1 address) {
            this.address = address;
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

            public String getStreet() {
                return street;
            }

            public void setStreet(String street) {
                this.street = street;
            }

            public String getCity() {
                return city;
            }

            public void setCity(String city) {
                this.city = city;
            }

            public String getState() {
                return state;
            }

            public void setState(String state) {
                this.state = state;
            }

            public String getPostalCode() {
                return postalCode;
            }

            public void setPostalCode(String postalCode) {
                this.postalCode = postalCode;
            }

            public String getCountryCode() {
                return countryCode;
            }

            public void setCountryCode(String countryCode) {
                this.countryCode = countryCode;
            }

            public String getCountry() {
                return country;
            }

            public void setCountry(String country) {
                this.country = country;
            }
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

            public String getIdentityDocumentNumber() {
                return identityDocumentNumber;
            }

            public void setIdentityDocumentNumber(String identityDocumentNumber) {
                this.identityDocumentNumber = identityDocumentNumber;
            }

            public String getIdentityDocumentType() {
                return identityDocumentType;
            }

            public void setIdentityDocumentType(String identityDocumentType) {
                this.identityDocumentType = identityDocumentType;
            }

            public String getIssuingCountryCode() {
                return issuingCountryCode;
            }

            public void setIssuingCountryCode(String issuingCountryCode) {
                this.issuingCountryCode = issuingCountryCode;
            }

            public String getCitizenshipCountryCode() {
                return citizenshipCountryCode;
            }

            public void setCitizenshipCountryCode(String citizenshipCountryCode) {
                this.citizenshipCountryCode = citizenshipCountryCode;
            }

            public String getResidenceCountryCode() {
                return residenceCountryCode;
            }

            public void setResidenceCountryCode(String residenceCountryCode) {
                this.residenceCountryCode = residenceCountryCode;
            }

            public String getIssueDate() {
                return issueDate;
            }

            public void setIssueDate(String issueDate) {
                this.issueDate = issueDate;
            }

            public String getExpiryDate() {
                return expiryDate;
            }

            public void setExpiryDate(String expiryDate) {
                this.expiryDate = expiryDate;
            }

            public String getBirthDate() {
                return birthDate;
            }

            public void setBirthDate(String birthDate) {
                this.birthDate = birthDate;
            }

            public String getBirthPlace() {
                return birthPlace;
            }

            public void setBirthPlace(String birthPlace) {
                this.birthPlace = birthPlace;
            }

            public String getGender() {
                return gender;
            }

            public void setGender(String gender) {
                this.gender = gender;
            }
        }
    }
}
