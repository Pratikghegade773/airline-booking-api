package com.airlines.go7api.responsedto;

import com.airlines.go7api.responsedto.common.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@lombok.EqualsAndHashCode(callSuper = true)

@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChangePaymentRspDto extends BaseOrderResponseDTO {
    private String agentId;
    private String agencyName;
    private String agencyId;
    private String rbd;
    private String exchangeRate;
    private BigDecimal totalOldPrice;
    private List<OtherServiceInformation> otherServiceInformation;
    private String ticketingTimeLimit;
    private String ticketedByTimeLimit;











}
