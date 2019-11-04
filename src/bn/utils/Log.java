package bn.utils;

import org.joda.time.*;

public class Log {
	public static void i(String msg) {
		System.out.println(createDatePrepend()+" "+msg);
	}

	public static void e(String msg) {
		System.err.println(createDatePrepend()+" "+msg);
	}

	public static String createDatePrepend() {
		final DateTime dt = new DateTime();

		final String day	=	String.format("%2s", dt.getDayOfMonth()).replace(' ', '0');
		final String mon	=	String.format("%2s", dt.getMonthOfYear()).replace(' ', '0');

		final String hour	=	String.format("%2s", dt.getHourOfDay()).replace(' ', '0');
		final String min	=	String.format("%2s", dt.getMinuteOfHour()).replace(' ', '0');
		final String sec	=	String.format("%2s", dt.getSecondOfMinute()).replace(' ', '0');

		return "["+day+"-"+mon+"-"+dt.getCenturyOfEra()+dt.getYearOfCentury()+" "+hour+":"+min+":"+sec+"]";
	}
}
