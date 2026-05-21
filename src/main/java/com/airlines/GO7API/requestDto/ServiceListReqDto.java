package com.airlines.go7api.requestdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceListReqDto {

    private String orderId;
    private String agencyId;
    private String agentId;
    private String apiKey;
    private String serviceListUrl;
    private String agencyName;
}

