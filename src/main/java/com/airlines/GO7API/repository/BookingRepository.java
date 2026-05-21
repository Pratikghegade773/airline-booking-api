package com.airlines.go7api.repository;

import com.airlines.go7api.entity.BookingEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingRepository extends MongoRepository<BookingEntity, String> {
    Optional<BookingEntity> findByOrderId(String orderId);
}
