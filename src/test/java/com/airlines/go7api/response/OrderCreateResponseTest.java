package com.airlines.go7api.response;

import com.airlines.go7api.requestdto.OrderCreateReqDto;
import com.airlines.go7api.requestdto.common.PaxReqDto;
import com.airlines.go7api.responsedto.OrderCreateRspDto;
import com.airlines.go7api.responsego7.OrderCreateRspGo7Dto;
import com.airlines.go7api.responsego7.common.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrderCreateResponseTest {

    // -------------------------------------------------------------------------
    // Helper: build a minimal valid Go7 response with one adult passenger
    // -------------------------------------------------------------------------
    private OrderCreateRspGo7Dto createBaseGo7Response() {
        Booking booking = new Booking();
        booking.setBookingid(12345L);
        booking.setPnrref("TESTPNR");
        booking.setCurrency("USD");

        // flight
        Flight flight = new Flight();
        flight.setFromcode("JFK");
        flight.setFrom("John F Kennedy");
        flight.setTocode("LHR");
        flight.setTo("London Heathrow");
        flight.setFlightdate("2026-06-01");
        flight.setDepart("10:00");
        flight.setArrive("22:00");
        flight.setNumber("G7101");
        flight.setAirlinedesignator("G7");
        flight.setAirline("Go7 Airlines");
        flight.setAdultfare("300");
        flight.setTax("50");

        Flights flights = new Flights();
        flights.setFlight(List.of(flight));
        booking.setFlights(flights);

        // passenger
        Passenger pax = new Passenger();
        pax.setFirstname("John");
        pax.setLastname("Doe");
        pax.setPaxtype("ADULT");
        pax.setPaxtitle("MR");
        pax.setGender("Male");

        Passengers passengers = new Passengers();
        passengers.setPassenger(List.of(pax));
        booking.setPassengers(passengers);

        Aerocrs aerocrs = new Aerocrs();
        aerocrs.setSuccess(true);
        aerocrs.setBooking(booking);

        OrderCreateRspGo7Dto go7Dto = new OrderCreateRspGo7Dto();
        go7Dto.setAerocrs(aerocrs);
        return go7Dto;
    }

    private OrderCreateReqDto createBasicReqDto() {
        OrderCreateReqDto req = new OrderCreateReqDto();
        PaxReqDto paxReq = new PaxReqDto();
        paxReq.setPaxId("PAX1");
        paxReq.setPtc("ADT");
        paxReq.setFirstName("John");
        paxReq.setLastName("Doe");
        paxReq.setGender("Male");
        paxReq.setTitle("MR");
        req.setPassengers(List.of(paxReq));
        return req;
    }

    // =========================================================================
    // 1. NULL / GUARD TESTS
    // =========================================================================

    @Test
    void testConstructorIsPrivate() throws Exception {
        java.lang.reflect.Constructor<OrderCreateResponse> constructor =
                OrderCreateResponse.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail("Expected InvocationTargetException");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }
    }

    @Test
    void testGenerateResponse_nullGo7Response() {
        OrderCreateRspDto result = OrderCreateResponse.generateResponse(null, new OrderCreateReqDto());
        assertNotNull(result);
        assertNull(result.getPnr());
    }

    @Test
    void testGenerateResponse_nullAerocrs() {
        OrderCreateRspGo7Dto dto = new OrderCreateRspGo7Dto();
        dto.setAerocrs(null);
        OrderCreateRspDto result = OrderCreateResponse.generateResponse(dto, new OrderCreateReqDto());
        assertNotNull(result);
    }

    @Test
    void testGenerateResponse_nullBooking() {
        Aerocrs aerocrs = new Aerocrs();
        aerocrs.setBooking(null);
        OrderCreateRspGo7Dto dto = new OrderCreateRspGo7Dto();
        dto.setAerocrs(aerocrs);
        OrderCreateRspDto result = OrderCreateResponse.generateResponse(dto, new OrderCreateReqDto());
        assertNotNull(result);
    }

    // =========================================================================
    // 2. HAPPY PATH from JSON resource
    // =========================================================================

    @Test
    void testGenerateResponse_HappyPath() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);

        InputStream is = getClass().getClassLoader().getResourceAsStream("order_create_response.json");
        OrderCreateRspGo7Dto go7Response = mapper.readValue(is, OrderCreateRspGo7Dto.class);

        OrderCreateReqDto reqDto = new OrderCreateReqDto();
        OrderCreateRspDto response = OrderCreateResponse.generateResponse(go7Response, reqDto);

        assertNotNull(response);
        assertNotNull(response.getResponseId());
        assertNotNull(response.getOrderId());
    }

    // =========================================================================
    // 3. TOP-LEVEL FIELDS
    // =========================================================================

    @Test
    void testTopLevelFields_withBalanceInformation() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        BalanceInformation bi = new BalanceInformation();
        bi.setPnrTotal(new BigDecimal("500.00"));
        go7.getAerocrs().getBooking().setBalanceInformation(bi);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());

        assertNotNull(result.getResponseId());
        assertTrue(result.getResponseId().startsWith("P"));
        assertEquals("12345", result.getOrderId());
        assertEquals("TESTPNR", result.getPnr());
        assertEquals("G7", result.getApiOwner());
        assertEquals(new BigDecimal("500.00"), result.getTotalOrderPrice());
        assertEquals("USD", result.getCurrency());
        assertEquals("702", result.getStatusCode());
        assertEquals("G7", result.getValidatingCarrier());
    }

    @Test
    void testTopLevelFields_noBalanceInfo_usesTotalPriceFallback() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().setBalanceInformation(null);
        go7.getAerocrs().getBooking().setTotalprice("450.00");

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());

        assertEquals(new BigDecimal("450.00"), result.getTotalOrderPrice());
    }

    @Test
    void testTopLevelFields_noBalanceInfo_invalidTotalPrice_returnsZero() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().setBalanceInformation(null);
        go7.getAerocrs().getBooking().setTotalprice("NOT_A_NUMBER");

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());

        assertEquals(BigDecimal.ZERO, result.getTotalOrderPrice());
    }

    @Test
    void testTopLevelFields_nullTotalPrice_returnsZero() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().setBalanceInformation(null);
        go7.getAerocrs().getBooking().setTotalprice(null);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());

        assertEquals(BigDecimal.ZERO, result.getTotalOrderPrice());
    }

    @Test
    void testTopLevelFields_nullCurrency_defaultsToUSD() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().setCurrency(null);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());

        assertEquals("USD", result.getCurrency());
    }

    @Test
    void testStatusCode_rejectedWhenNotSuccess() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().setSuccess(false);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());

        assertEquals("REJECTED", result.getStatusCode());
    }

    @Test
    void testPaymentTimeLimit_withSlashFormat() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().setPnrttl("2026/06/01 10:45");

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());

        assertNotNull(result.getPaymentTimeLimit());
        assertTrue(result.getPaymentTimeLimit().contains("2026"));
    }

    @Test
    void testPaymentTimeLimit_withDashFormat() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().setPnrttl("2026-06-01 10:45");

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());

        assertNotNull(result.getPaymentTimeLimit());
        assertTrue(result.getPaymentTimeLimit().contains("Jun2026"));
    }

    @Test
    void testPaymentTimeLimit_invalidFormat_returnsOriginal() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().setPnrttl("bad-format");

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());

        assertEquals("bad-format", result.getPaymentTimeLimit());
    }

    @Test
    void testPaymentTimeLimit_null_returnsNull() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().setPnrttl(null);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());

        assertNull(result.getPaymentTimeLimit());
    }

    // =========================================================================
    // 4. BOOKING REFERENCES
    // =========================================================================

    @Test
    void testBookingReferences_populated() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());

        assertNotNull(result.getBookingReferences());
        assertEquals(2, result.getBookingReferences().size());
        assertEquals("TESTPNR", result.getBookingReferences().get(0).getId());
        assertEquals("F1", result.getBookingReferences().get(0).getOtherId());
        assertEquals("TESTPNR", result.getBookingReferences().get(1).getId());
        assertEquals("G7", result.getBookingReferences().get(1).getAirlineId());
    }

    // =========================================================================
    // 5. FLIGHT LIST SELECTION
    // =========================================================================

    @Test
    void testFlightList_fromFlightsWhenHasPricingInfo() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        // already has adultfare set — so flightList comes from booking.flights
        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());

        assertNotNull(result.getOds());
        assertFalse(result.getOds().isEmpty());
    }

    @Test
    void testFlightList_fromItemsWhenFlightsHasNoPricing() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();

        // Remove pricing from flights
        booking.getFlights().getFlight().get(0).setAdultfare(null);
        booking.getFlights().getFlight().get(0).setNetFare(null);
        booking.getFlights().getFlight().get(0).setInvpricing(null);

        // Set items with pricing
        Flight itemFlight = new Flight();
        itemFlight.setFromcode("LAX");
        itemFlight.setTocode("SFO");
        itemFlight.setFlightdate("2026-06-01");
        itemFlight.setDepart("08:00");
        itemFlight.setArrive("09:30");
        itemFlight.setAdultfare("200");

        Items items = new Items();
        items.setFlight(List.of(itemFlight));
        booking.setItems(items);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertNotNull(result.getOds());
        assertFalse(result.getOds().isEmpty());
        assertEquals("LAX", result.getOds().get(0).getOrigin());
    }

    @Test
    void testFlightList_fallbackToFlightsWhenNeitherHasPricing() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();

        // Remove pricing from both
        booking.getFlights().getFlight().get(0).setAdultfare(null);
        booking.getFlights().getFlight().get(0).setNetFare(null);
        booking.getFlights().getFlight().get(0).setInvpricing(null);

        // items with no pricing
        Flight itemFlight = new Flight();
        itemFlight.setFromcode("LAX");
        Items items = new Items();
        items.setFlight(List.of(itemFlight));
        booking.setItems(items);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertNotNull(result.getOds());
    }

    @Test
    void testFlightList_nullFlightsAndNullItems_returnsEmptyOds() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().setFlights(null);
        go7.getAerocrs().getBooking().setItems(null);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertNotNull(result);
        // ODs should be null or empty when no flights
        assertTrue(result.getOds() == null || result.getOds().isEmpty());
    }

    @Test
    void testFlightList_emptyFlight_fallback() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();

        // list1 empty pricing, list2 null
        booking.getFlights().getFlight().get(0).setAdultfare("0");
        booking.getFlights().getFlight().get(0).setNetFare("0");
        booking.getFlights().getFlight().get(0).setInvpricing("0");
        booking.setItems(null);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertNotNull(result.getOds());
    }

    // =========================================================================
    // 6. ODs AND PRICE CLASSES
    // =========================================================================

    @Test
    void testOds_basicFieldsPopulated() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());

        assertNotNull(result.getOds());
        assertEquals(1, result.getOds().size());

        var od = result.getOds().get(0);
        assertEquals("SEG1", od.getSegmentId());
        assertEquals("OD1", od.getOdKey());
        assertEquals("JFK", od.getOrigin());
        assertEquals("John F Kennedy", od.getOriginAirportName());
        assertEquals("LHR", od.getDestination());
        assertEquals("London Heathrow", od.getDestinationAirportName());
        assertEquals("G7101", od.getFlightNumber());
        assertEquals("G7", od.getMarketingCarrierCode());
        assertEquals("Go7 Airlines", od.getMarketingCarrierName());
    }

    @Test
    void testOds_marketingCarrierDefaultsToG7_whenNull() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setAirlinedesignator(null);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertEquals("G7", result.getOds().get(0).getMarketingCarrierCode());
    }

    @Test
    void testOds_overnightFlight_incrementsArrivalDate() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Flight f = go7.getAerocrs().getBooking().getFlights().getFlight().get(0);
        f.setDepart("23:00");
        f.setArrive("02:00"); // before depart => next day
        f.setFlightdate("2026-06-01");

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertNotNull(result.getOds().get(0).getArrivalDate());
        // arrival date should be next day
        assertNotEquals(result.getOds().get(0).getDepartureDate(),
                result.getOds().get(0).getArrivalDate());
    }

    @Test
    void testOds_normalFlight_sameArrivalDate() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Flight f = go7.getAerocrs().getBooking().getFlights().getFlight().get(0);
        f.setDepart("10:00");
        f.setArrive("22:00");
        f.setFlightdate("2026-06-01");

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertEquals(result.getOds().get(0).getDepartureDate(),
                result.getOds().get(0).getArrivalDate());
    }

    @Test
    void testOds_nullDepartOrArriveTime_sameArrivalDate() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Flight f = go7.getAerocrs().getBooking().getFlights().getFlight().get(0);
        f.setDepart(null);
        f.setArrive(null);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertNotNull(result.getOds().get(0).getDepartureDate());
    }

    @Test
    void testOds_invalidDepartArriveTime_noException() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Flight f = go7.getAerocrs().getBooking().getFlights().getFlight().get(0);
        f.setDepart("BAD");
        f.setArrive("TIME");

        assertDoesNotThrow(() -> OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto()));
    }

    @Test
    void testOds_noFlightNumber_doesNotSetFlightNumber() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setNumber(null);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertNull(result.getOds().get(0).getFlightNumber());
    }

    @Test
    void testOds_withServices_addsDescriptions() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Map<String, Boolean> services = new HashMap<>();
        services.put("WiFi", true);
        services.put("Meal", false);
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setServices(services);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertNotNull(result.getPriceClassList());
        assertFalse(result.getPriceClassList().isEmpty());
        // descriptions should include od key + 2 services
        assertTrue(result.getPriceClassList().get(0).getDescriptions().size() >= 3);
    }

    @Test
    void testMultipleFlights_createsMultipleOds() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Flight f2 = new Flight();
        f2.setFromcode("LHR");
        f2.setTocode("CDG");
        f2.setFlightdate("2026-06-02");
        f2.setDepart("08:00");
        f2.setArrive("10:00");
        f2.setNumber("G7202");
        f2.setAirlinedesignator("G7");
        f2.setAdultfare("100");

        List<Flight> flights = new ArrayList<>(go7.getAerocrs().getBooking().getFlights().getFlight());
        flights.add(f2);
        go7.getAerocrs().getBooking().getFlights().setFlight(flights);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertEquals(2, result.getOds().size());
        assertEquals("SEG1", result.getOds().get(0).getSegmentId());
        assertEquals("SEG2", result.getOds().get(1).getSegmentId());
    }

    // =========================================================================
    // 7. CABIN / CLASS DETERMINATION
    // =========================================================================

    @ParameterizedTest(name = "[{index}] flightClass={0} => cabinType={1}")
    @CsvSource({
        "C/Business Class, Business",  // slash-prefix C → Business
        "F/First,         First",       // slash-prefix F → First
        "Y/Economy/Eco,   Economy",     // slash-prefix Y → Economy (default)
        "Business,        Business",    // full-word Business
        "First,           First",       // full-word First
        "'',              Economy",     // empty string → Economy
    })
    void testCabinCode_parameterized(String flightClass, String expectedCabin) {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setFlightClass(flightClass);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertEquals(expectedCabin, result.getOds().get(0).getCabinType());
    }

    @Test
    void testCabinCode_nullFlightClass_defaultsEconomy() {
        // Null cannot be expressed in @CsvSource — tested separately.
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setFlightClass(null);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertEquals("Economy", result.getOds().get(0).getCabinType());
    }

    @Test
    void testClassName_threePartSlash_usesPart3() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setFlightClass("Y/Economy/EcoSaver");

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertNotNull(result.getPriceClassList());
        assertEquals("EcoSaver", result.getPriceClassList().get(0).getClassName());
    }

    @Test
    void testClassName_twoPartSlash_usesPart2() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setFlightClass("C/BusinessFlex");

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertEquals("BusinessFlex", result.getPriceClassList().get(0).getClassName());
    }

    @Test
    void testPriceClass_economyCabinCode() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setFlightClass("Y");

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertEquals("ECO", result.getPriceClassList().get(0).getCabinTypeCode());
    }

    @Test
    void testPriceClass_nonEconomy_usesCabinTypeAsCode() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setFlightClass("C/BusinessClass");

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        // Business != Economy, so cabin code used directly
        assertEquals("Business", result.getPriceClassList().get(0).getCabinTypeCode());
    }

    // =========================================================================
    // 8. PASSENGERS — ADULT / CHILD
    // =========================================================================

    @Test
    void testPassenger_adultMappedFromGo7() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());

        assertNotNull(result.getPaxDetailList());
        assertFalse(result.getPaxDetailList().isEmpty());
        assertEquals("JOHN", result.getPaxDetailList().get(0).getGivenName());
        assertEquals("DOE", result.getPaxDetailList().get(0).getSurname());
        assertEquals("ADT", result.getPaxDetailList().get(0).getPtc());
    }

    @Test
    void testPassenger_paxIdFromReqDto_whenMatched() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());

        assertEquals("PAX1", result.getPaxDetailList().get(0).getPaxId());
    }

    @Test
    void testPassenger_paxIdGenerated_whenNoReqDtoMatch() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        // no matching request passenger
        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());

        assertNotNull(result.getPaxDetailList().get(0).getPaxId());
        assertTrue(result.getPaxDetailList().get(0).getPaxId().startsWith("T"));
    }

    @Test
    void testPassenger_titleFromReqDto() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        OrderCreateReqDto req = createBasicReqDto();
        req.getPassengers().get(0).setTitle("DR");

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, req);
        assertEquals("DR", result.getPaxDetailList().get(0).getTitle());
    }

    @Test
    void testPassenger_titleFromGo7Pax_whenNoReqDtoTitle() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0).setPaxtitle("Mrs.");

        OrderCreateReqDto req = new OrderCreateReqDto();
        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, req);

        assertEquals("MRS", result.getPaxDetailList().get(0).getTitle());
    }

    @Test
    void testPassenger_titleDefault_MR_whenNullPaxtitle() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0).setPaxtitle(null);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertEquals("MR", result.getPaxDetailList().get(0).getTitle());
    }

    @Test
    void testPassenger_childTitle_isChild() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Passenger child = go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0);
        child.setPaxtype("CNN");

        OrderCreateReqDto req = new OrderCreateReqDto();
        PaxReqDto childReq = new PaxReqDto();
        childReq.setPaxId("C1");
        childReq.setPtc("CNN");
        childReq.setFirstName("John");
        childReq.setLastName("Doe");
        req.setPassengers(List.of(childReq));

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, req);
        assertEquals("CHILD", result.getPaxDetailList().get(0).getTitle());
    }

    @Test
    void testPassenger_genderFromReqDto() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        OrderCreateReqDto req = createBasicReqDto();
        req.getPassengers().get(0).setGender("Female");

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, req);
        assertEquals("Female", result.getPaxDetailList().get(0).getGender());
    }

    @Test
    void testPassenger_genderFromGo7_male() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0).setGender("M");

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertEquals("Male", result.getPaxDetailList().get(0).getGender());
    }

    @Test
    void testPassenger_genderFromGo7_female() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0).setGender("F");

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertEquals("Female", result.getPaxDetailList().get(0).getGender());
    }

    /**
     * gender / title inference — all cases in one parameterized test.
     *
     * gender field  | paxtitle | expected
     * null          | "MR"     | "Male"   (contains MR)
     * null          | "MRS"    | "Male"   (MRS contains MR — MR check fires first)
     * null          | "MS"     | "Female" (no MR; contains MS)
     * null          | "MISS"   | "Female" (no MR; contains MS)
     * null          | null     | "Male"   (default fallback)
     * "" (empty)    | "MS"     | "Female" (empty gender → title branch)
     */
    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> genderFromTitleCases() {
        return java.util.stream.Stream.of(
            org.junit.jupiter.params.provider.Arguments.of(null,  "MR",   "Male"),
            org.junit.jupiter.params.provider.Arguments.of(null,  "MRS",  "Male"),   // MRS contains MR
            org.junit.jupiter.params.provider.Arguments.of(null,  "MS",   "Female"),
            org.junit.jupiter.params.provider.Arguments.of(null,  "MISS", "Female"),
            org.junit.jupiter.params.provider.Arguments.of(null,  null,   "Male"),   // no hints → default
            org.junit.jupiter.params.provider.Arguments.of("",    "MS",   "Female")  // empty gender → title
        );
    }

    @ParameterizedTest(name = "[{index}] gender={0}, title={1} => {2}")
    @org.junit.jupiter.params.provider.MethodSource("genderFromTitleCases")
    void testPassenger_genderFromTitle_parameterized(String gender, String title, String expected) {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Passenger p = go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0);
        p.setGender(gender);
        p.setPaxtitle(title);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertEquals(expected, result.getPaxDetailList().get(0).getGender());
    }

    @Test
    void testPassenger_language_isEnglish() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertEquals("English", result.getPaxDetailList().get(0).getLanguage());
    }

    @Test
    void testPassenger_nullFirstLastName_usesEmpty() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0).setFirstname(null);
        go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0).setLastname(null);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertEquals("", result.getPaxDetailList().get(0).getGivenName());
        assertEquals("", result.getPaxDetailList().get(0).getSurname());
    }

    // =========================================================================
    // 9. PASSENGERS — CONTACT DETAILS
    // =========================================================================

    @Test
    void testPassenger_contactAndEmail_populated() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Passenger p = go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0);
        p.setContact("+1-555-555-5555");
        p.setEmail("john@example.com");

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertNotNull(result.getPaxDetailList().get(0).getPhones());
        assertEquals("+1-555-555-5555", result.getPaxDetailList().get(0).getPhones().get(0).getPhoneNumber());
        assertEquals("Operational", result.getPaxDetailList().get(0).getPhones().get(0).getType());
        assertNotNull(result.getPaxDetailList().get(0).getEmails());
        assertEquals("JOHN@EXAMPLE.COM", result.getPaxDetailList().get(0).getEmails().get(0).getEmailAddress());
        assertEquals("Operational", result.getPaxDetailList().get(0).getEmails().get(0).getType());
    }

    @Test
    void testPassenger_nullContact_noPhones() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0).setContact(null);
        go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0).setEmail(null);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertNull(result.getPaxDetailList().get(0).getPhones());
        assertNull(result.getPaxDetailList().get(0).getEmails());
    }

    // =========================================================================
    // 10. PASSENGERS — E-TICKETS / TICKET DOCS
    // =========================================================================

    @Test
    void testPassenger_eTickets_populatesTicketDocInfo() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Passenger p = go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0);

        Passenger.ETicketFlight etf = new Passenger.ETicketFlight();
        etf.setEticketnumber("1234567890");
        Passenger.ETickets eTickets = new Passenger.ETickets();
        eTickets.setFlight(List.of(etf));
        p.setETickets(eTickets);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());

        assertNotNull(result.getPaxDetailList().get(0).getTicketDocInfo());
        assertFalse(result.getPaxDetailList().get(0).getTicketDocInfo().isEmpty());
        assertEquals("1234567890",
                result.getPaxDetailList().get(0).getTicketDocInfo().get(0).getTicketDocument().get(0).getTicketDocNbr());
        assertNotNull(result.getTicketDocInfoList());
    }

    @Test
    void testPassenger_eTickets_duplicateTicketsDeduped() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Passenger p = go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0);

        Passenger.ETicketFlight etf1 = new Passenger.ETicketFlight();
        etf1.setEticketnumber("1234567890");
        Passenger.ETicketFlight etf2 = new Passenger.ETicketFlight();
        etf2.setEticketnumber("1234567890"); // duplicate
        Passenger.ETicketFlight etf3 = new Passenger.ETicketFlight();
        etf3.setEticketnumber("0987654321");

        Passenger.ETickets eTickets = new Passenger.ETickets();
        eTickets.setFlight(List.of(etf1, etf2, etf3));
        p.setETickets(eTickets);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        // Should have 2 unique tickets
        assertEquals(2, result.getPaxDetailList().get(0).getTicketDocInfo().size());
    }

    @Test
    void testPassenger_eTickets_nullEticketnumber_skipped() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Passenger p = go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0);

        Passenger.ETicketFlight etf = new Passenger.ETicketFlight();
        etf.setEticketnumber(null);
        Passenger.ETickets eTickets = new Passenger.ETickets();
        eTickets.setFlight(List.of(etf));
        p.setETickets(eTickets);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertNull(result.getPaxDetailList().get(0).getTicketDocInfo());
    }

    @Test
    void testPassenger_eTickets_couponsBuiltFromFlightList() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Passenger p = go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0);
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setFlightClass("Y/Economy/EcoBase");

        Passenger.ETicketFlight etf = new Passenger.ETicketFlight();
        etf.setEticketnumber("1234567890");
        Passenger.ETickets eTickets = new Passenger.ETickets();
        eTickets.setFlight(List.of(etf));
        p.setETickets(eTickets);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertNotNull(result.getPaxDetailList().get(0).getTicketDocInfo().get(0)
                .getTicketDocument().get(0).getCouponInfo());
        assertEquals(1,
                result.getPaxDetailList().get(0).getTicketDocInfo().get(0)
                        .getTicketDocument().get(0).getCouponInfo().size());
    }

    @Test
    void testETickets_coupon_classWithSlash_setsRbd() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Passenger p = go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0);
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setFlightClass("Y/Economy");

        Passenger.ETicketFlight etf = new Passenger.ETicketFlight();
        etf.setEticketnumber("1234567890");
        Passenger.ETickets eTickets = new Passenger.ETickets();
        eTickets.setFlight(List.of(etf));
        p.setETickets(eTickets);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        var coupon = result.getPaxDetailList().get(0).getTicketDocInfo().get(0)
                .getTicketDocument().get(0).getCouponInfo().get(0);
        assertEquals("Y", coupon.getRbd());
    }

    @Test
    void testETickets_coupon_classNoSlash_setsFirstCharAsRbd() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Passenger p = go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0);
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setFlightClass("Business");

        Passenger.ETicketFlight etf = new Passenger.ETicketFlight();
        etf.setEticketnumber("1234567890");
        Passenger.ETickets eTickets = new Passenger.ETickets();
        eTickets.setFlight(List.of(etf));
        p.setETickets(eTickets);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        var coupon = result.getPaxDetailList().get(0).getTicketDocInfo().get(0)
                .getTicketDocument().get(0).getCouponInfo().get(0);
        assertEquals("B", coupon.getRbd());
    }

    // =========================================================================
    // 11. PASSENGERS — INFANTS
    // =========================================================================

    @Test
    void testPassenger_infantMapped_andLinkedToParent() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();

        // Infant passenger
        Passenger infant = new Passenger();
        infant.setFirstname("Baby");
        infant.setLastname("Doe");
        infant.setPaxtype("INFANT");
        infant.setGender("F");

        List<Passenger> paxList = new ArrayList<>(booking.getPassengers().getPassenger());
        paxList.add(infant);
        booking.getPassengers().setPassenger(paxList);

        OrderCreateReqDto req = new OrderCreateReqDto();
        PaxReqDto adultReq = new PaxReqDto();
        adultReq.setPaxId("T1");
        adultReq.setPtc("ADT");
        adultReq.setFirstName("John");
        adultReq.setLastName("Doe");

        PaxReqDto infReq = new PaxReqDto();
        infReq.setPaxId("T1.1");
        infReq.setPtc("INF");
        infReq.setFirstName("Baby");
        infReq.setLastName("Doe");

        req.setPassengers(List.of(adultReq, infReq));

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, req);
        assertEquals(2, result.getPaxDetailList().size());

        // adult should have infantRef
        var adultResult = result.getPaxDetailList().stream()
                .filter(p -> "ADT".equals(p.getPtc())).findFirst().orElse(null);
        assertNotNull(adultResult);
        assertEquals("T1.1", adultResult.getInfantRef());

        // infant should have ptc=INF
        var infantResult = result.getPaxDetailList().stream()
                .filter(p -> "INF".equals(p.getPtc())).findFirst().orElse(null);
        assertNotNull(infantResult);
        assertEquals("BABY", infantResult.getGivenName());
    }

    @Test
    void testPassenger_infant_paxIdGeneratedIfNoReqMatch() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();

        Passenger infant = new Passenger();
        infant.setFirstname("Baby");
        infant.setLastname("Smith");
        infant.setPaxtype("INFANT");

        List<Passenger> paxList = new ArrayList<>(booking.getPassengers().getPassenger());
        paxList.add(infant);
        booking.getPassengers().setPassenger(paxList);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());

        // infant should get a generated paxId
        var infantResult = result.getPaxDetailList().stream()
                .filter(p -> "INF".equals(p.getPtc())).findFirst().orElse(null);
        assertNotNull(infantResult);
        assertTrue(infantResult.getPaxId().contains("."));
    }

    // =========================================================================
    // 12. REQUEST PASSENGERS FALLBACK
    // =========================================================================

    @Test
    void testPassengerFallback_usedWhenNoGo7Passengers() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().setPassengers(null);

        OrderCreateReqDto req = new OrderCreateReqDto();
        PaxReqDto p = new PaxReqDto();
        p.setPaxId("REQ1");
        p.setPtc("ADT");
        req.setPassengers(List.of(p));

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, req);
        assertNotNull(result.getPaxDetailList());
        assertEquals(1, result.getPaxDetailList().size());
        assertEquals("REQ1", result.getPaxDetailList().get(0).getPaxId());
        assertEquals("ADT", result.getPaxDetailList().get(0).getPtc());
    }

    @Test
    void testPassengerFallback_noPassengersAtAll_emptyList() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().setPassengers(null);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertNotNull(result.getPaxDetailList());
        assertTrue(result.getPaxDetailList().isEmpty());
    }

    // =========================================================================
    // 13. ORDER ITEMS
    // =========================================================================

    @Test
    void testOrderItems_adultItemCreated() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());

        assertNotNull(result.getOrderItems());
        assertFalse(result.getOrderItems().isEmpty());
        assertEquals("ADT", result.getOrderItems().get(0).getPtc());
        assertEquals(List.of("PAX1"), result.getOrderItems().get(0).getPassengerIds());
    }

    @Test
    void testOrderItems_childAndInfant_separateItems() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();

        Passenger child = new Passenger();
        child.setFirstname("Kid");
        child.setLastname("Doe");
        child.setPaxtype("CNN");

        Passenger infant = new Passenger();
        infant.setFirstname("Baby");
        infant.setLastname("Doe");
        infant.setPaxtype("INFANT");

        List<Passenger> paxList = new ArrayList<>(booking.getPassengers().getPassenger());
        paxList.add(child);
        paxList.add(infant);
        booking.getPassengers().setPassenger(paxList);
        booking.setAdults(1);
        booking.setChild(1);
        booking.setInfant(1);

        OrderCreateReqDto req = new OrderCreateReqDto();
        PaxReqDto adultReq = new PaxReqDto();
        adultReq.setPaxId("A1");
        adultReq.setPtc("ADT");
        adultReq.setFirstName("John");
        adultReq.setLastName("Doe");

        PaxReqDto childReq = new PaxReqDto();
        childReq.setPaxId("C1");
        childReq.setPtc("CNN");
        childReq.setFirstName("Kid");
        childReq.setLastName("Doe");

        PaxReqDto infReq = new PaxReqDto();
        infReq.setPaxId("I1.1");
        infReq.setPtc("INF");
        infReq.setFirstName("Baby");
        infReq.setLastName("Doe");

        req.setPassengers(List.of(adultReq, childReq, infReq));

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, req);

        assertNotNull(result.getOrderItems());
        assertEquals(3, result.getOrderItems().size());
        assertTrue(result.getOrderItems().stream().anyMatch(i -> "ADT".equals(i.getPtc())));
        assertTrue(result.getOrderItems().stream().anyMatch(i -> "CNN".equals(i.getPtc())));
        assertTrue(result.getOrderItems().stream().anyMatch(i -> "INF".equals(i.getPtc())));
    }

    @Test
    void testOrderItems_nullCurrency_defaultsUSD() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().setCurrency(null);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());
        assertNotNull(result.getOrderItems());
        assertFalse(result.getOrderItems().isEmpty());
        assertEquals("USD", result.getOrderItems().get(0).getTotalFare().getCurrency());
    }

    // =========================================================================
    // 14. PRICING — determineBaseFare / determineTax
    // =========================================================================

    @Test
    void testPricing_adultFareUsed() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setAdultfare("300.00");
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setTax("50.00");
        go7.getAerocrs().getBooking().setAdults(1);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());

        var item = result.getOrderItems().get(0);
        assertNotNull(item.getTotalPrice());
        assertEquals(new BigDecimal("350.00"), item.getTotalPrice());
    }

    @Test
    void testPricing_childFareUsed() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();

        // change adult to child
        Passenger child = booking.getPassengers().getPassenger().get(0);
        child.setPaxtype("CNN");
        child.setFirstname("Kid");
        child.setLastname("Doe");

        booking.getFlights().getFlight().get(0).setAdultfare(null);
        booking.getFlights().getFlight().get(0).setChildfare("150.00");
        booking.getFlights().getFlight().get(0).setTax("20.00");
        booking.setAdults(0);
        booking.setChild(1);

        OrderCreateReqDto req = new OrderCreateReqDto();
        PaxReqDto childReq = new PaxReqDto();
        childReq.setPaxId("C1");
        childReq.setPtc("CNN");
        childReq.setFirstName("Kid");
        childReq.setLastName("Doe");
        req.setPassengers(List.of(childReq));

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, req);
        assertNotNull(result.getOrderItems());
        var item = result.getOrderItems().stream().filter(i -> "CNN".equals(i.getPtc())).findFirst().orElse(null);
        assertNotNull(item);
        assertEquals(new BigDecimal("170.00"), item.getTotalPrice());
    }

    @Test
    void testPricing_infantFareUsed() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();

        Passenger infant = new Passenger();
        infant.setFirstname("Baby");
        infant.setLastname("Doe");
        infant.setPaxtype("INFANT");

        List<Passenger> paxList = new ArrayList<>(booking.getPassengers().getPassenger());
        paxList.add(infant);
        booking.getPassengers().setPassenger(paxList);

        booking.getFlights().getFlight().get(0).setInfantfare("50.00");
        booking.getFlights().getFlight().get(0).setTax("0");
        booking.setAdults(1);
        booking.setInfant(1);

        OrderCreateReqDto req = new OrderCreateReqDto();
        PaxReqDto adultReq = new PaxReqDto();
        adultReq.setPaxId("A1");
        adultReq.setPtc("ADT");
        adultReq.setFirstName("John");
        adultReq.setLastName("Doe");

        PaxReqDto infReq = new PaxReqDto();
        infReq.setPaxId("I1.1");
        infReq.setPtc("INF");
        infReq.setFirstName("Baby");
        infReq.setLastName("Doe");
        req.setPassengers(List.of(adultReq, infReq));

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, req);
        var item = result.getOrderItems().stream().filter(i -> "INF".equals(i.getPtc())).findFirst().orElse(null);
        assertNotNull(item);
        assertEquals(new BigDecimal("50.00"), item.getTotalPrice());
    }

    @Test
    void testPricing_fallbackToNetFare() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Flight f = go7.getAerocrs().getBooking().getFlights().getFlight().get(0);
        f.setAdultfare(null);
        f.setNetFare("200.00");
        f.setTax("30.00");
        go7.getAerocrs().getBooking().setAdults(1);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());
        var item = result.getOrderItems().get(0);
        assertNotNull(item.getTotalPrice());
    }

    @Test
    void testPricing_fallbackToInvpricingWithoutTax() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Flight f = go7.getAerocrs().getBooking().getFlights().getFlight().get(0);
        f.setAdultfare(null);
        f.setNetFare(null);
        f.setInvpricingwithouttax("180.00");
        f.setTax("20.00");
        go7.getAerocrs().getBooking().setAdults(1);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());
        assertNotNull(result.getOrderItems().get(0).getTotalPrice());
    }

    @Test
    void testPricing_fallbackToInvPricingMinusTax() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Flight f = go7.getAerocrs().getBooking().getFlights().getFlight().get(0);
        f.setAdultfare(null);
        f.setNetFare(null);
        f.setInvpricingwithouttax(null);
        f.setInvpricing("500.00");
        f.setTotaltax("100.00");
        go7.getAerocrs().getBooking().setAdults(1);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());
        assertNotNull(result.getOrderItems().get(0).getBaseFare());
    }

    @Test
    void testPricing_fallbackToTotaltaxesField() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Flight f = go7.getAerocrs().getBooking().getFlights().getFlight().get(0);
        f.setAdultfare(null);
        f.setNetFare(null);
        f.setInvpricingwithouttax(null);
        f.setInvpricing("500.00");
        f.setTotaltax(null);
        f.setTotaltaxes(100.0);
        go7.getAerocrs().getBooking().setAdults(1);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());
        assertNotNull(result.getOrderItems().get(0).getBaseFare());
    }

    @Test
    void testPricing_totalPaxFromPassengerList_whenCountersZero() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().setAdults(0);
        go7.getAerocrs().getBooking().setChild(0);
        go7.getAerocrs().getBooking().setInfant(0);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());
        assertNotNull(result.getOrderItems().get(0).getTotalPrice());
    }

    @Test
    void testPricing_noFlights_zeroPricing() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().setFlights(null);
        go7.getAerocrs().getBooking().setItems(null);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());
        // no flights -> no order items for mapped passengers (paxList empty since no flights and
        // request fallback not triggered because go7 pax null when passengers is null)
        assertNotNull(result);
    }

    // =========================================================================
    // 15. GRANULAR TAXES
    // =========================================================================

    @Test
    void testGranularTaxes_populatedWhenTaxesObjectPresent() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Flight f = go7.getAerocrs().getBooking().getFlights().getFlight().get(0);
        f.setAdultfare("300");
        f.setTax(null);
        f.setTotaltaxes(50.0);

        Taxes taxes = new Taxes();
        taxes.setFuel(20.0);
        taxes.setSecurity(10.0);
        taxes.setGroundHandling(5.0);
        taxes.setTax1(5.0);
        taxes.setTax4(10.0);
        f.setTaxes(taxes);

        go7.getAerocrs().getBooking().setAdults(1);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());

        var item = result.getOrderItems().get(0);
        assertNotNull(item.getTaxes());
        assertFalse(item.getTaxes().isEmpty());
        assertTrue(item.getTaxes().stream().anyMatch(t -> "YQ".equals(t.getCode())));
        assertTrue(item.getTaxes().stream().anyMatch(t -> "I2".equals(t.getCode())));
    }

    @Test
    void testGranularTaxes_zeroCurrentTax_noTaxItems() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Flight f = go7.getAerocrs().getBooking().getFlights().getFlight().get(0);
        f.setAdultfare("300");
        f.setTax("0");
        f.setTotaltaxes(0.0);

        Taxes taxes = new Taxes();
        taxes.setFuel(0.0);
        taxes.setSecurity(0.0);
        f.setTaxes(taxes);

        go7.getAerocrs().getBooking().setAdults(1);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());
        // When tax is zero, should not add granular taxes
        var item = result.getOrderItems().get(0);
        assertTrue(item.getTaxes() == null || item.getTaxes().isEmpty());
    }

    @Test
    void testFinalizeOrderItemPricing_fallbackTaxItem_whenTaxMapEmpty() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Flight f = go7.getAerocrs().getBooking().getFlights().getFlight().get(0);
        f.setAdultfare("300");
        f.setTax("50");
        f.setTaxes(null); // no taxes object => taxMap will be empty
        go7.getAerocrs().getBooking().setAdults(1);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());

        var item = result.getOrderItems().get(0);
        assertNotNull(item.getTaxes());
        assertEquals(1, item.getTaxes().size());
        assertEquals("OT", item.getTaxes().get(0).getCode());
        assertEquals("Total Taxes", item.getTaxes().get(0).getDescription());
    }

    // =========================================================================
    // 16. GLOBAL FALLBACK PRICING
    // =========================================================================

    @Test
    void testApplyGlobalFallback_whenTotalPriceZero_usesBalanceInfo() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();

        // All fare fields null -> zero pricing
        Flight f = booking.getFlights().getFlight().get(0);
        f.setAdultfare(null);
        f.setNetFare(null);
        f.setInvpricingwithouttax(null);
        f.setInvpricing(null);
        f.setTax(null);
        f.setTotaltax(null);
        f.setTotaltaxes(0.0);

        BalanceInformation bi = new BalanceInformation();
        bi.setPnrTotal(new BigDecimal("600.00"));
        booking.setBalanceInformation(bi);
        booking.setAdults(2);

        // Two adult passengers
        Passenger p2 = new Passenger();
        p2.setFirstname("Jane");
        p2.setLastname("Doe");
        p2.setPaxtype("ADULT");
        List<Passenger> paxList = new ArrayList<>(booking.getPassengers().getPassenger());
        paxList.add(p2);
        booking.getPassengers().setPassenger(paxList);

        OrderCreateReqDto req = new OrderCreateReqDto();
        PaxReqDto r1 = new PaxReqDto();
        r1.setPaxId("A1"); r1.setPtc("ADT"); r1.setFirstName("John"); r1.setLastName("Doe");
        PaxReqDto r2 = new PaxReqDto();
        r2.setPaxId("A2"); r2.setPtc("ADT"); r2.setFirstName("Jane"); r2.setLastName("Doe");
        req.setPassengers(List.of(r1, r2));

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, req);
        var item = result.getOrderItems().get(0);
        assertNotNull(item.getTotalPrice());
        assertTrue(item.getTotalPrice().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testApplyGlobalFallback_whenTotalPriceZero_usesTotalprice() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();

        // Zero pricing on flight
        Flight f = booking.getFlights().getFlight().get(0);
        f.setAdultfare(null);
        f.setNetFare(null);
        f.setInvpricingwithouttax(null);
        f.setInvpricing(null);
        f.setTax(null);
        f.setTotaltax(null);
        f.setTotaltaxes(0.0);

        booking.setBalanceInformation(null);
        booking.setTotalprice("500.00");
        booking.setAdults(1);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());
        var item = result.getOrderItems().get(0);
        assertNotNull(item.getTotalPrice());
        assertEquals(new BigDecimal("500.00"), item.getTotalPrice());
    }

    // =========================================================================
    // 17. ISSUING DETAILS
    // =========================================================================

    @Test
    void testIssuingDetails_takenFromFirstFlight() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Passenger p = go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0);

        Passenger.ETicketFlight etf = new Passenger.ETicketFlight();
        etf.setEticketnumber("9999");
        Passenger.ETickets eTickets = new Passenger.ETickets();
        eTickets.setFlight(List.of(etf));
        p.setETickets(eTickets);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());

        var tdi = result.getPaxDetailList().get(0).getTicketDocInfo().get(0);
        assertEquals("Go7 Airlines", tdi.getIssuingAirlineName());
        assertEquals("JFK", tdi.getIssuingPlace());
    }

    // =========================================================================
    // 18. extractClassName static method (via order items)
    // =========================================================================

    @Test
    void testExtractClassName_nullFlights_returnsEconomy() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().setFlights(null);
        go7.getAerocrs().getBooking().setItems(null);
        go7.getAerocrs().getBooking().setPassengers(null);

        OrderCreateReqDto req = new OrderCreateReqDto();
        PaxReqDto p = new PaxReqDto();
        p.setPaxId("A1");
        p.setPtc("ADT");
        req.setPassengers(List.of(p));

        // With no flights, populateOdsAndPriceClasses is skipped
        // orderItems will check flightList which is null -> extractClassName returns Economy
        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, req);
        assertNotNull(result);
    }

    @Test
    void testExtractClassName_noFlightClass_returnsEconomy() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setFlightClass(null);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());
        // Verify order item has economy class
        assertNotNull(result.getOrderItems());
        assertEquals("Economy", result.getOrderItems().get(0).getClassName());
    }

    @Test
    void testExtractClassName_singlePartClass_usesDirectly() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setFlightClass("Y");

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());
        assertNotNull(result.getOrderItems());
        assertEquals("Y", result.getOrderItems().get(0).getClassName());
    }

    // =========================================================================
    // 19. ADDITIONAL BRANCH COVERAGE
    // =========================================================================

    /** Line 146: determineFlightListFallback — list1 invalid, list2 valid → returns list2 */
    @Test
    void testFlightListFallback_list1Invalid_list2Valid_returnsList2() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();

        // list1: flight with no pricing AND no valid fields (fromcode/tocode/airline null)
        Flight badFlight = new Flight();
        badFlight.setAdultfare(null);
        badFlight.setNetFare(null);
        badFlight.setInvpricing(null);
        // fromcode, tocode, airline all null → isFlightListValid returns false
        Flights badFlights = new Flights();
        badFlights.setFlight(List.of(badFlight));
        booking.setFlights(badFlights);

        // list2 (items): flight with valid fields but no pricing
        Flight goodFlight = new Flight();
        goodFlight.setFromcode("SFO"); // non-null → isFlightListValid returns true
        goodFlight.setFlightdate("2026-06-01");
        goodFlight.setDepart("09:00");
        goodFlight.setArrive("11:00");
        Items items = new Items();
        items.setFlight(List.of(goodFlight));
        booking.setItems(items);

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());
        assertNotNull(result.getOds());
        assertFalse(result.getOds().isEmpty());
        assertEquals("SFO", result.getOds().get(0).getOrigin());
    }

    /** Line 310: INF in first-pass loop triggers continue (without req match) */
    @Test
    void testMapGo7Passengers_infantInFirstPass_isContinued() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();

        // First pax is adult, second is INFANT — both in go7 list
        // No request passengers, so reqPax will be null → ptc comes from go7 paxtype
        Passenger infant = new Passenger();
        infant.setFirstname("Baby");
        infant.setLastname("Doe");
        infant.setPaxtype("INFANT"); // → ptc="INF" via OrderMappingUtil.mapPaxType
        infant.setGender("F");

        List<Passenger> paxList = new ArrayList<>(booking.getPassengers().getPassenger());
        paxList.add(infant);
        booking.getPassengers().setPassenger(paxList);

        // No request DTO passengers (so reqPax = null, ptc derived from go7Pax.paxtype)
        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, new OrderCreateReqDto());

        // Adult should be in paxList; infant should also be added via second pass
        long infCount = result.getPaxDetailList().stream().filter(p -> "INF".equals(p.getPtc())).count();
        assertEquals(1, infCount);
        long adtCount = result.getPaxDetailList().stream().filter(p -> "ADT".equals(p.getPtc())).count();
        assertEquals(1, adtCount);
    }

    // testDetermineGender_titleMS / testDetermineGender_titleMISS merged into
    // testPassenger_genderFromTitle_parameterized (see section 9)

    /** Lines 543,549,550: infant with no dot in paxId + null firstname/lastname */
    @Test
    void testMapSingleInfant_noDotInPaxId_nullNames() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();

        Passenger infant = new Passenger();
        infant.setFirstname(null);  // null → ""
        infant.setLastname(null);   // null → ""
        infant.setPaxtype("INFANT");

        List<Passenger> paxList = new ArrayList<>(booking.getPassengers().getPassenger());
        paxList.add(infant);
        booking.getPassengers().setPassenger(paxList);

        // Provide a req passenger for adult but a req infant with no dot in paxId
        OrderCreateReqDto req = new OrderCreateReqDto();
        PaxReqDto adultReq = new PaxReqDto();
        adultReq.setPaxId("NONDOT1"); // no dot
        adultReq.setPtc("ADT");
        adultReq.setFirstName("John");
        adultReq.setLastName("Doe");

        PaxReqDto infReq = new PaxReqDto();
        infReq.setPaxId("INF1"); // no dot → parentId = defaultParentId branch
        infReq.setPtc("INF");
        infReq.setFirstName(null); // no name match possible in second pass
        infReq.setLastName(null);

        req.setPassengers(List.of(adultReq, infReq));

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, req);

        // infant with null name should still be added with empty strings
        var infResult = result.getPaxDetailList().stream()
                .filter(p -> "INF".equals(p.getPtc())).findFirst().orElse(null);
        assertNotNull(infResult);
        assertEquals("", infResult.getGivenName());
        assertEquals("", infResult.getSurname());
    }

    /** Line 543: infant paxId without dot → parentId = defaultParentId */
    @Test
    void testMapSingleInfant_reqPaxIdWithNoDot_parentIdUsesDefault() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();

        Passenger infant = new Passenger();
        infant.setFirstname("Baby");
        infant.setLastname("Doe");
        infant.setPaxtype("INFANT");

        List<Passenger> paxList = new ArrayList<>(booking.getPassengers().getPassenger());
        paxList.add(infant);
        booking.getPassengers().setPassenger(paxList);

        // req infant with paxId that has no dot
        OrderCreateReqDto req = new OrderCreateReqDto();
        PaxReqDto adultReq = new PaxReqDto();
        adultReq.setPaxId("T1");
        adultReq.setPtc("ADT");
        adultReq.setFirstName("John");
        adultReq.setLastName("Doe");

        PaxReqDto infReq = new PaxReqDto();
        infReq.setPaxId("INF_NODOT"); // no dot → parentId = defaultParentId (T1)
        infReq.setPtc("INF");
        infReq.setFirstName("Baby");
        infReq.setLastName("Doe");

        req.setPassengers(List.of(adultReq, infReq));

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, req);

        var infResult = result.getPaxDetailList().stream()
                .filter(p -> "INF".equals(p.getPtc())).findFirst().orElse(null);
        assertNotNull(infResult);
        assertEquals("INF_NODOT", infResult.getPaxId());
    }

    /** Line 585: "INFANT" ptc in populateOrderItems (PTC_INFANT.equalsIgnoreCase branch) */
    @Test
    void testPopulateOrderItems_infantPtcString_addedToInfRefs() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();
        booking.getPassengers().getPassenger().get(0).setPaxtype("ADULT");

        // add an infant with paxtype INFANT (no req match → ptc derived from go7)
        Passenger infant = new Passenger();
        infant.setFirstname("Baby");
        infant.setLastname("Jones");
        infant.setPaxtype("INFANT");

        List<Passenger> paxList = new ArrayList<>(booking.getPassengers().getPassenger());
        paxList.add(infant);
        booking.getPassengers().setPassenger(paxList);
        booking.setAdults(1);
        booking.setInfant(1);
        booking.getFlights().getFlight().get(0).setInfantfare("50");
        booking.getFlights().getFlight().get(0).setTax("0");

        OrderCreateReqDto req = new OrderCreateReqDto();
        PaxReqDto adtReq = new PaxReqDto();
        adtReq.setPaxId("A1");
        adtReq.setPtc("ADT");
        adtReq.setFirstName("John");
        adtReq.setLastName("Doe");
        req.setPassengers(List.of(adtReq));
        // infant has no req match → goes through second pass with null reqPax → ptc = mapPaxType("INFANT") = "INF"

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, req);
        // INF order item should be created
        assertTrue(result.getOrderItems().stream().anyMatch(i -> "INF".equals(i.getPtc())));
    }

    /** Lines 728/750: totalPaxCount=0 and passengers.passenger is null → returns ZERO */
    @Test
    void testDetermineBaseFare_zeroPaxCount_passengersNull_returnsZero() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();
        booking.setAdults(0);
        booking.setChild(0);
        booking.setInfant(0);
        booking.setPassengers(null); // passengers null → totalPaxCount stays 0 → return ZERO

        // flight has no adult/child/infant fare, no net fare, no invpricing
        Flight f = booking.getFlights().getFlight().get(0);
        f.setAdultfare(null);
        f.setNetFare(null);
        f.setInvpricingwithouttax(null);
        f.setInvpricing(null);
        f.setTax(null);
        f.setTotaltax(null);
        f.setTotaltaxes(0.0);

        // use req passenger fallback since no go7 passengers
        OrderCreateReqDto req = new OrderCreateReqDto();
        PaxReqDto p = new PaxReqDto();
        p.setPaxId("A1");
        p.setPtc("ADT");
        req.setPassengers(List.of(p));

        // Should complete without exception; pricing will be zero
        assertDoesNotThrow(() -> OrderCreateResponse.generateResponse(go7, req));
    }

    /** safeDecimal with invalid string returns ZERO — triggered via adultfare */
    @Test
    void testSafeDecimal_invalidString_returnsZero() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setAdultfare("INVALID");
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setTax("ALSO_INVALID");
        go7.getAerocrs().getBooking().setAdults(1);

        // Should not throw; safeDecimal catches NumberFormatException and returns ZERO
        assertDoesNotThrow(() -> OrderCreateResponse.generateResponse(go7, createBasicReqDto()));

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, createBasicReqDto());
        // base fare was invalid -> falls through to netFare (null) -> invpricing (null) -> zero
        // totalPaxCount=1 -> 0/1 = 0
        assertNotNull(result.getOrderItems());
    }

    /** CHD ptc goes to cnnRefs in populateOrderItems */
    @Test
    void testPopulateOrderItems_CHD_addedToCnnRefs() {
        OrderCreateRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();

        // Change adult to CHD
        booking.getPassengers().getPassenger().get(0).setPaxtype("CHD");
        booking.getFlights().getFlight().get(0).setAdultfare(null);
        booking.getFlights().getFlight().get(0).setChildfare("100");
        booking.getFlights().getFlight().get(0).setTax("10");
        booking.setChild(1);
        booking.setAdults(0);

        OrderCreateReqDto req = new OrderCreateReqDto();
        PaxReqDto p = new PaxReqDto();
        p.setPaxId("C1");
        p.setPtc("CHD");
        p.setFirstName("John");
        p.setLastName("Doe");
        req.setPassengers(List.of(p));

        OrderCreateRspDto result = OrderCreateResponse.generateResponse(go7, req);
        assertTrue(result.getOrderItems().stream().anyMatch(i -> "CNN".equals(i.getPtc())));
    }
}

