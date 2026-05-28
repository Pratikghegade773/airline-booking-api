package com.airlines.go7api.response;

import com.airlines.go7api.responsego7.common.*;

import com.airlines.go7api.responsedto.common.*;

import com.airlines.go7api.requestdto.UnpaidCancelReqDto;
import com.airlines.go7api.responsedto.UnpaidCancelRspDto;
import com.airlines.go7api.responsego7.UnpaidCancelRspGo7Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;


public class UnpaidCancelResponse {

    public static UnpaidCancelRspDto generateResponse(UnpaidCancelRspGo7Dto go7Response,
            UnpaidCancelReqDto requestDto) {
        UnpaidCancelRspDto response = new UnpaidCancelRspDto();

        if (go7Response == null || go7Response.getAerocrs() == null || go7Response.getAerocrs().getBooking() == null) {
            return response;
        }

        Booking booking = go7Response.getAerocrs().getBooking();

        // 1. Top Level Fields
        // response.setResponseId("P" + UUID.randomUUID().toString().substring(0,
        // 15).toUpperCase()); // Removed
        response.setOrderId(booking.getBookingconfirmation() != null ? booking.getBookingconfirmation()
                : String.valueOf(booking.getBookingid()));
        response.setPnr(booking.getPnrref());

        response.setApiOwner("G7");

        
        // response.setPaymentTimeLimit(formatDateTime(booking.getPnrttl())); // Removed

        response.setStatusCode(go7Response.getAerocrs().isSuccess() ? "X" : "REJECTED");
        String validatingCarrier = "G7";
        response.setValidatingCarrier(validatingCarrier);

        // 2. Booking References
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

        
        // 4. Pax Details
        List<PaxDetailDTO> paxList = new ArrayList<>();
        List<Passenger> go7PaxList = null;

        if (booking.getPassengers() != null && booking.getPassengers().getPassenger() != null) {
            go7PaxList = booking.getPassengers().getPassenger();
        }

        if (go7PaxList != null) {
            int paxCounter = 1;
            List<PaxDetailDTO> adtList = new ArrayList<>();
            for (Passenger go7Pax : go7PaxList) {
                // Only requested fields: ptc, paxId, gender, title, givenName, surname,
                // birthDate, phones, emails
                PaxDetailDTO pax = new PaxDetailDTO();

                String rawTitle = go7Pax.getPaxtitle() != null ? go7Pax.getPaxtitle().toUpperCase().replace(".", "") : "MR";
                boolean isInfantByTitle = rawTitle.contains("INF");

                String assignedPtc = OrderMappingUtil.mapPaxType(go7Pax.getPaxtype());
                if (isInfantByTitle) {
                    assignedPtc = "INF";
                }

                String paxId;
                if ("INF".equals(assignedPtc)) {
                    if (!adtList.isEmpty()) {
                        PaxDetailDTO parent = adtList.get(adtList.size() - 1);
                        paxId = parent.getPaxId() + ".1";
                        // Using try-catch just in case UnpaidCancel DTO doesn't have infantRef yet
                        try {
                            parent.setInfantRef(paxId);
                        } catch(Exception e) {}
                    } else {
                        paxId = "T" + paxCounter++ + ".1";
                    }
                } else {
                    paxId = "T" + paxCounter++;
                    if ("ADT".equals(assignedPtc)) {
                        adtList.add(pax);
                    }
                }

                pax.setPaxId(paxId);
                pax.setPtc(assignedPtc);
                pax.setGivenName(go7Pax.getFirstname() != null ? go7Pax.getFirstname().toUpperCase() : "");
                pax.setSurname(go7Pax.getLastname() != null ? go7Pax.getLastname().toUpperCase() : "");
                pax.setTitle(rawTitle);

                // Gender Logic Match OrderCreate
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
                    pax.setGender("MALE"); // Default
                }

                if ("CNN".equals(pax.getPtc()) || "CHD".equals(pax.getPtc())) {
                    pax.setTitle("CHILD");
                    pax.setGender("MALE");
                } else if ("INF".equals(pax.getPtc())) {
                    pax.setTitle("INFANT");
                    pax.setGender("MALE");
                }

                pax.setBirthDate(OrderMappingUtil.formatDate(go7Pax.getDob()));
                // pax.setLanguage("English"); // Removed

                if (go7Pax.getContact() != null) {
                    PaxDetailDTO.PhoneDTO phone = new PaxDetailDTO.PhoneDTO();
                    phone.setPhoneNumber(go7Pax.getContact());
                    phone.setType("Operational");
                    phone.setLabel("Mobile");
                    List<PaxDetailDTO.PhoneDTO> phones = new ArrayList<>();
                    phones.add(phone);
                    pax.setPhones(phones);
                }

                if (go7Pax.getEmail() != null) {
                    PaxDetailDTO.EmailDTO email = new PaxDetailDTO.EmailDTO();
                    email.setEmailAddress(go7Pax.getEmail().toUpperCase());
                    email.setType("Operational");
                    List<PaxDetailDTO.EmailDTO> emails = new ArrayList<>();
                    emails.add(email);
                    pax.setEmails(emails);
                }

                // E-tickets Logic Removed as not requested
                // ... logic removed ...

                paxList.add(pax);
            }
        }
        response.setPaxDetailList(paxList);

        // 5. Order Items (Removed)
        // ... (skipping order items logic)
        // response.setOrderItems(orderItems); // Removed

        return response;
    }

    

    

    

    

    private static OrderItemsDTO createOrderItem(String responseId, int itemIndex, String ptc,
            List<String> paxIds, List<Flight> flights, String currency) {
        OrderItemsDTO item = new OrderItemsDTO();
        item.setOrderItemId(responseId + "_AIR-" + itemIndex);
        item.setPtc(ptc);

        String mainClassName = "Economy";
        if (flights != null && !flights.isEmpty()) {
            Flight f = flights.get(0);
            if (f.getFlightClass() != null) {
                String[] parts = f.getFlightClass().split("/");
                if (parts.length > 1) {
                    mainClassName = parts[1].trim();
                } else {
                    mainClassName = f.getFlightClass();
                }
            }
        }
        item.setClassName(mainClassName);
        item.setTimeStamp(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMMyyyy HH:mm:ss", Locale.ENGLISH)));
        item.setPassengerIds(paxIds);

        // Calculate Unit Totals
        BigDecimal unitBase = BigDecimal.ZERO;
        BigDecimal unitTax = BigDecimal.ZERO;

        List<OrderItemsDTO.Tax> taxList = new ArrayList<>();

        if (flights != null) {
            for (Flight f : flights) {
                // Base Fare
                String fareStr = null;
                boolean isAdt = "ADT".equals(ptc);
                boolean isCnn = "CNN".equals(ptc);
                boolean isInf = "INF".equals(ptc);

                if (isAdt)
                    fareStr = f.getAdultfare();
                else if (isCnn)
                    fareStr = f.getChildfare();
                else if (isInf)
                    fareStr = f.getInfantfare();

                // Fallback logic
                if (fareStr == null) {
                    if (isAdt)
                        fareStr = f.getNetFare();
                }

                // If fareStr has value, parse it.
                BigDecimal fareVal = BigDecimal.ZERO;
                if (fareStr != null) {
                    try {
                        fareVal = new BigDecimal(fareStr);
                    } catch (Exception e) {
                    }
                }
                unitBase = unitBase.add(fareVal);
                unitTax = unitTax.add(BigDecimal.valueOf(f.getTotaltaxes()));
            }
        }

        int count = paxIds.size();
        BigDecimal totalBase = unitBase.multiply(new BigDecimal(count));
        BigDecimal totalTaxAmount = unitTax.multiply(new BigDecimal(count));
        BigDecimal totalPrice = totalBase.add(totalTaxAmount);

        item.setTotalPrice(totalPrice);

        // Taxes Breakdown (Apply Count Multiplier)
        if (flights != null) {
            for (Flight f : flights) {
                if (f.getTaxes() != null) {
                    addTaxToList(taxList, "Ground_handling", f.getTaxes().getGroundHandling() * count, currency);
                    addTaxToList(taxList, "Security", f.getTaxes().getSecurity() * count, currency);
                    addTaxToList(taxList, "Fuel", f.getTaxes().getFuel() * count, currency);
                    addTaxToList(taxList, "tax_4", f.getTaxes().getTax4() * count, currency);
                }
            }
        }
        item.setTaxes(taxList);

        OrderItemsDTO.TotalTax totalTaxObj = new OrderItemsDTO.TotalTax();
        totalTaxObj.setAmount(totalTaxAmount);
        totalTaxObj.setCurrency(currency);
        item.setTotalTax(totalTaxObj);

        OrderItemsDTO.BaseFare baseFareObj = new OrderItemsDTO.BaseFare();
        baseFareObj.setAmount(totalBase);
        baseFareObj.setCurrency(currency);
        item.setBaseFare(baseFareObj);

        OrderItemsDTO.TotalFare totalFareObj = new OrderItemsDTO.TotalFare();
        totalFareObj.setAmount(totalPrice);
        totalFareObj.setCurrency(currency);
        item.setTotalFare(totalFareObj);

        return item;
    }

    private static void addTaxToList(List<OrderItemsDTO.Tax> list, String description,
            double amount, String currency) {
        if (amount <= 0.001)
            return;
        OrderItemsDTO.Tax t = new OrderItemsDTO.Tax();
        t.setCode("TAX");
        t.setDescription(description);
        t.setAmount(BigDecimal.valueOf(amount));
        t.setCurrency(currency);
        list.add(t);
    }

    private static String formatDateTime(String dateTimeStr) {
        if (dateTimeStr == null) {
            return null;
        }
        try {
            // Flexible parsing (Go7 might return various formats)
            // Example: "2026-02-13 11:55:06" or "2026/02/13 11:55:06"
            DateTimeFormatter inputFormatter;
            if (dateTimeStr.contains("/")) {
                inputFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            } else {
                inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            }
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, inputFormatter);

            // Desired format: "13Feb2026 11:55:06" -> ddMMMyyyy HH:mm:ss
            return dateTime.format(DateTimeFormatter.ofPattern("ddMMMyyyy HH:mm:ss", Locale.ENGLISH));
        } catch (Exception e) {
            // Fallback: try just date parsing if no time
            try {
                return OrderMappingUtil.formatDate(dateTimeStr);
            } catch (Exception ex) {
                return dateTimeStr; // Return raw if all fails
            }
        }
    }
}