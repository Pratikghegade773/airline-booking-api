package com.airlines.go7api.responsedto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // This will exclude null fields
public class SpecialServiceRequest {
    private List<String> travelIdRef;
    private String segmentRef;
    private String ssrCode;
    private String text;
    private String actionCode;
}
