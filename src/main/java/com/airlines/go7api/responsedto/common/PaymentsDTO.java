package com.airlines.go7api.responsedto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentsDTO {
    private String type;
    private String statusCode;
    private String currency;
    private List<String> orderItem;
    private BigDecimal amount;
    private String cardCode;
    private String cardNumber;
    private String cardHolderName;
    private String expiration;
    private AddressDTO address;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AddressDTO {

        private String street;
        private String city;
        private String state;
        private String postalCode;
        private String countryCode;
        private String country;
    }
}
