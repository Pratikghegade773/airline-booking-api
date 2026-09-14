package com.airlines.go7api.request;

import com.airlines.go7api.requestdto.UnpaidCancelReqDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnpaidCancelReq extends BaseGo7Req {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UnpaidCancelReq.class);

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
                if (logger.isWarnEnabled()) {
                    logger.warn("Invalid Booking ID for Cancel: {}", unpaidCancelReqDto.getOrderId());
                }
            }
        }

        aerocrs.setParms(parms);
        request.setAerocrs(aerocrs);

        return request;
    }

    @Override
    protected String getApiUrl() {
        return unpaidCancelUrl != null ? unpaidCancelUrl : "";
    }

    @Override
    protected String getRequestName() {
        return "UnpaidCancel";
    }
}
