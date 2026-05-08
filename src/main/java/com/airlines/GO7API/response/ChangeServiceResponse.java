package com.airlines.GO7API.response;

import com.airlines.GO7API.responseDto.ChangeServiceRspDto;
import com.airlines.GO7API.responseGo7.ChangeServiceRspGo7Dto;
import com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto;

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
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChangeServiceResponse {

    public static ChangeServiceRspDto generateResponse(ChangeServiceRspGo7Dto changeServiceRsp,
            OrderRetrieveRspGo7Dto bookingRsp, com.airlines.GO7API.requestDto.ChangeServiceReqDto requestDto) {
        ChangeServiceRspDto response = new ChangeServiceRspDto();

        if (bookingRsp == null || bookingRsp.getAerocrs() == null || bookingRsp.getAerocrs().getBooking() == null) {
            return response;
        }

        OrderRetrieveRspGo7Dto.Booking booking = bookingRsp.getAerocrs().getBooking();

        // 1. Top Level Fields
        response.setResponseId("R" + java.util.UUID.randomUUID().toString().substring(0, 15).toUpperCase());
        response.setStatusCode(changeServiceRsp != null && changeServiceRsp.getAerocrs() != null
                && changeServiceRsp.getAerocrs().isSuccess() ? "OPENED" : "FAILED");

        // Initialize total order price - will be summed from order items
        BigDecimal totalOrderPrice = BigDecimal.ZERO;
        response.setTotalOrderPrice(BigDecimal.ZERO); // Placeholder

        // 1b. Global Price Calculations (Pre-calculated for use in payments and items)
        List<OrderRetrieveRspGo7Dto.Flight> flightList = booking.getFlights() != null
                && booking.getFlights().getFlight() != null ? booking.getFlights().getFlight()
                        : (booking.getItems() != null ? booking.getItems().getFlight() : null);

        BigDecimal totalTax = BigDecimal.ZERO;
        if (flightList != null) {
            for (OrderRetrieveRspGo7Dto.Flight f : flightList) {
                if (f.getTotaltaxes() > 0)
                    totalTax = totalTax.add(BigDecimal.valueOf(f.getTotaltaxes()));
            }
        }

        // Define Carrier Code dynamically
        String carrierCode = (changeServiceRsp != null && changeServiceRsp.getAerocrs() != null
                && changeServiceRsp.getAerocrs().getCompanycode() != null)
                        ? changeServiceRsp.getAerocrs().getCompanycode()
                        : "G7";

        if (carrierCode.equals("G7") && flightList != null && !flightList.isEmpty()) {
            String desig = flightList.get(0).getAirlinedesignator();
            if (desig != null)
                carrierCode = desig;
        }

        response.setApiOwner(carrierCode);
        response.setPnr(booking.getPnrref());
        response.setOrderId(booking.getBookingconfirmation() != null ? booking.getBookingconfirmation()
                : String.valueOf(booking.getBookingid()));
        response.setValidatingCarrier(carrierCode);
        response.setCurrency(booking.getCurrency() != null ? booking.getCurrency() : "USD");

        // 2. ODs (Flights)
        List<ChangeServiceRspDto.OD> ods = new ArrayList<>();
        List<ChangeServiceRspDto.PriceClass> pcl = new ArrayList<>();

        Map<String, String> flightSegmentMap = new HashMap<>();

        if (flightList != null) {
            int segId = 1;
            int odId = 1;
            for (OrderRetrieveRspGo7Dto.Flight flight : flightList) {
                ChangeServiceRspDto.OD od = new ChangeServiceRspDto.OD();
                String segIdStr = "SEG" + segId;
                od.setSegmentId(segIdStr);
                od.setOdKey("OD" + odId);
                od.setOrigin(flight.getFromcode());
                od.setDestination(flight.getTocode());
                od.setOriginAirportName(flight.getFrom());
                od.setDestinationAirportName(flight.getTo());
                od.setDepartureDate(formatDate(flight.getFlightdate()));
                od.setArrivalDate(formatDate(flight.getFlightdate())); // Simplified

                // Adjust for overnight
                if (flight.getDepart() != null && flight.getArrive() != null) {
                    try {
                        LocalTime depTime = LocalTime.parse(flight.getDepart());
                        LocalTime arrTime = LocalTime.parse(flight.getArrive());
                        if (arrTime.isBefore(depTime)) {
                            od.setArrivalDate(formatDate(adjustDateByDays(flight.getFlightdate(), 1)));
                        }
                    } catch (Exception e) {
                    }
                }

                od.setDepartureTime(flight.getDepart());
                od.setArrivalTime(flight.getArrive());
                od.setJourneyTime(calculateJourneyTime(flight.getFlightdate(), flight.getDepart(),
                        flight.getFlightdate(), flight.getArrive()));
                od.setFlightNumber(flight.getNumber());
                od.setEquipment(flight.getAircraftType());

                // Store segment mapping
                String flightKey = (flight.getNumber() != null ? flight.getNumber() : "") + "_"
                        + (flight.getFromcode() != null ? flight.getFromcode() : "") + "_"
                        + (flight.getTocode() != null ? flight.getTocode() : "");
                flightSegmentMap.put(flightKey, segIdStr);

                String mktCarrier = flight.getAirlinedesignator() != null ? flight.getAirlinedesignator() : carrierCode;
                String mktName = flight.getAirline() != null ? flight.getAirline() : "Airline";

                od.setMarketingCarrierCode(mktCarrier);
                od.setMarketingCarrierName(mktName);
                od.setOperatingCarrierCode(mktCarrier);
                od.setOperatingCarrierName(mktName);

                od.setDepartureTerminal(flight.getDepartureTerminal());
                od.setArrivalTerminal(flight.getArrivalTerminal());
                od.setChangeOfDay(0);

                // Cabin/Class Mapping
                String rawClass = flight.getFlightClass() != null ? flight.getFlightClass() : "";
                String cabinCode = "Economy";
                String rbdCode = null;
                String fareBasis = null;

                if (rawClass.contains("/")) {
                    String[] parts = rawClass.split("/");
                    if (parts.length > 0) {
                        String code = parts[0].trim().toUpperCase();
                        if (code.equals("F") || code.equals("A") || code.equals("P"))
                            cabinCode = "First";
                        else if (code.equals("C") || code.equals("J") || code.equals("D") || code.equals("Z")
                                || code.equals("I"))
                            cabinCode = "Business";
                        else
                            cabinCode = "Economy";
                        if (code.length() == 1)
                            rbdCode = code;
                    }
                } else if (!rawClass.isEmpty()) {
                    if (rawClass.length() == 1)
                        rbdCode = rawClass;
                }

                od.setCabinType(cabinCode);
                od.setPriceClassId("PC" + odId);
                od.setFareBasisCode(fareBasis);
                od.setRbdCode(rbdCode);

                ods.add(od);

                // Dynamic PriceClass Generation
                ChangeServiceRspDto.PriceClass pc = new ChangeServiceRspDto.PriceClass();
                pc.setPriceClassId("PC" + odId);

                String finalClassName = cabinCode;
                if (rawClass.contains("/")) {
                    String[] parts = rawClass.split("/");
                    if (parts.length > 1) {
                        finalClassName = parts[1].trim();
                    } else if (rbdCode != null) {
                        finalClassName = cabinCode + " (" + rbdCode + ")";
                    }
                } else if (!rawClass.isEmpty()) {
                    finalClassName = rawClass;
                }

                pc.setClassName(finalClassName);
                pc.setCabinTypeCode(cabinCode);

                List<ChangeServiceRspDto.PriceClass.Description> descs = new ArrayList<>();
                if (descs.isEmpty()) {
                    ChangeServiceRspDto.PriceClass.Description d = new ChangeServiceRspDto.PriceClass.Description();
                    d.setText("Standard Service");
                    d.setOdKey("OD" + odId);
                    descs.add(d);
                }
                pc.setDescriptions(descs);
                pcl.add(pc);
                segId++;
                odId++;
            }
        }
        response.setOds(ods);
        response.setPriceClassList(pcl);

        // 3. Booking References
        List<ChangeServiceRspDto.BookingReferences> refs = new ArrayList<>();
        ChangeServiceRspDto.BookingReferences ref = new ChangeServiceRspDto.BookingReferences();
        ref.setId(booking.getPnrref());
        ref.setAirlineId(carrierCode);
        refs.add(ref);
        response.setBookingReferences(refs);

        // Reuse OD info for coupons if available
        String defaultIssuingPlace = null;
        String defaultIssuingAirline = null;

        if (flightList != null && !flightList.isEmpty()) {
            if (flightList.get(0).getFromcode() != null)
                defaultIssuingPlace = flightList.get(0).getFromcode();
            if (flightList.get(0).getAirline() != null)
                defaultIssuingAirline = flightList.get(0).getAirline();
        }

        List<ChangeServiceRspDto.TicketDocInfoDTO> topTicketDocInfos = new ArrayList<>();
        List<ChangeServiceRspDto.EMDInfoDTO> topEmdInfos = new ArrayList<>();
        List<String> rawPaxIds = new ArrayList<>();
        List<ChangeServiceRspDto.PaxDetailDTO> paxList = new ArrayList<>();

        // CHECK IF PAYMENT WAS PROVIDED
        boolean isPaymentProvided = (requestDto != null && requestDto.getPaymentInformation() != null);

        BigDecimal srvTotalFromReq = isPaymentProvided && requestDto.getPaymentInformation().getAmount() != null
                ? requestDto.getPaymentInformation().getAmount()
                : BigDecimal.ZERO;

        // 4. Passenger Details & Service Mapping Logic (Mirroring ChangeSeat)
        Map<String, List<String>> paxToRequestedServices = new HashMap<>(); // PAX1 -> ["SRV123", "SRV456"]
        if (requestDto != null && requestDto.getOffers() != null) {
            for (com.airlines.GO7API.requestDto.ChangeServiceReqDto.Offer offer : requestDto.getOffers()) {
                if (offer.getOfferItems() != null) {
                    for (com.airlines.GO7API.requestDto.ChangeServiceReqDto.Offer.OfferItemDto item : offer
                            .getOfferItems()) {
                        if (item.getPaxRefs() != null && item.getOfferItemId() != null) {
                            for (String paxRef : item.getPaxRefs()) {
                                // Keep as T1
                                String pid = paxRef;
                                paxToRequestedServices.computeIfAbsent(pid, k -> new ArrayList<>())
                                        .add(item.getOfferItemId());
                            }
                        }
                    }
                }
            }
        }

        // Agent/Agency ID handled in EMD/TicketDocInfo below

        if (booking.getPassengers() != null && booking.getPassengers().getPassenger() != null) {
            int paxCounter = 1;
            List<ChangeServiceRspDto.PaxDetailDTO> adtList = new ArrayList<>();
            for (OrderRetrieveRspGo7Dto.Passenger p : booking.getPassengers().getPassenger()) {
                ChangeServiceRspDto.PaxDetailDTO pax = new ChangeServiceRspDto.PaxDetailDTO();

                String rawTitle = p.getPaxtitle() != null ? p.getPaxtitle().toUpperCase().replace(".", "") : "MR";
                boolean isInfantByTitle = rawTitle.contains("INF");

                String assignedPtc = mapPaxType(p.getPaxtype());
                if (isInfantByTitle) {
                    assignedPtc = "INF";
                }

                String pid;
                if ("INF".equals(assignedPtc)) {
                    if (!adtList.isEmpty()) {
                        ChangeServiceRspDto.PaxDetailDTO parent = adtList.get(adtList.size() - 1);
                        pid = parent.getPaxId() + ".1";
                        try {
                            parent.setInfantRef(pid);
                        } catch (Exception e) {
                        }
                    } else {
                        pid = "T" + paxCounter++ + ".1";
                    }
                } else {
                    pid = "T" + paxCounter++;
                    if ("ADT".equals(assignedPtc)) {
                        adtList.add(pax);
                    }
                }

                rawPaxIds.add(pid);
                pax.setPaxId(pid);
                pax.setPtc(assignedPtc);
                pax.setTitle(rawTitle);
                pax.setGivenName(p.getFirstname() != null ? p.getFirstname().toUpperCase() : "");
                pax.setSurname(p.getLastname() != null ? p.getLastname().toUpperCase() : "");
                pax.setBirthDate(formatDate(p.getDob()));

                // Gender Logic Match OrderRetrieve
                String gender = null;
                if (p.getGender() != null && !p.getGender().isEmpty()) {
                    if (p.getGender().toUpperCase().startsWith("M")) {
                        gender = "Male";
                    } else if (p.getGender().toUpperCase().startsWith("F")) {
                        gender = "Female";
                    }
                }

                // Title-based fallback
                if (gender == null && p.getPaxtitle() != null) {
                    String title = p.getPaxtitle().toUpperCase();
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

                if (p.getEmail() != null) {
                    ChangeServiceRspDto.PaxDetailDTO.EmailDTO email = new ChangeServiceRspDto.PaxDetailDTO.EmailDTO();
                    email.setEmailAddress(p.getEmail().toUpperCase());
                    email.setLabel("OTH");
                    email.setType("OSI");
                    List<ChangeServiceRspDto.PaxDetailDTO.EmailDTO> emails = new ArrayList<>();
                    emails.add(email);
                    pax.setEmails(emails);
                }

                // Ticket Doc Info (Pax Level) - ONLY IF PAYMENT PROVIDED
                if (isPaymentProvided && p.getETickets() != null && p.getETickets().getFlight() != null) {
                    List<ChangeServiceRspDto.TicketDocInfoDTO> paxTicketDocs = new ArrayList<>();

                    ChangeServiceRspDto.TicketDocInfoDTO tdi = new ChangeServiceRspDto.TicketDocInfoDTO();
                    tdi.setPaxId(Arrays.asList(pid));
                    tdi.setValidatingCarrier(carrierCode);
                    tdi.setIssuingAirlineName(defaultIssuingAirline);
                    tdi.setIssuingPlace(defaultIssuingPlace);

                    List<ChangeServiceRspDto.TicketDocInfoDTO.TicketDocumentDTO> docs = new ArrayList<>();

                    java.util.Set<String> uniqueTickets = new java.util.LinkedHashSet<>();
                    for (OrderRetrieveRspGo7Dto.Passenger.ETicketFlight etf : p.getETickets().getFlight()) {
                        if (etf.getEticketnumber() != null && !etf.getEticketnumber().isEmpty()) {
                            uniqueTickets.add(etf.getEticketnumber().trim());
                        }
                    }

                    for (String ticketNbr : uniqueTickets) {
                        ChangeServiceRspDto.TicketDocInfoDTO.TicketDocumentDTO doc = new ChangeServiceRspDto.TicketDocInfoDTO.TicketDocumentDTO();
                        doc.setTicketDocNbr(ticketNbr);
                        doc.setType("T");
                        doc.setNumberOfBooklets(1);
                        doc.setDateOfIssue(formatCurrentDate());
                        doc.setTimeOfIssue("00:00");
                        doc.setTicketingLocation(tdi.getIssuingPlace());
                        doc.setReportingType("BSP");

                        // Coupons
                        List<ChangeServiceRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO> coupons = new ArrayList<>();
                        if (flightList != null) {
                            int couponNum = 1;
                            for (OrderRetrieveRspGo7Dto.Flight f : flightList) {
                                ChangeServiceRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO coupon = new ChangeServiceRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO();
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
                                coupon.setValidatingCarrier(carrierCode);

                                ChangeServiceRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO cai = new ChangeServiceRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO();
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
                                cai.setEquipmentAircraftCode(
                                        f.getAircraftTypeIataCode() != null ? f.getAircraftTypeIataCode()
                                                : f.getAircraftType());

                                List<ChangeServiceRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO> caiList = new ArrayList<>();
                                caiList.add(cai);
                                coupon.setCurrentAirlineInfo(caiList);
                                coupons.add(coupon);
                            }
                        }
                        doc.setCouponInfo(coupons);
                        docs.add(doc);
                    }
                    tdi.setTicketDocument(docs);
                    paxTicketDocs.add(tdi);
                    pax.setTicketDocInfo(paxTicketDocs);
                    topTicketDocInfos.add(tdi);
                }

                // EMD Info - ONLY IF PAYMENT PROVIDED
                if (isPaymentProvided && flightList != null && !flightList.isEmpty()) {
                    List<ChangeServiceRspDto.EMDInfoDTO> paxEmds = new ArrayList<>();

                    int flightIdx = 0;
                    for (OrderRetrieveRspGo7Dto.Flight f : flightList) {
                        flightIdx++;

                        ChangeServiceRspDto.EMDInfoDTO emd = new ChangeServiceRspDto.EMDInfoDTO();
                        emd.setValidatingCarrier(carrierCode);
                        emd.setPaxId(Arrays.asList(pid));
                        emd.setIssuingAirlineName(defaultIssuingAirline);
                        emd.setIssuingPlace(defaultIssuingPlace);

                        List<ChangeServiceRspDto.EMDInfoDTO.TicketDocumentDTO> emdDocs = new ArrayList<>();
                        ChangeServiceRspDto.EMDInfoDTO.TicketDocumentDTO emdDoc = new ChangeServiceRspDto.EMDInfoDTO.TicketDocumentDTO();
                        String emdNum = "619" + String.valueOf(System.currentTimeMillis()).substring(3);
                        if (booking.getLinktoticket() != null && !booking.getLinktoticket().isEmpty()) {
                            emdNum = booking.getLinktoticket();
                        }

                        String tktNum = (p.getETickets() != null && p.getETickets().getFlight() != null
                                && !p.getETickets().getFlight().isEmpty())
                                        ? p.getETickets().getFlight().get(0).getEticketnumber()
                                        : emdNum;

                        emdDoc.setTicketDocNbr(tktNum + "-" + (topEmdInfos.size() + 1));
                        emdDoc.setConnectedDocNbr(null);
                        emdDoc.setType("J");
                        emdDoc.setNumberOfBooklets(1);
                        emdDoc.setDateOfIssue(formatCurrentDate());
                        emdDoc.setTimeOfIssue("00:00");
                        emdDoc.setTicketingLocation(emd.getIssuingPlace());
                        emdDoc.setReportingType("BSP");

                        List<ChangeServiceRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO> emdCoupons = new ArrayList<>();
                        ChangeServiceRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO emdCoupon = new ChangeServiceRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO();
                        emdCoupon.setCouponNumber(1);
                        emdCoupon.setValidatingCarrier(carrierCode);
                        emdCoupon.setStatus("I");

                        if (requestDto.getOffers() != null && !requestDto.getOffers().isEmpty()) {
                            List<String> srvRefs = new ArrayList<>();
                            for (com.airlines.GO7API.requestDto.ChangeServiceReqDto.Offer offer : requestDto
                                    .getOffers()) {
                                if (offer.getOfferItems() != null) {
                                    for (com.airlines.GO7API.requestDto.ChangeServiceReqDto.Offer.OfferItemDto item : offer
                                            .getOfferItems()) {
                                        if (item.getOfferItemId() != null) {
                                            srvRefs.add(item.getOfferItemId());
                                        }
                                    }
                                }
                            }
                            emdCoupon.setServiceRefs(srvRefs);
                        } else {
                            // Fallback if no specific offer items are mapped
                            emdCoupon.setServiceRefs(Arrays.asList("SEG" + flightIdx + "_" + pid + "_SRV"));
                        }

                        List<ChangeServiceRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO> caiList = new ArrayList<>();
                        ChangeServiceRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO cai = new ChangeServiceRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO();
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
                        cai.setOperatingCarrierAirlineId(f.getAirlinedesignator());
                        cai.setMarketingCarrierName(f.getAirline());
                        cai.setOperatingCarrierName(
                                f.getAirline() != null ? f.getAirline().toUpperCase() : "");
                        cai.setFlightNumber(f.getNumber());
                        cai.setEquipmentAircraftCode(f.getAircraftTypeIataCode() != null ? f.getAircraftTypeIataCode()
                                : f.getAircraftType());

                        caiList.add(cai);
                        emdCoupon.setCurrentAirlineInfo(caiList);

                        emdCoupons.add(emdCoupon);
                        emdDoc.setCouponInfo(emdCoupons);
                        emdDocs.add(emdDoc);
                        emd.setTicketDocument(emdDocs);

                        paxEmds.add(emd);
                        topEmdInfos.add(emd);
                    }
                    if (!paxEmds.isEmpty()) {
                        pax.setEmdInfo(paxEmds);
                    }
                }

                paxList.add(pax);
            }
        }
        response.setPaxDetailList(paxList);

        // Conditional Top Level Population
        if (isPaymentProvided) {
            response.setTicketDocInfoList(topTicketDocInfos);
            response.setEmdInfoList(topEmdInfos);
        } else {
            response.setTicketDocInfoList(null);
            response.setEmdInfoList(null);
        }

        // 5. Payments (Reordered below Order Items)

        // 6. Order Items
        List<ChangeServiceRspDto.OrderItemsDTO> orderItems = new ArrayList<>();

        // 6a. Main Flight Item (AIR-1)
        ChangeServiceRspDto.OrderItemsDTO airItem = new ChangeServiceRspDto.OrderItemsDTO();
        airItem.setOrderItemId(response.getOrderId() + "_AIR-1");

        String primaryPtc = "ADT";
        if (booking.getPassengers() != null && booking.getPassengers().getPassenger() != null
                && !booking.getPassengers().getPassenger().isEmpty()) {
            primaryPtc = mapPaxType(booking.getPassengers().getPassenger().get(0).getPaxtype());
        }
        airItem.setPtc(primaryPtc);

        // 1. Calculate Actual Service Fare from AeroCRS details
        BigDecimal actualSrvPrice = BigDecimal.ZERO;
        if (changeServiceRsp != null && changeServiceRsp.getAerocrs() != null
                && changeServiceRsp.getAerocrs().getDetails() != null) {
            for (ChangeServiceRspGo7Dto.Aerocrs.Detail detail : changeServiceRsp.getAerocrs().getDetails()) {
                if (detail.getAncillary() != null) {
                    Object priceObj = detail.getAncillary().get("totalprice");
                    if (priceObj != null) {
                        try {
                            actualSrvPrice = actualSrvPrice.add(new BigDecimal(priceObj.toString()));
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }

        // 2. Calculate Air Item Price from original booking
        BigDecimal sumFlightPrice = BigDecimal.ZERO;
        totalTax = BigDecimal.ZERO;
        if (flightList != null) {
            for (OrderRetrieveRspGo7Dto.Flight f : flightList) {
                BigDecimal fBase = BigDecimal.ZERO;
                if (f.getInvpricingwithouttax() != null) {
                    try {
                        fBase = new BigDecimal(f.getInvpricingwithouttax());
                    } catch (Exception e) {}
                }
                if (f.getTotaltaxes() > 0) {
                    totalTax = totalTax.add(BigDecimal.valueOf(f.getTotaltaxes()));
                }
                sumFlightPrice = sumFlightPrice.add(fBase).add(BigDecimal.valueOf(f.getTotaltaxes()));
            }
        }
        
        BigDecimal invPricingBasis = BigDecimal.ZERO;
        if (flightList != null && !flightList.isEmpty()) {
            for (OrderRetrieveRspGo7Dto.Flight f : flightList) {
                if (f.getInvpricing() != null) {
                    try {
                        invPricingBasis = invPricingBasis.add(new BigDecimal(f.getInvpricing()));
                    } catch (Exception e) {}
                }
            }
        }
        
        BigDecimal airItemPrice = (invPricingBasis.compareTo(BigDecimal.ZERO) > 0) ? invPricingBasis : sumFlightPrice;

        // 3. Payment Amount for Services (Strictly from request if provided)
        BigDecimal paymentSrvAmount = BigDecimal.ZERO;
        if (isPaymentProvided && requestDto.getPaymentInformation().getAmount() != null) {
            paymentSrvAmount = requestDto.getPaymentInformation().getAmount();
        } else {
            paymentSrvAmount = actualSrvPrice;
        }

        // Calculate pnrTotal from booking response
        BigDecimal pnrTotal = BigDecimal.ZERO;
        if (booking.getBalanceInformation() != null && booking.getBalanceInformation().getPnrTotal() != null) {
            pnrTotal = booking.getBalanceInformation().getPnrTotal();
        } else if (booking.getTotalprice() != null) {
            try {
                pnrTotal = new BigDecimal(booking.getTotalprice());
            } catch (Exception e) {}
        }

        // If actualSrvPrice (from Go7) is 0:
        if (actualSrvPrice.compareTo(BigDecimal.ZERO) <= 0) {
            // Priority 1: Infer from PNR total difference (Ground Truth of what Go7 added to the booking)
            if (pnrTotal.compareTo(airItemPrice) > 0) {
                actualSrvPrice = pnrTotal.subtract(airItemPrice);
            } 
            // Priority 2: If no PNR diff, use specific payment block amount provided by user
            else if (isPaymentProvided && paymentSrvAmount.compareTo(BigDecimal.ZERO) > 0) {
                actualSrvPrice = paymentSrvAmount;
            } 
            // Priority 3: Default
            else {
                actualSrvPrice = paymentSrvAmount;
            }
        }

        airItemPrice = airItemPrice.setScale(2, java.math.RoundingMode.HALF_UP);

        BigDecimal airBaseFare = airItemPrice.subtract(totalTax);
        if (airBaseFare.compareTo(BigDecimal.ZERO) < 0)
            airBaseFare = airItemPrice;

        airItem.setBaseFare(new ChangeServiceRspDto.OrderItemsDTO.BaseFare(
                airBaseFare.setScale(2, java.math.RoundingMode.HALF_UP), response.getCurrency()));
        airItem.setTotalTax(new ChangeServiceRspDto.OrderItemsDTO.TotalTax(
                totalTax.setScale(2, java.math.RoundingMode.HALF_UP), response.getCurrency()));
        airItem.setTotalFare(new ChangeServiceRspDto.OrderItemsDTO.TotalFare(
                airItemPrice.setScale(2, java.math.RoundingMode.HALF_UP), response.getCurrency()));
        airItem.setTotalPrice(airItemPrice.setScale(2, java.math.RoundingMode.HALF_UP));
        airItem.setPassengerIds(rawPaxIds);

        // Taxes Breakdown
        List<ChangeServiceRspDto.OrderItemsDTO.Tax> airTaxes = new ArrayList<>();
        if (flightList != null && totalTax.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalGranular = BigDecimal.ZERO;
            for (OrderRetrieveRspGo7Dto.Flight f : flightList) {
                if (f.getTaxes() != null) {
                    totalGranular = totalGranular.add(BigDecimal.valueOf(f.getTaxes().getSecurity()));
                    totalGranular = totalGranular.add(BigDecimal.valueOf(f.getTaxes().getFuel()));
                    totalGranular = totalGranular.add(BigDecimal.valueOf(f.getTaxes().getGroundHandling()));
                    totalGranular = totalGranular.add(BigDecimal.valueOf(f.getTaxes().getTax_1()));
                    totalGranular = totalGranular.add(BigDecimal.valueOf(f.getTaxes().getTax_4()));
                }
            }

            BigDecimal scaleFactor = BigDecimal.ONE;
            if (totalGranular.compareTo(BigDecimal.ZERO) > 0) {
                scaleFactor = totalTax.divide(totalGranular, 10, java.math.RoundingMode.HALF_UP);
            }

            for (OrderRetrieveRspGo7Dto.Flight f : flightList) {
                if (f.getTaxes() != null) {
                    OrderRetrieveRspGo7Dto.Taxes tObj = f.getTaxes();
                    addScaledTaxItem(airTaxes, "I2", tObj.getSecurity(), scaleFactor, response.getCurrency(),
                            "Security Tax");
                    addScaledTaxItem(airTaxes, "YQ", tObj.getFuel(), scaleFactor, response.getCurrency(),
                            "Fuel Surcharge");
                    addScaledTaxItem(airTaxes, "GH", tObj.getGroundHandling(), scaleFactor, response.getCurrency(),
                            "Ground Handling");
                    addScaledTaxItem(airTaxes, "IN", tObj.getTax_1(), scaleFactor, response.getCurrency(),
                            "Infrastructure Tax");
                    addScaledTaxItem(airTaxes, "OT", tObj.getTax_4(), scaleFactor, response.getCurrency(), "Other Tax");
                }
            }
        }
        if (!airTaxes.isEmpty())
            airItem.setTaxes(airTaxes);

        // Flight Services (Confirmed segment services)
        List<ChangeServiceRspDto.Service> flightServices = new ArrayList<>();
        if (flightList != null) {
            for (OrderRetrieveRspGo7Dto.Flight f : flightList) {
                String flightKey = (f.getNumber() != null ? f.getNumber() : "") + "_"
                        + (f.getFromcode() != null ? f.getFromcode() : "") + "_"
                        + (f.getTocode() != null ? f.getTocode() : "");
                String segmentId = flightSegmentMap.getOrDefault(flightKey, "SEG1");
                for (String pid : rawPaxIds) {
                    ChangeServiceRspDto.Service srv = new ChangeServiceRspDto.Service();
                    srv.setServiceId(segmentId + "_" + pid);
                    srv.setServiceStatus("CONFIRMED");
                    flightServices.add(srv);
                }
            }
        }
        airItem.setServiceList(flightServices);
        orderItems.add(airItem);
        // Total order price tracking
        BigDecimal totalSrvPrice = BigDecimal.ZERO;

        boolean detailsProvided = false;
        int srvIdx = 2; // Starting index for services

        if (changeServiceRsp != null && changeServiceRsp.getAerocrs() != null
                && changeServiceRsp.getAerocrs().getDetails() != null) {
            for (ChangeServiceRspGo7Dto.Aerocrs.Detail detail : changeServiceRsp.getAerocrs().getDetails()) {
                if (detail.getAncillary() != null) {
                    detailsProvided = true;
                    ChangeServiceRspDto.OrderItemsDTO srvItem = new ChangeServiceRspDto.OrderItemsDTO();
                    srvItem.setOrderItemId(response.getOrderId() + "_SRV" + srvIdx++);
                    srvItem.setPtc("ADT");

                    // Extract Price from ancillary Map with Fallback
                    BigDecimal srvPrice = BigDecimal.ZERO;
                    Object priceObj = detail.getAncillary().get("totalprice");
                    if (priceObj != null) {
                        try {
                            srvPrice = new BigDecimal(priceObj.toString());
                        } catch (Exception e) {
                        }
                    }

                    if (srvPrice.compareTo(BigDecimal.ZERO) <= 0) {
                        srvPrice = actualSrvPrice;
                    }

                    String srvCurrency = response.getCurrency();
                    Object currObj = detail.getAncillary().get("currency");
                    if (currObj != null)
                        srvCurrency = currObj.toString();

                    srvItem.setBaseFare(new ChangeServiceRspDto.OrderItemsDTO.BaseFare(srvPrice, srvCurrency));
                    srvItem.setTotalTax(new ChangeServiceRspDto.OrderItemsDTO.TotalTax(BigDecimal.ZERO, srvCurrency));
                    srvItem.setTotalFare(new ChangeServiceRspDto.OrderItemsDTO.TotalFare(srvPrice, srvCurrency));
                    srvItem.setTotalPrice(srvPrice);
                    srvItem.setPassengerIds(Arrays.asList(rawPaxIds.isEmpty() ? "T1" : rawPaxIds.get(0)));

                    // Service List for this Ancillary
                    List<ChangeServiceRspDto.Service> srvList = new ArrayList<>();
                    ChangeServiceRspDto.Service srv = new ChangeServiceRspDto.Service();

                    String srvName = detail.getAncillary().get("name") != null
                            ? detail.getAncillary().get("name").toString()
                            : "Ancillary Service";
                    String srvCode = detail.getAncillary().get("code") != null
                            ? detail.getAncillary().get("code").toString()
                            : (detail.getAncillary().get("item_name") != null
                                    ? detail.getAncillary().get("item_name").toString()
                                    : "SRV");

                    if ("SRV".equals(srvCode) && !paxToRequestedServices.isEmpty()) {
                        String primaryPax = rawPaxIds.isEmpty() ? "T1" : rawPaxIds.get(0);
                        List<String> requestedSrvs = paxToRequestedServices.get(primaryPax);
                        if (requestedSrvs != null && !requestedSrvs.isEmpty()) {
                            srvCode = requestedSrvs.get(0);
                        }
                    }

                    srv.setServiceId("SRV_" + detail.getInvid());
                    srv.setServiceStatus(detail.isSuccess() ? "CONFIRMED" : "PENDING");
                    srv.setServiceCode(srvCode);
                    srv.setServiceName(srvName);

                    srvList.add(srv);
                    srvItem.setServiceList(srvList);
                    orderItems.add(srvItem);
                    totalSrvPrice = totalSrvPrice.add(srvPrice);
                }
            }
        }

        // Fallback: If Go7 didn't return ancillary details but services were requested
        if (!detailsProvided && !paxToRequestedServices.isEmpty()) {
            int totalServices = 0;
            for (List<String> srvs : paxToRequestedServices.values()) {
                totalServices += srvs.size();
            }
            BigDecimal pricePerSrv = BigDecimal.ZERO;
            if (totalServices > 0 && actualSrvPrice.compareTo(BigDecimal.ZERO) > 0) {
                pricePerSrv = actualSrvPrice.divide(new BigDecimal(totalServices), 2, java.math.RoundingMode.HALF_UP);
            }

            for (Map.Entry<String, List<String>> entry : paxToRequestedServices.entrySet()) {
                String pid = entry.getKey();
                for (String srvRef : entry.getValue()) {
                    ChangeServiceRspDto.OrderItemsDTO srvItem = new ChangeServiceRspDto.OrderItemsDTO();
                    srvItem.setOrderItemId(response.getOrderId() + "_SRV" + srvIdx++);
                    srvItem.setPtc("ADT");

                    String srvCurrency = response.getCurrency();

                    srvItem.setBaseFare(new ChangeServiceRspDto.OrderItemsDTO.BaseFare(pricePerSrv, srvCurrency));
                    srvItem.setTotalTax(new ChangeServiceRspDto.OrderItemsDTO.TotalTax(BigDecimal.ZERO, srvCurrency));
                    srvItem.setTotalFare(new ChangeServiceRspDto.OrderItemsDTO.TotalFare(pricePerSrv, srvCurrency));
                    srvItem.setTotalPrice(pricePerSrv);
                    String safePid = rawPaxIds.contains(pid) ? pid : (rawPaxIds.isEmpty() ? "T1" : rawPaxIds.get(0));
                    srvItem.setPassengerIds(Arrays.asList(safePid));

                    List<ChangeServiceRspDto.Service> srvList = new ArrayList<>();
                    ChangeServiceRspDto.Service srv = new ChangeServiceRspDto.Service();
                    srv.setServiceId("SRV_" + srvRef);
                    srv.setServiceStatus("CONFIRMED");
                    srv.setServiceCode(srvRef);
                    srv.setServiceName("Ancillary Service");

                    srvList.add(srv);
                    srvItem.setServiceList(srvList);
                    orderItems.add(srvItem);
                    totalSrvPrice = totalSrvPrice.add(pricePerSrv);
                }
            }
        }

        response.setOrderItems(orderItems);
        response.setTotalOrderPrice(airItemPrice.add(totalSrvPrice).setScale(2, java.math.RoundingMode.HALF_UP));

        // 5. Payments split
        if (isPaymentProvided) {
            List<ChangeServiceRspDto.PaymentsDTO> payments = new ArrayList<>();
            String pType = requestDto.getPaymentType() != null ? requestDto.getPaymentType() : "CA";

            // Payment for Service Item (Combined New Charges)
            if (paymentSrvAmount.compareTo(BigDecimal.ZERO) > 0 || actualSrvPrice.compareTo(BigDecimal.ZERO) > 0) {
                ChangeServiceRspDto.PaymentsDTO paySrv = new ChangeServiceRspDto.PaymentsDTO();
                paySrv.setType(pType);
                paySrv.setStatusCode("SUCCESSFUL");
                paySrv.setCurrency(response.getCurrency());

                // Prioritize paymentSrvAmount (which has the request amount) for the payment
                // block
                paySrv.setAmount(paymentSrvAmount.setScale(2, java.math.RoundingMode.HALF_UP));

                // If paymentSrvAmount is 0, fallback to totalSrvPrice if available
                if (paymentSrvAmount.compareTo(BigDecimal.ZERO) <= 0 && totalSrvPrice.compareTo(BigDecimal.ZERO) > 0) {
                    paySrv.setAmount(totalSrvPrice.setScale(2, java.math.RoundingMode.HALF_UP));
                }
                // Collect service item IDs
                List<String> srvRefs = new ArrayList<>();
                for (ChangeServiceRspDto.OrderItemsDTO item : orderItems) {
                    if (item.getOrderItemId() != null && item.getOrderItemId().contains("_SRV")) {
                        srvRefs.add(item.getOrderItemId());
                    }
                }
                paySrv.setOrderItem(srvRefs);
                payments.add(paySrv);
            }
            response.setPayments(payments);
        } else {
            response.setPayments(null);
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

    private static String formatDate(String dateStr) {
        if (dateStr == null)
            return null;
        try {
            DateTimeFormatter inputFormatter = dateStr.contains("/") ? DateTimeFormatter.ofPattern("yyyy/MM/dd")
                    : DateTimeFormatter.ofPattern("yyyy-MM-dd");
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
            DateTimeFormatter inputFormatter = dateStr.contains("/") ? DateTimeFormatter.ofPattern("yyyy/MM/dd")
                    : DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate date = LocalDate.parse(dateStr, inputFormatter);
            return date.plusDays(days).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return dateStr;
        }
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

    private static String formatCurrentDate() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMMyyyy", Locale.ENGLISH));
    }

    private static void addScaledTaxItem(List<ChangeServiceRspDto.OrderItemsDTO.Tax> list, String code, double amount,
            BigDecimal scaleFactor, String currency, String description) {
        if (amount > 0) {
            ChangeServiceRspDto.OrderItemsDTO.Tax t = new ChangeServiceRspDto.OrderItemsDTO.Tax();
            t.setCode(code);
            BigDecimal scaledAmount = BigDecimal.valueOf(amount).multiply(scaleFactor).setScale(2,
                    java.math.RoundingMode.HALF_UP);
            t.setAmount(scaledAmount);
            t.setCurrency(currency);
            t.setDescription(description);
            list.add(t);
        }
    }

    private static BigDecimal fetchPriceFromLink(String url) {
        if (url == null || url.isEmpty())
            return null;
        try {
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);
            if (response != null && !response.isEmpty()) {
                if (response.startsWith("{")) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(response);
                    if (root.has("amount")) {
                        return root.get("amount").decimalValue();
                    } else if (root.has("totalAmount")) {
                        return root.get("totalAmount").decimalValue();
                    } else if (root.has("total")) {
                        return root.get("total").decimalValue();
                    }
                }
                // Try to extract dollar-prefixed decimal number if it's plain text or html
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\$\\s?(\\d+(\\.\\d+)?)");
                java.util.regex.Matcher m = p.matcher(response);
                BigDecimal lastFound = null;
                while (m.find()) {
                    try {
                        lastFound = new BigDecimal(m.group(1));
                    } catch (Exception e) {
                    }
                }
                if (lastFound != null)
                    return lastFound;

                // Fallback to any decimal if no $ found
                p = java.util.regex.Pattern.compile("-?\\d+(\\.\\d+)");
                m = p.matcher(response);
                if (m.find()) {
                    return new BigDecimal(m.group());
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching price from link [" + url + "]: " + e.getMessage());
        }
        return null;
    }
}
