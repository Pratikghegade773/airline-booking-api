package com.airlines.go7api.request;

import com.airlines.go7api.requestdto.common.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.airlines.go7api.error.ErrorRsp;
import com.airlines.go7api.requestdto.OrderCreateReqDto;
import lombok.Data;
import org.springframework.http.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderCreateReq extends BaseGo7Req {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OrderCreateReq.class);

    private static final String PTC_INFANT = "INFANT";
    private static final String CREATE_BOOKING_URL = "https://api.aerocrs.com/v5/createBooking";
    private static final String PARM_BOOKING_ID = "bookingid";
    private static final String PARM_PAX_TITLE = "paxtitle";

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @JsonIgnore
    private String apiUrl = CREATE_BOOKING_URL; // Default

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

        List<Map<String, Object>> bookflightList = parseBookFlights(orderCreateRQ.getOfferId());
        int[] counts = calculatePaxCounts(orderCreateRQ.getPassengers());

        parms.put("triptype", bookflightList.size() > 1 ? "RT" : "OW");
        parms.put("adults", counts[0]);
        parms.put("child", counts[1]);
        parms.put("infant", counts[2]);
        parms.put("bookflight", bookflightList);
        parms.put("passenger", mapPassengers(orderCreateRQ.getPassengers()));

        // Agent/User
        parms.put("useremail", "apiconnector@go7.com"); // Placeholder/Default
        parms.put("agencypassword", "apiconnector");

        aerocrs.setParms(parms);
        request.setAerocrs(aerocrs);
        request.setApiUrl(CREATE_BOOKING_URL);
        return request;
    }

    private static List<Map<String, Object>> parseBookFlights(String offerId) {
        List<Map<String, Object>> bookflightList = new ArrayList<>();
        if (offerId == null) {
            return bookflightList;
        }

        String[] segments = offerId.contains("*") ? offerId.split("\\*") : new String[] { offerId };
        for (String segment : segments) {
            Map<String, Object> flightMap = parseSegment(segment);
            if (!flightMap.isEmpty()) {
                bookflightList.add(flightMap);
            }
        }
        return bookflightList;
    }

    private static Map<String, Object> parseSegment(String segment) {
        String[] parts = segment.split("-");
        if (parts.length < 4) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> flightMap = new LinkedHashMap<>();
        flightMap.put("fromcode", parts[2]);
        flightMap.put("tocode", parts[3]);
        try {
            flightMap.put("flightid", Long.parseLong(parts[0]));
            flightMap.put("fareid", Long.parseLong(parts[1]));
        } catch (NumberFormatException e) {
            flightMap.put("flightid", parts[0]);
            flightMap.put("fareid", parts[1]);
        }
        return flightMap;
    }

    private static int[] calculatePaxCounts(List<PaxReqDto> passengers) {
        int[] counts = {0, 0, 0}; // [adults, child, infant]
        if (passengers == null) {
            counts[0] = 1;
            return counts;
        }

        for (PaxReqDto pax : passengers) {
            incrementPaxCount(counts, pax.getPtc());
        }

        if (counts[0] == 0 && counts[1] == 0 && counts[2] == 0) {
            counts[0] = 1;
        }
        return counts;
    }

    private static void incrementPaxCount(int[] counts, String ptc) {
        String resolvedPtc = ptc != null ? ptc : "ADT";
        if ("CHD".equalsIgnoreCase(resolvedPtc) || "CNN".equalsIgnoreCase(resolvedPtc)) {
            counts[1]++;
        } else if ("INF".equalsIgnoreCase(resolvedPtc) || PTC_INFANT.equalsIgnoreCase(resolvedPtc)) {
            counts[2]++;
        } else {
            counts[0]++;
        }
    }

    private static List<Map<String, Object>> mapPassengers(List<PaxReqDto> passengers) {
        List<Map<String, Object>> passengerList = new ArrayList<>();
        if (passengers != null) {
            for (PaxReqDto dtoPax : passengers) {
                passengerList.add(mapPassenger(dtoPax));
            }
        }
        return passengerList;
    }

    private static Map<String, Object> mapPassenger(PaxReqDto dtoPax) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("firstname", dtoPax.getFirstName());
        p.put("lastname", dtoPax.getLastName());
        p.put("title", mapTitleToId(dtoPax.getTitle()));

        String gender = dtoPax.getGender();
        if (gender != null && !gender.isEmpty()) {
            p.put("gender", gender.toUpperCase().startsWith("M") ? "M" : "F");
        }

        String age = calculateAge(dtoPax.getDob());
        if (age != null) {
            p.put("paxage", age);
        }
        return p;
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

        parms.put(PARM_BOOKING_ID, bookingId);
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
                    .filter(pax -> "INF".equalsIgnoreCase(pax.getPtc()) || PTC_INFANT.equalsIgnoreCase(pax.getPtc()))
                    .count();
        }

        if (requestDto.getPassengers() != null) {
            long[] unassignedInfantsRef = { unassignedInfants };
            for (PaxReqDto dtoPax : requestDto.getPassengers()) {
                paxList.add(mapConfirmPassenger(dtoPax, unassignedInfantsRef));
            }
        }
        parms.put("passenger", paxList);

        aerocrs.setParms(parms);
        request.setAerocrs(aerocrs);
        request.setApiUrl("https://api.aerocrs.com/v5/confirmBooking");
        return request;
    }

    private static Map<String, Object> mapConfirmPassenger(PaxReqDto dtoPax, long[] unassignedInfantsRef) {
        String ptc = dtoPax.getPtc();
        String titleStr = determineTitle(ptc, dtoPax.getTitle());

        Map<String, Object> p = new LinkedHashMap<>();

        formatPaxTitle(p, titleStr);
        handleInfantCarrying(p, ptc, unassignedInfantsRef);
        populatePaxContactAndBio(p, dtoPax);
        populateIdentityDocument(p, dtoPax.getIdentityDocument());
        populatePaxGender(p, dtoPax.getGender());

        return p;
    }

    private static String determineTitle(String ptc, String originalTitle) {
        if ("CHD".equalsIgnoreCase(ptc) || "CNN".equalsIgnoreCase(ptc)) {
            return "Child";
        }
        if ("INF".equalsIgnoreCase(ptc) || PTC_INFANT.equalsIgnoreCase(ptc)) {
            return PTC_INFANT;
        }
        return originalTitle;
    }

    private static void formatPaxTitle(Map<String, Object> p, String titleStr) {
        if ("Child".equals(titleStr) || PTC_INFANT.equals(titleStr)) {
            p.put(PARM_PAX_TITLE, titleStr);
        } else if (titleStr != null && !titleStr.endsWith(".")) {
            p.put(PARM_PAX_TITLE, titleStr + ".");
        } else {
            p.put(PARM_PAX_TITLE, titleStr);
        }
    }

    private static void handleInfantCarrying(Map<String, Object> p, String ptc, long[] unassignedInfantsRef) {
        boolean isAdult = !("CHD".equalsIgnoreCase(ptc) || "CNN".equalsIgnoreCase(ptc)
                || "INF".equalsIgnoreCase(ptc) || PTC_INFANT.equalsIgnoreCase(ptc));

        if (isAdult && unassignedInfantsRef[0] > 0) {
            p.put("paxcarringinfant", 1);
            unassignedInfantsRef[0]--;
        }
    }

    private static void populatePaxContactAndBio(Map<String, Object> p, PaxReqDto dtoPax) {
        p.put("firstname", dtoPax.getFirstName());
        p.put("lastname", dtoPax.getLastName());

        String age = calculateAge(dtoPax.getDob());
        if (age != null) {
            p.put("paxage", age);
        }

        if (dtoPax.getPhoneNumber() != null) {
            String phone = dtoPax.getPhoneNumber().toString();
            String dialCode = dtoPax.getCountryDialingCode() != null ? dtoPax.getCountryDialingCode() : "";
            p.put("paxphone", dialCode + phone);
        }
        p.put("paxemail", dtoPax.getEmail());

        if (dtoPax.getDob() != null && dtoPax.getDob().contains("-")) {
            p.put("paxbirthdate", dtoPax.getDob().replace("-", "/"));
        } else {
            p.put("paxbirthdate", dtoPax.getDob());
        }
    }

    private static void populatePaxGender(Map<String, Object> p, String gender) {
        if (gender != null && !gender.isEmpty()) {
            p.put("gender", gender.toUpperCase().startsWith("M") ? "M" : "F");
        }
    }

    private static void populateIdentityDocument(Map<String, Object> p, PaxReqDto.IdentityDocument doc) {
        if (doc != null) {
            p.put("paxnationailty", doc.getCitizenshipCountryCode());
            p.put("paxdoctype", doc.getIdentityDocumentType() != null ? doc.getIdentityDocumentType() : "PP");
            p.put("paxdocnumber", doc.getIdentityDocumentNumber());
            p.put("paxdocissuer", doc.getIssuingCountryCode());
            p.put("paxdocexpiry", doc.getExpiryDate());
        }
    }

    // --------------------------------------------------------------------------------------------
    // 3. MakePayment Mapping
    // --------------------------------------------------------------------------------------------
    public static OrderCreateReq mapToMakePaymentReq(OrderCreateReqDto requestDto, Long bookingId) {
        OrderCreateReq request = new OrderCreateReq();
        Aerocrs aerocrs = new Aerocrs();
        Map<String, Object> parms = new LinkedHashMap<>();

        parms.put(PARM_BOOKING_ID, bookingId);

        if (requestDto.getPaymentInformation() != null) {
            PaymentInformationReqDto payInfo = requestDto.getPaymentInformation();

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
                com.airlines.go7api.util.RSADecryptor rsaDecryptor = new com.airlines.go7api.util.RSADecryptor(
                        privateKeyString);
                String decryptedCardNumber = rsaDecryptor.decrypt(payInfo.getCardNumber());
                parms.put("creditcardnumber", decryptedCardNumber);
            } catch (Exception e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Decryption failed: {}", e.getMessage());
                }
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

        parms.put(PARM_BOOKING_ID, bookingId);

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

    @Override
    protected String getApiUrl() {
        return apiUrl != null ? apiUrl : CREATE_BOOKING_URL;
    }

    @Override
    protected String getRequestName() {
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
        return logPrefix;
    }
}
