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

        List<OrderRetrieveRspGo7Dto.Flight> flightList = null;
        if (booking.getFlights() != null && booking.getFlights().getFlight() != null) {
            flightList = booking.getFlights().getFlight();
        } else if (booking.getItems() != null && booking.getItems().getFlight() != null) {
            flightList = booking.getItems().getFlight();
        }

        String carrierCode = "G7";
        if (flightList != null && !flightList.isEmpty()) {
            String desig = flightList.get(0).getAirlinedesignator();
            if (desig != null)
                carrierCode = desig;
        }

        response.setApiOwner(carrierCode);
        // response.setAgentId("1416-AGT40148"); // Default per example or mapping
        // response.setAgencyName("Fareintelligence"); // Default
        // response.setAgencyId("1416"); // Default

        // Pricing
        if (booking.getBalanceInformation() != null) {
            response.setTotalOrderPrice(
                    booking.getBalanceInformation().getPnrTotal().setScale(2, java.math.RoundingMode.HALF_UP));
        } else if (booking.getTotalprice() != null) {
            try {
                response.setTotalOrderPrice(
                        new BigDecimal(booking.getTotalprice()).setScale(2, java.math.RoundingMode.HALF_UP));
            } catch (NumberFormatException e) {
                response.setTotalOrderPrice(BigDecimal.ZERO.setScale(2));
            }
        }

        response.setCurrency(booking.getCurrency() != null ? booking.getCurrency() : "USD");
        // Exchange Rate if available in Go7 (Sample didn't explicitly show it in top
        // level clearly, but usually calculated or static)
        // response.setExchangeRate("239.10915543"); // Hardcoded from example or
        // derived? Leaving as example
        // default/placeholder

        response.setPaymentTimeLimit(formatDateTime(booking.getPnrttl()));

        response.setStatusCode(go7Response.getAerocrs().isSuccess() ? "702" : "REJECTED");
        response.setValidatingCarrier(carrierCode);

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
                    od.setFlightNumber(flight.getNumber());
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

                String gender = null;
                if (go7Pax.getGender() != null && !go7Pax.getGender().isEmpty()) {
                    if (go7Pax.getGender().toUpperCase().startsWith("M")) {
                        gender = "Male";
                    } else if (go7Pax.getGender().toUpperCase().startsWith("F")) {
                        gender = "Female";
                    }
                }

                // Title-based fallback
                if (gender == null && go7Pax.getPaxtitle() != null) {
                    String title = go7Pax.getPaxtitle().toUpperCase();
                    if (title.contains("MR") || title.contains("MSTR") || title.contains("MISTR")) {
                        gender = "Male";
                    } else if (title.contains("MS") || title.contains("MRS") || title.contains("MISS")) {
                        gender = "Female";
                    }
                }

                if (gender != null) {
                    pax.setGender(gender);
                } else {
                    pax.setGender("Male"); // Default
                }

                // Override for Child/Infant
                if ("CHD".equals(pax.getPtc()) || "CNN".equals(pax.getPtc())) {
                    pax.setTitle("CHILD");
                } else if ("INF".equals(pax.getPtc())) {
                    pax.setTitle("INFANT");
                }

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
                        tdi.setIssuingAirlineName(carrierCode);
                        tdi.setValidatingCarrier(carrierCode);

                        List<OrderRetrieveRspDto.TicketDocInfoDTO.TicketDocumentDTO> docs = new ArrayList<>();
                        OrderRetrieveRspDto.TicketDocInfoDTO.TicketDocumentDTO doc = new OrderRetrieveRspDto.TicketDocInfoDTO.TicketDocumentDTO();
                        doc.setTicketDocNbr(etf.getEticketnumber());
                        doc.setType("ET");
                        doc.setPrimaryDocInd("true");

                        // Try to find coupon/flight info? For now just ticket number is key
                        docs.add(doc);

                        // Link to Ticket (Seat Data) - EMD-like entry
                        if (booking.getLinktoticket() != null && !booking.getLinktoticket().isEmpty()) {
                            OrderRetrieveRspDto.TicketDocInfoDTO.TicketDocumentDTO linkDoc = new OrderRetrieveRspDto.TicketDocInfoDTO.TicketDocumentDTO();
                            linkDoc.setTicketDocNbr(booking.getLinktoticket());
                            linkDoc.setType("J"); // Using J to distinguish or match ChangeSeat logic
                            linkDoc.setNumberOfBooklets(1);
                            linkDoc.setDateOfIssue(formatDate(LocalDate.now().toString()));
                            linkDoc.setReportingType("BSP");
                            docs.add(linkDoc);
                        }

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
        // 5. Order Items
        List<OrderRetrieveRspDto.OrderItemDTO> orderItems = new ArrayList<>();
        int itemIdx = 1;

        // Group Pax
        List<String> adtRefs = new ArrayList<>();
        List<String> cnnRefs = new ArrayList<>();
        List<String> infRefs = new ArrayList<>();

        for (OrderRetrieveRspDto.PaxDetailDTO p : paxList) {
            String pid = p.getPaxId();
            if ("ADT".equals(p.getPtc())) {
                adtRefs.add(pid);
            } else if ("CHD".equals(p.getPtc())) {
                cnnRefs.add(pid);
            } else if ("INF".equals(p.getPtc())) {
                infRefs.add(pid);
            }
        }

        List<OrderRetrieveRspGo7Dto.Flight> flightListRef = flightList;
        String currency = booking.getCurrency();
        if (currency == null)
            currency = "USD";

        // ADT Item
        if (!adtRefs.isEmpty()) {
            OrderRetrieveRspDto.OrderItemDTO item = createOrderItem(response.getResponseId(), itemIdx++, "ADT", adtRefs,
                    flightListRef, currency);
            orderItems.add(item);
        }

        // CHD Item
        if (!cnnRefs.isEmpty()) {
            OrderRetrieveRspDto.OrderItemDTO item = createOrderItem(response.getResponseId(), itemIdx++, "CHD", cnnRefs,
                    flightListRef, currency);
            orderItems.add(item);
        }

        // INF Item
        if (!infRefs.isEmpty()) {
            OrderRetrieveRspDto.OrderItemDTO item = createOrderItem(response.getResponseId(), itemIdx++, "INF", infRefs,
                    flightListRef, currency);
            orderItems.add(item);
        }

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

    private static OrderRetrieveRspDto.OrderItemDTO createOrderItem(String responseId, int itemIndex, String ptc,
            List<String> paxIds, List<OrderRetrieveRspGo7Dto.Flight> flights, String currency) {
        OrderRetrieveRspDto.OrderItemDTO item = new OrderRetrieveRspDto.OrderItemDTO();
        item.setOrderItemId(responseId + "_AIR-" + itemIndex);
        item.setPtc(ptc);

        String mainClassName = "Economy";
        if (flights != null && !flights.isEmpty()) {
            OrderRetrieveRspGo7Dto.Flight f = flights.get(0);
            if (f.getFlightClass() != null) {
                String[] parts = f.getFlightClass().split("/");
                if (parts.length > 1) {
                    mainClassName = parts[1].trim();
                } else {
                    mainClassName = f.getFlightClass();
                }
            }
        }
        item.setClassName(mainClassName); // Actually item className might differ per segment but usually one PNR class
                                          // dominating
        item.setTimeStamp(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMMyyyy HH:mm:ss", Locale.ENGLISH)));
        item.setPassengerIds(paxIds);

        // Calculate Unit Totals
        BigDecimal unitBase = BigDecimal.ZERO;
        BigDecimal unitTax = BigDecimal.ZERO;

        List<OrderRetrieveRspDto.OrderItemDTO.Taxes> taxList = new ArrayList<>();

        if (flights != null) {
            for (OrderRetrieveRspGo7Dto.Flight f : flights) {
                // Base Fare
                String fareStr = null;
                boolean isAdt = "ADT".equals(ptc);
                boolean isCnn = "CNN".equals(ptc);
                boolean isInf = "INF".equals(ptc);

                if (isAdt)
                    fareStr = f.getAdultfare();
                else if (isCnn)
                    fareStr = f.getChildfare();
                else if (isInf)
                    fareStr = f.getInfantfare();

                // Fallback logic
                if (fareStr == null) {
                    if (isAdt)
                        fareStr = f.getNetFare();
                }

                // If fareStr has value, parse it.
                BigDecimal fareVal = BigDecimal.ZERO;
                if (fareStr != null) {
                    try {
                        fareVal = new BigDecimal(fareStr);
                    } catch (Exception e) {
                    }
                }
                unitBase = unitBase.add(fareVal);
                unitTax = unitTax.add(BigDecimal.valueOf(f.getTotaltaxes()));
            }
        }

        int count = paxIds.size();
        BigDecimal totalBase = unitBase.multiply(new BigDecimal(count));
        BigDecimal totalTaxAmount = unitTax.multiply(new BigDecimal(count));
        BigDecimal totalPrice = totalBase.add(totalTaxAmount);

        item.setTotalPrice(totalPrice.setScale(2, java.math.RoundingMode.HALF_UP));

        // Taxes Breakdown (Apply Count Multiplier and Proportional Scaling)
        if (flights != null) {
            BigDecimal totalGranularTaxForAllPax = BigDecimal.ZERO;
            for (OrderRetrieveRspGo7Dto.Flight f : flights) {
                if (f.getTaxes() != null) {
                    OrderRetrieveRspGo7Dto.Taxes t = f.getTaxes();
                    totalGranularTaxForAllPax = totalGranularTaxForAllPax
                            .add(BigDecimal.valueOf(t.getGroundHandling()));
                    totalGranularTaxForAllPax = totalGranularTaxForAllPax.add(BigDecimal.valueOf(t.getSecurity()));
                    totalGranularTaxForAllPax = totalGranularTaxForAllPax.add(BigDecimal.valueOf(t.getFuel()));
                    totalGranularTaxForAllPax = totalGranularTaxForAllPax.add(BigDecimal.valueOf(t.getTax_1()));
                    totalGranularTaxForAllPax = totalGranularTaxForAllPax.add(BigDecimal.valueOf(t.getTax_4()));
                }
            }
            totalGranularTaxForAllPax = totalGranularTaxForAllPax.multiply(new BigDecimal(count));

            BigDecimal scaleFactor = BigDecimal.ONE;
            if (totalGranularTaxForAllPax.compareTo(BigDecimal.ZERO) > 0) {
                scaleFactor = totalTaxAmount.divide(totalGranularTaxForAllPax, 10, java.math.RoundingMode.HALF_UP);
            }

            for (OrderRetrieveRspGo7Dto.Flight f : flights) {
                if (f.getTaxes() != null) {
                    OrderRetrieveRspGo7Dto.Taxes t = f.getTaxes();
                    addScaledTaxItem(taxList, "GH", t.getGroundHandling(), scaleFactor, count, currency,
                            "Ground Handling");
                    addScaledTaxItem(taxList, "I2", t.getSecurity(), scaleFactor, count, currency, "Security Tax");
                    addScaledTaxItem(taxList, "YQ", t.getFuel(), scaleFactor, count, currency, "Fuel Surcharge");
                    addScaledTaxItem(taxList, "IN", t.getTax_1(), scaleFactor, count, currency, "Infrastructure Tax");
                    addScaledTaxItem(taxList, "OT", t.getTax_4(), scaleFactor, count, currency, "Other Tax");
                }
            }
        }
        item.setTaxes(taxList);

        OrderRetrieveRspDto.OrderItemDTO.TotalTax totalTaxObj = new OrderRetrieveRspDto.OrderItemDTO.TotalTax();
        totalTaxObj.setAmount(totalTaxAmount.setScale(2, java.math.RoundingMode.HALF_UP));
        totalTaxObj.setCurrency(currency);
        item.setTotalTax(totalTaxObj);

        OrderRetrieveRspDto.OrderItemDTO.BaseFare baseFareObj = new OrderRetrieveRspDto.OrderItemDTO.BaseFare();
        baseFareObj.setAmount(totalBase.setScale(2, java.math.RoundingMode.HALF_UP));
        baseFareObj.setCurrency(currency);
        item.setBaseFare(baseFareObj);

        OrderRetrieveRspDto.OrderItemDTO.TotalFare totalFareObj = new OrderRetrieveRspDto.OrderItemDTO.TotalFare();
        totalFareObj.setAmount(totalPrice.setScale(2, java.math.RoundingMode.HALF_UP));
        totalFareObj.setCurrency(currency);
        item.setTotalFare(totalFareObj);

        return item;
    }

    private static void addScaledTaxItem(List<OrderRetrieveRspDto.OrderItemDTO.Taxes> list, String code, double amount,
            BigDecimal scaleFactor, int count, String currency, String description) {
        if (amount > 0) {
            OrderRetrieveRspDto.OrderItemDTO.Taxes t = new OrderRetrieveRspDto.OrderItemDTO.Taxes();
            t.setCode(code);
            BigDecimal scaledAmount = BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(count))
                    .multiply(scaleFactor)
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            t.setAmount(scaledAmount);
            t.setCurrency(currency);
            t.setDescription(description);
            list.add(t);
        }
    }

    private static String formatDateTime(String dateTimeStr) {
        if (dateTimeStr == null) {
            return null;
        }
        try {
            // Flexible parsing (Go7 might return various formats)
            // Example: "2026-02-13 11:55:06" or "2026/02/13 11:55:06"
            DateTimeFormatter inputFormatter;
            if (dateTimeStr.contains("/")) {
                inputFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            } else {
                inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            }
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, inputFormatter);

            // Desired format: "13Feb2026 11:55:06" -> ddMMMyyyy HH:mm:ss
            return dateTime.format(DateTimeFormatter.ofPattern("ddMMMyyyy HH:mm:ss", Locale.ENGLISH));
        } catch (Exception e) {
            // Fallback: try just date parsing if no time
            try {
                return formatDate(dateTimeStr);
            } catch (Exception ex) {
                return dateTimeStr; // Return raw if all fails
            }
        }
    }
}
