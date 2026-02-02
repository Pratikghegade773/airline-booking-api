package com.airlines.GO7API.requestDto;

public class OrderRetrieveReqDto {

    private Aerocrs aerocrs;

    public OrderRetrieveReqDto() {
    }

    public OrderRetrieveReqDto(Aerocrs aerocrs) {
        this.aerocrs = aerocrs;
    }

    public Aerocrs getAerocrs() {
        return aerocrs;
    }

    public void setAerocrs(Aerocrs aerocrs) {
        this.aerocrs = aerocrs;
    }

    private String apiKey;
    private String orderRetrieveUrl;

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
        private String bookingconfirmation; // mandatory
        private String passengerlastname; // optional depending on use
        private Boolean generateBookingId; // optional

        public Parms() {
        }

        public Parms(String bookingconfirmation, String passengerlastname, Boolean generateBookingId) {
            this.bookingconfirmation = bookingconfirmation;
            this.passengerlastname = passengerlastname;
            this.generateBookingId = generateBookingId;
        }

        public String getBookingconfirmation() {
            return bookingconfirmation;
        }

        public void setBookingconfirmation(String bookingconfirmation) {
            this.bookingconfirmation = bookingconfirmation;
        }

        public String getPassengerlastname() {
            return passengerlastname;
        }

        public void setPassengerlastname(String passengerlastname) {
            this.passengerlastname = passengerlastname;
        }

        public Boolean getGenerateBookingId() {
            return generateBookingId;
        }

        public void setGenerateBookingId(Boolean generateBookingId) {
            this.generateBookingId = generateBookingId;
        }
    }
}
