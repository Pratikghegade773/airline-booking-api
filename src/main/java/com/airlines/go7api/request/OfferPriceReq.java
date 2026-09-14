package com.airlines.go7api.request;

import com.airlines.go7api.requestdto.OfferPriceReqDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.http.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OfferPriceReq extends BaseGo7Req {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @JsonIgnore
    private String offerPriceUrl = "https://api.aerocrs.com/v5/getFlight";

    @Data
    public static class Aerocrs {
        @JsonProperty("parms")
        private Map<String, Object> parms;
    }

    private static void populateCommonParameters(Map<String, Object> parms, String firstOfferId) {
        String triptype = "OW";
        String fromcode = null;
        String tocode = null;
        int adults = 1;
        int child = 0;
        int infant = 0;

        if (firstOfferId != null && !firstOfferId.isEmpty()) {
            String[] parts = firstOfferId.split("-");
            if (parts.length >= 8) {
                fromcode = parts[2];
                tocode = parts[3];
                String type = parts[4];
                if ("Return".equalsIgnoreCase(type) || "RT".equalsIgnoreCase(type)
                        || "RoundTrip".equalsIgnoreCase(type)) {
                    triptype = "RT";
                }
                try {
                    adults = Integer.parseInt(parts[5]);
                    child = Integer.parseInt(parts[6]);
                    infant = Integer.parseInt(parts[7]);
                } catch (NumberFormatException e) {
                    // keep defaults
                }
            }
        }

        parms.put("triptype", triptype);
        parms.put("fromcode", fromcode);
        parms.put("tocode", tocode);
        parms.put("adults", adults);
        parms.put("child", child);
        parms.put("infant", infant);
    }

    private static void populateFlightAndFareIds(Map<String, Object> parms, java.util.List<OfferPriceReqDto.Offer> offers) {
        for (int i = 0; i < offers.size(); i++) {
            OfferPriceReqDto.Offer offer = offers.get(i);
            String offerId = offer.getOfferId();
            if (offerId != null) {
                String[] parts = offerId.split("-");
                if (parts.length >= 2) {
                    String suffix = String.valueOf(i + 1);
                    parms.put("flightid" + suffix, parts[0]);
                    parms.put("fareid" + suffix, parts[1]);
                }
            }
        }
    }

    public static OfferPriceReq mapToOfferPriceRequestDTO(OfferPriceReqDto offerPriceRQ) {
        OfferPriceReq request = new OfferPriceReq();
        Aerocrs aerocrs = new Aerocrs();
        // Use LinkedHashMap to preserve order: triptype, fromcode, tocode, adults,
        // child, infant, flightid1...
        Map<String, Object> parms = new LinkedHashMap<>();

        if (offerPriceRQ.getOffers() != null && !offerPriceRQ.getOffers().isEmpty()) {
            // Extract common parameters from the FIRST offer to ensure they come first in
            // the JSON
            OfferPriceReqDto.Offer firstOffer = offerPriceRQ.getOffers().get(0);
            populateCommonParameters(parms, firstOffer.getOfferId());
            populateFlightAndFareIds(parms, offerPriceRQ.getOffers());
        } else {
            // Fallback defaults if no offers
            parms.put("triptype", "OW");
            parms.put("adults", 1);
            parms.put("child", 0);
            parms.put("infant", 0);
        }

        aerocrs.setParms(parms);
        request.setAerocrs(aerocrs);

        // Map dynamic URL if provided
        if (offerPriceRQ.getOfferpriceurl() != null && !offerPriceRQ.getOfferpriceurl().isEmpty()) {
            request.setOfferPriceUrl(offerPriceRQ.getOfferpriceurl());
        }

        return request;
    }

    @Override
    protected String getApiUrl() {
        return offerPriceUrl != null ? offerPriceUrl : "";
    }

    @Override
    protected String getRequestName() {
        return "OfferPrice";
    }
}
