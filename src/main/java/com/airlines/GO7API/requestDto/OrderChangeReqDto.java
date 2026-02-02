package com.airlines.GO7API.requestDto;

import java.util.List;

public class OrderChangeReqDto {

    private String apiKey;
    private String orderChangeUrl;
    private Aerocrs aerocrs;

    public OrderChangeReqDto() {
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
        // Shared & Logic
        private Long bookingid; // Can be 0 for new reservation
        private String bookingconfirmation; // or PNR ref
        private String action; // cancel / amend
        private Boolean sendemail;
        private String companycode; // Airline designator

        // Context
        private Long paxid;
        private String remark;

        // Action Lists
        private List<Flight> flights;
        private List<Ancillary> ancillaries;
        private List<SSR> ssrs;
        private List<Seat> seats;

        public Parms() {
        }

        public Long getBookingid() {
            return bookingid;
        }

        public void setBookingid(Long bookingid) {
            this.bookingid = bookingid;
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

        public Boolean getSendemail() {
            return sendemail;
        }

        public void setSendemail(Boolean sendemail) {
            this.sendemail = sendemail;
        }

        public String getCompanycode() {
            return companycode;
        }

        public void setCompanycode(String companycode) {
            this.companycode = companycode;
        }

        public Long getPaxid() {
            return paxid;
        }

        public void setPaxid(Long paxid) {
            this.paxid = paxid;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public List<Flight> getFlights() {
            return flights;
        }

        public void setFlights(List<Flight> flights) {
            this.flights = flights;
        }

        public List<Ancillary> getAncillaries() {
            return ancillaries;
        }

        public void setAncillaries(List<Ancillary> ancillaries) {
            this.ancillaries = ancillaries;
        }

        public List<SSR> getSsrs() {
            return ssrs;
        }

        public void setSsrs(List<SSR> ssrs) {
            this.ssrs = ssrs;
        }

        public List<Seat> getSeats() {
            return seats;
        }

        public void setSeats(List<Seat> seats) {
            this.seats = seats;
        }
    }

    public static class Flight {
        private String flightnumber;
        private String flightdate;
        private String fromcode;
        private String tocode;
        private String clazz; // "class"
        private Long flightid;
        private Long fareid;
        private String currency;
        private String chargetype;
        private Integer leg;

        public Flight() {
        }

        public String getFlightnumber() {
            return flightnumber;
        }

        public void setFlightnumber(String flightnumber) {
            this.flightnumber = flightnumber;
        }

        public String getFlightdate() {
            return flightdate;
        }

        public void setFlightdate(String flightdate) {
            this.flightdate = flightdate;
        }

        public String getFromcode() {
            return fromcode;
        }

        public void setFromcode(String fromcode) {
            this.fromcode = fromcode;
        }

        public String getTocode() {
            return tocode;
        }

        public void setTocode(String tocode) {
            this.tocode = tocode;
        }

        public String getClazz() {
            return clazz;
        }

        public void setClazz(String clazz) {
            this.clazz = clazz;
        }

        public Long getFlightid() {
            return flightid;
        }

        public void setFlightid(Long flightid) {
            this.flightid = flightid;
        }

        public Long getFareid() {
            return fareid;
        }

        public void setFareid(Long fareid) {
            this.fareid = fareid;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getChargetype() {
            return chargetype;
        }

        public void setChargetype(String chargetype) {
            this.chargetype = chargetype;
        }

        public Integer getLeg() {
            return leg;
        }

        public void setLeg(Integer leg) {
            this.leg = leg;
        }
    }

    public static class Ancillary {
        private Long itemid; // Use itemid as per request
        private Long ancillaryid; // Keep for backward compat if needed, or map itemid here
        private Integer count;
        private Long flightid;
        private Integer paxnum;

        public Ancillary() {
        }

        public Long getItemid() {
            return itemid;
        }

        public void setItemid(Long itemid) {
            this.itemid = itemid;
        }

        public Long getAncillaryid() {
            return ancillaryid;
        }

        public void setAncillaryid(Long ancillaryid) {
            this.ancillaryid = ancillaryid;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public Long getFlightid() {
            return flightid;
        }

        public void setFlightid(Long flightid) {
            this.flightid = flightid;
        }

        public Integer getPaxnum() {
            return paxnum;
        }

        public void setPaxnum(Integer paxnum) {
            this.paxnum = paxnum;
        }
    }

    public static class SSR {
        private String firstname;
        private String lastname;
        private String paxtitle;
        private String code; // from getSSRs
        private Long bookingid; // SSR might have its own bookingid ref
        private List<Long> flightid; // "Array of flight internal codes"
        private Boolean applyToAllFlights;
        private String text; // Optional note

        public SSR() {
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

        public String getPaxtitle() {
            return paxtitle;
        }

        public void setPaxtitle(String paxtitle) {
            this.paxtitle = paxtitle;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public Long getBookingid() {
            return bookingid;
        }

        public void setBookingid(Long bookingid) {
            this.bookingid = bookingid;
        }

        public List<Long> getFlightid() {
            return flightid;
        }

        public void setFlightid(List<Long> flightid) {
            this.flightid = flightid;
        }

        public Boolean getApplyToAllFlights() {
            return applyToAllFlights;
        }

        public void setApplyToAllFlights(Boolean applyToAllFlights) {
            this.applyToAllFlights = applyToAllFlights;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class Seat {
        private Integer paxnum;
        private String seatname;
        private Long flightid; // If needed based on user input

        public Seat() {
        }

        public Integer getPaxnum() {
            return paxnum;
        }

        public void setPaxnum(Integer paxnum) {
            this.paxnum = paxnum;
        }

        public String getSeatname() {
            return seatname;
        }

        public void setSeatname(String seatname) {
            this.seatname = seatname;
        }

        public Long getFlightid() {
            return flightid;
        }

        public void setFlightid(Long flightid) {
            this.flightid = flightid;
        }
    }
}
