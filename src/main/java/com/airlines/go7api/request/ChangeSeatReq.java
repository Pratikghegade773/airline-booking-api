package com.airlines.go7api.request;

import com.airlines.go7api.requestdto.common.*;

import com.airlines.go7api.requestdto.ChangeSeatReqDto;
import com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto;
import com.fasterxml.jackson.annotation.JsonProperty;
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
        com.airlines.go7api.responsego7.common.Flight firstBookingFlight = extractFirstBookingFlight(bookingRsp);
        if (bookingRsp != null && bookingRsp.getAerocrs() != null && bookingRsp.getAerocrs().getBooking() != null) {
            parms.setBookingid(bookingRsp.getAerocrs().getBooking().getBookingid());
        }

        // 3. Flights Mapping
        // Use a Map to group seats by flight (Key: FlightNumber + Date)
        java.util.Map<String, Flight> flightMap = new java.util.LinkedHashMap<>();
        mapOffersToFlightSeats(dto, firstBookingFlight, flightMap);

        parms.setFlights(new ArrayList<>(flightMap.values()));
        aerocrs.setParms(parms);
        req.setAerocrs(aerocrs);
        return req;
    }

    private static com.airlines.go7api.responsego7.common.Flight extractFirstBookingFlight(OrderRetrieveRspGo7Dto bookingRsp) {
        if (bookingRsp == null || bookingRsp.getAerocrs() == null || bookingRsp.getAerocrs().getBooking() == null) {
            return null;
        }
        com.airlines.go7api.responsego7.common.Booking booking = bookingRsp.getAerocrs().getBooking();
        if (booking.getFlights() != null && booking.getFlights().getFlight() != null
                && !booking.getFlights().getFlight().isEmpty()) {
            return booking.getFlights().getFlight().get(0);
        }
        if (booking.getItems() != null && booking.getItems().getFlight() != null
                && !booking.getItems().getFlight().isEmpty()) {
            return booking.getItems().getFlight().get(0);
        }
        return null;
    }

    private static void mapOffersToFlightSeats(
            ChangeSeatReqDto dto,
            com.airlines.go7api.responsego7.common.Flight firstBookingFlight,
            java.util.Map<String, Flight> flightMap) {
        if (dto.getOffers() == null) {
            return;
        }
        for (ChangeOfferReqDto offer : dto.getOffers()) {
            if (offer.getOfferItems() != null) {
                for (ChangeOfferReqDto.OfferItemDto item : offer.getOfferItems()) {
                    processOfferItemSeats(item, firstBookingFlight, flightMap);
                }
            }
        }
    }

    private static void processOfferItemSeats(
            ChangeOfferReqDto.OfferItemDto item,
            com.airlines.go7api.responsego7.common.Flight firstBookingFlight,
            java.util.Map<String, Flight> flightMap) {
        if (firstBookingFlight == null) {
            return;
        }

        String key = firstBookingFlight.getNumber() + "|" + firstBookingFlight.getFlightdate();
        Flight reqFlight = flightMap.get(key);

        if (reqFlight == null) {
            reqFlight = new Flight();
            reqFlight.setFlightnumber(firstBookingFlight.getNumber());
            reqFlight.setFlightdate(firstBookingFlight.getFlightdate());
            reqFlight.setFromcode(firstBookingFlight.getFromcode());
            reqFlight.setTocode(firstBookingFlight.getTocode());
            reqFlight.setFlightClass(determineFlightClass(firstBookingFlight));
            reqFlight.setSeat(new ArrayList<>());
            flightMap.put(key, reqFlight);
        }

        addSeatToFlight(reqFlight, item);
    }

    private static String determineFlightClass(com.airlines.go7api.responsego7.common.Flight firstBookingFlight) {
        if (firstBookingFlight.getFlightClass() == null || firstBookingFlight.getFlightClass().isEmpty()) {
            return "Y";
        }
        String fullClass = firstBookingFlight.getFlightClass();
        if (fullClass.contains("/")) {
            return fullClass.split("/")[0];
        }
        return fullClass;
    }

    private static void addSeatToFlight(Flight reqFlight, ChangeOfferReqDto.OfferItemDto item) {
        if (item.getRow() != null && item.getColumn() != null) {
            reqFlight.getSeat().add(item.getRow() + item.getColumn());
        } else {
            reqFlight.getSeat().add(String.valueOf(item.getRow()) + item.getColumn());
        }
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
