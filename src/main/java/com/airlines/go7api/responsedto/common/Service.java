package com.airlines.go7api.responsedto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Service {
    private String serviceId;
    private String serviceStatus;
    private String serviceCode;
    private String serviceName;
    private String passengerRef;
    private String segmentRef;
    private String arrival;
    private String departure;
    private String serviceType;
    private List<String> description;
    private String segmentId;
    private String odKey;
    private ServiceDefinationRef serviceDefinitionRef;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ServiceDefinationRef {
        private String serviceDefId;
        private String name;
        private Encoding encoding;
        private java.util.List<Description> descriptions;

        @Data
        public static class Encoding {
            private String rfic;
            private String type;
            private String code;
            private String subCode;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Description {
            private String text;
            private String type;
        }
    }
    private String column;
    private BigInteger row;
    private List<SeatCharacteristic> seatCharacteristics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SeatCharacteristic {
        private String code;
        private String description;
    }
}
