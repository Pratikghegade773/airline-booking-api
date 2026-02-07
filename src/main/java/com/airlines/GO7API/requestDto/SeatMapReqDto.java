package com.airlines.GO7API.requestDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatMapReqDto {

    private String orderId;
    private String agencyId;
    private String agentId;
    private String apiKey;
    private String seatAvailabilityUrl;

    private String companyCode;
    private String flightNumber;
    private String flightDate;
    private String fromCode;
    private String toCode;
}
