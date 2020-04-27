package eu.trentorise.game.challenges.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public final class CalendarUtil {

    // change constant in order to change number of days in the challenge
    // (please see hours, minutes and seconds!)
    private static final int CHALLENGE_DURATION = 9;

    private static Calendar calendar;
    private static Calendar endCalendar;

    private static final SimpleDateFormat sdf = new SimpleDateFormat(
            "dd/MM/YYYY HH:mm:ss , zzz ZZ");

    private static void init() {
        calendar = new GregorianCalendar();
        calendar.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_MONTH, +1); // tomorrow
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 1);

        endCalendar = (Calendar) calendar.clone();
        endCalendar.add(Calendar.DAY_OF_MONTH, CalendarUtil.CHALLENGE_DURATION);
    }

    /**
     * Return fixed start date for every generated challenge
     *
     * @return start date
     */
    public static final Calendar getStart() {
        if (calendar == null) {
            init();
        }
        return (Calendar) calendar.clone();
    }

    public static final Calendar getEnd() {
        if (endCalendar == null) {
            init();
        }
        return (Calendar) endCalendar.clone();
    }

    public static final Calendar setStart(Date date) {
        if (calendar == null) {
            init();
        }
        calendar.setTime(date);
        return calendar;
    }

    public static final Calendar setEnd(Date date) {
        if (endCalendar == null) {
            init();
        }
        endCalendar.setTime(date);
        return endCalendar;
    }

    public static final String format(Long value) {
        if (value == null) {
            return "";
        }
        Date d = new Date(value);
        return sdf.format(d);
    }

}
