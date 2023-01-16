package com.ppm.integration.agilesdk.connector.jira.model;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static com.ppm.integration.agilesdk.connector.jira.JIRAConstants.NULL_VALUE;

/**
 * Created by canaud on 7/25/2017.
 */
public class JIRABase {

    protected final Logger logger = Logger.getLogger(this.getClass());

    private SimpleDateFormat DATE_ONLY_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat FULL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSZ");
    private SimpleDateFormat DATE_TIME_NO_TZ_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");
    private SimpleDateFormat DATE_TIME_NO_MS_NO_TZ_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");

    protected Date convertToNonNullDate(String dateStr) {

        Date date = convertToDate(dateStr);

        if (date == null) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            date = c.getTime();
        }

        return date;
    }


    public String convertDateToString(Date d) {
        if (d == null) {
            return null;
        }

        return DATE_ONLY_FORMAT.format(d);
    }

    /** Convert a string date to only the Date part, discarding the time & timezone part. */
    protected Date convertToDate(String dateStr) {
        if (StringUtils.isEmpty(dateStr) || NULL_VALUE.equalsIgnoreCase(dateStr)) {
            return null;
        }
        Date d = null;
        try {
            if (dateStr.length() > 10) {
                dateStr = dateStr.substring(0, 10);
            }
            return DATE_ONLY_FORMAT.parse(dateStr);
        } catch (ParseException e) {
            logger.error("Date Parse Error,the input dateStr is " + dateStr, e);
            return null;
        }
    }

    /** Convert JSon date string to full date - includes time & timezone info if included */
    protected Date convertToDateTime(String dateStr) {
        if (StringUtils.isBlank(dateStr) || NULL_VALUE.equalsIgnoreCase(dateStr)) {
            return null;
        }
        Date d = null;
        try {
            if (dateStr.length() >= 28) {
                return FULL_DATE_FORMAT.parse(dateStr);
            } else if (dateStr.length() >= 23) {
                return DATE_TIME_NO_TZ_FORMAT.parse(dateStr);
            } else if (dateStr.length() >= 19) {
                return DATE_TIME_NO_MS_NO_TZ_FORMAT.parse(dateStr);
            } else {
                return DATE_ONLY_FORMAT.parse(dateStr);
            }
        } catch (ParseException e) {
            logger.error("Date/Time Parse Error,the input dateStr is " + dateStr, e);
            return null;
        }
    }

    protected String convertToSimpleDate(String dateStr) {
        if (StringUtils.isEmpty(dateStr) || NULL_VALUE.equalsIgnoreCase(dateStr)) {
            return null;
        }

        if (dateStr.length() >= 10) {
            return dateStr.substring(0, 10);
        } else {
            return null;
        }
    }
}
