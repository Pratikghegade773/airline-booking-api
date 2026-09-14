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
public class OfferPriceRspDto {

    private List<String> warning;

    private String responseId;
    private String apiOwner;
    private String pricedOfferId;
    private String validatingCarrier;
    private BigDecimal totalPrice;
    private BigDecimal totalTaxes;
    private String currency;
    private String offerPriceExpiration;
    private String paymentTimeLimit;
    private String ticketedByTimeLimit;
    private List<OD> ods;
    private List<OfferItemDto> offerItems;
    private List<PriceClass> priceClassList;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OfferItemDto {
        private String offerItemId;
        private String ptc;
        private List<String> paxref;
        private BigDecimal totalPrice;
        private List<String> passengerIds;
        private TotalFare totalFare;
        private BaseFare baseFare;
        private TotalTax totalTax;
        private List<Tax> taxes;
        private List<BaggageAllowance> baggageAllowances;
        private List<String> segmentRefs;
        private List<String> givenName;
        private String currency;


        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class TotalFare {
            private BigDecimal amount;
            private String currency;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class BaseFare {
            private BigDecimal amount;
            private String currency;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class TotalTax {
            private BigDecimal amount;
            private String currency;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Tax {
            private String description;
            private BigDecimal amount;
            private String currency;
            private String code;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class BaggageAllowance {
            private String baggageAllowanceId;
            private String ptc;
            private List<String> passengerId;
            private String category;
            private String quantity;
            private List<Weight> weight;
            private List<DescriptionDTO> descriptions;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class Weight {
                private BigDecimal value;
                private String uom;
            }

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class DescriptionDTO {
                private String description;
            }
        }

        private String cabinTypeCode;
        private String cabinType;
    }
}
