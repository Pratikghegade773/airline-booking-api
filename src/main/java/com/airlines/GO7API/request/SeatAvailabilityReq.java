package com.airlines.GO7API.request;

import com.airlines.GO7API.requestDto.SeatAvailabilityReqDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeatAvailabilityReq {

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

    // ---------- Nested Classes ----------

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Aerocrs {
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
        private Long bookingid;
        private Long flightid;
        private String currency;

        // Search Params
        private String companycode;
        private String flightnumber;
        private String flightdate;
        private String fromcode;
        private String tocode;

        private List<Seat> seats;

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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Seat {
        private String seatnumber;
        private String row;
        private String letter;

        private Integer paxnum;
        private String seatname;

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

    // ---------- Mapping Logic ----------

    public static SeatAvailabilityReq mapToSeatAvailabilityRequestDTO(SeatAvailabilityReqDto dto) {
        SeatAvailabilityReq req = new SeatAvailabilityReq();
        req.setApiKey(dto.getApiKey());
        req.setSeatAvailabilityUrl(dto.getSeatAvailabilityUrl());

        if (dto.getAerocrs() != null && dto.getAerocrs().getParms() != null) {
            SeatAvailabilityReqDto.Parms dtoParms = dto.getAerocrs().getParms();
            Aerocrs aerocrs = new Aerocrs();
            Parms parms = new Parms();

            parms.setBookingid(dtoParms.getBookingid());
            parms.setFlightid(dtoParms.getFlightid());
            parms.setCurrency(dtoParms.getCurrency());

            // Map Search Params
            parms.setCompanycode(dtoParms.getCompanycode());
            parms.setFlightnumber(dtoParms.getFlightnumber());
            parms.setFlightdate(dtoParms.getFlightdate());
            parms.setFromcode(dtoParms.getFromcode());
            parms.setTocode(dtoParms.getTocode());

            if (dtoParms.getSeats() != null) {
                List<Seat> seatList = new ArrayList<>();
                for (SeatAvailabilityReqDto.Seat s : dtoParms.getSeats()) {
                    Seat internalSeat = new Seat();
                    internalSeat.setSeatnumber(s.getSeatnumber());
                    internalSeat.setRow(s.getRow());
                    internalSeat.setLetter(s.getLetter());

                    // Map Reserve Params
                    internalSeat.setPaxnum(s.getPaxnum());
                    internalSeat.setSeatname(s.getSeatname());

                    seatList.add(internalSeat);
                }
                parms.setSeats(seatList);
            }

            aerocrs.setParms(parms);

            req.setAerocrs(aerocrs);
        }

        return req;
    }

    public Object unmarshal() throws DatatypeConfigurationException, IOException, InterruptedException {
        String response = makeApiCall();
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(response);
    }

    public String makeApiCall() throws IOException {
        String baseUrl = seatAvailabilityUrl;

        String jsonBody = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(this);

        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null)
            headers.add("x-api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        System.out.println("Generated Request is:\n" + jsonBody);

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.POST, entity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            return e.getResponseBodyAsString();
        }
    }
}
