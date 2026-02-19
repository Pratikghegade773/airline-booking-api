package com.airlines.GO7API.requestDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceListReqDto {

    private String orderId;
    private String agencyId;
    private String agentId;
    private String apiKey;
    private String serviceListUrl;
}
