package com.airlines.go7api.response;

import com.airlines.go7api.requestdto.ChangePaymentReqDto;
import com.airlines.go7api.responsedto.ChangePaymentRspDto;
import com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto;
import com.airlines.go7api.responsego7.common.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ChangePaymentResponseTest {

    // -------------------------------------------------------------------------
    // Helper: build a minimal valid Go7 response with one adult passenger
    // -------------------------------------------------------------------------
    private ChangePaymentRspGo7Dto createBaseGo7Response() {
        Booking booking = new Booking();
        booking.setBookingid(12345L);
        booking.setPnrref("TESTPNR");
        booking.setCurrency("USD");
        booking.setTotalprice("350.00");

        BalanceInformation bi = new BalanceInformation();
        bi.setPnrTotal(BigDecimal.valueOf(350.00));
        booking.setBalanceInformation(bi);

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
        flight.setInvpricing("300");
        flight.setInvpricingwithouttax("250");
        flight.setTotaltaxes(50.0);

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

        ChangePaymentRspGo7Dto go7Dto = new ChangePaymentRspGo7Dto();
        go7Dto.setAerocrs(aerocrs);
        return go7Dto;
    }

    private ChangePaymentReqDto createBasicReqDto() {
        ChangePaymentReqDto req = new ChangePaymentReqDto();
        req.setOrderId("O12345");
        req.setPaymentType("CC");
        return req;
    }

    // =========================================================================
    // 1. NULL / GUARD & PRIVATE CONSTRUCTOR TESTS
    // =========================================================================

    @Test
    void testConstructorIsPrivate() throws Exception {
        java.lang.reflect.Constructor<ChangePaymentResponse> constructor =
                ChangePaymentResponse.class.getDeclaredConstructor();
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
    void testGenerateResponse_NullOrEmptyGo7Response() {
        assertNotNull(ChangePaymentResponse.generateResponse(null, new ChangePaymentReqDto(), Map.of(), Map.of()));

        ChangePaymentRspGo7Dto go7 = new ChangePaymentRspGo7Dto();
        assertNotNull(ChangePaymentResponse.generateResponse(go7, new ChangePaymentReqDto(), Map.of(), Map.of()));

        go7.setAerocrs(new Aerocrs());
        assertNotNull(ChangePaymentResponse.generateResponse(go7, new ChangePaymentReqDto(), Map.of(), Map.of()));
    }

    // =========================================================================
    // 2. HAPPY PATH from JSON resource
    // =========================================================================

    @Test
    void testGenerateResponse_HappyPath() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);

        InputStream is = getClass().getClassLoader().getResourceAsStream("change_payment_response.json");
        ChangePaymentRspGo7Dto go7Response = mapper.readValue(is, ChangePaymentRspGo7Dto.class);

        ChangePaymentReqDto reqDto = new ChangePaymentReqDto();
        reqDto.setOrderId("O123");

        Map<String, String> pSeats = new HashMap<>();
        pSeats.put("pax1", "1A");

        Map<String, String> pServices = new HashMap<>();

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(
                go7Response, reqDto, pSeats, pServices
        );

        assertNotNull(response);
    }

    // =========================================================================
    // 3. TOP-LEVEL FIELDS
    // =========================================================================

    @Test
    void testTopLevelFields_withPopulatedValues() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        ChangePaymentReqDto req = createBasicReqDto();

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, req, Map.of(), Map.of());

        assertNotNull(response.getResponseId());
        assertTrue(response.getResponseId().startsWith("P"));
        assertEquals("O12345", response.getOrderId());
        assertEquals("TESTPNR", response.getPnr());
        assertEquals("G7", response.getApiOwner());
        assertEquals("USD", response.getCurrency());
        assertEquals("OPENED", response.getStatusCode());
    }

    @Test
    void testTopLevelFields_nullOrderId_usesBookingId() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        ChangePaymentReqDto req = new ChangePaymentReqDto(); // null orderId

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, req, Map.of(), Map.of());
        assertEquals("12345", response.getOrderId());
    }

    @Test
    void testTopLevelFields_emptyOrderId_usesBookingId() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        ChangePaymentReqDto req = new ChangePaymentReqDto();
        req.setOrderId(""); // empty orderId

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, req, Map.of(), Map.of());
        assertEquals("12345", response.getOrderId());
    }

    @Test
    void testTopLevelFields_nullCurrency_defaultsUSD() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().setCurrency(null);

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        assertEquals("USD", response.getCurrency());
    }

    @Test
    void testTopLevelFields_rejectedWhenNotSuccess() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().setSuccess(false);

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        assertEquals("REJECTED", response.getStatusCode());
    }

    // =========================================================================
    // 4. FLIGHT LIST FALLBACKS
    // =========================================================================

    @Test
    void testFlightList_fallbackToItems() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();
        booking.setFlights(null); // flights null

        Flight itemFlight = new Flight();
        itemFlight.setFromcode("LAX");
        itemFlight.setTocode("SFO");
        itemFlight.setFlightdate("2026-06-02");
        itemFlight.setAirline("Go7 Airlines");

        Items items = new Items();
        items.setFlight(List.of(itemFlight));
        booking.setItems(items);

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        assertNotNull(response.getOds());
        assertEquals("LAX", response.getOds().get(0).getOrigin());
    }

    @Test
    void testFlightList_bothNull_returnsEmptyList() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().setFlights(null);
        go7.getAerocrs().getBooking().setItems(null);

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        assertTrue(response.getOds() == null || response.getOds().isEmpty());
    }

    // =========================================================================
    // 5. FLIGHT METADATA DEFAULTS
    // =========================================================================

    @Test
    void testFlightMetadata_emptyFlightList_defaults() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().setFlights(null);
        go7.getAerocrs().getBooking().setItems(null);

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        // Since flights/items are empty, TicketDocInfo should be empty
        assertTrue(response.getTicketDocInfoList() == null || response.getTicketDocInfoList().isEmpty());
    }

    @Test
    void testFlightMetadata_nullAirlineOrFromcode_defaults() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Flight f = go7.getAerocrs().getBooking().getFlights().getFlight().get(0);
        f.setAirline(null);
        f.setFromcode(null);

        Passenger p = go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0);
        Passenger.ETicketFlight et = new Passenger.ETicketFlight();
        et.setEticketnumber("TKT123");
        Passenger.ETickets tickets = new Passenger.ETickets();
        tickets.setFlight(List.of(et));
        p.setETickets(tickets);

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        // Issuing Airline and Issuing Place should use fallback defaults in ticket docs
        assertFalse(response.getTicketDocInfoList().isEmpty());
        assertEquals("G7", response.getTicketDocInfoList().get(0).getIssuingAirlineName());
        assertEquals("US", response.getTicketDocInfoList().get(0).getIssuingPlace());
    }

    // =========================================================================
    // 6. ODs AND PRICE CLASSES (NESTING / PARSING)
    // =========================================================================

    @Test
    void testOds_overnightFlight_incrementsArrivalDate() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Flight f = go7.getAerocrs().getBooking().getFlights().getFlight().get(0);
        f.setDepart("23:00");
        f.setArrive("02:00");
        f.setFlightdate("2026-06-01");

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        assertEquals("01Jun2026", response.getOds().get(0).getDepartureDate());
        assertEquals("02Jun2026", response.getOds().get(0).getArrivalDate());
    }

    @Test
    void testOds_invalidTimes_ignoresDateAdjustment() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Flight f = go7.getAerocrs().getBooking().getFlights().getFlight().get(0);
        f.setDepart("BAD_TIME");
        f.setArrive("OTHER_BAD");

        assertDoesNotThrow(() -> ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of()));
    }

    @ParameterizedTest(name = "[{index}] code={0} => expectedCabin={1}")
    @CsvSource({
        "C/Business, BUSINESS",
        "J/Business, BUSINESS",
        "D/Business, BUSINESS",
        "Z/Business, BUSINESS",
        "I/Business, BUSINESS",
        "F/First,    FIRST",
        "A/First,    FIRST",
        "P/First,    FIRST",
        "Y/Economy,  ECONOMY",
        "X,          ECONOMY",
        "'',         ECONOMY"
    })
    void testDetermineCabinCode(String flightClass, String expectedCabin) {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setFlightClass(flightClass);

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        assertEquals(expectedCabin, response.getOds().get(0).getCabinType());
    }

    @Test
    void testPriceClassDescriptions_mappedFromServices() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Flight f = go7.getAerocrs().getBooking().getFlights().getFlight().get(0);
        Map<String, Boolean> services = new HashMap<>();
        services.put("SeatSelection", true);
        services.put("HandBaggage", true);
        services.put("CheckedInBaggage", true);
        services.put("RefundableTicket", true);
        services.put("FoodOnBoard", true);
        services.put("SomeOtherService", true);
        services.put("FalseService", false);
        f.setServices(services);

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        var priceClass = response.getPriceClassList().get(0);
        assertNotNull(priceClass.getDescriptions());
        assertTrue(priceClass.getDescriptions().stream().anyMatch(d -> d.getText().contains("Seat Selection")));
        assertTrue(priceClass.getDescriptions().stream().anyMatch(d -> d.getText().contains("Cabin Baggage")));
        assertTrue(priceClass.getDescriptions().stream().anyMatch(d -> d.getText().contains("Check-in Baggage")));
        assertTrue(priceClass.getDescriptions().stream().anyMatch(d -> d.getText().contains("Cancellation Fee")));
        assertTrue(priceClass.getDescriptions().stream().anyMatch(d -> d.getText().contains("Food on Board")));
        assertTrue(priceClass.getDescriptions().stream().anyMatch(d -> d.getText().contains("SomeOtherService - Available")));
    }

    // =========================================================================
    // 7. PASSENGERS - TITLE / GENDER / EMAIL / INFANT LINKING
    // =========================================================================

    @Test
    void testPassengers_nullTitleOrNames_doesNotThrow() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Passenger p = go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0);
        p.setPaxtitle(null);
        p.setFirstname(null);
        p.setLastname(null);
        p.setGender(null);

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        assertEquals("MR", response.getPaxDetailList().get(0).getTitle());
        assertEquals("", response.getPaxDetailList().get(0).getGivenName());
        assertEquals("", response.getPaxDetailList().get(0).getSurname());
    }

    @Test
    void testPassengers_emailPopulated() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Passenger p = go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0);
        p.setEmail("john.doe@example.com");

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        assertNotNull(response.getPaxDetailList().get(0).getEmails());
        assertEquals("john.doe@example.com", response.getPaxDetailList().get(0).getEmails().get(0).getEmailAddress());
    }

    static Stream<Arguments> genderCases() {
        return Stream.of(
            Arguments.of("F",     "MR",   "Female"), // starts with F takes priority
            Arguments.of("M",     "MRS",  "Female"), // title contains MRS -> Female takes priority over startsWith M
            Arguments.of(null,    "MRS",  "Female"), // title contains MRS
            Arguments.of(null,    "MS",   "Female"), // title contains MS
            Arguments.of(null,    "MISS", "Female"), // title contains MISS
            Arguments.of(null,    "MR",   "Male"),   // title contains MR
            Arguments.of(null,    "MSTR", "Female"), // MSTR contains MS -> Female
            Arguments.of("M",     "Dr",   "Male"),   // starts with M logic branch
            Arguments.of("Other", "Dr",   "Male")    // fallback
        );
    }

    @ParameterizedTest
    @MethodSource("genderCases")
    void testGenderDetermination(String rawGender, String title, String expectedGender) {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Passenger p = go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0);
        p.setGender(rawGender);
        p.setPaxtitle(title);

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        assertEquals(expectedGender, response.getPaxDetailList().get(0).getGender());
    }

    @Test
    void testPassenger_infantMappingAndParentLinking() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();

        Passenger adult1 = booking.getPassengers().getPassenger().get(0);
        adult1.setPaxtype("ADULT");

        Passenger infant = new Passenger();
        infant.setFirstname("Baby");
        infant.setLastname("Doe");
        infant.setPaxtype("INFANT");
        infant.setPaxtitle("INFANT");

        List<Passenger> pList = new ArrayList<>(booking.getPassengers().getPassenger());
        pList.add(infant);
        booking.getPassengers().setPassenger(pList);

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        assertEquals(2, response.getPaxDetailList().size());

        var parentResult = response.getPaxDetailList().get(0);
        var infantResult = response.getPaxDetailList().get(1);

        assertEquals("T1", parentResult.getPaxId());
        assertEquals("T1.1", infantResult.getPaxId());
        assertEquals("T1.1", parentResult.getInfantRef());
        assertEquals("INF", infantResult.getPtc());
    }

    @Test
    void testPassenger_infantMappingWithoutAdult_defaultsT1_1() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();

        // only infant passenger
        Passenger infant = new Passenger();
        infant.setFirstname("Baby");
        infant.setLastname("Doe");
        infant.setPaxtype("INFANT");
        infant.setPaxtitle("INF");

        booking.getPassengers().setPassenger(List.of(infant));

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        assertEquals(1, response.getPaxDetailList().size());
        assertEquals("T1.1", response.getPaxDetailList().get(0).getPaxId());
    }

    @Test
    void testPassengers_nullPassengers_returnsEmptyList() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().setPassengers(null);

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        assertTrue(response.getPaxDetailList() == null || response.getPaxDetailList().isEmpty());
    }

    // =========================================================================
    // 8. TICKET DOCUMENTS / COUPONS / BAGGAGE
    // =========================================================================

    @Test
    void testTicketDocs_deduplicatesTickets() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Passenger p = go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0);

        Passenger.ETicketFlight et1 = new Passenger.ETicketFlight();
        et1.setEticketnumber("TKT123");
        Passenger.ETicketFlight et2 = new Passenger.ETicketFlight();
        et2.setEticketnumber("TKT123"); // duplicate
        Passenger.ETicketFlight et3 = new Passenger.ETicketFlight();
        et3.setEticketnumber("TKT456");

        Passenger.ETickets tickets = new Passenger.ETickets();
        tickets.setFlight(List.of(et1, et2, et3));
        p.setETickets(tickets);

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        assertNotNull(response.getTicketDocInfoList());
        assertEquals(2, response.getTicketDocInfoList().size());
    }

    @Test
    void testCouponRbd_slashFormat() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Flight flight = go7.getAerocrs().getBooking().getFlights().getFlight().get(0);
        flight.setFlightClass("Y/Economy");
        flight.setServices(Map.of("CheckedInBaggage", true));

        Passenger p = go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0);
        Passenger.ETicketFlight et = new Passenger.ETicketFlight();
        et.setEticketnumber("TKT123");
        Passenger.ETickets tickets = new Passenger.ETickets();
        tickets.setFlight(List.of(et));
        p.setETickets(tickets);

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        var coupon = response.getTicketDocInfoList().get(0).getTicketDocument().get(0).getCouponInfo().get(0);
        assertEquals("Y", coupon.getRbd());
        assertFalse(coupon.getBaggageAllowances().isEmpty());
    }

    @Test
    void testCouponRbd_nonSlashFormat() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setFlightClass("Business");

        Passenger p = go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0);
        Passenger.ETicketFlight et = new Passenger.ETicketFlight();
        et.setEticketnumber("TKT123");
        Passenger.ETickets tickets = new Passenger.ETickets();
        tickets.setFlight(List.of(et));
        p.setETickets(tickets);

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        var coupon = response.getTicketDocInfoList().get(0).getTicketDocument().get(0).getCouponInfo().get(0);
        assertEquals("B", coupon.getRbd());
    }

    @Test
    void testCouponRbd_emptyFormat() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setFlightClass("");

        Passenger p = go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0);
        Passenger.ETicketFlight et = new Passenger.ETicketFlight();
        et.setEticketnumber("TKT123");
        Passenger.ETickets tickets = new Passenger.ETickets();
        tickets.setFlight(List.of(et));
        p.setETickets(tickets);

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        var coupon = response.getTicketDocInfoList().get(0).getTicketDocument().get(0).getCouponInfo().get(0);
        assertEquals("", coupon.getRbd());
    }

    @Test
    void testBaggageAllowance_ADT_vs_INF() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();
        Flight f = booking.getFlights().getFlight().get(0);
        f.setServices(Map.of("CheckedInBaggage", true));

        Passenger adult = booking.getPassengers().getPassenger().get(0);
        Passenger infant = new Passenger();
        infant.setFirstname("Baby");
        infant.setLastname("Doe");
        infant.setPaxtype("INFANT");

        booking.getPassengers().setPassenger(List.of(adult, infant));

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        var orderItem = response.getOrderItems().get(0);
        assertNotNull(orderItem.getBaggageAllowances());
        assertEquals(2, orderItem.getBaggageAllowances().size());

        var bagAdult = orderItem.getBaggageAllowances().stream().filter(b -> "T1".equals(b.getPassengerId())).findFirst().orElse(null);
        var bagInfant = orderItem.getBaggageAllowances().stream().filter(b -> "T1.1".equals(b.getPassengerId())).findFirst().orElse(null);

        assertNotNull(bagAdult);
        assertNotNull(bagInfant);
        assertEquals("30", bagAdult.getWeight().get(0).getValue());
        assertEquals("10", bagInfant.getWeight().get(0).getValue());
    }

    // =========================================================================
    // 9. EMD INFORMATION
    // =========================================================================

    @Test
    void testEmds_populatedForSeatsAndServices() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Passenger p = go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0);
        Passenger.ETicketFlight et = new Passenger.ETicketFlight();
        et.setEticketnumber("TKT123");
        p.setETickets(new Passenger.ETickets(List.of(et)));

        Map<String, String> pSeats = Map.of("T1", "1A");
        Map<String, String> pServices = Map.of("T1", "SRV_WIFI");

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), pSeats, pServices);
        assertNotNull(response.getEmdInfoList());
        assertEquals(2, response.getEmdInfoList().size());

        var emd1 = response.getEmdInfoList().get(0);
        var emd2 = response.getEmdInfoList().get(1);

        assertEquals("TKT123-1", emd1.getTicketDocument().get(0).getTicketDocNbr());
        assertEquals("TKT123-2", emd2.getTicketDocument().get(0).getTicketDocNbr());
        
        var coupon1 = emd1.getTicketDocument().get(0).getCouponInfo().get(0);
        var coupon2 = emd2.getTicketDocument().get(0).getCouponInfo().get(0);

        assertEquals("1A", coupon1.getServiceRefs().get(0));
        assertEquals("WIFI", coupon2.getServiceRefs().get(0)); // SRV_ WIFI has prefix removed
    }

    @Test
    void testEmds_noTicketDocs_usesEMDFallback() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Passenger p = go7.getAerocrs().getBooking().getPassengers().getPassenger().get(0);
        p.setETickets(null); // No ticket docs

        Map<String, String> pSeats = Map.of("T1", "1A");

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), pSeats, Map.of());
        assertNotNull(response.getEmdInfoList());
        assertEquals("EMD-1", response.getEmdInfoList().get(0).getTicketDocument().get(0).getTicketDocNbr());
    }

    // =========================================================================
    // 10. ORDER ITEMS - PRICING & TAXES
    // =========================================================================

    @Test
    void testOrderItems_pricingFallbacks() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Flight f = go7.getAerocrs().getBooking().getFlights().getFlight().get(0);

        // Case 1: invpricing is set
        f.setInvpricing("400");
        f.setInvpricingwithouttax("350");
        ChangePaymentRspDto res1 = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        assertEquals(0, res1.getOrderItems().get(0).getTotalPrice().compareTo(new BigDecimal("400.00")));

        // Case 2: invpricing is null, fallback to sumFlightPrices (invpricingwithouttax + totaltaxes)
        f.setInvpricing(null);
        f.setInvpricingwithouttax("350");
        f.setTotaltaxes(50.0);
        ChangePaymentRspDto res2 = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        assertEquals(0, res2.getOrderItems().get(0).getTotalPrice().compareTo(new BigDecimal("400.00")));

        // Case 3: invpricing and invpricingwithouttax both invalid strings
        f.setInvpricing("INVALID");
        f.setInvpricingwithouttax("ALSO_INVALID");
        ChangePaymentRspDto res3 = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        // sum = 0 + 50 = 50. InvPricingBasis = 0. So returns sum = 50
        assertEquals(0, res3.getOrderItems().get(0).getTotalPrice().compareTo(new BigDecimal("50.00")));
    }

    @Test
    void testOrderItems_granularTaxes() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Flight f = go7.getAerocrs().getBooking().getFlights().getFlight().get(0);
        f.setTotaltaxes(100.0);

        Taxes t = new Taxes();
        t.setGroundHandling(10.0);
        t.setSecurity(20.0);
        t.setFuel(30.0);
        t.setTax4(40.0);
        f.setTaxes(t);

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        var item = response.getOrderItems().get(0);
        assertNotNull(item.getTaxes());
        assertEquals(4, item.getTaxes().size());

        assertTrue(item.getTaxes().stream().anyMatch(tx -> "GH".equals(tx.getCode()) && tx.getAmount().compareTo(BigDecimal.valueOf(10.0)) == 0));
        assertTrue(item.getTaxes().stream().anyMatch(tx -> "I2".equals(tx.getCode()) && tx.getAmount().compareTo(BigDecimal.valueOf(20.0)) == 0));
        assertTrue(item.getTaxes().stream().anyMatch(tx -> "YQ".equals(tx.getCode()) && tx.getAmount().compareTo(BigDecimal.valueOf(30.0)) == 0));
        assertTrue(item.getTaxes().stream().anyMatch(tx -> "OT".equals(tx.getCode()) && tx.getAmount().compareTo(BigDecimal.valueOf(40.0)) == 0));
    }

    @Test
    void testOrderItems_granularTaxesUnderThreshold_skipped() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Flight f = go7.getAerocrs().getBooking().getFlights().getFlight().get(0);

        Taxes t = new Taxes();
        t.setGroundHandling(0.0001); // Under threshold of 0.001
        f.setTaxes(t);

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        var item = response.getOrderItems().get(0);
        assertTrue(item.getTaxes() == null || item.getTaxes().isEmpty());
    }

    // =========================================================================
    // 11. SECONDARY ORDER ITEMS & PRICING RESOLUTIONS
    // =========================================================================

    @Test
    void testSecondaryOrderItems_resolveSeatPrice() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Flight f = go7.getAerocrs().getBooking().getFlights().getFlight().get(0);

        Seat s = new Seat();
        s.setSeatNumber("12F");
        s.setFare(BigDecimal.valueOf(45.00));
        f.setSeat(List.of(s));

        Map<String, String> pSeats = Map.of("T1", "12F"); // Price is not embedded in the value

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), pSeats, Map.of());
        assertEquals(2, response.getOrderItems().size());

        var seatItem = response.getOrderItems().get(1);

        assertEquals(0, seatItem.getTotalPrice().compareTo(new BigDecimal("45.00")));
    }

    @Test
    void testSecondaryOrderItems_resolveUnpaidCountPrice() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();

        // pnrTotal = 500, airItemPrice = 300 (difference of 200)
        booking.setTotalprice("500.00");
        booking.getBalanceInformation().setPnrTotal(BigDecimal.valueOf(500.00));

        // Let's add 2 unpaid secondary items (seat and service)
        Map<String, String> pSeats = Map.of("T1", "1A"); // No price info
        Map<String, String> pServices = Map.of("T1", "SRV_BAG"); // No price info

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), pSeats, pServices);
        // There should be 3 order items total (1 air, 2 secondary)
        assertEquals(3, response.getOrderItems().size());

        var seatItem = response.getOrderItems().stream().filter(i -> i.getOrderItemId().contains("_SRV")).findFirst().orElse(null);

        // Difference is 200.00. Unpaid count is 2. So 200.00 / 2 = 100.00 per item
        assertNotNull(seatItem);
        assertEquals(0, seatItem.getTotalPrice().compareTo(new BigDecimal("100.00")));
        assertEquals(0, response.getTotalOrderPrice().compareTo(new BigDecimal("500.00")));
    }

    @Test
    void testSecondaryOrderItems_seatCharacteristicsAndRowColumn() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Map<String, String> pSeats = Map.of("T1", "24B"); // Row 24, Column B

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), pSeats, Map.of());
        assertEquals(2, response.getOrderItems().size());

        var secondaryService = response.getOrderItems().get(1).getServiceList().get(0);
        assertEquals("24", secondaryService.getRow().toString());
        assertEquals("B", secondaryService.getColumn());
        assertNotNull(secondaryService.getSeatCharacteristics());
        assertEquals(4, secondaryService.getSeatCharacteristics().size());
    }

    @Test
    void testSecondaryOrderItems_seatCharacteristicsAndRowInvalid_ignoresRow() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Map<String, String> pSeats = Map.of("T1", "INVALID_ROW"); // row cannot be parsed to BigInteger

        assertDoesNotThrow(() -> ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), pSeats, Map.of()));
        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), pSeats, Map.of());
        assertNull(response.getOrderItems().get(1).getServiceList().get(0).getRow());
    }

    // =========================================================================
    // 12. PAYMENTS MAPPINGS
    // =========================================================================

    @Test
    void testPayments_multipleItems_splitsPayments() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        go7.getAerocrs().getBooking().setTotalprice("400.00");
        go7.getAerocrs().getBooking().getBalanceInformation().setPnrTotal(BigDecimal.valueOf(400.00));

        Map<String, String> pSeats = Map.of("T1", "1A|100.00"); // 100 secondary charge

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), pSeats, Map.of());
        assertNotNull(response.getPayments());
        assertEquals(2, response.getPayments().size()); // 1 for air, 1 for secondary seat

        var payAir = response.getPayments().get(0);
        var paySec = response.getPayments().get(1);

        assertEquals(0, payAir.getAmount().compareTo(new BigDecimal("300.00")));
        assertEquals(0, paySec.getAmount().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void testPayments_fallbackPayments_whenEmpty() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        // Set air item price to 0
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setInvpricing("0");
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setInvpricingwithouttax("0");
        go7.getAerocrs().getBooking().getFlights().getFlight().get(0).setTotaltaxes(0.0);
        go7.getAerocrs().getBooking().setTotalprice("0.00");
        go7.getAerocrs().getBooking().getBalanceInformation().setPnrTotal(BigDecimal.ZERO);

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), Map.of(), Map.of());
        assertNotNull(response.getPayments());
        assertEquals(1, response.getPayments().size());
        assertEquals(0, response.getPayments().get(0).getAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    void testOrderItems_taxesGreaterThanPrice_resetsBaseFare() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();
        Flight f = booking.getFlights().getFlight().get(0);
        f.setInvpricing("30");
        f.setTotaltaxes(50.0);

        // Add a secondary item to force setting base fare scaling block
        Map<String, String> pSeats = Map.of("T1", "1A|10.00");

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), pSeats, Map.of());
        assertNotNull(response);
        // airBaseFare = 30 - 50 = -20 < 0 -> resets to airItemPrice (30)
        var item = response.getOrderItems().get(0);
        assertEquals(0, item.getBaseFare().getAmount().compareTo(new BigDecimal("30.00")));
    }

    @Test
    void testDeterminePnrTotal_invalidTotalPrice_returnsZero() {
        ChangePaymentRspGo7Dto go7 = createBaseGo7Response();
        Booking booking = go7.getAerocrs().getBooking();
        booking.setBalanceInformation(null);
        booking.setTotalprice("NOT_A_NUMBER");

        // Add a secondary item to invoke processSecondaryOrderItems which calls determinePnrTotal
        Map<String, String> pSeats = Map.of("T1", "1A");

        ChangePaymentRspDto response = ChangePaymentResponse.generateResponse(go7, createBasicReqDto(), pSeats, Map.of());
        assertNotNull(response);
    }
}
