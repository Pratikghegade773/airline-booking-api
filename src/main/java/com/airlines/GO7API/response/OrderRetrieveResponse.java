package com.airlines.GO7API.response;

import com.airlines.GO7API.requestDto.OrderRetrieveReqDto;
import com.airlines.GO7API.responseDto.OrderRetrieveRspDto;
import com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public class OrderRetrieveResponse {

    public static OrderRetrieveRspDto generateResponse(OrderRetrieveRspGo7Dto go7Response,
            OrderRetrieveReqDto requestDto) {
        OrderRetrieveRspDto response = new OrderRetrieveRspDto();

        if (go7Response == null || go7Response.getAerocrs() == null || go7Response.getAerocrs().getBooking() == null) {
            return response;
        }

        OrderRetrieveRspGo7Dto.Booking booking = go7Response.getAerocrs().getBooking();

        // 1. Top Level Fields
        response.setResponseId("P" + UUID.randomUUID().toString().substring(0, 15).toUpperCase());
        response.setOrderId(booking.getBookingconfirmation() != null ? booking.getBookingconfirmation()
                : String.valueOf(booking.getBookingid()));
        response.setPnr(booking.getPnrref());

        response.setApiOwner("G7");
        // response.setAgentId("1416-AGT40148"); // Default per example or mapping
        // response.setAgencyName("Fareintelligence"); // Default
        // response.setAgencyId("1416"); // Default

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
        // Exchange Rate if available in Go7 (Sample didn't explicitly show it in top
        // level clearly, but usually calculated or static)
        // response.setExchangeRate("239.10915543"); // Hardcoded from example or
        // derived? Leaving as example
        // default/placeholder

        response.setPaymentTimeLimit(booking.getPnrttl());
        // response.setTicketingTimeLimit(booking.getPnrttl()); // Not in
        // OrderRetrieveRspDto

        response.setStatusCode(go7Response.getAerocrs().isSuccess() ? "702" : "REJECTED");
        String validatingCarrier = "G7";
        response.setValidatingCarrier(validatingCarrier);

        // 2. Booking References
        List<OrderRetrieveRspDto.BookingReference> refs = new ArrayList<>();
        OrderRetrieveRspDto.BookingReference ref1 = new OrderRetrieveRspDto.BookingReference();
        ref1.setId(booking.getPnrref());
        ref1.setOtherId("F1");
        refs.add(ref1);

        OrderRetrieveRspDto.BookingReference ref2 = new OrderRetrieveRspDto.BookingReference();
        ref2.setId(booking.getPnrref());
        ref2.setAirlineId("G7");
        refs.add(ref2);

        response.setBookingReferences(refs);

        // Remarks
        // Remarks mapping removed as per request to avoid specific Service Contact
        // details
        // if (booking.getRemarks() != null && booking.getRemarks().getRemark() != null)
        // {
        // List<String> remarkTexts = booking.getRemarks().getRemark().stream()
        // .map(OrderRetrieveRspGo7Dto.Remark::getText)
        // .collect(Collectors.toList());
        // response.setRemarks(remarkTexts);
        // }

        // 3. ODs (Flights)
        List<OrderRetrieveRspDto.OD> ods = new ArrayList<>();
        List<OrderRetrieveRspDto.PriceClass> priceClasses = new ArrayList<>();

        List<OrderRetrieveRspGo7Dto.Flight> flightList = null;
        if (booking.getFlights() != null && booking.getFlights().getFlight() != null) {
            flightList = booking.getFlights().getFlight();
        } else if (booking.getItems() != null && booking.getItems().getFlight() != null) {
            flightList = booking.getItems().getFlight();
        }

        if (flightList != null) {
            int segmentCounter = 1;
            int odCounter = 1;

            for (OrderRetrieveRspGo7Dto.Flight flight : flightList) {
                OrderRetrieveRspDto.OD od = new OrderRetrieveRspDto.OD();
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
                        // ignore parsing error
                    }
                }

                od.setDepartureDate(formattedDepDate);
                // Try format from Go7 which is yyyy/MM/dd to match example? Example output says
                // 04Feb2026.
                // formatDate handles conversion to ddMMMyyyy.

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
                String className = rawClass;
                String cabinCode = "Economy";

                if (rawClass.contains("/")) {
                    String[] parts = rawClass.split("/");
                    if (parts.length > 0) {
                        String code = parts[0].trim().toUpperCase();
                        if (code.equals("F") || code.equals("A") || code.equals("P")) {
                            cabinCode = "First";
                        } else if (code.equals("C") || code.equals("J") || code.equals("D") || code.equals("Z")
                                || code.equals("I")) {
                            cabinCode = "Business";
                        } else {
                            cabinCode = "Economy";
                        }
                    }
                    if (parts.length > 1) {
                        className = parts[1].trim();
                    }
                } else {
                    if (rawClass.toUpperCase().contains("BUSINESS")) {
                        cabinCode = "Business";
                    } else if (rawClass.toUpperCase().contains("FIRST")) {
                        cabinCode = "First";
                    }
                }

                String priceClassId = "PC" + odCounter;

                od.setCabinType(cabinCode);
                od.setPriceClassId(priceClassId);

                ods.add(od);

                // Populate PriceClass List
                // NOTE: OrderRetrieveRspDto has List<PriceClass> ? Check DTO definition. Yes,
                // line 35.
                OrderRetrieveRspDto.PriceClass pc = new OrderRetrieveRspDto.PriceClass();
                pc.setPriceClassId(priceClassId);
                pc.setClassName(className);
                pc.setCabinTypeCode(cabinCode.equals("Economy") ? "ECO" : cabinCode);

                List<OrderRetrieveRspDto.PriceClass.Description> descriptions = new ArrayList<>();
                if (flight.getServices() != null) {
                    for (java.util.Map.Entry<String, Boolean> entry : flight.getServices().entrySet()) {
                        OrderRetrieveRspDto.PriceClass.Description d = new OrderRetrieveRspDto.PriceClass.Description();
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
        List<OrderRetrieveRspDto.PaxDetailDTO> paxList = new ArrayList<>();
        List<OrderRetrieveRspGo7Dto.Passenger> go7PaxList = null;

        if (booking.getPassengers() != null && booking.getPassengers().getPassenger() != null) {
            go7PaxList = booking.getPassengers().getPassenger();
        }

        if (go7PaxList != null) {
            int paxCounter = 1;
            for (OrderRetrieveRspGo7Dto.Passenger go7Pax : go7PaxList) {
                OrderRetrieveRspDto.PaxDetailDTO pax = new OrderRetrieveRspDto.PaxDetailDTO();
                String paxId = "T" + paxCounter++;
                pax.setPaxId(paxId);
                pax.setPtc(mapPaxType(go7Pax.getPaxtype()));
                pax.setGivenName(go7Pax.getFirstname() != null ? go7Pax.getFirstname().toUpperCase() : "");
                pax.setSurname(go7Pax.getLastname() != null ? go7Pax.getLastname().toUpperCase() : "");
                pax.setTitle(go7Pax.getPaxtitle() != null ? go7Pax.getPaxtitle().toUpperCase().replace(".", "") : "MR");
                pax.setGender(go7Pax.getGender() != null && go7Pax.getGender().startsWith("M") ? "MALE" : "FEMALE");
                pax.setBirthDate(formatDate(go7Pax.getDob()));
                pax.setLanguage("English");

                if (go7Pax.getContact() != null) {
                    OrderRetrieveRspDto.PhoneDTO phone = new OrderRetrieveRspDto.PhoneDTO();
                    phone.setPhoneNumber(go7Pax.getContact());
                    phone.setType("Operational");
                    phone.setLabel("Mobile");
                    List<OrderRetrieveRspDto.PhoneDTO> phones = new ArrayList<>();
                    phones.add(phone);
                    pax.setPhones(phones);
                }

                if (go7Pax.getEmail() != null) {
                    OrderRetrieveRspDto.EmailDTO email = new OrderRetrieveRspDto.EmailDTO();
                    email.setEmailAddress(go7Pax.getEmail().toUpperCase());
                    email.setType("Operational");
                    List<OrderRetrieveRspDto.EmailDTO> emails = new ArrayList<>();
                    emails.add(email);
                    pax.setEmails(emails);
                }

                // Map TicketDocInfo (E-Tickets)
                if (go7Pax.getETickets() != null && go7Pax.getETickets().getFlight() != null) {
                    List<OrderRetrieveRspDto.TicketDocInfoDTO> ticketDocInfoList = new ArrayList<>();

                    for (OrderRetrieveRspGo7Dto.Passenger.ETicketFlight etf : go7Pax.getETickets().getFlight()) {
                        OrderRetrieveRspDto.TicketDocInfoDTO tdi = new OrderRetrieveRspDto.TicketDocInfoDTO();
                        tdi.setIssuingAirlineName("G7"); // Default or derive
                        tdi.setValidatingCarrier("G7");

                        List<OrderRetrieveRspDto.TicketDocInfoDTO.TicketDocumentDTO> docs = new ArrayList<>();
                        OrderRetrieveRspDto.TicketDocInfoDTO.TicketDocumentDTO doc = new OrderRetrieveRspDto.TicketDocInfoDTO.TicketDocumentDTO();
                        doc.setTicketDocNbr(etf.getEticketnumber());
                        doc.setType("ET");
                        doc.setPrimaryDocInd("true");

                        // Try to find coupon/flight info? For now just ticket number is key
                        docs.add(doc);

                        tdi.setTicketDocument(docs);
                        ticketDocInfoList.add(tdi);
                    }
                    pax.setTicketDocInfo(ticketDocInfoList);
                }

                paxList.add(pax);
            }
        }
        response.setPaxDetailList(paxList);

        // 5. Order Items
        List<OrderRetrieveRspDto.OrderItemDTO> orderItems = new ArrayList<>();
        OrderRetrieveRspDto.OrderItemDTO item = new OrderRetrieveRspDto.OrderItemDTO();
        item.setOrderItemId(response.getResponseId() + "-1");
        item.setPtc("ADT");

        String mainClassName = "Economy";
        if (flightList != null && !flightList.isEmpty()) {
            OrderRetrieveRspGo7Dto.Flight f = flightList.get(0);
            if (f.getFlightClass() != null) {
                String[] parts = f.getFlightClass().split("/");
                if (parts.length > 1) {
                    mainClassName = parts[1].trim();
                } else {
                    mainClassName = f.getFlightClass();
                }
            }
        }
        item.setClassName(mainClassName);
        item.setTimeStamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));

        List<String> paxIds = paxList.stream().map(OrderRetrieveRspDto.PaxDetailDTO::getPaxId)
                .collect(Collectors.toList());
        item.setPassengerIds(paxIds);

        if (booking.getBalanceInformation() != null) {
            item.setTotalPrice(booking.getBalanceInformation().getPnrTotal());
        } else if (booking.getTotalprice() != null) {
            item.setTotalPrice(safeDecimal(booking.getTotalprice()));
        }

        // Fares & Taxes
        BigDecimal totalBase = BigDecimal.ZERO;
        BigDecimal totalTaxAmount = BigDecimal.ZERO;
        java.util.Map<String, BigDecimal> taxAggregation = new java.util.HashMap<>();

        if (flightList != null) {
            for (OrderRetrieveRspGo7Dto.Flight flight : flightList) {
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

        List<OrderRetrieveRspDto.OrderItemDTO.Taxes> taxBreakdown = new ArrayList<>();
        for (java.util.Map.Entry<String, BigDecimal> entry : taxAggregation.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                OrderRetrieveRspDto.OrderItemDTO.Taxes t = new OrderRetrieveRspDto.OrderItemDTO.Taxes();
                t.setCode("TAX"); // or derive from key? Example had specific codes like YQ, IN
                // Mapping keys to some codes/descriptions if possible, or generic
                t.setAmount(entry.getValue());
                t.setCurrency(booking.getCurrency());
                t.setDescription(entry.getKey());
                taxBreakdown.add(t);
            }
        }

        OrderRetrieveRspDto.OrderItemDTO.BaseFare baseFareObj = new OrderRetrieveRspDto.OrderItemDTO.BaseFare();
        baseFareObj.setAmount(totalBase);
        baseFareObj.setCurrency(booking.getCurrency());
        item.setBaseFare(baseFareObj);

        OrderRetrieveRspDto.OrderItemDTO.TotalTax totalTaxObj = new OrderRetrieveRspDto.OrderItemDTO.TotalTax();
        totalTaxObj.setAmount(totalTaxAmount);
        totalTaxObj.setCurrency(booking.getCurrency());
        item.setTotalTax(totalTaxObj);

        item.setTaxes(taxBreakdown);

        OrderRetrieveRspDto.OrderItemDTO.TotalFare totalFareObj = new OrderRetrieveRspDto.OrderItemDTO.TotalFare();
        totalFareObj.setAmount(item.getTotalPrice());
        totalFareObj.setCurrency(booking.getCurrency());
        item.setTotalFare(totalFareObj);

        orderItems.add(item);
        response.setOrderItems(orderItems);

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
            return "PT0H0M";
        }
        try {
            DateTimeFormatter dateFormatter;
            if (depDate.contains("/")) {
                dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
            } else {
                dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            }

            LocalDateTime dep = LocalDateTime.parse(depDate + " " + depTime, dateFormatter);
            LocalDateTime arr = LocalDateTime.parse(arrDate + " " + arrTime, dateFormatter);

            if (arr.isBefore(dep)) {
                arr = arr.plusDays(1);
            }

            Duration duration = Duration.between(dep, arr);
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();

            return String.format("PT%dH%dM", hours, minutes);

        } catch (Exception e) {
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
            return date.format(DateTimeFormatter.ofPattern("ddMMMyyyy", Locale.ENGLISH));
        } catch (Exception e) {
            return dateStr;
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
