package com.ppm.integration.agilesdk.connector.jira;

import com.ppm.integration.agilesdk.tm.ExternalWorkItem;
import com.ppm.integration.agilesdk.tm.ExternalWorkItemEffortBreakdown;
import net.sf.json.JSONObject;

import javax.xml.datatype.XMLGregorianCalendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class JIRAExternalWorkItem extends ExternalWorkItem {
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private String name = "";

    private long totalEffort = 0;

    private String errorMessage = null;

    private Map<String, Long> timeSpentSeconds = new HashMap<>();

    private XMLGregorianCalendar dateFrom;

    private XMLGregorianCalendar dateTo;

    public JIRAExternalWorkItem(String name, long totalEffort, String errorMessage, XMLGregorianCalendar dateFrom,
            XMLGregorianCalendar dateTo, Map<String, Long> timeSpentSeconds)
    {
        this.name = name;
        this.totalEffort = totalEffort;
        this.errorMessage = errorMessage;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.timeSpentSeconds = timeSpentSeconds;
    }

    @Override public String getName() {
        return this.name;
    }

    @Override public Double getTotalEffort() {
        return (double)totalEffort;
    }

    public ExternalWorkItemEffortBreakdown getEffortBreakDown() {

        ExternalWorkItemEffortBreakdown effortBreakdown = new ExternalWorkItemEffortBreakdown();

        Calendar cursor = dateFrom.toGregorianCalendar();

        while (cursor.before(dateTo.toGregorianCalendar())) {
            String cursorDate = dateFormat.format(cursor.getTime());
            if (timeSpentSeconds.containsKey(cursorDate)) {
                effortBreakdown.addEffort(cursor.getTime(), (double)(timeSpentSeconds.get(cursorDate) / 3600));
            } else {
                effortBreakdown.addEffort(cursor.getTime(), 0.0);
            }
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }

        return effortBreakdown;
    }

    @Override public String getErrorMessage() {
        return errorMessage;
    }

}
