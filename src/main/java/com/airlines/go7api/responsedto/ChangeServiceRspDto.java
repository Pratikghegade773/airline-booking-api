package com.airlines.go7api.responsedto;

import com.airlines.go7api.responsedto.common.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@lombok.EqualsAndHashCode(callSuper = true)

@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChangeServiceRspDto extends BaseOrderResponseDTO {
    private String rbd;
    private List<OtherServiceInformation> otherServiceInformation;
    private String ticketingTimeLimit;
    private String ticketedByTimeLimit;












}