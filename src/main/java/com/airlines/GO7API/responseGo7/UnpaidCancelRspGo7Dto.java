package com.airlines.GO7API.responseGo7;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UnpaidCancelRspGo7Dto {

    private Aerocrs aerocrs;

    private boolean success;
    private Details details;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Details {
        private List<String> detail;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Aerocrs {
        private boolean success;
        private Booking booking;
        private Object infaspax; // Can be int or string based on API variability, example says 1
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Booking {
        private Items items;
        private int adults;
        private int child;
        private int infant;
        private String totalprice;
        private String defaultCurrency;
        private String totalpriceinCurrency;
        private String currency;
        private Long bookingid;
        private String pnrref;
        private String pnrptl;
        private String pnrttl;
        private String bookingconfirmation;
        private String invoicenumber;
        private String ticketnumber;
        private String status;
        private String source;
        private String linktoticket;
        private String linktobooking;
        private Flights flights;
        private Passengers passengers;
        private Remarks remarks;
        private List<Object> vouchers;
        @JsonProperty("Balanceinformation")
        private BalanceInformation balanceInformation;
        private Object currencies; // Map<String, Object>
        private Object balanceInCurrencies; // Map<String, Map<String, Double>>
        private List<Object> receipts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        private List<Flight> flight;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Flights {
        private List<Flight> flight;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Flight {
        private String airline;
        private int airlineid;

        @JsonProperty("from")
        private String from;

        @JsonProperty("to")
        private String to;

        private String fromcode;
        private String tocode;
        private String flightdate;
        private String depart;
        private String arrive;
        private String number;
        private String flighttype;

        @JsonProperty("class")
        private String flightClass;

        private Long invid;
        private String invpricing;

        @JsonProperty("Adultfare")
        private String adultfare;

        @JsonProperty("Childfare")
        private String childfare;

        @JsonProperty("Infantfare")
        private String infantfare;

        private String tax;

        @JsonProperty("net_fare")
        private String netFare;

        @JsonProperty("rack_fare")
        private String rackFare;

        private String totaltax;
        private String currency;
        private int converttousd;
        private String airlinedesignator;
        private int ckstatus;
        private int flightcode;
        private int flightid;
        @JsonProperty("aircraftType")
        private String aircraftType;
        @JsonProperty("aircraftTypeIataCode")
        private String aircraftTypeIataCode;
        @JsonProperty("airlineICAOcode")
        private String airlineICAOcode;
        @JsonProperty("arrivalTerminal")
        private String arrivalTerminal;
        @JsonProperty("departureTerminal")
        private String departureTerminal;
        private double totaltaxes;
        private Taxes taxes;
        private String invpricingwithouttax;
        private java.util.Map<String, Boolean> services;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Taxes {
        @JsonProperty("Ground_handling")
        private double groundHandling;
        @JsonProperty("Security")
        private double security;
        @JsonProperty("Fuel")
        private double fuel;
        private double tax_4;
        private double tax_1; // Common placeholder
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Passengers {
        private List<Passenger> passenger;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Passenger {
        private Long rph;
        private String paxtitle;
        private String lastname;
        private String firstname;
        private String paxtype;
        private String nationality;
        private String passportexpiry;
        private String paxdoctype;
        private String paxpassportnum;
        private String paxpassportcountry;
        private String gender;
        private String dob;
        private String email;
        private String contact;
        private Object checkin;
        @JsonProperty("e-tickets")
        private ETickets eTickets;
        // older fields if needed compatibility
        private int paxnum;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ETickets {
            private List<ETicketFlight> flight;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ETicketFlight {
            private String number;
            private String eticketnumber;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Remarks {
        private List<Remark> remark;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Remark {
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BalanceInformation {
        @JsonProperty("PNRoutstandingpayment")
        private double pnrOutstandingPayment;
        @JsonProperty("PNRtotal")
        private BigDecimal pnrTotal;
    }
}
