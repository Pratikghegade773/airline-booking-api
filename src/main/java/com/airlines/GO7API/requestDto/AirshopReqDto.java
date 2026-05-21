package com.airlines.go7api.requestdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AirshopReqDto {

    private String apiKey;
    private String airshopurl;
    private String agencyId;
    private String agentId;
    private String agencyName;


    private String tripType;
    private List<OD> ods;
    private int adults;
    private int children;
    private int infants;
    private String returnDate;
    private String flexibility;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OD {
        private String origin;
        private String destination;
        private String date;
        private String cabinPreference;
        private String preferenceLevel;
    }

}