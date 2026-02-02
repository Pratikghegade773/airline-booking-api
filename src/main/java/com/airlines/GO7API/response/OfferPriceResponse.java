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
        if (responseObj instanceof ErrorRsp) {
            return responseObj;
        } else if (responseObj instanceof OfferPriceRspGo7Dto) {
            go7Response = (OfferPriceRspGo7Dto) responseObj;
        } else if (responseObj instanceof LinkedHashMap) {
            // Fallback if Jackson returns Map instead of DTO
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            go7Response = mapper.convertValue(responseObj, OfferPriceRspGo7Dto.class);
        }

        if (go7Response == null || go7Response.getAerocrs() == null
                || go7Response.getAerocrs().getGetFlight() == null) {
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
            String pricedOfferId = response.getResponseId() + "-1";
            response.setPricedOfferId(pricedOfferId);

            response.setCurrency(firstFlight.getCurrency());
            response.setValidatingCarrier(firstFlight.getAirline());

            // Set time limits (Calculated or Defaults)
            // Example format: 30Jan2026 16:19:52
            DateTimeFormatter ndcTimeFormat = DateTimeFormatter.ofPattern("ddMMMyyyy HH:mm:ss", Locale.ENGLISH);
            LocalDateTime now = LocalDateTime.now();
            // response.setOfferPriceExpiration(now.plusMinutes(20).format(ndcTimeFormat));
            // response.setPaymentTimeLimit(now.plusDays(2).format(ndcTimeFormat));
            // response.setTicketedByTimeLimit(now.plusDays(2).format(ndcTimeFormat));

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

                    // Arrival Date/Time calculation (assuming simple case same day if not provided,
                    // or calculate duration)
                    // The source has "arrive" time. We might need to handle next day.
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

                od.setFlightNumber(flight.getNumber().replaceAll("[^0-9]", "")); // Extract number
                od.setMarketingCarrierName(flight.getAirline());
                od.setMarketingCarrierCode("G7"); // Placeholder or extract
                od.setEquipment("Boeing"); // Placeholder, source doesn't have specific aircraft type in this DTO?

                // Class/Cabin
                // Source: "ECO/Y/Flex Plus"
                String[] classParts = flight.getFlightClass() != null ? flight.getFlightClass().split("/")
                        : new String[] {};
                if (classParts.length > 0)
                    od.setCabinType(classParts[0]); // ECO
                // User requested to NOT map RBD and FareBasisCode
                // if (classParts.length > 1) od.setRbdCode(classParts[1]); // Y
                // if (classParts.length > 2) od.setFareBasisCode(classParts[2]); // Flex Plus

                // IDs
                od.setSegmentId("SEG" + (i + 1));
                od.setPriceClassId("PC" + (i + 1));

                ods.add(od);

                // Price Accumulation (Simplified for ADT 1 pax)
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
            OfferPriceRspDto.OfferItemDto item = new OfferPriceRspDto.OfferItemDto();
            item.setOfferItemId(pricedOfferId + "-1");
            item.setPtc("ADT");
            item.setPassengerIds(Collections.singletonList("T1"));
            item.setTotalPrice(totalPrice);

            // Breakdown
            OfferPriceRspDto.OfferItemDto.TotalFare tf = new OfferPriceRspDto.OfferItemDto.TotalFare();
            tf.setAmount(totalPrice);
            tf.setCurrency(firstFlight.getCurrency());
            item.setTotalFare(tf);

            OfferPriceRspDto.OfferItemDto.BaseFare bf = new OfferPriceRspDto.OfferItemDto.BaseFare();
            bf.setAmount(totalPrice.subtract(totalTax));
            bf.setCurrency(firstFlight.getCurrency());
            item.setBaseFare(bf);

            OfferPriceRspDto.OfferItemDto.TotalTax tt = new OfferPriceRspDto.OfferItemDto.TotalTax();
            tt.setAmount(totalTax);
            tt.setCurrency(firstFlight.getCurrency());
            item.setTotalTax(tt);

            // Taxes List (Dummy for now based on total, or can split if logic known)
            List<OfferPriceRspDto.OfferItemDto.Tax> taxList = new ArrayList<>();
            OfferPriceRspDto.OfferItemDto.Tax t1 = new OfferPriceRspDto.OfferItemDto.Tax();
            t1.setCode("TAX");
            t1.setAmount(totalTax);
            t1.setCurrency(firstFlight.getCurrency());
            t1.setDescription("Total Taxes");
            taxList.add(t1);
            item.setTaxes(taxList);

            // Baggage (Dynamic based on services)
            List<OfferPriceRspDto.OfferItemDto.BaggageAllowance> bags = new ArrayList<>();

            if (firstFlight.getServices() != null) {
                // Checked-In Baggage
                if (Boolean.TRUE.equals(firstFlight.getServices().get("CheckedInBaggage"))) {
                    OfferPriceRspDto.OfferItemDto.BaggageAllowance bag = new OfferPriceRspDto.OfferItemDto.BaggageAllowance();
                    bag.setCategory("Checked-In");
                    bag.setQuantity("1"); // Default quantity if service exists but no specific weight info
                    bags.add(bag);
                }

                // Carry-On Baggage
                if (Boolean.TRUE.equals(firstFlight.getServices().get("HandBaggage"))) {
                    OfferPriceRspDto.OfferItemDto.BaggageAllowance bag = new OfferPriceRspDto.OfferItemDto.BaggageAllowance();
                    bag.setCategory("Carry On");
                    bag.setQuantity("1");
                    bags.add(bag);
                }
            }
            item.setBaggageAllowances(bags);

            offerItems.add(item);
            response.setOfferItems(offerItems);

            // Price Class List
            List<OfferPriceRspDto.PriceClassList> pclList = new ArrayList<>();
            for (int i = 0; i < flights.size(); i++) {
                OfferPriceRspGo7Dto.Flight f = flights.get(i);
                OfferPriceRspDto.PriceClassList pcl = new OfferPriceRspDto.PriceClassList();
                pcl.setPriceClassId("PC" + (i + 1));
                pcl.setClassName(f.getFlightClass());

                List<OfferPriceRspDto.PriceClassList.Description> descs = new ArrayList<>();
                if (f.getServices() != null) {
                    for (Map.Entry<String, Boolean> entry : f.getServices().entrySet()) {
                        // Map all services with their status (true/false)
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
}
