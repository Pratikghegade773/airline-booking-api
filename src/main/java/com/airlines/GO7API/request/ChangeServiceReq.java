package com.airlines.GO7API.request;

import com.airlines.GO7API.requestDto.ChangeServiceReqDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@ToString
public class ChangeServiceReq {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @JsonIgnore
    private String apiKey;

    @JsonIgnore
    private String changeAncillariesUrl = "https://api.aerocrs.com/v5/createAncillary";

    @Data
    @NoArgsConstructor
    @ToString
    public static class Aerocrs {
        @JsonProperty("parms")
        private Parms parms;
    }

    @Data
    @NoArgsConstructor
    @ToString
    public static class Parms {
        @JsonProperty("ancillaries")
        private Ancillaries ancillaries;
    }

    @Data
    @NoArgsConstructor
    @ToString
    public static class Ancillaries {
        @JsonProperty("ancillary")
        private List<Ancillary> ancillary;
    }

    @Data
    @NoArgsConstructor
    @ToString
    public static class Ancillary {
        @JsonProperty("paxnum")
        private Integer paxnum;

        @JsonProperty("itemid")
        private Long itemid;

        @JsonProperty("bookingid")
        private Long bookingid;

        @JsonProperty("flightid")
        private Long flightid;
    }

    public static ChangeServiceReq mapToChangeServiceReq(ChangeServiceReqDto dto,
            com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto bookingRsp) {
        ChangeServiceReq request = new ChangeServiceReq();
        Aerocrs aerocrs = new Aerocrs();
        Parms parms = new Parms();
        Ancillaries ancillaries = new Ancillaries();
        List<Ancillary> ancillaryList = new ArrayList<>();

        if (dto.getOffers() != null) {
            for (ChangeServiceReqDto.Offer offer : dto.getOffers()) {
                if (offer.getOfferItems() != null) {
                    for (ChangeServiceReqDto.Offer.OfferItemDto item : offer.getOfferItems()) {
                        if (item.getSpecialServices() != null) {
                            String serviceDefId = item.getOfferItemId(); // User requested to map itemid from
                                                                         // offerItemId

                            // Map Pax Refs to PaxNum
                            List<Integer> paxNums = new ArrayList<>();
                            if (item.getPaxRefs() != null
                                    && bookingRsp.getAerocrs() != null
                                    && bookingRsp.getAerocrs().getBooking() != null
                                    && bookingRsp.getAerocrs().getBooking().getPassengers() != null) {
                                List<com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.Passenger> passengers = bookingRsp
                                        .getAerocrs().getBooking().getPassengers().getPassenger();
                                for (String paxRef : item.getPaxRefs()) {
                                    try {
                                        String numStr = paxRef.replaceAll("[^0-9]", "");
                                        if (!numStr.isEmpty()) {
                                            int idx = Integer.parseInt(numStr) - 1;
                                            if (idx >= 0 && idx < passengers.size()) {
                                                // If available use it, else fallback to index
                                                int pNum = passengers.get(idx).getPaxnum();
                                                if (pNum == 0)
                                                    pNum = idx;
                                                paxNums.add(pNum);
                                            }
                                        }
                                    } catch (Exception e) {
                                        System.out.println("Error parsing paxRef: " + paxRef);
                                    }
                                }
                            }

                            // Map Segment Refs to FlightID
                            List<Long> flightIds = new ArrayList<>();
                            if (item.getSpecialServices().getSegId() != null
                                    && bookingRsp.getAerocrs() != null
                                    && bookingRsp.getAerocrs().getBooking() != null
                                    && bookingRsp.getAerocrs().getBooking().getFlights() != null) {
                                List<com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.Flight> flights = bookingRsp
                                        .getAerocrs().getBooking().getFlights().getFlight();
                                for (String segRef : item.getSpecialServices().getSegId()) {
                                    try {
                                        String numStr = segRef.replaceAll("[^0-9]", "");
                                        if (!numStr.isEmpty()) {
                                            int idx = Integer.parseInt(numStr) - 1;
                                            if (idx >= 0 && idx < flights.size()) {
                                                flightIds.add((long) flights.get(idx).getFlightid());
                                            }
                                        }
                                    } catch (Exception e) {
                                        System.out.println("Error parsing segRef or flightId: " + segRef);
                                    }
                                }
                            }

                            // Create Ancillary Entry for each combination
                            for (Integer paxNum : paxNums) {
                                for (Long flightId : flightIds) {
                                    Ancillary ancillary = new Ancillary();
                                    ancillary.setPaxnum(paxNum);
                                    try {
                                        ancillary.setItemid(Long.parseLong(serviceDefId));
                                    } catch (Exception e) {
                                        // Provide fallback or log error if serviceDefId is not numeric
                                        System.out.println("Error parsing serviceDefId: " + serviceDefId);
                                    }

                                    if (bookingRsp.getAerocrs() != null
                                            && bookingRsp.getAerocrs().getBooking() != null) {
                                        Long bid = bookingRsp.getAerocrs().getBooking().getBookingid();
                                        ancillary.setBookingid(bid);
                                    }

                                    ancillary.setFlightid(flightId);
                                    ancillaryList.add(ancillary);
                                }
                            }
                        }
                    }
                }
            }
        }

        ancillaries.setAncillary(ancillaryList);
        parms.setAncillaries(ancillaries);
        aerocrs.setParms(parms);
        request.setAerocrs(aerocrs);
        request.setApiKey(dto.getApiKey());
        // Set other config if needed
        return request;
    }

    public Object unmarshal() throws java.io.IOException {
        String response = makeApiCall();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode root = objectMapper.readTree(response);
            if (root.has("errors")) {
                com.airlines.GO7API.error.ErrorRsp errorRsp = new com.airlines.GO7API.error.ErrorRsp();
                errorRsp.setErrorList(new java.util.ArrayList<>());
                JsonNode errorsArray = root.path("errors");
                if (errorsArray.isArray()) {
                    for (JsonNode errorNode : errorsArray) {
                        com.airlines.GO7API.error.ErrorRsp.Error tempError = new com.airlines.GO7API.error.ErrorRsp.Error();
                        tempError.setError(errorNode.path("message").asText());
                        tempError.setCode(errorNode.path("code").asText());
                        errorRsp.getErrorList().add(tempError);
                    }
                }
                return errorRsp;
            } else {
                return response;
            }
        } catch (Exception e) {
            System.out.println("Error parsing response: " + e.getMessage());
            return response;
        }
    }

    public String makeApiCall() throws java.io.IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
        String jsonBody = mapper.writeValueAsString(this);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Hardcoded Auth for consistency with other files
        headers.add("auth_id", "70DD4369-72F3-4426-A050-196FBC345009");
        headers.add("auth_password", "vJ3yGilZ9u7N");

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        System.out.println("Generated ChangeService Request is:\n" + jsonBody);

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.exchange(changeAncillariesUrl, HttpMethod.POST, entity,
                    String.class);
            System.out.println("ChangeService Response: " + response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.out.println("HTTP Error Response: " + e.getResponseBodyAsString());
            return e.getResponseBodyAsString();
        }
    }
}
