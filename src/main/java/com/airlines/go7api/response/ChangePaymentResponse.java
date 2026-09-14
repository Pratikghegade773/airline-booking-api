package com.airlines.go7api.response;

import com.airlines.go7api.responsego7.common.*;
import com.airlines.go7api.responsedto.common.*;
import com.airlines.go7api.requestdto.ChangePaymentReqDto;
import com.airlines.go7api.responsedto.ChangePaymentRspDto;
import com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChangePaymentResponse {

    private static final String CHECKED_IN_BAGGAGE = "CheckedInBaggage";
    private static final String BAG_ALLOWANCES = "Bag allowances";
    private static final String SUFFIX_SEAT = "_SEAT";
    private static final String SUFFIX_SRV = "_SRV";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_SUCCESSFUL = "SUCCESSFUL";

    private ChangePaymentResponse() {
        throw new IllegalStateException("Utility class");
    }

    public static ChangePaymentRspDto generateResponse(ChangePaymentRspGo7Dto go7Response,
            ChangePaymentReqDto requestDto, java.util.Map<String, String> passengerSeats,
            java.util.Map<String, String> passengerServices) {
        ChangePaymentRspDto response = new ChangePaymentRspDto();

        if (go7Response == null || go7Response.getAerocrs() == null || go7Response.getAerocrs().getBooking() == null) {
            return response;
        }

        Booking booking = go7Response.getAerocrs().getBooking();

        populateTopLevelFields(response, booking, requestDto, go7Response.getAerocrs().isSuccess());
        populateBookingReferences(response, booking);

        List<Flight> flightList = getFlightList(booking);
        populateOdsAndPriceClasses(response, flightList);

        String issuingAirlineName = getIssuingAirlineName(flightList);
        String issuingPlace = getIssuingPlace(flightList);

        List<TicketDocInfoDTO> topLevelTicketDocs = new ArrayList<>();
        List<PaxDetailDTO> paxList = populatePaxAndTicketDocs(booking, flightList, issuingAirlineName, issuingPlace, topLevelTicketDocs);
        response.setPaxDetailList(paxList);
        response.setTicketDocInfoList(topLevelTicketDocs);

        List<EMDInfoDTO> topEmdInfos = populateEmds(paxList, passengerSeats, passengerServices, flightList, issuingAirlineName, issuingPlace);
        if (!topEmdInfos.isEmpty()) {
            response.setEmdInfoList(topEmdInfos);
        }

        List<OrderItemsDTO> orderItems = new ArrayList<>();
        BigDecimal secondaryCharges = populateOrderItems(orderItems, response, booking, flightList, paxList, passengerSeats, passengerServices);
        response.setOrderItems(orderItems);

        populatePayments(response, requestDto, booking, secondaryCharges);

        return response;
    }

    private static void populateTopLevelFields(ChangePaymentRspDto response, Booking booking,
            ChangePaymentReqDto requestDto, boolean success) {
        response.setResponseId("P" + UUID.randomUUID().toString().substring(0, 15).toUpperCase());

        String requestOrderId = (requestDto.getOrderId() != null && !requestDto.getOrderId().isEmpty())
                ? requestDto.getOrderId()
                : String.valueOf(booking.getBookingid());
        response.setOrderId(requestOrderId);

        response.setPnr(booking.getPnrref());
        response.setApiOwner("G7");
        response.setTotalOrderPrice(BigDecimal.ZERO);
        response.setCurrency(booking.getCurrency() != null ? booking.getCurrency() : "USD");
        response.setPaymentTimeLimit(booking.getPnrttl());
        response.setTicketingTimeLimit(booking.getPnrttl());
        response.setStatusCode(success ? "OPENED" : "REJECTED");
        response.setValidatingCarrier("G7");
    }

    private static void populateBookingReferences(ChangePaymentRspDto response, Booking booking) {
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
    }

    private static List<Flight> getFlightList(Booking booking) {
        if (booking.getFlights() != null && booking.getFlights().getFlight() != null) {
            return booking.getFlights().getFlight();
        } else if (booking.getItems() != null && booking.getItems().getFlight() != null) {
            return booking.getItems().getFlight();
        }
        return new ArrayList<>();
    }

    private static String getIssuingAirlineName(List<Flight> flightList) {
        if (flightList != null && !flightList.isEmpty() && flightList.get(0).getAirline() != null) {
            return flightList.get(0).getAirline();
        }
        return "G7";
    }

    private static String getIssuingPlace(List<Flight> flightList) {
        if (flightList != null && !flightList.isEmpty() && flightList.get(0).getFromcode() != null) {
            return flightList.get(0).getFromcode();
        }
        return "US";
    }

    private static void populateOdsAndPriceClasses(ChangePaymentRspDto response, List<Flight> flightList) {
        List<OD> ods = new ArrayList<>();
        List<PriceClass> priceClasses = new ArrayList<>();

        if (flightList != null) {
            int segmentCounter = 1;
            int odCounter = 1;

            for (Flight flight : flightList) {
                OD od = buildOd(flight, segmentCounter, odCounter);
                ods.add(od);
                priceClasses.add(buildPriceClass(flight, od.getClassType(), od.getPriceClassId()));

                segmentCounter++;
                odCounter++;
            }
        }
        response.setOds(ods);
        response.setPriceClassList(priceClasses);
    }

    private static OD buildOd(Flight flight, int segmentCounter, int odCounter) {
        OD od = new OD();
        od.setSegmentId("S" + segmentCounter);
        od.setOdKey("OD" + odCounter);

        od.setOrigin(flight.getFromcode());
        od.setOriginAirportName(flight.getFrom());
        od.setDestination(flight.getTocode());
        od.setDestinationAirportName(flight.getTo());

        String formattedDepDate = OrderMappingUtil.formatDate(flight.getFlightdate());
        String formattedArrDate = determineArrivalDate(flight, formattedDepDate);

        od.setDepartureDate(formattedDepDate);
        od.setArrivalDate(formattedArrDate);
        od.setDepartureTime(flight.getDepart());
        od.setArrivalTime(flight.getArrive());
        od.setJourneyTime(OrderMappingUtil.calculateJourneyTime(flight.getFlightdate(), flight.getDepart(),
                flight.getFlightdate(), flight.getArrive()));

        if (flight.getNumber() != null) {
            od.setFlightNumber(flight.getNumber().replaceAll("\\D", ""));
        }
        od.setEquipment(flight.getAircraftType());

        String carrier = flight.getAirlinedesignator() != null ? flight.getAirlinedesignator() : "G7";
        od.setMarketingCarrierCode(carrier);
        od.setMarketingCarrierName(flight.getAirline());
        od.setOperatingCarrierCode(carrier);
        od.setOperatingCarrierName(flight.getAirline());

        od.setArrivalTerminal(flight.getArrivalTerminal());
        od.setDepartureTerminal(flight.getDepartureTerminal());
        od.setChangeOfDay(0);

        String rawClass = flight.getFlightClass() != null ? flight.getFlightClass() : "";
        String className = rawClass;
        String cabinCode = "ECONOMY";

        if (rawClass.contains("/")) {
            String[] parts = rawClass.split("/");
            if (parts.length > 0) {
                cabinCode = determineCabinCode(parts[0].trim().toUpperCase());
            }
            if (parts.length > 1) {
                className = parts[1].trim();
            }
        }

        od.setCabinType(cabinCode);
        od.setPriceClassId("PC" + odCounter);
        od.setRbdCode(null);
        od.setFareBasisCode(null);
        od.setClassType(className);

        return od;
    }

    private static String determineArrivalDate(Flight flight, String formattedDepDate) {
        if (flight.getDepart() != null && flight.getArrive() != null) {
            try {
                LocalTime depTime = LocalTime.parse(flight.getDepart());
                LocalTime arrTime = LocalTime.parse(flight.getArrive());
                if (arrTime.isBefore(depTime)) {
                    return OrderMappingUtil.formatDate(
                            OrderMappingUtil.adjustDateByDays(flight.getFlightdate(), 1));
                }
            } catch (Exception e) {
                // Ignore date adjustment parsing error
            }
        }
        return formattedDepDate;
    }

    private static String determineCabinCode(String code) {
        if (code.equals("C") || code.equals("J") || code.equals("D") || code.equals("Z") || code.equals("I")) {
            return "BUSINESS";
        } else if (code.equals("F") || code.equals("A") || code.equals("P")) {
            return "FIRST";
        }
        return "ECONOMY";
    }

    private static PriceClass buildPriceClass(Flight flight, String className, String priceClassId) {
        PriceClass pc = new PriceClass();
        pc.setPriceClassId(priceClassId);
        pc.setClassName(className);
        pc.setCabinTypeCode(priceClassId);
        pc.setDescriptions(buildPriceClassDescriptions(flight));
        return pc;
    }

    private static List<PriceClass.Description> buildPriceClassDescriptions(Flight flight) {
        List<PriceClass.Description> descriptions = new ArrayList<>();
        if (flight.getServices() != null) {
            for (java.util.Map.Entry<String, Boolean> entry : flight.getServices().entrySet()) {
                if (Boolean.TRUE.equals(entry.getValue())) {
                    PriceClass.Description d = new PriceClass.Description();
                    d.setText(mapServiceToDescription(entry.getKey()));
                    descriptions.add(d);
                }
            }
        }
        return descriptions;
    }

    private static List<PaxDetailDTO> populatePaxAndTicketDocs(Booking booking, List<Flight> flightList,
            String issuingAirlineName, String issuingPlace, List<TicketDocInfoDTO> topLevelTicketDocs) {
        List<PaxDetailDTO> paxList = new ArrayList<>();
        List<Passenger> go7PaxList = (booking.getPassengers() != null)
                ? booking.getPassengers().getPassenger()
                : null;

        if (go7PaxList != null) {
            int[] counters = new int[] {1, 1}; // paxCounter, infantCounter
            List<PaxDetailDTO> adtList = new ArrayList<>();
            for (Passenger go7Pax : go7PaxList) {
                PaxDetailDTO pax = buildPax(go7Pax, adtList, counters);
                if (go7Pax.getETickets() != null && go7Pax.getETickets().getFlight() != null) {
                    List<TicketDocInfoDTO> ticketDocInfoList = populateTicketDocs(go7Pax, pax.getPaxId(), flightList,
                            issuingAirlineName, issuingPlace, topLevelTicketDocs);
                    pax.setTicketDocInfo(ticketDocInfoList);
                }
                paxList.add(pax);
            }
        }
        return paxList;
    }

    private static PaxDetailDTO buildPax(Passenger go7Pax, List<PaxDetailDTO> adtList, int[] counters) {
        PaxDetailDTO pax = new PaxDetailDTO();
        String mappedPtc = OrderMappingUtil.mapPaxType(go7Pax.getPaxtype());
        String title = go7Pax.getPaxtitle() != null ? go7Pax.getPaxtitle().toUpperCase().replace(".", "") : "MR";
        if ("INFANT".equals(title) || "INF".equals(title)) {
            mappedPtc = "INF";
        }

        String paxId = assignPaxId(mappedPtc, adtList, counters);
        if ("ADT".equals(mappedPtc)) {
            adtList.add(pax);
        }

        pax.setPaxId(paxId);
        pax.setPtc(mappedPtc);
        pax.setGivenName(go7Pax.getFirstname() != null ? go7Pax.getFirstname().toUpperCase() : "");
        pax.setSurname(go7Pax.getLastname() != null ? go7Pax.getLastname().toUpperCase() : "");
        pax.setTitle(title);
        pax.setGender(determineGender(go7Pax.getGender(), title));
        pax.setBirthDate(OrderMappingUtil.formatDate(go7Pax.getDob()));
        pax.setLanguage("English");

        if (go7Pax.getEmail() != null) {
            PaxDetailDTO.EmailDTO email = new PaxDetailDTO.EmailDTO();
            email.setEmailAddress(go7Pax.getEmail());
            email.setLabel("OTH");
            email.setType("OSI");
            List<PaxDetailDTO.EmailDTO> emails = new ArrayList<>();
            emails.add(email);
            pax.setEmails(emails);
        }
        return pax;
    }

    private static String assignPaxId(String mappedPtc, List<PaxDetailDTO> adtList, int[] counters) {
        if ("INF".equals(mappedPtc)) {
            String paxId;
            if (!adtList.isEmpty()) {
                PaxDetailDTO parent = adtList.get((counters[1] - 1) % adtList.size());
                paxId = parent.getPaxId() + ".1";
                parent.setInfantRef(paxId);
            } else {
                paxId = "T1.1";
            }
            counters[1]++;
            return paxId;
        } else {
            return "T" + counters[0]++;
        }
    }

    private static String determineGender(String rawGender, String title) {
        if (rawGender != null && rawGender.toUpperCase().startsWith("F")) {
            return "Female";
        }
        if (title.contains("MRS") || title.contains("MS") || title.contains("MISS")) {
            return "Female";
        }
        if (title.contains("MR") || title.contains("MSTR")) {
            return "Male";
        }
        if (rawGender != null && rawGender.toUpperCase().startsWith("M")) {
            return "Male";
        }
        return "Male";
    }

    private static List<TicketDocInfoDTO> populateTicketDocs(Passenger go7Pax, String paxId,
            List<Flight> flightList, String issuingAirlineName, String issuingPlace,
            List<TicketDocInfoDTO> topLevelTicketDocs) {
        List<TicketDocInfoDTO> ticketDocInfoList = new ArrayList<>();
        java.util.Set<String> uniqueTickets = new java.util.LinkedHashSet<>();
        for (Passenger.ETicketFlight etf : go7Pax.getETickets().getFlight()) {
            if (etf.getEticketnumber() != null && !etf.getEticketnumber().isEmpty()) {
                uniqueTickets.add(etf.getEticketnumber().trim());
            }
        }

        for (String ticketNbr : uniqueTickets) {
            TicketDocInfoDTO tdi = buildTicketDocInfo(ticketNbr, paxId, flightList, issuingAirlineName, issuingPlace);
            ticketDocInfoList.add(tdi);
            topLevelTicketDocs.add(tdi);
        }
        return ticketDocInfoList;
    }

    private static TicketDocInfoDTO buildTicketDocInfo(String ticketNbr, String paxId,
            List<Flight> flightList, String issuingAirlineName, String issuingPlace) {
        TicketDocInfoDTO tdi = new TicketDocInfoDTO();
        tdi.setValidatingCarrier("G7");
        tdi.setIssuingAirlineName(issuingAirlineName);
        tdi.setIssuingPlace(issuingPlace);
        tdi.setPaxId(Arrays.asList(paxId));

        List<TicketDocInfoDTO.TicketDocumentDTO> docs = new ArrayList<>();
        TicketDocInfoDTO.TicketDocumentDTO doc = new TicketDocInfoDTO.TicketDocumentDTO();
        doc.setTicketDocNbr(ticketNbr);
        doc.setType("T");
        doc.setNumberOfBooklets(1);
        doc.setDateOfIssue(OrderMappingUtil.formatCurrentDate());
        doc.setTimeOfIssue("00:00");
        doc.setTicketingLocation(issuingPlace);
        doc.setReportingType("BSP");

        List<TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO> coupons = new ArrayList<>();
        if (flightList != null) {
            int couponNum = 1;
            for (Flight f : flightList) {
                coupons.add(buildCoupon(f, couponNum++, paxId));
            }
        }
        doc.setCouponInfo(coupons);
        docs.add(doc);
        tdi.setTicketDocument(docs);
        return tdi;
    }

    private static TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO buildCoupon(Flight f, int couponNum, String paxId) {
        TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO coupon = new TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO();
        coupon.setCouponNumber(couponNum);
        coupon.setCouponReference("FBA" + (couponNum + 1));

        String fClass = f.getFlightClass() != null ? f.getFlightClass() : "";
        String fRbd = determineRbd(fClass);

        coupon.setFareBasisCode(fClass);
        coupon.setRbd(fRbd);
        coupon.setStatus("I");
        coupon.setValidatingCarrier("G7");

        coupon.setCurrentAirlineInfo(Arrays.asList(buildCurrentAirlineInfo(f)));
        coupon.setBaggageAllowances(buildBaggageAllowances(f, paxId, couponNum));
        return coupon;
    }

    private static String determineRbd(String fClass) {
        if (fClass.contains("/")) {
            String[] parts = fClass.split("/");
            if (parts.length > 0) {
                return parts[0].trim().toUpperCase();
            }
        } else if (!fClass.isEmpty()) {
            return fClass.substring(0, 1).toUpperCase();
        }
        return "";
    }

    private static TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO buildCurrentAirlineInfo(Flight f) {
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
        cai.setFlightNumber(f.getNumber() != null ? f.getNumber().replaceAll("\\D", "") : "");
        cai.setEquipmentAircraftCode(f.getAircraftTypeIataCode());
        return cai;
    }

    private static List<BaggageAllowance> buildBaggageAllowances(Flight f, String paxId, int couponNum) {
        List<BaggageAllowance> bags = new ArrayList<>();
        if (f.getServices() != null && f.getServices().containsKey(CHECKED_IN_BAGGAGE)
                && Boolean.TRUE.equals(f.getServices().get(CHECKED_IN_BAGGAGE))) {
            BaggageAllowance bag = new BaggageAllowance();
            bag.setBaggageAllowanceId("FBA" + (couponNum + 1));
            bag.setPassengerId(paxId);
            bag.setCategory("Checked-In");
            bag.setName(BAG_ALLOWANCES);

            BaggageAllowance.Weight w = new BaggageAllowance.Weight();
            w.setValue("30");
            w.setUom("KG");
            bag.setWeight(Arrays.asList(w));

            bags.add(bag);
        }
        return bags;
    }

    private static List<EMDInfoDTO> populateEmds(List<PaxDetailDTO> paxList,
            Map<String, String> passengerSeats, Map<String, String> passengerServices,
            List<Flight> flightList, String issuingAirlineName, String issuingPlace) {
        List<EMDInfoDTO> topEmdInfos = new ArrayList<>();
        java.util.LinkedHashMap<String, String> combinedEmdMap = buildCombinedEmdMap(passengerSeats, passengerServices);

        if (!combinedEmdMap.isEmpty() && flightList != null && !flightList.isEmpty()) {
            Flight cf = flightList.get(0);
            for (PaxDetailDTO pax : paxList) {
                List<String> assignedValues = getAssignedEmdValues(pax, combinedEmdMap);
                for (String assignedValue : assignedValues) {
                    int emdIndex = topEmdInfos.size() + 1;
                    EMDInfoDTO emd = buildEmd(pax, assignedValue, emdIndex, cf, issuingAirlineName, issuingPlace);

                    List<EMDInfoDTO> paxEmds = pax.getEmdInfo();
                    if (paxEmds == null) {
                        paxEmds = new ArrayList<>();
                    }
                    paxEmds.add(emd);
                    pax.setEmdInfo(paxEmds);
                    topEmdInfos.add(emd);
                }
            }
        }
        return topEmdInfos;
    }

    private static java.util.LinkedHashMap<String, String> buildCombinedEmdMap(
            Map<String, String> passengerSeats, Map<String, String> passengerServices) {
        java.util.LinkedHashMap<String, String> combinedEmdMap = new java.util.LinkedHashMap<>();
        if (passengerSeats != null) {
            for (java.util.Map.Entry<String, String> entry : passengerSeats.entrySet()) {
                combinedEmdMap.put(entry.getKey() + SUFFIX_SEAT, entry.getValue());
            }
        }
        if (passengerServices != null) {
            for (java.util.Map.Entry<String, String> entry : passengerServices.entrySet()) {
                combinedEmdMap.put(entry.getKey() + SUFFIX_SRV, entry.getValue());
            }
        }
        return combinedEmdMap;
    }

    private static List<String> getAssignedEmdValues(PaxDetailDTO pax,
            java.util.LinkedHashMap<String, String> combinedEmdMap) {
        List<String> assignedValues = new ArrayList<>();
        for (java.util.Map.Entry<String, String> entry : combinedEmdMap.entrySet()) {
            String paxIdKey = entry.getKey().substring(0, entry.getKey().lastIndexOf("_"));
            if (paxIdKey.equals(pax.getPaxId())) {
                assignedValues.add(entry.getValue());
            }
        }
        return assignedValues;
    }

    private static EMDInfoDTO buildEmd(PaxDetailDTO pax, String assignedValue, int emdIndex, Flight cf,
            String issuingAirlineName, String issuingPlace) {
        EMDInfoDTO emd = new EMDInfoDTO();
        emd.setValidatingCarrier("API");
        emd.setPaxId(Arrays.asList(pax.getPaxId()));
        emd.setIssuingAirlineName(issuingAirlineName);
        emd.setIssuingPlace(issuingPlace);

        List<EMDInfoDTO.TicketDocumentDTO> emdDocs = new ArrayList<>();
        EMDInfoDTO.TicketDocumentDTO emdDoc = new EMDInfoDTO.TicketDocumentDTO();

        String baseTkt = determineBaseTicketNbr(pax);
        emdDoc.setTicketDocNbr(baseTkt + "-" + emdIndex);
        emdDoc.setConnectedDocNbr(null);
        emdDoc.setType("J");
        emdDoc.setNumberOfBooklets(1);
        emdDoc.setDateOfIssue(OrderMappingUtil.formatCurrentDate());
        emdDoc.setTimeOfIssue("00:00");
        emdDoc.setTicketingLocation(issuingPlace);
        emdDoc.setReportingType("BSP");

        EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO emdCoupon = new EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO();
        emdCoupon.setCouponNumber(1);
        emdCoupon.setValidatingCarrier("API");
        emdCoupon.setStatus("I");

        String refVal = assignedValue;
        if (refVal != null && refVal.startsWith("SRV_")) {
            refVal = refVal.substring(4);
        }
        emdCoupon.setServiceRefs(Arrays.asList(refVal));

        emdCoupon.setCurrentAirlineInfo(Arrays.asList(buildEmdCurrentAirlineInfo(cf)));
        emdDoc.setCouponInfo(Arrays.asList(emdCoupon));
        emdDocs.add(emdDoc);
        emd.setTicketDocument(emdDocs);

        return emd;
    }

    private static String determineBaseTicketNbr(PaxDetailDTO pax) {
        if (pax.getTicketDocInfo() != null && !pax.getTicketDocInfo().isEmpty()
                && pax.getTicketDocInfo().get(0).getTicketDocument() != null
                && !pax.getTicketDocInfo().get(0).getTicketDocument().isEmpty()) {
            return pax.getTicketDocInfo().get(0).getTicketDocument().get(0).getTicketDocNbr();
        }
        return "EMD";
    }

    private static EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO buildEmdCurrentAirlineInfo(Flight cf) {
        EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO cai = new EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO();
        cai.setDepartureAirportCode(cf.getFromcode());
        cai.setArrivalAirportCode(cf.getTocode());
        cai.setDepartureDate(OrderMappingUtil.formatDate(cf.getFlightdate()));
        cai.setDepartureTime(cf.getDepart());
        cai.setDepartureAirportName(cf.getFrom());
        cai.setArrivalAirportName(cf.getTo());
        cai.setFlightNumber(cf.getNumber());
        cai.setMarketingCarrierAirlineId(cf.getAirlinedesignator());
        return cai;
    }

    private static BigDecimal populateOrderItems(List<OrderItemsDTO> orderItems, ChangePaymentRspDto response,
            Booking booking, List<Flight> flightList, List<PaxDetailDTO> paxList,
            Map<String, String> passengerSeats, Map<String, String> passengerServices) {
        OrderItemsDTO item = new OrderItemsDTO();
        item.setOrderItemId(response.getResponseId() + "_AIR-1");
        item.setPtc("ADT");
        item.setClassName(null);

        List<String> paxIds = paxList.stream().map(PaxDetailDTO::getPaxId).toList();
        item.setPassengerIds(paxIds);
        item.setTotalPrice(BigDecimal.ZERO);

        String itemCurrency = booking.getCurrency() != null ? booking.getCurrency() : "USD";
        java.util.LinkedHashMap<String, String> combinedMap = buildCombinedMap(passengerSeats, passengerServices);

        BigDecimal totalTaxAmount = processFlightList(flightList, combinedMap, paxList,
                itemCurrency, item);

        orderItems.add(item);

        BigDecimal airItemPrice = calculateAirItemPrice(flightList);
        item.setTotalPrice(airItemPrice);

        List<OrderItemsDTO> secondaryOrderItems = new ArrayList<>();
        int initialSrvIdx = item.getServiceList().size() + 1;
        BigDecimal secondaryCharges = processSecondaryOrderItems(new SecondaryItemsRequest(
                secondaryOrderItems, response, booking, flightList, paxList, combinedMap, airItemPrice, itemCurrency, initialSrvIdx));

        if (secondaryCharges.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal airBaseFare = airItemPrice.subtract(totalTaxAmount);
            if (airBaseFare.compareTo(BigDecimal.ZERO) < 0) {
                airBaseFare = airItemPrice;
            }

            item.setBaseFare(new OrderItemsDTO.BaseFare(
                    airBaseFare.setScale(2, java.math.RoundingMode.HALF_UP), itemCurrency));
            item.setTotalFare(new OrderItemsDTO.TotalFare(
                    airItemPrice.setScale(2, java.math.RoundingMode.HALF_UP), itemCurrency));
            item.setTotalPrice(airItemPrice.setScale(2, java.math.RoundingMode.HALF_UP));
        }

        BigDecimal totalOrderPrice = airItemPrice.add(secondaryCharges);
        response.setTotalOrderPrice(totalOrderPrice.setScale(2, java.math.RoundingMode.HALF_UP));

        if (!secondaryOrderItems.isEmpty()) {
            orderItems.addAll(secondaryOrderItems);
        }

        return secondaryCharges;
    }

    private static java.util.LinkedHashMap<String, String> buildCombinedMap(
            Map<String, String> passengerSeats, Map<String, String> passengerServices) {
        java.util.LinkedHashMap<String, String> combinedMap = new java.util.LinkedHashMap<>();
        if (passengerSeats != null) {
            for (java.util.Map.Entry<String, String> entry : passengerSeats.entrySet()) {
                combinedMap.put(entry.getKey() + SUFFIX_SEAT, entry.getValue());
            }
        }
        if (passengerServices != null) {
            for (java.util.Map.Entry<String, String> entry : passengerServices.entrySet()) {
                combinedMap.put(entry.getKey() + SUFFIX_SRV, entry.getValue());
            }
        }
        return combinedMap;
    }

    private static BigDecimal processFlightList(List<Flight> flightList,
            java.util.LinkedHashMap<String, String> combinedMap,
            List<PaxDetailDTO> paxList, String itemCurrency,
            OrderItemsDTO item) {
        BigDecimal totalTaxAmount = BigDecimal.ZERO;
        List<String> paxIds = paxList.stream().map(PaxDetailDTO::getPaxId).toList();
        List<OrderItemsDTO.Tax> taxList = new ArrayList<>();
        List<Service> serviceList = new ArrayList<>();
        List<BaggageAllowance> orderItemBags = new ArrayList<>();

        if (flightList != null) {
            int segCount = 1;
            for (Flight f : flightList) {
                totalTaxAmount = totalTaxAmount.add(BigDecimal.valueOf(f.getTotaltaxes()));
                populateFlightTaxes(f, taxList, itemCurrency);

                if (combinedMap.isEmpty()) {
                    populateDefaultServicesAndBags(f, segCount, paxIds, paxList, serviceList, orderItemBags);
                } else {
                    populateCombinedServices(segCount, paxIds, serviceList);
                }
                segCount++;
            }
        }
        item.setTaxes(taxList);
        item.setServiceList(serviceList);
        item.setBaggageAllowances(orderItemBags);
        OrderItemsDTO.TotalTax tt = new OrderItemsDTO.TotalTax();
        tt.setAmount(totalTaxAmount);
        tt.setCurrency(itemCurrency);
        item.setTotalTax(tt);
        return totalTaxAmount;
    }

    private static void populateFlightTaxes(Flight f, List<OrderItemsDTO.Tax> taxList, String itemCurrency) {
        if (f.getTaxes() != null) {
            addTaxToList(taxList, "GH", "Ground Handling", f.getTaxes().getGroundHandling(), itemCurrency);
            addTaxToList(taxList, "I2", "Security Tax", f.getTaxes().getSecurity(), itemCurrency);
            addTaxToList(taxList, "YQ", "Fuel Surcharge", f.getTaxes().getFuel(), itemCurrency);
            addTaxToList(taxList, "OT", "Other Tax", f.getTaxes().getTax4(), itemCurrency);
        }
    }

    private static void populateDefaultServicesAndBags(Flight f, int segCount, List<String> paxIds,
            List<PaxDetailDTO> paxList, List<Service> serviceList, List<BaggageAllowance> orderItemBags) {
        if (f.getServices() != null) {
            for (java.util.Map.Entry<String, Boolean> entry : f.getServices().entrySet()) {
                if (Boolean.TRUE.equals(entry.getValue())) {
                    for (String loopPaxId : paxIds) {
                        serviceList.add(buildDefaultService(entry.getKey(), segCount, loopPaxId, f));
                        if (CHECKED_IN_BAGGAGE.equalsIgnoreCase(entry.getKey())) {
                            orderItemBags.add(buildDefaultBaggageAllowance(segCount, loopPaxId, paxList));
                        }
                    }
                }
            }
        }
    }

    private static Service buildDefaultService(String serviceKey, int segCount, String loopPaxId, Flight f) {
        Service svc = new Service();
        String serviceSuffix = serviceKey.length() > 3
                ? serviceKey.substring(0, 3).toUpperCase()
                : serviceKey.toUpperCase();
        svc.setServiceId("SEG" + segCount + "_" + loopPaxId + "_" + serviceSuffix);
        svc.setServiceStatus(STATUS_CONFIRMED);
        svc.setSegmentId("SEG" + segCount);
        svc.setOdKey("OD" + segCount);
        svc.setDeparture(f.getFromcode());
        svc.setArrival(f.getTocode());
        return svc;
    }

    private static BaggageAllowance buildDefaultBaggageAllowance(int segCount, String loopPaxId, List<PaxDetailDTO> paxList) {
        BaggageAllowance ba = new BaggageAllowance();
        ba.setBaggageAllowanceId("FBA" + segCount);

        String paxPtc = "ADT";
        for (PaxDetailDTO pax : paxList) {
            if (pax.getPaxId().equals(loopPaxId)) {
                paxPtc = pax.getPtc();
                break;
            }
        }

        ba.setPtc(paxPtc);
        ba.setPassengerId(loopPaxId);
        ba.setCategory("Checked-In");
        ba.setName(BAG_ALLOWANCES);

        BaggageAllowance.Weight w = new BaggageAllowance.Weight();
        w.setValue("INF".equals(paxPtc) ? "10" : "30");
        w.setUom("KG");
        ba.setWeight(Arrays.asList(w));

        BaggageAllowance.DescriptionDTO desc = new BaggageAllowance.DescriptionDTO();
        desc.setDescription(BAG_ALLOWANCES);
        ba.setDescriptions(Arrays.asList(desc));

        return ba;
    }

    private static void populateCombinedServices(int segCount, List<String> paxIds, List<Service> serviceList) {
        for (String pId : paxIds) {
            Service svc = new Service();
            svc.setServiceId("SEG" + segCount + "_" + pId);
            svc.setServiceStatus(STATUS_CONFIRMED);
            serviceList.add(svc);
        }
    }

    private static BigDecimal calculateAirItemPrice(List<Flight> flightList) {
        BigDecimal sumFlightPrice = sumFlightPrices(flightList);
        BigDecimal invPricingBasis = calculateInvPricingBasis(flightList);
        return (invPricingBasis.compareTo(BigDecimal.ZERO) > 0) ? invPricingBasis : sumFlightPrice;
    }

    private static BigDecimal sumFlightPrices(List<Flight> flightList) {
        BigDecimal sum = BigDecimal.ZERO;
        if (flightList != null) {
            for (Flight f : flightList) {
                BigDecimal fBase = safeDecimalParse(f.getInvpricingwithouttax());
                sum = sum.add(fBase).add(BigDecimal.valueOf(f.getTotaltaxes()));
            }
        }
        return sum;
    }

    private static BigDecimal calculateInvPricingBasis(List<Flight> flightList) {
        BigDecimal invPricingBasis = BigDecimal.ZERO;
        if (flightList != null) {
            for (Flight f : flightList) {
                invPricingBasis = invPricingBasis.add(safeDecimalParse(f.getInvpricing()));
            }
        }
        return invPricingBasis;
    }

    private static BigDecimal safeDecimalParse(String val) {
        if (val == null || val.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(val);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal processSecondaryOrderItems(SecondaryItemsRequest req) {
        BigDecimal secondaryCharges = BigDecimal.ZERO;
        if (req.flightList() == null || req.flightList().isEmpty()) {
            return secondaryCharges;
        }

        Flight f = req.flightList().get(0);
        String segmentId = "SEG1";
        BigDecimal pnrTotal = determinePnrTotal(req.booking());

        int srvIdx = req.initialSrvIdx();
        for (Map.Entry<String, String> paxSecondary : req.combinedMap().entrySet()) {
            String rawKey = paxSecondary.getKey();
            String assignedValue = paxSecondary.getValue();

            boolean isSeat = rawKey.endsWith(SUFFIX_SEAT);
            String assignedPaxId = rawKey.substring(0, rawKey.lastIndexOf("_"));

            OrderItemsDTO secondaryItem = new OrderItemsDTO();
            secondaryItem.setOrderItemId(req.response().getOrderId() + SUFFIX_SRV + srvIdx++);

            String seatPtc = determineSeatPtc(req.paxList(), assignedPaxId);
            secondaryItem.setPtc(seatPtc);
            secondaryItem.setPassengerIds(Arrays.asList(assignedPaxId));

            BigDecimal itemPrice = calculateSecondaryItemPrice(assignedValue, isSeat, f, pnrTotal, req.airItemPrice(), req.combinedMap());
            secondaryCharges = secondaryCharges.add(itemPrice);

            secondaryItem.setBaseFare(new OrderItemsDTO.BaseFare(itemPrice, req.itemCurrency()));
            secondaryItem.setTotalTax(new OrderItemsDTO.TotalTax(BigDecimal.ZERO, req.itemCurrency()));
            secondaryItem.setTotalFare(new OrderItemsDTO.TotalFare(itemPrice, req.itemCurrency()));
            secondaryItem.setTotalPrice(itemPrice);

            String itemCode = (assignedValue != null && assignedValue.contains("|")) ? assignedValue.split("\\|")[0] : assignedValue;
            secondaryItem.setServiceList(Arrays.asList(buildSecondaryService(isSeat, itemCode, segmentId, assignedPaxId)));
            req.secondaryOrderItems().add(secondaryItem);
        }
        return secondaryCharges;
    }

    private static BigDecimal determinePnrTotal(Booking booking) {
        if (booking.getBalanceInformation() != null && booking.getBalanceInformation().getPnrTotal() != null) {
            return booking.getBalanceInformation().getPnrTotal();
        } else if (booking.getTotalprice() != null) {
            try {
                return new BigDecimal(booking.getTotalprice());
            } catch (Exception e) {
                // Ignore parse error
            }
        }
        return BigDecimal.ZERO;
    }

    private static String determineSeatPtc(List<PaxDetailDTO> paxList, String assignedPaxId) {
        for (PaxDetailDTO pax : paxList) {
            if (pax.getPaxId().equals(assignedPaxId)) {
                return pax.getPtc();
            }
        }
        return "ADT";
    }

    private static record AssignedValueInfo(String itemCode, BigDecimal price) {}

    private static AssignedValueInfo parseAssignedValue(String assignedValue) {
        if (assignedValue != null && assignedValue.contains("|")) {
            String[] parts = assignedValue.split("\\|");
            return new AssignedValueInfo(parts[0], safeDecimalParse(parts[1]));
        }
        return new AssignedValueInfo(assignedValue, BigDecimal.ZERO);
    }

    private static BigDecimal resolveSeatPrice(String itemCode, Flight f) {
        if (f.getSeat() != null) {
            for (Seat s : f.getSeat()) {
                if (itemCode.equals(s.getSeatNumber()) && s.getFare() != null) {
                    return s.getFare();
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal resolveUnpaidCountPrice(BigDecimal diff, java.util.LinkedHashMap<String, String> combinedMap) {
        long unpaidCount = combinedMap.values().stream().filter(v -> !v.contains("|")
                || safeDecimalParse(v.split("\\|")[1]).compareTo(BigDecimal.ZERO) == 0).count();
        if (unpaidCount > 0) {
            return diff.divide(new BigDecimal(unpaidCount), 2, java.math.RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal calculateSecondaryItemPrice(String assignedValue, boolean isSeat, Flight f,
            BigDecimal pnrTotal, BigDecimal airItemPrice, java.util.LinkedHashMap<String, String> combinedMap) {
        AssignedValueInfo info = parseAssignedValue(assignedValue);
        BigDecimal itemPrice = info.price();

        if (isSeat && itemPrice.compareTo(BigDecimal.ZERO) == 0) {
            itemPrice = resolveSeatPrice(info.itemCode(), f);
        }

        if (itemPrice.compareTo(BigDecimal.ZERO) == 0 && pnrTotal.compareTo(airItemPrice) > 0) {
            BigDecimal diff = pnrTotal.subtract(airItemPrice);
            itemPrice = resolveUnpaidCountPrice(diff, combinedMap);
        }
        return itemPrice;
    }

    private static Service buildSecondaryService(boolean isSeat, String itemCode, String segmentId, String assignedPaxId) {
        Service mappedSrv = new Service();
        if (!isSeat) {
            String srvIdVal = itemCode;
            if (srvIdVal != null && srvIdVal.startsWith("SRV_")) {
                srvIdVal = srvIdVal.substring(4);
            }
            mappedSrv.setServiceId(srvIdVal);
            mappedSrv.setServiceStatus(STATUS_CONFIRMED);
            mappedSrv.setServiceCode("SRV");
            mappedSrv.setServiceName("Ancillary Service");
        } else {
            mappedSrv.setServiceId(segmentId + "_" + assignedPaxId);
            mappedSrv.setServiceStatus("PENDING");
            mappedSrv.setServiceCode("SEAT" + itemCode);
            mappedSrv.setServiceName("Specific Seat Request");
            mappedSrv.setSegmentId(segmentId);

            if (itemCode != null && !itemCode.isEmpty()) {
                String col = itemCode.substring(itemCode.length() - 1);
                String rowStr = itemCode.substring(0, itemCode.length() - 1);
                mappedSrv.setColumn(col);
                try {
                    mappedSrv.setRow(new java.math.BigInteger(rowStr));
                } catch (Exception e) {
                    // Ignore row biginteger parse error
                }
            }

            List<Service.SeatCharacteristic> chars = new ArrayList<>();
            chars.add(new Service.SeatCharacteristic("CH", "Chargeable Seat"));
            chars.add(new Service.SeatCharacteristic("W", "Window seat"));
            chars.add(new Service.SeatCharacteristic("FC", "Front of cabin class/compartment"));
            chars.add(new Service.SeatCharacteristic("N", "No smoking seat"));
            mappedSrv.setSeatCharacteristics(chars);
        }
        return mappedSrv;
    }

    private static record SecondaryItemsRequest(
        List<OrderItemsDTO> secondaryOrderItems,
        ChangePaymentRspDto response,
        Booking booking,
        List<Flight> flightList,
        List<PaxDetailDTO> paxList,
        java.util.LinkedHashMap<String, String> combinedMap,
        BigDecimal airItemPrice,
        String itemCurrency,
        int initialSrvIdx
    ) {}

    private static void populatePayments(ChangePaymentRspDto response, ChangePaymentReqDto requestDto,
            Booking booking, BigDecimal secondaryCharges) {
        List<PaymentsDTO> payments = new ArrayList<>();
        String pType = requestDto.getPaymentType() != null ? requestDto.getPaymentType() : "CC";

        BigDecimal airItemPrice = response.getOrderItems().isEmpty() ? BigDecimal.ZERO : response.getOrderItems().get(0).getTotalPrice();

        if (airItemPrice.compareTo(BigDecimal.ZERO) > 0) {
            PaymentsDTO payAir = new PaymentsDTO();
            payAir.setType(pType);
            payAir.setStatusCode(STATUS_SUCCESSFUL);
            payAir.setAmount(airItemPrice.setScale(2, java.math.RoundingMode.HALF_UP));
            payAir.setCurrency(booking.getCurrency());
            payAir.setOrderItem(Arrays.asList(response.getOrderItems().get(0).getOrderItemId()));
            payments.add(payAir);
        }

        if (secondaryCharges.compareTo(BigDecimal.ZERO) > 0 && response.getOrderItems().size() > 1) {
            for (int i = 1; i < response.getOrderItems().size(); i++) {
                OrderItemsDTO secItem = response.getOrderItems().get(i);
                PaymentsDTO payAncillary = new PaymentsDTO();
                payAncillary.setType(pType);
                payAncillary.setStatusCode(STATUS_SUCCESSFUL);
                BigDecimal itemAmount = (secItem.getTotalPrice() != null) ? secItem.getTotalPrice() : secondaryCharges;
                payAncillary.setAmount(itemAmount.setScale(2, java.math.RoundingMode.HALF_UP));
                payAncillary.setCurrency(booking.getCurrency());
                payAncillary.setOrderItem(Arrays.asList(secItem.getOrderItemId()));
                payments.add(payAncillary);
            }
        }

        if (payments.isEmpty() && !response.getOrderItems().isEmpty()) {
            PaymentsDTO pay = new PaymentsDTO();
            pay.setType(pType);
            pay.setStatusCode(STATUS_SUCCESSFUL);
            pay.setAmount(response.getTotalOrderPrice());
            pay.setCurrency(booking.getCurrency());
            pay.setOrderItem(Arrays.asList(response.getOrderItems().get(0).getOrderItemId()));
            payments.add(pay);
        }

        response.setPayments(payments);
    }

    private static void addTaxToList(List<OrderItemsDTO.Tax> list, String code, String description,
            double amount, String currency) {
        if (amount <= 0.001) {
            return;
        }
        OrderItemsDTO.Tax t = new OrderItemsDTO.Tax();
        t.setCode(code);
        t.setDescription(description);
        t.setAmount(BigDecimal.valueOf(amount));
        t.setCurrency(currency);
        list.add(t);
    }

    private static String mapServiceToDescription(String key) {
        if ("SeatSelection".equalsIgnoreCase(key)) {
            return "Seat Selection - Complimentary (Standard Seats)";
        }
        if ("HandBaggage".equalsIgnoreCase(key)) {
            return "Cabin Baggage - 1 piece Up to 7kg each";
        }
        if (CHECKED_IN_BAGGAGE.equalsIgnoreCase(key)) {
            return "Check-in Baggage - 30kg";
        }
        if ("RefundableTicket".equalsIgnoreCase(key)) {
            return "Booking Cancellation Fee - USD 100";
        }
        if ("FoodOnBoard".equalsIgnoreCase(key)) {
            return "Food on Board - Included";
        }
        return key + " - Available";
    }
}