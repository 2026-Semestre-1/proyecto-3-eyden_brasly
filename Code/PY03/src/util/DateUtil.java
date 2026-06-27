package util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Formatea fechas internas del File System para salidas de consola.
 * 
 * @author eyden
 */
public final class DateUtil {
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private DateUtil() {
    }

    public static String formatMillis(long epochMillis) {
        return formatInstant(Instant.ofEpochMilli(epochMillis));
    }

    public static String formatInstant(Instant instant) {
        if (instant == null) {
            return "N/D";
        }

        return DISPLAY_FORMAT.format(instant);
    }
}
