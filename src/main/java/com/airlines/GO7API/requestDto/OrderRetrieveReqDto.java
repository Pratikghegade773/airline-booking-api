package com.airlines.GO7API.requestDto;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class OrderRetrieveReqDto {
    private String countryCode;
    private String apiUrl;
    private String apiKey;
    private String retrieveUrl;

    private int httpResponseCode;

    private String orderId;
    private String pnr;
    private String surname;
    private String ticketNumber;
    private String agencyId;
    private String agentId;

}
