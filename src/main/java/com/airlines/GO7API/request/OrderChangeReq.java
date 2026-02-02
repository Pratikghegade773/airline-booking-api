package com.airlines.GO7API.request;

import com.airlines.GO7API.requestDto.OrderChangeReqDto;
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
public class OrderChangeReq {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @JsonIgnore
    private String apiKey;
    @JsonIgnore
    private String orderChangeUrl;

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

    public String getOrderChangeUrl() {
        return orderChangeUrl;
    }

    public void setOrderChangeUrl(String orderChangeUrl) {
        this.orderChangeUrl = orderChangeUrl;
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
        private Long paxid;
        private String remark;

        // New root params
        private String bookingconfirmation;
        private String action;
        private Boolean sendemail;
        private String companycode;

        private List<Flight> flights;
        private List<Ancillary> ancillaries;
        private List<SSR> ssrs;
        private List<Seat> seats;

        public Long getBookingid() {
            return bookingid;
        }

        public void setBookingid(Long bookingid) {
            this.bookingid = bookingid;
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Flight {
        private String flightnumber;
        private String flightdate;
        private String fromcode;
        private String tocode;
        @JsonProperty("class")
        private String clazz;

        private Long flightid;
        private Long fareid;
        private String currency;
        private String chargetype;
        private Integer leg;

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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Ancillary {
        private Long itemid;
        private Integer count;
        private Long flightid;
        private Integer paxnum;

        public Long getItemid() {
            return itemid;
        }

        public void setItemid(Long itemid) {
            this.itemid = itemid;
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SSR {
        private String firstname;
        private String lastname;
        private String paxtitle;
        private String code;
        private Long bookingid;
        private List<Long> flightid;
        private Boolean applyToAllFlights;
        private String text;

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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Seat {
        private Integer paxnum;
        private String seatname;
        private Long flightid;

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

    // ---------- Mapping Logic ----------

    public static OrderChangeReq mapToOrderChangeReq(OrderChangeReqDto dto) {
        OrderChangeReq req = new OrderChangeReq();
        req.setApiKey(dto.getApiKey());
        req.setOrderChangeUrl(dto.getOrderChangeUrl());

        if (dto.getAerocrs() != null && dto.getAerocrs().getParms() != null) {
            OrderChangeReqDto.Parms dtoParms = dto.getAerocrs().getParms();
            Aerocrs aerocrs = new Aerocrs();
            Parms parms = new Parms();

            parms.setBookingid(dtoParms.getBookingid());
            parms.setPaxid(dtoParms.getPaxid());
            parms.setRemark(dtoParms.getRemark());

            parms.setBookingconfirmation(dtoParms.getBookingconfirmation());
            parms.setAction(dtoParms.getAction());
            parms.setSendemail(dtoParms.getSendemail());
            parms.setCompanycode(dtoParms.getCompanycode());

            // Map Flights
            if (dtoParms.getFlights() != null) {
                List<Flight> list = new ArrayList<>();
                for (OrderChangeReqDto.Flight i : dtoParms.getFlights()) {
                    Flight o = new Flight();
                    o.setFlightnumber(i.getFlightnumber());
                    o.setFlightdate(i.getFlightdate());
                    o.setFromcode(i.getFromcode());
                    o.setTocode(i.getTocode());
                    o.setClazz(i.getClazz());
                    o.setFlightid(i.getFlightid());
                    o.setFareid(i.getFareid());
                    o.setCurrency(i.getCurrency());
                    o.setChargetype(i.getChargetype());
                    o.setLeg(i.getLeg());
                    list.add(o);
                }
                parms.setFlights(list);
            }

            // Map Ancillaries
            if (dtoParms.getAncillaries() != null) {
                List<Ancillary> list = new ArrayList<>();
                for (OrderChangeReqDto.Ancillary i : dtoParms.getAncillaries()) {
                    Ancillary o = new Ancillary();
                    o.setItemid(i.getItemid());
                    o.setCount(i.getCount());
                    o.setFlightid(i.getFlightid());
                    o.setPaxnum(i.getPaxnum());
                    list.add(o);
                }
                parms.setAncillaries(list);
            }

            // Map SSRs
            if (dtoParms.getSsrs() != null) {
                List<SSR> list = new ArrayList<>();
                for (OrderChangeReqDto.SSR i : dtoParms.getSsrs()) {
                    SSR o = new SSR();
                    o.setFirstname(i.getFirstname());
                    o.setLastname(i.getLastname());
                    o.setPaxtitle(i.getPaxtitle());
                    o.setCode(i.getCode());
                    o.setBookingid(i.getBookingid());
                    o.setFlightid(i.getFlightid());
                    o.setApplyToAllFlights(i.getApplyToAllFlights());
                    o.setText(i.getText());
                    list.add(o);
                }
                parms.setSsrs(list);
            }

            // Map Seats
            if (dtoParms.getSeats() != null) {
                List<Seat> list = new ArrayList<>();
                for (OrderChangeReqDto.Seat i : dtoParms.getSeats()) {
                    Seat o = new Seat();
                    o.setPaxnum(i.getPaxnum());
                    o.setSeatname(i.getSeatname());
                    o.setFlightid(i.getFlightid());
                    list.add(o);
                }
                parms.setSeats(list);
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
        String baseUrl = orderChangeUrl;

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
