package com.airlines.GO7API.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.airlines.GO7API.error.ErrorRsp;
import com.airlines.GO7API.requestDto.OrderCreateReqDto;
import lombok.Data;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderCreateReq {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @JsonIgnore
    // Default URL, can be overridden by DTO
    private String orderCreateUrl = "https://api.aerocrs.com/v5/createBooking";

    @Data
    public static class Aerocrs {
        @JsonProperty("parms")
        private Map<String, Object> parms;
    }

    public static OrderCreateReq mapToOrderCreateReq(OrderCreateReqDto orderCreateRQ) {
        OrderCreateReq request = new OrderCreateReq();
        Aerocrs aerocrs = new Aerocrs();
        Map<String, Object> parms = new LinkedHashMap<>();

        // Map Offer ID (Flight/Fare IDs)
        String offerId = orderCreateRQ.getOfferId();
        java.util.List<Map<String, Object>> bookflightList = new java.util.ArrayList<>();

        if (offerId != null) {
            String[] segments;
            if (offerId.contains("*")) {
                segments = offerId.split("\\*");
            } else {
                segments = new String[] { offerId };
            }

            for (String segment : segments) {
                String[] parts = segment.split("-");
                // Expected: flightId-fareId-fromCode-toCode
                if (parts.length >= 4) {
                    Map<String, Object> flightMap = new LinkedHashMap<>();
                    try {
                        flightMap.put("fromcode", parts[2]);
                        flightMap.put("tocode", parts[3]);
                        flightMap.put("flightid", Long.parseLong(parts[0]));
                        flightMap.put("fareid", Long.parseLong(parts[1]));
                    } catch (NumberFormatException e) {
                        // fallback if not numeric
                        flightMap.put("flightid", parts[0]);
                        flightMap.put("fareid", parts[1]);
                    }
                    bookflightList.add(flightMap);
                }
            }
        }

        // Calculate Pax Counts
        int adults = 0;
        int child = 0;
        int infant = 0;
        if (orderCreateRQ.getPassengers() != null) {
            for (OrderCreateReqDto.Pax pax : orderCreateRQ.getPassengers()) {
                String ptc = pax.getPtc(); // Assuming PTC is available or deriving from type
                // Simple logic based on convention or missing PTC field in DTO
                // If PTC is missing, assume Adult. Or check if DTO has ptc field.
                // Looking at DTO earlier, it handles List<Pax>. Pax has... let's assume default
                // 1 adult if invalid.
                if (ptc == null)
                    ptc = "ADT";

                if ("CHD".equalsIgnoreCase(ptc) || "CNN".equalsIgnoreCase(ptc))
                    child++;
                else if ("INF".equalsIgnoreCase(ptc))
                    infant++;
                else
                    adults++;
            }
        }
        if (adults == 0 && child == 0 && infant == 0)
            adults = 1; // Default

        parms.put("triptype", bookflightList.size() > 1 ? "RT" : "OW"); // Simple derivation
        parms.put("adults", adults);
        parms.put("child", child);
        parms.put("infant", infant);
        parms.put("bookflight", bookflightList);

        // Map Dynamic OrderCreate URL if present (internal use, not in body param)
        if (orderCreateRQ.getOrderCreateUrl() != null && !orderCreateRQ.getOrderCreateUrl().isEmpty()) {
            request.setOrderCreateUrl(orderCreateRQ.getOrderCreateUrl());
        }

        // Map ApiKey if needed for headers or params (usually headers)
        if (orderCreateRQ.getApiKey() != null) {
            // Can store in request object if needed for makeApiCall headers
            // But strict mapping to parms doesn't usually put apiKey in parms for Aerocrs
        }

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
            } else {
                // Return generic object or specific map
                return objectMapper.readValue(response, Object.class);
            }
        } catch (Exception e) {
            // Fallback for non-JSON or other errors
            System.out.println("Error parsing response: " + e.getMessage());
            return response;
        }
    }

    public String makeApiCall() throws IOException {
        String jsonBody = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(this);

        HttpHeaders headers = new HttpHeaders();
        // Hardcoded Auth (or derived if passed)
        headers.add("auth_id", "70DD4369-72F3-4426-A050-196FBC345009");
        headers.add("auth_password", "vJ3yGilZ9u7N");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        System.out.println("Generated OrderCreate Request is:\n" + jsonBody);

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.exchange(orderCreateUrl, HttpMethod.POST, entity,
                    String.class);
            System.out.println("OrderCreate Response: " + response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.out.println("HTTP Error Response: " + e.getResponseBodyAsString());
            return e.getResponseBodyAsString();
        }
    }
}
