package com.airlines.GO7API.request;

import com.airlines.GO7API.requestDto.AirshopReqDto;
import com.airlines.GO7API.responseDto.AirshopRspDto;
import com.airlines.GO7API.error.ErrorRsp;
import com.airlines.GO7API.responseGo7.AirshopRspGo7Dto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AirshopReq {
    @JsonProperty("journeys")
    private List<Journey> journeys;

    @JsonProperty("exactMatch")
    private Boolean exactMatch;

    @JsonProperty("nonStopFlight")
    private Boolean nonStopFlight;

    @JsonProperty("corporateAccount")
    private List<CorporateAccount> corporateAccount;

    @JsonProperty("cabinType")
    private String cabinType;

    @JsonProperty("flexibility")
    private String flexibility;

    @JsonProperty("fareType")
    private String fareType;

    @JsonProperty("passengers")
    private List<Passenger> passengers;

    @JsonProperty("miniFareRule")
    private Boolean miniFareRule;

    @JsonProperty("splitOffer")
    private Boolean splitOffer;

    @JsonProperty("originCurrency")
    private Boolean originCurrency;

    // Availability/Fares params
    @JsonProperty("fltnum")
    private String fltnum;

    @JsonProperty("chargetype")
    private String chargetype;

    @JsonProperty("currency")
    private String currency;

    // Schedule params
    @JsonProperty("fltsFROMperiod")
    private String fltsFROMperiod;

    @JsonProperty("fltsTOperiod")
    private String fltsTOperiod;

    @JsonProperty("codeformat")
    private String codeformat;

    @JsonProperty("companycode")
    private String companycode;

    @JsonProperty("soldonline")
    private Boolean soldonline;

    @JsonProperty("ssim")
    private Boolean ssim;

    // Getters and Setters for AirshopReq
    public List<Journey> getJourneys() {
        return journeys;
    }

    public void setJourneys(List<Journey> journeys) {
        this.journeys = journeys;
    }

    public Boolean getExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(Boolean exactMatch) {
        this.exactMatch = exactMatch;
    }

    public Boolean getNonStopFlight() {
        return nonStopFlight;
    }

    public void setNonStopFlight(Boolean nonStopFlight) {
        this.nonStopFlight = nonStopFlight;
    }

    public List<CorporateAccount> getCorporateAccount() {
        return corporateAccount;
    }

    public void setCorporateAccount(List<CorporateAccount> corporateAccount) {
        this.corporateAccount = corporateAccount;
    }

    public String getCabinType() {
        return cabinType;
    }

    public void setCabinType(String cabinType) {
        this.cabinType = cabinType;
    }

    public String getFlexibility() {
        return flexibility;
    }

    public void setFlexibility(String flexibility) {
        this.flexibility = flexibility;
    }

    public String getFareType() {
        return fareType;
    }

    public void setFareType(String fareType) {
        this.fareType = fareType;
    }

    public List<Passenger> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<Passenger> passengers) {
        this.passengers = passengers;
    }

    public Boolean getMiniFareRule() {
        return miniFareRule;
    }

    public void setMiniFareRule(Boolean miniFareRule) {
        this.miniFareRule = miniFareRule;
    }

    public Boolean getSplitOffer() {
        return splitOffer;
    }

    public void setSplitOffer(Boolean splitOffer) {
        this.splitOffer = splitOffer;
    }

    public Boolean getOriginCurrency() {
        return originCurrency;
    }

    public void setOriginCurrency(Boolean originCurrency) {
        this.originCurrency = originCurrency;
    }

    public String getFltnum() {
        return fltnum;
    }

    public void setFltnum(String fltnum) {
        this.fltnum = fltnum;
    }

    public String getChargetype() {
        return chargetype;
    }

    public void setChargetype(String chargetype) {
        this.chargetype = chargetype;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getFltsFROMperiod() {
        return fltsFROMperiod;
    }

    public void setFltsFROMperiod(String fltsFROMperiod) {
        this.fltsFROMperiod = fltsFROMperiod;
    }

    public String getFltsTOperiod() {
        return fltsTOperiod;
    }

    public void setFltsTOperiod(String fltsTOperiod) {
        this.fltsTOperiod = fltsTOperiod;
    }

    public String getCodeformat() {
        return codeformat;
    }

    public void setCodeformat(String codeformat) {
        this.codeformat = codeformat;
    }

    public String getCompanycode() {
        return companycode;
    }

    public void setCompanycode(String companycode) {
        this.companycode = companycode;
    }

    public Boolean getSoldonline() {
        return soldonline;
    }

    public void setSoldonline(Boolean soldonline) {
        this.soldonline = soldonline;
    }

    public Boolean getSsim() {
        return ssim;
    }

    public void setSsim(Boolean ssim) {
        this.ssim = ssim;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Journey {
        @JsonProperty("id")
        private String id;

        @JsonProperty("date")
        private JourneyDate date;

        @JsonProperty("aircraftPreference")
        private AircraftPreference aircraftPreference;

        @JsonProperty("departureAirport")
        private String departureAirport;

        @JsonProperty("arrivalAirport")
        private String arrivalAirport;

        @JsonProperty("action")
        private String action;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public JourneyDate getDate() {
            return date;
        }

        public void setDate(JourneyDate date) {
            this.date = date;
        }

        public AircraftPreference getAircraftPreference() {
            return aircraftPreference;
        }

        public void setAircraftPreference(AircraftPreference aircraftPreference) {
            this.aircraftPreference = aircraftPreference;
        }

        public String getDepartureAirport() {
            return departureAirport;
        }

        public void setDepartureAirport(String departureAirport) {
            this.departureAirport = departureAirport;
        }

        public String getArrivalAirport() {
            return arrivalAirport;
        }

        public void setArrivalAirport(String arrivalAirport) {
            this.arrivalAirport = arrivalAirport;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class JourneyDate {
            @JsonProperty("main")
            private String main;

            @JsonProperty("time")
            private String time;

            @JsonProperty("type")
            private String type;

            @JsonProperty("flexibilityRange")
            private Integer flexibilityRange;

            public String getMain() {
                return main;
            }

            public void setMain(String main) {
                this.main = main;
            }

            public String getTime() {
                return time;
            }

            public void setTime(String time) {
                this.time = time;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public Integer getFlexibilityRange() {
                return flexibilityRange;
            }

            public void setFlexibilityRange(Integer flexibilityRange) {
                this.flexibilityRange = flexibilityRange;
            }
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class AircraftPreference {
            @JsonProperty("code")
            private String code;

            @JsonProperty("name")
            private String name;

            public String getCode() {
                return code;
            }

            public void setCode(String code) {
                this.code = code;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CorporateAccount {
        @JsonProperty("airlineCode")
        private String airlineCode;

        @JsonProperty("number")
        private String number;

        public String getAirlineCode() {
            return airlineCode;
        }

        public void setAirlineCode(String airlineCode) {
            this.airlineCode = airlineCode;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Passenger {
        @JsonProperty("age")
        private Integer age;

        @JsonProperty("seatRequested")
        private Boolean seatRequested;

        @JsonProperty("frequentFlyer")
        private List<FrequentFlyer> frequentFlyer;

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public Boolean getSeatRequested() {
            return seatRequested;
        }

        public void setSeatRequested(Boolean seatRequested) {
            this.seatRequested = seatRequested;
        }

        public List<FrequentFlyer> getFrequentFlyer() {
            return frequentFlyer;
        }

        public void setFrequentFlyer(List<FrequentFlyer> frequentFlyer) {
            this.frequentFlyer = frequentFlyer;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class FrequentFlyer {
            @JsonProperty("airlineCode")
            private String airlineCode;

            @JsonProperty("number")
            private String number;

            public String getAirlineCode() {
                return airlineCode;
            }

            public void setAirlineCode(String airlineCode) {
                this.airlineCode = airlineCode;
            }

            public String getNumber() {
                return number;
            }

            public void setNumber(String number) {
                this.number = number;
            }
        }
    }

    public static AirshopReq mapToFlightSearchRequestDTO(AirshopReqDto airShoppingRQ) {
        AirshopReq flightSearchRequestDTO = new AirshopReq();

        List<Journey> journeyList = new ArrayList<>();
        if (airShoppingRQ.getOds() != null && !airShoppingRQ.getOds().isEmpty()) {
            for (AirshopReqDto.OD od : airShoppingRQ.getOds()) {
                Journey journey = new Journey();
                journey.setDepartureAirport(od.getOrigin());
                journey.setArrivalAirport(od.getDestination());

                Journey.JourneyDate journeyDate = new Journey.JourneyDate();
                journeyDate.setMain(od.getDate());
                journey.setDate(journeyDate);

                journeyList.add(journey);

                // Set cabin type from the OD if available
                if (od.getCabinPreference() != null) {
                    flightSearchRequestDTO.setCabinType(od.getCabinPreference().toLowerCase());
                }
            }
            flightSearchRequestDTO.setJourneys(journeyList);
        }

        flightSearchRequestDTO.setFlexibility(airShoppingRQ.getFlexibility());

        List<Passenger> passengers = new ArrayList<>();

        for (int i = 0; i < airShoppingRQ.getAdults(); i++) {
            Passenger passenger = new Passenger();
            passenger.setAge(30);
            passengers.add(passenger);
        }
        for (int i = 0; i < airShoppingRQ.getChildren(); i++) {
            Passenger passenger = new Passenger();
            passenger.setAge(11);
            passengers.add(passenger);
        }
        for (int i = 0; i < airShoppingRQ.getInfants(); i++) {
            Passenger passenger = new Passenger();
            passenger.setAge(1);
            passengers.add(passenger);
        }
        flightSearchRequestDTO.setPassengers(passengers);

        return flightSearchRequestDTO;
    }

    public AirshopRspGo7Dto unmarshal() throws DatatypeConfigurationException, IOException, InterruptedException {
        String response = makeApiCall(); // Always a JSON string
        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readValue(response, AirshopRspGo7Dto.class);
    }

    public String makeApiCall() throws IOException {
        String baseUrl = "https://api.aerocrs.com/v5/getDeepLink";

        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        urlBuilder.append("?");

        // Map 'journeys' to from/to/date parameters
        if (journeys != null && !journeys.isEmpty()) {
            Journey firstLeg = journeys.get(0);
            urlBuilder.append("from=").append(firstLeg.getDepartureAirport());
            urlBuilder.append("&to=").append(firstLeg.getArrivalAirport());
            // Convert YYYY-MM-DD to YYYY/MM/DD
            urlBuilder.append("&start=").append(firstLeg.getDate().getMain().replace("-", "/"));

            // Handle return leg if present (for 'end' parameter)
            if (journeys.size() > 1) {
                Journey returnLeg = journeys.get(1);
                // Convert YYYY-MM-DD to YYYY/MM/DD
                urlBuilder.append("&end=").append(returnLeg.getDate().getMain().replace("-", "/"));
            }
        }

        // Map 'passengers' to adults/children/infants parameters
        int adults = 0;
        int children = 0;
        int infants = 0;

        if (passengers != null) {
            for (Passenger p : passengers) {
                int age = p.getAge() != null ? p.getAge() : 30; // Default to adult if null
                if (age >= 12) {
                    adults++;
                } else if (age >= 2) {
                    children++;
                } else {
                    infants++;
                }
            }
        }
        urlBuilder.append("&adults=").append(adults);
        urlBuilder.append("&child=").append(children);
        urlBuilder.append("&infant=").append(infants);

        // Optional parameters could be added here if needed (e.g. cabin class)
        if (cabinType != null) {
            // Map cabinType string to AeroCRS code if needed, for now passing as is or
            // skipping if not standard
            // urlBuilder.append("&cabinClass=").append(cabinType);
        }

        String finalUrl = urlBuilder.toString();
        System.out.println("Generated URL is: " + finalUrl);

        HttpHeaders headers = new HttpHeaders();
        // Hardcoded Auth Credentials
        headers.add("auth_id", "70DD4369-72F3-4426-A050-196FBC345009");
        headers.add("auth_password", "vJ3yGilZ9u7N");
        headers.setContentType(MediaType.APPLICATION_JSON); // Content-Type for GET is often ignored but safe to keep

        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        try {
            ResponseEntity<String> response = restTemplate.exchange(finalUrl, HttpMethod.GET, entity, String.class);
            System.out.println("HTTP Response Status Code: " + response.getStatusCode());
            String rawJson = response.getBody();

            // Pretty Print
            try {
                // Check if response is JSON before parsing
                if (rawJson != null && rawJson.trim().startsWith("{")) {
                    Object json = mapper.readValue(rawJson, Object.class);
                    String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                    System.out.println("Pretty Response:\n" + prettyJson);
                } else {
                    System.out.println("Response is not JSON: " + rawJson);
                }
            } catch (Exception ex) {
                System.out.println("Failed to pretty print JSON: " + ex.getMessage());
            }

            return rawJson;

        } catch (HttpClientErrorException e) {
            System.out.println("HTTP Error Response: " + e.getResponseBodyAsString());

            // Return the error JSON string
            return e.getResponseBodyAsString();
        }
    }
}
