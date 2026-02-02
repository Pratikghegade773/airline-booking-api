package com.airlines.GO7API.request;

import com.airlines.GO7API.requestDto.OrderConfirmReqDto;
import com.airlines.GO7API.error.ErrorRsp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmReq {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @JsonIgnore
    private String apiKey;
    @JsonIgnore
    private String orderConfirmUrl;

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

    public String getOrderConfirmUrl() {
        return orderConfirmUrl;
    }

    public void setOrderConfirmUrl(String orderConfirmUrl) {
        this.orderConfirmUrl = orderConfirmUrl;
    }

    // ---------- Nested Classes ----------

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Parms {

        @JsonProperty("bookingid")
        private Long bookingid;

        @JsonProperty("agentconfirmation")
        private String agentconfirmation;

        @JsonProperty("remarks")
        private String remarks;

        @JsonProperty("confirmationemail")
        private String confirmationemail;

        @JsonProperty("holdBooking")
        private Boolean holdBooking;

        @JsonProperty("sendemail")
        private Boolean sendemail;

        @JsonProperty("passenger")
        private List<Passenger> passenger;

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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Passenger {

        @JsonProperty("paxtitle")
        private String paxtitle;

        @JsonProperty("firstname")
        private String firstname;

        @JsonProperty("lastname")
        private String lastname;

        @JsonProperty("paxage")
        private String paxage;

        @JsonProperty("paxnationailty")
        private String paxnationailty;

        @JsonProperty("paxdoctype")
        private String paxdoctype;

        @JsonProperty("paxdocnumber")
        private String paxdocnumber;

        @JsonProperty("paxdocissuer")
        private String paxdocissuer;

        @JsonProperty("paxdocexpiry")
        private String paxdocexpiry;

        @JsonProperty("paxbirthdate")
        private String paxbirthdate;

        @JsonProperty("paxvisanumber")
        private String paxvisanumber;

        @JsonProperty("paxvisaexpiry")
        private String paxvisaexpiry;

        @JsonProperty("paxphone")
        private String paxphone;

        @JsonProperty("paxemail")
        private String paxemail;

        @JsonProperty("paxweight")
        private Integer paxweight;

        @JsonProperty("paxcarringinfant")
        private Boolean paxcarringinfant;

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

    public static OrderConfirmReq mapToOrderConfirmRequestDTO(OrderConfirmReqDto dto) {
        OrderConfirmReq req = new OrderConfirmReq();

        // Manual mapping from DTO to Request
        if (dto.getAerocrs() != null) {
            Aerocrs aerocrsReq = new Aerocrs();

            if (dto.getAerocrs().getParms() != null) {
                OrderConfirmReqDto.Parms dtoParms = dto.getAerocrs().getParms();
                Parms parmsReq = new Parms();

                parmsReq.setBookingid(dtoParms.getBookingid());
                parmsReq.setAgentconfirmation(dtoParms.getAgentconfirmation());
                parmsReq.setRemarks(dtoParms.getRemarks());
                parmsReq.setConfirmationemail(dtoParms.getConfirmationemail());
                parmsReq.setHoldBooking(dtoParms.getHoldBooking());
                parmsReq.setSendemail(dtoParms.getSendemail());

                if (dtoParms.getPassenger() != null) {
                    List<Passenger> paxList = new ArrayList<>();
                    for (OrderConfirmReqDto.Passenger dtoPax : dtoParms.getPassenger()) {
                        Passenger pax = new Passenger();
                        pax.setPaxtitle(dtoPax.getPaxtitle());
                        pax.setFirstname(dtoPax.getFirstname());
                        pax.setLastname(dtoPax.getLastname());
                        pax.setPaxage(dtoPax.getPaxage());
                        pax.setPaxnationailty(dtoPax.getPaxnationailty());
                        pax.setPaxdoctype(dtoPax.getPaxdoctype());
                        pax.setPaxdocnumber(dtoPax.getPaxdocnumber());
                        pax.setPaxdocissuer(dtoPax.getPaxdocissuer());
                        pax.setPaxdocexpiry(dtoPax.getPaxdocexpiry());
                        pax.setPaxbirthdate(dtoPax.getPaxbirthdate());
                        pax.setPaxvisanumber(dtoPax.getPaxvisanumber());
                        pax.setPaxvisaexpiry(dtoPax.getPaxvisaexpiry());
                        pax.setPaxphone(dtoPax.getPaxphone());
                        pax.setPaxemail(dtoPax.getPaxemail());
                        pax.setPaxweight(dtoPax.getPaxweight());
                        pax.setPaxcarringinfant(dtoPax.getPaxcarringinfant());

                        paxList.add(pax);
                    }
                    parmsReq.setPassenger(paxList);
                }
                aerocrsReq.setParms(parmsReq);
            }
            req.setAerocrs(aerocrsReq);
        }

        // Note: apiKey and url are present in OrderConfirmReqDto
        req.setApiKey(dto.getApiKey());
        req.setOrderConfirmUrl(dto.getOrderConfirmUrl());

        return req;
    }

    public Object unmarshal() throws DatatypeConfigurationException, IOException, InterruptedException {
        String response = makeApiCall();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode root = objectMapper.readTree(response);

            if (root.has("errors") || !root.path("success").asBoolean(true)) { // Basic error check, adjust as needed
                // If specific error structure exists, parse it here
                if (root.has("errors")) {
                    ErrorRsp errorRsp = new ErrorRsp();
                    // Populate error logic... assuming generic for now or raw return
                    return root;
                }
            }
            return objectMapper.readValue(response, Object.class);

        } catch (Exception e) {
            System.out.println("Error parsing response: " + e.getMessage());
            return response;
        }
    }

    public String makeApiCall() throws IOException {
        String baseUrl = orderConfirmUrl; // URL from internal field
        String jsonBody = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(this);

        HttpHeaders headers = new HttpHeaders();
        // headers.add("x-api-key", apiKey); // Use if needed
        // Assuming AeroCRS content type or auth headers
        if (apiKey != null) {
            headers.add("x-api-key", apiKey);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        System.out.println("Generated Request is:\n" + jsonBody);

        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        try {
            // Using POST as standard for order creation/confirmation
            ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.POST, entity, String.class);
            System.out.println("HTTP Response Status Code: " + response.getStatusCode());
            String rawJson = response.getBody();

            try {
                Object json = mapper.readValue(rawJson, Object.class);
                String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                System.out.println("Pretty Response:\n" + prettyJson);
            } catch (Exception ex) {
                System.out.println("Failed to pretty print JSON: " + ex.getMessage());
            }

            return rawJson;

        } catch (HttpClientErrorException e) {
            System.out.println("HTTP Error Response: " + e.getResponseBodyAsString());
            return e.getResponseBodyAsString();
        }
    }
}
