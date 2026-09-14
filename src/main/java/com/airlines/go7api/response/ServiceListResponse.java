package com.airlines.go7api.response;

import com.airlines.go7api.responsego7.common.*;

import com.airlines.go7api.responsedto.common.*;

import com.airlines.go7api.responsego7.ServiceListRspGo7Dto;
import com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto;
import com.airlines.go7api.responsedto.ServiceListRspDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component

public class ServiceListResponse {

    public ServiceListRspDto generateResponse(ServiceListRspGo7Dto go7Response,
            OrderRetrieveRspGo7Dto bookingRsp) {
        ServiceListRspDto response = new ServiceListRspDto();

        if (go7Response == null || go7Response.getAerocrs() == null
                || go7Response.getAerocrs().getAncillaries() == null) {
            return response;
        }

        response.setResponseId(UUID.randomUUID().toString().toUpperCase());
        String offerId = response.getResponseId() + "-1";
        response.setOfferId(offerId);

        response.setApiOwner("G7");
        response.setValidatingCarrier("G7");

        List<ServiceListRspGo7Dto.Ancillary> ancillaries = go7Response.getAerocrs().getAncillaries().getAncillary();

        ServiceListRspDto.OfferItem offerItem = new ServiceListRspDto.OfferItem();
        List<ServiceListRspDto.OfferItem.Baggage> baggageList = new ArrayList<>();
        List<ServiceListRspDto.OfferItem.OtherService> otherServiceList = new ArrayList<>();

        List<String> paxRefs = new ArrayList<>();
        List<String> paxNames = new ArrayList<>();
        List<String> segmentRefs = new ArrayList<>();

        extractPassengerAndSegmentRefs(bookingRsp, paxRefs, paxNames, segmentRefs);

        MappingContext context = new MappingContext(offerId, baggageList, otherServiceList, paxRefs, paxNames, segmentRefs);
        processAncillaries(ancillaries, context);

        offerItem.setBaggageList(baggageList);
        offerItem.setOtherServiceList(otherServiceList);

        List<ServiceListRspDto.OfferItem> offerItems = new ArrayList<>();
        offerItems.add(offerItem);
        response.setOfferItem(offerItems);

        response.setQuantity(String.valueOf(baggageList.size() + otherServiceList.size()));

        return response;
    }

    private static class MappingContext {
        final String offerId;
        final List<ServiceListRspDto.OfferItem.Baggage> baggageList;
        final List<ServiceListRspDto.OfferItem.OtherService> otherServiceList;
        final List<String> paxRefs;
        final List<String> paxNames;
        final List<String> segmentRefs;
        int itemCounter = 1;
        int serviceCounter = 1;

        MappingContext(String offerId,
                       List<ServiceListRspDto.OfferItem.Baggage> baggageList,
                       List<ServiceListRspDto.OfferItem.OtherService> otherServiceList,
                       List<String> paxRefs,
                       List<String> paxNames,
                       List<String> segmentRefs) {
            this.offerId = offerId;
            this.baggageList = baggageList;
            this.otherServiceList = otherServiceList;
            this.paxRefs = paxRefs;
            this.paxNames = paxNames;
            this.segmentRefs = segmentRefs;
        }
    }

    private void processAncillaries(
            List<ServiceListRspGo7Dto.Ancillary> ancillaries,
            MappingContext context) {
        if (ancillaries != null) {
            for (ServiceListRspGo7Dto.Ancillary anc : ancillaries) {
                processAncillary(anc, context);
            }
        }
    }

    private void processAncillary(
            ServiceListRspGo7Dto.Ancillary anc,
            MappingContext context) {
        if (anc.getItems() == null) {
            return;
        }
        for (ServiceListRspGo7Dto.Item item : anc.getItems()) {
            boolean isBaggage = isBaggageGroup(anc.getGroupname())
                    || (anc.getName() != null && anc.getName().toLowerCase().contains("baggage"));

            String offerItemId = item.getItemid() != null && !item.getItemid().isEmpty() ? item.getItemid()
                    : context.offerId + "-" + (++context.itemCounter);
            String serviceId = offerItemId + "-" + context.serviceCounter;

            if (isBaggage) {
                context.baggageList.add(mapToBaggage(item, offerItemId, serviceId, context.paxRefs, context.paxNames, context.segmentRefs));
            } else {
                context.otherServiceList.add(mapToOtherService(item, offerItemId, serviceId, context.paxRefs, context.paxNames, context.segmentRefs));
            }
        }
    }

    private void extractPassengerAndSegmentRefs(OrderRetrieveRspGo7Dto bookingRsp, List<String> paxRefs, List<String> paxNames, List<String> segmentRefs) {
        if (bookingRsp != null && bookingRsp.getAerocrs() != null && bookingRsp.getAerocrs().getBooking() != null) {
            extractPassengers(bookingRsp, paxRefs, paxNames);
            extractSegments(bookingRsp, segmentRefs);
        }

        if (paxRefs.isEmpty()) {
            paxRefs.add("T1");
            paxNames.add("UNKNOWN");
        }
        if (segmentRefs.isEmpty()) {
            segmentRefs.add("S1");
        }
    }

    private String determinePtc(Passenger pax) {
        String rawTitle = pax.getPaxtitle() != null ? pax.getPaxtitle().toUpperCase().replace(".", "") : "MR";
        boolean isInfantByTitle = rawTitle.contains("INF");

        if ("CHILD".equalsIgnoreCase(pax.getPaxtype())) return "CNN";
        if ("INFANT".equalsIgnoreCase(pax.getPaxtype()) || isInfantByTitle) return "INF";
        return "ADT";
    }

    private String generatePaxRef(String assignedPtc, List<String> adtRefs, int[] pCounter) {
        if ("INF".equals(assignedPtc)) {
            if (!adtRefs.isEmpty()) {
                return adtRefs.get(adtRefs.size() - 1) + ".1";
            } else {
                return "T" + (pCounter[0]++) + ".1";
            }
        } else {
            String pRef = "T" + (pCounter[0]++);
            if ("ADT".equals(assignedPtc)) {
                adtRefs.add(pRef);
            }
            return pRef;
        }
    }

    private void extractPassengers(OrderRetrieveRspGo7Dto bookingRsp, List<String> paxRefs, List<String> paxNames) {
        if (bookingRsp.getAerocrs().getBooking().getPassengers() != null &&
                bookingRsp.getAerocrs().getBooking().getPassengers().getPassenger() != null) {
            int[] pCounter = { 1 };
            List<String> adtRefs = new ArrayList<>();
            for (Passenger pax : bookingRsp.getAerocrs().getBooking().getPassengers().getPassenger()) {
                
                String assignedPtc = determinePtc(pax);
                String pRef = generatePaxRef(assignedPtc, adtRefs, pCounter);

                paxRefs.add(pRef);
                paxNames.add((pax.getFirstname() != null ? pax.getFirstname() : "").toUpperCase());
            }
        }
    }

    private void extractSegments(OrderRetrieveRspGo7Dto bookingRsp, List<String> segmentRefs) {
        if (bookingRsp.getAerocrs().getBooking().getItems() != null &&
                bookingRsp.getAerocrs().getBooking().getItems().getFlight() != null) {
            int sCounter = 1;
            for (Flight flt : bookingRsp.getAerocrs().getBooking().getItems().getFlight()) {
                segmentRefs.add("S" + sCounter++);
            }
        } else if (bookingRsp.getAerocrs().getBooking().getFlights() != null &&
                bookingRsp.getAerocrs().getBooking().getFlights().getFlight() != null) {
            int sCounter = 1;
            for (Flight flt : bookingRsp.getAerocrs().getBooking().getFlights().getFlight()) {
                segmentRefs.add("S" + sCounter++);
            }
        }
    }
    private ServiceListRspDto.OfferItem.Baggage mapToBaggage(ServiceListRspGo7Dto.Item item, String offerItemId, String serviceId, List<String> paxRefs, List<String> paxNames, List<String> segmentRefs) {
        ServiceListRspDto.OfferItem.Baggage bag = new ServiceListRspDto.OfferItem.Baggage();
        bag.setOfferItemId(offerItemId);
        bag.setPassengerRefs(paxRefs);
        bag.setGivenName(paxNames);
        bag.setSegmentRefId(segmentRefs);
        bag.setCurrency(item.getCurrency());

        BigDecimal amount = safeDecimal(item.getFare());
        bag.setBaseAmount(amount);
        bag.setTotalAmount(amount);

        Service svc = new Service();
        svc.setServiceId(serviceId);

        Service.ServiceDefinationRef def = new Service.ServiceDefinationRef();
        String subCode = generateSubCode(item.getItemname());
        String rfic = "C";
        String type = "6";
        String code = "OC";

        def.setServiceDefId(String.format("%s-%s-%s-BG-%s-G7", rfic, code, subCode, "05"));
        def.setName(item.getItemname());

        Service.ServiceDefinationRef.Encoding enc = new Service.ServiceDefinationRef.Encoding();
        enc.setRfic(rfic);
        enc.setType(type);
        enc.setCode(code);
        enc.setSubCode(subCode);
        def.setEncoding(enc);

        List<Service.ServiceDefinationRef.Description> descs = new ArrayList<>();
        descs.add(new Service.ServiceDefinationRef.Description(item.getItemname(), "Details"));
        descs.add(new Service.ServiceDefinationRef.Description("Surcharge", "Type"));
        def.setDescriptions(descs);

        svc.setServiceDefinitionRef(def);
        bag.setService(svc);

        return bag;
    }

    private ServiceListRspDto.OfferItem.OtherService mapToOtherService(ServiceListRspGo7Dto.Item item, String offerItemId, String serviceId, List<String> paxRefs, List<String> paxNames, List<String> segmentRefs) {
        ServiceListRspDto.OfferItem.OtherService other = new ServiceListRspDto.OfferItem.OtherService();
        other.setOfferItemId(offerItemId);
        other.setPassengerRefs(paxRefs);
        other.setGivenName(paxNames);
        other.setSegmentRefId(segmentRefs);
        other.setCurrency(item.getCurrency());

        BigDecimal amount = safeDecimal(item.getFare());
        other.setBaseAmount(amount);
        other.setTotalAmount(amount);

        Service svc = new Service();
        svc.setServiceId(serviceId);

        Service.ServiceDefinationRef def = new Service.ServiceDefinationRef();
        String subCode = generateSubCode(item.getItemname());
        String rfic = isLounge(item.getItemname()) ? "E" : "F";
        String type = "6";
        String code = "OC";

        def.setServiceDefId(String.format("%s-%s-%s-G7", rfic, code, subCode));
        def.setName(item.getItemname());

        Service.ServiceDefinationRef.Encoding enc = new Service.ServiceDefinationRef.Encoding();
        enc.setRfic(rfic);
        enc.setType(type);
        enc.setCode(code);
        enc.setSubCode(subCode);
        def.setEncoding(enc);

        List<Service.ServiceDefinationRef.Description> descs = new ArrayList<>();
        descs.add(new Service.ServiceDefinationRef.Description(item.getItemname(), "Details"));
        descs.add(new Service.ServiceDefinationRef.Description("Surcharge", "Type"));
        def.setDescriptions(descs);

        svc.setServiceDefinitionRef(def);
        other.setService(svc);

        return other;
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
