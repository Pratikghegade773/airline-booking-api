package com.airlines.go7api.responsego7.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class Passenger {
    private Long rph;
    private String paxtitle;
    private String lastname;
    private String firstname;
    private String paxtype;
    private String nationality;
    private String passportexpiry;
    private String paxdoctype;
    private String paxpassportnum;
    private String paxpassportcountry;
    @com.fasterxml.jackson.annotation.JsonAlias({ "Gender", "sex", "gender" })
    private String gender;
    private String dob;
    private String email;
    private String contact;
    @com.fasterxml.jackson.annotation.JsonFormat(with = com.fasterxml.jackson.annotation.JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<Checkin> checkin;
    @JsonProperty("e-tickets")
    private ETickets eTickets;
    // older fields if needed compatibility
    private int paxnum;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    public static class ETickets {
        private List<ETicketFlight> flight;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    public static class ETicketFlight {
        private String number;
        private String eticketnumber;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class Checkin {
        @JsonProperty("flight")
        private String flight;
        @JsonProperty("seat")
        private String seat;
        @JsonProperty("status")
        private String status;
    }

}