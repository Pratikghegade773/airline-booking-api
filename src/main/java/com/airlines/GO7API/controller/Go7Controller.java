package com.airlines.GO7API.controller;

import com.airlines.GO7API.request.*;
import com.airlines.GO7API.requestDto.*;
import com.airlines.GO7API.response.AirshopResponse;
import com.airlines.GO7API.response.OfferPriceResponse;
import com.airlines.GO7API.responseDto.AirshopRspDto;
import com.airlines.GO7API.responseGo7.AirshopRspGo7Dto;
import com.airlines.GO7API.responseGo7.ChangeSeatRspGo7Dto;
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

@RestController
public class Go7Controller {
    @Autowired
    AirshopResponse airshopResponse;

    @Autowired
    private com.airlines.GO7API.response.OrderReshopResponse orderReshopResponse;

    @Autowired
    private com.airlines.GO7API.response.ServiceListResponse serviceListResponse;

    @Autowired
    private com.airlines.GO7API.service.BookingService bookingService;

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

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during Airshop request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/offerprice")
    public ResponseEntity<Object> offerPrice(@RequestBody OfferPriceReqDto offerPriceRQ) {
        try {
            // Map the DTO to the internal Request object
            OfferPriceReq offerPriceRequestDTO = OfferPriceReq.mapToOfferPriceRequestDTO(offerPriceRQ);

            // Execute the API call and return the raw response (JsonNode or ErrorRsp)
            // Object response = offerPriceRequestDTO.unmarshal();

            // Generate NDC Response
            Object response = OfferPriceResponse.generateResponse(offerPriceRequestDTO, offerPriceRQ);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during OfferPrice request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/ordercreate")
    public ResponseEntity<Object> orderCreate(@RequestBody OrderCreateReqDto orderCreateReqDto) {
        try {
            // 1. Order Create
            OrderCreateReq orderCreateReq = OrderCreateReq.mapToOrderCreateReq(orderCreateReqDto);
            Object createResponse = orderCreateReq.unmarshal();

            // 2. Convert to internal Go7 DTO
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            com.airlines.GO7API.responseGo7.OrderCreateRspGo7Dto go7Response;
            if (createResponse instanceof String) {
                go7Response = mapper.readValue((String) createResponse,
                        com.airlines.GO7API.responseGo7.OrderCreateRspGo7Dto.class);
            } else {
                go7Response = mapper.convertValue(createResponse,
                        com.airlines.GO7API.responseGo7.OrderCreateRspGo7Dto.class);
            }

            // Check for API Error Response (Root level details present)
            if (go7Response != null && go7Response.getDetails() != null
                    && go7Response.getDetails().getDetail() != null) {
                com.airlines.GO7API.error.ErrorRsp errorRsp = new com.airlines.GO7API.error.ErrorRsp();
                for (String msg : go7Response.getDetails().getDetail()) {
                    com.airlines.GO7API.error.ErrorRsp.Error error = new com.airlines.GO7API.error.ErrorRsp.Error();
                    error.setError(msg);
                    errorRsp.getErrorList().add(error);
                }
                return new ResponseEntity<>(errorRsp, HttpStatus.BAD_REQUEST);
            }

            // 3. Conditional Order Confirm
            // 3. Conditional Order Confirm
            if (go7Response != null && go7Response.getAerocrs() != null && go7Response.getAerocrs().isSuccess()) {
                Long bookingId = go7Response.getAerocrs().getBooking() != null
                        ? go7Response.getAerocrs().getBooking().getBookingid()
                        : null;

                if (bookingId != null && bookingId > 0) {
                    System.out.println("Booking ID extracted: " + bookingId + ". Proceeding to OrderConfirm.");

                    // Consolidated: OrderConfirm
                    OrderCreateReq orderConfirmReq = OrderCreateReq.mapToOrderConfirmReq(orderCreateReqDto, bookingId);
                    Object confirmResponse = orderConfirmReq.unmarshal();
                    System.out.println("OrderConfirm executed. Response: " + confirmResponse);

                    if (confirmResponse instanceof com.airlines.GO7API.error.ErrorRsp) {
                        return new ResponseEntity<>(confirmResponse, HttpStatus.BAD_REQUEST);
                    }

                    // Extract bookingconfirmation from OrderConfirm response
                    String bookingConfirmation = null;
                    try {
                        java.util.LinkedHashMap aerocrsMap = (java.util.LinkedHashMap) ((java.util.LinkedHashMap) confirmResponse)
                                .get("aerocrs");
                        if (aerocrsMap != null) {
                            if (aerocrsMap.containsKey("bookingconfirmation")) {
                                bookingConfirmation = (String) aerocrsMap.get("bookingconfirmation");
                            } else if (aerocrsMap.containsKey("booking")) {
                                Object bookingObj = aerocrsMap.get("booking");
                                if (bookingObj instanceof java.util.LinkedHashMap) {
                                    bookingConfirmation = (String) ((java.util.LinkedHashMap) bookingObj)
                                            .get("bookingconfirmation");
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to extract bookingconfirmation: " + e.getMessage());
                    }

                    if (bookingConfirmation != null) {
                        // Check payment first
                        if (orderCreateReqDto.getPaymentInformation() != null) {

                            // Consolidated: MakePayment
                            OrderCreateReq makePaymentReq = OrderCreateReq.mapToMakePaymentReq(orderCreateReqDto,
                                    bookingId);
                            Object paymentResponse = makePaymentReq.unmarshal();
                            System.out.println("MakePayment executed. Response: " + paymentResponse);

                            if (paymentResponse instanceof com.airlines.GO7API.error.ErrorRsp) {
                                return new ResponseEntity<>(paymentResponse, HttpStatus.BAD_REQUEST);
                            }

                            // Verify MakePayment Success before Ticket
                            boolean paymentSuccess = false;
                            try {
                                ObjectMapper pmMapper = new ObjectMapper();
                                String pmJson = pmMapper.writeValueAsString(paymentResponse);
                                com.fasterxml.jackson.databind.JsonNode pmRoot = pmMapper.readTree(pmJson);
                                if (pmRoot.has("success") && pmRoot.get("success").asBoolean()) {
                                    paymentSuccess = true;
                                } else if (pmRoot.has("aerocrs") && pmRoot.get("aerocrs").has("success")
                                        && pmRoot.get("aerocrs").get("success").asBoolean()) {
                                    paymentSuccess = true;
                                }
                            } catch (Exception e) {
                                System.out.println("Error verifying payment success: " + e.getMessage());
                            }

                            if (!paymentSuccess) {
                                return new ResponseEntity<>(
                                        "MakePayment failed or returned false success. Aborting Ticket issue.",
                                        HttpStatus.BAD_REQUEST);
                            }

                            // Consolidated: OrderTicket
                            OrderCreateReq orderTicketReq = OrderCreateReq.mapToOrderTicketReq(orderCreateReqDto,
                                    bookingId);
                            Object ticketResponse = orderTicketReq.unmarshal();
                            System.out.println("OrderTicket executed. Response: " + ticketResponse);

                            if (ticketResponse instanceof com.airlines.GO7API.error.ErrorRsp) {
                                return new ResponseEntity<>(ticketResponse, HttpStatus.BAD_REQUEST);
                            }
                        }

                        // Consolidated: GetBooking (OrderRetrieve)
                        System.out.println("Booking Confirmation: " + bookingConfirmation + ". Calling GetBooking.");
                        OrderCreateReq getBookingReq = OrderCreateReq.mapToGetBookingReq(bookingConfirmation);
                        Object finalResponse = getBookingReq.unmarshal();

                        if (finalResponse instanceof com.airlines.GO7API.error.ErrorRsp) {
                            return new ResponseEntity<>(finalResponse, HttpStatus.BAD_REQUEST);
                        }

                        // Deserialize GetBooking response
                        com.airlines.GO7API.responseGo7.OrderCreateRspGo7Dto getBookingResponse;
                        if (finalResponse instanceof String) {
                            getBookingResponse = mapper.readValue((String) finalResponse,
                                    com.airlines.GO7API.responseGo7.OrderCreateRspGo7Dto.class);
                        } else {
                            getBookingResponse = mapper.convertValue(finalResponse,
                                    com.airlines.GO7API.responseGo7.OrderCreateRspGo7Dto.class);
                        }

                        // Generate NDC Response from GetBooking data
                        com.airlines.GO7API.responseDto.OrderCreateRspDto ndcResponse = com.airlines.GO7API.response.OrderCreateResponse
                                .generateResponse(getBookingResponse, orderCreateReqDto);

                        // PERSIST TO DB
                        try {
                            String pnr = ndcResponse.getPnr();
                            String orderId = ndcResponse.getOrderId();
                            if (pnr != null && !pnr.isEmpty()) {
                                java.util.Optional<com.airlines.GO7API.entity.BookingEntity> existingOpt = bookingService
                                        .getBookingByPnr(pnr);
                                com.airlines.GO7API.entity.BookingEntity entityToSave;
                                if (existingOpt.isPresent()) {
                                    entityToSave = existingOpt.get();
                                } else {
                                    entityToSave = new com.airlines.GO7API.entity.BookingEntity();
                                    entityToSave.setPnr(pnr);
                                    entityToSave.setBookingConfirmation(bookingConfirmation);
                                    entityToSave.setOrderId(orderId);
                                }
                                entityToSave.setTotalOrderPrice(ndcResponse.getTotalOrderPrice());
                                
                                // Capture primary passenger last name
                                if (orderCreateReqDto.getPassengers() != null && !orderCreateReqDto.getPassengers().isEmpty()) {
                                    entityToSave.setPrimaryPassengerLastName(orderCreateReqDto.getPassengers().get(0).getLastName());
                                }
                                
                                bookingService.saveBooking(entityToSave);
                            }
                        } catch (Exception e) {
                            System.out.println("Failed to save booking to DB: " + e.getMessage());
                        }

                        return new ResponseEntity<>(ndcResponse, HttpStatus.OK);
                    } else {
                        System.out.println("No Booking Confirmation found in OrderConfirm response.");
                    }
                }
            } else {
                System.out.println("OrderCreate failed or no booking ID. Skipping OrderConfirm.");
            }

            // 4. Generate NDC Response (Fallback)
            com.airlines.GO7API.responseDto.OrderCreateRspDto ndcResponse = com.airlines.GO7API.response.OrderCreateResponse
                    .generateResponse(go7Response, orderCreateReqDto);

            return new ResponseEntity<>(ndcResponse, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during OrderCreate request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/change/payment")
    public ResponseEntity<Object> changePayment(@RequestBody ChangePaymentReqDto changePaymentReqDto) {
        try {
            java.util.Map<String, String> passengerSeats = null;
            java.util.Map<String, String> passengerServices = null;
            // Retrieve cached seats and services from BookingEntity if available
            if (changePaymentReqDto.getOrderId() != null && !changePaymentReqDto.getOrderId().isEmpty()) {
                java.util.Optional<com.airlines.GO7API.entity.BookingEntity> entityOpt = bookingService
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
                
                java.util.Optional<com.airlines.GO7API.entity.BookingEntity> entityOptPre = bookingService.getBookingByOrderId(changePaymentReqDto.getOrderId());
                if (entityOptPre.isPresent()) {
                    bookingConfirmationPre = entityOptPre.get().getBookingConfirmation();
                    lastNamePre = entityOptPre.get().getPrimaryPassengerLastName();
                }

                com.airlines.GO7API.request.ChangePaymentReq getBookingReqPre = com.airlines.GO7API.request.ChangePaymentReq
                        .mapToGetBookingReq(bookingConfirmationPre, lastNamePre);
                Object preResponse = getBookingReqPre.unmarshal();

                if (!(preResponse instanceof com.airlines.GO7API.error.ErrorRsp)) {
                    ObjectMapper preMapper = new ObjectMapper();
                    com.airlines.GO7API.responseGo7.ChangePaymentRspGo7Dto preBooking;
                    if (preResponse instanceof String) {
                        preBooking = preMapper.readValue((String) preResponse,
                                com.airlines.GO7API.responseGo7.ChangePaymentRspGo7Dto.class);
                    } else {
                        preBooking = preMapper.convertValue(preResponse,
                                com.airlines.GO7API.responseGo7.ChangePaymentRspGo7Dto.class);
                    }

                    if (preBooking != null && preBooking.getAerocrs() != null
                            && preBooking.getAerocrs().getBooking() != null) {
                        com.airlines.GO7API.responseGo7.ChangePaymentRspGo7Dto.BalanceInformation balance = preBooking
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
            com.airlines.GO7API.request.ChangePaymentReq changePaymentReq = com.airlines.GO7API.request.ChangePaymentReq
                    .mapToChangePaymentReq(changePaymentReqDto);

            // Execute the API call
            Object response = changePaymentReq.unmarshal();

            if (response instanceof com.airlines.GO7API.error.ErrorRsp) {
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Check details for success if it's a Map/Object
            boolean success = false;
            Long bookingId = null;
            try {
                ObjectMapper pmMapper = new ObjectMapper();
                String pmJson = pmMapper.writeValueAsString(response);
                com.fasterxml.jackson.databind.JsonNode pmRoot = pmMapper.readTree(pmJson);
                if (pmRoot.has("success") && pmRoot.get("success").asBoolean()) {
                    success = true;
                } else if (pmRoot.has("aerocrs") && pmRoot.get("aerocrs").has("success")
                        && pmRoot.get("aerocrs").get("success").asBoolean()) {
                    success = true;
                }

                // Extract BookingID for OrderTicket
                if (pmRoot.has("aerocrs")) {
                    com.fasterxml.jackson.databind.JsonNode aerocrsNode = pmRoot.get("aerocrs");
                    if (aerocrsNode.has("booking") && aerocrsNode.get("booking").has("bookingid")) {
                        bookingId = aerocrsNode.get("booking").get("bookingid").asLong();
                    } else if (aerocrsNode.has("bookingid")) {
                        bookingId = aerocrsNode.get("bookingid").asLong();
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
                com.airlines.GO7API.request.ChangePaymentReq orderTicketReq = com.airlines.GO7API.request.ChangePaymentReq
                        .mapToOrderTicketReq(bookingId);
                Object ticketResponse = orderTicketReq.unmarshal();
                System.out.println("OrderTicket executed. Response: " + ticketResponse);

                if (ticketResponse instanceof com.airlines.GO7API.error.ErrorRsp) {
                    return new ResponseEntity<>(ticketResponse, HttpStatus.BAD_REQUEST);
                }

                // Strictly validate OrderTicket success
                boolean ticketSuccess = false;
                try {
                    ObjectMapper ticketMapper = new ObjectMapper();
                    String ticketJson = ticketMapper.writeValueAsString(ticketResponse);
                    com.fasterxml.jackson.databind.JsonNode ticketRoot = ticketMapper.readTree(ticketJson);
                    if (ticketRoot.has("success") && ticketRoot.get("success").asBoolean()) {
                        ticketSuccess = true;
                    } else if (ticketRoot.has("aerocrs") && ticketRoot.get("aerocrs").has("success")
                            && ticketRoot.get("aerocrs").get("success").asBoolean()) {
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

                    if (tRoot.has("aerocrs")) {
                        com.fasterxml.jackson.databind.JsonNode aerocrsNode = tRoot.get("aerocrs");

                        // Check direct confirmation at aerocrs root
                        if (aerocrsNode.has("bookingconfirmation")) {
                            bookingConfirmation = aerocrsNode.get("bookingconfirmation").asText();
                            System.out.println(
                                    "Extracted bookingconfirmation from OrderTicket (root): " + bookingConfirmation);
                        }
                        // Fallback: Check nested under booking
                        else if (aerocrsNode.has("booking") && aerocrsNode.get("booking").has("bookingconfirmation")) {
                            bookingConfirmation = aerocrsNode.get("booking").get("bookingconfirmation").asText();
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
                java.util.Optional<com.airlines.GO7API.entity.BookingEntity> entityOpt = bookingService.getBookingByOrderId(changePaymentReqDto.getOrderId());
                if (entityOpt.isPresent()) {
                    if (bookingConfirmation == null || bookingConfirmation.equals(changePaymentReqDto.getOrderId())) {
                        bookingConfirmation = entityOpt.get().getBookingConfirmation();
                    }
                    finalLastName = entityOpt.get().getPrimaryPassengerLastName();
                }
            }
            
            com.airlines.GO7API.request.ChangePaymentReq getBookingReq = com.airlines.GO7API.request.ChangePaymentReq
                    .mapToGetBookingReq(bookingConfirmation, finalLastName);
            Object finalResponse = getBookingReq.unmarshal();

            System.out.println("GetBooking executed. Response: " + finalResponse);

            if (finalResponse instanceof com.airlines.GO7API.error.ErrorRsp) {
                return new ResponseEntity<>(finalResponse, HttpStatus.BAD_REQUEST);
            }

            // Convert final response (GetBooking result) to ChangePaymentRspGo7Dto
            ObjectMapper mapper = new ObjectMapper();
            com.airlines.GO7API.responseGo7.ChangePaymentRspGo7Dto getBookingResponse;
            if (finalResponse instanceof String) {
                getBookingResponse = mapper.readValue((String) finalResponse,
                        com.airlines.GO7API.responseGo7.ChangePaymentRspGo7Dto.class);
            } else {
                getBookingResponse = mapper.convertValue(finalResponse,
                        com.airlines.GO7API.responseGo7.ChangePaymentRspGo7Dto.class);
            }

            // Generate NDC Response using ChangePaymentResponse mapper
            com.airlines.GO7API.responseDto.ChangePaymentRspDto ndcResponse = com.airlines.GO7API.response.ChangePaymentResponse
                    .generateResponse(getBookingResponse, changePaymentReqDto, passengerSeats, passengerServices);

            if (changePaymentReqDto.getOrderId() != null) {
                java.util.Optional<com.airlines.GO7API.entity.BookingEntity> entityOpt = bookingService
                        .getBookingByOrderId(changePaymentReqDto.getOrderId());
                if (entityOpt.isPresent()) {
                    com.airlines.GO7API.entity.BookingEntity entity = entityOpt.get();
                    if (ndcResponse.getTotalOrderPrice() != null) {
                        entity.setTotalOrderPrice(ndcResponse.getTotalOrderPrice());
                        bookingService.saveBooking(entity);
                    }
                }
            }

            return new ResponseEntity<>(ndcResponse, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during ChangePayment request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/orderretrieve")
    public ResponseEntity<Object> orderRetrieve(@RequestBody OrderRetrieveReqDto orderRetrieveReqDto) {
        try {
            // DB Lookup Logic
            com.airlines.GO7API.entity.BookingEntity bookingEntity = null;

            if (orderRetrieveReqDto.getOrderId() != null && !orderRetrieveReqDto.getOrderId().isEmpty()) {
                System.out.println("Looking up by OrderID: " + orderRetrieveReqDto.getOrderId());
                java.util.Optional<com.airlines.GO7API.entity.BookingEntity> entityOpt = bookingService
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
                java.util.Optional<com.airlines.GO7API.entity.BookingEntity> entityOpt = bookingService
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

            if (response instanceof com.airlines.GO7API.error.ErrorRsp) {
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Convert raw response to Go7 DTO
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.enable(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
            com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto go7Response;
            if (response instanceof String) {
                go7Response = mapper.readValue((String) response,
                        com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
            } else {
                go7Response = mapper.convertValue(response,
                        com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
            }

            java.util.Map<String, String> passengerSeats = null;
            java.util.Map<String, String> passengerServices = null;

            java.math.BigDecimal dbTotalOrderPrice = null;

            if (bookingEntity != null) {
                passengerSeats = bookingEntity.getPassengerSeats();
                passengerServices = bookingEntity.getPassengerServices();
                dbTotalOrderPrice = bookingEntity.getTotalOrderPrice();
            }

            // Generate NDC Response
            com.airlines.GO7API.responseDto.OrderRetrieveRspDto ndcResponse = com.airlines.GO7API.response.OrderRetrieveResponse
                    .generateResponse(go7Response, orderRetrieveReqDto, passengerSeats, passengerServices,
                            dbTotalOrderPrice);

            // SYNC: Update database with any new info from OrderRetrieve (including last name if missing)
            if (bookingEntity != null && go7Response != null && go7Response.getAerocrs() != null && go7Response.getAerocrs().getBooking() != null) {
                boolean changed = false;
                if (bookingEntity.getPrimaryPassengerLastName() == null || bookingEntity.getPrimaryPassengerLastName().isEmpty()) {
                    com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.Passengers pList = go7Response.getAerocrs().getBooking().getPassengers();
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

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during OrderRetrieve request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/unpaidcancel")
    public ResponseEntity<Object> unpaidCancel(@RequestBody UnpaidCancelReqDto unpaidCancelReqDto) {
        try {
            System.out.println("Processing UnpaidCancel for OrderID: " + unpaidCancelReqDto.getOrderId());

            // --- VALIDATION: Check if already Ticketed/Paid ---
            String storedBookingConfirmationValidation = null;
            if (unpaidCancelReqDto.getOrderId() != null && !unpaidCancelReqDto.getOrderId().isEmpty()) {
                java.util.Optional<com.airlines.GO7API.entity.BookingEntity> entityOpt = bookingService
                        .getBookingByOrderId(unpaidCancelReqDto.getOrderId());
                if (entityOpt.isPresent()) {
                    storedBookingConfirmationValidation = entityOpt.get().getBookingConfirmation();
                }
            }

            com.airlines.GO7API.requestDto.OrderRetrieveReqDto tempRetrieveReq = new com.airlines.GO7API.requestDto.OrderRetrieveReqDto();
            tempRetrieveReq.setOrderId(unpaidCancelReqDto.getOrderId());
            tempRetrieveReq.setApiKey(unpaidCancelReqDto.getApiKey());

            com.airlines.GO7API.request.OrderRetrieveReq validationBookingReq = com.airlines.GO7API.request.OrderRetrieveReq
                    .mapToOrderRetrieveReq(tempRetrieveReq, storedBookingConfirmationValidation);
            Object validationResponse = validationBookingReq.unmarshal();

            if (!(validationResponse instanceof com.airlines.GO7API.error.ErrorRsp)) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        false);
                mapper.enable(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);

                com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto bookingRsp = null;
                try {
                    if (validationResponse instanceof String) {
                        bookingRsp = mapper.readValue((String) validationResponse,
                                com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
                    } else {
                        bookingRsp = mapper.convertValue(validationResponse,
                                com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
                    }
                } catch (Exception e) {
                }

                if (bookingRsp != null && bookingRsp.getAerocrs() != null
                        && bookingRsp.getAerocrs().getBooking() != null) {
                    boolean hasTickets = false;
                    if (bookingRsp.getAerocrs().getBooking().getPassengers() != null
                            && bookingRsp.getAerocrs().getBooking().getPassengers().getPassenger() != null) {
                        for (com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.Passenger p : bookingRsp
                                .getAerocrs().getBooking().getPassengers().getPassenger()) {
                            if (p.getETickets() != null && p.getETickets().getFlight() != null
                                    && !p.getETickets().getFlight().isEmpty()) {
                                hasTickets = true;
                                break;
                            }
                        }
                    }
                    if (hasTickets) {
                        com.airlines.GO7API.error.ErrorRsp errorRsp = new com.airlines.GO7API.error.ErrorRsp();
                        errorRsp.setAirlineCode("G7");
                        errorRsp.setSource("G7-API");
                        com.airlines.GO7API.error.ErrorRsp.Error error = new com.airlines.GO7API.error.ErrorRsp.Error();
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
                            com.airlines.GO7API.error.ErrorRsp errorRsp = new com.airlines.GO7API.error.ErrorRsp();
                            errorRsp.setAirlineCode("G7");
                            errorRsp.setSource("G7-API");
                            com.airlines.GO7API.error.ErrorRsp.Error error = new com.airlines.GO7API.error.ErrorRsp.Error();
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

            if (cancelResponse instanceof com.airlines.GO7API.error.ErrorRsp) {
                return new ResponseEntity<>(cancelResponse, HttpStatus.BAD_REQUEST);
            }

            // --- Post-call Validation: Handle provider-side failure without 'errors' array
            // ---
            try {
                ObjectMapper tempMapper = new ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = tempMapper.valueToTree(cancelResponse);
                if (root.has("success") && !root.get("success").asBoolean()) {
                    com.airlines.GO7API.error.ErrorRsp errorRsp = new com.airlines.GO7API.error.ErrorRsp();
                    errorRsp.setAirlineCode("G7");
                    errorRsp.setSource("G7-API");
                    String errMsg = "Cancellation failed on provider side.";
                    if (root.has("details") && root.get("details").has("detail")) {
                        com.fasterxml.jackson.databind.JsonNode detailNode = root.get("details").get("detail");
                        if (detailNode.isArray() && detailNode.size() > 0) {
                            errMsg = detailNode.get(0).asText();
                        }
                    }
                    com.airlines.GO7API.error.ErrorRsp.Error error = new com.airlines.GO7API.error.ErrorRsp.Error();
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
                java.util.Optional<com.airlines.GO7API.entity.BookingEntity> entityOpt = bookingService
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
                        if (root.has("aerocrs") && root.get("aerocrs").has("bookingconfirmation")) {
                            storedBookingConfirmation = root.get("aerocrs").get("bookingconfirmation").asText();
                        } else if (root.has("bookingconfirmation")) {
                            storedBookingConfirmation = root.get("bookingconfirmation").asText();
                        }
                    } catch (Exception e) {
                    }
                } else {
                    try {
                        ObjectMapper tempMapper = new ObjectMapper();
                        com.fasterxml.jackson.databind.JsonNode root = tempMapper.valueToTree(cancelResponse);
                        if (root.has("aerocrs") && root.get("aerocrs").has("bookingconfirmation")) {
                            storedBookingConfirmation = root.get("aerocrs").get("bookingconfirmation").asText();
                        }
                    } catch (Exception e) {
                    }
                }
            }

            com.airlines.GO7API.requestDto.OrderRetrieveReqDto retrieveReqDto = new com.airlines.GO7API.requestDto.OrderRetrieveReqDto();
            retrieveReqDto.setOrderId(unpaidCancelReqDto.getOrderId());
            retrieveReqDto.setApiKey(unpaidCancelReqDto.getApiKey());

            // Allow mapper to decide based on presence of storedBookingConfirmation
            // If storedBookingConfirmation is present, it will be used (and usually works
            // without surname)
            // If null, it falls back to OrderID (which might require surname)
            com.airlines.GO7API.request.OrderRetrieveReq getBookingReq = com.airlines.GO7API.request.OrderRetrieveReq
                    .mapToOrderRetrieveReq(retrieveReqDto, storedBookingConfirmation);

            Object bookingResponse = getBookingReq.unmarshal();

            if (bookingResponse instanceof com.airlines.GO7API.error.ErrorRsp) {
                System.out.println("Failed to retrieve booking details after cancellation.");
                return new ResponseEntity<>(bookingResponse, HttpStatus.BAD_REQUEST);
            }

            // 3. Convert Booking Response to UnpaidCancelRspGo7Dto
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.enable(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);

            com.airlines.GO7API.responseGo7.UnpaidCancelRspGo7Dto fullBooking;
            if (bookingResponse instanceof String) {
                fullBooking = mapper.readValue((String) bookingResponse,
                        com.airlines.GO7API.responseGo7.UnpaidCancelRspGo7Dto.class);
            } else {
                fullBooking = mapper.convertValue(bookingResponse,
                        com.airlines.GO7API.responseGo7.UnpaidCancelRspGo7Dto.class);
            }

            // 4. Generate NDC Response
            com.airlines.GO7API.responseDto.UnpaidCancelRspDto ndcResponse = com.airlines.GO7API.response.UnpaidCancelResponse
                    .generateResponse(fullBooking, unpaidCancelReqDto);

            return new ResponseEntity<>(ndcResponse, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during UnpaidCancel request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/servicelist")
    public ResponseEntity<Object> serviceList(@RequestBody String requestBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(requestBody);

            ServiceListReqDto serviceListReqDto = new ServiceListReqDto();

            // Handle nested "aerocrs.parms" structure
            if (rootNode.has("aerocrs") && rootNode.get("aerocrs").has("parms")) {
                com.fasterxml.jackson.databind.JsonNode parms = rootNode.get("aerocrs").get("parms");

                if (parms.has("bookingid")) {
                    serviceListReqDto.setOrderId(parms.get("bookingid").asText());
                }
                // flightid and currency removed from DTO
            } else {
                // Fallback: Try to map directly if structure is flat or matches DTO
                serviceListReqDto = mapper.readValue(requestBody, ServiceListReqDto.class);
            }

            // New Logic: Fetch Booking Details first
            String orderId = serviceListReqDto.getOrderId();
            String bookingConfirmation = null;

            // 1. DB Lookup
            if (orderId != null) {
                java.util.Optional<com.airlines.GO7API.entity.BookingEntity> entityOpt = bookingService
                        .getBookingByOrderId(orderId);
                if (entityOpt.isPresent()) {
                    bookingConfirmation = entityOpt.get().getBookingConfirmation();
                    System.out.println("ServiceList: Found BookingConfirmation in DB: " + bookingConfirmation);
                } else {
                    // Fallback to using orderId as confirmation if not found (or look up by other
                    // means if needed)
                    bookingConfirmation = orderId;
                    System.out.println("ServiceList: Booking not found in DB, using OrderId as confirmation: "
                            + bookingConfirmation);
                }
            }

            // 2. Call Internal GetBooking
            System.out.println("ServiceList: Calling internal getBooking with confirmation: " + bookingConfirmation);
            com.airlines.GO7API.request.OrderRetrieveReq getBookingReq = ServiceListReq
                    .mapToGetBookingReq(bookingConfirmation);
            Object getBookingResponse = getBookingReq.unmarshal();

            if (getBookingResponse instanceof com.airlines.GO7API.error.ErrorRsp) {
                System.out.println("ServiceList: Internal getBooking failed.");
                return new ResponseEntity<>(getBookingResponse, HttpStatus.BAD_REQUEST);
            }

            // 3. Parse Booking Response
            com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto bookingRsp;
            if (getBookingResponse instanceof String) {
                bookingRsp = mapper.readValue((String) getBookingResponse,
                        com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
            } else {
                bookingRsp = mapper.convertValue(getBookingResponse,
                        com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
            }

            // 4. Map to ServiceListReq (populating flightid and currency from booking)
            ServiceListReq serviceListReq = ServiceListReq.mapToServiceListRequestDTO(serviceListReqDto, bookingRsp);

            // 5. Execute ServiceList API call
            Object response = serviceListReq.unmarshal();

            // 6. Map to NDC Response
            com.airlines.GO7API.responseGo7.ServiceListRspGo7Dto serviceListRspGo7;
            if (response instanceof String) {
                serviceListRspGo7 = mapper.readValue((String) response,
                        com.airlines.GO7API.responseGo7.ServiceListRspGo7Dto.class);
            } else {
                serviceListRspGo7 = mapper.convertValue(response,
                        com.airlines.GO7API.responseGo7.ServiceListRspGo7Dto.class);
            }

            com.airlines.GO7API.responseDto.ServiceListRspDto ndcResponse = serviceListResponse
                    .generateResponse(serviceListRspGo7, serviceListReqDto, bookingRsp);

            return new ResponseEntity<>(ndcResponse, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during ServiceList request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/seatavailability")
    public ResponseEntity<Object> seatAvailability(@RequestBody SeatAvailabilityReqDto seatAvailabilityReqDto) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
                    true);

            // New Logic: Fetch Booking Details first (Same as ServiceList)
            String orderId = seatAvailabilityReqDto.getOrderId();
            String bookingConfirmation = null;

            // 1. DB Lookup
            if (orderId != null) {
                java.util.Optional<com.airlines.GO7API.entity.BookingEntity> entityOpt = bookingService
                        .getBookingByOrderId(orderId);
                if (entityOpt.isPresent()) {
                    bookingConfirmation = entityOpt.get().getBookingConfirmation();
                    System.out.println("SeatAvailability: Found BookingConfirmation in DB: " + bookingConfirmation);
                } else {
                    // Fallback to using orderId as confirmation if not found
                    bookingConfirmation = orderId;
                    System.out.println("SeatAvailability: Booking not found in DB, using OrderId as confirmation: "
                            + bookingConfirmation);
                }
            }

            // 2. Call Internal GetBooking
            System.out
                    .println("SeatAvailability: Calling internal getBooking with confirmation: " + bookingConfirmation);
            com.airlines.GO7API.request.OrderRetrieveReq getBookingReq = SeatAvailabilityReq
                    .mapToGetBookingReq(bookingConfirmation);
            Object getBookingResponse = getBookingReq.unmarshal();

            if (getBookingResponse instanceof com.airlines.GO7API.error.ErrorRsp) {
                System.out.println("SeatAvailability: Internal getBooking failed.");
                return new ResponseEntity<>(getBookingResponse, HttpStatus.BAD_REQUEST);
            }

            // 3. Parse Booking Response
            com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto bookingRsp;
            if (getBookingResponse instanceof String) {
                bookingRsp = mapper.readValue((String) getBookingResponse,
                        com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
            } else {
                bookingRsp = mapper.convertValue(getBookingResponse,
                        com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
            }

            // 4. Map the DTO to the internal Request object (using booking details)
            SeatAvailabilityReq seatAvailabilityReq = SeatAvailabilityReq
                    .mapToSeatAvailabilityRequestDTO(seatAvailabilityReqDto, bookingRsp);

            // 5. Execute the API call and return the raw response
            // 5. Execute the API call
            Object response = seatAvailabilityReq.unmarshal();

            // Check for Error Response
            if (response instanceof com.airlines.GO7API.error.ErrorRsp) {
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Convert to Go7 DTO
            com.airlines.GO7API.responseGo7.SeatAvailabilityRspGo7Dto go7Rsp;
            try {
                if (response instanceof String) {
                    go7Rsp = mapper.readValue((String) response,
                            com.airlines.GO7API.responseGo7.SeatAvailabilityRspGo7Dto.class);
                } else {
                    go7Rsp = mapper.convertValue(response,
                            com.airlines.GO7API.responseGo7.SeatAvailabilityRspGo7Dto.class);
                }
            } catch (IllegalArgumentException e) {
                // Determine if it's a raw JSON string that couldn't be parsed directly as DTO
                // or object
                // Fallback to error or raw
                System.out.println("SeatAvailability: Failed to convert response to DTO: " + e.getMessage());
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            com.airlines.GO7API.responseDto.SeatAvailabilityRspDto mappedRsp = com.airlines.GO7API.response.SeatAvailabilityResponse
                    .mapToSeatAvailabilityRspDto(go7Rsp, orderId, bookingRsp);

            return new ResponseEntity<>(mappedRsp, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during SeatAvailability request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/change/seat")
    public ResponseEntity<Object> changeSeat(@RequestBody ChangeSeatReqDto changeSeatReqDto) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
                    true);

            // 1. Fetch Booking Details first
            String orderId = changeSeatReqDto.getOrderId();
            String bookingConfirmation = null;

            // DB Lookup
            if (orderId != null) {
                java.util.Optional<com.airlines.GO7API.entity.BookingEntity> entityOpt = bookingService
                        .getBookingByOrderId(orderId);
                if (entityOpt.isPresent()) {
                    bookingConfirmation = entityOpt.get().getBookingConfirmation();
                    System.out.println("ChangeSeat: Found BookingConfirmation in DB: " + bookingConfirmation);
                } else {
                    bookingConfirmation = orderId;
                    System.out.println("ChangeSeat: Booking not found in DB, using OrderId as confirmation: "
                            + bookingConfirmation);
                }
            }

            // 2. Call Internal GetBooking
            System.out.println("ChangeSeat: Calling internal getBooking with confirmation: " + bookingConfirmation);
            com.airlines.GO7API.request.OrderRetrieveReq getBookingReq = SeatAvailabilityReq
                    .mapToGetBookingReq(bookingConfirmation);
            Object getBookingResponse = getBookingReq.unmarshal();

            if (getBookingResponse instanceof com.airlines.GO7API.error.ErrorRsp) {
                System.out.println("ChangeSeat: Internal getBooking failed.");
                return new ResponseEntity<>(getBookingResponse, HttpStatus.BAD_REQUEST);
            }

            // 3. Parse Booking Response
            com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto bookingRsp;
            if (getBookingResponse instanceof String) {
                bookingRsp = mapper.readValue((String) getBookingResponse,
                        com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
            } else {
                bookingRsp = mapper.convertValue(getBookingResponse,
                        com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
            }

            // 4. Map to ChangeSeatReq
            com.airlines.GO7API.request.ChangeSeatReq changeSeatReq = com.airlines.GO7API.request.ChangeSeatReq
                    .mapToChangeSeatReq(changeSeatReqDto, bookingRsp);

            // 5. Execute API Call
            Object response = changeSeatReq.unmarshal();

            ChangeSeatRspGo7Dto changeSeatRsp = null;
            if (response instanceof String) {
                try {
                    changeSeatRsp = mapper.readValue((String) response,
                            com.airlines.GO7API.responseGo7.ChangeSeatRspGo7Dto.class);
                } catch (Exception e) {
                    // If parsing fails, we might return error or raw response, but for now
                    // let's try to proceed or return error
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
            } else if (response instanceof com.airlines.GO7API.responseGo7.ChangeSeatRspGo7Dto) {
                changeSeatRsp = (com.airlines.GO7API.responseGo7.ChangeSeatRspGo7Dto) response;
            }

            if (changeSeatRsp != null) {
                // Determine if at least one seat was successfully assigned
                boolean anySeatSuccessful = false;
                if (changeSeatRsp.getAerocrs() != null && changeSeatRsp.getAerocrs().getFlights() != null) {
                    for (com.airlines.GO7API.responseGo7.ChangeSeatRspGo7Dto.Flight f : changeSeatRsp.getAerocrs().getFlights()) {
                        if (f.getSeat() != null) {
                            for (com.airlines.GO7API.responseGo7.ChangeSeatRspGo7Dto.Seat s : f.getSeat()) {
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
                    System.out.println("Processing Payment for Seat Change...");

                    // 1. Create ChangePaymentReqDto from SeatReq
                    com.airlines.GO7API.requestDto.ChangePaymentReqDto paymentReqDto = new com.airlines.GO7API.requestDto.ChangePaymentReqDto();
                    paymentReqDto.setOrderId(orderId); // Use original order ID

                    com.airlines.GO7API.requestDto.ChangePaymentReqDto.PaymentInformation payInfo = new com.airlines.GO7API.requestDto.ChangePaymentReqDto.PaymentInformation();
                    com.airlines.GO7API.requestDto.ChangeSeatReqDto.PaymentInformation seatPay = changeSeatReqDto
                            .getPaymentInformation();

                    payInfo.setAmount(seatPay.getAmount());
                    payInfo.setCurrencyCode(seatPay.getCurrencyCode());
                    payInfo.setCardCode(seatPay.getCardCode());
                    payInfo.setCardNumber(seatPay.getCardNumber());
                    payInfo.setSeriesCode(seatPay.getSeriesCode());
                    payInfo.setCardHolderName(seatPay.getCardHolderName());
                    payInfo.setExpiration(seatPay.getExpiration());

                    if (seatPay.getAddress() != null) {
                        com.airlines.GO7API.requestDto.ChangePaymentReqDto.PaymentInformation.Address addr = new com.airlines.GO7API.requestDto.ChangePaymentReqDto.PaymentInformation.Address();
                        addr.setStreet(seatPay.getAddress().getStreet());
                        addr.setCity(seatPay.getAddress().getCity());
                        addr.setState(seatPay.getAddress().getState());
                        addr.setPostalCode(seatPay.getAddress().getPostalCode());
                        addr.setCountryCode(seatPay.getAddress().getCountryCode());
                        addr.setCountry(seatPay.getAddress().getCountry());
                        payInfo.setAddress(addr);
                    }
                    paymentReqDto.setPaymentInformation(payInfo);

                    // 2. Map and Execute ChangePayment
                    com.airlines.GO7API.request.ChangePaymentReq changePaymentReq = com.airlines.GO7API.request.ChangePaymentReq
                            .mapToChangePaymentReq(paymentReqDto);

                    Object paymentResponse = changePaymentReq.unmarshal();
                    System.out.println("ChangePayment (Seat) executed. Response: " + paymentResponse);

                    // Verify Payment Success
                    boolean paymentSuccess = false;
                    try {
                        ObjectMapper pmMapper = new ObjectMapper();
                        String pmJson = pmMapper.writeValueAsString(paymentResponse);
                        com.fasterxml.jackson.databind.JsonNode pmRoot = pmMapper.readTree(pmJson);
                        if (pmRoot.has("success") && pmRoot.get("success").asBoolean()) {
                            paymentSuccess = true;
                        } else if (pmRoot.has("aerocrs") && pmRoot.get("aerocrs").has("success")
                                && pmRoot.get("aerocrs").get("success").asBoolean()) {
                            paymentSuccess = true;
                        }
                    } catch (Exception e) {
                        System.out.println("Error verifying payment success: " + e.getMessage());
                    }

                    if (paymentSuccess) {
                        Long bookingIdForTicket = null;
                        if (bookingRsp != null && bookingRsp.getAerocrs() != null
                                && bookingRsp.getAerocrs().getBooking() != null) {
                            bookingIdForTicket = bookingRsp.getAerocrs().getBooking().getBookingid();
                        }

                        if (bookingIdForTicket != null) {
                            System.out.println("Calling Internal OrderTicket for BookingID: " + bookingIdForTicket);
                            com.airlines.GO7API.request.ChangePaymentReq orderTicketReq = com.airlines.GO7API.request.ChangePaymentReq
                                    .mapToOrderTicketReq(bookingIdForTicket);
                            Object ticketResponse = orderTicketReq.unmarshal();
                            System.out.println("OrderTicket (Seat) executed. Response: " + ticketResponse);
                        }
                    } else {
                        System.out.println("Payment failed. Returning error response.");
                        return new ResponseEntity<>(paymentResponse, HttpStatus.BAD_REQUEST);
                    }
                }

                // ALWAYS Refresh Booking (GetBooking) after change to get updated PNR total and balance
                System.out.println("Refreshing Booking after ChangeSeat to capture price updates...");
                try {
                    com.airlines.GO7API.request.OrderRetrieveReq refreshReq = SeatAvailabilityReq.mapToGetBookingReq(bookingConfirmation);
                    Object refreshResp = refreshReq.unmarshal();
                    if (!(refreshResp instanceof com.airlines.GO7API.error.ErrorRsp)) {
                        if (refreshResp instanceof String) {
                            bookingRsp = mapper.readValue((String) refreshResp, com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
                        } else {
                            bookingRsp = mapper.convertValue(refreshResp, com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error refreshing booking after ChangeSeat: " + e.getMessage());
                }

                // --- Real-time Price Fetching (Added to Controller) ---
                java.util.Map<String, BigDecimal> actualSeatPrices = new java.util.HashMap<>();
                if (changeSeatRsp != null && changeSeatRsp.getAerocrs() != null
                        && changeSeatRsp.getAerocrs().getFlights() != null) {
                    for (com.airlines.GO7API.responseGo7.ChangeSeatRspGo7Dto.Flight f : changeSeatRsp.getAerocrs()
                            .getFlights()) {
                        if (f.getSeat() != null) {
                            for (com.airlines.GO7API.responseGo7.ChangeSeatRspGo7Dto.Seat s : f.getSeat()) {
                                if (s.getSeat() != null
                                        && (s.getFare() == null || s.getFare().compareTo(BigDecimal.ZERO) <= 0)) {
                                    BigDecimal actualFare = fetchActualSeatFare(
                                            bookingRsp.getAerocrs().getBooking().getBookingid(),
                                            f.getFlightnumber(), f.getFlightdate(), f.getFromcode(), f.getTocode(),
                                            s.getSeat(), f.getFlightClass());
                                    if (actualFare != null) {
                                        actualSeatPrices.put(s.getSeat(), actualFare);
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
                java.util.Optional<com.airlines.GO7API.entity.BookingEntity> entityOptForMaps = bookingService.getBookingByOrderId(orderId);
                if (entityOptForMaps.isPresent()) {
                    cachedSeats = entityOptForMaps.get().getPassengerSeats();
                    cachedServices = entityOptForMaps.get().getPassengerServices();
                }

                com.airlines.GO7API.responseDto.ChangeSeatRspDto ndcResponse = com.airlines.GO7API.response.ChangeSeatResponse
                        .generateResponse(changeSeatRsp, bookingRsp, changeSeatReqDto, actualSeatPrices, cachedSeats, cachedServices);

                // Cache the newly assigned seats into the BookingEntity so ChangePayment can
                // use them
                if (orderId != null && ndcResponse.getOrderItems() != null) {
                    java.util.Map<String, String> passengerSeats = new java.util.HashMap<>();
                    for (com.airlines.GO7API.responseDto.ChangeSeatRspDto.OrderItemsDTO item : ndcResponse
                            .getOrderItems()) {
                        if (item.getServiceList() != null) {
                            for (com.airlines.GO7API.responseDto.ChangeSeatRspDto.Service srv : item.getServiceList()) {
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
                        java.util.Optional<com.airlines.GO7API.entity.BookingEntity> entityOpt = bookingService
                                .getBookingByOrderId(orderId);
                        if (entityOpt.isPresent()) {
                            com.airlines.GO7API.entity.BookingEntity entity = entityOpt.get();

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
                                    com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.Passengers pList = bookingRsp.getAerocrs().getBooking().getPassengers();
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

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during ChangeSeat request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/change/service")
    public ResponseEntity<Object> orderService(@RequestBody ChangeServiceReqDto changeServiceReqDto) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
                    true);

            // 1. Retrieve Booking (to get current state, pax numbers, flights)
            String orderId = changeServiceReqDto.getOrderId();
            String bookingConfirmation = null;

            // DB Lookup
            if (orderId != null) {
                java.util.Optional<com.airlines.GO7API.entity.BookingEntity> entityOpt = bookingService
                        .getBookingByOrderId(orderId);
                if (entityOpt.isPresent()) {
                    bookingConfirmation = entityOpt.get().getBookingConfirmation();
                    System.out.println("ChangeService: Found BookingConfirmation in DB: " + bookingConfirmation);
                } else {
                    bookingConfirmation = orderId;
                    System.out.println("ChangeService: Booking not found in DB, using OrderId as confirmation: "
                            + bookingConfirmation);
                }
            }

            // Call Internal GetBooking
            com.airlines.GO7API.request.OrderRetrieveReq getBookingReq = SeatAvailabilityReq
                    .mapToGetBookingReq(bookingConfirmation);
            Object retrieveResponse = getBookingReq.unmarshal();

            com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto bookingRsp = null;
            if (retrieveResponse instanceof String) {
                try {
                    bookingRsp = mapper.readValue((String) retrieveResponse,
                            com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
                } catch (Exception e) {
                    // Ignore
                }
            } else {
                bookingRsp = mapper.convertValue(retrieveResponse,
                        com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
            }

            if (bookingRsp == null || bookingRsp.getAerocrs() == null || !bookingRsp.getAerocrs().isSuccess()) {
                return new ResponseEntity<>("Error: Could not retrieve booking for OrderID: " + orderId,
                        HttpStatus.BAD_REQUEST);
            }

            // 2. Map DTO to ChangeServiceReq (which converts NDC to Go7 structure)
            ChangeServiceReq changeServiceReq = ChangeServiceReq.mapToChangeServiceReq(changeServiceReqDto, bookingRsp);

            // 3. Execute ChangeService API
            Object response = changeServiceReq.unmarshal();

            com.airlines.GO7API.responseGo7.ChangeServiceRspGo7Dto changeServiceRsp = null;
            if (response instanceof String) {
                try {
                    changeServiceRsp = mapper.readValue((String) response,
                            com.airlines.GO7API.responseGo7.ChangeServiceRspGo7Dto.class);
                } catch (Exception e) {
                    System.out.println("Error parsing ChangeService response: " + e.getMessage());
                }
            } else if (response instanceof com.airlines.GO7API.responseGo7.ChangeServiceRspGo7Dto) {
                changeServiceRsp = (com.airlines.GO7API.responseGo7.ChangeServiceRspGo7Dto) response;
            } else {
                // Handle Map/LinkedHashMap returned by unmarshal
                try {
                    changeServiceRsp = mapper.convertValue(response,
                            com.airlines.GO7API.responseGo7.ChangeServiceRspGo7Dto.class);
                } catch (Exception e) {
                    System.out.println("Error converting ChangeService response map to DTO: " + e.getMessage());
                }
            }

            if (changeServiceRsp != null) {
                System.out.println("ChangeService: changeServiceRsp is NOT null. Success: "
                        + (changeServiceRsp.getAerocrs() != null ? changeServiceRsp.getAerocrs().isSuccess() : "null"));
                // 1. Strict Error Handling
                if (changeServiceRsp.getAerocrs() != null && !changeServiceRsp.getAerocrs().isSuccess()) {
                    System.out.println("ChangeService: AeroCRS reported failure, returning 400.");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }

                // 2. Conditional Payment & Ticketing Logic
                // Determine if at least one service was successfully added
                boolean anyServiceSuccessful = false;
                if (changeServiceRsp != null && changeServiceRsp.getAerocrs() != null && changeServiceRsp.getAerocrs().getDetails() != null) {
                    for (com.airlines.GO7API.responseGo7.ChangeServiceRspGo7Dto.Aerocrs.Detail d : changeServiceRsp.getAerocrs().getDetails()) {
                        if (d.isSuccess()) {
                            anyServiceSuccessful = true;
                            break;
                        }
                    }
                }

                if (anyServiceSuccessful && changeServiceReqDto.getPaymentInformation() != null 
                    && changeServiceReqDto.getPaymentInformation().getCardNumber() != null 
                    && !changeServiceReqDto.getPaymentInformation().getCardNumber().isEmpty()) {
                    System.out.println("Processing Payment for Service Change...");

                    // 1. Create ChangePaymentReqDto from ServiceReq
                    com.airlines.GO7API.requestDto.ChangePaymentReqDto paymentReqDto = new com.airlines.GO7API.requestDto.ChangePaymentReqDto();
                    paymentReqDto.setOrderId(orderId); // Use original order ID

                    com.airlines.GO7API.requestDto.ChangePaymentReqDto.PaymentInformation payInfo = new com.airlines.GO7API.requestDto.ChangePaymentReqDto.PaymentInformation();
                    com.airlines.GO7API.requestDto.ChangeServiceReqDto.PaymentInformation srvPay = changeServiceReqDto
                            .getPaymentInformation();

                    payInfo.setAmount(srvPay.getAmount());
                    payInfo.setCurrencyCode(srvPay.getCurrencyCode());
                    payInfo.setCardCode(srvPay.getCardCode());
                    payInfo.setCardNumber(srvPay.getCardNumber());
                    payInfo.setSeriesCode(srvPay.getSeriesCode());
                    payInfo.setCardHolderName(srvPay.getCardHolderName());
                    payInfo.setExpiration(srvPay.getExpiration());

                    if (srvPay.getAddress() != null) {
                        com.airlines.GO7API.requestDto.ChangePaymentReqDto.PaymentInformation.Address addr = new com.airlines.GO7API.requestDto.ChangePaymentReqDto.PaymentInformation.Address();
                        addr.setStreet(srvPay.getAddress().getStreet());
                        addr.setCity(srvPay.getAddress().getCity());
                        addr.setState(srvPay.getAddress().getState());
                        addr.setPostalCode(srvPay.getAddress().getPostalCode());
                        addr.setCountryCode(srvPay.getAddress().getCountryCode());
                        addr.setCountry(srvPay.getAddress().getCountry());
                        payInfo.setAddress(addr);
                    }
                    paymentReqDto.setPaymentInformation(payInfo);

                    // 2. Map and Execute ChangePayment
                    com.airlines.GO7API.request.ChangePaymentReq changePaymentReq = com.airlines.GO7API.request.ChangePaymentReq
                            .mapToChangePaymentReq(paymentReqDto);

                    Object paymentResponse = changePaymentReq.unmarshal();
                    System.out.println("ChangePayment (Service) executed. Response: " + paymentResponse);

                    // Verify Payment Success
                    boolean paymentSuccess = false;
                    try {
                        ObjectMapper pmMapper = new ObjectMapper();
                        String pmJson = pmMapper.writeValueAsString(paymentResponse);
                        com.fasterxml.jackson.databind.JsonNode pmRoot = pmMapper.readTree(pmJson);
                        if (pmRoot.has("success") && pmRoot.get("success").asBoolean()) {
                            paymentSuccess = true;
                        } else if (pmRoot.has("aerocrs") && pmRoot.get("aerocrs").has("success")
                                && pmRoot.get("aerocrs").get("success").asBoolean()) {
                            paymentSuccess = true;
                        }
                    } catch (Exception e) {
                        System.out.println("Error verifying payment success: " + e.getMessage());
                    }

                    if (paymentSuccess) {
                        Long bookingIdForTicket = null;
                        if (bookingRsp != null && bookingRsp.getAerocrs() != null
                                && bookingRsp.getAerocrs().getBooking() != null) {
                            bookingIdForTicket = bookingRsp.getAerocrs().getBooking().getBookingid();
                        }

                        if (bookingIdForTicket != null) {
                            System.out.println("Calling Internal OrderTicket for BookingID: " + bookingIdForTicket);
                            com.airlines.GO7API.request.ChangePaymentReq orderTicketReq = com.airlines.GO7API.request.ChangePaymentReq
                                    .mapToOrderTicketReq(bookingIdForTicket);
                            Object ticketResponse = orderTicketReq.unmarshal();
                            System.out.println("OrderTicket (Service) executed. Response: " + ticketResponse);
                        }
                    } else {
                        System.out.println("Payment failed. Returning error response.");
                        return new ResponseEntity<>(paymentResponse, HttpStatus.BAD_REQUEST);
                    }
                }

                // ALWAYS Refresh Booking (GetBooking) after change to get updated PNR total and balance
                System.out.println("Refreshing Booking after ChangeService to capture price updates...");
                try {
                    com.airlines.GO7API.request.OrderRetrieveReq refreshReq = SeatAvailabilityReq.mapToGetBookingReq(bookingConfirmation);
                    Object refreshResp = refreshReq.unmarshal();
                    if (!(refreshResp instanceof com.airlines.GO7API.error.ErrorRsp)) {
                        if (refreshResp instanceof String) {
                            bookingRsp = mapper.readValue((String) refreshResp, com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
                        } else {
                            bookingRsp = mapper.convertValue(refreshResp, com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error refreshing booking after ChangeService: " + e.getMessage());
                }

                // --- Real-time Price Fetching (Added to Controller) ---
                java.util.Map<String, BigDecimal> actualServicePrices = new java.util.HashMap<>();
                if (changeServiceRsp != null && changeServiceRsp.getAerocrs() != null
                        && changeServiceRsp.getAerocrs().getDetails() != null) {
                    for (com.airlines.GO7API.responseGo7.ChangeServiceRspGo7Dto.Aerocrs.Detail detail : changeServiceRsp
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
                java.util.Optional<com.airlines.GO7API.entity.BookingEntity> entityOptForMaps = bookingService.getBookingByOrderId(orderId);
                if (entityOptForMaps.isPresent()) {
                    cachedSeats = entityOptForMaps.get().getPassengerSeats();
                    cachedServices = entityOptForMaps.get().getPassengerServices();
                }

                // 4. Generate Standard Response
                com.airlines.GO7API.responseDto.ChangeServiceRspDto standardResponse = com.airlines.GO7API.response.ChangeServiceResponse
                        .generateResponse(changeServiceRsp, bookingRsp, changeServiceReqDto, actualServicePrices, cachedSeats, cachedServices);

                // Cache the newly assigned services into the BookingEntity so ChangePayment can
                // use them
                if (orderId != null && standardResponse.getOrderItems() != null) {
                    java.util.Map<String, String> passengerServices = new java.util.HashMap<>();
                    for (com.airlines.GO7API.responseDto.ChangeServiceRspDto.OrderItemsDTO item : standardResponse
                            .getOrderItems()) {
                        if (item.getOrderItemId() != null && item.getOrderItemId().contains("_SRV")) {
                            if (item.getServiceList() != null) {
                                for (com.airlines.GO7API.responseDto.ChangeServiceRspDto.Service srv : item
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
                        java.util.Optional<com.airlines.GO7API.entity.BookingEntity> entityOpt = bookingService
                                .getBookingByOrderId(orderId);
                        if (entityOpt.isPresent()) {
                            com.airlines.GO7API.entity.BookingEntity entity = entityOpt.get();

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
                                    com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.Passengers pList = bookingRsp.getAerocrs().getBooking().getPassengers();
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

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during ChangeService request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/orderchange")
    public ResponseEntity<Object> orderChange(@RequestBody OrderChangeReqDto orderChangeReqDto) {
        try {
            // Fetch real Booking Confirmation from MongoDB using OrderID if available
            if (orderChangeReqDto.getOrderId() != null) {
                java.util.Optional<com.airlines.GO7API.entity.BookingEntity> entityOpt = bookingService
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

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during OrderChange request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/orderreshop")
    public ResponseEntity<Object> orderReshop(@RequestBody String requestBody) {
        try {
            JsonNode root = mapper.readTree(requestBody);

            String orderId = null;
            if (root.has("orderId")) {
                orderId = root.get("orderId").asText();
            } else if (root.has("aerocrs") && root.get("aerocrs").has("parms")) {
                JsonNode parms = root.get("aerocrs").get("parms");
                if (parms.has("bookingconfirmation"))
                    orderId = parms.get("bookingconfirmation").asText();
                else if (parms.has("bookingid"))
                    orderId = parms.get("bookingid").asText();
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
                java.util.Optional<com.airlines.GO7API.entity.BookingEntity> entityOpt = bookingService
                        .getBookingByOrderId(orderId);
                if (entityOpt.isPresent()) {
                    bookingConfirmation = entityOpt.get().getBookingConfirmation();
                }

                // Call Internal GetBooking to get pax counts
                com.airlines.GO7API.request.OrderRetrieveReq getBookingReq = SeatAvailabilityReq
                        .mapToGetBookingReq(bookingConfirmation);
                Object getBookingResponse = getBookingReq.unmarshal();

                if (getBookingResponse != null && !(getBookingResponse instanceof com.airlines.GO7API.error.ErrorRsp)) {
                    com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto bookingRsp;
                    if (getBookingResponse instanceof String) {
                        bookingRsp = mapper.readValue((String) getBookingResponse,
                                com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
                    } else {
                        bookingRsp = mapper.convertValue(getBookingResponse,
                                com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
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
                com.airlines.GO7API.responseDto.OrderReshopRspDto reshopRspDto = orderReshopResponse.orderReshopMapper(
                        response,
                        airshopReqDto);
                return new ResponseEntity<>(reshopRspDto, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during OrderReshop request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
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
                com.airlines.GO7API.responseDto.OrderReshopRspDto reshopRspDto = orderReshopResponse
                        .orderReshopMapper(response, airshopReqDto);
                return new ResponseEntity<>(reshopRspDto, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during FSOrderReshop request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private BigDecimal fetchActualSeatFare(Long bookingId, String flightNumber, String flightDate,
            String fromCode, String toCode, String seatNumber, String classCode) {
        try {
            com.airlines.GO7API.request.SeatAvailabilityReq req = new com.airlines.GO7API.request.SeatAvailabilityReq();
            com.airlines.GO7API.request.SeatAvailabilityReq.Aerocrs aerocrs = new com.airlines.GO7API.request.SeatAvailabilityReq.Aerocrs();
            com.airlines.GO7API.request.SeatAvailabilityReq.Parms parms = new com.airlines.GO7API.request.SeatAvailabilityReq.Parms();

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
                com.airlines.GO7API.responseGo7.SeatAvailabilityRspGo7Dto go7Rsp = mapper.readValue(responseJson,
                        com.airlines.GO7API.responseGo7.SeatAvailabilityRspGo7Dto.class);

                if (go7Rsp != null && go7Rsp.getAerocrs() != null && go7Rsp.getAerocrs().getSeatMapFare() != null) {
                    java.util.Map<String, com.airlines.GO7API.responseGo7.SeatAvailabilityRspGo7Dto.SeatClass> classes = go7Rsp
                            .getAerocrs().getSeatMapFare().getClasses();

                    String cleanClass = (classCode != null && classCode.contains("/")) ? classCode.split("/")[0]
                            : classCode;

                    if (classes != null && classes.containsKey(cleanClass)) {
                        java.util.List<com.airlines.GO7API.responseGo7.SeatAvailabilityRspGo7Dto.PaidSeatRow> rows = classes
                                .get(cleanClass).getPaidSeats();
                        if (rows != null) {
                            for (com.airlines.GO7API.responseGo7.SeatAvailabilityRspGo7Dto.PaidSeatRow row : rows) {
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
            com.airlines.GO7API.request.ServiceListReq req = new com.airlines.GO7API.request.ServiceListReq();
            com.airlines.GO7API.request.ServiceListReq.Aerocrs aerocrs = new com.airlines.GO7API.request.ServiceListReq.Aerocrs();
            com.airlines.GO7API.request.ServiceListReq.Parms parms = new com.airlines.GO7API.request.ServiceListReq.Parms();

            parms.setBookingId(bookingId);
            parms.setFlightId(flightId);
            parms.setCurrency("USD");

            aerocrs.setParms(parms);
            req.setAerocrs(aerocrs);
            req.setServiceListUrl("https://api.aerocrs.com/v5/getAncillaries");

            String responseJson = req.makeApiCall();
            if (responseJson != null) {
                com.airlines.GO7API.responseGo7.ServiceListRspGo7Dto go7Rsp = mapper.readValue(responseJson,
                        com.airlines.GO7API.responseGo7.ServiceListRspGo7Dto.class);

                if (go7Rsp != null && go7Rsp.getAerocrs() != null && go7Rsp.getAerocrs().getAncillaries() != null) {
                    java.util.List<com.airlines.GO7API.responseGo7.ServiceListRspGo7Dto.Ancillary> ancillaries = go7Rsp
                            .getAerocrs().getAncillaries().getAncillary();
                    if (ancillaries != null) {
                        for (com.airlines.GO7API.responseGo7.ServiceListRspGo7Dto.Ancillary anc : ancillaries) {
                            if (anc.getItems() != null) {
                                for (com.airlines.GO7API.responseGo7.ServiceListRspGo7Dto.Item item : anc.getItems()) {
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
