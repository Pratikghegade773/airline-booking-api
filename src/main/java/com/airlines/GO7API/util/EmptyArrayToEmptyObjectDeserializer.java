package com.airlines.go7api.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

import java.io.IOException;

/**
 * Custom Jackson deserializer to handle third-party API variations where
 * an empty array {@code []} is returned instead of a structured empty object.
 */
public class EmptyArrayToEmptyObjectDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {

    private Class<?> targetClass;

    public EmptyArrayToEmptyObjectDeserializer() {
    }

    public EmptyArrayToEmptyObjectDeserializer(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        Class<?> rawClass = null;
        if (ctxt.getContextualType() != null) {
            rawClass = ctxt.getContextualType().getRawClass();
        } else if (property != null) {
            rawClass = property.getType().getRawClass();
        }
        if (rawClass == null) {
            rawClass = Object.class;
        }
        return new EmptyArrayToEmptyObjectDeserializer(rawClass);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.isExpectedStartArrayToken()) {
            p.skipChildren();
            if (targetClass != null && targetClass != Object.class) {
                try {
                    return targetClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        }
        return p.getCodec().readValue(p, targetClass);
    }
}
