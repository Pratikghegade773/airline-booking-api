package com.airlines.go7api.request;

import com.airlines.go7api.requestdto.OrderRetrieveReqDto;
import com.airlines.go7api.error.ErrorRsp;
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
public class OrderRetrieveReq extends BaseGo7Req {

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

        @JsonProperty("bookingid")
        private Long bookingId;

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

        public Long getBookingId() {
            return bookingId;
        }

        public void setBookingId(Long bookingId) {
            this.bookingId = bookingId;
        }
    }

    public static OrderRetrieveReq mapToOrderRetrieveReq(OrderRetrieveReqDto orderRetrieveReqDto,
            String storedBookingConfirmation) {
        OrderRetrieveReq request = new OrderRetrieveReq();

        if (orderRetrieveReqDto != null) {
            request.setApiKey(orderRetrieveReqDto.getApiKey());

            // Set URL with fallback
            if (orderRetrieveReqDto.getRetrieveUrl() != null && !orderRetrieveReqDto.getRetrieveUrl().isEmpty()) {
                request.setOrderRetrieveUrl(orderRetrieveReqDto.getRetrieveUrl());
            } else if (orderRetrieveReqDto.getApiUrl() != null && !orderRetrieveReqDto.getApiUrl().isEmpty()) {
                request.setOrderRetrieveUrl(orderRetrieveReqDto.getApiUrl());
            } else {
                request.setOrderRetrieveUrl("https://api.aerocrs.com/v5/getBooking");
            }
        } else {
            // Default URL if DTO is null
            request.setOrderRetrieveUrl("https://api.aerocrs.com/v5/getBooking");
        }

        Aerocrs aerocrs = new Aerocrs();
        Parms parms = new Parms();

        // Map to bookingid OR bookingconfirmation (Priority: Stored BookingConfirmation
        // > PNR >
        // OrderID)
        String idToUse = null;
        if (storedBookingConfirmation != null && !storedBookingConfirmation.isEmpty()) {
            idToUse = storedBookingConfirmation;
        } else if (orderRetrieveReqDto != null) {
            if (orderRetrieveReqDto.getPnr() != null && !orderRetrieveReqDto.getPnr().isEmpty()) {
                idToUse = orderRetrieveReqDto.getPnr();
            } else if (orderRetrieveReqDto.getOrderId() != null && !orderRetrieveReqDto.getOrderId().isEmpty()) {
                idToUse = orderRetrieveReqDto.getOrderId();
            }
        }

        if (idToUse != null) {
            // Smart Mapping: If numeric -> BookingID, Else -> BookingConfirmation/PNR
            if (idToUse.matches("\\d+")) {
                try {
                    parms.setBookingId(Long.parseLong(idToUse));
                } catch (NumberFormatException e) {
                    parms.setBookingConfirmation(idToUse);
                }
            } else {
                parms.setBookingConfirmation(idToUse);
            }
        }

        // Map surname if available
        if (orderRetrieveReqDto != null && orderRetrieveReqDto.getSurname() != null) {
            parms.setPassengerLastName(orderRetrieveReqDto.getSurname());
        }

        aerocrs.setParms(parms);
        request.setAerocrs(aerocrs);

        return request;
    }

    @Override
    protected String getApiUrl() {
        return orderRetrieveUrl != null ? orderRetrieveUrl : "";
    }

    @Override
    protected String getRequestName() {
        return "OrderRetrieve";
    }
}
