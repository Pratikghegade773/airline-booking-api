package com.airlines.go7api.response;

import com.airlines.go7api.responsego7.common.*;
import com.airlines.go7api.responsedto.common.*;
import com.airlines.go7api.request.OfferPriceReq;
import com.airlines.go7api.responsedto.OfferPriceRspDto;
import com.airlines.go7api.responsego7.OfferPriceRspGo7Dto;
import com.airlines.go7api.error.ErrorRsp;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class OfferPriceResponse {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OfferPriceResponse.class);

    private static final String KEY_FLIGHT_ID = "flightid";
    private static final String KEY_ADULTS = "adults";
    private static final String KEY_CHILD = "child";
    private static final String KEY_INFANT = "infant";
    private static final String KEY_FARE_ID = "fareid";
    private static final String KEY_TRIP_TYPE = "triptype";
    private static final String CARRIER_G7 = "G7";

    private OfferPriceResponse() {
        throw new IllegalStateException("Utility class");
    }

    public static Object generateResponse(OfferPriceReq request)
            throws IOException {
        Object responseObj = request.unmarshal();
        OfferPriceRspGo7Dto go7Response = parseGo7Response(responseObj);
        if (go7Response == null) {
            logger.warn("OfferPriceResp is null or missing required fields. Raw Response: {}", responseObj);
            return responseObj instanceof ErrorRsp ? responseObj : new ErrorRsp();
        }

        ResponseContext context = new ResponseContext(request, go7Response);
        context.initializeTopLevel();
        context.processFlights();
        context.populateOfferItems();
        context.populatePriceClassList();

        return context.response;
    }

    private static OfferPriceRspGo7Dto parseGo7Response(Object responseObj) {
        if (responseObj instanceof ErrorRsp) {
            return null;
        }
        OfferPriceRspGo7Dto go7Response = null;
        try {
            if (responseObj instanceof OfferPriceRspGo7Dto offerPriceRspGo7Dto) {
                go7Response = offerPriceRspGo7Dto;
            } else if (responseObj instanceof LinkedHashMap) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                go7Response = mapper.convertValue(responseObj, OfferPriceRspGo7Dto.class);
            }
        } catch (Exception e) {
            logger.error("Failed to parse OfferPriceResp from the response: {}", responseObj, e);
            throw new IllegalArgumentException("Failed to parse OfferPriceResp from the response", e);
        }
        if (go7Response == null || go7Response.getAerocrs() == null || go7Response.getAerocrs().getFlights() == null) {
            return null;
        }
        return go7Response;
    }

    private static class ResponseContext {
        final OfferPriceReq request;
        final OfferPriceRspGo7Dto go7Response;
        final OfferPriceRspDto response;
        final List<Flight> flights;
        String pricedOfferId;
        int adultsCount = 1;
        int childCount = 0;
        int infantCount = 0;

        ResponseContext(OfferPriceReq request, OfferPriceRspGo7Dto go7Response) {
            this.request = request;
            this.go7Response = go7Response;
            this.response = new OfferPriceRspDto();
            this.flights = go7Response.getAerocrs().getFlights().getFlight();
            resolvePassengerCounts();
        }

        private void resolvePassengerCounts() {
            Map<String, Object> reqParms = request.getAerocrs().getParms();
            if (reqParms != null) {
                adultsCount = parsePassengerCount(reqParms, KEY_ADULTS, 1);
                childCount = parsePassengerCount(reqParms, KEY_CHILD, 0);
                infantCount = parsePassengerCount(reqParms, KEY_INFANT, 0);
            }
        }

        private int parsePassengerCount(Map<String, Object> reqParms, String key, int defaultValue) {
            Object val = reqParms.get(key);
            if (val != null) {
                try {
                    return Integer.parseInt(val.toString());
                } catch (Exception e) {
                    // Ignore parsing exception, fallback to default count
                }
            }
            return defaultValue;
        }

        void initializeTopLevel() {
            response.setResponseId(UUID.randomUUID().toString().toUpperCase());
            response.setApiOwner(CARRIER_G7);
            response.setValidatingCarrier(CARRIER_G7);
        }

        void processFlights() {
            if (flights == null || flights.isEmpty()) {
                return;
            }
            processPricedOfferId();
            Flight firstFlight = flights.get(0);
            processFlightTimeLimits(firstFlight);

            List<OD> ods = new ArrayList<>();
            BigDecimal totalPrice = BigDecimal.ZERO;
            BigDecimal totalTax = BigDecimal.ZERO;

            for (int i = 0; i < flights.size(); i++) {
                Flight flight = flights.get(i);
                OD od = buildOd(flight, i + 1);
                ods.add(od);

                if (flight.getNetFare() != null) {
                    totalPrice = totalPrice.add(new BigDecimal(flight.getNetFare()));
                }
                if (flight.getTotaltax() != null) {
                    totalTax = totalTax.add(new BigDecimal(flight.getTotaltax()));
                }
            }
            response.setOds(ods);
            response.setTotalPrice(totalPrice);
            response.setTotalTaxes(totalTax);
        }

        private void processPricedOfferId() {
            StringBuilder idBuilder = new StringBuilder();
            Map<String, Object> reqParms = request.getAerocrs().getParms();

            for (int i = 0; i < flights.size(); i++) {
                Flight flight = flights.get(i);
                if (i > 0) {
                    idBuilder.append("*");
                }
                idBuilder.append(buildFlightOfferIdPart(flight, i + 1, reqParms));
            }

            this.pricedOfferId = idBuilder.toString();
            response.setPricedOfferId(pricedOfferId);
        }

        private String buildFlightOfferIdPart(Flight flight, int index, Map<String, Object> reqParms) {
            String flightId = getParamString(reqParms, KEY_FLIGHT_ID + index, String.valueOf(flight.getFlightid()));
            if (flightId == null && index == 1 && reqParms != null) {
                flightId = (String) reqParms.get(KEY_FLIGHT_ID);
            }

            String fareId = getParamString(reqParms, KEY_FARE_ID + index, "0");

            String tripTypeRaw = getParamString(reqParms, KEY_TRIP_TYPE, "OneWay");
            String tripType = "OW";
            if (tripTypeRaw.equalsIgnoreCase("Return") || tripTypeRaw.equalsIgnoreCase("RT")) {
                tripType = "RT";
            }

            Object adults = (reqParms != null) ? reqParms.get(KEY_ADULTS) : 1;
            Object child = (reqParms != null) ? reqParms.get(KEY_CHILD) : 0;
            Object infant = (reqParms != null) ? reqParms.get(KEY_INFANT) : 0;

            return flightId + "-" +
                    fareId + "-" +
                    flight.getFromcode() + "-" +
                    flight.getTocode() + "-" +
                    tripType + "-" +
                    adults + "-" +
                    child + "-" +
                    infant + "-" +
                    flight.getCurrency();
        }

        private String getParamString(Map<String, Object> reqParms, String key, String defaultValue) {
            if (reqParms != null && reqParms.get(key) != null) {
                return reqParms.get(key).toString();
            }
            return defaultValue;
        }

        private void processFlightTimeLimits(Flight firstFlight) {
            response.setCurrency(firstFlight.getCurrency());
            response.setValidatingCarrier(CARRIER_G7);

            DateTimeFormatter ndcTimeFormat = DateTimeFormatter.ofPattern("ddMMMuuuu HH:mm:ss", Locale.ENGLISH);
            java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
            response.setOfferPriceExpiration(now.plusMinutes(30).format(ndcTimeFormat));
            response.setPaymentTimeLimit(now.plusHours(24).format(ndcTimeFormat));
            response.setTicketedByTimeLimit(now.plusHours(24).format(ndcTimeFormat));
        }

        private OD buildOd(Flight flight, int odIndex) {
            OD od = new OD();
            od.setOdKey("OD" + odIndex);
            od.setOrigin(flight.getFromcode());
            od.setDestination(flight.getTocode());
            od.setOriginAirportName(flight.getFrom());
            od.setDestinationAirportName(flight.getTo());

            DateTimeFormatter targetDateFmt = DateTimeFormatter.ofPattern("ddMMMyyyy", Locale.ENGLISH);
            try {
                LocalDateTime depDT = LocalDateTime.parse(flight.getFlightdate() + " " + flight.getDepart(),
                        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
                od.setDepartureDate(depDT.format(targetDateFmt));
                od.setDepartureTime(flight.getDepart());

                LocalDateTime arrDT = LocalDateTime.parse(flight.getFlightdate() + " " + flight.getArrive(),
                        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
                if (arrDT.isBefore(depDT)) {
                    arrDT = arrDT.plusDays(1);
                }
                od.setArrivalDate(arrDT.format(targetDateFmt));
                od.setArrivalTime(flight.getArrive());

                Duration dur = Duration.between(depDT, arrDT);
                od.setJourneyTime(dur.toString());
            } catch (Exception e) {
                // Ignore parsing exception, fallback to flight date
                od.setDepartureDate(flight.getFlightdate());
            }

            od.setFlightNumber(flight.getNumber());
            od.setMarketingCarrierName(flight.getAirline());
            od.setMarketingCarrierCode(CARRIER_G7);

            String[] classParts = flight.getFlightClass() != null ? flight.getFlightClass().split("/")
                    : new String[] {};
            if (classParts.length > 0) {
                String cabin = classParts[0];
                if ("ECO".equalsIgnoreCase(cabin)) {
                    cabin = "ECONOMY";
                } else if ("BIZ".equalsIgnoreCase(cabin)) {
                    cabin = "BUSINESS";
                }
                od.setCabinType(cabin);
            }

            od.setRbdCode(null);
            od.setFareBasisCode(null);
            od.setSegmentId("SEG" + odIndex);
            od.setPriceClassId("PC" + odIndex);

            return od;
        }

        void populateOfferItems() {
            List<OfferPriceRspDto.OfferItemDto> offerItems = new ArrayList<>();
            Flight firstFlight = flights.get(0);

            addAdtOfferItem(offerItems, firstFlight);
            addCnnOfferItem(offerItems, firstFlight);
            addInfOfferItem(offerItems, firstFlight);

            response.setOfferItems(offerItems);
            calculateGrandTotals(offerItems);
        }

        private void addAdtOfferItem(List<OfferPriceRspDto.OfferItemDto> offerItems, Flight firstFlight) {
            if (adultsCount > 0) {
                List<String> adtRefs = new ArrayList<>();
                for (int k = 1; k <= adultsCount; k++) {
                    adtRefs.add("T" + k);
                }
                int itemIdx = offerItems.size() + 1;
                OfferPriceRspDto.OfferItemDto adtItem = createOfferItem(itemIdx, "ADT", adtRefs,
                        calculateAdtUnitTotal(), calculateAdtUnitTax(), firstFlight);
                offerItems.add(adtItem);
            }
        }

        private void addCnnOfferItem(List<OfferPriceRspDto.OfferItemDto> offerItems, Flight firstFlight) {
            if (childCount > 0) {
                List<String> cnnRefs = new ArrayList<>();
                for (int k = 1; k <= childCount; k++) {
                    cnnRefs.add("T" + (adultsCount + k));
                }
                int itemIdx = offerItems.size() + 1;
                OfferPriceRspDto.OfferItemDto cnnItem = createOfferItem(itemIdx, "CNN", cnnRefs,
                        calculateCnnUnitTotal(), calculateCnnUnitTax(), firstFlight);
                offerItems.add(cnnItem);
            }
        }

        private void addInfOfferItem(List<OfferPriceRspDto.OfferItemDto> offerItems, Flight firstFlight) {
            if (infantCount > 0) {
                List<String> infRefs = new ArrayList<>();
                for (int k = 1; k <= infantCount; k++) {
                    int parentRefIdx = (k <= adultsCount) ? k : ((k - 1) % adultsCount) + 1;
                    infRefs.add("T" + parentRefIdx + ".1");
                }
                int itemIdx = offerItems.size() + 1;
                OfferPriceRspDto.OfferItemDto infItem = createOfferItem(itemIdx, "INF", infRefs,
                        calculateInfUnitTotal(), BigDecimal.ZERO, firstFlight);
                offerItems.add(infItem);
            }
        }

        private void calculateGrandTotals(List<OfferPriceRspDto.OfferItemDto> offerItems) {
            BigDecimal grandTotal = BigDecimal.ZERO;
            BigDecimal grandTax = BigDecimal.ZERO;
            for (OfferPriceRspDto.OfferItemDto item : offerItems) {
                grandTotal = grandTotal.add(item.getTotalPrice());
                if (item.getTotalTax() != null && item.getTotalTax().getAmount() != null) {
                    grandTax = grandTax.add(item.getTotalTax().getAmount());
                }
            }
            response.setTotalPrice(grandTotal);
            response.setTotalTaxes(grandTax);
        }

        private BigDecimal calculateAdtUnitTotal() {
            BigDecimal total = BigDecimal.ZERO;
            for (Flight f : flights) {
                String fare = firstNonNull(
                        f.getAgtfareAdult(),
                        f.getRackfareAdult(),
                        f.getNetFare()
                );
                if (fare != null) {
                    total = total.add(new BigDecimal(fare));
                }
            }
            return total;
        }

        private BigDecimal calculateAdtUnitTax() {
            BigDecimal tax = BigDecimal.ZERO;
            for (Flight f : flights) {
                if (f.getTax() != null) {
                    try {
                        tax = tax.add(new BigDecimal(f.getTax()));
                    } catch (Exception e) {
                        // Ignore parsing error for specific flight tax
                    }
                } else if (f.getTotaltax() != null) {
                    tax = tax.add(new BigDecimal(f.getTotaltax()));
                }
            }
            return tax;
        }

        private BigDecimal calculateCnnUnitTotal() {
            BigDecimal total = BigDecimal.ZERO;
            for (Flight f : flights) {
                String fare = firstNonNull(
                        f.getAgtfareChild(),
                        f.getRackfareChild(),
                        f.getAgtfareAdult(),
                        f.getRackfareAdult(),
                        f.getNetFare()
                );
                if (fare != null) {
                    total = total.add(new BigDecimal(fare));
                }
            }
            return total;
        }

        private BigDecimal calculateCnnUnitTax() {
            return calculateAdtUnitTax();
        }

        private BigDecimal calculateInfUnitTotal() {
            BigDecimal total = BigDecimal.ZERO;
            for (Flight f : flights) {
                String fare = firstNonNull(
                        f.getAgtfareInfant(),
                        f.getRackfareInfant()
                );
                if (fare != null) {
                    total = total.add(new BigDecimal(fare));
                }
            }
            return total;
        }

        private String firstNonNull(String... values) {
            for (String val : values) {
                if (val != null) {
                    return val;
                }
            }
            return null;
        }

        void populatePriceClassList() {
            List<OfferPriceRspDto.PriceClassList> pclList = new ArrayList<>();
            for (int i = 0; i < flights.size(); i++) {
                Flight f = flights.get(i);
                OfferPriceRspDto.PriceClassList pcl = new OfferPriceRspDto.PriceClassList();
                pcl.setPriceClassId("PC" + (i + 1));
                pcl.setClassName(f.getFlightClass());

                String[] classParts = f.getFlightClass() != null ? f.getFlightClass().split("/") : new String[] {};
                String cabinCode = classParts.length > 0 ? classParts[0] : "ECO";
                if ("ECO".equalsIgnoreCase(cabinCode)) {
                    cabinCode = "ECONOMY";
                } else if ("BIZ".equalsIgnoreCase(cabinCode)) {
                    cabinCode = "BUSINESS";
                }
                pcl.setCabinTypeCode(cabinCode);

                List<OfferPriceRspDto.PriceClassList.Description> descs = new ArrayList<>();
                OfferPriceRspDto.PriceClassList.Description odDesc = new OfferPriceRspDto.PriceClassList.Description();
                odDesc.setOdKey("[OD" + (i + 1) + "]");
                descs.add(odDesc);

                if (f.getServices() != null) {
                    for (Map.Entry<String, Boolean> entry : f.getServices().entrySet()) {
                        OfferPriceRspDto.PriceClassList.Description d = new OfferPriceRspDto.PriceClassList.Description();
                        d.setText(entry.getKey() + ": " + entry.getValue());
                        descs.add(d);
                    }
                }
                pcl.setDescriptions(descs);
                pclList.add(pcl);
            }
            response.setPriceClassList(pclList);
        }

        private OfferPriceRspDto.OfferItemDto createOfferItem(int itemIndex, String ptc,
                List<String> paxRefs, BigDecimal unitTotal, BigDecimal unitTax, Flight flight) {

            OfferPriceRspDto.OfferItemDto item = new OfferPriceRspDto.OfferItemDto();
            item.setOfferItemId(pricedOfferId + "-" + itemIndex);
            item.setPtc(ptc);
            item.setPassengerIds(paxRefs);

            int count = paxRefs.size();
            BigDecimal total = unitTotal.multiply(new BigDecimal(count));
            BigDecimal tax = unitTax.multiply(new BigDecimal(count));

            item.setTotalPrice(total);

            // Breakdown
            OfferPriceRspDto.OfferItemDto.TotalFare tf = new OfferPriceRspDto.OfferItemDto.TotalFare();
            tf.setAmount(total);
            tf.setCurrency(flight.getCurrency());
            item.setTotalFare(tf);

            OfferPriceRspDto.OfferItemDto.BaseFare bf = new OfferPriceRspDto.OfferItemDto.BaseFare();
            bf.setAmount(total.subtract(tax));
            bf.setCurrency(flight.getCurrency());
            item.setBaseFare(bf);

            OfferPriceRspDto.OfferItemDto.TotalTax tt = new OfferPriceRspDto.OfferItemDto.TotalTax();
            tt.setAmount(tax);
            tt.setCurrency(flight.getCurrency());
            item.setTotalTax(tt);

            item.setTaxes(populateTaxList(flight, tax, unitTax, count));
            item.setBaggageAllowances(populateBaggageAllowances(flight, ptc, paxRefs));

            return item;
        }

        private List<OfferPriceRspDto.OfferItemDto.Tax> populateTaxList(Flight flight, BigDecimal tax, BigDecimal unitTax, int count) {
            List<OfferPriceRspDto.OfferItemDto.Tax> taxList = new ArrayList<>();
            if (hasTaxBreakdown(flight)) {
                Map<String, String> breakdown = flight.getRawFareObject().getRackFare().getTaxBreakdown();
                BigDecimal scaleFactor = calculateScaleFactor(breakdown, unitTax);
                buildTaxesFromBreakdown(taxList, breakdown, scaleFactor, tax, count, flight.getCurrency());
            } else if (tax.compareTo(BigDecimal.ZERO) > 0) {
                OfferPriceRspDto.OfferItemDto.Tax t1 = new OfferPriceRspDto.OfferItemDto.Tax();
                t1.setCode("TAX");
                t1.setAmount(tax);
                t1.setCurrency(flight.getCurrency());
                t1.setDescription("Total Taxes");
                taxList.add(t1);
            }
            return taxList;
        }

        private boolean hasTaxBreakdown(Flight flight) {
            return flight.getRawFareObject() != null &&
                    flight.getRawFareObject().getRackFare() != null &&
                    flight.getRawFareObject().getRackFare().getTaxBreakdown() != null;
        }

        private BigDecimal calculateScaleFactor(Map<String, String> breakdown, BigDecimal unitTax) {
            BigDecimal breakdownSum = BigDecimal.ZERO;
            for (String val : breakdown.values()) {
                try {
                    breakdownSum = breakdownSum.add(new BigDecimal(val));
                } catch (Exception e) {
                    // Ignore tax breakdown sum parsing error
                }
            }
            if (breakdownSum.compareTo(BigDecimal.ZERO) > 0) {
                return unitTax.divide(breakdownSum, 10, java.math.RoundingMode.HALF_UP);
            }
            return BigDecimal.ONE;
        }

        private void buildTaxesFromBreakdown(List<OfferPriceRspDto.OfferItemDto.Tax> taxList,
                                             Map<String, String> breakdown, BigDecimal scaleFactor,
                                             BigDecimal tax, int count, String currency) {
            for (Map.Entry<String, String> taxEntry : breakdown.entrySet()) {
                if (tax.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                OfferPriceRspDto.OfferItemDto.Tax taxesItem = new OfferPriceRspDto.OfferItemDto.Tax();

                BigDecimal rawUnitTax;
                try {
                    rawUnitTax = new BigDecimal(taxEntry.getValue());
                } catch (Exception e) {
                    // Ignore parsing error, fallback to zero
                    rawUnitTax = BigDecimal.ZERO;
                }

                BigDecimal scaledUnitTax = rawUnitTax.multiply(scaleFactor).setScale(2, java.math.RoundingMode.HALF_UP);
                BigDecimal totalTaxAmount = scaledUnitTax.multiply(new BigDecimal(count));

                taxesItem.setCurrency(currency);
                String key = taxEntry.getKey();
                if (key.length() <= 3) {
                    taxesItem.setCode(key);
                    taxesItem.setDescription("Tax " + key);
                } else {
                    taxesItem.setCode("TAX");
                    taxesItem.setDescription(key);
                }

                taxesItem.setAmount(totalTaxAmount);
                taxList.add(taxesItem);
            }
        }

        private List<OfferPriceRspDto.OfferItemDto.BaggageAllowance> populateBaggageAllowances(Flight flight, String ptc, List<String> paxRefs) {
            List<OfferPriceRspDto.OfferItemDto.BaggageAllowance> bags = new ArrayList<>();
            if (flight.getServices() != null) {
                if (!Boolean.FALSE.equals(flight.getServices().get("CheckedInBaggage"))) {
                    bags.add(createBaggageAllowance(ptc, paxRefs, "Checked-In", "1", "CHECKED IN ALLOWANCE"));
                }
                if (!Boolean.FALSE.equals(flight.getServices().get("HandBaggage"))) {
                    bags.add(createBaggageAllowance(ptc, paxRefs, "Carry On", "1", "CARRY ON ALLOWANCE"));
                }
            }
            return bags;
        }

        private OfferPriceRspDto.OfferItemDto.BaggageAllowance createBaggageAllowance(
                String ptc, List<String> paxRefs, String category, String quantity, String description) {
            OfferPriceRspDto.OfferItemDto.BaggageAllowance bag = new OfferPriceRspDto.OfferItemDto.BaggageAllowance();
            bag.setBaggageAllowanceId(UUID.randomUUID().toString());
            bag.setPtc(ptc);
            bag.setPassengerId(paxRefs);
            bag.setCategory(category);
            bag.setQuantity(quantity);

            OfferPriceRspDto.OfferItemDto.BaggageAllowance.DescriptionDTO descDto = new OfferPriceRspDto.OfferItemDto.BaggageAllowance.DescriptionDTO();
            descDto.setDescription(description);
            bag.setDescriptions(Collections.singletonList(descDto));
            return bag;
        }
    }
}
