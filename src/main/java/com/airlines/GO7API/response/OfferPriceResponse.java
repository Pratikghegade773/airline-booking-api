package com.airlines.GO7API.response;

import com.airlines.GO7API.request.OfferPriceReq;
import com.airlines.GO7API.requestDto.OfferPriceReqDto;
import com.airlines.GO7API.responseDto.OfferPriceRspDto;
import com.airlines.GO7API.responseGo7.OfferPriceRspGo7Dto;
import com.airlines.GO7API.error.ErrorRsp;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class OfferPriceResponse {

    public static Object generateResponse(OfferPriceReq request, OfferPriceReqDto offerPriceReqDto)
            throws DatatypeConfigurationException, IOException, InterruptedException, ParseException {
        // Unmarshal the response from the API
        Object responseObj = request.unmarshal();

        OfferPriceRspGo7Dto go7Response = null;

        // Check for error response or success response
        try {
            if (responseObj instanceof ErrorRsp) {
                return responseObj;
            } else if (responseObj instanceof OfferPriceRspGo7Dto) {
                go7Response = (OfferPriceRspGo7Dto) responseObj;
            } else if (responseObj instanceof LinkedHashMap) {
                // Fallback if Jackson returns Map instead of DTO
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        false);
                go7Response = mapper.convertValue(responseObj, OfferPriceRspGo7Dto.class);
            }
        } catch (Exception e) {
            System.out.println("Failed to parse OfferPriceResp from the response: " + responseObj);
            e.printStackTrace();
            throw new RuntimeException("Failed to parse OfferPriceResp from the response", e);
        }

        if (go7Response == null || go7Response.getAerocrs() == null
                || go7Response.getAerocrs().getGetFlight() == null) {
            System.out.println("OfferPriceResp is null or missing required fields. Raw Response: " + responseObj);
            return new ErrorRsp(); // Or return a specific error
        }

        OfferPriceRspDto response = new OfferPriceRspDto();

        // Hardcoded/Generated Top-Level Fields
        response.setResponseId(UUID.randomUUID().toString().toUpperCase());
        response.setApiOwner("G7"); // As per user request/Airshop
        response.setValidatingCarrier("G7"); // Placeholder or derive from first flight

        List<OfferPriceRspGo7Dto.Flight> flights = go7Response.getAerocrs().getGetFlight().getFlight();

        if (flights != null && !flights.isEmpty()) {
            OfferPriceRspGo7Dto.Flight firstFlight = flights.get(0);

            // Generate a PricedOfferId
            // String pricedOfferId = response.getResponseId() + "-1";
            StringBuilder idBuilder = new StringBuilder();
            Map<String, Object> reqParms = request.getAerocrs().getParms();

            for (int i = 0; i < flights.size(); i++) {
                OfferPriceRspGo7Dto.Flight flight = flights.get(i);
                int index = i + 1;

                // flightid
                String flightId = (reqParms != null && reqParms.get("flightid" + index) != null)
                        ? reqParms.get("flightid" + index).toString()
                        : flight.getFlightid(); // fallback
                if (flightId == null && index == 1 && reqParms != null)
                    flightId = (String) reqParms.get("flightid");

                // fareid
                String fareId = (reqParms != null && reqParms.get("fareid" + index) != null)
                        ? reqParms.get("fareid" + index).toString()
                        : "0"; // fallback

                // tripType
                String tripTypeRaw = (reqParms != null && reqParms.get("triptype") != null)
                        ? reqParms.get("triptype").toString()
                        : "OneWay";
                String tripType = "OW";
                if (tripTypeRaw.equalsIgnoreCase("Return") || tripTypeRaw.equalsIgnoreCase("RT")) {
                    tripType = "RT";
                }

                // counts
                Object adults = (reqParms != null) ? reqParms.get("adults") : 1;
                Object child = (reqParms != null) ? reqParms.get("child") : 0;
                Object infant = (reqParms != null) ? reqParms.get("infant") : 0;

                if (i > 0)
                    idBuilder.append("*");
                idBuilder.append(flightId)
                        .append("-").append(fareId)
                        .append("-").append(flight.getFromcode())
                        .append("-").append(flight.getTocode())
                        .append("-").append(tripType)
                        .append("-").append(adults)
                        .append("-").append(child)
                        .append("-").append(infant)
                        .append("-").append(flight.getCurrency());
            }

            String pricedOfferId = idBuilder.toString();
            response.setPricedOfferId(pricedOfferId);

            response.setCurrency(firstFlight.getCurrency());
            response.setValidatingCarrier(firstFlight.getAirline());

            // Set time limits (Calculated)
            DateTimeFormatter ndcTimeFormat = DateTimeFormatter.ofPattern("ddMMMyyyy HH:mm:ss", Locale.ENGLISH);
            LocalDateTime now = LocalDateTime.now();
            response.setOfferPriceExpiration(now.plusMinutes(20).format(ndcTimeFormat));
            response.setPaymentTimeLimit(now.plusDays(2).format(ndcTimeFormat));
            response.setTicketedByTimeLimit(now.plusDays(2).format(ndcTimeFormat));

            // ODs Mapping
            List<OfferPriceRspDto.OD> ods = new ArrayList<>();
            BigDecimal totalPrice = BigDecimal.ZERO;
            BigDecimal totalTax = BigDecimal.ZERO;

            for (int i = 0; i < flights.size(); i++) {
                OfferPriceRspGo7Dto.Flight flight = flights.get(i);
                OfferPriceRspDto.OD od = new OfferPriceRspDto.OD();

                od.setOdKey("OD" + (i + 1));
                od.setOrigin(flight.getFromcode());
                od.setDestination(flight.getTocode());
                od.setOriginAirportName(flight.getFromLocation()); // Mapping 'from' to Name
                od.setDestinationAirportName(flight.getToLocation()); // Mapping 'to' to Name

                // flightdate="2026/02/20", depart="16:30"
                // NDC format: 20Feb2026
                DateTimeFormatter sourceDateFmt = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                DateTimeFormatter targetDateFmt = DateTimeFormatter.ofPattern("ddMMMyyyy", Locale.ENGLISH);

                try {
                    LocalDateTime depDT = LocalDateTime.parse(flight.getFlightdate() + " " + flight.getDepart(),
                            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
                    od.setDepartureDate(depDT.format(targetDateFmt));
                    od.setDepartureTime(flight.getDepart());

                    // Arrival Date/Time calculation
                    LocalDateTime arrDT = LocalDateTime.parse(flight.getFlightdate() + " " + flight.getArrive(),
                            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
                    if (arrDT.isBefore(depDT)) {
                        arrDT = arrDT.plusDays(1); // Assume next day if time is earlier
                    }
                    od.setArrivalDate(arrDT.format(targetDateFmt));
                    od.setArrivalTime(flight.getArrive());

                    Duration dur = Duration.between(depDT, arrDT);
                    od.setJourneyTime(dur.toString()); // PT...

                } catch (Exception e) {
                    od.setDepartureDate(flight.getFlightdate()); // Fallback
                }

                od.setFlightNumber(flight.getNumber()); // Extract number
                od.setMarketingCarrierName(flight.getAirline());
                od.setMarketingCarrierCode("G7"); // Placeholder or extract
                // od.setEquipment("Boeing"); // Placeholder

                // Class/Cabin
                // Source: "ECO/Y/Flex Plus"
                String[] classParts = flight.getFlightClass() != null ? flight.getFlightClass().split("/")
                        : new String[] {};
                if (classParts.length > 0)
                    od.setCabinType(classParts[0]); // ECO

                // RBD and FareBasiscode
                od.setRbdCode(null);
                od.setFareBasisCode(null);

                // IDs
                od.setSegmentId("SEG" + (i + 1));
                od.setPriceClassId("PC" + (i + 1));

                ods.add(od);

                // Price Accumulation
                if (flight.getNetFare() != null)
                    totalPrice = totalPrice.add(new BigDecimal(flight.getNetFare()));
                if (flight.getTotaltax() != null)
                    totalTax = totalTax.add(new BigDecimal(flight.getTotaltax()));
            }
            response.setOds(ods);
            response.setTotalPrice(totalPrice);
            response.setTotalTaxes(totalTax);

            // Offer Items
            List<OfferPriceRspDto.OfferItemDto> offerItems = new ArrayList<>();
            int itemIdx = 1;

            // ADULTS
            int adultsCount = 1;
            if (reqParms != null && reqParms.get("adults") != null) {
                try {
                    adultsCount = Integer.parseInt(reqParms.get("adults").toString());
                } catch (Exception e) {
                }
            }

            if (adultsCount > 0) {
                List<String> adtRefs = new ArrayList<>();
                for (int k = 1; k <= adultsCount; k++) {
                    adtRefs.add("T" + k);
                }

                // Calculate ADT Unit Price (Sum of all flights)
                BigDecimal adtUnitTotal = BigDecimal.ZERO;
                BigDecimal adtUnitTax = BigDecimal.ZERO;

                for (OfferPriceRspGo7Dto.Flight f : flights) {
                    // Prefer agtfare_adult if available, else rackfare_adult, else net_fare?
                    // Airshop logic uses: new BigDecimal(flightClass.getFare().getAdultFare())
                    // Here we have getRackfareAdult, getAgtfareAdult..
                    // Let's use getNetFare() logic as per previous code, BUT per pax type?
                    // Previous code used flight.getNetFare() for TOTAL price.
                    // We need unit price.
                    // Let's try to find specific fare fields first.
                    String fareStr = f.getAgtfareAdult();
                    if (fareStr == null)
                        fareStr = f.getRackfareAdult();
                    if (fareStr == null)
                        fareStr = f.getNetFare(); // Fallback

                    if (fareStr != null)
                        adtUnitTotal = adtUnitTotal.add(new BigDecimal(fareStr));

                    // Tax: Use specific 'tax' field as requested
                    if (f.getTax() != null) {
                        try {
                            adtUnitTax = adtUnitTax.add(new BigDecimal(f.getTax()));
                        } catch (Exception e) {
                            // ignore parsing error
                        }
                    } else if (f.getTotaltax() != null) {
                        // Fallback to totaltax if tax is missing? Or prefer tax strictly?
                        // User request implies strict 'tax' usage (25.00), but fallback is safer
                        adtUnitTax = adtUnitTax.add(new BigDecimal(f.getTotaltax()));
                    }
                }

                OfferPriceRspDto.OfferItemDto adtItem = createOfferItem(pricedOfferId, itemIdx++, "ADT", adtRefs,
                        adtUnitTotal, adtUnitTax, firstFlight);
                offerItems.add(adtItem);
                // No need to add to overall total here, we'll sum up items at the end
            }

            // CHILDREN
            int childCount = 0;
            if (reqParms != null && reqParms.get("child") != null) {
                try {
                    childCount = Integer.parseInt(reqParms.get("child").toString());
                } catch (Exception e) {
                }
            }

            if (childCount > 0) {
                List<String> cnnRefs = new ArrayList<>();
                for (int k = 1; k <= childCount; k++) {
                    int paxRefIdx = adultsCount + k;
                    cnnRefs.add("T" + paxRefIdx);
                }

                BigDecimal cnnUnitTotal = BigDecimal.ZERO;
                BigDecimal cnnUnitTax = BigDecimal.ZERO;

                for (OfferPriceRspGo7Dto.Flight f : flights) {
                    String fareStr = f.getAgtfareChild();
                    if (fareStr == null)
                        fareStr = f.getRackfareChild();
                    // If null, fallback to Adult? Airshop logic mimics that.
                    if (fareStr == null) {
                        fareStr = f.getAgtfareAdult();
                        if (fareStr == null)
                            fareStr = f.getRackfareAdult();
                        if (fareStr == null)
                            fareStr = f.getNetFare();
                    }

                    if (fareStr != null)
                        cnnUnitTotal = cnnUnitTotal.add(new BigDecimal(fareStr));

                    // Tax: Use specific 'tax' field as requested
                    if (f.getTax() != null) {
                        try {
                            cnnUnitTax = cnnUnitTax.add(new BigDecimal(f.getTax()));
                        } catch (Exception e) {
                            // ignore parsing error
                        }
                    } else if (f.getTotaltax() != null) {
                        cnnUnitTax = cnnUnitTax.add(new BigDecimal(f.getTotaltax()));
                    }
                }

                OfferPriceRspDto.OfferItemDto cnnItem = createOfferItem(pricedOfferId, itemIdx++, "CNN", cnnRefs,
                        cnnUnitTotal, cnnUnitTax, firstFlight);
                offerItems.add(cnnItem);
            }

            // INFANTS
            int infantCount = 0;
            if (reqParms != null && reqParms.get("infant") != null) {
                try {
                    infantCount = Integer.parseInt(reqParms.get("infant").toString());
                } catch (Exception e) {
                }
            }

            if (infantCount > 0) {
                List<String> infRefs = new ArrayList<>();
                for (int k = 1; k <= infantCount; k++) {
                    int parentRefIdx = (k <= adultsCount) ? k : ((k - 1) % adultsCount) + 1;
                    infRefs.add("T" + parentRefIdx + ".1");
                }

                BigDecimal infUnitTotal = BigDecimal.ZERO;
                BigDecimal infUnitTax = BigDecimal.ZERO;

                for (OfferPriceRspGo7Dto.Flight f : flights) {
                    String fareStr = f.getAgtfareInfant();
                    if (fareStr == null)
                        fareStr = f.getRackfareInfant();

                    if (fareStr != null)
                        infUnitTotal = infUnitTotal.add(new BigDecimal(fareStr));
                    // Infant Tax usually 0, but if available add it
                }

                OfferPriceRspDto.OfferItemDto infItem = createOfferItem(pricedOfferId, itemIdx++, "INF", infRefs,
                        infUnitTotal, infUnitTax, firstFlight);
                offerItems.add(infItem);
            }

            response.setOfferItems(offerItems);

            // Recalculate Grand Total from Items
            BigDecimal grandTotal = BigDecimal.ZERO;
            BigDecimal grandTax = BigDecimal.ZERO;
            for (OfferPriceRspDto.OfferItemDto item : offerItems) {
                grandTotal = grandTotal.add(item.getTotalPrice());
                // Accessing tax from TotalTax object
                if (item.getTotalTax() != null && item.getTotalTax().getAmount() != null) {
                    grandTax = grandTax.add(item.getTotalTax().getAmount());
                }
            }
            response.setTotalPrice(grandTotal);
            response.setTotalTaxes(grandTax);

            // Price Class List
            List<OfferPriceRspDto.PriceClassList> pclList = new ArrayList<>();
            for (int i = 0; i < flights.size(); i++) {
                OfferPriceRspGo7Dto.Flight f = flights.get(i);
                OfferPriceRspDto.PriceClassList pcl = new OfferPriceRspDto.PriceClassList();
                pcl.setPriceClassId("PC" + (i + 1));
                pcl.setClassName(f.getFlightClass());

                // Parse Cabin Type from Flight Class if possible, or default
                String[] classParts = f.getFlightClass() != null ? f.getFlightClass().split("/") : new String[] {};
                pcl.setCabinTypeCode(classParts.length > 0 ? classParts[0] : "ECO");

                List<OfferPriceRspDto.PriceClassList.Description> descs = new ArrayList<>();

                // Add OD Key Description
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

        return response;
    }

    private static OfferPriceRspDto.OfferItemDto createOfferItem(String pricedOfferId, int itemIndex, String ptc,
            List<String> paxRefs, BigDecimal unitTotal, BigDecimal unitTax, OfferPriceRspGo7Dto.Flight flight) {

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

        // Taxes List
        List<OfferPriceRspDto.OfferItemDto.Tax> taxList = new ArrayList<>();
        if (flight.getRawFareObject() != null &&
                flight.getRawFareObject().getRackFare() != null &&
                flight.getRawFareObject().getRackFare().getTaxBreakdown() != null) {

            Map<String, String> breakdown = flight.getRawFareObject().getRackFare().getTaxBreakdown();

            // 1. Calculate sum of breakdown taxes to determine scaling factor
            BigDecimal breakdownSum = BigDecimal.ZERO;
            for (String val : breakdown.values()) {
                try {
                    breakdownSum = breakdownSum.add(new BigDecimal(val));
                } catch (Exception e) {
                }
            }

            BigDecimal scaleFactor = BigDecimal.ONE;
            if (breakdownSum.compareTo(BigDecimal.ZERO) > 0) {
                // unitTax is passed in
                scaleFactor = unitTax.divide(breakdownSum, 10, java.math.RoundingMode.HALF_UP);
            }

            for (Map.Entry<String, String> taxEntry : breakdown.entrySet()) {
                if (tax.compareTo(BigDecimal.ZERO) <= 0)
                    continue;

                OfferPriceRspDto.OfferItemDto.Tax taxesItem = new OfferPriceRspDto.OfferItemDto.Tax();

                BigDecimal rawUnitTax;
                try {
                    rawUnitTax = new BigDecimal(taxEntry.getValue());
                } catch (Exception e) {
                    rawUnitTax = BigDecimal.ZERO;
                }

                // Apply Scaling to get accurate Unit Tax
                BigDecimal scaledUnitTax = rawUnitTax.multiply(scaleFactor).setScale(2, java.math.RoundingMode.HALF_UP);
                BigDecimal totalTaxAmount = scaledUnitTax.multiply(new BigDecimal(count));

                taxesItem.setCurrency(flight.getCurrency());
                String key = taxEntry.getKey();
                if (key.length() <= 3) {
                    taxesItem.setCode(key);
                    taxesItem.setDescription("Tax " + key);
                } else {
                    taxesItem.setCode("TAX");
                    taxesItem.setDescription(key);
                }

                taxesItem.setAmount(totalTaxAmount);
                // Total field in Taxes DTO should be the same as amount (total for this
                // category)
                taxesItem.setTotal(totalTaxAmount);

                taxList.add(taxesItem);
            }
        } else if (tax.compareTo(BigDecimal.ZERO) > 0) {
            OfferPriceRspDto.OfferItemDto.Tax t1 = new OfferPriceRspDto.OfferItemDto.Tax();
            t1.setCode("TAX");
            t1.setAmount(tax);
            t1.setTotal(tax);
            t1.setCurrency(flight.getCurrency());
            t1.setDescription("Total Taxes");
            taxList.add(t1);
        }
        item.setTaxes(taxList);

        // Baggage (Dynamic based on services or default if service map exists)
        List<OfferPriceRspDto.OfferItemDto.BaggageAllowance> bags = new ArrayList<>();

        if (flight.getServices() != null) {
            // Checked-In Baggage - Default to true if not explicitly false and service map
            // is present
            if (!Boolean.FALSE.equals(flight.getServices().get("CheckedInBaggage"))) {
                OfferPriceRspDto.OfferItemDto.BaggageAllowance bag = new OfferPriceRspDto.OfferItemDto.BaggageAllowance();
                bag.setBaggageAllowanceId(UUID.randomUUID().toString());
                bag.setPtc(ptc);
                bag.setPassengerId(paxRefs);
                bag.setCategory("Checked-In");
                bag.setQuantity("1");

                OfferPriceRspDto.OfferItemDto.BaggageAllowance.DescriptionDTO descDto = new OfferPriceRspDto.OfferItemDto.BaggageAllowance.DescriptionDTO();
                descDto.setDescription("CHECKED IN ALLOWANCE");
                bag.setDescriptions(Collections.singletonList(descDto));

                bags.add(bag);
            }

            // Carry-On Baggage - Default to true if not explicitly false and service map is
            // present
            if (!Boolean.FALSE.equals(flight.getServices().get("HandBaggage"))) {
                OfferPriceRspDto.OfferItemDto.BaggageAllowance bag = new OfferPriceRspDto.OfferItemDto.BaggageAllowance();
                bag.setBaggageAllowanceId(UUID.randomUUID().toString());
                bag.setPtc(ptc);
                bag.setPassengerId(paxRefs);
                bag.setCategory("Carry On");
                bag.setQuantity("1");

                OfferPriceRspDto.OfferItemDto.BaggageAllowance.DescriptionDTO descDto = new OfferPriceRspDto.OfferItemDto.BaggageAllowance.DescriptionDTO();
                descDto.setDescription("CARRY ON ALLOWANCE");
                bag.setDescriptions(Collections.singletonList(descDto));

                bags.add(bag);
            }
        }
        item.setBaggageAllowances(bags);

        return item;
    }
}
