package com.ppm.integration.agilesdk.connector.jira;

import com.hp.ppm.user.model.User;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.jira.model.*;
import com.ppm.integration.agilesdk.connector.jira.rest.util.IRestConfig;
import com.ppm.integration.agilesdk.connector.jira.rest.util.JIRARestConfig;
import com.ppm.integration.agilesdk.connector.jira.rest.util.JiraRestWrapper;
import com.ppm.integration.agilesdk.connector.jira.rest.util.exception.RestRequestException;
import com.ppm.integration.agilesdk.connector.jira.service.JIRAService;
import com.ppm.integration.agilesdk.connector.jira.util.JiraIssuesRetrieverUrlBuilder;
import com.ppm.integration.agilesdk.connector.jira.util.dm.AgileEntityUtils;
import com.ppm.integration.agilesdk.provider.Providers;
import com.ppm.integration.agilesdk.provider.UserProvider;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wink.client.ClientResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.xml.datatype.XMLGregorianCalendar;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JIRAServiceProvider {

    public static UserProvider getUserProvider() {
        return Providers.getUserProvider(JIRAIntegrationConnector.class);
    }

    public static JIRAService get(ValueSet config) {
        IRestConfig adminRestConfig = new JIRARestConfig();
        adminRestConfig.setProxy(config.get(JIRAConstants.KEY_PROXY_HOST), config.get(JIRAConstants.KEY_PROXY_PORT));
        adminRestConfig.setBasicAuthorizationCredentials(config.get(JIRAConstants.KEY_ADMIN_USERNAME),
                config.get(JIRAConstants.KEY_ADMIN_PASSWORD));
        JiraRestWrapper adminWrapper = new JiraRestWrapper(adminRestConfig);

        IRestConfig userRestConfig = new JIRARestConfig();
        userRestConfig.setProxy(config.get(JIRAConstants.KEY_PROXY_HOST), config.get(JIRAConstants.KEY_PROXY_PORT));
        userRestConfig.setBasicAuthorizationCredentials(config.get(JIRAConstants.KEY_USERNAME),
                config.get(JIRAConstants.KEY_PASSWORD));
        JiraRestWrapper userWrapper = new JiraRestWrapper(userRestConfig);

        JIRAService service = new JIRAService(config.get(JIRAConstants.KEY_BASE_URL), adminWrapper, userWrapper);

        return service;
    }

    }
