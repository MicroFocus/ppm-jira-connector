package com.ppm.integration.agilesdk.connector.jira;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAAgileEntity;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAFieldInfo;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAIssueType;
import com.ppm.integration.agilesdk.connector.jira.service.JIRAService;
import com.ppm.integration.agilesdk.dm.*;
import com.ppm.integration.agilesdk.model.*;
import org.apache.commons.lang.StringUtils;

import java.util.*;

import static com.ppm.integration.agilesdk.connector.jira.JIRAConstants.JIRA_NAME_PREFIX;

public class JIRARequestIntegration extends RequestIntegration {

    @Override public List<AgileEntityInfo> getAgileEntitiesInfo(String agileProjectValue,  ValueSet instanceConfigurationParameters) {

        List<JIRAIssueType> issueTypes = JIRAServiceProvider.get(instanceConfigurationParameters).getProjectIssueTypes(agileProjectValue);

        List<AgileEntityInfo> entityList = new ArrayList<AgileEntityInfo>();
        for (JIRAIssueType issueType : issueTypes) {
            AgileEntityInfo feature = new AgileEntityInfo();
            feature.setName(issueType.getName());
            feature.setType(issueType.getId());
            entityList.add(feature);
        }

        return entityList;
    }

    @Override public List<AgileEntityFieldInfo> getAgileEntityFieldsInfo(String agileProjectValue, String entityType,
            ValueSet instanceConfigurationParameters)
    {
        List<AgileEntityFieldInfo> fieldsInfo = new ArrayList<>();

        List<JIRAFieldInfo> fields = new ArrayList(JIRAServiceProvider.get(instanceConfigurationParameters).getFields(agileProjectValue, entityType).values());

        for (JIRAFieldInfo field: fields) {

            // We support following JIRA fields types to map:
            // - string
            // - number
            // - array type with non-empty allowedValues (i.e. drop down list)
            // - User or array of User
            // - priority (provided that allowedValues is non-empty)

            if ("string".equals(field.getType())
                    || "number".equals(field.getType())
                    || "user".equals(field.getType())
                    || "priority".equals(field.getType())
                    || ("array".equals(field.getType()))) {

                if (field.isList() && !"user".equals(field.getType()) && (field.getAllowedValues()== null || field.getAllowedValues().isEmpty())) {
                    // We only allow to select lists that have some static value options or are users lists.
                    continue;
                }

                AgileEntityFieldInfo fieldInfo = new AgileEntityFieldInfo();
                fieldInfo.setId(field.getKey());
                fieldInfo.setLabel(field.getName());
                fieldInfo.setListType(field.isList());
                fieldInfo.setFieldType(field.getType());
                fieldsInfo.add(fieldInfo);
            }
        }

        return fieldsInfo;
    }

    /*@Override public List<AgileEntityField> getAgileEntityFieldValueList(String agileProjectValue, String entityType, String listIdentifier,
            ValueSet instanceConfigurationParameters)
    {
        // Not used.

        List<JIRAFieldInfo> fields = service.get(instanceConfigurationParameters).getFields(agileProjectValue, entityType);

        for (JIRAFieldInfo field : fields) {
            if (listIdentifier.equals(field.getKey())) {
                return field.getAllowedValues();
            }
        }

        // Field not found.
        return new ArrayList<AgileEntityField>();
    }*/

    @Override public AgileEntity updateEntity(String agileProjectValue, String entityType, AgileEntity entity,
            ValueSet instanceConfigurationParameters)
    {

        JIRAService jiraService = JIRAServiceProvider.get(instanceConfigurationParameters);

        Map<String, String> fields = getFieldsFromAgileEntity(entity, jiraService);

        String issueKey = jiraService.updateIssue(agileProjectValue, entity.getId(), fields);

        return jiraService.getSingleAgileEntityIssue(agileProjectValue, issueKey);
    }

    private Map<String,String> getFieldsFromAgileEntity(AgileEntity entity, JIRAService service) {
        Map<String, String> fields = new HashMap<>();

        Iterator<Map.Entry<String, DataField>> fieldsIterator = entity.getAllFields();

        while (fieldsIterator.hasNext()) {
            Map.Entry<String, DataField> field = fieldsIterator.next();

            DataField dataField = field.getValue();

            // As of PPM 9.50, data fields coming from PPM can be either simple string, simple User, or list of Users.
            if (dataField == null) {
                fields.put(field.getKey(), null);
            } else if (DataField.DATA_TYPE.USER.equals(dataField.getType())) {
                // This is one or more user; however, in JIRA the user fields usually only take one user - or at least that's how we'll synch them.
                User user = null;
                if (dataField.isList()) {
                    // PPM Multi user field
                    List<User> users = (List<User>)dataField.get();
                    if (users != null && !users.isEmpty()) {
                        user = users.get(0);
                    }
                } else {
                    // Single user
                    user = (User)dataField.get();
                }

                if (user == null) {
                    fields.put(field.getKey(), null);
                } else {
                    // We need to retrieve the right Jira user matching the PPM user's email or username.
                    String jiraUsername = service.getJiraUsernameFromPpmUser(user);
                    fields.put(field.getKey(), jiraUsername == null ? null : JIRA_NAME_PREFIX + jiraUsername);
                }

            } else {
                // we consider it a single String value.
                fields.put(field.getKey(), dataField.get() == null ? null : dataField.get().toString());
            }
        }

        return fields;
    }

    @Override public AgileEntity createEntity(String agileProjectValue, String entityType, AgileEntity entity,
            ValueSet instanceConfigurationParameters)
    {
        JIRAService jiraService = JIRAServiceProvider.get(instanceConfigurationParameters);

        Map<String, String> fields = getFieldsFromAgileEntity(entity, jiraService);

        String issueKey = jiraService.createIssue(agileProjectValue, fields, null, entityType);

        return jiraService.getSingleAgileEntityIssue(agileProjectValue, issueKey);
    }

    @Override public List<AgileEntity> getEntities(String agileProjectValue, String entityType,
            ValueSet instanceConfigurationParameters, Set<String> entityIds, Date modifiedSinceDate)
    {

        if (entityIds == null || entityIds.isEmpty()) {
            return new ArrayList<AgileEntity>();
        }

        List<JIRAAgileEntity> jiraEntities = JIRAServiceProvider.get(instanceConfigurationParameters).getAgileEntityIssuesModifiedSince(entityIds, modifiedSinceDate);

        List<AgileEntity> entities = new ArrayList<>(jiraEntities.size());
        entities.addAll(jiraEntities);

        return entities;
    }

    @Override public AgileEntity getEntity(String agileProjectValue, String entityType,
            ValueSet instanceConfigurationParameters, String entityId)
    {
        if (StringUtils.isBlank(entityId)) {
            return null;
        }

        return JIRAServiceProvider.get(instanceConfigurationParameters).getSingleAgileEntityIssue(agileProjectValue, entityId);
    }

}
