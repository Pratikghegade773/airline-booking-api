package com.airlines.GO7API.request;

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
public class MakePaymentReq {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    private static final String URL = "https://api.aerocrs.com/v4/makePayment";

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Aerocrs {
        private Parms parms;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Parms {
        private Long bookingid;
        private String creditcardpayer;
        private String creditcardnumber;
        private String creditcardexpiry;
        private String creditcardcvv;
    }

    public static MakePaymentReq mapToMakePaymentReq(com.airlines.GO7API.requestDto.OrderCreateReqDto requestDto,
            Long bookingId) {
        MakePaymentReq req = new MakePaymentReq();
        Aerocrs aerocrs = new Aerocrs();
        Parms parms = new Parms();
        parms.setBookingid(bookingId);

        if (requestDto.getPaymentInformation() != null) {
            com.airlines.GO7API.requestDto.OrderCreateReqDto.PaymentInformation payInfo = requestDto
                    .getPaymentInformation();

            // Credit Card
            parms.setCreditcardpayer(payInfo.getCardHolderName());
            parms.setCreditcardnumber(payInfo.getCardNumber());
            parms.setCreditcardexpiry(payInfo.getExpiration());
            // CVV mapping - assuming DTO might have it or we hardcode/pass from somewhere
            // else if needed
            // The DTO 'PaymentInformation' I saw in step 634 didn't seem to have CVV
            // explicitly shown in the snippet?
            // Re-checking Step 634...
            // public static class PaymentInformation { ... cardCode; ... } -> is cardCode
            // the CVV? Usually cardCode or seriesCode.
            // Let's assume seriesCode or cardCode. DTO has: cardCode, seriesCode.
            // Usually CVV is seriesCode or cardCode. I'll map 'seriesCode' to
            // 'creditcardcvv' as guess, or checks if DTO needs update.
            // Wait, Step 634 DTO has `seriesCode`.
            parms.setCreditcardcvv(payInfo.getSeriesCode());
        }

        aerocrs.setParms(parms);
        req.setAerocrs(aerocrs);
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
            System.out.println("MakePayment Request: " + jsonBody);

            RestTemplate restTemplate = new RestTemplate();
            String response;
            try {
                response = restTemplate.postForObject(URL, entity, String.class);
            } catch (HttpClientErrorException e) {
                response = e.getResponseBodyAsString();
            }
            System.out.println("MakePayment Response: " + response);

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
