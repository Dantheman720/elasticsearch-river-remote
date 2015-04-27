/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.elasticsearch.river.remote;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import org.elasticsearch.common.joda.time.format.DateTimeFormat;
import org.elasticsearch.common.joda.time.format.DateTimeFormatter;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;

/**
 * Date and Time related utility functions.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public abstract class DateTimeUtils {
    
    public static final String CUSTOM_MILISEC_EPOCH_DATETIME_FORMAT = "{milisecondEpoch}";
    public static final String CUSTOM_UNIX_EPOCH_DATETIME_FORMAT = "{unixEpoch}";
    
    private static final Map<String,DateTimeFormatter> dateTimeFormattersCache = new ConcurrentHashMap<String,DateTimeFormatter>();
    
	/**
	 * Parse ISO datetime string.
	 * 
	 * @param dateString to parse
	 * @return parsed date
	 * @throws IllegalArgumentException if date is not parseable
	 */
	public static final Date parseISODateTime(String dateString) {
		if (Utils.isEmpty(dateString))
			return null;
		return ISODateTimeFormat.dateTimeParser().parseDateTime(dateString).toDate();
	}

	protected static final SimpleDateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SZ");
	static {
		ISO_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	/**
	 * Format Date into ISO 8601 full datetime string.
	 * 
	 * @param date to format
	 * @return formatted string
	 */
	public static final String formatISODateTime(Date date) {
		if (date == null)
			return null;
		synchronized (ISO_DATE_FORMAT) {
			return ISO_DATE_FORMAT.format(date);
		}
	}

	/**
	 * Parse date string with minute precise - so seconds and milliseconds are set to 0. Used because JQL allows only
	 * minute precise queries.
	 * 
	 * @param dateString to parse
	 * @return parsed date rounded to minute precise
	 * @throws IllegalArgumentException if date is not parseable
	 */
	public static Date parseISODateTimeWithMinutePrecise(String dateString) {
		if (Utils.isEmpty(dateString))
			return null;
		return DateTimeUtils.roundDateTimeToMinutePrecise(ISODateTimeFormat.dateTimeParser().parseDateTime(dateString)
				.toDate());
	}

	/**
	 * Change date to minute precise - seconds and milliseconds are set to 0. Used because JQL allows only minute precise
	 * queries.
	 * 
	 * @param date to round
	 * @return rounded date
	 */
	public static Date roundDateTimeToMinutePrecise(Date date) {
		if (date == null)
			return null;
		Calendar cal = new GregorianCalendar();
		cal.setTime(date);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}
	
	/**
	 * Format date/time as in specified format parameter.
	 * 
	 * @param date to be formatted
	 * @param format for the date/time created using YodaTime compatible pattern. Alternatively there are two
	 * exceptions where you can provide "{milisecondEpoch}" or "{unixEpoch}" values which refer to long-number-based
	 * formats that cannot be expressed with YodaTime pattern. 
	 * @return formatted date as according to the given format. If the format is omitted ISO_DATE_FORMAT is used.
	 * If date is null then null is also returned.
	 */
	public static String formatDateTime( Date date , String format ) {
	    
	    if ( date==null ) return null;
	    
	    if ( format==null || format.trim().length()==0 ) {
	        return formatISODateTime(date);
	    }
	    
	    switch (format) {
	        case CUSTOM_MILISEC_EPOCH_DATETIME_FORMAT : return String.valueOf( date.getTime() );
	        case CUSTOM_UNIX_EPOCH_DATETIME_FORMAT : return String.valueOf( (long)(date.getTime()/1000) );
	    }
	    
	    if (dateTimeFormattersCache.containsKey(format)) {
	        return dateTimeFormattersCache.get(format).print(date.getTime());
	    } else {
	        DateTimeFormatter dtf = DateTimeFormat.forPattern(format);
	        dateTimeFormattersCache.put(format, dtf);
	        return dtf.print(date.getTime());
	    }
	}

}
