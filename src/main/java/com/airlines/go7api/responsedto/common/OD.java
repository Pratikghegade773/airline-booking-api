package com.airlines.go7api.responsedto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OD {
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
