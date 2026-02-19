package com.airlines.GO7API.requestDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatAvailabilityReqDto {

    private String orderId;
    private String agencyId;
    private String agentId;
    private String apiKey;
    private String seatAvailabilityUrl;

}
