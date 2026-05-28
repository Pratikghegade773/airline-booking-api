package com.airlines.go7api.responsego7.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceInformation {
    @JsonProperty("PNRoutstandingpayment")
    private double pnrOutstandingPayment;
    @JsonProperty("PNRtotal")
    private BigDecimal pnrTotal;
}