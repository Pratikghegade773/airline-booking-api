package com.airlines.GO7API.responseDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class ChangeSeatRspDto {

    private List<String> warnings;
    private String responseId;
    private String statusCode;
    private String apiOwner;
    private String pnr;
    private String orderId;
    private String validatingCarrier;
    private String rbd;
    private String currency;
    private BigDecimal totalOrderPrice;
    private List<String> remarks;
    private List<OD> ods;
    private List<BookingReferences> bookingReferences;
    private List<PaxDetailDTO> paxDetailList;
    private List<TicketDocInfoDTO> ticketDocInfoList;
    private List<EMDInfoDTO> emdInfoList;
    private List<PaymentsDTO> payments;
    private List<OrderItemsDTO> orderItems;
    private List<SpecialServiceRequest> specialServiceRequests;
    private List<OtherServiceInformation> otherServiceInformation;
    private List<PriceClass> priceClassList;
    private String paymentTimeLimit;
    private String ticketingTimeLimit;
    private String ticketedByTimeLimit;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL) // This will exclude null fields
    public static class OtherServiceInformation {
        private String airlineCode;
        private String osiCode;
        private String osiText;

    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OD {
        private String fareBasisCode;
        private String negotiatedCode;
        private String rbdCode;
        private String priceClassId;
        private String stp;
        private String classType;
        private String odKey;
        private String cabinType;
        private String origin;
        private String segmentId;
        private String destination;
        private String originAirportName;
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
        private String marketingCarrierLogo;
        private String operatingCarrierName;
        private String operatingCarrierCode;
        private String operatingCarrierLogo;
        private String operatingCarrierFlightNumber;
        private String departureTerminal;
        private String arrivalTerminal;
        private int changeOfDay;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BookingReferences {
        private String id;
        private String otherId;
        private String airlineId;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaxDetailDTO {
        private String paxId;
        private String ptc;
        private String gender;
        private String title;
        private String givenName;
        private String middleName;
        private String surname;
        private String loyaltyAccountNumber;
        private String infantRef;
        private String language;
        private String birthDate;
        private String contactInfoRef;
        private String countryDialingCode;
        private AddressDTO address;
        private List<PhoneDTO> phones;
        private List<EmailDTO> emails;
        private List<IdentityDocumentDTO> identityDocument;
        private List<TicketDocInfoDTO> ticketDocInfo;
        private List<EMDInfoDTO> emdInfo;

        @Data
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

        @Data
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class PhoneDTO {
            private String label;
            private String phoneNumber;
            private String type;

        }

        @Data
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class EmailDTO {
            private String label;
            private String emailAddress;
            private String type;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class IdentityDocumentDTO {
            private String identityDocumentNumber;
            private String identityDocumentType;
            private String issuingCountryCode;
            private String citizenshipCountryCode;
            private String issueDate;
            private String expiryDate;
            private String birthDate;
            private String birthPlace;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL) // This will exclude null fields
    public static class SpecialServiceRequest {
        private List<String> travelIdRef;
        private String segmentRef;
        private String ssrCode;
        private String text;
        private String actionCode;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TicketDocInfoDTO {
        private List<String> paxId;
        private String validatingCarrier;
        private String ptc;
        private String agentIdType;
        private String agentId;
        private String issuingAirlineName;
        private String issuingPlace;
        private String issuingCountry;
        private List<TicketDocumentDTO> ticketDocument;

        @Data
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TicketDocumentDTO {

            private String ticketDocNbr;
            private String primaryDocInd;
            private String exchTicketNbrInd;
            private String type;
            private int numberOfBooklets;
            private String dateOfIssue;
            private String timeOfIssue;
            private String ticketingLocation;
            private List<String> endorsements;
            private List<CouponInfoDTO> couponInfo;
            private String reportingType;

            @Data
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class CouponInfoDTO {
                private int couponNumber;
                private String classType;
                private String rbdCode;
                private String couponReference;
                private String fareBasisCode;
                private String rbd;
                private String stp;
                private String couponMedia;
                private String status;
                private String nvb;
                private String nva;
                private List<CurrentAirlineInfoDTO> currentAirlineInfo;
                private String validatingCarrier;
                private String validatingArline;
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
                    private String category;
                    private String quantity;
                    private String name;
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
                        private String category;
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
                    private String operatingCarrierAirlineId;
                    private String marketingCarrierName;
                    private String operatingCarrierName;
                    private String flightNumber;
                    private String equipmentAircraftCode;
                    private String equipmentName;
                    private String status;
                }

                @Data
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
    public static class EMDInfoDTO {
        private String ptc;
        private String validatingCarrier;
        private List<String> paxId;
        private String agentIdType;
        private String agentId;
        private String issuingAirlineName;
        private String issuingPlace;
        private String issuingCountry;
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
            private String exchTicketNbrInd;
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
                        private String category;
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
        private String serviceCode;
        private String serviceName;
        private String passengerRef;
        private String segmentRef;
        private String arrival;
        private String departure;
        private String serviceType;
        private List<String> description;
        private String segmentId;
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
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderItemsDTO {
        private String orderItemId;
        private String ptc;
        private BigDecimal totalPrice;
        private List<String> passengerIds;
        private String className;
        private String timeStamp;
        private TotalFare totalFare;
        private BaseFare baseFare;
        private TotalTax totalTax;
        private List<Tax> taxes;
        private List<Qsurcharges> qSurCharges;
        private List<String> endorsements;
        private List<Service> serviceList;
        private List<BaggageAllowance> baggageAllowances;
        private List<FareDetail> fareDetail;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class FareDetail {

            // private Price price;
            private DiffPrice diffPrice;
            private NewPrice newPrice;
            private OldPrice oldPrice;
            private Penalty penalty;
            private DifferentialDueByAirline airlineDue;
            private DifferentialDueByPassenger passengerDue;
            private TotalAmount totalAmount;
            private List<String> paxrefId;

            @Data
            public static class DiffPrice {
                private TotalFare totalFare;
                private TotalTax totalTax;
                private BaseFare baseFare;
                private List<Tax> taxes;

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
                    private BigDecimal amount;
                    private String currency;
                    private String nation;
                    private String taxCode;
                    private String description;
                }
            }

            @Data
            public static class OldPrice {
                private TotalFare totalFare;
                private TotalTax totalTax;
                private BaseFare baseFare;
                private List<Tax> taxes;

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
                public static class TotalTax {
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
                public static class Tax {
                    private String code;
                    private BigDecimal amount;
                    private String currency;
                    private String nation;
                    private String taxCode;
                    private String description;
                }
            }

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class Penalty {
                private BigDecimal amount;
                private String currency;
                private String amountType;
                private String purpose;
            }

            @Data
            public static class DifferentialDueByAirline {
                private BigDecimal amount;
                private String currency;
            }

            @Data
            public static class DifferentialDueByPassenger {
                private BigDecimal amount;
                private String currency;
            }

            @Data
            public static class TotalAmount {
                private BigDecimal amount;
                private String currency;
            }

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public static class NewPrice {
                private TotalFare totalFare;
                private TotalTax totalTax;
                private BaseFare baseFare;
                private List<Tax> taxes;

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
                    private BigDecimal amount;
                    private String currency;
                    private String nation;
                    private String taxCode;
                    private String description;
                }
            }
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
        public static class Tax {
            private String code;
            private BigDecimal amount;
            private String currency;
            private String description;
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
        public static class BaggageAllowance {
            private String baggageAllowanceId;
            private String ptc;
            private String passengerId;
            private String category;
            private String quantity;
            private String name;
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
                private String value;
                private String uom;
            }
        }

    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentsDTO {
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PriceClass {
        private String priceClassId;
        private String className;
        private String cabinTypeCode;
        private List<Description> descriptions;

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
