package com.airlines.go7api.response;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class OrderMappingUtil {

    private static final DateTimeFormatter FORMATTER_DASH = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FORMATTER_SLASH = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter FORMATTER_OUTPUT = DateTimeFormatter.ofPattern("ddMMMyyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATETIME_FORMATTER_SLASH = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER_DASH = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private OrderMappingUtil() {
        // Utility class
    }

    public static String mapPaxType(String go7Type) {
        if ("ADULT".equalsIgnoreCase(go7Type))
            return "ADT";
        if ("CHILD".equalsIgnoreCase(go7Type))
            return "CHD";
        if ("INFANT".equalsIgnoreCase(go7Type))
            return "INF";
        return "ADT";
    }

    public static String formatDate(String dateStr) {
        if (dateStr == null)
            return null;
        try {
            DateTimeFormatter inputFormatter = dateStr.contains("/") ? FORMATTER_SLASH : FORMATTER_DASH;
            LocalDate date = LocalDate.parse(dateStr, inputFormatter);
            return date.format(FORMATTER_OUTPUT);
        } catch (Exception e) {
            return dateStr;
        }
    }

    public static String adjustDateByDays(String dateStr, int days) {
        if (dateStr == null)
            return null;
        try {
            DateTimeFormatter inputFormatter = dateStr.contains("/") ? FORMATTER_SLASH : FORMATTER_DASH;
            LocalDate date = LocalDate.parse(dateStr, inputFormatter);
            return date.plusDays(days).format(FORMATTER_DASH);
        } catch (Exception e) {
            return dateStr;
        }
    }

    public static String calculateJourneyTime(String depDate, String depTime, String arrDate, String arrTime) {
        if (depDate == null || depTime == null || arrDate == null || arrTime == null) {
            return "PT0H0M";
        }
        try {
            DateTimeFormatter dateFormatter = depDate.contains("/") ? DATETIME_FORMATTER_SLASH : DATETIME_FORMATTER_DASH;

            LocalDateTime dep = LocalDateTime.parse(depDate + " " + depTime, dateFormatter);
            LocalDateTime arr = LocalDateTime.parse(arrDate + " " + arrTime, dateFormatter);

            if (arr.isBefore(dep)) {
                arr = arr.plusDays(1);
            }

            Duration duration = Duration.between(dep, arr);
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();

            return String.format("PT%dH%dM", hours, minutes);

        } catch (Exception e) {
            return "PT0H0M";
        }
    }

    public static String formatCurrentDate() {
        return LocalDate.now().format(FORMATTER_OUTPUT);
    }
}
