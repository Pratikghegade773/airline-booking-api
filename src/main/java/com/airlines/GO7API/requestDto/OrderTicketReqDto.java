package com.airlines.GO7API.requestDto;

public class OrderTicketReqDto {

    private Aerocrs aerocrs;
    private String apiKey;
    private String orderTicketUrl;

    public OrderTicketReqDto() {
    }

    public OrderTicketReqDto(Aerocrs aerocrs) {
        this.aerocrs = aerocrs;
    }

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

    public String getOrderTicketUrl() {
        return orderTicketUrl;
    }

    public void setOrderTicketUrl(String orderTicketUrl) {
        this.orderTicketUrl = orderTicketUrl;
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
        // -------- Ticket / Booking --------
        private Long bookingid; // mandatory for all
        private String currency; // optional

        // -------- Create Payment (PSP/Manual) --------
        private String bookingconfirmation;
        private String paymentmethodtoken;
        private Double amountpaid;
        private String amountcurrency;
        private String paymentref;

        // -------- Execute Payment (Credit Card) --------
        private String creditcardpayer;
        private String creditcardnumber;
        private String creditcardexpiry; // MMYY
        private String creditcardcvv;

        private Boolean transaction3ds;
        private Params3ds params3ds;

        private Boolean ismobilepayment;
        private String mobileaction;
        private Mobile mobile;

        public Parms() {
        }

        // Getters and Setters

        public Long getBookingid() {
            return bookingid;
        }

        public void setBookingid(Long bookingid) {
            this.bookingid = bookingid;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getBookingconfirmation() {
            return bookingconfirmation;
        }

        public void setBookingconfirmation(String bookingconfirmation) {
            this.bookingconfirmation = bookingconfirmation;
        }

        public String getPaymentmethodtoken() {
            return paymentmethodtoken;
        }

        public void setPaymentmethodtoken(String paymentmethodtoken) {
            this.paymentmethodtoken = paymentmethodtoken;
        }

        public Double getAmountpaid() {
            return amountpaid;
        }

        public void setAmountpaid(Double amountpaid) {
            this.amountpaid = amountpaid;
        }

        public String getAmountcurrency() {
            return amountcurrency;
        }

        public void setAmountcurrency(String amountcurrency) {
            this.amountcurrency = amountcurrency;
        }

        public String getPaymentref() {
            return paymentref;
        }

        public void setPaymentref(String paymentref) {
            this.paymentref = paymentref;
        }

        public String getCreditcardpayer() {
            return creditcardpayer;
        }

        public void setCreditcardpayer(String creditcardpayer) {
            this.creditcardpayer = creditcardpayer;
        }

        public String getCreditcardnumber() {
            return creditcardnumber;
        }

        public void setCreditcardnumber(String creditcardnumber) {
            this.creditcardnumber = creditcardnumber;
        }

        public String getCreditcardexpiry() {
            return creditcardexpiry;
        }

        public void setCreditcardexpiry(String creditcardexpiry) {
            this.creditcardexpiry = creditcardexpiry;
        }

        public String getCreditcardcvv() {
            return creditcardcvv;
        }

        public void setCreditcardcvv(String creditcardcvv) {
            this.creditcardcvv = creditcardcvv;
        }

        public Boolean getTransaction3ds() {
            return transaction3ds;
        }

        public void setTransaction3ds(Boolean transaction3ds) {
            this.transaction3ds = transaction3ds;
        }

        public Params3ds getParams3ds() {
            return params3ds;
        }

        public void setParams3ds(Params3ds params3ds) {
            this.params3ds = params3ds;
        }

        public Boolean getIsmobilepayment() {
            return ismobilepayment;
        }

        public void setIsmobilepayment(Boolean ismobilepayment) {
            this.ismobilepayment = ismobilepayment;
        }

        public String getMobileaction() {
            return mobileaction;
        }

        public void setMobileaction(String mobileaction) {
            this.mobileaction = mobileaction;
        }

        public Mobile getMobile() {
            return mobile;
        }

        public void setMobile(Mobile mobile) {
            this.mobile = mobile;
        }
    }

    // ---------- Nested Classes for Payment ----------

    public static class Params3ds {
        private String eci;
        private String xid;
        private String cavv;
        private String enrolled;
        private String status_3d;
        private String version_3d;

        public Params3ds() {
        }

        public String getEci() {
            return eci;
        }

        public void setEci(String eci) {
            this.eci = eci;
        }

        public String getXid() {
            return xid;
        }

        public void setXid(String xid) {
            this.xid = xid;
        }

        public String getCavv() {
            return cavv;
        }

        public void setCavv(String cavv) {
            this.cavv = cavv;
        }

        public String getEnrolled() {
            return enrolled;
        }

        public void setEnrolled(String enrolled) {
            this.enrolled = enrolled;
        }

        public String getStatus_3d() {
            return status_3d;
        }

        public void setStatus_3d(String status_3d) {
            this.status_3d = status_3d;
        }

        public String getVersion_3d() {
            return version_3d;
        }

        public void setVersion_3d(String version_3d) {
            this.version_3d = version_3d;
        }
    }

    public static class Mobile {
        private String mobiletype;
        private String mobilecountry;
        private String phonenumber;

        public Mobile() {
        }

        public String getMobiletype() {
            return mobiletype;
        }

        public void setMobiletype(String mobiletype) {
            this.mobiletype = mobiletype;
        }

        public String getMobilecountry() {
            return mobilecountry;
        }

        public void setMobilecountry(String mobilecountry) {
            this.mobilecountry = mobilecountry;
        }

        public String getPhonenumber() {
            return phonenumber;
        }

        public void setPhonenumber(String phonenumber) {
            this.phonenumber = phonenumber;
        }
    }
}
