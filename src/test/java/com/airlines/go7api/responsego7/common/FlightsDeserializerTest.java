package com.airlines.go7api.responsego7.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FlightsDeserializerTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "[{\"flightid\": 999, \"number\": \"G7101\"}]",
        "{\"count\": 1, \"flight\": [{\"flightid\": 999, \"number\": \"G7101\"}]}"
    })
    void testDeserialize(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Flights flights = mapper.readValue(json, Flights.class);

        assertNotNull(flights);
        assertEquals(1, flights.getCount());
        assertEquals(999, flights.getFlight().get(0).getFlightid());
        assertEquals("G7101", flights.getFlight().get(0).getNumber());
    }
}
