package com.airlines.go7api.request;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderReshopReq extends BaseGo7Req {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OrderReshopReq.class);

    private static final String PARM_AEROCRS = "aerocrs";
    private static final String PARM_PARMS = "parms";
    private static final String PARM_BOOKINGCONFIRMATION = "bookingconfirmation";
    private static final String PARM_ACTION = "action";
    private static final String PARM_CURRENCY = "currency";
    private static final String PARM_BOOKFLIGHT = "bookflight";

    @JsonProperty("aerocrs")
    public Aerocrs aerocrs;

    @JsonIgnore
    public String reshopUrl;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Aerocrs {
        @JsonProperty("parms")
        public java.util.Map<String, Object> parms;
    }

    public static OrderReshopReq mapFromJson(String json) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("mapFromJson starting. Body: {}", json);
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        OrderReshopReq req = new OrderReshopReq();
        JsonNode root = mapper.readTree(json);

        java.util.Map<String, Object> sourceParms = populateSourceParams(root, mapper);
        java.util.Map<String, Object> targetParms = buildTargetParams(sourceParms);

        Aerocrs a = new Aerocrs();
        a.parms = targetParms;
        req.aerocrs = a;

        if (logger.isDebugEnabled()) {
            logger.debug("Refined parms: {}", targetParms.keySet());
        }
        req.reshopUrl = "https://api.aerocrs.com/v5/changeBooking";
        return req;
    }

    private static java.util.Map<String, Object> populateSourceParams(JsonNode root, ObjectMapper mapper) {
        java.util.Map<String, Object> sourceParms = new java.util.LinkedHashMap<>();
        
        JsonNode aerocrsNode = root.has(PARM_AEROCRS) ? root.get(PARM_AEROCRS) : null;
        JsonNode parmsNode = extractParmsNode(root, aerocrsNode);

        if (parmsNode != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Found 'parms' node. Mapping...");
            }
            sourceParms = mapper.convertValue(parmsNode,
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                    });
        }

        mapRootFields(root, mapper, sourceParms);
        return sourceParms;
    }

    private static JsonNode extractParmsNode(JsonNode root, JsonNode aerocrsNode) {
        if (aerocrsNode != null && aerocrsNode.has(PARM_PARMS)) {
            return aerocrsNode.get(PARM_PARMS);
        } else if (root.has(PARM_PARMS)) {
            return root.get(PARM_PARMS);
        }
        return null;
    }

    private static void mapRootFields(JsonNode root, ObjectMapper mapper, java.util.Map<String, Object> sourceParms) {
        java.util.Iterator<String> fieldNames = root.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if ("orderId".equals(fieldName)) {
                mapOrderId(root.get("orderId").asText(), sourceParms);
            } else if (isStandardFieldToMap(fieldName)) {
                sourceParms.put(fieldName, mapper.convertValue(root.get(fieldName), Object.class));
            }
        }
    }

    private static void mapOrderId(String oid, java.util.Map<String, Object> sourceParms) {
        if (oid.matches("\\d+")) {
            sourceParms.put("bookingid", Long.parseLong(oid));
        } else {
            sourceParms.put(PARM_BOOKINGCONFIRMATION, oid);
        }
    }

    private static boolean isStandardFieldToMap(String fieldName) {
        return !fieldName.equals(PARM_AEROCRS) && !fieldName.equals("responseId") 
                && !fieldName.equals("agencyId") && !fieldName.equals("agentId") 
                && !fieldName.equals("agencyName") && !fieldName.equals("apiKey") 
                && !fieldName.equals("reshopUrl");
    }

    private static java.util.Map<String, Object> buildTargetParams(java.util.Map<String, Object> sourceParms) {
        java.util.Map<String, Object> targetParms = new java.util.LinkedHashMap<>();

        // Booking ID / Confirmation
        Object bid = sourceParms.get("bookingid");
        Object bconf = sourceParms.get(PARM_BOOKINGCONFIRMATION);
        if (bconf != null) {
            targetParms.put(PARM_BOOKINGCONFIRMATION, bconf);
        } else if (bid != null) {
            targetParms.put(PARM_BOOKINGCONFIRMATION, bid);
        }

        // Action & Currency
        String actionValue = sourceParms.containsKey(PARM_ACTION) ? (String) sourceParms.get(PARM_ACTION) : "amend";
        targetParms.put(PARM_ACTION, actionValue);

        String currencyValue = sourceParms.containsKey(PARM_CURRENCY) ? (String) sourceParms.get(PARM_CURRENCY) : "USD";
        targetParms.put(PARM_CURRENCY, currencyValue);

        // Bookflight (Extracted from deleteOrderItems or taken directly)
        java.util.List<java.util.Map<String, Object>> bookflightList = new java.util.ArrayList<>();
        if (sourceParms.containsKey(PARM_BOOKFLIGHT)) {
            bookflightList = (java.util.List) sourceParms.get(PARM_BOOKFLIGHT);
        } else if (sourceParms.containsKey("deleteOrderItems") && logger.isDebugEnabled()) {
            logger.debug("Ignoring deleteOrderItems for bookflight generation. Controller will fetch true flight IDs from AeroCRS.");
        }
        
        if (!bookflightList.isEmpty()) {
            targetParms.put(PARM_BOOKFLIGHT, bookflightList);
        }

        return targetParms;
    }

    @Override
    protected String getApiUrl() {
        return reshopUrl != null ? reshopUrl : "";
    }

    @Override
    protected String getRequestName() {
        return "OrderReshop";
    }
}
