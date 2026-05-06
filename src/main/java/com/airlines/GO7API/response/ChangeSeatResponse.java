package com.airlines.GO7API.response;

import com.airlines.GO7API.responseDto.ChangeSeatRspDto;
import com.airlines.GO7API.responseGo7.ChangeSeatRspGo7Dto;
import com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChangeSeatResponse {

    public static ChangeSeatRspDto generateResponse(ChangeSeatRspGo7Dto changeSeatRsp,
            OrderRetrieveRspGo7Dto bookingRsp, com.airlines.GO7API.requestDto.ChangeSeatReqDto requestDto) {
        ChangeSeatRspDto response = new ChangeSeatRspDto();

        if (bookingRsp == null || bookingRsp.getAerocrs() == null || bookingRsp.getAerocrs().getBooking() == null) {
            return response;
        }

        OrderRetrieveRspGo7Dto.Booking booking = bookingRsp.getAerocrs().getBooking();

        // 1. Top Level Fields
        // response.setResponseId("GR#QEW$S#CQ00K#03RR#4AA7M0"); // Static or generated
        // - keeping static/random for now as no upstream ID maps directly
        response.setResponseId("R" + java.util.UUID.randomUUID().toString().substring(0, 15).toUpperCase());

        response.setStatusCode(changeSeatRsp.getAerocrs().isSuccess() ? "OPENED" : "FAILED");

        // Dynamic Carrier Code
        String carrierCode = "G7"; // Default
        if (changeSeatRsp.getAerocrs() != null && changeSeatRsp.getAerocrs().getCompanycode() != null) {
            carrierCode = changeSeatRsp.getAerocrs().getCompanycode();
        } else if (booking.getFlights() != null && booking.getFlights().getFlight() != null
                && !booking.getFlights().getFlight().isEmpty()) {
            String airlineDesignator = booking.getFlights().getFlight().get(0).getAirlinedesignator();
            if (airlineDesignator != null)
                carrierCode = airlineDesignator;
        } else if (booking.getItems() != null && booking.getItems().getFlight() != null
                && !booking.getItems().getFlight().isEmpty()) {
            String airlineDesignator = booking.getItems().getFlight().get(0).getAirlinedesignator();
            if (airlineDesignator != null)
                carrierCode = airlineDesignator;
        }

        response.setApiOwner(carrierCode);
        response.setPnr(booking.getPnrref());
        response.setOrderId(booking.getBookingconfirmation() != null ? booking.getBookingconfirmation()
                : String.valueOf(booking.getBookingid()));
        response.setValidatingCarrier(carrierCode);
        response.setCurrency(booking.getCurrency() != null ? booking.getCurrency() : "USD");

        // Initialize total order price - will be summed from order items
        BigDecimal totalOrderPrice = BigDecimal.ZERO;
        response.setTotalOrderPrice(BigDecimal.ZERO); // Placeholder

        // Agent/Agency ID - Map if available, else keep generic or null if not in
        // booking
        // Assuming not available in standard Booking object, leaving commented or
        // setting optional
        // response.setAgentId("1416-AGT40148");
        // response.setAgencyId("1416");

        // 2. ODs (Flights)
        List<ChangeSeatRspDto.OD> ods = new ArrayList<>();
        List<ChangeSeatRspDto.PriceClass> pcl = new ArrayList<>();
        List<OrderRetrieveRspGo7Dto.Flight> flightList = booking.getFlights() != null
                && booking.getFlights().getFlight() != null ? booking.getFlights().getFlight()
                        : (booking.getItems() != null ? booking.getItems().getFlight() : null);

        Map<String, String> flightSegmentMap = new HashMap<>();

        if (flightList != null) {
            int segId = 1;
            int odId = 1;
            for (OrderRetrieveRspGo7Dto.Flight flight : flightList) {
                ChangeSeatRspDto.OD od = new ChangeSeatRspDto.OD();
                String segIdStr = "SEG" + segId;
                od.setSegmentId(segIdStr);
                od.setOdKey("OD" + odId);
                od.setOrigin(flight.getFromcode());
                od.setDestination(flight.getTocode());
                od.setOriginAirportName(flight.getFrom());
                od.setDestinationAirportName(flight.getTo());
                od.setDepartureDate(formatDate(flight.getFlightdate()));
                od.setArrivalDate(formatDate(flight.getFlightdate())); // Simplified, needs adjustment if overnight

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
                    // Often FareBasis or Class Name is in second part, not reliable in all Go7
                    // systems
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
                ChangeSeatRspDto.PriceClass pc = new ChangeSeatRspDto.PriceClass();
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

                List<ChangeSeatRspDto.PriceClass.Description> descs = new ArrayList<>();
                if (flight.getServices() != null) {
                    for (Map.Entry<String, Boolean> entry : flight.getServices().entrySet()) {
                        ChangeSeatRspDto.PriceClass.Description d = new ChangeSeatRspDto.PriceClass.Description();
                        d.setText(entry.getKey() + ": " + entry.getValue());
                        d.setOdKey("OD" + odId);
                        descs.add(d);
                    }
                }
                if (descs.isEmpty()) {
                    ChangeSeatRspDto.PriceClass.Description d = new ChangeSeatRspDto.PriceClass.Description();
                    d.setText("Standard Seat Selection");
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

        // 3. Booking References
        List<ChangeSeatRspDto.BookingReferences> refs = new ArrayList<>();
        ChangeSeatRspDto.BookingReferences ref = new ChangeSeatRspDto.BookingReferences();
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

        List<ChangeSeatRspDto.TicketDocInfoDTO> topTicketDocInfos = new ArrayList<>();
        List<ChangeSeatRspDto.EMDInfoDTO> topEmdInfos = new ArrayList<>();
        List<String> rawPaxIds = new ArrayList<>();
        List<ChangeSeatRspDto.PaxDetailDTO> paxList = new ArrayList<>();

        BigDecimal totalTax = BigDecimal.ZERO;
        if (flightList != null) {
            for (OrderRetrieveRspGo7Dto.Flight f : flightList) {
                if (f.getTotaltaxes() > 0)
                    totalTax = totalTax.add(BigDecimal.valueOf(f.getTotaltaxes()));
            }
        }
        BigDecimal bkTotal = BigDecimal.ZERO;
        if (booking.getBalanceInformation() != null && booking.getBalanceInformation().getPnrTotal() != null) {
            bkTotal = booking.getBalanceInformation().getPnrTotal();
        } else if (booking.getTotalprice() != null) {
            try {
                bkTotal = new BigDecimal(booking.getTotalprice());
            } catch (Exception e) {
            }
        }

        // CHECK IF PAYMENT WAS PROVIDED
        boolean isPaymentProvided = (requestDto != null && requestDto.getPaymentInformation() != null);

        // 4. Passenger Details & Seat Mapping Logic
        Map<String, List<String>> paxToRequestedSeats = new HashMap<>(); // PAX1 -> ["41D", "42D"]
        if (requestDto != null && requestDto.getOffers() != null) {
            for (com.airlines.GO7API.requestDto.ChangeSeatReqDto.Offer offer : requestDto.getOffers()) {
                if (offer.getOfferItems() != null) {
                    for (com.airlines.GO7API.requestDto.ChangeSeatReqDto.Offer.OfferItemDto item : offer
                            .getOfferItems()) {
                        if (item.getPaxRefs() != null && item.getRow() != null && item.getColumn() != null) {
                            String seatCoord = item.getRow().toString() + item.getColumn();
                            for (String paxRef : item.getPaxRefs()) {
                                // Keep as T1
                                String pid = paxRef;
                                paxToRequestedSeats.computeIfAbsent(pid, k -> new ArrayList<>()).add(seatCoord);
                            }
                        }
                    }
                }
            }
        }

        if (booking.getPassengers() != null && booking.getPassengers().getPassenger() != null) {
            int paxCounter = 1;
            List<ChangeSeatRspDto.PaxDetailDTO> adtList = new ArrayList<>();
            for (OrderRetrieveRspGo7Dto.Passenger p : booking.getPassengers().getPassenger()) {
                ChangeSeatRspDto.PaxDetailDTO pax = new ChangeSeatRspDto.PaxDetailDTO();
                
                String rawTitle = p.getPaxtitle() != null ? p.getPaxtitle().toUpperCase().replace(".", "") : "MR";
                boolean isInfantByTitle = rawTitle.contains("INF");

                String assignedPtc = mapPaxType(p.getPaxtype());
                if (isInfantByTitle) {
                    assignedPtc = "INF";
                }

                String pid;
                if ("INF".equals(assignedPtc)) {
                    if (!adtList.isEmpty()) {
                        ChangeSeatRspDto.PaxDetailDTO parent = adtList.get(adtList.size() - 1);
                        pid = parent.getPaxId() + ".1";
                        try {
                            parent.setInfantRef(pid);
                        } catch(Exception e) {}
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

                // Email
                if (p.getEmail() != null) {
                    ChangeSeatRspDto.PaxDetailDTO.EmailDTO email = new ChangeSeatRspDto.PaxDetailDTO.EmailDTO();
                    email.setEmailAddress(p.getEmail().toUpperCase()); // User sample shows uppercase
                    email.setLabel("OTH");
                    email.setType("OSI");
                    List<ChangeSeatRspDto.PaxDetailDTO.EmailDTO> emails = new ArrayList<>();
                    emails.add(email);
                    pax.setEmails(emails);
                }

                // Ticket Doc Info (Pax Level) - ONLY IF PAYMENT PROVIDED
                if (isPaymentProvided && p.getETickets() != null && p.getETickets().getFlight() != null) {
                    List<ChangeSeatRspDto.TicketDocInfoDTO> paxTicketDocs = new ArrayList<>();

                    ChangeSeatRspDto.TicketDocInfoDTO tdi = new ChangeSeatRspDto.TicketDocInfoDTO();
                    tdi.setPaxId(Arrays.asList(pid));
                    tdi.setValidatingCarrier(carrierCode);
                    tdi.setIssuingAirlineName(defaultIssuingAirline);
                    tdi.setIssuingPlace(defaultIssuingPlace);

                    List<ChangeSeatRspDto.TicketDocInfoDTO.TicketDocumentDTO> docs = new ArrayList<>();

                    java.util.Set<String> uniqueTickets = new java.util.LinkedHashSet<>();
                    for (OrderRetrieveRspGo7Dto.Passenger.ETicketFlight etf : p.getETickets().getFlight()) {
                        if (etf.getEticketnumber() != null && !etf.getEticketnumber().isEmpty()) {
                            uniqueTickets.add(etf.getEticketnumber().trim());
                        }
                    }

                    for (String ticketNbr : uniqueTickets) {
                        ChangeSeatRspDto.TicketDocInfoDTO.TicketDocumentDTO doc = new ChangeSeatRspDto.TicketDocInfoDTO.TicketDocumentDTO();
                        doc.setTicketDocNbr(ticketNbr);
                        doc.setType("T");
                        doc.setNumberOfBooklets(1);
                        doc.setDateOfIssue(formatCurrentDate()); // Approximated
                        doc.setTimeOfIssue("00:00");
                        doc.setTicketingLocation(tdi.getIssuingPlace());
                        doc.setReportingType("BSP");

                        // Coupons
                        List<ChangeSeatRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO> coupons = new ArrayList<>();
                        if (flightList != null) {
                            int couponNum = 1;
                            for (OrderRetrieveRspGo7Dto.Flight f : flightList) {
                                ChangeSeatRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO coupon = new ChangeSeatRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO();
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

                                ChangeSeatRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO cai = new ChangeSeatRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO();
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

                                List<ChangeSeatRspDto.TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO> caiList = new ArrayList<>();
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

                    // Add to top level list as well? User sample has it.
                    topTicketDocInfos.add(tdi);
                }

                // EMD Info - Link individual seats to specific passengers
                if (isPaymentProvided && changeSeatRsp.getAerocrs() != null
                        && changeSeatRsp.getAerocrs().getFlights() != null) {
                    List<ChangeSeatRspDto.EMDInfoDTO> paxEmds = new ArrayList<>();
                    List<String> assignedSeatsForPax = paxToRequestedSeats.getOrDefault(pid, new ArrayList<>());

                    for (String reqSeat : assignedSeatsForPax) {
                        // Find this seat in the Go7 response
                        ChangeSeatRspGo7Dto.Seat matchedSeat = null;
                        for (ChangeSeatRspGo7Dto.Flight f : changeSeatRsp.getAerocrs().getFlights()) {
                            if (f.getSeat() != null) {
                                for (ChangeSeatRspGo7Dto.Seat s : f.getSeat()) {
                                    if (reqSeat.equalsIgnoreCase(s.getSeat())) {
                                        matchedSeat = s;
                                        break;
                                    }
                                }
                            }
                            if (matchedSeat != null)
                                break;
                        }

                        if (matchedSeat != null) {
                            ChangeSeatRspDto.EMDInfoDTO emd = new ChangeSeatRspDto.EMDInfoDTO();
                            emd.setValidatingCarrier(carrierCode);
                            emd.setPaxId(Arrays.asList(pid));
                            emd.setIssuingAirlineName(defaultIssuingAirline);
                            emd.setIssuingPlace(defaultIssuingPlace);

                            List<ChangeSeatRspDto.EMDInfoDTO.TicketDocumentDTO> emdDocs = new ArrayList<>();
                            ChangeSeatRspDto.EMDInfoDTO.TicketDocumentDTO emdDoc = new ChangeSeatRspDto.EMDInfoDTO.TicketDocumentDTO();

                            // Generate EMD number using ticket number with counter suffix
                            String baseTkt = (p.getETickets() != null && p.getETickets().getFlight() != null
                                    && !p.getETickets().getFlight().isEmpty())
                                            ? p.getETickets().getFlight().get(0).getEticketnumber()
                                            : "EMD";

                            emdDoc.setTicketDocNbr(baseTkt + "-" + (topEmdInfos.size() + 1));
                            emdDoc.setConnectedDocNbr(null); // Explicitly don't set connectedDocNbr
                            emdDoc.setType("J");
                            emdDoc.setNumberOfBooklets(1);
                            emdDoc.setDateOfIssue(formatCurrentDate());
                            emdDoc.setTimeOfIssue("00:00");
                            emdDoc.setTicketingLocation(emd.getIssuingPlace());
                            emdDoc.setReportingType("BSP");

                            List<ChangeSeatRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO> emdCoupons = new ArrayList<>();
                            ChangeSeatRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO emdCoupon = new ChangeSeatRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO();
                            emdCoupon.setCouponNumber(1);
                            emdCoupon.setValidatingCarrier(carrierCode);
                            emdCoupon.setStatus("I");
                            emdCoupon.setServiceRefs(Arrays.asList(matchedSeat.getSeat()));

                            // Current Airline Info
                            if (flightList != null && !flightList.isEmpty()) {
                                OrderRetrieveRspGo7Dto.Flight cf = flightList.get(0);
                                List<ChangeSeatRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO> caiList = new ArrayList<>();
                                ChangeSeatRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO cai = new ChangeSeatRspDto.EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO();
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
                            }

                            emdCoupons.add(emdCoupon);
                            emdDoc.setCouponInfo(emdCoupons);
                            emdDocs.add(emdDoc);
                            emd.setTicketDocument(emdDocs);

                            paxEmds.add(emd);
                            topEmdInfos.add(emd);
                        }
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

        // 6. Order Items (The core part for Seats)
        List<ChangeSeatRspDto.OrderItemsDTO> orderItems = new ArrayList<>();

        // Derive primary PTC
        String primaryPtc = "ADT";
        if (booking.getPassengers() != null && booking.getPassengers().getPassenger() != null
                && !booking.getPassengers().getPassenger().isEmpty()) {
            primaryPtc = mapPaxType(booking.getPassengers().getPassenger().get(0).getPaxtype());
        }

        // 6a. Main Flight Item
        ChangeSeatRspDto.OrderItemsDTO airItem = new ChangeSeatRspDto.OrderItemsDTO();
        airItem.setOrderItemId(response.getOrderId() + "_AIR-1");
        airItem.setPtc(primaryPtc);

        // 1. Calculate Unified Seat Charges
        BigDecimal seatCharges = BigDecimal.ZERO;
        if (changeSeatRsp.getAerocrs() != null && changeSeatRsp.getAerocrs().getFlights() != null) {
            for (ChangeSeatRspGo7Dto.Flight f : changeSeatRsp.getAerocrs().getFlights()) {
                if (f.getSeat() != null) {
                    for (ChangeSeatRspGo7Dto.Seat s : f.getSeat()) {
                        if (s.getFare() != null) {
                            seatCharges = seatCharges.add(s.getFare());
                        }
                    }
                }
            }
        }

        // 2. Calculate Air Item Price by summing flight base + taxes (The "Original" price)
        BigDecimal sumFlightPrice = BigDecimal.ZERO;
        if (flightList != null) {
            for (OrderRetrieveRspGo7Dto.Flight f : flightList) {
                BigDecimal fBase = BigDecimal.ZERO;
                if (f.getInvpricingwithouttax() != null) {
                    try {
                        fBase = new BigDecimal(f.getInvpricingwithouttax());
                    } catch (Exception e) {}
                }
                sumFlightPrice = sumFlightPrice.add(fBase).add(BigDecimal.valueOf(f.getTotaltaxes()));
            }
        }

        BigDecimal airItemPrice = sumFlightPrice;
        
        // 3. Payment Amount for Seats (Strictly from request if provided)
        BigDecimal paymentSeatAmount = BigDecimal.ZERO;
        if (isPaymentProvided && requestDto.getPaymentInformation().getAmount() != null) {
            paymentSeatAmount = requestDto.getPaymentInformation().getAmount();
        } else {
            paymentSeatAmount = seatCharges;
        }

        // If seatCharges (from Go7) is 0, use payment amount as a fallback for the product price
        if (seatCharges.compareTo(BigDecimal.ZERO) <= 0) {
            seatCharges = paymentSeatAmount;
        }

        if (airItemPrice.compareTo(BigDecimal.ZERO) <= 0) {
            airItemPrice = bkTotal.subtract(seatCharges);
        }

        if (airItemPrice.compareTo(BigDecimal.ZERO) < 0) {
            airItemPrice = bkTotal; // fallback
        }

        airItemPrice = airItemPrice.setScale(2, java.math.RoundingMode.HALF_UP);

        BigDecimal airBaseFare = airItemPrice.subtract(totalTax);
        if (airBaseFare.compareTo(BigDecimal.ZERO) < 0)
            airBaseFare = airItemPrice;

        airItem.setBaseFare(new ChangeSeatRspDto.OrderItemsDTO.BaseFare(
                airBaseFare.setScale(2, java.math.RoundingMode.HALF_UP), response.getCurrency()));
        airItem.setTotalTax(new ChangeSeatRspDto.OrderItemsDTO.TotalTax(
                totalTax.setScale(2, java.math.RoundingMode.HALF_UP), response.getCurrency()));
        airItem.setTotalFare(new ChangeSeatRspDto.OrderItemsDTO.TotalFare(
                airItemPrice.setScale(2, java.math.RoundingMode.HALF_UP), response.getCurrency()));
        airItem.setTotalPrice(airItemPrice.setScale(2, java.math.RoundingMode.HALF_UP));
        airItem.setPassengerIds(rawPaxIds);

        // Taxes Breakdown
        List<ChangeSeatRspDto.OrderItemsDTO.Tax> airTaxes = new ArrayList<>();
        if (flightList != null && totalTax.compareTo(BigDecimal.ZERO) > 0) {
            // Calculate sum of granular taxes to determine scaling factor
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
        } else if (totalTax.compareTo(BigDecimal.ZERO) > 0) {
            // Fallback for missing breakdown
            ChangeSeatRspDto.OrderItemsDTO.Tax tx = new ChangeSeatRspDto.OrderItemsDTO.Tax();
            tx.setCode("TAX");
            tx.setAmount(totalTax.setScale(2, java.math.RoundingMode.HALF_UP));
            tx.setCurrency(response.getCurrency());
            tx.setDescription("Total Taxes");
            airTaxes.add(tx);
        }

        if (!airTaxes.isEmpty())
            airItem.setTaxes(airTaxes);

        List<ChangeSeatRspDto.Service> flightServices = new ArrayList<>();
        // Add segment confirmation service
        if (flightList != null && !flightList.isEmpty()) {
            for (String segKey : flightSegmentMap.values()) {
                // For each pax? Sample shows "SEG1_PAX2"
                for (String pid : rawPaxIds) {
                    ChangeSeatRspDto.Service srv = new ChangeSeatRspDto.Service();
                    srv.setServiceId(segKey + "_" + pid);
                    srv.setServiceStatus("CONFIRMED");
                    flightServices.add(srv);
                }
                break; // usually just one segment in these examples
            }
        }
        airItem.setServiceList(flightServices);
        orderItems.add(airItem);
        totalOrderPrice = totalOrderPrice.add(airItemPrice); // Add corrected air price
        if (changeSeatRsp.getAerocrs() != null && changeSeatRsp.getAerocrs().getFlights() != null) {
            int srvIdx = flightServices.size() + 1; // Start after flight services or 1
            for (ChangeSeatRspGo7Dto.Flight f : changeSeatRsp.getAerocrs().getFlights()) {
                if (f.getSeat() != null) {
                    String flightKey = (f.getFlightnumber() != null ? f.getFlightnumber() : "") + "_"
                            + (f.getFromcode() != null ? f.getFromcode() : "") + "_"
                            + (f.getTocode() != null ? f.getTocode() : "");
                    String segmentId = flightSegmentMap.getOrDefault(flightKey, "SEG1");

                    // 1. Calculate total number of seats to distribute fallback price
                    int totalSeats = f.getSeat().size();
                    BigDecimal distributedSeatPrice = BigDecimal.ZERO;
                    if (totalSeats > 0 && seatCharges.compareTo(BigDecimal.ZERO) > 0) {
                        distributedSeatPrice = seatCharges.divide(new BigDecimal(totalSeats), 2,
                                java.math.RoundingMode.HALF_UP);
                    }

                    int seatCounter = 0;
                    for (ChangeSeatRspGo7Dto.Seat s : f.getSeat()) {
                        ChangeSeatRspDto.OrderItemsDTO seatItem = new ChangeSeatRspDto.OrderItemsDTO();
                        seatItem.setOrderItemId(response.getOrderId() + "_SRV" + srvIdx++);

                        String assignedPaxId = "T1";
                        String seatPtc = "ADT";

                        if (booking.getPassengers() != null && booking.getPassengers().getPassenger() != null) {
                            int paxIdx = seatCounter % booking.getPassengers().getPassenger().size();
                            assignedPaxId = "T" + (paxIdx + 1);
                            seatPtc = mapPaxType(booking.getPassengers().getPassenger().get(paxIdx).getPaxtype());
                        }

                        seatCounter++;

                        seatItem.setPtc(seatPtc);
                        seatItem.setPassengerIds(Arrays.asList(assignedPaxId));

                        BigDecimal seatPrice = s.getFare() != null && s.getFare().compareTo(BigDecimal.ZERO) > 0
                                ? s.getFare()
                                : distributedSeatPrice; // Use divided charge if individual fare missing
                        String currency = s.getCurrency() != null ? s.getCurrency() : response.getCurrency();

                        seatItem.setBaseFare(new ChangeSeatRspDto.OrderItemsDTO.BaseFare(seatPrice, currency));
                        seatItem.setTotalTax(new ChangeSeatRspDto.OrderItemsDTO.TotalTax(BigDecimal.ZERO, currency));
                        seatItem.setTotalFare(new ChangeSeatRspDto.OrderItemsDTO.TotalFare(seatPrice, currency));
                        seatItem.setTotalPrice(seatPrice);

                        List<ChangeSeatRspDto.Service> seatServices = new ArrayList<>();
                        ChangeSeatRspDto.Service seatSrv = new ChangeSeatRspDto.Service();

                        seatSrv.setServiceId(segmentId + "_" + assignedPaxId);
                        seatSrv.setServiceStatus(s.isStatus() ? "CONFIRMED" : "PENDING");
                        seatSrv.setServiceCode("SEAT" + (s.getSeat() != null ? s.getSeat() : ""));
                        seatSrv.setServiceName("Specific Seat Request");
                        seatSrv.setSegmentId(segmentId);

                        // Parse Row/Col
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

                        List<ChangeSeatRspDto.Service.SeatCharacteristic> chars = new ArrayList<>();
                        chars.add(new ChangeSeatRspDto.Service.SeatCharacteristic("CH", "Chargeable Seat"));
                        chars.add(new ChangeSeatRspDto.Service.SeatCharacteristic("W", "Window seat"));
                        chars.add(new ChangeSeatRspDto.Service.SeatCharacteristic("FC",
                                "Front of cabin class/compartment"));
                        chars.add(new ChangeSeatRspDto.Service.SeatCharacteristic("N", "No smoking seat"));
                        seatSrv.setSeatCharacteristics(chars);

                        seatServices.add(seatSrv);
                        seatItem.setServiceList(seatServices);

                        orderItems.add(seatItem);
                        totalOrderPrice = totalOrderPrice.add(seatPrice);
                    }
                }
            }
        }
        response.setOrderItems(orderItems);
        response.setTotalOrderPrice(airItemPrice.add(seatCharges).setScale(2, java.math.RoundingMode.HALF_UP));

        // 5. Payments - Combined structure
        if (isPaymentProvided) {
            List<ChangeSeatRspDto.PaymentsDTO> payments = new ArrayList<>();
            String pType = (requestDto.getPaymentType() != null) ? requestDto.getPaymentType() : "CA";

            // Payment for Air Item (Existing Booking)
            if (airItemPrice.compareTo(BigDecimal.ZERO) > 0) {
                ChangeSeatRspDto.PaymentsDTO payAir = new ChangeSeatRspDto.PaymentsDTO();
                payAir.setType(pType);
                payAir.setStatusCode("SUCCESSFUL");
                payAir.setCurrency(response.getCurrency());
                payAir.setAmount(airItemPrice.setScale(2, java.math.RoundingMode.HALF_UP));
                payAir.setOrderItem(Arrays.asList(response.getOrderId() + "_AIR-1"));
                payments.add(payAir);
            }

            // Payment for Seat Item (Combined New Charges)
            if (paymentSeatAmount.compareTo(BigDecimal.ZERO) > 0 || seatCharges.compareTo(BigDecimal.ZERO) > 0) {
                ChangeSeatRspDto.PaymentsDTO paySeat = new ChangeSeatRspDto.PaymentsDTO();
                paySeat.setType(pType);
                paySeat.setStatusCode("SUCCESSFUL");
                paySeat.setCurrency(response.getCurrency());
                // Prioritize paymentSeatAmount (which has the request amount) for the payment block
                paySeat.setAmount(paymentSeatAmount.setScale(2, java.math.RoundingMode.HALF_UP));
                
                // If paymentSeatAmount is 0, fallback to seatCharges if available
                if (paymentSeatAmount.compareTo(BigDecimal.ZERO) <= 0 && seatCharges.compareTo(BigDecimal.ZERO) > 0) {
                    paySeat.setAmount(seatCharges.setScale(2, java.math.RoundingMode.HALF_UP));
                }
                // Link to first SRV item as before
                paySeat.setOrderItem(Arrays.asList(response.getOrderId() + "_SRV" + (flightServices.size() + 1)));
                payments.add(paySeat);
            }
            response.setPayments(payments);
        } else {
            response.setPayments(null);
        }

        response.setPriceClassList(pcl);

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

    private static void addScaledTaxItem(List<ChangeSeatRspDto.OrderItemsDTO.Tax> list, String code, double amount,
            BigDecimal scaleFactor, String currency, String description) {
        if (amount > 0) {
            ChangeSeatRspDto.OrderItemsDTO.Tax t = new ChangeSeatRspDto.OrderItemsDTO.Tax();
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
