package com.airlines.GO7API.requestDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePaymentReqDto {

    private String countryCode;
    private String apiUrl;
    private String changeurl;
    public String sellerOrgId;
    public String distributorOrgId;
    private String agencyId;
    private String agentId;

    private int httpResponseCode;
    private String orderId;
    private String offerId;
    private String paymentType;
    private PaymentInformation paymentInformation;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInformation {
        private String currencyCode;
        private BigDecimal amount;
        private String cardCode;
        private String cardNumber;
        private String seriesCode;
        private String cardHolderName;
        private String expiration;
        private Address address;

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
}
