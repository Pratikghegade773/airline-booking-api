package com.airlines.go7api.response;

import com.airlines.go7api.responsedto.UnpaidCancelRspDto;
import com.airlines.go7api.responsedto.common.PaxDetailDTO;
import com.airlines.go7api.responsego7.UnpaidCancelRspGo7Dto;
import com.airlines.go7api.responsego7.common.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class UnpaidCancelResponseTest {

    @Test
    void testConstructorIsPrivate() throws Exception {
        java.lang.reflect.Constructor<UnpaidCancelResponse> constructor = UnpaidCancelResponse.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail("Expected InvocationTargetException wrapping IllegalStateException");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertEquals("Utility class", e.getCause().getMessage());
        }
    }

    @Test
    void testGenerateResponse_InvalidInputs() {
        UnpaidCancelRspDto result = UnpaidCancelResponse.generateResponse(null);
        assertNotNull(result);
        assertNull(result.getOrderId());

        UnpaidCancelRspGo7Dto go7Rsp = new UnpaidCancelRspGo7Dto();
        result = UnpaidCancelResponse.generateResponse(go7Rsp);
        assertNotNull(result);
        assertNull(result.getOrderId());

        Aerocrs aerocrs = new Aerocrs();
        go7Rsp.setAerocrs(aerocrs);
        result = UnpaidCancelResponse.generateResponse(go7Rsp);
        assertNotNull(result);
        assertNull(result.getOrderId());
    }

    @Test
    void testGenerateResponse_HappyPath() {
        UnpaidCancelRspGo7Dto go7Rsp = new UnpaidCancelRspGo7Dto();
        Aerocrs aerocrs = new Aerocrs();
        aerocrs.setSuccess(true);

        Booking booking = new Booking();
        booking.setBookingconfirmation("CONF123");
        booking.setPnrref("PNR999");
        booking.setBookingid(12345L);

        // Passengers
        Passengers passengers = new Passengers();
        List<Passenger> passengerList = new ArrayList<>();
        
        Passenger p1 = new Passenger();
        p1.setPaxtype("ADT");
        p1.setPaxtitle("Mr.");
        p1.setFirstname("John");
        p1.setLastname("Doe");
        p1.setContact("123456789");
        p1.setEmail("john.doe@test.com");
        passengerList.add(p1);

        Passenger p2 = new Passenger();
        p2.setPaxtype("INFANT");
        p2.setPaxtitle("Mstr.");
        p2.setFirstname("Baby");
        p2.setLastname("Doe");
        passengerList.add(p2);

        Passenger p3 = new Passenger();
        p3.setPaxtype("CHILD");
        p3.setPaxtitle("Miss");
        p3.setFirstname("Kid");
        p3.setLastname("Doe");
        passengerList.add(p3);

        passengers.setPassenger(passengerList);
        booking.setPassengers(passengers);

        aerocrs.setBooking(booking);
        go7Rsp.setAerocrs(aerocrs);

        UnpaidCancelRspDto result = UnpaidCancelResponse.generateResponse(go7Rsp);

        assertNotNull(result);
        assertEquals("CONF123", result.getOrderId());
        assertEquals("PNR999", result.getPnr());
        assertEquals("X", result.getStatusCode());
        assertEquals("G7", result.getApiOwner());
        assertEquals("G7", result.getValidatingCarrier());

        List<PaxDetailDTO> paxList = result.getPaxDetailList();
        assertEquals(3, paxList.size());
        
        PaxDetailDTO adtPax = paxList.get(0);
        assertEquals("T1", adtPax.getPaxId());
        assertEquals("ADT", adtPax.getPtc());
        assertEquals("MALE", adtPax.getGender());
        assertEquals("john.doe@test.com".toUpperCase(), adtPax.getEmails().get(0).getEmailAddress());

        PaxDetailDTO infPax = paxList.get(1);
        assertEquals("T1.1", infPax.getPaxId());
        assertEquals("INF", infPax.getPtc());
        
        PaxDetailDTO chdPax = paxList.get(2);
        assertEquals("T2", chdPax.getPaxId());
        assertEquals("CHD", chdPax.getPtc());
    }
}
