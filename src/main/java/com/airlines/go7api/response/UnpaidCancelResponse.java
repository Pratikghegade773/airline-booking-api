package com.airlines.go7api.response;

import com.airlines.go7api.responsego7.common.*;

import com.airlines.go7api.responsedto.common.*;

import com.airlines.go7api.responsedto.UnpaidCancelRspDto;
import com.airlines.go7api.responsego7.UnpaidCancelRspGo7Dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class UnpaidCancelResponse {

    private UnpaidCancelResponse() {
        throw new IllegalStateException("Utility class");
    }

    public static UnpaidCancelRspDto generateResponse(UnpaidCancelRspGo7Dto go7Response) {
        UnpaidCancelRspDto response = new UnpaidCancelRspDto();

        if (isInvalidResponse(go7Response)) {
            return response;
        }

        Booking booking = go7Response.getAerocrs().getBooking();
        initializeTopLevel(response, go7Response, booking);
        response.setPaxDetailList(buildPaxDetails(booking));

        return response;
    }

    private static boolean isInvalidResponse(UnpaidCancelRspGo7Dto go7Response) {
        return go7Response == null || go7Response.getAerocrs() == null || go7Response.getAerocrs().getBooking() == null;
    }

    private static void initializeTopLevel(UnpaidCancelRspDto response, UnpaidCancelRspGo7Dto go7Response, Booking booking) {
        response.setOrderId(booking.getBookingconfirmation() != null ? booking.getBookingconfirmation()
                : String.valueOf(booking.getBookingid()));
        response.setPnr(booking.getPnrref());
        response.setApiOwner("G7");
        response.setStatusCode(go7Response.getAerocrs().isSuccess() ? "X" : "REJECTED");
        response.setValidatingCarrier("G7");

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

    private static List<PaxDetailDTO> buildPaxDetails(Booking booking) {
        List<PaxDetailDTO> paxList = new ArrayList<>();
        if (booking.getPassengers() == null || booking.getPassengers().getPassenger() == null) {
            return paxList;
        }

        List<Passenger> go7PaxList = booking.getPassengers().getPassenger();
        int[] paxCounter = {1};
        List<PaxDetailDTO> adtList = new ArrayList<>();
        for (Passenger go7Pax : go7PaxList) {
            paxList.add(mapPassenger(go7Pax, paxCounter, adtList));
        }
        return paxList;
    }

    private static PaxDetailDTO mapPassenger(Passenger go7Pax, int[] paxCounter, List<PaxDetailDTO> adtList) {
        PaxDetailDTO pax = new PaxDetailDTO();

        String rawTitle = go7Pax.getPaxtitle() != null ? go7Pax.getPaxtitle().toUpperCase().replace(".", "") : "MR";
        boolean isInfantByTitle = rawTitle.contains("INF");

        String assignedPtc = OrderMappingUtil.mapPaxType(go7Pax.getPaxtype());
        if (isInfantByTitle) {
            assignedPtc = "INF";
        }

        String paxId;
        if ("INF".equals(assignedPtc)) {
            paxId = assignInfantPaxId(adtList, paxCounter);
        } else {
            paxId = "T" + paxCounter[0]++;
            if ("ADT".equals(assignedPtc)) {
                adtList.add(pax);
            }
        }

        pax.setPaxId(paxId);
        pax.setPtc(assignedPtc);
        pax.setGivenName(go7Pax.getFirstname() != null ? go7Pax.getFirstname().toUpperCase() : "");
        pax.setSurname(go7Pax.getLastname() != null ? go7Pax.getLastname().toUpperCase() : "");
        pax.setTitle(rawTitle);

        setPassengerGenderAndTitle(pax, go7Pax);

        pax.setBirthDate(OrderMappingUtil.formatDate(go7Pax.getDob()));
        pax.setPhones(buildPaxContact(go7Pax.getContact()));
        pax.setEmails(buildPaxEmail(go7Pax.getEmail()));

        return pax;
    }

    /** Assigns an infant pax ID, linking to the last adult's ID when one exists. */
    private static String assignInfantPaxId(List<PaxDetailDTO> adtList, int[] paxCounter) {
        if (!adtList.isEmpty()) {
            PaxDetailDTO parent = adtList.get(adtList.size() - 1);
            String paxId = parent.getPaxId() + ".1";
            try {
                parent.setInfantRef(paxId);
            } catch (Exception e) {
                // Ignore exception in case infantRef setter fails
            }
            return paxId;
        }
        return "T" + paxCounter[0]++ + ".1";
    }

    /** Builds a single-entry phone list, or an empty list if contact is absent. */
    private static List<PaxDetailDTO.PhoneDTO> buildPaxContact(String contact) {
        if (contact == null) {
            return Collections.emptyList();
        }
        PaxDetailDTO.PhoneDTO phone = new PaxDetailDTO.PhoneDTO();
        phone.setPhoneNumber(contact);
        phone.setType("Operational");
        phone.setLabel("Mobile");
        return Collections.singletonList(phone);
    }

    /** Builds a single-entry email list, or an empty list if email is absent. */
    private static List<PaxDetailDTO.EmailDTO> buildPaxEmail(String email) {
        if (email == null) {
            return Collections.emptyList();
        }
        PaxDetailDTO.EmailDTO emailDto = new PaxDetailDTO.EmailDTO();
        emailDto.setEmailAddress(email.toUpperCase());
        emailDto.setType("Operational");
        return Collections.singletonList(emailDto);
    }

    private static void setPassengerGenderAndTitle(PaxDetailDTO pax, Passenger go7Pax) {
        if (go7Pax.getPaxtitle() != null && (go7Pax.getPaxtitle().toUpperCase().contains("MR")
                || go7Pax.getPaxtitle().toUpperCase().contains("MISTR"))) {
            pax.setGender("MALE");
        } else if (go7Pax.getPaxtitle() != null && (go7Pax.getPaxtitle().toUpperCase().contains("MS")
                || go7Pax.getPaxtitle().toUpperCase().contains("MRS")
                || go7Pax.getPaxtitle().toUpperCase().contains("MISS"))) {
            pax.setGender("FEMALE");
        } else if (go7Pax.getGender() != null) {
            pax.setGender(go7Pax.getGender().startsWith("M") ? "MALE" : "FEMALE");
        } else {
            pax.setGender("MALE");
        }

        if ("CNN".equals(pax.getPtc()) || "CHD".equals(pax.getPtc())) {
            pax.setTitle("CHILD");
            pax.setGender("MALE");
        } else if ("INF".equals(pax.getPtc())) {
            pax.setTitle("INFANT");
            pax.setGender("MALE");
        }
    }
}