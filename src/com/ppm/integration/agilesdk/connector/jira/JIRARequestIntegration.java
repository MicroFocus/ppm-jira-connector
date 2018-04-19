package com.ppm.integration.agilesdk.connector.jira;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAFieldInfo;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAIssueType;
import com.ppm.integration.agilesdk.dm.*;
import com.ppm.integration.agilesdk.model.*;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class JIRARequestIntegration extends RequestIntegration {

    private JIRAServiceProvider service = new JIRAServiceProvider().useAdminAccount();

    @Override public List<AgileEntityInfo> getAgileEntitiesInfo(String agileProjectValue,  ValueSet instanceConfigurationParameters) {

        List<JIRAIssueType> issueTypes = service.get(instanceConfigurationParameters).getProjectIssueTypes(agileProjectValue);

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

        List<JIRAFieldInfo> fields = service.get(instanceConfigurationParameters).getFields(agileProjectValue, entityType);

        for (JIRAFieldInfo field: fields) {

            // We support following JIRA fields types to map:
            // - string
            // - number
            // - array type with non-empty allowedValues
            // - priority (provided that allowedValues is non-empty)

            if ("string".equals(field.getType())
                    || "number".equals(field.getType())
                    || "priority".equals(field.getType())
                    || ("array".equals(field.getType()))) {

                if (field.isList() && (field.getAllowedValues()== null || field.getAllowedValues().isEmpty())) {
                    // We only allow to select lists that have some value options.
                    continue;
                }

                AgileEntityFieldInfo fieldInfo = new AgileEntityFieldInfo();
                fieldInfo.setId(field.getKey());
                fieldInfo.setLabel(field.getName());
                fieldInfo.setListType(field.isList());
                fieldsInfo.add(fieldInfo);
            }
        }

        return fieldsInfo;
    }

    @Override public List<AgileEntityField> getAgileEntityFieldValueList(String agileProjectValue, String entityType, String listIdentifier,
            ValueSet instanceConfigurationParameters)
    {
        List<JIRAFieldInfo> fields = service.get(instanceConfigurationParameters).getFields(agileProjectValue, entityType);

        for (JIRAFieldInfo field : fields) {
            if (listIdentifier.equals(field.getKey())) {
                return field.getAllowedValues();
            }
        }

        // Field not found.
        return new ArrayList<AgileEntityField>();
    }

    @Override public AgileEntity updateEntity(String agileProjectValue, String entityType, AgileEntity entity,
            ValueSet instanceConfigurationParameters)
    {
        Map<String, String> fields = getFieldsFromAgileEntity(entity);

        JIRAServiceProvider.JIRAService jiraService = service.get(instanceConfigurationParameters);

        String issueKey = jiraService.updateIssue(agileProjectValue, entity.getId(), fields);

        return jiraService.getSingleAgileEntityIssue(agileProjectValue, issueKey);
    }

    private Map<String,String> getFieldsFromAgileEntity(AgileEntity entity) {
        Map<String, String> fields = new HashMap<>();

        Iterator<Map.Entry<String, List<AgileEntityFieldValue>>> fieldsIterator = entity.getAllFields();

        while (fieldsIterator.hasNext()) {
            Map.Entry<String, List<AgileEntityFieldValue>> field = fieldsIterator.next();

            StringBuilder value = new StringBuilder("");

            for (AgileEntityFieldValue v : field.getValue()) {
                value.append(v.getValue());
            }

            fields.put(field.getKey(), value.toString());
        }

        return fields;
    }

    @Override public AgileEntity createEntity(String agileProjectValue, String entityType, AgileEntity entity,
            ValueSet instanceConfigurationParameters)
    {
        Map<String, String> fields = getFieldsFromAgileEntity(entity);

        JIRAServiceProvider.JIRAService jiraService = service.get(instanceConfigurationParameters);

        String issueKey = jiraService.createIssue(agileProjectValue, fields, null, entityType);

        return jiraService.getSingleAgileEntityIssue(agileProjectValue, issueKey);
    }

    @Override public List<AgileEntity> getEntities(String agileProjectValue, String entityType,
            ValueSet instanceConfigurationParameters, Set<String> entityIds, Date modifiedSinceDate)
    {

        if (entityIds == null || entityIds.isEmpty()) {
            return new ArrayList<AgileEntity>();
        }

        return service.get(instanceConfigurationParameters).getAgileEntityIssuesModifiedSince(entityIds, modifiedSinceDate);
    }

    @Override public AgileEntity getEntity(String agileProjectValue, String entityType,
            ValueSet instanceConfigurationParameters, String entityId)
    {
        if (StringUtils.isBlank(entityId)) {
            return null;
        }

        return service.get(instanceConfigurationParameters).getSingleAgileEntityIssue(agileProjectValue, entityId);
    }

}
