package com.airlines.go7api.response;

import com.airlines.go7api.responsego7.common.*;
import com.airlines.go7api.responsedto.common.*;
import com.airlines.go7api.responsedto.OrderRetrieveRspDto;
import com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class OrderRetrieveResponse {

    private OrderRetrieveResponse() {
        throw new IllegalStateException("Utility class");
    }

    public static OrderRetrieveRspDto generateResponse(OrderRetrieveRspGo7Dto go7Response,
            java.util.Map<String, String> passengerSeats,
            java.util.Map<String, String> passengerServices, java.math.BigDecimal dbTotalOrderPrice) {
        if (go7Response == null || go7Response.getAerocrs() == null || go7Response.getAerocrs().getBooking() == null) {
            return new OrderRetrieveRspDto();
        }
        return new ResponseContext(go7Response, passengerSeats, passengerServices, dbTotalOrderPrice).buildResponse();
    }

    private static class ResponseContext {
        final OrderRetrieveRspGo7Dto go7Response;
        final Booking booking;
        final java.util.Map<String, String> passengerSeats;
        final java.util.Map<String, String> passengerServices;
        final java.math.BigDecimal dbTotalOrderPrice;

        final OrderRetrieveRspDto response;
        List<Flight> flightList;
        String carrierCode = "G7";
        String currency;

        final List<OD> ods = new ArrayList<>();
        final List<PriceClass> priceClasses = new ArrayList<>();
        final List<PaxDetailDTO> paxList = new ArrayList<>();
        final List<TicketDocInfoDTO> topLevelTicketDocs = new ArrayList<>();
        final List<EMDInfoDTO> topLevelEmdInfos = new ArrayList<>();
        final List<PaxDetailDTO> adtList = new ArrayList<>();
        final List<OrderItemsDTO> orderItems = new ArrayList<>();
        final List<PaymentsDTO> payments = new ArrayList<>();

        private static final String CABIN_ECONOMY = "Economy";
        private static final String DEFAULT_ISSUING_AIRLINE = "API Airways";
        private static final String SUFFIX_SEAT = "_SEAT";
        private static final String DEFAULT_TIME_OF_ISSUE = "00:00";

        BigDecimal distributedItemPrice = BigDecimal.ZERO;
        BigDecimal secondaryTotal = BigDecimal.ZERO;
        BigDecimal bookingTotal = BigDecimal.ZERO;
        int paxCounter = 1;
        int itemIdx = 1;

        ResponseContext(OrderRetrieveRspGo7Dto go7Response,
                java.util.Map<String, String> passengerSeats,
                java.util.Map<String, String> passengerServices,
                java.math.BigDecimal dbTotalOrderPrice) {
            this.go7Response = go7Response;
            this.booking = go7Response.getAerocrs().getBooking();
            this.passengerSeats = passengerSeats;
            this.passengerServices = passengerServices;
            this.dbTotalOrderPrice = dbTotalOrderPrice;
            this.response = new OrderRetrieveRspDto();
        }

        OrderRetrieveRspDto buildResponse() {
            determineFlightList();
            populateTopLevelFields();
            populateBookingReferences();
            populateOdsAndPriceClasses();
            populatePassengers();
            populateOrderItems();
            populatePayments();
            return response;
        }

        private void determineFlightList() {
            if (booking.getFlights() != null && booking.getFlights().getFlight() != null) {
                flightList = booking.getFlights().getFlight();
            } else if (booking.getItems() != null && booking.getItems().getFlight() != null) {
                flightList = booking.getItems().getFlight();
            }

            if (flightList != null && !flightList.isEmpty()) {
                String desig = flightList.get(0).getAirlinedesignator();
                if (desig != null) {
                    carrierCode = desig;
                }
            }
        }

        private void populateTopLevelFields() {
            response.setResponseId("P" + UUID.randomUUID().toString().substring(0, 15).toUpperCase());
            response.setOrderId(booking.getBookingconfirmation() != null ? booking.getBookingconfirmation()
                    : String.valueOf(booking.getBookingid()));
            response.setPnr(booking.getPnrref());
            response.setApiOwner(carrierCode);
            response.setTotalOrderPrice(BigDecimal.ZERO);
            response.setCurrency(booking.getCurrency() != null ? booking.getCurrency() : "USD");
            response.setPaymentTimeLimit(formatDateTime(booking.getPnrttl()));
            response.setStatusCode(go7Response.getAerocrs().isSuccess() ? "OPENED" : "REJECTED");
            response.setValidatingCarrier(carrierCode);
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

        private void populateOdsAndPriceClasses() {
            if (flightList == null) {
                return;
            }
            int segmentCounter = 1;
            int odCounter = 1;

            for (Flight flight : flightList) {
                processSingleFlight(flight, segmentCounter, odCounter);
                segmentCounter++;
                odCounter++;
            }
            response.setOds(ods);
            response.setPriceClassList(priceClasses);
        }

        private static class CabinAndClass {
            final String cabinCode;
            final String className;
            CabinAndClass(String cabinCode, String className) {
                this.cabinCode = cabinCode;
                this.className = className;
            }
        }

        private CabinAndClass parseCabinAndRbd(String rawClass) {
            if (rawClass.contains("/")) {
                return parseCabinWithSlash(rawClass);
            }
            return parseCabinWithoutSlash(rawClass);
        }

        private CabinAndClass parseCabinWithSlash(String rawClass) {
            String[] parts = rawClass.split("/");
            String cabinCode = CABIN_ECONOMY;
            String className = rawClass;

            if (parts.length > 0) {
                cabinCode = determineCabinFromCode(parts[0].trim().toUpperCase());
            }
            if (parts.length > 1) {
                className = parts[1].trim();
            }
            return new CabinAndClass(cabinCode, className);
        }

        private String determineCabinFromCode(String code) {
            if (code.equals("F") || code.equals("A") || code.equals("P")) {
                return "First";
            }
            if (code.equals("C") || code.equals("J") || code.equals("D") || code.equals("Z")
                    || code.equals("I")) {
                return "Business";
            }
            return CABIN_ECONOMY;
        }

        private CabinAndClass parseCabinWithoutSlash(String rawClass) {
            String cabinCode = CABIN_ECONOMY;
            String upper = rawClass.toUpperCase();
            if (upper.contains("BUSINESS")) {
                cabinCode = "Business";
            } else if (upper.contains("FIRST")) {
                cabinCode = "First";
            }
            return new CabinAndClass(cabinCode, rawClass);
        }

        private String calculateArrivalDate(Flight flight, String formattedDepDate) {
            String formattedArrDate = formattedDepDate;
            if (flight.getDepart() != null && flight.getArrive() != null) {
                try {
                    LocalTime depTime = LocalTime.parse(flight.getDepart());
                    LocalTime arrTime = LocalTime.parse(flight.getArrive());
                    if (arrTime.isBefore(depTime)) {
                        formattedArrDate = OrderMappingUtil.formatDate(OrderMappingUtil.adjustDateByDays(flight.getFlightdate(), 1));
                    }
                } catch (Exception e) {
                    // ignore parsing error
                }
            }
            return formattedArrDate;
        }

        private List<PriceClass.Description> generatePriceClassDescriptions(Flight flight) {
            List<PriceClass.Description> descriptions = new ArrayList<>();
            if (flight.getServices() != null) {
                for (java.util.Map.Entry<String, Boolean> entry : flight.getServices().entrySet()) {
                    PriceClass.Description d = new PriceClass.Description();
                    d.setText(entry.getKey() + ": " + entry.getValue());
                    descriptions.add(d);
                }
            }
            return descriptions;
        }

        private void processSingleFlight(Flight flight, int segmentCounter, int odCounter) {
            OD od = new OD();
            od.setSegmentId("S" + segmentCounter);
            od.setOdKey("OD" + odCounter);

            od.setOrigin(flight.getFromcode());
            od.setOriginAirportName(flight.getFrom());
            od.setDestination(flight.getTocode());
            od.setDestinationAirportName(flight.getTo());

            String formattedDepDate = OrderMappingUtil.formatDate(flight.getFlightdate());
            String formattedArrDate = calculateArrivalDate(flight, formattedDepDate);

            od.setDepartureDate(formattedDepDate);
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
            CabinAndClass cc = parseCabinAndRbd(rawClass);

            String priceClassId = "PC" + odCounter;

            od.setCabinType(cc.cabinCode);
            od.setPriceClassId(priceClassId);

            ods.add(od);

            PriceClass pc = new PriceClass();
            pc.setPriceClassId(priceClassId);
            pc.setClassName(cc.className);
            pc.setCabinTypeCode(cc.cabinCode.equals(CABIN_ECONOMY) ? "ECO" : cc.cabinCode);
            pc.setDescriptions(generatePriceClassDescriptions(flight));
            priceClasses.add(pc);
        }

        private void populatePassengers() {
            List<Passenger> go7PaxList = null;
            if (booking.getPassengers() != null && booking.getPassengers().getPassenger() != null) {
                go7PaxList = booking.getPassengers().getPassenger();
            }

            if (go7PaxList != null) {
                paxCounter = 1;
                for (Passenger go7Pax : go7PaxList) {
                    processPassenger(go7Pax);
                }
                if (!topLevelTicketDocs.isEmpty()) {
                    response.setTicketDocInfoList(topLevelTicketDocs);
                }
                if (!topLevelEmdInfos.isEmpty()) {
                    response.setEmdInfoList(topLevelEmdInfos);
                }
            }
            response.setPaxDetailList(paxList);
        }

        private void processPassenger(Passenger go7Pax) {
            PaxDetailDTO pax = new PaxDetailDTO();

            String rawTitle = go7Pax.getPaxtitle() != null ? go7Pax.getPaxtitle().toUpperCase().replace(".", "")
                    : "MR";
            boolean isInfantByTitle = rawTitle.contains("INF");

            String assignedPtc = OrderMappingUtil.mapPaxType(go7Pax.getPaxtype());
            if (isInfantByTitle) {
                assignedPtc = "INF";
            }

            String paxId = determinePaxId(assignedPtc, pax);

            pax.setPaxId(paxId);
            pax.setPtc(assignedPtc);
            pax.setGivenName(go7Pax.getFirstname() != null ? go7Pax.getFirstname().toUpperCase() : "");
            pax.setSurname(go7Pax.getLastname() != null ? go7Pax.getLastname().toUpperCase() : "");
            pax.setTitle(rawTitle);

            pax.setGender(determineGender(go7Pax));

            // Override for Child/Infant
            if ("CHD".equals(pax.getPtc()) || "CNN".equals(pax.getPtc())) {
                pax.setTitle("CHILD");
            } else if ("INF".equals(pax.getPtc())) {
                pax.setTitle("INFANT");
            }

            pax.setBirthDate(OrderMappingUtil.formatDate(go7Pax.getDob()));
            pax.setLanguage("English");

            populateContactDetails(pax, go7Pax);
            populatePassengerTicketDocs(pax, go7Pax, paxId);
            mapPassengerEmds(pax, go7Pax, paxId);

            paxList.add(pax);
        }

        private String determinePaxId(String assignedPtc, PaxDetailDTO pax) {
            if ("INF".equals(assignedPtc)) {
                if (!adtList.isEmpty()) {
                    PaxDetailDTO parent = adtList.get(adtList.size() - 1);
                    String paxId = parent.getPaxId() + ".1";
                    parent.setInfantRef(paxId);
                    return paxId;
                } else {
                    return "T" + paxCounter++ + ".1";
                }
            } else {
                String paxId = "T" + paxCounter++;
                if ("ADT".equals(assignedPtc)) {
                    adtList.add(pax);
                }
                return paxId;
            }
        }

        private String determineGender(Passenger go7Pax) {
            String gender = null;
            if (go7Pax.getGender() != null && !go7Pax.getGender().isEmpty()) {
                if (go7Pax.getGender().toUpperCase().startsWith("M")) {
                    gender = "Male";
                } else if (go7Pax.getGender().toUpperCase().startsWith("F")) {
                    gender = "Female";
                }
            }

            if (gender == null && go7Pax.getPaxtitle() != null) {
                String title = go7Pax.getPaxtitle().toUpperCase();
                if (title.contains("MR") || title.contains("MSTR") || title.contains("MISTR")) {
                    gender = "Male";
                } else if (title.contains("MS") || title.contains("MRS") || title.contains("MISS")) {
                    gender = "Female";
                }
            }

            return (gender != null) ? gender : "Male";
        }

        private void populateContactDetails(PaxDetailDTO pax, Passenger go7Pax) {
            if (go7Pax.getContact() != null) {
                PaxDetailDTO.PhoneDTO phone = new PaxDetailDTO.PhoneDTO();
                phone.setPhoneNumber(go7Pax.getContact());
                phone.setType("Operational");
                phone.setLabel("Mobile");
                pax.setPhones(new ArrayList<>(List.of(phone)));
            }

            if (go7Pax.getEmail() != null) {
                PaxDetailDTO.EmailDTO email = new PaxDetailDTO.EmailDTO();
                email.setEmailAddress(go7Pax.getEmail().toUpperCase());
                email.setType("Operational");
                pax.setEmails(new ArrayList<>(List.of(email)));
            }
        }

        private void populatePassengerTicketDocs(PaxDetailDTO pax, Passenger go7Pax, String paxId) {
            if (go7Pax.getETickets() == null || go7Pax.getETickets().getFlight() == null) {
                return;
            }
            List<TicketDocInfoDTO> paxTicketDocs = new ArrayList<>();
            java.util.Set<String> uniqueTickets = collectUniqueTickets(go7Pax);

            for (String ticketNbr : uniqueTickets) {
                TicketDocInfoDTO tdi = buildTicketDocInfo(ticketNbr, paxId);
                paxTicketDocs.add(tdi);
                topLevelTicketDocs.add(tdi);
            }
            pax.setTicketDocInfo(paxTicketDocs);
        }

        private java.util.Set<String> collectUniqueTickets(Passenger go7Pax) {
            java.util.Set<String> uniqueTickets = new java.util.LinkedHashSet<>();
            for (Passenger.ETicketFlight etf : go7Pax.getETickets().getFlight()) {
                if (etf.getEticketnumber() != null && !etf.getEticketnumber().isEmpty()) {
                    uniqueTickets.add(etf.getEticketnumber().trim());
                }
            }
            return uniqueTickets;
        }

        private TicketDocInfoDTO buildTicketDocInfo(String ticketNbr, String paxId) {
            TicketDocInfoDTO tdi = new TicketDocInfoDTO();
            String issuingDesig = DEFAULT_ISSUING_AIRLINE;
            String issuingPlace = "TLV";
            if (flightList != null && !flightList.isEmpty()) {
                Flight f0 = flightList.get(0);
                if (f0.getAirline() != null) {
                    issuingDesig = f0.getAirline();
                }
                if (f0.getFromcode() != null) {
                    issuingPlace = f0.getFromcode();
                }
            }
            tdi.setIssuingAirlineName(issuingDesig);
            tdi.setIssuingPlace(issuingPlace);
            tdi.setValidatingCarrier("G7");
            tdi.setPaxId(new ArrayList<>(List.of(paxId)));

            TicketDocInfoDTO.TicketDocumentDTO doc = new TicketDocInfoDTO.TicketDocumentDTO();
            doc.setTicketDocNbr(ticketNbr);
            doc.setType("T");
            doc.setNumberOfBooklets(1);
            doc.setDateOfIssue(OrderMappingUtil.formatDate(LocalDate.now().toString()));
            doc.setTimeOfIssue(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            doc.setTicketingLocation(issuingPlace);
            doc.setReportingType("BSP");
            doc.setCouponInfo(buildCoupons(paxId));

            tdi.setTicketDocument(new ArrayList<>(List.of(doc)));
            return tdi;
        }

        private List<TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO> buildCoupons(String paxId) {
            List<TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO> coupons = new ArrayList<>();
            if (flightList != null) {
                int couponNum = 1;
                for (Flight f : flightList) {
                    coupons.add(buildCoupon(f, couponNum++, paxId));
                }
            }
            return coupons;
        }

        private TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO buildCoupon(Flight f, int couponNum, String paxId) {
            TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO coupon = new TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO();
            coupon.setCouponNumber(couponNum);
            coupon.setCouponReference("FBA" + (couponNum + 1));

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
            coupon.setCurrentAirlineInfo(new ArrayList<>(List.of(buildCouponCurrentAirlineInfo(f))));
            coupon.setBaggageAllowances(buildCouponBaggageAllowances(f, paxId, couponNum + 1));

            return coupon;
        }

        private TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO buildCouponCurrentAirlineInfo(Flight f) {
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
            return cai;
        }

        private List<BaggageAllowance> buildCouponBaggageAllowances(Flight f, String paxId, int nextCouponNum) {
            List<BaggageAllowance> bags = new ArrayList<>();
            if (f.getServices() != null && f.getServices().containsKey("CheckedInBaggage")
                    && Boolean.TRUE.equals(f.getServices().get("CheckedInBaggage"))) {
                BaggageAllowance bag = new BaggageAllowance();
                bag.setBaggageAllowanceId("FBA" + nextCouponNum);
                bag.setPassengerId(paxId);
                bag.setCategory("Checked-In");
                bag.setName("Bag allowances");
                bags.add(bag);
            }
            return bags;
        }

        private void mapPassengerEmds(PaxDetailDTO pax, Passenger go7Pax, String paxId) {
            if (flightList == null) {
                return;
            }
            List<EMDInfoDTO> paxEmds = new ArrayList<>();

            java.util.LinkedHashMap<String, String> combinedEmdMap = new java.util.LinkedHashMap<>();
            if (passengerSeats != null) {
                for (java.util.Map.Entry<String, String> entry : passengerSeats.entrySet()) {
                    combinedEmdMap.put(entry.getKey() + SUFFIX_SEAT, entry.getValue());
                }
            }
            if (passengerServices != null) {
                for (java.util.Map.Entry<String, String> entry : passengerServices.entrySet()) {
                    combinedEmdMap.put(entry.getKey() + "_SRV", entry.getValue());
                }
            }

            if (!combinedEmdMap.isEmpty()) {
                mapDbEmds(go7Pax, paxId, combinedEmdMap, paxEmds);
            } else {
                mapFallbackEmds(go7Pax, paxId, paxEmds);
            }

            if (!paxEmds.isEmpty()) {
                pax.setEmdInfo(paxEmds);
            }
        }

        private void mapDbEmds(Passenger go7Pax, String paxId, java.util.LinkedHashMap<String, String> combinedEmdMap, List<EMDInfoDTO> paxEmds) {
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

                String issuingDesig = DEFAULT_ISSUING_AIRLINE;
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
                emdDoc.setDateOfIssue(OrderMappingUtil.formatDate(LocalDate.now().toString()));
                emdDoc.setTimeOfIssue(DEFAULT_TIME_OF_ISSUE);
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

                EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO cai = new EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO();
                cai.setDepartureAirportCode(f0.getFromcode());
                cai.setArrivalAirportCode(f0.getTocode());
                cai.setDepartureDate(OrderMappingUtil.formatDate(f0.getFlightdate()));
                cai.setDepartureTime(f0.getDepart());
                cai.setDepartureAirportName(f0.getFrom());
                cai.setArrivalAirportName(f0.getTo());
                cai.setFlightNumber(f0.getNumber());
                cai.setMarketingCarrierAirlineId(f0.getAirlinedesignator());
                emdCoupon.setCurrentAirlineInfo(new ArrayList<>(List.of(cai)));

                emdCoupons.add(emdCoupon);
                emdDoc.setCouponInfo(emdCoupons);
                emdDocs.add(emdDoc);
                emd.setTicketDocument(emdDocs);

                paxEmds.add(emd);
                topLevelEmdInfos.add(emd);
            }
        }

        private void mapFallbackEmds(Passenger go7Pax, String paxId, List<EMDInfoDTO> paxEmds) {
            mapSeatFallbackEmds(go7Pax, paxId, paxEmds);

            if (paxEmds.isEmpty() && go7Pax.getCheckin() != null && !go7Pax.getCheckin().isEmpty()) {
                mapCheckinFallbackEmds(go7Pax, paxId, paxEmds);
            }
        }

        private void mapSeatFallbackEmds(Passenger go7Pax, String paxId, List<EMDInfoDTO> paxEmds) {
            for (Flight f : flightList) {
                if (f.getSeat() != null) {
                    int seatIdxInFlight = 0;
                    for (Seat s : f.getSeat()) {
                        List<Passenger> originalList = booking.getPassengers().getPassenger();
                        int currentPaxIndex = originalList.indexOf(go7Pax);

                        if (seatIdxInFlight == currentPaxIndex) {
                            EMDInfoDTO emd = buildSeatEmd(f, s, go7Pax, paxId);
                            paxEmds.add(emd);
                            topLevelEmdInfos.add(emd);
                        }
                        seatIdxInFlight++;
                    }
                }
            }
        }

        private EMDInfoDTO buildSeatEmd(Flight f, Seat s, Passenger go7Pax, String paxId) {
            EMDInfoDTO emd = new EMDInfoDTO();
            emd.setValidatingCarrier("G7");
            emd.setPaxId(Arrays.asList(paxId));

            String issuingDesig = DEFAULT_ISSUING_AIRLINE;
            String issuingPlace = "TLV";
            if (f.getAirline() != null) {
                issuingDesig = f.getAirline();
            }
            if (f.getFromcode() != null) {
                issuingPlace = f.getFromcode();
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
            emdDoc.setDateOfIssue(OrderMappingUtil.formatDate(LocalDate.now().toString()));
            emdDoc.setTimeOfIssue(DEFAULT_TIME_OF_ISSUE);
            emdDoc.setTicketingLocation(issuingPlace);
            emdDoc.setReportingType("BSP");

            List<EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO> emdCoupons = new ArrayList<>();
            EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO emdCoupon = new EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO();
            emdCoupon.setCouponNumber(1);
            emdCoupon.setValidatingCarrier("G7");
            emdCoupon.setStatus("I");
            emdCoupon.setServiceRefs(Arrays.asList(s.getSeatNumber()));

            EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO cai = new EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO();
            cai.setDepartureAirportCode(f.getFromcode());
            cai.setArrivalAirportCode(f.getTocode());
            cai.setDepartureDate(OrderMappingUtil.formatDate(f.getFlightdate()));
            cai.setDepartureTime(f.getDepart());
            cai.setDepartureAirportName(f.getFrom());
            cai.setArrivalAirportName(f.getTo());
            cai.setFlightNumber(f.getNumber());
            cai.setMarketingCarrierAirlineId(f.getAirlinedesignator());
            emdCoupon.setCurrentAirlineInfo(new ArrayList<>(List.of(cai)));

            emdCoupons.add(emdCoupon);
            emdDoc.setCouponInfo(emdCoupons);
            emdDocs.add(emdDoc);
            emd.setTicketDocument(emdDocs);

            return emd;
        }

        private void mapCheckinFallbackEmds(Passenger go7Pax, String paxId, List<EMDInfoDTO> paxEmds) {
            for (Passenger.Checkin chk : go7Pax.getCheckin()) {
                if (chk.getSeat() != null && !chk.getSeat().isEmpty()) {
                    EMDInfoDTO emd = buildCheckinEmd(chk, go7Pax, paxId);
                    paxEmds.add(emd);
                    topLevelEmdInfos.add(emd);
                }
            }
        }

        private EMDInfoDTO buildCheckinEmd(Passenger.Checkin chk, Passenger go7Pax, String paxId) {
            EMDInfoDTO emd = new EMDInfoDTO();
            emd.setValidatingCarrier("G7");
            emd.setPaxId(Arrays.asList(paxId));

            Flight matchedFlight = findMatchedFlight(chk);
            populateEmdIssuingInfo(emd, matchedFlight);

            EMDInfoDTO.TicketDocumentDTO emdDoc = new EMDInfoDTO.TicketDocumentDTO();
            String baseTkt = getBaseTicketNumber(go7Pax);
            emdDoc.setTicketDocNbr(baseTkt + "-" + (topLevelEmdInfos.size() + 1));
            emdDoc.setType("J");
            emdDoc.setNumberOfBooklets(1);
            emdDoc.setDateOfIssue(OrderMappingUtil.formatDate(LocalDate.now().toString()));
            emdDoc.setTimeOfIssue(DEFAULT_TIME_OF_ISSUE);
            emdDoc.setTicketingLocation(emd.getIssuingPlace());
            emdDoc.setReportingType("BSP");
            emdDoc.setCouponInfo(buildCheckinCoupons(chk, matchedFlight));

            emd.setTicketDocument(new ArrayList<>(List.of(emdDoc)));
            return emd;
        }

        private Flight findMatchedFlight(Passenger.Checkin chk) {
            if (chk.getFlight() != null) {
                for (Flight f : flightList) {
                    if (f.getNumber() != null && chk.getFlight().contains(f.getNumber())) {
                        return f;
                    }
                }
            }
            if (!flightList.isEmpty()) {
                return flightList.get(0);
            }
            return null;
        }

        private void populateEmdIssuingInfo(EMDInfoDTO emd, Flight matchedFlight) {
            String issuingDesig = DEFAULT_ISSUING_AIRLINE;
            String issuingPlace = "TLV";
            if (matchedFlight != null) {
                if (matchedFlight.getAirline() != null) {
                    issuingDesig = matchedFlight.getAirline();
                }
                if (matchedFlight.getFromcode() != null) {
                    issuingPlace = matchedFlight.getFromcode();
                }
            }
            emd.setIssuingAirlineName(issuingDesig);
            emd.setIssuingPlace(issuingPlace);
        }

        private String getBaseTicketNumber(Passenger go7Pax) {
            if (go7Pax.getETickets() != null && go7Pax.getETickets().getFlight() != null
                    && !go7Pax.getETickets().getFlight().isEmpty()) {
                return go7Pax.getETickets().getFlight().get(0).getEticketnumber();
            }
            return "EMD";
        }

        private EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO buildEmdCouponCurrentAirlineInfo(Flight matchedFlight) {
            EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO cai = new EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO();
            cai.setDepartureAirportCode(matchedFlight.getFromcode());
            cai.setArrivalAirportCode(matchedFlight.getTocode());
            cai.setDepartureDate(OrderMappingUtil.formatDate(matchedFlight.getFlightdate()));
            cai.setDepartureTime(matchedFlight.getDepart());
            cai.setDepartureAirportName(matchedFlight.getFrom());
            cai.setArrivalAirportName(matchedFlight.getTo());
            cai.setFlightNumber(matchedFlight.getNumber());
            cai.setMarketingCarrierAirlineId(matchedFlight.getAirlinedesignator());
            return cai;
        }

        private List<EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO> buildCheckinCoupons(Passenger.Checkin chk, Flight matchedFlight) {
            EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO emdCoupon = new EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO();
            emdCoupon.setCouponNumber(1);
            emdCoupon.setValidatingCarrier("G7");
            emdCoupon.setStatus("I");
            emdCoupon.setServiceRefs(Arrays.asList(chk.getSeat()));

            if (matchedFlight != null) {
                emdCoupon.setCurrentAirlineInfo(new ArrayList<>(List.of(buildEmdCouponCurrentAirlineInfo(matchedFlight))));
            }
            return new ArrayList<>(List.of(emdCoupon));
        }

        private void populateOrderItems() {
            populateMainOrderItems();
            populateSecondaryServiceItems(buildCombinedSrvMap());
            response.setOrderItems(orderItems);
            aggregateTotalOrderPrice();
        }

        private void populateMainOrderItems() {
            List<String> adtRefs = new ArrayList<>();
            List<String> cnnRefs = new ArrayList<>();
            List<String> infRefs = new ArrayList<>();
            groupPassengersByPtc(adtRefs, cnnRefs, infRefs);

            currency = booking.getCurrency();
            if (currency == null) {
                currency = "USD";
            }

            BigDecimal airTotalForFallback = getAirTotalForFallback();

            createAndAddMainOrderItem("ADT", adtRefs, airTotalForFallback);
            createAndAddMainOrderItem("CHD", cnnRefs, airTotalForFallback);
            createAndAddMainOrderItem("INF", infRefs, airTotalForFallback);
        }

        private void groupPassengersByPtc(List<String> adtRefs, List<String> cnnRefs, List<String> infRefs) {
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
        }

        private BigDecimal getAirTotalForFallback() {
            if (booking.getBalanceInformation() != null && booking.getBalanceInformation().getPnrTotal() != null) {
                return booking.getBalanceInformation().getPnrTotal();
            } else if (booking.getTotalprice() != null) {
                try {
                    return new BigDecimal(String.valueOf(booking.getTotalprice()));
                } catch (Exception e) {
                    // Ignore total price parsing exception
                }
            }
            return null;
        }

        private void createAndAddMainOrderItem(String ptc, List<String> refs, BigDecimal fallbackPrice) {
            if (!refs.isEmpty()) {
                OrderItemsDTO item = createOrderItem(ptc, refs, fallbackPrice);
                orderItems.add(item);
            }
        }

        private java.util.LinkedHashMap<String, String> buildCombinedSrvMap() {
            java.util.LinkedHashMap<String, String> combinedSrvMap = new java.util.LinkedHashMap<>();
            if (passengerSeats != null) {
                for (java.util.Map.Entry<String, String> entry : passengerSeats.entrySet()) {
                    combinedSrvMap.put(entry.getKey() + SUFFIX_SEAT, entry.getValue());
                }
            }
            if (passengerServices != null) {
                for (java.util.Map.Entry<String, String> entry : passengerServices.entrySet()) {
                    combinedSrvMap.put(entry.getKey() + "_SRV", entry.getValue());
                }
            }
            return combinedSrvMap;
        }

        private void populateSecondaryServiceItems(java.util.LinkedHashMap<String, String> combinedSrvMap) {
            if (combinedSrvMap.isEmpty() || flightList == null || flightList.isEmpty()) {
                return;
            }
            calculateDistributedItemPrice(combinedSrvMap);

            for (java.util.Map.Entry<String, String> entry : combinedSrvMap.entrySet()) {
                orderItems.add(buildSecondaryOrderItem(entry));
            }
        }

        private static class SecondaryItemPriceInfo {
            final String itemCode;
            final BigDecimal itemPrice;
            SecondaryItemPriceInfo(String itemCode, BigDecimal itemPrice) {
                this.itemCode = itemCode;
                this.itemPrice = itemPrice;
            }
        }

        private SecondaryItemPriceInfo parseSecondaryItemPrice(String rawValue) {
            String itemCode = rawValue;
            BigDecimal itemPrice = distributedItemPrice;

            if (rawValue != null && rawValue.contains("|")) {
                String[] parts = rawValue.split("\\|");
                itemCode = parts[0];
                try {
                    BigDecimal storedPrice = new BigDecimal(parts[1]);
                    if (storedPrice.compareTo(BigDecimal.ZERO) > 0) {
                        itemPrice = storedPrice;
                    }
                } catch (Exception e) {
                    // Ignore stored price parsing exception
                }
            }
            return new SecondaryItemPriceInfo(itemCode, itemPrice);
        }

        private String findPassengerPtc(String paxId) {
            for (PaxDetailDTO p : paxList) {
                if (p.getPaxId().equals(paxId)) {
                    return p.getPtc();
                }
            }
            return "ADT";
        }

        private List<Service> buildServiceList(boolean isSeat, String itemCode, String assignedPaxId) {
            Service srv = new Service();
            if (isSeat) {
                populateSeatService(srv, itemCode, assignedPaxId);
            } else {
                populateAncillaryService(srv, itemCode);
            }
            return new ArrayList<>(List.of(srv));
        }

        private void populateSeatService(Service srv, String itemCode, String assignedPaxId) {
            srv.setServiceId("SEG1_" + assignedPaxId);
            srv.setServiceStatus("CONFIRMED");
            srv.setServiceCode("SEAT" + itemCode);
            srv.setServiceName("Specific Seat Request");
            srv.setSegmentId("SEG1");

            if (itemCode != null && !itemCode.isEmpty()) {
                String col = itemCode.substring(itemCode.length() - 1);
                String rowStr = itemCode.substring(0, itemCode.length() - 1);
                srv.setColumn(col);
                try {
                    srv.setRow(new java.math.BigInteger(rowStr));
                } catch (Exception e) {
                    // Ignore seat row number parsing exception
                }
            }
        }

        private void populateAncillaryService(Service srv, String itemCode) {
            srv.setServiceId(itemCode);
            srv.setServiceStatus("CONFIRMED");
            srv.setServiceCode("SRV");
            srv.setServiceName("Ancillary Service");
        }

        private OrderItemsDTO buildSecondaryOrderItem(java.util.Map.Entry<String, String> entry) {
            String rawKey = entry.getKey();
            boolean isSeat = rawKey.endsWith(SUFFIX_SEAT);
            String assignedPaxId = rawKey.substring(0, rawKey.lastIndexOf("_"));

            SecondaryItemPriceInfo priceInfo = parseSecondaryItemPrice(entry.getValue());

            OrderItemsDTO srvItem = new OrderItemsDTO();
            srvItem.setOrderItemId(response.getOrderId() + "_SRV" + itemIdx++);
            srvItem.setPtc(findPassengerPtc(assignedPaxId));
            srvItem.setPassengerIds(java.util.Arrays.asList(assignedPaxId));
            srvItem.setTotalPrice(priceInfo.itemPrice);
            srvItem.setTotalFare(new OrderItemsDTO.TotalFare(priceInfo.itemPrice, currency));
            srvItem.setBaseFare(new OrderItemsDTO.BaseFare(priceInfo.itemPrice, currency));
            srvItem.setServiceList(buildServiceList(isSeat, priceInfo.itemCode, assignedPaxId));

            return srvItem;
        }

        private void aggregateTotalOrderPrice() {
            BigDecimal aggregatedTotal = BigDecimal.ZERO;
            for (OrderItemsDTO o : orderItems) {
                if (o.getTotalPrice() != null) {
                    aggregatedTotal = aggregatedTotal.add(o.getTotalPrice());
                }
            }
            response.setTotalOrderPrice(aggregatedTotal.setScale(2, java.math.RoundingMode.HALF_UP));
        }

        private void calculateDistributedItemPrice(java.util.LinkedHashMap<String, String> combinedSrvMap) {
            BigDecimal totalAirItemsPrice = BigDecimal.ZERO;
            for (OrderItemsDTO o : orderItems) {
                if (o.getTotalPrice() != null) {
                    totalAirItemsPrice = totalAirItemsPrice.add(o.getTotalPrice());
                }
            }

            if (dbTotalOrderPrice != null) {
                bookingTotal = dbTotalOrderPrice;
            } else if (booking.getBalanceInformation() != null && booking.getBalanceInformation().getPnrTotal() != null) {
                bookingTotal = booking.getBalanceInformation().getPnrTotal();
            } else if (booking.getTotalprice() != null) {
                try {
                    bookingTotal = new BigDecimal(booking.getTotalprice());
                } catch (Exception e) {
                    // Ignore parsing exception, fallback to other total price fields
                }
            }

            secondaryTotal = bookingTotal.subtract(totalAirItemsPrice);
            if (secondaryTotal.compareTo(BigDecimal.ZERO) < 0) secondaryTotal = BigDecimal.ZERO;

            if (secondaryTotal.compareTo(BigDecimal.ZERO) > 0) {
                long unpaidCount = combinedSrvMap.values().stream().filter(v -> !v.contains("|") || new BigDecimal(v.split("\\|")[1]).compareTo(BigDecimal.ZERO) == 0).count();
                if (unpaidCount > 0) {
                    distributedItemPrice = secondaryTotal.divide(new BigDecimal(unpaidCount), 2, java.math.RoundingMode.HALF_UP);
                }
            }
        }

        private void populatePayments() {
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
        }

        private OrderItemsDTO createOrderItem(String ptc, List<String> paxIds, java.math.BigDecimal fallbackPrice) {
            OrderItemsDTO item = new OrderItemsDTO();
            item.setOrderItemId(response.getResponseId() + "_AIR-" + (itemIdx++));
            item.setPtc(ptc);

            String mainClassName = CABIN_ECONOMY;
            if (flightList != null && !flightList.isEmpty()) {
                Flight f = flightList.get(0);
                if (f.getFlightClass() != null) {
                    String[] parts = f.getFlightClass().split("/");
                    if (parts.length > 1) {
                        mainClassName = parts[1].trim();
                    } else {
                        mainClassName = f.getFlightClass();
                    }
                }
            }
            item.setClassName(mainClassName);
            item.setTimeStamp(
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMMyyyy HH:mm:ss", Locale.ENGLISH)));
            item.setPassengerIds(paxIds);

            int count = paxIds.size();
            BigDecimal[] fares = calculateBaseAndTaxFares(ptc, flightList, booking, fallbackPrice);
            BigDecimal totalBase = fares[0].multiply(new BigDecimal(count));
            BigDecimal totalTaxAmount = fares[1].multiply(new BigDecimal(count));
            BigDecimal totalPrice = totalBase.add(totalTaxAmount);

            List<OrderItemsDTO.Tax> taxList = new ArrayList<>();
            populateGranularTaxes(flightList, count, totalTaxAmount, currency, taxList);
            item.setTaxes(taxList);

            finalizeRetrieveOrderItem(item, totalBase, totalTaxAmount, totalPrice, currency);

            return item;
        }

        private static BigDecimal[] calculateBaseAndTaxFares(
                String ptc, List<Flight> flights, Booking booking, BigDecimal dbTotalOrderPrice) {
            BigDecimal unitBase = BigDecimal.ZERO;
            BigDecimal unitTax = BigDecimal.ZERO;
            int totalPaxCount = getRetrieveTotalPaxCount(booking);

            if (flights != null) {
                for (Flight f : flights) {
                    String fareStr = selectFareByPtc(ptc, f);
                    fareStr = applyFareFallback(fareStr, "ADT".equals(ptc), f);
                    unitBase = unitBase.add(parseFareSafely(fareStr));
                    unitTax = accumulateFlightTax(unitTax, f, totalPaxCount);
                }
            }

            unitBase = applyRetrieveBaseFareFallback(unitBase, flights, booking, dbTotalOrderPrice);
            return new BigDecimal[]{unitBase, unitTax};
        }

        private static String selectFareByPtc(String ptc, Flight f) {
            if ("ADT".equals(ptc)) {
                return f.getAdultfare();
            } else if ("CHD".equals(ptc) || "CNN".equals(ptc)) {
                return f.getChildfare();
            } else if ("INF".equals(ptc)) {
                return f.getInfantfare();
            }
            return null;
        }

        private static String applyFareFallback(String fareStr, boolean isAdt, Flight f) {
            if (fareStr != null && !fareStr.isEmpty() && !"0".equals(fareStr)) {
                return fareStr;
            }
            if (f.getInvpricingwithouttax() != null && !f.getInvpricingwithouttax().isEmpty()) {
                return f.getInvpricingwithouttax();
            }
            if (isAdt) {
                return f.getNetFare();
            }
            return fareStr;
        }

        private static BigDecimal parseFareSafely(String fareStr) {
            if (fareStr == null) {
                return BigDecimal.ZERO;
            }
            try {
                return new BigDecimal(fareStr);
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }

        private static BigDecimal accumulateFlightTax(BigDecimal unitTax, Flight f, int totalPaxCount) {
            BigDecimal flightTotalTax = BigDecimal.valueOf(f.getTotaltaxes());
            if (totalPaxCount > 0) {
                return unitTax.add(
                        flightTotalTax.divide(new BigDecimal(totalPaxCount), 2, java.math.RoundingMode.HALF_UP));
            }
            return unitTax.add(flightTotalTax);
        }

        private static int getRetrieveTotalPaxCount(Booking booking) {
            int totalPaxCount = booking.getAdults() + booking.getChild() + booking.getInfant();
            if (totalPaxCount == 0 && booking.getPassengers() != null
                    && booking.getPassengers().getPassenger() != null) {
                totalPaxCount = booking.getPassengers().getPassenger().size();
            }
            return totalPaxCount;
        }

        private static BigDecimal applyRetrieveBaseFareFallback(
                BigDecimal unitBase, List<Flight> flights, Booking booking, BigDecimal dbTotalOrderPrice) {
            if (unitBase.compareTo(BigDecimal.ZERO) != 0) {
                return unitBase;
            }
            try {
                BigDecimal totalBookingPrice = BigDecimal.ZERO;
                if (dbTotalOrderPrice != null) {
                    totalBookingPrice = dbTotalOrderPrice;
                } else if (booking.getBalanceInformation() != null && booking.getBalanceInformation().getPnrTotal() != null) {
                    totalBookingPrice = booking.getBalanceInformation().getPnrTotal();
                } else if (booking.getTotalprice() != null) {
                    totalBookingPrice = new BigDecimal(booking.getTotalprice());
                }

                BigDecimal totalBookingTax = BigDecimal.ZERO;
                if (flights != null) {
                    for (Flight f : flights) {
                        totalBookingTax = totalBookingTax.add(BigDecimal.valueOf(f.getTotaltaxes()));
                    }
                }

                BigDecimal totalBookingBaseFare = totalBookingPrice.subtract(totalBookingTax);
                if (totalBookingBaseFare.compareTo(BigDecimal.ZERO) > 0) {
                    int totalPaxCount = getRetrieveTotalPaxCount(booking);
                    if (totalPaxCount > 0) {
                        return totalBookingBaseFare.divide(new BigDecimal(totalPaxCount), 2,
                                java.math.RoundingMode.HALF_UP);
                    } else {
                        return totalBookingBaseFare;
                    }
                }
            } catch (Exception e) {
                // Ignore exception, fallback to base fare unit unitBase
            }
            return unitBase;
        }

        private static void populateGranularTaxes(
                List<Flight> flights, int count, BigDecimal totalTaxAmount, String currency, List<OrderItemsDTO.Tax> taxList) {
            if (flights == null) {
                return;
            }

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

        private static void finalizeRetrieveOrderItem(
                OrderItemsDTO item, BigDecimal totalBase, BigDecimal totalTaxAmount, BigDecimal totalPrice, String currency) {
            item.setTotalPrice(totalPrice.setScale(2, java.math.RoundingMode.HALF_UP));

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
                DateTimeFormatter inputFormatter;
                if (dateTimeStr.contains("/")) {
                    inputFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                } else {
                    inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                }
                LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, inputFormatter);
                return dateTime.format(DateTimeFormatter.ofPattern("ddMMMyyyy HH:mm:ss", Locale.ENGLISH));
            } catch (Exception e) {
                try {
                    return OrderMappingUtil.formatDate(dateTimeStr);
                } catch (Exception ex) {
                    return dateTimeStr;
                }
            }
        }
    }
}