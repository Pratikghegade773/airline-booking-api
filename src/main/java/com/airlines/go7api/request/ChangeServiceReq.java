package com.airlines.go7api.request;

import com.airlines.go7api.requestdto.common.*;

import com.airlines.go7api.requestdto.ChangeServiceReqDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@ToString
public class ChangeServiceReq extends BaseGo7Req {

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
                            String serviceDefId = item.getOfferItemId(); // User requested to map itemid from
                                                                         // offerItemId

                            // Map Pax Refs to PaxNum
                            List<Integer> paxNums = new ArrayList<>();
                            if (item.getPaxRefs() != null
                                    && bookingRsp.getAerocrs() != null
                                    && bookingRsp.getAerocrs().getBooking() != null
                                    && bookingRsp.getAerocrs().getBooking().getPassengers() != null) {
                                List<com.airlines.go7api.responsego7.common.Passenger> passengers = bookingRsp
                                        .getAerocrs().getBooking().getPassengers().getPassenger();
                                for (String paxRef : item.getPaxRefs()) {
                                    try {
                                        String numStr = paxRef.replaceAll("\\D", "");
                                        if (!numStr.isEmpty()) {
                                            int idx = Integer.parseInt(numStr) - 1;
                                            if (idx >= 0 && idx < passengers.size()) {
                                                // If available use it, else fallback to index
                                                int pNum = passengers.get(idx).getPaxnum();
                                                if (pNum == 0)
                                                    pNum = idx;
                                                paxNums.add(pNum);
                                            }
                                        }
                                    } catch (Exception e) {
                                        System.out.println("Error parsing paxRef: " + paxRef);
                                    }
                                }
                            }

                            // Map Segment Refs to FlightID
                            List<Long> flightIds = new ArrayList<>();
                            if (item.getSpecialServices().getSegId() != null
                                    && bookingRsp.getAerocrs() != null
                                    && bookingRsp.getAerocrs().getBooking() != null
                                    && bookingRsp.getAerocrs().getBooking().getFlights() != null) {
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
                                        System.out.println("Error parsing segRef or flightId: " + segRef);
                                    }
                                }
                            }

                            // Create Ancillary Entry for each combination
                            for (Integer paxNum : paxNums) {
                                for (Long flightId : flightIds) {
                                    Ancillary ancillary = new Ancillary();
                                    ancillary.setPaxnum(paxNum);
                                    try {
                                        ancillary.setItemid(Long.parseLong(serviceDefId));
                                    } catch (Exception e) {
                                        // Provide fallback or log error if serviceDefId is not numeric
                                        System.out.println("Error parsing serviceDefId: " + serviceDefId);
                                    }

                                    if (bookingRsp.getAerocrs() != null
                                            && bookingRsp.getAerocrs().getBooking() != null) {
                                        Long bid = bookingRsp.getAerocrs().getBooking().getBookingid();
                                        ancillary.setBookingid(bid);
                                    }

                                    ancillary.setFlightid(flightId);
                                    ancillaryList.add(ancillary);
                                }
                            }
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
        // Set other config if needed
        return request;
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
