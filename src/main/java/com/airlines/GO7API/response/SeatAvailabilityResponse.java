package com.airlines.GO7API.response;

import com.airlines.GO7API.responseDto.SeatAvailabilityRspDto;
import com.airlines.GO7API.responseGo7.SeatAvailabilityRspGo7Dto;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class SeatAvailabilityResponse {

    public static SeatAvailabilityRspDto mapToSeatAvailabilityRspDto(SeatAvailabilityRspGo7Dto go7Rsp, String orderId,
            com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto bookingRsp) {
        SeatAvailabilityRspDto rsp = new SeatAvailabilityRspDto();

        if (go7Rsp == null || go7Rsp.getAerocrs() == null || !go7Rsp.getAerocrs().isSuccess()
                || go7Rsp.getAerocrs().getSeatMapFare() == null) {
            rsp.setResponseId(UUID.randomUUID().toString());
            return rsp;
        }

        SeatAvailabilityRspGo7Dto.SeatMapFare seatMap = go7Rsp.getAerocrs().getSeatMapFare();
        String currency = seatMap.getCurrency();

        // 1. Root Fields
        rsp.setResponseId(UUID.randomUUID().toString());
        rsp.setOfferId(rsp.getResponseId() + "-1");
        rsp.setOrderId(orderId);

        // Extract from Booking Response or Fallback
        String validatingCarrier = "G7"; // Default
        String apiOwner = "G7"; // Default

        if (bookingRsp != null && bookingRsp.getAerocrs() != null && bookingRsp.getAerocrs().getBooking() != null) {
            com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.Booking booking = bookingRsp.getAerocrs()
                    .getBooking();

            // Extract Carrier from first flight if available
            if (booking.getItems() != null && booking.getItems().getFlight() != null
                    && !booking.getItems().getFlight().isEmpty()) {
                com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.Flight flight = booking.getItems().getFlight()
                        .get(0);
                if (flight.getAirlineICAOcode() != null) {
                    validatingCarrier = flight.getAirlineICAOcode();
                    apiOwner = flight.getAirlineICAOcode();
                } else if (flight.getAirlinedesignator() != null) {
                    // Fallback to designator if ICAO not present (though example used WY which
                    // looks like IATA/Designator, user prompt said WY)
                    validatingCarrier = flight.getAirlinedesignator();
                    apiOwner = flight.getAirlinedesignator();
                }
            }
        }

        rsp.setApiOwner(apiOwner);
        rsp.setValidatingCarrier(validatingCarrier);

        // Offer Expiration (Now + 30 mins)
        rsp.setOfferExpiration(LocalDateTime.now().plusMinutes(30).format(DateTimeFormatter.ISO_DATE_TIME));

        // 2. Offer Items
        List<SeatAvailabilityRspDto.OfferItem> offerItems = new ArrayList<>();

        // Create one OfferItem
        SeatAvailabilityRspDto.OfferItem offerItem = new SeatAvailabilityRspDto.OfferItem();
        offerItem.setOfferItemId(rsp.getOfferId() + "-1");
        offerItem.setCurrency(currency);

        // Dynamic Segment Refs and Pax Refs
        List<String> segmentRefs = new ArrayList<>();
        List<String> paxRefs = new ArrayList<>();
        List<String> givenNames = new ArrayList<>();

        if (bookingRsp != null && bookingRsp.getAerocrs() != null && bookingRsp.getAerocrs().getBooking() != null) {
            com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.Booking booking = bookingRsp.getAerocrs()
                    .getBooking();

            // Segments
            if (booking.getItems() != null && booking.getItems().getFlight() != null) {
                int segCount = 1;
                for (com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.Flight flight : booking.getItems()
                        .getFlight()) {
                    // Generating IDs as S1, S2 etc to match typical pattern, or extracting if
                    // available.
                    // Go7 doesn't seem to have "segmentId" specifically in flight structure shown,
                    // using generated S<index>
                    segmentRefs.add("S" + segCount++);
                }
            } else {
                segmentRefs.add("S1");
            }

            // Passengers
            if (booking.getPassengers() != null && booking.getPassengers().getPassenger() != null) {
                int paxCount = 1;
                List<String> adtRefs = new ArrayList<>();
                for (com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.Passenger pax : booking.getPassengers()
                        .getPassenger()) {
                    
                    String rawTitle = pax.getPaxtitle() != null ? pax.getPaxtitle().toUpperCase().replace(".", "") : "MR";
                    boolean isInfantByTitle = rawTitle.contains("INF");

                    String assignedPtc = "ADT";
                    if ("CHILD".equalsIgnoreCase(pax.getPaxtype())) assignedPtc = "CNN";
                    if ("INFANT".equalsIgnoreCase(pax.getPaxtype()) || isInfantByTitle) assignedPtc = "INF";

                    String paxId;
                    if ("INF".equals(assignedPtc)) {
                        if (!adtRefs.isEmpty()) {
                            paxId = adtRefs.get(adtRefs.size() - 1) + ".1";
                        } else {
                            paxId = "T" + paxCount++ + ".1";
                        }
                    } else {
                        paxId = "T" + paxCount++;
                        if ("ADT".equals(assignedPtc)) {
                            adtRefs.add(paxId);
                        }
                    }

                    paxRefs.add(paxId);
                    if (pax.getFirstname() != null) {
                        givenNames.add(pax.getFirstname().toUpperCase());
                    }
                }
            } else {
                paxRefs.add("T1");
                givenNames.add("UNKNOWN");
            }
        } else {
            // Fallbacks
            segmentRefs.add("S1");
            paxRefs.add("T1");
            givenNames.add("UNKNOWN");
        }

        offerItem.setSegmentRefs(segmentRefs);
        offerItem.setPaxref(paxRefs);
        offerItem.setGivenName(givenNames);

        // Map Classes to Compartments
        List<SeatAvailabilityRspDto.OfferItem.Compartment> compartments = new ArrayList<>();
        if (seatMap.getClasses() != null) {
            for (Map.Entry<String, SeatAvailabilityRspGo7Dto.SeatClass> entry : seatMap.getClasses().entrySet()) {
                String classCode = entry.getKey();
                SeatAvailabilityRspGo7Dto.SeatClass seatClass = entry.getValue();

                SeatAvailabilityRspDto.OfferItem.Compartment compartment = mapCompartment(classCode, seatClass,
                        currency);
                if (compartment != null) {
                    compartments.add(compartment);
                }
            }
        }
        offerItem.setCompartmentList(compartments);
        offerItems.add(offerItem);

        rsp.setOfferItems(offerItems);

        return rsp;
    }

    private static SeatAvailabilityRspDto.OfferItem.Compartment mapCompartment(String classCode,
            SeatAvailabilityRspGo7Dto.SeatClass seatClass, String currency) {
        if (seatClass == null || seatClass.getPaidSeats() == null)
            return null;

        SeatAvailabilityRspDto.OfferItem.Compartment compartment = new SeatAvailabilityRspDto.OfferItem.Compartment();

        // Map Cabin Type: B->J, C->W, Y->Y? Or pass code?
        // User example explicitly requested "Y" -> "Y".
        // Let's assume standard mapping or pass key. Code "Y" matches standard "Y".
        // Code "B" might be Business (C or J). Code "C" might be Business (C or J).
        // Let's just use the key for now as it's the safest bet without explicit
        // mapping table.
        compartment.setCabinType(classCode);

        List<SeatAvailabilityRspDto.OfferItem.Compartment.Seat> seats = new ArrayList<>();

        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;

        for (SeatAvailabilityRspGo7Dto.PaidSeatRow row : seatClass.getPaidSeats()) {
            if (row.getRowNumber() != null) {
                minRow = Math.min(minRow, row.getRowNumber());
                maxRow = Math.max(maxRow, row.getRowNumber());
            }

            if (row.getSeats() != null) {
                for (Map.Entry<String, String> seatEntry : row.getSeats().entrySet()) {
                    String seatKey = seatEntry.getKey(); // e.g., "10A"
                    String status = seatEntry.getValue(); // e.g., "F" (Free)

                    SeatAvailabilityRspDto.OfferItem.Compartment.Seat seat = mapSeat(row, seatKey, status, currency);
                    if (seat != null) {
                        seats.add(seat);
                    }
                }
            }
        }

        if (minRow != Integer.MAX_VALUE)
            compartment.setFirstRow(BigInteger.valueOf(minRow));
        if (maxRow != Integer.MIN_VALUE)
            compartment.setLastRow(BigInteger.valueOf(maxRow));
        compartment.setTotalRow(BigInteger.valueOf(seatClass.getPaidSeats().size())); // Total rows in this class

        compartment.setSeat(seats);
        return compartment;
    }

    private static SeatAvailabilityRspDto.OfferItem.Compartment.Seat mapSeat(SeatAvailabilityRspGo7Dto.PaidSeatRow row,
            String seatKey, String status, String currency) {
        SeatAvailabilityRspDto.OfferItem.Compartment.Seat seat = new SeatAvailabilityRspDto.OfferItem.Compartment.Seat();

        // Parse Row and Column
        // Assuming format like "10A", "1A"
        String columnId = seatKey.replaceAll("\\d", "");
        String rowNumStr = seatKey.replaceAll("[^\\d]", "");

        seat.setRowNumber(rowNumStr);
        seat.setColumId(columnId);

        // Occupancy Code
        // Go7: F = Free?
        // Target: A = Available (implied from context logic? Or user used "A" for rows
        // 10C-F in example?)
        // Let's assume F -> A.
        if ("F".equalsIgnoreCase(status)) {
            seat.setOccupancyCode("A");
        } else {
            seat.setOccupancyCode("R"); // Occupied/Reserved
        }

        // Characteristics
        List<SeatAvailabilityRspDto.OfferItem.Compartment.Seat.SeatCharacteristic> characteristics = new ArrayList<>();

        // 1. Chargeable?
        if (row.getSeatFare() != null && row.getSeatFare() > 0) {
            characteristics.add(
                    new SeatAvailabilityRspDto.OfferItem.Compartment.Seat.SeatCharacteristic("CH", "Chargeable Seat"));
        }

        // 2. Window/Aisle Heuristic (assuming standard 3-3 ABC-DEF)
        // A, F -> Window (W)
        // C, D -> Aisle (A)
        // B, E -> Center (9)
        if ("A".equalsIgnoreCase(columnId) || "F".equalsIgnoreCase(columnId)) {
            characteristics
                    .add(new SeatAvailabilityRspDto.OfferItem.Compartment.Seat.SeatCharacteristic("W", "Window seat"));
        } else if ("C".equalsIgnoreCase(columnId) || "D".equalsIgnoreCase(columnId)) {
            characteristics
                    .add(new SeatAvailabilityRspDto.OfferItem.Compartment.Seat.SeatCharacteristic("A", "Aisle seat"));
        } else if ("B".equalsIgnoreCase(columnId) || "E".equalsIgnoreCase(columnId)) {
            characteristics.add(new SeatAvailabilityRspDto.OfferItem.Compartment.Seat.SeatCharacteristic("9",
                    "Center seat (not window, not aisle)"));
        }

        // 3. Exit Row? (Hard to know w/o data, but "brandname" might hint)
        // Check brandname for "Exit" or similar keywords? Or map explicitly if user
        // provided logic?
        // User example didn't strictly correlate brands to exit, but let's leave it
        // generic for now.

        seat.setSeatCharacteristics(characteristics);

        // Pricing
        double amount = (row.getSeatFare() != null) ? row.getSeatFare() : 0.0;
        BigDecimal bigAmount = BigDecimal.valueOf(amount);

        SeatAvailabilityRspDto.OfferItem.Compartment.Seat.BaseFare baseFare = new SeatAvailabilityRspDto.OfferItem.Compartment.Seat.BaseFare(
                bigAmount, currency);
        SeatAvailabilityRspDto.OfferItem.Compartment.Seat.TotalFare totalFare = new SeatAvailabilityRspDto.OfferItem.Compartment.Seat.TotalFare(
                bigAmount, currency);

        seat.setBaseFare(baseFare);
        seat.setTotalFare(totalFare);

        return seat;
    }
}
