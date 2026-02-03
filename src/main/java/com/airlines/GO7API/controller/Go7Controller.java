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
            if (go7Response != null && go7Response.getAerocrs() != null && go7Response.getAerocrs().isSuccess()) {
                Long bookingId = go7Response.getAerocrs().getBooking() != null
                        ? go7Response.getAerocrs().getBooking().getBookingid()
                        : null;

                if (bookingId != null && bookingId > 0) {
                    System.out.println("Booking ID extracted: " + bookingId + ". Proceeding to OrderConfirm.");

                    OrderConfirmReq orderConfirmReq = OrderConfirmReq.mapToOrderConfirmReq(orderCreateReqDto,
                            bookingId);
                    Object confirmResponse = orderConfirmReq.unmarshal();
                    System.out.println("OrderConfirm executed. Response: " + confirmResponse);

                    if (confirmResponse instanceof com.airlines.GO7API.error.ErrorRsp) {
                        return new ResponseEntity<>(confirmResponse, HttpStatus.BAD_REQUEST);
                    }

                    // Extract bookingconfirmation from OrderConfirm response
                    String bookingConfirmation = null;
                    try {
                        // confirmResponse is likely a LinkedHashMap
                        java.util.LinkedHashMap aerocrsMap = (java.util.LinkedHashMap) ((java.util.LinkedHashMap) confirmResponse)
                                .get("aerocrs");
                        if (aerocrsMap != null) {
                            // Check directly in 'aerocrs' object first (as per latest log)
                            if (aerocrsMap.containsKey("bookingconfirmation")) {
                                bookingConfirmation = (String) aerocrsMap.get("bookingconfirmation");
                            }
                            // Fallback: Check for 'booking' object (as per previous assumption)
                            else if (aerocrsMap.containsKey("booking")) {
                                Object bookingObj = aerocrsMap.get("booking");
                                if (bookingObj instanceof java.util.LinkedHashMap) {
                                    bookingConfirmation = (String) ((java.util.LinkedHashMap) bookingObj)
                                            .get("bookingconfirmation");
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println(
                                "Failed to extract bookingconfirmation from OrderConfirm response: " + e.getMessage());
                    }

                    if (bookingConfirmation != null) {
                        // Check payment first
                        if (orderCreateReqDto.getPaymentInformation() != null) {
                            MakePaymentReq makePaymentReq = MakePaymentReq.mapToMakePaymentReq(orderCreateReqDto,
                                    bookingId);
                            Object paymentResponse = makePaymentReq.unmarshal();
                            System.out.println("MakePayment executed. Response: " + paymentResponse);

                            if (paymentResponse instanceof com.airlines.GO7API.error.ErrorRsp) {
                                return new ResponseEntity<>(paymentResponse, HttpStatus.BAD_REQUEST);
                            }

                            OrderTicketReq orderTicketReq = OrderTicketReq.mapFromBookingId(bookingId);
                            Object ticketResponse = orderTicketReq.unmarshal();
                            System.out.println("OrderTicket executed. Response: " + ticketResponse);

                            if (ticketResponse instanceof com.airlines.GO7API.error.ErrorRsp) {
                                return new ResponseEntity<>(ticketResponse, HttpStatus.BAD_REQUEST);
                            }
                        }

                        // CALL GET BOOKING (OrderRetrieve)
                        System.out.println("Booking Confirmation: " + bookingConfirmation + ". Calling GetBooking.");
                        GetBookingReqDto getBookingReqDto = new GetBookingReqDto();
                        GetBookingReqDto.Aerocrs aerocrsDto = new GetBookingReqDto.Aerocrs();
                        GetBookingReqDto.Parms parmsDto = new GetBookingReqDto.Parms();
                        parmsDto.setBookingconfirmation(bookingConfirmation);
                        aerocrsDto.setParms(parmsDto);
                        getBookingReqDto.setAerocrs(aerocrsDto);

                        GetBookingReq getBookingReq = GetBookingReq.mapToGetBookingReq(getBookingReqDto);
                        Object finalResponse = getBookingReq.unmarshal();

                        if (finalResponse instanceof com.airlines.GO7API.error.ErrorRsp) {
                            return new ResponseEntity<>(finalResponse, HttpStatus.BAD_REQUEST);
                        }
                        // Deserialize GetBooking response to Go7 DTO
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

                        return new ResponseEntity<>(ndcResponse, HttpStatus.OK);
                    } else {
                        System.out.println("No Booking Confirmation found in OrderConfirm response.");
                    }
                }
            } else {
                System.out.println("OrderCreate failed or no booking ID. Skipping OrderConfirm.");
            }

            // 4. Generate NDC Response
            com.airlines.GO7API.responseDto.OrderCreateRspDto ndcResponse = com.airlines.GO7API.response.OrderCreateResponse
                    .generateResponse(go7Response, orderCreateReqDto);

            return new ResponseEntity<>(ndcResponse, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during OrderCreate request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/orderticket")
    public ResponseEntity<Object> orderTicket(@RequestBody OrderTicketReqDto orderTicketReqDto) {
        try {
            // Map the DTO to the internal Request object
            OrderTicketReq orderTicketReq = OrderTicketReq.mapToOrderTicketRequestDTO(orderTicketReqDto);

            // Execute the API call and return the raw response
            Object response = orderTicketReq.unmarshal();

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during OrderTicket request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/orderretrieve")
    public ResponseEntity<Object> orderRetrieve(@RequestBody OrderRetrieveReqDto orderRetrieveReqDto) {
        try {
            // Map the DTO to the internal Request object
            OrderRetrieveReq orderRetrieveReq = OrderRetrieveReq.mapToOrderRetrieveReq(orderRetrieveReqDto);

            // Execute the API call and return the raw response
            Object response = orderRetrieveReq.unmarshal();

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during OrderRetrieve request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/ordercancel")
    public ResponseEntity<Object> orderCancel(@RequestBody OrderCancelReqDto orderCancelReqDto) {
        try {
            // Map the DTO to the internal Request object
            OrderCancelReq orderCancelReq = OrderCancelReq.mapToOrderCancelReq(orderCancelReqDto);

            // Execute the API call and return the raw response
            Object response = orderCancelReq.unmarshal();

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during OrderCancel request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/servicelist")
    public ResponseEntity<Object> serviceList(@RequestBody ServiceListReqDto serviceListReqDto) {
        try {
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
    public ResponseEntity<Object> seatAvailability(@RequestBody SeatAvailabilityReqDto seatAvailabilityReqDto) {
        try {
            // Map the DTO to the internal Request object
            SeatAvailabilityReq seatAvailabilityReq = SeatAvailabilityReq
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
