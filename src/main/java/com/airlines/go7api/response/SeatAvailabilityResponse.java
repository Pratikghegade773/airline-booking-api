package com.airlines.go7api.response;

import com.airlines.go7api.responsego7.common.*;

import com.airlines.go7api.responsedto.common.*;

import com.airlines.go7api.responsedto.SeatAvailabilityRspDto;
import com.airlines.go7api.responsego7.SeatAvailabilityRspGo7Dto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class SeatAvailabilityResponse {

    private SeatAvailabilityResponse() {
        throw new IllegalStateException("Utility class");
    }

    public static SeatAvailabilityRspDto mapToSeatAvailabilityRspDto(SeatAvailabilityRspGo7Dto go7Rsp, String orderId,
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp) {
        SeatAvailabilityRspDto rsp = new SeatAvailabilityRspDto();

        if (isInvalidResponse(go7Rsp)) {
            rsp.setResponseId(UUID.randomUUID().toString());
            return rsp;
        }

        SeatAvailabilityRspGo7Dto.SeatMapFare seatMap = go7Rsp.getAerocrs().getSeatMapFare();
        initializeRootFields(rsp, orderId, bookingRsp);
        rsp.setOfferItems(buildOfferItems(rsp.getOfferId(), seatMap, bookingRsp));

        return rsp;
    }

    private static boolean isInvalidResponse(SeatAvailabilityRspGo7Dto go7Rsp) {
        return go7Rsp == null || go7Rsp.getAerocrs() == null || !go7Rsp.getAerocrs().isSuccess()
                || go7Rsp.getAerocrs().getSeatMapFare() == null;
    }

    private static void initializeRootFields(SeatAvailabilityRspDto rsp, String orderId,
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp) {
        rsp.setResponseId(UUID.randomUUID().toString());
        rsp.setOfferId(rsp.getResponseId() + "-1");
        rsp.setOrderId(orderId);

        String[] carriers = determineCarrier(bookingRsp);
        rsp.setApiOwner(carriers[1]);
        rsp.setValidatingCarrier(carriers[0]);

        rsp.setOfferExpiration(LocalDateTime.now().plusMinutes(30).format(DateTimeFormatter.ISO_DATE_TIME));
    }

    private static List<SeatAvailabilityRspDto.OfferItem> buildOfferItems(
            String offerId,
            SeatAvailabilityRspGo7Dto.SeatMapFare seatMap,
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp) {
        List<SeatAvailabilityRspDto.OfferItem> offerItems = new ArrayList<>();
        SeatAvailabilityRspDto.OfferItem offerItem = new SeatAvailabilityRspDto.OfferItem();
        offerItem.setOfferItemId(offerId + "-1");
        offerItem.setCurrency(seatMap.getCurrency());

        offerItem.setSegmentRefs(extractSegmentRefs(bookingRsp));
        
        List<String> paxRefs = new ArrayList<>();
        List<String> givenNames = new ArrayList<>();
        extractPaxRefsAndNames(bookingRsp, paxRefs, givenNames);
        offerItem.setPaxref(paxRefs);
        offerItem.setGivenName(givenNames);

        offerItem.setCompartmentList(buildCompartments(seatMap, seatMap.getCurrency()));
        offerItems.add(offerItem);
        return offerItems;
    }

    private static List<SeatAvailabilityRspDto.OfferItem.Compartment> buildCompartments(
            SeatAvailabilityRspGo7Dto.SeatMapFare seatMap, String currency) {
        List<SeatAvailabilityRspDto.OfferItem.Compartment> compartments = new ArrayList<>();
        if (seatMap.getClasses() != null) {
            for (Map.Entry<String, SeatAvailabilityRspGo7Dto.SeatClass> entry : seatMap.getClasses().entrySet()) {
                SeatAvailabilityRspDto.OfferItem.Compartment compartment = mapCompartment(entry.getKey(), entry.getValue(), currency);
                if (compartment != null) {
                    compartments.add(compartment);
                }
            }
        }
        return compartments;
    }

    private static String[] determineCarrier(com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp) {
        String validatingCarrier = "G7";
        String apiOwner = "G7";

        if (bookingRsp != null && bookingRsp.getAerocrs() != null && bookingRsp.getAerocrs().getBooking() != null) {
            com.airlines.go7api.responsego7.common.Booking booking = bookingRsp.getAerocrs().getBooking();

            if (booking.getItems() != null && booking.getItems().getFlight() != null
                    && !booking.getItems().getFlight().isEmpty()) {
                com.airlines.go7api.responsego7.common.Flight flight = booking.getItems().getFlight().get(0);
                if (flight.getAirlineICAOcode() != null) {
                    validatingCarrier = flight.getAirlineICAOcode();
                    apiOwner = flight.getAirlineICAOcode();
                } else if (flight.getAirlinedesignator() != null) {
                    validatingCarrier = flight.getAirlinedesignator();
                    apiOwner = flight.getAirlinedesignator();
                }
            }
        }
        return new String[]{validatingCarrier, apiOwner};
    }

    private static List<String> extractSegmentRefs(com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp) {
        List<String> segmentRefs = new ArrayList<>();
        if (bookingRsp != null && bookingRsp.getAerocrs() != null && bookingRsp.getAerocrs().getBooking() != null) {
            com.airlines.go7api.responsego7.common.Booking booking = bookingRsp.getAerocrs().getBooking();

            if (booking.getItems() != null && booking.getItems().getFlight() != null) {
                int segCount = 1;
                for (com.airlines.go7api.responsego7.common.Flight flight : booking.getItems().getFlight()) {
                    segmentRefs.add("S" + segCount++);
                }
                return segmentRefs;
            }
        }
        segmentRefs.add("S1");
        return segmentRefs;
    }

    private static void extractPaxRefsAndNames(
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp,
            List<String> paxRefs,
            List<String> givenNames) {
        List<com.airlines.go7api.responsego7.common.Passenger> passengers = resolvePassengers(bookingRsp);
        if (passengers.isEmpty()) {
            paxRefs.add("T1");
            givenNames.add("UNKNOWN");
            return;
        }

        int paxCount = 1;
        List<String> adtRefs = new ArrayList<>();
        for (com.airlines.go7api.responsego7.common.Passenger pax : passengers) {
            String assignedPtc = resolvePtc(pax);
            String paxId = assignPaxId(assignedPtc, adtRefs, paxCount);
            if (!"INF".equals(assignedPtc)) {
                paxCount++;
                if ("ADT".equals(assignedPtc)) {
                    adtRefs.add(paxId);
                }
            } else if (!adtRefs.isEmpty()) {
                // infant linked to parent — counter not consumed
            } else {
                paxCount++;
            }
            paxRefs.add(paxId);
            if (pax.getFirstname() != null) {
                givenNames.add(pax.getFirstname().toUpperCase());
            }
        }
    }

    /** Traverses the booking response and returns the passenger list, or an empty list if unavailable. */
    private static List<com.airlines.go7api.responsego7.common.Passenger> resolvePassengers(
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp) {
        if (bookingRsp == null || bookingRsp.getAerocrs() == null
                || bookingRsp.getAerocrs().getBooking() == null) {
            return Collections.emptyList();
        }
        com.airlines.go7api.responsego7.common.Booking booking = bookingRsp.getAerocrs().getBooking();
        if (booking.getPassengers() == null || booking.getPassengers().getPassenger() == null) {
            return Collections.emptyList();
        }
        return booking.getPassengers().getPassenger();
    }

    /** Resolves the effective PTC for a passenger from its paxtype and title. */
    private static String resolvePtc(com.airlines.go7api.responsego7.common.Passenger pax) {
        String rawTitle = pax.getPaxtitle() != null
                ? pax.getPaxtitle().toUpperCase().replace(".", "")
                : "MR";
        boolean isInfantByTitle = rawTitle.contains("INF");

        if ("INFANT".equalsIgnoreCase(pax.getPaxtype()) || isInfantByTitle) {
            return "INF";
        }
        if ("CHILD".equalsIgnoreCase(pax.getPaxtype())) {
            return "CNN";
        }
        return "ADT";
    }

    /** Assigns a pax ID, linking infants to the last adult reference when available. */
    private static String assignPaxId(String ptc, List<String> adtRefs, int paxCount) {
        if ("INF".equals(ptc)) {
            return adtRefs.isEmpty() ? "T" + paxCount + ".1" : adtRefs.get(adtRefs.size() - 1) + ".1";
        }
        return "T" + paxCount;
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
            processRowSeats(row, seats, currency);
        }

        if (minRow != Integer.MAX_VALUE)
            compartment.setFirstRow(BigInteger.valueOf(minRow));
        if (maxRow != Integer.MIN_VALUE)
            compartment.setLastRow(BigInteger.valueOf(maxRow));
        compartment.setTotalRow(BigInteger.valueOf(seatClass.getPaidSeats().size())); // Total rows in this class

        compartment.setSeat(seats);
        return compartment;
    }

    private static void processRowSeats(SeatAvailabilityRspGo7Dto.PaidSeatRow row,
            List<SeatAvailabilityRspDto.OfferItem.Compartment.Seat> seats, String currency) {
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

        // 2. Window/Aisle Heuristic (Handling ABC-DEF and ABC-EFG patterns)
        // Window (W): A, F, G
        // Aisle (A): C, D, E
        // Center (9): B, F (if G exists)
        if ("A".equalsIgnoreCase(columnId) || "G".equalsIgnoreCase(columnId) || ("F".equalsIgnoreCase(columnId) && !row.getSeats().containsKey(columnId.substring(0, columnId.length() - 1) + "G"))) {
            // Note: F is window if G doesn't exist in the row
            characteristics.add(new SeatAvailabilityRspDto.OfferItem.Compartment.Seat.SeatCharacteristic("W", "Window seat"));
        } else if ("C".equalsIgnoreCase(columnId) || "D".equalsIgnoreCase(columnId) || "E".equalsIgnoreCase(columnId)) {
            characteristics.add(new SeatAvailabilityRspDto.OfferItem.Compartment.Seat.SeatCharacteristic("A", "Aisle seat"));
        } else if ("B".equalsIgnoreCase(columnId) || "F".equalsIgnoreCase(columnId)) {
            characteristics.add(new SeatAvailabilityRspDto.OfferItem.Compartment.Seat.SeatCharacteristic("9", "Center seat (not window, not aisle)"));
        }

        // 3. Exit Row?
        if (row.getBrandName() != null && row.getBrandName().toLowerCase().contains("exit")) {
            characteristics.add(new SeatAvailabilityRspDto.OfferItem.Compartment.Seat.SeatCharacteristic("EX", "Exit row seat"));
        }

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
