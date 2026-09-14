package com.airlines.go7api.response;

import com.airlines.go7api.request.OfferPriceReq;
import com.airlines.go7api.responsedto.OfferPriceRspDto;
import com.airlines.go7api.responsedto.common.OD;
import com.airlines.go7api.responsego7.OfferPriceRspGo7Dto;
import com.airlines.go7api.responsego7.common.*;
import com.airlines.go7api.error.ErrorRsp;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class OfferPriceResponseTest {

    @Test
    void testConstructorIsPrivate() throws Exception {
        java.lang.reflect.Constructor<OfferPriceResponse> constructor = OfferPriceResponse.class.getDeclaredConstructor();
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
    void testGenerateResponse_InvalidInputs() throws Exception {
        OfferPriceReq mockReq = org.mockito.Mockito.mock(OfferPriceReq.class);
        
        // 1. unmarshal returns ErrorRsp
        org.mockito.Mockito.when(mockReq.unmarshal()).thenReturn(new ErrorRsp());
        Object result = OfferPriceResponse.generateResponse(mockReq);
        assertTrue(result instanceof ErrorRsp);

        // 2. unmarshal returns null
        org.mockito.Mockito.when(mockReq.unmarshal()).thenReturn(null);
        result = OfferPriceResponse.generateResponse(mockReq);
        assertTrue(result instanceof ErrorRsp);

        // 3. unmarshal returns DTO but getFlights is null
        OfferPriceRspGo7Dto go7Rsp = new OfferPriceRspGo7Dto();
        org.mockito.Mockito.when(mockReq.unmarshal()).thenReturn(go7Rsp);
        result = OfferPriceResponse.generateResponse(mockReq);
        assertTrue(result instanceof ErrorRsp);
    }

    @Test
    void testGenerateResponse_HappyPath() throws Exception {
        OfferPriceReq mockReq = org.mockito.Mockito.mock(OfferPriceReq.class);
        
        OfferPriceRspGo7Dto go7Rsp = new OfferPriceRspGo7Dto();
        com.airlines.go7api.responsego7.common.Aerocrs responseAerocrs = new com.airlines.go7api.responsego7.common.Aerocrs();
        responseAerocrs.setSuccess(true);
        
        Flights flights = new Flights();
        List<Flight> flightList = new ArrayList<>();
        
        Flight f1 = new Flight();
        f1.setFlightid(123);
        f1.setFromcode("JFK");
        f1.setTocode("LAX");
        f1.setFrom("John F. Kennedy");
        f1.setTo("Los Angeles");
        f1.setFlightdate("2026/06/15");
        f1.setDepart("10:00");
        f1.setArrive("13:00");
        f1.setNumber("G7100");
        f1.setAirline("Go7 Airlines");
        f1.setFlightClass("Y/Economy");
        f1.setCurrency("USD");
        f1.setNetFare("150.00");
        f1.setTotaltax("30.00");
        f1.setTax("30.00");
        
        flightList.add(f1);
        flights.setFlight(flightList);
        responseAerocrs.setFlights(flights);
        go7Rsp.setAerocrs(responseAerocrs);
        
        com.airlines.go7api.request.OfferPriceReq.Aerocrs requestAerocrs = new com.airlines.go7api.request.OfferPriceReq.Aerocrs();
        Map<String, Object> parms = new HashMap<>();
        parms.put("flightid1", "123");
        parms.put("fareid1", "456");
        parms.put("triptype", "Return");
        parms.put("adults", 1);
        parms.put("child", 1);
        parms.put("infant", 1);
        requestAerocrs.setParms(parms);
        
        org.mockito.Mockito.when(mockReq.unmarshal()).thenReturn(go7Rsp);
        org.mockito.Mockito.when(mockReq.getAerocrs()).thenReturn(requestAerocrs);

        Object result = OfferPriceResponse.generateResponse(mockReq);
        
        assertTrue(result instanceof OfferPriceRspDto);
        OfferPriceRspDto response = (OfferPriceRspDto) result;
        
        assertEquals("G7", response.getApiOwner());
        assertEquals("G7", response.getValidatingCarrier());
        assertEquals("USD", response.getCurrency());
        assertEquals("123-456-JFK-LAX-RT-1-1-1-USD", response.getPricedOfferId());
        
        assertNotNull(response.getOds());
        assertEquals(1, response.getOds().size());
        OD od = response.getOds().get(0);
        assertEquals("OD1", od.getOdKey());
        assertEquals("JFK", od.getOrigin());
        assertEquals("LAX", od.getDestination());
        assertEquals("15Jun2026", od.getDepartureDate());
        assertEquals("10:00", od.getDepartureTime());
        
        assertNotNull(response.getOfferItems());
        assertEquals(3, response.getOfferItems().size());
        
        // ADT item
        OfferPriceRspDto.OfferItemDto adtItem = response.getOfferItems().get(0);
        assertEquals("ADT", adtItem.getPtc());
        
        // CNN item
        OfferPriceRspDto.OfferItemDto cnnItem = response.getOfferItems().get(1);
        assertEquals("CNN", cnnItem.getPtc());

        OfferPriceRspDto.OfferItemDto infItem = response.getOfferItems().get(2);
        assertEquals("INF", infItem.getPtc());
    }

    @Test
    void testGenerateResponse_ParsingException() throws Exception {
        OfferPriceReq mockReq = org.mockito.Mockito.mock(OfferPriceReq.class);
        LinkedHashMap<String, Object> invalidMap = new LinkedHashMap<>();
        invalidMap.put("aerocrs", "invalid_type_not_a_map");

        org.mockito.Mockito.when(mockReq.unmarshal()).thenReturn(invalidMap);

        try {
            OfferPriceResponse.generateResponse(mockReq);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Failed to parse OfferPriceResp"));
        }
    }
}
