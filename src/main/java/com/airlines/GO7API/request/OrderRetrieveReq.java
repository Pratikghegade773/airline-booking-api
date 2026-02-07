package com.airlines.GO7API.request;

import com.airlines.GO7API.requestDto.OrderRetrieveReqDto;
import com.airlines.GO7API.error.ErrorRsp;
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
public class OrderRetrieveReq {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @JsonIgnore
    private String apiKey;

    @JsonIgnore
    private String orderRetrieveUrl;

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

    public String getOrderRetrieveUrl() {
        return orderRetrieveUrl;
    }

    public void setOrderRetrieveUrl(String orderRetrieveUrl) {
        this.orderRetrieveUrl = orderRetrieveUrl;
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

        @JsonProperty("passengerlastname")
        private String passengerLastName;

        @JsonProperty("generateBookingId")
        private Boolean generateBookingId;

        public String getBookingConfirmation() {
            return bookingConfirmation;
        }

        public void setBookingConfirmation(String bookingConfirmation) {
            this.bookingConfirmation = bookingConfirmation;
        }

        public String getPassengerLastName() {
            return passengerLastName;
        }

        public void setPassengerLastName(String passengerLastName) {
            this.passengerLastName = passengerLastName;
        }

        public Boolean getGenerateBookingId() {
            return generateBookingId;
        }

        public void setGenerateBookingId(Boolean generateBookingId) {
            this.generateBookingId = generateBookingId;
        }
    }

    public static OrderRetrieveReq mapToOrderRetrieveReq(OrderRetrieveReqDto orderRetrieveReqDto,
            String storedBookingConfirmation) {
        OrderRetrieveReq request = new OrderRetrieveReq();
        request.setApiKey(orderRetrieveReqDto.getApiKey());

        // Set URL with fallback
        if (orderRetrieveReqDto.getRetrieveUrl() != null && !orderRetrieveReqDto.getRetrieveUrl().isEmpty()) {
            request.setOrderRetrieveUrl(orderRetrieveReqDto.getRetrieveUrl());
        } else if (orderRetrieveReqDto.getApiUrl() != null && !orderRetrieveReqDto.getApiUrl().isEmpty()) {
            request.setOrderRetrieveUrl(orderRetrieveReqDto.getApiUrl());
        } else {
            request.setOrderRetrieveUrl("https://api.aerocrs.com/v5/getBooking");
        }

        Aerocrs aerocrs = new Aerocrs();
        Parms parms = new Parms();


        // Map to bookingconfirmation (Priority: Stored BookingConfirmation > PNR >
        // OrderID)
        if (storedBookingConfirmation != null && !storedBookingConfirmation.isEmpty()) {
            parms.setBookingConfirmation(storedBookingConfirmation);
        } else if (orderRetrieveReqDto.getPnr() != null && !orderRetrieveReqDto.getPnr().isEmpty()) {
            parms.setBookingConfirmation(orderRetrieveReqDto.getPnr());
        } else if (orderRetrieveReqDto.getOrderId() != null && !orderRetrieveReqDto.getOrderId().isEmpty()) {
            parms.setBookingConfirmation(orderRetrieveReqDto.getOrderId());
        }

        // Map surname if available
        if (orderRetrieveReqDto.getSurname() != null) {
            parms.setPassengerLastName(orderRetrieveReqDto.getSurname());
        }

        aerocrs.setParms(parms);
        request.setAerocrs(aerocrs);

        return request;
    }

    public Object unmarshal() throws DatatypeConfigurationException, IOException, InterruptedException {
        String response = makeApiCall();
        ObjectMapper objectMapper = new ObjectMapper();

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
                // Return raw response or map to response object if available
                return objectMapper.readValue(response, Object.class);
            }
        } catch (Exception e) {
            System.out.println("Error parsing response: " + e.getMessage());
            return response;
        }
    }

    public String makeApiCall() throws IOException {
        String baseUrl = orderRetrieveUrl;
        String jsonBody = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(this);

        HttpHeaders headers = new HttpHeaders();
        // Standard headers
        headers.add("auth_id", "70DD4369-72F3-4426-A050-196FBC345009");
        headers.add("auth_password", "vJ3yGilZ9u7N");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        System.out.println("Generated Request is:\n" + jsonBody);

        RestTemplate restTemplate = new RestTemplate();
        // ObjectMapper mapper = new ObjectMapper(); // Redundant if not used for pretty
        // print only

        try {
            ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.POST, entity, String.class);
            System.out.println("HTTP Response Status Code: " + response.getStatusCode());
            System.out.println("OrderRetrieve Response: " + response.getBody());
            return response.getBody();

        } catch (HttpClientErrorException e) {
            System.out.println("HTTP Error Response: " + e.getResponseBodyAsString());
            return e.getResponseBodyAsString();
        }
    }
}
