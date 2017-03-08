
package com.ppm.integration.agilesdk.connector.jira;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import com.ppm.integration.agilesdk.tm.ExternalWorkItem;
import com.ppm.integration.agilesdk.tm.ExternalWorkItemEffortBreakdown;

public class JIRAExternalWorkItem extends ExternalWorkItem {

    private String name = "";

    private Double totalEffort = 0.0;

    private String errorMessage = null;

    private Map<String, Double> timeSpentSeconds = new HashMap<>();

    private XMLGregorianCalendar dateFrom;

    private XMLGregorianCalendar dateTo;

    public JIRAExternalWorkItem(String name, Double totalEffort, String errorMessage, XMLGregorianCalendar dateFrom,
            XMLGregorianCalendar dateTo, Map<String, Double> timeSpentSeconds) {
        this.name = name;
        this.totalEffort = totalEffort;
        this.errorMessage = errorMessage;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.timeSpentSeconds = timeSpentSeconds;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public Double getTotalEffort() {
        return null;
    }

    public ExternalWorkItemEffortBreakdown getEffortBreakDown() {
        ExternalWorkItemEffortBreakdown eb = new ExternalWorkItemEffortBreakdown();
        Calendar cursor = dateFrom.toGregorianCalendar();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        while (cursor.before(dateTo.toGregorianCalendar())) {
            String cursorDate = dateFormat.format(cursor.getTime());
            if (timeSpentSeconds.containsKey(cursorDate)) {
                eb.addEffort(cursorDate, timeSpentSeconds.get(cursorDate));
            } else {
                eb.addEffort(cursorDate, 0);
            }
            cursor.add(Calendar.DAY_OF_MONTH, 1);

        }
        return eb;
    }

}
