package com.airlines.go7api.service;

import com.airlines.go7api.entity.BookingEntity;
import com.airlines.go7api.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    @Autowired
    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public void saveBooking(String pnr, String bookingConfirmation, String orderId) {
        if (pnr == null || pnr.isEmpty())
            return;

        BookingEntity entity = new BookingEntity();
        entity.setPnr(pnr);
        entity.setBookingConfirmation(bookingConfirmation);
        entity.setOrderId(orderId);

        bookingRepository.save(entity);
        System.out.println("Saved booking to MongoDB: PNR=" + pnr + ", OrderID=" + orderId);
    }

    public void saveBooking(BookingEntity entity) {
        bookingRepository.save(entity);
        System.out.println(
                "Saved existing booking to MongoDB: PNR=" + entity.getPnr() + ", OrderID=" + entity.getOrderId());
    }

    public Optional<BookingEntity> getBookingByPnr(String pnr) {
        return bookingRepository.findById(pnr);
    }

    public Optional<BookingEntity> getBookingByOrderId(String orderId) {
        return bookingRepository.findByOrderId(orderId);
    }
}
