package com.airlines.go7api.response;

import com.airlines.go7api.requestdto.OrderCreateReqDto;
import com.airlines.go7api.responsedto.OrderCreateRspDto;
import com.airlines.go7api.responsego7.OrderCreateRspGo7Dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        response.setOrderId(String.valueOf(booking.getBookingid()));
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

        response.setPaymentTimeLimit(formatDateTimeForResponse(booking.getPnrttl()));
        response.setTicketingTimeLimit(formatDateTimeForResponse(booking.getPnrttl()));

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

        List<OrderCreateRspGo7Dto.Flight> list1 = (booking.getFlights() != null) ? booking.getFlights().getFlight()
                : null;
        List<OrderCreateRspGo7Dto.Flight> list2 = (booking.getItems() != null) ? booking.getItems().getFlight() : null;

        // Prefer list with Pricing Data FIRST, then flight data
        boolean list1HasPrice = hasPricingInfo(list1);
        boolean list2HasPrice = hasPricingInfo(list2);

        if (list1HasPrice) {
            flightList = list1;
        } else if (list2HasPrice) {
            flightList = list2;
        } else {
            // Fallback to whichever has valid flight info
            if (isFlightListValid(list1))
                flightList = list1;
            else if (isFlightListValid(list2))
                flightList = list2;
            else
                flightList = (list1 != null) ? list1 : list2;
        }

        if (flightList != null) {
            int segmentCounter = 1;
            int odCounter = 1;

            for (OrderCreateRspGo7Dto.Flight flight : flightList) {
                OrderCreateRspDto.OD od = new OrderCreateRspDto.OD();
                od.setSegmentId("SEG" + segmentCounter);
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
                    // e.g. ECO/Y/Flex Plus
                    if (parts.length > 0) {
                        String code = parts[0].trim().toUpperCase();
                        // Map code to Cabin
                        if (code.startsWith("F") || code.startsWith("A") || code.startsWith("P")) {
                            cabinCode = "First";
                        } else if (code.startsWith("C") || code.startsWith("J") || code.startsWith("D")
                                || code.startsWith("Z")
                                || code.startsWith("I")) {
                            cabinCode = "Business";
                        } else {
                            cabinCode = "Economy";
                        }
                    }
                    if (parts.length > 2) {
                        className = parts[2].trim();
                    } else if (parts.length > 1) {
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
            int adtCounter = 1;
            int cnnCounter = 1;
            int infCounter = 1;

            List<OrderCreateRspDto.TicketDocInfoDTO> topLevelTicketDocs = new ArrayList<>();
            java.util.Set<OrderCreateReqDto.Pax> mappedReqPaxes = new java.util.HashSet<>();
            String issuingAirlineName = "G7";
            String issuingPlace = "US";
            if (flightList != null && !flightList.isEmpty()) {
                OrderCreateRspGo7Dto.Flight f0 = flightList.get(0);
                if (f0.getAirline() != null)
                    issuingAirlineName = f0.getAirline();
                if (f0.getFromcode() != null)
                    issuingPlace = f0.getFromcode();
            }

            // Map ADT/CNN first to establish base IDs
            for (OrderCreateRspGo7Dto.Passenger go7Pax : go7PaxList) {
                // Find matching passenger in Request
                OrderCreateReqDto.Pax reqPax = null;
                if (requestDto.getPassengers() != null) {
                    for (OrderCreateReqDto.Pax rp : requestDto.getPassengers()) {
                        if (!mappedReqPaxes.contains(rp) &&
                                rp.getFirstName() != null && rp.getLastName() != null &&
                                rp.getFirstName().equalsIgnoreCase(go7Pax.getFirstname()) &&
                                rp.getLastName().equalsIgnoreCase(go7Pax.getLastname())) {
                            reqPax = rp;
                            if (!("INF".equalsIgnoreCase(rp.getPtc()) || "INFANT".equalsIgnoreCase(rp.getPtc()))) {
                                mappedReqPaxes.add(rp);
                            }
                            break;
                        }
                    }
                }

                String ptc = (reqPax != null && reqPax.getPtc() != null) ? reqPax.getPtc()
                        : mapPaxType(go7Pax.getPaxtype());

                if ("INF".equals(ptc) || "INFANT".equalsIgnoreCase(ptc))
                    continue;

                OrderCreateRspDto.PaxDetailDTO pax = new OrderCreateRspDto.PaxDetailDTO();
                String paxId = (reqPax != null && reqPax.getPaxId() != null && !reqPax.getPaxId().isEmpty()) ? reqPax.getPaxId() : "T" + (adtCounter + cnnCounter - 1);
                if ("ADT".equals(ptc))
                    adtCounter++;
                else
                    cnnCounter++;

                pax.setPaxId(paxId);
                pax.setPtc(ptc);
                pax.setGivenName(go7Pax.getFirstname() != null ? go7Pax.getFirstname().toUpperCase() : "");
                pax.setSurname(go7Pax.getLastname() != null ? go7Pax.getLastname().toUpperCase() : "");

                // Title
                String title = "MR";
                if (reqPax != null && reqPax.getTitle() != null) {
                    title = reqPax.getTitle();
                } else if (go7Pax.getPaxtitle() != null) {
                    title = go7Pax.getPaxtitle().toUpperCase().replace(".", "");
                }

                // Override Title for Child if request was mapped as CHD/CNN
                if ("CHD".equals(ptc) || "CNN".equals(ptc)) {
                    title = "CHILD";
                }
                pax.setTitle(title);

                // Gender
                String gender = null;
                if (reqPax != null && reqPax.getGender() != null) {
                    gender = reqPax.getGender();
                } else if (go7Pax.getGender() != null && !go7Pax.getGender().isEmpty()) {
                    if (go7Pax.getGender().toUpperCase().startsWith("M")) {
                        gender = "Male";
                    } else if (go7Pax.getGender().toUpperCase().startsWith("F")) {
                        gender = "Female";
                    }
                }

                // Title-based fallback
                if (gender == null && go7Pax.getPaxtitle() != null) {
                    String titleStr = go7Pax.getPaxtitle().toUpperCase();
                    if (titleStr.contains("MR") || titleStr.contains("MSTR") || titleStr.contains("MISTR")) {
                        gender = "Male";
                    } else if (titleStr.contains("MS") || titleStr.contains("MRS") || titleStr.contains("MISS")) {
                        gender = "Female";
                    }
                }

                if (gender != null) {
                    pax.setGender(gender);
                } else {
                    pax.setGender("Male"); // Default
                }

                pax.setBirthDate(formatDate(go7Pax.getDob()));
                pax.setLanguage("English");

                if (go7Pax.getContact() != null) {
                    OrderCreateRspDto.PhoneDTO phone = new OrderCreateRspDto.PhoneDTO();
                    phone.setPhoneNumber(go7Pax.getContact());
                    phone.setType("Operational");
                    List<OrderCreateRspDto.PhoneDTO> phones = new ArrayList<>();
                    phones.add(phone);
                    pax.setPhones(phones);
                }

                if (go7Pax.getEmail() != null) {
                    OrderCreateRspDto.EmailDTO email = new OrderCreateRspDto.EmailDTO();
                    email.setEmailAddress(go7Pax.getEmail().toUpperCase());
                    email.setType("Operational");
                    List<OrderCreateRspDto.EmailDTO> emails = new ArrayList<>();
                    emails.add(email);
                    pax.setEmails(emails);
                }

                // Map TicketDocInfo
                List<OrderCreateRspDto.TicketDocInfoDTO> ticketDocInfoList = new ArrayList<>();
                if (go7Pax.getETickets() != null && go7Pax.getETickets().getFlight() != null) {
                    java.util.Set<String> uniqueTickets = new java.util.LinkedHashSet<>();
                    for (OrderCreateRspGo7Dto.Passenger.ETicketFlight etf : go7Pax.getETickets().getFlight()) {
                        if (etf.getEticketnumber() != null && !etf.getEticketnumber().isEmpty()) {
                            uniqueTickets.add(etf.getEticketnumber().trim());
                        }
                    }

                    for (String ticketNbr : uniqueTickets) {
                        OrderCreateRspDto.TicketDocInfoDTO tdi = new OrderCreateRspDto.TicketDocInfoDTO();
                        tdi.setValidatingCarrier("G7");
                        tdi.setIssuingAirlineName(issuingAirlineName);
                        tdi.setIssuingPlace(issuingPlace);
                        List<String> pIds = new ArrayList<>();
                        pIds.add(paxId);
                        tdi.setPaxId(pIds);
                        tdi.setPtc(ptc);

                        List<OrderCreateRspDto.TicketDocInfoDTO.TicketDocumentDTO> docs = new ArrayList<>();
                        OrderCreateRspDto.TicketDocInfoDTO.TicketDocumentDTO doc = new OrderCreateRspDto.TicketDocInfoDTO.TicketDocumentDTO();
                        doc.setTicketDocNbr(ticketNbr);
                        doc.setType("T");
                        doc.setNumberOfBooklets(1);
                        doc.setDateOfIssue(formatDate(LocalDate.now().toString()));
                        doc.setTimeOfIssue(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
                        doc.setTicketingLocation(issuingPlace);
                        doc.setReportingType("BSP");

                        // Coupons
                        List<OrderCreateRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO> coupons = new ArrayList<>();
                        if (flightList != null) {
                            int couponNum = 1;
                            for (OrderCreateRspGo7Dto.Flight f : flightList) {
                                OrderCreateRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO coupon = new OrderCreateRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO();
                                coupon.setCouponNumber(couponNum++);
                                coupon.setCouponReference("FBA" + couponNum);

                                String fClass = f.getFlightClass() != null ? f.getFlightClass() : "";
                                String fRbd = "";
                                if (fClass.contains("/")) {
                                    String[] parts = fClass.split("/");
                                    if (parts.length > 0)
                                        fRbd = parts[0].trim().toUpperCase();
                                } else if (!fClass.isEmpty()) {
                                    fRbd = fClass.substring(0, 1).toUpperCase();
                                }

                                coupon.setFareBasisCode(fClass);
                                coupon.setRbd(fRbd);
                                coupon.setStatus("I");
                                coupon.setValidatingCarrier("G7");

                                OrderCreateRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO cai = new OrderCreateRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO();
                                cai.setDepartureAirportCode(f.getFromcode());
                                cai.setArrivalAirportCode(f.getTocode());
                                cai.setDepartureDate(formatDate(f.getFlightdate()));
                                cai.setDepartureTime(f.getDepart());
                                cai.setDepartureAirportName(f.getFrom());
                                cai.setDepartureTerminal(f.getDepartureTerminal());
                                cai.setArrivalDate(formatDate(f.getFlightdate()));
                                cai.setArrivalTime(f.getArrive());
                                cai.setArrivalAirportName(f.getTo());
                                cai.setArrivalTerminal(f.getArrivalTerminal());
                                cai.setMarketingCarrierAirlineId(f.getAirlinedesignator());
                                cai.setMarketingCarrierName(f.getAirline());
                                cai.setOperatingCarrierAirlineId(f.getAirlinedesignator());
                                cai.setOperatingCarrierName(f.getAirline());
                                cai.setFlightNumber(f.getNumber());
                                cai.setEquipmentAircraftCode(f.getAircraftTypeIataCode());

                                List<OrderCreateRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO> caiList = new ArrayList<>();
                                caiList.add(cai);
                                coupon.setCurrentAirlineInfo(caiList);
                                coupons.add(coupon);
                            }
                        }
                        doc.setCouponInfo(coupons);
                        docs.add(doc);
                        tdi.setTicketDocument(docs);
                        ticketDocInfoList.add(tdi);
                        topLevelTicketDocs.add(tdi);
                    }
                }
                if (!ticketDocInfoList.isEmpty()) {
                    pax.setTicketDocInfo(ticketDocInfoList);
                }

                paxList.add(pax);
            }
            if (!topLevelTicketDocs.isEmpty())

            {
                response.setTicketDocInfoList(topLevelTicketDocs);
            }

            // Map INF and associate with parents
            for (OrderCreateRspGo7Dto.Passenger go7Pax : go7PaxList) {
                // Find matching passenger in Request
                OrderCreateReqDto.Pax reqPax = null;
                if (requestDto.getPassengers() != null) {
                    for (OrderCreateReqDto.Pax rp : requestDto.getPassengers()) {
                        if (!mappedReqPaxes.contains(rp) &&
                                rp.getFirstName() != null && rp.getLastName() != null &&
                                rp.getFirstName().equalsIgnoreCase(go7Pax.getFirstname()) &&
                                rp.getLastName().equalsIgnoreCase(go7Pax.getLastname())) {
                            reqPax = rp;
                            mappedReqPaxes.add(rp);
                            break;
                        }
                    }
                }

                String ptcCheck = (reqPax != null && reqPax.getPtc() != null) ? reqPax.getPtc() : mapPaxType(go7Pax.getPaxtype());
                if (!"INF".equals(ptcCheck) && !"INFANT".equalsIgnoreCase(ptcCheck))
                    continue;

                String defaultParentId = "T" + infCounter;
                String paxId = (reqPax != null && reqPax.getPaxId() != null && !reqPax.getPaxId().isEmpty()) ? reqPax.getPaxId() : (defaultParentId + ".1");
                String parentId = paxId.contains(".") ? paxId.substring(0, paxId.indexOf(".")) : defaultParentId;
                infCounter++;

                OrderCreateRspDto.PaxDetailDTO inf = new OrderCreateRspDto.PaxDetailDTO();
                inf.setPaxId(paxId);
                inf.setPtc("INF");
                inf.setGivenName(go7Pax.getFirstname() != null ? go7Pax.getFirstname().toUpperCase() : "");
                inf.setSurname(go7Pax.getLastname() != null ? go7Pax.getLastname().toUpperCase() : "");
                inf.setTitle("INFANT");

                // Gender
                String gender = null;
                if (reqPax != null && reqPax.getGender() != null) {
                    gender = reqPax.getGender();
                } else if (go7Pax.getGender() != null && !go7Pax.getGender().isEmpty()) {
                    if (go7Pax.getGender().toUpperCase().startsWith("M")) {
                        gender = "Male";
                    } else if (go7Pax.getGender().toUpperCase().startsWith("F")) {
                        gender = "Female";
                    }
                }

                // Title-based fallback
                if (gender == null && go7Pax.getPaxtitle() != null) {
                    String titleStr = go7Pax.getPaxtitle().toUpperCase();
                    if (titleStr.contains("MR") || titleStr.contains("MSTR") || titleStr.contains("MISTR")) {
                        gender = "Male";
                    } else if (titleStr.contains("MS") || titleStr.contains("MRS") || titleStr.contains("MISS")) {
                        gender = "Female";
                    }
                }

                if (gender != null) {
                    inf.setGender(gender);
                } else {
                    inf.setGender("Male"); // Default
                }

                inf.setBirthDate(formatDate(go7Pax.getDob()));
                paxList.add(inf);

                // Update parent's infantRef
                for (OrderCreateRspDto.PaxDetailDTO parent : paxList) {
                    if (parent.getPaxId().equals(parentId)) {
                        parent.setInfantRef(paxId);
                        break;
                    }
                }
            }
        } else if (requestDto.getPassengers() != null)

        {
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
        int itemIdx = 1;

        // Group Pax
        List<String> adtRefs = new ArrayList<>();
        List<String> cnnRefs = new ArrayList<>();
        List<String> infRefs = new ArrayList<>();

        for (OrderCreateRspDto.PaxDetailDTO p : paxList) {
            String pid = p.getPaxId();
            if ("ADT".equals(p.getPtc())) {
                adtRefs.add(pid);
            } else if ("CNN".equals(p.getPtc()) || "CHD".equals(p.getPtc())) {
                cnnRefs.add(pid);
            } else if ("INF".equals(p.getPtc()) || "INFANT".equalsIgnoreCase(p.getPtc())) {
                infRefs.add(pid);
            }
        }

        List<OrderCreateRspGo7Dto.Flight> flightListRef = flightList;
        String currency = booking.getCurrency();
        if (currency == null)
            currency = "USD";

        // ADT Item
        if (!adtRefs.isEmpty()) {
            orderItems.add(createOrderItem(response.getResponseId(), itemIdx++, "ADT", adtRefs, flightListRef, currency,
                    booking));
        }

        // CNN Item
        if (!cnnRefs.isEmpty()) {
            orderItems.add(createOrderItem(response.getResponseId(), itemIdx++, "CNN", cnnRefs, flightListRef, currency,
                    booking));
        }

        // INF Item
        if (!infRefs.isEmpty()) {
            orderItems.add(createOrderItem(response.getResponseId(), itemIdx++, "INF", infRefs, flightListRef, currency,
                    booking));
        }

        // Service List (from flags)
        // Kept minimal as per previous code

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

    // Helper Method to Create Order Item
    private static OrderCreateRspDto.OrderItemDTO createOrderItem(String responseId, int itemIndex, String ptc,
            List<String> paxIds, List<OrderCreateRspGo7Dto.Flight> flights, String currency,
            OrderCreateRspGo7Dto.Booking booking) {
        OrderCreateRspDto.OrderItemDTO item = new OrderCreateRspDto.OrderItemDTO();
        item.setOrderItemId(responseId + "-" + itemIndex);
        item.setPtc(ptc);
        item.setPassengerIds(paxIds);
        item.setTimeStamp(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMMyyyy HH:mm:ss", Locale.ENGLISH)));

        // Class Name from first flight
        String className = "Economy";
        if (flights != null && !flights.isEmpty()) {
            OrderCreateRspGo7Dto.Flight f = flights.get(0);
            if (f.getFlightClass() != null) {
                // Parse like Airshop: "ECO/Y/Flex Plus"
                String[] parts = f.getFlightClass().split("/");
                if (parts.length > 2) {
                    // e.g. Flex Plus from 3rd part if exists, or combine?
                    // Airshop uses: parts[2] mostly for className
                    className = parts[2].trim();
                } else if (parts.length > 1) {
                    className = parts[1].trim();
                } else {
                    className = f.getFlightClass();
                }
            }
        }
        item.setClassName(className);

        // Calculate Unit Totals
        BigDecimal unitBase = BigDecimal.ZERO;
        BigDecimal unitTax = BigDecimal.ZERO;
        java.util.Map<String, OrderCreateRspDto.OrderItemDTO.Tax> taxMap = new java.util.HashMap<>();

        if (flights != null) {
            for (OrderCreateRspGo7Dto.Flight f : flights) {
                // Determine Unit Base Fare
                // Priority: (Adultfare/Childfare/Infantfare) -> NetFare/PaxCount ->
                // InvPricing/PaxCount -> (Total - Tax)/PaxCount
                String baseStr = null;
                if ("ADT".equals(ptc))
                    baseStr = f.getAdultfare();
                else if ("CNN".equals(ptc) || "CHD".equals(ptc))
                    baseStr = f.getChildfare();
                else if ("INF".equals(ptc))
                    baseStr = f.getInfantfare();

                BigDecimal currentBase = BigDecimal.ZERO;
                if (baseStr != null && !baseStr.isEmpty() && !"0".equals(baseStr)) {
                    currentBase = safeDecimal(baseStr);
                } else {
                    // Fallback: derive from Net Fare
                    BigDecimal netFare = safeDecimal(f.getNetFare());
                    if (netFare.compareTo(BigDecimal.ZERO) == 0 && f.getInvpricingwithouttax() != null) {
                        netFare = safeDecimal(f.getInvpricingwithouttax());
                    }

                    // If Net still 0, try Total - Tax
                    if (netFare.compareTo(BigDecimal.ZERO) == 0) {
                        BigDecimal total = safeDecimal(f.getInvpricing());
                        // Removed incorrect f.getTotalprice() check

                        BigDecimal totalTax = safeDecimal(f.getTotaltax());
                        if (totalTax.compareTo(BigDecimal.ZERO) == 0) {
                            totalTax = BigDecimal.valueOf(f.getTotaltaxes());
                        }
                        netFare = total.subtract(totalTax);
                    }

                    // Distribute across passengers
                    int totalPaxCount = booking.getAdults() + booking.getChild() + booking.getInfant();
                    if (totalPaxCount == 0 && booking.getPassengers() != null
                            && booking.getPassengers().getPassenger() != null) {
                        totalPaxCount = booking.getPassengers().getPassenger().size();
                    }
                    if (totalPaxCount > 0) {
                        currentBase = netFare.divide(new BigDecimal(totalPaxCount), 2, RoundingMode.HALF_UP);
                    }
                }

                // Determine Unit Tax
                // Priority: tax (ADT/CHD/INF) -> TotalTax/PaxCount -> TotalTaxes/PaxCount
                BigDecimal currentTax = BigDecimal.ZERO;

                // Allow Tax for ALL passengers as requested (ADT, CHD, INF)
                String taxStr = f.getTax();
                if (taxStr != null && !taxStr.isEmpty()) {
                    currentTax = safeDecimal(taxStr);
                } else {
                    // Fallback: distribute total tax
                    BigDecimal totalTax = safeDecimal(f.getTotaltax());
                    if (totalTax.compareTo(BigDecimal.ZERO) == 0) {
                        totalTax = BigDecimal.valueOf(f.getTotaltaxes());
                    }

                    int totalPaxCount = booking.getAdults() + booking.getChild(); // Only tax paying pax? Or include
                                                                                  // INF?
                    // Usually ADT+CHD pay tax. INF is 0.
                    if (totalPaxCount == 0 && booking.getPassengers() != null) {
                        // This count logic is tricky if we don't know who pays.
                        // Simplified: use all pax count from earlier for safety if missing
                        totalPaxCount = booking.getAdults() + booking.getChild() + booking.getInfant();
                        if (totalPaxCount == 0 && booking.getPassengers().getPassenger() != null)
                            totalPaxCount = booking.getPassengers().getPassenger().size();
                    }

                    if (totalPaxCount > 0) {
                        currentTax = totalTax.divide(new BigDecimal(totalPaxCount), 2, RoundingMode.HALF_UP);
                    }
                }

                // Accumulate
                unitBase = unitBase.add(currentBase);
                unitTax = unitTax.add(currentTax);

                // Granular Taxes
                // We ALWAYS populate granular taxes if available, but we must SCALE them to
                // match the unit currentTax.
                // Go7 often returns Total Taxes for the flight/booking in the breakdown, not
                // unit.
                // E.g. Tax = 25.00 (Unit), but Fuel = 30.00 + Security = 20.00 (Total 50.00 for
                // 2 pax).

                if (currentTax.compareTo(BigDecimal.ZERO) > 0 && f.getTaxes() != null) {
                    OrderCreateRspGo7Dto.Taxes tObj = f.getTaxes();

                    // Calculate Total Granular Tax from Go7 to determine scaling factor
                    BigDecimal totalGranular = BigDecimal.ZERO;
                    totalGranular = totalGranular.add(BigDecimal.valueOf(tObj.getFuel()));
                    totalGranular = totalGranular.add(BigDecimal.valueOf(tObj.getSecurity()));
                    totalGranular = totalGranular.add(BigDecimal.valueOf(tObj.getGroundHandling()));
                    totalGranular = totalGranular.add(BigDecimal.valueOf(tObj.getTax_1()));
                    totalGranular = totalGranular.add(BigDecimal.valueOf(tObj.getTax_4()));

                    BigDecimal scaleFactor = BigDecimal.ONE;
                    if (totalGranular.compareTo(BigDecimal.ZERO) > 0) {
                        scaleFactor = currentTax.divide(totalGranular, 6, RoundingMode.HALF_UP);
                    }

                    // Helper to add scaled tax
                    addScaledTaxItem(taxMap, "YQ", tObj.getFuel(), scaleFactor, currency, "Fuel Surcharge");
                    addScaledTaxItem(taxMap, "I2", tObj.getSecurity(), scaleFactor, currency, "Security Tax");
                    addScaledTaxItem(taxMap, "GH", tObj.getGroundHandling(), scaleFactor, currency, "Ground Handling");
                    addScaledTaxItem(taxMap, "IN", tObj.getTax_1(), scaleFactor, currency, "Infrastructure Tax");
                    addScaledTaxItem(taxMap, "OT", tObj.getTax_4(), scaleFactor, currency, "Other Tax");
                }
            }
        }

        int count = paxIds.size();
        BigDecimal totalBase = unitBase.multiply(new BigDecimal(count));
        BigDecimal totalTaxAmount = unitTax.multiply(new BigDecimal(count));
        BigDecimal totalPrice = totalBase.add(totalTaxAmount);

        // Final Global Fallback if totalPrice is still 0
        if (totalPrice.compareTo(BigDecimal.ZERO) == 0 && booking != null) {
            BigDecimal bkTotal = BigDecimal.ZERO;
            if (booking.getBalanceInformation() != null && booking.getBalanceInformation().getPnrTotal() != null) {
                bkTotal = booking.getBalanceInformation().getPnrTotal();
            } else if (booking.getTotalprice() != null) {
                bkTotal = safeDecimal(booking.getTotalprice());
            }

            if (bkTotal.compareTo(BigDecimal.ZERO) > 0) {
                // Determine shared portion (highly simplified: total / totalPaxCount *
                // currentPaxCount)
                int totalPaxCount = booking.getAdults() + booking.getChild() + booking.getInfant();
                if (totalPaxCount == 0 && booking.getPassengers() != null
                        && booking.getPassengers().getPassenger() != null) {
                    totalPaxCount = booking.getPassengers().getPassenger().size();
                }

                if (totalPaxCount > 0) {
                    totalPrice = bkTotal.multiply(new BigDecimal(count)).divide(new BigDecimal(totalPaxCount), 2,
                            RoundingMode.HALF_UP);
                    totalBase = totalPrice; // Simplified: set all as base if breakdown unavailable
                }
            }
        }

        item.setTotalPrice(totalPrice.setScale(2, RoundingMode.HALF_UP));

        // Fill inner objects
        OrderCreateRspDto.OrderItemDTO.TotalFare tf = new OrderCreateRspDto.OrderItemDTO.TotalFare();
        tf.setAmount(totalPrice.setScale(2, RoundingMode.HALF_UP));
        tf.setCurrency(currency);
        item.setTotalFare(tf);

        OrderCreateRspDto.OrderItemDTO.BaseFare bf = new OrderCreateRspDto.OrderItemDTO.BaseFare();
        bf.setAmount(totalBase.setScale(2, RoundingMode.HALF_UP));
        bf.setCurrency(currency);
        item.setBaseFare(bf);

        OrderCreateRspDto.OrderItemDTO.TotalTax tt = new OrderCreateRspDto.OrderItemDTO.TotalTax();
        tt.setAmount(totalTaxAmount.setScale(2, RoundingMode.HALF_UP));
        tt.setCurrency(currency);
        item.setTotalTax(tt);

        List<OrderCreateRspDto.OrderItemDTO.Tax> taxes = new ArrayList<>();
        // If we have a specific 'tax' field used (and no granular breakdown from map),
        // create the 'TAX' item
        // But if granular breakdown exists, we usually prefer that.
        // However, user output implies they want "TAX" explicitly.
        // Let's create "TAX" item matching the total tax amount if map is empty OR if
        // we want to force it?
        // User requested: "taxes": [ { "code": "TAX", "amount": 25.00 ... } ]
        // So we should adhere to that.

        if (!taxMap.isEmpty()) {
            for (OrderCreateRspDto.OrderItemDTO.Tax t : taxMap.values()) {
                t.setAmount(t.getAmount().multiply(new BigDecimal(count)).setScale(2, RoundingMode.HALF_UP));
                taxes.add(t);
            }
        } else if (totalTaxAmount.compareTo(BigDecimal.ZERO) > 0) {
            OrderCreateRspDto.OrderItemDTO.Tax tx = new OrderCreateRspDto.OrderItemDTO.Tax();
            tx.setCode("OT");
            tx.setAmount(totalTaxAmount.setScale(2, RoundingMode.HALF_UP));
            tx.setCurrency(currency);
            tx.setDescription("Total Taxes");
            taxes.add(tx);
        }
        item.setTaxes(taxes);

        /*
         * // Service List (Mapped from Flight.services)
         * // OrderCreateRspDto uses 'serviceList' not 'baggageAllowances'
         * List<OrderCreateRspDto.Service> serviceList = new ArrayList<>();
         * if (flights != null) {
         * int uniqueServiceIdx = 1;
         * for (int i = 0; i < flights.size(); i++) {
         * OrderCreateRspGo7Dto.Flight f = flights.get(i);
         * String segmentId = "SEG" + (i + 1); // Matches generic segment ID logic
         * 
         * String from = f.getFromcode() != null ? f.getFromcode() : "";
         * String to = f.getTocode() != null ? f.getTocode() : "";
         * 
         * if (f.getServices() != null) {
         * for (java.util.Map.Entry<String, Boolean> entry : f.getServices().entrySet())
         * {
         * // Only include if true? Or include all?
         * // OfferPrice includes checked/carry-on if true.
         * // Airshop includes if active.
         * // Here map is <String, Boolean>.
         * if (Boolean.TRUE.equals(entry.getValue())) {
         * String serviceName = entry.getKey();
         * 
         * // Map specific known services to nice names if needed,
         * // or distinct types.
         * // For OrderCreate we just dump into serviceList as per DTO structure.
         * 
         * OrderCreateRspDto.Service srv = new OrderCreateRspDto.Service();
         * String serviceCode = "SRV" + uniqueServiceIdx++;
         * 
         * srv.setServiceId(serviceCode);
         * srv.setServiceCode(serviceName); // Use generic name as code or map?
         * srv.setServiceName(serviceName);
         * srv.setSegmentId(segmentId);
         * srv.setDeparture(from);
         * srv.setArrival(to);
         * srv.setServiceStatus("CONFIRMED");
         * 
         * List<String> descList = new ArrayList<>();
         * descList.add(serviceName + " - Included");
         * srv.setDescription(descList);
         * 
         * serviceList.add(srv);
         * }
         * }
         * }
         * }
         * }
         * item.setServiceList(serviceList);
         */

        return item;
    }

    private static String formatDateTimeForResponse(String dateTimeStr) {
        if (dateTimeStr == null) {
            return null;
        }
        try {
            DateTimeFormatter inputFormatter;
            if (dateTimeStr.contains("/")) {
                inputFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
            } else {
                inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            }
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, inputFormatter);
            // Format to ddMMMyyyy HH:mm:ss (e.g., 26Feb2026 10:45:00)
            return dateTime.format(DateTimeFormatter.ofPattern("ddMMMyyyy HH:mm:ss", Locale.ENGLISH));
        } catch (Exception e) {
            return dateTimeStr; // Return original if parsing fails
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

    private static boolean isFlightListValid(List<OrderCreateRspGo7Dto.Flight> list) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        OrderCreateRspGo7Dto.Flight f = list.get(0);
        return f.getFromcode() != null || f.getTocode() != null || f.getAirline() != null;
    }

    private static boolean hasPricingInfo(List<OrderCreateRspGo7Dto.Flight> list) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        OrderCreateRspGo7Dto.Flight f = list.get(0);
        // Check for ANY pricing signal
        return (f.getAdultfare() != null && !"0".equals(f.getAdultfare())) ||
                (f.getNetFare() != null && !"0".equals(f.getNetFare())) ||
                (f.getInvpricing() != null && !"0".equals(f.getInvpricing()));
    }

    private static void addScaledTaxItem(java.util.Map<String, OrderCreateRspDto.OrderItemDTO.Tax> map, String code,
            double amount,
            BigDecimal scaleFactor, String currency, String description) {
        if (amount > 0) {
            OrderCreateRspDto.OrderItemDTO.Tax t = new OrderCreateRspDto.OrderItemDTO.Tax();
            t.setCode(code);
            // Apply scale factor to amount
            BigDecimal scaledAmount = BigDecimal.valueOf(amount).multiply(scaleFactor).setScale(2,
                    RoundingMode.HALF_UP);
            t.setAmount(scaledAmount);
            t.setCurrency(currency);
            t.setDescription(description);
            map.put(code, t);
        }
    }
}
