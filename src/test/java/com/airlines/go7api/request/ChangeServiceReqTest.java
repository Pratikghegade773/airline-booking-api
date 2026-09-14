package com.airlines.go7api.request;

import com.airlines.go7api.requestdto.ChangeServiceReqDto;
import com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChangeServiceReqTest {

    @Test
    void testMapToChangeServiceReq() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ChangeServiceReqDto dto = mapper.readValue(new File("src/test/resources/change_service_request.json"), ChangeServiceReqDto.class);
        OrderRetrieveRspGo7Dto bookingRsp = mapper.readValue(new File("src/test/resources/order_retrieve_response_mock.json"), OrderRetrieveRspGo7Dto.class);
        
        ChangeServiceReq req = ChangeServiceReq.mapToChangeServiceReq(dto, bookingRsp);
        assertNotNull(req);
        assertNotNull(req.getAerocrs());
    }
}
