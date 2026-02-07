package com.airlines.GO7API.requestDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelReqDto {

    private String offerId;
    private String orderId;
    private String agencyId;
    private String agentId;
    private String apiKey;
    private String cancelUrl;
}
