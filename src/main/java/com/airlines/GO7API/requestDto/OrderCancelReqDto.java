package com.airlines.GO7API.requestDto;

public class OrderCancelReqDto {

    private Aerocrs aerocrs;

    public OrderCancelReqDto() {
    }

    public OrderCancelReqDto(Aerocrs aerocrs) {
        this.aerocrs = aerocrs;
    }

    public Aerocrs getAerocrs() {
        return aerocrs;
    }

    public void setAerocrs(Aerocrs aerocrs) {
        this.aerocrs = aerocrs;
    }

    private String apiKey;
    private String orderCancelUrl;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getOrderCancelUrl() {
        return orderCancelUrl;
    }

    public void setOrderCancelUrl(String orderCancelUrl) {
        this.orderCancelUrl = orderCancelUrl;
    }

    // ---------- Nested Classes ----------

    public static class Aerocrs {
        private Parms parms;

        public Aerocrs() {
        }

        public Aerocrs(Parms parms) {
            this.parms = parms;
        }

        public Parms getParms() {
            return parms;
        }

        public void setParms(Parms parms) {
            this.parms = parms;
        }
    }

    public static class Parms {
        private Long bookingid; // mandatory

        public Parms() {
        }

        public Parms(Long bookingid) {
            this.bookingid = bookingid;
        }

        public Long getBookingid() {
            return bookingid;
        }

        public void setBookingid(Long bookingid) {
            this.bookingid = bookingid;
        }
    }
}
