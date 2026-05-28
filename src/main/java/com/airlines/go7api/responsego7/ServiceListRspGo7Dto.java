package com.airlines.go7api.responsego7;

import com.airlines.go7api.responsego7.common.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)

public class ServiceListRspGo7Dto {

    @JsonProperty("aerocrs")
    private Aerocrs aerocrs;

    

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ancillaries {
        private List<Ancillary> ancillary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ancillary {
        private String name;
        private String description;
        private String image;
        private String ssrcode;
        private int ancibeorder;
        private String icon;
        private String groupname;
        private List<Item> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String itemid;
        private String itemname;
        private String itemimage;
        private String quantityforitem;
        private String fare;
        private String tax1;
        private String tax2;
        private String tax3;
        private String tax4;
        private String vat;
        private boolean vatontax;
        private String currency;
    }
}
