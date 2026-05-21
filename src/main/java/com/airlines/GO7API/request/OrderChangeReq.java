package com.airlines.go7api.request;

import com.airlines.go7api.error.ErrorRsp;
import com.airlines.go7api.requestdto.OrderChangeReqDto;
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
public class OrderChangeReq {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @JsonIgnore
    private String apiKey;

    @JsonIgnore
    private String orderChangeUrl;

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

    public String getOrderChangeUrl() {
        return orderChangeUrl;
    }

    public void setOrderChangeUrl(String orderChangeUrl) {
        this.orderChangeUrl = orderChangeUrl;
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
        @JsonProperty("bookingconfirmation")
        private String bookingConfirmation;

        @JsonProperty("action")
        private String action = "amend";

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("bookflight")
        private java.util.List<BookFlight> bookFlight;

        public String getBookingConfirmation() {
            return bookingConfirmation;
        }

        public void setBookingConfirmation(String bookingConfirmation) {
            this.bookingConfirmation = bookingConfirmation;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public java.util.List<BookFlight> getBookFlight() {
            return bookFlight;
        }

        public void setBookFlight(java.util.List<BookFlight> bookFlight) {
            this.bookFlight = bookFlight;
        }
    }

    public static class BookFlight {
        private String fromcode;
        private String tocode;
        private String flightid;
        private String fareid;

        public String getFromcode() { return fromcode; }
        public void setFromcode(String fromcode) { this.fromcode = fromcode; }
        public String getTocode() { return tocode; }
        public void setTocode(String tocode) { this.tocode = tocode; }
        public String getFlightid() { return flightid; }
        public void setFlightid(String flightid) { this.flightid = flightid; }
        public String getFareid() { return fareid; }
        public void setFareid(String fareid) { this.fareid = fareid; }
    }

    public static OrderChangeReq mapToOrderChangeReq(OrderChangeReqDto dto) {
        OrderChangeReq req = new OrderChangeReq();
        req.setApiKey(dto.getApiKey());

        // Use changeBooking endpoint for amendments
        req.setOrderChangeUrl("https://api.aerocrs.com/v5/changeBooking");

        Aerocrs aerocrs = new Aerocrs();
        Parms parms = new Parms();

        parms.setBookingConfirmation(dto.getOrderId());
        parms.setAction("amend");

        if (dto.getOffers() != null && !dto.getOffers().isEmpty()) {
            java.util.List<BookFlight> bookFlights = new java.util.ArrayList<>();
            for (OrderChangeReqDto.Offer offer : dto.getOffers()) {
                String offerId = offer.getOfferId();
                if (offerId != null && offerId.contains("-")) {
                    String[] parts = offerId.split("-");
                    // Structure: flightid-fareid-fromcode-tocode-...
                    if (parts.length >= 4) {
                        BookFlight bf = new BookFlight();
                        bf.setFlightid(parts[0]);
                        bf.setFareid(parts[1]);
                        bf.setFromcode(parts[2]);
                        bf.setTocode(parts[3]);
                        bookFlights.add(bf);
                        
                        // Set currency from the last part of offerId if available
                        if (parms.getCurrency() == null) {
                            parms.setCurrency(parts[parts.length - 1]);
                        }
                    }
                }
            }
            parms.setBookFlight(bookFlights);
        }

        aerocrs.setParms(parms);
        req.setAerocrs(aerocrs);
        return req;
    }

    public Object unmarshal() throws DatatypeConfigurationException, IOException, InterruptedException {
        String response = makeApiCall();
        if (response == null || response.trim().isEmpty()) {
            return null;
        }
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
            System.out.println("Error parsing OrderChange response: " + e.getMessage());
            return response;
        }
    }

    public String makeApiCall() throws IOException {
        String baseUrl = orderChangeUrl;

        // Simplify URL: Only include bookingconfirmation
        StringBuilder urlWithParams = new StringBuilder(baseUrl);
        Parms p = this.aerocrs.getParms();
        if (p != null && p.getBookingConfirmation() != null) {
            urlWithParams.append("?bookingconfirmation=").append(p.getBookingConfirmation());
        }

        String finalUrl = urlWithParams.toString();
        String jsonBody = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(this);

        HttpHeaders headers = new HttpHeaders();
        headers.add("auth_id", "70DD4369-72F3-4426-A050-196FBC345009");
        headers.add("auth_password", "vJ3yGilZ9u7N");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        System.out.println("Generated OrderChange Request URL: " + finalUrl);
        System.out.println("Generated OrderChange Request Body:\n" + jsonBody);

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.exchange(finalUrl, HttpMethod.POST, entity, String.class);
            System.out.println("HTTP Response Status Code: " + response.getStatusCode());
            System.out.println("OrderChange Response: " + response.getBody());
            return response.getBody();

        } catch (HttpClientErrorException e) {
            System.out.println("HTTP Error Status Code: " + e.getStatusCode());
            System.out.println("HTTP Error Response: " + e.getResponseBodyAsString());
            return e.getResponseBodyAsString();
        } catch (Exception e) {
            System.out.println("General Error in makeApiCall: " + e.getMessage());
            return null;
        }
    }
}
