package com.airlines.GO7API.request;

import com.airlines.GO7API.error.ErrorRsp;
import com.airlines.GO7API.requestDto.OfferPriceReqDto;
import com.airlines.GO7API.responseDto.OfferPriceRspDto;
//import com.airlines.GO7API.responseDto.OfferPriceRspGo7Dto;
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
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OfferPriceReq {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @JsonIgnore
    private String offerPriceUrl = "https://api.aerocrs.com/v5/getFlight";

    @Data
    public static class Aerocrs {
        @JsonProperty("parms")
        private Map<String, Object> parms;
    }

    public static OfferPriceReq mapToOfferPriceRequestDTO(OfferPriceReqDto offerPriceRQ) {
        OfferPriceReq request = new OfferPriceReq();
        Aerocrs aerocrs = new Aerocrs();
        // Use LinkedHashMap to preserve order: triptype, fromcode, tocode, adults,
        // child, infant, flightid1...
        Map<String, Object> parms = new LinkedHashMap<>();

        if (offerPriceRQ.getOffers() != null && !offerPriceRQ.getOffers().isEmpty()) {
            // Extract common parameters from the FIRST offer to ensure they come first in
            // the JSON
            OfferPriceReqDto.Offer firstOffer = offerPriceRQ.getOffers().get(0);
            String firstOfferId = firstOffer.getOfferId();

            String triptype = "OW";
            String fromcode = null;
            String tocode = null;
            int adults = 1;
            int child = 0;
            int infant = 0;

            if (firstOfferId != null && !firstOfferId.isEmpty()) {
                String[] parts = firstOfferId.split("-");
                if (parts.length >= 8) {
                    // parts[2]=from, parts[3]=to, parts[4]=type, parts[5/6/7]=pax
                    fromcode = parts[2];
                    tocode = parts[3];
                    String type = parts[4];
                    if ("Return".equalsIgnoreCase(type) || "RT".equalsIgnoreCase(type)) {
                        triptype = "RT";
                    }
                    try {
                        adults = Integer.parseInt(parts[5]);
                        child = Integer.parseInt(parts[6]);
                        infant = Integer.parseInt(parts[7]);
                    } catch (NumberFormatException e) {
                        // keep defaults
                    }
                }
            }

            // Put headers in desired order
            parms.put("triptype", triptype);
            parms.put("fromcode", fromcode);
            parms.put("tocode", tocode);
            parms.put("adults", adults);
            parms.put("child", child);
            parms.put("infant", infant);

            // Loop for Flight/Fare IDs to append them after the headers
            for (int i = 0; i < offerPriceRQ.getOffers().size(); i++) {
                OfferPriceReqDto.Offer offer = offerPriceRQ.getOffers().get(i);
                String offerId = offer.getOfferId();
                if (offerId != null) {
                    String[] parts = offerId.split("-");
                    if (parts.length >= 2) {
                        String suffix = String.valueOf(i + 1);
                        parms.put("flightid" + suffix, parts[0]);
                        parms.put("fareid" + suffix, parts[1]);
                    }
                }
            }

        } else {
            // Fallback defaults if no offers
            parms.put("triptype", "OW");
            parms.put("adults", 1);
            parms.put("child", 0);
            parms.put("infant", 0);
        }

        aerocrs.setParms(parms);
        request.setAerocrs(aerocrs);

        // Map dynamic URL if provided
        if (offerPriceRQ.getOfferpriceurl() != null && !offerPriceRQ.getOfferpriceurl().isEmpty()) {
            request.setOfferPriceUrl(offerPriceRQ.getOfferpriceurl());
        }

        return request;
    }

    public Object unmarshal() throws DatatypeConfigurationException, IOException, InterruptedException {
        String response = makeApiCall();
        ObjectMapper objectMapper = new ObjectMapper();
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
            // Return raw response for inspection
            return objectMapper.readValue(response, Object.class);
        }
    }

    public String makeApiCall() throws IOException {
        String jsonBody = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(this);

        HttpHeaders headers = new HttpHeaders();
        // Hardcoded Auth Credentials
        headers.add("auth_id", "70DD4369-72F3-4426-A050-196FBC345009");
        headers.add("auth_password", "vJ3yGilZ9u7N");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        System.out.println("Generated OfferPrice Request is:\n" + jsonBody);

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.exchange(offerPriceUrl, HttpMethod.POST, entity,
                    String.class);
            System.out.println("OfferPrice Response: " + response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.out.println("HTTP Error Response: " + e.getResponseBodyAsString());
            return e.getResponseBodyAsString();
        }
    }
}
