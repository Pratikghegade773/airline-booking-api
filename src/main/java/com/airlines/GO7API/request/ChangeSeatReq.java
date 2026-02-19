package com.airlines.GO7API.request;

import com.airlines.GO7API.requestDto.ChangeSeatReqDto;
import com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChangeSeatReq {

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
        com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.Flight firstBookingFlight = null;

        if (bookingRsp != null && bookingRsp.getAerocrs() != null && bookingRsp.getAerocrs().getBooking() != null) {
            com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.Booking booking = bookingRsp.getAerocrs()
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
            for (ChangeSeatReqDto.Offer offer : dto.getOffers()) {
                if (offer.getOfferItems() != null) {
                    for (ChangeSeatReqDto.Offer.OfferItemDto item : offer.getOfferItems()) {

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

    public Object unmarshal() {
        try {
            String response = makeApiCall();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    false);

            // Check for errors
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);
            if (root.has("errors")) {
                com.airlines.GO7API.error.ErrorRsp errorRsp = new com.airlines.GO7API.error.ErrorRsp();
                com.fasterxml.jackson.databind.JsonNode errorsArray = root.path("errors");
                if (errorsArray.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode errorNode : errorsArray) {
                        com.airlines.GO7API.error.ErrorRsp.Error tempError = new com.airlines.GO7API.error.ErrorRsp.Error();
                        tempError.setError(errorNode.path("message").asText());
                        tempError.setCode(errorNode.path("code").asText());
                        errorRsp.getErrorList().add(tempError);
                    }
                }
                return errorRsp;
            } else {
                return response;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String makeApiCall() throws java.io.IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
        String jsonBody = mapper.writeValueAsString(this);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("auth_id", "70DD4369-72F3-4426-A050-196FBC345009");
        headers.add("auth_password", "vJ3yGilZ9u7N");
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(jsonBody,
                headers);
        System.out.println("Generated ChangeSeat Request is:\n" + jsonBody);

        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
        org.springframework.http.ResponseEntity<String> response = restTemplate
                .postForEntity(changeSeatUrl, entity, String.class);

        System.out.println("HTTP Response Status Code: " + response.getStatusCode());
        System.out.println("ChangeSeat Response: " + response.getBody());

        return response.getBody();
    }
}
