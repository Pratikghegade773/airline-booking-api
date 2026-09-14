package com.airlines.go7api.request;

import com.airlines.go7api.requestdto.common.*;

import com.airlines.go7api.requestdto.ChangeServiceReqDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@ToString
public class ChangeServiceReq extends BaseGo7Req {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ChangeServiceReq.class);

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @JsonIgnore
    private String apiKey;

    @JsonIgnore
    private String changeAncillariesUrl = "https://api.aerocrs.com/v5/createAncillary";

    @Data
    @NoArgsConstructor
    @ToString
    public static class Aerocrs {
        @JsonProperty("parms")
        private Parms parms;
    }

    @Data
    @NoArgsConstructor
    @ToString
    public static class Parms {
        @JsonProperty("ancillaries")
        private Ancillaries ancillaries;
    }

    @Data
    @NoArgsConstructor
    @ToString
    public static class Ancillaries {
        @JsonProperty("ancillary")
        private List<Ancillary> ancillary;
    }

    @Data
    @NoArgsConstructor
    @ToString
    public static class Ancillary {
        @JsonProperty("paxnum")
        private Integer paxnum;

        @JsonProperty("itemid")
        private Long itemid;

        @JsonProperty("bookingid")
        private Long bookingid;

        @JsonProperty("flightid")
        private Long flightid;
    }

    public static ChangeServiceReq mapToChangeServiceReq(ChangeServiceReqDto dto,
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp) {
        ChangeServiceReq request = new ChangeServiceReq();
        Aerocrs aerocrs = new Aerocrs();
        Parms parms = new Parms();
        Ancillaries ancillaries = new Ancillaries();
        List<Ancillary> ancillaryList = new ArrayList<>();

        if (dto.getOffers() != null) {
            for (ChangeOfferReqDto offer : dto.getOffers()) {
                if (offer.getOfferItems() != null) {
                    for (ChangeOfferReqDto.OfferItemDto item : offer.getOfferItems()) {
                        if (item.getSpecialServices() != null) {
                            List<Integer> paxNums = extractPaxNums(item, bookingRsp);
                            List<Long> flightIds = extractFlightIds(item, bookingRsp);
                            buildAncillaryEntries(item, paxNums, flightIds, bookingRsp, ancillaryList);
                        }
                    }
                }
            }
        }

        ancillaries.setAncillary(ancillaryList);
        parms.setAncillaries(ancillaries);
        aerocrs.setParms(parms);
        request.setAerocrs(aerocrs);
        request.setApiKey(dto.getApiKey());
        return request;
    }

    private static List<Integer> extractPaxNums(
            ChangeOfferReqDto.OfferItemDto item,
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp) {
        List<Integer> paxNums = new ArrayList<>();
        if (item.getPaxRefs() == null || bookingRsp == null || bookingRsp.getAerocrs() == null
                || bookingRsp.getAerocrs().getBooking() == null
                || bookingRsp.getAerocrs().getBooking().getPassengers() == null) {
            return paxNums;
        }

        List<com.airlines.go7api.responsego7.common.Passenger> passengers = bookingRsp
                .getAerocrs().getBooking().getPassengers().getPassenger();
        for (String paxRef : item.getPaxRefs()) {
            try {
                String numStr = paxRef.replaceAll("\\D", "");
                if (!numStr.isEmpty()) {
                    int idx = Integer.parseInt(numStr) - 1;
                    if (idx >= 0 && idx < passengers.size()) {
                        int pNum = passengers.get(idx).getPaxnum();
                        if (pNum == 0) {
                            pNum = idx;
                        }
                        paxNums.add(pNum);
                    }
                }
            } catch (Exception e) {
                logger.error("Error parsing paxRef: {}", paxRef);
            }
        }
        return paxNums;
    }

    private static List<Long> extractFlightIds(
            ChangeOfferReqDto.OfferItemDto item,
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp) {
        List<Long> flightIds = new ArrayList<>();
        if (item.getSpecialServices() == null || item.getSpecialServices().getSegId() == null
                || bookingRsp == null || bookingRsp.getAerocrs() == null
                || bookingRsp.getAerocrs().getBooking() == null
                || bookingRsp.getAerocrs().getBooking().getFlights() == null) {
            return flightIds;
        }

        List<com.airlines.go7api.responsego7.common.Flight> flights = bookingRsp
                .getAerocrs().getBooking().getFlights().getFlight();
        for (String segRef : item.getSpecialServices().getSegId()) {
            try {
                String numStr = segRef.replaceAll("\\D", "");
                if (!numStr.isEmpty()) {
                    int idx = Integer.parseInt(numStr) - 1;
                    if (idx >= 0 && idx < flights.size()) {
                        flightIds.add((long) flights.get(idx).getFlightid());
                    }
                }
            } catch (Exception e) {
                logger.error("Error parsing segRef or flightId: {}", segRef);
            }
        }
        return flightIds;
    }

    private static void buildAncillaryEntries(
            ChangeOfferReqDto.OfferItemDto item,
            List<Integer> paxNums,
            List<Long> flightIds,
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp,
            List<Ancillary> ancillaryList) {
        String serviceDefId = item.getOfferItemId();
        for (Integer paxNum : paxNums) {
            for (Long flightId : flightIds) {
                Ancillary ancillary = new Ancillary();
                ancillary.setPaxnum(paxNum);
                try {
                    ancillary.setItemid(Long.parseLong(serviceDefId));
                } catch (Exception e) {
                    logger.error("Error parsing serviceDefId: {}", serviceDefId);
                }

                if (bookingRsp != null && bookingRsp.getAerocrs() != null
                        && bookingRsp.getAerocrs().getBooking() != null) {
                    ancillary.setBookingid(bookingRsp.getAerocrs().getBooking().getBookingid());
                }

                ancillary.setFlightid(flightId);
                ancillaryList.add(ancillary);
            }
        }
    }

    @Override
    protected String getApiUrl() {
        return changeAncillariesUrl != null ? changeAncillariesUrl : "";
    }

    @Override
    protected String getRequestName() {
        return "ChangeService";
    }
}
