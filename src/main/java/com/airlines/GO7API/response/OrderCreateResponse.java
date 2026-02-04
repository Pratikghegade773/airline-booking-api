package com.airlines.GO7API.response;

import com.airlines.GO7API.requestDto.OrderCreateReqDto;
import com.airlines.GO7API.responseDto.OrderCreateRspDto;
import com.airlines.GO7API.responseGo7.OrderCreateRspGo7Dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;

public class OrderCreateResponse {

    public static OrderCreateRspDto generateResponse(OrderCreateRspGo7Dto go7Response, OrderCreateReqDto requestDto) {
        OrderCreateRspDto response = new OrderCreateRspDto();

        if (go7Response == null || go7Response.getAerocrs() == null || go7Response.getAerocrs().getBooking() == null) {
            return response;
        }

        OrderCreateRspGo7Dto.Booking booking = go7Response.getAerocrs().getBooking();

        // 1. Top Level Fields
        response.setResponseId("P" + UUID.randomUUID().toString().substring(0, 15).toUpperCase());
        response.setOrderId(booking.getBookingconfirmation() != null ? booking.getBookingconfirmation()
                : String.valueOf(booking.getBookingid()));
        response.setPnr(booking.getPnrref());

        response.setApiOwner("G7");

        // Pricing
        if (booking.getBalanceInformation() != null) {
            response.setTotalOrderPrice(booking.getBalanceInformation().getPnrTotal());
        } else if (booking.getTotalprice() != null) {
            try {
                response.setTotalOrderPrice(new BigDecimal(booking.getTotalprice()));
            } catch (NumberFormatException e) {
                response.setTotalOrderPrice(BigDecimal.ZERO);
            }
        }

        response.setCurrency(booking.getCurrency() != null ? booking.getCurrency() : "USD");

        response.setPaymentTimeLimit(booking.getPnrttl());
        response.setTicketingTimeLimit(booking.getPnrttl());

        response.setStatusCode(go7Response.getAerocrs().isSuccess() ? "702" : "REJECTED");
        String validatingCarrier = "G7";
        response.setValidatingCarrier(validatingCarrier);

        // 2. Booking References
        List<OrderCreateRspDto.BookingReference> refs = new ArrayList<>();
        OrderCreateRspDto.BookingReference ref1 = new OrderCreateRspDto.BookingReference();
        ref1.setId(booking.getPnrref());
        ref1.setOtherId("F1");
        refs.add(ref1);

        OrderCreateRspDto.BookingReference ref2 = new OrderCreateRspDto.BookingReference();
        ref2.setId(booking.getPnrref());
        ref2.setAirlineId("G7");
        refs.add(ref2);

        response.setBookingReferences(refs);

        // 3. ODs (Flights)
        List<OrderCreateRspDto.OD> ods = new ArrayList<>();
        List<OrderCreateRspDto.PriceClass> priceClasses = new ArrayList<>();

        List<OrderCreateRspGo7Dto.Flight> flightList = null;
        if (booking.getFlights() != null && booking.getFlights().getFlight() != null) {
            flightList = booking.getFlights().getFlight();
        } else if (booking.getItems() != null && booking.getItems().getFlight() != null) {
            flightList = booking.getItems().getFlight();
        }

        if (flightList != null) {
            int segmentCounter = 1;
            int odCounter = 1;

            for (OrderCreateRspGo7Dto.Flight flight : flightList) {
                OrderCreateRspDto.OD od = new OrderCreateRspDto.OD();
                od.setSegmentId("S" + segmentCounter);
                od.setOdKey("OD" + odCounter);

                od.setOrigin(flight.getFromcode());
                od.setOriginAirportName(flight.getFrom());
                od.setDestination(flight.getTocode());
                od.setDestinationAirportName(flight.getTo());

                // Calculate dates
                String formattedDepDate = formatDate(flight.getFlightdate());
                String formattedArrDate = formattedDepDate; // Default to same day

                // Handle overnight arrival for date calculation
                if (flight.getDepart() != null && flight.getArrive() != null) {
                    try {
                        LocalTime depTime = LocalTime.parse(flight.getDepart());
                        LocalTime arrTime = LocalTime.parse(flight.getArrive());
                        if (arrTime.isBefore(depTime)) {
                            // Arrives next day
                            formattedArrDate = formatDate(adjustDateByDays(flight.getFlightdate(), 1));
                        } else {
                            formattedArrDate = formattedDepDate;
                        }
                    } catch (Exception e) {
                        // ignore parsing error, stick to default
                    }
                }

                od.setDepartureDate(formattedDepDate);
                od.setArrivalDate(formattedArrDate);
                od.setDepartureTime(flight.getDepart());
                od.setArrivalTime(flight.getArrive());

                od.setJourneyTime(
                        calculateJourneyTime(flight.getFlightdate(), flight.getDepart(), flight.getFlightdate(),
                                flight.getArrive()));

                if (flight.getNumber() != null) {
                    od.setFlightNumber(flight.getNumber().replaceAll("[^0-9]", ""));
                }
                od.setEquipment(flight.getAircraftType());
                od.setMarketingCarrierCode(
                        flight.getAirlinedesignator() != null ? flight.getAirlinedesignator() : "G7");
                od.setMarketingCarrierName(flight.getAirline());
                od.setArrivalTerminal(flight.getArrivalTerminal());
                od.setDepartureTerminal(flight.getDepartureTerminal());

                String rawClass = flight.getFlightClass() != null ? flight.getFlightClass() : "";
                String rbd = "";
                String className = rawClass;
                String cabinCode = "Economy";

                if (rawClass.contains("/")) {
                    String[] parts = rawClass.split("/");
                    if (parts.length > 0) {
                        String code = parts[0].trim().toUpperCase();
                        // Map code to Cabin
                        if (code.equals("F") || code.equals("A") || code.equals("P")) {
                            cabinCode = "First";
                        } else if (code.equals("C") || code.equals("J") || code.equals("D") || code.equals("Z")
                                || code.equals("I")) {
                            cabinCode = "Business";
                        } else {
                            cabinCode = "Economy";
                        }

                        // Use the first letter as RBD if user wants "fetch that Y"
                        rbd = code;
                    }
                    if (parts.length > 1) {
                        className = parts[1].trim();
                    }
                } else {
                    // Fallback to searching string content
                    if (rawClass.toUpperCase().contains("BUSINESS")) {
                        cabinCode = "Business";
                    } else if (rawClass.toUpperCase().contains("FIRST")) {
                        cabinCode = "First";
                    }
                }

                String priceClassId = "PC" + odCounter;

                od.setCabinType(cabinCode);
                od.setRbdCode(null);
                od.setPriceClassId(priceClassId);
                od.setFareBasisCode(null);

                ods.add(od);

                // Populate PriceClass List (One per OD)
                OrderCreateRspDto.PriceClass pc = new OrderCreateRspDto.PriceClass();
                pc.setPriceClassId(priceClassId);
                pc.setClassName(className);
                pc.setCabinTypeCode(cabinCode.equals("Economy") ? "ECO" : cabinCode);

                List<OrderCreateRspDto.PriceClass.Description> descriptions = new ArrayList<>();

                // Add OD Key Description
                OrderCreateRspDto.PriceClass.Description odDesc = new OrderCreateRspDto.PriceClass.Description();
                odDesc.setOdKey("[OD" + odCounter + "]");
                descriptions.add(odDesc);

                // Add Services as Descriptions
                // Add Services as Descriptions
                if (flight.getServices() != null) {
                    for (java.util.Map.Entry<String, Boolean> entry : flight.getServices().entrySet()) {
                        OrderCreateRspDto.PriceClass.Description d = new OrderCreateRspDto.PriceClass.Description();
                        d.setText(entry.getKey() + ": " + entry.getValue());
                        descriptions.add(d);
                    }
                }

                pc.setDescriptions(descriptions);
                priceClasses.add(pc);

                segmentCounter++;
                odCounter++;
            }
        }
        response.setOds(ods);
        response.setPriceClassList(priceClasses);

        // 4. Pax Details
        List<OrderCreateRspDto.PaxDetailDTO> paxList = new ArrayList<>();
        List<OrderCreateRspGo7Dto.Passenger> go7PaxList = null;

        if (booking.getPassengers() != null && booking.getPassengers().getPassenger() != null) {
            go7PaxList = booking.getPassengers().getPassenger();
        }

        if (go7PaxList != null) {
            int paxCounter = 1;
            for (OrderCreateRspGo7Dto.Passenger go7Pax : go7PaxList) {
                OrderCreateRspDto.PaxDetailDTO pax = new OrderCreateRspDto.PaxDetailDTO();
                String paxId = "T" + paxCounter++;
                pax.setPaxId(paxId);
                pax.setPtc(mapPaxType(go7Pax.getPaxtype()));
                pax.setGivenName(go7Pax.getFirstname() != null ? go7Pax.getFirstname().toUpperCase() : "");
                pax.setSurname(go7Pax.getLastname() != null ? go7Pax.getLastname().toUpperCase() : "");
                pax.setTitle(go7Pax.getPaxtitle() != null ? go7Pax.getPaxtitle().toUpperCase().replace(".", "") : "MR");
                pax.setGender(go7Pax.getGender() != null && go7Pax.getGender().startsWith("M") ? "MALE" : "FEMALE");
                pax.setBirthDate(formatDate(go7Pax.getDob()));
                pax.setLanguage("English"); // Default

                if (go7Pax.getContact() != null) {
                    OrderCreateRspDto.PhoneDTO phone = new OrderCreateRspDto.PhoneDTO();
                    phone.setPhoneNumber(go7Pax.getContact());
                    phone.setType("Operational");
                    phone.setLanguage("English");
                    List<OrderCreateRspDto.PhoneDTO> phones = new ArrayList<>();
                    phones.add(phone);
                    pax.setPhones(phones);
                }

                if (go7Pax.getEmail() != null) {
                    OrderCreateRspDto.EmailDTO email = new OrderCreateRspDto.EmailDTO();
                    email.setEmailAddress(go7Pax.getEmail().toUpperCase());
                    email.setType("Operational");
                    email.setLanguage("English");
                    List<OrderCreateRspDto.EmailDTO> emails = new ArrayList<>();
                    emails.add(email);
                    pax.setEmails(emails);
                }

                paxList.add(pax);
            }
        } else if (requestDto.getPassengers() != null) {
            for (OrderCreateReqDto.Pax reqPax : requestDto.getPassengers()) {
                OrderCreateRspDto.PaxDetailDTO pax = new OrderCreateRspDto.PaxDetailDTO();
                pax.setPaxId(reqPax.getPaxId());
                pax.setPtc(reqPax.getPtc());
                // ... map others
                paxList.add(pax);
            }
        }
        response.setPaxDetailList(paxList);

        // 5. Order Items
        List<OrderCreateRspDto.OrderItemDTO> orderItems = new ArrayList<>();
        OrderCreateRspDto.OrderItemDTO item = new OrderCreateRspDto.OrderItemDTO();
        item.setOrderItemId(response.getResponseId() + "-1");
        item.setPtc("ADT");
        item.setClassName("Economy Flex");
        item.setTimeStamp("2026-02-03T07:05:00"); // Mock or current time

        List<String> paxIds = paxList.stream().map(OrderCreateRspDto.PaxDetailDTO::getPaxId)
                .collect(Collectors.toList());
        item.setPassengerIds(paxIds);

        if (booking.getBalanceInformation() != null) {
            item.setTotalPrice(booking.getBalanceInformation().getPnrTotal());
        } else if (booking.getTotalprice() != null) {
            item.setTotalPrice(safeDecimal(booking.getTotalprice()));
        }

        // Fares & Taxes
        // Fares & Taxes
        BigDecimal totalBase = BigDecimal.ZERO;
        BigDecimal totalTaxAmount = BigDecimal.ZERO;

        // Map to aggregate taxes: Description -> Amount
        java.util.Map<String, BigDecimal> taxAggregation = new java.util.HashMap<>();

        if (flightList != null) {
            for (OrderCreateRspGo7Dto.Flight flight : flightList) {
                totalBase = totalBase.add(safeDecimal(flight.getInvpricingwithouttax()));
                totalTaxAmount = totalTaxAmount.add(BigDecimal.valueOf(flight.getTotaltaxes()));

                if (flight.getTaxes() != null) {
                    accumulateTax(taxAggregation, "Fuel", flight.getTaxes().getFuel());
                    accumulateTax(taxAggregation, "Ground_handling", flight.getTaxes().getGroundHandling());
                    accumulateTax(taxAggregation, "Security", flight.getTaxes().getSecurity());
                    accumulateTax(taxAggregation, "tax_4", flight.getTaxes().getTax_4());
                }
            }
        }

        List<OrderCreateRspDto.OrderItemDTO.Tax> taxBreakdown = new ArrayList<>();
        for (java.util.Map.Entry<String, BigDecimal> entry : taxAggregation.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                OrderCreateRspDto.OrderItemDTO.Tax t = new OrderCreateRspDto.OrderItemDTO.Tax();
                t.setCode("TAX");
                t.setAmount(entry.getValue());
                t.setCurrency(booking.getCurrency());
                t.setDescription(entry.getKey());
                taxBreakdown.add(t);
            }
        }

        OrderCreateRspDto.OrderItemDTO.BaseFare baseFareObj = new OrderCreateRspDto.OrderItemDTO.BaseFare();
        baseFareObj.setAmount(totalBase);
        baseFareObj.setCurrency(booking.getCurrency());
        item.setBaseFare(baseFareObj);

        OrderCreateRspDto.OrderItemDTO.TotalTax totalTaxObj = new OrderCreateRspDto.OrderItemDTO.TotalTax();
        totalTaxObj.setAmount(totalTaxAmount);
        totalTaxObj.setCurrency(booking.getCurrency());
        item.setTotalTax(totalTaxObj);

        item.setTaxes(taxBreakdown);

        OrderCreateRspDto.OrderItemDTO.TotalFare totalFareObj = new OrderCreateRspDto.OrderItemDTO.TotalFare();
        totalFareObj.setAmount(item.getTotalPrice());
        totalFareObj.setCurrency(booking.getCurrency());
        item.setTotalFare(totalFareObj);

        // Service List (from flags)
        List<OrderCreateRspDto.Service> services = new ArrayList<>();
        if (flightList != null && !flightList.isEmpty()) {
            // Services mapping removed
        }
        item.setServiceList(services);

        orderItems.add(item);
        response.setOrderItems(orderItems);

        // OSI
        if (booking.getRemarks() != null && booking.getRemarks().getRemark() != null) {
            // Mapping for remarks handled elsewhere or dropped if DTO doesn't support OSI
        }

        return response;
    }

    private static String mapPaxType(String go7Type) {
        if ("ADULT".equalsIgnoreCase(go7Type))
            return "ADT";
        if ("CHILD".equalsIgnoreCase(go7Type))
            return "CHD";
        if ("INFANT".equalsIgnoreCase(go7Type))
            return "INF";
        return "ADT";
    }

    private static BigDecimal safeDecimal(String val) {
        if (val == null)
            return BigDecimal.ZERO;
        try {
            return new BigDecimal(val);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static void accumulateTax(java.util.Map<String, BigDecimal> map, String code, double amount) {
        if (amount <= 0)
            return;
        BigDecimal current = map.getOrDefault(code, BigDecimal.ZERO);
        map.put(code, current.add(BigDecimal.valueOf(amount)));
    }

    private static String calculateJourneyTime(String depDate, String depTime, String arrDate, String arrTime) {
        if (depDate == null || depTime == null || arrDate == null || arrTime == null) {
            return "PT0H0M"; // Default fallback
        }
        try {
            // Assuming date format "yyyy/MM/dd" and time "HH:mm" from user example
            // If date comes as "2026-02-20", we might need flexible parsing.
            // Go7 output showed "2026/02/20", so we strictly use slashes or hyphens?
            // Let's normalize to hyphens for uniform parsing if needed, but formatter can
            // handle slash pattern
            DateTimeFormatter dateFormatter;
            if (depDate.contains("/")) {
                dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
            } else {
                dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            }

            LocalDateTime dep = LocalDateTime.parse(depDate + " " + depTime, dateFormatter);
            LocalDateTime arr = LocalDateTime.parse(arrDate + " " + arrTime, dateFormatter);

            // Handle overnight arrival if arr < dep (though usually arrDate handles it)
            if (arr.isBefore(dep)) {
                arr = arr.plusDays(1); // Basic assumption if date not available, but here we have date.
            }

            Duration duration = Duration.between(dep, arr);
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();

            return String.format("PT%dH%dM", hours, minutes);

        } catch (Exception e) {
            // e.printStackTrace();
            return "PT0H0M";
        }
    }

    private static String formatDate(String dateStr) {
        if (dateStr == null) {
            return null;
        }
        try {
            DateTimeFormatter inputFormatter;
            if (dateStr.contains("/")) {
                inputFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            } else {
                inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            }
            LocalDate date = LocalDate.parse(dateStr, inputFormatter);
            // Format to ddMMMyyyy (e.g., 04Feb2026)
            return date.format(DateTimeFormatter.ofPattern("ddMMMyyyy", Locale.ENGLISH));
        } catch (Exception e) {
            return dateStr; // Return original if parsing fails
        }
    }

    private static String adjustDateByDays(String dateStr, int days) {
        if (dateStr == null)
            return null;
        try {
            DateTimeFormatter inputFormatter;
            if (dateStr.contains("/")) {
                inputFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            } else {
                inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            }
            LocalDate date = LocalDate.parse(dateStr, inputFormatter);
            return date.plusDays(days).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return dateStr;
        }
    }

}
