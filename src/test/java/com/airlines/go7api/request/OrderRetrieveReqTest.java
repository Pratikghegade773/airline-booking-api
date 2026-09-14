package com.airlines.go7api.request;

import com.airlines.go7api.requestdto.OrderRetrieveReqDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderRetrieveReqTest {

    @Test
    void testGettersSettersAndMetadata() {
        OrderRetrieveReq req = new OrderRetrieveReq();
        req.setApiKey("retrieve-key");
        assertEquals("retrieve-key", req.getApiKey());

        req.setOrderRetrieveUrl("https://custom.com/retrieve");
        assertEquals("https://custom.com/retrieve", req.getOrderRetrieveUrl());
        assertEquals("https://custom.com/retrieve", req.getApiUrl());
        
        req.setOrderRetrieveUrl(null);
        assertEquals("", req.getApiUrl());

        assertEquals("OrderRetrieve", req.getRequestName());

        OrderRetrieveReq.Aerocrs aerocrs = new OrderRetrieveReq.Aerocrs();
        OrderRetrieveReq.Parms parms = new OrderRetrieveReq.Parms();
        
        parms.setBookingConfirmation("ABCDEF");
        assertEquals("ABCDEF", parms.getBookingConfirmation());

        parms.setBookingId(12345L);
        assertEquals(12345L, parms.getBookingId());

        parms.setPassengerLastName("Smith");
        assertEquals("Smith", parms.getPassengerLastName());

        parms.setGenerateBookingId(true);
        assertTrue(parms.getGenerateBookingId());

        aerocrs.setParms(parms);
        assertNotNull(aerocrs.getParms());

        req.setAerocrs(aerocrs);
        assertNotNull(req.getAerocrs());
    }

    @Test
    void testMapToOrderRetrieveReq_NumericBookingId() {
        OrderRetrieveReqDto dto = new OrderRetrieveReqDto();
        dto.setApiKey("my-key");
        dto.setOrderId("9876543"); // Numeric Order ID -> Booking ID
        dto.setSurname("Smith");
        dto.setRetrieveUrl("https://retrieve.url");

        OrderRetrieveReq req = OrderRetrieveReq.mapToOrderRetrieveReq(dto, null);

        assertNotNull(req);
        assertEquals("my-key", req.getApiKey());
        assertEquals("https://retrieve.url", req.getApiUrl());

        var parms = req.getAerocrs().getParms();
        assertEquals(9876543L, parms.getBookingId());
        assertNull(parms.getBookingConfirmation());
        assertEquals("Smith", parms.getPassengerLastName());
    }

    @Test
    void testMapToOrderRetrieveReq_StringConfirmation() {
        OrderRetrieveReqDto dto = new OrderRetrieveReqDto();
        dto.setPnr("ABCDEF"); // String PNR -> Booking Confirmation
        dto.setApiUrl("https://api.url");

        OrderRetrieveReq req = OrderRetrieveReq.mapToOrderRetrieveReq(dto, null);

        assertNotNull(req);
        assertEquals("https://api.url", req.getApiUrl());

        var parms = req.getAerocrs().getParms();
        assertNull(parms.getBookingId());
        assertEquals("ABCDEF", parms.getBookingConfirmation());
    }

    @Test
    void testMapToOrderRetrieveReq_StoredConfirmationOverride() {
        OrderRetrieveReqDto dto = new OrderRetrieveReqDto();
        dto.setPnr("ABCDEF");
        dto.setOrderId("9999");

        // Stored confirmation takes highest priority
        OrderRetrieveReq req = OrderRetrieveReq.mapToOrderRetrieveReq(dto, "XYZ789");

        assertNotNull(req);
        assertEquals("https://api.aerocrs.com/v5/getBooking", req.getApiUrl()); // default URL

        var parms = req.getAerocrs().getParms();
        assertEquals("XYZ789", parms.getBookingConfirmation());
        assertNull(parms.getBookingId());
    }

    @Test
    void testMapToOrderRetrieveReq_NullDto() {
        OrderRetrieveReq req = OrderRetrieveReq.mapToOrderRetrieveReq(null, "8888");

        assertNotNull(req);
        assertEquals("https://api.aerocrs.com/v5/getBooking", req.getApiUrl());

        var parms = req.getAerocrs().getParms();
        assertEquals(8888L, parms.getBookingId()); // Stored confirmation is numeric
        assertNull(parms.getBookingConfirmation());
    }

    @Test
    void testMapToOrderRetrieveReq_NumberFormatExceptionBlock() {
        OrderRetrieveReqDto dto = new OrderRetrieveReqDto();
        // Generates numeric PNR that exceeds Long.MAX_VALUE to trigger NumberFormatException in parsing
        dto.setPnr("99999999999999999999999999999999999"); 

        OrderRetrieveReq req = OrderRetrieveReq.mapToOrderRetrieveReq(dto, null);

        assertNotNull(req);
        var parms = req.getAerocrs().getParms();
        assertNull(parms.getBookingId());
        assertEquals("99999999999999999999999999999999999", parms.getBookingConfirmation());
    }
}
