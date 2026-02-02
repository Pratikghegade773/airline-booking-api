package com.airlines.GO7API.requestDto;

// Standard headers/auth
public class ServiceListReqDto {

    private String apiKey;
    private String serviceListUrl;

    // Fields from kyte-api reference
    private String orderId;
    private String agencyId;
    private String agentId;

    private Aerocrs aerocrs;

    public ServiceListReqDto() {
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getServiceListUrl() {
        return serviceListUrl;
    }

    public void setServiceListUrl(String serviceListUrl) {
        this.serviceListUrl = serviceListUrl;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
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
        private String currency;

        // getAncillaries
        private Long bookingid;
        private Long flightid;

        // getAirlineAncillaries
        private String companycode;
        private java.util.List<Flight> flights;

        // getSSRs
        private java.util.List<Flight> flightsSSR; // Reuse unified Flight class

        public Parms() {
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
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

        public String getCompanycode() {
            return companycode;
        }

        public void setCompanycode(String companycode) {
            this.companycode = companycode;
        }

        public java.util.List<Flight> getFlights() {
            return flights;
        }

        public void setFlights(java.util.List<Flight> flights) {
            this.flights = flights;
        }

        public java.util.List<Flight> getFlightsSSR() {
            return flightsSSR;
        }

        public void setFlightsSSR(java.util.List<Flight> flightsSSR) {
            this.flightsSSR = flightsSSR;
        }
    }

    // Unified Flight class for both Ancillaries and SSRs
    public static class Flight {
        // Airline Ancillaries
        private String flightnumber;
        private String flightdate; // YYYY/MM/DD
        private String fromcode;
        private String tocode;
        private String clazz;

        // SSRs
        private java.util.List<Long> flightid;

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

        public java.util.List<Long> getFlightid() {
            return flightid;
        }

        public void setFlightid(java.util.List<Long> flightid) {
            this.flightid = flightid;
        }
    }
}
