package com.airlines.go7api.service;

import com.airlines.go7api.entity.BookingEntity;
import com.airlines.go7api.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BookingService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BookingService.class);

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
        logger.info("Saved booking to MongoDB: PNR={}, OrderID={}", pnr, orderId);
    }

    public void saveBooking(BookingEntity entity) {
        bookingRepository.save(entity);
        logger.info("Saved existing booking to MongoDB: PNR={}, OrderID={}", entity.getPnr(), entity.getOrderId());
    }

    public Optional<BookingEntity> getBookingByPnr(String pnr) {
        return bookingRepository.findById(pnr);
    }

    public Optional<BookingEntity> getBookingByOrderId(String orderId) {
        return bookingRepository.findByOrderId(orderId);
    }
}
