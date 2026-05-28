package com.airlines.go7api.responsego7;

import com.airlines.go7api.responsego7.common.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)

public class UnpaidCancelRspGo7Dto {

    private Aerocrs aerocrs;

    private boolean success;
    private Details details;

    

    

    

    

    

    

    

    

    

    

    

    
}