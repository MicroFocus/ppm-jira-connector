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

    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

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

        return format.format(d);
    }

    protected Date convertToDate(String dateStr) {
        if (StringUtils.isEmpty(dateStr) || NULL_VALUE.equalsIgnoreCase(dateStr)) {
            return null;
        }
        Date d = null;
        try {
            if (dateStr.length() > 10) {
                dateStr = dateStr.substring(0, 10);
            }
            return format.parse(dateStr);
        } catch (ParseException e) {
            logger.error("Date Parse Error,the input dateStr is " + dateStr, e);
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
