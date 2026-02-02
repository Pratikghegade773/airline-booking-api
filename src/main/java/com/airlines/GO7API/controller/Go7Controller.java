package com.airlines.GO7API.controller;

import com.airlines.GO7API.request.AirshopReq;
import com.airlines.GO7API.request.OfferPriceReq;
import com.airlines.GO7API.requestDto.AirshopReqDto;
import com.airlines.GO7API.requestDto.OfferPriceReqDto;
import com.airlines.GO7API.request.OrderCreateReq;
import com.airlines.GO7API.requestDto.OrderCreateReqDto;
import com.airlines.GO7API.request.OrderConfirmReq;
import com.airlines.GO7API.requestDto.OrderConfirmReqDto;
import com.airlines.GO7API.request.OrderTicketReq;
import com.airlines.GO7API.requestDto.OrderTicketReqDto;
import com.airlines.GO7API.request.OrderRetrieveReq;
import com.airlines.GO7API.requestDto.OrderRetrieveReqDto;
import com.airlines.GO7API.request.OrderCancelReq;
import com.airlines.GO7API.requestDto.OrderCancelReqDto;
import com.airlines.GO7API.request.ServiceListReq;
import com.airlines.GO7API.requestDto.ServiceListReqDto;
import com.airlines.GO7API.request.SeatAvailabilityReq;
import com.airlines.GO7API.requestDto.SeatAvailabilityReqDto;
import com.airlines.GO7API.request.OrderChangeReq;
import com.airlines.GO7API.requestDto.OrderChangeReqDto;
import com.airlines.GO7API.request.OrderReshopReq;
import com.airlines.GO7API.requestDto.OrderReshopReqDto;
import com.airlines.GO7API.response.AirshopResponse;
import com.airlines.GO7API.response.OfferPriceResponse;
import com.airlines.GO7API.responseDto.AirshopRspDto;
import com.airlines.GO7API.responseGo7.AirshopRspGo7Dto;
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
            // Map the DTO to the internal Request object
            OrderCreateReq orderCreateReq = OrderCreateReq.mapToFlightSearchRequestDTO(orderCreateReqDto);

            // Execute the API call and return the raw response
            Object response = orderCreateReq.unmarshal();

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during OrderCreate request: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/orderconfirm")
    public ResponseEntity<Object> orderConfirm(@RequestBody OrderConfirmReqDto orderConfirmReqDto) {
        try {
            // Map the DTO to the internal Request object
            OrderConfirmReq orderConfirmReq = OrderConfirmReq.mapToOrderConfirmRequestDTO(orderConfirmReqDto);

            // Execute the API call and return the raw response
            Object response = orderConfirmReq.unmarshal();

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred during OrderConfirm request: " + e.getMessage(),
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
