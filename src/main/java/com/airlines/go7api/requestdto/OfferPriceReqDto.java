package com.airlines.go7api.requestdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OfferPriceReqDto {
    private String countryCode;
    private String apiKey;
    private String offerpriceurl;

    private String agencyId;
    private String agentId;
    private String agencyName;

    private int httpResponseCode;
    private List<Offer> offers;
    private String cardType;
    private String paymentType;
    @JsonProperty("IINnumber")
    private long iinNumber;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Offer {
        private String responseId;
        private String offerId;
        private List<OfferItemDto> offerItems;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OfferItemDto {
            private String offerItemId;
            private String ptc;
            private int count;
            private List<String> paxRefs;
            private String column;
            private BigDecimal row;
            private SpecialServices specialServices;

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
    }
}