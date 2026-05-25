package com.airlines.go7api.controller;

import com.airlines.go7api.responsedto.common.*;

import com.airlines.go7api.request.*;
import com.airlines.go7api.requestdto.*;
import com.airlines.go7api.response.AirshopResponse;
import com.airlines.go7api.response.OfferPriceResponse;
import com.airlines.go7api.responsedto.AirshopRspDto;
import com.airlines.go7api.responsego7.AirshopRspGo7Dto;
import com.airlines.go7api.responsego7.ChangeSeatRspGo7Dto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("java:S120")
@RestController
public class Go7Controller {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Go7Controller.class);
    private static final String INTERNAL_GET_BOOKING_FAILED = "Internal getBooking failed.";
    private static final String CHANGE_SEAT = "ChangeSeat";
    private static final String CHANGE_SERVICE = "ChangeService";

    private final AirshopResponse airshopResponse;
    private final com.airlines.go7api.response.OrderReshopResponse orderReshopResponse;
    private final com.airlines.go7api.response.ServiceListResponse serviceListResponse;
    private final com.airlines.go7api.service.BookingService bookingService;

    @Autowired
    public Go7Controller(
            AirshopResponse airshopResponse,
            com.airlines.go7api.response.OrderReshopResponse orderReshopResponse,
            com.airlines.go7api.response.ServiceListResponse serviceListResponse,
            com.airlines.go7api.service.BookingService bookingService) {
        this.airshopResponse = airshopResponse;
        this.orderReshopResponse = orderReshopResponse;
        this.serviceListResponse = serviceListResponse;
        this.bookingService = bookingService;
    }

    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/airshop")
    public ResponseEntity<Object> flightSearch(@RequestBody AirshopReqDto airShoppingRQ) {
        try {
            // Map the DTO to the internal Request object
            AirshopReq flightSearchRequestDTO = AirshopReq.mapToFlightSearchRequestDTO(airShoppingRQ);

            // Execute the API call and return the raw response (JsonNode or ErrorRsp)
            AirshopRspGo7Dto response = flightSearchRequestDTO.unmarshal();
            AirshopRspDto airshopRspDto = airshopResponse.airshoppingMapper(response, airShoppingRQ);

            return new ResponseEntity<>(airshopRspDto, HttpStatus.OK);

        } catch (InterruptedException e) {
            return handleInterruptedException("Airshop", e);
        } catch (Exception e) {
            return handleException("Airshop", e);
        }
    }

    @PostMapping("/offerprice")
    public ResponseEntity<Object> offerPrice(@RequestBody OfferPriceReqDto offerPriceRQ) {
        try {
            // Map the DTO to the internal Request object
            OfferPriceReq offerPriceRequestDTO = OfferPriceReq.mapToOfferPriceRequestDTO(offerPriceRQ);

            // Generate NDC Response
            Object response = OfferPriceResponse.generateResponse(offerPriceRequestDTO, offerPriceRQ);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (InterruptedException e) {
            return handleInterruptedException("OfferPrice", e);
        } catch (Exception e) {
            return handleException("OfferPrice", e);
        }
    }

    private static final String KEY_AEROCRS = "aerocrs";
    private static final String KEY_BOOKING = "booking";
    private static final String KEY_BOOKING_CONFIRMATION = "bookingconfirmation";
    private static final String KEY_SUCCESS = "success";
    private static final String KEY_BOOKING_ID = "bookingid";
    private static final String KEY_PARMS = "parms";

    @PostMapping("/ordercreate")
    public ResponseEntity<Object> orderCreate(@RequestBody OrderCreateReqDto orderCreateReqDto) {
        try {
            OrderCreateReq orderCreateReq = OrderCreateReq.mapToOrderCreateReq(orderCreateReqDto);
            Object createResponse = orderCreateReq.unmarshal();
            com.airlines.go7api.responsego7.OrderCreateRspGo7Dto go7Response = deserializeResponse(createResponse, com.airlines.go7api.responsego7.OrderCreateRspGo7Dto.class);

            ResponseEntity<Object> errorResponse = checkApiErrorResponse(go7Response);
            if (errorResponse != null) {
                return errorResponse;
            }

            if (go7Response != null && go7Response.getAerocrs() != null && go7Response.getAerocrs().isSuccess()) {
                Long bookingId = go7Response.getAerocrs().getBooking() != null
                        ? go7Response.getAerocrs().getBooking().getBookingid()
                        : null;

                if (bookingId != null && bookingId > 0) {
                    ResponseEntity<Object> confirmResponse = processOrderConfirmAndGetBooking(orderCreateReqDto, bookingId);
                    if (confirmResponse != null) {
                        return confirmResponse;
                    }
                }
            } else {
                logger.info("OrderCreate failed or no booking ID. Skipping OrderConfirm.");
            }

            com.airlines.go7api.responsedto.OrderCreateRspDto ndcResponse = com.airlines.go7api.response.OrderCreateResponse
                    .generateResponse(go7Response, orderCreateReqDto);

            return new ResponseEntity<>(ndcResponse, HttpStatus.OK);

        } catch (InterruptedException e) {
            return handleInterruptedException("OrderCreate", e);
        } catch (Exception e) {
            return handleException("OrderCreate", e);
        }
    }

    private ResponseEntity<Object> processOrderConfirmAndGetBooking(OrderCreateReqDto orderCreateReqDto, Long bookingId) throws java.io.IOException, InterruptedException, javax.xml.datatype.DatatypeConfigurationException {
        logger.info("Booking ID extracted: {}. Proceeding to OrderConfirm.", bookingId);

        OrderCreateReq orderConfirmReq = OrderCreateReq.mapToOrderConfirmReq(orderCreateReqDto, bookingId);
        Object confirmResponse = orderConfirmReq.unmarshal();
        logger.info("OrderConfirm executed. Response: {}", confirmResponse);

        if (confirmResponse instanceof com.airlines.go7api.error.ErrorRsp) {
            return new ResponseEntity<>(confirmResponse, HttpStatus.BAD_REQUEST);
        }

        String bookingConfirmation = extractBookingConfirmation(confirmResponse);

        if (bookingConfirmation != null) {
            ResponseEntity<Object> paymentTicketingResponse = processPaymentAndTicketing(orderCreateReqDto, bookingId);
            if (paymentTicketingResponse != null) {
                return paymentTicketingResponse;
            }

            logger.info("Booking Confirmation: {}. Calling GetBooking.", bookingConfirmation);
            OrderCreateReq getBookingReq = OrderCreateReq.mapToGetBookingReq(bookingConfirmation);
            Object finalResponse = getBookingReq.unmarshal();

            if (finalResponse instanceof com.airlines.go7api.error.ErrorRsp) {
                return new ResponseEntity<>(finalResponse, HttpStatus.BAD_REQUEST);
            }

            com.airlines.go7api.responsego7.OrderCreateRspGo7Dto getBookingResponse = deserializeResponse(finalResponse, com.airlines.go7api.responsego7.OrderCreateRspGo7Dto.class);
            com.airlines.go7api.responsedto.OrderCreateRspDto ndcResponse = com.airlines.go7api.response.OrderCreateResponse
                    .generateResponse(getBookingResponse, orderCreateReqDto);

            persistBookingToDatabase(ndcResponse, bookingConfirmation, orderCreateReqDto);

            return new ResponseEntity<>(ndcResponse, HttpStatus.OK);
        } else {
            logger.info("No Booking Confirmation found in OrderConfirm response.");
            return null;
        }
    }

    private ResponseEntity<Object> checkApiErrorResponse(com.airlines.go7api.responsego7.OrderCreateRspGo7Dto go7Response) {
        if (go7Response != null && go7Response.getDetails() != null
                && go7Response.getDetails().getDetail() != null) {
            com.airlines.go7api.error.ErrorRsp errorRsp = new com.airlines.go7api.error.ErrorRsp();
            for (String msg : go7Response.getDetails().getDetail()) {
                com.airlines.go7api.error.ErrorRsp.Error error = new com.airlines.go7api.error.ErrorRsp.Error();
                error.setError(msg);
                errorRsp.getErrorList().add(error);
            }
            return new ResponseEntity<>(errorRsp, HttpStatus.BAD_REQUEST);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractBookingConfirmation(Object confirmResponse) {
        try {
            java.util.LinkedHashMap<String, Object> responseMap = (java.util.LinkedHashMap<String, Object>) confirmResponse;
            java.util.LinkedHashMap<String, Object> aerocrsMap = (java.util.LinkedHashMap<String, Object>) responseMap.get(KEY_AEROCRS);
            if (aerocrsMap != null) {
                if (aerocrsMap.containsKey(KEY_BOOKING_CONFIRMATION)) {
                    return (String) aerocrsMap.get(KEY_BOOKING_CONFIRMATION);
                } else if (aerocrsMap.containsKey(KEY_BOOKING)) {
                    Object bookingObj = aerocrsMap.get(KEY_BOOKING);
                    if (bookingObj instanceof java.util.LinkedHashMap) {
                        return (String) ((java.util.LinkedHashMap<String, Object>) bookingObj).get(KEY_BOOKING_CONFIRMATION);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to extract bookingconfirmation: {}", e.getMessage());
        }
        return null;
    }

    private ResponseEntity<Object> processPaymentAndTicketing(OrderCreateReqDto orderCreateReqDto, Long bookingId) throws java.io.IOException, InterruptedException, javax.xml.datatype.DatatypeConfigurationException {
        if (orderCreateReqDto.getPaymentInformation() != null) {
            OrderCreateReq makePaymentReq = OrderCreateReq.mapToMakePaymentReq(orderCreateReqDto, bookingId);
            Object paymentResponse = makePaymentReq.unmarshal();
            logger.info("MakePayment executed. Response: {}", paymentResponse);

            if (paymentResponse instanceof com.airlines.go7api.error.ErrorRsp) {
                return new ResponseEntity<>(paymentResponse, HttpStatus.BAD_REQUEST);
            }

            if (!isPaymentSuccessful(paymentResponse)) {
                return new ResponseEntity<>(
                        "MakePayment failed or returned false success. Aborting Ticket issue.",
                        HttpStatus.BAD_REQUEST);
            }

            OrderCreateReq orderTicketReq = OrderCreateReq.mapToOrderTicketReq(orderCreateReqDto, bookingId);
            Object ticketResponse = orderTicketReq.unmarshal();
            logger.info("OrderTicket executed. Response: {}", ticketResponse);

            if (ticketResponse instanceof com.airlines.go7api.error.ErrorRsp) {
                return new ResponseEntity<>(ticketResponse, HttpStatus.BAD_REQUEST);
            }
        }
        return null;
    }

    private boolean isPaymentSuccessful(Object paymentResponse) {
        try {
            ObjectMapper pmMapper = new ObjectMapper();
            String pmJson = pmMapper.writeValueAsString(paymentResponse);
            com.fasterxml.jackson.databind.JsonNode pmRoot = pmMapper.readTree(pmJson);
            if ((pmRoot.has(KEY_SUCCESS) && pmRoot.get(KEY_SUCCESS).asBoolean()) ||
                (pmRoot.has(KEY_AEROCRS) && pmRoot.get(KEY_AEROCRS).has(KEY_SUCCESS) && pmRoot.get(KEY_AEROCRS).get(KEY_SUCCESS).asBoolean())) {
                return true;
            }
        } catch (Exception e) {
            logger.error("Error verifying payment success: {}", e.getMessage());
        }
        return false;
    }

    private void persistBookingToDatabase(com.airlines.go7api.responsedto.OrderCreateRspDto ndcResponse, String bookingConfirmation, OrderCreateReqDto orderCreateReqDto) {
        try {
            String pnr = ndcResponse.getPnr();
            String orderId = ndcResponse.getOrderId();
            if (pnr != null && !pnr.isEmpty()) {
                java.util.Optional<com.airlines.go7api.entity.BookingEntity> existingOpt = bookingService.getBookingByPnr(pnr);
                com.airlines.go7api.entity.BookingEntity entityToSave;
                if (existingOpt.isPresent()) {
                    entityToSave = existingOpt.get();
                } else {
                    entityToSave = new com.airlines.go7api.entity.BookingEntity();
                    entityToSave.setPnr(pnr);
                    entityToSave.setBookingConfirmation(bookingConfirmation);
                    entityToSave.setOrderId(orderId);
                }
                entityToSave.setTotalOrderPrice(ndcResponse.getTotalOrderPrice());

                if (orderCreateReqDto.getPassengers() != null && !orderCreateReqDto.getPassengers().isEmpty()) {
                    entityToSave.setPrimaryPassengerLastName(orderCreateReqDto.getPassengers().get(0).getLastName());
                }

                bookingService.saveBooking(entityToSave);
            }
        } catch (Exception e) {
            logger.error("Failed to save booking to DB: {}", e.getMessage());
        }
    }

    @PostMapping("/change/payment")
    public ResponseEntity<Object> changePayment(@RequestBody ChangePaymentReqDto changePaymentReqDto) {
        try {
            java.util.Map<String, String> passengerSeats = null;
            java.util.Map<String, String> passengerServices = null;
            // Retrieve cached seats and services from BookingEntity if available
            if (changePaymentReqDto.getOrderId() != null && !changePaymentReqDto.getOrderId().isEmpty()) {
                java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOpt = bookingService
                        .getBookingByOrderId(changePaymentReqDto.getOrderId());
                if (entityOpt.isPresent()) {
                    if (entityOpt.get().getPassengerSeats() != null) {
                        passengerSeats = entityOpt.get().getPassengerSeats();
                        System.out.println("ChangePayment: Loaded " + passengerSeats.size()
                                + " cached passenger seat mappings from DB.");
                    }
                    if (entityOpt.get().getPassengerServices() != null) {
                        passengerServices = entityOpt.get().getPassengerServices();
                        System.out.println("ChangePayment: Loaded " + passengerServices.size()
                                + " cached passenger service mappings from DB.");
                    }
                }
            }

            // 0. Pre-fetch GetBooking to calculate combined amount for seats/services if
            // included
            if (changePaymentReqDto.getOrderId() != null && !changePaymentReqDto.getOrderId().isEmpty()) {
                String bookingConfirmationPre = changePaymentReqDto.getOrderId();
                String lastNamePre = null;
                
                java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOptPre = bookingService.getBookingByOrderId(changePaymentReqDto.getOrderId());
                if (entityOptPre.isPresent()) {
                    bookingConfirmationPre = entityOptPre.get().getBookingConfirmation();
                    lastNamePre = entityOptPre.get().getPrimaryPassengerLastName();
                }

                com.airlines.go7api.request.ChangePaymentReq getBookingReqPre = com.airlines.go7api.request.ChangePaymentReq
                        .mapToGetBookingReq(bookingConfirmationPre, lastNamePre);
                Object preResponse = getBookingReqPre.unmarshal();

                if (!(preResponse instanceof com.airlines.go7api.error.ErrorRsp)) {
                    com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto preBooking = deserializeResponse(preResponse, com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto.class);

                    if (preBooking != null && preBooking.getAerocrs() != null
                            && preBooking.getAerocrs().getBooking() != null) {
                        com.airlines.go7api.responsego7.common.BalanceInformation balance = preBooking
                                .getAerocrs().getBooking().getBalanceInformation();
                        if (balance != null && balance.getPnrOutstandingPayment() > 0) {
                            System.out.println(
                                    "ChangePayment: Found outstanding balance: " + balance.getPnrOutstandingPayment());
                            // If seat or service flow is included (cached maps present), combine payment
                            if ((passengerSeats != null && !passengerSeats.isEmpty())
                                    || (passengerServices != null && !passengerServices.isEmpty())) {
                                if (changePaymentReqDto.getPaymentInformation() != null) {
                                    changePaymentReqDto.getPaymentInformation()
                                            .setAmount(BigDecimal.valueOf(balance.getPnrOutstandingPayment()));
                                    System.out.println(
                                            "ChangePayment: Combined payment amount updated with outstanding balance: "
                                                    + balance.getPnrOutstandingPayment());
                                }
                            }
                        }
                    }
                }
            }

            // Map the DTO to the internal Request object
            com.airlines.go7api.request.ChangePaymentReq changePaymentReq = com.airlines.go7api.request.ChangePaymentReq
                    .mapToChangePaymentReq(changePaymentReqDto);

            // Execute the API call
            Object response = changePaymentReq.unmarshal();

            if (response instanceof com.airlines.go7api.error.ErrorRsp) {
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Check details for success if it's a Map/Object
            boolean success = false;
            Long bookingId = null;
            try {
                ObjectMapper pmMapper = new ObjectMapper();
                String pmJson = pmMapper.writeValueAsString(response);
                com.fasterxml.jackson.databind.JsonNode pmRoot = pmMapper.readTree(pmJson);
                if ((pmRoot.has(KEY_SUCCESS) && pmRoot.get(KEY_SUCCESS).asBoolean()) ||
                    (pmRoot.has(KEY_AEROCRS) && pmRoot.get(KEY_AEROCRS).has(KEY_SUCCESS) && pmRoot.get(KEY_AEROCRS).get(KEY_SUCCESS).asBoolean())) {
                    success = true;
                }

                // Extract BookingID for OrderTicket
                if (pmRoot.has(KEY_AEROCRS)) {
                    com.fasterxml.jackson.databind.JsonNode aerocrsNode = pmRoot.get(KEY_AEROCRS);
                    if (aerocrsNode.has(KEY_BOOKING) && aerocrsNode.get(KEY_BOOKING).has(KEY_BOOKING_ID)) {
                        bookingId = aerocrsNode.get(KEY_BOOKING).get(KEY_BOOKING_ID).asLong();
                    } else if (aerocrsNode.has(KEY_BOOKING_ID)) {
                        bookingId = aerocrsNode.get(KEY_BOOKING_ID).asLong();
                    }
                }
            } catch (Exception e) {
                System.out.println("Error verifying change payment success/extracting bookingId: " + e.getMessage());
            }

            if (!success) {
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            String bookingConfirmation = null;

            // Internal Call: OrderTicket (New Addition)
            if (bookingId != null && bookingId > 0) {
                System.out.println("Calling Internal OrderTicket for BookingID: " + bookingId);
                // Use ChangePaymentReq's method
                com.airlines.go7api.request.ChangePaymentReq orderTicketReq = com.airlines.go7api.request.ChangePaymentReq
                        .mapToOrderTicketReq(bookingId);
                Object ticketResponse = orderTicketReq.unmarshal();
                System.out.println("OrderTicket executed. Response: " + ticketResponse);

                if (ticketResponse instanceof com.airlines.go7api.error.ErrorRsp) {
                    return new ResponseEntity<>(ticketResponse, HttpStatus.BAD_REQUEST);
                }

                // Strictly validate OrderTicket success
                boolean ticketSuccess = false;
                try {
                    ObjectMapper ticketMapper = new ObjectMapper();
                    String ticketJson = ticketMapper.writeValueAsString(ticketResponse);
                    com.fasterxml.jackson.databind.JsonNode ticketRoot = ticketMapper.readTree(ticketJson);
                    if ((ticketRoot.has(KEY_SUCCESS) && ticketRoot.get(KEY_SUCCESS).asBoolean()) ||
                        (ticketRoot.has(KEY_AEROCRS) && ticketRoot.get(KEY_AEROCRS).has(KEY_SUCCESS) && ticketRoot.get(KEY_AEROCRS).get(KEY_SUCCESS).asBoolean())) {
                        ticketSuccess = true;
                    }
                } catch (Exception e) {
                    System.out.println("Error verifying OrderTicket success: " + e.getMessage());
                }

                if (!ticketSuccess) {
                    System.out.println("OrderTicket failed. Aborting GetBooking.");
                    return new ResponseEntity<>(ticketResponse, HttpStatus.BAD_REQUEST);
                }

                // Extract bookingconfirmation from Ticket Response if possible
                try {
                    ObjectMapper tMapper = new ObjectMapper();
                    String tJson = tMapper.writeValueAsString(ticketResponse);
                    com.fasterxml.jackson.databind.JsonNode tRoot = tMapper.readTree(tJson);

                    if (tRoot.has(KEY_AEROCRS)) {
                        com.fasterxml.jackson.databind.JsonNode aerocrsNode = tRoot.get(KEY_AEROCRS);

                        // Check direct confirmation at aerocrs root
                        if (aerocrsNode.has(KEY_BOOKING_CONFIRMATION)) {
                            bookingConfirmation = aerocrsNode.get(KEY_BOOKING_CONFIRMATION).asText();
                            System.out.println(
                                    "Extracted bookingconfirmation from OrderTicket (root): " + bookingConfirmation);
                        }
                        // Fallback: Check nested under booking
                        else if (aerocrsNode.has(KEY_BOOKING) && aerocrsNode.get(KEY_BOOKING).has(KEY_BOOKING_CONFIRMATION)) {
                            bookingConfirmation = aerocrsNode.get(KEY_BOOKING).get(KEY_BOOKING_CONFIRMATION).asText();
                            System.out.println(
                                    "Extracted bookingconfirmation from OrderTicket (nested): " + bookingConfirmation);
                        }
                    }
                } catch (Exception e) {
                    System.out.println(
                            "Failed to extract bookingconfirmation from OrderTicket response: " + e.getMessage());
                }

            } else {
                System.out.println("BookingID could not be extracted. Skipping OrderTicket.");
            }

            // Internal Call: GetBooking
            // Logic: Use extracted confirmation or fallback to OrderID
            if (bookingConfirmation == null && changePaymentReqDto.getOrderId() != null) {
                bookingConfirmation = changePaymentReqDto.getOrderId();
                System.out.println("Using OrderID as fallback for bookingconfirmation: " + bookingConfirmation);
            }

            System.out.println("ChangePayment successful. Calling GetBooking for ID: " + bookingConfirmation);

            // Use ChangePaymentReq's method with cached last name and confirmation if available
            String finalLastName = null;
            if (changePaymentReqDto.getOrderId() != null) {
                java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOpt = bookingService.getBookingByOrderId(changePaymentReqDto.getOrderId());
                if (entityOpt.isPresent()) {
                    if (bookingConfirmation == null || bookingConfirmation.equals(changePaymentReqDto.getOrderId())) {
                        bookingConfirmation = entityOpt.get().getBookingConfirmation();
                    }
                    finalLastName = entityOpt.get().getPrimaryPassengerLastName();
                }
            }
            
            com.airlines.go7api.request.ChangePaymentReq getBookingReq = com.airlines.go7api.request.ChangePaymentReq
                    .mapToGetBookingReq(bookingConfirmation, finalLastName);
            Object finalResponse = getBookingReq.unmarshal();

            System.out.println("GetBooking executed. Response: " + finalResponse);

            if (finalResponse instanceof com.airlines.go7api.error.ErrorRsp) {
                return new ResponseEntity<>(finalResponse, HttpStatus.BAD_REQUEST);
            }

            // Convert final response (GetBooking result) to ChangePaymentRspGo7Dto
            com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto getBookingResponse = deserializeResponse(finalResponse, com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto.class);

            // Generate NDC Response using ChangePaymentResponse mapper
            com.airlines.go7api.responsedto.ChangePaymentRspDto ndcResponse = com.airlines.go7api.response.ChangePaymentResponse
                    .generateResponse(getBookingResponse, changePaymentReqDto, passengerSeats, passengerServices);

            if (changePaymentReqDto.getOrderId() != null) {
                java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOpt = bookingService
                        .getBookingByOrderId(changePaymentReqDto.getOrderId());
                if (entityOpt.isPresent()) {
                    com.airlines.go7api.entity.BookingEntity entity = entityOpt.get();
                    if (ndcResponse.getTotalOrderPrice() != null) {
                        entity.setTotalOrderPrice(ndcResponse.getTotalOrderPrice());
                        bookingService.saveBooking(entity);
                    }
                }
            }

            return new ResponseEntity<>(ndcResponse, HttpStatus.OK);

        } catch (Exception e) {
            return handleException("ChangePayment", e);
        }
    }

    @PostMapping("/orderretrieve")
    public ResponseEntity<Object> orderRetrieve(@RequestBody OrderRetrieveReqDto orderRetrieveReqDto) {
        try {
            // DB Lookup Logic
            com.airlines.go7api.entity.BookingEntity bookingEntity = null;

            if (orderRetrieveReqDto.getOrderId() != null && !orderRetrieveReqDto.getOrderId().isEmpty()) {
                System.out.println("Looking up by OrderID: " + orderRetrieveReqDto.getOrderId());
                java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOpt = bookingService
                        .getBookingByOrderId(orderRetrieveReqDto.getOrderId());
                if (entityOpt.isPresent()) {
                    bookingEntity = entityOpt.get();
                    System.out.println("Found Booking in DB by OrderID: " + bookingEntity.getOrderId() + " (PNR: "
                            + bookingEntity.getPnr() + ")");
                } else {
                    System.out.println("Booking not found in DB by OrderID.");
                }
            }

            // If not found by OrderID (or OrderID not provided), and PNR is present, look
            // up by PNR
            if (bookingEntity == null && orderRetrieveReqDto.getPnr() != null
                    && !orderRetrieveReqDto.getPnr().isEmpty()) {
                System.out.println("Looking up by PNR: " + orderRetrieveReqDto.getPnr());
                java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOpt = bookingService
                        .getBookingByPnr(orderRetrieveReqDto.getPnr());
                if (entityOpt.isPresent()) {
                    bookingEntity = entityOpt.get();
                    System.out.println("Found Booking in DB by PNR: " + bookingEntity.getPnr());
                } else {
                    System.out.println("Booking not found in DB by PNR.");
                }
            }

            // Populate DTO with retrieved data if PNR was missing
            if (bookingEntity != null) {
                if (orderRetrieveReqDto.getPnr() == null || orderRetrieveReqDto.getPnr().isEmpty()) {
                    orderRetrieveReqDto.setPnr(bookingEntity.getPnr());
                }
            }

            String storedBookingConfirmation = null;
            if (bookingEntity != null) {
                storedBookingConfirmation = bookingEntity.getBookingConfirmation();
                if (storedBookingConfirmation != null) {
                    System.out.println("Using BookingConfirmation from DB: " + storedBookingConfirmation);
                }
            }

            // Map the DTO to the internal Request object
            OrderRetrieveReq orderRetrieveReq = OrderRetrieveReq.mapToOrderRetrieveReq(orderRetrieveReqDto,
                    storedBookingConfirmation);

            // Execute the API call and return the raw response
            Object response = orderRetrieveReq.unmarshal();

            if (response instanceof com.airlines.go7api.error.ErrorRsp) {
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Convert raw response to Go7 DTO
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto go7Response = deserializeResponse(response, com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto.class);

            java.util.Map<String, String> passengerSeats = null;
            java.util.Map<String, String> passengerServices = null;

            java.math.BigDecimal dbTotalOrderPrice = null;

            if (bookingEntity != null) {
                passengerSeats = bookingEntity.getPassengerSeats();
                passengerServices = bookingEntity.getPassengerServices();
                dbTotalOrderPrice = bookingEntity.getTotalOrderPrice();
            }

            // Generate NDC Response
            com.airlines.go7api.responsedto.OrderRetrieveRspDto ndcResponse = com.airlines.go7api.response.OrderRetrieveResponse
                    .generateResponse(go7Response, orderRetrieveReqDto, passengerSeats, passengerServices,
                            dbTotalOrderPrice);

            // SYNC: Update database with any new info from OrderRetrieve (including last name if missing)
            if (bookingEntity != null && go7Response != null && go7Response.getAerocrs() != null && go7Response.getAerocrs().getBooking() != null) {
                boolean changed = false;
                if (bookingEntity.getPrimaryPassengerLastName() == null || bookingEntity.getPrimaryPassengerLastName().isEmpty()) {
                    com.airlines.go7api.responsego7.common.Passengers pList = go7Response.getAerocrs().getBooking().getPassengers();
                    if (pList != null && pList.getPassenger() != null && !pList.getPassenger().isEmpty()) {
                        bookingEntity.setPrimaryPassengerLastName(pList.getPassenger().get(0).getLastname());
                        changed = true;
                    }
                }
                if (changed) {
                    bookingService.saveBooking(bookingEntity);
                }
            }

            return new ResponseEntity<>(ndcResponse, HttpStatus.OK);

        } catch (InterruptedException e) {
            return handleInterruptedException("OrderRetrieve", e);
        } catch (Exception e) {
            return handleException("OrderRetrieve", e);
        }
    }

    @PostMapping("/unpaidcancel")
    public ResponseEntity<Object> unpaidCancel(@RequestBody UnpaidCancelReqDto unpaidCancelReqDto) {
        try {
            System.out.println("Processing UnpaidCancel for OrderID: " + unpaidCancelReqDto.getOrderId());

            // --- VALIDATION: Check if already Ticketed/Paid ---
            String storedBookingConfirmationValidation = null;
            if (unpaidCancelReqDto.getOrderId() != null && !unpaidCancelReqDto.getOrderId().isEmpty()) {
                java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOpt = bookingService
                        .getBookingByOrderId(unpaidCancelReqDto.getOrderId());
                if (entityOpt.isPresent()) {
                    storedBookingConfirmationValidation = entityOpt.get().getBookingConfirmation();
                }
            }

            com.airlines.go7api.requestdto.OrderRetrieveReqDto tempRetrieveReq = new com.airlines.go7api.requestdto.OrderRetrieveReqDto();
            tempRetrieveReq.setOrderId(unpaidCancelReqDto.getOrderId());
            tempRetrieveReq.setApiKey(unpaidCancelReqDto.getApiKey());

            com.airlines.go7api.request.OrderRetrieveReq validationBookingReq = com.airlines.go7api.request.OrderRetrieveReq
                    .mapToOrderRetrieveReq(tempRetrieveReq, storedBookingConfirmationValidation);
            Object validationResponse = validationBookingReq.unmarshal();

            if (!(validationResponse instanceof com.airlines.go7api.error.ErrorRsp)) {
                com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp = null;
                try {
                    bookingRsp = deserializeResponse(validationResponse, com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto.class);
                } catch (Exception e) {
                    // Ignored: If deserialization fails, validation fails safely via subsequent null-check on bookingRsp
                }

                if (bookingRsp != null && bookingRsp.getAerocrs() != null
                        && bookingRsp.getAerocrs().getBooking() != null) {
                    boolean hasTickets = false;
                    if (bookingRsp.getAerocrs().getBooking().getPassengers() != null
                            && bookingRsp.getAerocrs().getBooking().getPassengers().getPassenger() != null) {
                        for (com.airlines.go7api.responsego7.common.Passenger p : bookingRsp
                                .getAerocrs().getBooking().getPassengers().getPassenger()) {
                            if (p.getETickets() != null && p.getETickets().getFlight() != null
                                    && !p.getETickets().getFlight().isEmpty()) {
                                hasTickets = true;
                                break;
                            }
                        }
                    }
                    if (hasTickets) {
                        com.airlines.go7api.error.ErrorRsp errorRsp = new com.airlines.go7api.error.ErrorRsp();
                        errorRsp.setAirlineCode("G7");
                        errorRsp.setSource("G7-API");
                        com.airlines.go7api.error.ErrorRsp.Error error = new com.airlines.go7api.error.ErrorRsp.Error();
                        error.setError(
                                "Cannot perform UnpaidCancel. Booking is ticketed and can not be canceled from this interface at the moment, please consult airline.");
                        error.setCode("400");
                        errorRsp.getErrorList().add(error);
                        return new ResponseEntity<>(errorRsp, HttpStatus.BAD_REQUEST);
                    }

                    // Additional Check: Balance/Paid Status
                    if (bookingRsp.getAerocrs().getBooking().getBalanceInformation() != null) {
                        double outstanding = bookingRsp.getAerocrs().getBooking().getBalanceInformation()
                                .getPnrOutstandingPayment();
                        if (outstanding <= 0) {
                            com.airlines.go7api.error.ErrorRsp errorRsp = new com.airlines.go7api.error.ErrorRsp();
                            errorRsp.setAirlineCode("G7");
                            errorRsp.setSource("G7-API");
                            com.airlines.go7api.error.ErrorRsp.Error error = new com.airlines.go7api.error.ErrorRsp.Error();
                            error.setError("Cannot perform UnpaidCancel. The order has already been fully paid.");
                            error.setCode("400");
                            errorRsp.getErrorList().add(error);
                            return new ResponseEntity<>(errorRsp, HttpStatus.BAD_REQUEST);
                        }
                    }

                    // Additional Check: Status
                    String status = bookingRsp.getAerocrs().getBooking().getStatus();
                    if ("OK".equalsIgnoreCase(status) || "TICKETED".equalsIgnoreCase(status)) {
                        // If it's OK but not caught by hasTickets, we still might want to be cautious,
                        // but usually hasTickets is the definitive check for 'Unpaid'.
                        // For now, let's stick to hasTickets and Balance.
                    }
                }
            }
            // --- END VALIDATION ---

            // 1. Execute UnpaidCancel Request
            UnpaidCancelReq unpaidCancelReq = UnpaidCancelReq.mapToUnpaidCancelReq(unpaidCancelReqDto);
            Object cancelResponse = unpaidCancelReq.unmarshal();

            if (cancelResponse instanceof com.airlines.go7api.error.ErrorRsp) {
                return new ResponseEntity<>(cancelResponse, HttpStatus.BAD_REQUEST);
            }

            // --- Post-call Validation: Handle provider-side failure without 'errors' array
            // ---
            try {
                ObjectMapper tempMapper = new ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = tempMapper.valueToTree(cancelResponse);
                if (root.has(KEY_SUCCESS) && !root.get(KEY_SUCCESS).asBoolean()) {
                    com.airlines.go7api.error.ErrorRsp errorRsp = new com.airlines.go7api.error.ErrorRsp();
                    errorRsp.setAirlineCode("G7");
                    errorRsp.setSource("G7-API");
                    String errMsg = "Cancellation failed on provider side.";
                    if (root.has("details") && root.get("details").has("detail")) {
                        com.fasterxml.jackson.databind.JsonNode detailNode = root.get("details").get("detail");
                        if (detailNode.isArray() && detailNode.size() > 0) {
                            errMsg = detailNode.get(0).asText();
                        }
                    }
                    com.airlines.go7api.error.ErrorRsp.Error error = new com.airlines.go7api.error.ErrorRsp.Error();
                    error.setError(errMsg);
                    error.setCode("400");
                    errorRsp.getErrorList().add(error);
                    return new ResponseEntity<>(errorRsp, HttpStatus.BAD_REQUEST);
                }
            } catch (Exception e) {
                System.out.println("Error parsing cancelResponse for failure: " + e.getMessage());
            }
            // --- End Post-call Validation ---

            // 2. Fetch Full Booking Details (Internal OrderRetrieve)
            // 2. Fetch Full Booking Details (Internal OrderRetrieve)
            System.out.println("UnpaidCancel executed. Calling internal GetBooking for OrderID: "
                    + unpaidCancelReqDto.getOrderId());

            // DB Lookup Logic for BookingConfirmation (Matches OrderRetrieve flow)
            String storedBookingConfirmation = null;
            if (unpaidCancelReqDto.getOrderId() != null && !unpaidCancelReqDto.getOrderId().isEmpty()) {
                java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOpt = bookingService
                        .getBookingByOrderId(unpaidCancelReqDto.getOrderId());
                if (entityOpt.isPresent()) {
                    storedBookingConfirmation = entityOpt.get().getBookingConfirmation();
                    System.out.println(
                            "Found Booking in DB. Using stored BookingConfirmation: " + storedBookingConfirmation);
                } else {
                    System.out.println("Booking not found in DB by OrderID during UnpaidCancel.");
                }
            }

            // Fallback: Extract from cancel response if DB lookup failed
            if (storedBookingConfirmation == null) {
                if (cancelResponse instanceof String) {
                    try {
                        ObjectMapper tempMapper = new ObjectMapper();
                        com.fasterxml.jackson.databind.JsonNode root = tempMapper.readTree((String) cancelResponse);
                        if (root.has(KEY_AEROCRS) && root.get(KEY_AEROCRS).has(KEY_BOOKING_CONFIRMATION)) {
                            storedBookingConfirmation = root.get(KEY_AEROCRS).get(KEY_BOOKING_CONFIRMATION).asText();
                        } else if (root.has(KEY_BOOKING_CONFIRMATION)) {
                            storedBookingConfirmation = root.get(KEY_BOOKING_CONFIRMATION).asText();
                        }
                    } catch (Exception e) {
                    }
                } else {
                    try {
                        ObjectMapper tempMapper = new ObjectMapper();
                        com.fasterxml.jackson.databind.JsonNode root = tempMapper.valueToTree(cancelResponse);
                        if (root.has(KEY_AEROCRS) && root.get(KEY_AEROCRS).has(KEY_BOOKING_CONFIRMATION)) {
                            storedBookingConfirmation = root.get(KEY_AEROCRS).get(KEY_BOOKING_CONFIRMATION).asText();
                        }
                    } catch (Exception e) {
                    }
                }
            }

            com.airlines.go7api.requestdto.OrderRetrieveReqDto retrieveReqDto = new com.airlines.go7api.requestdto.OrderRetrieveReqDto();
            retrieveReqDto.setOrderId(unpaidCancelReqDto.getOrderId());
            retrieveReqDto.setApiKey(unpaidCancelReqDto.getApiKey());

            // Allow mapper to decide based on presence of storedBookingConfirmation
            // If storedBookingConfirmation is present, it will be used (and usually works
            // without surname)
            // If null, it falls back to OrderID (which might require surname)
            com.airlines.go7api.request.OrderRetrieveReq getBookingReq = com.airlines.go7api.request.OrderRetrieveReq
                    .mapToOrderRetrieveReq(retrieveReqDto, storedBookingConfirmation);

            Object bookingResponse = getBookingReq.unmarshal();

            if (bookingResponse instanceof com.airlines.go7api.error.ErrorRsp) {
                System.out.println("Failed to retrieve booking details after cancellation.");
                return new ResponseEntity<>(bookingResponse, HttpStatus.BAD_REQUEST);
            }

            // 3. Convert Booking Response to UnpaidCancelRspGo7Dto
            com.airlines.go7api.responsego7.UnpaidCancelRspGo7Dto fullBooking = deserializeResponse(bookingResponse, com.airlines.go7api.responsego7.UnpaidCancelRspGo7Dto.class);

            // 4. Generate NDC Response
            com.airlines.go7api.responsedto.UnpaidCancelRspDto ndcResponse = com.airlines.go7api.response.UnpaidCancelResponse
                    .generateResponse(fullBooking, unpaidCancelReqDto);

            return new ResponseEntity<>(ndcResponse, HttpStatus.OK);

        } catch (InterruptedException e) {
            return handleInterruptedException("UnpaidCancel", e);
        } catch (Exception e) {
            return handleException("UnpaidCancel", e);
        }
    }

        private ResponseEntity<Object> handleInterruptedException(String requestName, InterruptedException e) {
        Thread.currentThread().interrupt();
        return new ResponseEntity<>("Error occurred during " + requestName + " request: " + e.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Object> handleException(String requestName, Exception e) {
        // Exception ignored or handled by fallback
        return new ResponseEntity<>("Error occurred during " + requestName + " request: " + e.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private <T> T deserializeResponse(Object response, Class<T> targetClass) throws com.fasterxml.jackson.core.JsonProcessingException {
        if (targetClass.isInstance(response)) {
            return targetClass.cast(response);
        } else if (response instanceof String str) {
            return mapper.readValue(str, targetClass);
        } else {
            return mapper.convertValue(response, targetClass);
        }
    }

    private com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto fetchBookingDetails(String orderId) throws javax.xml.datatype.DatatypeConfigurationException, java.io.IOException, InterruptedException {
        String bookingConfirmation = null;
        if (orderId != null) {
            java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOpt = bookingService
                    .getBookingByOrderId(orderId);
            if (entityOpt.isPresent()) {
                bookingConfirmation = entityOpt.get().getBookingConfirmation();
            } else {
                bookingConfirmation = orderId;
            }
        }
        com.airlines.go7api.request.OrderRetrieveReq getBookingReq = com.airlines.go7api.request.OrderRetrieveReq
                .mapToOrderRetrieveReq(new com.airlines.go7api.requestdto.OrderRetrieveReqDto(), bookingConfirmation);
        Object getBookingResponse = getBookingReq.unmarshal();
        if (getBookingResponse instanceof com.airlines.go7api.error.ErrorRsp) {
            return null;
        }
        return deserializeResponse(getBookingResponse, com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto.class);
    }

    private ResponseEntity<Object> processAmendmentPayment(
            String orderId,
            com.airlines.go7api.requestdto.ChangePaymentReqDto.PaymentInformation payInfo,
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp)
            throws java.io.IOException {

        // 1. Create ChangePaymentReqDto
        com.airlines.go7api.requestdto.ChangePaymentReqDto paymentReqDto = new com.airlines.go7api.requestdto.ChangePaymentReqDto();
        paymentReqDto.setOrderId(orderId);
        paymentReqDto.setPaymentInformation(payInfo);

        // 2. Map and Execute ChangePayment
        com.airlines.go7api.request.ChangePaymentReq changePaymentReq = com.airlines.go7api.request.ChangePaymentReq
                .mapToChangePaymentReq(paymentReqDto);

        Object paymentResponse = changePaymentReq.unmarshal();
        logger.info("ChangePayment executed. Response: {}", paymentResponse);

        // Verify Payment Success
        boolean paymentSuccess = false;
        try {
            String pmJson = mapper.writeValueAsString(paymentResponse);
            com.fasterxml.jackson.databind.JsonNode pmRoot = mapper.readTree(pmJson);
            if ((pmRoot.has(KEY_SUCCESS) && pmRoot.get(KEY_SUCCESS).asBoolean()) ||
                (pmRoot.has(KEY_AEROCRS) && pmRoot.get(KEY_AEROCRS).has(KEY_SUCCESS) && pmRoot.get(KEY_AEROCRS).get(KEY_SUCCESS).asBoolean())) {
                paymentSuccess = true;
            }
        } catch (Exception e) {
            logger.error("Error verifying payment success: {}", e.getMessage());
        }

        if (paymentSuccess) {
            Long bookingIdForTicket = null;
            if (bookingRsp != null && bookingRsp.getAerocrs() != null
                    && bookingRsp.getAerocrs().getBooking() != null) {
                bookingIdForTicket = bookingRsp.getAerocrs().getBooking().getBookingid();
            }

            if (bookingIdForTicket != null) {
                logger.info("Calling Internal OrderTicket for BookingID: {}", bookingIdForTicket);
                com.airlines.go7api.request.ChangePaymentReq orderTicketReq = com.airlines.go7api.request.ChangePaymentReq
                        .mapToOrderTicketReq(bookingIdForTicket);
                Object ticketResponse = orderTicketReq.unmarshal();
                logger.info("OrderTicket executed. Response: {}", ticketResponse);
            }
            return null; // Indicates success
        } else {
            logger.warn("Payment failed. Returning error response.");
            return new ResponseEntity<>(paymentResponse, HttpStatus.BAD_REQUEST);
        }
    }

    private com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto refreshBookingDetails(
            String bookingConfirmation,
            String changeType,
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto defaultRsp) {
        logger.info("Refreshing Booking after {} to capture price updates...", changeType);
        try {
            com.airlines.go7api.request.OrderRetrieveReq refreshReq = SeatAvailabilityReq.mapToGetBookingReq(bookingConfirmation);
            Object refreshResp = refreshReq.unmarshal();
            if (!(refreshResp instanceof com.airlines.go7api.error.ErrorRsp)) {
                return deserializeResponse(refreshResp, com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto.class);
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted refreshing booking after {}: {}", changeType, e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Error refreshing booking after {}: {}", changeType, e.getMessage());
        }
        return defaultRsp;
    }

    private ServiceListReqDto parseServiceListReqDto(String requestBody, ObjectMapper mapper) throws com.fasterxml.jackson.core.JsonProcessingException {
        com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(requestBody);
        ServiceListReqDto serviceListReqDto = new ServiceListReqDto();
        // Handle nested "aerocrs.parms" structure
        if (rootNode.has(KEY_AEROCRS) && rootNode.get(KEY_AEROCRS).has(KEY_PARMS)) {
            com.fasterxml.jackson.databind.JsonNode parms = rootNode.get(KEY_AEROCRS).get(KEY_PARMS);
            if (parms.has(KEY_BOOKING_ID)) {
                serviceListReqDto.setOrderId(parms.get(KEY_BOOKING_ID).asText());
            }
        } else {
            // Fallback: Try to map directly if structure is flat or matches DTO
            serviceListReqDto = mapper.readValue(requestBody, ServiceListReqDto.class);
        }
        return serviceListReqDto;
    }

    @PostMapping("/servicelist")
    public ResponseEntity<Object> serviceList(@RequestBody String requestBody) {
        try {
            ServiceListReqDto serviceListReqDto = parseServiceListReqDto(requestBody, mapper);

            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp = fetchBookingDetails(serviceListReqDto.getOrderId());
            if (bookingRsp == null) {
                logger.warn("ServiceList: Internal getBooking failed.");
                return new ResponseEntity<>(INTERNAL_GET_BOOKING_FAILED, HttpStatus.BAD_REQUEST);
            }

            // Map to ServiceListReq (populating flightid and currency from booking)
            ServiceListReq serviceListReq = ServiceListReq.mapToServiceListRequestDTO(serviceListReqDto, bookingRsp);

            // Execute ServiceList API call
            Object response = serviceListReq.unmarshal();

            // Map to NDC Response
            com.airlines.go7api.responsego7.ServiceListRspGo7Dto serviceListRspGo7 = deserializeResponse(response, com.airlines.go7api.responsego7.ServiceListRspGo7Dto.class);

            com.airlines.go7api.responsedto.ServiceListRspDto ndcResponse = serviceListResponse
                    .generateResponse(serviceListRspGo7, serviceListReqDto, bookingRsp);

            return new ResponseEntity<>(ndcResponse, HttpStatus.OK);

        } catch (InterruptedException e) {
            return handleInterruptedException("ServiceList", e);
        } catch (Exception e) {
            return handleException("ServiceList", e);
        }
    }

    @PostMapping("/seatavailability")
    public ResponseEntity<Object> seatAvailability(@RequestBody SeatAvailabilityReqDto seatAvailabilityReqDto) {
        try {
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp = fetchBookingDetails(seatAvailabilityReqDto.getOrderId());
            if (bookingRsp == null) {
                logger.warn("SeatAvailability: Internal getBooking failed.");
                return new ResponseEntity<>(INTERNAL_GET_BOOKING_FAILED, HttpStatus.BAD_REQUEST);
            }

            // Map the DTO to the internal Request object (using booking details)
            SeatAvailabilityReq seatAvailabilityReq = SeatAvailabilityReq
                    .mapToSeatAvailabilityRequestDTO(seatAvailabilityReqDto, bookingRsp);

            // Execute the API call
            Object response = seatAvailabilityReq.unmarshal();

            // Check for Error Response
            if (response instanceof com.airlines.go7api.error.ErrorRsp) {
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Convert to Go7 DTO
            com.airlines.go7api.responsego7.SeatAvailabilityRspGo7Dto go7Rsp = deserializeResponse(response, com.airlines.go7api.responsego7.SeatAvailabilityRspGo7Dto.class);

            com.airlines.go7api.responsedto.SeatAvailabilityRspDto mappedRsp = com.airlines.go7api.response.SeatAvailabilityResponse
                    .mapToSeatAvailabilityRspDto(go7Rsp, seatAvailabilityReqDto.getOrderId(), bookingRsp);

            return new ResponseEntity<>(mappedRsp, HttpStatus.OK);

        } catch (InterruptedException e) {
            return handleInterruptedException("SeatAvailability", e);
        } catch (Exception e) {
            return handleException("SeatAvailability", e);
        }
    }

    @PostMapping("/change/seat")
    public ResponseEntity<Object> changeSeat(@RequestBody ChangeSeatReqDto changeSeatReqDto) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
                    true);

            String orderId = changeSeatReqDto.getOrderId();
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp = fetchBookingDetails(orderId);
            if (bookingRsp == null) {
                logger.warn("ChangeSeat: Internal getBooking failed.");
                return new ResponseEntity<>(INTERNAL_GET_BOOKING_FAILED, HttpStatus.BAD_REQUEST);
            }
            String bookingConfirmation = bookingRsp.getAerocrs() != null && bookingRsp.getAerocrs().getBooking() != null
                    ? bookingRsp.getAerocrs().getBooking().getBookingconfirmation()
                    : orderId;

            // 4. Map to ChangeSeatReq
            com.airlines.go7api.request.ChangeSeatReq changeSeatReq = com.airlines.go7api.request.ChangeSeatReq
                    .mapToChangeSeatReq(changeSeatReqDto, bookingRsp);

            // 5. Execute API Call
            Object response = changeSeatReq.unmarshal();

            ChangeSeatRspGo7Dto changeSeatRsp = deserializeResponse(response, ChangeSeatRspGo7Dto.class);

            if (changeSeatRsp != null) {
                // Determine if at least one seat was successfully assigned
                boolean anySeatSuccessful = false;
                if (changeSeatRsp.getAerocrs() != null && changeSeatRsp.getAerocrs().getFlights() != null) {
                    for (com.airlines.go7api.responsego7.common.Flight f : changeSeatRsp.getAerocrs().getFlights().getFlight()) {
                        if (f.getSeat() != null) {
                            for (com.airlines.go7api.responsego7.common.Seat s : f.getSeat()) {
                                if (Boolean.TRUE.equals(s.isStatus())) {
                                    anySeatSuccessful = true;
                                    break;
                                }
                            }
                        }
                        if (anySeatSuccessful) break;
                    }
                }

                // Conditional Payment & Ticketing Logic
                if (anySeatSuccessful && changeSeatReqDto.getPaymentInformation() != null 
                    && changeSeatReqDto.getPaymentInformation().getCardNumber() != null 
                    && !changeSeatReqDto.getPaymentInformation().getCardNumber().isEmpty()) {
                    
                    com.airlines.go7api.requestdto.ChangePaymentReqDto.PaymentInformation payInfo = mapper.convertValue(
                            changeSeatReqDto.getPaymentInformation(),
                            com.airlines.go7api.requestdto.ChangePaymentReqDto.PaymentInformation.class
                    );
                    ResponseEntity<Object> paymentResult = processAmendmentPayment(orderId, payInfo, bookingRsp);
                    if (paymentResult != null) {
                        return paymentResult;
                    }
                }

                // ALWAYS Refresh Booking (GetBooking) after change to get updated PNR total and balance
                bookingRsp = refreshBookingDetails(bookingConfirmation, CHANGE_SEAT, bookingRsp);

                // --- Real-time Price Fetching (Added to Controller) ---
                java.util.Map<String, BigDecimal> actualSeatPrices = new java.util.HashMap<>();
                if (changeSeatRsp != null && changeSeatRsp.getAerocrs() != null
                        && changeSeatRsp.getAerocrs().getFlights() != null) {
                    for (com.airlines.go7api.responsego7.common.Flight f : changeSeatRsp.getAerocrs()
                            .getFlights().getFlight()) {
                        if (f.getSeat() != null) {
                            for (com.airlines.go7api.responsego7.common.Seat s : f.getSeat()) {
                                if (s.getSeatNumber() != null
                                        && (s.getFare() == null || s.getFare().compareTo(BigDecimal.ZERO) <= 0)) {
                                    BigDecimal actualFare = fetchActualSeatFare(
                                            bookingRsp.getAerocrs().getBooking().getBookingid(),
                                            f.getNumber(), f.getFlightdate(), f.getFromcode(), f.getTocode(),
                                            s.getSeatNumber(), f.getFlightClass());
                                    if (actualFare != null) {
                                        actualSeatPrices.put(s.getSeatNumber(), actualFare);
                                    }
                                }
                            }
                        }
                    }
                }
                // -----------------------------------------------------

                // Load cached maps from DB to pass to mapper for cumulative response
                java.util.Map<String, String> cachedSeats = null;
                java.util.Map<String, String> cachedServices = null;
                java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOptForMaps = bookingService.getBookingByOrderId(orderId);
                if (entityOptForMaps.isPresent()) {
                    cachedSeats = entityOptForMaps.get().getPassengerSeats();
                    cachedServices = entityOptForMaps.get().getPassengerServices();
                }

                com.airlines.go7api.responsedto.ChangeSeatRspDto ndcResponse = com.airlines.go7api.response.ChangeSeatResponse
                        .generateResponse(changeSeatRsp, bookingRsp, changeSeatReqDto, actualSeatPrices, cachedSeats, cachedServices);

                // Cache the newly assigned seats into the BookingEntity so ChangePayment can
                // use them
                if (orderId != null && ndcResponse.getOrderItems() != null) {
                    java.util.Map<String, String> passengerSeats = new java.util.HashMap<>();
                    for (com.airlines.go7api.responsedto.common.OrderItemsDTO item : ndcResponse
                            .getOrderItems()) {
                        if (item.getServiceList() != null) {
                            for (com.airlines.go7api.responsedto.common.Service srv : item.getServiceList()) {
                                if (srv.getServiceCode() != null && srv.getServiceCode().startsWith("SEAT")) {
                                    String seatNum = srv.getServiceCode().substring(4);
                                    if (item.getPassengerIds() != null && !item.getPassengerIds().isEmpty()) {
                                        BigDecimal price = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
                                        passengerSeats.put(item.getPassengerIds().get(0), seatNum + "|" + price);
                                    }
                                }
                            }
                        }
                    }
                    if (!passengerSeats.isEmpty()) {
                        java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOpt = bookingService
                                .getBookingByOrderId(orderId);
                        if (entityOpt.isPresent()) {
                            com.airlines.go7api.entity.BookingEntity entity = entityOpt.get();

                            // Merge with existing seats if any
                            java.util.Map<String, String> existingSeats = entity.getPassengerSeats();
                            if (existingSeats == null) {
                                existingSeats = new java.util.HashMap<>();
                            }
                            for (java.util.Map.Entry<String, String> newSeat : passengerSeats.entrySet()) {
                                existingSeats.put(newSeat.getKey(), newSeat.getValue());
                            }

                            entity.setPassengerSeats(existingSeats);
                            
                            // SYNC: Capture last name if missing
                            if (entity.getPrimaryPassengerLastName() == null || entity.getPrimaryPassengerLastName().isEmpty()) {
                                if (bookingRsp != null && bookingRsp.getAerocrs() != null && bookingRsp.getAerocrs().getBooking() != null) {
                                    com.airlines.go7api.responsego7.common.Passengers pList = bookingRsp.getAerocrs().getBooking().getPassengers();
                                    if (pList != null && pList.getPassenger() != null && !pList.getPassenger().isEmpty()) {
                                        entity.setPrimaryPassengerLastName(pList.getPassenger().get(0).getLastname());
                                    }
                                }
                            }

                            if (ndcResponse.getTotalOrderPrice() != null) {
                                entity.setTotalOrderPrice(ndcResponse.getTotalOrderPrice());
                            }
                            bookingService.saveBooking(entity);
                            System.out.println("ChangeSeat: Cached " + passengerSeats.size()
                                    + " seat assignments to BookingEntity for OrderID: " + orderId);
                        }
                    }
                }

                return new ResponseEntity<>(ndcResponse, HttpStatus.OK);
            }

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (InterruptedException e) {
            return handleInterruptedException(CHANGE_SEAT, e);
        } catch (Exception e) {
            return handleException(CHANGE_SEAT, e);
        }
    }

    @PostMapping("/change/service")
    public ResponseEntity<Object> orderService(@RequestBody ChangeServiceReqDto changeServiceReqDto) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
                    true);

            String orderId = changeServiceReqDto.getOrderId();
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp = fetchBookingDetails(orderId);
            if (bookingRsp == null) {
                logger.warn("ChangeService: Internal getBooking failed.");
                return new ResponseEntity<>(INTERNAL_GET_BOOKING_FAILED, HttpStatus.BAD_REQUEST);
            }
            String bookingConfirmation = bookingRsp.getAerocrs() != null && bookingRsp.getAerocrs().getBooking() != null
                    ? bookingRsp.getAerocrs().getBooking().getBookingconfirmation()
                    : orderId;

            if (bookingRsp == null || bookingRsp.getAerocrs() == null || !bookingRsp.getAerocrs().isSuccess()) {
                return new ResponseEntity<>("Error: Could not retrieve booking for OrderID: " + orderId,
                        HttpStatus.BAD_REQUEST);
            }

            // 2. Map DTO to ChangeServiceReq (which converts NDC to Go7 structure)
            ChangeServiceReq changeServiceReq = ChangeServiceReq.mapToChangeServiceReq(changeServiceReqDto, bookingRsp);

            // 3. Execute ChangeService API
            Object response = changeServiceReq.unmarshal();

            com.airlines.go7api.responsego7.ChangeServiceRspGo7Dto changeServiceRsp = deserializeResponse(response, com.airlines.go7api.responsego7.ChangeServiceRspGo7Dto.class);

            if (changeServiceRsp != null) {
                logger.info("ChangeService: changeServiceRsp is NOT null. Success: {}",
                        (changeServiceRsp.getAerocrs() != null ? changeServiceRsp.getAerocrs().isSuccess() : "null"));
                // 1. Strict Error Handling
                if (changeServiceRsp.getAerocrs() != null && !changeServiceRsp.getAerocrs().isSuccess()) {
                    logger.warn("ChangeService: AeroCRS reported failure, returning 400.");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }

                // 2. Conditional Payment & Ticketing Logic
                // Determine if at least one service was successfully added
                boolean anyServiceSuccessful = false;
                if (changeServiceRsp != null && changeServiceRsp.getAerocrs() != null && changeServiceRsp.getAerocrs().getDetails() != null) {
                    for (com.airlines.go7api.responsego7.common.Aerocrs.Detail d : changeServiceRsp.getAerocrs().getDetails()) {
                        if (d.isSuccess()) {
                            anyServiceSuccessful = true;
                            break;
                        }
                    }
                }

                if (anyServiceSuccessful && changeServiceReqDto.getPaymentInformation() != null 
                    && changeServiceReqDto.getPaymentInformation().getCardNumber() != null 
                    && !changeServiceReqDto.getPaymentInformation().getCardNumber().isEmpty()) {
                    
                    com.airlines.go7api.requestdto.ChangePaymentReqDto.PaymentInformation payInfo = mapper.convertValue(
                            changeServiceReqDto.getPaymentInformation(),
                            com.airlines.go7api.requestdto.ChangePaymentReqDto.PaymentInformation.class
                    );
                    ResponseEntity<Object> paymentResult = processAmendmentPayment(orderId, payInfo, bookingRsp);
                    if (paymentResult != null) {
                        return paymentResult;
                    }
                }

                // ALWAYS Refresh Booking (GetBooking) after change to get updated PNR total and balance
                bookingRsp = refreshBookingDetails(bookingConfirmation, CHANGE_SERVICE, bookingRsp);

                // --- Real-time Price Fetching (Added to Controller) ---
                java.util.Map<String, BigDecimal> actualServicePrices = new java.util.HashMap<>();
                if (changeServiceRsp != null && changeServiceRsp.getAerocrs() != null
                        && changeServiceRsp.getAerocrs().getDetails() != null) {
                    for (com.airlines.go7api.responsego7.common.Aerocrs.Detail detail : changeServiceRsp
                            .getAerocrs().getDetails()) {
                        if (detail.getAncillary() != null) {
                            Object itemIdObj = detail.getAncillary().get("itemid");
                            Object flightIdObj = detail.getAncillary().get("flightid");
                            Object priceObj = detail.getAncillary().get("totalprice");
                            BigDecimal currentPrice = BigDecimal.ZERO;
                            if (priceObj != null) {
                                try {
                                    currentPrice = new BigDecimal(priceObj.toString());
                                } catch (Exception e) {
                                }
                            }
                            if (itemIdObj != null && flightIdObj != null
                                    && currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                                BigDecimal actualFare = fetchActualServiceFare(
                                        bookingRsp.getAerocrs().getBooking().getBookingid(),
                                        Long.parseLong(flightIdObj.toString()), itemIdObj.toString());
                                if (actualFare != null) {
                                    actualServicePrices.put(itemIdObj.toString(), actualFare);
                                }
                            }
                        }
                    }
                }
                // -----------------------------------------------------

                // Load cached maps from DB to pass to mapper for cumulative response
                java.util.Map<String, String> cachedSeats = null;
                java.util.Map<String, String> cachedServices = null;
                java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOptForMaps = bookingService.getBookingByOrderId(orderId);
                if (entityOptForMaps.isPresent()) {
                    cachedSeats = entityOptForMaps.get().getPassengerSeats();
                    cachedServices = entityOptForMaps.get().getPassengerServices();
                }

                // 4. Generate Standard Response
                com.airlines.go7api.responsedto.ChangeServiceRspDto standardResponse = com.airlines.go7api.response.ChangeServiceResponse
                        .generateResponse(changeServiceRsp, bookingRsp, changeServiceReqDto, actualServicePrices, cachedSeats, cachedServices);

                // Cache the newly assigned services into the BookingEntity so ChangePayment can
                // use them
                if (orderId != null && standardResponse.getOrderItems() != null) {
                    java.util.Map<String, String> passengerServices = new java.util.HashMap<>();
                    for (com.airlines.go7api.responsedto.common.OrderItemsDTO item : standardResponse
                            .getOrderItems()) {
                        if (item.getOrderItemId() != null && item.getOrderItemId().contains("_SRV")) {
                            if (item.getServiceList() != null) {
                                for (com.airlines.go7api.responsedto.common.Service srv : item
                                        .getServiceList()) {
                                    if (srv.getServiceCode() != null && !srv.getServiceCode().startsWith("SEAT")) {
                                        if (item.getPassengerIds() != null && !item.getPassengerIds().isEmpty()) {
                                            BigDecimal price = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
                                            passengerServices.put(item.getPassengerIds().get(0), srv.getServiceCode() + "|" + price);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!passengerServices.isEmpty()) {
                        java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOpt = bookingService
                                .getBookingByOrderId(orderId);
                        if (entityOpt.isPresent()) {
                            com.airlines.go7api.entity.BookingEntity entity = entityOpt.get();

                            // Merge with existing services if any
                            java.util.Map<String, String> existingServices = entity.getPassengerServices();
                            if (existingServices == null) {
                                existingServices = new java.util.HashMap<>();
                            }
                            for (java.util.Map.Entry<String, String> newSrv : passengerServices.entrySet()) {
                                existingServices.put(newSrv.getKey(), newSrv.getValue());
                            }

                            entity.setPassengerServices(existingServices);
                            
                            // SYNC: Capture last name if missing
                            if (entity.getPrimaryPassengerLastName() == null || entity.getPrimaryPassengerLastName().isEmpty()) {
                                if (bookingRsp != null && bookingRsp.getAerocrs() != null && bookingRsp.getAerocrs().getBooking() != null) {
                                    com.airlines.go7api.responsego7.common.Passengers pList = bookingRsp.getAerocrs().getBooking().getPassengers();
                                    if (pList != null && pList.getPassenger() != null && !pList.getPassenger().isEmpty()) {
                                        entity.setPrimaryPassengerLastName(pList.getPassenger().get(0).getLastname());
                                    }
                                }
                            }

                            if (standardResponse.getTotalOrderPrice() != null) {
                                entity.setTotalOrderPrice(standardResponse.getTotalOrderPrice());
                            }
                            bookingService.saveBooking(entity);
                            System.out.println("ChangeService: Cached " + passengerServices.size()
                                    + " service assignments to BookingEntity for OrderID: " + orderId);
                        }
                    }
                }

                return new ResponseEntity<>(standardResponse, HttpStatus.OK);
            }

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (InterruptedException e) {
            return handleInterruptedException(CHANGE_SERVICE, e);
        } catch (Exception e) {
            return handleException(CHANGE_SERVICE, e);
        }
    }

    @PostMapping("/orderchange")
    public ResponseEntity<Object> orderChange(@RequestBody OrderChangeReqDto orderChangeReqDto) {
        try {
            // Fetch real Booking Confirmation from MongoDB using OrderID if available
            if (orderChangeReqDto.getOrderId() != null) {
                java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOpt = bookingService
                        .getBookingByOrderId(orderChangeReqDto.getOrderId());
                if (entityOpt.isPresent()) {
                    String realConfirmation = entityOpt.get().getBookingConfirmation();
                    if (realConfirmation != null && !realConfirmation.isEmpty()) {
                        System.out.println("OrderChange: Found real BookingConfirmation in DB: " + realConfirmation);
                        orderChangeReqDto.setOrderId(realConfirmation);
                    }
                }
            }

            // Map the DTO to the internal Request object
            OrderChangeReq orderChangeReq = OrderChangeReq.mapToOrderChangeReq(orderChangeReqDto);

            // Execute the API call and return the raw response
            Object response = orderChangeReq.unmarshal();

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (InterruptedException e) {
            return handleInterruptedException("OrderChange", e);
        } catch (Exception e) {
            return handleException("OrderChange", e);
        }
    }

    @PostMapping("/orderreshop")
    public ResponseEntity<Object> orderReshop(@RequestBody String requestBody) {
        try {
            JsonNode root = mapper.readTree(requestBody);

            String orderId = null;
            if (root.has("orderId")) {
                orderId = root.get("orderId").asText();
            } else if (root.has(KEY_AEROCRS) && root.get(KEY_AEROCRS).has(KEY_PARMS)) {
                JsonNode parms = root.get(KEY_AEROCRS).get(KEY_PARMS);
                if (parms.has(KEY_BOOKING_CONFIRMATION))
                    orderId = parms.get(KEY_BOOKING_CONFIRMATION).asText();
                else if (parms.has(KEY_BOOKING_ID))
                    orderId = parms.get(KEY_BOOKING_ID).asText();
            }

            // 1. Determine Passenger Counts
            int adults = 0, children = 0, infants = 0;
            JsonNode paxList = root.path("paxList");

            if (paxList.isArray() && paxList.size() > 0) {
                for (JsonNode pax : paxList) {
                    String ptc = pax.path("ptc").asText("");
                    if ("ADT".equalsIgnoreCase(ptc))
                        adults++;
                    else if ("CHD".equalsIgnoreCase(ptc))
                        children++;
                    else if ("INF".equalsIgnoreCase(ptc))
                        infants++;
                }
            } else if (orderId != null) {
                // Fetch real Booking Confirmation from MongoDB if needed
                String bookingConfirmation = orderId;
                java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOpt = bookingService
                        .getBookingByOrderId(orderId);
                if (entityOpt.isPresent()) {
                    bookingConfirmation = entityOpt.get().getBookingConfirmation();
                }

                // Call Internal GetBooking to get pax counts
                com.airlines.go7api.request.OrderRetrieveReq getBookingReq = SeatAvailabilityReq
                        .mapToGetBookingReq(bookingConfirmation);
                Object getBookingResponse = getBookingReq.unmarshal();

                if (getBookingResponse != null && !(getBookingResponse instanceof com.airlines.go7api.error.ErrorRsp)) {
                    com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp;
                    if (getBookingResponse instanceof String) {
                        bookingRsp = mapper.readValue((String) getBookingResponse,
                                com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto.class);
                    } else {
                        bookingRsp = mapper.convertValue(getBookingResponse,
                                com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto.class);
                    }
                    if (bookingRsp != null && bookingRsp.getAerocrs() != null
                            && bookingRsp.getAerocrs().getBooking() != null) {
                        adults = bookingRsp.getAerocrs().getBooking().getAdults();
                        children = bookingRsp.getAerocrs().getBooking().getChild();
                        infants = bookingRsp.getAerocrs().getBooking().getInfant();
                    }
                }
            }

            // 2. Prepare Airshop Request
            AirshopReqDto airshopReqDto = new AirshopReqDto();
            java.util.List<AirshopReqDto.OD> ods = new java.util.ArrayList<>();
            JsonNode odsNode = root.path("ods");
            if (odsNode.isArray()) {
                for (JsonNode odNode : odsNode) {
                    AirshopReqDto.OD od = new AirshopReqDto.OD();
                    od.setOrigin(odNode.path("origin").asText(null));
                    od.setDestination(odNode.path("destination").asText(null));
                    od.setDate(odNode.path("date").asText(null));
                    od.setCabinPreference(odNode.path("cabinPreference").asText(null));
                    od.setPreferenceLevel(odNode.path("preferenceLevel").asText(null));
                    ods.add(od);
                }
            }
            airshopReqDto.setOds(ods);
            airshopReqDto.setAdults(adults > 0 ? adults : 1);
            airshopReqDto.setChildren(children);
            airshopReqDto.setInfants(infants);
            airshopReqDto.setTripType(ods.size() > 1 ? "return" : "oneway");
            if (ods.size() > 1) {
                airshopReqDto.setReturnDate(ods.get(1).getDate());
            }

            // 3. Execute Internal Airshop Search
            System.out.println("DEBUG: Executing internal Airshop search for OrderReshop. OrderId: " + orderId);
            AirshopReq flightSearchRequestDTO = AirshopReq.mapToFlightSearchRequestDTO(airshopReqDto);
            AirshopRspGo7Dto response = flightSearchRequestDTO.unmarshal();

            if (response != null && response.getAerocrs() != null && response.getAerocrs().isSuccess()) {
                com.airlines.go7api.responsedto.OrderReshopRspDto reshopRspDto = orderReshopResponse.orderReshopMapper(
                        response,
                        airshopReqDto);
                return new ResponseEntity<>(reshopRspDto, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

        } catch (InterruptedException e) {
            return handleInterruptedException("OrderReshop", e);
        } catch (Exception e) {
            return handleException("OrderReshop", e);
        }
    }

    @PostMapping("/FSorderreshop")
    public ResponseEntity<Object> fsOrderReshop(@RequestBody String requestBody) {
        try {
            JsonNode root = mapper.readTree(requestBody);

            AirshopReqDto airshopReqDto = new AirshopReqDto();

            // Populate ods
            java.util.List<AirshopReqDto.OD> ods = new java.util.ArrayList<>();
            JsonNode odsNode = root.path("ods");
            if (odsNode.isArray()) {
                for (JsonNode odNode : odsNode) {
                    AirshopReqDto.OD od = new AirshopReqDto.OD();
                    od.setOrigin(odNode.path("origin").asText(null));
                    od.setDestination(odNode.path("destination").asText(null));
                    od.setDate(odNode.path("date").asText(null));
                    od.setCabinPreference(odNode.path("cabinPreference").asText(null));
                    od.setPreferenceLevel(odNode.path("preferenceLevel").asText(null));
                    ods.add(od);
                }
            }
            airshopReqDto.setOds(ods);

            // Populate passengers
            int adults = 0, children = 0, infants = 0;
            JsonNode paxList = root.path("paxList");
            if (paxList.isArray()) {
                for (JsonNode pax : paxList) {
                    String ptc = pax.path("ptc").asText("");
                    if ("ADT".equalsIgnoreCase(ptc))
                        adults++;
                    else if ("CHD".equalsIgnoreCase(ptc))
                        children++;
                    else if ("INF".equalsIgnoreCase(ptc))
                        infants++;
                }
            }
            airshopReqDto.setAdults(adults > 0 ? adults : 1); // Default to 1 ADT if none found
            airshopReqDto.setChildren(children);
            airshopReqDto.setInfants(infants);

            airshopReqDto.setTripType(ods.size() > 1 ? "return" : "oneway");
            if (ods.size() > 1) {
                airshopReqDto.setReturnDate(ods.get(1).getDate());
            }

            // Execute Airshop logic
            System.out.println("DEBUG: Executing Airshop search for FSOrderReshop");
            AirshopReq flightSearchRequestDTO = AirshopReq.mapToFlightSearchRequestDTO(airshopReqDto);
            AirshopRspGo7Dto response = flightSearchRequestDTO.unmarshal();

            if (response != null && response.getAerocrs() != null && response.getAerocrs().isSuccess()) {
                com.airlines.go7api.responsedto.OrderReshopRspDto reshopRspDto = orderReshopResponse
                        .orderReshopMapper(response, airshopReqDto);
                return new ResponseEntity<>(reshopRspDto, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

        } catch (InterruptedException e) {
            return handleInterruptedException("FSOrderReshop", e);
        } catch (Exception e) {
            return handleException("FSOrderReshop", e);
        }
    }

    private BigDecimal fetchActualSeatFare(Long bookingId, String flightNumber, String flightDate,
            String fromCode, String toCode, String seatNumber, String classCode) {
        try {
            com.airlines.go7api.request.SeatAvailabilityReq req = new com.airlines.go7api.request.SeatAvailabilityReq();
            com.airlines.go7api.request.SeatAvailabilityReq.Aerocrs aerocrs = new com.airlines.go7api.request.SeatAvailabilityReq.Aerocrs();
            com.airlines.go7api.request.SeatAvailabilityReq.Parms parms = new com.airlines.go7api.request.SeatAvailabilityReq.Parms();

            parms.setBookingId(bookingId);
            parms.setCompanyCode("API");
            parms.setFlightNumber(flightNumber);
            parms.setFlightDate(flightDate);
            parms.setFromCode(fromCode);
            parms.setToCode(toCode);

            aerocrs.setParms(parms);
            req.setAerocrs(aerocrs);
            req.setSeatAvailabilityUrl("https://api.aerocrs.com/v5/getSeatMapFare");

            String responseJson = req.makeApiCall();
            if (responseJson != null) {
                com.airlines.go7api.responsego7.SeatAvailabilityRspGo7Dto go7Rsp = mapper.readValue(responseJson,
                        com.airlines.go7api.responsego7.SeatAvailabilityRspGo7Dto.class);

                if (go7Rsp != null && go7Rsp.getAerocrs() != null && go7Rsp.getAerocrs().getSeatMapFare() != null) {
                    java.util.Map<String, com.airlines.go7api.responsego7.SeatAvailabilityRspGo7Dto.SeatClass> classes = go7Rsp
                            .getAerocrs().getSeatMapFare().getClasses();

                    String cleanClass = (classCode != null && classCode.contains("/")) ? classCode.split("/")[0]
                            : classCode;

                    if (classes != null && classes.containsKey(cleanClass)) {
                        java.util.List<com.airlines.go7api.responsego7.SeatAvailabilityRspGo7Dto.PaidSeatRow> rows = classes
                                .get(cleanClass).getPaidSeats();
                        if (rows != null) {
                            for (com.airlines.go7api.responsego7.SeatAvailabilityRspGo7Dto.PaidSeatRow row : rows) {
                                if (row.getSeats() != null && row.getSeats().containsKey(seatNumber)) {
                                    if (row.getSeatFare() != null) {
                                        return BigDecimal.valueOf(row.getSeatFare());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching actual seat fare for seat " + seatNumber + ": " + e.getMessage());
        }
        return null;
    }

    private BigDecimal fetchActualServiceFare(Long bookingId, Long flightId, String itemId) {
        try {
            com.airlines.go7api.request.ServiceListReq req = new com.airlines.go7api.request.ServiceListReq();
            com.airlines.go7api.request.ServiceListReq.Aerocrs aerocrs = new com.airlines.go7api.request.ServiceListReq.Aerocrs();
            com.airlines.go7api.request.ServiceListReq.Parms parms = new com.airlines.go7api.request.ServiceListReq.Parms();

            parms.setBookingId(bookingId);
            parms.setFlightId(flightId);
            parms.setCurrency("USD");

            aerocrs.setParms(parms);
            req.setAerocrs(aerocrs);
            req.setServiceListUrl("https://api.aerocrs.com/v5/getAncillaries");

            String responseJson = req.makeApiCall();
            if (responseJson != null) {
                com.airlines.go7api.responsego7.ServiceListRspGo7Dto go7Rsp = mapper.readValue(responseJson,
                        com.airlines.go7api.responsego7.ServiceListRspGo7Dto.class);

                if (go7Rsp != null && go7Rsp.getAerocrs() != null && go7Rsp.getAerocrs().getAncillaries() != null) {
                    java.util.List<com.airlines.go7api.responsego7.ServiceListRspGo7Dto.Ancillary> ancillaries = go7Rsp
                            .getAerocrs().getAncillaries().getAncillary();
                    if (ancillaries != null) {
                        for (com.airlines.go7api.responsego7.ServiceListRspGo7Dto.Ancillary anc : ancillaries) {
                            if (anc.getItems() != null) {
                                for (com.airlines.go7api.responsego7.ServiceListRspGo7Dto.Item item : anc.getItems()) {
                                    if (itemId.equals(item.getItemid())) {
                                        if (item.getFare() != null && !item.getFare().isEmpty()) {
                                            return new BigDecimal(item.getFare());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching actual service fare for item " + itemId + ": " + e.getMessage());
        }
        return null;
    }
}
