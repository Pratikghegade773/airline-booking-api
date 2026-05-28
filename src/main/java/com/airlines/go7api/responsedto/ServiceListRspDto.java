package com.airlines.go7api.responsedto;

import com.airlines.go7api.responsedto.common.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceListRspDto {
    private List<String>warnings;
    private String responseId;
    private String offerId;
    private String quantity;
    private String offerExpiration;
    private List<OfferItem> offerItem;
    private String apiOwner;
    private String validatingCarrier;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class OfferItem {
        private List<Meal> mealList;
        private List<Baggage> baggageList;
        private List<OtherService> otherServiceList;


        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public static class Meal {
            private String offerItemId;
            private List<String> passengerRefs;
            private List<String> givenName;
            private List<String> segmentRefId;
            private String currency;
            private BigDecimal baseAmount;
            private BigDecimal discountAmount;
            private BigDecimal taxAmount;
            private BigDecimal totalAmount;
            private String description;
            private Service service;

        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public static class Baggage{
            private String offerItemId;
            private List<String> passengerRefs;
            private List<String> givenName;
            private List<String>  segmentRefId;
            private String currency;
            private BigDecimal baseAmount;
            private BigDecimal discountAmount;
            private BigDecimal taxAmount;
            private BigDecimal totalAmount;
            private BigDecimal totalBaggageWeight;
            private String weightUnitOfMeasurement;
            private String description;
            private Service service;

        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public static class OtherService {
            private String offerItemId;
            private List<String> passengerRefs;
            private List<String> givenName;
            private List<String>  segmentRefId;
            private String currency;
            private BigDecimal baseAmount;
            private BigDecimal discountAmount;
            private BigDecimal taxAmount;
            private BigDecimal totalAmount;
            private String description;
            private Service service;

        }
    }

}
