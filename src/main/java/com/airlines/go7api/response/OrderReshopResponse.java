package com.airlines.go7api.response;

import com.airlines.go7api.requestdto.AirshopReqDto;
import com.airlines.go7api.responsedto.AirshopRspDto;
import com.airlines.go7api.responsedto.OrderReshopRspDto;
import com.airlines.go7api.responsego7.AirshopRspGo7Dto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderReshopResponse {

    private final AirshopResponse airshopResponse;

    @Autowired
    public OrderReshopResponse(AirshopResponse airshopResponse) {
        this.airshopResponse = airshopResponse;
    }

    public OrderReshopRspDto orderReshopMapper(AirshopRspGo7Dto response,
                                               AirshopReqDto request) {
        
        // Delegate to AirshopResponse to reuse the complex mapping logic
        AirshopRspDto airshopRspDto = airshopResponse.airshoppingMapper(response, request);

        if (airshopRspDto == null) {
            return new OrderReshopRspDto();
        }

        // Convert the mapped DTO to the expected OrderReshopRspDto structure using Jackson
        // This eliminates over 400 lines of duplicated code and resolves SonarQube CPD warnings.
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(airshopRspDto, OrderReshopRspDto.class);
    }
}
