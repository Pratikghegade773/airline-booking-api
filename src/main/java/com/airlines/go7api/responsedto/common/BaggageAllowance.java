package com.airlines.go7api.responsedto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaggageAllowance {
    private String baggageAllowanceId;
    private String ptc;
    private String passengerId;
    private String category;
    private String quantity;
    private String name;
    private List<Weight> weight;
    private List<Dimension> dimensions;
    private List<DescriptionDTO> descriptions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DescriptionDTO {
        private String description;
        private String text;
        private String label;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Weight {
        private String value;
        private String unit;
        private String uom;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Dimension {
        private String unit;
        private String category;
        private String value;
        private String uom;
    }
}