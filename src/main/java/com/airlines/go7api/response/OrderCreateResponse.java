package com.airlines.go7api.response;

import com.airlines.go7api.requestdto.common.*;

import com.airlines.go7api.responsego7.common.*;

import com.airlines.go7api.responsedto.common.*;

import com.airlines.go7api.requestdto.OrderCreateReqDto;
import com.airlines.go7api.responsedto.OrderCreateRspDto;
import com.airlines.go7api.responsego7.OrderCreateRspGo7Dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;


public class OrderCreateResponse {

    private static final String CABIN_ECONOMY = "Economy";
    private static final String PTC_INFANT = "INFANT";

    private OrderCreateResponse() {
        throw new IllegalStateException("Utility class");
    }

    public static OrderCreateRspDto generateResponse(OrderCreateRspGo7Dto go7Response, OrderCreateReqDto requestDto) {
        if (go7Response == null || go7Response.getAerocrs() == null || go7Response.getAerocrs().getBooking() == null) {
            return new OrderCreateRspDto();
        }
        return new ResponseContext(go7Response, requestDto).buildResponse();
    }

    private static class ResponseContext {
        final OrderCreateRspGo7Dto go7Response;
        final OrderCreateReqDto requestDto;
        final OrderCreateRspDto response;
        final Booking booking;

        List<Flight> flightList;
        final List<OD> ods = new ArrayList<>();
        final List<PriceClass> priceClasses = new ArrayList<>();
        final List<PaxDetailDTO> paxList = new ArrayList<>();
        final List<TicketDocInfoDTO> topLevelTicketDocs = new ArrayList<>();

        final List<String> adtRefs = new ArrayList<>();
        final List<String> cnnRefs = new ArrayList<>();
        final List<String> infRefs = new ArrayList<>();

        int adtCounter = 1;
        int cnnCounter = 1;
        int infCounter = 1;
        final java.util.Set<PaxReqDto> mappedReqPaxes = new java.util.HashSet<>();
        String issuingAirlineName = "G7";
        String issuingPlace = "US";

        ResponseContext(OrderCreateRspGo7Dto go7Response, OrderCreateReqDto requestDto) {
            this.go7Response = go7Response;
            this.requestDto = requestDto;
            this.response = new OrderCreateRspDto();
            this.booking = go7Response.getAerocrs().getBooking();
        }

        OrderCreateRspDto buildResponse() {
            populateTopLevelFields();
            populateBookingReferences();
            determineFlightList();
            populateOdsAndPriceClasses();
            populatePassengers();
            populateOrderItems();
            return response;
        }

        private void populateTopLevelFields() {
            response.setResponseId("P" + UUID.randomUUID().toString().substring(0, 15).toUpperCase());
            response.setOrderId(String.valueOf(booking.getBookingid()));
            response.setPnr(booking.getPnrref());
            response.setApiOwner("G7");

            if (booking.getBalanceInformation() != null) {
                response.setTotalOrderPrice(booking.getBalanceInformation().getPnrTotal());
            } else {
                response.setTotalOrderPrice(parseTotalOrderPriceFallback());
            }

            response.setCurrency(booking.getCurrency() != null ? booking.getCurrency() : "USD");
            response.setPaymentTimeLimit(formatDateTimeForResponse(booking.getPnrttl()));
            response.setTicketingTimeLimit(formatDateTimeForResponse(booking.getPnrttl()));
            response.setStatusCode(go7Response.getAerocrs().isSuccess() ? "702" : "REJECTED");
            response.setValidatingCarrier("G7");
        }

        private BigDecimal parseTotalOrderPriceFallback() {
            if (booking.getTotalprice() != null) {
                try {
                    return new BigDecimal(booking.getTotalprice());
                } catch (NumberFormatException e) {
                    // Ignore parsing error, fallback to zero
                }
            }
            return BigDecimal.ZERO;
        }

        private void populateBookingReferences() {
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

        private void determineFlightList() {
            List<Flight> list1 = (booking.getFlights() != null) ? booking.getFlights().getFlight() : null;
            List<Flight> list2 = (booking.getItems() != null) ? booking.getItems().getFlight() : null;

            boolean list1HasPrice = hasPricingInfo(list1);
            boolean list2HasPrice = hasPricingInfo(list2);

            if (list1HasPrice) {
                flightList = list1;
            } else if (list2HasPrice) {
                flightList = list2;
            } else {
                flightList = determineFlightListFallback(list1, list2);
            }
        }

        private List<Flight> determineFlightListFallback(List<Flight> list1, List<Flight> list2) {
            if (isFlightListValid(list1)) {
                return list1;
            } else if (isFlightListValid(list2)) {
                return list2;
            }
            return (list1 != null) ? list1 : list2;
        }

        private void populateOdsAndPriceClasses() {
            if (flightList == null) {
                return;
            }
            int segmentCounter = 1;
            int odCounter = 1;

            for (Flight flight : flightList) {
                OD od = new OD();
                od.setSegmentId("SEG" + segmentCounter);
                od.setOdKey("OD" + odCounter);
                od.setOrigin(flight.getFromcode());
                od.setOriginAirportName(flight.getFrom());
                od.setDestination(flight.getTocode());
                od.setDestinationAirportName(flight.getTo());

                // Calculate dates
                String formattedDepDate = OrderMappingUtil.formatDate(flight.getFlightdate());
                String formattedArrDate = calculateArrivalDate(flight, formattedDepDate);

                od.setDepartureDate(formattedDepDate);
                od.setArrivalDate(formattedArrDate);
                od.setDepartureTime(flight.getDepart());
                od.setArrivalTime(flight.getArrive());
                od.setJourneyTime(OrderMappingUtil.calculateJourneyTime(
                        flight.getFlightdate(), flight.getDepart(), flight.getFlightdate(), flight.getArrive()));

                if (flight.getNumber() != null) {
                    od.setFlightNumber(flight.getNumber());
                }
                od.setEquipment(flight.getAircraftType());
                od.setMarketingCarrierCode(flight.getAirlinedesignator() != null ? flight.getAirlinedesignator() : "G7");
                od.setMarketingCarrierName(flight.getAirline());
                od.setArrivalTerminal(flight.getArrivalTerminal());
                od.setDepartureTerminal(flight.getDepartureTerminal());

                String priceClassId = "PC" + odCounter;
                String rawClass = flight.getFlightClass() != null ? flight.getFlightClass() : "";
                String cabinCode = determineCabinCode(rawClass);
                String className = determineClassName(rawClass);

                od.setCabinType(cabinCode);
                od.setRbdCode(null);
                od.setPriceClassId(priceClassId);
                od.setFareBasisCode(null);
                ods.add(od);

                // Populate PriceClass List
                PriceClass pc = new PriceClass();
                pc.setPriceClassId(priceClassId);
                pc.setClassName(className);
                pc.setCabinTypeCode(CABIN_ECONOMY.equals(cabinCode) ? "ECO" : cabinCode);

                List<PriceClass.Description> descriptions = new ArrayList<>();
                PriceClass.Description odDesc = new PriceClass.Description();
                odDesc.setOdKey("[OD" + odCounter + "]");
                descriptions.add(odDesc);

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
            response.setOds(ods);
            response.setPriceClassList(priceClasses);
        }

        private String calculateArrivalDate(Flight flight, String formattedDepDate) {
            if (flight.getDepart() != null && flight.getArrive() != null) {
                try {
                    LocalTime depTime = LocalTime.parse(flight.getDepart());
                    LocalTime arrTime = LocalTime.parse(flight.getArrive());
                    if (arrTime.isBefore(depTime)) {
                        return OrderMappingUtil.formatDate(OrderMappingUtil.adjustDateByDays(flight.getFlightdate(), 1));
                    }
                } catch (Exception e) {
                    // Ignore parsing error
                }
            }
            return formattedDepDate;
        }

        private String determineCabinCode(String rawClass) {
            if (rawClass.contains("/")) {
                String[] parts = rawClass.split("/");
                if (parts.length > 0) {
                    String code = parts[0].trim().toUpperCase();
                    if (code.startsWith("F") || code.startsWith("A") || code.startsWith("P")) {
                        return "First";
                    } else if (code.startsWith("C") || code.startsWith("J") || code.startsWith("D")
                            || code.startsWith("Z") || code.startsWith("I")) {
                        return "Business";
                    }
                }
            } else {
                if (rawClass.toUpperCase().contains("BUSINESS")) {
                    return "Business";
                } else if (rawClass.toUpperCase().contains("FIRST")) {
                    return "First";
                }
            }
            return CABIN_ECONOMY;
        }

        private String determineClassName(String rawClass) {
            if (rawClass.contains("/")) {
                String[] parts = rawClass.split("/");
                if (parts.length > 2) {
                    return parts[2].trim();
                } else if (parts.length > 1) {
                    return parts[1].trim();
                }
            }
            return rawClass;
        }

        private void populatePassengers() {
            List<Passenger> go7PaxList = null;
            if (booking.getPassengers() != null && booking.getPassengers().getPassenger() != null) {
                go7PaxList = booking.getPassengers().getPassenger();
            }

            if (go7PaxList != null) {
                mapGo7Passengers(go7PaxList);
            } else if (requestDto.getPassengers() != null) {
                mapRequestPassengersFallback();
            }
            response.setPaxDetailList(paxList);
        }

        private void initIssuingDetails() {
            if (flightList != null && !flightList.isEmpty()) {
                Flight f0 = flightList.get(0);
                if (f0.getAirline() != null) {
                    issuingAirlineName = f0.getAirline();
                }
                if (f0.getFromcode() != null) {
                    issuingPlace = f0.getFromcode();
                }
            }
        }

        private void mapGo7Passengers(List<Passenger> go7PaxList) {
            initIssuingDetails();

            // 1. Map ADT/CNN first
            for (Passenger go7Pax : go7PaxList) {
                PaxReqDto reqPax = findMatchingReqPassengerFirstPass(go7Pax);
                String ptc = getPassengerTypeCode(reqPax, go7Pax);

                if ("INF".equals(ptc) || PTC_INFANT.equalsIgnoreCase(ptc)) {
                    continue;
                }

                paxList.add(mapSingleAdultOrChild(go7Pax, reqPax, ptc));
            }
            if (!topLevelTicketDocs.isEmpty()) {
                response.setTicketDocInfoList(topLevelTicketDocs);
            }

            // 2. Map INF
            for (Passenger go7Pax : go7PaxList) {
                PaxReqDto reqPax = findMatchingReqPassengerSecondPass(go7Pax);
                String ptc = getPassengerTypeCode(reqPax, go7Pax);

                if ("INF".equals(ptc) || PTC_INFANT.equalsIgnoreCase(ptc)) {
                    mapSingleInfant(go7Pax, reqPax);
                }
            }
        }

        private PaxReqDto findMatchingReqPassengerFirstPass(Passenger go7Pax) {
            if (requestDto.getPassengers() != null) {
                for (PaxReqDto rp : requestDto.getPassengers()) {
                    if (!mappedReqPaxes.contains(rp) &&
                            rp.getFirstName() != null && rp.getLastName() != null &&
                            rp.getFirstName().equalsIgnoreCase(go7Pax.getFirstname()) &&
                            rp.getLastName().equalsIgnoreCase(go7Pax.getLastname())) {
                        if (!("INF".equalsIgnoreCase(rp.getPtc()) || PTC_INFANT.equalsIgnoreCase(rp.getPtc()))) {
                            mappedReqPaxes.add(rp);
                        }
                        return rp;
                    }
                }
            }
            return null;
        }

        private PaxReqDto findMatchingReqPassengerSecondPass(Passenger go7Pax) {
            if (requestDto.getPassengers() != null) {
                for (PaxReqDto rp : requestDto.getPassengers()) {
                    if (!mappedReqPaxes.contains(rp) &&
                            rp.getFirstName() != null && rp.getLastName() != null &&
                            rp.getFirstName().equalsIgnoreCase(go7Pax.getFirstname()) &&
                            rp.getLastName().equalsIgnoreCase(go7Pax.getLastname())) {
                        mappedReqPaxes.add(rp);
                        return rp;
                    }
                }
            }
            return null;
        }

        private String getPassengerTypeCode(PaxReqDto reqPax, Passenger go7Pax) {
            return (reqPax != null && reqPax.getPtc() != null) ? reqPax.getPtc()
                    : OrderMappingUtil.mapPaxType(go7Pax.getPaxtype());
        }

        private PaxDetailDTO mapSingleAdultOrChild(Passenger go7Pax, PaxReqDto reqPax, String ptc) {
            PaxDetailDTO pax = new PaxDetailDTO();
            String paxId = (reqPax != null && reqPax.getPaxId() != null && !reqPax.getPaxId().isEmpty())
                    ? reqPax.getPaxId()
                    : "T" + (adtCounter + cnnCounter - 1);

            if ("ADT".equals(ptc)) {
                adtCounter++;
            } else {
                cnnCounter++;
            }

            pax.setPaxId(paxId);
            pax.setPtc(ptc);
            pax.setGivenName(go7Pax.getFirstname() != null ? go7Pax.getFirstname().toUpperCase() : "");
            pax.setSurname(go7Pax.getLastname() != null ? go7Pax.getLastname().toUpperCase() : "");
            pax.setTitle(determineTitle(reqPax, go7Pax, ptc));
            pax.setGender(determineGender(reqPax, go7Pax));
            pax.setBirthDate(OrderMappingUtil.formatDate(go7Pax.getDob()));
            pax.setLanguage("English");

            populateContactDetails(pax, go7Pax);
            populatePassengerTicketDocs(pax, go7Pax, paxId, ptc);

            return pax;
        }

        private String determineTitle(PaxReqDto reqPax, Passenger go7Pax, String ptc) {
            if ("CHD".equals(ptc) || "CNN".equals(ptc)) {
                return "CHILD";
            }
            if (reqPax != null && reqPax.getTitle() != null) {
                return reqPax.getTitle();
            }
            if (go7Pax.getPaxtitle() != null) {
                return go7Pax.getPaxtitle().toUpperCase().replace(".", "");
            }
            return "MR";
        }

        private String determineGender(PaxReqDto reqPax, Passenger go7Pax) {
            if (reqPax != null && reqPax.getGender() != null) {
                return reqPax.getGender();
            }
            if (go7Pax.getGender() != null && !go7Pax.getGender().isEmpty()) {
                String gUpper = go7Pax.getGender().toUpperCase();
                if (gUpper.startsWith("M")) {
                    return "Male";
                } else if (gUpper.startsWith("F")) {
                    return "Female";
                }
            }
            if (go7Pax.getPaxtitle() != null) {
                String titleStr = go7Pax.getPaxtitle().toUpperCase();
                if (titleStr.contains("MR") || titleStr.contains("MSTR") || titleStr.contains("MISTR")) {
                    return "Male";
                } else if (titleStr.contains("MS") || titleStr.contains("MRS") || titleStr.contains("MISS")) {
                    return "Female";
                }
            }
            return "Male";
        }

        private void populateContactDetails(PaxDetailDTO pax, Passenger go7Pax) {
            if (go7Pax.getContact() != null) {
                PaxDetailDTO.PhoneDTO phone = new PaxDetailDTO.PhoneDTO();
                phone.setPhoneNumber(go7Pax.getContact());
                phone.setType("Operational");
                pax.setPhones(new ArrayList<>(List.of(phone)));
            }
            if (go7Pax.getEmail() != null) {
                PaxDetailDTO.EmailDTO email = new PaxDetailDTO.EmailDTO();
                email.setEmailAddress(go7Pax.getEmail().toUpperCase());
                email.setType("Operational");
                pax.setEmails(new ArrayList<>(List.of(email)));
            }
        }

        private void populatePassengerTicketDocs(PaxDetailDTO pax, Passenger go7Pax, String paxId, String ptc) {
            if (go7Pax.getETickets() == null || go7Pax.getETickets().getFlight() == null) {
                return;
            }
            java.util.Set<String> uniqueTickets = new java.util.LinkedHashSet<>();
            for (Passenger.ETicketFlight etf : go7Pax.getETickets().getFlight()) {
                if (etf.getEticketnumber() != null && !etf.getEticketnumber().isEmpty()) {
                    uniqueTickets.add(etf.getEticketnumber().trim());
                }
            }

            List<TicketDocInfoDTO> ticketDocInfoList = new ArrayList<>();
            for (String ticketNbr : uniqueTickets) {
                TicketDocInfoDTO tdi = new TicketDocInfoDTO();
                tdi.setValidatingCarrier("G7");
                tdi.setIssuingAirlineName(issuingAirlineName);
                tdi.setIssuingPlace(issuingPlace);
                tdi.setPaxId(new ArrayList<>(List.of(paxId)));
                tdi.setPtc(ptc);

                List<TicketDocInfoDTO.TicketDocumentDTO> docs = new ArrayList<>();
                TicketDocInfoDTO.TicketDocumentDTO doc = new TicketDocInfoDTO.TicketDocumentDTO();
                doc.setTicketDocNbr(ticketNbr);
                doc.setType("T");
                doc.setNumberOfBooklets(1);
                doc.setDateOfIssue(OrderMappingUtil.formatDate(LocalDate.now().toString()));
                doc.setTimeOfIssue(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
                doc.setTicketingLocation(issuingPlace);
                doc.setReportingType("BSP");

                if (flightList != null) {
                    doc.setCouponInfo(buildCoupons());
                }

                docs.add(doc);
                tdi.setTicketDocument(docs);
                ticketDocInfoList.add(tdi);
                topLevelTicketDocs.add(tdi);
            }

            if (!ticketDocInfoList.isEmpty()) {
                pax.setTicketDocInfo(ticketDocInfoList);
            }
        }

        private List<TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO> buildCoupons() {
            List<TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO> coupons = new ArrayList<>();
            int couponNum = 1;
            for (Flight f : flightList) {
                TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO coupon = new TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO();
                coupon.setCouponNumber(couponNum++);
                coupon.setCouponReference("FBA" + couponNum);

                String fClass = f.getFlightClass() != null ? f.getFlightClass() : "";
                String fRbd = "";
                if (fClass.contains("/")) {
                    String[] parts = fClass.split("/");
                    if (parts.length > 0) {
                        fRbd = parts[0].trim().toUpperCase();
                    }
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
                cai.setEquipmentAircraftCode(f.getAircraftTypeIataCode());

                coupon.setCurrentAirlineInfo(new ArrayList<>(List.of(cai)));
                coupons.add(coupon);
            }
            return coupons;
        }

        private void mapSingleInfant(Passenger go7Pax, PaxReqDto reqPax) {
            String defaultParentId = "T" + infCounter;
            String paxId = (reqPax != null && reqPax.getPaxId() != null && !reqPax.getPaxId().isEmpty())
                    ? reqPax.getPaxId()
                    : (defaultParentId + ".1");
            String parentId = paxId.contains(".") ? paxId.substring(0, paxId.indexOf(".")) : defaultParentId;
            infCounter++;

            PaxDetailDTO inf = new PaxDetailDTO();
            inf.setPaxId(paxId);
            inf.setPtc("INF");
            inf.setGivenName(go7Pax.getFirstname() != null ? go7Pax.getFirstname().toUpperCase() : "");
            inf.setSurname(go7Pax.getLastname() != null ? go7Pax.getLastname().toUpperCase() : "");
            inf.setTitle(PTC_INFANT);
            inf.setGender(determineGender(reqPax, go7Pax));
            inf.setBirthDate(OrderMappingUtil.formatDate(go7Pax.getDob()));
            paxList.add(inf);

            // Update parent's infantRef
            for (PaxDetailDTO parent : paxList) {
                if (parent.getPaxId().equals(parentId)) {
                    parent.setInfantRef(paxId);
                    break;
                }
            }
        }

        private void mapRequestPassengersFallback() {
            for (PaxReqDto reqPax : requestDto.getPassengers()) {
                PaxDetailDTO pax = new PaxDetailDTO();
                pax.setPaxId(reqPax.getPaxId());
                pax.setPtc(reqPax.getPtc());
                // ... map others
                paxList.add(pax);
            }
        }

        private void populateOrderItems() {
            List<OrderItemsDTO> orderItems = new ArrayList<>();
            int itemIdx = 1;

            for (PaxDetailDTO p : paxList) {
                String pid = p.getPaxId();
                if ("ADT".equals(p.getPtc())) {
                    adtRefs.add(pid);
                } else if ("CNN".equals(p.getPtc()) || "CHD".equals(p.getPtc())) {
                    cnnRefs.add(pid);
                } else if ("INF".equals(p.getPtc()) || PTC_INFANT.equalsIgnoreCase(p.getPtc())) {
                    infRefs.add(pid);
                }
            }

            String currency = booking.getCurrency();
            if (currency == null) {
                currency = "USD";
            }

            // ADT Item
            if (!adtRefs.isEmpty()) {
                orderItems.add(createOrderItem(itemIdx++, "ADT", adtRefs, currency));
            }

            // CNN Item
            if (!cnnRefs.isEmpty()) {
                orderItems.add(createOrderItem(itemIdx++, "CNN", cnnRefs, currency));
            }

            // INF Item
            if (!infRefs.isEmpty()) {
                orderItems.add(createOrderItem(itemIdx, "INF", infRefs, currency));
            }

            response.setOrderItems(orderItems);
        }

        private String formatDateTimeForResponse(String dateTimeStr) {
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

        private OrderItemsDTO createOrderItem(int itemIndex, String ptc, List<String> paxIds, String currency) {
            OrderItemsDTO item = new OrderItemsDTO();
            item.setOrderItemId(response.getResponseId() + "-" + itemIndex);
            item.setPtc(ptc);
            item.setPassengerIds(paxIds);
            item.setTimeStamp(
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMMyyyy HH:mm:ss", Locale.ENGLISH)));

            item.setClassName(extractClassName(flightList));
            applyPricingAndTaxes(item, ptc, paxIds.size(), flightList, currency, booking);

            return item;
        }

        private static String extractClassName(List<Flight> flights) {
            String className = CABIN_ECONOMY;
            if (flights != null && !flights.isEmpty()) {
                Flight f = flights.get(0);
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
            return className;
        }

        private static void applyPricingAndTaxes(OrderItemsDTO item, String ptc, int count,
                List<Flight> flights, String currency,
                Booking booking) {
            BigDecimal unitBase = BigDecimal.ZERO;
            BigDecimal unitTax = BigDecimal.ZERO;
            java.util.Map<String, OrderItemsDTO.Tax> taxMap = new java.util.HashMap<>();

            if (flights != null) {
                for (Flight f : flights) {
                    BigDecimal currentBase = determineBaseFare(f, ptc, booking);
                    BigDecimal currentTax = determineTax(f, booking);

                    unitBase = unitBase.add(currentBase);
                    unitTax = unitTax.add(currentTax);

                    processGranularTaxes(f, currentTax, taxMap, currency);
                }
            }

            BigDecimal totalBase = unitBase.multiply(new BigDecimal(count));
            BigDecimal totalTaxAmount = unitTax.multiply(new BigDecimal(count));
            BigDecimal totalPrice = totalBase.add(totalTaxAmount);

            BigDecimal[] fallbackValues = applyGlobalFallback(totalPrice, totalBase, count, booking);
            totalPrice = fallbackValues[0];
            totalBase = fallbackValues[1];

            finalizeOrderItemPricing(item, totalBase, totalTaxAmount, totalPrice, count, taxMap, currency);
        }

        private static BigDecimal determineBaseFare(Flight f, String ptc, Booking booking) {
            String baseStr = null;
            if ("ADT".equals(ptc))
                baseStr = f.getAdultfare();
            else if ("CNN".equals(ptc) || "CHD".equals(ptc))
                baseStr = f.getChildfare();
            else if ("INF".equals(ptc))
                baseStr = f.getInfantfare();

            if (baseStr != null && !baseStr.isEmpty() && !"0".equals(baseStr)) {
                return safeDecimal(baseStr);
            }
            
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
            if (totalPaxCount == 0 && booking.getPassengers() != null && booking.getPassengers().getPassenger() != null) {
                totalPaxCount = booking.getPassengers().getPassenger().size();
            }
            if (totalPaxCount > 0) {
                return netFare.divide(new BigDecimal(totalPaxCount), 2, RoundingMode.HALF_UP);
            }
            return BigDecimal.ZERO;
        }

        private static BigDecimal determineTax(Flight f, Booking booking) {
            String taxStr = f.getTax();
            if (taxStr != null && !taxStr.isEmpty()) {
                return safeDecimal(taxStr);
            }

            BigDecimal totalTax = safeDecimal(f.getTotaltax());
            if (totalTax.compareTo(BigDecimal.ZERO) == 0) {
                totalTax = BigDecimal.valueOf(f.getTotaltaxes());
            }

            int totalPaxCount = booking.getAdults() + booking.getChild() + booking.getInfant();
            if (totalPaxCount == 0 && booking.getPassengers() != null && booking.getPassengers().getPassenger() != null) {
                totalPaxCount = booking.getPassengers().getPassenger().size();
            }

            if (totalPaxCount > 0) {
                return totalTax.divide(new BigDecimal(totalPaxCount), 2, RoundingMode.HALF_UP);
            }
            return BigDecimal.ZERO;
        }

        private static void processGranularTaxes(Flight f, BigDecimal currentTax, java.util.Map<String, OrderItemsDTO.Tax> taxMap, String currency) {
            if (currentTax.compareTo(BigDecimal.ZERO) <= 0 || f.getTaxes() == null) return;
            
            Taxes tObj = f.getTaxes();
            BigDecimal totalGranular = BigDecimal.ZERO;
            totalGranular = totalGranular.add(BigDecimal.valueOf(tObj.getFuel()));
            totalGranular = totalGranular.add(BigDecimal.valueOf(tObj.getSecurity()));
            totalGranular = totalGranular.add(BigDecimal.valueOf(tObj.getGroundHandling()));
            totalGranular = totalGranular.add(BigDecimal.valueOf(tObj.getTax1()));
            totalGranular = totalGranular.add(BigDecimal.valueOf(tObj.getTax4()));

            BigDecimal scaleFactor = BigDecimal.ONE;
            if (totalGranular.compareTo(BigDecimal.ZERO) > 0) {
                scaleFactor = currentTax.divide(totalGranular, 6, RoundingMode.HALF_UP);
            }

            addScaledTaxItem(taxMap, "YQ", tObj.getFuel(), scaleFactor, currency, "Fuel Surcharge");
            addScaledTaxItem(taxMap, "I2", tObj.getSecurity(), scaleFactor, currency, "Security Tax");
            addScaledTaxItem(taxMap, "GH", tObj.getGroundHandling(), scaleFactor, currency, "Ground Handling");
            addScaledTaxItem(taxMap, "IN", tObj.getTax1(), scaleFactor, currency, "Infrastructure Tax");
            addScaledTaxItem(taxMap, "OT", tObj.getTax4(), scaleFactor, currency, "Other Tax");
        }

        private static BigDecimal[] applyGlobalFallback(BigDecimal totalPrice, BigDecimal totalBase, int count, Booking booking) {
            if (totalPrice.compareTo(BigDecimal.ZERO) == 0 && booking != null) {
                BigDecimal bkTotal = BigDecimal.ZERO;
                if (booking.getBalanceInformation() != null && booking.getBalanceInformation().getPnrTotal() != null) {
                    bkTotal = booking.getBalanceInformation().getPnrTotal();
                } else if (booking.getTotalprice() != null) {
                    bkTotal = safeDecimal(booking.getTotalprice());
                }

                if (bkTotal.compareTo(BigDecimal.ZERO) > 0) {
                    int totalPaxCount = booking.getAdults() + booking.getChild() + booking.getInfant();
                    if (totalPaxCount == 0 && booking.getPassengers() != null && booking.getPassengers().getPassenger() != null) {
                        totalPaxCount = booking.getPassengers().getPassenger().size();
                    }

                    if (totalPaxCount > 0) {
                        BigDecimal newTotal = bkTotal.multiply(new BigDecimal(count)).divide(new BigDecimal(totalPaxCount), 2, RoundingMode.HALF_UP);
                        return new BigDecimal[]{newTotal, newTotal};
                    }
                }
            }
            return new BigDecimal[]{totalPrice, totalBase};
        }

        private static boolean isFlightListValid(List<Flight> list) {
            if (list == null || list.isEmpty()) {
                return false;
            }
            Flight f = list.get(0);
            return f.getFromcode() != null || f.getTocode() != null || f.getAirline() != null;
        }

        private static boolean hasPricingInfo(List<Flight> list) {
            if (list == null || list.isEmpty()) {
                return false;
            }
            Flight f = list.get(0);
            // Check for ANY pricing signal
            return (f.getAdultfare() != null && !"0".equals(f.getAdultfare())) ||
                    (f.getNetFare() != null && !"0".equals(f.getNetFare())) ||
                    (f.getInvpricing() != null && !"0".equals(f.getInvpricing()));
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

        private static void finalizeOrderItemPricing(OrderItemsDTO item, BigDecimal totalBase, BigDecimal totalTaxAmount, BigDecimal totalPrice, int count, java.util.Map<String, OrderItemsDTO.Tax> taxMap, String currency) {
            item.setTotalPrice(totalPrice.setScale(2, RoundingMode.HALF_UP));

            OrderItemsDTO.TotalFare tf = new OrderItemsDTO.TotalFare();
            tf.setAmount(totalPrice.setScale(2, RoundingMode.HALF_UP));
            tf.setCurrency(currency);
            item.setTotalFare(tf);

            OrderItemsDTO.BaseFare bf = new OrderItemsDTO.BaseFare();
            bf.setAmount(totalBase.setScale(2, RoundingMode.HALF_UP));
            bf.setCurrency(currency);
            item.setBaseFare(bf);

            OrderItemsDTO.TotalTax tt = new OrderItemsDTO.TotalTax();
            tt.setAmount(totalTaxAmount.setScale(2, RoundingMode.HALF_UP));
            tt.setCurrency(currency);
            item.setTotalTax(tt);

            List<OrderItemsDTO.Tax> taxes = new ArrayList<>();
            if (!taxMap.isEmpty()) {
                for (OrderItemsDTO.Tax t : taxMap.values()) {
                    t.setAmount(t.getAmount().multiply(new BigDecimal(count)).setScale(2, RoundingMode.HALF_UP));
                    taxes.add(t);
                }
            } else if (totalTaxAmount.compareTo(BigDecimal.ZERO) > 0) {
                OrderItemsDTO.Tax tx = new OrderItemsDTO.Tax();
                tx.setCode("OT");
                tx.setAmount(totalTaxAmount.setScale(2, RoundingMode.HALF_UP));
                tx.setCurrency(currency);
                tx.setDescription("Total Taxes");
                taxes.add(tx);
            }
            item.setTaxes(taxes);
        }

        private static void addScaledTaxItem(java.util.Map<String, OrderItemsDTO.Tax> map, String code,
                double amount,
                BigDecimal scaleFactor, String currency, String description) {
            if (amount > 0) {
                OrderItemsDTO.Tax t = new OrderItemsDTO.Tax();
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

    



    

    



















    






}