
package com.ppm.integration.agilesdk.connector.jira.model;

import static com.ppm.integration.agilesdk.connector.jira.JIRAConstants.NULL_VALUE;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

import com.ppm.integration.agilesdk.pm.ExternalTask;

public class JIRAEntity extends ExternalTask {
    private final Logger logger = Logger.getLogger(this.getClass());

    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    Date checkDate(String dateStr) {
        if (dateStr != null && !NULL_VALUE.equalsIgnoreCase(dateStr)) {

            Date d = null;
            try {
                if (dateStr.length() > 10) {
                    dateStr = dateStr.substring(0, 10);
                }
                d = format.parse(dateStr);
            } catch (ParseException e) {
                logger.error("Date Parse Error,the input dateStr is " + dateStr, e);
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                return c.getTime();
            }
            return d;
        } else {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            return c.getTime();

        }
    }

}
