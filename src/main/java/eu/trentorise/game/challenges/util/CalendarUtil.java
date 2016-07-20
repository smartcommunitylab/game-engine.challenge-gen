package eu.trentorise.game.challenges.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public final class CalendarUtil {

	// change constant in order to change number of days in the challenge
	// (please see hours, minutes and seconds!)
	private static final int CHALLENGE_DURATION = 9;

	private static Calendar calendar;

	private static void init() {
		calendar = new GregorianCalendar();
		calendar.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
		calendar.setTime(new Date());
		calendar.add(Calendar.DAY_OF_MONTH, +1); // tomorrow
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 1);
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
		if (calendar == null) {
			init();
		}
		Calendar copy = (Calendar) calendar.clone();
		copy.add(Calendar.DAY_OF_MONTH, CalendarUtil.CHALLENGE_DURATION);
		return copy;
	}

}