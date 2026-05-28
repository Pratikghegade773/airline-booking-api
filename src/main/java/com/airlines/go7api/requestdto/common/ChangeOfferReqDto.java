package com.airlines.go7api.requestdto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeOfferReqDto {
    private String offerId;
    private List<OfferItemDto> offerItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfferItemDto {
        private String offerItemId;
        private String ptc;
        private List<String> paxRefs;
        private BigInteger row;
        private String column;
        private SpecialServices specialServices;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SpecialServices {
            private BigDecimal qty;
            private String text;
            private String name;
            private String type;
            private String serviceDefId;
            private List<String> segId;
        }
    }
}
