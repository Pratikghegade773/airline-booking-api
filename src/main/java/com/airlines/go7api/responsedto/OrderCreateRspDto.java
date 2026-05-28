package com.airlines.go7api.responsedto;

import com.airlines.go7api.responsedto.common.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@lombok.EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderCreateRspDto extends BaseOrderResponseDTO {
    private String ticketingTimeLimit;
}
