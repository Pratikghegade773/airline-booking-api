package com.airlines.go7api.response;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrderMappingUtilTest {

    @Test
    void testMapPaxType() {
        assertEquals("ADT", OrderMappingUtil.mapPaxType("ADULT"));
        assertEquals("ADT", OrderMappingUtil.mapPaxType("adult"));
        assertEquals("CHD", OrderMappingUtil.mapPaxType("CHILD"));
        assertEquals("CHD", OrderMappingUtil.mapPaxType("child"));
        assertEquals("INF", OrderMappingUtil.mapPaxType("INFANT"));
        assertEquals("INF", OrderMappingUtil.mapPaxType("infant"));
        assertEquals("ADT", OrderMappingUtil.mapPaxType("UNKNOWN")); // Default fallback
        assertEquals("ADT", OrderMappingUtil.mapPaxType(null));
    }

    @Test
    void testFormatDate() {
        assertNull(OrderMappingUtil.formatDate(null));
        
        // yyyy-MM-dd format
        assertEquals("15May2023", OrderMappingUtil.formatDate("2023-05-15"));
        
        // yyyy/MM/dd format
        assertEquals("15May2023", OrderMappingUtil.formatDate("2023/05/15"));
        
        // Invalid date format should return the original string
        assertEquals("invalid-date", OrderMappingUtil.formatDate("invalid-date"));
    }

    @Test
    void testAdjustDateByDays() {
        assertNull(OrderMappingUtil.adjustDateByDays(null, 5));
        
        // yyyy-MM-dd format
        assertEquals("2023-05-20", OrderMappingUtil.adjustDateByDays("2023-05-15", 5));
        
        // yyyy/MM/dd format
        assertEquals("2023-05-10", OrderMappingUtil.adjustDateByDays("2023/05/15", -5));
        
        // Invalid date format should return the original string
        assertEquals("invalid-date", OrderMappingUtil.adjustDateByDays("invalid-date", 1));
    }

    @Test
    void testCalculateJourneyTime() {
        // Null checks
        assertEquals("PT0H0M", OrderMappingUtil.calculateJourneyTime(null, "10:00", "2023-05-15", "12:00"));
        
        // Standard calculation (yyyy-MM-dd format)
        assertEquals("PT2H30M", OrderMappingUtil.calculateJourneyTime("2023-05-15", "10:00", "2023-05-15", "12:30"));
        
        // Standard calculation (yyyy/MM/dd format)
        assertEquals("PT2H30M", OrderMappingUtil.calculateJourneyTime("2023/05/15", "10:00", "2023/05/15", "12:30"));
        
        // Wait! The logic says if arr.isBefore(dep) arr = arr.plusDays(1).
        // If 10:00 to 08:00 on same date -> 08:00 is before 10:00 -> +1 day -> 22 hours.
        assertEquals("PT22H0M", OrderMappingUtil.calculateJourneyTime("2023-05-15", "10:00", "2023-05-15", "08:00"));
        
        // Invalid parse should return default
        assertEquals("PT0H0M", OrderMappingUtil.calculateJourneyTime("invalid", "time", "invalid", "time"));
    }

    @Test
    void testFormatCurrentDate() {
        String expectedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMMyyyy", Locale.ENGLISH));
        assertEquals(expectedDate, OrderMappingUtil.formatCurrentDate());
    }
}
