package com.airlines.GO7API.responseGo7;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OfferPriceRspGo7Dto {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Aerocrs {
        private boolean success;
        @JsonProperty("getFlight")
        private GetFlight getFlight;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GetFlight {
        private List<Flight> flight;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Flight {
        private String airline;
        private int airlineid;
        private String flightid;
        private String fromcode;
        private String tocode;
        @JsonProperty("from")
        private String fromLocation;
        @JsonProperty("to")
        private String toLocation;
        private String via;
        private String flightdate;
        private String depart;
        private String arrive;
        private String number;
        @JsonProperty("class")
        private String flightClass;
        private String flighttype;
        @JsonProperty("rackfare_adult")
        private String rackfareAdult;
        @JsonProperty("rackfare_child")
        private String rackfareChild;
        @JsonProperty("rackfare_infant")
        private String rackfareInfant;
        @JsonProperty("agtfare_adult")
        private String agtfareAdult;
        @JsonProperty("agtfare_child")
        private String agtfareChild;
        @JsonProperty("agtfare_infant")
        private String agtfareInfant;
        private String tax;
        @JsonProperty("net_fare")
        private String netFare;
        @JsonProperty("rack_fare")
        private String rackFare;
        private String totaltax;
        private String companywebdiscount;
        private String currency;
        private int converttousd;
        private Object terms;
        private Map<String, Boolean> services;
    }
}
