package com.airlines.GO7API.requestDto;

import java.util.List;

public class OrderConfirmReqDto {

    private Aerocrs aerocrs;

    public OrderConfirmReqDto() {
    }

    public Aerocrs getAerocrs() {
        return aerocrs;
    }

    public void setAerocrs(Aerocrs aerocrs) {
        this.aerocrs = aerocrs;
    }

    private String apiKey;
    private String orderConfirmUrl;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getOrderConfirmUrl() {
        return orderConfirmUrl;
    }

    public void setOrderConfirmUrl(String orderConfirmUrl) {
        this.orderConfirmUrl = orderConfirmUrl;
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

        // REQUIRED
        private Long bookingid;
        private String agentconfirmation;

        // OPTIONAL
        private String remarks;
        private String confirmationemail;
        private Boolean holdBooking;
        private Boolean sendemail;

        // REQUIRED
        private List<Passenger> passenger;

        public Parms() {
        }

        public Long getBookingid() {
            return bookingid;
        }

        public void setBookingid(Long bookingid) {
            this.bookingid = bookingid;
        }

        public String getAgentconfirmation() {
            return agentconfirmation;
        }

        public void setAgentconfirmation(String agentconfirmation) {
            this.agentconfirmation = agentconfirmation;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }

        public String getConfirmationemail() {
            return confirmationemail;
        }

        public void setConfirmationemail(String confirmationemail) {
            this.confirmationemail = confirmationemail;
        }

        public Boolean getHoldBooking() {
            return holdBooking;
        }

        public void setHoldBooking(Boolean holdBooking) {
            this.holdBooking = holdBooking;
        }

        public Boolean getSendemail() {
            return sendemail;
        }

        public void setSendemail(Boolean sendemail) {
            this.sendemail = sendemail;
        }

        public List<Passenger> getPassenger() {
            return passenger;
        }

        public void setPassenger(List<Passenger> passenger) {
            this.passenger = passenger;
        }
    }

    public static class Passenger {

        private String paxtitle;
        private String firstname;
        private String lastname;
        private String paxage;
        private String paxnationailty;
        private String paxdoctype;
        private String paxdocnumber;
        private String paxdocissuer;
        private String paxdocexpiry;
        private String paxbirthdate;
        private String paxvisanumber;
        private String paxvisaexpiry;
        private String paxphone;
        private String paxemail;
        private Integer paxweight;
        private Boolean paxcarringinfant;

        public Passenger() {
        }

        public String getPaxtitle() {
            return paxtitle;
        }

        public void setPaxtitle(String paxtitle) {
            this.paxtitle = paxtitle;
        }

        public String getFirstname() {
            return firstname;
        }

        public void setFirstname(String firstname) {
            this.firstname = firstname;
        }

        public String getLastname() {
            return lastname;
        }

        public void setLastname(String lastname) {
            this.lastname = lastname;
        }

        public String getPaxage() {
            return paxage;
        }

        public void setPaxage(String paxage) {
            this.paxage = paxage;
        }

        public String getPaxnationailty() {
            return paxnationailty;
        }

        public void setPaxnationailty(String paxnationailty) {
            this.paxnationailty = paxnationailty;
        }

        public String getPaxdoctype() {
            return paxdoctype;
        }

        public void setPaxdoctype(String paxdoctype) {
            this.paxdoctype = paxdoctype;
        }

        public String getPaxdocnumber() {
            return paxdocnumber;
        }

        public void setPaxdocnumber(String paxdocnumber) {
            this.paxdocnumber = paxdocnumber;
        }

        public String getPaxdocissuer() {
            return paxdocissuer;
        }

        public void setPaxdocissuer(String paxdocissuer) {
            this.paxdocissuer = paxdocissuer;
        }

        public String getPaxdocexpiry() {
            return paxdocexpiry;
        }

        public void setPaxdocexpiry(String paxdocexpiry) {
            this.paxdocexpiry = paxdocexpiry;
        }

        public String getPaxbirthdate() {
            return paxbirthdate;
        }

        public void setPaxbirthdate(String paxbirthdate) {
            this.paxbirthdate = paxbirthdate;
        }

        public String getPaxvisanumber() {
            return paxvisanumber;
        }

        public void setPaxvisanumber(String paxvisanumber) {
            this.paxvisanumber = paxvisanumber;
        }

        public String getPaxvisaexpiry() {
            return paxvisaexpiry;
        }

        public void setPaxvisaexpiry(String paxvisaexpiry) {
            this.paxvisaexpiry = paxvisaexpiry;
        }

        public String getPaxphone() {
            return paxphone;
        }

        public void setPaxphone(String paxphone) {
            this.paxphone = paxphone;
        }

        public String getPaxemail() {
            return paxemail;
        }

        public void setPaxemail(String paxemail) {
            this.paxemail = paxemail;
        }

        public Integer getPaxweight() {
            return paxweight;
        }

        public void setPaxweight(Integer paxweight) {
            this.paxweight = paxweight;
        }

        public Boolean getPaxcarringinfant() {
            return paxcarringinfant;
        }

        public void setPaxcarringinfant(Boolean paxcarringinfant) {
            this.paxcarringinfant = paxcarringinfant;
        }
    }
}
