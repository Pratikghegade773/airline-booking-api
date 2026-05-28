package com.airlines.go7api.request;

import com.airlines.go7api.requestdto.common.*;

import com.airlines.go7api.requestdto.ChangeSeatReqDto;
import com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChangeSeatReq extends BaseGo7Req {

    private Aerocrs aerocrs;
    private String changeSeatUrl = "https://api.aerocrs.com/v5/makeSeatReservation";
    private String apiKey;

    public void setChangeSeatUrl(String url) {
        this.changeSeatUrl = url;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Data
    public static class Aerocrs {
        private Parms parms;
    }

    @Data
    public static class Parms {
        private Long bookingid;
        private String companycode;
        private List<Flight> flights;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Flight {
        private String flightnumber;
        private String flightdate;
        private String fromcode;
        private String tocode;
        @JsonProperty("class")
        private String flightClass;
        private List<String> seat;
    }

    public static ChangeSeatReq mapToChangeSeatReq(ChangeSeatReqDto dto, OrderRetrieveRspGo7Dto bookingRsp) {
        ChangeSeatReq req = new ChangeSeatReq();
        Aerocrs aerocrs = new Aerocrs();
        Parms parms = new Parms();

        // 1. Company Code
        parms.setCompanycode("API");

        // 2. Booking ID & Flight Retrieval
        com.airlines.go7api.responsego7.common.Flight firstBookingFlight = null;

        if (bookingRsp != null && bookingRsp.getAerocrs() != null && bookingRsp.getAerocrs().getBooking() != null) {
            com.airlines.go7api.responsego7.common.Booking booking = bookingRsp.getAerocrs()
                    .getBooking();

            parms.setBookingid(booking.getBookingid());

            // Try to get flight from 'flights' first (common in GetBooking), then 'items'
            // (common in Book)
            if (booking.getFlights() != null && booking.getFlights().getFlight() != null
                    && !booking.getFlights().getFlight().isEmpty()) {
                firstBookingFlight = booking.getFlights().getFlight().get(0);
            } else if (booking.getItems() != null && booking.getItems().getFlight() != null
                    && !booking.getItems().getFlight().isEmpty()) {
                firstBookingFlight = booking.getItems().getFlight().get(0);
            }
        }

        // 3. Flights Mapping
        // Use a Map to group seats by flight (Key: FlightNumber + Date)
        java.util.Map<String, Flight> flightMap = new java.util.LinkedHashMap<>();

        if (dto.getOffers() != null) {
            for (ChangeOfferReqDto offer : dto.getOffers()) {
                if (offer.getOfferItems() != null) {
                    for (ChangeOfferReqDto.OfferItemDto item : offer.getOfferItems()) {

                        // Use the found flight (defaulting to first flight)
                        if (firstBookingFlight != null) {
                            String key = firstBookingFlight.getNumber() + "|" + firstBookingFlight.getFlightdate();

                            Flight reqFlight = flightMap.get(key);
                            if (reqFlight == null) {
                                reqFlight = new Flight();
                                reqFlight.setFlightnumber(firstBookingFlight.getNumber());
                                reqFlight.setFlightdate(firstBookingFlight.getFlightdate());
                                reqFlight.setFromcode(firstBookingFlight.getFromcode());
                                reqFlight.setTocode(firstBookingFlight.getTocode());

                                // Map class. If booking says "Y/Flex Plus", we want "Y".
                                if (firstBookingFlight.getFlightClass() != null
                                        && !firstBookingFlight.getFlightClass().isEmpty()) {
                                    String fullClass = firstBookingFlight.getFlightClass();
                                    if (fullClass.contains("/")) {
                                        reqFlight.setFlightClass(fullClass.split("/")[0]);
                                    } else {
                                        reqFlight.setFlightClass(fullClass);
                                    }
                                } else {
                                    reqFlight.setFlightClass("Y");
                                }

                                reqFlight.setSeat(new ArrayList<>());
                                flightMap.put(key, reqFlight);
                            }

                            // Add seat
                            if (item.getRow() != null && item.getColumn() != null) {
                                reqFlight.getSeat().add(item.getRow() + item.getColumn());
                            } else {
                                // Fallback
                                reqFlight.getSeat().add(String.valueOf(item.getRow()) + item.getColumn());
                            }
                        }
                    }
                }
            }
        }

        parms.setFlights(new ArrayList<>(flightMap.values()));
        aerocrs.setParms(parms);
        req.setAerocrs(aerocrs);
        return req;
    }

    @Override
    protected String getApiUrl() {
        return changeSeatUrl != null ? changeSeatUrl : "";
    }

    @Override
    protected String getRequestName() {
        return "ChangeSeat";
    }
}
