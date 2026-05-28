package com.airlines.go7api.request;

import com.airlines.go7api.error.ErrorRsp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DeserializationFeature;
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

import java.io.IOException;

public abstract class BaseGo7Req {

    @JsonIgnore
    protected abstract String getApiUrl();

    @JsonIgnore
    protected abstract String getRequestName();

    public Object unmarshal() throws IOException {
        String response = makeApiCall();
        if (response == null || response.trim().isEmpty()) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
                return objectMapper.readValue(response, Object.class);
            }
        } catch (Exception e) {
            System.out.println("Error parsing " + getRequestName() + " response: " + e.getMessage());
            return response;
        }
    }

    public String makeApiCall() throws IOException {
        String baseUrl = getApiUrl();
        String jsonBody = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(this);

        HttpHeaders headers = new HttpHeaders();
        // Standard headers matching across API
        headers.add("auth_id", "70DD4369-72F3-4426-A050-196FBC345009");
        headers.add("auth_password", "vJ3yGilZ9u7N");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        System.out.println("Generated " + getRequestName() + " Request is:\n" + jsonBody);

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.POST, entity, String.class);
            System.out.println("HTTP Response Status Code: " + response.getStatusCode());
            System.out.println(getRequestName() + " Response: " + response.getBody());
            return response.getBody();

        } catch (HttpClientErrorException e) {
            System.out.println("HTTP Error Response: " + e.getResponseBodyAsString());
            return e.getResponseBodyAsString();
        } catch (Exception e) {
            System.out.println("General Error in makeApiCall: " + e.getMessage());
            return null;
        }
    }
}
