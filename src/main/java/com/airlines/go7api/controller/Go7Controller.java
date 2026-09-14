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

@SuppressWarnings({"java:S120", "java:S2142"})
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
            Object response = OfferPriceResponse.generateResponse(offerPriceRequestDTO);

            return new ResponseEntity<>(response, HttpStatus.OK);

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
    private static final String ORDERTICKET_RESPONSE_LOG = "OrderTicket executed. Response: {}";

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

        } catch (Exception e) {
            return handleException("OrderCreate", e);
        }
    }

    private ResponseEntity<Object> processOrderConfirmAndGetBooking(OrderCreateReqDto orderCreateReqDto, Long bookingId) throws java.io.IOException {
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
                error.setErrorMessage(msg);
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
            if (logger.isErrorEnabled()) {
                logger.error("Failed to extract bookingconfirmation: {}", e.getMessage());
            }
        }
        return null;
    }

    private ResponseEntity<Object> processPaymentAndTicketing(OrderCreateReqDto orderCreateReqDto, Long bookingId) throws java.io.IOException {
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
            logger.info(ORDERTICKET_RESPONSE_LOG, ticketResponse);

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
            if (logger.isErrorEnabled()) {
                logger.error("Error verifying payment success: {}", e.getMessage());
            }
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
            if (logger.isErrorEnabled()) {
                logger.error("Failed to save booking to DB: {}", e.getMessage());
            }
        }
    }

    private static class CachedMappings {
        java.util.Map<String, String> passengerSeats = null;
        java.util.Map<String, String> passengerServices = null;
    }

    private CachedMappings loadCachedMappings(String orderId) {
        CachedMappings mappings = new CachedMappings();
        if (orderId == null || orderId.isEmpty()) {
            return mappings;
        }

        java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOpt = bookingService
                .getBookingByOrderId(orderId);
        if (!entityOpt.isPresent()) {
            return mappings;
        }

        com.airlines.go7api.entity.BookingEntity entity = entityOpt.get();
        if (entity.getPassengerSeats() != null) {
            mappings.passengerSeats = entity.getPassengerSeats();
            logger.info("ChangePayment: Loaded {} cached passenger seat mappings from DB.", mappings.passengerSeats.size());
        }
        if (entity.getPassengerServices() != null) {
            mappings.passengerServices = entity.getPassengerServices();
            logger.info("ChangePayment: Loaded {} cached passenger service mappings from DB.", mappings.passengerServices.size());
        }
        return mappings;
    }

    private static class PaymentSuccessResult {
        boolean success = false;
        Long bookingId = null;
    }

    private PaymentSuccessResult verifyPaymentSuccessAndExtractBookingId(Object response) {
        PaymentSuccessResult result = new PaymentSuccessResult();
        try {
            ObjectMapper pmMapper = new ObjectMapper();
            String pmJson = pmMapper.writeValueAsString(response);
            com.fasterxml.jackson.databind.JsonNode pmRoot = pmMapper.readTree(pmJson);
            if ((pmRoot.has(KEY_SUCCESS) && pmRoot.get(KEY_SUCCESS).asBoolean()) ||
                (pmRoot.has(KEY_AEROCRS) && pmRoot.get(KEY_AEROCRS).has(KEY_SUCCESS) && pmRoot.get(KEY_AEROCRS).get(KEY_SUCCESS).asBoolean())) {
                result.success = true;
            }

            // Extract BookingID for OrderTicket
            if (pmRoot.has(KEY_AEROCRS)) {
                com.fasterxml.jackson.databind.JsonNode aerocrsNode = pmRoot.get(KEY_AEROCRS);
                if (aerocrsNode.has(KEY_BOOKING) && aerocrsNode.get(KEY_BOOKING).has(KEY_BOOKING_ID)) {
                    result.bookingId = aerocrsNode.get(KEY_BOOKING).get(KEY_BOOKING_ID).asLong();
                } else if (aerocrsNode.has(KEY_BOOKING_ID)) {
                    result.bookingId = aerocrsNode.get(KEY_BOOKING_ID).asLong();
                }
            }
        } catch (Exception e) {
            if (logger.isInfoEnabled()) {
                logger.info("Error verifying change payment success/extracting bookingId: {}", e.getMessage());
            }
        }
        return result;
    }

    private boolean verifyTicketSuccess(Object ticketResponse) {
        try {
            ObjectMapper ticketMapper = new ObjectMapper();
            String ticketJson = ticketMapper.writeValueAsString(ticketResponse);
            com.fasterxml.jackson.databind.JsonNode ticketRoot = ticketMapper.readTree(ticketJson);
            if ((ticketRoot.has(KEY_SUCCESS) && ticketRoot.get(KEY_SUCCESS).asBoolean()) ||
                (ticketRoot.has(KEY_AEROCRS) && ticketRoot.get(KEY_AEROCRS).has(KEY_SUCCESS) && ticketRoot.get(KEY_AEROCRS).get(KEY_SUCCESS).asBoolean())) {
                return true;
            }
        } catch (Exception e) {
            if (logger.isInfoEnabled()) {
                logger.info("Error verifying OrderTicket success: {}", e.getMessage());
            }
        }
        return false;
    }

    private String extractBookingConfirmationFromTicketResponse(Object ticketResponse) {
        try {
            ObjectMapper tMapper = new ObjectMapper();
            String tJson = tMapper.writeValueAsString(ticketResponse);
            com.fasterxml.jackson.databind.JsonNode tRoot = tMapper.readTree(tJson);

            if (tRoot.has(KEY_AEROCRS)) {
                com.fasterxml.jackson.databind.JsonNode aerocrsNode = tRoot.get(KEY_AEROCRS);

                // Check direct confirmation at aerocrs root
                if (aerocrsNode.has(KEY_BOOKING_CONFIRMATION)) {
                    String bookingConfirmation = aerocrsNode.get(KEY_BOOKING_CONFIRMATION).asText();
                    logger.info("Extracted bookingconfirmation from OrderTicket (root): {}", bookingConfirmation);
                    return bookingConfirmation;
                }
                // Fallback: Check nested under booking
                else if (aerocrsNode.has(KEY_BOOKING) && aerocrsNode.get(KEY_BOOKING).has(KEY_BOOKING_CONFIRMATION)) {
                    String bookingConfirmation = aerocrsNode.get(KEY_BOOKING).get(KEY_BOOKING_CONFIRMATION).asText();
                    logger.info("Extracted bookingconfirmation from OrderTicket (nested): {}", bookingConfirmation);
                    return bookingConfirmation;
                }
            }
        } catch (Exception e) {
            if (logger.isInfoEnabled()) {
                logger.info("Failed to extract bookingconfirmation from OrderTicket response: {}", e.getMessage());
            }
        }
        return null;
    }

    private static class OrderTicketResult {
        ResponseEntity<Object> errorResponse = null;
        String bookingConfirmation = null;
    }

    private OrderTicketResult processOrderTicket(Long bookingId) throws java.io.IOException {
        OrderTicketResult result = new OrderTicketResult();
        if (bookingId != null && bookingId > 0) {
            logger.info("Calling Internal OrderTicket for BookingID: {}", bookingId);
            com.airlines.go7api.request.ChangePaymentReq orderTicketReq = com.airlines.go7api.request.ChangePaymentReq
                    .mapToOrderTicketReq(bookingId);
            Object ticketResponse = orderTicketReq.unmarshal();
            logger.info(ORDERTICKET_RESPONSE_LOG, ticketResponse);

            if (ticketResponse instanceof com.airlines.go7api.error.ErrorRsp) {
                result.errorResponse = new ResponseEntity<>(ticketResponse, HttpStatus.BAD_REQUEST);
                return result;
            }

            if (!verifyTicketSuccess(ticketResponse)) {
                logger.info("OrderTicket failed. Aborting GetBooking.");
                result.errorResponse = new ResponseEntity<>(ticketResponse, HttpStatus.BAD_REQUEST);
                return result;
            }

            result.bookingConfirmation = extractBookingConfirmationFromTicketResponse(ticketResponse);
        } else {
            logger.info("BookingID could not be extracted. Skipping OrderTicket.");
        }
        return result;
    }

    private static class BookingDetailsResult {
        String bookingConfirmation;
        String finalLastName;
    }

    private BookingDetailsResult resolveFinalBookingDetails(String orderId, String bookingConfirmation) {
        BookingDetailsResult result = new BookingDetailsResult();
        result.bookingConfirmation = bookingConfirmation;
        if (orderId != null) {
            java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOpt = bookingService.getBookingByOrderId(orderId);
            if (entityOpt.isPresent()) {
                if (result.bookingConfirmation == null || result.bookingConfirmation.equals(orderId)) {
                    result.bookingConfirmation = entityOpt.get().getBookingConfirmation();
                }
                result.finalLastName = entityOpt.get().getPrimaryPassengerLastName();
            }
        }
        return result;
    }

    private void updateTotalOrderPriceInDb(String orderId, BigDecimal totalOrderPrice) {
        if (orderId != null && totalOrderPrice != null) {
            java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOpt = bookingService
                    .getBookingByOrderId(orderId);
            if (entityOpt.isPresent()) {
                com.airlines.go7api.entity.BookingEntity entity = entityOpt.get();
                entity.setTotalOrderPrice(totalOrderPrice);
                bookingService.saveBooking(entity);
            }
        }
    }

    @PostMapping("/change/payment")
    public ResponseEntity<Object> changePayment(@RequestBody ChangePaymentReqDto changePaymentReqDto) {
        try {
            CachedMappings mappings = loadCachedMappings(changePaymentReqDto.getOrderId());

            // 0. Pre-fetch GetBooking to calculate combined amount for seats/services if
            // included
            preFetchCombinedAmount(changePaymentReqDto, mappings.passengerSeats, mappings.passengerServices);

            // Map the DTO to the internal Request object
            com.airlines.go7api.request.ChangePaymentReq changePaymentReq = com.airlines.go7api.request.ChangePaymentReq
                    .mapToChangePaymentReq(changePaymentReqDto);

            // Execute the API call
            Object response = changePaymentReq.unmarshal();

            if (response instanceof com.airlines.go7api.error.ErrorRsp) {
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            PaymentSuccessResult payResult = verifyPaymentSuccessAndExtractBookingId(response);

            if (!payResult.success) {
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Internal Call: OrderTicket (New Addition)
            OrderTicketResult ticketResult = processOrderTicket(payResult.bookingId);
            if (ticketResult.errorResponse != null) {
                return ticketResult.errorResponse;
            }

            String bookingConfirmation = ticketResult.bookingConfirmation;

            // Internal Call: GetBooking
            // Logic: Use extracted confirmation or fallback to OrderID
            if (bookingConfirmation == null && changePaymentReqDto.getOrderId() != null) {
                bookingConfirmation = changePaymentReqDto.getOrderId();
                logger.info("Using OrderID as fallback for bookingconfirmation: {}", bookingConfirmation);
            }

            logger.info("ChangePayment successful. Calling GetBooking for ID: {}", bookingConfirmation);

            BookingDetailsResult details = resolveFinalBookingDetails(changePaymentReqDto.getOrderId(), bookingConfirmation);

            com.airlines.go7api.request.ChangePaymentReq getBookingReq = com.airlines.go7api.request.ChangePaymentReq
                    .mapToGetBookingReq(details.bookingConfirmation, details.finalLastName);
            Object finalResponse = getBookingReq.unmarshal();

            logger.info("GetBooking executed. Response: {}", finalResponse);

            if (finalResponse instanceof com.airlines.go7api.error.ErrorRsp) {
                return new ResponseEntity<>(finalResponse, HttpStatus.BAD_REQUEST);
            }

            // Convert final response (GetBooking result) to ChangePaymentRspGo7Dto
            com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto getBookingResponse = deserializeResponse(finalResponse, com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto.class);

            // Generate NDC Response using ChangePaymentResponse mapper
            com.airlines.go7api.responsedto.ChangePaymentRspDto ndcResponse = com.airlines.go7api.response.ChangePaymentResponse
                    .generateResponse(getBookingResponse, changePaymentReqDto, mappings.passengerSeats, mappings.passengerServices);

            updateTotalOrderPriceInDb(changePaymentReqDto.getOrderId(), ndcResponse.getTotalOrderPrice());

            return new ResponseEntity<>(ndcResponse, HttpStatus.OK);

        } catch (Exception e) {
            return handleException("ChangePayment", e);
        }
    }

    @PostMapping("/orderretrieve")
    public ResponseEntity<Object> orderRetrieve(@RequestBody OrderRetrieveReqDto orderRetrieveReqDto) {
        try {
            // DB Lookup Logic
            com.airlines.go7api.entity.BookingEntity bookingEntity = lookupBookingEntityForOrderRetrieve(orderRetrieveReqDto);

            // Populate DTO with retrieved data if PNR was missing
            if (bookingEntity != null && (orderRetrieveReqDto.getPnr() == null || orderRetrieveReqDto.getPnr().isEmpty())) {
                orderRetrieveReqDto.setPnr(bookingEntity.getPnr());
            }

            String storedBookingConfirmation = null;
            if (bookingEntity != null) {
                storedBookingConfirmation = bookingEntity.getBookingConfirmation();
                if (storedBookingConfirmation != null) {
                    logger.info("Using BookingConfirmation from DB: {}", storedBookingConfirmation);
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
                    .generateResponse(go7Response, passengerSeats, passengerServices,
                            dbTotalOrderPrice);

            // SYNC: Update database with any new info from OrderRetrieve (including last name if missing)
            syncBookingEntityFromGo7Response(bookingEntity, go7Response);

            return new ResponseEntity<>(ndcResponse, HttpStatus.OK);

        } catch (Exception e) {
            return handleException("OrderRetrieve", e);
        }
    }

    private com.airlines.go7api.entity.BookingEntity lookupBookingEntityForOrderRetrieve(OrderRetrieveReqDto orderRetrieveReqDto) {
        com.airlines.go7api.entity.BookingEntity bookingEntity = null;

        if (orderRetrieveReqDto.getOrderId() != null && !orderRetrieveReqDto.getOrderId().isEmpty()) {
            logger.info("Looking up by OrderID: {}", orderRetrieveReqDto.getOrderId());
            java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOpt = bookingService
                    .getBookingByOrderId(orderRetrieveReqDto.getOrderId());
            if (entityOpt.isPresent()) {
                bookingEntity = entityOpt.get();
                logger.info("Found Booking in DB by OrderID: {} (PNR: {})", bookingEntity.getOrderId(), bookingEntity.getPnr());
            } else {
                logger.info("Booking not found in DB by OrderID.");
            }
        }

        // If not found by OrderID (or OrderID not provided), and PNR is present, look
        // up by PNR
        if (bookingEntity == null && orderRetrieveReqDto.getPnr() != null
                && !orderRetrieveReqDto.getPnr().isEmpty()) {
            logger.info("Looking up by PNR: {}", orderRetrieveReqDto.getPnr());
            java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOpt = bookingService
                    .getBookingByPnr(orderRetrieveReqDto.getPnr());
            if (entityOpt.isPresent()) {
                bookingEntity = entityOpt.get();
                logger.info("Found Booking in DB by PNR: {}", bookingEntity.getPnr());
            } else {
                logger.info("Booking not found in DB by PNR.");
            }
        }
        return bookingEntity;
    }

    private void syncBookingEntityFromGo7Response(com.airlines.go7api.entity.BookingEntity bookingEntity, com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto go7Response) {
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
    }

    private static final String G7_API = "G7-API";
    private static final String KEY_DETAILS = "details";
    private static final String KEY_DETAIL = "detail";

    @PostMapping("/unpaidcancel")
    public ResponseEntity<Object> unpaidCancel(@RequestBody UnpaidCancelReqDto unpaidCancelReqDto) {
        try {
            logger.info("Processing UnpaidCancel for OrderID: {}", unpaidCancelReqDto.getOrderId());

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

            ResponseEntity<Object> validationError = validateUnpaidCancelResponse(validationResponse);
            if (validationError != null) {
                return validationError;
            }
            // --- END VALIDATION ---

            // 1. Execute UnpaidCancel Request
            UnpaidCancelReq unpaidCancelReq = UnpaidCancelReq.mapToUnpaidCancelReq(unpaidCancelReqDto);
            Object cancelResponse = unpaidCancelReq.unmarshal();

            if (cancelResponse instanceof com.airlines.go7api.error.ErrorRsp) {
                return new ResponseEntity<>(cancelResponse, HttpStatus.BAD_REQUEST);
            }

            // --- Post-call Validation: Handle provider-side failure without 'errors' array
            ResponseEntity<Object> postCallError = validateProviderSideCancelFailure(cancelResponse);
            if (postCallError != null) {
                return postCallError;
            }
            // --- End Post-call Validation ---

            // 2. Fetch Full Booking Details (Internal OrderRetrieve)
            logger.info("UnpaidCancel executed. Calling internal GetBooking for OrderID: {}", unpaidCancelReqDto.getOrderId());

            // DB Lookup Logic for BookingConfirmation (Matches OrderRetrieve flow)
            String storedBookingConfirmation = getStoredBookingConfirmationForCancel(unpaidCancelReqDto, cancelResponse);

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
                logger.info("Failed to retrieve booking details after cancellation.");
                return new ResponseEntity<>(bookingResponse, HttpStatus.BAD_REQUEST);
            }

            // 3. Convert Booking Response to UnpaidCancelRspGo7Dto
            com.airlines.go7api.responsego7.UnpaidCancelRspGo7Dto fullBooking = deserializeResponse(bookingResponse, com.airlines.go7api.responsego7.UnpaidCancelRspGo7Dto.class);

            // 4. Generate NDC Response
            com.airlines.go7api.responsedto.UnpaidCancelRspDto ndcResponse = com.airlines.go7api.response.UnpaidCancelResponse
                    .generateResponse(fullBooking);

            return new ResponseEntity<>(ndcResponse, HttpStatus.OK);

        } catch (Exception e) {
            return handleException("UnpaidCancel", e);
        }
    }

    private ResponseEntity<Object> validateUnpaidCancelResponse(Object validationResponse) {
        if (!(validationResponse instanceof com.airlines.go7api.error.ErrorRsp)) {
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp = deserializeValidationResponse(validationResponse);
            if (bookingRsp != null && bookingRsp.getAerocrs() != null && bookingRsp.getAerocrs().getBooking() != null) {
                if (hasTickets(bookingRsp)) {
                    return buildErrorRsp("Cannot perform UnpaidCancel. Booking is ticketed and can not be canceled from this interface at the moment, please consult airline.");
                }
                if (isFullyPaid(bookingRsp)) {
                    return buildErrorRsp("Cannot perform UnpaidCancel. The order has already been fully paid.");
                }
            }
        }
        return null;
    }

    private com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto deserializeValidationResponse(Object validationResponse) {
        try {
            return deserializeResponse(validationResponse, com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto.class);
        } catch (Exception e) {
            return null; // Ignored: If deserialization fails, validation fails safely via subsequent null-check on bookingRsp
        }
    }

    private boolean hasTickets(com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp) {
        if (bookingRsp.getAerocrs().getBooking().getPassengers() != null && bookingRsp.getAerocrs().getBooking().getPassengers().getPassenger() != null) {
            for (com.airlines.go7api.responsego7.common.Passenger p : bookingRsp.getAerocrs().getBooking().getPassengers().getPassenger()) {
                if (p.getETickets() != null && p.getETickets().getFlight() != null && !p.getETickets().getFlight().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isFullyPaid(com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp) {
        if (bookingRsp.getAerocrs().getBooking().getBalanceInformation() != null) {
            double outstanding = bookingRsp.getAerocrs().getBooking().getBalanceInformation().getPnrOutstandingPayment();
            return outstanding <= 0;
        }
        return false;
    }

    private ResponseEntity<Object> buildErrorRsp(String message) {
        com.airlines.go7api.error.ErrorRsp errorRsp = new com.airlines.go7api.error.ErrorRsp();
        errorRsp.setAirlineCode("G7");
        errorRsp.setSource(G7_API);
        com.airlines.go7api.error.ErrorRsp.Error error = new com.airlines.go7api.error.ErrorRsp.Error();
        error.setErrorMessage(message);
        error.setCode("400");
        errorRsp.getErrorList().add(error);
        return new ResponseEntity<>(errorRsp, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Object> validateProviderSideCancelFailure(Object cancelResponse) {
        try {
            ObjectMapper tempMapper = new ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = tempMapper.valueToTree(cancelResponse);
            if (root.has(KEY_SUCCESS) && !root.get(KEY_SUCCESS).asBoolean()) {
                String errMsg = "Cancellation failed on provider side.";
                if (root.has(KEY_DETAILS) && root.get(KEY_DETAILS).has(KEY_DETAIL)) {
                    com.fasterxml.jackson.databind.JsonNode detailNode = root.get(KEY_DETAILS).get(KEY_DETAIL);
                    if (detailNode.isArray() && detailNode.size() > 0) {
                        errMsg = detailNode.get(0).asText();
                    }
                }
                return buildErrorRsp(errMsg);
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Error parsing cancelResponse for failure: {}", e.getMessage());
            }
        }
        return null;
    }

    private String getStoredBookingConfirmationForCancel(UnpaidCancelReqDto unpaidCancelReqDto, Object cancelResponse) {
        if (unpaidCancelReqDto.getOrderId() != null && !unpaidCancelReqDto.getOrderId().isEmpty()) {
            java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOpt = bookingService.getBookingByOrderId(unpaidCancelReqDto.getOrderId());
            if (entityOpt.isPresent()) {
                String conf = entityOpt.get().getBookingConfirmation();
                logger.info("Found Booking in DB. Using stored BookingConfirmation: {}", conf);
                return conf;
            } else {
                logger.info("Booking not found in DB by OrderID during UnpaidCancel.");
            }
        }

        // Fallback: Extract from cancel response if DB lookup failed
        return extractBookingConfirmationFromCancelResponse(cancelResponse);
    }

    private com.fasterxml.jackson.databind.JsonNode convertCancelResponseToNode(Object cancelResponse) {
        ObjectMapper tempMapper = new ObjectMapper();
        if (cancelResponse instanceof String stringResponse) {
            try {
                return tempMapper.readTree(stringResponse);
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to extract confirmation from String response: {}", e.getMessage());
                }
            }
        } else if (cancelResponse != null) {
            try {
                return tempMapper.valueToTree(cancelResponse);
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to extract confirmation from Object response: {}", e.getMessage());
                }
            }
        }
        return null;
    }

    private String extractBookingConfirmationFromCancelResponse(Object cancelResponse) {
        com.fasterxml.jackson.databind.JsonNode root = convertCancelResponseToNode(cancelResponse);
        if (root != null) {
            if (root.has(KEY_AEROCRS) && root.get(KEY_AEROCRS).has(KEY_BOOKING_CONFIRMATION)) {
                return root.get(KEY_AEROCRS).get(KEY_BOOKING_CONFIRMATION).asText();
            } else if (root.has(KEY_BOOKING_CONFIRMATION)) {
                return root.get(KEY_BOOKING_CONFIRMATION).asText();
            }
        }
        return null;
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

    private com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto fetchBookingDetails(String orderId) throws java.io.IOException {
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
            if (logger.isErrorEnabled()) {
                logger.error("Error verifying payment success: {}", e.getMessage());
            }
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
                logger.info(ORDERTICKET_RESPONSE_LOG, ticketResponse);
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
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Error refreshing booking after {}: {}", changeType, e.getMessage());
            }
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
                    .generateResponse(serviceListRspGo7, bookingRsp);

            return new ResponseEntity<>(ndcResponse, HttpStatus.OK);

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

        } catch (Exception e) {
            return handleException("SeatAvailability", e);
        }
    }

    @PostMapping("/change/seat")
    public ResponseEntity<Object> changeSeat(@RequestBody ChangeSeatReqDto changeSeatReqDto) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
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
                return handleChangeSeatResponse(changeSeatRsp, orderId, bookingConfirmation, bookingRsp, changeSeatReqDto, objectMapper);
            }

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            return handleException(CHANGE_SEAT, e);
        }
    }

    private ResponseEntity<Object> handleChangeSeatResponse(
            ChangeSeatRspGo7Dto changeSeatRsp,
            String orderId, String bookingConfirmation,
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp,
            ChangeSeatReqDto changeSeatReqDto,
            ObjectMapper objectMapper) throws java.io.IOException {

        // Determine if at least one seat was successfully assigned
        boolean anySeatSuccessful = isAnySeatSuccessful(changeSeatRsp);

        // Conditional Payment & Ticketing Logic
        ResponseEntity<Object> paymentResult = processConditionalPaymentForSeat(
                changeSeatReqDto, anySeatSuccessful, orderId, bookingRsp, objectMapper);
        if (paymentResult != null) {
            return paymentResult;
        }

        // ALWAYS Refresh Booking (GetBooking) after change to get updated PNR total and balance
        bookingRsp = refreshBookingDetails(bookingConfirmation, CHANGE_SEAT, bookingRsp);

        // --- Real-time Price Fetching (Added to Controller) ---
        java.util.Map<String, BigDecimal> actualSeatPrices = fetchActualSeatPrices(changeSeatRsp, bookingRsp);
        // -----------------------------------------------------

        // Load cached maps from DB to pass to mapper for cumulative response
        java.util.Map<String, String> cachedSeats = null;
        java.util.Map<String, String> cachedServices = null;
        java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOptForMaps = bookingService.getBookingByOrderId(orderId);
        if (entityOptForMaps.isPresent()) {
            cachedSeats = entityOptForMaps.get().getPassengerSeats();
            cachedServices = entityOptForMaps.get().getPassengerServices();
        }

        // Generate Standard Response
        com.airlines.go7api.responsedto.ChangeSeatRspDto standardResponse = com.airlines.go7api.response.ChangeSeatResponse
                .generateResponse(changeSeatRsp, bookingRsp, changeSeatReqDto, actualSeatPrices, cachedSeats, cachedServices);

        // Cache the newly assigned seats into the BookingEntity so ChangePayment can use them
        cacheAssignedSeats(orderId, standardResponse, bookingRsp);

        return new ResponseEntity<>(standardResponse, HttpStatus.OK);
    }

    private boolean isAnySeatSuccessful(ChangeSeatRspGo7Dto changeSeatRsp) {
        if (changeSeatRsp.getAerocrs() == null || changeSeatRsp.getAerocrs().getFlights() == null) {
            return false;
        }
        for (com.airlines.go7api.responsego7.common.Flight f : changeSeatRsp.getAerocrs().getFlights().getFlight()) {
            if (isFlightSeatSuccessful(f)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFlightSeatSuccessful(com.airlines.go7api.responsego7.common.Flight flight) {
        if (flight.getSeat() == null) {
            return false;
        }
        for (com.airlines.go7api.responsego7.common.Seat s : flight.getSeat()) {
            if (Boolean.TRUE.equals(s.isStatus())) {
                return true;
            }
        }
        return false;
    }

    private ResponseEntity<Object> processConditionalPaymentForSeat(
            ChangeSeatReqDto changeSeatReqDto, boolean anySeatSuccessful, String orderId, 
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp, ObjectMapper objectMapper) throws java.io.IOException {
        
        if (anySeatSuccessful && changeSeatReqDto.getPaymentInformation() != null 
            && changeSeatReqDto.getPaymentInformation().getCardNumber() != null 
            && !changeSeatReqDto.getPaymentInformation().getCardNumber().isEmpty()) {
            
            com.airlines.go7api.requestdto.ChangePaymentReqDto.PaymentInformation payInfo = objectMapper.convertValue(
                    changeSeatReqDto.getPaymentInformation(),
                    com.airlines.go7api.requestdto.ChangePaymentReqDto.PaymentInformation.class
            );
            return processAmendmentPayment(orderId, payInfo, bookingRsp);
        }
        return null;
    }

    private java.util.Map<String, BigDecimal> fetchActualSeatPrices(
            ChangeSeatRspGo7Dto changeSeatRsp, com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp) {
        java.util.Map<String, BigDecimal> actualSeatPrices = new java.util.HashMap<>();
        if (changeSeatRsp != null && changeSeatRsp.getAerocrs() != null
                && changeSeatRsp.getAerocrs().getFlights() != null) {
            for (com.airlines.go7api.responsego7.common.Flight f : changeSeatRsp.getAerocrs().getFlights().getFlight()) {
                processFlightSeatsForActualPrices(f, bookingRsp, actualSeatPrices);
            }
        }
        return actualSeatPrices;
    }

    private void processFlightSeatsForActualPrices(
            com.airlines.go7api.responsego7.common.Flight f,
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp,
            java.util.Map<String, BigDecimal> actualSeatPrices) {
        if (f.getSeat() != null) {
            for (com.airlines.go7api.responsego7.common.Seat s : f.getSeat()) {
                if (s.getSeatNumber() != null && (s.getFare() == null || s.getFare().compareTo(BigDecimal.ZERO) <= 0)) {
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

    private java.util.Map<String, String> extractPassengerSeats(com.airlines.go7api.responsedto.ChangeSeatRspDto ndcResponse) {
        java.util.Map<String, String> passengerSeats = new java.util.HashMap<>();
        if (ndcResponse.getOrderItems() != null) {
            for (com.airlines.go7api.responsedto.common.OrderItemsDTO item : ndcResponse.getOrderItems()) {
                if (item.getServiceList() != null) {
                    for (com.airlines.go7api.responsedto.common.Service srv : item.getServiceList()) {
                        processSeatAssignment(item, srv, passengerSeats);
                    }
                }
            }
        }
        return passengerSeats;
    }

    private void processSeatAssignment(
            com.airlines.go7api.responsedto.common.OrderItemsDTO item,
            com.airlines.go7api.responsedto.common.Service srv,
            java.util.Map<String, String> passengerSeats) {
        if (srv.getServiceCode() != null && srv.getServiceCode().startsWith("SEAT")
                && item.getPassengerIds() != null && !item.getPassengerIds().isEmpty()) {
            String seatNum = srv.getServiceCode().substring(4);
            BigDecimal price = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
            passengerSeats.put(item.getPassengerIds().get(0), seatNum + "|" + price);
        }
    }

    private void updateBookingPassengerInfo(com.airlines.go7api.entity.BookingEntity entity, com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp) {
        if ((entity.getPrimaryPassengerLastName() == null || entity.getPrimaryPassengerLastName().isEmpty())
                && bookingRsp != null && bookingRsp.getAerocrs() != null && bookingRsp.getAerocrs().getBooking() != null) {
            com.airlines.go7api.responsego7.common.Passengers pList = bookingRsp.getAerocrs().getBooking().getPassengers();
            if (pList != null && pList.getPassenger() != null && !pList.getPassenger().isEmpty()) {
                entity.setPrimaryPassengerLastName(pList.getPassenger().get(0).getLastname());
            }
        }
    }

    private void cacheAssignedSeats(String orderId, com.airlines.go7api.responsedto.ChangeSeatRspDto ndcResponse, com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp) {
        if (orderId != null && ndcResponse != null) {
            java.util.Map<String, String> passengerSeats = extractPassengerSeats(ndcResponse);
            if (!passengerSeats.isEmpty()) {
                java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOpt = bookingService.getBookingByOrderId(orderId);
                if (entityOpt.isPresent()) {
                    com.airlines.go7api.entity.BookingEntity entity = entityOpt.get();

                    // Merge with existing seats if any
                    java.util.Map<String, String> existingSeats = entity.getPassengerSeats();
                    if (existingSeats == null) {
                        existingSeats = new java.util.HashMap<>();
                    }
                    existingSeats.putAll(passengerSeats);
                    entity.setPassengerSeats(existingSeats);
                    
                    updateBookingPassengerInfo(entity, bookingRsp);

                    if (ndcResponse.getTotalOrderPrice() != null) {
                        entity.setTotalOrderPrice(ndcResponse.getTotalOrderPrice());
                    }
                    bookingService.saveBooking(entity);
                    logger.info("ChangeSeat: Cached {} seat assignments to BookingEntity for OrderID: {}", passengerSeats.size(), orderId);
                }
            }
        }
    }

    @PostMapping("/change/service")
    public ResponseEntity<Object> orderService(@RequestBody ChangeServiceReqDto changeServiceReqDto) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
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
                return handleChangeServiceResponse(changeServiceRsp, orderId, bookingConfirmation, bookingRsp, changeServiceReqDto, objectMapper);
            }

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            return handleException(CHANGE_SERVICE, e);
        }
    }

    private ResponseEntity<Object> handleChangeServiceResponse(
            com.airlines.go7api.responsego7.ChangeServiceRspGo7Dto changeServiceRsp,
            String orderId, String bookingConfirmation,
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp,
            ChangeServiceReqDto changeServiceReqDto,
            ObjectMapper objectMapper) throws java.io.IOException {

        logger.info("ChangeService: changeServiceRsp is NOT null. Success: {}",
                (changeServiceRsp.getAerocrs() != null ? changeServiceRsp.getAerocrs().isSuccess() : "null"));
        // 1. Strict Error Handling
        if (changeServiceRsp.getAerocrs() != null && !changeServiceRsp.getAerocrs().isSuccess()) {
            logger.warn("ChangeService: AeroCRS reported failure, returning 400.");
            return new ResponseEntity<>(changeServiceRsp, HttpStatus.BAD_REQUEST);
        }

        // 2. Conditional Payment & Ticketing Logic
        // Determine if at least one service was successfully added
        boolean anyServiceSuccessful = isAnyServiceSuccessful(changeServiceRsp);

        ResponseEntity<Object> paymentResult = processConditionalPaymentForService(
                changeServiceReqDto, anyServiceSuccessful, orderId, bookingRsp, objectMapper);
        if (paymentResult != null) {
            return paymentResult;
        }

        // ALWAYS Refresh Booking (GetBooking) after change to get updated PNR total and balance
        bookingRsp = refreshBookingDetails(bookingConfirmation, CHANGE_SERVICE, bookingRsp);

        // --- Real-time Price Fetching (Added to Controller) ---
        java.util.Map<String, BigDecimal> actualServicePrices = fetchActualServicePrices(changeServiceRsp, bookingRsp);
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
        cacheAssignedServices(orderId, standardResponse, bookingRsp);

        return new ResponseEntity<>(standardResponse, HttpStatus.OK);
    }

    private boolean isAnyServiceSuccessful(com.airlines.go7api.responsego7.ChangeServiceRspGo7Dto changeServiceRsp) {
        if (changeServiceRsp != null && changeServiceRsp.getAerocrs() != null && changeServiceRsp.getAerocrs().getDetails() != null) {
            for (com.airlines.go7api.responsego7.common.Aerocrs.Detail d : changeServiceRsp.getAerocrs().getDetails()) {
                if (d.isSuccess()) {
                    return true;
                }
            }
        }
        return false;
    }

    private ResponseEntity<Object> processConditionalPaymentForService(
            ChangeServiceReqDto changeServiceReqDto, boolean anyServiceSuccessful, String orderId, 
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp, ObjectMapper objectMapper) throws java.io.IOException {
        
        if (anyServiceSuccessful && changeServiceReqDto.getPaymentInformation() != null 
            && changeServiceReqDto.getPaymentInformation().getCardNumber() != null 
            && !changeServiceReqDto.getPaymentInformation().getCardNumber().isEmpty()) {
            
            com.airlines.go7api.requestdto.ChangePaymentReqDto.PaymentInformation payInfo = objectMapper.convertValue(
                    changeServiceReqDto.getPaymentInformation(),
                    com.airlines.go7api.requestdto.ChangePaymentReqDto.PaymentInformation.class
            );
            return processAmendmentPayment(orderId, payInfo, bookingRsp);
        }
        return null;
    }

    private java.util.Map<String, BigDecimal> fetchActualServicePrices(
            com.airlines.go7api.responsego7.ChangeServiceRspGo7Dto changeServiceRsp, com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp) {
        java.util.Map<String, BigDecimal> actualServicePrices = new java.util.HashMap<>();
        if (changeServiceRsp != null && changeServiceRsp.getAerocrs() != null
                && changeServiceRsp.getAerocrs().getDetails() != null) {
            for (com.airlines.go7api.responsego7.common.Aerocrs.Detail detail : changeServiceRsp.getAerocrs().getDetails()) {
                processAncillaryForActualPrices(detail, bookingRsp, actualServicePrices);
            }
        }
        return actualServicePrices;
    }

    private void processAncillaryForActualPrices(
            com.airlines.go7api.responsego7.common.Aerocrs.Detail detail,
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp,
            java.util.Map<String, BigDecimal> actualServicePrices) {
        if (detail.getAncillary() != null) {
            Object itemIdObj = detail.getAncillary().get("itemid");
            Object flightIdObj = detail.getAncillary().get("flightid");
            Object priceObj = detail.getAncillary().get("totalprice");
            BigDecimal currentPrice = BigDecimal.ZERO;
            if (priceObj != null) {
                try {
                    currentPrice = new BigDecimal(priceObj.toString());
                } catch (NumberFormatException e) {
                    logger.debug("Failed to parse price for ancillary item: {}", priceObj);
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

    private java.util.Map<String, String> extractPassengerServices(com.airlines.go7api.responsedto.ChangeServiceRspDto standardResponse) {
        java.util.Map<String, String> passengerServices = new java.util.HashMap<>();
        if (standardResponse.getOrderItems() != null) {
            for (com.airlines.go7api.responsedto.common.OrderItemsDTO item : standardResponse.getOrderItems()) {
                if (item.getOrderItemId() != null && item.getOrderItemId().contains("_SRV")
                        && item.getServiceList() != null) {
                    for (com.airlines.go7api.responsedto.common.Service srv : item.getServiceList()) {
                        processServiceAssignment(item, srv, passengerServices);
                    }
                }
            }
        }
        return passengerServices;
    }

    private void processServiceAssignment(
            com.airlines.go7api.responsedto.common.OrderItemsDTO item,
            com.airlines.go7api.responsedto.common.Service srv,
            java.util.Map<String, String> passengerServices) {
        if (srv.getServiceCode() != null && !srv.getServiceCode().startsWith("SEAT")
                && item.getPassengerIds() != null && !item.getPassengerIds().isEmpty()) {
            BigDecimal price = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
            passengerServices.put(item.getPassengerIds().get(0), srv.getServiceCode() + "|" + price);
        }
    }

    private void cacheAssignedServices(String orderId, com.airlines.go7api.responsedto.ChangeServiceRspDto standardResponse, com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp) {
        if (orderId != null && standardResponse != null) {
            java.util.Map<String, String> passengerServices = extractPassengerServices(standardResponse);
            if (!passengerServices.isEmpty()) {
                java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOpt = bookingService.getBookingByOrderId(orderId);
                if (entityOpt.isPresent()) {
                    com.airlines.go7api.entity.BookingEntity entity = entityOpt.get();

                    // Merge with existing services if any
                    java.util.Map<String, String> existingServices = entity.getPassengerServices();
                    if (existingServices == null) {
                        existingServices = new java.util.HashMap<>();
                    }
                    existingServices.putAll(passengerServices);

                    entity.setPassengerServices(existingServices);
                    
                    updateBookingPassengerInfo(entity, bookingRsp);

                    if (standardResponse.getTotalOrderPrice() != null) {
                        entity.setTotalOrderPrice(standardResponse.getTotalOrderPrice());
                    }
                    bookingService.saveBooking(entity);
                    logger.info("ChangeService: Cached {} service assignments to BookingEntity for OrderID: {}", passengerServices.size(), orderId);
                }
            }
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
                        logger.info("OrderChange: Found real BookingConfirmation in DB: {}", realConfirmation);
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
            return handleException("OrderChange", e);
        }
    }

    private String extractOrderIdForReshop(JsonNode root) {
        if (root.has("orderId")) {
            return root.get("orderId").asText();
        } else if (root.has(KEY_AEROCRS) && root.get(KEY_AEROCRS).has(KEY_PARMS)) {
            JsonNode parms = root.get(KEY_AEROCRS).get(KEY_PARMS);
            if (parms.has(KEY_BOOKING_CONFIRMATION)) {
                return parms.get(KEY_BOOKING_CONFIRMATION).asText();
            } else if (parms.has(KEY_BOOKING_ID)) {
                return parms.get(KEY_BOOKING_ID).asText();
            }
        }
        return null;
    }

    private void fetchPaxCountsFromBooking(String orderId, int[] counts) throws java.io.IOException {
        String bookingConfirmation = orderId;
        java.util.Optional<com.airlines.go7api.entity.BookingEntity> entityOpt = bookingService
                .getBookingByOrderId(orderId);
        if (entityOpt.isPresent()) {
            bookingConfirmation = entityOpt.get().getBookingConfirmation();
        }
        com.airlines.go7api.request.OrderRetrieveReq getBookingReq = SeatAvailabilityReq
                .mapToGetBookingReq(bookingConfirmation);
        Object getBookingResponse = getBookingReq.unmarshal();
        if (getBookingResponse != null && !(getBookingResponse instanceof com.airlines.go7api.error.ErrorRsp)) {
            com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto bookingRsp = deserializeResponse(
                    getBookingResponse,
                    com.airlines.go7api.responsego7.OrderRetrieveRspGo7Dto.class
            );
            if (bookingRsp != null && bookingRsp.getAerocrs() != null && bookingRsp.getAerocrs().getBooking() != null) {
                counts[0] = bookingRsp.getAerocrs().getBooking().getAdults();
                counts[1] = bookingRsp.getAerocrs().getBooking().getChild();
                counts[2] = bookingRsp.getAerocrs().getBooking().getInfant();
            }
        }
    }

    private int[] determinePaxCounts(JsonNode root, String orderId) throws java.io.IOException {
        int[] counts = new int[3]; // [adults, children, infants]
        JsonNode paxList = root.path("paxList");
        if (paxList.isArray() && paxList.size() > 0) {
            for (JsonNode pax : paxList) {
                String ptc = pax.path("ptc").asText("");
                if ("ADT".equalsIgnoreCase(ptc)) {
                    counts[0]++;
                } else if ("CHD".equalsIgnoreCase(ptc)) {
                    counts[1]++;
                } else if ("INF".equalsIgnoreCase(ptc)) {
                    counts[2]++;
                }
            }
        } else if (orderId != null) {
            fetchPaxCountsFromBooking(orderId, counts);
        }
        return counts;
    }

    private java.util.List<AirshopReqDto.OD> parseOdsForReshop(JsonNode root) {
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
        return ods;
    }

    @PostMapping("/orderreshop")
    public ResponseEntity<Object> orderReshop(@RequestBody String requestBody) {
        try {
            JsonNode root = mapper.readTree(requestBody);
            String orderId = extractOrderIdForReshop(root);

            // 1. Determine Passenger Counts
            int[] counts = determinePaxCounts(root, orderId);

            // 2. Prepare Airshop Request
            AirshopReqDto airshopReqDto = new AirshopReqDto();
            java.util.List<AirshopReqDto.OD> ods = parseOdsForReshop(root);

            airshopReqDto.setOds(ods);
            airshopReqDto.setAdults(counts[0] > 0 ? counts[0] : 1);
            airshopReqDto.setChildren(counts[1]);
            airshopReqDto.setInfants(counts[2]);
            airshopReqDto.setTripType(ods.size() > 1 ? "return" : "oneway");
            if (ods.size() > 1) {
                airshopReqDto.setReturnDate(ods.get(1).getDate());
            }

            // 3. Execute Internal Airshop Search
            logger.info("DEBUG: Executing internal Airshop search for OrderReshop. OrderId: {}", orderId);
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

        } catch (Exception e) {
            return handleException("OrderReshop", e);
        }
    }

    private int[] countPaxFromList(JsonNode paxList) {
        int[] counts = new int[3]; // [adults, children, infants]
        if (paxList.isArray()) {
            for (JsonNode pax : paxList) {
                String ptc = pax.path("ptc").asText("");
                if ("ADT".equalsIgnoreCase(ptc)) {
                    counts[0]++;
                } else if ("CHD".equalsIgnoreCase(ptc)) {
                    counts[1]++;
                } else if ("INF".equalsIgnoreCase(ptc)) {
                    counts[2]++;
                }
            }
        }
        return counts;
    }

    @PostMapping("/FSorderreshop")
    public ResponseEntity<Object> fsOrderReshop(@RequestBody String requestBody) {
        try {
            JsonNode root = mapper.readTree(requestBody);
            AirshopReqDto airshopReqDto = new AirshopReqDto();
            
            // Populate ods
            java.util.List<AirshopReqDto.OD> ods = parseOdsForReshop(root);
            airshopReqDto.setOds(ods);
            
            // Populate passengers
            int[] counts = countPaxFromList(root.path("paxList"));
            airshopReqDto.setAdults(counts[0] > 0 ? counts[0] : 1);
            airshopReqDto.setChildren(counts[1]);
            airshopReqDto.setInfants(counts[2]);
            airshopReqDto.setTripType(ods.size() > 1 ? "return" : "oneway");
            if (ods.size() > 1) {
                airshopReqDto.setReturnDate(ods.get(1).getDate());
            }

            // Execute Airshop logic
            logger.info("DEBUG: Executing Airshop search for FSOrderReshop");
            AirshopReq flightSearchRequestDTO = AirshopReq.mapToFlightSearchRequestDTO(airshopReqDto);
            AirshopRspGo7Dto response = flightSearchRequestDTO.unmarshal();

            if (response != null && response.getAerocrs() != null && response.getAerocrs().isSuccess()) {
                com.airlines.go7api.responsedto.OrderReshopRspDto reshopRspDto = orderReshopResponse
                        .orderReshopMapper(response, airshopReqDto);
                return new ResponseEntity<>(reshopRspDto, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e) {
            return handleException("FSOrderReshop", e);
        }
    }

    private BigDecimal findSeatFareInMap(
            com.airlines.go7api.responsego7.SeatAvailabilityRspGo7Dto go7Rsp,
            String classCode,
            String seatNumber) {
        if (go7Rsp == null || go7Rsp.getAerocrs() == null || go7Rsp.getAerocrs().getSeatMapFare() == null) {
            return null;
        }

        java.util.Map<String, com.airlines.go7api.responsego7.SeatAvailabilityRspGo7Dto.SeatClass> classes = go7Rsp
                .getAerocrs().getSeatMapFare().getClasses();
        if (classes == null) {
            return null;
        }

        String cleanClass = (classCode != null && classCode.contains("/")) ? classCode.split("/")[0] : classCode;
        if (!classes.containsKey(cleanClass)) {
            return null;
        }

        java.util.List<com.airlines.go7api.responsego7.SeatAvailabilityRspGo7Dto.PaidSeatRow> rows = classes
                .get(cleanClass).getPaidSeats();
        if (rows == null) {
            return null;
        }

        for (com.airlines.go7api.responsego7.SeatAvailabilityRspGo7Dto.PaidSeatRow row : rows) {
            if (row.getSeats() != null && row.getSeats().containsKey(seatNumber) && row.getSeatFare() != null) {
                return BigDecimal.valueOf(row.getSeatFare());
            }
        }
        return null;
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
                return findSeatFareInMap(go7Rsp, classCode, seatNumber);
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Error fetching actual seat fare for seat {}: {}", seatNumber, e.getMessage());
            }
        }
        return null;
    }

    private BigDecimal findServiceFareInAncillaries(
            com.airlines.go7api.responsego7.ServiceListRspGo7Dto go7Rsp,
            String itemId) {
        if (go7Rsp == null || go7Rsp.getAerocrs() == null || go7Rsp.getAerocrs().getAncillaries() == null) {
            return null;
        }

        java.util.List<com.airlines.go7api.responsego7.ServiceListRspGo7Dto.Ancillary> ancillaries = go7Rsp
                .getAerocrs().getAncillaries().getAncillary();
        if (ancillaries == null) {
            return null;
        }

        for (com.airlines.go7api.responsego7.ServiceListRspGo7Dto.Ancillary anc : ancillaries) {
            if (anc.getItems() != null) {
                for (com.airlines.go7api.responsego7.ServiceListRspGo7Dto.Item item : anc.getItems()) {
                    if (itemId.equals(item.getItemid()) && item.getFare() != null && !item.getFare().isEmpty()) {
                        return new BigDecimal(item.getFare());
                    }
                }
            }
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
                return findServiceFareInAncillaries(go7Rsp, itemId);
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Error fetching actual service fare for item {}: {}", itemId, e.getMessage());
            }
        }
        return null;
    }

    private void preFetchCombinedAmount(
            ChangePaymentReqDto changePaymentReqDto,
            java.util.Map<String, String> passengerSeats,
            java.util.Map<String, String> passengerServices) throws java.io.IOException {
        if (changePaymentReqDto.getOrderId() == null || changePaymentReqDto.getOrderId().isEmpty()) {
            return;
        }

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

        if (preResponse instanceof com.airlines.go7api.error.ErrorRsp) {
            return;
        }

        com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto preBooking = deserializeResponse(preResponse, com.airlines.go7api.responsego7.ChangePaymentRspGo7Dto.class);
        if (preBooking == null || preBooking.getAerocrs() == null || preBooking.getAerocrs().getBooking() == null) {
            return;
        }

        com.airlines.go7api.responsego7.common.BalanceInformation balance = preBooking
                .getAerocrs().getBooking().getBalanceInformation();
        
        updatePaymentAmountIfOutstanding(changePaymentReqDto, balance, passengerSeats, passengerServices);
    }

    private void updatePaymentAmountIfOutstanding(
            ChangePaymentReqDto changePaymentReqDto,
            com.airlines.go7api.responsego7.common.BalanceInformation balance,
            java.util.Map<String, String> passengerSeats,
            java.util.Map<String, String> passengerServices) {
        if (balance == null || balance.getPnrOutstandingPayment() <= 0) {
            return;
        }

        if (logger.isInfoEnabled()) {
            logger.info("ChangePayment: Found outstanding balance: {}", balance.getPnrOutstandingPayment());
        }

        // If seat or service flow is included (cached maps present), combine payment
        boolean hasSeats = passengerSeats != null && !passengerSeats.isEmpty();
        boolean hasServices = passengerServices != null && !passengerServices.isEmpty();
        if ((hasSeats || hasServices) && changePaymentReqDto.getPaymentInformation() != null) {
            changePaymentReqDto.getPaymentInformation()
                    .setAmount(BigDecimal.valueOf(balance.getPnrOutstandingPayment()));
            if (logger.isInfoEnabled()) {
                logger.info("ChangePayment: Combined payment amount updated with outstanding balance: {}",
                        balance.getPnrOutstandingPayment());
            }
        }
    }
}
