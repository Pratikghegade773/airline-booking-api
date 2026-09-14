package com.airlines.go7api.responsego7.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PassengersDeserializerTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "[{\"paxnum\": 1, \"paxtype\": \"ADT\"}]",
        "{\"count\": 1, \"passenger\": [{\"paxnum\": 1, \"paxtype\": \"ADT\"}]}",
        "{\"count\": 1, \"passenger\": {\"paxnum\": 1, \"paxtype\": \"ADT\"}}"
    })
    void testDeserialize(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Passengers pax = mapper.readValue(json, Passengers.class);

        assertNotNull(pax);
        assertEquals(1, pax.getCount());
        assertEquals(1, pax.getPassenger().get(0).getPaxnum());
        assertEquals("ADT", pax.getPassenger().get(0).getPaxtype());
    }
}
