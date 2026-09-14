package com.airlines.go7api.response;

import com.airlines.go7api.responsego7.common.*;

import com.airlines.go7api.responsedto.common.*;

import com.airlines.go7api.requestdto.AirshopReqDto;
import com.airlines.go7api.responsedto.AirshopRspDto;
import com.airlines.go7api.responsedto.AirshopRspDto.Offer;
import com.airlines.go7api.responsego7.AirshopRspGo7Dto;

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
            // Exception ignored or handled by fallback
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

        String responseId = java.util.UUID.randomUUID().toString().toUpperCase();
        if (!response.getAerocrs().getFlights().getFlight().isEmpty()) {
            Flight firstFlight = response.getAerocrs().getFlights().getFlight().get(0);
            if (firstFlight.getClasses() != null && !firstFlight.getClasses().isEmpty()) {
                AirshopRspGo7Dto.FlightClass fc = firstFlight.getClasses().values().iterator().next();
                responseId = String.valueOf(fc.getFareid());
            }
        }
        airshopRspDto.setResponseId(responseId);
        airshopRspDto.setApiOwner("G7");
        airshopRspDto.setRoundTripType("RT");



        int flightIndex = 0;
        for (Flight flight : response.getAerocrs().getFlights().getFlight()) {
            flightIndex++;
            if (flight.getClasses() != null) {
                for (Map.Entry<String, AirshopRspGo7Dto.FlightClass> entry : flight.getClasses().entrySet()) {
                    AirshopRspGo7Dto.FlightClass flightClass = entry.getValue();
                    String classKey = entry.getKey();

                    Offer offer = mapToOffer(flight, flightClass, classKey, flightIndex, request);
                    offers.add(offer);
                }
            }
        }

        airshopRspDto.setOffers(offers);
        return airshopRspDto;
    }

    private String determineClassType(String classKey, String defaultClassName) {
        if (classKey.contains("Y/Flex Plus")) {
            return "Economy Flex Plus";
        } else if (classKey.contains("Y/Basic")) {
            return "Economy Basic";
        } else if (classKey.contains("B")) {
            return "Business";
        } else {
            return defaultClassName;
        }
    }

    private Offer mapToOffer(Flight flight, AirshopRspGo7Dto.FlightClass flightClass, String classKey, int flightIndex, AirshopReqDto request) {
        int adults = 1;
        int children = 0;
        int infants = 0;
        if (request != null) {
            adults = request.getAdults();
            children = request.getChildren();
            infants = request.getInfants();
        }

        Offer offer = new Offer();
        
        offer.setOfferId(generateOfferId(flight, flightClass, request));

        DateTimeFormatter ndcTimeFormat = DateTimeFormatter.ofPattern("ddMMMyyyy HH:mm:ss", java.util.Locale.ENGLISH);
        LocalDateTime now = LocalDateTime.now();
        offer.setOfferExpiration(now.plusMinutes(20).format(ndcTimeFormat));
        offer.setPaymentTimeLimit(now.plusDays(2).format(ndcTimeFormat));
        offer.setTicketedByTimeLimit(now.plusDays(2).format(ndcTimeFormat));

        offer.setValidatingCarrier("G7");
        offer.setCurrency(flightClass.getCurrency());
        offer.setClassType(determineClassType(classKey, flightClass.getClassName()));
        offer.setCabinTypeCode(flightClass.getCabinClass());

        String segmentId = "SEG" + flightIndex;
        offer.setOds(createODList(flight, flightClass, flightIndex, segmentId));

        BigDecimal grandTotal = BigDecimal.ZERO;
        List<Offer.OfferItemDto> offerItems = new ArrayList<>();
        int itemIdx = 1;

        if (adults > 0) {
            List<String> adtRefs = new ArrayList<>();
            for (int i = 1; i <= adults; i++) {
                adtRefs.add("T" + i);
            }
            Offer.OfferItemDto adtItem = createOfferItem(offer.getOfferId(), itemIdx++, "ADT", adtRefs, flightClass, flightIndex, segmentId);
            offerItems.add(adtItem);
            grandTotal = grandTotal.add(adtItem.getTotalPrice());
        }

        if (children > 0) {
            List<String> cnnRefs = new ArrayList<>();
            for (int i = 1; i <= children; i++) {
                int paxRefIdx = adults + i;
                cnnRefs.add("T" + paxRefIdx);
            }
            Offer.OfferItemDto cnnItem = createOfferItem(offer.getOfferId(), itemIdx++, "CNN", cnnRefs, flightClass, flightIndex, segmentId);
            offerItems.add(cnnItem);
            grandTotal = grandTotal.add(cnnItem.getTotalPrice());
        }

        if (infants > 0) {
            List<String> infRefs = new ArrayList<>();
            for (int i = 1; i <= infants; i++) {
                int parentRefIdx = (i <= adults) ? i : ((i - 1) % adults) + 1;
                infRefs.add("T" + parentRefIdx + ".1");
            }
            Offer.OfferItemDto infItem = createOfferItem(offer.getOfferId(), itemIdx, "INF", infRefs, flightClass, flightIndex, segmentId);
            offerItems.add(infItem);
            grandTotal = grandTotal.add(infItem.getTotalPrice());
        }

        offer.setOfferItems(offerItems);
        offer.setTotalPrice(grandTotal);

        return offer;
    }

    private String generateOfferId(Flight flight, AirshopRspGo7Dto.FlightClass flightClass, AirshopReqDto request) {
        StringBuilder idBuilder = new StringBuilder();
        idBuilder.append(flightClass.getFlightid());
        idBuilder.append("-").append(flightClass.getFareid());
        idBuilder.append("-").append(flight.getFromcode());
        idBuilder.append("-").append(flight.getTocode());
        idBuilder.append("-").append(
                (request != null && request.getTripType() != null
                        && (request.getTripType().equalsIgnoreCase("RT")
                                || request.getTripType().equalsIgnoreCase("RoundTrip")
                                || request.getTripType().equalsIgnoreCase("Return"))) ? "RT" : "OW");
        idBuilder.append("-").append(request != null ? request.getAdults() : 1);
        idBuilder.append("-").append(request != null ? request.getChildren() : 0);
        idBuilder.append("-").append(request != null ? request.getInfants() : 0);
        idBuilder.append("-").append(flightClass.getCurrency());
        return idBuilder.toString();
    }

    private List<OD> createODList(Flight flight, AirshopRspGo7Dto.FlightClass flightClass, int flightIndex, String segmentId) {
        OD od = new OD();
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
        od.setMarketingCarrierCode(flight.getAirlinedesignator());
        od.setMarketingCarrierName(flight.getAirline());
        od.setCabinType(flightClass.getCabinClass());
        od.setSegmentId(segmentId);
        od.setPriceClassId("PC" + flightIndex);
        od.setRbdCode(null);
        od.setFareBasisCode(null);
        od.setDepartureTerminal(flight.getDepartureTerminal());
        od.setArrivalTerminal(flight.getArrivalTerminal());

        if (flight.getStd() != null && flight.getSta() != null) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");
                LocalDateTime dep = LocalDateTime.parse(flight.getStd(), formatter);
                LocalDateTime arr = LocalDateTime.parse(flight.getSta(), formatter);
                Duration duration = Duration.between(dep, arr);
                od.setJourneyTime(duration.toString());
            } catch (Exception e) {
                // Leave null if parsing fails
            }
        }

        String std = flight.getStd();
        if (std != null && std.contains(" ")) {
            String[] parts = std.split(" ");
            od.setDepartureDate(OrderMappingUtil.formatDate(parts[0]));
            od.setDepartureTime(parts[1].substring(0, 5));
        }
        String sta = flight.getSta();
        if (sta != null && sta.contains(" ")) {
            String[] parts = sta.split(" ");
            od.setArrivalDate(OrderMappingUtil.formatDate(parts[0]));
            od.setArrivalTime(parts[1].substring(0, 5));
        }

        List<OD> odList = new ArrayList<>();
        odList.add(od);
        return odList;
    }

    private Offer.OfferItemDto createOfferItem(String offerId, int itemIndex, String ptc, List<String> paxRefs,
            AirshopRspGo7Dto.FlightClass flightClass, int flightIndex, String segmentId) {
        Offer.OfferItemDto item = new Offer.OfferItemDto();
        item.setOfferItemId(offerId + "-" + itemIndex);
        item.setPtc(ptc);
        item.setPassengerRefs(paxRefs);

        int count = paxRefs.size();
        BigDecimal unitTotal = BigDecimal.ZERO;
        BigDecimal unitTax = BigDecimal.ZERO;

        if ("ADT".equals(ptc)) {
            try {
                unitTotal = new BigDecimal(flightClass.getFare().getAdultFare());
                unitTax = new BigDecimal(flightClass.getFare().getTax());
            } catch (Exception e) {
                // Fallback to zero total/tax if fare fields are empty or invalid
            }
        } else if ("CNN".equals(ptc)) {
            String cFare = flightClass.getFare().getChildFare();
            try {
                unitTotal = cFare != null ? new BigDecimal(cFare)
                        : new BigDecimal(flightClass.getFare().getAdultFare());
                unitTax = new BigDecimal(flightClass.getFare().getTax());
            } catch (Exception e) {
                // Fallback to zero total/tax if child fare fields are empty or invalid
            }
        } else if ("INF".equals(ptc)) {
            String iFare = flightClass.getFare().getInfantFare();
            try {
                unitTotal = iFare != null ? new BigDecimal(iFare) : BigDecimal.ZERO;
                unitTax = BigDecimal.ZERO;
            } catch (Exception e) {
                // Fallback to zero total/tax if infant fare fields are empty or invalid
            }
        }

        BigDecimal total = unitTotal.multiply(new BigDecimal(count));
        BigDecimal tax = unitTax.multiply(new BigDecimal(count));

        item.setTotalPrice(total);
        item.setBaggageAllowances(getBaggage(flightClass, segmentId, paxRefs, ptc));
        item.setPriceClassReferences(getPriceClass(flightClass, flightIndex));
        item.setFareDetail(getFareDetail(flightClass, total, tax, count));

        return item;
    }

    private List<Offer.OfferItemDto.BaggageAllowance> getBaggage(AirshopRspGo7Dto.FlightClass flightClass,
            String segmentId, List<String> paxRefs, String type) {
        List<Offer.OfferItemDto.BaggageAllowance> bagList = new ArrayList<>();
        Offer.OfferItemDto.BaggageAllowance checkedBag = new Offer.OfferItemDto.BaggageAllowance();
        checkedBag.setCategory("Checked-In");
        checkedBag.setSegmentrefId(java.util.Collections.singletonList(segmentId));
        checkedBag.setPassengerId(paxRefs);

        int allowance = flightClass.getBaggageAllowance();
        if ("INF".equals(type) && flightClass.getInfbaggageallowance() > 0) {
            allowance = flightClass.getInfbaggageallowance();
        }

        if (allowance > 0) {
            checkedBag.setQuantity("1");
            Offer.OfferItemDto.BaggageAllowance.Weight weight = new Offer.OfferItemDto.BaggageAllowance.Weight();
            weight.setValue(new BigDecimal(allowance));
            weight.setUom(flightClass.getBaggageUnit());
            List<Offer.OfferItemDto.BaggageAllowance.Weight> weightList = new ArrayList<>();
            weightList.add(weight);
            checkedBag.setWeight(weightList);
        }
        bagList.add(checkedBag);

        if (flightClass.getServices() != null && flightClass.getServices().containsKey("HandBaggage")) {
            AirshopRspGo7Dto.Service hbService = flightClass.getServices().get("HandBaggage");
            if (hbService.isActive()) {
                Offer.OfferItemDto.BaggageAllowance handBag = new Offer.OfferItemDto.BaggageAllowance();
                handBag.setCategory("Carry On");
                handBag.setSegmentrefId(java.util.Collections.singletonList(segmentId));
                handBag.setPassengerId(paxRefs);
                handBag.setQuantity("1");

                String text = hbService.getText();
                if (text != null) {
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d{1,10})\\s{0,10}(KG|kg|Kg)");
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
        return bagList;
    }

    private List<Offer.OfferItemDto.PriceClassReference> getPriceClass(AirshopRspGo7Dto.FlightClass flightClass,
            int flightIndex) {
        Offer.OfferItemDto.PriceClassReference priceClassRef = new Offer.OfferItemDto.PriceClassReference();
        priceClassRef.setClassName(
                flightClass.getType() != null && !flightClass.getType().isEmpty() ? flightClass.getType()
                        : flightClass.getClassName());
        priceClassRef.setCabinTypeCode(flightClass.getCabinClass());
        priceClassRef.setPriceClassId("PC" + flightIndex);

        List<Offer.OfferItemDto.PriceClassReference.Description> descriptions = new ArrayList<>();
        if (flightClass.getServices() != null) {
            for (Map.Entry<String, AirshopRspGo7Dto.Service> serviceEntry : flightClass.getServices().entrySet()) {
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
        List<Offer.OfferItemDto.PriceClassReference> list = new ArrayList<>();
        list.add(priceClassRef);
        return list;
    }

    private void populateBasicFares(Offer.OfferItemDto.FareDetail.Price price,
            AirshopRspGo7Dto.FlightClass flightClass, BigDecimal totalFare, BigDecimal tax) {
        BigDecimal base = totalFare.subtract(tax);

        Offer.OfferItemDto.FareDetail.Price.TotalFare totalFareObj = new Offer.OfferItemDto.FareDetail.Price.TotalFare();
        totalFareObj.setAmount(totalFare);
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
    }

    private BigDecimal calculateScaleFactor(Map<String, String> breakdown, BigDecimal tax, int count) {
        BigDecimal breakdownSum = BigDecimal.ZERO;
        for (String val : breakdown.values()) {
            try {
                breakdownSum = breakdownSum.add(new BigDecimal(val));
            } catch (Exception e) {
                // Ignore non-numeric values in tax breakdown summation
            }
        }

        BigDecimal scaleFactor = BigDecimal.ONE;
        if (breakdownSum.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal unitTax = tax.divide(new BigDecimal(count), 10, java.math.RoundingMode.HALF_UP);
            scaleFactor = unitTax.divide(breakdownSum, 10, java.math.RoundingMode.HALF_UP);
        }
        return scaleFactor;
    }

    private void buildTaxesFromBreakdown(List<Offer.OfferItemDto.FareDetail.Price.Taxes> taxesList,
            Map<String, String> breakdown, AirshopRspGo7Dto.FlightClass flightClass,
            BigDecimal tax, BigDecimal scaleFactor, int count) {
        for (Map.Entry<String, String> taxEntry : breakdown.entrySet()) {
            if (tax.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Offer.OfferItemDto.FareDetail.Price.Taxes taxesItem = new Offer.OfferItemDto.FareDetail.Price.Taxes();

            BigDecimal rawUnitTax;
            try {
                rawUnitTax = new BigDecimal(taxEntry.getValue());
            } catch (Exception e) {
                rawUnitTax = BigDecimal.ZERO;
            }

            BigDecimal scaledUnitTax = rawUnitTax.multiply(scaleFactor).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal totalTaxAmount = scaledUnitTax.multiply(new BigDecimal(count));

            taxesItem.setCurrency(flightClass.getCurrency());
            String key = taxEntry.getKey();
            if (key.length() <= 3) {
                taxesItem.setCode(key);
                taxesItem.setDescription("Tax " + key);
            } else {
                taxesItem.setCode("TAX");
                taxesItem.setDescription(key);
            }

            taxesItem.setAmount(totalTaxAmount);
            taxesItem.setTotal(totalTaxAmount);

            taxesList.add(taxesItem);
        }
    }

    private void populateTaxDetails(List<Offer.OfferItemDto.FareDetail.Price.Taxes> taxesList,
            AirshopRspGo7Dto.FlightClass flightClass, BigDecimal tax, int count) {
        if (flightClass.getRawFareObject() != null &&
                flightClass.getRawFareObject().getRackFare() != null &&
                flightClass.getRawFareObject().getRackFare().getTaxBreakdown() != null) {

            Map<String, String> breakdown = flightClass.getRawFareObject().getRackFare().getTaxBreakdown();
            BigDecimal scaleFactor = calculateScaleFactor(breakdown, tax, count);
            buildTaxesFromBreakdown(taxesList, breakdown, flightClass, tax, scaleFactor, count);

        } else if (tax.compareTo(BigDecimal.ZERO) > 0) {
            Offer.OfferItemDto.FareDetail.Price.Taxes taxesItem = new Offer.OfferItemDto.FareDetail.Price.Taxes();
            taxesItem.setCode("TAX");
            taxesItem.setAmount(tax);
            taxesItem.setTotal(tax);
            taxesItem.setCurrency(flightClass.getCurrency());
            taxesItem.setDescription("Total Taxes");
            taxesList.add(taxesItem);
        }
    }

    private List<Offer.OfferItemDto.FareDetail> getFareDetail(AirshopRspGo7Dto.FlightClass flightClass,
            BigDecimal totalFare, BigDecimal tax, int count) {
        Offer.OfferItemDto.FareDetail fareDetail = new Offer.OfferItemDto.FareDetail();
        Offer.OfferItemDto.FareDetail.Price price = new Offer.OfferItemDto.FareDetail.Price();

        populateBasicFares(price, flightClass, totalFare, tax);

        List<Offer.OfferItemDto.FareDetail.Price.Taxes> taxesList = new ArrayList<>();
        populateTaxDetails(taxesList, flightClass, tax, count);

        price.setTaxes(taxesList);
        fareDetail.setPrice(price);

        List<Offer.OfferItemDto.FareDetail> list = new ArrayList<>();
        list.add(fareDetail);
        return list;
    }

    
}
