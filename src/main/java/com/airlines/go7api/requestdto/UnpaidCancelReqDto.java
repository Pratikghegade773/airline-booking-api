package com.airlines.go7api.requestdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UnpaidCancelReqDto {

    private String offerId;
    private String orderId;
    private String agencyId;
    private String agentId;
    private String apiKey;
    private String cancelUrl;
    private String agencyName;
}

