package com.airlines.GO7API.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bookings")
public class BookingEntity {
    @Id
    private String pnr; // This stores the PNR and maps to _id in MongoDB

    private String bookingConfirmation;
    private String orderId;

    // Cache assigned seats temporarily to survive GetBooking wiping them out before
    // payment
    private java.util.Map<String, String> passengerSeats;

    // Cache generic ancillary services separately from seats for strictly separate
    // flows
    private java.util.Map<String, String> passengerServices;

    // Cache exact total order price to overcome Go7 stripping ancillaries from
    // totals
    private java.math.BigDecimal totalOrderPrice;

    // Store primary passenger last name to support GetBooking calls without NDC
    // context
    private String primaryPassengerLastName;
}
