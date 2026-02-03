package com.airlines.GO7API.request;

import com.airlines.GO7API.requestDto.GetBookingReqDto;
import com.airlines.GO7API.error.ErrorRsp;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetBookingReq {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    private static final String URL = "https://api.aerocrs.com/v5/getBooking";

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
    public static class Parms {
        @JsonProperty("bookingconfirmation")
        private String bookingconfirmation;
    }

    public static GetBookingReq mapToGetBookingReq(GetBookingReqDto dto) {
        GetBookingReq req = new GetBookingReq();
        if (dto.getAerocrs() != null && dto.getAerocrs().getParms() != null) {
            Aerocrs aerocrs = new Aerocrs();
            Parms parms = new Parms();
            parms.setBookingconfirmation(dto.getAerocrs().getParms().getBookingconfirmation());
            aerocrs.setParms(parms);
            req.setAerocrs(aerocrs);
        }
        return req;
    }

    public Object unmarshal() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            String jsonBody = mapper.writeValueAsString(this);

            HttpHeaders headers = new HttpHeaders();
            headers.add("auth_id", "70DD4369-72F3-4426-A050-196FBC345009");
            headers.add("auth_password", "vJ3yGilZ9u7N");
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            System.out.println("GetBooking Request: " + jsonBody);

            RestTemplate restTemplate = new RestTemplate();
            String response;
            try {
                response = restTemplate.postForObject(URL, entity, String.class);
            } catch (HttpClientErrorException e) {
                response = e.getResponseBodyAsString();
            }
            System.out.println("GetBooking Response: " + response);

            JsonNode root = mapper.readTree(response);
            if (root.has("success") && !root.path("success").asBoolean() && root.has("details")) {
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
                    }
                }
                return errorRsp;
            }

            return mapper.readValue(response, Object.class);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
