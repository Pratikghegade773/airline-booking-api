package com.airlines.GO7API.responseDto;

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
public class OrderCreateRspDto {
    private List<String> warnings;
    private String responseId;
    private String validatingCarrier;
    private String pnr;
    private String apiOwner;
    private String orderId;
    private BigDecimal totalOrderPrice;
    private String currency;
    private String paymentTimeLimit;
    private String ticketingTimeLimit;
    private List<OD> ods;
    private String statusCode;
    private List<String> remarks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OD {
        private String fareBasisCode;
        private String rbdCode;
        private String cabinType;
        private String classType;
        private String priceClassId;
        private String odKey;
        private String origin;
        private String segmentId;
        private String originAirportName;
        private String destination;
        private String destinationAirportName;
        private String departureDate;
        private String arrivalDate;
        private String departureTime;
        private String arrivalTime;
        private String journeyTime;
        private String equipment;
        private String flightNumber;
        private String marketingCarrierName;
        private String marketingCarrierCode;
        private String operatingCarrierName;
        private String operatingCarrierCode;
        private String arrivalTerminal;
        private String departureTerminal;
        private String operatingCarrierFlightNumber;
        private int changeOfDay;
        private String stp;
    }
    // private String id;

    private List<BookingReference> bookingReferences;
    private List<PaxDetailDTO> paxDetailList;
    private List<SpecialServiceRequest> specialServiceRequests;
    // private List<ServiceDTO> serviceDTOList;

    private List<EMDInfoDTO> emdInfoList;

    private List<TicketDocInfoDTO> ticketDocInfoList;
    private List<PaymentsDTO> payments;

    // Add this line to include the order items list
    private List<OrderItemDTO> orderItems;
    private List<PriceClass> priceClassList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL) // This will exclude null fields
    public static class BookingReference {
        private String id;
        private String otherId;
        private String airlineId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaxDetailDTO {
        private String ptc;
        private String paxId;
        private String gender;
        private String title;
        private String givenName;
        private String middleName;
        private String surname;
        private String loyaltyAccountNumber;
        private String language;
        private String infantRef;
        private String birthDate;
        private AddressDTO address;
        private List<PhoneDTO> phones;
        private List<EmailDTO> emails;
        private List<TicketDocInfoDTO> ticketDocInfo;
        private List<EMDInfoDTO> emdInfo;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class AddressDTO {
            private String label;
            private String street;
            private String postalCode;
            private String cityName;
            private String stateName;
            private String countryName;
            private String countryCode;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PhoneDTO {
        private String language;
        private String type;
        private String phoneNumber;

        // private String type;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EmailDTO {
        private String language;
        private String type;
        private String emailAddress;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentsDTO {
        private String type;
        private String statusCode;
        private List<String> orderItem;
        private String currency;
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL) // This will exclude null fields
    public static class SpecialServiceRequest {
        private List<String> travelIdRef;
        private String segmentId;
        private String ssrCode;
        private String text;
        private String actionCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TicketDocInfoDTO {
        private List<String> paxId;
        private String ptc;
        private String validatingCarrier;
        private String agentIdType;
        private String agentId;
        private String issuingAirlineName;
        private String issuingPlace;
        private String issuingCountry;
        private List<TicketDocumentDTO> ticketDocument;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class TicketDocumentDTO {
            private String ticketDocNbr;
            private String type;
            private int numberOfBooklets;
            private String primaryDocInd;
            private String exchTicketNbrInd;
            private String dateOfIssue;
            private String timeOfIssue;
            private String ticketingLocation;
            private List<CouponInfoDTO> couponInfo;
            private String reportingType;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class CouponInfoDTO {
                private String classType;
                private String rbd;
                private String stp;
                private int couponNumber;
                private String couponReference;
                private String fareBasisCode;
                private String couponMedia;
                private String status;
                private String nvb;
                private String nva;
                private String validatingCarrier;
                private List<CurrentAirlineInfoDTO> currentAirlineInfo;
                private List<AllowableBagDTO> allowableBags;
                private List<BaggageAllowance> baggageAllowances;

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public static class CurrentAirlineInfoDTO {
                    private String departureAirportCode;
                    private String arrivalAirportCode;
                    private String departureDate;
                    private String departureTime;
                    private String departureAirportName;
                    private String departureTerminal;
                    private String arrivalDate;
                    private String arrivalTime;
                    private String arrivalAirportName;
                    private String arrivalTerminal;
                    private String changeOfDay;
                    private String marketingCarrierAirlineId;
                    private String marketingCarrierName;
                    private String operatingCarrierAirlineId;
                    private String operatingCarrierName;
                    private String flightNumber;
                    private String operatingCarrierFlightNumber;
                    private String equipmentAircraftCode;
                    private String equipmentName;
                    private String status;
                }

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public static class AllowableBagDTO {
                    private int number;
                    private String type;
                }

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public static class BaggageAllowance {

                    private String baggageAllowanceId;
                    private String ptc;
                    private String passengerId;
                    private String category;
                    private String quantity;
                    private List<Weight> weight;
                    private List<Dimension> dimensions;
                    private List<DescriptionDTO> descriptions;

                    @Data
                    @NoArgsConstructor
                    @AllArgsConstructor
                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    public static class DescriptionDTO {
                        private String description;
                        private String text;
                        private String label;
                    }

                    @Data
                    @NoArgsConstructor
                    @AllArgsConstructor
                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    public static class Weight {
                        private String value;
                        private String uom;
                    }

                    @Data
                    @NoArgsConstructor
                    @AllArgsConstructor
                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    public static class Dimension {
                        private String category;
                        private String value;
                        private String uom;
                    }

                }
            }
        }

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EMDInfoDTO {
        private String ptc;
        private String validatingCarrier;
        private List<String> paxId;
        private String agentIdType;
        private String agentId;
        private List<String> refOrderItemIds;
        private String issuingAirlineName;
        private String issuingPlace;
        private List<TicketDocumentDTO> ticketDocument;
        private String paymentRefId;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class TicketDocumentDTO {
            private String connectedDocNbr;
            private String ticketDocNbr;
            private String primaryDocInd;
            private String type;
            private int numberOfBooklets;
            private String dateOfIssue;
            private String timeOfIssue;
            private String ticketingLocation;
            private List<String> endorsements;
            private List<CouponInfoDTO> couponInfo;
            private String reportingType;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class CouponInfoDTO {
                private String validatingCarrier;
                private String classType;
                private String rbdCode;
                private int couponNumber;
                private String couponReference;
                private List<String> serviceRefs;
                private String fareBasisCode;
                private String rbd;
                private String stp;
                private String name;
                private String rfic;
                private String rfisc;
                private String couponMedia;
                private String status;
                private String nvb;
                private String nva;
                private String column;
                private BigInteger row;
                private List<SeatCharacteristic> seatCharacteristics;

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public static class SeatCharacteristic {
                    private String code;
                    private String description;
                }

                private List<CurrentAirlineInfoDTO> currentAirlineInfo;
                private List<AllowableBagDTO> allowableBags;
                private List<BaggageAllowance> baggageAllowances;

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public static class BaggageAllowance {
                    private String baggageAllowanceId;
                    private String ptc;
                    private String passengerId;
                    private String name;
                    private String category;
                    private String quantity;
                    private List<Weight> weight;
                    private List<Dimension> dimensions;
                    private List<DescriptionDTO> descriptions;

                    @Data
                    @NoArgsConstructor
                    @AllArgsConstructor
                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    public static class Weight {
                        private BigDecimal value;
                        private String unit;
                        private String uom;
                    }

                    @Data
                    @NoArgsConstructor
                    @AllArgsConstructor
                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    public static class Dimension {
                        private String unit;
                        private BigDecimal value;
                        private String uom;
                    }

                    @Data
                    @NoArgsConstructor
                    @AllArgsConstructor
                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    public static class DescriptionDTO {
                        private String description;
                        private String text;
                        private String label;
                    }
                }

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public static class CurrentAirlineInfoDTO {
                    private String departureAirportCode;
                    private String arrivalAirportCode;
                    private String departureDate;
                    private String departureTime;
                    private String departureAirportName;
                    private String departureTerminal;
                    private String arrivalDate;
                    private String arrivalTime;
                    private String arrivalAirportName;
                    private String arrivalTerminal;
                    private String changeOfDay;
                    private String marketingCarrierAirlineId;
                    private String marketingCarrierName;
                    private String operatingCarrierAirlineId;
                    private String operatingCarrierName;
                    private String flightNumber;
                    private String equipmentAircraftCode;
                    private String equipmentName;
                    private String status;
                }

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public static class AllowableBagDTO {
                    private int number;
                    private String type;
                }
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Service {
        private String serviceId;
        private String serviceStatus;

        // private ServiceDefinitionRef serviceDefinitionRef;
        private String serviceName;
        private List<String> description;
        private String segmentId;
        private String serviceCode;
        private BigInteger row;
        private String column;
        private String departure;
        private String arrival;

        // @Data
        // @NoArgsConstructor
        // @AllArgsConstructor
        // @JsonInclude(JsonInclude.Include.NON_NULL)
        // public static class ServiceDefinitionRef {
        // private ListOfFlightSegmentType segmentRef;
        // private ServiceDefinitionType value;
        // }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderItemDTO {
        private String orderItemId;
        private String ptc;
        private BigDecimal totalPrice;
        private List<String> passengerIds;
        private String className;
        private String timeStamp;
        private TotalFare totalFare;
        private BaseFare baseFare;
        private List<Qsurcharges> qSurCharges;
        private TotalTax totalTax;
        private List<Tax> taxes;
        private List<String> endorsements;
        private List<Service> serviceList;

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
        public static class Qsurcharges {
            private BigDecimal amount;
            private String currency;
            private List<Fee> fee;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Fee {
                private BigDecimal amount;
                private String currency;
                private String designator;
                private String description;
            }
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Tax {
            private String code;
            private BigDecimal amount;
            private String currency;
            private String description;
        }

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PriceClass {
        private String priceClassId;
        private String className;
        private String cabinTypeCode;
        private List<Description> descriptions;
        private java.util.Map<String, Boolean> services;
        // private int displayOrder;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Description {
            private String text;
            private String odKey;
        }
    }
}
