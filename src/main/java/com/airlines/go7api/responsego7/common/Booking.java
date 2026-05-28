package com.airlines.go7api.responsego7.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class Booking {
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.airlines.go7api.util.EmptyArrayToEmptyObjectDeserializer.class)
    private Items items;
    private int adults;
    private int child;
    private int infant;
    private String totalprice;
    private String defaultCurrency;
    private String totalpriceinCurrency;
    private String currency;
    private Long bookingid;
    private String pnrref;
    private String pnrptl;
    private String pnrttl;
    private String bookingconfirmation;
    private String invoicenumber;
    private String ticketnumber;
    private String status;
    private String source;
    private String linktoticket;
    private String linktobooking;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.airlines.go7api.util.EmptyArrayToEmptyObjectDeserializer.class)
    private Flights flights;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.airlines.go7api.util.EmptyArrayToEmptyObjectDeserializer.class)
    private Passengers passengers;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.airlines.go7api.util.EmptyArrayToEmptyObjectDeserializer.class)
    private Remarks remarks;
    private List<Object> vouchers;
    @JsonProperty("Balanceinformation")
    private BalanceInformation balanceInformation;
    private Object currencies; // Map<String, Object>
    private Object balanceInCurrencies; // Map<String, Map<String, Double>>
    private List<Object> receipts;
}