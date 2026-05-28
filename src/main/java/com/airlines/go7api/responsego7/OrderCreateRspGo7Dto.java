package com.airlines.go7api.responsego7;

import com.airlines.go7api.responsego7.common.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)

public class OrderCreateRspGo7Dto {

    private Aerocrs aerocrs;

    private boolean success;
    private Details details;

    

    

    

    

    

    

    

    

    

    

    

    
}