package com.airlines.go7api.requestdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangePaymentReqDto {

    private String countryCode;
    private String apiUrl;
    private String changeurl;
    private String sellerOrgId;
    private String distributorOrgId;
    private String agencyId;
    private String agentId;
    private String agencyName;


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
