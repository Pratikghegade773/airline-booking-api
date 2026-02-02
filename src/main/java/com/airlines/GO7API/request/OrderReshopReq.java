package com.airlines.GO7API.request;

import com.airlines.GO7API.requestDto.OrderReshopReqDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderReshopReq {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @JsonIgnore
    private String apiKey;
    @JsonIgnore
    private String orderReshopUrl;

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

    public String getOrderReshopUrl() {
        return orderReshopUrl;
    }

    public void setOrderReshopUrl(String orderReshopUrl) {
        this.orderReshopUrl = orderReshopUrl;
    }

    // ---------- Nested Classes ----------

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Aerocrs {
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
        private String bookingconfirmation;
        private String action; // "all"
        private Integer flightcode;

        public String getBookingconfirmation() {
            return bookingconfirmation;
        }

        public void setBookingconfirmation(String bookingconfirmation) {
            this.bookingconfirmation = bookingconfirmation;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public Integer getFlightcode() {
            return flightcode;
        }

        public void setFlightcode(Integer flightcode) {
            this.flightcode = flightcode;
        }
    }

    // ---------- Mapping Logic ----------

    public static OrderReshopReq mapToOrderReshopReq(OrderReshopReqDto dto) {
        OrderReshopReq req = new OrderReshopReq();
        req.setApiKey(dto.getApiKey());
        req.setOrderReshopUrl(dto.getOrderReshopUrl());

        if (dto.getAerocrs() != null && dto.getAerocrs().getParms() != null) {
            OrderReshopReqDto.Parms dtoParms = dto.getAerocrs().getParms();
            Aerocrs aerocrs = new Aerocrs();
            Parms parms = new Parms();

            parms.setBookingconfirmation(dtoParms.getBookingconfirmation());
            parms.setAction(dtoParms.getAction());
            parms.setFlightcode(dtoParms.getFlightcode());

            aerocrs.setParms(parms);

            req.setAerocrs(aerocrs);
        }
        return req;
    }

    public Object unmarshal() throws DatatypeConfigurationException, IOException, InterruptedException {
        String response = makeApiCall();
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(response);
    }

    public String makeApiCall() throws IOException {
        String baseUrl = orderReshopUrl;

        String jsonBody = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(this);

        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null)
            headers.add("x-api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        System.out.println("Generated Request is:\n" + jsonBody);

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.POST, entity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            return e.getResponseBodyAsString();
        }
    }
}
