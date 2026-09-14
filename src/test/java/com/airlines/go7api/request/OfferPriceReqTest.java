package com.airlines.go7api.request;

import com.airlines.go7api.requestdto.OfferPriceReqDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OfferPriceReqTest {

    @Test
    void testGettersSettersAndMetadata() {
        OfferPriceReq req = new OfferPriceReq();
        OfferPriceReq.Aerocrs aerocrs = new OfferPriceReq.Aerocrs();
        req.setAerocrs(aerocrs);
        assertNotNull(req.getAerocrs());

        assertEquals("https://api.aerocrs.com/v5/getFlight", req.getApiUrl());
        assertEquals("OfferPrice", req.getRequestName());
    }

    @Test
    void testMapToOfferPriceRequestDTO_EmptyOffers() {
        OfferPriceReqDto dto = new OfferPriceReqDto();
        dto.setOffers(null);

        OfferPriceReq req = OfferPriceReq.mapToOfferPriceRequestDTO(dto);
        assertNotNull(req);
        assertNotNull(req.getAerocrs());
        assertEquals("OW", req.getAerocrs().getParms().get("triptype"));
        assertEquals(1, req.getAerocrs().getParms().get("adults"));
        assertEquals(0, req.getAerocrs().getParms().get("child"));
        assertEquals(0, req.getAerocrs().getParms().get("infant"));
    }

    @Test
    void testMapToOfferPriceRequestDTO_HappyPathRoundTrip() {
        OfferPriceReqDto dto = new OfferPriceReqDto();
        dto.setOfferpriceurl("https://api.custom.com/price");

        List<OfferPriceReqDto.Offer> offers = new ArrayList<>();
        OfferPriceReqDto.Offer offer1 = new OfferPriceReqDto.Offer();
        // Format: flightid-fareid-fromcode-tocode-triptype-adults-child-infant
        offer1.setOfferId("100-200-JFK-LAX-RoundTrip-2-1-0");
        offers.add(offer1);

        OfferPriceReqDto.Offer offer2 = new OfferPriceReqDto.Offer();
        offer2.setOfferId("101-201-LAX-JFK-RoundTrip-2-1-0");
        offers.add(offer2);

        dto.setOffers(offers);

        OfferPriceReq req = OfferPriceReq.mapToOfferPriceRequestDTO(dto);
        assertNotNull(req);
        assertEquals("https://api.custom.com/price", req.getApiUrl());

        var parms = req.getAerocrs().getParms();
        assertEquals("RT", parms.get("triptype"));
        assertEquals("JFK", parms.get("fromcode"));
        assertEquals("LAX", parms.get("tocode"));
        assertEquals(2, parms.get("adults"));
        assertEquals(1, parms.get("child"));
        assertEquals(0, parms.get("infant"));

        assertEquals("100", parms.get("flightid1"));
        assertEquals("200", parms.get("fareid1"));
        assertEquals("101", parms.get("flightid2"));
        assertEquals("201", parms.get("fareid2"));
    }

    @Test
    void testMapToOfferPriceRequestDTO_InvalidOfferFormatAndExceptions() {
        OfferPriceReqDto dto = new OfferPriceReqDto();
        List<OfferPriceReqDto.Offer> offers = new ArrayList<>();
        
        OfferPriceReqDto.Offer offer1 = new OfferPriceReqDto.Offer();
        // offerId parts length < 8, but has >= 2 parts
        offer1.setOfferId("100-200"); 
        offers.add(offer1);
        dto.setOffers(offers);

        OfferPriceReq req = OfferPriceReq.mapToOfferPriceRequestDTO(dto);
        assertNotNull(req);
        var parms = req.getAerocrs().getParms();
        assertEquals("OW", parms.get("triptype"));
        assertNull(parms.get("fromcode"));
        assertNull(parms.get("tocode"));
        assertEquals(1, parms.get("adults"));

        // Let's test non-numeric pax format to hit catch block
        OfferPriceReqDto.Offer offerInvalidPax = new OfferPriceReqDto.Offer();
        offerInvalidPax.setOfferId("100-200-JFK-LAX-OW-invalid-invalid-invalid");
        offers.clear();
        offers.add(offerInvalidPax);

        req = OfferPriceReq.mapToOfferPriceRequestDTO(dto);
        assertNotNull(req);
        parms = req.getAerocrs().getParms();
        assertEquals(1, parms.get("adults")); // default maintained
        assertEquals(0, parms.get("child"));  // default maintained
        assertEquals(0, parms.get("infant")); // default maintained
    }
}
