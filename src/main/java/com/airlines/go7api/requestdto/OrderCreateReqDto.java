package com.airlines.go7api.requestdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.airlines.go7api.requestdto.common.*;
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
    private PaymentInformationReqDto paymentInformation;
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
    public static class Address {
        private String street;
        private String city;
        private String state;
        private String postalCode;
        private String countryCode;
        private String country;
    }

    private List<PaxReqDto> passengers;

    

}
