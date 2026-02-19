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
public class UnpaidCancelRspDto {
    private List<String> warnings;
    private String responseId;
    private String pnr;
    private String apiOwner;
    private String orderId;
    private String validatingCarrier;
    private BigDecimal totalOrderPrice;
    private String fareBasisCode;
    private String currency;
    private String paymentTimeLimit;
    private List<OD> ods;
    private String statusCode;
    private List<String> remarks;
    private BigDecimal totalTaxes;
    private List<BookingReference> bookingReferences;
    private List<PaxDetailDTO> paxDetailList;
    private List<SpecialServiceRequest> specialServiceRequests;
    private List<PriceClass> priceClassList;
    private List<TicketDocInfoDTO> ticketDocInfoList;
    private List<EMDInfoDTO> emdInfoList;
    private List<PaymentsDTO> payments;
    private List<OrderItemDTO> orderItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OD {
        private String classType;
        private String cabinType;
        private String rbdCode;
        private String stp;
        private String fareBasisCode;
        private String priceClassId;
        private String segmentId;
        private String negotiatedCode;
        private String odKey;
        private String origin;
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
        private String marketingCarrierLogo;
        private String operatingCarrierName;
        private String operatingCarrierCode;
        private String operatingCarrierLogo;
        private String arrivalTerminal;
        private String departureTerminal;
        private String operatingCarrierFlightNumber;
        private int changeOfDay;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BookingReference {
        private String Id;
        private String otherId;
        private String AirlineId;
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
        private String language;
        private String infantRef;
        private String birthDate;
        private String contactInfoRef;
        private String countryDialingCode;
        private AddressDTO address;
        private List<PhoneDTO> phones;
        private List<EmailDTO> emails;
        private List<IdentityDocument> identityDocument;
        private List<TicketDocInfoDTO> ticketDocInfo;
        private List<EMDInfoDTO> emdInfo;
        private String loyaltyAccountNumber;

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
        private String label;
        private String phoneNumber;
        private String type;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EmailDTO {
        private String language;
        private String label;
        private String emailAddress;
        private String type;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IdentityDocument {
        private String identityDocumentNumber;
        private String identityDocumentType;
        private String issuingCountryCode;
        private String citizenshipCountryCode;
        private String issueDate;
        private String expiryDate;
        private String birthDate;
        private String birthPlace;
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
        @JsonInclude(JsonInclude.Include.NON_NULL)
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SpecialServiceRequest {
        private List<String> travelIdRef;
        private String segmentRef;
        private String ssrCode;
        private String text;
        private String actionCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TicketDocInfoDTO {
        private String validatingCarrier;
        private List<String> paxId;
        private String agentIdType;
        private String agentId;
        private String issuingAirlineName;
        private String issuingPlace;
        private List<TicketDocumentDTO> ticketDocument;
        private String paymentRefID;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class TicketDocumentDTO {
            private String classType;
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
                private String fareBasisCode;
                private String rbd;
                private String stp;
                private String couponMedia;
                private String status;
                private String nvb;
                private String nva;
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
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderItemDTO {
        private String orderItemId;
        private String ptc;
        private BigDecimal totalPrice;
        private String className;
        private List<String> passengerIds;
        private String timeStamp;
        private TotalFare totalFare;
        private BaseFare baseFare;
        private TotalTax totalTax;
        private List<Taxes> taxes;
        private List<String> endorsements;
        private List<Service> serviceList;
        private List<BaggageAllowance> baggageAllowances;
        private List<FareDetail> fareDetail;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class FareDetail {

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
        public static class Taxes {
            private String code;
            private BigDecimal amount;
            private String currency;
            private String description;
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
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Service {
        private String serviceType;
        private String serviceCode;
        private String serviceName;
        private String description;
        private String serviceId;
        private String serviceStatus;
        private String Odkey;
        private String segmentRef;
        private String arrival;
        private String departure;
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
