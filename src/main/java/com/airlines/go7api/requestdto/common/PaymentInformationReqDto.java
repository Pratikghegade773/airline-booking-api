package com.airlines.go7api.requestdto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInformationReqDto {
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
