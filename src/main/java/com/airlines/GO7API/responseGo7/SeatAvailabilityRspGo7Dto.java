package com.airlines.GO7API.responseGo7;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatAvailabilityRspGo7Dto {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Aerocrs {
        @JsonProperty("success")
        private boolean success;

        @JsonProperty("seatmapfare")
        private SeatMapFare seatMapFare;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatMapFare {
        @JsonProperty("actype")
        private String actype;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("classes")
        private Map<String, SeatClass> classes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatClass {
        @JsonProperty("cabinclass")
        private String cabinClass;

        @JsonProperty("paidSeats")
        private List<PaidSeatRow> paidSeats; // List of seat rows
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaidSeatRow {
        @JsonProperty("rownumber")
        private Integer rowNumber;

        @JsonProperty("brandname")
        private String brandName;

        @JsonProperty("seatfare")
        private Double seatFare;

        @JsonProperty("seatficonid")
        private Integer seatIconId;

        @JsonProperty("seats")
        private Map<String, String> seats;
    }
}
