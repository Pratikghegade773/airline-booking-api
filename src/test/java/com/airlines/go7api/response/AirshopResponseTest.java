package com.airlines.go7api.response;

import com.airlines.go7api.requestdto.AirshopReqDto;
import com.airlines.go7api.responsedto.AirshopRspDto;
import com.airlines.go7api.responsedto.AirshopRspDto.Offer;
import com.airlines.go7api.responsedto.common.OD;
import com.airlines.go7api.responsego7.AirshopRspGo7Dto;
import com.airlines.go7api.responsego7.common.Aerocrs;
import com.airlines.go7api.responsego7.common.Flight;
import com.airlines.go7api.responsego7.common.Flights;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AirshopResponseTest {

    @Test
    void testAirshoppingMapper_InvalidInputs() {
        AirshopResponse mapper = new AirshopResponse();
        mapper.loadAirports();

        // 1. Null response
        AirshopRspDto result = mapper.airshoppingMapper(null, null);
        assertNotNull(result);
        assertNull(result.getResponseId());

        // 2. Null aerocrs
        AirshopRspGo7Dto go7Rsp = new AirshopRspGo7Dto();
        result = mapper.airshoppingMapper(go7Rsp, null);
        assertNotNull(result);
        assertNull(result.getResponseId());

        // 3. success = false
        Aerocrs aerocrs = new Aerocrs();
        aerocrs.setSuccess(false);
        go7Rsp.setAerocrs(aerocrs);
        result = mapper.airshoppingMapper(go7Rsp, null);
        assertNotNull(result);
        assertNull(result.getResponseId());

        // 4. Null flights
        aerocrs.setSuccess(true);
        result = mapper.airshoppingMapper(go7Rsp, null);
        assertNotNull(result);
        assertNull(result.getResponseId());

        // 5. Null flight list
        Flights flights = new Flights();
        aerocrs.setFlights(flights);
        result = mapper.airshoppingMapper(go7Rsp, null);
        assertNotNull(result);
        assertNull(result.getResponseId());
    }

    @Test
    void testAirshoppingMapper_HappyPath() {
        AirshopResponse mapper = new AirshopResponse();
        mapper.loadAirports();

        AirshopRspGo7Dto go7Rsp = new AirshopRspGo7Dto();
        Aerocrs aerocrs = new Aerocrs();
        aerocrs.setSuccess(true);
        go7Rsp.setAerocrs(aerocrs);

        Flights flights = new Flights();
        List<Flight> flightList = new ArrayList<>();
        
        // Setup flight
        Flight flight = new Flight();
        flight.setFromcode("JFK");
        flight.setTocode("LAX");
        flight.setFltnum("123");
        flight.setAircraftType("A320");
        flight.setAirlinedesignator("G7");
        flight.setAirline("AeroCRS Airlines");
        flight.setDepartureTerminal("T1");
        flight.setArrivalTerminal("T2");
        flight.setStd("2026/06/01 10:00:00.000");
        flight.setSta("2026/06/01 12:30:00.000");
        flight.setDirection("outbound");

        // Flight Classes setup
        Map<String, AirshopRspGo7Dto.FlightClass> classes = new HashMap<>();
        
        AirshopRspGo7Dto.FlightClass fc = new AirshopRspGo7Dto.FlightClass();
        fc.setFlightid(1001L);
        fc.setFareid(5001L);
        fc.setCurrency("USD");
        fc.setClassName("Economy");
        fc.setCabinClass("Y");
        fc.setBaggageAllowance(23);
        fc.setBaggageUnit("KG");
        fc.setInfbaggageallowance(10);
        fc.setType("Y/Flex Plus");

        // Set Fare details
        AirshopRspGo7Dto.Fare fare = new AirshopRspGo7Dto.Fare();
        fare.setAdultFare("250.00");
        fare.setChildFare("180.00");
        fare.setInfantFare("50.00");
        fare.setTax("30.00");
        fc.setFare(fare);

        // Hand Baggage service
        Map<String, AirshopRspGo7Dto.Service> services = new HashMap<>();
        AirshopRspGo7Dto.Service handBaggage = new AirshopRspGo7Dto.Service();
        handBaggage.setActive(true);
        handBaggage.setText("Hand Baggage allowance 8 KG");
        services.put("HandBaggage", handBaggage);

        // Extra active service for price class references
        AirshopRspGo7Dto.Service mealService = new AirshopRspGo7Dto.Service();
        mealService.setActive(true);
        mealService.setText("Hot Meal Included");
        services.put("Meal", mealService);

        // Inactive service (should be ignored or handled but not fail)
        AirshopRspGo7Dto.Service wifiService = new AirshopRspGo7Dto.Service();
        wifiService.setActive(false);
        wifiService.setText("Wifi service");
        services.put("Wifi", wifiService);

        fc.setServices(services);

        // Setup raw fare object for tax breakdown coverage
        AirshopRspGo7Dto.RawFareObject rawFare = new AirshopRspGo7Dto.RawFareObject();
        AirshopRspGo7Dto.RackFareDetails rackFare = new AirshopRspGo7Dto.RackFareDetails();
        Map<String, String> taxBreakdown = new LinkedHashMap<>();
        taxBreakdown.put("US", "10.00");
        taxBreakdown.put("ZP", "5.00");
        taxBreakdown.put("AY", "5.60");
        taxBreakdown.put("OTHER_LONG_TAX_CODE", "9.40");
        rackFare.setTaxBreakdown(taxBreakdown);
        rackFare.setAdultFare("300.00");
        rawFare.setRackFare(rackFare);
        fc.setRawFareObject(rawFare);

        classes.put("Y/Flex Plus", fc);
        flight.setClasses(classes);
        flightList.add(flight);
        flights.setFlight(flightList);
        aerocrs.setFlights(flights);

        // Request setup
        AirshopReqDto request = new AirshopReqDto();
        request.setAdults(1);
        request.setChildren(1);
        request.setInfants(1);
        request.setTripType("RT");

        // Map and assert
        AirshopRspDto result = mapper.airshoppingMapper(go7Rsp, request);

        assertNotNull(result);
        assertEquals("5001|G7|RT|1", result.getResponseId() + "|" + result.getApiOwner() + "|" + result.getRoundTripType() + "|" + result.getOffers().size());

        Offer offer = result.getOffers().get(0);
        assertNotNull(offer.getOfferId());
        assertEquals("USD|Economy Flex Plus|Y|1|3", offer.getCurrency() + "|" + offer.getClassType() + "|" + offer.getCabinTypeCode() + "|" + offer.getOds().size() + "|" + offer.getOfferItems().size());
        
        // Assert OD details
        OD od = offer.getOds().get(0);
        assertEquals("JFK|LAX|123|PT2H30M", od.getOrigin() + "|" + od.getDestination() + "|" + od.getFlightNumber() + "|" + od.getJourneyTime());

        // Assert Offer Items (ADT, CNN, INF)
        AirshopRspDto.Offer.OfferItemDto adtItem = offer.getOfferItems().get(0);
        AirshopRspDto.Offer.OfferItemDto cnnItem = offer.getOfferItems().get(1);
        AirshopRspDto.Offer.OfferItemDto infItem = offer.getOfferItems().get(2);

        assertEquals("ADT|250.00|T1", adtItem.getPtc() + "|" + adtItem.getTotalPrice() + "|" + adtItem.getPassengerRefs().get(0));
        assertEquals("CNN|180.00|T2", cnnItem.getPtc() + "|" + cnnItem.getTotalPrice() + "|" + cnnItem.getPassengerRefs().get(0));
        assertEquals("INF|50.00|T1.1", infItem.getPtc() + "|" + infItem.getTotalPrice() + "|" + infItem.getPassengerRefs().get(0));

        // Assert Baggage
        assertEquals(2, adtItem.getBaggageAllowances().size());
        assertEquals("Checked-In|23|KG", adtItem.getBaggageAllowances().get(0).getCategory() + "|" + adtItem.getBaggageAllowances().get(0).getWeight().get(0).getValue() + "|" + adtItem.getBaggageAllowances().get(0).getWeight().get(0).getUom());
        assertEquals("Carry On|8|KG", adtItem.getBaggageAllowances().get(1).getCategory() + "|" + adtItem.getBaggageAllowances().get(1).getWeight().get(0).getValue() + "|" + adtItem.getBaggageAllowances().get(1).getWeight().get(0).getUom());

        assertEquals(2, infItem.getBaggageAllowances().size());
        assertEquals("Checked-In|10|KG", infItem.getBaggageAllowances().get(0).getCategory() + "|" + infItem.getBaggageAllowances().get(0).getWeight().get(0).getValue() + "|" + infItem.getBaggageAllowances().get(0).getWeight().get(0).getUom());

        // Assert Price Class
        assertEquals("Y/Flex Plus", adtItem.getPriceClassReferences().get(0).getClassName());
        
        // Assert Fare Details and Tax breakdown scaling
        AirshopRspDto.Offer.OfferItemDto.FareDetail adtFareDetail = adtItem.getFareDetail().get(0);
        assertEquals("250.00|220.00|30.00", adtFareDetail.getPrice().getTotalFare().getAmount() + "|" + adtFareDetail.getPrice().getBaseFare().getAmount() + "|" + adtFareDetail.getPrice().getTotalTax().getAmount());

        // Verification of scaled taxes breakdown list
        List<AirshopRspDto.Offer.OfferItemDto.FareDetail.Price.Taxes> taxesList = adtFareDetail.getPrice().getTaxes();
        assertEquals(4, taxesList.size());
        assertEquals("US|10.00", taxesList.get(0).getCode() + "|" + taxesList.get(0).getAmount());
        assertEquals("TAX|OTHER_LONG_TAX_CODE", taxesList.get(3).getCode() + "|" + taxesList.get(3).getDescription());
    }

    @Test
    void testAirshoppingMapper_AlternativeBranchPaths() {
        AirshopResponse mapper = new AirshopResponse();
        mapper.loadAirports();

        AirshopRspGo7Dto go7Rsp = new AirshopRspGo7Dto();
        Aerocrs aerocrs = new Aerocrs();
        aerocrs.setSuccess(true);
        go7Rsp.setAerocrs(aerocrs);

        Flights flights = new Flights();
        List<Flight> flightList = new ArrayList<>();
        
        Flight flight = new Flight();
        flight.setFromcode("JFK");
        flight.setTocode("LAX");
        flight.setStd("invalid_std_format");
        flight.setSta("invalid_sta_format");
        flight.setDirection("inbound"); // tests OD2 key

        Map<String, AirshopRspGo7Dto.FlightClass> classes = new HashMap<>();
        AirshopRspGo7Dto.FlightClass fc = new AirshopRspGo7Dto.FlightClass();
        fc.setFlightid(1002L);
        fc.setFareid(5002L);
        fc.setCurrency("EUR");
        fc.setClassName("Y/Basic");
        fc.setCabinClass("Y");
        
        // Fare details without child/infant fares to test fallbacks
        AirshopRspGo7Dto.Fare fare = new AirshopRspGo7Dto.Fare();
        fare.setAdultFare("100.00");
        fare.setTax("0.00"); // Zero tax path
        fc.setFare(fare);

        classes.put("Y/Basic", fc);
        flight.setClasses(classes);
        flightList.add(flight);
        flights.setFlight(flightList);
        aerocrs.setFlights(flights);

        AirshopReqDto request = new AirshopReqDto();
        request.setAdults(1);
        request.setChildren(1);
        request.setInfants(1);
        request.setTripType("OW");

        AirshopRspDto result = mapper.airshoppingMapper(go7Rsp, request);
        assertNotNull(result);
        Offer offer = result.getOffers().get(0);
        assertEquals("Economy Basic", offer.getClassType());
        
        // Assert inbound OD key is OD2
        assertEquals("OD2", offer.getOds().get(0).getOdKey());
        assertNull(offer.getOds().get(0).getJourneyTime()); // failed parse falls back to null

        // Child and infant price fallbacks
        AirshopRspDto.Offer.OfferItemDto cnnItem = offer.getOfferItems().get(1);
        assertEquals(new BigDecimal("100.00"), cnnItem.getTotalPrice()); // falls back to adult fare

        AirshopRspDto.Offer.OfferItemDto infItem = offer.getOfferItems().get(2);
        assertEquals(BigDecimal.ZERO, infItem.getTotalPrice()); // infant falls back to zero

        // Assert empty service description
        AirshopRspDto.Offer.OfferItemDto adtItem = offer.getOfferItems().get(0);
        assertNotNull(adtItem.getFareDetail().get(0).getPrice().getTaxes());
        assertTrue(adtItem.getFareDetail().get(0).getPrice().getTaxes().isEmpty()); // tax = 0 path
    }

    @Test
    void testAirshoppingMapper_BusinessClassAndDefaultFares() {
        AirshopResponse mapper = new AirshopResponse();

        AirshopRspGo7Dto go7Rsp = new AirshopRspGo7Dto();
        Aerocrs aerocrs = new Aerocrs();
        aerocrs.setSuccess(true);
        go7Rsp.setAerocrs(aerocrs);

        Flights flights = new Flights();
        List<Flight> flightList = new ArrayList<>();
        
        Flight flight = new Flight();
        flight.setFromcode("JFK");
        flight.setTocode("LAX");

        Map<String, AirshopRspGo7Dto.FlightClass> classes = new HashMap<>();
        AirshopRspGo7Dto.FlightClass fc = new AirshopRspGo7Dto.FlightClass();
        fc.setClassName("CustomClass");
        fc.setType("B"); // tests Business class type mapping

        AirshopRspGo7Dto.Fare fare = new AirshopRspGo7Dto.Fare();
        fare.setAdultFare("500.00");
        fare.setTax("50.00");
        fc.setFare(fare);

        classes.put("B", fc);
        flight.setClasses(classes);
        flightList.add(flight);
        flights.setFlight(flightList);
        aerocrs.setFlights(flights);

        AirshopRspDto result = mapper.airshoppingMapper(go7Rsp, null); // request is null, defaults to 1 ADT
        assertNotNull(result);
        Offer offer = result.getOffers().get(0);
        assertEquals("Business", offer.getClassType());
        assertEquals(1, offer.getOfferItems().size());
        assertEquals("ADT", offer.getOfferItems().get(0).getPtc());
    }
}
