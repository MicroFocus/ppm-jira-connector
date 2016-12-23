package com.ppm.integration.agilesdk.connector.jira;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import com.hp.ppm.integration.tm.ExternalWorkItemActualEfforts;
import com.hp.ppm.integration.tm.IExternalWorkItem;

import net.sf.json.JSONObject;

public class JIRAExternalWorkItem implements IExternalWorkItem {
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private String name = "";
	private long totalEffort = 0;
	private String errorMessage = null;

	private Map<String, Long> timeSpentSeconds = new HashMap<>();
	private XMLGregorianCalendar dateFrom;
	private XMLGregorianCalendar dateTo;

	public JIRAExternalWorkItem(String name, long totalEffort, String errorMessage, XMLGregorianCalendar dateFrom,
			XMLGregorianCalendar dateTo, Map<String, Long> timeSpentSeconds) {
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

	@Override
	public double getEffort() {
		return totalEffort;
	}

	@Override
	public String getExternalData() {

		JSONObject json = new JSONObject();

		ExternalWorkItemActualEfforts actual = new ExternalWorkItemActualEfforts();

		Calendar cursor = dateFrom.toGregorianCalendar();

		while (cursor.before(dateTo.toGregorianCalendar())) {
			String cursorDate = dateFormat.format(cursor.getTime());
			if (timeSpentSeconds.containsKey(cursorDate)) {
				actual.getEffortList().put(cursorDate, (double) (timeSpentSeconds.get(cursorDate) / 3600));
			} else {
				actual.getEffortList().put(cursorDate, 0.0);
			}
			cursor.add(Calendar.DAY_OF_MONTH, 1);
		}

		json.put(ExternalWorkItemActualEfforts.JSON_KEY_FOR_ACTUAL_EFFORT, actual.toJson());

		return json.toString();
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

}
