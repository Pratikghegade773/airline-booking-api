package com.airlines.go7api.request;

import com.airlines.go7api.requestdto.UnpaidCancelReqDto;
import com.airlines.go7api.error.ErrorRsp; // Assuming ErrorRsp exists
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
public class UnpaidCancelReq {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @JsonIgnore
    private String apiKey;

    @JsonIgnore
    private String unpaidCancelUrl;

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

    public String getUnpaidCancelUrl() {
        return unpaidCancelUrl;
    }

    public void setUnpaidCancelUrl(String unpaidCancelUrl) {
        this.unpaidCancelUrl = unpaidCancelUrl;
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

        public Long getBookingId() {
            return bookingId;
        }

        public void setBookingId(Long bookingId) {
            this.bookingId = bookingId;
        }
    }

    public static UnpaidCancelReq mapToUnpaidCancelReq(UnpaidCancelReqDto unpaidCancelReqDto) {
        UnpaidCancelReq request = new UnpaidCancelReq();
        request.setApiKey(unpaidCancelReqDto.getApiKey());
        if (unpaidCancelReqDto.getCancelUrl() != null && !unpaidCancelReqDto.getCancelUrl().isEmpty()) {
            request.setUnpaidCancelUrl(unpaidCancelReqDto.getCancelUrl());
        } else {
            request.setUnpaidCancelUrl("https://api.aerocrs.com/v5/cancelBooking");
        }

        Aerocrs aerocrs = new Aerocrs();
        Parms parms = new Parms();

        if (unpaidCancelReqDto.getOrderId() != null) {
            try {
                parms.setBookingId(Long.parseLong(unpaidCancelReqDto.getOrderId()));
            } catch (NumberFormatException e) {
                System.out.println("Invalid Booking ID for Cancel: " + unpaidCancelReqDto.getOrderId());
            }
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
                return objectMapper.readValue(response, Object.class);
            }
        } catch (Exception e) {
            System.out.println("Error parsing response: " + e.getMessage());
            return response;
        }
    }

    public String makeApiCall() throws IOException {
        String baseUrl = "https://api.aerocrs.com/v5/cancelBooking";
        String jsonBody = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(this);

        HttpHeaders headers = new HttpHeaders();
        // Standard headers matching CheckPayment/OrderRetrieve
        headers.add("auth_id", "70DD4369-72F3-4426-A050-196FBC345009");
        headers.add("auth_password", "vJ3yGilZ9u7N");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        System.out.println("Generated UnpaidCancel Request is:\n" + jsonBody);

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.POST, entity, String.class);
            System.out.println("HTTP Response Status Code: " + response.getStatusCode());
            System.out.println("UnpaidCancel Response: " + response.getBody());
            return response.getBody();

        } catch (HttpClientErrorException e) {
            System.out.println("HTTP Error Response: " + e.getResponseBodyAsString());
            return e.getResponseBodyAsString();
        }
    }
}
