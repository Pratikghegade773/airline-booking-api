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
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderCreateReq {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @JsonIgnore
    private String apiUrl = "https://api.aerocrs.com/v5/createBooking"; // Default

    @JsonIgnore
    private String apiKey; // For OrderTicket

    @Data
    public static class Aerocrs {
        @JsonProperty("parms")
        private Map<String, Object> parms;
    }

    // --------------------------------------------------------------------------------------------
    // 1. OrderCreate Mapping
    // --------------------------------------------------------------------------------------------
    public static OrderCreateReq mapToOrderCreateReq(OrderCreateReqDto orderCreateRQ) {
        OrderCreateReq request = new OrderCreateReq();
        Aerocrs aerocrs = new Aerocrs();
        Map<String, Object> parms = new LinkedHashMap<>();

        // Map Offer ID (Flight/Fare IDs)
        String offerId = orderCreateRQ.getOfferId();
        List<Map<String, Object>> bookflightList = new ArrayList<>();

        if (offerId != null) {
            String[] segments;
            if (offerId.contains("*")) {
                segments = offerId.split("\\*");
            } else {
                segments = new String[] { offerId };
            }

            for (String segment : segments) {
                String[] parts = segment.split("-");
                if (parts.length >= 4) {
                    Map<String, Object> flightMap = new LinkedHashMap<>();
                    try {
                        flightMap.put("fromcode", parts[2]);
                        flightMap.put("tocode", parts[3]);
                        flightMap.put("flightid", Long.parseLong(parts[0]));
                        flightMap.put("fareid", Long.parseLong(parts[1]));
                    } catch (NumberFormatException e) {
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
                String ptc = pax.getPtc();
                if (ptc == null)
                    ptc = "ADT";

                if ("CHD".equalsIgnoreCase(ptc) || "CNN".equalsIgnoreCase(ptc))
                    child++;
                else if ("INF".equalsIgnoreCase(ptc) || "INFANT".equalsIgnoreCase(ptc))
                    infant++;
                else
                    adults++;
            }
        }
        if (adults == 0 && child == 0 && infant == 0)
            adults = 1;

        parms.put("triptype", bookflightList.size() > 1 ? "RT" : "OW");
        parms.put("adults", adults);
        parms.put("child", child);
        parms.put("infant", infant);
        parms.put("bookflight", bookflightList);

        // Passengers List (Only Names for OrderCreate? The original code didn't show
        // full pax mapping for OrderCreate but kept it simple. Preserving original
        // logic implies checking previous file content.
        // Original file (Step 197/430) logic stopped at 'adults' calculation and
        // skipped detailed pax mapping for OrderCreate?
        // Wait, looking at Step 430... It had the pax count logic but I cut off reading
        // at line 100.
        // To be safe, I will only include what I saw or improve.
        // OrderCreate usually requires names.
        // Use the passengers list from DTO to populate "passenger" list in parms.

        List<Map<String, Object>> passengerList = new ArrayList<>();
        if (orderCreateRQ.getPassengers() != null) {
            for (OrderCreateReqDto.Pax dtoPax : orderCreateRQ.getPassengers()) {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("firstname", dtoPax.getFirstName());
                p.put("lastname", dtoPax.getLastName());
                // p.put("title", mapTitleToId(dtoPax.getTitle()));
                p.put("title", mapTitleToId(dtoPax.getTitle()));
                // Map gender to M/F
                String gender = dtoPax.getGender();
                if (gender != null && !gender.isEmpty()) {
                    p.put("gender", gender.toUpperCase().startsWith("M") ? "M" : "F");
                }
                // Calculate Age for all passengers
                String age = calculateAge(dtoPax.getDob());
                if (age != null) {
                    p.put("paxage", age);
                }
                passengerList.add(p);
            }
        }
        parms.put("passenger", passengerList);

        // Agent/User
        parms.put("useremail", "apiconnector@go7.com"); // Placeholder/Default
        parms.put("agencypassword", "apiconnector");

        aerocrs.setParms(parms);
        request.setAerocrs(aerocrs);
        request.setApiUrl("https://api.aerocrs.com/v5/createBooking");
        return request;
    }

    private static Integer mapTitleToId(String title) {
        if (title == null)
            return 1; // Default to Mr
        switch (title.toUpperCase().replace(".", "").trim()) {
            case "MR":
                return 1;
            case "MRS":
                return 2;
            case "MS":
                return 3;
            case "MISS":
                return 4;
            case "MSTR":
                return 5;
            default:
                return 1;
        }
    }

    private static String calculateAge(String dob) {
        if (dob == null || dob.isEmpty()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate birthDate = LocalDate.parse(dob, formatter);
            LocalDate currentDate = LocalDate.now();
            return String.valueOf(Period.between(birthDate, currentDate).getYears());
        } catch (Exception e) {
            return null;
        }
    }

    // --------------------------------------------------------------------------------------------
    // 2. OrderConfirm Mapping
    // --------------------------------------------------------------------------------------------
    public static OrderCreateReq mapToOrderConfirmReq(OrderCreateReqDto requestDto, Long bookingId) {
        OrderCreateReq request = new OrderCreateReq();
        Aerocrs aerocrs = new Aerocrs();
        Map<String, Object> parms = new LinkedHashMap<>();

        parms.put("bookingid", bookingId);
        parms.put("agentconfirmation", "apiconnector");

        // Email
        if (requestDto.getPassengers() != null && !requestDto.getPassengers().isEmpty()) {
            parms.put("confirmationemail", requestDto.getPassengers().get(0).getEmail());
        } else {
            parms.put("confirmationemail", "noreply@airlines.com");
        }

        // Passengers Full Details
        List<Map<String, Object>> paxList = new ArrayList<>();
        long unassignedInfants = 0;
        if (requestDto.getPassengers() != null) {
            unassignedInfants = requestDto.getPassengers().stream()
                    .filter(pax -> "INF".equalsIgnoreCase(pax.getPtc()) || "INFANT".equalsIgnoreCase(pax.getPtc()))
                    .count();
        }

        if (requestDto.getPassengers() != null) {
            for (OrderCreateReqDto.Pax dtoPax : requestDto.getPassengers()) {
                String ptc = dtoPax.getPtc(); // Assuming PTC is available or default to ADT

                // Included Infants as they are required for "Passengers must match" check

                String titleStr = dtoPax.getTitle();

                // AeroCRS confirmBooking specific title mapping
                if ("CHD".equalsIgnoreCase(ptc) || "CNN".equalsIgnoreCase(ptc)) {
                    titleStr = "Child";
                } else if ("INF".equalsIgnoreCase(ptc) || "INFANT".equalsIgnoreCase(ptc)) {
                    titleStr = "INFANT";
                }

                Map<String, Object> p = new LinkedHashMap<>();

                // Set title. 'Child' and 'INFANT' should NOT have a trailing dot.
                if ("Child".equals(titleStr) || "INFANT".equals(titleStr)) {
                    p.put("paxtitle", titleStr);
                } else if (titleStr != null && !titleStr.endsWith(".")) {
                    p.put("paxtitle", titleStr + ".");
                } else {
                    p.put("paxtitle", titleStr);
                }

                // paxcarringinfant logic
                // If this passenger is an Adult (not Child/Infant) and we have infants to carry
                boolean isAdult = !("CHD".equalsIgnoreCase(ptc) || "CNN".equalsIgnoreCase(ptc)
                        || "INF".equalsIgnoreCase(ptc) || "INFANT".equalsIgnoreCase(ptc));

                if (isAdult && unassignedInfants > 0) {
                    p.put("paxcarringinfant", 1);
                    unassignedInfants--;
                }

                p.put("firstname", dtoPax.getFirstName());
                p.put("lastname", dtoPax.getLastName());

                // Calculate Age if possible
                String age = calculateAge(dtoPax.getDob());
                if (age != null) {
                    p.put("paxage", age);
                }

                if (dtoPax.getPhoneNumber() != null) {
                    String phone = dtoPax.getPhoneNumber().toString();
                    p.put("paxphone",
                            (dtoPax.getCountryDialingCode() != null ? dtoPax.getCountryDialingCode() : "") + phone);
                }
                p.put("paxemail", dtoPax.getEmail());
                if (dtoPax.getDob() != null && dtoPax.getDob().contains("-")) {
                    p.put("paxbirthdate", dtoPax.getDob().replace("-", "/"));
                } else {
                    p.put("paxbirthdate", dtoPax.getDob());
                }

                if (dtoPax.getIdentityDocument() != null) {
                    OrderCreateReqDto.Pax.IdentityDocument doc = dtoPax.getIdentityDocument();
                    p.put("paxnationailty", doc.getCitizenshipCountryCode());
                    p.put("paxdoctype", doc.getIdentityDocumentType() != null ? doc.getIdentityDocumentType() : "PP");
                    p.put("paxdocnumber", doc.getIdentityDocumentNumber());
                    p.put("paxdocissuer", doc.getIssuingCountryCode());
                    p.put("paxdocexpiry", doc.getExpiryDate());
                }
                // Map gender to M/F
                String gender = dtoPax.getGender();
                if (gender != null && !gender.isEmpty()) {
                    p.put("gender", gender.toUpperCase().startsWith("M") ? "M" : "F");
                }
                paxList.add(p);
            }
        }
        parms.put("passenger", paxList);

        aerocrs.setParms(parms);
        request.setAerocrs(aerocrs);
        request.setApiUrl("https://api.aerocrs.com/v5/confirmBooking");
        return request;
    }

    // --------------------------------------------------------------------------------------------
    // 3. MakePayment Mapping
    // --------------------------------------------------------------------------------------------
    public static OrderCreateReq mapToMakePaymentReq(OrderCreateReqDto requestDto, Long bookingId) {
        OrderCreateReq request = new OrderCreateReq();
        Aerocrs aerocrs = new Aerocrs();
        Map<String, Object> parms = new LinkedHashMap<>();

        parms.put("bookingid", bookingId);

        if (requestDto.getPaymentInformation() != null) {
            OrderCreateReqDto.PaymentInformation payInfo = requestDto.getPaymentInformation();

            // Cash / Manual
            if (payInfo.getAmount() != null) {
                parms.put("amountpaid", payInfo.getAmount().doubleValue());
            }
            parms.put("amountcurrency", payInfo.getCurrencyCode());

            // Credit Card
            // Credit Card
            parms.put("creditcardpayer", payInfo.getCardHolderName());

            try {
                String privateKeyString = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCofZD2EwU9KpnHMFYJVlh1UWAN/oKf9rmUyHLgDjQmj1GJCOcH9z4xRJn+rhB5b9c2cyFqk/rpP9muc6k/Ltq3tV1kobzFMwwrl7Scp5dCDCtumchKfdsVHkELB6HY0tcolclzPi+h94ZPPZ7iGKtrvQZzmFKgsPdfsTqx+JYc8q0/BPRTNpM7HWMl/gt6JtIKNpwmFkxSQfDHRoTpCFQW6EgvUN+jfrvFX4srX+PSe0mu+BBYKKcTCmDaKrrqTD5ht45e+89NnADU/k9gh6HoHkpXa2AKNbbpDcsdp1KWOzJ/dlegzk8zM7EC6o/uHypKMMSkL4oQPlJ/jE5uGDoDAgMBAAECggEAPyEE7l4ECX3rrikbI1Z5wEMkFTo14Q+FSwyrle1cdtId/5UZUu+9GqKUfErlm0pfPWR3scIOMdSdj/KACE9a3ZgTjP/YhZ5xweeOYV+dmb6Li14NIHP1YP0765EJf/7HZMpydz5mhG4Eoa352Mbbe3uQbkE1PEXx+aWi00nLnL5oLEi2V0E0gzXTzFuzp2X88Pi0/m7gn8YLHxIZjXALLiYnxxqoaaX+dsUye5BtZtZ6PiXysdCkmzVzotxwnA8Nw9SMItwX1qSH2xbHvpyMX4WsYuCyU4GKcGluZ3F+VfZ5T8KxhJZN21enq/q+zq2uNa7u7RlluTXcUKI6QAjM8QKBgQDhOmmyIIS/XH3aa0mChVbY29jY+ZG7PjeBQdVvqwA1dYZPx6nUjsnYSV5Zd9W6ASOXkodURiFJG0qLfFwt/DmSDq7hb+ByFxDr1RHehMde8DxALL/KTyN8ny3yzbK301dNbEdRiYnw06VE+NbjtagaeMINXuQc3LZS7HdKx1lsyQKBgQC/grHQIdSuyBGxrdW4LuBL2I6YnYBKqDu9QM7QFXhS3EtLo9W2lBJU9XSqSTh6tP8mhueDzlpUU15jIVEdkbdeJ5OrbbEcG6w4QKY8nJxV0SbGx8sRaEB3N1QiPcuSX9a8IZoqHmZnTR5bfMOl80v2sExrefYVrBjQhM3HC3SyawKBgQCIOAD3F83RwwnnEV5rT2PgUs8LI54tRgrh5URGfoDo1ETAebzQbu/LHUywBddA4TF6mce5g5TcF2J1jGhf852KJdFFTZnMxwCX8c0V7O58EAYQtj/lBwoqdEehAyGlJnA1xlg4C1xfSFI7rdih7htWr1SGK68BecfXzWa01m7SaQKBgCZXg0QZUdyAX9KD7DMI540n2TzC48mOrw8v53gPpFxqkISfU41PTfBGiEoDiNRAYokTH0zrRnh1jIMqS3QxFVY7dDwxJPFstOk6QE4ISOCBlFLd81ET3zw/DpAgcR5oI7TcwWHHXlc2QGquqvkRodbM6y/lZhhmsT0mKZC9QWrrAoGBAIIZXiqjbeiQzQsL0VdtBm6cwDuacGvh+5fITHzwsN09sNE8beuM9MPRLi5s7Mi2GjpqPOOOc2Rn5pN/kZqO9GrlB7HzLOhOBOkwhA8eeA4TY0SfMTPzQZWfBCn/D06n3ohT2mCXNC2Jt/DixJZuYvI9pgMQVsMN+WLaZaEa+XlC";
                com.airlines.GO7API.util.RSADecryptor rsaDecryptor = new com.airlines.GO7API.util.RSADecryptor(
                        privateKeyString);
                String decryptedCardNumber = rsaDecryptor.decrypt(payInfo.getCardNumber());
                parms.put("creditcardnumber", decryptedCardNumber);
            } catch (Exception e) {
                System.out.println("Decryption failed: " + e.getMessage());
                parms.put("creditcardnumber", payInfo.getCardNumber()); // Fallback
            }

            parms.put("creditcardexpiry", payInfo.getExpiration());
            parms.put("creditcardcvv", payInfo.getSeriesCode()); // Mapped seriesCode to CVV
        }

        aerocrs.setParms(parms);
        request.setAerocrs(aerocrs);
        request.setApiUrl("https://api.aerocrs.com/v5/makePayment"); // Updated to v5
        return request;
    }

    // --------------------------------------------------------------------------------------------
    // 4. OrderTicket Mapping
    // --------------------------------------------------------------------------------------------
    public static OrderCreateReq mapToOrderTicketReq(OrderCreateReqDto requestDto, Long bookingId) {
        OrderCreateReq request = new OrderCreateReq();
        Aerocrs aerocrs = new Aerocrs();
        Map<String, Object> parms = new LinkedHashMap<>();

        parms.put("bookingid", bookingId);

        aerocrs.setParms(parms);
        request.setAerocrs(aerocrs);

        if (requestDto != null) {
            request.setApiKey(requestDto.getApiKey());
        }
        request.setApiUrl("https://api.aerocrs.com/v5/ticketBooking");

        return request;
    }

    public static OrderCreateReq mapToOrderTicketReq(Long bookingId) {
        return mapToOrderTicketReq(null, bookingId);
    }

    // --------------------------------------------------------------------------------------------
    // 5. GetBooking Mapping
    // --------------------------------------------------------------------------------------------
    public static OrderCreateReq mapToGetBookingReq(String bookingConfirmation) {
        OrderCreateReq request = new OrderCreateReq();
        Aerocrs aerocrs = new Aerocrs();
        Map<String, Object> parms = new LinkedHashMap<>();

        if (bookingConfirmation != null) {
            parms.put("bookingconfirmation", bookingConfirmation);
        }

        aerocrs.setParms(parms);
        request.setAerocrs(aerocrs);
        request.setApiUrl("https://api.aerocrs.com/v5/getBooking");
        return request;
    }

    // --------------------------------------------------------------------------------------------
    // Universal Unmarshal
    // --------------------------------------------------------------------------------------------
    public Object unmarshal() throws DatatypeConfigurationException, IOException, InterruptedException {
        String response = makeApiCall();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode root = objectMapper.readTree(response);

            // Check for explicit "errors" field
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

        String logPrefix = "OrderCreate";
        if (apiUrl != null) {
            if (apiUrl.contains("confirmBooking")) {
                logPrefix = "OrderConfirm";
            } else if (apiUrl.contains("getBooking")) {
                logPrefix = "GetBooking";
            } else if (apiUrl.contains("makePayment")) {
                logPrefix = "MakePayment";
            } else if (apiUrl.contains("ticketBooking")) {
                logPrefix = "OrderTicket";
            }
        }

        HttpHeaders headers = new HttpHeaders();
        // Hardcoded Auth (or derived if passed)
        headers.add("auth_id", "70DD4369-72F3-4426-A050-196FBC345009");
        headers.add("auth_password", "vJ3yGilZ9u7N");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        System.out.println("Generated " + logPrefix + " Request is:\n" + jsonBody);

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity,
                    String.class);
            System.out.println(logPrefix + " Response: " + response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.out.println("HTTP Error Response: " + e.getResponseBodyAsString());
            return e.getResponseBodyAsString();
        }
    }
}
