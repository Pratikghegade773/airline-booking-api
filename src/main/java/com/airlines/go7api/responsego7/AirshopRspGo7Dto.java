package com.airlines.go7api.responsego7;

import com.airlines.go7api.responsego7.common.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)

public class AirshopRspGo7Dto {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    

    

    

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FlightClass {
        private String fltflimitcurrency;
        private long flightid;
        private int freeseats;
        private String className;
        private String classCode;
        private String cabinClass;
        private String cabinCode;
        private String currency;
        private String chargeType;
        private String chargeTypeCode;
        private int baggageAllowance;
        private int infbaggageallowance;
        private String baggageUnit;
        private Fare fare;
        private Fare rackFare;
        private Map<String, Service> services;
        private long fareid;
        private String type;
        private String notification;
        private Object breakdown; // Can be empty or object
        private RawFareObject rawFareObject;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fare {
        private String tax;
        private String adultFare;
        private String childFare;
        private String infantFare;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Service {
        private boolean active;
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RawFareObject {
        private RackFareDetails rackFare;
        private FareDetails fare;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RackFareDetails {
        private String tax;
        private Map<String, String> taxBreakdown;
        private String adultFare;
        private String childFare;
        private String infantFare;
        private String vat;
        private String tax4forChildRE;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FareDetails {
        private String tax;
        private String adultFare;
        private String childFare;
        private String infantFare;
    }
}