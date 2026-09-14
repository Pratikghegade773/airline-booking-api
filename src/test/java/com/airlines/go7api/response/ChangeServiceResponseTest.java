package com.airlines.go7api.response;

import com.airlines.go7api.requestdto.ChangeServiceReqDto;
import com.airlines.go7api.requestdto.common.ChangeOfferReqDto;
import com.airlines.go7api.requestdto.common.PaymentInformationReqDto;
import com.airlines.go7api.responsedto.ChangeServiceRspDto;
import com.airlines.go7api.responsedto.common.OrderItemsDTO;
import com.airlines.go7api.responsego7.ChangeServiceRspGo7Dto;
import com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto;
import com.airlines.go7api.responsego7.common.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ChangeServiceResponseTest {

    @Test
    void testGenerateResponse_HappyPath() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);

        InputStream is = getClass().getClassLoader().getResourceAsStream("change_service_response.json");
        ChangeServiceRspGo7Dto go7Response = mapper.readValue(is, ChangeServiceRspGo7Dto.class);

        InputStream is2 = getClass().getClassLoader().getResourceAsStream("order_retrieve_response.json");
        OrderRetrieveRspGo7Dto bookingRsp = mapper.readValue(is2, OrderRetrieveRspGo7Dto.class);

        ChangeServiceReqDto reqDto = new ChangeServiceReqDto();

        ChangeServiceRspDto response = ChangeServiceResponse.generateResponse(
                go7Response, bookingRsp, reqDto, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );

        assertNotNull(response);
    }

    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<ChangeServiceResponse> constructor = ChangeServiceResponse.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertTrue(exception.getCause() instanceof IllegalStateException);
        assertEquals("Utility class", exception.getCause().getMessage());
    }

    @Test
    void testGenerateResponse_NullOrEmptyBookingRsp() {
        // Test absolute null
        ChangeServiceRspDto response = ChangeServiceResponse.generateResponse(
                new ChangeServiceRspGo7Dto(), null, new ChangeServiceReqDto(), new HashMap<>(), new HashMap<>(), new HashMap<>()
        );
        assertNotNull(response);
        assertNull(response.getPnr());

        // Test missing Aerocrs
        OrderRetrieveRspGo7Dto bookingRsp = new OrderRetrieveRspGo7Dto();
        response = ChangeServiceResponse.generateResponse(
                new ChangeServiceRspGo7Dto(), bookingRsp, new ChangeServiceReqDto(), new HashMap<>(), new HashMap<>(), new HashMap<>()
        );
        assertNotNull(response);

        // Test missing Booking
        Aerocrs aerocrs = new Aerocrs();
        bookingRsp.setAerocrs(aerocrs);
        response = ChangeServiceResponse.generateResponse(
                new ChangeServiceRspGo7Dto(), bookingRsp, new ChangeServiceReqDto(), new HashMap<>(), new HashMap<>(), new HashMap<>()
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

        ChangeServiceRspDto response = ChangeServiceResponse.generateResponse(
                new ChangeServiceRspGo7Dto(), bookingRsp, null, new HashMap<>(), new HashMap<>(), new HashMap<>()
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

        ChangeServiceRspDto response = ChangeServiceResponse.generateResponse(
                new ChangeServiceRspGo7Dto(), bookingRsp, null, new HashMap<>(), new HashMap<>(), new HashMap<>()
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

        ChangeServiceRspDto response = ChangeServiceResponse.generateResponse(
                new ChangeServiceRspGo7Dto(), bookingRsp, null, new HashMap<>(), new HashMap<>(), new HashMap<>()
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
        ChangeServiceReqDto requestDto = new ChangeServiceReqDto();
        PaymentInformationReqDto payInfo = new PaymentInformationReqDto();
        payInfo.setAmount(new BigDecimal("100.00"));
        requestDto.setPaymentInformation(payInfo);

        // Setup offers for Service "SRV123"
        List<ChangeOfferReqDto> offers = new ArrayList<>();
        ChangeOfferReqDto offer = new ChangeOfferReqDto();
        List<ChangeOfferReqDto.OfferItemDto> offerItems = new ArrayList<>();
        ChangeOfferReqDto.OfferItemDto item = new ChangeOfferReqDto.OfferItemDto();
        item.setPaxRefs(Arrays.asList("T1"));
        item.setOfferItemId("SRV123");
        offerItems.add(item);
        offer.setOfferItems(offerItems);
        offers.add(offer);
        requestDto.setOffers(offers);

        // Setup changeServiceRsp with ancillary details
        ChangeServiceRspGo7Dto changeServiceRsp = new ChangeServiceRspGo7Dto();
        Aerocrs csrAerocrs = new Aerocrs();
        List<Aerocrs.Detail> details = new ArrayList<>();
        Aerocrs.Detail detail = new Aerocrs.Detail();
        detail.setInvid("999");
        detail.setSuccess(true);
        Map<String, Object> ancillary = new HashMap<>();
        ancillary.put("totalprice", 30.00);
        ancillary.put("code", "MEAL");
        ancillary.put("name", "Warm Meal");
        detail.setAncillary(ancillary);
        details.add(detail);
        csrAerocrs.setDetails(details);
        changeServiceRsp.setAerocrs(csrAerocrs);

        ChangeServiceRspDto response = ChangeServiceResponse.generateResponse(
                changeServiceRsp, bookingRsp, requestDto, new HashMap<>(), new HashMap<>(), new HashMap<>()
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
    void testServiceChargesAndCalculations() {
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
        actualPrices.put("SRV1", new BigDecimal("50.00"));

        Map<String, String> passengerSeatsMap = new HashMap<>();
        passengerSeatsMap.put("T1", "1A|25.00");

        Map<String, String> passengerServicesMap = new HashMap<>();
        passengerServicesMap.put("T1", "SRV1|50.00");
        passengerServicesMap.put("T2", "SRV2|invalid_price");
        passengerServicesMap.put("T3", "SRV3|-10.00");
        passengerServicesMap.put("T4", "SRV4"); // doesn't contain |

        ChangeServiceRspGo7Dto changeServiceRsp = new ChangeServiceRspGo7Dto();
        Aerocrs csrAerocrs = new Aerocrs();
        List<Aerocrs.Detail> details = new ArrayList<>();
        Aerocrs.Detail detail = new Aerocrs.Detail();
        detail.setInvid("888");
        detail.setSuccess(true);
        Map<String, Object> ancillary = new HashMap<>();
        // Null price trigger fallback to actualPrices
        ancillary.put("totalprice", null);
        ancillary.put("itemid", "SRV1");
        detail.setAncillary(ancillary);
        details.add(detail);
        csrAerocrs.setDetails(details);
        changeServiceRsp.setAerocrs(csrAerocrs);

        ChangeServiceRspDto response = ChangeServiceResponse.generateResponse(
                changeServiceRsp, bookingRsp, null, actualPrices, passengerSeatsMap, passengerServicesMap
        );

        assertNotNull(response);
        // Verify total price is correct (airItemPrice + secondary services prices)
        assertEquals(new BigDecimal("240.00"), response.getTotalOrderPrice());
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

        ChangeServiceRspDto response = ChangeServiceResponse.generateResponse(
                new ChangeServiceRspGo7Dto(), bookingRsp, null, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );

        assertNotNull(response);
        assertFalse(response.getOrderItems().isEmpty());
        OrderItemsDTO item = response.getOrderItems().get(0);
        assertNotNull(item.getTaxes());
        // Verify there are scaled tax entries (5 entries)
        assertEquals(5, item.getTaxes().size());

        // Now test when totalTax > 0 but totalGranular == 0 (zero granular tax fallback)
        f1.setTaxes(null);
        response = ChangeServiceResponse.generateResponse(
                new ChangeServiceRspGo7Dto(), bookingRsp, null, new HashMap<>(), new HashMap<>(), new HashMap<>()
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
        ChangeServiceReqDto requestDto = new ChangeServiceReqDto();
        PaymentInformationReqDto payInfo = new PaymentInformationReqDto();
        payInfo.setAmount(new BigDecimal("150.00"));
        requestDto.setPaymentInformation(payInfo);
        requestDto.setPaymentType("CC");

        ChangeServiceRspDto response = ChangeServiceResponse.generateResponse(
                new ChangeServiceRspGo7Dto(), bookingRsp, requestDto, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );

        assertNotNull(response);
        assertNotNull(response.getPayments());
        assertEquals(1, response.getPayments().size());
        assertEquals("CC", response.getPayments().get(0).getType());
        assertEquals(new BigDecimal("150.00"), response.getPayments().get(0).getAmount());

        // Test fallback when payment type is null -> defaults to "CA"
        requestDto.setPaymentType(null);
        response = ChangeServiceResponse.generateResponse(
                new ChangeServiceRspGo7Dto(), bookingRsp, requestDto, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );
        assertEquals("CA", response.getPayments().get(0).getType());

        // Test fallback when payment amount is null -> defaults to actualSrvPrice
        booking.setTotalprice("0.00");
        payInfo.setAmount(null);
        response = ChangeServiceResponse.generateResponse(
                new ChangeServiceRspGo7Dto(), bookingRsp, requestDto, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );
        // Now both paymentSrvAmount and actualSrvPrice are 0, so payments list is empty
        assertTrue(response.getPayments().isEmpty());
    }

    @Test
    void testAdditionalUncoveredBranches() {
        // 1. booking.getFlights() is null, but booking.getItems() has flights (line 85, 87, 88)
        OrderRetrieveRspGo7Dto bookingRsp = createBaseBookingResponse();
        Booking booking = bookingRsp.getAerocrs().getBooking();
        booking.setFlights(null);
        
        Items items = new Items();
        List<Flight> flightList = new ArrayList<>();
        Flight f = new Flight();
        f.setNumber("101");
        f.setFromcode("JFK");
        f.setTocode("LHR");
        f.setFlightdate("2026-06-01");
        f.setDepart(null); // to hit depart/arrive null branch (line 211)
        f.setArrive(null);
        f.setFlightClass("Y/Economy");
        f.setAirlinedesignator(null); // to hit desig == null (line 121)
        f.setAirline(null); // will default to "Airline" (line 177)
        f.setTotaltaxes(10.00);
        f.setInvpricing("100.00");
        flightList.add(f);
        items.setFlight(flightList);
        booking.setItems(items);

        // Also test: changeServiceRsp is null, or aerocrs is null (line 116-118)
        // And booking.getCurrency() is null (line 133)
        booking.setCurrency(null);

        ChangeServiceRspDto response = ChangeServiceResponse.generateResponse(
                null, bookingRsp, null, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );
        assertNotNull(response);
        assertEquals("G7", response.getValidatingCarrier());
        assertEquals("USD", response.getCurrency()); // defaults to USD
        assertEquals("Airline", response.getOds().get(0).getMarketingCarrierName()); // defaults to Airline
        assertNull(response.getOds().get(0).getDepartureTime());
    }

    @Test
    void testCabinAndRbdSplitsAndEdgeCases() {
        OrderRetrieveRspGo7Dto bookingRsp = createBaseBookingResponse();
        Booking booking = bookingRsp.getAerocrs().getBooking();
        
        Flights flights = new Flights();
        List<Flight> flightList = new ArrayList<>();
        
        // Flight 1: rawClass is "/" (parts.length == 0, line 230, 410)
        Flight f1 = new Flight();
        f1.setFlightClass("/");
        f1.setFlightdate("2026-06-01");
        flightList.add(f1);

        // Flight 2: rawClass is "FIRST/" (parts.length == 1, rbdCode is null, line 261)
        Flight f2 = new Flight();
        f2.setFlightClass("FIRST/");
        f2.setFlightdate("2026-06-01");
        flightList.add(f2);
        
        // Flight 3: rawClass is single char but length 1 without slash (line 236)
        Flight f3 = new Flight();
        f3.setFlightClass("Y");
        f3.setFlightdate("2026-06-01");
        flightList.add(f3);

        // Flight 4: rawClass containing F/A/P or C/J/D/Z/I (line 243, 246)
        Flight f4 = new Flight();
        f4.setFlightClass("P/Premium");
        f4.setFlightdate("2026-06-01");
        flightList.add(f4);

        Flight f5 = new Flight();
        f5.setFlightClass("J/Club");
        f5.setFlightdate("2026-06-01");
        flightList.add(f5);

        flights.setFlight(flightList);
        booking.setFlights(flights);

        ChangeServiceRspDto response = ChangeServiceResponse.generateResponse(
                new ChangeServiceRspGo7Dto(), bookingRsp, null, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );
        assertNotNull(response);
    }

    @Test
    void testPassengerDetailsAdditionalBranches() {
        OrderRetrieveRspGo7Dto bookingRsp = createBaseBookingResponse();
        Booking booking = bookingRsp.getAerocrs().getBooking();

        Passengers passengers = new Passengers();
        List<Passenger> passengerList = new ArrayList<>();

        // P1: CHD with null first name, null last name, gender starting with F (e.g. "Female") (line 611, 612, 617)
        Passenger p1 = new Passenger();
        p1.setPaxtype("CHILD");
        p1.setGender("Female");
        p1.setPaxtitle("MISS");
        passengerList.add(p1);

        // P2: CNN with gender unknown (line 375 fallback, line 382, 384 title searches)
        Passenger p2 = new Passenger();
        p2.setPaxtype("CHILD");
        p2.setGender("Unknown");
        p2.setPaxtitle("DR"); // neither MR nor MRS
        passengerList.add(p2);

        passengers.setPassenger(passengerList);
        booking.setPassengers(passengers);

        ChangeServiceRspDto response = ChangeServiceResponse.generateResponse(
                new ChangeServiceRspGo7Dto(), bookingRsp, null, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );
        assertNotNull(response);
        assertEquals("CHILD", response.getPaxDetailList().get(0).getTitle());
        assertEquals("", response.getPaxDetailList().get(0).getGivenName());
        assertEquals("Female", response.getPaxDetailList().get(0).getGender());
        assertEquals("Male", response.getPaxDetailList().get(1).getGender()); // fallback gender
    }

    @Test
    void testTicketDocsAndEmdEdgeCases() {
        OrderRetrieveRspGo7Dto bookingRsp = createBaseBookingResponse();
        Booking booking = bookingRsp.getAerocrs().getBooking();

        // Flight 1: set aircraft type IATA code (line 439, 567)
        Flights flights = new Flights();
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
        flightList.add(f);
        flights.setFlight(flightList);
        booking.setFlights(flights);

        // Passenger 1: has ETickets but flight list is null, or empty (line 528-529)
        Passengers passengers = new Passengers();
        List<Passenger> passengerList = new ArrayList<>();
        Passenger p = new Passenger();
        p.setPaxtype("ADT");
        p.setFirstname("John");
        p.setLastname("Doe");
        Passenger.ETickets eTickets = new Passenger.ETickets();
        eTickets.setFlight(null); // flight list is null
        p.setETickets(eTickets);
        passengerList.add(p);
        passengers.setPassenger(passengerList);
        booking.setPassengers(passengers);

        // Request has payment info (isPaymentProvided is true)
        ChangeServiceReqDto requestDto = new ChangeServiceReqDto();
        PaymentInformationReqDto payInfo = new PaymentInformationReqDto();
        payInfo.setAmount(new BigDecimal("100.00"));
        requestDto.setPaymentInformation(payInfo);

        // booking has linktoticket (line 524, 525)
        booking.setLinktoticket("9991234567");

        ChangeServiceRspDto response = ChangeServiceResponse.generateResponse(
                new ChangeServiceRspGo7Dto(), bookingRsp, requestDto, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );
        assertNotNull(response);
        // EMD doc number should be derived from linktoticket ("9991234567")
        assertEquals("9991234567-1", response.getEmdInfoList().get(0).getTicketDocument().get(0).getTicketDocNbr());
    }

    @Test
    void testFallbackServicesAdditionalCoverage() {
        OrderRetrieveRspGo7Dto bookingRsp = createBaseBookingResponse();
        Booking booking = bookingRsp.getAerocrs().getBooking();
        // 1. Set pnrTotal = 150.00, airItemPrice = 100.00 (calculated from flight list). So actualSrvPrice = 50.00
        booking.setTotalprice("150.00");
        
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
        f.setInvpricing("100.00");
        flightList.add(f);
        flights.setFlight(flightList);
        booking.setFlights(flights);

        // Passengers: empty rawPaxIds check (line 930)
        Passengers passengers = new Passengers();
        passengers.setPassenger(null); // passenger list null
        booking.setPassengers(passengers);

        // Request with offers (so paxToRequestedServices is not empty)
        ChangeServiceReqDto requestDto = new ChangeServiceReqDto();
        List<ChangeOfferReqDto> offers = new ArrayList<>();
        
        // Offer with null offerItems (line 324, 493)
        ChangeOfferReqDto offer1 = new ChangeOfferReqDto();
        offer1.setOfferItems(null);
        offers.add(offer1);

        ChangeOfferReqDto offer2 = new ChangeOfferReqDto();
        List<ChangeOfferReqDto.OfferItemDto> offerItems = new ArrayList<>();
        
        // Offer item with null paxRefs or null offerItemId (line 326, 495)
        ChangeOfferReqDto.OfferItemDto item1 = new ChangeOfferReqDto.OfferItemDto();
        item1.setPaxRefs(null);
        item1.setOfferItemId(null);
        offerItems.add(item1);

        ChangeOfferReqDto.OfferItemDto item2 = new ChangeOfferReqDto.OfferItemDto();
        item2.setPaxRefs(Arrays.asList("T1")); // T1 is not in rawPaxIds because passengers list is null/empty
        item2.setOfferItemId("SRV123");
        offerItems.add(item2);
        
        offer2.setOfferItems(offerItems);
        offers.add(offer2);
        requestDto.setOffers(offers);

        // Details provided is false (changeServiceRsp has no ancillary)
        ChangeServiceRspGo7Dto changeServiceRsp = new ChangeServiceRspGo7Dto();
        
        // Actual prices contains SRV123 (to hit actualPrices.containsKey inside processFallbackService)
        Map<String, BigDecimal> actualPrices = new HashMap<>();
        actualPrices.put("SRV123", new BigDecimal("50.00"));

        ChangeServiceRspDto response = ChangeServiceResponse.generateResponse(
                changeServiceRsp, bookingRsp, requestDto, actualPrices, new HashMap<>(), new HashMap<>()
        );
        assertNotNull(response);
    }

    @Test
    void testCachedPricesAndDistributedLeftoverPrice() {
        OrderRetrieveRspGo7Dto bookingRsp = createBaseBookingResponse();
        Booking booking = bookingRsp.getAerocrs().getBooking();
        // 1. Set pnrTotal = 500.00, airItemPrice = 100.00 (from flight list). So totalSecondaryNeeded = 400.00
        booking.setTotalprice("500.00");
        
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
        f.setInvpricing("100.00");
        flightList.add(f);
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

        // We have a payment in requestDto, but amount is 0.00 (or null) to hit paymentSrvAmount <= 0 (line 1196)
        ChangeServiceReqDto requestDto = new ChangeServiceReqDto();
        PaymentInformationReqDto payInfo = new PaymentInformationReqDto();
        payInfo.setAmount(BigDecimal.ZERO);
        requestDto.setPaymentInformation(payInfo);

        // Details: changeServiceRsp has details, but with totalprice = null or ZERO, and itemid = SRV_NONE (not in actualPrices)
        // This makes detail ancillary price = ZERO, triggering lookupCachedServicePrice with currentPrice = ZERO (line 803)
        ChangeServiceRspGo7Dto changeServiceRsp = new ChangeServiceRspGo7Dto();
        Aerocrs csrAerocrs = new Aerocrs();
        
        // Test when companycode is present in aerocrs (line 116-118)
        csrAerocrs.setCompanycode("XX");

        List<Aerocrs.Detail> details = new ArrayList<>();
        Aerocrs.Detail detail1 = new Aerocrs.Detail();
        detail1.setInvid("999");
        detail1.setSuccess(false); // test detail.isSuccess() is false (line 902)
        Map<String, Object> ancillary = new HashMap<>();
        ancillary.put("totalprice", "invalid_price"); // test NumberFormatException in addAncillaryPrice (line 640)
        ancillary.put("itemid", "SRV_NONE");
        ancillary.put("currency", "EUR"); // currency is EUR to hit currObj != null (line 883)
        // Also test item_name instead of code (line 850, 851)
        ancillary.put("item_name", "AncillaryName");
        detail1.setAncillary(ancillary);
        details.add(detail1);

        csrAerocrs.setDetails(details);
        changeServiceRsp.setAerocrs(csrAerocrs);

        // passengerServicesMap has T1 cached with "|" and positive price (will be found in lookupCachedServicePrice)
        // also has another entry with invalid price (to trigger NumberFormatException in line 813, 1054)
        Map<String, String> passengerServicesMap = new HashMap<>();
        passengerServicesMap.put("T1", "SRV1|150.00");
        passengerServicesMap.put("T2", "SRV2|invalid_price");

        // passengerSeatsMap has seat cached with "|" and positive price, and another with invalid price
        Map<String, String> passengerSeatsMap = new HashMap<>();
        passengerSeatsMap.put("T1", "1A|50.00");
        passengerSeatsMap.put("T2", "1B|invalid_price");

        ChangeServiceRspDto response = ChangeServiceResponse.generateResponse(
                changeServiceRsp, bookingRsp, requestDto, null, passengerSeatsMap, passengerServicesMap
        );

        assertNotNull(response);
        assertEquals("XX", response.getValidatingCarrier());
        // Verify payment is added and its amount matches totalSrvPrice (which is > 0)
        assertFalse(response.getPayments().isEmpty());
    }

    @Test
    void testAirBaseFareNegativeFallback() {
        OrderRetrieveRspGo7Dto bookingRsp = createBaseBookingResponse();
        Booking booking = bookingRsp.getAerocrs().getBooking();
        
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
        f.setInvpricing("10.00");
        flightList.add(f);
        flights.setFlight(flightList);
        booking.setFlights(flights);

        ChangeServiceRspDto response = ChangeServiceResponse.generateResponse(
                new ChangeServiceRspGo7Dto(), bookingRsp, null, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );
        assertNotNull(response);
        assertEquals(new BigDecimal("10.00"), response.getOrderItems().get(0).getBaseFare().getAmount());
    }

    @Test
    void testPnrTotalParsingException() {
        OrderRetrieveRspGo7Dto bookingRsp = createBaseBookingResponse();
        Booking booking = bookingRsp.getAerocrs().getBooking();
        
        BalanceInformation balance = new BalanceInformation();
        balance.setPnrTotal(null);
        booking.setBalanceInformation(balance);
        booking.setTotalprice("invalid_price"); // triggers exception in parsing (line 714)

        ChangeServiceRspDto response = ChangeServiceResponse.generateResponse(
                new ChangeServiceRspGo7Dto(), bookingRsp, null, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );
        assertNotNull(response);
    }

    @Test
    void testCachedServicePriceLookupZero() {
        OrderRetrieveRspGo7Dto bookingRsp = createBaseBookingResponse();
        Booking booking = bookingRsp.getAerocrs().getBooking();
        booking.setTotalprice("100.00");
        
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
        f.setInvpricing("100.00");
        flightList.add(f);
        flights.setFlight(flightList);
        booking.setFlights(flights);

        Passengers passengers = new Passengers();
        List<Passenger> passengerList = new ArrayList<>();
        Passenger p = new Passenger();
        p.setPaxtype("ADT");
        p.setFirstname("John");
        p.setLastname("Doe");
        passengerList.add(p);
        passengers.setPassenger(passengerList);
        booking.setPassengers(passengers);

        ChangeServiceRspGo7Dto changeServiceRsp = new ChangeServiceRspGo7Dto();
        Aerocrs csrAerocrs = new Aerocrs();
        List<Aerocrs.Detail> details = new ArrayList<>();
        Aerocrs.Detail detail = new Aerocrs.Detail();
        detail.setInvid("999");
        detail.setSuccess(true);
        Map<String, Object> ancillary = new HashMap<>();
        ancillary.put("totalprice", 0.0); // ZERO price
        ancillary.put("itemid", "SRV_NONE");
        detail.setAncillary(ancillary);
        details.add(detail);
        csrAerocrs.setDetails(details);
        changeServiceRsp.setAerocrs(csrAerocrs);

        Map<String, String> passengerServicesMap = new HashMap<>();
        passengerServicesMap.put("T1", "SRV1|150.00");

        ChangeServiceRspDto response = ChangeServiceResponse.generateResponse(
                changeServiceRsp, bookingRsp, null, new HashMap<>(), new HashMap<>(), passengerServicesMap
        );
        assertNotNull(response);
    }

    @Test
    void testPaymentProvidedWithPositiveAmountAndZeroSrvPrice() {
        OrderRetrieveRspGo7Dto bookingRsp = createBaseBookingResponse();
        Booking booking = bookingRsp.getAerocrs().getBooking();
        booking.setTotalprice("100.00");
        
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
        f.setInvpricing("100.00");
        flightList.add(f);
        flights.setFlight(flightList);
        booking.setFlights(flights);

        Passengers passengers = new Passengers();
        List<Passenger> passengerList = new ArrayList<>();
        Passenger p = new Passenger();
        p.setPaxtype("ADT");
        p.setFirstname("John");
        p.setLastname("Doe");
        passengerList.add(p);
        passengers.setPassenger(passengerList);
        booking.setPassengers(passengers);

        ChangeServiceReqDto requestDto = new ChangeServiceReqDto();
        PaymentInformationReqDto payInfo = new PaymentInformationReqDto();
        payInfo.setAmount(new BigDecimal("50.00")); // paymentSrvAmount > 0
        requestDto.setPaymentInformation(payInfo);

        // Details provided is false (so actualSrvPrice initially 0)
        ChangeServiceRspGo7Dto changeServiceRsp = new ChangeServiceRspGo7Dto();

        ChangeServiceRspDto response = ChangeServiceResponse.generateResponse(
                changeServiceRsp, bookingRsp, requestDto, new HashMap<>(), new HashMap<>(), new HashMap<>()
        );
        assertNotNull(response);
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
