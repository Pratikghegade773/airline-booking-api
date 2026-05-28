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
public class OrderChangeReq extends BaseGo7Req {

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

    @Override
    protected String getApiUrl() {
        String baseUrl = orderChangeUrl != null ? orderChangeUrl : "";
        StringBuilder urlWithParams = new StringBuilder(baseUrl);
        Parms p = this.aerocrs != null ? this.aerocrs.getParms() : null;
        if (p != null && p.getBookingConfirmation() != null) {
            urlWithParams.append("?bookingconfirmation=").append(p.getBookingConfirmation());
        }
        return urlWithParams.toString();
    }

    @Override
    protected String getRequestName() {
        return "OrderChange";
    }
}
