package com.airlines.GO7API.response;

import com.airlines.GO7API.requestDto.AirshopReqDto;
import com.airlines.GO7API.responseDto.OrderReshopRspDto;
import com.airlines.GO7API.responseDto.OrderReshopRspDto.Offer;
import com.airlines.GO7API.responseGo7.AirshopRspGo7Dto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderReshopResponse {

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

    public OrderReshopRspDto orderReshopMapper(AirshopRspGo7Dto response,
                                                   AirshopReqDto request) {
        OrderReshopRspDto reshopRspDto = new OrderReshopRspDto();

        if (response == null || response.getAerocrs() == null || !response.getAerocrs().isSuccess()
                || response.getAerocrs().getFlights() == null
                || response.getAerocrs().getFlights().getFlight() == null) {
            return reshopRspDto;
        }

        List<Offer> offers = new ArrayList<>();

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
        reshopRspDto.setResponseId(responseId);
        reshopRspDto.setApiOwner("G7");

        String type = request != null && request.getTripType() != null ? request.getTripType() : "OneWay";
        if (type.equalsIgnoreCase("OneWay")) {
            reshopRspDto.setRoundTripType("OneWay");
        } else {
            reshopRspDto.setRoundTripType("RoundTrip");
        }

        int adults = request != null ? request.getAdults() : 1;
        int children = request != null ? request.getChildren() : 0;
        int infants = request != null ? request.getInfants() : 0;

        int flightIndex = 0;
        for (AirshopRspGo7Dto.Flight flight : response.getAerocrs().getFlights().getFlight()) {
            flightIndex++;
            if (flight.getClasses() != null) {
                for (Map.Entry<String, AirshopRspGo7Dto.FlightClass> entry : flight.getClasses().entrySet()) {
                    AirshopRspGo7Dto.FlightClass flightClass = entry.getValue();
                    String classKey = entry.getKey();

                    Offer offer = new Offer();
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

                    offer.setOfferId(idBuilder.toString());

                    DateTimeFormatter ndcTimeFormat = DateTimeFormatter.ofPattern("ddMMMyyyy HH:mm:ss",
                            java.util.Locale.ENGLISH);
                    LocalDateTime now = LocalDateTime.now();
                    offer.setOfferExpiration(now.plusMinutes(20).format(ndcTimeFormat));
                    offer.setPaymentTimeLimit(now.plusDays(2).format(ndcTimeFormat));
                    offer.setTicketedByTimeLimit(now.plusDays(2).format(ndcTimeFormat));

                    offer.setValidatingCarrier("G7");
                    offer.setCurrency(flightClass.getCurrency());
                    if (classKey.contains("Y/Flex Plus")) {
                        offer.setClassType("Economy Flex Plus");
                    } else if (classKey.contains("Y/Basic")) {
                        offer.setClassType("Economy Basic");
                    } else if (classKey.contains("B")) {
                        offer.setClassType("Business");
                    } else {
                        offer.setClassType(flightClass.getClassName());
                    }
                    offer.setCabinTypeCode(flightClass.getCabinClass());

                    BigDecimal grandTotal = BigDecimal.ZERO;

                    Offer.OD od = new Offer.OD();
                    String segmentId = "SEG" + flightIndex;

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
                    od.setCabinType(flightClass.getCabinClass());
                    od.setSegmentId(segmentId);
                    od.setPriceClassId("PC" + flightIndex);

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
                        }
                    }

                    String std = flight.getStd();
                    if (std != null && std.contains(" ")) {
                        String[] parts = std.split(" ");
                        od.setDepartureDate(parts[0].replace("/", "-"));
                        od.setDepartureTime(parts[1].substring(0, 5));
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

                    List<Offer.OfferItemDto> offerItems = new ArrayList<>();
                    int itemIdx = 1;

                    if (adults > 0) {
                        List<String> adtRefs = new ArrayList<>();
                        for (int i = 1; i <= adults; i++) {
                            adtRefs.add("T" + i);
                        }
                        Offer.OfferItemDto adtItem = createOfferItem(offer.getOfferId(), itemIdx++, "ADT", adtRefs,
                                flightClass, flightIndex, segmentId, "ADT");
                        offerItems.add(adtItem);
                        grandTotal = grandTotal.add(adtItem.getTotalPrice());
                    }

                    if (children > 0) {
                        List<String> cnnRefs = new ArrayList<>();
                        for (int i = 1; i <= children; i++) {
                            int paxRefIdx = adults + i;
                            cnnRefs.add("T" + paxRefIdx);
                        }
                        Offer.OfferItemDto cnnItem = createOfferItem(offer.getOfferId(), itemIdx++, "CNN", cnnRefs,
                                flightClass, flightIndex, segmentId, "CNN");
                        offerItems.add(cnnItem);
                        grandTotal = grandTotal.add(cnnItem.getTotalPrice());
                    }

                    if (infants > 0) {
                        List<String> infRefs = new ArrayList<>();
                        for (int i = 1; i <= infants; i++) {
                            int parentRefIdx = (i <= adults) ? i : ((i - 1) % adults) + 1;
                            infRefs.add("T" + parentRefIdx + ".1");
                        }
                        Offer.OfferItemDto infItem = createOfferItem(offer.getOfferId(), itemIdx++, "INF", infRefs,
                                flightClass, flightIndex, segmentId, "INF");
                        offerItems.add(infItem);
                        grandTotal = grandTotal.add(infItem.getTotalPrice());
                    }
                    offer.setOfferItems(offerItems);
                    offer.setTotalPrice(grandTotal);

                    offers.add(offer);
                }
            }
        }

        reshopRspDto.setOffers(offers);
        return reshopRspDto;
    }

    private Offer.OfferItemDto createOfferItem(String offerId, int itemIndex, String ptc, List<String> paxRefs,
                                               AirshopRspGo7Dto.FlightClass flightClass, int flightIndex, String segmentId, String type) {
        Offer.OfferItemDto item = new Offer.OfferItemDto();
        item.setOfferItemId(offerId + "-" + itemIndex);
        item.setPtc(ptc);
        item.setPassengerRefs(paxRefs);

        int count = paxRefs.size();

        BigDecimal unitTotal = BigDecimal.ZERO;
        BigDecimal unitTax = BigDecimal.ZERO;

        if ("ADT".equals(type)) {
            try {
                unitTotal = new BigDecimal(flightClass.getFare().getAdultFare());
                unitTax = new BigDecimal(flightClass.getFare().getTax());
            } catch (Exception e) {
            }
        } else if ("CNN".equals(type)) {
            String cFare = flightClass.getFare().getChildFare();
            try {
                unitTotal = cFare != null ? new BigDecimal(cFare)
                        : new BigDecimal(flightClass.getFare().getAdultFare());
                unitTax = new BigDecimal(flightClass.getFare().getTax());
            } catch (Exception e) {
            }
        } else if ("INF".equals(type)) {
            String iFare = flightClass.getFare().getInfantFare();
            try {
                unitTotal = iFare != null ? new BigDecimal(iFare) : BigDecimal.ZERO;
                unitTax = BigDecimal.ZERO;
            } catch (Exception e) {
            }
        }

        BigDecimal total = unitTotal.multiply(new BigDecimal(count));
        BigDecimal tax = unitTax.multiply(new BigDecimal(count));

        item.setTotalPrice(total);

        item.setBaggageAllowances(getBaggage(flightClass, segmentId, paxRefs, type));
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

    private List<Offer.OfferItemDto.FareDetail> getFareDetail(AirshopRspGo7Dto.FlightClass flightClass,
                                                              BigDecimal totalFare, BigDecimal tax, int count) {
        Offer.OfferItemDto.FareDetail fareDetail = new Offer.OfferItemDto.FareDetail();
        Offer.OfferItemDto.FareDetail.Price price = new Offer.OfferItemDto.FareDetail.Price();

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

        List<Offer.OfferItemDto.FareDetail.Price.Taxes> taxesList = new ArrayList<>();
        if (flightClass.getRawFareObject() != null &&
                flightClass.getRawFareObject().getRackFare() != null &&
                flightClass.getRawFareObject().getRackFare().getTaxBreakdown() != null) {

            Map<String, String> breakdown = flightClass.getRawFareObject().getRackFare().getTaxBreakdown();

            BigDecimal breakdownSum = BigDecimal.ZERO;
            for (String val : breakdown.values()) {
                try {
                    breakdownSum = breakdownSum.add(new BigDecimal(val));
                } catch (Exception e) {
                }
            }

            BigDecimal scaleFactor = BigDecimal.ONE;
            if (breakdownSum.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal unitTax = tax.divide(new BigDecimal(count), 10, java.math.RoundingMode.HALF_UP);
                scaleFactor = unitTax.divide(breakdownSum, 10, java.math.RoundingMode.HALF_UP);
            }

            for (Map.Entry<String, String> taxEntry : breakdown.entrySet()) {
                if (tax.compareTo(BigDecimal.ZERO) <= 0)
                    continue;

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
        } else if (tax.compareTo(BigDecimal.ZERO) > 0) {
            Offer.OfferItemDto.FareDetail.Price.Taxes taxesItem = new Offer.OfferItemDto.FareDetail.Price.Taxes();
            taxesItem.setCode("TAX");
            taxesItem.setAmount(tax);
            taxesItem.setTotal(tax);
            taxesItem.setCurrency(flightClass.getCurrency());
            taxesItem.setDescription("Total Taxes");
            taxesList.add(taxesItem);
        }

        price.setTaxes(taxesList);
        fareDetail.setPrice(price);

        List<Offer.OfferItemDto.FareDetail> list = new ArrayList<>();
        list.add(fareDetail);
        return list;
    }
}
