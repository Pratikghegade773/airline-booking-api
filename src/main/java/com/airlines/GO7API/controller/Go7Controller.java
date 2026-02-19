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
                                bookingService.saveBooking(pnr, bookingConfirmation, orderId);
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

            // Use ChangePaymentReq's method
            com.airlines.GO7API.request.ChangePaymentReq getBookingReq = com.airlines.GO7API.request.ChangePaymentReq
                    .mapToGetBookingReq(bookingConfirmation);
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
                    .generateResponse(getBookingResponse, changePaymentReqDto);

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
                    System.out.println("Found Booking in DB by OrderID: " + bookingEntity.getPnr());
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

            // Generate NDC Response
            com.airlines.GO7API.responseDto.OrderRetrieveRspDto ndcResponse = com.airlines.GO7API.response.OrderRetrieveResponse
                    .generateResponse(go7Response, orderRetrieveReqDto);

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

            // 1. Execute UnpaidCancel Request
            UnpaidCancelReq unpaidCancelReq = UnpaidCancelReq.mapToUnpaidCancelReq(unpaidCancelReqDto);
            Object cancelResponse = unpaidCancelReq.unmarshal();

            if (cancelResponse instanceof com.airlines.GO7API.error.ErrorRsp) {
                return new ResponseEntity<>(cancelResponse, HttpStatus.BAD_REQUEST);
            }

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
                // Conditional Payment & Ticketing Logic
                if (changeSeatReqDto.getPaymentInformation() != null) {
                    System.out.println("Processing Payment for Seat Change...");

                    // 1. Create ChangePaymentReqDto from SeatReq
                    com.airlines.GO7API.requestDto.ChangePaymentReqDto paymentReqDto = new com.airlines.GO7API.requestDto.ChangePaymentReqDto();
                    paymentReqDto.setOrderId(orderId); // Use original order ID
                    if (bookingRsp != null && bookingRsp.getAerocrs() != null
                            && bookingRsp.getAerocrs().getBooking() != null) {
                        // Prefer BookingID for payment if available, but DTO uses specific fields
                    }

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
                            // 3. Execute OrderTicket
                            com.airlines.GO7API.request.ChangePaymentReq orderTicketReq = com.airlines.GO7API.request.ChangePaymentReq
                                    .mapToOrderTicketReq(bookingIdForTicket);
                            Object ticketResponse = orderTicketReq.unmarshal();
                            System.out.println("OrderTicket (Seat) executed. Response: " + ticketResponse);

                            // 4. Refresh Booking (GetBooking) to populate tickets in response
                            System.out.println("Refreshing Booking after Payment/Ticket...");
                            com.airlines.GO7API.request.OrderRetrieveReq refreshBookingReq = SeatAvailabilityReq
                                    .mapToGetBookingReq(bookingConfirmation);
                            Object refreshResponse = refreshBookingReq.unmarshal();

                            if (!(refreshResponse instanceof com.airlines.GO7API.error.ErrorRsp)) {
                                if (refreshResponse instanceof String) {
                                    bookingRsp = mapper.readValue((String) refreshResponse,
                                            com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
                                } else {
                                    bookingRsp = mapper.convertValue(refreshResponse,
                                            com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
                                }
                            }
                        }
                    } else {
                        // Propagate Upstream Error (e.g. AeroCRS support team error)
                        System.out.println("Payment failed. Returning error response.");
                        return new ResponseEntity<>(paymentResponse, HttpStatus.BAD_REQUEST);
                    }
                }

                com.airlines.GO7API.responseDto.ChangeSeatRspDto ndcResponse = com.airlines.GO7API.response.ChangeSeatResponse
                        .generateResponse(changeSeatRsp, bookingRsp, changeSeatReqDto);
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

                System.out.println("ChangeService: Checking paymentInformation: "
                        + (changeServiceReqDto.getPaymentInformation() != null));
                // 2. Conditional Payment & Ticketing Logic
                if (changeServiceReqDto.getPaymentInformation() != null) {
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
                            // 3. Execute OrderTicket
                            com.airlines.GO7API.request.ChangePaymentReq orderTicketReq = com.airlines.GO7API.request.ChangePaymentReq
                                    .mapToOrderTicketReq(bookingIdForTicket);
                            Object ticketResponse = orderTicketReq.unmarshal();
                            System.out.println("OrderTicket (Service) executed. Response: " + ticketResponse);

                            // 4. Refresh Booking (GetBooking) to populate tickets in response
                            System.out.println("Refreshing Booking after Payment/Ticket...");
                            com.airlines.GO7API.request.OrderRetrieveReq refreshBookingReq = SeatAvailabilityReq
                                    .mapToGetBookingReq(bookingConfirmation);
                            Object refreshResponse = refreshBookingReq.unmarshal();

                            if (!(refreshResponse instanceof com.airlines.GO7API.error.ErrorRsp)) {
                                if (refreshResponse instanceof String) {
                                    bookingRsp = mapper.readValue((String) refreshResponse,
                                            com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
                                } else {
                                    bookingRsp = mapper.convertValue(refreshResponse,
                                            com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.class);
                                }
                            }
                        }
                    } else {
                        // Propagate Upstream Error (e.g. AeroCRS support team error)
                        System.out.println("Payment failed. Returning error response.");
                        return new ResponseEntity<>(paymentResponse, HttpStatus.BAD_REQUEST);
                    }
                }
            }

            // 4. Generate Standard Response
            com.airlines.GO7API.responseDto.ChangeServiceRspDto standardResponse = com.airlines.GO7API.response.ChangeServiceResponse
                    .generateResponse(changeServiceRsp, bookingRsp, changeServiceReqDto);

            return new ResponseEntity<>(standardResponse, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during ChangeService request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/orderchange")
    public ResponseEntity<Object> orderChange(@RequestBody OrderChangeReqDto orderChangeReqDto) {
        try {
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
            // Map the raw JSON to the internal Request object
            FSorderreshopReq orderReshopReq = FSorderreshopReq.mapFromJson(requestBody);

            // Call internal get booking to get booking confirmation if missing
            Object bConfObj = orderReshopReq.aerocrs.parms.get("bookingconfirmation");
            String bookingConfirmation = (bConfObj != null) ? String.valueOf(bConfObj) : null;
            Object bIdObj = orderReshopReq.aerocrs.parms.get("bookingid");
            Long bookingId = null;
            if (bIdObj instanceof Number) {
                bookingId = ((Number) bIdObj).longValue();
            } else if (bIdObj instanceof String) {
                try {
                    bookingId = Long.parseLong((String) bIdObj);
                } catch (Exception e) {
                }
            }

            // If confirmation is purely numeric, treat as ID and trigger lookup
            if (bookingConfirmation != null && bookingConfirmation.matches("\\d+")) {
                if (bookingId == null) {
                    try {
                        bookingId = Long.parseLong(bookingConfirmation);
                    } catch (Exception e) {
                    }
                }
                bookingConfirmation = null;
            }

            if (bookingConfirmation == null || bookingConfirmation.isEmpty()) {
                System.out.println(
                        "DEBUG: Missing bookingconfirmation. Attempting internal lookup for bookingId: " + bookingId);

                // 1. Try Database
                if (bookingId != null) {
                    java.util.Optional<com.airlines.GO7API.entity.BookingEntity> entity = bookingService
                            .getBookingByOrderId(String.valueOf(bookingId));
                    if (entity.isPresent()) {
                        bookingConfirmation = entity.get().getBookingConfirmation();
                        System.out.println("DEBUG: Found bookingconfirmation in DB: " + bookingConfirmation);
                    }
                }

                // 2. Try GetBooking API if still missing
                if (bookingConfirmation == null || bookingConfirmation.isEmpty()) {
                    if (bookingId != null) {
                        System.out.println("DEBUG: Calling internal getBooking API for bookingId: " + bookingId);
                        OrderCreateReq getBookingReq = OrderCreateReq.mapToOrderTicketReq(bookingId); // Using ticket
                                                                                                      // req logic as it
                                                                                                      // maps ID to
                                                                                                      // parms
                        getBookingReq.setApiUrl("https://api.aerocrs.com/v5/getBooking");
                        Object getBookingRsp = getBookingReq.unmarshal();

                        // Parse confirmation from response
                        if (getBookingRsp != null && !(getBookingRsp instanceof com.airlines.GO7API.error.ErrorRsp)) {
                            JsonNode root = mapper.valueToTree(getBookingRsp);
                            bookingConfirmation = root.path("aerocrs").path("booking").path("bookingconfirmation")
                                    .asText();
                            System.out.println("DEBUG: Retrieved bookingconfirmation from API: " + bookingConfirmation);
                        }
                    }
                }

                // 3. Update Request
                if (bookingConfirmation != null && !bookingConfirmation.isEmpty()) {
                    orderReshopReq.aerocrs.parms.put("bookingconfirmation", bookingConfirmation);
                }
            }

            // Execute the API call
            Object response = orderReshopReq.unmarshal();

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during OrderReshop request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/FSorderreshop")
    public ResponseEntity<Object> fsOrderReshop(@RequestBody String requestBody) {
        try {
            // Map the raw JSON to the internal Request object
            FSorderreshopReq orderReshopReq = FSorderreshopReq.mapFromJson(requestBody);

            // Call internal get booking to get booking confirmation if missing
            Object bConfObj = orderReshopReq.aerocrs.parms.get("bookingconfirmation");
            String bookingConfirmation = (bConfObj != null) ? String.valueOf(bConfObj) : null;
            Object bIdObj = orderReshopReq.aerocrs.parms.get("bookingid");
            Long bookingId = null;
            if (bIdObj instanceof Number) {
                bookingId = ((Number) bIdObj).longValue();
            } else if (bIdObj instanceof String) {
                try {
                    bookingId = Long.parseLong((String) bIdObj);
                } catch (Exception e) {
                }
            }

            // If confirmation is purely numeric, treat as ID and trigger lookup
            if (bookingConfirmation != null && bookingConfirmation.matches("\\d+")) {
                if (bookingId == null) {
                    try {
                        bookingId = Long.parseLong(bookingConfirmation);
                    } catch (Exception e) {
                    }
                }
                bookingConfirmation = null;
            }

            if (bookingConfirmation == null || bookingConfirmation.isEmpty()) {
                System.out.println(
                        "DEBUG: Missing bookingconfirmation. Attempting internal lookup for bookingId: " + bookingId);

                // 1. Try Database
                if (bookingId != null) {
                    java.util.Optional<com.airlines.GO7API.entity.BookingEntity> entity = bookingService
                            .getBookingByOrderId(String.valueOf(bookingId));
                    if (entity.isPresent()) {
                        bookingConfirmation = entity.get().getBookingConfirmation();
                        System.out.println("DEBUG: Found bookingconfirmation in DB: " + bookingConfirmation);
                    }
                }

                // 2. Try GetBooking API if still missing
                if (bookingConfirmation == null || bookingConfirmation.isEmpty()) {
                    if (bookingId != null) {
                        System.out.println("DEBUG: Calling internal getBooking API for bookingId: " + bookingId);
                        OrderCreateReq getBookingReq = OrderCreateReq.mapToOrderTicketReq(bookingId);
                        getBookingReq.setApiUrl("https://api.aerocrs.com/v5/getBooking");
                        Object getBookingRsp = getBookingReq.unmarshal();

                        if (getBookingRsp != null && !(getBookingRsp instanceof com.airlines.GO7API.error.ErrorRsp)) {
                            JsonNode root = mapper.valueToTree(getBookingRsp);
                            bookingConfirmation = root.path("aerocrs").path("booking").path("bookingconfirmation")
                                    .asText();
                            System.out.println("DEBUG: Retrieved bookingconfirmation from API: " + bookingConfirmation);
                        }
                    }
                }

                if (bookingConfirmation != null && !bookingConfirmation.isEmpty()) {
                    orderReshopReq.aerocrs.parms.put("bookingconfirmation", bookingConfirmation);
                }
            }

            // Execute the API call
            Object response = orderReshopReq.unmarshal();

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during FSOrderReshop request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
