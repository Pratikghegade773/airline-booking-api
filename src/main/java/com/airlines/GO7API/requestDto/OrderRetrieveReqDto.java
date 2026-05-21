package com.airlines.go7api.requestdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
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
    private String agencyName;

}

