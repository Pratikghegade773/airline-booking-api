package com.airlines.go7api.response;

import com.airlines.go7api.responsedto.ServiceListRspDto;
import com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto;
import com.airlines.go7api.responsego7.ServiceListRspGo7Dto;
import com.airlines.go7api.responsego7.common.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServiceListResponseTest {

    @Test
    void testGenerateResponse_NullOrEmptyInputs() {
        ServiceListResponse serviceListResponse = new ServiceListResponse();

        // 1. Go7Response is null
        ServiceListRspDto response = serviceListResponse.generateResponse(null, null);
        assertNotNull(response);
        assertNull(response.getResponseId());

        // 2. Go7Response.getAerocrs() is null
        ServiceListRspGo7Dto go7Rsp = new ServiceListRspGo7Dto();
        response = serviceListResponse.generateResponse(go7Rsp, null);
        assertNotNull(response);
        assertNull(response.getResponseId());

        // 3. Go7Response.getAerocrs().getAncillaries() is null
        Aerocrs aerocrs = new Aerocrs();
        go7Rsp.setAerocrs(aerocrs);
        response = serviceListResponse.generateResponse(go7Rsp, null);
        assertNotNull(response);
        assertNull(response.getResponseId());
    }

    @Test
    void testGenerateResponse_HappyPath() {
        ServiceListResponse serviceListResponse = new ServiceListResponse();

        // Setup Booking Response
        OrderRetrieveRspGo7Dto bookingRsp = new OrderRetrieveRspGo7Dto();
        Aerocrs bookingAerocrs = new Aerocrs();
        Booking booking = new Booking();

        // Setup Passengers
        Passengers passengers = new Passengers();
        List<Passenger> passengerList = new ArrayList<>();

        Passenger adult1 = new Passenger();
        adult1.setPaxtype("ADT");
        adult1.setPaxtitle("Mr.");
        adult1.setFirstname("John");
        adult1.setLastname("Doe");
        passengerList.add(adult1);

        Passenger child1 = new Passenger();
        child1.setPaxtype("CHILD");
        child1.setPaxtitle("Miss");
        child1.setFirstname("Jane");
        child1.setLastname("Doe");
        passengerList.add(child1);

        Passenger infant1 = new Passenger();
        infant1.setPaxtype("INFANT");
        infant1.setPaxtitle("INF.");
        infant1.setFirstname("Baby");
        infant1.setLastname("Doe");
        passengerList.add(infant1);

        passengers.setPassenger(passengerList);
        booking.setPassengers(passengers);

        // Setup Segment/Flights (using Items first)
        Items items = new Items();
        List<Flight> flightList = new ArrayList<>();
        Flight flight1 = new Flight();
        flight1.setNumber("G7101");
        flightList.add(flight1);
        items.setFlight(flightList);
        booking.setItems(items);

        bookingAerocrs.setBooking(booking);
        bookingRsp.setAerocrs(bookingAerocrs);

        // Setup Go7 Service List Response
        ServiceListRspGo7Dto go7Response = new ServiceListRspGo7Dto();
        Aerocrs serviceAerocrs = new Aerocrs();

        ServiceListRspGo7Dto.Ancillaries ancillaries = new ServiceListRspGo7Dto.Ancillaries();
        List<ServiceListRspGo7Dto.Ancillary> ancillaryList = new ArrayList<>();

        // Baggage Ancillary
        ServiceListRspGo7Dto.Ancillary bagAnc = new ServiceListRspGo7Dto.Ancillary();
        bagAnc.setName("Extra Baggage");
        bagAnc.setGroupname("Baggage");
        List<ServiceListRspGo7Dto.Item> bagItems = new ArrayList<>();
        ServiceListRspGo7Dto.Item bagItem = new ServiceListRspGo7Dto.Item();
        bagItem.setItemid("BAG123");
        bagItem.setItemname("23KG Baggage");
        bagItem.setFare("50.00");
        bagItem.setCurrency("USD");
        bagItems.add(bagItem);
        bagAnc.setItems(bagItems);
        ancillaryList.add(bagAnc);

        // Lounge Access Ancillary (Other service)
        ServiceListRspGo7Dto.Ancillary loungeAnc = new ServiceListRspGo7Dto.Ancillary();
        loungeAnc.setName("Lounge Access");
        loungeAnc.setGroupname("Lounge Service");
        List<ServiceListRspGo7Dto.Item> loungeItems = new ArrayList<>();
        ServiceListRspGo7Dto.Item loungeItem = new ServiceListRspGo7Dto.Item();
        loungeItem.setItemid(""); // Empty itemid test
        loungeItem.setItemname("VIP Lounge");
        loungeItem.setFare("35.00");
        loungeItem.setCurrency("USD");
        loungeItems.add(loungeItem);
        loungeAnc.setItems(loungeItems);
        ancillaryList.add(loungeAnc);

        // Meal Ancillary (Other service, null itemid, generateSubCode clean length < 3)
        ServiceListRspGo7Dto.Ancillary mealAnc = new ServiceListRspGo7Dto.Ancillary();
        mealAnc.setName("Hot Meals");
        mealAnc.setGroupname("Meal Group");
        List<ServiceListRspGo7Dto.Item> mealItems = new ArrayList<>();
        ServiceListRspGo7Dto.Item mealItem = new ServiceListRspGo7Dto.Item();
        mealItem.setItemid(null); // Null itemid test
        mealItem.setItemname("WI"); // generateSubCode -> WIX
        mealItem.setFare("12.50");
        mealItem.setCurrency("USD");
        mealItems.add(mealItem);
        mealAnc.setItems(mealItems);
        ancillaryList.add(mealAnc);

        // Other service, invalid price
        ServiceListRspGo7Dto.Ancillary otherAnc = new ServiceListRspGo7Dto.Ancillary();
        otherAnc.setName("Invalid Service");
        otherAnc.setGroupname("Other Group");
        List<ServiceListRspGo7Dto.Item> otherItems = new ArrayList<>();
        ServiceListRspGo7Dto.Item otherItem = new ServiceListRspGo7Dto.Item();
        otherItem.setItemid("INV999");
        otherItem.setItemname(null); // generateSubCode -> XXX
        otherItem.setFare("invalid_number"); // safeDecimal format exception test
        otherItem.setCurrency("USD");
        otherItems.add(otherItem);
        otherAnc.setItems(otherItems);
        ancillaryList.add(otherAnc);

        // Ancillary with null items
        ServiceListRspGo7Dto.Ancillary nullItemsAnc = new ServiceListRspGo7Dto.Ancillary();
        nullItemsAnc.setName("Null Items");
        nullItemsAnc.setItems(null);
        ancillaryList.add(nullItemsAnc);

        ancillaries.setAncillary(ancillaryList);
        serviceAerocrs.setAncillaries(ancillaries);
        go7Response.setAerocrs(serviceAerocrs);

        // Run
        ServiceListRspDto response = serviceListResponse.generateResponse(go7Response, bookingRsp);

        // Assertions
        assertNotNull(response);
        assertNotNull(response.getResponseId());
        assertEquals("G7", response.getApiOwner());
        assertEquals("G7", response.getValidatingCarrier());
        assertEquals("4", response.getQuantity());

        List<ServiceListRspDto.OfferItem> offerItems = response.getOfferItem();
        assertEquals(1, offerItems.size());

        ServiceListRspDto.OfferItem offerItem = offerItems.get(0);

        // Baggage assertions
        assertEquals(1, offerItem.getBaggageList().size());
        ServiceListRspDto.OfferItem.Baggage bag = offerItem.getBaggageList().get(0);
        assertEquals("BAG123", bag.getOfferItemId());
        assertEquals(new BigDecimal("50.00"), bag.getBaseAmount());
        assertEquals(new BigDecimal("50.00"), bag.getTotalAmount());
        assertEquals("USD", bag.getCurrency());
        assertEquals("C-OC-23K-BG-05-G7", bag.getService().getServiceDefinitionRef().getServiceDefId());
        assertEquals("23KG Baggage", bag.getService().getServiceDefinitionRef().getName());

        // Other service assertions
        assertEquals(3, offerItem.getOtherServiceList().size());
        
        // Lounge access
        ServiceListRspDto.OfferItem.OtherService lounge = offerItem.getOtherServiceList().get(0);
        assertTrue(lounge.getOfferItemId().contains("-2")); // auto-generated
        assertEquals(new BigDecimal("35.00"), lounge.getBaseAmount());
        assertEquals("E-OC-VIP-G7", lounge.getService().getServiceDefinitionRef().getServiceDefId()); // Lounge -> RFIC E
        
        // Meal (WI)
        ServiceListRspDto.OfferItem.OtherService meal = offerItem.getOtherServiceList().get(1);
        assertTrue(meal.getOfferItemId().contains("-3")); // auto-generated
        assertEquals(new BigDecimal("12.50"), meal.getBaseAmount());
        assertEquals("F-OC-WIX-G7", meal.getService().getServiceDefinitionRef().getServiceDefId()); // Non-lounge -> RFIC F

        // Invalid service (safeDecimal returns ZERO)
        ServiceListRspDto.OfferItem.OtherService invalidSvc = offerItem.getOtherServiceList().get(2);
        assertEquals("INV999", invalidSvc.getOfferItemId());
        assertEquals(BigDecimal.ZERO, invalidSvc.getBaseAmount());
        assertEquals("F-OC-XXX-G7", invalidSvc.getService().getServiceDefinitionRef().getServiceDefId()); // Name null -> XXX
    }

    @Test
    void testExtractPassengerAndSegmentRefs_EmptyBooking() {
        ServiceListResponse serviceListResponse = new ServiceListResponse();

        // 1. bookingRsp is completely null
        ServiceListRspGo7Dto go7Response = new ServiceListRspGo7Dto();
        Aerocrs serviceAerocrs = new Aerocrs();
        ServiceListRspGo7Dto.Ancillaries ancillaries = new ServiceListRspGo7Dto.Ancillaries();
        List<ServiceListRspGo7Dto.Ancillary> ancillaryList = new ArrayList<>();
        
        ServiceListRspGo7Dto.Ancillary bagAnc = new ServiceListRspGo7Dto.Ancillary();
        bagAnc.setName("Baggage Allowance");
        bagAnc.setGroupname("Baggage");
        List<ServiceListRspGo7Dto.Item> bagItems = new ArrayList<>();
        ServiceListRspGo7Dto.Item bagItem = new ServiceListRspGo7Dto.Item();
        bagItem.setItemid("BAG1");
        bagItem.setItemname("Standard Baggage");
        bagItem.setFare("20.00");
        bagItem.setCurrency("USD");
        bagItems.add(bagItem);
        bagAnc.setItems(bagItems);
        ancillaryList.add(bagAnc);

        ancillaries.setAncillary(ancillaryList);
        serviceAerocrs.setAncillaries(ancillaries);
        go7Response.setAerocrs(serviceAerocrs);

        ServiceListRspDto response = serviceListResponse.generateResponse(go7Response, null);

        assertNotNull(response);
        ServiceListRspDto.OfferItem.Baggage bag = response.getOfferItem().get(0).getBaggageList().get(0);
        // Should fallback to T1 / UNKNOWN / S1
        assertEquals(Collections.singletonList("T1"), bag.getPassengerRefs());
        assertEquals(Collections.singletonList("UNKNOWN"), bag.getGivenName());
        assertEquals(Collections.singletonList("S1"), bag.getSegmentRefId());
    }

    @Test
    void testExtractSegments_FlightsAlternative() {
        ServiceListResponse serviceListResponse = new ServiceListResponse();

        // Setup Booking Response using flights instead of items
        OrderRetrieveRspGo7Dto bookingRsp = new OrderRetrieveRspGo7Dto();
        Aerocrs bookingAerocrs = new Aerocrs();
        Booking booking = new Booking();

        // Setup Flights directly under Booking
        Flights flights = new Flights();
        List<Flight> flightList = new ArrayList<>();
        Flight flight1 = new Flight();
        flight1.setNumber("G7201");
        flightList.add(flight1);
        flights.setFlight(flightList);
        booking.setFlights(flights);

        bookingAerocrs.setBooking(booking);
        bookingRsp.setAerocrs(bookingAerocrs);

        // Setup Go7 Service List Response
        ServiceListRspGo7Dto go7Response = new ServiceListRspGo7Dto();
        Aerocrs serviceAerocrs = new Aerocrs();
        ServiceListRspGo7Dto.Ancillaries ancillaries = new ServiceListRspGo7Dto.Ancillaries();
        List<ServiceListRspGo7Dto.Ancillary> ancillaryList = new ArrayList<>();
        
        ServiceListRspGo7Dto.Ancillary bagAnc = new ServiceListRspGo7Dto.Ancillary();
        bagAnc.setName("Extra Baggage");
        bagAnc.setGroupname("Luggage"); // test group name match "Luggage"
        List<ServiceListRspGo7Dto.Item> bagItems = new ArrayList<>();
        ServiceListRspGo7Dto.Item bagItem = new ServiceListRspGo7Dto.Item();
        bagItem.setItemid("BAG2");
        bagItem.setItemname("Luggage Service");
        bagItem.setFare("40.00");
        bagItem.setCurrency("EUR");
        bagItems.add(bagItem);
        bagAnc.setItems(bagItems);
        ancillaryList.add(bagAnc);

        ancillaries.setAncillary(ancillaryList);
        serviceAerocrs.setAncillaries(ancillaries);
        go7Response.setAerocrs(serviceAerocrs);

        ServiceListRspDto response = serviceListResponse.generateResponse(go7Response, bookingRsp);

        assertNotNull(response);
        ServiceListRspDto.OfferItem.Baggage bag = response.getOfferItem().get(0).getBaggageList().get(0);
        // segmentRefId should be S1 since we have 1 flight in getFlights()
        assertEquals(Collections.singletonList("S1"), bag.getSegmentRefId());
    }

    @Test
    void testPassengerTitleInfantMapping() {
        ServiceListResponse serviceListResponse = new ServiceListResponse();

        OrderRetrieveRspGo7Dto bookingRsp = new OrderRetrieveRspGo7Dto();
        Aerocrs bookingAerocrs = new Aerocrs();
        Booking booking = new Booking();

        Passengers passengers = new Passengers();
        List<Passenger> passengerList = new ArrayList<>();

        // Test infant mapping based on title INF
        Passenger p1 = new Passenger();
        p1.setPaxtype("ADT");
        p1.setPaxtitle("INF");
        p1.setFirstname("Baby");
        p1.setLastname("One");
        passengerList.add(p1);

        passengers.setPassenger(passengerList);
        booking.setPassengers(passengers);
        bookingAerocrs.setBooking(booking);
        bookingRsp.setAerocrs(bookingAerocrs);

        ServiceListRspGo7Dto go7Response = new ServiceListRspGo7Dto();
        Aerocrs serviceAerocrs = new Aerocrs();
        ServiceListRspGo7Dto.Ancillaries ancillaries = new ServiceListRspGo7Dto.Ancillaries();
        List<ServiceListRspGo7Dto.Ancillary> ancillaryList = new ArrayList<>();
        
        ServiceListRspGo7Dto.Ancillary bagAnc = new ServiceListRspGo7Dto.Ancillary();
        bagAnc.setGroupname("Other");
        bagAnc.setName("Baggage"); // matches baggage by name contains "baggage"
        List<ServiceListRspGo7Dto.Item> bagItems = new ArrayList<>();
        ServiceListRspGo7Dto.Item bagItem = new ServiceListRspGo7Dto.Item();
        bagItem.setItemid("BAG3");
        bagItem.setItemname("Standard Baggage");
        bagItem.setFare("25.00");
        bagItem.setCurrency("USD");
        bagItems.add(bagItem);
        bagAnc.setItems(bagItems);
        ancillaryList.add(bagAnc);

        ancillaries.setAncillary(ancillaryList);
        serviceAerocrs.setAncillaries(ancillaries);
        go7Response.setAerocrs(serviceAerocrs);

        ServiceListRspDto response = serviceListResponse.generateResponse(go7Response, bookingRsp);

        assertNotNull(response);
        ServiceListRspDto.OfferItem.Baggage bag = response.getOfferItem().get(0).getBaggageList().get(0);
        // Since it's infant, and there is no adult, it fallback to T1.1
        assertEquals(Collections.singletonList("T1.1"), bag.getPassengerRefs());
    }
}
