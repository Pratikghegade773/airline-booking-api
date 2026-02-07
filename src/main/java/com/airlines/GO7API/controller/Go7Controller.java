package com.airlines.GO7API.controller;

import com.airlines.GO7API.request.*;
import com.airlines.GO7API.requestDto.*;
import com.airlines.GO7API.response.AirshopResponse;
import com.airlines.GO7API.response.OfferPriceResponse;
import com.airlines.GO7API.responseDto.AirshopRspDto;
import com.airlines.GO7API.responseGo7.AirshopRspGo7Dto;
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
    private com.airlines.GO7API.service.BookingService bookingService;

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

    @PostMapping("/ordercancel")
    public ResponseEntity<Object> orderCancel(@RequestBody OrderCancelReqDto orderCancelReqDto) {
        try {
            System.out.println("Processing OrderCancel for: " + orderCancelReqDto.getOrderId());

            // 1. Direct OrderCancel Request
            // We assume orderCancelReqDto.getOrderId() is the numeric bookingid expected by
            // OrderCancelReq
            OrderCancelReq orderCancelReq = OrderCancelReq.mapToOrderCancelReq(orderCancelReqDto);
            Object cancelResponse = orderCancelReq.unmarshal();

            if (cancelResponse instanceof com.airlines.GO7API.error.ErrorRsp) {
                return new ResponseEntity<>(cancelResponse, HttpStatus.BAD_REQUEST);
            }

            // 2. Map OrderCancel Response to OrderRetrieveRspDto
            // The OrderCancel API returns a flat structure under 'aerocrs' (success,
            // bookingid, status, pnrref),
            // while OrderRetrieveRspGo7Dto expects 'aerocrs.booking'. We must bridge this
            // gap manually.

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto go7CancelRsp = new com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto();
            com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.Aerocrs aerocrs = new com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.Aerocrs();
            com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.Booking booking = new com.airlines.GO7API.responseGo7.OrderRetrieveRspGo7Dto.Booking();

            com.fasterxml.jackson.databind.JsonNode rootNode;
            if (cancelResponse instanceof String) {
                rootNode = mapper.readTree((String) cancelResponse);
            } else {
                rootNode = mapper.valueToTree(cancelResponse);
            }

            if (rootNode.has("aerocrs")) {
                com.fasterxml.jackson.databind.JsonNode aNode = rootNode.get("aerocrs");
                aerocrs.setSuccess(aNode.path("success").asBoolean());

                // Map flat fields to Booking object
                if (aNode.has("bookingid"))
                    booking.setBookingid(aNode.path("bookingid").asLong());
                if (aNode.has("status")) {
                    String status = aNode.path("status").asText();
                    booking.setStatus(status);

                    // Check if Ticketed and return error
                    if ("Ticketed".equalsIgnoreCase(status)) {
                        com.airlines.GO7API.error.ErrorRsp errorRsp = new com.airlines.GO7API.error.ErrorRsp();
                        com.airlines.GO7API.error.ErrorRsp.Error error = new com.airlines.GO7API.error.ErrorRsp.Error();
                        error.setCode("ORDER_CANCEL_FAILED");
                        error.setError("Cannot cancel booking. Status is Ticketed.");
                        errorRsp.getErrorList().add(error);
                        return new ResponseEntity<>(errorRsp, HttpStatus.BAD_REQUEST);
                    }
                }
                if (aNode.has("bookingconfirmation"))
                    booking.setBookingconfirmation(aNode.path("bookingconfirmation").asText());
                if (aNode.has("pnrref"))
                    booking.setPnrref(aNode.path("pnrref").asText());

                aerocrs.setBooking(booking);
                go7CancelRsp.setAerocrs(aerocrs);
            }

            // Generate NDC Response (Sparse)
            // We construct a temporary retrieveReqDto just for the mapper context
            com.airlines.GO7API.requestDto.OrderRetrieveReqDto tempRetrieveReqDto = new com.airlines.GO7API.requestDto.OrderRetrieveReqDto();
            tempRetrieveReqDto.setOrderId(orderCancelReqDto.getOrderId());

            com.airlines.GO7API.responseDto.OrderRetrieveRspDto ndcResponse = com.airlines.GO7API.response.OrderRetrieveResponse
                    .generateResponse(go7CancelRsp, tempRetrieveReqDto);

            return new ResponseEntity<>(ndcResponse, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during OrderCancel request: " + e.getMessage(),
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
                if (parms.has("flightid")) {
                    serviceListReqDto.setFlightid(parms.get("flightid").asText());
                }
                if (parms.has("currency")) {
                    serviceListReqDto.setCurrency(parms.get("currency").asText());
                }
                // Map other fields if necessary
            } else {
                // Fallback: Try to map directly if structure is flat or matches DTO
                serviceListReqDto = mapper.readValue(requestBody, ServiceListReqDto.class);
            }

            // Map the DTO to the internal Request object
            ServiceListReq serviceListReq = ServiceListReq.mapToServiceListRequestDTO(serviceListReqDto);

            // Execute the API call and return the raw response
            Object response = serviceListReq.unmarshal();

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during ServiceList request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/seatavailability")
    public ResponseEntity<Object> seatAvailability(@RequestBody SeatMapReqDto seatAvailabilityReqDto) {
        try {
            // Map the DTO to the internal Request object
            SeatMapReq seatAvailabilityReq = SeatMapReq
                    .mapToSeatAvailabilityRequestDTO(seatAvailabilityReqDto);

            // Execute the API call and return the raw response
            Object response = seatAvailabilityReq.unmarshal();

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during SeatAvailability request: " + e.getMessage(),
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
    public ResponseEntity<Object> orderReshop(@RequestBody OrderReshopReqDto orderReshopReqDto) {
        try {
            // Map the DTO to the internal Request object
            OrderReshopReq orderReshopReq = OrderReshopReq.mapToOrderReshopReq(orderReshopReqDto);

            // Execute the API call and return the raw response
            Object response = orderReshopReq.unmarshal();

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during OrderReshop request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
