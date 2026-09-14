package com.airlines.go7api.request;

import com.airlines.go7api.requestdto.ServiceListReqDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceListReq extends BaseGo7Req {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @JsonIgnore
    private String apiKey;

    @JsonIgnore
    private String serviceListUrl;

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

    public String getServiceListUrl() {
        return serviceListUrl;
    }

    public void setServiceListUrl(String serviceListUrl) {
        this.serviceListUrl = serviceListUrl;
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

        @JsonProperty("flightid")
        private Long flightId;

        @JsonProperty("currency")
        private String currency;

        public Long getBookingId() {
            return bookingId;
        }

        public void setBookingId(Long bookingId) {
            this.bookingId = bookingId;
        }

        public Long getFlightId() {
            return flightId;
        }

        public void setFlightId(Long flightId) {
            this.flightId = flightId;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }

    private static final String DEFAULT_SERVICE_LIST_URL = "https://api.aerocrs.com/v5/getAncillaries";

    private static String determineServiceListUrl(ServiceListReqDto dto) {
        if (dto.getServiceListUrl() != null && !dto.getServiceListUrl().isEmpty()) {
            return dto.getServiceListUrl();
        }
        return DEFAULT_SERVICE_LIST_URL;
    }

    private static void populateParmsFromBooking(Parms parms, com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp) {
        if (bookingRsp == null || bookingRsp.getAerocrs() == null || bookingRsp.getAerocrs().getBooking() == null) {
            return;
        }
        com.airlines.go7api.responsego7.common.Booking booking = bookingRsp.getAerocrs().getBooking();

        // 1. Booking ID
        if (booking.getBookingid() != null) {
            parms.setBookingId(booking.getBookingid());
        }

        // 2. Currency
        populateCurrency(parms, booking);

        // 3. Flight ID (Take first flight)
        populateFlightId(parms, booking);
    }

    private static void populateCurrency(Parms parms, com.airlines.go7api.responsego7.common.Booking booking) {
        if (booking.getCurrency() != null) {
            parms.setCurrency(booking.getCurrency());
        } else if (booking.getDefaultCurrency() != null) {
            parms.setCurrency(booking.getDefaultCurrency());
        } else {
            parms.setCurrency("USD");
        }
    }

    private static void populateFlightId(Parms parms, com.airlines.go7api.responsego7.common.Booking booking) {
        if (booking.getFlights() != null && booking.getFlights().getFlight() != null
                && !booking.getFlights().getFlight().isEmpty()) {
            com.airlines.go7api.responsego7.common.Flight firstFlight = booking.getFlights().getFlight().get(0);
            parms.setFlightId((long) firstFlight.getFlightid());
        } else if (booking.getItems() != null && booking.getItems().getFlight() != null
                && !booking.getItems().getFlight().isEmpty()) {
            com.airlines.go7api.responsego7.common.Flight firstFlight = booking.getItems().getFlight().get(0);
            parms.setFlightId((long) firstFlight.getFlightid());
        }
    }

    public static ServiceListReq mapToServiceListRequestDTO(ServiceListReqDto dto,
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp) {
        ServiceListReq req = new ServiceListReq();
        req.setApiKey(dto.getApiKey());
        req.setServiceListUrl(determineServiceListUrl(dto));

        Aerocrs aerocrs = new Aerocrs();
        Parms parms = new Parms();

        populateParmsFromBooking(parms, bookingRsp);

        aerocrs.setParms(parms);
        req.setAerocrs(aerocrs);
        return req;
    }

    public static com.airlines.go7api.request.OrderRetrieveReq mapToGetBookingReq(String bookingConfirmation) {
        com.airlines.go7api.request.OrderRetrieveReq req = new com.airlines.go7api.request.OrderRetrieveReq();
        req.setApiKey("8d123dcd262ad942852233f81e649089"); // Default or constant if not passed
        req.setOrderRetrieveUrl("https://api.aerocrs.com/v5/getBooking");

        com.airlines.go7api.request.OrderRetrieveReq.Aerocrs aerocrs = new com.airlines.go7api.request.OrderRetrieveReq.Aerocrs();
        com.airlines.go7api.request.OrderRetrieveReq.Parms parms = new com.airlines.go7api.request.OrderRetrieveReq.Parms();
        parms.setBookingConfirmation(bookingConfirmation);

        aerocrs.setParms(parms);
        req.setAerocrs(aerocrs);
        return req;
    }

    @Override
    protected String getApiUrl() {
        return serviceListUrl != null ? serviceListUrl : "";
    }

    @Override
    protected String getRequestName() {
        return "ServiceList";
    }
}
