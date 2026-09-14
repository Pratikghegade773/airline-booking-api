package com.airlines.go7api.response;

import com.airlines.go7api.responsedto.OrderRetrieveRspDto;
import com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto;
import com.airlines.go7api.responsego7.common.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class OrderRetrieveResponseTest {

    @Test
    void testConstructorIsPrivate() throws Exception {
        java.lang.reflect.Constructor<OrderRetrieveResponse> constructor = OrderRetrieveResponse.class.getDeclaredConstructor();
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
    void testGenerateResponse_HappyPath() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);

        InputStream is = getClass().getClassLoader().getResourceAsStream("order_retrieve_response.json");
        OrderRetrieveRspGo7Dto go7Response = mapper.readValue(is, OrderRetrieveRspGo7Dto.class);

        OrderRetrieveRspDto response = OrderRetrieveResponse.generateResponse(
                go7Response, new HashMap<>(), new HashMap<>(), BigDecimal.valueOf(250.00)
        );

        assertNotNull(response);
    }

    @Test
    void testGenerateResponse_NullOrEmptyGo7Response() {
        // null go7Response
        OrderRetrieveRspDto response = OrderRetrieveResponse.generateResponse(null, new HashMap<>(), new HashMap<>(), null);
        assertNotNull(response);
        assertNull(response.getPnr());

        // null aerocrs
        OrderRetrieveRspGo7Dto go7Response = new OrderRetrieveRspGo7Dto();
        response = OrderRetrieveResponse.generateResponse(go7Response, new HashMap<>(), new HashMap<>(), null);
        assertNotNull(response);

        // null booking
        Aerocrs aerocrs = new Aerocrs();
        go7Response.setAerocrs(aerocrs);
        response = OrderRetrieveResponse.generateResponse(go7Response, new HashMap<>(), new HashMap<>(), null);
        assertNotNull(response);
    }

    @Test
    void testAdditionalUncoveredOdsAndPriceClasses() {
        OrderRetrieveRspGo7Dto go7Response = createBaseBookingResponse();
        Booking booking = go7Response.getAerocrs().getBooking();
        booking.setFlights(null); // flights null, triggers items check
        
        Items items = new Items();
        List<Flight> flightList = new ArrayList<>();
        
        // Flight 1: depart 23:00, arrive 02:00 next day (overnight)
        // has services map for price class descriptions
        Flight f1 = new Flight();
        f1.setNumber("101");
        f1.setFromcode("JFK");
        f1.setTocode("LHR");
        f1.setFlightdate("2026-06-01");
        f1.setDepart("23:00");
        f1.setArrive("02:00");
        f1.setAirlinedesignator("XX");
        f1.setAirline("Test Airline");
        f1.setFlightClass("Y/Economy");
        Map<String, Boolean> services = new HashMap<>();
        services.put("WiFi", true);
        f1.setServices(services);
        flightList.add(f1);

        // Flight 2: rawClass is "/", or null, or "FIRST/", or "Business Class"
        Flight f2 = new Flight();
        f2.setNumber("102");
        f2.setFromcode("LHR");
        f2.setTocode("CDG");
        f2.setFlightdate("2026-06-02");
        f2.setDepart("invalid_time"); // triggers parsing error in overnight check
        f2.setArrive("12:00");
        f2.setAirlinedesignator(null); // will default validating carrier
        f2.setFlightClass("/");
        flightList.add(f2);

        Flight f3 = new Flight();
        f3.setFlightdate("2026-06-02");
        f3.setFlightClass("FIRST/");
        flightList.add(f3);

        Flight f4 = new Flight();
        f4.setFlightdate("2026-06-02");
        f4.setFlightClass("F/First Class");
        flightList.add(f4);

        Flight f5 = new Flight();
        f5.setFlightdate("2026-06-02");
        f5.setFlightClass("C/Business Class");
        flightList.add(f5);

        Flight f6 = new Flight();
        f6.setFlightdate("2026-06-02");
        f6.setFlightClass("A/Suite");
        flightList.add(f6);

        Flight f7 = new Flight();
        f7.setFlightdate("2026-06-02");
        f7.setFlightClass("Business Cabin");
        flightList.add(f7);

        Flight f8 = new Flight();
        f8.setFlightdate("2026-06-02");
        f8.setFlightClass("First Cabin");
        flightList.add(f8);

        items.setFlight(flightList);
        booking.setItems(items);

        OrderRetrieveRspDto response = OrderRetrieveResponse.generateResponse(
                go7Response, new HashMap<>(), new HashMap<>(), null
        );

        assertNotNull(response);
        assertEquals("XX", response.getValidatingCarrier());
        assertEquals("02Jun2026", response.getOds().get(0).getArrivalDate()); // overnight f1 adjusted by 1 day
        assertEquals("02Jun2026", response.getOds().get(1).getArrivalDate()); // f2 arrival date remains unchanged
        assertFalse(response.getPriceClassList().get(0).getDescriptions().isEmpty());
    }

    @Test
    void testPassengerDetailsAdditionalBranches() {
        OrderRetrieveRspGo7Dto go7Response = createBaseBookingResponse();
        Booking booking = go7Response.getAerocrs().getBooking();

        Passengers passengers = new Passengers();
        List<Passenger> passengerList = new ArrayList<>();

        // P1: ADT, Male by gender starts with M, has contact & email
        Passenger p1 = new Passenger();
        p1.setPaxtype("ADULT");
        p1.setGender("Male");
        p1.setPaxtitle("MR");
        p1.setFirstname("John");
        p1.setLastname("Doe");
        p1.setContact("1234567890");
        p1.setEmail("john.doe@example.com");
        passengerList.add(p1);

        // P2: ADT, Female by gender starts with F
        Passenger p2 = new Passenger();
        p2.setPaxtype("ADULT");
        p2.setGender("Female");
        p2.setPaxtitle("MRS");
        p2.setFirstname("Jane");
        p2.setLastname("Doe");
        passengerList.add(p2);

        // P3: CHILD, Male by title
        Passenger p3 = new Passenger();
        p3.setPaxtype("CHILD");
        p3.setGender("");
        p3.setPaxtitle("MSTR");
        p3.setFirstname("Billy");
        p3.setLastname("Doe");
        passengerList.add(p3);

        // P4: INFANT, Female by title
        Passenger p4 = new Passenger();
        p4.setPaxtype("INFANT");
        p4.setGender("");
        p4.setPaxtitle("MISS");
        p4.setFirstname("Lily");
        p4.setLastname("Doe");
        passengerList.add(p4);

        // P5: INFANT (with title INF, linked to parent ADT Jane -> T2)
        Passenger p5 = new Passenger();
        p5.setPaxtype("INFANT");
        p5.setGender("");
        p5.setPaxtitle("INF");
        p5.setFirstname("Baby");
        p5.setLastname("Doe");
        passengerList.add(p5);

        // P6: ADT, Gender null/empty (fallback MR to Male)
        Passenger p6 = new Passenger();
        p6.setPaxtype("ADULT");
        p6.setGender(null);
        p6.setPaxtitle("MR.");
        p6.setFirstname("Bob");
        p6.setLastname("Smith");
        passengerList.add(p6);

        // P7: ADT, Gender null, title MS (Female)
        Passenger p7 = new Passenger();
        p7.setPaxtype("ADULT");
        p7.setGender("");
        p7.setPaxtitle("MS");
        p7.setFirstname("Alice");
        p7.setLastname("Smith");
        passengerList.add(p7);

        passengers.setPassenger(passengerList);
        booking.setPassengers(passengers);

        OrderRetrieveRspDto response = OrderRetrieveResponse.generateResponse(
                go7Response, new HashMap<>(), new HashMap<>(), null
        );

        assertNotNull(response);
        assertEquals(7, response.getPaxDetailList().size());
        assertEquals("Male", response.getPaxDetailList().get(0).getGender());
        assertEquals("Female", response.getPaxDetailList().get(1).getGender());
        assertEquals("CHILD", response.getPaxDetailList().get(2).getTitle());
        assertEquals("INFANT", response.getPaxDetailList().get(3).getTitle());
        assertEquals("T2.1", response.getPaxDetailList().get(4).getPaxId()); // linked to P2 (T2)
    }

    @Test
    void testTicketDocsAndEmdEdgeCases() {
        OrderRetrieveRspGo7Dto go7Response = createBaseBookingResponse();
        Booking booking = go7Response.getAerocrs().getBooking();

        // 1. Add Flight with baggage allowance check, equipment type IATA code, and airline name
        List<Flight> flightList = new ArrayList<>();
        Flight f = new Flight();
        f.setNumber("101");
        f.setFromcode("JFK");
        f.setTocode("LHR");
        f.setFlightdate("2026-06-01");
        f.setDepart("10:00");
        f.setArrive("12:00");
        f.setFlightClass("Y");
        f.setAirlinedesignator("G7");
        f.setAirline("Test Airline");
        f.setAircraftTypeIataCode("77W");
        Map<String, Boolean> services = new HashMap<>();
        services.put("CheckedInBaggage", true);
        f.setServices(services);
        flightList.add(f);
        booking.getFlights().setFlight(flightList);

        // 2. Add Passenger with ETickets and Checkin list
        Passengers passengers = new Passengers();
        List<Passenger> passengerList = new ArrayList<>();
        Passenger p = new Passenger();
        p.setPaxtype("ADULT");
        p.setFirstname("John");
        p.setLastname("Doe");
        p.setPaxtitle("MR");

        // ETickets
        Passenger.ETickets eTickets = new Passenger.ETickets();
        List<Passenger.ETicketFlight> etfList = new ArrayList<>();
        Passenger.ETicketFlight etf = new Passenger.ETicketFlight();
        etf.setEticketnumber("1234567890");
        etfList.add(etf);
        eTickets.setFlight(etfList);
        p.setETickets(eTickets);

        // Checkin
        List<Passenger.Checkin> checkinList = new ArrayList<>();
        Passenger.Checkin chk = new Passenger.Checkin();
        chk.setSeat("12A");
        chk.setFlight("101");
        checkinList.add(chk);
        p.setCheckin(checkinList);

        passengerList.add(p);
        passengers.setPassenger(passengerList);
        booking.setPassengers(passengers);

        // Map inputs for database seat and services EMDs
        Map<String, String> passengerSeats = new HashMap<>();
        passengerSeats.put("T1", "12A|50.00");

        Map<String, String> passengerServices = new HashMap<>();
        passengerServices.put("T1", "SRV_Meal|100.00");

        OrderRetrieveRspDto response = OrderRetrieveResponse.generateResponse(
                go7Response, passengerSeats, passengerServices, null
        );

        assertNotNull(response);
        assertNotNull(response.getTicketDocInfoList());
        assertEquals("1234567890", response.getTicketDocInfoList().get(0).getTicketDocument().get(0).getTicketDocNbr());
        assertEquals("FBA2", response.getTicketDocInfoList().get(0).getTicketDocument().get(0).getCouponInfo().get(0).getBaggageAllowances().get(0).getBaggageAllowanceId());
        
        // EMD Info
        assertNotNull(response.getEmdInfoList());
        assertEquals("1234567890-1", response.getEmdInfoList().get(0).getTicketDocument().get(0).getTicketDocNbr());
    }

    @Test
    void testEmdFallbackSeatAndCheckinPaths() {
        OrderRetrieveRspGo7Dto go7Response = createBaseBookingResponse();
        Booking booking = go7Response.getAerocrs().getBooking();

        // 1. Flight with seat assignments
        List<Flight> flightList = new ArrayList<>();
        Flight f = new Flight();
        f.setNumber("101");
        f.setFromcode("JFK");
        f.setTocode("LHR");
        f.setFlightdate("2026-06-01");
        f.setDepart("10:00");
        f.setArrive("12:00");
        f.setFlightClass("Y");
        f.setAirlinedesignator("G7");
        f.setAirline("Test Airline");
        
        List<Seat> seatsList = new ArrayList<>();
        Seat s = new Seat();
        s.setSeatNumber("12A");
        seatsList.add(s);
        f.setSeat(seatsList);
        
        flightList.add(f);
        booking.getFlights().setFlight(flightList);

        // 2. Passenger
        Passengers passengers = new Passengers();
        List<Passenger> passengerList = new ArrayList<>();
        Passenger p = new Passenger();
        p.setPaxtype("ADULT");
        p.setFirstname("John");
        p.setLastname("Doe");
        p.setPaxtitle("MR");
        
        // Empty ETickets to fall back to "EMD" base ticket number
        p.setETickets(null);
        
        passengerList.add(p);
        passengers.setPassenger(passengerList);
        booking.setPassengers(passengers);

        // No seats or services maps (triggers fallback seat EMD mapping)
        OrderRetrieveRspDto response = OrderRetrieveResponse.generateResponse(
                go7Response, null, null, null
        );

        assertNotNull(response);
        assertEquals("EMD-1", response.getEmdInfoList().get(0).getTicketDocument().get(0).getTicketDocNbr());
    }

    @Test
    void testOrderItemsFaresAndTaxesScaling() {
        OrderRetrieveRspGo7Dto go7Response = createBaseBookingResponse();
        Booking booking = go7Response.getAerocrs().getBooking();
        booking.setAdults(1);
        booking.setChild(1);
        booking.setInfant(1);

        // Flight with fares & taxes
        List<Flight> flightList = new ArrayList<>();
        Flight f = new Flight();
        f.setNumber("101");
        f.setFromcode("JFK");
        f.setTocode("LHR");
        f.setFlightdate("2026-06-01");
        f.setDepart("10:00");
        f.setArrive("12:00");
        f.setFlightClass("Y/Economy");
        f.setAirlinedesignator("G7");
        f.setTotaltaxes(15.00);
        f.setAdultfare("100.00");
        f.setChildfare("75.00");
        f.setInfantfare("25.00");
        
        Taxes t = new Taxes();
        t.setSecurity(2.0);
        t.setFuel(3.0);
        t.setGroundHandling(1.0);
        t.setTax1(2.0);
        t.setTax4(2.0);
        f.setTaxes(t);
        
        flightList.add(f);
        booking.getFlights().setFlight(flightList);

        // Passengers
        Passengers passengers = new Passengers();
        List<Passenger> passengerList = new ArrayList<>();
        Passenger p1 = new Passenger();
        p1.setPaxtype("ADULT");
        p1.setFirstname("John");
        p1.setLastname("Doe");
        p1.setPaxtitle("MR");
        passengerList.add(p1);

        Passenger p2 = new Passenger();
        p2.setPaxtype("CHILD");
        p2.setFirstname("Billy");
        p2.setLastname("Doe");
        p2.setPaxtitle("MSTR");
        passengerList.add(p2);

        Passenger p3 = new Passenger();
        p3.setPaxtype("INFANT");
        p3.setFirstname("Baby");
        p3.setLastname("Doe");
        p3.setPaxtitle("INF");
        passengerList.add(p3);

        passengers.setPassenger(passengerList);
        booking.setPassengers(passengers);

        // pnrttl with slash /
        booking.setPnrttl("2026/06/10 12:00:00");

        OrderRetrieveRspDto response = OrderRetrieveResponse.generateResponse(
                go7Response, null, null, null
        );

        assertNotNull(response);
        assertEquals(3, response.getOrderItems().size()); // ADT, CHD, INF
        // Verify PNR TTL formatting
        assertEquals("10Jun2026 12:00:00", response.getPaymentTimeLimit());
    }

    @Test
    void testOrderItemsFareFallbackPaths() {
        OrderRetrieveRspGo7Dto go7Response = createBaseBookingResponse();
        Booking booking = go7Response.getAerocrs().getBooking();
        booking.setAdults(1);
        booking.setTotalprice("200.00");

        // BalanceInformation pnrTotal
        BalanceInformation balance = new BalanceInformation();
        balance.setPnrTotal(BigDecimal.valueOf(250.00));
        balance.setPnrOutstandingPayment(0.00);
        booking.setBalanceInformation(balance);

        // Flight with zero selective fare, but netFare/invpricing fallback
        List<Flight> flightList = new ArrayList<>();
        Flight f = new Flight();
        f.setNumber("101");
        f.setFlightdate("2026-06-01");
        f.setTotaltaxes(10.00);
        // zero adultfare triggers netFare or invpricingwithouttax fallback
        f.setAdultfare("0");
        f.setInvpricingwithouttax("120.00");
        flightList.add(f);
        booking.getFlights().setFlight(flightList);

        Passengers passengers = new Passengers();
        List<Passenger> passengerList = new ArrayList<>();
        Passenger p = new Passenger();
        p.setPaxtype("ADULT");
        p.setFirstname("John");
        p.setLastname("Doe");
        passengerList.add(p);
        passengers.setPassenger(passengerList);
        booking.setPassengers(passengers);

        booking.setPnrttl("2026-06-10 12:00:00"); // dash format

        OrderRetrieveRspDto response = OrderRetrieveResponse.generateResponse(
                go7Response, null, null, null
        );

        assertNotNull(response);
        assertEquals(new BigDecimal("120.00"), response.getOrderItems().get(0).getBaseFare().getAmount());
    }

    @Test
    void testRetrieveBaseFarePnrTotalFallback() {
        OrderRetrieveRspGo7Dto go7Response = createBaseBookingResponse();
        Booking booking = go7Response.getAerocrs().getBooking();
        booking.setAdults(1);
        booking.setTotalprice("150.00");

        List<Flight> flightList = new ArrayList<>();
        Flight f = new Flight();
        f.setNumber("101");
        f.setFlightdate("2026-06-01");
        f.setTotaltaxes(10.00);
        f.setAdultfare("0");
        // No invpricing either -> triggers PNR Total fallback in applyRetrieveBaseFareFallback
        flightList.add(f);
        booking.getFlights().setFlight(flightList);

        Passengers passengers = new Passengers();
        List<Passenger> passengerList = new ArrayList<>();
        Passenger p = new Passenger();
        p.setPaxtype("ADULT");
        p.setFirstname("John");
        p.setLastname("Doe");
        passengerList.add(p);
        passengers.setPassenger(passengerList);
        booking.setPassengers(passengers);

        OrderRetrieveRspDto response = OrderRetrieveResponse.generateResponse(
                go7Response, null, null, null
        );

        assertNotNull(response);
        // base fare = total price (150.00) - tax (10.00) = 140.00
        assertEquals(new BigDecimal("140.00"), response.getOrderItems().get(0).getBaseFare().getAmount());
    }

    @Test
    void testParsingExceptionsAndTaxesEdgeCases() {
        OrderRetrieveRspGo7Dto go7Response = createBaseBookingResponse();
        Booking booking = go7Response.getAerocrs().getBooking();
        booking.setAdults(1);
        booking.setTotalprice("invalid_price"); // triggers format exception in applyRetrieveBaseFareFallback
        booking.setPnrttl("invalid_date");

        List<Flight> flightList = new ArrayList<>();
        Flight f = new Flight();
        f.setNumber("101");
        f.setFlightdate("2026-06-01");
        f.setTotaltaxes(10.00);
        f.setAdultfare("invalid_fare"); // triggers format exception in parseFareSafely
        flightList.add(f);
        booking.getFlights().setFlight(flightList);

        Passengers passengers = new Passengers();
        List<Passenger> passengerList = new ArrayList<>();
        Passenger p = new Passenger();
        p.setPaxtype("ADULT");
        p.setFirstname("John");
        p.setLastname("Doe");
        passengerList.add(p);
        passengers.setPassenger(passengerList);
        booking.setPassengers(passengers);

        // Pass passengerSeats so combinedSrvMap is not empty, and dbTotalOrderPrice is null
        // This triggers bookingTotal = new BigDecimal(booking.getTotalprice()) catch block
        Map<String, String> passengerSeats = new HashMap<>();
        passengerSeats.put("T1", "12A|50.00");

        OrderRetrieveRspDto response = OrderRetrieveResponse.generateResponse(
                go7Response, passengerSeats, null, null
        );

        assertNotNull(response);
        assertEquals(new BigDecimal("0.00"), response.getOrderItems().get(0).getBaseFare().getAmount());
    }

    @Test
    void testEmdCheckinFallbackAndPriceChecks() {
        OrderRetrieveRspGo7Dto go7Response = createBaseBookingResponse();
        Booking booking = go7Response.getAerocrs().getBooking();
        booking.setAdults(1);
        booking.setTotalprice("400.00");

        // BalanceInformation has outstanding payment for isPaid check (line 1029)
        BalanceInformation balance = new BalanceInformation();
        balance.setPnrOutstandingPayment(50.00); // > 0, so isPaid = false
        booking.setBalanceInformation(balance);

        // 1. Flight with NO seat assignments (so seat fallback is skipped)
        List<Flight> flightList = new ArrayList<>();
        Flight f = new Flight();
        f.setNumber("101");
        f.setFromcode("JFK");
        f.setTocode("LHR");
        f.setFlightdate("2026-06-01");
        f.setDepart("10:00");
        f.setArrive("12:00");
        f.setFlightClass("Y/Economy");
        f.setAirlinedesignator("G7");
        f.setAirline(null); // defaults to API Airways
        f.setTotaltaxes(10.00);
        f.setAdultfare("100.00");
        flightList.add(f);
        booking.getFlights().setFlight(flightList);

        // 2. Passenger with checkin seat (triggers check-in fallback EMD)
        // Infant as first passenger to hit line 342 (determinePaxId infant first)
        Passengers passengers = new Passengers();
        List<Passenger> passengerList = new ArrayList<>();
        
        Passenger pInf = new Passenger();
        pInf.setPaxtype("INFANT");
        pInf.setFirstname("Baby");
        pInf.setLastname("Doe");
        pInf.setPaxtitle("INF");
        passengerList.add(pInf);

        Passenger p = new Passenger();
        p.setPaxtype("ADULT");
        p.setFirstname("John");
        p.setLastname("Doe");
        p.setPaxtitle("MR");
        
        List<Passenger.Checkin> checkinList = new ArrayList<>();
        Passenger.Checkin chk = new Passenger.Checkin();
        chk.setSeat("12A");
        chk.setFlight("101");
        checkinList.add(chk);
        p.setCheckin(checkinList);
        p.setETickets(null); // baseTkt = "EMD" fallback
        passengerList.add(p);
        
        passengers.setPassenger(passengerList);
        booking.setPassengers(passengers);

        // Map inputs for database services to ensure combinedSrvMap is not empty
        // And we pass dbTotalOrderPrice to hit line 1004 (bookingTotal = dbTotalOrderPrice)
        Map<String, String> passengerSeats = new HashMap<>();
        passengerSeats.put("T2", "12A|50.00"); // T2 is the adult passenger (infant was T1.1)

        OrderRetrieveRspDto response = OrderRetrieveResponse.generateResponse(
                go7Response, passengerSeats, null, BigDecimal.valueOf(300.00)
        );

        assertNotNull(response);
        assertEquals("T1.1", response.getPaxDetailList().get(0).getPaxId()); // infant first
        assertFalse(response.getEmdInfoList().isEmpty());
    }

    private OrderRetrieveRspGo7Dto createBaseBookingResponse() {
        OrderRetrieveRspGo7Dto bookingRsp = new OrderRetrieveRspGo7Dto();
        Aerocrs aerocrs = new Aerocrs();
        Booking booking = new Booking();
        booking.setPnrref("TESTPNR");
        booking.setBookingid(12345L);
        booking.setCurrency("USD");
        booking.setTotalprice("100.00");
        booking.setBookingconfirmation("CONF123");
        
        Flights flights = new Flights();
        flights.setFlight(new ArrayList<>());
        booking.setFlights(flights);
        
        aerocrs.setBooking(booking);
        bookingRsp.setAerocrs(aerocrs);
        return bookingRsp;
    }
}
