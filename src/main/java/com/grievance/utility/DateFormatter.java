package com.grievance.utility;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateFormatter {
    private static final DateTimeFormatter DD_MM_YY_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yy");

    /**
     * Format LocalDateTime to dd-MM-yy format
     * Returns empty string if input is null
     */
    public static String formatToDDMMYY(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DD_MM_YY_FORMATTER);
    }

    /**
     * Format LocalDate to dd-MM-yy format
     * Returns empty string if input is null
     */
    public static String formatToDDMMYY(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DD_MM_YY_FORMATTER);
    }

    public static String convertUtcToISTDate(LocalDateTime utcTime) {
        if (utcTime == null) return "";

        return utcTime
                .atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.of("Asia/Kolkata"))
                .toLocalDate()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    }

    public static LocalDateTime convertUtcToIst(LocalDateTime utcTime) {
        if (utcTime == null) return null;

        return utcTime
                .atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.of("Asia/Kolkata"))
                .toLocalDateTime();
    }
}

