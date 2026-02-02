package com.airlines.GO7API.requestDto;

import java.util.List;

public class SeatAvailabilityReqDto {

    private String apiKey;
    private String seatAvailabilityUrl;
    private Aerocrs aerocrs;

    public SeatAvailabilityReqDto() {
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSeatAvailabilityUrl() {
        return seatAvailabilityUrl;
    }

    public void setSeatAvailabilityUrl(String seatAvailabilityUrl) {
        this.seatAvailabilityUrl = seatAvailabilityUrl;
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
        // Shared
        private Long bookingid;
        private Long flightid;
        private String currency; // Optional filter

        // getSeatMapFare specific
        private String companycode;
        private String flightnumber;
        private String flightdate; // YYYY/MM/DD
        private String fromcode;
        private String tocode;

        // For reserveSeats
        private List<Seat> seats;

        public Parms() {
        }

        public Long getBookingid() {
            return bookingid;
        }

        public void setBookingid(Long bookingid) {
            this.bookingid = bookingid;
        }

        public Long getFlightid() {
            return flightid;
        }

        public void setFlightid(Long flightid) {
            this.flightid = flightid;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getCompanycode() {
            return companycode;
        }

        public void setCompanycode(String companycode) {
            this.companycode = companycode;
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

        public List<Seat> getSeats() {
            return seats;
        }

        public void setSeats(List<Seat> seats) {
            this.seats = seats;
        }
    }

    public static class Seat {
        private String seatnumber; // "1A"
        private String row; // "1"
        private String letter; // "A"

        // reserveSeats specific
        private Integer paxnum;
        private String seatname; // e.g. "1A"

        public Seat() {
        }

        public String getSeatnumber() {
            return seatnumber;
        }

        public void setSeatnumber(String seatnumber) {
            this.seatnumber = seatnumber;
        }

        public String getRow() {
            return row;
        }

        public void setRow(String row) {
            this.row = row;
        }

        public String getLetter() {
            return letter;
        }

        public void setLetter(String letter) {
            this.letter = letter;
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
    }
}
