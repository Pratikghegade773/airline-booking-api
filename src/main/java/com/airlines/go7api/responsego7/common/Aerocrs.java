package com.airlines.go7api.responsego7.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Aerocrs {
    private boolean success;
    private Booking booking;
    private Object infaspax; // Can be int or string based on API variability, example says 1
    
    @com.fasterxml.jackson.annotation.JsonAlias("getFlight")
    private Flights flights; // Needed for Airshop responses where flights are directly under aerocrs
    
    private String companycode; // Needed for ChangeSeat
    
    @JsonProperty("seatmapfare")
    private com.airlines.go7api.responsego7.SeatAvailabilityRspGo7Dto.SeatMapFare seatMapFare; // Needed for SeatAvailability
    
    private com.airlines.go7api.responsego7.ServiceListRspGo7Dto.Ancillaries ancillaries; // Needed for ServiceList
    private java.util.List<Detail> details; // Needed for ChangeService

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class Detail {
        private java.util.Map<String, Object> ancillary;
        private String invid;
        private boolean success;
    }
}