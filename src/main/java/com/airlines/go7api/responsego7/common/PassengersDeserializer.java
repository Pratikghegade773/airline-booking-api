package com.airlines.go7api.responsego7.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.List;

public class PassengersDeserializer extends JsonDeserializer<Passengers> {

    @Override
    public Passengers deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        Passengers passengers = new Passengers();

        if (node.isArray()) {
            List<Passenger> list = mapper.convertValue(node, new TypeReference<List<Passenger>>() {});
            passengers.setPassenger(list);
            passengers.setCount(list.size());
        } else if (node.isObject()) {
            if (node.has("count")) {
                passengers.setCount(node.get("count").asInt());
            }
            if (node.has("passenger")) {
                JsonNode paxNode = node.get("passenger");
                if (paxNode.isArray()) {
                    List<Passenger> list = mapper.convertValue(paxNode, new TypeReference<List<Passenger>>() {});
                    passengers.setPassenger(list);
                } else if (paxNode.isObject()) {
                    Passenger singlePax = mapper.convertValue(paxNode, Passenger.class);
                    passengers.setPassenger(java.util.Collections.singletonList(singlePax));
                }
            }
        }
        return passengers;
    }
}
