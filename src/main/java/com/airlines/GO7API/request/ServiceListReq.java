package com.airlines.GO7API.request;

import com.airlines.GO7API.error.ErrorRsp;
import com.airlines.GO7API.requestDto.ServiceListReqDto;
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
public class ServiceListReq {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @JsonIgnore
    private String apiKey;

    @JsonIgnore
    private String serviceListUrl;

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

    public String getServiceListUrl() {
        return serviceListUrl;
    }

    public void setServiceListUrl(String serviceListUrl) {
        this.serviceListUrl = serviceListUrl;
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

        @JsonProperty("flightid")
        private Long flightId;

        @JsonProperty("currency")
        private String currency;

        public Long getBookingId() {
            return bookingId;
        }

        public void setBookingId(Long bookingId) {
            this.bookingId = bookingId;
        }

        public Long getFlightId() {
            return flightId;
        }

        public void setFlightId(Long flightId) {
            this.flightId = flightId;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }

    public static ServiceListReq mapToServiceListRequestDTO(ServiceListReqDto dto,
            com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto bookingRsp) {
        ServiceListReq req = new ServiceListReq();
        req.setApiKey(dto.getApiKey());

        if (dto.getServiceListUrl() != null && !dto.getServiceListUrl().isEmpty()) {
            req.setServiceListUrl(dto.getServiceListUrl());
        } else {
            req.setServiceListUrl("https://api.aerocrs.com/v5/getAncillaries");
        }

        Aerocrs aerocrs = new Aerocrs();
        Parms parms = new Parms();

        // Map from Booking Response
        if (bookingRsp != null && bookingRsp.getAerocrs() != null && bookingRsp.getAerocrs().getBooking() != null) {
            com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.Booking booking = bookingRsp.getAerocrs()
                    .getBooking();

            // 1. Booking ID
            if (booking.getBookingid() != null) {
                parms.setBookingId(booking.getBookingid());
            }

            // 2. Currency
            if (booking.getCurrency() != null) {
                parms.setCurrency(booking.getCurrency());
            } else if (booking.getDefaultCurrency() != null) {
                parms.setCurrency(booking.getDefaultCurrency());
            } else {
                parms.setCurrency("USD");
            }

            // 3. Flight ID (Take first flight)
            if (booking.getFlights() != null && booking.getFlights().getFlight() != null
                    && !booking.getFlights().getFlight().isEmpty()) {
                com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.Flight firstFlight = booking.getFlights()
                        .getFlight().get(0);
                parms.setFlightId((long) firstFlight.getFlightid());
            } else if (booking.getItems() != null && booking.getItems().getFlight() != null
                    && !booking.getItems().getFlight().isEmpty()) {
                com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.Flight firstFlight = booking.getItems()
                        .getFlight().get(0);
                parms.setFlightId((long) firstFlight.getFlightid());
            }
        }

        aerocrs.setParms(parms);
        req.setAerocrs(aerocrs);
        return req;
    }

    public static com.airlines.GO7API.request.OrderRetrieveReq mapToGetBookingReq(String bookingConfirmation) {
        com.airlines.GO7API.request.OrderRetrieveReq req = new com.airlines.GO7API.request.OrderRetrieveReq();
        req.setApiKey("8d123dcd262ad942852233f81e649089"); // Default or constant if not passed
        req.setOrderRetrieveUrl("https://api.aerocrs.com/v5/getBooking");

        com.airlines.GO7API.request.OrderRetrieveReq.Aerocrs aerocrs = new com.airlines.GO7API.request.OrderRetrieveReq.Aerocrs();
        com.airlines.GO7API.request.OrderRetrieveReq.Parms parms = new com.airlines.GO7API.request.OrderRetrieveReq.Parms();
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
            System.out.println("Error parsing ServiceList response: " + e.getMessage());
            return response;
        }
    }

    public String makeApiCall() throws IOException {
        String baseUrl = serviceListUrl;
        String jsonBody = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(this);

        HttpHeaders headers = new HttpHeaders();
        // Standard headers matching OrderCancel/OrderRetrieve
        headers.add("auth_id", "70DD4369-72F3-4426-A050-196FBC345009");
        headers.add("auth_password", "vJ3yGilZ9u7N");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        System.out.println("Generated ServiceList Request is:\n" + jsonBody);

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.POST, entity, String.class);
            System.out.println("HTTP Response Status Code: " + response.getStatusCode());
            System.out.println("ServiceList Response: " + response.getBody());
            return response.getBody();

        } catch (HttpClientErrorException e) {
            System.out.println("HTTP Error Response: " + e.getResponseBodyAsString());
            return e.getResponseBodyAsString();
        }
    }
}
