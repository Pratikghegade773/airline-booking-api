package com.airlines.GO7API.requestDto;

public class OrderReshopReqDto {

    private String apiKey;
    private String orderReshopUrl;
    private Aerocrs aerocrs;

    public OrderReshopReqDto() {
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

    public Aerocrs getAerocrs() {
        return aerocrs;
    }

    public void setAerocrs(Aerocrs aerocrs) {
        this.aerocrs = aerocrs;
    }

    // ---------- Nested Classes ----------

    public static class Aerocrs {
        private Parms parms;

        public Aerocrs() {
        }

        public Parms getParms() {
            return parms;
        }

        public void setParms(Parms parms) {
            this.parms = parms;
        }
    }

    public static class Parms {
        private String bookingconfirmation;
        private String action; // "all"
        private Integer flightcode;

        public Parms() {
        }

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
}
