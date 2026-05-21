package com.airlines.go7api.responsedto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
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

            @Data
            @AllArgsConstructor
            @NoArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class Service {
                private String serviceId;
                private ServiceDefinationRef serviceDefinationRef;

                @Data
                @AllArgsConstructor
                @NoArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public static class ServiceDefinationRef {
                    private String serviceDefId;
                    private String name;
                    private Encoding encoding;

                    @Data
                    @AllArgsConstructor
                    @NoArgsConstructor
                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    public static class Encoding {
                        private String rfic;
                        private String type;
                        private String code;
                        private String subCode;
                    }

                    private List<Description> descriptions;

                    @Data
                    @AllArgsConstructor
                    @NoArgsConstructor
                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    public static class Description {
                        private String text;
                        private String application;
                    }

                    private String settlement;
                    private String validatingCarrier;
                    private Detail detail;

                    @Data
                    @AllArgsConstructor
                    @NoArgsConstructor
                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    public static class Detail {
                        private String couponType;
                        private BigInteger maximumQuantity;
                    }
                }

            }
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

            @Data
            @AllArgsConstructor
            @NoArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class Service {
                private String serviceId;
                private ServiceDefinationRef serviceDefinationRef;

                @Data
                @AllArgsConstructor
                @NoArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public static class ServiceDefinationRef {
                    private String serviceDefId;
                    private String name;
                    private Encoding encoding;

                    @Data
                    @AllArgsConstructor
                    @NoArgsConstructor
                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    public static class Encoding {
                        private String rfic;
                        private String type;
                        private String code;
                        private String subCode;
                    }

                    private List<Description> descriptions;

                    @Data
                    @AllArgsConstructor
                    @NoArgsConstructor
                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    public static class Description {
                        private String text;
                        private String application;
                    }

                    private String settlement;
                    private String validatingCarrier;
                    private Detail detail;

                    @Data
                    @AllArgsConstructor
                    @NoArgsConstructor
                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    public static class Detail {
                        private String couponType;
                        private BigInteger maximumQuantity;
                    }
                }
            }
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

            @Data
            @AllArgsConstructor
            @NoArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class Service {
                private String serviceId;
                private ServiceDefinationRef serviceDefinationRef;

                @Data
                @AllArgsConstructor
                @NoArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public static class ServiceDefinationRef {
                    private String serviceDefId;
                    private String name;
                    private Encoding encoding;

                    @Data
                    @AllArgsConstructor
                    @NoArgsConstructor
                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    public static class Encoding {
                        private String rfic;
                        private String rfisc;
                        private String type;
                        private String code;
                        private String subCode;
                    }

                    @JsonInclude(JsonInclude.Include.NON_EMPTY)
                    private List<Description> descriptions;

                    @Data
                    @AllArgsConstructor
                    @NoArgsConstructor
                    @JsonInclude(JsonInclude.Include.NON_EMPTY)
                    public static class Description {
                        private String text;
                        private String application;
                    }

                    private String settlement;
                    private String validatingCarrier;
                    private Detail detail;

                    @Data
                    @AllArgsConstructor
                    @NoArgsConstructor
                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    public static class Detail {
                        private String couponType;
                        private BigInteger maximumQuantity;
                    }
                }
            }
        }
    }

}
