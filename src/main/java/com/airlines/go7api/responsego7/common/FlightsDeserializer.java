package com.airlines.go7api.responsego7.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.List;

public class FlightsDeserializer extends JsonDeserializer<Flights> {

    @Override
    public Flights deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        Flights flights = new Flights();

        if (node.isArray()) {
            List<Flight> flightList = mapper.convertValue(node, new TypeReference<List<Flight>>() {});
            flights.setFlight(flightList);
            flights.setCount(flightList.size());
        } else if (node.isObject()) {
            if (node.has("count")) {
                flights.setCount(node.get("count").asInt());
            }
            if (node.has("flight")) {
                JsonNode flightNode = node.get("flight");
                if (flightNode.isArray()) {
                    List<Flight> flightList = mapper.convertValue(flightNode, new TypeReference<List<Flight>>() {});
                    flights.setFlight(flightList);
                } else if (flightNode.isObject()) {
                    // Sometimes a single flight is an object instead of array of length 1
                    Flight singleFlight = mapper.convertValue(flightNode, Flight.class);
                    flights.setFlight(java.util.Collections.singletonList(singleFlight));
                }
            }
        }
        return flights;
    }
}
