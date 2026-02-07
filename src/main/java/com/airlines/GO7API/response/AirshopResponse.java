package com.airlines.GO7API.response;

import com.airlines.GO7API.requestDto.AirshopReqDto;
import com.airlines.GO7API.responseDto.AirshopRspDto;
import com.airlines.GO7API.responseDto.AirshopRspDto.Offer;
import com.airlines.GO7API.responseGo7.AirshopRspGo7Dto;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.core.io.ClassPathResource;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import org.springframework.stereotype.Service;

@Service
public class AirshopResponse {

    private Map<String, String> airportMap = new HashMap<>();

    @PostConstruct
    public void loadAirports() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = new ClassPathResource("airports.json").getInputStream();
            List<Map<String, String>> airports = mapper.readValue(inputStream,
                    new TypeReference<List<Map<String, String>>>() {
                    });
            for (Map<String, String> airport : airports) {
                airportMap.put(airport.get("code"), airport.get("name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AirshopRspDto airshoppingMapper(AirshopRspGo7Dto response,
            AirshopReqDto request) {
        AirshopRspDto airshopRspDto = new AirshopRspDto();

        if (response == null || response.getAerocrs() == null || !response.getAerocrs().isSuccess()
                || response.getAerocrs().getFlights() == null
                || response.getAerocrs().getFlights().getFlight() == null) {
            return airshopRspDto;
        }

        List<Offer> offers = new ArrayList<>();

        // Logic to extract fareid for responseId
        String responseId = java.util.UUID.randomUUID().toString().toUpperCase();
        if (!response.getAerocrs().getFlights().getFlight().isEmpty()) {
            AirshopRspGo7Dto.Flight firstFlight = response.getAerocrs().getFlights().getFlight().get(0);
            if (firstFlight.getClasses() != null && !firstFlight.getClasses().isEmpty()) {
                for (AirshopRspGo7Dto.FlightClass fc : firstFlight.getClasses().values()) {
                    responseId = String.valueOf(fc.getFareid());
                    break;
                }
            }
        }
        airshopRspDto.setResponseId(responseId);
        airshopRspDto.setApiOwner("G7");

        String type = request != null && request.getTripType() != null ? request.getTripType() : "OneWay";
        if (type.equalsIgnoreCase("OneWay")) {
            airshopRspDto.setRoundTripType("OneWay");
        } else {
            airshopRspDto.setRoundTripType("RoundTrip");
        }

        // Generate Passenger Refs
        List<String> passengerRefs = new ArrayList<>();
        int paxCount = 1;
        if (request != null) {
            for (int i = 0; i < request.getAdults(); i++)
                passengerRefs.add("T" + (paxCount++));
            for (int i = 0; i < request.getChildren(); i++)
                passengerRefs.add("T" + (paxCount++));
            for (int i = 0; i < request.getInfants(); i++)
                passengerRefs.add("T" + (paxCount++));
        } else {
            passengerRefs.add("T1");
        }

        int flightIndex = 0;
        for (AirshopRspGo7Dto.Flight flight : response.getAerocrs().getFlights().getFlight()) {
            flightIndex++;
            if (flight.getClasses() != null) {
                for (Map.Entry<String, AirshopRspGo7Dto.FlightClass> entry : flight.getClasses().entrySet()) {
                    AirshopRspGo7Dto.FlightClass flightClass = entry.getValue();
                    String classKey = entry.getKey(); // e.g. "Y/Flex Plus" or "B"

                    Offer offer = new Offer();
                    // Generate ID with all required params (hyphen separated)
                    StringBuilder idBuilder = new StringBuilder();
                    idBuilder.append(flightClass.getFlightid());
                    idBuilder.append("-").append(flightClass.getFareid());
                    idBuilder.append("-").append(flight.getFromcode());
                    idBuilder.append("-").append(flight.getTocode());
                    idBuilder.append("-").append(
                            (request != null && request.getTripType() != null
                                    && (request.getTripType().equalsIgnoreCase("RT")
                                            || request.getTripType().equalsIgnoreCase("RoundTrip")
                                            || request.getTripType().equalsIgnoreCase("Return"))) ? "RT" : "OneWay");
                    idBuilder.append("-").append(request != null ? request.getAdults() : 1);
                    idBuilder.append("-").append(request != null ? request.getChildren() : 0);
                    idBuilder.append("-").append(request != null ? request.getInfants() : 0);
                    idBuilder.append("-").append(flightClass.getCurrency());

                    offer.setOfferId(idBuilder.toString());

                    // Time Limits
                    DateTimeFormatter ndcTimeFormat = DateTimeFormatter.ofPattern("ddMMMyyyy HH:mm:ss",
                            java.util.Locale.ENGLISH);
                    LocalDateTime now = LocalDateTime.now();
                    offer.setOfferExpiration(now.plusMinutes(20).format(ndcTimeFormat));
                    offer.setPaymentTimeLimit(now.plusDays(2).format(ndcTimeFormat));
                    offer.setTicketedByTimeLimit(now.plusDays(2).format(ndcTimeFormat));

                    offer.setValidatingCarrier(flight.getAirlineDesignator());
                    offer.setCurrency(flightClass.getCurrency());
                    offer.setClassType(flightClass.getClassName());
                    offer.setCabinTypeCode(flightClass.getCabinClass());

                    // Price
                    BigDecimal adultFare = new BigDecimal(flightClass.getFare().getAdultFare());
                    offer.setTotalPrice(adultFare);

                    // OD Mapping
                    Offer.OD od = new Offer.OD();
                    String segmentId = "SEG" + flightIndex;

                    // Set odKey based on direction (Outbound=OD1, Inbound=OD2)
                    String odKey = "OD1";
                    if (flight.getDirection() != null && flight.getDirection().equalsIgnoreCase("inbound")) {
                        odKey = "OD2";
                    }
                    od.setOdKey(odKey);

                    od.setOrigin(flight.getFromcode());
                    od.setDestination(flight.getTocode());
                    od.setOriginAirportName(airportMap.getOrDefault(flight.getFromcode(), flight.getFromcode()));
                    od.setDestinationAirportName(airportMap.getOrDefault(flight.getTocode(), flight.getTocode()));
                    od.setFlightNumber(flight.getFltnum());
                    od.setEquipment(flight.getAircraftType());
                    od.setMarketingCarrierCode(flight.getAirlineDesignator());
                    od.setMarketingCarrierName(flight.getAirlineName());
                    // Set cabinType as cached from flightClass (requested by user)
                    od.setCabinType(flightClass.getCabinClass());
                    od.setSegmentId(segmentId);
                    od.setPriceClassId("PC" + flightIndex);

                    // RBD and FareBasiscode
                    od.setRbdCode(null);
                    od.setFareBasisCode(null);

                    // Map Terminals (keep empty if empty)
                    od.setDepartureTerminal(flight.getDepartureTerminal());
                    od.setArrivalTerminal(flight.getArrivalTerminal());

                    // Calculate Journey Time
                    if (flight.getStdInUtc() != null && flight.getStaInUtc() != null) {
                        try {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");
                            LocalDateTime dep = LocalDateTime.parse(flight.getStdInUtc(), formatter);
                            LocalDateTime arr = LocalDateTime.parse(flight.getStaInUtc(), formatter);
                            Duration duration = Duration.between(dep, arr);
                            od.setJourneyTime(duration.toString());
                        } catch (Exception e) {
                            // Leave null if parsing fails
                        }
                    }

                    // Date/Time split (Assume standard format YYYY/MM/DD HH:mm:ss.SSS)
                    String std = flight.getStd();
                    if (std != null && std.contains(" ")) {
                        String[] parts = std.split(" ");
                        od.setDepartureDate(parts[0].replace("/", "-")); // Normalize to YYYY-MM-DD
                        od.setDepartureTime(parts[1].substring(0, 5)); // HH:mm
                    }
                    String sta = flight.getSta();
                    if (sta != null && sta.contains(" ")) {
                        String[] parts = sta.split(" ");
                        od.setArrivalDate(parts[0].replace("/", "-"));
                        od.setArrivalTime(parts[1].substring(0, 5));
                    }

                    List<Offer.OD> odList = new ArrayList<>();
                    odList.add(od);
                    offer.setOds(odList);

                    // OfferItem
                    Offer.OfferItemDto offerItem = new Offer.OfferItemDto();
                    offerItem.setOfferItemId(offer.getOfferId() + "-1");
                    offerItem.setPtc("ADT");
                    offerItem.setPassengerRefs(passengerRefs);
                    offerItem.setTotalPrice(adultFare);

                    // Baggage
                    List<Offer.OfferItemDto.BaggageAllowance> bagList = new ArrayList<>();

                    // 1. Checked-In Baggage (from flat fields)
                    Offer.OfferItemDto.BaggageAllowance checkedBag = new Offer.OfferItemDto.BaggageAllowance();
                    checkedBag.setCategory("Checked-In");
                    checkedBag.setSegmentrefId(java.util.Collections.singletonList(segmentId));
                    checkedBag.setPassengerId(passengerRefs);

                    if (flightClass.getBaggageAllowance() > 0) {
                        checkedBag.setQuantity("1");
                        Offer.OfferItemDto.BaggageAllowance.Weight weight = new Offer.OfferItemDto.BaggageAllowance.Weight();
                        weight.setValue(new BigDecimal(flightClass.getBaggageAllowance()));
                        weight.setUom(flightClass.getBaggageUnit());
                        List<Offer.OfferItemDto.BaggageAllowance.Weight> weightList = new ArrayList<>();
                        weightList.add(weight);
                        checkedBag.setWeight(weightList);
                    }
                    bagList.add(checkedBag);

                    // 2. Hand Baggage (from Services)
                    if (flightClass.getServices() != null && flightClass.getServices().containsKey("HandBaggage")) {
                        AirshopRspGo7Dto.Service hbService = flightClass.getServices().get("HandBaggage");
                        if (hbService.isActive()) {
                            Offer.OfferItemDto.BaggageAllowance handBag = new Offer.OfferItemDto.BaggageAllowance();
                            handBag.setCategory("Carry On");
                            handBag.setSegmentrefId(java.util.Collections.singletonList(segmentId));
                            handBag.setPassengerId(passengerRefs);
                            handBag.setQuantity("1");

                            // Try to parse weight from text e.g. "1 Item per PAX, max 10 KG"
                            String text = hbService.getText();
                            if (text != null) {
                                // Simple parser for "max XX KG" or "XX KG"
                                java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+)\\s*(KG|kg|Kg)");
                                java.util.regex.Matcher m = p.matcher(text);
                                if (m.find()) {
                                    Offer.OfferItemDto.BaggageAllowance.Weight hbWeight = new Offer.OfferItemDto.BaggageAllowance.Weight();
                                    hbWeight.setValue(new BigDecimal(m.group(1)));
                                    hbWeight.setUom(m.group(2).toUpperCase());
                                    List<Offer.OfferItemDto.BaggageAllowance.Weight> hbWeightList = new ArrayList<>();
                                    hbWeightList.add(hbWeight);
                                    handBag.setWeight(hbWeightList);
                                }

                                List<Offer.OfferItemDto.BaggageAllowance.DescriptionDTO> descList = new ArrayList<>();
                                Offer.OfferItemDto.BaggageAllowance.DescriptionDTO descDto = new Offer.OfferItemDto.BaggageAllowance.DescriptionDTO();
                                descDto.setDescription(text);
                                descList.add(descDto);
                                handBag.setDescriptions(descList);
                            }
                            bagList.add(handBag);
                        }
                    }

                    offerItem.setBaggageAllowances(bagList);

                    // Price Class References (Services)
                    Offer.OfferItemDto.PriceClassReference priceClassRef = new Offer.OfferItemDto.PriceClassReference();
                    priceClassRef.setClassName(
                            flightClass.getType() != null && !flightClass.getType().isEmpty() ? flightClass.getType()
                                    : flightClass.getClassName());
                    priceClassRef.setCabinTypeCode(flightClass.getCabinClass());
                    priceClassRef.setPriceClassId("PC" + flightIndex);

                    List<Offer.OfferItemDto.PriceClassReference.Description> descriptions = new ArrayList<>();

                    if (flightClass.getServices() != null) {
                        for (Map.Entry<String, AirshopRspGo7Dto.Service> serviceEntry : flightClass.getServices()
                                .entrySet()) {
                            // Use text provided in service if available
                            if (serviceEntry.getValue().isActive()) {
                                Offer.OfferItemDto.PriceClassReference.Description desc = new Offer.OfferItemDto.PriceClassReference.Description();
                                String text = serviceEntry.getValue().getText();
                                if (text != null && !text.isEmpty()) {
                                    desc.setText(serviceEntry.getKey() + ": " + text);
                                } else {
                                    desc.setText(null);
                                }
                                descriptions.add(desc);
                            }
                        }
                    }
                    priceClassRef.setDescriptions(descriptions);
                    List<Offer.OfferItemDto.PriceClassReference> priceClassRefList = new ArrayList<>();
                    priceClassRefList.add(priceClassRef);
                    offerItem.setPriceClassReferences(priceClassRefList);

                    // FareDetail
                    Offer.OfferItemDto.FareDetail fareDetail = new Offer.OfferItemDto.FareDetail();
                    Offer.OfferItemDto.FareDetail.Price price = new Offer.OfferItemDto.FareDetail.Price();

                    // Base and Tax
                    BigDecimal tax = new BigDecimal(flightClass.getFare().getTax());
                    BigDecimal base = adultFare.subtract(tax);

                    Offer.OfferItemDto.FareDetail.Price.TotalFare totalFareObj = new Offer.OfferItemDto.FareDetail.Price.TotalFare();
                    totalFareObj.setAmount(adultFare);
                    totalFareObj.setCurrency(flightClass.getCurrency());
                    price.setTotalFare(totalFareObj);

                    Offer.OfferItemDto.FareDetail.Price.BaseFare baseFareObj = new Offer.OfferItemDto.FareDetail.Price.BaseFare();
                    baseFareObj.setAmount(base);
                    baseFareObj.setCurrency(flightClass.getCurrency());
                    price.setBaseFare(baseFareObj);

                    Offer.OfferItemDto.FareDetail.Price.TotalTax totalTaxObj = new Offer.OfferItemDto.FareDetail.Price.TotalTax();
                    totalTaxObj.setAmount(tax);
                    totalTaxObj.setCurrency(flightClass.getCurrency());
                    price.setTotalTax(totalTaxObj);

                    // Tax Breakdown
                    List<Offer.OfferItemDto.FareDetail.Price.Taxes> taxesList = new ArrayList<>();
                    if (flightClass.getRawFareObject() != null &&
                            flightClass.getRawFareObject().getRackFare() != null &&
                            flightClass.getRawFareObject().getRackFare().getTaxBreakdown() != null) {

                        for (Map.Entry<String, String> taxEntry : flightClass.getRawFareObject().getRackFare()
                                .getTaxBreakdown().entrySet()) {
                            Offer.OfferItemDto.FareDetail.Price.Taxes taxesItem = new Offer.OfferItemDto.FareDetail.Price.Taxes();
                            taxesItem.setTotal(tax);
                            taxesItem.setCurrency(flightClass.getCurrency());

                            // Try to map code - if key is short e.g. "YQ" use as code, else description
                            String key = taxEntry.getKey();
                            if (key.length() <= 3) {
                                taxesItem.setCode(key);
                                taxesItem.setDescription("Tax " + key); // Or Generic
                            } else {
                                taxesItem.setCode("TAX"); // Generic code
                                taxesItem.setDescription(key);
                            }

                            try {
                                taxesItem.setAmount(new BigDecimal(taxEntry.getValue()));
                            } catch (Exception e) {
                                taxesItem.setAmount(BigDecimal.ZERO);
                            }
                            taxesList.add(taxesItem);
                        }
                    }
                    price.setTaxes(taxesList);

                    fareDetail.setPrice(price);

                    List<Offer.OfferItemDto.FareDetail> fareDetailList = new ArrayList<>();
                    fareDetailList.add(fareDetail);
                    offerItem.setFareDetail(fareDetailList);

                    List<Offer.OfferItemDto> offerItems = new ArrayList<>();
                    offerItems.add(offerItem);
                    offer.setOfferItems(offerItems);

                    offers.add(offer);
                }
            }
        }

        airshopRspDto.setOffers(offers);
        return airshopRspDto;
    }
}
