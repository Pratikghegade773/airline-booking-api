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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

        ChangePaymentRspGo7Dto.Booking booking = go7Response.getAerocrs().getBooking();

        // 1. Top Level Fields
        // response.setAgentId(requestDto.getAgentId() != null ? requestDto.getAgentId()
        // : "1416-AGT40148");
        // response.setAgencyName(requestDto.getAgencyName() != null ?
        // requestDto.getAgencyName() : "Fareintelligence");
        // response.setAgencyId(requestDto.getAgencyId() != null ?
        // requestDto.getAgencyId() : "1416");

        response.setResponseId("P" + UUID.randomUUID().toString().substring(0, 15).toUpperCase());

        String requestOrderId = (requestDto.getOrderId() != null && !requestDto.getOrderId().isEmpty())
                ? requestDto.getOrderId()
                : String.valueOf(booking.getBookingid());
        response.setOrderId(requestOrderId);

        response.setPnr(booking.getPnrref());

        response.setApiOwner("G7");

        // Pricing
        BigDecimal totalOrderPrice = BigDecimal.ZERO;
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

        if (flightList != null) {
            int segmentCounter = 1;
            int odCounter = 1;

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
                    // ... same date logic ...
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
                    od.setFlightNumber(flight.getNumber().replaceAll("[^0-9]", ""));
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
        // Also populate top-level TicketDocInfoList?? The example shows both pax-level
        // and top-level.
        List<ChangePaymentRspDto.TicketDocInfoDTO> topLevelTicketDocs = new ArrayList<>();

        List<ChangePaymentRspGo7Dto.Passenger> go7PaxList = (booking.getPassengers() != null)
                ? booking.getPassengers().getPassenger()
                : null;

        // Validation/Issuing Info from first flight if available
        String issuingAirlineName = "G7";
        String issuingPlace = "US"; // Default
        if (flightList != null && !flightList.isEmpty()) {
            ChangePaymentRspGo7Dto.Flight f = flightList.get(0);
            if (f.getAirline() != null)
                issuingAirlineName = f.getAirline();
            if (f.getFromcode() != null)
                issuingPlace = f.getFromcode();
        }

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
                pax.setGender(go7Pax.getGender() != null && go7Pax.getGender().startsWith("M") ? "Male" : "Female");
                pax.setBirthDate(formatDate(go7Pax.getDob())); // Should match ddMMMyyyy
                pax.setLanguage("English");

                // Emails match example
                if (go7Pax.getEmail() != null) {
                    ChangePaymentRspDto.PaxDetailDTO.EmailDTO email = new ChangePaymentRspDto.PaxDetailDTO.EmailDTO();
                    email.setEmailAddress(go7Pax.getEmail()); // Example NUK1234? Keeping actual email
                    email.setLabel("OTH");
                    email.setType("OSI");
//                    email.setLanguage("English");
                    List<ChangePaymentRspDto.PaxDetailDTO.EmailDTO> emails = new ArrayList<>();
                    emails.add(email);
                    pax.setEmails(emails);
                }

                // Map TicketDocInfo
                if (go7Pax.getETickets() != null && go7Pax.getETickets().getFlight() != null) {
                    List<ChangePaymentRspDto.TicketDocInfoDTO> ticketDocInfoList = new ArrayList<>();

                    for (ChangePaymentRspGo7Dto.Passenger.ETicketFlight etf : go7Pax.getETickets().getFlight()) {
                        ChangePaymentRspDto.TicketDocInfoDTO tdi = new ChangePaymentRspDto.TicketDocInfoDTO();
                        tdi.setValidatingCarrier("G7"); // Default
                        tdi.setIssuingAirlineName(issuingAirlineName); // Example
                        tdi.setIssuingPlace(issuingPlace); // Example
                        List<String> pIds = new ArrayList<>();
                        pIds.add(paxId);
                        tdi.setPaxId(pIds);

                        List<ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO> docs = new ArrayList<>();
                        ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO doc = new ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO();
                        doc.setTicketDocNbr(etf.getEticketnumber());
                        doc.setType("T");
                        doc.setNumberOfBooklets(1);
                        doc.setDateOfIssue(formatCurrentDate()); // Today
                        doc.setTimeOfIssue("00:00");
                        doc.setTicketingLocation(issuingPlace);
                        doc.setReportingType("BSP");

                        // Coupons - Map from Flight List
                        List<ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO> coupons = new ArrayList<>();
                        if (flightList != null) {
                            int couponNum = 1;
                            for (ChangePaymentRspGo7Dto.Flight f : flightList) {
                                ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO coupon = new ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO();
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
                                ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO cai = new ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO();
                                cai.setDepartureAirportCode(f.getFromcode());
                                cai.setArrivalAirportCode(f.getTocode());
                                cai.setDepartureDate(formatDate(f.getFlightdate()));
                                cai.setDepartureTime(f.getDepart());
                                cai.setDepartureAirportName(f.getFrom()); // Map name better?
                                cai.setDepartureTerminal(f.getDepartureTerminal());

                                // Arrival date calc again? Simplified here
                                cai.setArrivalDate(formatDate(f.getFlightdate()));
                                cai.setArrivalTime(f.getArrive());
                                cai.setArrivalAirportName(f.getTo());
                                cai.setArrivalTerminal(f.getArrivalTerminal());

                                cai.setMarketingCarrierAirlineId(f.getAirlinedesignator());
                                cai.setMarketingCarrierName(f.getAirline());
                                cai.setOperatingCarrierAirlineId(f.getAirlinedesignator());
                                cai.setOperatingCarrierName(f.getAirline());
                                cai.setFlightNumber(
                                        f.getNumber() != null ? f.getNumber().replaceAll("[^0-9]", "") : "");
                                cai.setEquipmentAircraftCode(f.getAircraftTypeIataCode());

                                List<ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO> caiList = new ArrayList<>();
                                caiList.add(cai);
                                coupon.setCurrentAirlineInfo(caiList);

                                // Baggage from Service?
                                List<ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.BaggageAllowance> bags = new ArrayList<>();
                                if (f.getServices() != null && f.getServices().containsKey("CheckedInBaggage")
                                        && f.getServices().get("CheckedInBaggage")) {
                                    ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.BaggageAllowance bag = new ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.BaggageAllowance();
                                    bag.setBaggageAllowanceId("FBA" + couponNum);
                                    bag.setPassengerId(paxId);
                                    bag.setCategory("Checked-In");
                                    bag.setName("Bag allowances");

                                    ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.BaggageAllowance.Weight w = new ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.BaggageAllowance.Weight();
                                    w.setValue(new BigDecimal(30));
                                    w.setUom("KG");
                                    List<ChangePaymentRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.BaggageAllowance.Weight> wList = new ArrayList<>();
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

        List<ChangePaymentRspDto.EMDInfoDTO> topEmdInfos = new ArrayList<>();

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
            for (ChangePaymentRspDto.PaxDetailDTO pax : paxList) {
                // Find all assigned secondary items for this specific PAX
                List<String> assignedValues = new ArrayList<>();
                for (java.util.Map.Entry<String, String> entry : combinedEmdMap.entrySet()) {
                    String paxIdKey = entry.getKey().substring(0, entry.getKey().lastIndexOf("_"));
                    if (paxIdKey.equals(pax.getPaxId())) {
                        assignedValues.add(entry.getValue());
                    }
                }

                for (String assignedValue : assignedValues) {
                    ChangePaymentRspDto.EMDInfoDTO emd = new ChangePaymentRspDto.EMDInfoDTO();
                    emd.setValidatingCarrier("API");
                    emd.setPaxId(Arrays.asList(pax.getPaxId()));
                    emd.setIssuingAirlineName(issuingAirlineName);
                    emd.setIssuingPlace(issuingPlace);

                    List<ChangePaymentRspDto.EMDInfoDTO.TicketDocumentDTO> emdDocs = new ArrayList<>();
                    ChangePaymentRspDto.EMDInfoDTO.TicketDocumentDTO emdDoc = new ChangePaymentRspDto.EMDInfoDTO.TicketDocumentDTO();

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
                    emdDoc.setDateOfIssue(formatCurrentDate());
                    emdDoc.setTimeOfIssue("00:00");
                    emdDoc.setTicketingLocation(emd.getIssuingPlace());
                    emdDoc.setReportingType("BSP");

                    List<ChangePaymentRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO> emdCoupons = new ArrayList<>();
                    ChangePaymentRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO emdCoupon = new ChangePaymentRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO();
                    emdCoupon.setCouponNumber(1);
                    emdCoupon.setValidatingCarrier("API");
                    emdCoupon.setStatus("I");

                    String refVal = assignedValue;
                    if (refVal != null && refVal.startsWith("SRV_")) {
                        refVal = refVal.substring(4);
                    }
                    emdCoupon.setServiceRefs(Arrays.asList(refVal));

                    ChangePaymentRspGo7Dto.Flight cf = flightList.get(0);
                    List<ChangePaymentRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO> caiList = new ArrayList<>();
                    ChangePaymentRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO cai = new ChangePaymentRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO();
                    cai.setDepartureAirportCode(cf.getFromcode());
                    cai.setArrivalAirportCode(cf.getTocode());
                    cai.setDepartureDate(formatDate(cf.getFlightdate()));
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

                    List<ChangePaymentRspDto.EMDInfoDTO> paxEmds = pax.getEmdInfo();
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
        List<ChangePaymentRspDto.OrderItemsDTO> orderItems = new ArrayList<>();
        ChangePaymentRspDto.OrderItemsDTO item = new ChangePaymentRspDto.OrderItemsDTO();
        item.setOrderItemId(response.getResponseId() + "_AIR-1"); // Matching example format roughly
        item.setPtc("ADT");
        item.setTotalPrice(response.getTotalOrderPrice());
        item.setClassName(null); // Explicitly null per request

        List<String> paxIds = paxList.stream().map(ChangePaymentRspDto.PaxDetailDTO::getPaxId)
                .collect(Collectors.toList());
        item.setPassengerIds(paxIds);

        if (booking.getBalanceInformation() != null) {
            item.setTotalPrice(booking.getBalanceInformation().getPnrTotal());
        } else if (booking.getTotalprice() != null) {
            try {
                item.setTotalPrice(new BigDecimal(booking.getTotalprice()));
            } catch (Exception e) {
                item.setTotalPrice(BigDecimal.ZERO);
            }
        }

        // Fares & Taxes
        BigDecimal totalTaxAmount = BigDecimal.ZERO;
        List<ChangePaymentRspDto.OrderItemsDTO.Tax> taxList = new ArrayList<>();

        // Define currency for this item
        String itemCurrency = booking.getCurrency() != null ? booking.getCurrency() : "USD";

        // Services & Baggage for OrderItem
        List<ChangePaymentRspDto.Service> serviceList = new ArrayList<>();
        List<ChangePaymentRspDto.OrderItemsDTO.BaggageAllowance> orderItemBags = new ArrayList<>();

        if (flightList != null) {
            int segCount = 1;
            for (ChangePaymentRspGo7Dto.Flight f : flightList) {
                totalTaxAmount = totalTaxAmount.add(BigDecimal.valueOf(f.getTotaltaxes()));
                if (f.getTaxes() != null) {
                    addTaxToList(taxList, "GH", "Ground Handling", f.getTaxes().getGroundHandling(), itemCurrency);
                    addTaxToList(taxList, "I2", "Security Tax", f.getTaxes().getSecurity(), itemCurrency);
                    addTaxToList(taxList, "YQ", "Fuel Surcharge", f.getTaxes().getFuel(), itemCurrency);
                    addTaxToList(taxList, "OT", "Other Tax", f.getTaxes().getTax_4(), itemCurrency);
                }

                // Services
                if (combinedEmdMap.isEmpty()) {
                    if (f.getServices() != null) {
                        for (java.util.Map.Entry<String, Boolean> entry : f.getServices().entrySet()) {
                            if (Boolean.TRUE.equals(entry.getValue())) {
                                ChangePaymentRspDto.Service svc = new ChangePaymentRspDto.Service();
                                String serviceSuffix = entry.getKey().length() > 3
                                        ? entry.getKey().substring(0, 3).toUpperCase()
                                        : entry.getKey().toUpperCase();
                                svc.setServiceId("SEG" + segCount + "_" + paxIds.get(0) + "_" + serviceSuffix);
                                svc.setServiceStatus("CONFIRMED");
                                svc.setSegmentId("SEG" + segCount);
                                svc.setOdKey("OD" + segCount);
                                svc.setDeparture(f.getFromcode());
                                svc.setArrival(f.getTocode());
                                // svc.setServiceName(entry.getKey()); // If field exists
                                serviceList.add(svc);

                                // Checked bag?
                                if ("CheckedInBaggage".equalsIgnoreCase(entry.getKey())) {
                                    ChangePaymentRspDto.OrderItemsDTO.BaggageAllowance ba = new ChangePaymentRspDto.OrderItemsDTO.BaggageAllowance();
                                    ba.setBaggageAllowanceId("FBA" + segCount);

                                    // Derive PTC from paxList
                                    String paxPtc = "ADT";
                                    for (ChangePaymentRspDto.PaxDetailDTO pax : paxList) {
                                        if (pax.getPaxId().equals(paxIds.get(0))) {
                                            paxPtc = pax.getPtc();
                                            break;
                                        }
                                    }

                                    ba.setPtc(paxPtc);
                                    ba.setPassengerId(paxIds.get(0));
                                    ba.setCategory("Checked-In");
                                    ba.setName("Bag allowances");

                                    // Only add 30KG if ADT or equivalent, otherwise maybe 10KG for infant, etc.
                                    // Keeping it generic or omitting weight payload if unknown?
                                    // The user requested to remove hardcoding. We can map standard weights based on
                                    // PTC.
                                    ChangePaymentRspDto.OrderItemsDTO.BaggageAllowance.Weight w = new ChangePaymentRspDto.OrderItemsDTO.BaggageAllowance.Weight();
                                    w.setValue("INF".equals(paxPtc) ? "10" : "30");
                                    w.setUom("KG");
                                    List<ChangePaymentRspDto.OrderItemsDTO.BaggageAllowance.Weight> wl = new ArrayList<>();
                                    wl.add(w);
                                    ba.setWeight(wl);

                                    ChangePaymentRspDto.OrderItemsDTO.BaggageAllowance.DescriptionDTO desc = new ChangePaymentRspDto.OrderItemsDTO.BaggageAllowance.DescriptionDTO();
                                    desc.setDescription("Bag allowances");
                                    List<ChangePaymentRspDto.OrderItemsDTO.BaggageAllowance.DescriptionDTO> dl = new ArrayList<>();
                                    dl.add(desc);
                                    ba.setDescriptions(dl);

                                    orderItemBags.add(ba);
                                }
                            }
                        }
                    }
                } else {
                    // Match ChangeSeat mapping exactly for AIR item mapping
                    for (String pId : paxIds) {
                        ChangePaymentRspDto.Service svc = new ChangePaymentRspDto.Service();
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

        ChangePaymentRspDto.OrderItemsDTO.TotalTax tt = new ChangePaymentRspDto.OrderItemsDTO.TotalTax();
        tt.setAmount(totalTaxAmount);
        tt.setCurrency(itemCurrency);
        item.setTotalTax(tt);

        orderItems.add(item);

        BigDecimal totalBookingPrice = item.getTotalPrice(); // Initially holds full booking total
        BigDecimal airItemPrice = totalBookingPrice;

        BigDecimal invPricingBasis = BigDecimal.ZERO;
        if (flightList != null && !flightList.isEmpty() && flightList.get(0).getInvpricing() != null) {
            try {
                invPricingBasis = new BigDecimal(flightList.get(0).getInvpricing());
            } catch (Exception e) {
            }
        }

        item.setTotalPrice(airItemPrice);

        List<ChangePaymentRspDto.OrderItemsDTO> secondaryOrderItems = new ArrayList<>();
        int srvIdx = serviceList.size() + 1;
        BigDecimal secondaryCharges = BigDecimal.ZERO;

        if (flightList != null && !flightList.isEmpty()) {
            ChangePaymentRspGo7Dto.Flight f = flightList.get(0);
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

            // Map each designated seat/service to a separate SRV item
            for (Map.Entry<String, String> paxSecondary : combinedMap.entrySet()) {
                String rawKey = paxSecondary.getKey();
                String assignedValue = paxSecondary.getValue();

                boolean isSeat = rawKey.endsWith("_SEAT");
                String assignedPaxId = rawKey.substring(0, rawKey.lastIndexOf("_"));

                ChangePaymentRspDto.OrderItemsDTO secondaryItem = new ChangePaymentRspDto.OrderItemsDTO();
                secondaryItem.setOrderItemId(response.getOrderId() + "_SRV" + srvIdx++);

                // Fallback basic attributes
                String seatPtc = "ADT";
                for (ChangePaymentRspDto.PaxDetailDTO pax : paxList) {
                    if (pax.getPaxId().equals(assignedPaxId)) {
                        seatPtc = pax.getPtc();
                        break;
                    }
                }

                secondaryItem.setPtc(seatPtc);
                secondaryItem.setPassengerIds(Arrays.asList(assignedPaxId));

                // Fare info can't be purely extracted from Go7 response, check if specific seat
                // is present and use provided payment
                BigDecimal itemPrice = BigDecimal.ZERO;
                if (isSeat && f.getSeat() != null) {
                    for (ChangePaymentRspGo7Dto.Seat s : f.getSeat()) {
                        if (assignedValue.equals(s.getSeat()) && s.getFare() != null) {
                            itemPrice = s.getFare();
                            break;
                        }
                    }
                }

                // If the payment details contain an amount, prioritize mapping the charge from
                // there if only one seat or equally distribute
                if (itemPrice.compareTo(BigDecimal.ZERO) == 0 && requestDto.getPaymentInformation() != null
                        && requestDto.getPaymentInformation().getAmount() != null) {
                    // Count how many items of this type (seat or service) are in the map to evenly
                    // distribute the payment specific to that type
                    long subsetSize = combinedMap.entrySet().stream()
                            .filter(e -> e.getKey().endsWith(isSeat ? "_SEAT" : "_SRV")).count();

                    if (subsetSize > 0) {
                        itemPrice = requestDto.getPaymentInformation().getAmount()
                                .divide(new BigDecimal(subsetSize), 2, java.math.RoundingMode.HALF_UP);
                    } else {
                        itemPrice = requestDto.getPaymentInformation().getAmount()
                                .divide(new BigDecimal(combinedMap.size()), 2, java.math.RoundingMode.HALF_UP);
                    }
                }

                secondaryCharges = secondaryCharges.add(itemPrice);

                secondaryItem.setBaseFare(new ChangePaymentRspDto.OrderItemsDTO.BaseFare(itemPrice, itemCurrency));
                secondaryItem
                        .setTotalTax(new ChangePaymentRspDto.OrderItemsDTO.TotalTax(BigDecimal.ZERO, itemCurrency));
                secondaryItem.setTotalFare(new ChangePaymentRspDto.OrderItemsDTO.TotalFare(itemPrice, itemCurrency));
                secondaryItem.setTotalPrice(itemPrice);

                List<ChangePaymentRspDto.Service> secondaryServices = new ArrayList<>();
                ChangePaymentRspDto.Service mappedSrv = new ChangePaymentRspDto.Service();

                if (!isSeat) { // isService
                    String srvIdVal = assignedValue;
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
                    mappedSrv.setServiceCode("SEAT" + assignedValue);
                    mappedSrv.setServiceName("Specific Seat Request");
                    mappedSrv.setSegmentId(segmentId);

                    // Parse Row/Col
                    if (assignedValue != null && assignedValue.length() > 0) {
                        String col = assignedValue.substring(assignedValue.length() - 1);
                        String rowStr = assignedValue.substring(0, assignedValue.length() - 1);
                        mappedSrv.setColumn(col);
                        try {
                            mappedSrv.setRow(new java.math.BigInteger(rowStr));
                        } catch (Exception e) {
                        }
                    }

                    List<ChangePaymentRspDto.Service.SeatCharacteristic> chars = new ArrayList<>();
                    chars.add(new ChangePaymentRspDto.Service.SeatCharacteristic("CH", "Chargeable Seat"));
                    chars.add(new ChangePaymentRspDto.Service.SeatCharacteristic("W", "Window seat"));
                    chars.add(new ChangePaymentRspDto.Service.SeatCharacteristic("FC",
                            "Front of cabin class/compartment"));
                    chars.add(new ChangePaymentRspDto.Service.SeatCharacteristic("N", "No smoking seat"));
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
                    : totalBookingPrice.subtract(secondaryCharges);
            if (airItemPrice.compareTo(BigDecimal.ZERO) < 0)
                airItemPrice = totalBookingPrice;

            // Recalculate AirItem based on separation
            BigDecimal airBaseFare = airItemPrice.subtract(totalTaxAmount);
            if (airBaseFare.compareTo(BigDecimal.ZERO) < 0)
                airBaseFare = airItemPrice;

            item.setBaseFare(new ChangePaymentRspDto.OrderItemsDTO.BaseFare(
                    airBaseFare.setScale(2, java.math.RoundingMode.HALF_UP), itemCurrency));
            item.setTotalFare(new ChangePaymentRspDto.OrderItemsDTO.TotalFare(
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
        List<ChangePaymentRspDto.PaymentsDTO> payments = new ArrayList<>();
        String pType = requestDto.getPaymentType() != null ? requestDto.getPaymentType() : "CC";

        if (airItemPrice.compareTo(BigDecimal.ZERO) > 0) {
            ChangePaymentRspDto.PaymentsDTO payAir = new ChangePaymentRspDto.PaymentsDTO();
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
            ChangePaymentRspDto.PaymentsDTO paySeat = new ChangePaymentRspDto.PaymentsDTO();
            paySeat.setType(pType);
            paySeat.setStatusCode("SUCCESSFUL");
            paySeat.setAmount(secondaryCharges.setScale(2, java.math.RoundingMode.HALF_UP));
            paySeat.setCurrency(booking.getCurrency());

            List<String> secItemIds = new ArrayList<>();
            for (ChangePaymentRspDto.OrderItemsDTO secItem : secondaryOrderItems) {
                secItemIds.add(secItem.getOrderItemId());
            }
            paySeat.setOrderItem(secItemIds);

            payments.add(paySeat);
        }

        if (payments.isEmpty()) {
            ChangePaymentRspDto.PaymentsDTO pay = new ChangePaymentRspDto.PaymentsDTO();
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

    private static void addTaxToList(List<ChangePaymentRspDto.OrderItemsDTO.Tax> list, String code, String description,
            double amount,
            String currency) {
        if (amount <= 0.001)
            return;
        ChangePaymentRspDto.OrderItemsDTO.Tax t = new ChangePaymentRspDto.OrderItemsDTO.Tax();
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

    private static String formatCurrentDate() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMMyyyy", Locale.ENGLISH));
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
