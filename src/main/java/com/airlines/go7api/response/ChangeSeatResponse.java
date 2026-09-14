package com.airlines.go7api.response;

import com.airlines.go7api.responsego7.common.*;
import com.airlines.go7api.responsedto.common.*;
import com.airlines.go7api.responsedto.ChangeSeatRspDto;
import com.airlines.go7api.responsego7.ChangeSeatRspGo7Dto;
import com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;

public class ChangeSeatResponse {

    private static final Set<String> FIRST_CLASS_CODES = new HashSet<>(Arrays.asList("F", "A", "P"));
    private static final Set<String> BUSINESS_CLASS_CODES = new HashSet<>(Arrays.asList("C", "J", "D", "Z", "I"));
    private static final String ECONOMY_CLASS = "Economy";
    private static final String STATUS_CONFIRMED = "CONFIRMED";

    private ChangeSeatResponse() {
        throw new IllegalStateException("Utility class");
    }

    public static ChangeSeatRspDto generateResponse(ChangeSeatRspGo7Dto changeSeatRsp,
            OrderRetrieveRspGo7Dto bookingRsp, com.airlines.go7api.requestdto.ChangeSeatReqDto requestDto,
            Map<String, BigDecimal> actualPrices,
            Map<String, String> passengerSeatsMap,
            Map<String, String> passengerServicesMap) {
        if (bookingRsp == null || bookingRsp.getAerocrs() == null || bookingRsp.getAerocrs().getBooking() == null) {
            return new ChangeSeatRspDto();
        }
        return new ResponseContext(changeSeatRsp, bookingRsp, requestDto, actualPrices, passengerSeatsMap, passengerServicesMap).buildResponse();
    }

    private static class ResponseContext {
        final ChangeSeatRspGo7Dto changeSeatRsp;
        final OrderRetrieveRspGo7Dto bookingRsp;
        final com.airlines.go7api.requestdto.ChangeSeatReqDto requestDto;
        final Map<String, BigDecimal> actualPrices;
        final Map<String, String> passengerSeatsMap;
        final Map<String, String> passengerServicesMap;

        final Booking booking;
        final ChangeSeatRspDto response;
        final List<Flight> flightList;
        final Map<String, String> flightSegmentMap = new HashMap<>();
        final Map<String, List<String>> paxToRequestedSeats = new HashMap<>();
        final List<String> rawPaxIds = new ArrayList<>();
        final List<TicketDocInfoDTO> topTicketDocInfos = new ArrayList<>();
        final List<EMDInfoDTO> topEmdInfos = new ArrayList<>();
        final List<PaxDetailDTO> paxList = new ArrayList<>();
        final List<PriceClass> priceClassList = new ArrayList<>();
        final List<OD> ods = new ArrayList<>();
        final List<OrderItemsDTO> orderItems = new ArrayList<>();
        final List<Service> flightServices = new ArrayList<>();
        final Set<String> processedSeatKeys = new HashSet<>();

        String carrierCode;
        BigDecimal totalTax = BigDecimal.ZERO;
        boolean isPaymentProvided;
        BigDecimal seatCharges = BigDecimal.ZERO;
        BigDecimal airItemPrice = BigDecimal.ZERO;
        BigDecimal pnrTotal = BigDecimal.ZERO;
        BigDecimal paymentSeatAmount = BigDecimal.ZERO;
        BigDecimal totalOrderPrice = BigDecimal.ZERO;
        int srvIdx = 1;
        int paxCounter = 1;
        final List<PaxDetailDTO> adtList = new ArrayList<>();

        ResponseContext(ChangeSeatRspGo7Dto changeSeatRsp,
                OrderRetrieveRspGo7Dto bookingRsp,
                com.airlines.go7api.requestdto.ChangeSeatReqDto requestDto,
                Map<String, BigDecimal> actualPrices,
                Map<String, String> passengerSeatsMap,
                Map<String, String> passengerServicesMap) {
            this.changeSeatRsp = changeSeatRsp;
            this.bookingRsp = bookingRsp;
            this.requestDto = requestDto;
            this.actualPrices = actualPrices;
            this.passengerSeatsMap = passengerSeatsMap;
            this.passengerServicesMap = passengerServicesMap;

            this.booking = Objects.requireNonNull(bookingRsp.getAerocrs().getBooking());
            this.response = new ChangeSeatRspDto();

            if (booking.getFlights() != null && booking.getFlights().getFlight() != null) {
                this.flightList = booking.getFlights().getFlight();
            } else if (booking.getItems() != null && booking.getItems().getFlight() != null) {
                this.flightList = booking.getItems().getFlight();
            } else {
                this.flightList = null;
            }
        }

        ChangeSeatRspDto buildResponse() {
            populateTopLevelFields();
            populateOdsAndPriceClasses();
            populatePassengerDetails();
            populateOrderItems();
            populatePayments();
            return response;
        }

        private void populateTopLevelFields() {
            response.setResponseId("R" + java.util.UUID.randomUUID().toString().substring(0, 15).toUpperCase());
            response.setStatusCode("OPENED");

            carrierCode = "G7";
            if (changeSeatRsp.getAerocrs() != null && changeSeatRsp.getAerocrs().getCompanycode() != null) {
                carrierCode = changeSeatRsp.getAerocrs().getCompanycode();
            } else if (booking.getFlights() != null && booking.getFlights().getFlight() != null
                    && !booking.getFlights().getFlight().isEmpty()) {
                String airlineDesignator = booking.getFlights().getFlight().get(0).getAirlinedesignator();
                if (airlineDesignator != null) {
                    carrierCode = airlineDesignator;
                }
            } else if (booking.getItems() != null && booking.getItems().getFlight() != null
                    && !booking.getItems().getFlight().isEmpty()) {
                String airlineDesignator = booking.getItems().getFlight().get(0).getAirlinedesignator();
                if (airlineDesignator != null) {
                    carrierCode = airlineDesignator;
                }
            }

            response.setApiOwner(carrierCode);
            response.setPnr(booking.getPnrref());
            response.setOrderId(booking.getBookingconfirmation() != null ? booking.getBookingconfirmation()
                    : String.valueOf(booking.getBookingid()));
            response.setValidatingCarrier(carrierCode);
            response.setCurrency(booking.getCurrency() != null ? booking.getCurrency() : "USD");
            response.setTotalOrderPrice(BigDecimal.ZERO);
        }

        private void populateOdsAndPriceClasses() {
            if (flightList == null) {
                response.setOds(ods);
                return;
            }
            int segId = 1;
            int odId = 1;
            for (Flight flight : flightList) {
                processSingleFlight(flight, segId, odId);
                segId++;
                odId++;
            }
            response.setOds(ods);
        }

        private void processSingleFlight(Flight flight, int segId, int odId) {
            OD od = new OD();
            String segIdStr = "SEG" + segId;
            od.setSegmentId(segIdStr);
            od.setOdKey("OD" + odId);
            od.setOrigin(flight.getFromcode());
            od.setDestination(flight.getTocode());
            od.setOriginAirportName(flight.getFrom());
            od.setDestinationAirportName(flight.getTo());
            od.setDepartureDate(OrderMappingUtil.formatDate(flight.getFlightdate()));
            od.setArrivalDate(OrderMappingUtil.formatDate(flight.getFlightdate()));

            adjustOvernightFlight(flight, od);

            od.setDepartureTime(flight.getDepart());
            od.setArrivalTime(flight.getArrive());
            od.setJourneyTime(OrderMappingUtil.calculateJourneyTime(flight.getFlightdate(), flight.getDepart(),
                    flight.getFlightdate(), flight.getArrive()));
            od.setFlightNumber(flight.getNumber());
            od.setEquipment(flight.getAircraftType());

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

            CabinRbdInfo cabinRbd = parseCabinAndRbd(flight.getFlightClass());
            od.setCabinType(cabinRbd.cabinCode);
            od.setPriceClassId("PC" + odId);
            od.setFareBasisCode(null);
            od.setRbdCode(cabinRbd.rbdCode);

            ods.add(od);

            PriceClass pc = new PriceClass();
            pc.setPriceClassId("PC" + odId);
            pc.setClassName(determineClassName(flight.getFlightClass(), cabinRbd.cabinCode, cabinRbd.rbdCode));
            pc.setCabinTypeCode(cabinRbd.cabinCode);
            pc.setDescriptions(generatePriceClassDescriptions(flight, odId));
            priceClassList.add(pc);
        }

        private void adjustOvernightFlight(Flight flight, OD od) {
            if (flight.getDepart() != null && flight.getArrive() != null) {
                try {
                    LocalTime depTime = LocalTime.parse(flight.getDepart());
                    LocalTime arrTime = LocalTime.parse(flight.getArrive());
                    if (arrTime.isBefore(depTime)) {
                        od.setArrivalDate(OrderMappingUtil.formatDate(OrderMappingUtil.adjustDateByDays(flight.getFlightdate(), 1)));
                    }
                } catch (Exception e) {
                    // Ignore parsing exception
                }
            }
        }

        private String determineCabinCode(String code) {
            if (FIRST_CLASS_CODES.contains(code)) {
                return "First";
            }
            if (BUSINESS_CLASS_CODES.contains(code)) {
                return "Business";
            }
            return ECONOMY_CLASS;
        }

        private CabinRbdInfo parseCabinAndRbd(String rawClass) {
            if (rawClass == null) {
                return new CabinRbdInfo(ECONOMY_CLASS, null);
            }

            if (rawClass.contains("/")) {
                String[] parts = rawClass.split("/");
                if (parts.length > 0) {
                    String code = parts[0].trim().toUpperCase();
                    String cabinCode = determineCabinCode(code);
                    String rbdCode = (code.length() == 1) ? code : null;
                    return new CabinRbdInfo(cabinCode, rbdCode);
                }
            } else if (!rawClass.isEmpty() && rawClass.length() == 1) {
                return new CabinRbdInfo(ECONOMY_CLASS, rawClass);
            }
            return new CabinRbdInfo(ECONOMY_CLASS, null);
        }

        private String determineClassName(String rawClass, String cabinCode, String rbdCode) {
            if (rawClass == null) {
                return cabinCode;
            }
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
            return finalClassName;
        }

        private List<PriceClass.Description> generatePriceClassDescriptions(Flight flight, int odId) {
            List<PriceClass.Description> descs = new ArrayList<>();
            if (flight.getServices() != null) {
                for (Map.Entry<String, Boolean> entry : flight.getServices().entrySet()) {
                    PriceClass.Description d = new PriceClass.Description();
                    d.setText(entry.getKey() + ": " + entry.getValue());
                    d.setOdKey("OD" + odId);
                    descs.add(d);
                }
            }
            if (descs.isEmpty()) {
                PriceClass.Description d = new PriceClass.Description();
                d.setText("Standard Seat Selection");
                d.setOdKey("OD" + odId);
                descs.add(d);
            }
            return descs;
        }

        private String determineDefaultIssuingPlace() {
            if (flightList != null && !flightList.isEmpty() && flightList.get(0).getFromcode() != null) {
                return flightList.get(0).getFromcode();
            }
            return null;
        }

        private String determineDefaultIssuingAirline() {
            if (flightList != null && !flightList.isEmpty() && flightList.get(0).getAirline() != null) {
                return flightList.get(0).getAirline();
            }
            return null;
        }

        private void calculateTotalTax() {
            if (flightList != null) {
                for (Flight f : flightList) {
                    if (f.getTotaltaxes() > 0) {
                        totalTax = totalTax.add(BigDecimal.valueOf(f.getTotaltaxes()));
                    }
                }
            }
        }

        private void mapPassengers(String defaultIssuingPlace, String defaultIssuingAirline) {
            if (booking.getPassengers() != null && booking.getPassengers().getPassenger() != null) {
                paxCounter = 1;
                adtList.clear();
                for (Passenger p : booking.getPassengers().getPassenger()) {
                    processSinglePassenger(p, defaultIssuingPlace, defaultIssuingAirline);
                }
            }
        }

        private void populatePassengerDetails() {
            List<BookingReferences> refs = new ArrayList<>();
            BookingReferences ref = new BookingReferences();
            ref.setId(booking.getPnrref());
            ref.setAirlineId(carrierCode);
            refs.add(ref);
            response.setBookingReferences(refs);

            String defaultIssuingPlace = determineDefaultIssuingPlace();
            String defaultIssuingAirline = determineDefaultIssuingAirline();

            calculateTotalTax();

            isPaymentProvided = (requestDto != null && requestDto.getPaymentInformation() != null);

            buildPaxToRequestedSeatsMap();

            mapPassengers(defaultIssuingPlace, defaultIssuingAirline);

            response.setPaxDetailList(paxList);

            if (isPaymentProvided) {
                response.setTicketDocInfoList(topTicketDocInfos);
                response.setEmdInfoList(topEmdInfos);
            } else {
                response.setTicketDocInfoList(null);
                response.setEmdInfoList(null);
            }
        }

        private void processOffer(com.airlines.go7api.requestdto.common.ChangeOfferReqDto offer) {
            if (offer.getOfferItems() != null) {
                for (com.airlines.go7api.requestdto.common.ChangeOfferReqDto.OfferItemDto item : offer.getOfferItems()) {
                    processOfferItem(item);
                }
            }
        }

        private void processOfferItem(com.airlines.go7api.requestdto.common.ChangeOfferReqDto.OfferItemDto item) {
            if (item.getPaxRefs() != null && item.getRow() != null && item.getColumn() != null) {
                String seatCoord = item.getRow().toString() + item.getColumn();
                for (String paxRef : item.getPaxRefs()) {
                    paxToRequestedSeats.computeIfAbsent(paxRef, k -> new ArrayList<>()).add(seatCoord);
                }
            }
        }

        private void buildPaxToRequestedSeatsMap() {
            if (requestDto != null && requestDto.getOffers() != null) {
                for (com.airlines.go7api.requestdto.common.ChangeOfferReqDto offer : requestDto.getOffers()) {
                    processOffer(offer);
                }
            }
        }

        private String assignPassengerIdAndLinkInfant(String assignedPtc, PaxDetailDTO pax) {
            String pid;
            if ("INF".equals(assignedPtc)) {
                if (!adtList.isEmpty()) {
                    PaxDetailDTO parent = adtList.get(adtList.size() - 1);
                    pid = parent.getPaxId() + ".1";
                    try {
                        parent.setInfantRef(pid);
                    } catch (Exception e) {
                        // Ignore exception
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
            return pid;
        }

        private void populateEmail(Passenger p, PaxDetailDTO pax) {
            if (p.getEmail() != null) {
                PaxDetailDTO.EmailDTO email = new PaxDetailDTO.EmailDTO();
                email.setEmailAddress(p.getEmail().toUpperCase());
                email.setLabel("OTH");
                email.setType("OSI");
                List<PaxDetailDTO.EmailDTO> emails = new ArrayList<>();
                emails.add(email);
                pax.setEmails(emails);
            }
        }

        private void populatePassengerDocs(Passenger p, PaxDetailDTO pax, String pid,
                String defaultIssuingPlace, String defaultIssuingAirline) {
            if (isPaymentProvided && p.getETickets() != null && p.getETickets().getFlight() != null) {
                populatePassengerTicketDocs(p, pax, pid, defaultIssuingPlace, defaultIssuingAirline);
            }

            if (isPaymentProvided && changeSeatRsp.getAerocrs() != null && changeSeatRsp.getAerocrs().getFlights() != null) {
                populatePassengerEmds(p, pax, pid, defaultIssuingPlace, defaultIssuingAirline);
            }
        }

        private void processSinglePassenger(Passenger p, String defaultIssuingPlace, String defaultIssuingAirline) {
            PaxDetailDTO pax = new PaxDetailDTO();
            String rawTitle = p.getPaxtitle() != null ? p.getPaxtitle().toUpperCase().replace(".", "") : "MR";
            boolean isInfantByTitle = rawTitle.contains("INF");

            String assignedPtc = OrderMappingUtil.mapPaxType(p.getPaxtype());
            if (isInfantByTitle) {
                assignedPtc = "INF";
            }

            String pid = assignPassengerIdAndLinkInfant(assignedPtc, pax);

            rawPaxIds.add(pid);
            pax.setPaxId(pid);
            pax.setPtc(assignedPtc);
            pax.setTitle(rawTitle);
            pax.setGivenName(p.getFirstname() != null ? p.getFirstname().toUpperCase() : "");
            pax.setSurname(p.getLastname() != null ? p.getLastname().toUpperCase() : "");
            pax.setBirthDate(OrderMappingUtil.formatDate(p.getDob()));

            pax.setGender(determineGender(p));

            if ("CHD".equals(pax.getPtc()) || "CNN".equals(pax.getPtc())) {
                pax.setTitle("CHILD");
            } else if ("INF".equals(pax.getPtc())) {
                pax.setTitle("INFANT");
            }

            populateEmail(p, pax);
            populatePassengerDocs(p, pax, pid, defaultIssuingPlace, defaultIssuingAirline);

            paxList.add(pax);
        }

        private String determineGender(Passenger p) {
            String gender = null;
            if (p.getGender() != null && !p.getGender().isEmpty()) {
                if (p.getGender().toUpperCase().startsWith("M")) {
                    gender = "Male";
                } else if (p.getGender().toUpperCase().startsWith("F")) {
                    gender = "Female";
                }
            }

            if (gender == null && p.getPaxtitle() != null) {
                String title = p.getPaxtitle().toUpperCase();
                if (title.contains("MR") || title.contains("MSTR") || title.contains("MISTR")) {
                    gender = "Male";
                } else if (title.contains("MS") || title.contains("MRS") || title.contains("MISS")) {
                    gender = "Female";
                }
            }

            return (gender != null) ? gender : "Male";
        }

        private Set<String> getUniqueTickets(Passenger p) {
            Set<String> uniqueTickets = new java.util.LinkedHashSet<>();
            for (Passenger.ETicketFlight etf : p.getETickets().getFlight()) {
                if (etf.getEticketnumber() != null && !etf.getEticketnumber().isEmpty()) {
                    uniqueTickets.add(etf.getEticketnumber().trim());
                }
            }
            return uniqueTickets;
        }

        private TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO buildCoupon(Flight f, int couponNum) {
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
            coupon.setValidatingCarrier(carrierCode);

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
            return coupon;
        }

        private List<TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO> buildCoupons() {
            List<TicketDocInfoDTO.TicketDocumentDTO.CouponInfoDTO> coupons = new ArrayList<>();
            if (flightList != null) {
                int couponNum = 1;
                for (Flight f : flightList) {
                    coupons.add(buildCoupon(f, couponNum++));
                }
            }
            return coupons;
        }

        private void populatePassengerTicketDocs(Passenger p, PaxDetailDTO pax, String pid,
                String defaultIssuingPlace, String defaultIssuingAirline) {
            List<TicketDocInfoDTO> paxTicketDocs = new ArrayList<>();

            TicketDocInfoDTO tdi = new TicketDocInfoDTO();
            tdi.setPaxId(Arrays.asList(pid));
            tdi.setValidatingCarrier(carrierCode);
            tdi.setIssuingAirlineName(defaultIssuingAirline);
            tdi.setIssuingPlace(defaultIssuingPlace);

            List<TicketDocInfoDTO.TicketDocumentDTO> docs = new ArrayList<>();
            Set<String> uniqueTickets = getUniqueTickets(p);

            for (String ticketNbr : uniqueTickets) {
                TicketDocInfoDTO.TicketDocumentDTO doc = new TicketDocInfoDTO.TicketDocumentDTO();
                doc.setTicketDocNbr(ticketNbr);
                doc.setType("T");
                doc.setNumberOfBooklets(1);
                doc.setDateOfIssue(OrderMappingUtil.formatCurrentDate());
                doc.setTimeOfIssue("00:00");
                doc.setTicketingLocation(tdi.getIssuingPlace());
                doc.setReportingType("BSP");

                doc.setCouponInfo(buildCoupons());
                docs.add(doc);
            }
            tdi.setTicketDocument(docs);
            paxTicketDocs.add(tdi);
            pax.setTicketDocInfo(paxTicketDocs);

            topTicketDocInfos.add(tdi);
        }

        private Seat findSeatInFlight(Flight f, String reqSeat) {
            if (f.getSeat() != null) {
                for (Seat s : f.getSeat()) {
                    if (reqSeat.equalsIgnoreCase(s.getSeatNumber())) {
                        return s;
                    }
                }
            }
            return null;
        }

        private Seat findMatchedSeat(String reqSeat) {
            if (changeSeatRsp.getAerocrs() == null || changeSeatRsp.getAerocrs().getFlights() == null) {
                return null;
            }
            for (Flight f : changeSeatRsp.getAerocrs().getFlights().getFlight()) {
                Seat s = findSeatInFlight(f, reqSeat);
                if (s != null) {
                    return s;
                }
            }
            return null;
        }

        private EMDInfoDTO buildEmd(Passenger p, String pid, Seat matchedSeat,
                String defaultIssuingPlace, String defaultIssuingAirline) {
            EMDInfoDTO emd = new EMDInfoDTO();
            emd.setValidatingCarrier(carrierCode);
            emd.setPaxId(Arrays.asList(pid));
            emd.setIssuingAirlineName(defaultIssuingAirline);
            emd.setIssuingPlace(defaultIssuingPlace);

            List<EMDInfoDTO.TicketDocumentDTO> emdDocs = new ArrayList<>();
            EMDInfoDTO.TicketDocumentDTO emdDoc = new EMDInfoDTO.TicketDocumentDTO();

            String baseTkt = "EMD";
            if (p.getETickets() != null && p.getETickets().getFlight() != null && !p.getETickets().getFlight().isEmpty()) {
                baseTkt = p.getETickets().getFlight().get(0).getEticketnumber();
            }

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
            emdCoupon.setValidatingCarrier(carrierCode);
            emdCoupon.setStatus("I");
            emdCoupon.setServiceRefs(Arrays.asList(matchedSeat.getSeatNumber()));

            if (flightList != null && !flightList.isEmpty()) {
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
            }

            emdCoupons.add(emdCoupon);
            emdDoc.setCouponInfo(emdCoupons);
            emdDocs.add(emdDoc);
            emd.setTicketDocument(emdDocs);
            return emd;
        }

        private void populatePassengerEmds(Passenger p, PaxDetailDTO pax, String pid,
                String defaultIssuingPlace, String defaultIssuingAirline) {
            List<EMDInfoDTO> paxEmds = new ArrayList<>();
            List<String> assignedSeatsForPax = paxToRequestedSeats.getOrDefault(pid, new ArrayList<>());

            for (String reqSeat : assignedSeatsForPax) {
                Seat matchedSeat = findMatchedSeat(reqSeat);
                if (matchedSeat != null) {
                    EMDInfoDTO emd = buildEmd(p, pid, matchedSeat, defaultIssuingPlace, defaultIssuingAirline);
                    paxEmds.add(emd);
                    topEmdInfos.add(emd);
                }
            }
            if (!paxEmds.isEmpty()) {
                pax.setEmdInfo(paxEmds);
            }
        }

        private void populateOrderItems() {
            String primaryPtc = "ADT";
            if (booking.getPassengers() != null && booking.getPassengers().getPassenger() != null
                    && !booking.getPassengers().getPassenger().isEmpty()) {
                primaryPtc = OrderMappingUtil.mapPaxType(booking.getPassengers().getPassenger().get(0).getPaxtype());
            }

            OrderItemsDTO airItem = new OrderItemsDTO();
            airItem.setOrderItemId(response.getOrderId() + "_AIR-1");
            airItem.setPtc(primaryPtc);

            calculateSeatCharges();
            calculateAirItemPrice();
            calculatePnrTotal();

            if (isPaymentProvided && requestDto.getPaymentInformation().getAmount() != null) {
                paymentSeatAmount = requestDto.getPaymentInformation().getAmount();
            } else {
                paymentSeatAmount = seatCharges;
            }

            detectSeatCharges();

            airItemPrice = airItemPrice.setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal airBaseFare = airItemPrice.subtract(totalTax);
            if (airBaseFare.compareTo(BigDecimal.ZERO) < 0) {
                airBaseFare = airItemPrice;
            }

            airItem.setBaseFare(new OrderItemsDTO.BaseFare(
                    airBaseFare.setScale(2, java.math.RoundingMode.HALF_UP), response.getCurrency()));
            airItem.setTotalTax(new OrderItemsDTO.TotalTax(
                    totalTax.setScale(2, java.math.RoundingMode.HALF_UP), response.getCurrency()));
            airItem.setTotalFare(new OrderItemsDTO.TotalFare(
                    airItemPrice.setScale(2, java.math.RoundingMode.HALF_UP), response.getCurrency()));
            airItem.setTotalPrice(airItemPrice.setScale(2, java.math.RoundingMode.HALF_UP));
            airItem.setPassengerIds(rawPaxIds);

            populateAirItemTaxes(airItem);
            populateAirItemServices(airItem);

            orderItems.add(airItem);
            totalOrderPrice = totalOrderPrice.add(airItemPrice);

            populateSecondaryOrderItems();

            response.setOrderItems(orderItems);
            response.setTotalOrderPrice(totalOrderPrice.setScale(2, java.math.RoundingMode.HALF_UP));
        }

        private BigDecimal sumFlightSeatFares(Flight f) {
            BigDecimal sum = BigDecimal.ZERO;
            if (f.getSeat() != null) {
                for (Seat s : f.getSeat()) {
                    if (s.getFare() != null) {
                        sum = sum.add(s.getFare());
                    }
                }
            }
            return sum;
        }

        private BigDecimal sumSeatFares(List<Flight> flights) {
            BigDecimal sum = BigDecimal.ZERO;
            if (flights != null) {
                for (Flight f : flights) {
                    sum = sum.add(sumFlightSeatFares(f));
                }
            }
            return sum;
        }

        private void calculateSeatCharges() {
            List<Flight> changeSeatFlights = null;
            if (changeSeatRsp.getAerocrs() != null && changeSeatRsp.getAerocrs().getFlights() != null) {
                changeSeatFlights = changeSeatRsp.getAerocrs().getFlights().getFlight();
            }

            seatCharges = sumSeatFares(changeSeatFlights);

            if (seatCharges.compareTo(BigDecimal.ZERO) <= 0) {
                seatCharges = sumSeatFares(flightList);
            }
        }

        private BigDecimal getFlightBasePrice(Flight f) {
            if (f.getInvpricingwithouttax() != null) {
                try {
                    return new BigDecimal(f.getInvpricingwithouttax());
                } catch (Exception e) {
                    // Ignore parsing exception
                }
            }
            return BigDecimal.ZERO;
        }

        private BigDecimal calculateSumFlightPrice() {
            BigDecimal sumFlightPrice = BigDecimal.ZERO;
            if (flightList != null) {
                for (Flight f : flightList) {
                    sumFlightPrice = sumFlightPrice.add(getFlightBasePrice(f)).add(BigDecimal.valueOf(f.getTotaltaxes()));
                }
            }
            return sumFlightPrice;
        }

        private BigDecimal getFlightInvPricing(Flight f) {
            if (f.getInvpricing() != null) {
                try {
                    return new BigDecimal(f.getInvpricing());
                } catch (Exception e) {
                    // Ignore parsing exception
                }
            }
            return BigDecimal.ZERO;
        }

        private BigDecimal calculateInvPricingBasis() {
            BigDecimal invPricingBasis = BigDecimal.ZERO;
            if (flightList != null) {
                for (Flight f : flightList) {
                    invPricingBasis = invPricingBasis.add(getFlightInvPricing(f));
                }
            }
            return invPricingBasis;
        }

        private void calculateAirItemPrice() {
            BigDecimal sumFlightPrice = calculateSumFlightPrice();
            BigDecimal invPricingBasis = calculateInvPricingBasis();
            airItemPrice = (invPricingBasis.compareTo(BigDecimal.ZERO) > 0) ? invPricingBasis : sumFlightPrice;
        }

        private void calculatePnrTotal() {
            if (booking.getBalanceInformation() != null && booking.getBalanceInformation().getPnrTotal() != null) {
                pnrTotal = booking.getBalanceInformation().getPnrTotal();
            } else if (booking.getTotalprice() != null) {
                try {
                    pnrTotal = new BigDecimal(booking.getTotalprice());
                } catch (Exception e) {
                    // Ignore parsing exception
                }
            }
        }

        private void detectSeatCharges() {
            if (seatCharges.compareTo(BigDecimal.ZERO) <= 0) {
                if (pnrTotal.compareTo(airItemPrice) > 0) {
                    seatCharges = pnrTotal.subtract(airItemPrice);
                } else {
                    detectSeatChargesFromActualPrices();
                }
            }
        }

        private BigDecimal sumActualPricesOfSeatsInFlight(com.airlines.go7api.responsego7.common.Flight f) {
            BigDecimal totalFetched = BigDecimal.ZERO;
            if (f.getSeat() != null) {
                for (com.airlines.go7api.responsego7.common.Seat s : f.getSeat()) {
                    if (s.getSeatNumber() != null && actualPrices != null && actualPrices.containsKey(s.getSeatNumber())) {
                        BigDecimal actualFare = actualPrices.get(s.getSeatNumber());
                        s.setFare(actualFare);
                        totalFetched = totalFetched.add(actualFare);
                    }
                }
            }
            return totalFetched;
        }

        private BigDecimal sumActualPricesOfSeats() {
            BigDecimal totalFetched = BigDecimal.ZERO;
            if (changeSeatRsp.getAerocrs() != null && changeSeatRsp.getAerocrs().getFlights() != null) {
                for (com.airlines.go7api.responsego7.common.Flight f : changeSeatRsp.getAerocrs().getFlights().getFlight()) {
                    totalFetched = totalFetched.add(sumActualPricesOfSeatsInFlight(f));
                }
            }
            return totalFetched;
        }

        private void detectSeatChargesFromActualPrices() {
            BigDecimal totalFetched = BigDecimal.ZERO;
            if (changeSeatRsp != null) {
                totalFetched = sumActualPricesOfSeats();
            }

            if (totalFetched.compareTo(BigDecimal.ZERO) > 0) {
                seatCharges = totalFetched;
            } else {
                seatCharges = paymentSeatAmount;
            }
        }

        private BigDecimal sumGranularTaxes() {
            BigDecimal totalGranular = BigDecimal.ZERO;
            if (flightList != null) {
                for (Flight f : flightList) {
                    if (f.getTaxes() != null) {
                        totalGranular = totalGranular.add(BigDecimal.valueOf(f.getTaxes().getSecurity()));
                        totalGranular = totalGranular.add(BigDecimal.valueOf(f.getTaxes().getFuel()));
                        totalGranular = totalGranular.add(BigDecimal.valueOf(f.getTaxes().getGroundHandling()));
                        totalGranular = totalGranular.add(BigDecimal.valueOf(f.getTaxes().getTax1()));
                        totalGranular = totalGranular.add(BigDecimal.valueOf(f.getTaxes().getTax4()));
                    }
                }
            }
            return totalGranular;
        }

        private void scaleAndAddTaxes(List<OrderItemsDTO.Tax> airTaxes, BigDecimal scaleFactor) {
            if (flightList != null) {
                for (Flight f : flightList) {
                    if (f.getTaxes() != null) {
                        Taxes tObj = f.getTaxes();
                        addScaledTaxItem(airTaxes, "I2", tObj.getSecurity(), scaleFactor, response.getCurrency(), "Security Tax");
                        addScaledTaxItem(airTaxes, "YQ", tObj.getFuel(), scaleFactor, response.getCurrency(), "Fuel Surcharge");
                        addScaledTaxItem(airTaxes, "GH", tObj.getGroundHandling(), scaleFactor, response.getCurrency(), "Ground Handling");
                        addScaledTaxItem(airTaxes, "IN", tObj.getTax1(), scaleFactor, response.getCurrency(), "Infrastructure Tax");
                        addScaledTaxItem(airTaxes, "OT", tObj.getTax4(), scaleFactor, response.getCurrency(), "Other Tax");
                    }
                }
            }
        }

        private void populateAirItemTaxes(OrderItemsDTO airItem) {
            List<OrderItemsDTO.Tax> airTaxes = new ArrayList<>();
            if (flightList != null && totalTax.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal totalGranular = sumGranularTaxes();
                BigDecimal scaleFactor = BigDecimal.ONE;
                if (totalGranular.compareTo(BigDecimal.ZERO) > 0) {
                    scaleFactor = totalTax.divide(totalGranular, 10, java.math.RoundingMode.HALF_UP);
                }
                scaleAndAddTaxes(airTaxes, scaleFactor);
            } else if (totalTax.compareTo(BigDecimal.ZERO) > 0) {
                OrderItemsDTO.Tax tx = new OrderItemsDTO.Tax();
                tx.setCode("TAX");
                tx.setAmount(totalTax.setScale(2, java.math.RoundingMode.HALF_UP));
                tx.setCurrency(response.getCurrency());
                tx.setDescription("Total Taxes");
                airTaxes.add(tx);
            }

            if (!airTaxes.isEmpty()) {
                airItem.setTaxes(airTaxes);
            }
        }

        private void addFlightServicesForSegment(String segKey) {
            for (String pid : rawPaxIds) {
                Service srv = new Service();
                srv.setServiceId(segKey + "_" + pid);
                srv.setServiceStatus(STATUS_CONFIRMED);
                flightServices.add(srv);
            }
        }

        private void populateAirItemServices(OrderItemsDTO airItem) {
            if (flightList != null && !flightList.isEmpty()) {
                for (String segKey : flightSegmentMap.values()) {
                    addFlightServicesForSegment(segKey);
                }
            }
            airItem.setServiceList(flightServices);
        }

        private void populateSecondaryOrderItems() {
            processGo7Seats();

            BigDecimal currentSecondaryPrice = totalOrderPrice.subtract(airItemPrice);
            BigDecimal totalSecondaryNeeded = pnrTotal.subtract(airItemPrice);
            BigDecimal leftoverPrice = totalSecondaryNeeded.subtract(currentSecondaryPrice);
            if (leftoverPrice.compareTo(BigDecimal.ZERO) < 0) {
                leftoverPrice = BigDecimal.ZERO;
            }

            int cachedCount = countCachedItems();
            BigDecimal distributedCachedPrice = BigDecimal.ZERO;
            if (cachedCount > 0 && leftoverPrice.compareTo(BigDecimal.ZERO) > 0) {
                distributedCachedPrice = leftoverPrice.divide(new BigDecimal(cachedCount), 2, java.math.RoundingMode.HALF_UP);
            }

            addCachedSeats(distributedCachedPrice);
            addCachedServices(distributedCachedPrice);
        }

        private String buildFlightKey(Flight f) {
            String num = f.getNumber() != null ? f.getNumber() : "";
            String from = f.getFromcode() != null ? f.getFromcode() : "";
            String to = f.getTocode() != null ? f.getTocode() : "";
            return num + "_" + from + "_" + to;
        }

        private void processFlightSeats(Flight f) {
            if (f.getSeat() != null) {
                String flightKey = buildFlightKey(f);
                String segmentId = flightSegmentMap.getOrDefault(flightKey, "SEG1");

                int totalSeats = f.getSeat().size();
                BigDecimal distributedSeatPrice = BigDecimal.ZERO;
                if (totalSeats > 0 && seatCharges.compareTo(BigDecimal.ZERO) > 0) {
                    distributedSeatPrice = seatCharges.divide(new BigDecimal(totalSeats), 2, java.math.RoundingMode.HALF_UP);
                }

                int seatCounter = 0;
                for (Seat s : f.getSeat()) {
                    processSingleGo7Seat(s, segmentId, distributedSeatPrice, seatCounter);
                    seatCounter++;
                }
            }
        }

        private void processGo7Seats() {
            if (changeSeatRsp.getAerocrs() == null || changeSeatRsp.getAerocrs().getFlights() == null) {
                return;
            }

            for (Flight f : changeSeatRsp.getAerocrs().getFlights().getFlight()) {
                processFlightSeats(f);
            }
        }

        private SeatPassengerInfo determineSeatPassengerInfo(int seatCounter) {
            String assignedPaxId = "T1";
            String seatPtc = "ADT";
            if (booking.getPassengers() != null && booking.getPassengers().getPassenger() != null && !booking.getPassengers().getPassenger().isEmpty()) {
                int paxIdx = seatCounter % booking.getPassengers().getPassenger().size();
                assignedPaxId = "T" + (paxIdx + 1);
                seatPtc = OrderMappingUtil.mapPaxType(booking.getPassengers().getPassenger().get(paxIdx).getPaxtype());
            }
            return new SeatPassengerInfo(assignedPaxId, seatPtc);
        }

        private BigDecimal lookupCachedSeatPrice(String assignedPaxId, String seatNumber, BigDecimal defaultPrice) {
            String rawValue = passengerSeatsMap.get(assignedPaxId);
            if (rawValue == null || !rawValue.contains("|")) {
                return defaultPrice;
            }
            String[] parts = rawValue.split("\\|");
            if (!parts[0].equals(seatNumber)) {
                return defaultPrice;
            }
            try {
                BigDecimal storedPrice = new BigDecimal(parts[1]);
                if (storedPrice.compareTo(BigDecimal.ZERO) > 0) {
                    return storedPrice;
                }
            } catch (Exception e) {
                // Ignore parsing exception
            }
            return defaultPrice;
        }

        private BigDecimal determineSeatPrice(Seat s, String assignedPaxId, BigDecimal distributedSeatPrice) {
            BigDecimal price = s.getFare() != null && s.getFare().compareTo(BigDecimal.ZERO) > 0
                    ? s.getFare()
                    : BigDecimal.ZERO;

            if (price.compareTo(BigDecimal.ZERO) <= 0 && actualPrices != null && s.getSeatNumber() != null
                    && actualPrices.containsKey(s.getSeatNumber())) {
                price = actualPrices.get(s.getSeatNumber());
            }

            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                price = distributedSeatPrice;
            }

            if (price.compareTo(BigDecimal.ZERO) == 0 && passengerSeatsMap != null) {
                price = lookupCachedSeatPrice(assignedPaxId, s.getSeatNumber(), price);
            }
            return price;
        }

        private void populateSeatRowAndColumn(String seatNum, Service seatSrv) {
            if (seatNum != null && !seatNum.isEmpty()) {
                String col = seatNum.substring(seatNum.length() - 1);
                String rowStr = seatNum.substring(0, seatNum.length() - 1);
                seatSrv.setColumn(col);
                try {
                    seatSrv.setRow(new java.math.BigInteger(rowStr));
                } catch (Exception e) {
                    // Ignore parsing exception
                }
            }
        }

        private Service buildSeatService(Seat s, String segmentId, String assignedPaxId) {
            Service seatSrv = new Service();
            seatSrv.setServiceId(segmentId + "_" + assignedPaxId);
            seatSrv.setServiceStatus(s.isStatus() ? STATUS_CONFIRMED : "PENDING");
            seatSrv.setServiceCode("SEAT" + (s.getSeatNumber() != null ? s.getSeatNumber() : ""));
            seatSrv.setServiceName("Specific Seat Request");
            seatSrv.setSegmentId(segmentId);

            populateSeatRowAndColumn(s.getSeatNumber(), seatSrv);

            List<Service.SeatCharacteristic> chars = new ArrayList<>();
            chars.add(new Service.SeatCharacteristic("CH", "Chargeable Seat"));
            seatSrv.setSeatCharacteristics(chars);
            return seatSrv;
        }

        private void processSingleGo7Seat(Seat s, String segmentId, BigDecimal distributedSeatPrice, int seatCounter) {
            SeatPassengerInfo paxInfo = determineSeatPassengerInfo(seatCounter);

            BigDecimal seatPrice = determineSeatPrice(s, paxInfo.paxId, distributedSeatPrice);
            String currency = s.getCurrency() != null ? s.getCurrency() : response.getCurrency();

            OrderItemsDTO seatItem = new OrderItemsDTO();
            seatItem.setOrderItemId(response.getOrderId() + "_SRV" + srvIdx++);
            seatItem.setPtc(paxInfo.ptc);
            seatItem.setPassengerIds(Arrays.asList(paxInfo.paxId));
            seatItem.setBaseFare(new OrderItemsDTO.BaseFare(seatPrice, currency));
            seatItem.setTotalTax(new OrderItemsDTO.TotalTax(BigDecimal.ZERO, currency));
            seatItem.setTotalFare(new OrderItemsDTO.TotalFare(seatPrice, currency));
            seatItem.setTotalPrice(seatPrice);

            Service seatSrv = buildSeatService(s, segmentId, paxInfo.paxId);
            seatItem.setServiceList(Arrays.asList(seatSrv));

            orderItems.add(seatItem);
            totalOrderPrice = totalOrderPrice.add(seatPrice);
            processedSeatKeys.add(paxInfo.paxId + "_" + (s.getSeatNumber() != null ? s.getSeatNumber() : ""));
        }

        private int countCachedItems() {
            int cachedCount = 0;
            if (passengerSeatsMap != null) {
                for (Map.Entry<String, String> entry : passengerSeatsMap.entrySet()) {
                    String seatNum = entry.getValue();
                    if (seatNum != null && seatNum.contains("|")) {
                        seatNum = seatNum.split("\\|")[0];
                    }
                    if (!processedSeatKeys.contains(entry.getKey() + "_" + seatNum)) {
                        cachedCount++;
                    }
                }
            }
            if (passengerServicesMap != null) {
                cachedCount += passengerServicesMap.size();
            }
            return cachedCount;
        }

        private void addCachedSeats(BigDecimal distributedCachedPrice) {
            if (passengerSeatsMap == null) {
                return;
            }
            for (Map.Entry<String, String> entry : passengerSeatsMap.entrySet()) {
                String pId = entry.getKey();
                String rawValue = entry.getValue();
                String seatNum = rawValue;
                BigDecimal seatPrice = distributedCachedPrice;

                if (rawValue != null && rawValue.contains("|")) {
                    String[] parts = rawValue.split("\\|");
                    seatNum = parts[0];
                    try {
                        BigDecimal storedPrice = new BigDecimal(parts[1]);
                        if (storedPrice.compareTo(BigDecimal.ZERO) > 0) {
                            seatPrice = storedPrice;
                        }
                    } catch (Exception e) {
                        // Ignore parsing exception
                    }
                }

                if (!processedSeatKeys.contains(pId + "_" + seatNum)) {
                    OrderItemsDTO seatItem = new OrderItemsDTO();
                    seatItem.setOrderItemId(response.getOrderId() + "_SRV" + srvIdx++);
                    seatItem.setPassengerIds(Arrays.asList(pId));

                    seatItem.setTotalPrice(seatPrice);
                    seatItem.setBaseFare(new OrderItemsDTO.BaseFare(seatPrice, response.getCurrency()));
                    seatItem.setTotalFare(new OrderItemsDTO.TotalFare(seatPrice, response.getCurrency()));

                    List<Service> seatServicesList = new ArrayList<>();
                    Service seatSrv = new Service();
                    seatSrv.setServiceId("SEG1_" + pId);
                    seatSrv.setServiceStatus(STATUS_CONFIRMED);
                    seatSrv.setServiceCode("SEAT" + seatNum);
                    seatSrv.setServiceName("Specific Seat Request");
                    seatServicesList.add(seatSrv);
                    seatItem.setServiceList(seatServicesList);

                    orderItems.add(seatItem);
                    totalOrderPrice = totalOrderPrice.add(seatPrice);
                }
            }
        }

        private void addCachedServices(BigDecimal distributedCachedPrice) {
            if (passengerServicesMap == null) {
                return;
            }
            for (Map.Entry<String, String> entry : passengerServicesMap.entrySet()) {
                String pId = entry.getKey();
                String rawValue = entry.getValue();
                String srvCode = rawValue;
                BigDecimal srvPrice = distributedCachedPrice;

                if (rawValue != null && rawValue.contains("|")) {
                    String[] parts = rawValue.split("\\|");
                    srvCode = parts[0];
                    try {
                        BigDecimal storedPrice = new BigDecimal(parts[1]);
                        if (storedPrice.compareTo(BigDecimal.ZERO) > 0) {
                            srvPrice = storedPrice;
                        }
                    } catch (Exception e) {
                        // Ignore parsing exception
                    }
                }

                OrderItemsDTO srvItem = new OrderItemsDTO();
                srvItem.setOrderItemId(response.getOrderId() + "_SRV" + srvIdx++);
                srvItem.setPassengerIds(Arrays.asList(pId));

                srvItem.setTotalPrice(srvPrice);
                srvItem.setBaseFare(new OrderItemsDTO.BaseFare(srvPrice, response.getCurrency()));
                srvItem.setTotalFare(new OrderItemsDTO.TotalFare(srvPrice, response.getCurrency()));

                List<Service> srvList = new ArrayList<>();
                Service srv = new Service();
                srv.setServiceId(srvCode);
                srv.setServiceStatus(STATUS_CONFIRMED);
                srv.setServiceCode(srvCode);
                srv.setServiceName("Ancillary Service");
                srvList.add(srv);
                srvItem.setServiceList(srvList);

                orderItems.add(srvItem);
                totalOrderPrice = totalOrderPrice.add(srvPrice);
            }
        }

        private void populatePayments() {
            if (isPaymentProvided) {
                List<PaymentsDTO> payments = new ArrayList<>();
                String pType = (requestDto.getPaymentType() != null) ? requestDto.getPaymentType() : "CA";

                if (paymentSeatAmount.compareTo(BigDecimal.ZERO) > 0 || seatCharges.compareTo(BigDecimal.ZERO) > 0) {
                    PaymentsDTO paySeat = new PaymentsDTO();
                    paySeat.setType(pType);
                    paySeat.setStatusCode("SUCCESSFUL");
                    paySeat.setCurrency(response.getCurrency());

                    paySeat.setAmount(paymentSeatAmount.setScale(2, java.math.RoundingMode.HALF_UP));

                    if (paymentSeatAmount.compareTo(BigDecimal.ZERO) <= 0 && seatCharges.compareTo(BigDecimal.ZERO) > 0) {
                        paySeat.setAmount(seatCharges.setScale(2, java.math.RoundingMode.HALF_UP));
                    }

                    paySeat.setOrderItem(Arrays.asList(response.getOrderId() + "_SRV" + (flightServices.size() + 1)));
                    payments.add(paySeat);
                }
                response.setPayments(payments);
            } else {
                response.setPayments(null);
            }
            response.setPriceClassList(priceClassList);
        }

        private void addScaledTaxItem(List<OrderItemsDTO.Tax> list, String code, double amount,
                BigDecimal scaleFactor, String currency, String description) {
            if (amount > 0) {
                OrderItemsDTO.Tax t = new OrderItemsDTO.Tax();
                t.setCode(code);
                BigDecimal scaledAmount = BigDecimal.valueOf(amount).multiply(scaleFactor).setScale(2,
                        java.math.RoundingMode.HALF_UP);
                t.setAmount(scaledAmount);
                t.setCurrency(currency);
                t.setDescription(description);
                list.add(t);
            }
        }
    }

    private static class CabinRbdInfo {
        final String cabinCode;
        final String rbdCode;

        CabinRbdInfo(String cabinCode, String rbdCode) {
            this.cabinCode = cabinCode;
            this.rbdCode = rbdCode;
        }
    }

    private static class SeatPassengerInfo {
        final String paxId;
        final String ptc;

        SeatPassengerInfo(String paxId, String ptc) {
            this.paxId = paxId;
            this.ptc = ptc;
        }
    }

}