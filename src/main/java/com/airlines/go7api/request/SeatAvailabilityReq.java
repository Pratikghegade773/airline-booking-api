package com.airlines.go7api.request;

import com.airlines.go7api.requestdto.SeatAvailabilityReqDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeatAvailabilityReq extends BaseGo7Req {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @JsonIgnore
    private String apiKey;

    @JsonIgnore
    private String seatAvailabilityUrl;

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

    public String getSeatAvailabilityUrl() {
        return seatAvailabilityUrl;
    }

    public void setSeatAvailabilityUrl(String seatAvailabilityUrl) {
        this.seatAvailabilityUrl = seatAvailabilityUrl;
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

        @JsonProperty("companycode")
        private String companyCode;

        @JsonProperty("flightnumber")
        private String flightNumber;

        @JsonProperty("flightdate")
        private String flightDate;

        @JsonProperty("fromcode")
        private String fromCode;

        @JsonProperty("tocode")
        private String toCode;

        public Long getBookingId() {
            return bookingId;
        }

        public void setBookingId(Long bookingId) {
            this.bookingId = bookingId;
        }

        public String getCompanyCode() {
            return companyCode;
        }

        public void setCompanyCode(String companyCode) {
            this.companyCode = companyCode;
        }

        public String getFlightNumber() {
            return flightNumber;
        }

        public void setFlightNumber(String flightNumber) {
            this.flightNumber = flightNumber;
        }

        public String getFlightDate() {
            return flightDate;
        }

        public void setFlightDate(String flightDate) {
            this.flightDate = flightDate;
        }

        public String getFromCode() {
            return fromCode;
        }

        public void setFromCode(String fromCode) {
            this.fromCode = fromCode;
        }

        public String getToCode() {
            return toCode;
        }

        public void setToCode(String toCode) {
            this.toCode = toCode;
        }
    }

    private static final String DEFAULT_SEAT_AVAILABILITY_URL = "https://api.aerocrs.com/v5/getSeatMapFare";

    private static String determineSeatAvailabilityUrl(SeatAvailabilityReqDto dto) {
        if (dto.getSeatAvailabilityUrl() != null && !dto.getSeatAvailabilityUrl().isEmpty()) {
            return dto.getSeatAvailabilityUrl();
        }
        return DEFAULT_SEAT_AVAILABILITY_URL;
    }

    private static void populateBookingId(Parms parms, String orderId) {
        if (orderId != null && !orderId.isEmpty()) {
            try {
                parms.setBookingId(Long.parseLong(orderId));
            } catch (NumberFormatException e) {
                // Ignore invalid ID
            }
        }
    }

    private static void populateFlightParams(Parms parms, com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp) {
        if (bookingRsp == null || bookingRsp.getAerocrs() == null || bookingRsp.getAerocrs().getBooking() == null) {
            return;
        }
        com.airlines.go7api.responsego7.common.Booking booking = bookingRsp.getAerocrs().getBooking();
        com.airlines.go7api.responsego7.common.Flight firstFlight = extractFirstFlight(booking);
        if (firstFlight != null) {
            parms.setFlightNumber(firstFlight.getNumber());
            parms.setFlightDate(firstFlight.getFlightdate());
            parms.setFromCode(firstFlight.getFromcode());
            parms.setToCode(firstFlight.getTocode());
        }
    }

    private static com.airlines.go7api.responsego7.common.Flight extractFirstFlight(com.airlines.go7api.responsego7.common.Booking booking) {
        if (booking.getFlights() != null && booking.getFlights().getFlight() != null
                && !booking.getFlights().getFlight().isEmpty()) {
            return booking.getFlights().getFlight().get(0);
        }
        if (booking.getItems() != null && booking.getItems().getFlight() != null
                && !booking.getItems().getFlight().isEmpty()) {
            return booking.getItems().getFlight().get(0);
        }
        return null;
    }

    public static SeatAvailabilityReq mapToSeatAvailabilityRequestDTO(SeatAvailabilityReqDto dto,
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp) {
        SeatAvailabilityReq req = new SeatAvailabilityReq();
        req.setApiKey(dto.getApiKey());
        req.setSeatAvailabilityUrl(determineSeatAvailabilityUrl(dto));

        Aerocrs aerocrs = new Aerocrs();
        Parms parms = new Parms();

        populateBookingId(parms, dto.getOrderId());
        parms.setCompanyCode("API");
        populateFlightParams(parms, bookingRsp);

        aerocrs.setParms(parms);
        req.setAerocrs(aerocrs);
        return req;
    }

    public static com.airlines.go7api.request.OrderRetrieveReq mapToGetBookingReq(String bookingConfirmation) {
        com.airlines.go7api.request.OrderRetrieveReq req = new com.airlines.go7api.request.OrderRetrieveReq();
        req.setApiKey("8d123dcd262ad942852233f81e649089");
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
        return seatAvailabilityUrl != null ? seatAvailabilityUrl : "";
    }

    @Override
    protected String getRequestName() {
        return "SeatAvailability";
    }
}
