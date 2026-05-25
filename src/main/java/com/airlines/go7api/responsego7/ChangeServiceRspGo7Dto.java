package com.airlines.go7api.responsego7;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeServiceRspGo7Dto {

    private Aerocrs aerocrs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Aerocrs {
        private boolean success;
        private Long bookingid;
        private String companycode;
        private String status;
        private String message;
        private java.util.List<Detail> details;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Detail {
            private java.util.Map<String, Object> ancillary;
            private boolean success;
            private int invid;
        }
    }
}
