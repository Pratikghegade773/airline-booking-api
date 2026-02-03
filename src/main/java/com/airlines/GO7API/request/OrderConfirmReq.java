package com.airlines.GO7API.request;

import com.airlines.GO7API.error.ErrorRsp;
import com.airlines.GO7API.requestDto.OrderCreateReqDto;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderConfirmReq {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @JsonIgnore
    private String orderConfirmUrl = "https://api.aerocrs.com/v5/confirmBooking";

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Aerocrs {
        @JsonProperty("parms")
        private Parms parms;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Parms {
        private Long bookingid;
        private String agentconfirmation;
        private String confirmationemail;
        private List<Passenger> passenger;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Passenger {
        private String paxtitle;
        private String firstname;
        private String lastname;
        private Integer paxage;
        private String paxnationailty;
        private String paxdoctype;
        private String paxdocnumber;
        private String paxdocissuer;
        private String paxdocexpiry;
        private String paxbirthdate;
        private String paxphone;
        private String paxemail;
    }

    public static OrderConfirmReq mapToOrderConfirmReq(OrderCreateReqDto requestDto, Long bookingId) {
        OrderConfirmReq request = new OrderConfirmReq();
        Aerocrs aerocrs = new Aerocrs();
        Parms parms = new Parms();

        // 1. Booking ID
        if (bookingId != null) {
            parms.setBookingid(bookingId);
        } else {
            // Fallback: try parsing responseId if it happens to be numeric
            if (requestDto.getResponseId() != null) {
                try {
                    parms.setBookingid(Long.parseLong(requestDto.getResponseId()));
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }

        // 2. Constants / Agent Info
        parms.setAgentconfirmation("apiconnector");

        // 3. Confirmation Email (Use first passenger's email or agent default)
        if (requestDto.getPassengers() != null && !requestDto.getPassengers().isEmpty()) {
            parms.setConfirmationemail(requestDto.getPassengers().get(0).getEmail());
        } else {
            parms.setConfirmationemail("noreply@airlines.com"); // Fallback
        }

        // 4. Map Passengers
        List<Passenger> paxList = new ArrayList<>();
        if (requestDto.getPassengers() != null) {
            for (OrderCreateReqDto.Pax dtoPax : requestDto.getPassengers()) {
                Passenger p = new Passenger();
                p.setPaxtitle(dtoPax.getTitle()+".");
                p.setFirstname(dtoPax.getFirstName());
                p.setLastname(dtoPax.getLastName());

                // Age - if not in DTO, leave null or calculate? User sample had null.
                // p.setPaxage(null);

                // Address / Contact
                if (dtoPax.getPhoneNumber() != null) {
                    String phone = dtoPax.getPhoneNumber().toString();
                    if (dtoPax.getCountryDialingCode() != null) {
                        p.setPaxphone(dtoPax.getCountryDialingCode() + phone);
                    } else {
                        p.setPaxphone(phone);
                    }
                }
                p.setPaxemail(dtoPax.getEmail());
                p.setPaxbirthdate(dtoPax.getDob()); // format check?

                // Documents
                if (dtoPax.getIdentityDocument() != null) {
                    OrderCreateReqDto.Pax.IdentityDocument doc = dtoPax.getIdentityDocument();
                    p.setPaxnationailty(doc.getCitizenshipCountryCode());
                    p.setPaxdoctype(doc.getIdentityDocumentType() != null ? doc.getIdentityDocumentType() : "PP");
                    p.setPaxdocnumber(doc.getIdentityDocumentNumber());
                    p.setPaxdocissuer(doc.getIssuingCountryCode());
                    p.setPaxdocexpiry(doc.getExpiryDate());
                } else {
                    // Defaults if missing or required?
                    p.setPaxdoctype("PP");
                    p.setPaxnationailty("US"); // Fallback from sample
                }

                paxList.add(p);
            }
        }
        parms.setPassenger(paxList);

        aerocrs.setParms(parms);
        request.setAerocrs(aerocrs);
        return request;
    }

    public Object unmarshal() throws DatatypeConfigurationException, IOException, InterruptedException {
        String response = makeApiCall();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode root = objectMapper.readTree(response);

            if (root.has("errors")) {
                ErrorRsp errorRsp = new ErrorRsp();
                JsonNode errorsArray = root.path("errors");
                if (errorsArray.isArray()) {
                    for (JsonNode errorNode : errorsArray) {
                        String errorMessage = errorNode.path("message").asText();
                        String code = errorNode.path("code").asText();
                        ErrorRsp.Error tempError = new ErrorRsp.Error();
                        tempError.setError(errorMessage);
                        tempError.setCode(code);
                        errorRsp.getErrorList().add(tempError);
                    }
                }
                return errorRsp;
            } else if (root.has("success") && !root.path("success").asBoolean() && root.has("details")) {
                ErrorRsp errorRsp = new ErrorRsp();
                JsonNode detailsNode = root.path("details");
                if (detailsNode.has("detail")) {
                    JsonNode detailArray = detailsNode.path("detail");
                    if (detailArray.isArray()) {
                        for (JsonNode msgNode : detailArray) {
                            ErrorRsp.Error tempError = new ErrorRsp.Error();
                            tempError.setError(msgNode.asText());
                            errorRsp.getErrorList().add(tempError);
                        }
                    } else {
                        ErrorRsp.Error tempError = new ErrorRsp.Error();
                        tempError.setError(detailArray.asText());
                        errorRsp.getErrorList().add(tempError);
                    }
                }
                return errorRsp;
            } else {
                return objectMapper.readValue(response, Object.class);
            }
        } catch (Exception e) {
            System.out.println("Error parsing response: " + e.getMessage());
            return response;
        }
    }

    public String makeApiCall() throws IOException {
        String jsonBody = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(this);

        HttpHeaders headers = new HttpHeaders();
        // Hardcoded Auth
        headers.add("auth_id", "70DD4369-72F3-4426-A050-196FBC345009");
        headers.add("auth_password", "vJ3yGilZ9u7N");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        System.out.println("Generated OrderConfirm Request is:\n" + jsonBody);

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.exchange(orderConfirmUrl, HttpMethod.POST, entity,
                    String.class);
            System.out.println("OrderConfirm Response: " + response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.out.println("HTTP Error Response: " + e.getResponseBodyAsString());
            return e.getResponseBodyAsString();
        }
    }
}
