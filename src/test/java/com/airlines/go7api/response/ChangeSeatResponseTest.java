package com.airlines.go7api.response;

import com.airlines.go7api.requestdto.ChangeSeatReqDto;
import com.airlines.go7api.requestdto.common.ChangeOfferReqDto;
import com.airlines.go7api.requestdto.common.PaymentInformationReqDto;
import com.airlines.go7api.responsedto.ChangeSeatRspDto;
import com.airlines.go7api.responsedto.common.OrderItemsDTO;
import com.airlines.go7api.responsego7.ChangeSeatRspGo7Dto;
import com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto;
import com.airlines.go7api.responsego7.common.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ChangeSeatResponseTest {

    @Test
    void testGenerateResponse_HappyPath() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);

        InputStream is = getClass().getClassLoader().getResourceAsStream("change_seat_response.json");
        ChangeSeatRspGo7Dto go7Response = mapper.readValue(is, ChangeSeatRspGo7Dto.class);

        InputStream is2 = getClass().getClassLoader().getResourceAsStream("order_retrieve_response.json");
        OrderRetrieveRspGo7Dto bookingRsp = mapper.readValue(is2, OrderRetrieveRspGo7Dto.class);

        ChangeSeatReqDto reqDto = new ChangeSeatReqDto();

        ChangeSeatRspDto response = ChangeSeatResponse.generateResponse(
                go7Response, bookingRsp, reqDto, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );

        assertNotNull(response);
    }

    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<ChangeSeatResponse> constructor = ChangeSeatResponse.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertTrue(exception.getCause() instanceof IllegalStateException);
        assertEquals("Utility class", exception.getCause().getMessage());
    }

    @Test
    void testGenerateResponse_NullOrEmptyBookingRsp() {
        // Test absolute null
        ChangeSeatRspDto response = ChangeSeatResponse.generateResponse(
                new ChangeSeatRspGo7Dto(), null, new ChangeSeatReqDto(), new HashMap<>(), new HashMap<>(), new HashMap<>()
        );
        assertNotNull(response);
        assertNull(response.getPnr());

        // Test missing Aerocrs
        OrderRetrieveRspGo7Dto bookingRsp = new OrderRetrieveRspGo7Dto();
        response = ChangeSeatResponse.generateResponse(
                new ChangeSeatRspGo7Dto(), bookingRsp, new ChangeSeatReqDto(), new HashMap<>(), new HashMap<>(), new HashMap<>()
        );
        assertNotNull(response);

        // Test missing Booking
        Aerocrs aerocrs = new Aerocrs();
        bookingRsp.setAerocrs(aerocrs);
        response = ChangeSeatResponse.generateResponse(
                new ChangeSeatRspGo7Dto(), bookingRsp, new ChangeSeatReqDto(), new HashMap<>(), new HashMap<>(), new HashMap<>()
        );
        assertNotNull(response);
    }

    @Test
    void testOvernightFlightDateAdjustment() {
        OrderRetrieveRspGo7Dto bookingRsp = createBaseBookingResponse();
        Booking booking = bookingRsp.getAerocrs().getBooking();

        Flights flights = new Flights();
        List<Flight> flightList = new ArrayList<>();

        // 1. Overnight flight (depart 23:00, arrive 02:00 next day)
        Flight f1 = new Flight();
        f1.setNumber("101");
        f1.setFromcode("JFK");
        f1.setTocode("LHR");
        f1.setFlightdate("2026-06-01");
        f1.setDepart("23:00");
        f1.setArrive("02:00");
        f1.setFlightClass("F/First");
        f1.setAirlinedesignator("G7");
        flightList.add(f1);

        // 2. Flight with parsing exception in overnight logic
        Flight f2 = new Flight();
        f2.setNumber("102");
        f2.setFromcode("LHR");
        f2.setTocode("CDG");
        f2.setFlightdate("2026-06-02");
        f2.setDepart("invalid_time");
        f2.setArrive("12:00");
        f2.setFlightClass("C/Business");
        f2.setAirlinedesignator("G7");
        flightList.add(f2);

        flights.setFlight(flightList);
        booking.setFlights(flights);

        ChangeSeatRspDto response = ChangeSeatResponse.generateResponse(
                new ChangeSeatRspGo7Dto(), bookingRsp, null, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );

        assertNotNull(response);
        assertEquals(2, response.getOds().size());
        // f1 arrival date should be adjusted by +1 day (overnight)
        assertEquals("02Jun2026", response.getOds().get(0).getArrivalDate());
        // f2 arrival date should remain the same due to parsing exception
        assertEquals("02Jun2026", response.getOds().get(1).getArrivalDate());
    }

    @Test
    void testCabinAndRbdAndClassNameParsing() {
        OrderRetrieveRspGo7Dto bookingRsp = createBaseBookingResponse();
        Booking booking = bookingRsp.getAerocrs().getBooking();

        Flights flights = new Flights();
        List<Flight> flightList = new ArrayList<>();

        // Test cases for parseCabinAndRbd & determineClassName:
        // Case A: F/First (slash split, code length 1, First class code)
        Flight f1 = new Flight();
        f1.setFlightClass("F/First Class");
        f1.setDepart("10:00");
        f1.setArrive("12:00");
        f1.setFlightdate("2026-06-01");
        flightList.add(f1);

        // Case B: C/Business (slash split, code length 1, Business class code)
        Flight f2 = new Flight();
        f2.setFlightClass("C/Business Class");
        f2.setDepart("10:00");
        f2.setArrive("12:00");
        f2.setFlightdate("2026-06-01");
        flightList.add(f2);

        // Case C: Y/Economy (slash split, code length 1, Economy code)
        Flight f3 = new Flight();
        f3.setFlightClass("Y/Economy Class");
        f3.setDepart("10:00");
        f3.setArrive("12:00");
        f3.setFlightdate("2026-06-01");
        flightList.add(f3);

        // Case D: A (single-char class code without slash, First class code)
        Flight f4 = new Flight();
        f4.setFlightClass("A");
        f4.setDepart("10:00");
        f4.setArrive("12:00");
        f4.setFlightdate("2026-06-01");
        flightList.add(f4);

        // Case E: Z (single-char class code without slash, Business class code)
        Flight f5 = new Flight();
        f5.setFlightClass("Z");
        f5.setDepart("10:00");
        f5.setArrive("12:00");
        f5.setFlightdate("2026-06-01");
        flightList.add(f5);

        // Case F: YY (multiple-char class code, no slash)
        Flight f6 = new Flight();
        f6.setFlightClass("YY");
        f6.setDepart("10:00");
        f6.setArrive("12:00");
        f6.setFlightdate("2026-06-01");
        flightList.add(f6);

        // Case G: null rawClass
        Flight f7 = new Flight();
        f7.setFlightClass(null);
        f7.setDepart("10:00");
        f7.setArrive("12:00");
        f7.setFlightdate("2026-06-01");
        flightList.add(f7);

        // Case H: empty rawClass
        Flight f8 = new Flight();
        f8.setFlightClass("");
        f8.setDepart("10:00");
        f8.setArrive("12:00");
        f8.setFlightdate("2026-06-01");
        flightList.add(f8);

        // Case I: Y/ (slash split, parts length <= 1, rbdCode != null)
        Flight f9 = new Flight();
        f9.setFlightClass("Y/");
        f9.setDepart("10:00");
        f9.setArrive("12:00");
        f9.setFlightdate("2026-06-01");
        flightList.add(f9);

        // Case J: FIRST/Class (slash split, code length > 1)
        Flight f10 = new Flight();
        f10.setFlightClass("FIRST/Class");
        f10.setDepart("10:00");
        f10.setArrive("12:00");
        f10.setFlightdate("2026-06-01");
        flightList.add(f10);

        flights.setFlight(flightList);
        booking.setFlights(flights);

        ChangeSeatRspDto response = ChangeSeatResponse.generateResponse(
                new ChangeSeatRspGo7Dto(), bookingRsp, null, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );

        assertNotNull(response);
        assertEquals(10, response.getOds().size());
        assertEquals("First", response.getOds().get(0).getCabinType());
        assertEquals("First Class", response.getPriceClassList().get(0).getClassName());

        assertEquals("Business", response.getOds().get(1).getCabinType());
        assertEquals("Business Class", response.getPriceClassList().get(1).getClassName());

        assertEquals("Economy", response.getOds().get(2).getCabinType());
        assertEquals("Economy Class", response.getPriceClassList().get(2).getClassName());

        assertEquals("Economy", response.getOds().get(3).getCabinType());
        assertEquals("A", response.getPriceClassList().get(3).getClassName());

        assertEquals("Economy", response.getOds().get(4).getCabinType());
        assertEquals("Z", response.getPriceClassList().get(4).getClassName());

        assertEquals("Economy", response.getOds().get(5).getCabinType());
        assertEquals("YY", response.getPriceClassList().get(5).getClassName());

        assertEquals("Economy", response.getOds().get(6).getCabinType());
        assertEquals("Economy", response.getPriceClassList().get(6).getClassName());

        assertEquals("Economy", response.getOds().get(7).getCabinType());
        assertEquals("Economy", response.getPriceClassList().get(7).getClassName());

        assertEquals("Economy", response.getOds().get(8).getCabinType());
        assertEquals("Economy (Y)", response.getPriceClassList().get(8).getClassName());

        assertEquals("Economy", response.getOds().get(9).getCabinType());
        assertEquals("Class", response.getPriceClassList().get(9).getClassName());
    }

    @Test
    void testGenderAndInfantLinking() {
        OrderRetrieveRspGo7Dto bookingRsp = createBaseBookingResponse();
        Booking booking = bookingRsp.getAerocrs().getBooking();

        Passengers passengers = new Passengers();
        List<Passenger> passengerList = new ArrayList<>();

        // P1: Male, explicit gender
        Passenger p1 = new Passenger();
        p1.setPaxtype("ADULT");
        p1.setGender("Male");
        p1.setPaxtitle("MR");
        p1.setFirstname("John");
        p1.setLastname("Doe");

        // P2: Female, explicit gender starts with F
        Passenger p2 = new Passenger();
        p2.setPaxtype("ADULT");
        p2.setGender("Female");
        p2.setPaxtitle("MRS");
        p2.setFirstname("Jane");
        p2.setLastname("Doe");

        // P3: Title-based Male
        Passenger p3 = new Passenger();
        p3.setPaxtype("CHILD");
        p3.setGender("");
        p3.setPaxtitle("MSTR");
        p3.setFirstname("Billy");
        p3.setLastname("Doe");

        // P4: Title-based Female
        Passenger p4 = new Passenger();
        p4.setPaxtype("CHILD");
        p4.setGender("");
        p4.setPaxtitle("MISS");
        p4.setFirstname("Lily");
        p4.setLastname("Doe");

        // P5: Title-based Infant linked to ADT (parent)
        Passenger p5 = new Passenger();
        p5.setPaxtype("INFANT");
        p5.setGender("");
        p5.setPaxtitle("INF");
        p5.setFirstname("Baby");
        p5.setLastname("Doe");

        // P6: Title-based Infant where firstname contains INF but title doesn't, no ADT before it (empty adtList branch)
        Passenger p6 = new Passenger();
        p6.setPaxtype("INFANT");
        p6.setGender("");
        p6.setPaxtitle("DR");
        p6.setFirstname("BABYINF");
        p6.setLastname("Doe");

        passengerList.add(p6); // Infant first -> adtList is empty
        passengerList.add(p1); // Adult
        passengerList.add(p2);
        passengerList.add(p3);
        passengerList.add(p4);
        passengerList.add(p5); // Infant linked to p2

        passengers.setPassenger(passengerList);
        booking.setPassengers(passengers);

        ChangeSeatRspDto response = ChangeSeatResponse.generateResponse(
                new ChangeSeatRspGo7Dto(), bookingRsp, null, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );

        assertNotNull(response);
        assertEquals(6, response.getPaxDetailList().size());
        
        // p6: infant first, so gets ID T1.1 (no parent ADT)
        assertEquals("T1.1", response.getPaxDetailList().get(0).getPaxId());
        // p1: ADT, ID T2
        assertEquals("T2", response.getPaxDetailList().get(1).getPaxId());
        // p5: Infant linked to parent ADT (which is p2 -> ID T3)
        assertEquals("T3.1", response.getPaxDetailList().get(5).getPaxId());
        assertEquals("T3.1", response.getPaxDetailList().get(2).getInfantRef()); // P2's infantRef should be T3.1
    }

    @Test
    void testTicketDocsAndEmdGeneration() {
        OrderRetrieveRspGo7Dto bookingRsp = createBaseBookingResponse();
        Booking booking = bookingRsp.getAerocrs().getBooking();

        // Add flight
        Flights flights = new Flights();
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
        flightList.add(f);
        flights.setFlight(flightList);
        booking.setFlights(flights);

        // Add passenger
        Passengers passengers = new Passengers();
        List<Passenger> passengerList = new ArrayList<>();
        Passenger p = new Passenger();
        p.setPaxtype("ADT");
        p.setGender("Male");
        p.setPaxtitle("MR");
        p.setFirstname("John");
        p.setLastname("Doe");
        p.setEmail("john.doe@example.com");

        // Add ETicket
        Passenger.ETickets eTickets = new Passenger.ETickets();
        List<Passenger.ETicketFlight> etfList = new ArrayList<>();
        Passenger.ETicketFlight etf = new Passenger.ETicketFlight();
        etf.setEticketnumber("1234567890");
        etfList.add(etf);
        eTickets.setFlight(etfList);
        p.setETickets(eTickets);

        passengerList.add(p);
        passengers.setPassenger(passengerList);
        booking.setPassengers(passengers);

        // Setup requestDto with payment info
        ChangeSeatReqDto requestDto = new ChangeSeatReqDto();
        PaymentInformationReqDto payInfo = new PaymentInformationReqDto();
        payInfo.setAmount(new BigDecimal("100.00"));
        requestDto.setPaymentInformation(payInfo);

        // Setup offers for Seat "1A"
        List<ChangeOfferReqDto> offers = new ArrayList<>();
        ChangeOfferReqDto offer = new ChangeOfferReqDto();
        List<ChangeOfferReqDto.OfferItemDto> offerItems = new ArrayList<>();
        ChangeOfferReqDto.OfferItemDto item = new ChangeOfferReqDto.OfferItemDto();
        item.setPaxRefs(Arrays.asList("T1"));
        item.setRow(BigInteger.ONE);
        item.setColumn("A");
        offerItems.add(item);
        offer.setOfferItems(offerItems);
        offers.add(offer);
        requestDto.setOffers(offers);

        // Setup changeSeatRsp with seat "1A"
        ChangeSeatRspGo7Dto changeSeatRsp = new ChangeSeatRspGo7Dto();
        Aerocrs csrAerocrs = new Aerocrs();
        com.airlines.go7api.responsego7.common.Flights csrFlights = new com.airlines.go7api.responsego7.common.Flights();
        List<Flight> csrFlightList = new ArrayList<>();
        Flight csrF = new Flight();
        csrF.setNumber("101");
        csrF.setFromcode("JFK");
        csrF.setTocode("LHR");
        csrF.setFlightdate("2026-06-01");
        List<Seat> csrSeatList = new ArrayList<>();
        Seat s = new Seat();
        s.setSeatNumber("1A");
        s.setFare(new BigDecimal("25.00"));
        s.setStatus(true);
        csrSeatList.add(s);
        csrF.setSeat(csrSeatList);
        csrFlightList.add(csrF);
        csrFlights.setFlight(csrFlightList);
        csrAerocrs.setFlights(csrFlights);
        changeSeatRsp.setAerocrs(csrAerocrs);

        ChangeSeatRspDto response = ChangeSeatResponse.generateResponse(
                changeSeatRsp, bookingRsp, requestDto, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );

        assertNotNull(response);
        // Verify ETickets mapped
        assertNotNull(response.getTicketDocInfoList());
        assertEquals(1, response.getTicketDocInfoList().size());
        assertEquals("1234567890", response.getTicketDocInfoList().get(0).getTicketDocument().get(0).getTicketDocNbr());

        // Verify EMDs mapped
        assertNotNull(response.getEmdInfoList());
        assertEquals(1, response.getEmdInfoList().size());
        assertEquals("1234567890-1", response.getEmdInfoList().get(0).getTicketDocument().get(0).getTicketDocNbr());
    }

    @Test
    void testSeatChargesAndCalculations() {
        OrderRetrieveRspGo7Dto bookingRsp = createBaseBookingResponse();
        Booking booking = bookingRsp.getAerocrs().getBooking();

        // 1. Inv pricing parser check
        Flights flights = new Flights();
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
        // Inv pricing valid numbers
        f.setInvpricingwithouttax("100.00");
        f.setInvpricing("115.00");
        flightList.add(f);

        // F2 with parsing exception for invpricing
        Flight f2 = new Flight();
        f2.setNumber("102");
        f2.setFromcode("LHR");
        f2.setTocode("CDG");
        f2.setFlightdate("2026-06-01");
        f2.setDepart("10:00");
        f2.setArrive("12:00");
        f2.setFlightClass("Y/Economy");
        f2.setAirlinedesignator("G7");
        f2.setTotaltaxes(0.0);
        f2.setInvpricingwithouttax("invalid_num");
        f2.setInvpricing("invalid_num");
        flightList.add(f2);

        flights.setFlight(flightList);
        booking.setFlights(flights);

        // Setup passengers
        Passengers passengers = new Passengers();
        List<Passenger> passengerList = new ArrayList<>();
        Passenger p = new Passenger();
        p.setPaxtype("ADT");
        p.setGender("Male");
        p.setPaxtitle("MR");
        p.setFirstname("John");
        p.setLastname("Doe");
        passengerList.add(p);
        passengers.setPassenger(passengerList);
        booking.setPassengers(passengers);

        // Setup actual prices, passenger seats map, passenger services map
        Map<String, BigDecimal> actualPrices = new HashMap<>();
        actualPrices.put("1B", new BigDecimal("50.00"));

        Map<String, String> passengerSeatsMap = new HashMap<>();
        passengerSeatsMap.put("T1", "1B|50.00");
        // We also want to cover lookupCachedSeatPrice branch where seat number doesn't match or price fails
        passengerSeatsMap.put("T2", "1C|invalid_price");
        passengerSeatsMap.put("T3", "1D|-10.00");
        passengerSeatsMap.put("T4", "1E"); // doesn't contain |

        Map<String, String> passengerServicesMap = new HashMap<>();
        passengerServicesMap.put("T1", "MEAL|30.00");
        passengerServicesMap.put("T2", "BAGGAGE|invalid_price");

        ChangeSeatRspGo7Dto changeSeatRsp = new ChangeSeatRspGo7Dto();
        Aerocrs csrAerocrs = new Aerocrs();
        com.airlines.go7api.responsego7.common.Flights csrFlights = new com.airlines.go7api.responsego7.common.Flights();
        List<Flight> csrFlightList = new ArrayList<>();
        Flight csrF = new Flight();
        csrF.setNumber("101");
        csrF.setFromcode("JFK");
        csrF.setTocode("LHR");
        csrF.setFlightdate("2026-06-01");
        List<Seat> csrSeatList = new ArrayList<>();
        Seat s = new Seat();
        s.setSeatNumber("1B");
        s.setFare(null); // triggers fallback to actualPrices
        s.setStatus(true);
        csrSeatList.add(s);
        csrF.setSeat(csrSeatList);
        csrFlightList.add(csrF);
        csrFlights.setFlight(csrFlightList);
        csrAerocrs.setFlights(csrFlights);
        changeSeatRsp.setAerocrs(csrAerocrs);

        ChangeSeatRspDto response = ChangeSeatResponse.generateResponse(
                changeSeatRsp, bookingRsp, null, actualPrices, passengerSeatsMap, passengerServicesMap
        );

        assertNotNull(response);
        // Verify seatCharges calculated from actualPrices
        assertEquals(new BigDecimal("195.00"), response.getTotalOrderPrice());
    }

    @Test
    void testGranularTaxScalingAndZeroGranularFallback() {
        OrderRetrieveRspGo7Dto bookingRsp = createBaseBookingResponse();
        Booking booking = bookingRsp.getAerocrs().getBooking();

        Flights flights = new Flights();
        List<Flight> flightList = new ArrayList<>();

        // Flight 1: Has total tax and granular taxes
        Flight f1 = new Flight();
        f1.setNumber("101");
        f1.setFromcode("JFK");
        f1.setTocode("LHR");
        f1.setFlightdate("2026-06-01");
        f1.setDepart("10:00");
        f1.setArrive("12:00");
        f1.setFlightClass("Y/Economy");
        f1.setAirlinedesignator("G7");
        f1.setTotaltaxes(10.00);

        Taxes t1 = new Taxes();
        t1.setSecurity(2.0);
        t1.setFuel(3.0);
        t1.setGroundHandling(1.0);
        t1.setTax1(2.0); // Infrastructure
        t1.setTax4(2.0); // Other
        f1.setTaxes(t1);
        flightList.add(f1);

        flights.setFlight(flightList);
        booking.setFlights(flights);

        // Passengers
        Passengers passengers = new Passengers();
        List<Passenger> passengerList = new ArrayList<>();
        Passenger p = new Passenger();
        p.setPaxtype("ADT");
        p.setFirstname("John");
        p.setLastname("Doe");
        passengerList.add(p);
        passengers.setPassenger(passengerList);
        booking.setPassengers(passengers);

        ChangeSeatRspDto response = ChangeSeatResponse.generateResponse(
                new ChangeSeatRspGo7Dto(), bookingRsp, null, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );

        assertNotNull(response);
        assertFalse(response.getOrderItems().isEmpty());
        OrderItemsDTO item = response.getOrderItems().get(0);
        assertNotNull(item.getTaxes());
        // Verify there are scaled tax entries (5 entries)
        assertEquals(5, item.getTaxes().size());

        // Now test when totalTax > 0 but totalGranular == 0 (zero granular tax fallback)
        f1.setTaxes(null);
        response = ChangeSeatResponse.generateResponse(
                new ChangeSeatRspGo7Dto(), bookingRsp, null, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );
        assertNotNull(response);
        assertNull(response.getOrderItems().get(0).getTaxes());
    }

    @Test
    void testPaymentMapping() {
        OrderRetrieveRspGo7Dto bookingRsp = createBaseBookingResponse();
        Booking booking = bookingRsp.getAerocrs().getBooking();

        Passengers passengers = new Passengers();
        List<Passenger> passengerList = new ArrayList<>();
        Passenger p = new Passenger();
        p.setPaxtype("ADT");
        p.setFirstname("John");
        p.setLastname("Doe");
        passengerList.add(p);
        passengers.setPassenger(passengerList);
        booking.setPassengers(passengers);

        // Setup requestDto with payment info and custom paymentType
        ChangeSeatReqDto requestDto = new ChangeSeatReqDto();
        PaymentInformationReqDto payInfo = new PaymentInformationReqDto();
        payInfo.setAmount(new BigDecimal("150.00"));
        requestDto.setPaymentInformation(payInfo);
        requestDto.setPaymentType("CC");

        ChangeSeatRspDto response = ChangeSeatResponse.generateResponse(
                new ChangeSeatRspGo7Dto(), bookingRsp, requestDto, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );

        assertNotNull(response);
        assertNotNull(response.getPayments());
        assertEquals(1, response.getPayments().size());
        assertEquals("CC", response.getPayments().get(0).getType());
        assertEquals(new BigDecimal("150.00"), response.getPayments().get(0).getAmount());

        // Test fallback when payment type is null -> defaults to "CA"
        requestDto.setPaymentType(null);
        response = ChangeSeatResponse.generateResponse(
                new ChangeSeatRspGo7Dto(), bookingRsp, requestDto, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );
        assertEquals("CA", response.getPayments().get(0).getType());

        // Test fallback when payment amount is null -> defaults to seatCharges
        booking.setTotalprice("0.00");
        payInfo.setAmount(null);
        response = ChangeSeatResponse.generateResponse(
                new ChangeSeatRspGo7Dto(), bookingRsp, requestDto, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );
        // Now both paymentSeatAmount and seatCharges are 0, so payments list is empty
        assertTrue(response.getPayments().isEmpty());
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
        
        aerocrs.setBooking(booking);
        bookingRsp.setAerocrs(aerocrs);
        return bookingRsp;
    }
}
