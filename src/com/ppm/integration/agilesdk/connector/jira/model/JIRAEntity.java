
package com.ppm.integration.agilesdk.connector.jira.model;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.ppm.integration.agilesdk.connector.jira.JIRAConstants.NULL_VALUE;

public abstract class JIRAEntity extends JIRABase {

    private String name;

    private String key;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public static Date getDefaultStartDate() {
        Calendar todayMorning = new GregorianCalendar();
        todayMorning.set(Calendar.HOUR, 1);
        todayMorning.set(Calendar.MINUTE, 0);
        todayMorning.set(Calendar.SECOND, 0);
        todayMorning.set(Calendar.MILLISECOND, 0);
        return todayMorning.getTime();

    }

    public static Date getDefaultFinishDate() {
        Calendar todayEvening = new GregorianCalendar();
        todayEvening.set(Calendar.HOUR, 23);
        todayEvening.set(Calendar.MINUTE, 0);
        todayEvening.set(Calendar.SECOND, 0);
        todayEvening.set(Calendar.MILLISECOND, 0);
        return todayEvening.getTime();
    }

    /**
     * Dates returned from external systems don't have time set and are set to midnight (i.e.
     * start of the day). However, a date set as a finish date usually means
     * "after the day as finished", and we need to set the hour to the end of
     * the day so that PPM computes duration correctly. <br>
     * This will return a modified date, and will keep date passed in parameter unmodified.
     */
    protected Date adjustFinishDateTime(Date date) {
        return modifyDateTime(23, date);
    }

    /**
     * Dates returned from external systems don't always have time set or use some default time.
     * This method will ensure that time is set at 1:00 am (start of the date) so that PPM computes duration correctly. <br>
     * This will return a modified date, and will keep date passed in parameter unmodified.
     */
    protected Date adjustStartDateTime(Date date) {
        return modifyDateTime(1, date);
    }

    private Date modifyDateTime(int hour, Date date) {
        if (date == null) {
            return null;
        }

        Calendar c = Calendar.getInstance();
        c.setTime(date);

        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        return c.getTime();
    }

}
