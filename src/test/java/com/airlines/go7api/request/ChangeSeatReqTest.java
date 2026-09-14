package com.airlines.go7api.request;

import com.airlines.go7api.requestdto.ChangeSeatReqDto;
import com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChangeSeatReqTest {

    @Test
    void testMapToChangeSeatReq_HappyPath() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ChangeSeatReqDto dto = mapper.readValue(new File("src/test/resources/change_seat_request.json"), ChangeSeatReqDto.class);
        OrderRetrieveRspGo7Dto bookingRsp = mapper.readValue(new File("src/test/resources/order_retrieve_response_mock.json"), OrderRetrieveRspGo7Dto.class);
        
        ChangeSeatReq req = ChangeSeatReq.mapToChangeSeatReq(dto, bookingRsp);
        assertNotNull(req);
        assertNotNull(req.getAerocrs());
    }
}
