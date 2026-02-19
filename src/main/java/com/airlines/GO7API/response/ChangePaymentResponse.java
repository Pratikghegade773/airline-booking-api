package com.airlines.GO7API.response;

import com.airlines.GO7API.requestDto.ChangePaymentReqDto;
import com.airlines.GO7API.responseDto.ChangePaymentRspDto;
import com.airlines.GO7API.responseGo7.ChangePaymentRspGo7Dto;

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
import java.util.Arrays;
import java.util.Map;

public class ChangePaymentResponse {

    public static ChangePaymentRspDto generateResponse(ChangePaymentRspGo7Dto go7Response,
            ChangePaymentReqDto requestDto) {
        ChangePaymentRspDto response = new ChangePaymentRspDto();

        if (go7Response == null || go7Response.getAerocrs() == null || go7Response.getAerocrs().getBooking() == null) {
            return response;
        }

        ChangePaymentRspGo7Dto.Booking booking = go7Response.getAerocrs().getBooking();

        response.setResponseId("P" + UUID.randomUUID().toString().substring(0, 15).toUpperCase());
        response.setOrderId(String.valueOf(booking.getBookingid()));
        response.setPnr(booking.getPnrref());

        response.setApiOwner("G7");

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

        response.setPaymentTimeLimit(booking.getPnrttl());
        response.setTicketingTimeLimit(booking.getPnrttl());

        response.setStatusCode(go7Response.getAerocrs().isSuccess() ? "702" : "REJECTED");
        String validatingCarrier = "G7";
        response.setValidatingCarrier(validatingCarrier);

        // 2. Booking References
        List<ChangePaymentRspDto.BookingReferences> refs = new ArrayList<>();
        ChangePaymentRspDto.BookingReferences ref1 = new ChangePaymentRspDto.BookingReferences();
        ref1.setId(booking.getPnrref());
        ref1.setOtherId("F1");
        refs.add(ref1);

        ChangePaymentRspDto.BookingReferences ref2 = new ChangePaymentRspDto.BookingReferences();
        ref2.setId(booking.getPnrref());
        ref2.setAirlineId("G7");
        refs.add(ref2);

        response.setBookingReferences(refs);

        // 3. ODs (Flights)
        List<ChangePaymentRspDto.OD> ods = new ArrayList<>();
        List<ChangePaymentRspDto.PriceClass> priceClasses = new ArrayList<>();

        List<ChangePaymentRspGo7Dto.Flight> flightList = null;
        if (booking.getFlights() != null && booking.getFlights().getFlight() != null) {
            flightList = booking.getFlights().getFlight();
        } else if (booking.getItems() != null && booking.getItems().getFlight() != null) {
            flightList = booking.getItems().getFlight();
        }

        String issuingAirlineName = "G7";
        String issuingPlace = "US"; // Default

        if (flightList != null) {
            int segmentCounter = 1;
            int odCounter = 1;

            if (!flightList.isEmpty()) {
                ChangePaymentRspGo7Dto.Flight f0 = flightList.get(0);
                if (f0.getAirline() != null)
                    issuingAirlineName = f0.getAirline();
                if (f0.getFromcode() != null)
                    issuingPlace = f0.getFromcode();
            }

            for (ChangePaymentRspGo7Dto.Flight flight : flightList) {
                ChangePaymentRspDto.OD od = new ChangePaymentRspDto.OD();
                od.setSegmentId("S" + segmentCounter); // Matches SEG1 format
                od.setOdKey("OD" + odCounter);

                od.setOrigin(flight.getFromcode());
                od.setOriginAirportName(flight.getFrom());
                od.setDestination(flight.getTocode());
                od.setDestinationAirportName(flight.getTo());

                // Dates
                String formattedDepDate = formatDate(flight.getFlightdate());
                String formattedArrDate = formattedDepDate;
                if (flight.getDepart() != null && flight.getArrive() != null) {
                    try {
                        LocalTime depTime = LocalTime.parse(flight.getDepart());
                        LocalTime arrTime = LocalTime.parse(flight.getArrive());
                        if (arrTime.isBefore(depTime)) {
                            formattedArrDate = formatDate(adjustDateByDays(flight.getFlightdate(), 1));
                        }
                    } catch (Exception e) {
                    }
                }
                od.setDepartureDate(formattedDepDate);
                od.setArrivalDate(formattedArrDate);

                od.setDepartureTime(flight.getDepart());
                od.setArrivalTime(flight.getArrive());
                od.setJourneyTime(calculateJourneyTime(flight.getFlightdate(), flight.getDepart(),
                        flight.getFlightdate(), flight.getArrive()));

                if (flight.getNumber() != null) {
                    od.setFlightNumber(flight.getNumber());
                }
                od.setEquipment(flight.getAircraftType());
                od.setMarketingCarrierCode(
                        flight.getAirlinedesignator() != null ? flight.getAirlinedesignator() : "G7");
                od.setMarketingCarrierName(flight.getAirline());
                od.setOperatingCarrierCode(od.getMarketingCarrierCode());
                od.setOperatingCarrierName(flight.getAirline());

                od.setArrivalTerminal(flight.getArrivalTerminal());
                od.setDepartureTerminal(flight.getDepartureTerminal());
                od.setChangeOfDay(0); // Default

                String rawClass = flight.getFlightClass() != null ? flight.getFlightClass() : "";
                String className = rawClass;
                String cabinCode = "ECONOMY";
                String rbdCode = "";

                if (rawClass.contains("/")) {
                    String[] parts = rawClass.split("/");
                    if (parts.length > 0) {
                        String code = parts[0].trim().toUpperCase();
                        rbdCode = code; // Use the code part as RBD
                        if (code.equals("C") || code.equals("J") || code.equals("D") || code.equals("Z")
                                || code.equals("I")) {
                            cabinCode = "BUSINESS";
                        } else if (code.equals("F") || code.equals("A") || code.equals("P")) {
                            cabinCode = "FIRST";
                        }
                    }
                    if (parts.length > 1) {
                        className = parts[1].trim();
                    }
                } else if (!rawClass.isEmpty()) {
                    rbdCode = rawClass.substring(0, 1).toUpperCase();
                }

                String priceClassId = "PC" + odCounter;
                od.setCabinType(cabinCode);
                od.setPriceClassId(priceClassId);
                od.setRbdCode(null); // Explicitly null per request
                od.setFareBasisCode(null); // Explicitly null per request
                od.setClassType(className); // Use class name as class type

                ods.add(od);

                // PriceClass
                ChangePaymentRspDto.PriceClass pc = new ChangePaymentRspDto.PriceClass();
                pc.setPriceClassId(priceClassId);
                pc.setClassName(className);
                pc.setCabinTypeCode(priceClassId); // Example uses ID as code

                List<ChangePaymentRspDto.PriceClass.Description> descriptions = new ArrayList<>();
                if (flight.getServices() != null) {
                    // Map services to descriptions
                    for (java.util.Map.Entry<String, Boolean> entry : flight.getServices().entrySet()) {
                        if (Boolean.TRUE.equals(entry.getValue())) {
                            ChangePaymentRspDto.PriceClass.Description d = new ChangePaymentRspDto.PriceClass.Description();
                            d.setText(mapServiceToDescription(entry.getKey()));
                            descriptions.add(d);
                        }
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

        // 4. Pax Details & Ticket Doc Info
        List<ChangePaymentRspDto.PaxDetailDTO> paxList = new ArrayList<>();
        List<ChangePaymentRspDto.TicketDocInfoDTO> topLevelTicketDocs = new ArrayList<>();

        List<ChangePaymentRspGo7Dto.Passenger> go7PaxList = (booking.getPassengers() != null)
                ? booking.getPassengers().getPassenger()
                : null;

        if (go7PaxList != null) {
            int paxCounter = 1;
            for (ChangePaymentRspGo7Dto.Passenger go7Pax : go7PaxList) {
                ChangePaymentRspDto.PaxDetailDTO pax = new ChangePaymentRspDto.PaxDetailDTO();
                String paxId = "PAX" + paxCounter++; // Example uses PAX1
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

                pax.setBirthDate(formatDate(go7Pax.getDob())); // Should match ddMMMyyyy
                pax.setLanguage("English");

                // Emails match example
                if (go7Pax.getEmail() != null) {
                    ChangePaymentRspDto.PaxDetailDTO.EmailDTO email = new ChangePaymentRspDto.PaxDetailDTO.EmailDTO();
                    email.setEmailAddress(go7Pax.getEmail());
                    email.setLabel("OTH");
                    email.setType("OSI");
                    email.setLanguage("English");
                    List<ChangePaymentRspDto.PaxDetailDTO.EmailDTO> emails = new ArrayList<>();
                    emails.add(email);
                    pax.setEmails(emails);
                }

                List<ChangePaymentRspDto.TicketDocInfoDTO> ticketDocInfoList = new ArrayList<>();

                // Map TicketDocInfo
                if (go7Pax.getETickets() != null && go7Pax.getETickets().getFlight() != null) {
                    for (ChangePaymentRspGo7Dto.Passenger.ETicketFlight etf : go7Pax.getETickets().getFlight()) {
                        ChangePaymentRspDto.TicketDocInfoDTO tdi = new ChangePaymentRspDto.TicketDocInfoDTO();
                        tdi.setValidatingCarrier("G7"); // Default
                        tdi.setIssuingAirlineName(issuingAirlineName);
                        tdi.setIssuingPlace(issuingPlace);
                        List<String> pIds = new ArrayList<>();
                        pIds.add(paxId);
                        tdi.setPaxId(pIds);

                        List<ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO> docs = new ArrayList<>();
                        ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO doc = new ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO();
                        doc.setTicketDocNbr(etf.getEticketnumber());
                        doc.setType("T");
                        doc.setNumberOfBooklets(1);
                        doc.setDateOfIssue(formatDate(LocalDate.now().toString()));
                        doc.setTimeOfIssue(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
                        doc.setTicketingLocation(issuingPlace);
                        doc.setReportingType("BSP");

                        // NOTE: linktoticket is NOT added as a "ticket" here anymore; it's an EMD now.
                        // Wait, previous version added it here too.
                        // But user wants EMD explicitly. Adding it as "J" type ticket *as well* might
                        // be confusing or duplicative.
                        // I will skip adding it to "ticketDocument" list directly if I am mapping it to
                        // "emdInfo".
                        // Unless "emdInfo" IS just another representation.
                        // The user's request: "add Seat data after Ticket as emdInfo".
                        // This implies separate field. So I won't mix it here.

                        // Coupons - Map from Flight List
                        List<ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO> coupons = new ArrayList<>();
                        if (flightList != null) {
                            int couponNum = 1;
                            for (ChangePaymentRspGo7Dto.Flight f : flightList) {
                                ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO coupon = new ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO();
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

                                ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO cai = new ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO();
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

                                List<ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO> caiList = new ArrayList<>();
                                caiList.add(cai);
                                coupon.setCurrentAirlineInfo(caiList);

                                List<ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.BaggageAllowance> bags = new ArrayList<>();
                                if (f.getServices() != null && f.getServices().containsKey("CheckedInBaggage")
                                        && Boolean.TRUE.equals(f.getServices().get("CheckedInBaggage"))) {
                                    ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.BaggageAllowance bag = new ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.BaggageAllowance();
                                    bag.setBaggageAllowanceId("FBA" + couponNum);
                                    bag.setPassengerId(paxId);
                                    bag.setCategory("Checked-In");
                                    bag.setName("Bag allowances");
                                    bags.add(bag);
                                }
                                coupon.setBaggageAllowances(bags);

                                coupons.add(coupon);
                            }
                        }
                        doc.setCouponInfo(coupons);

                        docs.add(doc);
                        tdi.setTicketDocument(docs);
                        ticketDocInfoList.add(tdi);
                        topLevelTicketDocs.add(tdi);
                    }
                    pax.setTicketDocInfo(ticketDocInfoList);
                }

                // EMD Info Logic
                boolean hasSeats = false;
                if (flightList != null) {
                    for (ChangePaymentRspGo7Dto.Flight f : flightList) {
                        if (f.getSeat() != null && !f.getSeat().isEmpty()) {
                            hasSeats = true;
                            break;
                        }
                    }
                }

                if (hasSeats && booking.getLinktoticket() != null && !booking.getLinktoticket().isEmpty()
                        && flightList != null
                        && !flightList.isEmpty()) {
                    List<ChangePaymentRspDto.EMDInfoDTO> emdInfoList = new ArrayList<>();
                    ChangePaymentRspDto.EMDInfoDTO emd = new ChangePaymentRspDto.EMDInfoDTO();

                    // Use response validating carrier for consistency or defaulting to G7/API
                    // context
                    emd.setValidatingCarrier(response.getValidatingCarrier());

                    List<String> pIds = new ArrayList<>();
                    pIds.add(paxId);
                    emd.setPaxId(pIds);
                    emd.setIssuingAirlineName(issuingAirlineName);
                    emd.setIssuingPlace(issuingPlace);

                    List<ChangePaymentRspDto.EMDInfoDTO.TicketDocumentDTO> emdDocs = new ArrayList<>();

                    // Iterate over Ticket Documents just mapped
                    if (!ticketDocInfoList.isEmpty()) {
                        for (ChangePaymentRspDto.TicketDocInfoDTO tdi : ticketDocInfoList) {
                            if (tdi.getTicketDocument() != null) {
                                for (ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO td : tdi
                                        .getTicketDocument()) {
                                    // Map only primary tickets
                                    if ("T".equals(td.getType())) {
                                        ChangePaymentRspDto.EMDInfoDTO.TicketDocumentDTO emdDoc = new ChangePaymentRspDto.EMDInfoDTO.TicketDocumentDTO();
                                        // emdDoc.setConnectedDocNbr(td.getTicketDocNbr());

                                        // Set ticket doc number from connected doc
                                        emdDoc.setTicketDocNbr(td.getTicketDocNbr());

                                        emdDoc.setType("J");
                                        emdDoc.setNumberOfBooklets(1);
                                        emdDoc.setDateOfIssue(formatDate(LocalDate.now().toString()));

                                        List<ChangePaymentRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO> emdCoupons = new ArrayList<>();
                                        int cNum = 1;
                                        for (ChangePaymentRspGo7Dto.Flight f : flightList) {
                                            ChangePaymentRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO ec = new ChangePaymentRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO();
                                            ec.setValidatingCarrier(response.getValidatingCarrier());
                                            ec.setCouponNumber(cNum++);

                                            // Seat Service Ref
                                            List<String> sRefs = new ArrayList<>();
                                            sRefs.add("SEAT" + (flightList.indexOf(f) + 1));
                                            ec.setServiceRefs(sRefs);

                                            String fClass = f.getFlightClass() != null ? f.getFlightClass() : "";
                                            String fRbd = "";
                                            if (fClass.contains("/")) {
                                                String[] parts = fClass.split("/");
                                                if (parts.length > 0)
                                                    fRbd = parts[0];
                                            } else if (!fClass.isEmpty()) {
                                                fRbd = fClass.substring(0, 1);
                                            }
                                            ec.setRbd(fRbd);
                                            ec.setRfic("A");
                                            ec.setRfisc("0B5");
                                            ec.setStatus("I");

                                            // Current Airline Info reuse logic
                                            ChangePaymentRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO cai = new ChangePaymentRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO();
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

                                            List<ChangePaymentRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO> caiList = new ArrayList<>();
                                            caiList.add(cai);
                                            ec.setCurrentAirlineInfo(caiList);

                                            emdCoupons.add(ec);
                                        }
                                        emdDoc.setCouponInfo(emdCoupons);
                                        emdDocs.add(emdDoc);
                                    }
                                }
                            }
                        }
                    }
                    emd.setTicketDocument(emdDocs);
                    if (!emdDocs.isEmpty()) {
                        emdInfoList.add(emd);
                    }
                    pax.setEmdInfo(emdInfoList);
                }

                paxList.add(pax);
            }
        }
        response.setPaxDetailList(paxList);
        response.setTicketDocInfoList(topLevelTicketDocs);

        // 5. Order Items
        List<ChangePaymentRspDto.OrderItemsDTO> orderItems = new ArrayList<>();
        int itemIdx = 1;

        // Group Pax
        List<String> adtRefs = new ArrayList<>();
        List<String> cnnRefs = new ArrayList<>();
        List<String> infRefs = new ArrayList<>();

        for (ChangePaymentRspDto.PaxDetailDTO p : paxList) {
            String pid = p.getPaxId();
            if ("ADT".equals(p.getPtc())) {
                adtRefs.add(pid);
            } else if ("CNN".equals(p.getPtc()) || "CHD".equals(p.getPtc())) {
                cnnRefs.add(pid);
            } else if ("INF".equals(p.getPtc())) {
                infRefs.add(pid);
            }
        }

        List<ChangePaymentRspGo7Dto.Flight> flightListRef = flightList;
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

        // Map Seats as Order Items
        if (flightList != null) {
            int seatSrvIdx = 1;
            for (ChangePaymentRspGo7Dto.Flight f : flightList) {
                if (f.getSeat() != null) {
                    String segmentId = "SEG" + (flightList.indexOf(f) + 1);
                    String odId = "OD" + (flightList.indexOf(f) + 1);

                    int seatCounter = 0;
                    for (ChangePaymentRspGo7Dto.Seat s : f.getSeat()) {
                        ChangePaymentRspDto.OrderItemsDTO seatItem = new ChangePaymentRspDto.OrderItemsDTO();
                        seatItem.setOrderItemId(response.getResponseId() + "_SRV" + seatSrvIdx++);
                        seatItem.setPtc("ADT");

                        // Retrieve ALL existing pax IDs from the paxList to ensure we can cycle through
                        // them if needed,
                        // or just use "PAX1" if for some reason list is empty (unlikely if we are
                        // here).
                        List<String> allPaxIds = paxList.stream().map(ChangePaymentRspDto.PaxDetailDTO::getPaxId)
                                .collect(Collectors.toList());
                        String assignedPaxId = (!allPaxIds.isEmpty()) ? allPaxIds.get(seatCounter % allPaxIds.size())
                                : "PAX1";

                        seatCounter++;
                        seatItem.setPassengerIds(Arrays.asList(assignedPaxId));

                        BigDecimal seatPrice = s.getFare() != null ? s.getFare() : BigDecimal.ZERO;
                        String sCurrency = s.getCurrency() != null ? s.getCurrency() : booking.getCurrency();
                        if (sCurrency == null)
                            sCurrency = "USD";

                        seatPrice = seatPrice.setScale(2, java.math.RoundingMode.HALF_UP);
                        seatItem.setBaseFare(new ChangePaymentRspDto.OrderItemsDTO.BaseFare(seatPrice, sCurrency));
                        seatItem.setTotalTax(
                                new ChangePaymentRspDto.OrderItemsDTO.TotalTax(BigDecimal.ZERO.setScale(2), sCurrency));
                        seatItem.setTotalFare(new ChangePaymentRspDto.OrderItemsDTO.TotalFare(seatPrice, sCurrency));
                        seatItem.setTotalPrice(seatPrice);

                        List<ChangePaymentRspDto.Service> seatServices = new ArrayList<>();
                        ChangePaymentRspDto.Service seatSrv = new ChangePaymentRspDto.Service();

                        seatSrv.setServiceId(segmentId + "_" + assignedPaxId + "_SEAT");
                        seatSrv.setServiceStatus(s.isStatus() ? "CONFIRMED" : "PENDING");
                        seatSrv.setServiceCode("SEAT");
                        seatSrv.setServiceName("Specific Seat Request");
                        seatSrv.setSegmentId(segmentId);
                        seatSrv.setOdKey(odId);
                        seatSrv.setDeparture(f.getFromcode());
                        seatSrv.setArrival(f.getTocode());

                        String seatNum = s.getSeat();
                        if (seatNum != null && seatNum.length() > 0) {
                            String col = seatNum.substring(seatNum.length() - 1);
                            String rowStr = seatNum.substring(0, seatNum.length() - 1);
                            seatSrv.setColumn(col);
                            try {
                                seatSrv.setRow(new java.math.BigInteger(rowStr));
                            } catch (Exception e) {
                            }
                        }

                        List<ChangePaymentRspDto.Service.SeatCharacteristic> chars = new ArrayList<>();
                        chars.add(new ChangePaymentRspDto.Service.SeatCharacteristic("CH", "Chargeable Seat"));
                        seatSrv.setSeatCharacteristics(chars);

                        seatServices.add(seatSrv);
                        seatItem.setServiceList(seatServices);

                        orderItems.add(seatItem);
                    }
                }
            }
        }

        response.setOrderItems(orderItems);

        // Payments
        List<ChangePaymentRspDto.PaymentsDTO> payments = new ArrayList<>();
        ChangePaymentRspDto.PaymentsDTO pay = new ChangePaymentRspDto.PaymentsDTO();
        pay.setType("CA");
        pay.setStatusCode("SUCCESSFUL");
        pay.setAmount(response.getTotalOrderPrice());
        pay.setCurrency(booking.getCurrency());
        List<String> orderItemIds = orderItems.stream().map(ChangePaymentRspDto.OrderItemsDTO::getOrderItemId)
                .collect(Collectors.toList());
        pay.setOrderItem(orderItemIds);
        payments.add(pay);
        response.setPayments(payments);

        return response;
    }

    // Helper Method to Create Order Item (Mirrors OrderCreateResponse logic)
    private static ChangePaymentRspDto.OrderItemsDTO createOrderItem(String responseId, int itemIndex, String ptc,
            List<String> paxIds, List<ChangePaymentRspGo7Dto.Flight> flights, String currency,
            ChangePaymentRspGo7Dto.Booking booking) {
        ChangePaymentRspDto.OrderItemsDTO item = new ChangePaymentRspDto.OrderItemsDTO();
        item.setOrderItemId(responseId + "-" + itemIndex);
        item.setPtc(ptc);
        item.setPassengerIds(paxIds);
        // timestamp not in DTO? Check DTO.
        // OrderItemsDTO in ChangePayment might not have timeStamp.
        // Checked DTO: It does NOT have timestamp.

        // Class Name from first flight
        String className = "Economy";
        if (flights != null && !flights.isEmpty()) {
            ChangePaymentRspGo7Dto.Flight f = flights.get(0);
            if (f.getFlightClass() != null) {
                String[] parts = f.getFlightClass().split("/");
                if (parts.length > 2) {
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
        java.util.Map<String, ChangePaymentRspDto.OrderItemsDTO.Tax> taxMap = new java.util.HashMap<>();

        // Flight list for Baggage/Service logic
        List<ChangePaymentRspDto.Service> serviceList = new ArrayList<>();
        List<ChangePaymentRspDto.OrderItemsDTO.BaggageAllowance> orderItemBags = new ArrayList<>();

        if (flights != null) {
            int segCount = 1;
            for (ChangePaymentRspGo7Dto.Flight f : flights) {
                // Determine Unit Base Fare
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
                    // Fallback
                    BigDecimal netFare = safeDecimal(f.getNetFare());
                    if (netFare.compareTo(BigDecimal.ZERO) == 0 && f.getInvpricingwithouttax() != null) {
                        netFare = safeDecimal(f.getInvpricingwithouttax());
                    }
                    if (netFare.compareTo(BigDecimal.ZERO) == 0) {
                        BigDecimal total = safeDecimal(f.getInvpricing());
                        BigDecimal totalTax = safeDecimal(f.getTotaltax());
                        if (totalTax.compareTo(BigDecimal.ZERO) == 0) {
                            totalTax = BigDecimal.valueOf(f.getTotaltaxes());
                        }
                        netFare = total.subtract(totalTax);
                    }

                    int totalPaxCount = booking.getAdults() + booking.getChild() + booking.getInfant();
                    // Safety if 0
                    if (totalPaxCount == 0 && booking.getPassengers() != null
                            && booking.getPassengers().getPassenger() != null)
                        totalPaxCount = booking.getPassengers().getPassenger().size();

                    if (totalPaxCount > 0) {
                        currentBase = netFare.divide(new BigDecimal(totalPaxCount), 2, java.math.RoundingMode.HALF_UP);
                    }
                }

                // Determine Unit Tax
                BigDecimal currentTax = BigDecimal.ZERO;
                String taxStr = f.getTax();
                if (taxStr != null && !taxStr.isEmpty()) {
                    currentTax = safeDecimal(taxStr);
                } else {
                    BigDecimal totalTax = safeDecimal(f.getTotaltax());
                    if (totalTax.compareTo(BigDecimal.ZERO) == 0) {
                        totalTax = BigDecimal.valueOf(f.getTotaltaxes());
                    }

                    int totalPaxCount = booking.getAdults() + booking.getChild() + booking.getInfant();
                    if (totalPaxCount == 0 && booking.getPassengers() != null
                            && booking.getPassengers().getPassenger() != null)
                        totalPaxCount = booking.getPassengers().getPassenger().size();

                    if (totalPaxCount > 0) {
                        currentTax = totalTax.divide(new BigDecimal(totalPaxCount), 2, java.math.RoundingMode.HALF_UP);
                    }
                }

                // Accumulate
                unitBase = unitBase.add(currentBase);
                unitTax = unitTax.add(currentTax);

                // Granular Taxes
                if (currentTax.compareTo(BigDecimal.ZERO) > 0 && f.getTaxes() != null) {
                    ChangePaymentRspGo7Dto.Taxes tObj = f.getTaxes();
                    BigDecimal totalGranular = BigDecimal.ZERO;
                    totalGranular = totalGranular.add(BigDecimal.valueOf(tObj.getFuel()));
                    totalGranular = totalGranular.add(BigDecimal.valueOf(tObj.getSecurity()));
                    totalGranular = totalGranular.add(BigDecimal.valueOf(tObj.getGroundHandling()));
                    totalGranular = totalGranular.add(BigDecimal.valueOf(tObj.getTax_4()));

                    BigDecimal scaleFactor = BigDecimal.ONE;
                    if (totalGranular.compareTo(BigDecimal.ZERO) > 0) {
                        scaleFactor = currentTax.divide(totalGranular, 6, java.math.RoundingMode.HALF_UP);
                    }

                    addScaledTaxItem(taxMap, "YQ", tObj.getFuel(), scaleFactor, currency, "Fuel Surcharge");
                    addScaledTaxItem(taxMap, "I2", tObj.getSecurity(), scaleFactor, currency, "Security Tax");
                    addScaledTaxItem(taxMap, "GH", tObj.getGroundHandling(), scaleFactor, currency, "Ground Handling");
                    addScaledTaxItem(taxMap, "S6", tObj.getTax_4(), scaleFactor, currency, "Other Tax");
                }

                // Services & Bags (Only for ADT/CHD mostly, but code allows for all)
                if (f.getServices() != null) {
                    for (java.util.Map.Entry<String, Boolean> entry : f.getServices().entrySet()) {
                        if (Boolean.TRUE.equals(entry.getValue())) {
                            ChangePaymentRspDto.Service svc = new ChangePaymentRspDto.Service();
                            String serviceSuffix = entry.getKey().length() > 3
                                    ? entry.getKey().substring(0, 3).toUpperCase()
                                    : entry.getKey().toUpperCase();
                            // Use first pax ID for service ID generation to keep it simple or unique per
                            // Item?
                            // ChangePayment original logic used paxIds.get(0).
                            String pid = paxIds.isEmpty() ? "P1" : paxIds.get(0);

                            svc.setServiceId("SEG" + segCount + "_" + pid + "_" + serviceSuffix);
                            svc.setServiceStatus("CONFIRMED");
                            svc.setSegmentId("SEG" + segCount);
                            svc.setOdKey("OD" + segCount);
                            svc.setDeparture(f.getFromcode());
                            svc.setArrival(f.getTocode());
                            serviceList.add(svc);

                            if ("CheckedInBaggage".equalsIgnoreCase(entry.getKey())) {
                                // Add bag for EACH pax in this Item?
                                // Original logic added bag for paxIds.get(0).
                                // Let's add for all pax in this item since they share the PTC/services.
                                for (String p : paxIds) {
                                    ChangePaymentRspDto.OrderItemsDTO.BaggageAllowance ba = createBaggageAllowance(
                                            entry.getKey(), segCount, p);
                                    if (ba != null) {
                                        orderItemBags.add(ba);
                                    }
                                }
                            }
                        }
                    }
                }
                segCount++;
            }
        }

        int count = paxIds.size();
        BigDecimal totalBase = unitBase.multiply(new BigDecimal(count));
        BigDecimal totalTaxAmount = unitTax.multiply(new BigDecimal(count));
        BigDecimal totalPrice = totalBase.add(totalTaxAmount);

        item.setTotalPrice(totalPrice.setScale(2, java.math.RoundingMode.HALF_UP));

        ChangePaymentRspDto.OrderItemsDTO.BaseFare bf = new ChangePaymentRspDto.OrderItemsDTO.BaseFare();
        bf.setAmount(totalBase.setScale(2, java.math.RoundingMode.HALF_UP));
        bf.setCurrency(currency);
        item.setBaseFare(bf);

        // Convert Tax Map to List
        List<ChangePaymentRspDto.OrderItemsDTO.Tax> taxList = new ArrayList<>(taxMap.values());
        item.setTaxes(taxList);

        ChangePaymentRspDto.OrderItemsDTO.TotalTax tt = new ChangePaymentRspDto.OrderItemsDTO.TotalTax();
        tt.setAmount(totalTaxAmount.setScale(2, java.math.RoundingMode.HALF_UP));
        tt.setCurrency(currency);
        item.setTotalTax(tt);

        ChangePaymentRspDto.OrderItemsDTO.TotalFare tf = new ChangePaymentRspDto.OrderItemsDTO.TotalFare();
        tf.setAmount(totalPrice.setScale(2, java.math.RoundingMode.HALF_UP));
        tf.setCurrency(currency);
        item.setTotalFare(tf);

        item.setServiceList(serviceList);
        item.setBaggageAllowances(orderItemBags);

        return item;
    }

    private static void addScaledTaxItem(java.util.Map<String, ChangePaymentRspDto.OrderItemsDTO.Tax> map, String code,
            double amount,
            BigDecimal scaleFactor, String currency, String description) {
        if (amount > 0) {
            ChangePaymentRspDto.OrderItemsDTO.Tax t = new ChangePaymentRspDto.OrderItemsDTO.Tax();
            // Description is used as key in some contexts, but here we set description
            // field
            // ChangePayment DTO Tax has: description, amount, currency. NO CODE field in
            // DTO?
            // Checking DTO: Tax has description, amount, currency.
            // So we use description.
            t.setCode(code);
            t.setDescription(description);

            BigDecimal scaledAmount = BigDecimal.valueOf(amount).multiply(scaleFactor).setScale(2,
                    java.math.RoundingMode.HALF_UP);
            t.setAmount(scaledAmount);
            t.setCurrency(currency);

            // Map key: separate by code/desc
            map.put(code, t);
        }
    }

    private static BigDecimal safeDecimal(String val) {
        if (val == null)
            return BigDecimal.ZERO;
        try {
            return new BigDecimal(val);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static ChangePaymentRspDto.OrderItemsDTO.BaggageAllowance createBaggageAllowance(String serviceKey,
            int segmentSeq, String paxId) {
        ChangePaymentRspDto.OrderItemsDTO.BaggageAllowance ba = new ChangePaymentRspDto.OrderItemsDTO.BaggageAllowance();
        ba.setBaggageAllowanceId("FBA" + segmentSeq);
        ba.setPtc("ADT");
        ba.setPassengerId(paxId);
        ba.setCategory("Checked-In");
        ba.setName("Bag allowances");

        // Logic to determine weight/allowance based on service key or external logic
        // Currently defaulted for standard economy if key matches CheckedInBaggage
        String weightVal = "30"; // Default
        String uom = "KG";

        // Could add logic here to check class/farebasis if passed in future
        if ("HandBaggage".equalsIgnoreCase(serviceKey)) {
            weightVal = "7";
            ba.setCategory("Carry-On");
        }

        ChangePaymentRspDto.OrderItemsDTO.BaggageAllowance.Weight w = new ChangePaymentRspDto.OrderItemsDTO.BaggageAllowance.Weight();
        w.setValue(weightVal);
        w.setUom(uom);
        List<ChangePaymentRspDto.OrderItemsDTO.BaggageAllowance.Weight> wl = new ArrayList<>();
        wl.add(w);
        ba.setWeight(wl);

        ChangePaymentRspDto.OrderItemsDTO.BaggageAllowance.DescriptionDTO desc = new ChangePaymentRspDto.OrderItemsDTO.BaggageAllowance.DescriptionDTO();
        desc.setDescription("Bag allowances");
        List<ChangePaymentRspDto.OrderItemsDTO.BaggageAllowance.DescriptionDTO> dl = new ArrayList<>();
        dl.add(desc);
        ba.setDescriptions(dl);

        return ba;
    }

    private static String mapServiceToDescription(String key) {
        if ("SeatSelection".equalsIgnoreCase(key))
            return "Seat Selection - Complimentary (Standard Seats)";
        if ("HandBaggage".equalsIgnoreCase(key))
            return "Cabin Baggage - 1 piece Up to 7kg each";
        if ("CheckedInBaggage".equalsIgnoreCase(key))
            return "Check-in Baggage - 30kg";
        if ("RefundableTicket".equalsIgnoreCase(key))
            return "Booking Cancellation Fee - USD 100";
        if ("FoodOnBoard".equalsIgnoreCase(key))
            return "Food on Board - Included";
        return key + " - Available";
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

}
