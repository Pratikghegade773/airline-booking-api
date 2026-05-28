package com.airlines.go7api.responsedto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseOrderResponseDTO {
    private List<String> warnings;
    private String responseId;
    private String pnr;
    private String apiOwner;
    private String orderId;
    private String validatingCarrier;
    private BigDecimal totalOrderPrice;
    private String currency;
    private String paymentTimeLimit;
    private List<OD> ods;
    private String statusCode;
    private List<String> remarks;
    private List<BookingReferences> bookingReferences;
    private List<PaxDetailDTO> paxDetailList;
    private List<SpecialServiceRequest> specialServiceRequests;
    private List<PriceClass> priceClassList;
    private List<TicketDocInfoDTO> ticketDocInfoList;
    private List<EMDInfoDTO> emdInfoList;
    private List<PaymentsDTO> payments;
    private List<OrderItemsDTO> orderItems;
}
