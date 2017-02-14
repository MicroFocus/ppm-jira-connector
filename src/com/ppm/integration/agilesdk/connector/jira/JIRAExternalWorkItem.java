package com.ppm.integration.agilesdk.connector.jira;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
		return totalEffort;
	}

	// The code is replaced by the method getEffortBreakDown,it will be removed
	// once the new method works well
	// once the function passed the test and i am
	// @Override
	// public String getExternalData() {
	//
	// JSONObject json = new JSONObject();
	//
	// ExternalWorkItemActualEfforts actual = new
	// ExternalWorkItemActualEfforts();
	//
	// Calendar cursor = dateFrom.toGregorianCalendar();
	//
	// while (cursor.before(dateTo.toGregorianCalendar())) {
	// String cursorDate = dateFormat.format(cursor.getTime());
	// if (timeSpentSeconds.containsKey(cursorDate)) {
	// long actualEffort = timeSpentSeconds.get(cursorDate) / 3600;
	// actual.getEffortList().put(cursorDate, (double) actualEffort);
	// } else {
	// actual.getEffortList().put(cursorDate, 0.0);
	// }
	// cursor.add(Calendar.DAY_OF_MONTH, 1);
	// }
	//
	// json.put(ExternalWorkItemActualEfforts.JSON_KEY_FOR_ACTUAL_EFFORT,
	// actual.toJson());
	//
	// return json.toString();
	// }

	public ExternalWorkItemEffortBreakdown getEffortBreakDown() {
		ExternalWorkItemEffortBreakdown eb = new ExternalWorkItemEffortBreakdown();

		Set<String> dateKeys = timeSpentSeconds.keySet();
		for (String date : dateKeys) {
			eb.addEffort(date, timeSpentSeconds.get(date));
		}
		return eb;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

}
