package com.airlines.go7api.request;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChangePaymentReqTest {

    @Test
    void testMapToOrderTicketReq() {
        ChangePaymentReq req = ChangePaymentReq.mapToOrderTicketReq(12345L);
        assertNotNull(req);
    }
}
