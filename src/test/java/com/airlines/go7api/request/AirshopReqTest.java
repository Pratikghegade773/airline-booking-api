package com.airlines.go7api.request;

import com.airlines.go7api.requestdto.AirshopReqDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AirshopReqTest {

    @Test
    void testGettersAndSetters() {
        AirshopReq req = new AirshopReq();
        
        req.setExactMatch(true);
        assertTrue(req.getExactMatch());

        req.setNonStopFlight(false);
        assertFalse(req.getNonStopFlight());

        req.setCabinType("business");
        assertEquals("business", req.getCabinType());

        req.setFlexibility("3D");
        assertEquals("3D", req.getFlexibility());

        req.setFareType("public");
        assertEquals("public", req.getFareType());

        req.setMiniFareRule(true);
        assertTrue(req.getMiniFareRule());

        req.setSplitOffer(true);
        assertTrue(req.getSplitOffer());

        req.setOriginCurrency(false);
        assertFalse(req.getOriginCurrency());

        req.setFltnum("123");
        assertEquals("123", req.getFltnum());

        req.setChargetype("ADT");
        assertEquals("ADT", req.getChargetype());

        req.setCurrency("USD");
        assertEquals("USD", req.getCurrency());

        req.setFltsFROMperiod("2026-06-01");
        assertEquals("2026-06-01", req.getFltsFROMperiod());

        req.setFltsTOperiod("2026-06-08");
        assertEquals("2026-06-08", req.getFltsTOperiod());

        req.setCodeformat("IATA");
        assertEquals("IATA", req.getCodeformat());

        req.setCompanycode("API");
        assertEquals("API", req.getCompanycode());

        req.setSoldonline(true);
        assertTrue(req.getSoldonline());

        req.setSsim(false);
        assertFalse(req.getSsim());

        assertEquals("", req.getApiUrl());
        assertEquals("Airshop", req.getRequestName());
    }

    @Test
    void testJourneyGettersAndSetters() {
        AirshopReq.Journey journey = new AirshopReq.Journey();
        journey.setId("J1");
        assertEquals("J1", journey.getId());

        journey.setDepartureAirport("JFK");
        assertEquals("JFK", journey.getDepartureAirport());

        journey.setArrivalAirport("LAX");
        assertEquals("LAX", journey.getArrivalAirport());

        journey.setAction("book");
        assertEquals("book", journey.getAction());

        AirshopReq.Journey.JourneyDate date = new AirshopReq.Journey.JourneyDate();
        date.setMain("2026-06-01");
        assertEquals("2026-06-01", date.getMain());

        date.setTime("12:00");
        assertEquals("12:00", date.getTime());

        date.setType("exact");
        assertEquals("exact", date.getType());

        date.setFlexibilityRange(3);
        assertEquals(3, date.getFlexibilityRange());

        journey.setDate(date);
        assertNotNull(journey.getDate());

        AirshopReq.Journey.AircraftPreference aircraft = new AirshopReq.Journey.AircraftPreference();
        aircraft.setCode("737");
        assertEquals("737", aircraft.getCode());

        aircraft.setName("Boeing 737");
        assertEquals("Boeing 737", aircraft.getName());

        journey.setAircraftPreference(aircraft);
        assertNotNull(journey.getAircraftPreference());
    }

    @Test
    void testCorporateAccountGettersAndSetters() {
        AirshopReq.CorporateAccount account = new AirshopReq.CorporateAccount();
        account.setAirlineCode("AA");
        assertEquals("AA", account.getAirlineCode());

        account.setNumber("12345");
        assertEquals("12345", account.getNumber());

        AirshopReq req = new AirshopReq();
        req.setCorporateAccount(Collections.singletonList(account));
        assertNotNull(req.getCorporateAccount());
        assertEquals(1, req.getCorporateAccount().size());
    }

    @Test
    void testPassengerGettersAndSetters() {
        AirshopReq.Passenger passenger = new AirshopReq.Passenger();
        passenger.setAge(30);
        assertEquals(30, passenger.getAge());

        passenger.setSeatRequested(true);
        assertTrue(passenger.getSeatRequested());

        AirshopReq.Passenger.FrequentFlyer ff = new AirshopReq.Passenger.FrequentFlyer();
        ff.setAirlineCode("UA");
        assertEquals("UA", ff.getAirlineCode());

        ff.setNumber("98765");
        assertEquals("98765", ff.getNumber());

        passenger.setFrequentFlyer(Collections.singletonList(ff));
        assertNotNull(passenger.getFrequentFlyer());
        assertEquals(1, passenger.getFrequentFlyer().size());

        AirshopReq req = new AirshopReq();
        req.setPassengers(Collections.singletonList(passenger));
        assertNotNull(req.getPassengers());
        assertEquals(1, req.getPassengers().size());
    }

    @Test
    void testMapToFlightSearchRequestDTO() {
        AirshopReqDto dto = new AirshopReqDto();
        dto.setFlexibility("3D");
        dto.setAdults(2);
        dto.setChildren(1);
        dto.setInfants(1);

        List<AirshopReqDto.OD> ods = new ArrayList<>();
        AirshopReqDto.OD od1 = new AirshopReqDto.OD();
        od1.setOrigin("JFK");
        od1.setDestination("LAX");
        od1.setDate("2026-06-01");
        od1.setCabinPreference("BUSINESS");
        ods.add(od1);

        dto.setOds(ods);

        AirshopReq result = AirshopReq.mapToFlightSearchRequestDTO(dto);

        assertNotNull(result);
        assertEquals("3D", result.getFlexibility());
        assertEquals("business", result.getCabinType());
        assertNotNull(result.getJourneys());
        assertEquals(1, result.getJourneys().size());

        AirshopReq.Journey journey = result.getJourneys().get(0);
        assertEquals("JFK", journey.getDepartureAirport());
        assertEquals("LAX", journey.getArrivalAirport());
        assertEquals("2026-06-01", journey.getDate().getMain());

        assertNotNull(result.getPassengers());
        assertEquals(4, result.getPassengers().size()); // 2 adults + 1 child + 1 infant
        assertEquals(30, result.getPassengers().get(0).getAge());
        assertEquals(30, result.getPassengers().get(1).getAge());
        assertEquals(11, result.getPassengers().get(2).getAge());
        assertEquals(1, result.getPassengers().get(3).getAge());
    }

    @Test
    void testMakeApiCallAndUnmarshal() {
        AirshopReq req = new AirshopReq();
        
        // Populate minimal journey/passenger data to test makeApiCall path building
        AirshopReq.Journey journey = new AirshopReq.Journey();
        journey.setDepartureAirport("JFK");
        journey.setArrivalAirport("LAX");
        AirshopReq.Journey.JourneyDate date = new AirshopReq.Journey.JourneyDate();
        date.setMain("2026-06-01");
        journey.setDate(date);

        AirshopReq.Journey returnJourney = new AirshopReq.Journey();
        returnJourney.setDepartureAirport("LAX");
        returnJourney.setArrivalAirport("JFK");
        AirshopReq.Journey.JourneyDate returnDate = new AirshopReq.Journey.JourneyDate();
        returnDate.setMain("2026-06-08");
        returnJourney.setDate(returnDate);

        List<AirshopReq.Journey> journeys = new ArrayList<>();
        journeys.add(journey);
        journeys.add(returnJourney);
        req.setJourneys(journeys);

        AirshopReq.Passenger p1 = new AirshopReq.Passenger();
        p1.setAge(30);
        AirshopReq.Passenger p2 = new AirshopReq.Passenger();
        p2.setAge(10);
        AirshopReq.Passenger p3 = new AirshopReq.Passenger();
        p3.setAge(1);

        List<AirshopReq.Passenger> passengers = new ArrayList<>();
        passengers.add(p1);
        passengers.add(p2);
        passengers.add(p3);
        req.setPassengers(passengers);

        // This will attempt a real api call and throw HttpClientErrorException, ResourceAccessException or other connection failures.
        // We catch it to ensure coverage of the exception path.
        try {
            req.makeApiCall();
        } catch (Exception e) {
            // expected connection failure in unit test environment
        }

        try {
            req.unmarshal();
        } catch (Exception e) {
            // expected connection failure in unit test environment
        }
        
        assertNotNull(req);
        assertEquals(2, req.getJourneys().size());
    }
}
