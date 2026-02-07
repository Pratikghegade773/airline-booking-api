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
}
