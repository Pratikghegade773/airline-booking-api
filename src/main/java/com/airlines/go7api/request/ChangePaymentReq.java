package com.airlines.go7api.request;

import com.airlines.go7api.error.ErrorRsp;
import com.airlines.go7api.requestdto.ChangePaymentReqDto;
import com.airlines.go7api.util.RSADecryptor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Data;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ChangePaymentReq extends BaseGo7Req {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @JsonIgnore
    public String apiUrl = "https://api.aerocrs.com/v4/makePayment"; // Updated to v5 to match OrderCreateReq logic

    @JsonIgnore
    private String apiKey; // For OrderTicket

    @Data
    public static class Aerocrs {
        @JsonProperty("parms")
        private Map<String, Object> parms;
    }

    public static ChangePaymentReq mapToChangePaymentReq(ChangePaymentReqDto dto) {
        ChangePaymentReq request = new ChangePaymentReq();
        Aerocrs aerocrs = new Aerocrs();
        Map<String, Object> parms = new LinkedHashMap<>();

        // Map Booking ID
        if (dto.getOrderId() != null) {
            try {
                parms.put("bookingid", Long.parseLong(dto.getOrderId()));
            } catch (NumberFormatException e) {
                // If strict Long is needed but String provided.
                // User sample has 66243880 (number).
                // Or try parsing, fallback to string if needed but bookingid usually Long.
                parms.put("bookingid", dto.getOrderId());
            }

        }

        if (dto.getPaymentInformation() != null) {
            ChangePaymentReqDto.PaymentInformation payInfo = dto.getPaymentInformation();

            // Amount Paid
            if (payInfo.getAmount() != null) {
                // User sample: 148.35. Previous logic x100?
                // If sample is 148.35, it's likely double value.
                // Let's use doubleValue() without multiplier if the sample implies raw amount.
                // NOTE: If Go7 expects cents, it would be integer. 148.35 implies
                // decimal/standard unit.
                parms.put("amountpaid", payInfo.getAmount().doubleValue());
            }

            parms.put("amountcurrency", payInfo.getCurrencyCode() != null ? payInfo.getCurrencyCode() : "USD");

            // Credit Card Info
            parms.put("creditcardpayer", payInfo.getCardHolderName());

            // Decryption
            if (payInfo.getCardNumber() != null) {
                try {
                    String privateKeyString = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCofZD2EwU9KpnHMFYJVlh1UWAN/oKf9rmUyHLgDjQmj1GJCOcH9z4xRJn+rhB5b9c2cyFqk/rpP9muc6k/Ltq3tV1kobzFMwwrl7Scp5dCDCtumchKfdsVHkELB6HY0tcolclzPi+h94ZPPZ7iGKtrvQZzmFKgsPdfsTqx+JYc8q0/BPRTNpM7HWMl/gt6JtIKNpwmFkxSQfDHRoTpCFQW6EgvUN+jfrvFX4srX+PSe0mu+BBYKKcTCmDaKrrqTD5ht45e+89NnADU/k9gh6HoHkpXa2AKNbbpDcsdp1KWOzJ/dlegzk8zM7EC6o/uHypKMMSkL4oQPlJ/jE5uGDoDAgMBAAECggEAPyEE7l4ECX3rrikbI1Z5wEMkFTo14Q+FSwyrle1cdtId/5UZUu+9GqKUfErlm0pfPWR3scIOMdSdj/KACE9a3ZgTjP/YhZ5xweeOYV+dmb6Li14NIHP1YP0765EJf/7HZMpydz5mhG4Eoa352Mbbe3uQbkE1PEXx+aWi00nLnL5oLEi2V0E0gzXTzFuzp2X88Pi0/m7gn8YLHxIZjXALLiYnxxqoaaX+dsUye5BtZtZ6PiXysdCkmzVzotxwnA8Nw9SMItwX1qSH2xbHvpyMX4WsYuCyU4GKcGluZ3F+VfZ5T8KxhJZN21enq/q+zq2uNa7u7RlluTXcUKI6QAjM8QKBgQDhOmmyIIS/XH3aa0mChVbY29jY+ZG7PjeBQdVvqwA1dYZPx6nUjsnYSV5Zd9W6ASOXkodURiFJG0qLfFwt/DmSDq7hb+ByFxDr1RHehMde8DxALL/KTyN8ny3yzbK301dNbEdRiYnw06VE+NbjtagaeMINXuQc3LZS7HdKx1lsyQKBgQC/grHQIdSuyBGxrdW4LuBL2I6YnYBKqDu9QM7QFXhS3EtLo9W2lBJU9XSqSTh6tP8mhueDzlpUU15jIVEdkbdeJ5OrbbEcG6w4QKY8nJxV0SbGx8sRaEB3N1QiPcuSX9a8IZoqHmZnTR5bfMOl80v2sExrefYVrBjQhM3HC3SyawKBgQCIOAD3F83RwwnnEV5rT2PgUs8LI54tRgrh5URGfoDo1ETAebzQbu/LHUywBddA4TF6mce5g5TcF2J1jGhf852KJdFFTZnMxwCX8c0V7O58EAYQtj/lBwoqdEehAyGlJnA1xlg4C1xfSFI7rdih7htWr1SGK68BecfXzWa01m7SaQKBgCZXg0QZUdyAX9KD7DMI540n2TzC48mOrw8v53gPpFxqkISfU41PTfBGiEoDiNRAYokTH0zrRnh1jIMqS3QxFVY7dDwxJPFstOk6QE4ISOCBlFLd81ET3zw/DpAgcR5oI7TcwWHHXlc2QGquqvkRodbM6y/lZhhmsT0mKZC9QWrrAoGBAIIZXiqjbeiQzQsL0VdtBm6cwDuacGvh+5fITHzwsN09sNE8beuM9MPRLi5s7Mi2GjpqPOOOc2Rn5pN/kZqO9GrlB7HzLOhOBOkwhA8eeA4TY0SfMTPzQZWfBCn/D06n3ohT2mCXNC2Jt/DixJZuYvI9pgMQVsMN+WLaZaEa+XlC";
                    RSADecryptor rsaDecryptor = new RSADecryptor(privateKeyString);
                    String decryptedCardNumber = rsaDecryptor.decrypt(payInfo.getCardNumber());
                    parms.put("creditcardnumber", decryptedCardNumber);
                } catch (Exception e) {
                    System.out.println("Card Decryption failed: " + e.getMessage());
                    parms.put("creditcardnumber", payInfo.getCardNumber());
                }
            }

            parms.put("creditcardexpiry", payInfo.getExpiration()); // Assuming MMYY format passed directly
            parms.put("creditcardcvv", payInfo.getSeriesCode());
        }

        aerocrs.setParms(parms);
        request.setAerocrs(aerocrs);
        request.setApiUrl("https://secure.aerocrs.com/v4/makePayment");


        return request;
    }

    // --------------------------------------------------------------------------------------------
    // OrderTicket Mapping (For ChangePayment Flow)
    // --------------------------------------------------------------------------------------------
    public static ChangePaymentReq mapToOrderTicketReq(Long bookingId) {
        ChangePaymentReq request = new ChangePaymentReq();
        Aerocrs aerocrs = new Aerocrs();
        Map<String, Object> parms = new LinkedHashMap<>();

        parms.put("bookingid", bookingId);

        aerocrs.setParms(parms);
        request.setAerocrs(aerocrs);
        request.setApiUrl("https://api.aerocrs.com/v5/ticketBooking");

        return request;
    }

    // --------------------------------------------------------------------------------------------
    // GetBooking Mapping (For ChangePayment Flow)
    // --------------------------------------------------------------------------------------------
    public static ChangePaymentReq mapToGetBookingReq(String bookingConfirmation) {
        return mapToGetBookingReq(bookingConfirmation, null);
    }

    public static ChangePaymentReq mapToGetBookingReq(String bookingConfirmation, String lastName) {
        ChangePaymentReq request = new ChangePaymentReq();
        Aerocrs aerocrs = new Aerocrs();
        Map<String, Object> parms = new LinkedHashMap<>();

        if (bookingConfirmation != null) {
            parms.put("bookingconfirmation", bookingConfirmation);
        }
        if (lastName != null && !lastName.isEmpty()) {
            parms.put("passengerlastname", lastName);
        }

        aerocrs.setParms(parms);
        request.setAerocrs(aerocrs);
        request.setApiUrl("https://api.aerocrs.com/v5/getBooking");
        return request;
    }

    @Override
    protected String getApiUrl() {
        return apiUrl != null ? apiUrl : "";
    }

    @Override
    protected String getRequestName() {
        return "ChangePayment";
    }
}
