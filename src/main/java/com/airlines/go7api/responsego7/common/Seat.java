package com.airlines.go7api.responsego7.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class Seat {
    private boolean status;
    @com.fasterxml.jackson.annotation.JsonProperty("seat")
    private String seatNumber;
    private Object msg;
    private BigDecimal fare;
    private String currency;
}