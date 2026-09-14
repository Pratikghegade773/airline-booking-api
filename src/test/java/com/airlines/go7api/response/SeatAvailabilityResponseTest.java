package com.airlines.go7api.response;

import com.airlines.go7api.responsedto.SeatAvailabilityRspDto;
import com.airlines.go7api.responsego7.SeatAvailabilityRspGo7Dto;
import com.airlines.go7api.responsego7.common.*;
import com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SeatAvailabilityResponseTest {

    @Test
    void testConstructorIsPrivate() throws Exception {
        java.lang.reflect.Constructor<SeatAvailabilityResponse> constructor = SeatAvailabilityResponse.class.getDeclaredConstructor();
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
    void testMapToSeatAvailabilityRspDto_InvalidInputs() {
        // Null response
        SeatAvailabilityRspDto result = SeatAvailabilityResponse.mapToSeatAvailabilityRspDto(null, "ORD123", null);
        assertNotNull(result.getResponseId());

        // Null aerocrs
        SeatAvailabilityRspGo7Dto go7Rsp = new SeatAvailabilityRspGo7Dto();
        result = SeatAvailabilityResponse.mapToSeatAvailabilityRspDto(go7Rsp, "ORD123", null);
        assertNotNull(result.getResponseId());

        // success = false
        Aerocrs aerocrs = new Aerocrs();
        aerocrs.setSuccess(false);
        go7Rsp.setAerocrs(aerocrs);
        result = SeatAvailabilityResponse.mapToSeatAvailabilityRspDto(go7Rsp, "ORD123", null);
        assertNotNull(result.getResponseId());

        // Null seatMapFare
        aerocrs.setSuccess(true);
        result = SeatAvailabilityResponse.mapToSeatAvailabilityRspDto(go7Rsp, "ORD123", null);
        assertNotNull(result.getResponseId());
    }

    @Test
    void testMapToSeatAvailabilityRspDto_HappyPath() {
        SeatAvailabilityRspGo7Dto go7Rsp = new SeatAvailabilityRspGo7Dto();
        Aerocrs aerocrs = new Aerocrs();
        aerocrs.setSuccess(true);
        
        SeatAvailabilityRspGo7Dto.SeatMapFare seatMap = new SeatAvailabilityRspGo7Dto.SeatMapFare();
        seatMap.setActype("A320");
        seatMap.setCurrency("USD");

        // Map classes
        Map<String, SeatAvailabilityRspGo7Dto.SeatClass> classes = new HashMap<>();
        SeatAvailabilityRspGo7Dto.SeatClass seatClass = new SeatAvailabilityRspGo7Dto.SeatClass();
        seatClass.setCabinClass("Y");

        List<SeatAvailabilityRspGo7Dto.PaidSeatRow> paidSeats = new ArrayList<>();
        
        // Exit Row chargeable
        SeatAvailabilityRspGo7Dto.PaidSeatRow row1 = new SeatAvailabilityRspGo7Dto.PaidSeatRow();
        row1.setRowNumber(12);
        row1.setBrandName("Exit Row Extra Legroom");
        row1.setSeatFare(45.0);
        Map<String, String> row1Seats = new LinkedHashMap<>();
        row1Seats.put("12A", "F"); // Window Free -> A
        row1Seats.put("12C", "F"); // Aisle Free -> A
        row1Seats.put("12B", "R"); // Center Reserved -> R
        row1.setSeats(row1Seats);
        paidSeats.add(row1);

        // Standard Row
        SeatAvailabilityRspGo7Dto.PaidSeatRow row2 = new SeatAvailabilityRspGo7Dto.PaidSeatRow();
        row2.setRowNumber(15);
        row2.setBrandName("Standard");
        row2.setSeatFare(0.0);
        Map<String, String> row2Seats = new LinkedHashMap<>();
        row2Seats.put("15F", "F"); // Window Free -> A
        row2Seats.put("15G", "F"); // Window Free -> A (Window)
        row2.setSeats(row2Seats);
        paidSeats.add(row2);

        seatClass.setPaidSeats(paidSeats);
        classes.put("Y", seatClass);
        seatMap.setClasses(classes);
        aerocrs.setSeatMapFare(seatMap);
        go7Rsp.setAerocrs(aerocrs);

        // Booking Retrieve Response
        OrderRetrieveRspGo7Dto bookingRsp = new OrderRetrieveRspGo7Dto();
        Aerocrs bookingAerocrs = new Aerocrs();
        bookingAerocrs.setSuccess(true);
        Booking booking = new Booking();
        booking.setBookingconfirmation("CONF123");
        booking.setPnrref("PNR123");

        // Flight
        Items items = new Items();
        List<Flight> flights = new ArrayList<>();
        Flight flight = new Flight();
        flight.setAirlineICAOcode("G7");
        flights.add(flight);
        items.setFlight(flights);
        booking.setItems(items);

        // Passengers
        Passengers passengers = new Passengers();
        List<Passenger> passengerList = new ArrayList<>();
        Passenger p1 = new Passenger();
        p1.setPaxtype("ADT");
        p1.setPaxtitle("Mr.");
        p1.setFirstname("John");
        p1.setLastname("Doe");
        passengerList.add(p1);

        Passenger p2 = new Passenger();
        p2.setPaxtype("INFANT");
        p2.setPaxtitle("Mstr.");
        p2.setFirstname("Baby");
        p2.setLastname("Doe");
        passengerList.add(p2);

        passengers.setPassenger(passengerList);
        booking.setPassengers(passengers);

        bookingAerocrs.setBooking(booking);
        bookingRsp.setAerocrs(bookingAerocrs);

        // Map DTO
        SeatAvailabilityRspDto result = SeatAvailabilityResponse.mapToSeatAvailabilityRspDto(go7Rsp, "ORD123", bookingRsp);

        assertNotNull(result);
        assertEquals("ORD123", result.getOrderId());
        assertEquals("G7", result.getValidatingCarrier());
        assertEquals("G7", result.getApiOwner());

        assertFalse(result.getOfferItems().isEmpty());
        SeatAvailabilityRspDto.OfferItem item = result.getOfferItems().get(0);
        assertEquals("USD", item.getCurrency());
        assertEquals(2, item.getPaxref().size());
        assertEquals("T1", item.getPaxref().get(0));
        assertEquals("T1.1", item.getPaxref().get(1));

        assertEquals(1, item.getCompartmentList().size());
        SeatAvailabilityRspDto.OfferItem.Compartment comp = item.getCompartmentList().get(0);
        assertEquals("Y", comp.getCabinType());
        assertEquals(BigInteger.valueOf(12), comp.getFirstRow());
        assertEquals(BigInteger.valueOf(15), comp.getLastRow());

        // Validate seats inside compartment
        List<SeatAvailabilityRspDto.OfferItem.Compartment.Seat> seats = comp.getSeat();
        assertNotNull(seats);
        assertEquals(5, seats.size());

        // "12A" - chargeable, window, exit, free
        SeatAvailabilityRspDto.OfferItem.Compartment.Seat seat12A = seats.get(0);
        assertEquals("12", seat12A.getRowNumber());
        assertEquals("A", seat12A.getColumId());
        assertEquals("A", seat12A.getOccupancyCode());
        assertEquals(BigDecimal.valueOf(45.0), seat12A.getBaseFare().getAmount());
        
        boolean hasExit = seat12A.getSeatCharacteristics().stream().anyMatch(c -> "EX".equals(c.getCode()));
        boolean hasWindow = seat12A.getSeatCharacteristics().stream().anyMatch(c -> "W".equals(c.getCode()));
        boolean hasChargeable = seat12A.getSeatCharacteristics().stream().anyMatch(c -> "CH".equals(c.getCode()));
        assertTrue(hasExit);
        assertTrue(hasWindow);
        assertTrue(hasChargeable);
    }
}
