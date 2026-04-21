package com.airlines.GO7API.response;

import com.airlines.GO7API.requestDto.ServiceListReqDto;
import com.airlines.GO7API.responseGo7.ServiceListRspGo7Dto;
import com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto;
import com.airlines.GO7API.responseDto.ServiceListRspDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ServiceListResponse {

    public ServiceListRspDto generateResponse(ServiceListRspGo7Dto go7Response, ServiceListReqDto requestDto,
            OrderRetrieveRspGo7Dto bookingRsp) {
        ServiceListRspDto response = new ServiceListRspDto();

        if (go7Response == null || go7Response.getAerocrs() == null
                || go7Response.getAerocrs().getAncillaries() == null) {
            return response;
        }

        response.setResponseId(UUID.randomUUID().toString().toUpperCase());
        // OfferID construction: ResponseID + "-1"
        String offerId = response.getResponseId() + "-1";
        response.setOfferId(offerId);

        response.setApiOwner("G7");
        response.setValidatingCarrier("G7");

        List<ServiceListRspGo7Dto.Ancillary> ancillaries = go7Response.getAerocrs().getAncillaries().getAncillary();

        ServiceListRspDto.OfferItem offerItem = new ServiceListRspDto.OfferItem();
        List<ServiceListRspDto.OfferItem.Baggage> baggageList = new ArrayList<>();
        List<ServiceListRspDto.OfferItem.OtherService> otherServiceList = new ArrayList<>();

        // Extract Passenger Refs from Booking
        List<String> paxRefs = new ArrayList<>();
        List<String> paxNames = new ArrayList<>();
        List<String> segmentRefs = new ArrayList<>();

        if (bookingRsp != null && bookingRsp.getAerocrs() != null && bookingRsp.getAerocrs().getBooking() != null) {
            if (bookingRsp.getAerocrs().getBooking().getPassengers() != null &&
                    bookingRsp.getAerocrs().getBooking().getPassengers().getPassenger() != null) {
                int pCounter = 1;
                List<String> adtRefs = new ArrayList<>();
                for (OrderRetrieveRspGo7Dto.Passenger pax : bookingRsp.getAerocrs().getBooking().getPassengers()
                        .getPassenger()) {
                    
                    String rawTitle = pax.getPaxtitle() != null ? pax.getPaxtitle().toUpperCase().replace(".", "") : "MR";
                    boolean isInfantByTitle = rawTitle.contains("INF");

                    String assignedPtc = "ADT";
                    if ("CHILD".equalsIgnoreCase(pax.getPaxtype())) assignedPtc = "CNN";
                    if ("INFANT".equalsIgnoreCase(pax.getPaxtype()) || isInfantByTitle) assignedPtc = "INF";

                    String pRef;
                    if ("INF".equals(assignedPtc)) {
                        if (!adtRefs.isEmpty()) {
                            pRef = adtRefs.get(adtRefs.size() - 1) + ".1";
                        } else {
                            pRef = "T" + pCounter++ + ".1";
                        }
                    } else {
                        pRef = "T" + pCounter++;
                        if ("ADT".equals(assignedPtc)) {
                            adtRefs.add(pRef);
                        }
                    }

                    paxRefs.add(pRef);
                    paxNames.add((pax.getFirstname() != null ? pax.getFirstname() : "").toUpperCase());
                }
            }

            // Extract Segments
            if (bookingRsp.getAerocrs().getBooking().getItems() != null &&
                    bookingRsp.getAerocrs().getBooking().getItems().getFlight() != null) {
                int sCounter = 1;
                for (OrderRetrieveRspGo7Dto.Flight flt : bookingRsp.getAerocrs().getBooking().getItems().getFlight()) {
                    segmentRefs.add("S" + sCounter++);
                }
            } else if (bookingRsp.getAerocrs().getBooking().getFlights() != null &&
                    bookingRsp.getAerocrs().getBooking().getFlights().getFlight() != null) {
                int sCounter = 1;
                for (OrderRetrieveRspGo7Dto.Flight flt : bookingRsp.getAerocrs().getBooking().getFlights()
                        .getFlight()) {
                    segmentRefs.add("S" + sCounter++);
                }
            }
        }

        // Fallback
        if (paxRefs.isEmpty()) {
            paxRefs.add("T1");
            paxNames.add("UNKNOWN");
        }
        if (segmentRefs.isEmpty()) {
            segmentRefs.add("S1");
        }

        int itemCounter = 1;
        int serviceCounter = 1;

        if (ancillaries != null) {
            for (ServiceListRspGo7Dto.Ancillary anc : ancillaries) {
                if (anc.getItems() != null) {
                    for (ServiceListRspGo7Dto.Item item : anc.getItems()) {
                        boolean isBaggage = isBaggageGroup(anc.getGroupname())
                                || anc.getName().toLowerCase().contains("baggage");

                        // Construct IDs
                        String offerItemId = item.getItemid() != null && !item.getItemid().isEmpty() ? item.getItemid()
                                : offerId + "-" + (++itemCounter);
                        String serviceId = offerItemId + "-" + serviceCounter;

                        if (isBaggage) {
                            ServiceListRspDto.OfferItem.Baggage bag = new ServiceListRspDto.OfferItem.Baggage();
                            bag.setOfferItemId(offerItemId);
                            bag.setPassengerRefs(paxRefs);
                            bag.setGivenName(paxNames);
                            bag.setSegmentRefId(segmentRefs);
                            bag.setCurrency(item.getCurrency());

                            // Amounts
                            BigDecimal amount = safeDecimal(item.getFare());

                            bag.setBaseAmount(amount);
                            bag.setTotalAmount(amount);

                            // Service Def
                            ServiceListRspDto.OfferItem.Baggage.Service svc = new ServiceListRspDto.OfferItem.Baggage.Service();
                            svc.setServiceId(serviceId);

                            ServiceListRspDto.OfferItem.Baggage.Service.ServiceDefinationRef def = new ServiceListRspDto.OfferItem.Baggage.Service.ServiceDefinationRef();
                            // Keep 'WY' for service def ID as per initial instruction, or 'G7'?
                            // User asked to "get fields from ... and set it into ndc style".
                            // The user specifically asked to avoid hardcoding "T1", "PRATIK", "S2".
                            // The user also asked earlier "apiOwner": "WY", "validatingCarrier": "WY" as
                            // G7.
                            // I should stick to G7 for these IDs as per previous step.
                            String subCode = generateSubCode(item.getItemname());
                            String rfic = "C";
                            String type = "6";
                            String code = "OC";

                            def.setServiceDefId(String.format("%s-%s-%s-BG-%s-G7", rfic, code, subCode, "05"));
                            def.setName(item.getItemname());

                            ServiceListRspDto.OfferItem.Baggage.Service.ServiceDefinationRef.Encoding enc = new ServiceListRspDto.OfferItem.Baggage.Service.ServiceDefinationRef.Encoding();
                            enc.setRfic(rfic);
                            enc.setType(type);
                            enc.setCode(code);
                            enc.setSubCode(subCode);
                            def.setEncoding(enc);

                            List<ServiceListRspDto.OfferItem.Baggage.Service.ServiceDefinationRef.Description> descs = new ArrayList<>();
                            descs.add(new ServiceListRspDto.OfferItem.Baggage.Service.ServiceDefinationRef.Description(
                                    item.getItemname(), "Details"));
                            descs.add(new ServiceListRspDto.OfferItem.Baggage.Service.ServiceDefinationRef.Description(
                                    "Surcharge", "Type"));
                            def.setDescriptions(descs);

                            svc.setServiceDefinationRef(def);
                            bag.setService(svc);

                            baggageList.add(bag);

                        } else {
                            // Other Service
                            ServiceListRspDto.OfferItem.OtherService other = new ServiceListRspDto.OfferItem.OtherService();
                            other.setOfferItemId(offerItemId);
                            other.setPassengerRefs(paxRefs);
                            other.setGivenName(paxNames);
                            other.setSegmentRefId(segmentRefs);
                            other.setCurrency(item.getCurrency());

                            BigDecimal amount = safeDecimal(item.getFare());
                            other.setBaseAmount(amount);
                            other.setTotalAmount(amount);

                            ServiceListRspDto.OfferItem.OtherService.Service svc = new ServiceListRspDto.OfferItem.OtherService.Service();
                            svc.setServiceId(serviceId);

                            ServiceListRspDto.OfferItem.OtherService.Service.ServiceDefinationRef def = new ServiceListRspDto.OfferItem.OtherService.Service.ServiceDefinationRef();
                            // Construct ServiceDefID
                            String subCode = generateSubCode(item.getItemname());
                            String rfic = isLounge(item.getItemname()) ? "E" : "F";
                            String type = "6";
                            String code = "OC";

                            def.setServiceDefId(String.format("%s-%s-%s-G7", rfic, code, subCode));
                            def.setName(item.getItemname());

                            ServiceListRspDto.OfferItem.OtherService.Service.ServiceDefinationRef.Encoding enc = new ServiceListRspDto.OfferItem.OtherService.Service.ServiceDefinationRef.Encoding();
                            enc.setRfic(rfic);
                            enc.setType(type);
                            enc.setCode(code);
                            enc.setSubCode(subCode);
                            def.setEncoding(enc);

                            List<ServiceListRspDto.OfferItem.OtherService.Service.ServiceDefinationRef.Description> descs = new ArrayList<>();
                            descs.add(
                                    new ServiceListRspDto.OfferItem.OtherService.Service.ServiceDefinationRef.Description(
                                            item.getItemname(), "Details"));
                            descs.add(
                                    new ServiceListRspDto.OfferItem.OtherService.Service.ServiceDefinationRef.Description(
                                            "Surcharge", "Type"));
                            def.setDescriptions(descs);

                            svc.setServiceDefinationRef(def);
                            other.setService(svc);

                            otherServiceList.add(other);
                        }
                    }
                }
            }
        }

        offerItem.setBaggageList(baggageList);
        offerItem.setOtherServiceList(otherServiceList);

        List<ServiceListRspDto.OfferItem> offerItems = new ArrayList<>();
        offerItems.add(offerItem);
        response.setOfferItem(offerItems);

        response.setQuantity(String.valueOf(baggageList.size() + otherServiceList.size()));

        return response;
    }

    private boolean isBaggageGroup(String group) {
        return group != null && (group.toLowerCase().contains("baggage") || group.toLowerCase().contains("luggage"));
    }

    private boolean isLounge(String name) {
        return name != null && name.toLowerCase().contains("lounge");
    }

    private String generateSubCode(String name) {
        if (name == null)
            return "XXX";
        String clean = name.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (clean.length() >= 3)
            return clean.substring(0, 3);
        return clean + "X";
    }

    private BigDecimal safeDecimal(String val) {
        if (val == null)
            return BigDecimal.ZERO;
        try {
            return new BigDecimal(val);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
