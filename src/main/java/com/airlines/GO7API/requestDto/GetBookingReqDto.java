package com.airlines.GO7API.requestDto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetBookingReqDto {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Aerocrs {
        @JsonProperty("parms")
        private Parms parms;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parms {
        @JsonProperty("bookingconfirmation")
        private String bookingconfirmation;
    }
}
