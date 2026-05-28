package com.airlines.go7api.responsego7.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class Flight {
    @com.fasterxml.jackson.annotation.JsonAlias("airlineName")
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
    @com.fasterxml.jackson.annotation.JsonAlias("flightnumber")
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
    
    @com.fasterxml.jackson.annotation.JsonAlias({"agtfare_adult", "agtFareAdult"})
    private String agtfareAdult;

    @com.fasterxml.jackson.annotation.JsonAlias({"rackfare_adult", "rackFareAdult"})
    private String rackfareAdult;

    @com.fasterxml.jackson.annotation.JsonAlias({"agtfare_child", "agtFareChild"})
    private String agtfareChild;

    @com.fasterxml.jackson.annotation.JsonAlias({"rackfare_child", "rackFareChild"})
    private String rackfareChild;

    @com.fasterxml.jackson.annotation.JsonAlias({"agtfare_infant", "agtFareInfant"})
    private String agtfareInfant;

    @com.fasterxml.jackson.annotation.JsonAlias({"rackfare_infant", "rackFareInfant"})
    private String rackfareInfant;
    
    @com.fasterxml.jackson.annotation.JsonAlias({"raw_fare_object", "rawFareObject"})
    private com.airlines.go7api.responsego7.OfferPriceRspGo7Dto.RawFareObject rawFareObject;
    
    @com.fasterxml.jackson.annotation.JsonAlias("airlineDesignator")
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
    private List<Seat> seat;
    
    // Additional fields for Airshop responses
    private java.util.Map<String, com.airlines.go7api.responsego7.AirshopRspGo7Dto.FlightClass> classes;
    private String direction;
    private String fltnum;
    private String std;
    private String sta;
}