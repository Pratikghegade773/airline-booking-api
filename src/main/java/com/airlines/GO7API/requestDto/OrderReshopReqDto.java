package com.airlines.GO7API.requestDto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderReshopReqDto {
    private String airlineCode;
    private String orderId;
    private String agencyId;
    private String agentId;
    private String apiKey;
    private String reshopUrl;
    private String agencyName;
    private List<OD> ods;
    private List<String> offerItems;
    private List<DeleteOrderItem> deleteOrderItems;
    private List<Passengers> paxList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteOrderItem {
        private String orderItemId;
        private List<String> serviceRetainIds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OD {

        private String origin;
        private String destination;
        private String cabinPreference;
        private String preferenceLevel;
        private String date;
    }

    @Data
    public static class Passengers {
        private String paxId;
        private String ptc;
        private String birthDate;
        private Individual individual;

        @Data
        public static class Individual {
            private String givenName;
            private String surname;
            private String nameTitle;
            private String gender;
        }

    }
}

