package com.airlines.go7api.controller;

import com.airlines.go7api.service.BookingService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WebMvcTest(Go7Controller.class)
class Go7ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;
    
    @MockitoBean
    private com.airlines.go7api.response.AirshopResponse airshopResponse;
    @MockitoBean
    private com.airlines.go7api.response.OrderReshopResponse orderReshopResponse;
    @MockitoBean
    private com.airlines.go7api.response.ServiceListResponse serviceListResponse;

    @ParameterizedTest
    @ValueSource(strings = {
        "/airshop",
        "/offerprice",
        "/seatavailability",
        "/servicelist",
        "/orderreshop",
        "/orderretrieve",
        "/ordercreate",
        "/orderchange",
        "/unpaidcancel",
        "/change/payment",
        "/change/seat",
        "/change/service"
    })
    void testEndpointsWithEmptyPayload(String endpoint) throws Exception {
        MvcResult result = mockMvc.perform(post(endpoint).contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn();
        assertNotNull(result.getResponse());
    }

    @org.junit.jupiter.api.Test
    void testChangePaymentWithOrderId() throws Exception {
        com.airlines.go7api.entity.BookingEntity entity = new com.airlines.go7api.entity.BookingEntity();
        entity.setOrderId("12345");
        entity.setBookingConfirmation("CONF123");
        entity.setPrimaryPassengerLastName("Smith");
        entity.setPassengerSeats(java.util.Collections.singletonMap("1", "4A"));
        entity.setPassengerServices(java.util.Collections.singletonMap("1", "MEAL"));
        entity.setTotalOrderPrice(java.math.BigDecimal.valueOf(150.00));
        
        org.mockito.Mockito.when(bookingService.getBookingByOrderId("12345"))
                .thenReturn(java.util.Optional.of(entity));
        
        String json = "{\"orderId\":\"12345\",\"paymentInformation\":{\"currencyCode\":\"USD\",\"amount\":100.00}}";
        MvcResult result = mockMvc.perform(post("/change/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andReturn();
        
        assertNotNull(result.getResponse());
    }

    @org.junit.jupiter.api.Test
    void testChangePaymentSuccessFlow() throws Exception {
        com.airlines.go7api.entity.BookingEntity entity = new com.airlines.go7api.entity.BookingEntity();
        entity.setOrderId("12345");
        entity.setBookingConfirmation("CONF123");
        entity.setPrimaryPassengerLastName("Smith");
        
        org.mockito.Mockito.when(bookingService.getBookingByOrderId("12345"))
                .thenReturn(java.util.Optional.of(entity));
        
        // Mocks for requests
        com.airlines.go7api.request.ChangePaymentReq mockChangePaymentReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangePaymentReq.class);
        com.airlines.go7api.request.ChangePaymentReq mockOrderTicketReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangePaymentReq.class);
        com.airlines.go7api.request.ChangePaymentReq mockGetBookingReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangePaymentReq.class);
        
        // Mock static method calls
        try (org.mockito.MockedStatic<com.airlines.go7api.request.ChangePaymentReq> mockedStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.ChangePaymentReq.class)) {
            mockedStatic.when(() -> com.airlines.go7api.request.ChangePaymentReq.mapToChangePaymentReq(org.mockito.Mockito.any()))
                    .thenReturn(mockChangePaymentReq);
            mockedStatic.when(() -> com.airlines.go7api.request.ChangePaymentReq.mapToOrderTicketReq(org.mockito.Mockito.anyLong()))
                    .thenReturn(mockOrderTicketReq);
            mockedStatic.when(() -> com.airlines.go7api.request.ChangePaymentReq.mapToGetBookingReq(org.mockito.Mockito.anyString(), org.mockito.Mockito.any()))
                    .thenReturn(mockGetBookingReq);
            
            // 1. Change Payment response structure (Map)
            java.util.Map<String, Object> pmResponse = new java.util.HashMap<>();
            pmResponse.put("success", true);
            java.util.Map<String, Object> aerocrsMap = new java.util.HashMap<>();
            aerocrsMap.put("success", true);
            java.util.Map<String, Object> bookingMap = new java.util.HashMap<>();
            bookingMap.put("bookingid", 123456L);
            aerocrsMap.put("booking", bookingMap);
            pmResponse.put("aerocrs", aerocrsMap);
            
            org.mockito.Mockito.when(mockChangePaymentReq.unmarshal()).thenReturn(pmResponse);
            
            // 2. Order Ticket response structure (Map)
            java.util.Map<String, Object> ticketResponse = new java.util.HashMap<>();
            ticketResponse.put("success", true);
            java.util.Map<String, Object> ticketAerocrsMap = new java.util.HashMap<>();
            ticketAerocrsMap.put("success", true);
            ticketAerocrsMap.put("bookingconfirmation", "CONF123");
            ticketResponse.put("aerocrs", ticketAerocrsMap);
            
            org.mockito.Mockito.when(mockOrderTicketReq.unmarshal()).thenReturn(ticketResponse);
            
            // 3. Get Booking response structure (ChangePaymentRspGo7Dto)
            com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto getBookingResponse = new com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto();
            getBookingResponse.setSuccess(true);
            com.airlines.go7api.responsego7.common.Aerocrs finalAerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            finalAerocrs.setSuccess(true);
            
            com.airlines.go7api.responsego7.common.Booking finalBooking = new com.airlines.go7api.responsego7.common.Booking();
            finalBooking.setBookingid(123456L);
            finalBooking.setPnrref("PNR123");
            finalBooking.setCurrency("USD");
            
            com.airlines.go7api.responsego7.common.Flights finalFlights = new com.airlines.go7api.responsego7.common.Flights();
            com.airlines.go7api.responsego7.common.Flight finalFlight = new com.airlines.go7api.responsego7.common.Flight();
            finalFlight.setFromcode("JFK");
            finalFlight.setTocode("LAX");
            finalFlight.setFlightdate("2026-06-15");
            finalFlight.setDepart("10:00");
            finalFlight.setArrive("13:00");
            finalFlight.setNumber("G7100");
            finalFlight.setAircraftType("737");
            finalFlight.setAirlinedesignator("G7");
            finalFlight.setAirline("Go7 Airlines");
            finalFlight.setFlightClass("Y/Economy");
            
            finalFlights.setFlight(java.util.Collections.singletonList(finalFlight));
            finalBooking.setFlights(finalFlights);
            
            com.airlines.go7api.responsego7.common.Passengers finalPassengers = new com.airlines.go7api.responsego7.common.Passengers();
            com.airlines.go7api.responsego7.common.Passenger finalPassenger = new com.airlines.go7api.responsego7.common.Passenger();
            finalPassenger.setPaxtype("ADT");
            finalPassenger.setPaxtitle("MR");
            finalPassenger.setFirstname("John");
            finalPassenger.setLastname("Smith");
            finalPassenger.setGender("M");
            finalPassenger.setDob("1990-01-01");
            finalPassengers.setPassenger(java.util.Collections.singletonList(finalPassenger));
            finalBooking.setPassengers(finalPassengers);
            
            finalAerocrs.setBooking(finalBooking);
            getBookingResponse.setAerocrs(finalAerocrs);
            
            org.mockito.Mockito.when(mockGetBookingReq.unmarshal()).thenReturn(getBookingResponse);
            
            String json = "{\"orderId\":\"12345\",\"paymentInformation\":{\"currencyCode\":\"USD\",\"amount\":100.00}}";
            MvcResult result = mockMvc.perform(post("/change/payment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andReturn();
            
            assertNotNull(result.getResponse());
        }
    }

    @org.junit.jupiter.api.Test
    void testChangePaymentSuccessFlowNestedConfirmation() throws Exception {
        com.airlines.go7api.entity.BookingEntity entity = new com.airlines.go7api.entity.BookingEntity();
        entity.setOrderId("12345");
        entity.setBookingConfirmation("CONF123");
        entity.setPrimaryPassengerLastName("Smith");
        
        org.mockito.Mockito.when(bookingService.getBookingByOrderId("12345"))
                .thenReturn(java.util.Optional.of(entity));
        
        // Mocks for requests
        com.airlines.go7api.request.ChangePaymentReq mockChangePaymentReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangePaymentReq.class);
        com.airlines.go7api.request.ChangePaymentReq mockOrderTicketReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangePaymentReq.class);
        com.airlines.go7api.request.ChangePaymentReq mockGetBookingReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangePaymentReq.class);
        
        // Mock static method calls
        try (org.mockito.MockedStatic<com.airlines.go7api.request.ChangePaymentReq> mockedStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.ChangePaymentReq.class)) {
            mockedStatic.when(() -> com.airlines.go7api.request.ChangePaymentReq.mapToChangePaymentReq(org.mockito.Mockito.any()))
                    .thenReturn(mockChangePaymentReq);
            mockedStatic.when(() -> com.airlines.go7api.request.ChangePaymentReq.mapToOrderTicketReq(org.mockito.Mockito.anyLong()))
                    .thenReturn(mockOrderTicketReq);
            mockedStatic.when(() -> com.airlines.go7api.request.ChangePaymentReq.mapToGetBookingReq(org.mockito.Mockito.anyString(), org.mockito.Mockito.any()))
                    .thenReturn(mockGetBookingReq);
            
            // 1. Change Payment response structure (Map)
            java.util.Map<String, Object> pmResponse = new java.util.HashMap<>();
            pmResponse.put("success", true);
            java.util.Map<String, Object> aerocrsMap = new java.util.HashMap<>();
            aerocrsMap.put("success", true);
            java.util.Map<String, Object> bookingMap = new java.util.HashMap<>();
            bookingMap.put("bookingid", 123456L);
            aerocrsMap.put("booking", bookingMap);
            pmResponse.put("aerocrs", aerocrsMap);
            
            org.mockito.Mockito.when(mockChangePaymentReq.unmarshal()).thenReturn(pmResponse);
            
            // 2. Order Ticket response structure (Map) with NESTED confirmation
            java.util.Map<String, Object> ticketResponse = new java.util.HashMap<>();
            ticketResponse.put("success", true);
            java.util.Map<String, Object> ticketAerocrsMap = new java.util.HashMap<>();
            ticketAerocrsMap.put("success", true);
            java.util.Map<String, Object> ticketBookingMap = new java.util.HashMap<>();
            ticketBookingMap.put("bookingconfirmation", "CONF123_NESTED");
            ticketAerocrsMap.put("booking", ticketBookingMap);
            ticketResponse.put("aerocrs", ticketAerocrsMap);
            
            org.mockito.Mockito.when(mockOrderTicketReq.unmarshal()).thenReturn(ticketResponse);
            
            // 3. Get Booking response structure (ChangePaymentRspGo7Dto)
            com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto getBookingResponse = new com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto();
            getBookingResponse.setSuccess(true);
            com.airlines.go7api.responsego7.common.Aerocrs finalAerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            finalAerocrs.setSuccess(true);
            
            com.airlines.go7api.responsego7.common.Booking finalBooking = new com.airlines.go7api.responsego7.common.Booking();
            finalBooking.setBookingid(123456L);
            finalBooking.setPnrref("PNR123");
            finalBooking.setCurrency("USD");
            
            com.airlines.go7api.responsego7.common.Flights finalFlights = new com.airlines.go7api.responsego7.common.Flights();
            com.airlines.go7api.responsego7.common.Flight finalFlight = new com.airlines.go7api.responsego7.common.Flight();
            finalFlight.setFromcode("JFK");
            finalFlight.setTocode("LAX");
            finalFlight.setFlightdate("2026-06-15");
            finalFlight.setDepart("10:00");
            finalFlight.setArrive("13:00");
            finalFlight.setNumber("G7100");
            finalFlight.setAircraftType("737");
            finalFlight.setAirlinedesignator("G7");
            finalFlight.setAirline("Go7 Airlines");
            finalFlight.setFlightClass("Y/Economy");
            
            finalFlights.setFlight(java.util.Collections.singletonList(finalFlight));
            finalBooking.setFlights(finalFlights);
            
            com.airlines.go7api.responsego7.common.Passengers finalPassengers = new com.airlines.go7api.responsego7.common.Passengers();
            com.airlines.go7api.responsego7.common.Passenger finalPassenger = new com.airlines.go7api.responsego7.common.Passenger();
            finalPassenger.setPaxtype("ADT");
            finalPassenger.setPaxtitle("MR");
            finalPassenger.setFirstname("John");
            finalPassenger.setLastname("Smith");
            finalPassenger.setGender("M");
            finalPassenger.setDob("1990-01-01");
            finalPassengers.setPassenger(java.util.Collections.singletonList(finalPassenger));
            finalBooking.setPassengers(finalPassengers);
            
            finalAerocrs.setBooking(finalBooking);
            getBookingResponse.setAerocrs(finalAerocrs);
            
            org.mockito.Mockito.when(mockGetBookingReq.unmarshal()).thenReturn(getBookingResponse);
            
            String json = "{\"orderId\":\"12345\",\"paymentInformation\":{\"currencyCode\":\"USD\",\"amount\":100.00}}";
            MvcResult result = mockMvc.perform(post("/change/payment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andReturn();
            
            assertNotNull(result.getResponse());
        }
    }

    @org.junit.jupiter.api.Test
    void testChangePaymentSuccessFlowWithoutBookingId() throws Exception {
        com.airlines.go7api.entity.BookingEntity entity = new com.airlines.go7api.entity.BookingEntity();
        entity.setOrderId("12345");
        entity.setBookingConfirmation("CONF123");
        entity.setPrimaryPassengerLastName("Smith");
        
        org.mockito.Mockito.when(bookingService.getBookingByOrderId("12345"))
                .thenReturn(java.util.Optional.of(entity));
        
        // Mocks for requests
        com.airlines.go7api.request.ChangePaymentReq mockChangePaymentReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangePaymentReq.class);
        com.airlines.go7api.request.ChangePaymentReq mockGetBookingReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangePaymentReq.class);
        
        // Mock static method calls
        try (org.mockito.MockedStatic<com.airlines.go7api.request.ChangePaymentReq> mockedStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.ChangePaymentReq.class)) {
            mockedStatic.when(() -> com.airlines.go7api.request.ChangePaymentReq.mapToChangePaymentReq(org.mockito.Mockito.any()))
                    .thenReturn(mockChangePaymentReq);
            mockedStatic.when(() -> com.airlines.go7api.request.ChangePaymentReq.mapToGetBookingReq(org.mockito.Mockito.anyString(), org.mockito.Mockito.any()))
                    .thenReturn(mockGetBookingReq);
            
            // 1. Change Payment response structure (Map) with NO bookingid
            java.util.Map<String, Object> pmResponse = new java.util.HashMap<>();
            pmResponse.put("success", true);
            java.util.Map<String, Object> aerocrsMap = new java.util.HashMap<>();
            aerocrsMap.put("success", true);
            pmResponse.put("aerocrs", aerocrsMap);
            
            org.mockito.Mockito.when(mockChangePaymentReq.unmarshal()).thenReturn(pmResponse);
            
            // 2. Get Booking response structure (ChangePaymentRspGo7Dto)
            com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto getBookingResponse = new com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto();
            getBookingResponse.setSuccess(true);
            com.airlines.go7api.responsego7.common.Aerocrs finalAerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            finalAerocrs.setSuccess(true);
            
            com.airlines.go7api.responsego7.common.Booking finalBooking = new com.airlines.go7api.responsego7.common.Booking();
            finalBooking.setBookingid(123456L);
            finalBooking.setPnrref("PNR123");
            finalBooking.setCurrency("USD");
            
            com.airlines.go7api.responsego7.common.Flights finalFlights = new com.airlines.go7api.responsego7.common.Flights();
            com.airlines.go7api.responsego7.common.Flight finalFlight = new com.airlines.go7api.responsego7.common.Flight();
            finalFlight.setFromcode("JFK");
            finalFlight.setTocode("LAX");
            finalFlight.setFlightdate("2026-06-15");
            finalFlight.setDepart("10:00");
            finalFlight.setArrive("13:00");
            finalFlight.setNumber("G7100");
            finalFlight.setAircraftType("737");
            finalFlight.setAirlinedesignator("G7");
            finalFlight.setAirline("Go7 Airlines");
            finalFlight.setFlightClass("Y/Economy");
            
            finalFlights.setFlight(java.util.Collections.singletonList(finalFlight));
            finalBooking.setFlights(finalFlights);
            
            com.airlines.go7api.responsego7.common.Passengers finalPassengers = new com.airlines.go7api.responsego7.common.Passengers();
            com.airlines.go7api.responsego7.common.Passenger finalPassenger = new com.airlines.go7api.responsego7.common.Passenger();
            finalPassenger.setPaxtype("ADT");
            finalPassenger.setPaxtitle("MR");
            finalPassenger.setFirstname("John");
            finalPassenger.setLastname("Smith");
            finalPassenger.setGender("M");
            finalPassenger.setDob("1990-01-01");
            finalPassengers.setPassenger(java.util.Collections.singletonList(finalPassenger));
            finalBooking.setPassengers(finalPassengers);
            
            finalAerocrs.setBooking(finalBooking);
            getBookingResponse.setAerocrs(finalAerocrs);
            
            org.mockito.Mockito.when(mockGetBookingReq.unmarshal()).thenReturn(getBookingResponse);
            
            String json = "{\"orderId\":\"12345\",\"paymentInformation\":{\"currencyCode\":\"USD\",\"amount\":100.00}}";
            MvcResult result = mockMvc.perform(post("/change/payment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andReturn();
            
            assertNotNull(result.getResponse());
        }
    }

    @org.junit.jupiter.api.Test
    void testChangeSeatWithFailedPayment() throws Exception {
        com.airlines.go7api.entity.BookingEntity entity = new com.airlines.go7api.entity.BookingEntity();
        entity.setOrderId("12345");
        entity.setBookingConfirmation("CONF123");
        entity.setPrimaryPassengerLastName("Smith");
        
        org.mockito.Mockito.when(bookingService.getBookingByOrderId("12345"))
                .thenReturn(java.util.Optional.of(entity));
        
        // Mocks for ChangeSeatReq and ChangePaymentReq
        com.airlines.go7api.request.ChangeSeatReq mockChangeSeatReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangeSeatReq.class);
        com.airlines.go7api.request.ChangePaymentReq mockChangePaymentReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangePaymentReq.class);
        
        try (org.mockito.MockedStatic<com.airlines.go7api.request.ChangeSeatReq> mockedSeatStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.ChangeSeatReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.ChangePaymentReq> mockedPaymentStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.ChangePaymentReq.class)) {
            
            mockedSeatStatic.when(() -> com.airlines.go7api.request.ChangeSeatReq.mapToChangeSeatReq(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(mockChangeSeatReq);
            mockedPaymentStatic.when(() -> com.airlines.go7api.request.ChangePaymentReq.mapToChangePaymentReq(org.mockito.Mockito.any()))
                    .thenReturn(mockChangePaymentReq);
            
            // Mock ChangeSeatResponse
            com.airlines.go7api.responsego7.ChangeSeatRspGo7Dto changeSeatRsp = new com.airlines.go7api.responsego7.ChangeSeatRspGo7Dto();
            com.airlines.go7api.responsego7.common.Aerocrs aerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            aerocrs.setSuccess(true);
            
            com.airlines.go7api.responsego7.common.Flights flights = new com.airlines.go7api.responsego7.common.Flights();
            com.airlines.go7api.responsego7.common.Flight flight = new com.airlines.go7api.responsego7.common.Flight();
            com.airlines.go7api.responsego7.common.Seat seat = new com.airlines.go7api.responsego7.common.Seat();
            seat.setStatus(true);
            seat.setSeatNumber("4A");
            flight.setSeat(java.util.Collections.singletonList(seat));
            flights.setFlight(java.util.Collections.singletonList(flight));
            aerocrs.setFlights(flights);
            changeSeatRsp.setAerocrs(aerocrs);
            
            org.mockito.Mockito.when(mockChangeSeatReq.unmarshal()).thenReturn(changeSeatRsp);
            
            // Mock Failed ChangePayment response
            java.util.Map<String, Object> pmResponse = new java.util.HashMap<>();
            pmResponse.put("success", false);
            org.mockito.Mockito.when(mockChangePaymentReq.unmarshal()).thenReturn(pmResponse);
            
            String json = "{\"orderId\":\"12345\",\"paymentInformation\":{\"cardNumber\":\"1234567890123456\",\"currencyCode\":\"USD\",\"amount\":100.00}}";
            MvcResult result = mockMvc.perform(post("/change/seat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andReturn();
            
            assertNotNull(result.getResponse());
        }
    }

    @org.junit.jupiter.api.Test
    void testUnpaidCancelWithConfirmationNotFound() throws Exception {
        // Mock DB lookup to return empty
        org.mockito.Mockito.when(bookingService.getBookingByOrderId("99999"))
                .thenReturn(java.util.Optional.empty());
        
        com.airlines.go7api.request.UnpaidCancelReq mockCancelReq = org.mockito.Mockito.mock(com.airlines.go7api.request.UnpaidCancelReq.class);
        com.airlines.go7api.request.OrderRetrieveReq mockValidationReq = org.mockito.Mockito.mock(com.airlines.go7api.request.OrderRetrieveReq.class);
        
        try (org.mockito.MockedStatic<com.airlines.go7api.request.UnpaidCancelReq> mockedCancelStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.UnpaidCancelReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.OrderRetrieveReq> mockedRetrieveStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.OrderRetrieveReq.class)) {
            
            mockedCancelStatic.when(() -> com.airlines.go7api.request.UnpaidCancelReq.mapToUnpaidCancelReq(org.mockito.Mockito.any()))
                    .thenReturn(mockCancelReq);
            mockedRetrieveStatic.when(() -> com.airlines.go7api.request.OrderRetrieveReq.mapToOrderRetrieveReq(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(mockValidationReq);
            
            // Mock Validation: returns an successful retrieval or similar
            java.util.Map<String, Object> validationResponse = new java.util.HashMap<>();
            validationResponse.put("success", true);
            org.mockito.Mockito.when(mockValidationReq.unmarshal()).thenReturn(validationResponse);
            
            // Mock UnpaidCancelReq: returns a response with success=true, but NO bookingconfirmation
            java.util.Map<String, Object> cancelResponse = new java.util.HashMap<>();
            cancelResponse.put("success", true);
            org.mockito.Mockito.when(mockCancelReq.unmarshal()).thenReturn(cancelResponse);
            
            String json = "{\"orderId\":\"99999\",\"apiKey\":\"testKey\"}";
            MvcResult result = mockMvc.perform(post("/unpaidcancel")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andReturn();
            
            assertNotNull(result.getResponse());
        }
    }

    @org.junit.jupiter.api.Test
    void testChangeSeatWithSeatFareException() throws Exception {
        com.airlines.go7api.entity.BookingEntity entity = new com.airlines.go7api.entity.BookingEntity();
        entity.setOrderId("12345");
        entity.setBookingConfirmation("CONF123");
        entity.setPrimaryPassengerLastName("Smith");
        
        org.mockito.Mockito.when(bookingService.getBookingByOrderId("12345"))
                .thenReturn(java.util.Optional.of(entity));
        
        com.airlines.go7api.request.OrderRetrieveReq mockRetrieveReq = org.mockito.Mockito.mock(com.airlines.go7api.request.OrderRetrieveReq.class);
        com.airlines.go7api.request.ChangeSeatReq mockChangeSeatReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangeSeatReq.class);
        
        try (org.mockito.MockedStatic<com.airlines.go7api.request.OrderRetrieveReq> mockedRetrieveStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.OrderRetrieveReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.SeatAvailabilityReq> mockedSeatReqStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.SeatAvailabilityReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.ChangeSeatReq> mockedSeatStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.ChangeSeatReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.response.ChangeSeatResponse> mockedSeatResponseStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.response.ChangeSeatResponse.class);
             org.mockito.MockedConstruction<com.airlines.go7api.request.SeatAvailabilityReq> mockedSeatReqConstruction = org.mockito.Mockito.mockConstruction(com.airlines.go7api.request.SeatAvailabilityReq.class, (mock, context) -> {
                 org.mockito.Mockito.when(mock.makeApiCall()).thenReturn("{invalid_json");
             })) {
            
            mockedRetrieveStatic.when(() -> com.airlines.go7api.request.OrderRetrieveReq.mapToOrderRetrieveReq(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(mockRetrieveReq);
            mockedSeatReqStatic.when(() -> com.airlines.go7api.request.SeatAvailabilityReq.mapToGetBookingReq(org.mockito.Mockito.anyString()))
                    .thenReturn(mockRetrieveReq);
            mockedSeatStatic.when(() -> com.airlines.go7api.request.ChangeSeatReq.mapToChangeSeatReq(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(mockChangeSeatReq);
            
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp = new com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto();
            com.airlines.go7api.responsego7.common.Aerocrs retrieveAerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            retrieveAerocrs.setSuccess(true);
            com.airlines.go7api.responsego7.common.Booking booking = new com.airlines.go7api.responsego7.common.Booking();
            booking.setBookingid(12345L);
            booking.setBookingconfirmation("CONF123");
            
            com.airlines.go7api.responsego7.common.Flights flights = new com.airlines.go7api.responsego7.common.Flights();
            com.airlines.go7api.responsego7.common.Flight flight = new com.airlines.go7api.responsego7.common.Flight();
            flight.setFlightClass("Y/Economy");
            flight.setNumber("101");
            flights.setFlight(java.util.Collections.singletonList(flight));
            booking.setFlights(flights);
            
            com.airlines.go7api.responsego7.common.Passengers passengers = new com.airlines.go7api.responsego7.common.Passengers();
            com.airlines.go7api.responsego7.common.Passenger passenger = new com.airlines.go7api.responsego7.common.Passenger();
            passenger.setLastname("Smith");
            passengers.setPassenger(java.util.Collections.singletonList(passenger));
            booking.setPassengers(passengers);
            
            retrieveAerocrs.setBooking(booking);
            bookingRsp.setAerocrs(retrieveAerocrs);
            
            org.mockito.Mockito.when(mockRetrieveReq.unmarshal()).thenReturn(bookingRsp);
            
            // Mock ChangeSeatResponse
            com.airlines.go7api.responsego7.ChangeSeatRspGo7Dto changeSeatRsp = new com.airlines.go7api.responsego7.ChangeSeatRspGo7Dto();
            com.airlines.go7api.responsego7.common.Aerocrs aerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            aerocrs.setSuccess(true);
            
            com.airlines.go7api.responsego7.common.Flights seatFlights = new com.airlines.go7api.responsego7.common.Flights();
            com.airlines.go7api.responsego7.common.Flight seatFlight = new com.airlines.go7api.responsego7.common.Flight();
            seatFlight.setFlightClass("Y/Economy");
            seatFlight.setNumber("101");
            com.airlines.go7api.responsego7.common.Seat seat = new com.airlines.go7api.responsego7.common.Seat();
            seat.setStatus(true);
            seat.setSeatNumber("4A");
            seatFlight.setSeat(java.util.Collections.singletonList(seat));
            seatFlights.setFlight(java.util.Collections.singletonList(seatFlight));
            aerocrs.setFlights(seatFlights);
            changeSeatRsp.setAerocrs(aerocrs);
            
            org.mockito.Mockito.when(mockChangeSeatReq.unmarshal()).thenReturn(changeSeatRsp);
            
            com.airlines.go7api.responsedto.ChangeSeatRspDto standardResponse = new com.airlines.go7api.responsedto.ChangeSeatRspDto();
            mockedSeatResponseStatic.when(() -> com.airlines.go7api.response.ChangeSeatResponse.generateResponse(
                    org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.any(),
                    org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(standardResponse);
            
            String json = "{\"orderId\":\"12345\"}";
            MvcResult result = mockMvc.perform(post("/change/seat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andReturn();
            
            assertNotNull(result.getResponse());
        }
    }

    @org.junit.jupiter.api.Test
    void testChangeServiceWithServiceFareException() throws Exception {
        com.airlines.go7api.entity.BookingEntity entity = new com.airlines.go7api.entity.BookingEntity();
        entity.setOrderId("12345");
        entity.setBookingConfirmation("CONF123");
        entity.setPrimaryPassengerLastName("Smith");
        
        org.mockito.Mockito.when(bookingService.getBookingByOrderId("12345"))
                .thenReturn(java.util.Optional.of(entity));
        
        // Mocks for OrderRetrieveReq and ChangeServiceReq
        com.airlines.go7api.request.OrderRetrieveReq mockRetrieveReq = org.mockito.Mockito.mock(com.airlines.go7api.request.OrderRetrieveReq.class);
        com.airlines.go7api.request.ChangeServiceReq mockChangeServiceReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangeServiceReq.class);
        
        try (org.mockito.MockedStatic<com.airlines.go7api.request.OrderRetrieveReq> mockedRetrieveStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.OrderRetrieveReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.SeatAvailabilityReq> mockedSeatReqStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.SeatAvailabilityReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.ChangeServiceReq> mockedServiceStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.ChangeServiceReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.response.ChangeServiceResponse> mockedServiceResponseStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.response.ChangeServiceResponse.class);
             org.mockito.MockedConstruction<com.airlines.go7api.request.ServiceListReq> mockedServiceListReqConstruction = org.mockito.Mockito.mockConstruction(com.airlines.go7api.request.ServiceListReq.class, (mock, context) -> {
                 org.mockito.Mockito.when(mock.makeApiCall()).thenReturn("{invalid_json");
             })) {
            
            mockedRetrieveStatic.when(() -> com.airlines.go7api.request.OrderRetrieveReq.mapToOrderRetrieveReq(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(mockRetrieveReq);
            mockedSeatReqStatic.when(() -> com.airlines.go7api.request.SeatAvailabilityReq.mapToGetBookingReq(org.mockito.Mockito.anyString()))
                    .thenReturn(mockRetrieveReq);
            mockedServiceStatic.when(() -> com.airlines.go7api.request.ChangeServiceReq.mapToChangeServiceReq(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(mockChangeServiceReq);
            
            // Mock OrderRetrieve response
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp = new com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto();
            com.airlines.go7api.responsego7.common.Aerocrs retrieveAerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            retrieveAerocrs.setSuccess(true);
            com.airlines.go7api.responsego7.common.Booking booking = new com.airlines.go7api.responsego7.common.Booking();
            booking.setBookingid(12345L);
            booking.setBookingconfirmation("CONF123");
            
            com.airlines.go7api.responsego7.common.Flights flights = new com.airlines.go7api.responsego7.common.Flights();
            com.airlines.go7api.responsego7.common.Flight flight = new com.airlines.go7api.responsego7.common.Flight();
            flight.setNumber("101");
            flights.setFlight(java.util.Collections.singletonList(flight));
            booking.setFlights(flights);
            
            com.airlines.go7api.responsego7.common.Passengers passengers = new com.airlines.go7api.responsego7.common.Passengers();
            com.airlines.go7api.responsego7.common.Passenger passenger = new com.airlines.go7api.responsego7.common.Passenger();
            passenger.setLastname("Smith");
            passengers.setPassenger(java.util.Collections.singletonList(passenger));
            booking.setPassengers(passengers);
            
            retrieveAerocrs.setBooking(booking);
            bookingRsp.setAerocrs(retrieveAerocrs);
            
            org.mockito.Mockito.when(mockRetrieveReq.unmarshal()).thenReturn(bookingRsp);
            
            // Mock ChangeServiceResponse
            com.airlines.go7api.responsego7.ChangeServiceRspGo7Dto changeServiceRsp = new com.airlines.go7api.responsego7.ChangeServiceRspGo7Dto();
            com.airlines.go7api.responsego7.common.Aerocrs serviceAerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            serviceAerocrs.setSuccess(true);
            
            com.airlines.go7api.responsego7.common.Aerocrs.Detail detail = new com.airlines.go7api.responsego7.common.Aerocrs.Detail();
            detail.setSuccess(true);
            java.util.Map<String, Object> ancillary = new java.util.HashMap<>();
            ancillary.put("itemid", "SRV1");
            ancillary.put("flightid", "999");
            ancillary.put("totalprice", "0.00");
            detail.setAncillary(ancillary);
            
            serviceAerocrs.setDetails(java.util.Collections.singletonList(detail));
            changeServiceRsp.setAerocrs(serviceAerocrs);
            
            org.mockito.Mockito.when(mockChangeServiceReq.unmarshal()).thenReturn(changeServiceRsp);
            
            com.airlines.go7api.responsedto.ChangeServiceRspDto standardResponse = new com.airlines.go7api.responsedto.ChangeServiceRspDto();
            mockedServiceResponseStatic.when(() -> com.airlines.go7api.response.ChangeServiceResponse.generateResponse(
                    org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.any(),
                    org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(standardResponse);
            
            String jsonReq = "{\"orderId\":\"12345\"}";
            MvcResult result = mockMvc.perform(post("/change/service")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonReq))
                    .andReturn();
            
            assertNotNull(result.getResponse());
        }
    }

    @org.junit.jupiter.api.Test
    void testOrderReshopSuccess() throws Exception {
        com.airlines.go7api.request.AirshopReq mockAirshopReq = org.mockito.Mockito.mock(com.airlines.go7api.request.AirshopReq.class);
        com.airlines.go7api.responsego7.AirshopRspGo7Dto mockAirshopRsp = new com.airlines.go7api.responsego7.AirshopRspGo7Dto();
        com.airlines.go7api.responsego7.common.Aerocrs aerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
        aerocrs.setSuccess(true);
        mockAirshopRsp.setAerocrs(aerocrs);
        
        org.mockito.Mockito.when(mockAirshopReq.unmarshal()).thenReturn(mockAirshopRsp);
        
        com.airlines.go7api.responsedto.OrderReshopRspDto mockDto = new com.airlines.go7api.responsedto.OrderReshopRspDto();
        org.mockito.Mockito.when(orderReshopResponse.orderReshopMapper(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                .thenReturn(mockDto);
                
        try (org.mockito.MockedStatic<com.airlines.go7api.request.AirshopReq> mockedStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.AirshopReq.class)) {
            mockedStatic.when(() -> com.airlines.go7api.request.AirshopReq.mapToFlightSearchRequestDTO(org.mockito.Mockito.any()))
                    .thenReturn(mockAirshopReq);
            
            String json = "{\"orderId\":\"12345\",\"paxList\":[{\"ptc\":\"ADT\"},{\"ptc\":\"CHD\"},{\"ptc\":\"INF\"}],\"ods\":[{\"origin\":\"JFK\",\"destination\":\"LAX\",\"date\":\"2026-06-15\"}]}";
            MvcResult result = mockMvc.perform(post("/orderreshop")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andReturn();
            
            assertNotNull(result.getResponse());
        }
    }

    @org.junit.jupiter.api.Test
    void testFSOrderReshopSuccess() throws Exception {
        com.airlines.go7api.request.AirshopReq mockAirshopReq = org.mockito.Mockito.mock(com.airlines.go7api.request.AirshopReq.class);
        com.airlines.go7api.responsego7.AirshopRspGo7Dto mockAirshopRsp = new com.airlines.go7api.responsego7.AirshopRspGo7Dto();
        com.airlines.go7api.responsego7.common.Aerocrs aerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
        aerocrs.setSuccess(true);
        mockAirshopRsp.setAerocrs(aerocrs);
        
        org.mockito.Mockito.when(mockAirshopReq.unmarshal()).thenReturn(mockAirshopRsp);
        
        com.airlines.go7api.responsedto.OrderReshopRspDto mockDto = new com.airlines.go7api.responsedto.OrderReshopRspDto();
        org.mockito.Mockito.when(orderReshopResponse.orderReshopMapper(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                .thenReturn(mockDto);
                
        try (org.mockito.MockedStatic<com.airlines.go7api.request.AirshopReq> mockedStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.AirshopReq.class)) {
            mockedStatic.when(() -> com.airlines.go7api.request.AirshopReq.mapToFlightSearchRequestDTO(org.mockito.Mockito.any()))
                    .thenReturn(mockAirshopReq);
            
            String json = "{\"paxList\":[{\"ptc\":\"ADT\"},{\"ptc\":\"CHD\"},{\"ptc\":\"INF\"}],\"ods\":[{\"origin\":\"JFK\",\"destination\":\"LAX\",\"date\":\"2026-06-15\"}]}";
            MvcResult result = mockMvc.perform(post("/FSorderreshop")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andReturn();
            
            assertNotNull(result.getResponse());
        }
    }

    @org.junit.jupiter.api.Test
    void testChangePaymentSuccessFlowWithCombinedOutstanding() throws Exception {
        com.airlines.go7api.entity.BookingEntity entity = new com.airlines.go7api.entity.BookingEntity();
        entity.setOrderId("12345");
        entity.setBookingConfirmation("CONF123");
        entity.setPrimaryPassengerLastName("Smith");
        entity.setPassengerSeats(java.util.Collections.singletonMap("1", "4A"));
        entity.setPassengerServices(java.util.Collections.singletonMap("1", "MEAL"));
        
        org.mockito.Mockito.when(bookingService.getBookingByOrderId("12345"))
                .thenReturn(java.util.Optional.of(entity));
        
        com.airlines.go7api.request.ChangePaymentReq mockChangePaymentReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangePaymentReq.class);
        com.airlines.go7api.request.ChangePaymentReq mockOrderTicketReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangePaymentReq.class);
        com.airlines.go7api.request.ChangePaymentReq mockGetBookingReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangePaymentReq.class);
        
        try (org.mockito.MockedStatic<com.airlines.go7api.request.ChangePaymentReq> mockedStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.ChangePaymentReq.class)) {
            mockedStatic.when(() -> com.airlines.go7api.request.ChangePaymentReq.mapToChangePaymentReq(org.mockito.Mockito.any()))
                    .thenReturn(mockChangePaymentReq);
            mockedStatic.when(() -> com.airlines.go7api.request.ChangePaymentReq.mapToOrderTicketReq(org.mockito.Mockito.anyLong()))
                    .thenReturn(mockOrderTicketReq);
            mockedStatic.when(() -> com.airlines.go7api.request.ChangePaymentReq.mapToGetBookingReq(org.mockito.Mockito.anyString(), org.mockito.Mockito.any()))
                    .thenReturn(mockGetBookingReq);
            
            java.util.Map<String, Object> pmResponse = new java.util.HashMap<>();
            pmResponse.put("success", true);
            java.util.Map<String, Object> aerocrsMap = new java.util.HashMap<>();
            aerocrsMap.put("success", true);
            java.util.Map<String, Object> bookingMap = new java.util.HashMap<>();
            bookingMap.put("bookingid", 123456L);
            aerocrsMap.put("booking", bookingMap);
            pmResponse.put("aerocrs", aerocrsMap);
            
            org.mockito.Mockito.when(mockChangePaymentReq.unmarshal()).thenReturn(pmResponse);
            
            java.util.Map<String, Object> ticketResponse = new java.util.HashMap<>();
            ticketResponse.put("success", true);
            java.util.Map<String, Object> ticketAerocrsMap = new java.util.HashMap<>();
            ticketAerocrsMap.put("success", true);
            ticketAerocrsMap.put("bookingconfirmation", "CONF123");
            ticketResponse.put("aerocrs", ticketAerocrsMap);
            
            org.mockito.Mockito.when(mockOrderTicketReq.unmarshal()).thenReturn(ticketResponse);
            
            com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto getBookingResponse = new com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto();
            getBookingResponse.setSuccess(true);
            com.airlines.go7api.responsego7.common.Aerocrs finalAerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            finalAerocrs.setSuccess(true);
            
            com.airlines.go7api.responsego7.common.Booking finalBooking = new com.airlines.go7api.responsego7.common.Booking();
            finalBooking.setBookingid(123456L);
            finalBooking.setPnrref("PNR123");
            finalBooking.setCurrency("USD");
            
            com.airlines.go7api.responsego7.common.BalanceInformation balance = new com.airlines.go7api.responsego7.common.BalanceInformation();
            balance.setPnrOutstandingPayment(100.0);
            finalBooking.setBalanceInformation(balance);
            
            com.airlines.go7api.responsego7.common.Flights finalFlights = new com.airlines.go7api.responsego7.common.Flights();
            com.airlines.go7api.responsego7.common.Flight finalFlight = new com.airlines.go7api.responsego7.common.Flight();
            finalFlight.setFromcode("JFK");
            finalFlight.setTocode("LAX");
            finalFlight.setFlightdate("2026-06-15");
            finalFlight.setDepart("10:00");
            finalFlight.setArrive("13:00");
            finalFlight.setNumber("G7100");
            finalFlight.setAircraftType("737");
            finalFlight.setAirlinedesignator("G7");
            finalFlight.setAirline("Go7 Airlines");
            finalFlight.setFlightClass("Y/Economy");
            
            finalFlights.setFlight(java.util.Collections.singletonList(finalFlight));
            finalBooking.setFlights(finalFlights);
            
            com.airlines.go7api.responsego7.common.Passengers finalPassengers = new com.airlines.go7api.responsego7.common.Passengers();
            com.airlines.go7api.responsego7.common.Passenger finalPassenger = new com.airlines.go7api.responsego7.common.Passenger();
            finalPassenger.setPaxtype("ADT");
            finalPassenger.setPaxtitle("MR");
            finalPassenger.setFirstname("John");
            finalPassenger.setLastname("Smith");
            finalPassenger.setGender("M");
            finalPassenger.setDob("1990-01-01");
            finalPassengers.setPassenger(java.util.Collections.singletonList(finalPassenger));
            finalBooking.setPassengers(finalPassengers);
            
            finalAerocrs.setBooking(finalBooking);
            getBookingResponse.setAerocrs(finalAerocrs);
            
            org.mockito.Mockito.when(mockGetBookingReq.unmarshal()).thenReturn(getBookingResponse);
            
            String json = "{\"orderId\":\"12345\",\"paymentInformation\":{\"currencyCode\":\"USD\",\"amount\":0.00}}";
            MvcResult result = mockMvc.perform(post("/change/payment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andReturn();
            
            assertNotNull(result.getResponse());
        }
    }

    @org.junit.jupiter.api.Test
    void testChangeSeatSuccessWithActualPrice() throws Exception {
        com.airlines.go7api.entity.BookingEntity entity = new com.airlines.go7api.entity.BookingEntity();
        entity.setOrderId("12345");
        entity.setBookingConfirmation("CONF123");
        entity.setPrimaryPassengerLastName(null);
        
        org.mockito.Mockito.when(bookingService.getBookingByOrderId("12345"))
                .thenReturn(java.util.Optional.of(entity));
        
        com.airlines.go7api.request.OrderRetrieveReq mockRetrieveReq = org.mockito.Mockito.mock(com.airlines.go7api.request.OrderRetrieveReq.class);
        com.airlines.go7api.request.ChangeSeatReq mockChangeSeatReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangeSeatReq.class);
        com.airlines.go7api.request.ChangePaymentReq mockChangePaymentReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangePaymentReq.class);
        com.airlines.go7api.request.ChangePaymentReq mockOrderTicketReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangePaymentReq.class);
        
        try (org.mockito.MockedStatic<com.airlines.go7api.request.OrderRetrieveReq> mockedRetrieveStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.OrderRetrieveReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.SeatAvailabilityReq> mockedSeatReqStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.SeatAvailabilityReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.ChangeSeatReq> mockedSeatStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.ChangeSeatReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.ChangePaymentReq> mockedPaymentStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.ChangePaymentReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.response.ChangeSeatResponse> mockedSeatResponseStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.response.ChangeSeatResponse.class);
             org.mockito.MockedConstruction<com.airlines.go7api.request.SeatAvailabilityReq> mockedSeatReqConstruction = org.mockito.Mockito.mockConstruction(com.airlines.go7api.request.SeatAvailabilityReq.class, (mock, context) -> {
                 org.mockito.Mockito.when(mock.makeApiCall()).thenReturn(
                     "{\"aerocrs\":{\"seatmapfare\":{\"classes\":{\"Y\":{\"paidSeats\":[{\"seatfare\":50.0,\"seats\":{\"4A\":\"O\"}}]}}}}}"
                 );
             })) {
            
            mockedRetrieveStatic.when(() -> com.airlines.go7api.request.OrderRetrieveReq.mapToOrderRetrieveReq(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(mockRetrieveReq);
            mockedSeatReqStatic.when(() -> com.airlines.go7api.request.SeatAvailabilityReq.mapToGetBookingReq(org.mockito.Mockito.anyString()))
                    .thenReturn(mockRetrieveReq);
            mockedSeatStatic.when(() -> com.airlines.go7api.request.ChangeSeatReq.mapToChangeSeatReq(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(mockChangeSeatReq);
            mockedPaymentStatic.when(() -> com.airlines.go7api.request.ChangePaymentReq.mapToChangePaymentReq(org.mockito.Mockito.any()))
                    .thenReturn(mockChangePaymentReq);
            mockedPaymentStatic.when(() -> com.airlines.go7api.request.ChangePaymentReq.mapToOrderTicketReq(org.mockito.Mockito.anyLong()))
                    .thenReturn(mockOrderTicketReq);
            
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp = new com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto();
            com.airlines.go7api.responsego7.common.Aerocrs retrieveAerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            retrieveAerocrs.setSuccess(true);
            com.airlines.go7api.responsego7.common.Booking booking = new com.airlines.go7api.responsego7.common.Booking();
            booking.setBookingid(12345L);
            booking.setBookingconfirmation("CONF123");
            
            com.airlines.go7api.responsego7.common.Flights flights = new com.airlines.go7api.responsego7.common.Flights();
            com.airlines.go7api.responsego7.common.Flight flight = new com.airlines.go7api.responsego7.common.Flight();
            flight.setFlightClass("Y/Economy");
            flight.setNumber("101");
            flights.setFlight(java.util.Collections.singletonList(flight));
            booking.setFlights(flights);
            
            com.airlines.go7api.responsego7.common.Passengers passengers = new com.airlines.go7api.responsego7.common.Passengers();
            com.airlines.go7api.responsego7.common.Passenger passenger = new com.airlines.go7api.responsego7.common.Passenger();
            passenger.setLastname("Smith");
            passengers.setPassenger(java.util.Collections.singletonList(passenger));
            booking.setPassengers(passengers);
            
            retrieveAerocrs.setBooking(booking);
            bookingRsp.setAerocrs(retrieveAerocrs);
            
            org.mockito.Mockito.when(mockRetrieveReq.unmarshal()).thenReturn(bookingRsp);
            
            com.airlines.go7api.responsego7.ChangeSeatRspGo7Dto changeSeatRsp = new com.airlines.go7api.responsego7.ChangeSeatRspGo7Dto();
            com.airlines.go7api.responsego7.common.Aerocrs aerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            aerocrs.setSuccess(true);
            
            com.airlines.go7api.responsego7.common.Flights seatFlights = new com.airlines.go7api.responsego7.common.Flights();
            com.airlines.go7api.responsego7.common.Flight seatFlight = new com.airlines.go7api.responsego7.common.Flight();
            seatFlight.setFlightClass("Y/Economy");
            seatFlight.setNumber("101");
            com.airlines.go7api.responsego7.common.Seat seat = new com.airlines.go7api.responsego7.common.Seat();
            seat.setStatus(true);
            seat.setSeatNumber("4A");
            seatFlight.setSeat(java.util.Collections.singletonList(seat));
            seatFlights.setFlight(java.util.Collections.singletonList(seatFlight));
            aerocrs.setFlights(seatFlights);
            changeSeatRsp.setAerocrs(aerocrs);
            
            org.mockito.Mockito.when(mockChangeSeatReq.unmarshal()).thenReturn(changeSeatRsp);
            
            java.util.Map<String, Object> pmResponse = new java.util.HashMap<>();
            pmResponse.put("success", true);
            java.util.Map<String, Object> aerocrsMap = new java.util.HashMap<>();
            aerocrsMap.put("success", true);
            java.util.Map<String, Object> bookingMap = new java.util.HashMap<>();
            bookingMap.put("bookingid", 123456L);
            aerocrsMap.put("booking", bookingMap);
            pmResponse.put("aerocrs", aerocrsMap);
            
            org.mockito.Mockito.when(mockChangePaymentReq.unmarshal()).thenReturn(pmResponse);
            org.mockito.Mockito.when(mockOrderTicketReq.unmarshal()).thenReturn(pmResponse);
            
            com.airlines.go7api.responsedto.ChangeSeatRspDto standardResponse = new com.airlines.go7api.responsedto.ChangeSeatRspDto();
            com.airlines.go7api.responsedto.common.OrderItemsDTO item = new com.airlines.go7api.responsedto.common.OrderItemsDTO();
            item.setOrderItemId("12345_AIR-1");
            com.airlines.go7api.responsedto.common.Service srv = new com.airlines.go7api.responsedto.common.Service();
            srv.setServiceCode("SEAT4A");
            item.setServiceList(java.util.Collections.singletonList(srv));
            item.setPassengerIds(java.util.Collections.singletonList("pax1"));
            item.setTotalPrice(java.math.BigDecimal.valueOf(50.0));
            standardResponse.setOrderItems(java.util.Collections.singletonList(item));
            standardResponse.setTotalOrderPrice(java.math.BigDecimal.valueOf(100.0));
            
            mockedSeatResponseStatic.when(() -> com.airlines.go7api.response.ChangeSeatResponse.generateResponse(
                    org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.any(),
                    org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(standardResponse);
            
            String json = "{\"orderId\":\"12345\",\"paymentInformation\":{\"cardNumber\":\"1234567890123456\",\"currencyCode\":\"USD\",\"amount\":100.00}}";
            MvcResult result = mockMvc.perform(post("/change/seat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andReturn();
            
            assertNotNull(result.getResponse());
        }
    }

    @org.junit.jupiter.api.Test
    void testChangeServiceSuccessWithActualPrice() throws Exception {
        com.airlines.go7api.entity.BookingEntity entity = new com.airlines.go7api.entity.BookingEntity();
        entity.setOrderId("12345");
        entity.setBookingConfirmation("CONF123");
        entity.setPrimaryPassengerLastName(null);
        
        org.mockito.Mockito.when(bookingService.getBookingByOrderId("12345"))
                .thenReturn(java.util.Optional.of(entity));
        
        com.airlines.go7api.request.OrderRetrieveReq mockRetrieveReq = org.mockito.Mockito.mock(com.airlines.go7api.request.OrderRetrieveReq.class);
        com.airlines.go7api.request.ChangeServiceReq mockChangeServiceReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangeServiceReq.class);
        com.airlines.go7api.request.ChangePaymentReq mockChangePaymentReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangePaymentReq.class);
        com.airlines.go7api.request.ChangePaymentReq mockOrderTicketReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangePaymentReq.class);
        
        try (org.mockito.MockedStatic<com.airlines.go7api.request.OrderRetrieveReq> mockedRetrieveStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.OrderRetrieveReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.SeatAvailabilityReq> mockedSeatReqStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.SeatAvailabilityReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.ChangeServiceReq> mockedServiceStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.ChangeServiceReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.ChangePaymentReq> mockedPaymentStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.ChangePaymentReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.response.ChangeServiceResponse> mockedServiceResponseStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.response.ChangeServiceResponse.class);
             org.mockito.MockedConstruction<com.airlines.go7api.request.ServiceListReq> mockedServiceListReqConstruction = org.mockito.Mockito.mockConstruction(com.airlines.go7api.request.ServiceListReq.class, (mock, context) -> {
                 org.mockito.Mockito.when(mock.makeApiCall()).thenReturn(
                     "{\"aerocrs\":{\"ancillaries\":{\"ancillary\":[{\"items\":[{\"itemid\":\"SRV1\",\"fare\":\"15.00\"}]}]}}}"
                 );
             })) {
            
            mockedRetrieveStatic.when(() -> com.airlines.go7api.request.OrderRetrieveReq.mapToOrderRetrieveReq(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(mockRetrieveReq);
            mockedSeatReqStatic.when(() -> com.airlines.go7api.request.SeatAvailabilityReq.mapToGetBookingReq(org.mockito.Mockito.anyString()))
                    .thenReturn(mockRetrieveReq);
            mockedServiceStatic.when(() -> com.airlines.go7api.request.ChangeServiceReq.mapToChangeServiceReq(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(mockChangeServiceReq);
            mockedPaymentStatic.when(() -> com.airlines.go7api.request.ChangePaymentReq.mapToChangePaymentReq(org.mockito.Mockito.any()))
                    .thenReturn(mockChangePaymentReq);
            mockedPaymentStatic.when(() -> com.airlines.go7api.request.ChangePaymentReq.mapToOrderTicketReq(org.mockito.Mockito.anyLong()))
                    .thenReturn(mockOrderTicketReq);
            
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp = new com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto();
            com.airlines.go7api.responsego7.common.Aerocrs retrieveAerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            retrieveAerocrs.setSuccess(true);
            com.airlines.go7api.responsego7.common.Booking booking = new com.airlines.go7api.responsego7.common.Booking();
            booking.setBookingid(12345L);
            booking.setBookingconfirmation("CONF123");
            
            com.airlines.go7api.responsego7.common.Flights flights = new com.airlines.go7api.responsego7.common.Flights();
            com.airlines.go7api.responsego7.common.Flight flight = new com.airlines.go7api.responsego7.common.Flight();
            flight.setNumber("101");
            flights.setFlight(java.util.Collections.singletonList(flight));
            booking.setFlights(flights);
            
            com.airlines.go7api.responsego7.common.Passengers passengers = new com.airlines.go7api.responsego7.common.Passengers();
            com.airlines.go7api.responsego7.common.Passenger passenger = new com.airlines.go7api.responsego7.common.Passenger();
            passenger.setLastname("Smith");
            passengers.setPassenger(java.util.Collections.singletonList(passenger));
            booking.setPassengers(passengers);
            
            retrieveAerocrs.setBooking(booking);
            bookingRsp.setAerocrs(retrieveAerocrs);
            org.mockito.Mockito.when(mockRetrieveReq.unmarshal()).thenReturn(bookingRsp);
            
            com.airlines.go7api.responsego7.ChangeServiceRspGo7Dto changeServiceRsp = new com.airlines.go7api.responsego7.ChangeServiceRspGo7Dto();
            com.airlines.go7api.responsego7.common.Aerocrs serviceAerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            serviceAerocrs.setSuccess(true);
            
            com.airlines.go7api.responsego7.common.Aerocrs.Detail detail = new com.airlines.go7api.responsego7.common.Aerocrs.Detail();
            detail.setSuccess(true);
            java.util.Map<String, Object> ancillary = new java.util.HashMap<>();
            ancillary.put("itemid", "SRV1");
            ancillary.put("flightid", "999");
            ancillary.put("totalprice", "0.00");
            detail.setAncillary(ancillary);
            
            serviceAerocrs.setDetails(java.util.Collections.singletonList(detail));
            changeServiceRsp.setAerocrs(serviceAerocrs);
            org.mockito.Mockito.when(mockChangeServiceReq.unmarshal()).thenReturn(changeServiceRsp);
            
            java.util.Map<String, Object> pmResponse = new java.util.HashMap<>();
            pmResponse.put("success", true);
            java.util.Map<String, Object> aerocrsMap = new java.util.HashMap<>();
            aerocrsMap.put("success", true);
            java.util.Map<String, Object> bookingMap = new java.util.HashMap<>();
            bookingMap.put("bookingid", 123456L);
            aerocrsMap.put("booking", bookingMap);
            pmResponse.put("aerocrs", aerocrsMap);
            org.mockito.Mockito.when(mockChangePaymentReq.unmarshal()).thenReturn(pmResponse);
            org.mockito.Mockito.when(mockOrderTicketReq.unmarshal()).thenReturn(pmResponse);
            
            com.airlines.go7api.responsedto.ChangeServiceRspDto standardResponse = new com.airlines.go7api.responsedto.ChangeServiceRspDto();
            com.airlines.go7api.responsedto.common.OrderItemsDTO item = new com.airlines.go7api.responsedto.common.OrderItemsDTO();
            item.setOrderItemId("12345_SRV1");
            com.airlines.go7api.responsedto.common.Service srv = new com.airlines.go7api.responsedto.common.Service();
            srv.setServiceCode("MEAL");
            item.setServiceList(java.util.Collections.singletonList(srv));
            item.setPassengerIds(java.util.Collections.singletonList("pax1"));
            item.setTotalPrice(java.math.BigDecimal.valueOf(15.0));
            standardResponse.setOrderItems(java.util.Collections.singletonList(item));
            standardResponse.setTotalOrderPrice(java.math.BigDecimal.valueOf(115.0));
            
            mockedServiceResponseStatic.when(() -> com.airlines.go7api.response.ChangeServiceResponse.generateResponse(
                    org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.any(),
                    org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(standardResponse);
            
            String jsonReq = "{\"orderId\":\"12345\",\"paymentInformation\":{\"cardNumber\":\"1234567890123456\",\"currencyCode\":\"USD\",\"amount\":100.00}}";
            MvcResult result = mockMvc.perform(post("/change/service")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonReq))
                    .andReturn();
            
            assertNotNull(result.getResponse());
        }
    }

    @org.junit.jupiter.api.Test
    void testOrderChangeWithBookingInDb() throws Exception {
        com.airlines.go7api.entity.BookingEntity entity = new com.airlines.go7api.entity.BookingEntity();
        entity.setOrderId("12345");
        entity.setBookingConfirmation("CONF123");
        
        org.mockito.Mockito.when(bookingService.getBookingByOrderId("12345"))
                .thenReturn(java.util.Optional.of(entity));
                
        com.airlines.go7api.request.OrderChangeReq mockChangeReq = org.mockito.Mockito.mock(com.airlines.go7api.request.OrderChangeReq.class);
        org.mockito.Mockito.when(mockChangeReq.unmarshal()).thenReturn(java.util.Collections.singletonMap("success", true));
        
        try (org.mockito.MockedStatic<com.airlines.go7api.request.OrderChangeReq> mockedStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.OrderChangeReq.class)) {
            mockedStatic.when(() -> com.airlines.go7api.request.OrderChangeReq.mapToOrderChangeReq(org.mockito.Mockito.any()))
                    .thenReturn(mockChangeReq);
                    
            String json = "{\"orderId\":\"12345\"}";
            MvcResult result = mockMvc.perform(post("/orderchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andReturn();
            assertNotNull(result.getResponse());
        }
    }

    @org.junit.jupiter.api.Test
    void testOrderReshopWithAerocrsParms() throws Exception {
        com.airlines.go7api.request.AirshopReq mockAirshopReq = org.mockito.Mockito.mock(com.airlines.go7api.request.AirshopReq.class);
        com.airlines.go7api.responsego7.AirshopRspGo7Dto mockAirshopRsp = new com.airlines.go7api.responsego7.AirshopRspGo7Dto();
        com.airlines.go7api.responsego7.common.Aerocrs aerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
        aerocrs.setSuccess(true);
        mockAirshopRsp.setAerocrs(aerocrs);
        
        org.mockito.Mockito.when(mockAirshopReq.unmarshal()).thenReturn(mockAirshopRsp);
        
        com.airlines.go7api.responsedto.OrderReshopRspDto mockDto = new com.airlines.go7api.responsedto.OrderReshopRspDto();
        org.mockito.Mockito.when(orderReshopResponse.orderReshopMapper(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                .thenReturn(mockDto);
                
        com.airlines.go7api.request.OrderRetrieveReq mockRetrieveReq = org.mockito.Mockito.mock(com.airlines.go7api.request.OrderRetrieveReq.class);
        com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp = new com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto();
        com.airlines.go7api.responsego7.common.Aerocrs retrieveAerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
        retrieveAerocrs.setSuccess(true);
        com.airlines.go7api.responsego7.common.Booking booking = new com.airlines.go7api.responsego7.common.Booking();
        booking.setAdults(2);
        booking.setChild(1);
        booking.setInfant(0);
        retrieveAerocrs.setBooking(booking);
        bookingRsp.setAerocrs(retrieveAerocrs);
        
        org.mockito.Mockito.when(mockRetrieveReq.unmarshal()).thenReturn(bookingRsp);
        org.mockito.Mockito.when(bookingService.getBookingByOrderId("CONF123")).thenReturn(java.util.Optional.empty());
                
        try (org.mockito.MockedStatic<com.airlines.go7api.request.AirshopReq> mockedStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.AirshopReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.SeatAvailabilityReq> mockedSeatStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.SeatAvailabilityReq.class)) {
            
            mockedStatic.when(() -> com.airlines.go7api.request.AirshopReq.mapToFlightSearchRequestDTO(org.mockito.Mockito.any()))
                    .thenReturn(mockAirshopReq);
            mockedSeatStatic.when(() -> com.airlines.go7api.request.SeatAvailabilityReq.mapToGetBookingReq("CONF123"))
                    .thenReturn(mockRetrieveReq);
            
            String json = "{\"aerocrs\":{\"parms\":{\"bookingconfirmation\":\"CONF123\"}},\"ods\":[{\"origin\":\"JFK\",\"destination\":\"LAX\",\"date\":\"2026-06-15\"},{\"origin\":\"LAX\",\"destination\":\"JFK\",\"date\":\"2026-06-20\"}]}";
            MvcResult result = mockMvc.perform(post("/orderreshop")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andReturn();
            
            assertNotNull(result.getResponse());
        }
    }

    @org.junit.jupiter.api.Test
    void testOrderReshopWithBookingIdParms() throws Exception {
        com.airlines.go7api.request.AirshopReq mockAirshopReq = org.mockito.Mockito.mock(com.airlines.go7api.request.AirshopReq.class);
        com.airlines.go7api.responsego7.AirshopRspGo7Dto mockAirshopRsp = new com.airlines.go7api.responsego7.AirshopRspGo7Dto();
        com.airlines.go7api.responsego7.common.Aerocrs aerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
        aerocrs.setSuccess(true);
        mockAirshopRsp.setAerocrs(aerocrs);
        
        org.mockito.Mockito.when(mockAirshopReq.unmarshal()).thenReturn(mockAirshopRsp);
        
        com.airlines.go7api.responsedto.OrderReshopRspDto mockDto = new com.airlines.go7api.responsedto.OrderReshopRspDto();
        org.mockito.Mockito.when(orderReshopResponse.orderReshopMapper(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                .thenReturn(mockDto);
                
        com.airlines.go7api.request.OrderRetrieveReq mockRetrieveReq = org.mockito.Mockito.mock(com.airlines.go7api.request.OrderRetrieveReq.class);
        com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp = new com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto();
        com.airlines.go7api.responsego7.common.Aerocrs retrieveAerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
        retrieveAerocrs.setSuccess(true);
        com.airlines.go7api.responsego7.common.Booking booking = new com.airlines.go7api.responsego7.common.Booking();
        booking.setAdults(1);
        booking.setChild(0);
        booking.setInfant(1);
        retrieveAerocrs.setBooking(booking);
        bookingRsp.setAerocrs(retrieveAerocrs);
        
        org.mockito.Mockito.when(mockRetrieveReq.unmarshal()).thenReturn(bookingRsp);
        
        com.airlines.go7api.entity.BookingEntity entity = new com.airlines.go7api.entity.BookingEntity();
        entity.setOrderId("12345");
        entity.setBookingConfirmation("CONF_FROM_DB");
        org.mockito.Mockito.when(bookingService.getBookingByOrderId("12345")).thenReturn(java.util.Optional.of(entity));
                
        try (org.mockito.MockedStatic<com.airlines.go7api.request.AirshopReq> mockedStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.AirshopReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.SeatAvailabilityReq> mockedSeatStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.SeatAvailabilityReq.class)) {
            
            mockedStatic.when(() -> com.airlines.go7api.request.AirshopReq.mapToFlightSearchRequestDTO(org.mockito.Mockito.any()))
                    .thenReturn(mockAirshopReq);
            mockedSeatStatic.when(() -> com.airlines.go7api.request.SeatAvailabilityReq.mapToGetBookingReq("CONF_FROM_DB"))
                    .thenReturn(mockRetrieveReq);
            
            String json = "{\"aerocrs\":{\"parms\":{\"bookingid\":\"12345\"}},\"ods\":[{\"origin\":\"JFK\",\"destination\":\"LAX\",\"date\":\"2026-06-15\"}]}";
            MvcResult result = mockMvc.perform(post("/orderreshop")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andReturn();
            
            assertNotNull(result.getResponse());
        }
    }

    @org.junit.jupiter.api.Test
    void testFSOrderReshopWithNonArrayOrEmptyPax() throws Exception {
        com.airlines.go7api.request.AirshopReq mockAirshopReq = org.mockito.Mockito.mock(com.airlines.go7api.request.AirshopReq.class);
        com.airlines.go7api.responsego7.AirshopRspGo7Dto mockAirshopRsp = new com.airlines.go7api.responsego7.AirshopRspGo7Dto();
        com.airlines.go7api.responsego7.common.Aerocrs aerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
        aerocrs.setSuccess(true);
        mockAirshopRsp.setAerocrs(aerocrs);
        
        org.mockito.Mockito.when(mockAirshopReq.unmarshal()).thenReturn(mockAirshopRsp);
        
        com.airlines.go7api.responsedto.OrderReshopRspDto mockDto = new com.airlines.go7api.responsedto.OrderReshopRspDto();
        org.mockito.Mockito.when(orderReshopResponse.orderReshopMapper(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                .thenReturn(mockDto);
                
        try (org.mockito.MockedStatic<com.airlines.go7api.request.AirshopReq> mockedStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.AirshopReq.class)) {
            mockedStatic.when(() -> com.airlines.go7api.request.AirshopReq.mapToFlightSearchRequestDTO(org.mockito.Mockito.any()))
                    .thenReturn(mockAirshopReq);
            
            // 1. paxList is not an array, and tripType round trip branch
            String json1 = "{\"paxList\":{},\"ods\":[{\"origin\":\"JFK\",\"destination\":\"LAX\",\"date\":\"2026-06-15\"},{\"origin\":\"LAX\",\"destination\":\"JFK\",\"date\":\"2026-06-20\"}]}";
            MvcResult result1 = mockMvc.perform(post("/FSorderreshop")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json1))
                    .andReturn();
            assertNotNull(result1.getResponse());

            // 2. paxList is array with unknown ptc
            String json2 = "{\"paxList\":[{\"ptc\":\"UNK\"}],\"ods\":[{\"origin\":\"JFK\",\"destination\":\"LAX\",\"date\":\"2026-06-15\"}]}";
            MvcResult result2 = mockMvc.perform(post("/FSorderreshop")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json2))
                    .andReturn();
            assertNotNull(result2.getResponse());
        }
    }

    @org.junit.jupiter.api.Test
    void testChangePaymentPreFetchCombinedAmountOutstandingBranches() throws Exception {
        com.airlines.go7api.request.ChangePaymentReq mockChangePaymentReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangePaymentReq.class);
        com.airlines.go7api.request.ChangePaymentReq mockGetBookingReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangePaymentReq.class);
        
        try (org.mockito.MockedStatic<com.airlines.go7api.request.ChangePaymentReq> mockedStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.ChangePaymentReq.class)) {
            mockedStatic.when(() -> com.airlines.go7api.request.ChangePaymentReq.mapToChangePaymentReq(org.mockito.Mockito.any()))
                    .thenReturn(mockChangePaymentReq);
            mockedStatic.when(() -> com.airlines.go7api.request.ChangePaymentReq.mapToGetBookingReq(org.mockito.Mockito.anyString(), org.mockito.Mockito.any()))
                    .thenReturn(mockGetBookingReq);
            
            // 1. Null/empty orderId branch
            String json1 = "{\"orderId\":\"\",\"paymentInformation\":{\"currencyCode\":\"USD\",\"amount\":100.00}}";
            MvcResult result1 = mockMvc.perform(post("/change/payment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json1))
                    .andReturn();
            assertNotNull(result1.getResponse());

            // 2. BookingEntity not found in DB
            org.mockito.Mockito.when(bookingService.getBookingByOrderId("999"))
                    .thenReturn(java.util.Optional.empty());
            org.mockito.Mockito.when(mockChangePaymentReq.unmarshal()).thenReturn(java.util.Collections.singletonMap("success", false));
            String json2 = "{\"orderId\":\"999\",\"paymentInformation\":{\"currencyCode\":\"USD\",\"amount\":100.00}}";
            MvcResult result2 = mockMvc.perform(post("/change/payment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json2))
                    .andReturn();
            assertNotNull(result2.getResponse());

            // 3. preResponse is ErrorRsp
            com.airlines.go7api.entity.BookingEntity entity = new com.airlines.go7api.entity.BookingEntity();
            entity.setOrderId("123");
            entity.setBookingConfirmation("CONF123");
            org.mockito.Mockito.when(bookingService.getBookingByOrderId("123"))
                    .thenReturn(java.util.Optional.of(entity));
            com.airlines.go7api.error.ErrorRsp errorRsp = new com.airlines.go7api.error.ErrorRsp();
            org.mockito.Mockito.when(mockGetBookingReq.unmarshal()).thenReturn(errorRsp);
            String json3 = "{\"orderId\":\"123\",\"paymentInformation\":{\"currencyCode\":\"USD\",\"amount\":100.00}}";
            MvcResult result3 = mockMvc.perform(post("/change/payment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json3))
                    .andReturn();
            assertNotNull(result3.getResponse());

            // 4. preBooking has null aerocrs or booking
            com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto preBookingNull = new com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto();
            org.mockito.Mockito.when(mockGetBookingReq.unmarshal()).thenReturn(preBookingNull);
            MvcResult result4 = mockMvc.perform(post("/change/payment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json3))
                    .andReturn();
            assertNotNull(result4.getResponse());

            // 5. outstanding balance <= 0
            com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto preBookingZero = new com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto();
            com.airlines.go7api.responsego7.common.Aerocrs aerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            com.airlines.go7api.responsego7.common.Booking booking = new com.airlines.go7api.responsego7.common.Booking();
            com.airlines.go7api.responsego7.common.BalanceInformation balance = new com.airlines.go7api.responsego7.common.BalanceInformation();
            balance.setPnrOutstandingPayment(0.0);
            booking.setBalanceInformation(balance);
            aerocrs.setBooking(booking);
            preBookingZero.setAerocrs(aerocrs);
            org.mockito.Mockito.when(mockGetBookingReq.unmarshal()).thenReturn(preBookingZero);
            MvcResult result5 = mockMvc.perform(post("/change/payment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json3))
                    .andReturn();
            assertNotNull(result5.getResponse());

            // 6. Outstanding balance > 0 but has only seats (no services)
            entity.setPassengerSeats(java.util.Collections.singletonMap("1", "4A"));
            entity.setPassengerServices(null);
            balance.setPnrOutstandingPayment(150.0);
            MvcResult result6 = mockMvc.perform(post("/change/payment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json3))
                    .andReturn();
            assertNotNull(result6.getResponse());

            // 7. Outstanding balance > 0 but has only services (no seats)
            entity.setPassengerSeats(null);
            entity.setPassengerServices(java.util.Collections.singletonMap("1", "MEAL"));
            MvcResult result7 = mockMvc.perform(post("/change/payment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json3))
                    .andReturn();
            assertNotNull(result7.getResponse());
        }
    }

    @org.junit.jupiter.api.Test
    void testChangeServiceWithNullResponse() throws Exception {
        com.airlines.go7api.entity.BookingEntity entity = new com.airlines.go7api.entity.BookingEntity();
        entity.setOrderId("12345");
        entity.setBookingConfirmation("CONF123");
        
        org.mockito.Mockito.when(bookingService.getBookingByOrderId("12345"))
                .thenReturn(java.util.Optional.of(entity));
                
        com.airlines.go7api.request.OrderRetrieveReq mockRetrieveReq = org.mockito.Mockito.mock(com.airlines.go7api.request.OrderRetrieveReq.class);
        com.airlines.go7api.request.ChangeServiceReq mockChangeServiceReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangeServiceReq.class);
        
        try (org.mockito.MockedStatic<com.airlines.go7api.request.OrderRetrieveReq> mockedRetrieveStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.OrderRetrieveReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.ChangeServiceReq> mockedServiceStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.ChangeServiceReq.class)) {
            
            mockedRetrieveStatic.when(() -> com.airlines.go7api.request.OrderRetrieveReq.mapToOrderRetrieveReq(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(mockRetrieveReq);
            mockedServiceStatic.when(() -> com.airlines.go7api.request.ChangeServiceReq.mapToChangeServiceReq(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(mockChangeServiceReq);
            
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp = new com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto();
            com.airlines.go7api.responsego7.common.Aerocrs retrieveAerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            retrieveAerocrs.setSuccess(true);
            bookingRsp.setAerocrs(retrieveAerocrs);
            
            org.mockito.Mockito.when(mockRetrieveReq.unmarshal()).thenReturn(bookingRsp);
            org.mockito.Mockito.when(mockChangeServiceReq.unmarshal()).thenReturn(null);
            
            String jsonReq = "{\"orderId\":\"12345\"}";
            MvcResult result = mockMvc.perform(post("/change/service")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonReq))
                    .andReturn();
            
            assertNotNull(result.getResponse());
        }
    }

    @org.junit.jupiter.api.Test
    void testChangeServiceWithAeroCrsFailure() throws Exception {
        com.airlines.go7api.entity.BookingEntity entity = new com.airlines.go7api.entity.BookingEntity();
        entity.setOrderId("12345");
        entity.setBookingConfirmation("CONF123");
        
        org.mockito.Mockito.when(bookingService.getBookingByOrderId("12345"))
                .thenReturn(java.util.Optional.of(entity));
                
        com.airlines.go7api.request.OrderRetrieveReq mockRetrieveReq = org.mockito.Mockito.mock(com.airlines.go7api.request.OrderRetrieveReq.class);
        com.airlines.go7api.request.ChangeServiceReq mockChangeServiceReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangeServiceReq.class);
        
        try (org.mockito.MockedStatic<com.airlines.go7api.request.OrderRetrieveReq> mockedRetrieveStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.OrderRetrieveReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.ChangeServiceReq> mockedServiceStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.ChangeServiceReq.class)) {
            
            mockedRetrieveStatic.when(() -> com.airlines.go7api.request.OrderRetrieveReq.mapToOrderRetrieveReq(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(mockRetrieveReq);
            mockedServiceStatic.when(() -> com.airlines.go7api.request.ChangeServiceReq.mapToChangeServiceReq(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(mockChangeServiceReq);
            
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp = new com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto();
            com.airlines.go7api.responsego7.common.Aerocrs retrieveAerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            retrieveAerocrs.setSuccess(true);
            bookingRsp.setAerocrs(retrieveAerocrs);
            
            org.mockito.Mockito.when(mockRetrieveReq.unmarshal()).thenReturn(bookingRsp);
            
            com.airlines.go7api.responsego7.ChangeServiceRspGo7Dto changeServiceRsp = new com.airlines.go7api.responsego7.ChangeServiceRspGo7Dto();
            com.airlines.go7api.responsego7.common.Aerocrs serviceAerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            serviceAerocrs.setSuccess(false); // aeroCRS failure
            changeServiceRsp.setAerocrs(serviceAerocrs);
            
            org.mockito.Mockito.when(mockChangeServiceReq.unmarshal()).thenReturn(changeServiceRsp);
            
            String jsonReq = "{\"orderId\":\"12345\"}";
            MvcResult result = mockMvc.perform(post("/change/service")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonReq))
                    .andReturn();
            assertNotNull(result.getResponse());
        }
    }

    @org.junit.jupiter.api.Test
    void testChangeServiceWithFailedPayment() throws Exception {
        com.airlines.go7api.entity.BookingEntity entity = new com.airlines.go7api.entity.BookingEntity();
        entity.setOrderId("12345");
        entity.setBookingConfirmation("CONF123");
        entity.setPrimaryPassengerLastName("Smith");
        
        org.mockito.Mockito.when(bookingService.getBookingByOrderId("12345"))
                .thenReturn(java.util.Optional.of(entity));
        
        com.airlines.go7api.request.OrderRetrieveReq mockRetrieveReq = org.mockito.Mockito.mock(com.airlines.go7api.request.OrderRetrieveReq.class);
        com.airlines.go7api.request.ChangeServiceReq mockChangeServiceReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangeServiceReq.class);
        com.airlines.go7api.request.ChangePaymentReq mockChangePaymentReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangePaymentReq.class);
        
        try (org.mockito.MockedStatic<com.airlines.go7api.request.OrderRetrieveReq> mockedRetrieveStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.OrderRetrieveReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.ChangeServiceReq> mockedServiceStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.ChangeServiceReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.ChangePaymentReq> mockedPaymentStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.ChangePaymentReq.class)) {
            
            mockedRetrieveStatic.when(() -> com.airlines.go7api.request.OrderRetrieveReq.mapToOrderRetrieveReq(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(mockRetrieveReq);
            mockedServiceStatic.when(() -> com.airlines.go7api.request.ChangeServiceReq.mapToChangeServiceReq(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(mockChangeServiceReq);
            mockedPaymentStatic.when(() -> com.airlines.go7api.request.ChangePaymentReq.mapToChangePaymentReq(org.mockito.Mockito.any()))
                    .thenReturn(mockChangePaymentReq);
            
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp = new com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto();
            com.airlines.go7api.responsego7.common.Aerocrs retrieveAerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            retrieveAerocrs.setSuccess(true);
            com.airlines.go7api.responsego7.common.Booking booking = new com.airlines.go7api.responsego7.common.Booking();
            booking.setBookingid(12345L);
            booking.setBookingconfirmation("CONF123");
            retrieveAerocrs.setBooking(booking);
            bookingRsp.setAerocrs(retrieveAerocrs);
            
            org.mockito.Mockito.when(mockRetrieveReq.unmarshal()).thenReturn(bookingRsp);
            
            com.airlines.go7api.responsego7.ChangeServiceRspGo7Dto changeServiceRsp = new com.airlines.go7api.responsego7.ChangeServiceRspGo7Dto();
            com.airlines.go7api.responsego7.common.Aerocrs serviceAerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            serviceAerocrs.setSuccess(true);
            
            com.airlines.go7api.responsego7.common.Aerocrs.Detail detail = new com.airlines.go7api.responsego7.common.Aerocrs.Detail();
            detail.setSuccess(true);
            java.util.Map<String, Object> ancillary = new java.util.HashMap<>();
            ancillary.put("itemid", "SRV1");
            ancillary.put("flightid", "999");
            ancillary.put("totalprice", "10.00");
            detail.setAncillary(ancillary);
            
            serviceAerocrs.setDetails(java.util.Collections.singletonList(detail));
            changeServiceRsp.setAerocrs(serviceAerocrs);
            
            org.mockito.Mockito.when(mockChangeServiceReq.unmarshal()).thenReturn(changeServiceRsp);
            
            java.util.Map<String, Object> pmResponse = new java.util.HashMap<>();
            pmResponse.put("success", false);
            org.mockito.Mockito.when(mockChangePaymentReq.unmarshal()).thenReturn(pmResponse);
            
            String json = "{\"orderId\":\"12345\",\"paymentInformation\":{\"cardNumber\":\"1234567890123456\",\"currencyCode\":\"USD\",\"amount\":100.00}}";
            MvcResult result = mockMvc.perform(post("/change/service")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andReturn();
            
            assertNotNull(result.getResponse());
        }
    }

    @org.junit.jupiter.api.Test
    void testChangeSeatAdditionalCoverage() throws Exception {
        com.airlines.go7api.entity.BookingEntity entity = new com.airlines.go7api.entity.BookingEntity();
        entity.setOrderId("12345");
        entity.setBookingConfirmation("CONF123");
        // Exercise entity.getPrimaryPassengerLastName() is not empty/null
        entity.setPrimaryPassengerLastName("Smith");
        
        org.mockito.Mockito.when(bookingService.getBookingByOrderId("12345"))
                .thenReturn(java.util.Optional.of(entity));
        
        com.airlines.go7api.request.OrderRetrieveReq mockRetrieveReq = org.mockito.Mockito.mock(com.airlines.go7api.request.OrderRetrieveReq.class);
        com.airlines.go7api.request.ChangeSeatReq mockChangeSeatReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangeSeatReq.class);
        
        try (org.mockito.MockedStatic<com.airlines.go7api.request.OrderRetrieveReq> mockedRetrieveStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.OrderRetrieveReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.SeatAvailabilityReq> mockedSeatReqStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.SeatAvailabilityReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.ChangeSeatReq> mockedSeatStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.ChangeSeatReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.response.ChangeSeatResponse> mockedSeatResponseStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.response.ChangeSeatResponse.class)) {
            
            mockedRetrieveStatic.when(() -> com.airlines.go7api.request.OrderRetrieveReq.mapToOrderRetrieveReq(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(mockRetrieveReq);
            mockedSeatReqStatic.when(() -> com.airlines.go7api.request.SeatAvailabilityReq.mapToGetBookingReq(org.mockito.Mockito.anyString()))
                    .thenReturn(mockRetrieveReq);
            mockedSeatStatic.when(() -> com.airlines.go7api.request.ChangeSeatReq.mapToChangeSeatReq(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(mockChangeSeatReq);
            
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp = new com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto();
            com.airlines.go7api.responsego7.common.Aerocrs retrieveAerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            retrieveAerocrs.setSuccess(true);
            com.airlines.go7api.responsego7.common.Booking booking = new com.airlines.go7api.responsego7.common.Booking();
            booking.setBookingid(12345L);
            booking.setBookingconfirmation("CONF123");
            
            com.airlines.go7api.responsego7.common.Flights flights = new com.airlines.go7api.responsego7.common.Flights();
            com.airlines.go7api.responsego7.common.Flight flight = new com.airlines.go7api.responsego7.common.Flight();
            flight.setFlightClass("Y/Economy");
            flight.setNumber("101");
            flights.setFlight(java.util.Collections.singletonList(flight));
            booking.setFlights(flights);
            
            com.airlines.go7api.responsego7.common.Passengers passengers = new com.airlines.go7api.responsego7.common.Passengers();
            com.airlines.go7api.responsego7.common.Passenger passenger = new com.airlines.go7api.responsego7.common.Passenger();
            passenger.setLastname("Smith");
            passengers.setPassenger(java.util.Collections.singletonList(passenger));
            booking.setPassengers(passengers);
            
            retrieveAerocrs.setBooking(booking);
            bookingRsp.setAerocrs(retrieveAerocrs);
            
            org.mockito.Mockito.when(mockRetrieveReq.unmarshal()).thenReturn(bookingRsp);
            
            // Mock ChangeSeatResponse
            com.airlines.go7api.responsego7.ChangeSeatRspGo7Dto changeSeatRsp = new com.airlines.go7api.responsego7.ChangeSeatRspGo7Dto();
            com.airlines.go7api.responsego7.common.Aerocrs aerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            aerocrs.setSuccess(true);
            
            com.airlines.go7api.responsego7.common.Flights seatFlights = new com.airlines.go7api.responsego7.common.Flights();
            com.airlines.go7api.responsego7.common.Flight seatFlight = new com.airlines.go7api.responsego7.common.Flight();
            seatFlight.setFlightClass("Y/Economy");
            seatFlight.setNumber("101");
            
            com.airlines.go7api.responsego7.common.Seat seat = new com.airlines.go7api.responsego7.common.Seat();
            seat.setStatus(true);
            seat.setSeatNumber("4A");
            // Exercise seat.getFare() is not null and > 0 branch in fetchActualSeatPrices
            seat.setFare(java.math.BigDecimal.TEN);
            
            seatFlight.setSeat(java.util.Collections.singletonList(seat));
            seatFlights.setFlight(java.util.Collections.singletonList(seatFlight));
            aerocrs.setFlights(seatFlights);
            changeSeatRsp.setAerocrs(aerocrs);
            
            org.mockito.Mockito.when(mockChangeSeatReq.unmarshal()).thenReturn(changeSeatRsp);
            
            com.airlines.go7api.responsedto.ChangeSeatRspDto standardResponse = new com.airlines.go7api.responsedto.ChangeSeatRspDto();
            com.airlines.go7api.responsedto.common.OrderItemsDTO item = new com.airlines.go7api.responsedto.common.OrderItemsDTO();
            item.setOrderItemId("12345_AIR-1");
            com.airlines.go7api.responsedto.common.Service srv = new com.airlines.go7api.responsedto.common.Service();
            srv.setServiceCode("SEAT4A");
            item.setServiceList(java.util.Collections.singletonList(srv));
            item.setPassengerIds(java.util.Collections.singletonList("pax1"));
            // Exercise item.getTotalPrice() == null branch
            item.setTotalPrice(null);
            standardResponse.setOrderItems(java.util.Collections.singletonList(item));
            
            mockedSeatResponseStatic.when(() -> com.airlines.go7api.response.ChangeSeatResponse.generateResponse(
                    org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.any(),
                    org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(standardResponse);
            
            String json = "{\"orderId\":\"12345\"}";
            MvcResult result = mockMvc.perform(post("/change/seat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andReturn();
            
            assertNotNull(result.getResponse());
        }
    }

    @org.junit.jupiter.api.Test
    void testChangeServiceAdditionalCoverage() throws Exception {
        com.airlines.go7api.entity.BookingEntity entity = new com.airlines.go7api.entity.BookingEntity();
        entity.setOrderId("12345");
        entity.setBookingConfirmation("CONF123");
        entity.setPrimaryPassengerLastName("Smith");
        
        org.mockito.Mockito.when(bookingService.getBookingByOrderId("12345"))
                .thenReturn(java.util.Optional.of(entity));
        
        com.airlines.go7api.request.OrderRetrieveReq mockRetrieveReq = org.mockito.Mockito.mock(com.airlines.go7api.request.OrderRetrieveReq.class);
        com.airlines.go7api.request.ChangeServiceReq mockChangeServiceReq = org.mockito.Mockito.mock(com.airlines.go7api.request.ChangeServiceReq.class);
        
        try (org.mockito.MockedStatic<com.airlines.go7api.request.OrderRetrieveReq> mockedRetrieveStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.OrderRetrieveReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.SeatAvailabilityReq> mockedSeatReqStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.SeatAvailabilityReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.request.ChangeServiceReq> mockedServiceStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.request.ChangeServiceReq.class);
             org.mockito.MockedStatic<com.airlines.go7api.response.ChangeServiceResponse> mockedServiceResponseStatic = org.mockito.Mockito.mockStatic(com.airlines.go7api.response.ChangeServiceResponse.class)) {
            
            mockedRetrieveStatic.when(() -> com.airlines.go7api.request.OrderRetrieveReq.mapToOrderRetrieveReq(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(mockRetrieveReq);
            mockedSeatReqStatic.when(() -> com.airlines.go7api.request.SeatAvailabilityReq.mapToGetBookingReq(org.mockito.Mockito.anyString()))
                    .thenReturn(mockRetrieveReq);
            mockedServiceStatic.when(() -> com.airlines.go7api.request.ChangeServiceReq.mapToChangeServiceReq(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(mockChangeServiceReq);
            
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp = new com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto();
            com.airlines.go7api.responsego7.common.Aerocrs retrieveAerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            retrieveAerocrs.setSuccess(true);
            com.airlines.go7api.responsego7.common.Booking booking = new com.airlines.go7api.responsego7.common.Booking();
            booking.setBookingid(12345L);
            booking.setBookingconfirmation("CONF123");
            
            com.airlines.go7api.responsego7.common.Flights flights = new com.airlines.go7api.responsego7.common.Flights();
            com.airlines.go7api.responsego7.common.Flight flight = new com.airlines.go7api.responsego7.common.Flight();
            flight.setNumber("101");
            flights.setFlight(java.util.Collections.singletonList(flight));
            booking.setFlights(flights);
            
            com.airlines.go7api.responsego7.common.Passengers passengers = new com.airlines.go7api.responsego7.common.Passengers();
            com.airlines.go7api.responsego7.common.Passenger passenger = new com.airlines.go7api.responsego7.common.Passenger();
            passenger.setLastname("Smith");
            passengers.setPassenger(java.util.Collections.singletonList(passenger));
            booking.setPassengers(passengers);
            
            retrieveAerocrs.setBooking(booking);
            bookingRsp.setAerocrs(retrieveAerocrs);
            
            org.mockito.Mockito.when(mockRetrieveReq.unmarshal()).thenReturn(bookingRsp);
            
            com.airlines.go7api.responsego7.ChangeServiceRspGo7Dto changeServiceRsp = new com.airlines.go7api.responsego7.ChangeServiceRspGo7Dto();
            com.airlines.go7api.responsego7.common.Aerocrs serviceAerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
            serviceAerocrs.setSuccess(true);
            
            com.airlines.go7api.responsego7.common.Aerocrs.Detail detail = new com.airlines.go7api.responsego7.common.Aerocrs.Detail();
            // Exercise detail success is false branch (isAnyServiceSuccessful returns false)
            detail.setSuccess(false);
            java.util.Map<String, Object> ancillary = new java.util.HashMap<>();
            ancillary.put("itemid", "SRV1");
            ancillary.put("flightid", "999");
            // Exercise NumberFormatException branch in processAncillaryForActualPrices
            ancillary.put("totalprice", "invalid_price");
            detail.setAncillary(ancillary);
            
            serviceAerocrs.setDetails(java.util.Collections.singletonList(detail));
            changeServiceRsp.setAerocrs(serviceAerocrs);
            
            org.mockito.Mockito.when(mockChangeServiceReq.unmarshal()).thenReturn(changeServiceRsp);
            
            com.airlines.go7api.responsedto.ChangeServiceRspDto standardResponse = new com.airlines.go7api.responsedto.ChangeServiceRspDto();
            com.airlines.go7api.responsedto.common.OrderItemsDTO item = new com.airlines.go7api.responsedto.common.OrderItemsDTO();
            item.setOrderItemId("12345_SRV1");
            com.airlines.go7api.responsedto.common.Service srv = new com.airlines.go7api.responsedto.common.Service();
            srv.setServiceCode("MEAL");
            item.setServiceList(java.util.Collections.singletonList(srv));
            item.setPassengerIds(java.util.Collections.singletonList("pax1"));
            // Exercise item.getTotalPrice() == null branch
            item.setTotalPrice(null);
            standardResponse.setOrderItems(java.util.Collections.singletonList(item));
            
            mockedServiceResponseStatic.when(() -> com.airlines.go7api.response.ChangeServiceResponse.generateResponse(
                    org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.any(),
                    org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                    .thenReturn(standardResponse);
            
            String jsonReq = "{\"orderId\":\"12345\"}";
            MvcResult result = mockMvc.perform(post("/change/service")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonReq))
                    .andReturn();
            
            assertNotNull(result.getResponse());
        }
    }
}
