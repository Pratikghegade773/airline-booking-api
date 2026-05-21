package com.airlines.go7api.request;

import com.airlines.go7api.error.ErrorRsp;
import com.airlines.go7api.requestdto.SeatAvailabilityReqDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeatAvailabilityReq {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @JsonIgnore
    private String apiKey;

    @JsonIgnore
    private String seatAvailabilityUrl;

    public Aerocrs getAerocrs() {
        return aerocrs;
    }

    public void setAerocrs(Aerocrs aerocrs) {
        this.aerocrs = aerocrs;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSeatAvailabilityUrl() {
        return seatAvailabilityUrl;
    }

    public void setSeatAvailabilityUrl(String seatAvailabilityUrl) {
        this.seatAvailabilityUrl = seatAvailabilityUrl;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Aerocrs {
        @JsonProperty("parms")
        private Parms parms;

        public Parms getParms() {
            return parms;
        }

        public void setParms(Parms parms) {
            this.parms = parms;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Parms {
        @JsonProperty("bookingid")
        private Long bookingId;

        @JsonProperty("companycode")
        private String companyCode;

        @JsonProperty("flightnumber")
        private String flightNumber;

        @JsonProperty("flightdate")
        private String flightDate;

        @JsonProperty("fromcode")
        private String fromCode;

        @JsonProperty("tocode")
        private String toCode;

        public Long getBookingId() {
            return bookingId;
        }

        public void setBookingId(Long bookingId) {
            this.bookingId = bookingId;
        }

        public String getCompanyCode() {
            return companyCode;
        }

        public void setCompanyCode(String companyCode) {
            this.companyCode = companyCode;
        }

        public String getFlightNumber() {
            return flightNumber;
        }

        public void setFlightNumber(String flightNumber) {
            this.flightNumber = flightNumber;
        }

        public String getFlightDate() {
            return flightDate;
        }

        public void setFlightDate(String flightDate) {
            this.flightDate = flightDate;
        }

        public String getFromCode() {
            return fromCode;
        }

        public void setFromCode(String fromCode) {
            this.fromCode = fromCode;
        }

        public String getToCode() {
            return toCode;
        }

        public void setToCode(String toCode) {
            this.toCode = toCode;
        }
    }

    public static SeatAvailabilityReq mapToSeatAvailabilityRequestDTO(SeatAvailabilityReqDto dto,
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp) {
        SeatAvailabilityReq req = new SeatAvailabilityReq();
        req.setApiKey(dto.getApiKey());

        if (dto.getSeatAvailabilityUrl() != null && !dto.getSeatAvailabilityUrl().isEmpty()) {
            req.setSeatAvailabilityUrl(dto.getSeatAvailabilityUrl());
        } else {
            req.setSeatAvailabilityUrl("https://api.aerocrs.com/v5/getSeatMapFare");
        }

        Aerocrs aerocrs = new Aerocrs();
        Parms parms = new Parms();

        // 1. Map bookingid from orderId if present in DTO or from bookingRsp
        if (dto.getOrderId() != null && !dto.getOrderId().isEmpty()) {
            try {
                parms.setBookingId(Long.parseLong(dto.getOrderId()));
            } catch (NumberFormatException e) {
                // Ignore invalid ID
            }
        }

        // 2. Map default company code
        String companyCode = "API";
        // No companyCode in DTO anymore, use strictly default or from booking if
        // possible
        parms.setCompanyCode(companyCode);

        // 3. Map Flight Params from Booking Response (Preferred)
        if (bookingRsp != null && bookingRsp.getAerocrs() != null && bookingRsp.getAerocrs().getBooking() != null) {
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto.Booking booking = bookingRsp.getAerocrs()
                    .getBooking();

            // Extract First Flight Details
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto.Flight firstFlight = null;
            if (booking.getFlights() != null && booking.getFlights().getFlight() != null
                    && !booking.getFlights().getFlight().isEmpty()) {
                firstFlight = booking.getFlights().getFlight().get(0);
            } else if (booking.getItems() != null && booking.getItems().getFlight() != null
                    && !booking.getItems().getFlight().isEmpty()) {
                firstFlight = booking.getItems().getFlight().get(0);
            }

            if (firstFlight != null) {
                parms.setFlightNumber(firstFlight.getNumber());
                parms.setFlightDate(firstFlight.getFlightdate());
                parms.setFromCode(firstFlight.getFromcode());
                parms.setToCode(firstFlight.getTocode());
            }
        }

        aerocrs.setParms(parms);
        req.setAerocrs(aerocrs);
        return req;
    }

    public static com.airlines.go7api.request.OrderRetrieveReq mapToGetBookingReq(String bookingConfirmation) {
        com.airlines.go7api.request.OrderRetrieveReq req = new com.airlines.go7api.request.OrderRetrieveReq();
        req.setApiKey("8d123dcd262ad942852233f81e649089");
        req.setOrderRetrieveUrl("https://api.aerocrs.com/v5/getBooking");

        com.airlines.go7api.request.OrderRetrieveReq.Aerocrs aerocrs = new com.airlines.go7api.request.OrderRetrieveReq.Aerocrs();
        com.airlines.go7api.request.OrderRetrieveReq.Parms parms = new com.airlines.go7api.request.OrderRetrieveReq.Parms();
        parms.setBookingConfirmation(bookingConfirmation);

        aerocrs.setParms(parms);
        req.setAerocrs(aerocrs);
        return req;
    }

    public Object unmarshal() throws DatatypeConfigurationException, IOException, InterruptedException {
        String response = makeApiCall();
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            JsonNode root = objectMapper.readTree(response);

            if (root.has("errors")) {
                ErrorRsp errorRsp = new ErrorRsp();
                JsonNode errorsArray = root.path("errors");

                if (errorsArray.isArray()) {
                    for (JsonNode errorNode : errorsArray) {
                        String errorMessage = errorNode.path("message").asText();
                        String code = errorNode.path("code").asText();

                        ErrorRsp.Error tempError = new ErrorRsp.Error();
                        tempError.setError(errorMessage);
                        tempError.setCode(code);
                        errorRsp.getErrorList().add(tempError);
                    }
                }
                return errorRsp;
            } else {
                return objectMapper.readValue(response, Object.class);
            }
        } catch (Exception e) {
            System.out.println("Error parsing SeatAvailability response: " + e.getMessage());
            return response;
        }
    }

    public String makeApiCall() throws IOException {
        String baseUrl = seatAvailabilityUrl;
        String jsonBody = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(this);

        HttpHeaders headers = new HttpHeaders();
        // Standard headers matching OrderCancel/OrderRetrieve
        headers.add("auth_id", "70DD4369-72F3-4426-A050-196FBC345009");
        headers.add("auth_password", "vJ3yGilZ9u7N");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        System.out.println("Generated SeatAvailability Request is:\n" + jsonBody);

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.POST, entity, String.class);
            System.out.println("HTTP Response Status Code: " + response.getStatusCode());
            System.out.println("SeatAvailability Response: " + response.getBody());
            return response.getBody();

        } catch (HttpClientErrorException e) {
            System.out.println("HTTP Error Response: " + e.getResponseBodyAsString());
            return e.getResponseBodyAsString();
        }
    }
}
