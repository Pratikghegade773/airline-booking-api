package com.airlines.go7api.request;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrderReshopReqTest {

    @Test
    void testMapFromJson() throws IOException {
        String json = "{ \"aerocrs\": { \"shopping\": {} } }";
        OrderReshopReq req = OrderReshopReq.mapFromJson(json);
        assertNotNull(req);
    }
}
