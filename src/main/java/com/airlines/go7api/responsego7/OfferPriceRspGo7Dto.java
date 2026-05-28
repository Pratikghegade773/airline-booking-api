package com.airlines.go7api.responsego7;

import com.airlines.go7api.responsego7.common.*;

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
    public static class GetFlight {
        private List<Flight> flight;
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
