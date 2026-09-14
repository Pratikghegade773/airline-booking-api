package com.airlines.go7api.request;

import com.airlines.go7api.requestdto.SeatAvailabilityReqDto;
import com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto;
import com.airlines.go7api.responsego7.common.Booking;
import com.airlines.go7api.responsego7.common.Flight;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SeatAvailabilityReqTest {

    @Test
    void testGettersSettersAndMetadata() {
        SeatAvailabilityReq req = new SeatAvailabilityReq();
        req.setApiKey("api-key-123");
        assertEquals("api-key-123", req.getApiKey());

        req.setSeatAvailabilityUrl("https://seat.com/api");
        assertEquals("https://seat.com/api", req.getSeatAvailabilityUrl());
        assertEquals("https://seat.com/api", req.getApiUrl());

        req.setSeatAvailabilityUrl(null);
        assertEquals("", req.getApiUrl());

        assertEquals("SeatAvailability", req.getRequestName());

        SeatAvailabilityReq.Aerocrs aerocrs = new SeatAvailabilityReq.Aerocrs();
        SeatAvailabilityReq.Parms parms = new SeatAvailabilityReq.Parms();
        
        parms.setBookingId(12345L);
        assertEquals(12345L, parms.getBookingId());

        parms.setCompanyCode("XYZ");
        assertEquals("XYZ", parms.getCompanyCode());

        parms.setFlightNumber("AA100");
        assertEquals("AA100", parms.getFlightNumber());

        parms.setFlightDate("2026-06-01");
        assertEquals("2026-06-01", parms.getFlightDate());

        parms.setFromCode("JFK");
        assertEquals("JFK", parms.getFromCode());

        parms.setToCode("LAX");
        assertEquals("LAX", parms.getToCode());

        aerocrs.setParms(parms);
        assertNotNull(aerocrs.getParms());

        req.setAerocrs(aerocrs);
        assertNotNull(req.getAerocrs());
    }

    @Test
    void testMapToSeatAvailabilityRequestDTO_HappyPathWithFlights() {
        SeatAvailabilityReqDto dto = new SeatAvailabilityReqDto();
        dto.setApiKey("test-api-key");
        dto.setOrderId("98765");
        dto.setSeatAvailabilityUrl("https://custom.com/seats");

        OrderRetrieveRspGo7Dto bookingRsp = new OrderRetrieveRspGo7Dto();
        com.airlines.go7api.responsego7.common.Aerocrs aerocrsCommon = new com.airlines.go7api.responsego7.common.Aerocrs();
        Booking booking = new Booking();
        
        com.airlines.go7api.responsego7.common.Flights flights = new com.airlines.go7api.responsego7.common.Flights();
        List<Flight> flightList = new ArrayList<>();
        Flight flight1 = new Flight();
        flight1.setNumber("QA300");
        flight1.setFlightdate("2026-06-01");
        flight1.setFromcode("DOH");
        flight1.setTocode("LHR");
        flightList.add(flight1);
        flights.setFlight(flightList);
        booking.setFlights(flights);

        aerocrsCommon.setBooking(booking);
        bookingRsp.setAerocrs(aerocrsCommon);

        SeatAvailabilityReq req = SeatAvailabilityReq.mapToSeatAvailabilityRequestDTO(dto, bookingRsp);

        assertNotNull(req);
        assertEquals("test-api-key", req.getApiKey());
        assertEquals("https://custom.com/seats", req.getSeatAvailabilityUrl());

        var parms = req.getAerocrs().getParms();
        assertEquals(98765L, parms.getBookingId());
        assertEquals("API", parms.getCompanyCode());
        assertEquals("QA300", parms.getFlightNumber());
        assertEquals("2026-06-01", parms.getFlightDate());
        assertEquals("DOH", parms.getFromCode());
        assertEquals("LHR", parms.getToCode());
    }

    @Test
    void testMapToSeatAvailabilityRequestDTO_HappyPathWithItems() {
        SeatAvailabilityReqDto dto = new SeatAvailabilityReqDto();
        dto.setApiKey("test-api-key");
        dto.setOrderId("abc"); // Invalid numeric order ID to trigger exception block

        OrderRetrieveRspGo7Dto bookingRsp = new OrderRetrieveRspGo7Dto();
        com.airlines.go7api.responsego7.common.Aerocrs aerocrsCommon = new com.airlines.go7api.responsego7.common.Aerocrs();
        Booking booking = new Booking();
        
        com.airlines.go7api.responsego7.common.Items items = new com.airlines.go7api.responsego7.common.Items();
        List<Flight> flightList = new ArrayList<>();
        Flight flight1 = new Flight();
        flight1.setNumber("QA400");
        flight1.setFlightdate("2026-06-02");
        flight1.setFromcode("LHR");
        flight1.setTocode("JFK");
        flightList.add(flight1);
        items.setFlight(flightList);
        booking.setItems(items);

        aerocrsCommon.setBooking(booking);
        bookingRsp.setAerocrs(aerocrsCommon);

        SeatAvailabilityReq req = SeatAvailabilityReq.mapToSeatAvailabilityRequestDTO(dto, bookingRsp);

        assertNotNull(req);
        var parms = req.getAerocrs().getParms();
        assertNull(parms.getBookingId()); // should remain null due to NumberFormatException
        assertEquals("QA400", parms.getFlightNumber());
        assertEquals("2026-06-02", parms.getFlightDate());
        assertEquals("LHR", parms.getFromCode());
        assertEquals("JFK", parms.getToCode());
    }

    @Test
    void testMapToGetBookingReq() {
        OrderRetrieveReq req = SeatAvailabilityReq.mapToGetBookingReq("ABCDEF");
        assertNotNull(req);
        assertEquals("8d123dcd262ad942852233f81e649089", req.getApiKey());
        assertEquals("https://api.aerocrs.com/v5/getBooking", req.getApiUrl());
        assertEquals("ABCDEF", req.getAerocrs().getParms().getBookingConfirmation());
    }
}
