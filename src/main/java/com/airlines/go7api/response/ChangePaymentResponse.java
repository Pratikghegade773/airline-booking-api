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

    public static ChangePaymentRspDto generateResponse(ChangePaymentRspGo7Dto go7Response,
            ChangePaymentReqDto requestDto, java.util.Map<String, String> passengerSeats,
            java.util.Map<String, String> passengerServices) {
        ChangePaymentRspDto response = new ChangePaymentRspDto();

        if (go7Response == null || go7Response.getAerocrs() == null || go7Response.getAerocrs().getBooking() == null) {
            return response;
        }

        Booking booking = go7Response.getAerocrs().getBooking();

        // 1. Top Level Fields
        // response.setAgentId(requestDto.getAgentId() != null ? requestDto.getAgentId()
        // response.setAgencyName(requestDto.getAgencyName() != null ?
        // response.setAgencyId(requestDto.getAgencyId() != null ?

        response.setResponseId("P" + UUID.randomUUID().toString().substring(0, 15).toUpperCase());

        String requestOrderId = (requestDto.getOrderId() != null && !requestDto.getOrderId().isEmpty())
                ? requestDto.getOrderId()
                : String.valueOf(booking.getBookingid());
        response.setOrderId(requestOrderId);

        response.setPnr(booking.getPnrref());

        response.setApiOwner("G7");

        // Pricing
        BigDecimal totalOrderPrice = BigDecimal.ZERO;
        response.setTotalOrderPrice(BigDecimal.ZERO); // Initial placeholder

        response.setCurrency(booking.getCurrency() != null ? booking.getCurrency() : "USD");
        // Exchange Rate if available in Go7 (Sample didn't explicitly show it in top
        // level clearly, but usually calculated or static)
        // response.setExchangeRate("239.10915543"); // Hardcoded from example or
        // derived? Leaving as example
        // default/placeholder

        response.setPaymentTimeLimit(booking.getPnrttl());
        response.setTicketingTimeLimit(booking.getPnrttl());

        response.setStatusCode(go7Response.getAerocrs().isSuccess() ? "OPENED" : "REJECTED");
        String validatingCarrier = "G7";
        response.setValidatingCarrier(validatingCarrier);

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

        // 3. ODs (Flights)
        List<OD> ods = new ArrayList<>();
        List<PriceClass> priceClasses = new ArrayList<>();

        List<Flight> flightList = null;
        if (booking.getFlights() != null && booking.getFlights().getFlight() != null) {
            flightList = booking.getFlights().getFlight();
        } else if (booking.getItems() != null && booking.getItems().getFlight() != null) {
            flightList = booking.getItems().getFlight();
        }

        if (flightList != null) {
            int segmentCounter = 1;
            int odCounter = 1;

            for (Flight flight : flightList) {
                OD od = new OD();
                od.setSegmentId("S" + segmentCounter); // Matches SEG1 format
                od.setOdKey("OD" + odCounter);

                od.setOrigin(flight.getFromcode());
                od.setOriginAirportName(flight.getFrom());
                od.setDestination(flight.getTocode());
                od.setDestinationAirportName(flight.getTo());

                // Dates
                String formattedDepDate = OrderMappingUtil.formatDate(flight.getFlightdate());
                String formattedArrDate = formattedDepDate;
                if (flight.getDepart() != null && flight.getArrive() != null) {
                    // ... same date logic ...
                    try {
                        LocalTime depTime = LocalTime.parse(flight.getDepart());
                        LocalTime arrTime = LocalTime.parse(flight.getArrive());
                        if (arrTime.isBefore(depTime)) {
                            formattedArrDate = OrderMappingUtil.formatDate(OrderMappingUtil.adjustDateByDays(flight.getFlightdate(), 1));
                        }
                    } catch (Exception e) {
                    }
                }
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
                PriceClass pc = new PriceClass();
                pc.setPriceClassId(priceClassId);
                pc.setClassName(className);
                pc.setCabinTypeCode(priceClassId); // Example uses ID as code

                List<PriceClass.Description> descriptions = new ArrayList<>();
                if (flight.getServices() != null) {
                    // Map services to descriptions
                    for (java.util.Map.Entry<String, Boolean> entry : flight.getServices().entrySet()) {
                        if (Boolean.TRUE.equals(entry.getValue())) {
                            PriceClass.Description d = new PriceClass.Description();
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
        List<PaxDetailDTO> paxList = new ArrayList<>();
        // Also populate top-level TicketDocInfoList?? The example shows both pax-level
        // and top-level.
        List<TicketDocInfoDTO> topLevelTicketDocs = new ArrayList<>();

        List<Passenger> go7PaxList = (booking.getPassengers() != null)
                ? booking.getPassengers().getPassenger()
                : null;

        // Validation/Issuing Info from first flight if available
        String issuingAirlineName = "G7";
        String issuingPlace = "US"; // Default
        if (flightList != null && !flightList.isEmpty()) {
            Flight f = flightList.get(0);
            if (f.getAirline() != null)
                issuingAirlineName = f.getAirline();
            if (f.getFromcode() != null)
                issuingPlace = f.getFromcode();
        }

        if (go7PaxList != null) {
            int paxCounter = 1;
            int infantCounter = 1;
            List<PaxDetailDTO> adtList = new ArrayList<>();
            for (Passenger go7Pax : go7PaxList) {
                PaxDetailDTO pax = new PaxDetailDTO();
                String mappedPtc = OrderMappingUtil.mapPaxType(go7Pax.getPaxtype());
                String title = go7Pax.getPaxtitle() != null ? go7Pax.getPaxtitle().toUpperCase().replace(".", "")
                        : "MR";
                if ("INFANT".equals(title) || "INF".equals(title)) {
                    mappedPtc = "INF";
                }

                String paxId;
                if ("INF".equals(mappedPtc)) {
                    // find a parent
                    if (!adtList.isEmpty()) {
                        PaxDetailDTO parent = adtList.get((infantCounter - 1) % adtList.size());
                        paxId = parent.getPaxId() + ".1";
                        parent.setInfantRef(paxId);
                    } else {
                        paxId = "T1.1";
                    }
                    infantCounter++;
                } else {
                    paxId = "T" + paxCounter++;
                    if ("ADT".equals(mappedPtc)) {
                        adtList.add(pax);
                    }
                }

                pax.setPaxId(paxId);
                pax.setPtc(mappedPtc);
                pax.setGivenName(go7Pax.getFirstname() != null ? go7Pax.getFirstname().toUpperCase() : "");
                pax.setSurname(go7Pax.getLastname() != null ? go7Pax.getLastname().toUpperCase() : "");
                pax.setTitle(title);

                String gender = "Male";
                if (go7Pax.getGender() != null && go7Pax.getGender().toUpperCase().startsWith("F")) {
                    gender = "Female";
                } else if (title.contains("MRS") || title.contains("MS") || title.contains("MISS")) {
                    gender = "Female";
                } else if (title.contains("MR") || title.contains("MSTR")) {
                    gender = "Male";
                } else if (go7Pax.getGender() != null && go7Pax.getGender().toUpperCase().startsWith("M")) {
                    gender = "Male";
                }
                pax.setGender(gender);
                pax.setBirthDate(OrderMappingUtil.formatDate(go7Pax.getDob())); // Should match ddMMMyyyy
                pax.setLanguage("English");

                // Emails match example
                if (go7Pax.getEmail() != null) {
                    PaxDetailDTO.EmailDTO email = new PaxDetailDTO.EmailDTO();
                    email.setEmailAddress(go7Pax.getEmail()); // Example NUK1234? Keeping actual email
                    email.setLabel("OTH");
                    email.setType("OSI");
                    List<PaxDetailDTO.EmailDTO> emails = new ArrayList<>();
                    emails.add(email);
                    pax.setEmails(emails);
                }

                // Map TicketDocInfo
                if (go7Pax.getETickets() != null && go7Pax.getETickets().getFlight() != null) {
                    List<TicketDocInfoDTO> ticketDocInfoList = new ArrayList<>();

                    java.util.Set<String> uniqueTickets = new java.util.LinkedHashSet<>();
                    for (Passenger.ETicketFlight etf : go7Pax.getETickets().getFlight()) {
                        if (etf.getEticketnumber() != null && !etf.getEticketnumber().isEmpty()) {
                            uniqueTickets.add(etf.getEticketnumber().trim());
                        }
                    }

                    for (String ticketNbr : uniqueTickets) {
                        TicketDocInfoDTO tdi = new TicketDocInfoDTO();
                        tdi.setValidatingCarrier("G7"); // Default
                        tdi.setIssuingAirlineName(issuingAirlineName); // Example
                        tdi.setIssuingPlace(issuingPlace); // Example
                        List<String> pIds = new ArrayList<>();
                        pIds.add(paxId);
                        tdi.setPaxId(pIds);

                        List<TicketDocInfoDTO.TicketDocumentDTO> docs = new ArrayList<>();
                        TicketDocInfoDTO.TicketDocumentDTO doc = new TicketDocInfoDTO.TicketDocumentDTO();
                        doc.setTicketDocNbr(ticketNbr);
                        doc.setType("T");
                        doc.setNumberOfBooklets(1);
                        doc.setDateOfIssue(OrderMappingUtil.formatCurrentDate()); // Today
                        doc.setTimeOfIssue("00:00");
                        doc.setTicketingLocation(issuingPlace);
                        doc.setReportingType("BSP");

                        // Coupons - Map from Flight List
                        List<TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO> coupons = new ArrayList<>();
                        if (flightList != null) {
                            int couponNum = 1;
                            for (Flight f : flightList) {
                                TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO coupon = new TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO();
                                coupon.setCouponNumber(couponNum++);
                                coupon.setCouponReference("FBA" + couponNum); // Sequential ref

                                // Extract RBD/Class again for coupon
                                String fClass = f.getFlightClass() != null ? f.getFlightClass() : "";
                                String fRbd = "";
                                if (fClass.contains("/")) {
                                    String[] parts = fClass.split("/");
                                    if (parts.length > 0)
                                        fRbd = parts[0].trim().toUpperCase();
                                } else if (!fClass.isEmpty()) {
                                    fRbd = fClass.substring(0, 1).toUpperCase();
                                }

                                coupon.setFareBasisCode(fClass); // Fallback
                                coupon.setRbd(fRbd);
                                coupon.setStatus("I");
                                coupon.setValidatingCarrier("G7");

                                // Current Airline Info
                                TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO cai = new TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO();
                                cai.setDepartureAirportCode(f.getFromcode());
                                cai.setArrivalAirportCode(f.getTocode());
                                cai.setDepartureDate(OrderMappingUtil.formatDate(f.getFlightdate()));
                                cai.setDepartureTime(f.getDepart());
                                cai.setDepartureAirportName(f.getFrom()); // Map name better?
                                cai.setDepartureTerminal(f.getDepartureTerminal());

                                // Arrival date calc again? Simplified here
                                cai.setArrivalDate(OrderMappingUtil.formatDate(f.getFlightdate()));
                                cai.setArrivalTime(f.getArrive());
                                cai.setArrivalAirportName(f.getTo());
                                cai.setArrivalTerminal(f.getArrivalTerminal());

                                cai.setMarketingCarrierAirlineId(f.getAirlinedesignator());
                                cai.setMarketingCarrierName(f.getAirline());
                                cai.setOperatingCarrierAirlineId(f.getAirlinedesignator());
                                cai.setOperatingCarrierName(f.getAirline());
                                cai.setFlightNumber(
                                        f.getNumber() != null ? f.getNumber().replaceAll("\\D", "") : "");
                                cai.setEquipmentAircraftCode(f.getAircraftTypeIataCode());

                                List<TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO> caiList = new ArrayList<>();
                                caiList.add(cai);
                                coupon.setCurrentAirlineInfo(caiList);

                                // Baggage from Service?
                                List<BaggageAllowance> bags = new ArrayList<>();
                                if (f.getServices() != null && f.getServices().containsKey("CheckedInBaggage")
                                        && Boolean.TRUE.equals(f.getServices().get("CheckedInBaggage"))) {
                                    BaggageAllowance bag = new BaggageAllowance();
                                    bag.setBaggageAllowanceId("FBA" + couponNum);
                                    bag.setPassengerId(paxId);
                                    bag.setCategory("Checked-In");
                                    bag.setName("Bag allowances");

                                    BaggageAllowance.Weight w = new BaggageAllowance.Weight();
                                    w.setValue("30");
                                    w.setUom("KG");
                                    List<BaggageAllowance.Weight> wList = new ArrayList<>();
                                    wList.add(w);
                                    bag.setWeight(wList);

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
                paxList.add(pax);
            }
        }

        List<EMDInfoDTO> topEmdInfos = new ArrayList<>();

        // Determine active secondary map for EMDs
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

        // Inject EMDs if secondary items are present
        if (!combinedEmdMap.isEmpty() && flightList != null && !flightList.isEmpty()) {
            for (PaxDetailDTO pax : paxList) {
                // Find all assigned secondary items for this specific PAX
                List<String> assignedValues = new ArrayList<>();
                for (java.util.Map.Entry<String, String> entry : combinedEmdMap.entrySet()) {
                    String paxIdKey = entry.getKey().substring(0, entry.getKey().lastIndexOf("_"));
                    if (paxIdKey.equals(pax.getPaxId())) {
                        assignedValues.add(entry.getValue());
                    }
                }

                for (String assignedValue : assignedValues) {
                    EMDInfoDTO emd = new EMDInfoDTO();
                    emd.setValidatingCarrier("API");
                    emd.setPaxId(Arrays.asList(pax.getPaxId()));
                    emd.setIssuingAirlineName(issuingAirlineName);
                    emd.setIssuingPlace(issuingPlace);

                    List<EMDInfoDTO.TicketDocumentDTO> emdDocs = new ArrayList<>();
                    EMDInfoDTO.TicketDocumentDTO emdDoc = new EMDInfoDTO.TicketDocumentDTO();

                    String baseTkt = (go7PaxList != null && !go7PaxList.isEmpty()
                            && go7PaxList.get(0).getETickets() != null
                            && go7PaxList.get(0).getETickets().getFlight() != null
                            && !go7PaxList.get(0).getETickets().getFlight().isEmpty())
                                    ? go7PaxList.get(0).getETickets().getFlight().get(0).getEticketnumber()
                                    : "EMD";

                    emdDoc.setTicketDocNbr(baseTkt + "-" + (topEmdInfos.size() + 1));
                    emdDoc.setConnectedDocNbr(null);
                    emdDoc.setType("J");
                    emdDoc.setNumberOfBooklets(1);
                    emdDoc.setDateOfIssue(OrderMappingUtil.formatCurrentDate());
                    emdDoc.setTimeOfIssue("00:00");
                    emdDoc.setTicketingLocation(emd.getIssuingPlace());
                    emdDoc.setReportingType("BSP");

                    List<EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO> emdCoupons = new ArrayList<>();
                    EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO emdCoupon = new EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO();
                    emdCoupon.setCouponNumber(1);
                    emdCoupon.setValidatingCarrier("API");
                    emdCoupon.setStatus("I");

                    String refVal = assignedValue;
                    if (refVal != null && refVal.startsWith("SRV_")) {
                        refVal = refVal.substring(4);
                    }
                    emdCoupon.setServiceRefs(Arrays.asList(refVal));

                    Flight cf = flightList.get(0);
                    List<EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO> caiList = new ArrayList<>();
                    EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO cai = new EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO();
                    cai.setDepartureAirportCode(cf.getFromcode());
                    cai.setArrivalAirportCode(cf.getTocode());
                    cai.setDepartureDate(OrderMappingUtil.formatDate(cf.getFlightdate()));
                    cai.setDepartureTime(cf.getDepart());
                    cai.setDepartureAirportName(cf.getFrom());
                    cai.setArrivalAirportName(cf.getTo());
                    cai.setFlightNumber(cf.getNumber());
                    cai.setMarketingCarrierAirlineId(cf.getAirlinedesignator());
                    caiList.add(cai);
                    emdCoupon.setCurrentAirlineInfo(caiList);

                    emdCoupons.add(emdCoupon);
                    emdDoc.setCouponInfo(emdCoupons);
                    emdDocs.add(emdDoc);
                    emd.setTicketDocument(emdDocs);

                    List<EMDInfoDTO> paxEmds = pax.getEmdInfo();
                    if (paxEmds == null)
                        paxEmds = new ArrayList<>();
                    paxEmds.add(emd);
                    pax.setEmdInfo(paxEmds);
                    topEmdInfos.add(emd);
                }
            }
        }

        response.setPaxDetailList(paxList);
        response.setTicketDocInfoList(topLevelTicketDocs);
        if (!topEmdInfos.isEmpty()) {
            response.setEmdInfoList(topEmdInfos);
        }

        // 5. Order Items
        List<OrderItemsDTO> orderItems = new ArrayList<>();
        OrderItemsDTO item = new OrderItemsDTO();
        item.setOrderItemId(response.getResponseId() + "_AIR-1"); // Matching example format roughly
        item.setPtc("ADT");
        item.setTotalPrice(response.getTotalOrderPrice());
        item.setClassName(null); // Explicitly null per request

        List<String> paxIds = paxList.stream().map(PaxDetailDTO::getPaxId)
                .collect(Collectors.toList());
        item.setPassengerIds(paxIds);

        item.setTotalPrice(BigDecimal.ZERO);

        // Fares & Taxes
        BigDecimal totalTaxAmount = BigDecimal.ZERO;
        List<OrderItemsDTO.Tax> taxList = new ArrayList<>();

        // Define currency for this item
        String itemCurrency = booking.getCurrency() != null ? booking.getCurrency() : "USD";

        // Services & Baggage for OrderItem
        List<Service> serviceList = new ArrayList<>();
        List<BaggageAllowance> orderItemBags = new ArrayList<>();

        if (flightList != null) {
            int segCount = 1;
            for (Flight f : flightList) {
                totalTaxAmount = totalTaxAmount.add(BigDecimal.valueOf(f.getTotaltaxes()));
                if (f.getTaxes() != null) {
                    addTaxToList(taxList, "GH", "Ground Handling", f.getTaxes().getGroundHandling(), itemCurrency);
                    addTaxToList(taxList, "I2", "Security Tax", f.getTaxes().getSecurity(), itemCurrency);
                    addTaxToList(taxList, "YQ", "Fuel Surcharge", f.getTaxes().getFuel(), itemCurrency);
                    addTaxToList(taxList, "OT", "Other Tax", f.getTaxes().getTax4(), itemCurrency);
                }

                // Services
                if (combinedEmdMap.isEmpty()) {
                    if (f.getServices() != null) {
                        for (java.util.Map.Entry<String, Boolean> entry : f.getServices().entrySet()) {
                            if (Boolean.TRUE.equals(entry.getValue())) {
                                for (String loopPaxId : paxIds) {
                                    Service svc = new Service();
                                    String serviceSuffix = entry.getKey().length() > 3
                                            ? entry.getKey().substring(0, 3).toUpperCase()
                                            : entry.getKey().toUpperCase();
                                    svc.setServiceId("SEG" + segCount + "_" + loopPaxId + "_" + serviceSuffix);
                                    svc.setServiceStatus("CONFIRMED");
                                    svc.setSegmentId("SEG" + segCount);
                                    svc.setOdKey("OD" + segCount);
                                    svc.setDeparture(f.getFromcode());
                                    svc.setArrival(f.getTocode());
                                    serviceList.add(svc);

                                    // Checked bag?
                                    if ("CheckedInBaggage".equalsIgnoreCase(entry.getKey())) {
                                        BaggageAllowance ba = new BaggageAllowance();
                                        ba.setBaggageAllowanceId("FBA" + segCount);

                                        // Derive PTC from paxList
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
                                        ba.setName("Bag allowances");

                                        // Only add 30KG if ADT or equivalent, otherwise maybe 10KG for infant, etc.
                                        // Keeping it generic or omitting weight payload if unknown?
                                        // The user requested to remove hardcoding. We can map standard weights based on
                                        // PTC.
                                        BaggageAllowance.Weight w = new BaggageAllowance.Weight();
                                        w.setValue("INF".equals(paxPtc) ? "10" : "30");
                                        w.setUom("KG");
                                        List<BaggageAllowance.Weight> wl = new ArrayList<>();
                                        wl.add(w);
                                        ba.setWeight(wl);

                                        BaggageAllowance.DescriptionDTO desc = new BaggageAllowance.DescriptionDTO();
                                        desc.setDescription("Bag allowances");
                                        List<BaggageAllowance.DescriptionDTO> dl = new ArrayList<>();
                                        dl.add(desc);
                                        ba.setDescriptions(dl);

                                        orderItemBags.add(ba);
                                    } // end if CheckedInBaggage
                                } // end for loopPaxId
                            } // end if TRUE
                        } // end for entry
                    } // end if services != null
                } else {
                    // Match ChangeSeat mapping exactly for AIR item mapping
                    for (String pId : paxIds) {
                        Service svc = new Service();
                        svc.setServiceId("SEG" + segCount + "_" + pId);
                        svc.setServiceStatus("CONFIRMED");
                        serviceList.add(svc);
                    }
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

        orderItems.add(item);

        BigDecimal sumFlightPrice = BigDecimal.ZERO;
        if (flightList != null) {
            for (Flight f : flightList) {
                BigDecimal fBase = BigDecimal.ZERO;
                if (f.getInvpricingwithouttax() != null) {
                    try {
                        fBase = new BigDecimal(f.getInvpricingwithouttax());
                    } catch (Exception e) {}
                }
                sumFlightPrice = sumFlightPrice.add(fBase).add(BigDecimal.valueOf(f.getTotaltaxes()));
            }
        }

        BigDecimal invPricingBasis = BigDecimal.ZERO;
        if (flightList != null && !flightList.isEmpty()) {
            for (Flight f : flightList) {
                if (f.getInvpricing() != null) {
                    try {
                        invPricingBasis = invPricingBasis.add(new BigDecimal(f.getInvpricing()));
                    } catch (Exception e) {}
                }
            }
        }

        BigDecimal airItemPrice = (invPricingBasis.compareTo(BigDecimal.ZERO) > 0) ? invPricingBasis : sumFlightPrice;
        item.setTotalPrice(airItemPrice);

        List<OrderItemsDTO> secondaryOrderItems = new ArrayList<>();
        int srvIdx = serviceList.size() + 1;
        BigDecimal secondaryCharges = BigDecimal.ZERO;

        if (flightList != null && !flightList.isEmpty()) {
            Flight f = flightList.get(0);
            String segmentId = "SEG1";

            // Combine seats and services for unified iteration
            java.util.LinkedHashMap<String, String> combinedMap = new java.util.LinkedHashMap<>();
            if (passengerSeats != null) {
                for (java.util.Map.Entry<String, String> entry : passengerSeats.entrySet()) {
                    combinedMap.put(entry.getKey() + "_SEAT", entry.getValue());
                }
            }
            if (passengerServices != null) {
                for (Map.Entry<String, String> entry : passengerServices.entrySet()) {
                    combinedMap.put(entry.getKey() + "_SRV", entry.getValue());
                }
            }

            BigDecimal pnrTotal = BigDecimal.ZERO;
            if (booking.getBalanceInformation() != null && booking.getBalanceInformation().getPnrTotal() != null) {
                pnrTotal = booking.getBalanceInformation().getPnrTotal();
            } else if (booking.getTotalprice() != null) {
                try {
                    pnrTotal = new BigDecimal(booking.getTotalprice());
                } catch (Exception e) {}
            }

            // Map each designated seat/service to a separate SRV item
            for (Map.Entry<String, String> paxSecondary : combinedMap.entrySet()) {
                String rawKey = paxSecondary.getKey();
                String assignedValue = paxSecondary.getValue();

                boolean isSeat = rawKey.endsWith("_SEAT");
                String assignedPaxId = rawKey.substring(0, rawKey.lastIndexOf("_"));

                OrderItemsDTO secondaryItem = new OrderItemsDTO();
                secondaryItem.setOrderItemId(response.getOrderId() + "_SRV" + srvIdx++);

                // Fallback basic attributes
                String seatPtc = "ADT";
                for (PaxDetailDTO pax : paxList) {
                    if (pax.getPaxId().equals(assignedPaxId)) {
                        seatPtc = pax.getPtc();
                        break;
                    }
                }

                secondaryItem.setPtc(seatPtc);
                secondaryItem.setPassengerIds(Arrays.asList(assignedPaxId));

                String itemCode = assignedValue;
                BigDecimal itemPrice = BigDecimal.ZERO;
                
                if (assignedValue != null && assignedValue.contains("|")) {
                    String[] parts = assignedValue.split("\\|");
                    itemCode = parts[0];
                    try {
                        itemPrice = new BigDecimal(parts[1]);
                    } catch (Exception e) {}
                }

                // Fare info can't be purely extracted from Go7 response, check if specific seat
                // is present and use provided payment
                if (isSeat && f.getSeat() != null && itemPrice.compareTo(BigDecimal.ZERO) == 0) {
                    for (Seat s : f.getSeat()) {
                        if (itemCode.equals(s.getSeatNumber()) && s.getFare() != null) {
                            itemPrice = s.getFare();
                            break;
                        }
                    }
                }

                // Fallback: Infer price from PNR total difference if seat/service price is not explicitly stated
                if (itemPrice.compareTo(BigDecimal.ZERO) == 0 && pnrTotal.compareTo(airItemPrice) > 0) {
                    BigDecimal diff = pnrTotal.subtract(airItemPrice);
                    // Filter out items that already have a price from the cache to avoid diluting the distribution
                    long unpaidCount = combinedMap.values().stream().filter(v -> !v.contains("|") || new BigDecimal(v.split("\\|")[1]).compareTo(BigDecimal.ZERO) == 0).count();
                    if (unpaidCount > 0) {
                        itemPrice = diff.divide(new BigDecimal(unpaidCount), 2, java.math.RoundingMode.HALF_UP);
                    }
                }

                secondaryCharges = secondaryCharges.add(itemPrice);

                secondaryItem.setBaseFare(new OrderItemsDTO.BaseFare(itemPrice, itemCurrency));
                secondaryItem
                        .setTotalTax(new OrderItemsDTO.TotalTax(BigDecimal.ZERO, itemCurrency));
                secondaryItem.setTotalFare(new OrderItemsDTO.TotalFare(itemPrice, itemCurrency));
                secondaryItem.setTotalPrice(itemPrice);

                List<Service> secondaryServices = new ArrayList<>();
                Service mappedSrv = new Service();

                if (!isSeat) { // isService
                    String srvIdVal = itemCode;
                    if (srvIdVal != null && srvIdVal.startsWith("SRV_")) {
                        srvIdVal = srvIdVal.substring(4);
                    }
                    mappedSrv.setServiceId(srvIdVal); // The exact SRV_ID
                    mappedSrv.setServiceStatus("CONFIRMED"); // Standard for ancillary
                    mappedSrv.setServiceCode("SRV");
                    mappedSrv.setServiceName("Ancillary Service");
                } else { // isSeat
                    mappedSrv.setServiceId(segmentId + "_" + assignedPaxId);
                    mappedSrv.setServiceStatus("PENDING");
                    mappedSrv.setServiceCode("SEAT" + itemCode);
                    mappedSrv.setServiceName("Specific Seat Request");
                    mappedSrv.setSegmentId(segmentId);

                    // Parse Row/Col
                    if (itemCode != null && itemCode.length() > 0) {
                        String col = itemCode.substring(itemCode.length() - 1);
                        String rowStr = itemCode.substring(0, itemCode.length() - 1);
                        mappedSrv.setColumn(col);
                        try {
                            mappedSrv.setRow(new java.math.BigInteger(rowStr));
                        } catch (Exception e) {
                        }
                    }

                    List<Service.SeatCharacteristic> chars = new ArrayList<>();
                    chars.add(new Service.SeatCharacteristic("CH", "Chargeable Seat"));
                    chars.add(new Service.SeatCharacteristic("W", "Window seat"));
                    chars.add(new Service.SeatCharacteristic("FC",
                            "Front of cabin class/compartment"));
                    chars.add(new Service.SeatCharacteristic("N", "No smoking seat"));
                    mappedSrv.setSeatCharacteristics(chars);
                }

                secondaryServices.add(mappedSrv);
                secondaryItem.setServiceList(secondaryServices);

                secondaryOrderItems.add(secondaryItem);
            }
        }

        // Correct AirItem price logic - separate actual Base air from Seats/Services
        if (secondaryCharges.compareTo(BigDecimal.ZERO) > 0) {

            airItemPrice = (invPricingBasis.compareTo(BigDecimal.ZERO) > 0) ? invPricingBasis
                    : sumFlightPrice;

            // Recalculate AirItem based on separation
            BigDecimal airBaseFare = airItemPrice.subtract(totalTaxAmount);
            if (airBaseFare.compareTo(BigDecimal.ZERO) < 0)
                airBaseFare = airItemPrice;

            item.setBaseFare(new OrderItemsDTO.BaseFare(
                    airBaseFare.setScale(2, java.math.RoundingMode.HALF_UP), itemCurrency));
            item.setTotalFare(new OrderItemsDTO.TotalFare(
                    airItemPrice.setScale(2, java.math.RoundingMode.HALF_UP), itemCurrency));
            item.setTotalPrice(airItemPrice.setScale(2, java.math.RoundingMode.HALF_UP));
        }

        // Ensure global total price captures everything accurately
        totalOrderPrice = airItemPrice.add(secondaryCharges);

        response.setTotalOrderPrice(totalOrderPrice.setScale(2, java.math.RoundingMode.HALF_UP));
        if (!secondaryOrderItems.isEmpty()) {
            orderItems.addAll(secondaryOrderItems);
        }
        response.setOrderItems(orderItems);

        // Payments
        List<PaymentsDTO> payments = new ArrayList<>();
        String pType = requestDto.getPaymentType() != null ? requestDto.getPaymentType() : "CC";

        if (airItemPrice.compareTo(BigDecimal.ZERO) > 0) {
            PaymentsDTO payAir = new PaymentsDTO();
            payAir.setType(pType);
            payAir.setStatusCode("SUCCESSFUL");

            // Map the overarching uninflated Booking price to the Air item payment block
            // Since we removed the subtraction logic earlier, airItemPrice represents the
            // exact Air amount (e.g. 346.70)
            payAir.setAmount(airItemPrice.setScale(2, java.math.RoundingMode.HALF_UP));
            payAir.setCurrency(booking.getCurrency());
            payAir.setOrderItem(Arrays.asList(item.getOrderItemId()));
            payments.add(payAir);
        }

        if (secondaryCharges.compareTo(BigDecimal.ZERO) > 0 && !secondaryOrderItems.isEmpty()) {
            for (OrderItemsDTO secItem : secondaryOrderItems) {
                PaymentsDTO payAncillary = new PaymentsDTO();
                payAncillary.setType(pType);
                payAncillary.setStatusCode("SUCCESSFUL");

                // Individual item price
                BigDecimal itemAmount = (secItem.getTotalPrice() != null) ? secItem.getTotalPrice() : secondaryCharges;
                payAncillary.setAmount(itemAmount.setScale(2, java.math.RoundingMode.HALF_UP));
                payAncillary.setCurrency(booking.getCurrency());
                payAncillary.setOrderItem(Arrays.asList(secItem.getOrderItemId()));
                payments.add(payAncillary);
            }
        }

        if (payments.isEmpty()) {
            PaymentsDTO pay = new PaymentsDTO();
            pay.setType(pType);
            pay.setStatusCode("SUCCESSFUL");
            pay.setAmount(response.getTotalOrderPrice());
            pay.setCurrency(booking.getCurrency());
            pay.setOrderItem(Arrays.asList(item.getOrderItemId()));
            payments.add(pay);
        }

        response.setPayments(payments);

        return response;
    }

    private static void addTaxToList(List<OrderItemsDTO.Tax> list, String code, String description,
            double amount,
            String currency) {
        if (amount <= 0.001)
            return;
        OrderItemsDTO.Tax t = new OrderItemsDTO.Tax();
        t.setCode(code);
        t.setDescription(description);
        t.setAmount(BigDecimal.valueOf(amount));
        t.setCurrency(currency);
        list.add(t);
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

    

    

    

    

    

}