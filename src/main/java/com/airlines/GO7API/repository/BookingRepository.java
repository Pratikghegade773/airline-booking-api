package com.airlines.GO7API.repository;

import com.airlines.GO7API.entity.BookingEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingRepository extends MongoRepository<BookingEntity, String> {
    Optional<BookingEntity> findByOrderId(String orderId);
}
