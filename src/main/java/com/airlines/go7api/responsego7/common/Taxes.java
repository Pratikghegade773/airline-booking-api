package com.airlines.go7api.responsego7.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class Taxes {
    @JsonProperty("Ground_handling")
    private double groundHandling;
    @JsonProperty("Security")
    private double security;
    @JsonProperty("Fuel")
    private double fuel;
    @JsonProperty("tax_4")
    private double tax4;
    @JsonProperty("tax_1")
    private double tax1; // Common placeholder
}