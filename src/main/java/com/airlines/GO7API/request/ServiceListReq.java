package com.airlines.GO7API.request;

import com.airlines.GO7API.requestDto.ServiceListReqDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Data;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceListReq {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    public Aerocrs getAerocrs() {
        return aerocrs;
    }

    public void setAerocrs(Aerocrs aerocrs) {
        this.aerocrs = aerocrs;
    }

    @JsonIgnore
    public String orderId;
    @JsonIgnore
    public String apiKey;
    @JsonIgnore
    public String serviceListUrl;

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setServiceListUrl(String serviceListUrl) {
        this.serviceListUrl = serviceListUrl;
    }

    public String getServiceListUrl() {
        return serviceListUrl;
    }

    // Internal classes to match JSON structure for serialization
    @Data
    public static class Aerocrs {
        private Parms parms;

        public Parms getParms() {
            return parms;
        }

        public void setParms(Parms parms) {
            this.parms = parms;
        }
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Parms {
        private String currency;
        private Long bookingid;
        private Long flightid;
        private String companycode;
        private List<Flight> flights;
        // SSRs might use 'flights' key but with different structure.
        // If strict JSON is required, we might need a custom serializer or just use
        // Object for 'flights'
        // For now, mapping straightforward properties.

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

        public List<Flight> getFlights() {
            return flights;
        }

        public void setFlights(List<Flight> flights) {
            this.flights = flights;
        }
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Flight {
        private String flightnumber;
        private String flightdate;
        private String fromcode;
        private String tocode;
        @JsonProperty("class")
        private String clazz;

        // For SSRs which might have flightid array
        private List<Long> flightid;

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

        public List<Long> getFlightid() {
            return flightid;
        }

        public void setFlightid(List<Long> flightid) {
            this.flightid = flightid;
        }
    }

    public static ServiceListReq mapToServiceListRequestDTO(ServiceListReqDto dto) {
        ServiceListReq req = new ServiceListReq();
        req.setOrderId(dto.getOrderId());
        req.setApiKey(dto.getApiKey());
        req.setServiceListUrl(dto.getServiceListUrl());

        if (dto.getAerocrs() != null && dto.getAerocrs().getParms() != null) {
            ServiceListReqDto.Parms dtoParms = dto.getAerocrs().getParms();
            Aerocrs aerocrs = new Aerocrs();
            Parms parms = new Parms();

            parms.setCurrency(dtoParms.getCurrency());
            parms.setBookingid(dtoParms.getBookingid());
            parms.setFlightid(dtoParms.getFlightid());
            parms.setCompanycode(dtoParms.getCompanycode());

            // Map Airline Ancillaries Flights (Object List)
            if (dtoParms.getFlights() != null) {
                List<Flight> flightList = new ArrayList<>();
                for (ServiceListReqDto.Flight f : dtoParms.getFlights()) {
                    Flight internalFlight = new Flight();
                    internalFlight.setFlightnumber(f.getFlightnumber());
                    internalFlight.setFlightdate(f.getFlightdate());
                    internalFlight.setFromcode(f.getFromcode());
                    internalFlight.setTocode(f.getTocode());
                    internalFlight.setClazz(f.getClazz());
                    flightList.add(internalFlight);
                }
                parms.setFlights(flightList);
            }

            // Map SSR Flights (ID List) - Mapping to same 'flights' list or separate logic?
            // Map SSR Flights (ID List) - Mapping to same 'flights' list or separate logic?
            // User requirement had 'flights' key for both.
            // If we have SSR flights, we map them too.
            if (dtoParms.getFlightsSSR() != null) {
                List<Flight> flightList = new ArrayList<>();
                // If existing list exists, append? Typically these are exclusive calls.
                if (parms.getFlights() != null)
                    flightList.addAll(parms.getFlights());

                for (ServiceListReqDto.Flight f : dtoParms.getFlightsSSR()) {
                    Flight internalFlight = new Flight();
                    internalFlight.setFlightid(f.getFlightid());
                    flightList.add(internalFlight);
                }
                parms.setFlights(flightList);
            }

            aerocrs.setParms(parms);

            req.setAerocrs(aerocrs);
        }

        return req;
    }

    public Object unmarshal() throws DatatypeConfigurationException, IOException, InterruptedException {
        String response = makeApiCall();
        ObjectMapper objectMapper = new ObjectMapper();
        // Return raw JSON or specific RS object if defined. Returning generic node for
        // now.
        return objectMapper.readTree(response);
    }

    public String makeApiCall() throws IOException {
        String baseUrl = serviceListUrl;
        // If orderId is part of URL in some cases, append it.
        // But user examples show generic getAncillaries with bookingid in body.
        // We will stick to the provided URL.

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
