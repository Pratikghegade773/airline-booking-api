package com.airlines.go7api.request;

import com.airlines.go7api.requestdto.OrderRetrieveReqDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


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

    private static final String DEFAULT_RETRIEVE_URL = "https://api.aerocrs.com/v5/getBooking";

    private static String determineRetrieveUrl(OrderRetrieveReqDto orderRetrieveReqDto) {
        if (orderRetrieveReqDto == null) {
            return DEFAULT_RETRIEVE_URL;
        }
        if (orderRetrieveReqDto.getRetrieveUrl() != null && !orderRetrieveReqDto.getRetrieveUrl().isEmpty()) {
            return orderRetrieveReqDto.getRetrieveUrl();
        }
        if (orderRetrieveReqDto.getApiUrl() != null && !orderRetrieveReqDto.getApiUrl().isEmpty()) {
            return orderRetrieveReqDto.getApiUrl();
        }
        return DEFAULT_RETRIEVE_URL;
    }

    private static String determineIdToUse(OrderRetrieveReqDto orderRetrieveReqDto, String storedBookingConfirmation) {
        if (storedBookingConfirmation != null && !storedBookingConfirmation.isEmpty()) {
            return storedBookingConfirmation;
        }
        if (orderRetrieveReqDto == null) {
            return null;
        }
        if (orderRetrieveReqDto.getPnr() != null && !orderRetrieveReqDto.getPnr().isEmpty()) {
            return orderRetrieveReqDto.getPnr();
        }
        if (orderRetrieveReqDto.getOrderId() != null && !orderRetrieveReqDto.getOrderId().isEmpty()) {
            return orderRetrieveReqDto.getOrderId();
        }
        return null;
    }

    private static void populateParmsId(Parms parms, String idToUse) {
        if (idToUse == null) {
            return;
        }
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

    public static OrderRetrieveReq mapToOrderRetrieveReq(OrderRetrieveReqDto orderRetrieveReqDto,
            String storedBookingConfirmation) {
        OrderRetrieveReq request = new OrderRetrieveReq();

        if (orderRetrieveReqDto != null) {
            request.setApiKey(orderRetrieveReqDto.getApiKey());
        }
        request.setOrderRetrieveUrl(determineRetrieveUrl(orderRetrieveReqDto));

        Aerocrs aerocrs = new Aerocrs();
        Parms parms = new Parms();

        String idToUse = determineIdToUse(orderRetrieveReqDto, storedBookingConfirmation);
        populateParmsId(parms, idToUse);

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
