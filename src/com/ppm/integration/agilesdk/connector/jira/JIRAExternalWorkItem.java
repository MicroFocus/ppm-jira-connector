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
