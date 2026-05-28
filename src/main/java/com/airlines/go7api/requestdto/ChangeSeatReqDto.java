package com.airlines.go7api.requestdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.airlines.go7api.requestdto.common.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangeSeatReqDto {

    /// Config Variables
    private String subscriptionKey;
    private String apiUrl;
    private String agencyId;
    private String agentId;
    private String apiKey;
    private String changeAncillariesUrl;
    private String agencyName;


    private String owner;
    private String responseId;
    private String orderId;
    private List<ChangeOfferReqDto> offers;
    private List<ChangeOrderItemReqDto> deleteOrderItems;
    private PaymentInformationReqDto paymentInformation;


    


    


    private String paymentType;

    

    private List<PaxReqDto> passengers;

    
}