package com.airlines.go7api.request;

import com.airlines.go7api.error.ErrorRsp;
import com.airlines.go7api.requestdto.OrderReshopReqDto;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderReshopReq {

    @JsonProperty("aerocrs")
    public Aerocrs aerocrs;

    @JsonIgnore
    public String reshopUrl;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Aerocrs {
        @JsonProperty("parms")
        public java.util.Map<String, Object> parms;
    }

    public static OrderReshopReq mapFromJson(String json) throws IOException {
        System.out.println("DEBUG: mapFromJson starting. Body: " + json);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        OrderReshopReq req = new OrderReshopReq();
        JsonNode root = mapper.readTree(json);

        java.util.Map<String, Object> sourceParms = new java.util.LinkedHashMap<>();

        // 1. Gather all potential sources of parameters
        JsonNode aerocrsNode = root.has("aerocrs") ? root.get("aerocrs") : null;
        JsonNode parmsNode = (aerocrsNode != null && aerocrsNode.has("parms")) ? aerocrsNode.get("parms")
                : (root.has("parms") ? root.get("parms") : null);

        if (parmsNode != null) {
            System.out.println("DEBUG: Found 'parms' node. Mapping...");
            sourceParms = mapper.convertValue(parmsNode,
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                    });
        }

        // Root field mapping (supporting flat input format)
        java.util.Iterator<String> fieldNames = root.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (fieldName.equals("orderId")) {
                String oid = root.get("orderId").asText();
                if (oid.matches("\\d+"))
                    sourceParms.put("bookingid", Long.parseLong(oid));
                else
                    sourceParms.put("bookingconfirmation", oid);
            } else if (!fieldName.equals("aerocrs") && !fieldName.equals("responseId") && !fieldName.equals("agencyId")
                    &&
                    !fieldName.equals("agentId") && !fieldName.equals("agencyName") &&
                    !fieldName.equals("apiKey") && !fieldName.equals("reshopUrl")) {
                sourceParms.put(fieldName, mapper.convertValue(root.get(fieldName), Object.class));
            }
        }

        // 2. Build the STRICT output parms for Go7
        java.util.Map<String, Object> targetParms = new java.util.LinkedHashMap<>();

        // Booking ID / Confirmation
        Object bid = sourceParms.get("bookingid");
        Object bconf = sourceParms.get("bookingconfirmation");
        if (bconf != null)
            targetParms.put("bookingconfirmation", bconf);
        else if (bid != null)
            targetParms.put("bookingconfirmation", bid); // Go7 often accepts ID in confirmation field or uses
                                                         // specialized logic

        // Action & Currency
        targetParms.put("action", sourceParms.containsKey("action") ? sourceParms.get("action") : "amend");
        targetParms.put("currency", sourceParms.containsKey("currency") ? sourceParms.get("currency") : "USD");

        // Bookflight (Extracted from deleteOrderItems or taken directly)
        java.util.List<java.util.Map<String, Object>> bookflightList = new java.util.ArrayList<>();
        if (sourceParms.containsKey("bookflight")) {
            bookflightList = (java.util.List) sourceParms.get("bookflight");
        } else if (sourceParms.containsKey("deleteOrderItems")) {
            System.out.println("DEBUG: Ignoring deleteOrderItems for bookflight generation. Controller will fetch true flight IDs from AeroCRS.");
            // Do not parse from deleteOrderItems because GO7 orderItemIds (e.g., PF7963B26) are not AeroCRS flight IDs!
        }
        if (!bookflightList.isEmpty()) {
            targetParms.put("bookflight", bookflightList);
        }

        // 3. Finalize Request
        Aerocrs a = new Aerocrs();
        a.parms = targetParms;
        req.aerocrs = a;

        System.out.println("DEBUG: Refined parms: " + targetParms.keySet());
        req.reshopUrl = "https://api.aerocrs.com/v5/changeBooking";
        return req;
    }

    public Object unmarshal() throws DatatypeConfigurationException, IOException, InterruptedException {
        String response = makeApiCall();
        if (response == null || response.trim().isEmpty()) {
            System.out.println("DEBUG: Response is NULL or EMPTY");
            return response;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            JsonNode root = objectMapper.readTree(response);
            if (root.has("errors")) {
                ErrorRsp errorRsp = new ErrorRsp();
                JsonNode errorsArray = root.path("errors");
                if (errorsArray.isArray()) {
                    for (JsonNode errorNode : errorsArray) {
                        ErrorRsp.Error tempError = new ErrorRsp.Error();
                        tempError.setError(errorNode.path("message").asText());
                        tempError.setCode(errorNode.path("code").asText());
                        errorRsp.getErrorList().add(tempError);
                    }
                }
                return errorRsp;
            } else {
                return objectMapper.readValue(response, Object.class);
            }
        } catch (Exception e) {
            System.out.println("Error parsing OrderReshop response: " + e.getMessage());
            return response;
        }
    }

    public String makeApiCall() throws IOException {
        String baseUrl = reshopUrl;

        if (this.aerocrs == null || this.aerocrs.parms == null) {
            System.out.println("DEBUG: aerocrs or parms is NULL in makeApiCall");
            return null;
        }

        // Add query parameters as per user sample URL
        StringBuilder urlWithParams = new StringBuilder(baseUrl);
        boolean first = true;

        java.util.Map<String, Object> p = this.aerocrs.parms;
        if (p.containsKey("bookingconfirmation") || p.containsKey("bookingid")) {
            urlWithParams.append("?").append("bookingconfirmation=")
                    .append(p.get("bookingconfirmation") != null ? p.get("bookingconfirmation") : p.get("bookingid"));
            first = false;
        }
        if (p.containsKey("currency")) {
            urlWithParams.append(first ? "?" : "&").append("currency=").append(p.get("currency"));
            first = false;
        }
        if (p.containsKey("action")) {
            urlWithParams.append(first ? "?" : "&").append("action=").append(p.get("action"));
            first = false;
        }

        String finalUrl = urlWithParams.toString();
        ObjectMapper mapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .enable(SerializationFeature.INDENT_OUTPUT);
        String jsonBody = mapper.writeValueAsString(this);

        System.out.println("Generated OrderReshop Request URL: " + finalUrl);
        System.out.println("Generated OrderReshop Request Body:\n" + jsonBody);

        HttpHeaders headers = new HttpHeaders();
        headers.add("auth_id", "70DD4369-72F3-4426-A050-196FBC345009");
        headers.add("auth_password", "vJ3yGilZ9u7N");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.exchange(finalUrl, HttpMethod.POST, entity, String.class);
            System.out.println("HTTP Response Status Code: " + response.getStatusCode());
            System.out.println("OrderReshop Response: " + response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.out.println("HTTP Error Response: " + e.getResponseBodyAsString());
            return e.getResponseBodyAsString();
        }
    }
}
