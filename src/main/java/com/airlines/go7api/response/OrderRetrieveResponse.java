package com.airlines.go7api.response;

import com.airlines.go7api.responsego7.common.*;

import com.airlines.go7api.responsedto.common.*;

import com.airlines.go7api.requestdto.OrderRetrieveReqDto;
import com.airlines.go7api.responsedto.OrderRetrieveRspDto;
import com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class OrderRetrieveResponse {

    public static OrderRetrieveRspDto generateResponse(OrderRetrieveRspGo7Dto go7Response,
            OrderRetrieveReqDto requestDto, java.util.Map<String, String> passengerSeats,
            java.util.Map<String, String> passengerServices, java.math.BigDecimal dbTotalOrderPrice) {
        OrderRetrieveRspDto response = new OrderRetrieveRspDto();

        if (go7Response == null || go7Response.getAerocrs() == null || go7Response.getAerocrs().getBooking() == null) {
            return response;
        }

        Booking booking = go7Response.getAerocrs().getBooking();

        // 1. Top Level Fields
        response.setResponseId("P" + UUID.randomUUID().toString().substring(0, 15).toUpperCase());
        response.setOrderId(booking.getBookingconfirmation() != null ? booking.getBookingconfirmation()
                : String.valueOf(booking.getBookingid()));
        response.setPnr(booking.getPnrref());

        List<Flight> flightList = null;
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

        response.setTotalOrderPrice(BigDecimal.ZERO);

        response.setCurrency(booking.getCurrency() != null ? booking.getCurrency() : "USD");
        // Exchange Rate if available in Go7 (Sample didn't explicitly show it in top
        // level clearly, but usually calculated or static)
        // response.setExchangeRate("239.10915543"); // Hardcoded from example or
        // derived? Leaving as example
        // default/placeholder

        response.setPaymentTimeLimit(formatDateTime(booking.getPnrttl()));

        response.setStatusCode(go7Response.getAerocrs().isSuccess() ? "OPENED" : "REJECTED");
        response.setValidatingCarrier(carrierCode);

        // 2. Booking References
        List<BookingReferences> refs = new ArrayList<>();
        BookingReferences ref1 = new BookingReferences();
        ref1.setId(booking.getPnrref());
        ref1.setOtherId("F1");
        refs.add(ref1);

        BookingReferences ref2 = new BookingReferences();
        ref2.setId(booking.getPnrref());
        ref2.setAirlineId("G7");
        refs.add(ref2);

        response.setBookingReferences(refs);

        // Remarks
        // Remarks mapping removed as per request to avoid specific Service Contact
        // details
        // {
        // List<String> remarkTexts = booking.getRemarks().getRemark().stream()
        // .map(Remark::getText)
        // }

        // 3. ODs (Flights)
        List<OD> ods = new ArrayList<>();
        List<PriceClass> priceClasses = new ArrayList<>();

        if (flightList != null) {
            int segmentCounter = 1;
            int odCounter = 1;

            for (Flight flight : flightList) {
                OD od = new OD();
                od.setSegmentId("S" + segmentCounter);
                od.setOdKey("OD" + odCounter);

                od.setOrigin(flight.getFromcode());
                od.setOriginAirportName(flight.getFrom());
                od.setDestination(flight.getTocode());
                od.setDestinationAirportName(flight.getTo());

                // Calculate dates
                String formattedDepDate = OrderMappingUtil.formatDate(flight.getFlightdate());
                String formattedArrDate = formattedDepDate; // Default to same day

                // Handle overnight arrival for date calculation
                if (flight.getDepart() != null && flight.getArrive() != null) {
                    try {
                        LocalTime depTime = LocalTime.parse(flight.getDepart());
                        LocalTime arrTime = LocalTime.parse(flight.getArrive());
                        if (arrTime.isBefore(depTime)) {
                            // Arrives next day
                            formattedArrDate = OrderMappingUtil.formatDate(OrderMappingUtil.adjustDateByDays(flight.getFlightdate(), 1));
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
                        OrderMappingUtil.calculateJourneyTime(flight.getFlightdate(), flight.getDepart(), flight.getFlightdate(),
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
                PriceClass pc = new PriceClass();
                pc.setPriceClassId(priceClassId);
                pc.setClassName(className);
                pc.setCabinTypeCode(cabinCode.equals("Economy") ? "ECO" : cabinCode);

                List<PriceClass.Description> descriptions = new ArrayList<>();
                if (flight.getServices() != null) {
                    for (java.util.Map.Entry<String, Boolean> entry : flight.getServices().entrySet()) {
                        PriceClass.Description d = new PriceClass.Description();
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
        List<PaxDetailDTO> paxList = new ArrayList<>();
        List<Passenger> go7PaxList = null;

        if (booking.getPassengers() != null && booking.getPassengers().getPassenger() != null) {
            go7PaxList = booking.getPassengers().getPassenger();
        }

        if (go7PaxList != null) {
            int paxCounter = 1;
            List<TicketDocInfoDTO> topLevelTicketDocs = new ArrayList<>();
            List<EMDInfoDTO> topLevelEmdInfos = new ArrayList<>();
            List<PaxDetailDTO> adtList = new ArrayList<>();

            for (Passenger go7Pax : go7PaxList) {
                PaxDetailDTO pax = new PaxDetailDTO();

                String rawTitle = go7Pax.getPaxtitle() != null ? go7Pax.getPaxtitle().toUpperCase().replace(".", "")
                        : "MR";
                boolean isInfantByTitle = rawTitle.contains("INF");

                String assignedPtc = OrderMappingUtil.mapPaxType(go7Pax.getPaxtype());
                if (isInfantByTitle) {
                    assignedPtc = "INF";
                }

                String paxId;
                if ("INF".equals(assignedPtc)) {
                    if (!adtList.isEmpty()) {
                        PaxDetailDTO parent = adtList.get(adtList.size() - 1);
                        paxId = parent.getPaxId() + ".1";
                        parent.setInfantRef(paxId);
                    } else {
                        paxId = "T" + paxCounter++ + ".1";
                    }
                } else {
                    paxId = "T" + paxCounter++;
                    if ("ADT".equals(assignedPtc)) {
                        adtList.add(pax);
                    }
                }

                pax.setPaxId(paxId);
                pax.setPtc(assignedPtc);
                pax.setGivenName(go7Pax.getFirstname() != null ? go7Pax.getFirstname().toUpperCase() : "");
                pax.setSurname(go7Pax.getLastname() != null ? go7Pax.getLastname().toUpperCase() : "");
                pax.setTitle(rawTitle);

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

                pax.setBirthDate(OrderMappingUtil.formatDate(go7Pax.getDob()));
                pax.setLanguage("English");

                if (go7Pax.getContact() != null) {
                    PaxDetailDTO.PhoneDTO phone = new PaxDetailDTO.PhoneDTO();
                    phone.setPhoneNumber(go7Pax.getContact());
                    phone.setType("Operational");
                    phone.setLabel("Mobile");
                    List<PaxDetailDTO.PhoneDTO> phones = new ArrayList<>();
                    phones.add(phone);
                    pax.setPhones(phones);
                }

                if (go7Pax.getEmail() != null) {
                    PaxDetailDTO.EmailDTO email = new PaxDetailDTO.EmailDTO();
                    email.setEmailAddress(go7Pax.getEmail().toUpperCase());
                    email.setType("Operational");
                    List<PaxDetailDTO.EmailDTO> emails = new ArrayList<>();
                    emails.add(email);
                    pax.setEmails(emails);
                }

                // Map TicketDocInfo (E-Tickets)
                if (go7Pax.getETickets() != null && go7Pax.getETickets().getFlight() != null) {
                    List<TicketDocInfoDTO> paxTicketDocs = new ArrayList<>();

                    java.util.Set<String> uniqueTickets = new java.util.LinkedHashSet<>();
                    for (Passenger.ETicketFlight etf : go7Pax.getETickets().getFlight()) {
                        if (etf.getEticketnumber() != null && !etf.getEticketnumber().isEmpty()) {
                            uniqueTickets.add(etf.getEticketnumber().trim());
                        }
                    }

                    for (String ticketNbr : uniqueTickets) {
                        TicketDocInfoDTO tdi = new TicketDocInfoDTO();
                        String issuingDesig = "API Airways";
                        String issuingPlace = "TLV";
                        if (flightList != null && !flightList.isEmpty()) {
                            Flight f0 = flightList.get(0);
                            if (f0.getAirline() != null)
                                issuingDesig = f0.getAirline();
                            if (f0.getFromcode() != null)
                                issuingPlace = f0.getFromcode();
                        }
                        tdi.setIssuingAirlineName(issuingDesig);
                        tdi.setIssuingPlace(issuingPlace);
                        tdi.setValidatingCarrier("G7");
                        List<String> pIds = new ArrayList<>();
                        pIds.add(paxId);
                        tdi.setPaxId(pIds);

                        List<TicketDocInfoDTO.TicketDocumentDTO> docs = new ArrayList<>();
                        TicketDocInfoDTO.TicketDocumentDTO doc = new TicketDocInfoDTO.TicketDocumentDTO();
                        doc.setTicketDocNbr(ticketNbr);
                        doc.setType("T");
                        doc.setNumberOfBooklets(1);
                        doc.setDateOfIssue(OrderMappingUtil.formatDate(java.time.LocalDate.now().toString()));
                        doc.setTimeOfIssue(java.time.LocalTime.now()
                                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
                        doc.setTicketingLocation(issuingPlace);
                        doc.setReportingType("BSP");

                        // Coupons
                        List<TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO> coupons = new ArrayList<>();
                        if (flightList != null) {
                            int couponNum = 1;
                            for (Flight f : flightList) {
                                TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO coupon = new TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO();
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

                                TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO cai = new TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO();
                                cai.setDepartureAirportCode(f.getFromcode());
                                cai.setArrivalAirportCode(f.getTocode());
                                cai.setDepartureDate(OrderMappingUtil.formatDate(f.getFlightdate()));
                                cai.setDepartureTime(f.getDepart());
                                cai.setDepartureAirportName(f.getFrom());
                                cai.setDepartureTerminal(f.getDepartureTerminal());
                                cai.setArrivalDate(OrderMappingUtil.formatDate(f.getFlightdate()));
                                cai.setArrivalTime(f.getArrive());
                                cai.setArrivalAirportName(f.getTo());
                                cai.setArrivalTerminal(f.getArrivalTerminal());
                                cai.setMarketingCarrierAirlineId(f.getAirlinedesignator());
                                cai.setMarketingCarrierName(f.getAirline());
                                cai.setOperatingCarrierAirlineId(f.getAirlinedesignator());
                                cai.setOperatingCarrierName(f.getAirline());
                                cai.setFlightNumber(f.getNumber());
                                cai.setEquipmentAircraftCode(
                                        f.getAircraftTypeIataCode() != null ? f.getAircraftTypeIataCode()
                                                : f.getAircraftType());

                                List<TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO> caiList = new ArrayList<>();
                                caiList.add(cai);
                                coupon.setCurrentAirlineInfo(caiList);

                                List<BaggageAllowance> bags = new ArrayList<>();
                                if (f.getServices() != null && f.getServices().containsKey("CheckedInBaggage")
                                        && Boolean.TRUE.equals(f.getServices().get("CheckedInBaggage"))) {
                                    BaggageAllowance bag = new BaggageAllowance();
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
                        paxTicketDocs.add(tdi);
                        topLevelTicketDocs.add(tdi);
                    }
                    pax.setTicketDocInfo(paxTicketDocs);
                }

                // EMD Info Mapping (Seats & Services)
                if (flightList != null) {
                    List<EMDInfoDTO> paxEmds = new ArrayList<>();

                    // Build priority map from ChangePayment flow
                    java.util.LinkedHashMap<String, String> combinedEmdMap = new java.util.LinkedHashMap<>();
                    if (passengerSeats != null) {
                        for (java.util.Map.Entry<String, String> entry : passengerSeats.entrySet()) {
                            combinedEmdMap.put(entry.getKey() + "_SEAT", entry.getValue());
                        }
                    }
                    if (passengerServices != null) {
                        for (java.util.Map.Entry<String, String> entry : passengerServices.entrySet()) {
                            combinedEmdMap.put(entry.getKey() + "_SRV", entry.getValue());
                        }
                    }

                    if (!combinedEmdMap.isEmpty()) {
                        // Priority 1: Use DB maps populated by ChangeSeat / ChangeService
                        List<String> assignedValues = new ArrayList<>();
                        for (java.util.Map.Entry<String, String> entry : combinedEmdMap.entrySet()) {
                            String paxIdKey = entry.getKey().substring(0, entry.getKey().lastIndexOf("_"));
                            if (paxIdKey.equals(paxId)) {
                                assignedValues.add(entry.getValue());
                            }
                        }

                        for (String assignedValue : assignedValues) {
                            EMDInfoDTO emd = new EMDInfoDTO();
                            emd.setValidatingCarrier("G7");
                            emd.setPaxId(Arrays.asList(paxId));

                            String issuingDesig = "API Airways";
                            String issuingPlace = "TLV";
                            Flight f0 = flightList.get(0);
                            if (f0.getAirline() != null)
                                issuingDesig = f0.getAirline();
                            if (f0.getFromcode() != null)
                                issuingPlace = f0.getFromcode();

                            emd.setIssuingAirlineName(issuingDesig);
                            emd.setIssuingPlace(issuingPlace);

                            List<EMDInfoDTO.TicketDocumentDTO> emdDocs = new ArrayList<>();
                            EMDInfoDTO.TicketDocumentDTO emdDoc = new EMDInfoDTO.TicketDocumentDTO();

                            String baseTkt = (go7Pax.getETickets() != null && go7Pax.getETickets().getFlight() != null
                                    && !go7Pax.getETickets().getFlight().isEmpty())
                                            ? go7Pax.getETickets().getFlight().get(0).getEticketnumber()
                                            : "EMD";

                            emdDoc.setTicketDocNbr(baseTkt + "-" + (topLevelEmdInfos.size() + 1));
                            emdDoc.setType("J");
                            emdDoc.setNumberOfBooklets(1);
                            emdDoc.setDateOfIssue(OrderMappingUtil.formatDate(java.time.LocalDate.now().toString()));
                            emdDoc.setTimeOfIssue("00:00");
                            emdDoc.setTicketingLocation(issuingPlace);
                            emdDoc.setReportingType("BSP");

                            List<EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO> emdCoupons = new ArrayList<>();
                            EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO emdCoupon = new EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO();
                            emdCoupon.setCouponNumber(1);
                            emdCoupon.setValidatingCarrier("G7");
                            emdCoupon.setStatus("I");

                            String refVal = assignedValue;
                            if (refVal != null && refVal.startsWith("SRV_")) {
                                refVal = refVal.substring(4);
                            }
                            emdCoupon.setServiceRefs(Arrays.asList(refVal));

                            List<EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO> caiList = new ArrayList<>();
                            EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO cai = new EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO();
                            cai.setDepartureAirportCode(f0.getFromcode());
                            cai.setArrivalAirportCode(f0.getTocode());
                            cai.setDepartureDate(OrderMappingUtil.formatDate(f0.getFlightdate()));
                            cai.setDepartureTime(f0.getDepart());
                            cai.setDepartureAirportName(f0.getFrom());
                            cai.setArrivalAirportName(f0.getTo());
                            cai.setFlightNumber(f0.getNumber());
                            cai.setMarketingCarrierAirlineId(f0.getAirlinedesignator());
                            caiList.add(cai);
                            emdCoupon.setCurrentAirlineInfo(caiList);

                            emdCoupons.add(emdCoupon);
                            emdDoc.setCouponInfo(emdCoupons);
                            emdDocs.add(emdDoc);
                            emd.setTicketDocument(emdDocs);

                            paxEmds.add(emd);
                            topLevelEmdInfos.add(emd);
                        }

                    } else {
                        // Priority 2: Fallback to existing Go7 Flight/Checkin parsing
                        int seatCounterGlobal = 0;
                        for (Flight f : flightList) {
                            if (f.getSeat() != null) {
                                int seatIdxInFlight = 0;
                                for (Seat s : f.getSeat()) {
                                    int currentPaxIndex = paxCounter - 2;

                                    if (seatIdxInFlight == currentPaxIndex) {
                                        EMDInfoDTO emd = new EMDInfoDTO();
                                        emd.setValidatingCarrier("G7");
                                        emd.setPaxId(Arrays.asList(paxId));

                                        String issuingDesig = "API Airways";
                                        String issuingPlace = "TLV";
                                        if (f.getAirline() != null)
                                            issuingDesig = f.getAirline();
                                        if (f.getFromcode() != null)
                                            issuingPlace = f.getFromcode();

                                        emd.setIssuingAirlineName(issuingDesig);
                                        emd.setIssuingPlace(issuingPlace);

                                        List<EMDInfoDTO.TicketDocumentDTO> emdDocs = new ArrayList<>();
                                        EMDInfoDTO.TicketDocumentDTO emdDoc = new EMDInfoDTO.TicketDocumentDTO();

                                        String baseTkt = (go7Pax.getETickets() != null
                                                && go7Pax.getETickets().getFlight() != null
                                                && !go7Pax.getETickets().getFlight().isEmpty())
                                                        ? go7Pax.getETickets().getFlight().get(0).getEticketnumber()
                                                        : "EMD";

                                        emdDoc.setTicketDocNbr(baseTkt + "-" + (topLevelEmdInfos.size() + 1));
                                        emdDoc.setType("J");
                                        emdDoc.setNumberOfBooklets(1);
                                        emdDoc.setDateOfIssue(OrderMappingUtil.formatDate(java.time.LocalDate.now().toString()));
                                        emdDoc.setTimeOfIssue("00:00");
                                        emdDoc.setTicketingLocation(issuingPlace);
                                        emdDoc.setReportingType("BSP");

                                        List<EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO> emdCoupons = new ArrayList<>();
                                        EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO emdCoupon = new EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO();
                                        emdCoupon.setCouponNumber(1);
                                        emdCoupon.setValidatingCarrier("G7");
                                        emdCoupon.setStatus("I");
                                        emdCoupon.setServiceRefs(Arrays.asList(s.getSeatNumber()));

                                        List<EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO> caiList = new ArrayList<>();
                                        EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO cai = new EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO();
                                        cai.setDepartureAirportCode(f.getFromcode());
                                        cai.setArrivalAirportCode(f.getTocode());
                                        cai.setDepartureDate(OrderMappingUtil.formatDate(f.getFlightdate()));
                                        cai.setDepartureTime(f.getDepart());
                                        cai.setDepartureAirportName(f.getFrom());
                                        cai.setArrivalAirportName(f.getTo());
                                        cai.setFlightNumber(f.getNumber());
                                        cai.setMarketingCarrierAirlineId(f.getAirlinedesignator());
                                        caiList.add(cai);
                                        emdCoupon.setCurrentAirlineInfo(caiList);

                                        emdCoupons.add(emdCoupon);
                                        emdDoc.setCouponInfo(emdCoupons);
                                        emdDocs.add(emdDoc);
                                        emd.setTicketDocument(emdDocs);

                                        paxEmds.add(emd);
                                        topLevelEmdInfos.add(emd);
                                    }
                                    seatIdxInFlight++;
                                }
                            }
                        }

                        if (paxEmds.isEmpty() && go7Pax.getCheckin() != null && !go7Pax.getCheckin().isEmpty()) {
                            for (Passenger.Checkin chk : go7Pax.getCheckin()) {
                                if (chk.getSeat() != null && !chk.getSeat().isEmpty()) {
                                    EMDInfoDTO emd = new EMDInfoDTO();
                                    emd.setValidatingCarrier("G7");
                                    emd.setPaxId(Arrays.asList(paxId));

                                    Flight matchedFlight = null;
                                    if (chk.getFlight() != null) {
                                        for (Flight f : flightList) {
                                            if (f.getNumber() != null && chk.getFlight().contains(f.getNumber())) {
                                                matchedFlight = f;
                                                break;
                                            }
                                        }
                                    }
                                    if (matchedFlight == null && !flightList.isEmpty())
                                        matchedFlight = flightList.get(0);

                                    String issuingDesig = "API Airways";
                                    String issuingPlace = "TLV";
                                    if (matchedFlight != null) {
                                        if (matchedFlight.getAirline() != null)
                                            issuingDesig = matchedFlight.getAirline();
                                        if (matchedFlight.getFromcode() != null)
                                            issuingPlace = matchedFlight.getFromcode();
                                    }

                                    emd.setIssuingAirlineName(issuingDesig);
                                    emd.setIssuingPlace(issuingPlace);

                                    List<EMDInfoDTO.TicketDocumentDTO> emdDocs = new ArrayList<>();
                                    EMDInfoDTO.TicketDocumentDTO emdDoc = new EMDInfoDTO.TicketDocumentDTO();

                                    String baseTkt = (go7Pax.getETickets() != null
                                            && go7Pax.getETickets().getFlight() != null
                                            && !go7Pax.getETickets().getFlight().isEmpty())
                                                    ? go7Pax.getETickets().getFlight().get(0).getEticketnumber()
                                                    : "EMD";

                                    emdDoc.setTicketDocNbr(baseTkt + "-" + (topLevelEmdInfos.size() + 1));
                                    emdDoc.setType("J");
                                    emdDoc.setNumberOfBooklets(1);
                                    emdDoc.setDateOfIssue(OrderMappingUtil.formatDate(java.time.LocalDate.now().toString()));
                                    emdDoc.setTimeOfIssue("00:00");
                                    emdDoc.setTicketingLocation(issuingPlace);
                                    emdDoc.setReportingType("BSP");

                                    List<EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO> emdCoupons = new ArrayList<>();
                                    EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO emdCoupon = new EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO();
                                    emdCoupon.setCouponNumber(1);
                                    emdCoupon.setValidatingCarrier("G7");
                                    emdCoupon.setStatus("I");
                                    emdCoupon.setServiceRefs(Arrays.asList(chk.getSeat()));

                                    if (matchedFlight != null) {
                                        List<EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO> caiList = new ArrayList<>();
                                        EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO cai = new EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO();
                                        cai.setDepartureAirportCode(matchedFlight.getFromcode());
                                        cai.setArrivalAirportCode(matchedFlight.getTocode());
                                        cai.setDepartureDate(OrderMappingUtil.formatDate(matchedFlight.getFlightdate()));
                                        cai.setDepartureTime(matchedFlight.getDepart());
                                        cai.setDepartureAirportName(matchedFlight.getFrom());
                                        cai.setArrivalAirportName(matchedFlight.getTo());
                                        cai.setFlightNumber(matchedFlight.getNumber());
                                        cai.setMarketingCarrierAirlineId(matchedFlight.getAirlinedesignator());
                                        caiList.add(cai);
                                        emdCoupon.setCurrentAirlineInfo(caiList);
                                    }

                                    emdCoupons.add(emdCoupon);
                                    emdDoc.setCouponInfo(emdCoupons);
                                    emdDocs.add(emdDoc);
                                    emd.setTicketDocument(emdDocs);

                                    paxEmds.add(emd);
                                    topLevelEmdInfos.add(emd);
                                }
                            }
                        }
                    }

                    if (!paxEmds.isEmpty()) {
                        pax.setEmdInfo(paxEmds);
                    }
                }

                paxList.add(pax);
            }
            if (!topLevelTicketDocs.isEmpty()) {
                response.setTicketDocInfoList(topLevelTicketDocs);
            }
            if (!topLevelEmdInfos.isEmpty()) {
                response.setEmdInfoList(topLevelEmdInfos);
            }
        }
        response.setPaxDetailList(paxList);

        // 5. Order Items
        // 5. Order Items
        List<OrderItemsDTO> orderItems = new ArrayList<>();
        int itemIdx = 1;

        // Group Pax
        List<String> adtRefs = new ArrayList<>();
        List<String> cnnRefs = new ArrayList<>();
        List<String> infRefs = new ArrayList<>();

        for (PaxDetailDTO p : paxList) {
            String pid = p.getPaxId();
            if ("ADT".equals(p.getPtc())) {
                adtRefs.add(pid);
            } else if ("CHD".equals(p.getPtc())) {
                cnnRefs.add(pid);
            } else if ("INF".equals(p.getPtc())) {
                infRefs.add(pid);
            }
        }

        List<Flight> flightListRef = flightList;
        String currency = booking.getCurrency();
        if (currency == null)
            currency = "USD";

        BigDecimal airTotalForFallback = null;
        if (booking.getBalanceInformation() != null && booking.getBalanceInformation().getPnrTotal() != null) {
            airTotalForFallback = booking.getBalanceInformation().getPnrTotal();
        } else if (booking.getTotalprice() != null) {
            try {
                airTotalForFallback = new BigDecimal(String.valueOf(booking.getTotalprice()));
            } catch (Exception e) {}
        }

        // ADT Item
        if (!adtRefs.isEmpty()) {
            OrderItemsDTO item = createOrderItem(response.getResponseId(), itemIdx++, "ADT", adtRefs,
                    flightListRef, currency, booking, airTotalForFallback);
            orderItems.add(item);
        }

        // CHD Item
        if (!cnnRefs.isEmpty()) {
            OrderItemsDTO item = createOrderItem(response.getResponseId(), itemIdx++, "CHD", cnnRefs,
                    flightListRef, currency, booking, airTotalForFallback);
            orderItems.add(item);
        }

        // INF Item
        if (!infRefs.isEmpty()) {
            OrderItemsDTO item = createOrderItem(response.getResponseId(), itemIdx++, "INF", infRefs,
                    flightListRef, currency, booking, airTotalForFallback);
            orderItems.add(item);
        }

        // Generate SRV items from passengerSeats and passengerServices
        java.util.LinkedHashMap<String, String> combinedSrvMap = new java.util.LinkedHashMap<>();
        if (passengerSeats != null) {
            for (java.util.Map.Entry<String, String> entry : passengerSeats.entrySet()) {
                combinedSrvMap.put(entry.getKey() + "_SEAT", entry.getValue());
            }
        }
        if (passengerServices != null) {
            for (java.util.Map.Entry<String, String> entry : passengerServices.entrySet()) {
                                        combinedSrvMap.put(entry.getKey() + "_SRV", entry.getValue());
            }
        }

        if (!combinedSrvMap.isEmpty() && flightList != null && !flightList.isEmpty()) {
            // Calculate distributed price as fallback
            BigDecimal totalAirItemsPrice = BigDecimal.ZERO;
            for (OrderItemsDTO o : orderItems) {
                if (o.getTotalPrice() != null) {
                    totalAirItemsPrice = totalAirItemsPrice.add(o.getTotalPrice());
                }
            }

            BigDecimal bookingTotal = BigDecimal.ZERO;
            if (dbTotalOrderPrice != null) {
                bookingTotal = dbTotalOrderPrice;
            } else if (booking.getBalanceInformation() != null && booking.getBalanceInformation().getPnrTotal() != null) {
                bookingTotal = booking.getBalanceInformation().getPnrTotal();
            } else if (booking.getTotalprice() != null) {
                try {
                    bookingTotal = new BigDecimal(booking.getTotalprice());
                } catch (Exception e) {}
            }

            BigDecimal secondaryTotal = bookingTotal.subtract(totalAirItemsPrice);
            if (secondaryTotal.compareTo(BigDecimal.ZERO) < 0) secondaryTotal = BigDecimal.ZERO;

            BigDecimal distributedItemPrice = BigDecimal.ZERO;
            if (secondaryTotal.compareTo(BigDecimal.ZERO) > 0) {
                // Filter out items that already have a price from the cache
                long unpaidCount = combinedSrvMap.values().stream().filter(v -> !v.contains("|") || new BigDecimal(v.split("\\|")[1]).compareTo(BigDecimal.ZERO) == 0).count();
                if (unpaidCount > 0) {
                    distributedItemPrice = secondaryTotal.divide(new BigDecimal(unpaidCount), 2, java.math.RoundingMode.HALF_UP);
                }
            }

            for (java.util.Map.Entry<String, String> paxSecondary : combinedSrvMap.entrySet()) {
                String rawKey = paxSecondary.getKey();
                String rawValue = paxSecondary.getValue();
                
                boolean isSeat = rawKey.endsWith("_SEAT");
                String assignedPaxId = rawKey.substring(0, rawKey.lastIndexOf("_"));
                
                String itemCode = rawValue;
                BigDecimal itemPrice = distributedItemPrice;
                
                if (rawValue != null && rawValue.contains("|")) {
                    String[] parts = rawValue.split("\\|");
                    itemCode = parts[0];
                    try {
                        BigDecimal storedPrice = new BigDecimal(parts[1]);
                        if (storedPrice.compareTo(BigDecimal.ZERO) > 0) itemPrice = storedPrice;
                    } catch (Exception e) {}
                }

                OrderItemsDTO srvItem = new OrderItemsDTO();
                srvItem.setOrderItemId(response.getOrderId() + "_SRV" + itemIdx++);

                String ptc = "ADT";
                for (PaxDetailDTO p : response.getPaxDetailList()) {
                    if (p.getPaxId().equals(assignedPaxId)) {
                        ptc = p.getPtc();
                        break;
                    }
                }

                srvItem.setPtc(ptc);
                srvItem.setPassengerIds(java.util.Arrays.asList(assignedPaxId));
                srvItem.setTotalPrice(itemPrice);
                srvItem.setTotalFare(new OrderItemsDTO.TotalFare(itemPrice, currency));
                srvItem.setBaseFare(new OrderItemsDTO.BaseFare(itemPrice, currency));

                List<Service> srvList = new ArrayList<>();
                Service srv = new Service();

                if (isSeat) {
                    srv.setServiceId("SEG1_" + assignedPaxId);
                    srv.setServiceStatus("CONFIRMED");
                    srv.setServiceCode("SEAT" + itemCode);
                    srv.setServiceName("Specific Seat Request");
                    srv.setSegmentId("SEG1");
                    
                    if (itemCode != null && itemCode.length() > 0) {
                        String col = itemCode.substring(itemCode.length() - 1);
                        String rowStr = itemCode.substring(0, itemCode.length() - 1);
                        srv.setColumn(col);
                        try {
                            srv.setRow(new java.math.BigInteger(rowStr));
                        } catch (Exception e) {}
                    }
                } else {
                    srv.setServiceId(itemCode);
                    srv.setServiceStatus("CONFIRMED");
                    srv.setServiceCode("SRV");
                    srv.setServiceName("Ancillary Service");
                }

                srvList.add(srv);
                srvItem.setServiceList(srvList);
                orderItems.add(srvItem);
            }
        }

        response.setOrderItems(orderItems);

        // Aggregate final totalOrderPrice from all generated OrderItems
        BigDecimal aggregatedTotal = BigDecimal.ZERO;
        for (OrderItemsDTO o : orderItems) {
            if (o.getTotalPrice() != null) {
                aggregatedTotal = aggregatedTotal.add(o.getTotalPrice());
            }
        }
        response.setTotalOrderPrice(aggregatedTotal.setScale(2, java.math.RoundingMode.HALF_UP));
        
        // 7. Separate Payments (Added per user request to match ChangePayment structure)
        List<PaymentsDTO> payments = new ArrayList<>();
        boolean isPaid = false;
        if (booking.getBalanceInformation() != null) {
            isPaid = booking.getBalanceInformation().getPnrOutstandingPayment() <= 0;
        }

        for (OrderItemsDTO o : orderItems) {
            if (o.getTotalPrice() != null && o.getTotalPrice().compareTo(BigDecimal.ZERO) > 0) {
                PaymentsDTO payItem = new PaymentsDTO();
                payItem.setType("CC");
                payItem.setStatusCode(isPaid ? "SUCCESSFUL" : "PENDING");
                payItem.setAmount(o.getTotalPrice().setScale(2, java.math.RoundingMode.HALF_UP));
                payItem.setCurrency(response.getCurrency());
                payItem.setOrderItem(Arrays.asList(o.getOrderItemId()));
                payments.add(payItem);
            }
        }
        response.setPayments(payments);

        return response;
    }

    

    

    

    

    private static OrderItemsDTO createOrderItem(String responseId, int itemIndex, String ptc,
            List<String> paxIds, List<Flight> flights, String currency,
            Booking booking, java.math.BigDecimal dbTotalOrderPrice) {
        OrderItemsDTO item = new OrderItemsDTO();
        item.setOrderItemId(responseId + "_AIR-" + itemIndex);
        item.setPtc(ptc);

        String mainClassName = "Economy";
        if (flights != null && !flights.isEmpty()) {
            Flight f = flights.get(0);
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

        List<OrderItemsDTO.Tax> taxList = new ArrayList<>();

        if (flights != null) {
            for (Flight f : flights) {
                // Base Fare
                String fareStr = null;
                boolean isAdt = "ADT".equals(ptc);
                boolean isCnn = "CHD".equals(ptc) || "CNN".equals(ptc); // Handle both CHD and CNN
                boolean isInf = "INF".equals(ptc);

                if (isAdt)
                    fareStr = f.getAdultfare();
                else if (isCnn)
                    fareStr = f.getChildfare();
                else if (isInf)
                    fareStr = f.getInfantfare();

                // Fallback logic
                if (fareStr == null || fareStr.isEmpty() || "0".equals(fareStr)) {
                    if (f.getInvpricingwithouttax() != null && !f.getInvpricingwithouttax().isEmpty()) {
                        fareStr = f.getInvpricingwithouttax();
                    } else if (isAdt) {
                        fareStr = f.getNetFare();
                    }
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

                // Tax Splitting Logic
                BigDecimal flightTotalTax = BigDecimal.valueOf(f.getTotaltaxes());
                int totalPaxCount = booking.getAdults() + booking.getChild() + booking.getInfant();
                if (totalPaxCount == 0 && booking.getPassengers() != null
                        && booking.getPassengers().getPassenger() != null) {
                    totalPaxCount = booking.getPassengers().getPassenger().size();
                }

                if (totalPaxCount > 0) {
                    unitTax = unitTax.add(
                            flightTotalTax.divide(new BigDecimal(totalPaxCount), 2, java.math.RoundingMode.HALF_UP));
                } else {
                    unitTax = unitTax.add(flightTotalTax);
                }
            }
        }

        // --- NEW FALLBACK FOR BASE FARE IF IT IS 0 AND NO EXPLICIT FARES WERE GIVEN
        // ---
        if (unitBase.compareTo(BigDecimal.ZERO) == 0) {
            try {
                BigDecimal totalBookingPrice = BigDecimal.ZERO;
                if (dbTotalOrderPrice != null) {
                    totalBookingPrice = dbTotalOrderPrice;
                } else if (booking.getBalanceInformation() != null && booking.getBalanceInformation().getPnrTotal() != null) {
                    totalBookingPrice = booking.getBalanceInformation().getPnrTotal();
                } else if (booking.getTotalprice() != null) {
                    totalBookingPrice = new BigDecimal(booking.getTotalprice());
                }

                // Calculate total tax for the entire booking
                BigDecimal totalBookingTax = BigDecimal.ZERO;
                if (flights != null) {
                    for (Flight f : flights) {
                        totalBookingTax = totalBookingTax.add(BigDecimal.valueOf(f.getTotaltaxes()));
                    }
                }

                BigDecimal totalBookingBaseFare = totalBookingPrice.subtract(totalBookingTax);
                if (totalBookingBaseFare.compareTo(BigDecimal.ZERO) > 0) {
                    int totalPaxCount = booking.getAdults() + booking.getChild() + booking.getInfant();
                    if (totalPaxCount == 0 && booking.getPassengers() != null
                            && booking.getPassengers().getPassenger() != null) {
                        totalPaxCount = booking.getPassengers().getPassenger().size();
                    }
                    if (totalPaxCount > 0) {
                        unitBase = totalBookingBaseFare.divide(new BigDecimal(totalPaxCount), 2,
                                java.math.RoundingMode.HALF_UP);
                    } else {
                        unitBase = totalBookingBaseFare;
                    }
                }
            } catch (Exception e) {
            }
        }
        // ---------------------------------------------------------------------------------

        int count = paxIds.size();
        BigDecimal totalBase = unitBase.multiply(new BigDecimal(count));
        BigDecimal totalTaxAmount = unitTax.multiply(new BigDecimal(count));
        BigDecimal totalPrice = totalBase.add(totalTaxAmount);

        item.setTotalPrice(totalPrice.setScale(2, java.math.RoundingMode.HALF_UP));

        // Taxes Breakdown (Apply Count Multiplier and Proportional Scaling)
        if (flights != null) {
            BigDecimal totalGranularTaxForAllPax = BigDecimal.ZERO;
            for (Flight f : flights) {
                if (f.getTaxes() != null) {
                    Taxes t = f.getTaxes();
                    totalGranularTaxForAllPax = totalGranularTaxForAllPax
                            .add(BigDecimal.valueOf(t.getGroundHandling()));
                    totalGranularTaxForAllPax = totalGranularTaxForAllPax.add(BigDecimal.valueOf(t.getSecurity()));
                    totalGranularTaxForAllPax = totalGranularTaxForAllPax.add(BigDecimal.valueOf(t.getFuel()));
                    totalGranularTaxForAllPax = totalGranularTaxForAllPax.add(BigDecimal.valueOf(t.getTax1()));
                    totalGranularTaxForAllPax = totalGranularTaxForAllPax.add(BigDecimal.valueOf(t.getTax4()));
                }
            }
            totalGranularTaxForAllPax = totalGranularTaxForAllPax.multiply(new BigDecimal(count));

            BigDecimal scaleFactor = BigDecimal.ONE;
            if (totalGranularTaxForAllPax.compareTo(BigDecimal.ZERO) > 0) {
                scaleFactor = totalTaxAmount.divide(totalGranularTaxForAllPax, 10, java.math.RoundingMode.HALF_UP);
            }

            for (Flight f : flights) {
                if (f.getTaxes() != null) {
                    Taxes t = f.getTaxes();
                    addScaledTaxItem(taxList, "GH", t.getGroundHandling(), scaleFactor, count, currency,
                            "Ground Handling");
                    addScaledTaxItem(taxList, "I2", t.getSecurity(), scaleFactor, count, currency, "Security Tax");
                    addScaledTaxItem(taxList, "YQ", t.getFuel(), scaleFactor, count, currency, "Fuel Surcharge");
                    addScaledTaxItem(taxList, "IN", t.getTax1(), scaleFactor, count, currency, "Infrastructure Tax");
                    addScaledTaxItem(taxList, "OT", t.getTax4(), scaleFactor, count, currency, "Other Tax");
                }
            }
        }
        item.setTaxes(taxList);

        OrderItemsDTO.TotalTax totalTaxObj = new OrderItemsDTO.TotalTax();
        totalTaxObj.setAmount(totalTaxAmount.setScale(2, java.math.RoundingMode.HALF_UP));
        totalTaxObj.setCurrency(currency);
        item.setTotalTax(totalTaxObj);

        OrderItemsDTO.BaseFare baseFareObj = new OrderItemsDTO.BaseFare();
        baseFareObj.setAmount(totalBase.setScale(2, java.math.RoundingMode.HALF_UP));
        baseFareObj.setCurrency(currency);
        item.setBaseFare(baseFareObj);

        OrderItemsDTO.TotalFare totalFareObj = new OrderItemsDTO.TotalFare();
        totalFareObj.setAmount(totalPrice.setScale(2, java.math.RoundingMode.HALF_UP));
        totalFareObj.setCurrency(currency);
        item.setTotalFare(totalFareObj);

        return item;
    }

    private static void addScaledTaxItem(List<OrderItemsDTO.Tax> list, String code, double amount,
            BigDecimal scaleFactor, int count, String currency, String description) {
        if (amount > 0) {
            OrderItemsDTO.Tax t = new OrderItemsDTO.Tax();
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
                return OrderMappingUtil.formatDate(dateTimeStr);
            } catch (Exception ex) {
                return dateTimeStr; // Return raw if all fails
            }
        }
    }
}