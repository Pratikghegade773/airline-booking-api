package com.airlines.GO7API.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.airlines.GO7API.error.ErrorRsp;
import com.airlines.GO7API.requestDto.OrderCreateReqDto;
// import com.airlines.GO7API.responseDto.OrderviewRS; // Not found yet
// import com.airlines.GO7API.utils.CountryCodeMapper; // Not found yet
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateReq {

    @JsonProperty("offerIds")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> offerIds;

    @JsonProperty("passengers")
    private List<Passenger> passengers;

    @JsonProperty("languageCode")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String languageCode;

    // Confirmation / Hold parameters
    @JsonProperty("agentConfirmation")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String agentConfirmation;

    @JsonProperty("remarks")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String remarks;

    @JsonProperty("confirmationEmail")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String confirmationEmail;

    @JsonProperty("holdBooking")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean holdBooking;

    @JsonProperty("sendEmail")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean sendEmail;

    public List<String> getOfferIds() {
        return offerIds;
    }

    public void setOfferIds(List<String> offerIds) {
        this.offerIds = offerIds;
    }

    public List<Passenger> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<Passenger> passengers) {
        this.passengers = passengers;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getAgentConfirmation() {
        return agentConfirmation;
    }

    public void setAgentConfirmation(String agentConfirmation) {
        this.agentConfirmation = agentConfirmation;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getConfirmationEmail() {
        return confirmationEmail;
    }

    public void setConfirmationEmail(String confirmationEmail) {
        this.confirmationEmail = confirmationEmail;
    }

    public Boolean getHoldBooking() {
        return holdBooking;
    }

    public void setHoldBooking(Boolean holdBooking) {
        this.holdBooking = holdBooking;
    }

    public Boolean getSendEmail() {
        return sendEmail;
    }

    public void setSendEmail(Boolean sendEmail) {
        this.sendEmail = sendEmail;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Passenger {
        @JsonProperty("id")
        private String id;

        @JsonProperty("frequentFlyer")
        private List<FrequentFlyer> frequentFlyer;

        @JsonProperty("firstName")
        private String firstName;

        @JsonProperty("middleName")
        private String middleName;

        @JsonProperty("lastName")
        private String lastName;

        @JsonProperty("dateOfBirth")
        private String dateOfBirth;

        @JsonProperty("gender")
        private String gender;

        @JsonProperty("title")
        private String title;

        @JsonProperty("contactInformation")
        private ContactInformation contactInformation;

        @JsonProperty("apisData")
        private ApisData apisData;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<FrequentFlyer> getFrequentFlyer() {
            return frequentFlyer;
        }

        public void setFrequentFlyer(List<FrequentFlyer> frequentFlyer) {
            this.frequentFlyer = frequentFlyer;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getMiddleName() {
            return middleName;
        }

        public void setMiddleName(String middleName) {
            this.middleName = middleName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getDateOfBirth() {
            return dateOfBirth;
        }

        public void setDateOfBirth(String dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public ContactInformation getContactInformation() {
            return contactInformation;
        }

        public void setContactInformation(ContactInformation contactInformation) {
            this.contactInformation = contactInformation;
        }

        public ApisData getApisData() {
            return apisData;
        }

        public void setApisData(ApisData apisData) {
            this.apisData = apisData;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContactInformation {
        @JsonProperty("email")
        private String email;

        @JsonProperty("phone")
        private List<Phone> phone;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public List<Phone> getPhone() {
            return phone;
        }

        public void setPhone(List<Phone> phone) {
            this.phone = phone;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Phone {
        @JsonProperty("countryCode")
        private String countryCode;

        @JsonProperty("number")
        private String number;

        @JsonProperty("type")
        private String type;

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApisData {
        @JsonProperty("nationality")
        private String nationality;

        @JsonProperty("countryOfResidence")
        private String countryOfResidence;

        @JsonProperty("travelDocType")
        private String travelDocType;

        @JsonProperty("travelDocNumber")
        private String travelDocNumber;

        @JsonProperty("travelDocIssueDate")
        private String travelDocIssueDate;

        @JsonProperty("travelDocExpiry")
        private String travelDocExpiry;

        @JsonProperty("address")
        private Address address;

        public String getNationality() {
            return nationality;
        }

        public void setNationality(String nationality) {
            this.nationality = nationality;
        }

        public String getCountryOfResidence() {
            return countryOfResidence;
        }

        public void setCountryOfResidence(String countryOfResidence) {
            this.countryOfResidence = countryOfResidence;
        }

        public String getTravelDocType() {
            return travelDocType;
        }

        public void setTravelDocType(String travelDocType) {
            this.travelDocType = travelDocType;
        }

        public String getTravelDocNumber() {
            return travelDocNumber;
        }

        public void setTravelDocNumber(String travelDocNumber) {
            this.travelDocNumber = travelDocNumber;
        }

        public String getTravelDocIssueDate() {
            return travelDocIssueDate;
        }

        public void setTravelDocIssueDate(String travelDocIssueDate) {
            this.travelDocIssueDate = travelDocIssueDate;
        }

        public String getTravelDocExpiry() {
            return travelDocExpiry;
        }

        public void setTravelDocExpiry(String travelDocExpiry) {
            this.travelDocExpiry = travelDocExpiry;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Address {
        @JsonProperty("addressLines")
        private List<String> addressLines;

        @JsonProperty("city")
        private String city;

        @JsonProperty("postalCode")
        private String postalCode;

        @JsonProperty("countryCode")
        private String countryCode;

        public List<String> getAddressLines() {
            return addressLines;
        }

        public void setAddressLines(List<String> addressLines) {
            this.addressLines = addressLines;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getPostalCode() {
            return postalCode;
        }

        public void setPostalCode(String postalCode) {
            this.postalCode = postalCode;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }
    }

    @JsonIgnore
    private String offerId;
    @JsonIgnore
    private String apiKey;
    @JsonIgnore
    private String orderCreateUrl;

    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }

    public String getOfferId() {
        return offerId;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setOrderCreateUrl(String orderCreateUrl) {
        this.orderCreateUrl = orderCreateUrl;
    }

    public String getOrderCreateUrl() {
        return orderCreateUrl;
    }

    public static OrderCreateReq mapToFlightSearchRequestDTO(OrderCreateReqDto orderCreateRQ) {

        OrderCreateReq orderCreateReq = new OrderCreateReq();
        orderCreateReq.setOrderCreateUrl(orderCreateRQ.getOrderCreateUrl());
        orderCreateReq.setApiKey(orderCreateRQ.getApiKey());

        orderCreateReq.setOfferId(orderCreateRQ.getOfferId());

        // Map confirmation fields
        orderCreateReq.setAgentConfirmation(orderCreateRQ.getAgentConfirmation());
        orderCreateReq.setRemarks(orderCreateRQ.getRemarks());
        orderCreateReq.setConfirmationEmail(orderCreateRQ.getConfirmationEmail());
        orderCreateReq.setHoldBooking(orderCreateRQ.getHoldBooking());
        orderCreateReq.setSendEmail(orderCreateRQ.getSendEmail());

        List<Passenger> passengerList = new ArrayList<>();

        if (orderCreateRQ.getPassengers() != null) {
            for (OrderCreateReqDto.Pax pax : orderCreateRQ.getPassengers()) {
                Passenger passenger = new Passenger();
                passenger.setId(pax.getPaxId());
                if (pax.getGender() != null) {
                    passenger.setGender(pax.getGender().toLowerCase());
                }
                if (pax.getTitle() != null) {
                    passenger.setTitle(pax.getTitle().toLowerCase());
                }
                passenger.setFirstName(pax.getFirstName());
                if (pax.getMiddleName() != null) {
                    passenger.setMiddleName(pax.getMiddleName());
                }
                passenger.setLastName(pax.getLastName());
                passenger.setDateOfBirth(pax.getDob());
                if (pax.getPhoneNumber() != null) {
                    ContactInformation contactInformation = new ContactInformation();

                    List<Phone> phones = new ArrayList<>();
                    Phone phone = new Phone();
                    phone.setCountryCode(pax.getCountryDialingCode());
                    phone.setNumber(pax.getPhoneNumber().toString());
                    phone.setType("Mobile");
                    phones.add(phone);

                    // Duplicate phone for Home type as in reference
                    phone = new Phone();
                    phone.setCountryCode(pax.getCountryDialingCode());
                    phone.setNumber(pax.getPhoneNumber().toString());
                    phone.setType("Home");
                    phones.add(phone);

                    contactInformation.setPhone(phones);

                    contactInformation.setEmail(pax.getEmail());

                    passenger.setContactInformation(contactInformation);
                }
                if (pax.getIdentityDocument() != null) {
                    ApisData apisData = new ApisData();
                    if (pax.getIdentityDocument().getCitizenshipCountryCode() != null) {
                        // CountryCodeMapper missing, using raw code or TODO
                        // apisData.setNationality(CountryCodeMapper.countryCodeMap.getOrDefault(pax.getIdentityDocument().getCitizenshipCountryCode().toUpperCase(),
                        // pax.getIdentityDocument().getCitizenshipCountryCode()));
                        apisData.setNationality(pax.getIdentityDocument().getCitizenshipCountryCode());
                    }
                    if (pax.getIdentityDocument().getResidenceCountryCode() != null) {
                        apisData.setCountryOfResidence(pax.getIdentityDocument().getResidenceCountryCode());
                    }
                    if (pax.getIdentityDocument().getIdentityDocumentType() != null) {
                        apisData.setTravelDocType(
                                pax.getIdentityDocument().getIdentityDocumentType().equalsIgnoreCase("PT")
                                        ? "passport"
                                        : pax.getIdentityDocument().getIdentityDocumentType());
                    }
                    if (pax.getIdentityDocument().getIdentityDocumentNumber() != null) {
                        apisData.setTravelDocNumber(pax.getIdentityDocument().getIdentityDocumentNumber());
                    }
                    if (pax.getIdentityDocument().getIssueDate() != null) {
                        apisData.setTravelDocIssueDate(pax.getIdentityDocument().getIssueDate());
                    }
                    if (pax.getIdentityDocument().getExpiryDate() != null) {
                        apisData.setTravelDocExpiry(pax.getIdentityDocument().getExpiryDate());
                    }
                    passenger.setApisData(apisData);
                }
                passengerList.add(passenger);
            }
        }
        orderCreateReq.setPassengers(passengerList);
        return orderCreateReq;
    }

    public Object unmarshal() throws DatatypeConfigurationException, IOException, InterruptedException {
        String response = makeApiCall(); // Always string now
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode root = objectMapper.readTree(response);

            if (root.has("errors")) {
                // It's an Error Response
                ErrorRsp errorRsp = new ErrorRsp();
                JsonNode errorsArray = root.path("errors");

                if (errorsArray.isArray()) {
                    for (JsonNode errorNode : errorsArray) {
                        String errorMessage = errorNode.path("message").asText();
                        String code = errorNode.path("code").asText();

                        ErrorRsp.Error tempError = new ErrorRsp.Error();
                        tempError.setError(errorMessage);
                        tempError.setCode(code);
                        errorRsp.getErrorList().add(tempError);
                    }
                }

                return errorRsp; // return ErrorRsp object
            } else {
                // Normal Successful response
                // OrderviewRS missing, returning JsonNode or map
                // OrderviewRS orderCreateResponse = objectMapper.readValue(response,
                // OrderviewRS.class);
                // return orderCreateResponse;
                return objectMapper.readValue(response, Object.class);
            }
        } catch (Exception e) {
            System.out.println("Error parsing response: " + e.getMessage());
            return response; // Fallback
        }
    }

    public String makeApiCall() throws IOException {
        String baseUrl = orderCreateUrl + offerId;
        String jsonBody = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(this);

        HttpHeaders headers = new HttpHeaders();
        headers.add("x-api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        System.out.println("Generated Request is:\n" + jsonBody);

        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        try {
            ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.POST, entity, String.class);
            System.out.println("HTTP Response Status Code: " + response.getStatusCode());
            String rawJson = response.getBody();

            // Pretty Print
            try {
                Object json = mapper.readValue(rawJson, Object.class);
                String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                System.out.println("Pretty Response:\n" + prettyJson);
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
