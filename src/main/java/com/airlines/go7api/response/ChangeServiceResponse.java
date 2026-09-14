package com.airlines.go7api.response;

import com.airlines.go7api.responsego7.common.*;
import com.airlines.go7api.responsedto.common.*;
import com.airlines.go7api.responsedto.ChangeServiceRspDto;
import com.airlines.go7api.responsego7.ChangeServiceRspGo7Dto;
import com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;

public class ChangeServiceResponse {

    private static final String ECONOMY_CLASS = "Economy";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String ANCILLARY_SERVICE_NAME = "Ancillary Service";

    private ChangeServiceResponse() {
        throw new IllegalStateException("Utility class");
    }

    public static ChangeServiceRspDto generateResponse(ChangeServiceRspGo7Dto changeServiceRsp,
            OrderRetrieveRspGo7Dto bookingRsp, com.airlines.go7api.requestdto.ChangeServiceReqDto requestDto,
            Map<String, BigDecimal> actualPrices,
            Map<String, String> passengerSeatsMap,
            Map<String, String> passengerServicesMap) {
        if (bookingRsp == null || bookingRsp.getAerocrs() == null || bookingRsp.getAerocrs().getBooking() == null) {
            return new ChangeServiceRspDto();
        }
        return new ResponseContext(changeServiceRsp, bookingRsp, requestDto, actualPrices, passengerSeatsMap, passengerServicesMap).buildResponse();
    }

    private static class ResponseContext {
        final ChangeServiceRspGo7Dto changeServiceRsp;
        final OrderRetrieveRspGo7Dto bookingRsp;
        final com.airlines.go7api.requestdto.ChangeServiceReqDto requestDto;
        final Map<String, BigDecimal> actualPrices;
        final Map<String, String> passengerSeatsMap;
        final Map<String, String> passengerServicesMap;

        final Booking booking;
        final ChangeServiceRspDto response;
        final List<Flight> flightList;
        final Map<String, String> flightSegmentMap = new HashMap<>();
        final Map<String, List<String>> paxToRequestedServices = new HashMap<>();
        final List<String> rawPaxIds = new ArrayList<>();
        final List<TicketDocInfoDTO> topTicketDocInfos = new ArrayList<>();
        final List<EMDInfoDTO> topEmdInfos = new ArrayList<>();
        final List<PaxDetailDTO> paxList = new ArrayList<>();
        final List<PriceClass> priceClassList = new ArrayList<>();
        final List<OD> ods = new ArrayList<>();
        final List<OrderItemsDTO> orderItems = new ArrayList<>();
        final List<Service> flightServices = new ArrayList<>();
        final Set<String> processedSrvKeys = new HashSet<>();

        String carrierCode;
        BigDecimal totalTax = BigDecimal.ZERO;
        boolean isPaymentProvided;
        BigDecimal srvTotalFromReq = BigDecimal.ZERO;
        BigDecimal actualSrvPrice = BigDecimal.ZERO;
        BigDecimal airItemPrice = BigDecimal.ZERO;
        BigDecimal pnrTotal = BigDecimal.ZERO;
        BigDecimal paymentSrvAmount = BigDecimal.ZERO;
        BigDecimal totalSrvPrice = BigDecimal.ZERO;
        int srvIdx = 2;

        ResponseContext(ChangeServiceRspGo7Dto changeServiceRsp,
                OrderRetrieveRspGo7Dto bookingRsp,
                com.airlines.go7api.requestdto.ChangeServiceReqDto requestDto,
                Map<String, BigDecimal> actualPrices,
                Map<String, String> passengerSeatsMap,
                Map<String, String> passengerServicesMap) {
            this.changeServiceRsp = changeServiceRsp;
            this.bookingRsp = bookingRsp;
            this.requestDto = requestDto;
            this.actualPrices = actualPrices;
            this.passengerSeatsMap = passengerSeatsMap;
            this.passengerServicesMap = passengerServicesMap;

            this.booking = Objects.requireNonNull(bookingRsp.getAerocrs().getBooking());
            this.response = new ChangeServiceRspDto();

            List<Flight> tempFlightList = null;
            if (booking.getFlights() != null && booking.getFlights().getFlight() != null) {
                tempFlightList = booking.getFlights().getFlight();
            } else if (booking.getItems() != null) {
                tempFlightList = booking.getItems().getFlight();
            }
            this.flightList = tempFlightList;
        }

        ChangeServiceRspDto buildResponse() {
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
            response.setTotalOrderPrice(BigDecimal.ZERO);

            totalTax = BigDecimal.ZERO;
            if (flightList != null) {
                for (Flight f : flightList) {
                    if (f.getTotaltaxes() > 0) {
                        totalTax = totalTax.add(BigDecimal.valueOf(f.getTotaltaxes()));
                    }
                }
            }

            carrierCode = (changeServiceRsp != null && changeServiceRsp.getAerocrs() != null
                    && changeServiceRsp.getAerocrs().getCompanycode() != null)
                            ? changeServiceRsp.getAerocrs().getCompanycode()
                            : "G7";

            if ("G7".equals(carrierCode) && flightList != null && !flightList.isEmpty()) {
                String desig = flightList.get(0).getAirlinedesignator();
                if (desig != null) {
                    carrierCode = desig;
                }
            }

            response.setApiOwner(carrierCode);
            response.setPnr(booking.getPnrref());
            response.setOrderId(booking.getBookingconfirmation() != null ? booking.getBookingconfirmation()
                    : String.valueOf(booking.getBookingid()));
            response.setValidatingCarrier(carrierCode);
            response.setCurrency(booking.getCurrency() != null ? booking.getCurrency() : "USD");
        }

        private void populateOdsAndPriceClasses() {
            if (flightList != null) {
                int segId = 1;
                int odId = 1;
                for (Flight flight : flightList) {
                    processSingleFlight(flight, segId, odId);
                    segId++;
                    odId++;
                }
            }
            response.setOds(ods);
            response.setPriceClassList(priceClassList);
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

            List<PriceClass.Description> descs = new ArrayList<>();
            PriceClass.Description d = new PriceClass.Description();
            d.setText("Standard Service");
            d.setOdKey("OD" + odId);
            descs.add(d);
            pc.setDescriptions(descs);
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

        private String determineCabinCode(String code) {
            if ("F".equals(code) || "A".equals(code) || "P".equals(code)) {
                return "First";
            }
            if ("C".equals(code) || "J".equals(code) || "D".equals(code) || "Z".equals(code) || "I".equals(code)) {
                return "Business";
            }
            return ECONOMY_CLASS;
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

        private void populatePassengerDetails() {
            List<BookingReferences> refs = new ArrayList<>();
            BookingReferences ref = new BookingReferences();
            ref.setId(booking.getPnrref());
            ref.setAirlineId(carrierCode);
            refs.add(ref);
            response.setBookingReferences(refs);

            String defaultIssuingPlace = null;
            String defaultIssuingAirline = null;
            if (flightList != null && !flightList.isEmpty()) {
                if (flightList.get(0).getFromcode() != null) {
                    defaultIssuingPlace = flightList.get(0).getFromcode();
                }
                if (flightList.get(0).getAirline() != null) {
                    defaultIssuingAirline = flightList.get(0).getAirline();
                }
            }

            isPaymentProvided = (requestDto != null && requestDto.getPaymentInformation() != null);
            srvTotalFromReq = isPaymentProvided && requestDto.getPaymentInformation().getAmount() != null
                    ? requestDto.getPaymentInformation().getAmount()
                    : BigDecimal.ZERO;

            buildPaxToRequestedServicesMap();

            if (booking.getPassengers() != null && booking.getPassengers().getPassenger() != null) {
                int paxCounter = 1;
                List<PaxDetailDTO> adtList = new ArrayList<>();
                for (Passenger p : booking.getPassengers().getPassenger()) {
                    processSinglePassenger(p, paxCounter, adtList, defaultIssuingPlace, defaultIssuingAirline);
                    paxCounter++;
                }
            }
            response.setPaxDetailList(paxList);

            if (isPaymentProvided) {
                response.setTicketDocInfoList(topTicketDocInfos);
                response.setEmdInfoList(topEmdInfos);
            } else {
                response.setTicketDocInfoList(null);
                response.setEmdInfoList(null);
            }
        }

        private void buildPaxToRequestedServicesMap() {
            if (requestDto != null && requestDto.getOffers() != null) {
                for (com.airlines.go7api.requestdto.common.ChangeOfferReqDto offer : requestDto.getOffers()) {
                    processOffer(offer);
                }
            }
        }

        private void processOffer(com.airlines.go7api.requestdto.common.ChangeOfferReqDto offer) {
            if (offer.getOfferItems() != null) {
                for (com.airlines.go7api.requestdto.common.ChangeOfferReqDto.OfferItemDto item : offer.getOfferItems()) {
                    if (item.getPaxRefs() != null && item.getOfferItemId() != null) {
                        for (String paxRef : item.getPaxRefs()) {
                            paxToRequestedServices.computeIfAbsent(paxRef, k -> new ArrayList<>()).add(item.getOfferItemId());
                        }
                    }
                }
            }
        }

        private String assignPassengerIdAndLinkInfant(String assignedPtc, PaxDetailDTO pax, List<PaxDetailDTO> adtList, int paxCounter) {
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
                    pid = "T" + paxCounter + ".1";
                }
            } else {
                pid = "T" + paxCounter;
                if ("ADT".equals(assignedPtc)) {
                    adtList.add(pax);
                }
            }
            return pid;
        }

        private void populatePassengerEmail(Passenger p, PaxDetailDTO pax) {
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

        private void collectOfferItemIds(com.airlines.go7api.requestdto.common.ChangeOfferReqDto offer, List<String> srvRefs) {
            if (offer.getOfferItems() != null) {
                for (com.airlines.go7api.requestdto.common.ChangeOfferReqDto.OfferItemDto item : offer.getOfferItems()) {
                    if (item.getOfferItemId() != null) {
                        srvRefs.add(item.getOfferItemId());
                    }
                }
            }
        }

        private List<String> determineServiceRefs(int flightIdx, String pid) {
            List<String> srvRefs = new ArrayList<>();
            if (requestDto.getOffers() != null && !requestDto.getOffers().isEmpty()) {
                for (com.airlines.go7api.requestdto.common.ChangeOfferReqDto offer : requestDto.getOffers()) {
                    collectOfferItemIds(offer, srvRefs);
                }
            } else {
                srvRefs.add("SEG" + flightIdx + "_" + pid + "_SRV");
            }
            return srvRefs;
        }

        private EMDInfoDTO buildEmd(Flight f, Passenger p, String pid, int flightIdx, String defaultIssuingPlace, String defaultIssuingAirline) {
            EMDInfoDTO emd = new EMDInfoDTO();
            emd.setValidatingCarrier(carrierCode);
            emd.setPaxId(Arrays.asList(pid));
            emd.setIssuingAirlineName(defaultIssuingAirline);
            emd.setIssuingPlace(defaultIssuingPlace);

            List<EMDInfoDTO.TicketDocumentDTO> emdDocs = new ArrayList<>();
            EMDInfoDTO.TicketDocumentDTO emdDoc = new EMDInfoDTO.TicketDocumentDTO();
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
            emdDoc.setDateOfIssue(OrderMappingUtil.formatCurrentDate());
            emdDoc.setTimeOfIssue("00:00");
            emdDoc.setTicketingLocation(emd.getIssuingPlace());
            emdDoc.setReportingType("BSP");

            List<EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO> emdCoupons = new ArrayList<>();
            EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO emdCoupon = new EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO();
            emdCoupon.setCouponNumber(1);
            emdCoupon.setValidatingCarrier(carrierCode);
            emdCoupon.setStatus("I");

            emdCoupon.setServiceRefs(determineServiceRefs(flightIdx, pid));

            List<EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO> caiList = new ArrayList<>();
            EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO cai = new EMDInfoDTO.TicketDocumentDTO.CouponInfoDTO.CurrentAirlineInfoDTO();
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
            cai.setOperatingCarrierAirlineId(f.getAirlinedesignator());
            cai.setMarketingCarrierName(f.getAirline());
            cai.setOperatingCarrierName(f.getAirline() != null ? f.getAirline().toUpperCase() : "");
            cai.setFlightNumber(f.getNumber());
            cai.setEquipmentAircraftCode(f.getAircraftTypeIataCode() != null ? f.getAircraftTypeIataCode()
                    : f.getAircraftType());

            caiList.add(cai);
            emdCoupon.setCurrentAirlineInfo(caiList);

            emdCoupons.add(emdCoupon);
            emdDoc.setCouponInfo(emdCoupons);
            emdDocs.add(emdDoc);
            emd.setTicketDocument(emdDocs);
            return emd;
        }

        private void populatePassengerEmds(Passenger p, PaxDetailDTO pax, String pid, String defaultIssuingPlace, String defaultIssuingAirline) {
            if (isPaymentProvided && flightList != null && !flightList.isEmpty()) {
                List<EMDInfoDTO> paxEmds = new ArrayList<>();
                int flightIdx = 1;
                for (Flight f : flightList) {
                    EMDInfoDTO emd = buildEmd(f, p, pid, flightIdx, defaultIssuingPlace, defaultIssuingAirline);
                    paxEmds.add(emd);
                    topEmdInfos.add(emd);
                    flightIdx++;
                }
                if (!paxEmds.isEmpty()) {
                    pax.setEmdInfo(paxEmds);
                }
            }
        }

        private void processSinglePassenger(Passenger p, int paxCounter, List<PaxDetailDTO> adtList, String defaultIssuingPlace, String defaultIssuingAirline) {
            PaxDetailDTO pax = new PaxDetailDTO();
            String rawTitle = p.getPaxtitle() != null ? p.getPaxtitle().toUpperCase().replace(".", "") : "MR";
            boolean isInfantByTitle = rawTitle.contains("INF");

            String assignedPtc = OrderMappingUtil.mapPaxType(p.getPaxtype());
            if (isInfantByTitle) {
                assignedPtc = "INF";
            }

            String pid = assignPassengerIdAndLinkInfant(assignedPtc, pax, adtList, paxCounter);
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

            populatePassengerEmail(p, pax);

            if (isPaymentProvided && p.getETickets() != null && p.getETickets().getFlight() != null) {
                populatePassengerTicketDocs(p, pax, pid, defaultIssuingPlace, defaultIssuingAirline);
            }

            populatePassengerEmds(p, pax, pid, defaultIssuingPlace, defaultIssuingAirline);

            paxList.add(pax);
        }

        private void addAncillaryPrice(Aerocrs.Detail detail) {
            if (detail.getAncillary() != null) {
                Object priceObj = detail.getAncillary().get("totalprice");
                if (priceObj != null) {
                    try {
                        actualSrvPrice = actualSrvPrice.add(new BigDecimal(priceObj.toString()));
                    } catch (Exception e) {
                        // Ignore parsing exception
                    }
                }
            }
        }

        private void calculateActualSrvPrice() {
            actualSrvPrice = BigDecimal.ZERO;
            if (changeServiceRsp != null && changeServiceRsp.getAerocrs() != null
                    && changeServiceRsp.getAerocrs().getDetails() != null) {
                for (Aerocrs.Detail detail : changeServiceRsp.getAerocrs().getDetails()) {
                    addAncillaryPrice(detail);
                }
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
            BigDecimal sum = BigDecimal.ZERO;
            if (flightList != null) {
                for (Flight f : flightList) {
                    BigDecimal fBase = getFlightBasePrice(f);
                    sum = sum.add(fBase).add(BigDecimal.valueOf(f.getTotaltaxes()));
                }
            }
            return sum;
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
            BigDecimal sum = BigDecimal.ZERO;
            if (flightList != null) {
                for (Flight f : flightList) {
                    sum = sum.add(getFlightInvPricing(f));
                }
            }
            return sum;
        }

        private void calculateAirItemPrice() {
            BigDecimal sumFlightPrice = calculateSumFlightPrice();
            BigDecimal invPricingBasis = calculateInvPricingBasis();
            airItemPrice = (invPricingBasis.compareTo(BigDecimal.ZERO) > 0) ? invPricingBasis : sumFlightPrice;
            airItemPrice = airItemPrice.setScale(2, java.math.RoundingMode.HALF_UP);
        }

        private void calculatePnrTotal() {
            pnrTotal = BigDecimal.ZERO;
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

        private void calculatePaymentSrvAmount() {
            if (isPaymentProvided && requestDto.getPaymentInformation().getAmount() != null) {
                paymentSrvAmount = requestDto.getPaymentInformation().getAmount();
            } else {
                paymentSrvAmount = actualSrvPrice;
            }

            if (actualSrvPrice.compareTo(BigDecimal.ZERO) <= 0) {
                if (pnrTotal.compareTo(airItemPrice) > 0) {
                    actualSrvPrice = pnrTotal.subtract(airItemPrice);
                } else if (isPaymentProvided && paymentSrvAmount.compareTo(BigDecimal.ZERO) > 0) {
                    actualSrvPrice = paymentSrvAmount;
                } else {
                    actualSrvPrice = paymentSrvAmount;
                }
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
            }
            if (!airTaxes.isEmpty()) {
                airItem.setTaxes(airTaxes);
            }
        }

        private void populateAirItemServices(OrderItemsDTO airItem) {
            if (flightList != null) {
                for (Flight f : flightList) {
                    String flightKey = (f.getNumber() != null ? f.getNumber() : "") + "_"
                            + (f.getFromcode() != null ? f.getFromcode() : "") + "_"
                            + (f.getTocode() != null ? f.getTocode() : "");
                    String segmentId = flightSegmentMap.getOrDefault(flightKey, "SEG1");
                    for (String pid : rawPaxIds) {
                        Service srv = new Service();
                        srv.setServiceId(segmentId + "_" + pid);
                        srv.setServiceStatus(STATUS_CONFIRMED);
                        flightServices.add(srv);
                    }
                }
            }
            airItem.setServiceList(flightServices);
        }

        private BigDecimal lookupCachedServicePrice(BigDecimal currentPrice) {
            if (currentPrice.compareTo(BigDecimal.ZERO) == 0 && passengerServicesMap != null) {
                String assignedPid = rawPaxIds.isEmpty() ? "T1" : rawPaxIds.get(0);
                String rawCached = passengerServicesMap.get(assignedPid);
                if (rawCached != null && rawCached.contains("|")) {
                    String[] parts = rawCached.split("\\|");
                    try {
                        BigDecimal storedPrice = new BigDecimal(parts[1]);
                        if (storedPrice.compareTo(BigDecimal.ZERO) > 0) {
                            return storedPrice;
                        }
                    } catch (Exception e) {
                        // Ignore parsing exception
                    }
                }
            }
            return currentPrice;
        }

        private BigDecimal determineAncillaryPrice(Aerocrs.Detail detail) {
            BigDecimal srvPrice = BigDecimal.ZERO;
            Object priceObj = detail.getAncillary().get("totalprice");
            if (priceObj != null) {
                try {
                    srvPrice = new BigDecimal(priceObj.toString());
                } catch (Exception e) {
                    // Ignore parsing exception
                }
            }

            if (srvPrice.compareTo(BigDecimal.ZERO) <= 0) {
                Object itemIdObj = detail.getAncillary().get("itemid");
                if (itemIdObj != null && actualPrices != null && actualPrices.containsKey(itemIdObj.toString())) {
                    srvPrice = actualPrices.get(itemIdObj.toString());
                }
            }

            if (srvPrice.compareTo(BigDecimal.ZERO) <= 0) {
                srvPrice = actualSrvPrice;
            }

            return lookupCachedServicePrice(srvPrice);
        }

        private String determineAncillaryCode(Aerocrs.Detail detail) {
            String srvCode = "SRV";
            if (detail.getAncillary().get("code") != null) {
                srvCode = detail.getAncillary().get("code").toString();
            } else if (detail.getAncillary().get("item_name") != null) {
                srvCode = detail.getAncillary().get("item_name").toString();
            }

            if ("SRV".equals(srvCode) && !paxToRequestedServices.isEmpty()) {
                String primaryPax = rawPaxIds.isEmpty() ? "T1" : rawPaxIds.get(0);
                List<String> requestedSrvs = paxToRequestedServices.get(primaryPax);
                if (requestedSrvs != null && !requestedSrvs.isEmpty()) {
                    srvCode = requestedSrvs.get(0);
                }
            }
            return srvCode;
        }

        private void processAncillaryDetails() {
            if (changeServiceRsp != null && changeServiceRsp.getAerocrs() != null
                    && changeServiceRsp.getAerocrs().getDetails() != null) {
                for (Aerocrs.Detail detail : changeServiceRsp.getAerocrs().getDetails()) {
                    if (detail.getAncillary() != null) {
                        processSingleAncillary(detail);
                    }
                }
            }
        }

        private void processSingleAncillary(Aerocrs.Detail detail) {
            OrderItemsDTO srvItem = new OrderItemsDTO();
            srvItem.setOrderItemId(response.getOrderId() + "_SRV" + srvIdx++);
            srvItem.setPtc("ADT");

            BigDecimal srvPrice = determineAncillaryPrice(detail);
            String srvCurrency = response.getCurrency();
            Object currObj = detail.getAncillary().get("currency");
            if (currObj != null) {
                srvCurrency = currObj.toString();
            }

            srvItem.setBaseFare(new OrderItemsDTO.BaseFare(srvPrice, srvCurrency));
            srvItem.setTotalTax(new OrderItemsDTO.TotalTax(BigDecimal.ZERO, srvCurrency));
            srvItem.setTotalFare(new OrderItemsDTO.TotalFare(srvPrice, srvCurrency));
            srvItem.setTotalPrice(srvPrice);
            srvItem.setPassengerIds(Arrays.asList(rawPaxIds.isEmpty() ? "T1" : rawPaxIds.get(0)));

            List<Service> srvList = new ArrayList<>();
            Service srv = new Service();

            String srvName = detail.getAncillary().get("name") != null
                    ? detail.getAncillary().get("name").toString()
                    : ANCILLARY_SERVICE_NAME;
            String srvCode = determineAncillaryCode(detail);

            srv.setServiceId("SRV_" + detail.getInvid());
            srv.setServiceStatus(detail.isSuccess() ? STATUS_CONFIRMED : "PENDING");
            srv.setServiceCode(srvCode);
            srv.setServiceName(srvName);

            srvList.add(srv);
            srvItem.setServiceList(srvList);
            orderItems.add(srvItem);
            totalSrvPrice = totalSrvPrice.add(srvPrice);
            processedSrvKeys.add((rawPaxIds.isEmpty() ? "T1" : rawPaxIds.get(0)) + "_" + srvCode);
        }

        private void processFallbackService(String pid, String srvRef, BigDecimal pricePerSrv) {
            OrderItemsDTO srvItem = new OrderItemsDTO();
            srvItem.setOrderItemId(response.getOrderId() + "_SRV" + srvIdx++);
            srvItem.setPtc("ADT");

            String srvCurrency = response.getCurrency();
            BigDecimal srvPrice = pricePerSrv;
            if (srvPrice.compareTo(BigDecimal.ZERO) <= 0 && actualPrices != null && actualPrices.containsKey(srvRef)) {
                srvPrice = actualPrices.get(srvRef);
            }
            srvItem.setBaseFare(new OrderItemsDTO.BaseFare(srvPrice, srvCurrency));
            srvItem.setTotalTax(new OrderItemsDTO.TotalTax(BigDecimal.ZERO, srvCurrency));
            srvItem.setTotalFare(new OrderItemsDTO.TotalFare(pricePerSrv, srvCurrency));
            srvItem.setTotalPrice(pricePerSrv);
            String safePid;
            if (rawPaxIds.contains(pid)) {
                safePid = pid;
            } else if (rawPaxIds.isEmpty()) {
                safePid = "T1";
            } else {
                safePid = rawPaxIds.get(0);
            }
            srvItem.setPassengerIds(Arrays.asList(safePid));

            List<Service> srvList = new ArrayList<>();
            Service srv = new Service();
            srv.setServiceId("SRV_" + srvRef);
            srv.setServiceStatus(STATUS_CONFIRMED);
            srv.setServiceCode(srvRef);
            srv.setServiceName(ANCILLARY_SERVICE_NAME);

            srvList.add(srv);
            srvItem.setServiceList(srvList);
            orderItems.add(srvItem);
            totalSrvPrice = totalSrvPrice.add(pricePerSrv);
            processedSrvKeys.add(safePid + "_" + srvRef);
        }

        private int countUnprocessedServices() {
            int count = 0;
            if (passengerServicesMap != null) {
                for (Map.Entry<String, String> entry : passengerServicesMap.entrySet()) {
                    if (!processedSrvKeys.contains(entry.getKey() + "_" + entry.getValue())) {
                        count++;
                    }
                }
            }
            return count;
        }

        private int countUnprocessedSplitServices() {
            int count = 0;
            if (passengerServicesMap != null) {
                for (Map.Entry<String, String> entry : passengerServicesMap.entrySet()) {
                    String srvCode = entry.getValue();
                    if (srvCode != null && srvCode.contains("|")) {
                        srvCode = srvCode.split("\\|")[0];
                    }
                    if (!processedSrvKeys.contains(entry.getKey() + "_" + srvCode)) {
                        count++;
                    }
                }
            }
            return count;
        }

        private int countCachedItems() {
            int cachedCount = countUnprocessedServices();
            if (passengerSeatsMap != null) {
                cachedCount += passengerSeatsMap.size();
            }
            cachedCount += countUnprocessedSplitServices();
            return cachedCount;
        }

        private void processSingleCachedService(Map.Entry<String, String> entry, BigDecimal distributedCachedPrice) {
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

            if (!processedSrvKeys.contains(pId + "_" + srvCode)) {
                OrderItemsDTO srvItem = new OrderItemsDTO();
                srvItem.setOrderItemId(response.getOrderId() + "_SRV" + srvIdx++);
                srvItem.setPassengerIds(Arrays.asList(pId));

                srvItem.setTotalPrice(srvPrice);
                srvItem.setBaseFare(new OrderItemsDTO.BaseFare(srvPrice, response.getCurrency()));
                srvItem.setTotalFare(new OrderItemsDTO.TotalFare(srvPrice, response.getCurrency()));

                List<Service> srvListList = new ArrayList<>();
                Service srv = new Service();
                srv.setServiceId(srvCode);
                srv.setServiceStatus(STATUS_CONFIRMED);
                srv.setServiceCode(srvCode);
                srv.setServiceName(ANCILLARY_SERVICE_NAME);
                srvListList.add(srv);
                srvItem.setServiceList(srvListList);

                orderItems.add(srvItem);
                totalSrvPrice = totalSrvPrice.add(srvPrice);
            }
        }

        private void addCachedServices(BigDecimal distributedCachedPrice) {
            if (passengerServicesMap != null) {
                for (Map.Entry<String, String> entry : passengerServicesMap.entrySet()) {
                    processSingleCachedService(entry, distributedCachedPrice);
                }
            }
        }

        private void addCachedSeats(BigDecimal distributedCachedPrice) {
            if (passengerSeatsMap != null) {
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
                    totalSrvPrice = totalSrvPrice.add(seatPrice);
                }
            }
        }

        private boolean isDetailsProvided() {
            if (changeServiceRsp != null && changeServiceRsp.getAerocrs() != null
                    && changeServiceRsp.getAerocrs().getDetails() != null) {
                for (Aerocrs.Detail detail : changeServiceRsp.getAerocrs().getDetails()) {
                    if (detail.getAncillary() != null) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void processFallbackServicesIfNeeded(boolean detailsProvided) {
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
                        processFallbackService(pid, srvRef, pricePerSrv);
                    }
                }
            }
        }

        private BigDecimal calculateDistributedCachedPrice() {
            BigDecimal currentSecondaryPrice = totalSrvPrice;
            BigDecimal totalSecondaryNeeded = pnrTotal.subtract(airItemPrice);
            BigDecimal leftoverPrice = totalSecondaryNeeded.subtract(currentSecondaryPrice);
            if (leftoverPrice.compareTo(BigDecimal.ZERO) < 0) {
                leftoverPrice = BigDecimal.ZERO;
            }

            int cachedCount = countCachedItems();
            if (cachedCount > 0 && leftoverPrice.compareTo(BigDecimal.ZERO) > 0) {
                return leftoverPrice.divide(new BigDecimal(cachedCount), 2, java.math.RoundingMode.HALF_UP);
            }
            return BigDecimal.ZERO;
        }

        private void populateSecondaryOrderItems() {
            boolean detailsProvided = isDetailsProvided();
            processAncillaryDetails();
            processFallbackServicesIfNeeded(detailsProvided);

            BigDecimal distributedCachedPrice = calculateDistributedCachedPrice();
            addCachedServices(distributedCachedPrice);
            addCachedSeats(distributedCachedPrice);
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

            calculateActualSrvPrice();
            calculateAirItemPrice();
            calculatePnrTotal();
            calculatePaymentSrvAmount();

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

            populateSecondaryOrderItems();

            response.setOrderItems(orderItems);
            response.setTotalOrderPrice(airItemPrice.add(totalSrvPrice).setScale(2, java.math.RoundingMode.HALF_UP));
        }

        private List<String> collectSrvRefs() {
            List<String> srvRefs = new ArrayList<>();
            for (OrderItemsDTO item : orderItems) {
                if (item.getOrderItemId() != null && item.getOrderItemId().contains("_SRV")) {
                    srvRefs.add(item.getOrderItemId());
                }
            }
            return srvRefs;
        }

        private PaymentsDTO buildPaySrv(String pType) {
            PaymentsDTO paySrv = new PaymentsDTO();
            paySrv.setType(pType);
            paySrv.setStatusCode("SUCCESSFUL");
            paySrv.setCurrency(response.getCurrency());

            paySrv.setAmount(paymentSrvAmount.setScale(2, java.math.RoundingMode.HALF_UP));
            if (paymentSrvAmount.compareTo(BigDecimal.ZERO) <= 0 && totalSrvPrice.compareTo(BigDecimal.ZERO) > 0) {
                paySrv.setAmount(totalSrvPrice.setScale(2, java.math.RoundingMode.HALF_UP));
            }
            paySrv.setOrderItem(collectSrvRefs());
            return paySrv;
        }

        private void populatePayments() {
            if (isPaymentProvided) {
                List<PaymentsDTO> payments = new ArrayList<>();
                String pType = requestDto.getPaymentType() != null ? requestDto.getPaymentType() : "CA";

                if (paymentSrvAmount.compareTo(BigDecimal.ZERO) > 0 || actualSrvPrice.compareTo(BigDecimal.ZERO) > 0) {
                    payments.add(buildPaySrv(pType));
                }
                response.setPayments(payments);
            } else {
                response.setPayments(null);
            }
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
}