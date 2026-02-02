package com.airlines.GO7API.request;

import com.airlines.GO7API.requestDto.OrderTicketReqDto;
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

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTicketReq {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @JsonIgnore
    private String apiKey;
    @JsonIgnore
    private String orderTicketUrl;

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

    public String getOrderTicketUrl() {
        return orderTicketUrl;
    }

    public void setOrderTicketUrl(String orderTicketUrl) {
        this.orderTicketUrl = orderTicketUrl;
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
        // -------- Ticket / Booking --------
        @JsonProperty("bookingid")
        private Long bookingid; // mandatory for all
        @JsonProperty("currency")
        private String currency; // optional

        // -------- Create Payment (PSP/Manual) --------
        @JsonProperty("bookingconfirmation")
        private String bookingconfirmation;
        @JsonProperty("paymentmethodtoken")
        private String paymentmethodtoken;
        @JsonProperty("amountpaid")
        private Double amountpaid;
        @JsonProperty("amountcurrency")
        private String amountcurrency;
        @JsonProperty("paymentref")
        private String paymentref;

        // -------- Execute Payment (Credit Card) --------
        @JsonProperty("creditcardpayer")
        private String creditcardpayer;
        @JsonProperty("creditcardnumber")
        private String creditcardnumber;
        @JsonProperty("creditcardexpiry")
        private String creditcardexpiry; // MMYY
        @JsonProperty("creditcardcvv")
        private String creditcardcvv;

        @JsonProperty("transaction3ds")
        private Boolean transaction3ds;
        @JsonProperty("params3ds")
        private Params3ds params3ds;

        @JsonProperty("ismobilepayment")
        private Boolean ismobilepayment;
        @JsonProperty("mobileaction")
        private String mobileaction;
        @JsonProperty("mobile")
        private Mobile mobile;

        public Long getBookingid() {
            return bookingid;
        }

        public void setBookingid(Long bookingid) {
            this.bookingid = bookingid;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getBookingconfirmation() {
            return bookingconfirmation;
        }

        public void setBookingconfirmation(String bookingconfirmation) {
            this.bookingconfirmation = bookingconfirmation;
        }

        public String getPaymentmethodtoken() {
            return paymentmethodtoken;
        }

        public void setPaymentmethodtoken(String paymentmethodtoken) {
            this.paymentmethodtoken = paymentmethodtoken;
        }

        public Double getAmountpaid() {
            return amountpaid;
        }

        public void setAmountpaid(Double amountpaid) {
            this.amountpaid = amountpaid;
        }

        public String getAmountcurrency() {
            return amountcurrency;
        }

        public void setAmountcurrency(String amountcurrency) {
            this.amountcurrency = amountcurrency;
        }

        public String getPaymentref() {
            return paymentref;
        }

        public void setPaymentref(String paymentref) {
            this.paymentref = paymentref;
        }

        public String getCreditcardpayer() {
            return creditcardpayer;
        }

        public void setCreditcardpayer(String creditcardpayer) {
            this.creditcardpayer = creditcardpayer;
        }

        public String getCreditcardnumber() {
            return creditcardnumber;
        }

        public void setCreditcardnumber(String creditcardnumber) {
            this.creditcardnumber = creditcardnumber;
        }

        public String getCreditcardexpiry() {
            return creditcardexpiry;
        }

        public void setCreditcardexpiry(String creditcardexpiry) {
            this.creditcardexpiry = creditcardexpiry;
        }

        public String getCreditcardcvv() {
            return creditcardcvv;
        }

        public void setCreditcardcvv(String creditcardcvv) {
            this.creditcardcvv = creditcardcvv;
        }

        public Boolean getTransaction3ds() {
            return transaction3ds;
        }

        public void setTransaction3ds(Boolean transaction3ds) {
            this.transaction3ds = transaction3ds;
        }

        public Params3ds getParams3ds() {
            return params3ds;
        }

        public void setParams3ds(Params3ds params3ds) {
            this.params3ds = params3ds;
        }

        public Boolean getIsmobilepayment() {
            return ismobilepayment;
        }

        public void setIsmobilepayment(Boolean ismobilepayment) {
            this.ismobilepayment = ismobilepayment;
        }

        public String getMobileaction() {
            return mobileaction;
        }

        public void setMobileaction(String mobileaction) {
            this.mobileaction = mobileaction;
        }

        public Mobile getMobile() {
            return mobile;
        }

        public void setMobile(Mobile mobile) {
            this.mobile = mobile;
        }
    }

    // ---------- Nested Classes for Payment ----------

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Params3ds {
        @JsonProperty("eci")
        private String eci;
        @JsonProperty("xid")
        private String xid;
        @JsonProperty("cavv")
        private String cavv;
        @JsonProperty("enrolled")
        private String enrolled;
        @JsonProperty("status_3d")
        private String status_3d;
        @JsonProperty("version_3d")
        private String version_3d;

        public String getEci() {
            return eci;
        }

        public void setEci(String eci) {
            this.eci = eci;
        }

        public String getXid() {
            return xid;
        }

        public void setXid(String xid) {
            this.xid = xid;
        }

        public String getCavv() {
            return cavv;
        }

        public void setCavv(String cavv) {
            this.cavv = cavv;
        }

        public String getEnrolled() {
            return enrolled;
        }

        public void setEnrolled(String enrolled) {
            this.enrolled = enrolled;
        }

        public String getStatus_3d() {
            return status_3d;
        }

        public void setStatus_3d(String status_3d) {
            this.status_3d = status_3d;
        }

        public String getVersion_3d() {
            return version_3d;
        }

        public void setVersion_3d(String version_3d) {
            this.version_3d = version_3d;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Mobile {
        @JsonProperty("mobiletype")
        private String mobiletype;
        @JsonProperty("mobilecountry")
        private String mobilecountry;
        @JsonProperty("phonenumber")
        private String phonenumber;

        public String getMobiletype() {
            return mobiletype;
        }

        public void setMobiletype(String mobiletype) {
            this.mobiletype = mobiletype;
        }

        public String getMobilecountry() {
            return mobilecountry;
        }

        public void setMobilecountry(String mobilecountry) {
            this.mobilecountry = mobilecountry;
        }

        public String getPhonenumber() {
            return phonenumber;
        }

        public void setPhonenumber(String phonenumber) {
            this.phonenumber = phonenumber;
        }
    }

    public static OrderTicketReq mapToOrderTicketRequestDTO(OrderTicketReqDto dto) {
        OrderTicketReq req = new OrderTicketReq();

        if (dto.getAerocrs() != null) {
            Aerocrs aerocrsReq = new Aerocrs();

            if (dto.getAerocrs().getParms() != null) {
                OrderTicketReqDto.Parms dtoParms = dto.getAerocrs().getParms();
                Parms parmsReq = new Parms();

                // Standard Ticket Params
                parmsReq.setBookingid(dtoParms.getBookingid());
                parmsReq.setCurrency(dtoParms.getCurrency());

                // Create Payment Params
                parmsReq.setBookingconfirmation(dtoParms.getBookingconfirmation());
                parmsReq.setPaymentmethodtoken(dtoParms.getPaymentmethodtoken());
                parmsReq.setAmountpaid(dtoParms.getAmountpaid());
                parmsReq.setAmountcurrency(dtoParms.getAmountcurrency());
                parmsReq.setPaymentref(dtoParms.getPaymentref());

                // Execute Payment Params
                parmsReq.setCreditcardpayer(dtoParms.getCreditcardpayer());
                parmsReq.setCreditcardnumber(dtoParms.getCreditcardnumber());
                parmsReq.setCreditcardexpiry(dtoParms.getCreditcardexpiry());
                parmsReq.setCreditcardcvv(dtoParms.getCreditcardcvv());
                parmsReq.setTransaction3ds(dtoParms.getTransaction3ds());

                if (dtoParms.getParams3ds() != null) {
                    Params3ds p3ds = new Params3ds();
                    p3ds.setEci(dtoParms.getParams3ds().getEci());
                    p3ds.setXid(dtoParms.getParams3ds().getXid());
                    p3ds.setCavv(dtoParms.getParams3ds().getCavv());
                    p3ds.setEnrolled(dtoParms.getParams3ds().getEnrolled());
                    p3ds.setStatus_3d(dtoParms.getParams3ds().getStatus_3d());
                    p3ds.setVersion_3d(dtoParms.getParams3ds().getVersion_3d());
                    parmsReq.setParams3ds(p3ds);
                }

                parmsReq.setIsmobilepayment(dtoParms.getIsmobilepayment());
                parmsReq.setMobileaction(dtoParms.getMobileaction());

                if (dtoParms.getMobile() != null) {
                    Mobile mob = new Mobile();
                    mob.setMobiletype(dtoParms.getMobile().getMobiletype());
                    mob.setMobilecountry(dtoParms.getMobile().getMobilecountry());
                    mob.setPhonenumber(dtoParms.getMobile().getPhonenumber());
                    parmsReq.setMobile(mob);
                }

                aerocrsReq.setParms(parmsReq);
            }
            req.setAerocrs(aerocrsReq);
        }

        req.setApiKey(dto.getApiKey());
        req.setOrderTicketUrl(dto.getOrderTicketUrl());

        return req;
    }

    public Object unmarshal() throws DatatypeConfigurationException, IOException, InterruptedException {
        String response = makeApiCall();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode root = objectMapper.readTree(response);

            // Basic error handling assuming standard "success" or "errors" fields
            // similar to previous pattern
            if (root.has("errors")) {
                ErrorRsp errorRsp = new ErrorRsp();
                // Populate error logic if needed
                return root;
            }

            return objectMapper.readValue(response, Object.class);

        } catch (Exception e) {
            System.out.println("Error parsing response: " + e.getMessage());
            return response;
        }
    }

    public String makeApiCall() throws IOException {
        String baseUrl = orderTicketUrl;
        String jsonBody = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(this);

        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null) {
            headers.add("x-api-key", apiKey);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        System.out.println("Generated Request is:\n" + jsonBody);

        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        try {
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
