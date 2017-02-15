package com.ppm.integration.agilesdk.connector.jira.model;

import static com.ppm.integration.agilesdk.connector.jira.JIRAConstants.NULL_VALUE;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.ppm.integration.agilesdk.pm.ExternalTask;

public class JIRAEntity extends ExternalTask {

	Date checkDate(String dateStr) {
		if (dateStr != null && !NULL_VALUE.equalsIgnoreCase(dateStr)) {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			Date d = null;
			try {
				if (dateStr.length() > 10) {
					dateStr = dateStr.substring(0, 10);
				}
				d = format.parse(dateStr);
			} catch (ParseException e) {
				return new Date();
			}
			return d;
		} else {
			return new Date();
		}
	}

}
