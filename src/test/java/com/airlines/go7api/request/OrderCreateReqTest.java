package com.airlines.go7api.request;

import com.airlines.go7api.requestdto.OrderCreateReqDto;
import com.airlines.go7api.requestdto.common.PaxReqDto;
import com.airlines.go7api.requestdto.common.PaymentInformationReqDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrderCreateReqTest {

    @Test
    void testGettersAndMetadata() {
        OrderCreateReq req = new OrderCreateReq();
        req.setApiKey("test-api-key");
        assertEquals("test-api-key", req.getApiKey());

        req.setApiUrl("https://api.aerocrs.com/v5/confirmBooking");
        assertEquals("https://api.aerocrs.com/v5/confirmBooking", req.getApiUrl());
        assertEquals("OrderConfirm", req.getRequestName());

        req.setApiUrl("https://api.aerocrs.com/v5/getBooking");
        assertEquals("GetBooking", req.getRequestName());

        req.setApiUrl("https://api.aerocrs.com/v5/makePayment");
        assertEquals("MakePayment", req.getRequestName());

        req.setApiUrl("https://api.aerocrs.com/v5/ticketBooking");
        assertEquals("OrderTicket", req.getRequestName());

        req.setApiUrl("https://api.aerocrs.com/v5/createBooking");
        assertEquals("OrderCreate", req.getRequestName());
        
        req.setApiUrl(null);
        assertEquals("https://api.aerocrs.com/v5/createBooking", req.getApiUrl());
        assertEquals("OrderCreate", req.getRequestName());
    }

    @Test
    void testMapToOrderCreateReq_VariousOfferIdsAndPtc() {
        OrderCreateReqDto dto = new OrderCreateReqDto();
        
        // Scenario 1: Multi-segment with stars and non-numeric fields
        dto.setOfferId("100-200-JFK-LAX*abc-def-LAX-JFK");

        List<PaxReqDto> passengers = new ArrayList<>();
        
        PaxReqDto pax1 = new PaxReqDto();
        pax1.setPtc("ADT");
        pax1.setTitle("MR.");
        pax1.setFirstName("John");
        pax1.setLastName("Doe");
        pax1.setGender("Male");
        pax1.setDob(LocalDate.now().minusYears(35).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        passengers.add(pax1);

        PaxReqDto pax2 = new PaxReqDto();
        pax2.setPtc("CHD");
        pax2.setTitle("MISS");
        pax2.setFirstName("Jane");
        pax2.setLastName("Doe");
        pax2.setGender("Female");
        pax2.setDob(LocalDate.now().minusYears(8).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        passengers.add(pax2);

        PaxReqDto pax3 = new PaxReqDto();
        pax3.setPtc("INFANT");
        pax3.setTitle("MSTR");
        pax3.setFirstName("Baby");
        pax3.setLastName("Doe");
        pax3.setGender("M");
        pax3.setDob(LocalDate.now().minusYears(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        passengers.add(pax3);

        PaxReqDto pax4 = new PaxReqDto();
        pax4.setPtc(null); // default to ADT
        pax4.setTitle("MRS");
        pax4.setFirstName("Mary");
        pax4.setLastName("Doe");
        pax4.setGender("F");
        pax4.setDob("invalid-dob-format"); // trigger exception
        passengers.add(pax4);

        dto.setPassengers(passengers);

        OrderCreateReq req = OrderCreateReq.mapToOrderCreateReq(dto);

        assertNotNull(req);
        var parms = req.getAerocrs().getParms();
        assertEquals("RT", parms.get("triptype"));
        assertEquals(2, parms.get("adults")); // ADT and null PTC
        assertEquals(1, parms.get("child"));  // CHD
        assertEquals(1, parms.get("infant")); // INFANT

        List<Map<String, Object>> flights = (List<Map<String, Object>>) parms.get("bookflight");
        assertEquals(2, flights.size());
        assertEquals(100L, flights.get(0).get("flightid"));
        assertEquals(200L, flights.get(0).get("fareid"));
        assertEquals("abc", flights.get(1).get("flightid")); // default strings
        assertEquals("def", flights.get(1).get("fareid"));

        List<Map<String, Object>> paxList = (List<Map<String, Object>>) parms.get("passenger");
        assertEquals(4, paxList.size());
        assertEquals("John", paxList.get(0).get("firstname"));
        assertEquals(1, paxList.get(0).get("title")); // MR
        assertEquals("M", paxList.get(0).get("gender"));
        assertEquals("35", paxList.get(0).get("paxage"));

        assertEquals(4, paxList.get(1).get("title")); // MISS
        assertEquals("F", paxList.get(1).get("gender"));
        assertEquals("8", paxList.get(1).get("paxage"));

        assertEquals(5, paxList.get(2).get("title")); // MSTR
        assertEquals("1", paxList.get(2).get("paxage"));

        assertEquals(2, paxList.get(3).get("title")); // MRS
        assertNull(paxList.get(3).get("paxage")); // invalid DOB should keep it null
    }

    @Test
    void testMapToOrderCreateReq_EmptyData() {
        OrderCreateReqDto dto = new OrderCreateReqDto();
        dto.setOfferId(null);
        dto.setPassengers(null);

        OrderCreateReq req = OrderCreateReq.mapToOrderCreateReq(dto);
        assertNotNull(req);
        var parms = req.getAerocrs().getParms();
        assertEquals("OW", parms.get("triptype"));
        assertEquals(1, parms.get("adults"));
        assertEquals(0, parms.get("child"));
        assertEquals(0, parms.get("infant"));
    }

    @Test
    void testMapToOrderConfirmReq_InfantsAndDocument() {
        OrderCreateReqDto dto = new OrderCreateReqDto();

        List<PaxReqDto> passengers = new ArrayList<>();
        PaxReqDto pax1 = new PaxReqDto();
        pax1.setPtc("ADT");
        pax1.setTitle("Mr");
        pax1.setFirstName("John");
        pax1.setLastName("Doe");
        pax1.setEmail("john@doe.com");
        pax1.setPhoneNumber(new java.math.BigDecimal("1234567890"));
        pax1.setCountryDialingCode("+1");
        pax1.setDob("1991-01-01");
        pax1.setGender("M");

        PaxReqDto.IdentityDocument doc = new PaxReqDto.IdentityDocument();
        doc.setCitizenshipCountryCode("US");
        doc.setIdentityDocumentType("Passport");
        doc.setIdentityDocumentNumber("AB12345");
        doc.setIssuingCountryCode("US");
        doc.setExpiryDate("2030-01-01");
        pax1.setIdentityDocument(doc);
        passengers.add(pax1);

        PaxReqDto pax2 = new PaxReqDto();
        pax2.setPtc("INFANT");
        pax2.setTitle("Mstr");
        pax2.setFirstName("Baby");
        pax2.setLastName("Doe");
        pax2.setDob("2025-01-01");
        pax2.setGender("M");
        passengers.add(pax2);

        dto.setPassengers(passengers);

        OrderCreateReq req = OrderCreateReq.mapToOrderConfirmReq(dto, 9999L);
        assertNotNull(req);

        var parms = req.getAerocrs().getParms();
        assertEquals(9999L, parms.get("bookingid"));
        assertEquals("john@doe.com", parms.get("confirmationemail"));

        List<Map<String, Object>> paxList = (List<Map<String, Object>>) parms.get("passenger");
        assertEquals(2, paxList.size());

        // Mr should be formatted with trailing dot
        assertEquals("Mr.", paxList.get(0).get("paxtitle"));
        assertEquals(1, paxList.get(0).get("paxcarringinfant")); // Adult gets assigned infant
        assertEquals("+11234567890", paxList.get(0).get("paxphone"));
        assertEquals("US", paxList.get(0).get("paxnationailty"));
        assertEquals("Passport", paxList.get(0).get("paxdoctype"));

        // INFANT should not have a trailing dot
        assertEquals("INFANT", paxList.get(1).get("paxtitle"));
        assertNull(paxList.get(1).get("paxcarringinfant"));
    }

    @Test
    void testMapToOrderConfirmReq_ComprehensivePaxTitleAndInfantCarrying() {
        OrderCreateReqDto dto = new OrderCreateReqDto();
        List<PaxReqDto> passengers = new ArrayList<>();

        // 1. Adult 1 with infant (total 2 infants in reservation)
        PaxReqDto pax1 = new PaxReqDto();
        pax1.setPtc("ADT");
        pax1.setTitle("Mr");
        pax1.setFirstName("Adult");
        pax1.setLastName("One");
        passengers.add(pax1);

        // 2. Adult 2 with infant (total 2 infants in reservation)
        PaxReqDto pax2 = new PaxReqDto();
        pax2.setPtc("ADT");
        pax2.setTitle("Ms");
        pax2.setFirstName("Adult");
        pax2.setLastName("Two");
        passengers.add(pax2);

        // 3. Adult 3 with NO infant (both infants will be consumed by Adult 1 and 2)
        PaxReqDto pax3 = new PaxReqDto();
        pax3.setPtc("ADT");
        pax3.setTitle("Dr");
        pax3.setFirstName("Adult");
        pax3.setLastName("Three");
        passengers.add(pax3);

        // 4. Child CHD
        PaxReqDto pax4 = new PaxReqDto();
        pax4.setPtc("CHD");
        pax4.setTitle("Miss");
        pax4.setFirstName("Child");
        pax4.setLastName("One");
        passengers.add(pax4);

        // 5. Child CNN
        PaxReqDto pax5 = new PaxReqDto();
        pax5.setPtc("CNN");
        pax5.setTitle("Miss");
        pax5.setFirstName("Child");
        pax5.setLastName("Two");
        passengers.add(pax5);

        // 6. Infant INF
        PaxReqDto pax6 = new PaxReqDto();
        pax6.setPtc("INF");
        pax6.setTitle("Mstr");
        pax6.setFirstName("Infant");
        pax6.setLastName("One");
        passengers.add(pax6);

        // 7. Infant INFANT
        PaxReqDto pax7 = new PaxReqDto();
        pax7.setPtc("INFANT");
        pax7.setTitle("Mstr");
        pax7.setFirstName("Infant");
        pax7.setLastName("Two");
        passengers.add(pax7);

        dto.setPassengers(passengers);

        OrderCreateReq req = OrderCreateReq.mapToOrderConfirmReq(dto, 12345L);
        assertNotNull(req);

        var parms = req.getAerocrs().getParms();
        List<Map<String, Object>> paxList = (List<Map<String, Object>>) parms.get("passenger");
        assertEquals(7, paxList.size());

        // Pax 1: Adult carrying infant
        assertEquals("Mr.", paxList.get(0).get("paxtitle"));
        assertEquals(1, paxList.get(0).get("paxcarringinfant"));

        // Pax 2: Adult carrying infant
        assertEquals("Ms.", paxList.get(1).get("paxtitle"));
        assertEquals(1, paxList.get(1).get("paxcarringinfant"));

        // Pax 3: Adult with NO infant assigned
        assertEquals("Dr.", paxList.get(2).get("paxtitle"));
        assertNull(paxList.get(2).get("paxcarringinfant"));

        // Pax 4: CHD -> title Child, carrying infant null
        assertEquals("Child", paxList.get(3).get("paxtitle"));
        assertNull(paxList.get(3).get("paxcarringinfant"));

        // Pax 5: CNN -> title Child, carrying infant null
        assertEquals("Child", paxList.get(4).get("paxtitle"));
        assertNull(paxList.get(4).get("paxcarringinfant"));

        // Pax 6: INF -> title INFANT, carrying infant null
        assertEquals("INFANT", paxList.get(5).get("paxtitle"));
        assertNull(paxList.get(5).get("paxcarringinfant"));

        // Pax 7: INFANT -> title INFANT, carrying infant null
        assertEquals("INFANT", paxList.get(6).get("paxtitle"));
        assertNull(paxList.get(6).get("paxcarringinfant"));
    }

    @Test
    void testMapToOrderConfirmReq_EmptyPassengers() {
        OrderCreateReqDto dto = new OrderCreateReqDto();
        dto.setPassengers(null);

        OrderCreateReq req = OrderCreateReq.mapToOrderConfirmReq(dto, 8888L);
        assertNotNull(req);
        var parms = req.getAerocrs().getParms();
        assertEquals("noreply@airlines.com", parms.get("confirmationemail"));
    }

    @Test
    void testMapToMakePaymentReq_FullData() {
        OrderCreateReqDto dto = new OrderCreateReqDto();
        PaymentInformationReqDto payInfo = new PaymentInformationReqDto();
        payInfo.setAmount(new BigDecimal("250.50"));
        payInfo.setCurrencyCode("USD");
        payInfo.setCardHolderName("John Doe");
        payInfo.setCardNumber("1234567812345678");
        payInfo.setExpiration("12/29");
        payInfo.setSeriesCode("123");
        dto.setPaymentInformation(payInfo);

        OrderCreateReq req = OrderCreateReq.mapToMakePaymentReq(dto, 7777L);
        assertNotNull(req);

        var parms = req.getAerocrs().getParms();
        assertEquals(7777L, parms.get("bookingid"));
        assertEquals(250.50, parms.get("amountpaid"));
        assertEquals("USD", parms.get("amountcurrency"));
        assertEquals("John Doe", parms.get("creditcardpayer"));
        assertEquals("1234567812345678", parms.get("creditcardnumber")); // fallback due to invalid decryption key format or parsing
        assertEquals("12/29", parms.get("creditcardexpiry"));
        assertEquals("123", parms.get("creditcardcvv"));
    }

    @Test
    void testMapToOrderTicketReq() {
        OrderCreateReqDto dto = new OrderCreateReqDto();
        dto.setApiKey("test-key-confirm");

        OrderCreateReq req1 = OrderCreateReq.mapToOrderTicketReq(dto, 5555L);
        assertNotNull(req1);
        assertEquals("test-key-confirm", req1.getApiKey());
        assertEquals("https://api.aerocrs.com/v5/ticketBooking", req1.getApiUrl());
        assertEquals(5555L, req1.getAerocrs().getParms().get("bookingid"));

        OrderCreateReq req2 = OrderCreateReq.mapToOrderTicketReq(4444L);
        assertNotNull(req2);
        assertNull(req2.getApiKey());
        assertEquals(4444L, req2.getAerocrs().getParms().get("bookingid"));
    }

    @Test
    void testMapToGetBookingReq() {
        OrderCreateReq req = OrderCreateReq.mapToGetBookingReq("XYZ123");
        assertNotNull(req);
        assertEquals("https://api.aerocrs.com/v5/getBooking", req.getApiUrl());
        assertEquals("XYZ123", req.getAerocrs().getParms().get("bookingconfirmation"));
    }
}
