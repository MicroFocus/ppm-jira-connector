package com.ppm.integration.agilesdk.connector.jira;

import static com.ppm.integration.agilesdk.connector.jira.JIRAConstants.JIRA_NAME_PREFIX;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.hp.ppm.integration.model.AgileEntityFieldValue;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAAgileEntity;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAFieldInfo;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAIssueType;
import com.ppm.integration.agilesdk.connector.jira.service.JIRAService;
import com.ppm.integration.agilesdk.dm.DataField;
import com.ppm.integration.agilesdk.dm.DataField.DATA_TYPE;
import com.ppm.integration.agilesdk.dm.ListNode;
import com.ppm.integration.agilesdk.dm.ListNodeField;
import com.ppm.integration.agilesdk.dm.RequestIntegration;
import com.ppm.integration.agilesdk.dm.StringField;
import com.ppm.integration.agilesdk.dm.User;
import com.ppm.integration.agilesdk.model.AgileEntity;
import com.ppm.integration.agilesdk.model.AgileEntityFieldInfo;
import com.ppm.integration.agilesdk.model.AgileEntityInfo;

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
            // - option type with non-empty allowedValues (single select)

            if ("string".equals(field.getType())
                    || "number".equals(field.getType())
                    || "user".equals(field.getType())
                    || "priority".equals(field.getType())
                    || "array".equals(field.getType()) 
                    || "option".equals(field.getType())) {

                if (field.isList() && !"user".equals(field.getType()) && !("array".equals(field.getType()) && "user".equals(field.getItems())) && (field.getAllowedValues()== null || field.getAllowedValues().isEmpty())) {
                    // We only allow to select lists that have some static value options or are users lists.
                    continue;
                }

                AgileEntityFieldInfo fieldInfo = new AgileEntityFieldInfo();
                fieldInfo.setId(field.getKey());
                fieldInfo.setLabel(field.getName());
                fieldInfo.setListType(field.isList());
                if("array".equals(field.getType()) && "user".equals(field.getItems())){
                	fieldInfo.setFieldType(DATA_TYPE.USER.name());
                } else {
                    fieldInfo.setFieldType(getAgileFieldtype(field.getType()));
                }
                if(field.getType().equalsIgnoreCase(JIRAConstants.KEY_FIELD_TYPE_ARRAY)){
                	fieldInfo.setMultiValue(true);
                }
                fieldsInfo.add(fieldInfo);
            }
        }

        return fieldsInfo;
    }
    
    private String getAgileFieldtype(String fieldType) {
        if(fieldType == null) {
            return "";
        }
        
        switch(fieldType) {
            case JIRAConstants.KEY_FIELD_TYPE_STRING:
                return DATA_TYPE.STRING.name();
            case JIRAConstants.KEY_FIELD_TYPE_NUMBER:
                return DATA_TYPE.INTEGER.name();
            case JIRAConstants.KEY_FIELD_TYPE_USER:
                return DATA_TYPE.USER.name();
            case JIRAConstants.KEY_FIELD_TYPE_OPTION:
            case JIRAConstants.KEY_FIELD_TYPE_ARRAY:
            case JIRAConstants.KEY_FIELD_TYPE_PRIORITY:
                return DATA_TYPE.ListNode.name();
            default :
                return DATA_TYPE.STRING.name();                
        }
    }

    @Override
    public List<AgileEntityFieldValue> getAgileEntityFieldsValueList(final String agileProjectValue,
            final String entityType, final ValueSet instanceConfigurationParameters, final String fieldName,
            final boolean isLogicalName)
    {

        List<JIRAFieldInfo> fields = new ArrayList(JIRAServiceProvider.get(instanceConfigurationParameters)
                .getFields(agileProjectValue, entityType).values());

        for (JIRAFieldInfo field : fields) {
            if (fieldName.equals(field.getKey())) {
                return field.getAllowedValues();
            }
        }

        // Field not found.
        return new ArrayList<AgileEntityFieldValue>();
    }


    @Override public AgileEntity updateEntity(String agileProjectValue, String entityType, AgileEntity entity,
            ValueSet instanceConfigurationParameters)
    {

        JIRAService jiraService = JIRAServiceProvider.get(instanceConfigurationParameters);
        Map<String, JIRAFieldInfo> fieldsInfo = new HashMap<>();

        if (!StringUtils.isBlank(entityType)) {
        	fieldsInfo = JIRAServiceProvider.get(instanceConfigurationParameters).getFields(agileProjectValue, entityType);
        }

        Map<String, String> fields = getFieldsFromAgileEntity(entity, fieldsInfo, jiraService);

        String issueKey = jiraService.updateIssue(agileProjectValue, entity.getId(), fields);

        return jiraService.getSingleAgileEntityIssue(agileProjectValue, entityType, issueKey);
    }

    private Map<String,String> getFieldsFromAgileEntity(AgileEntity entity, Map<String, JIRAFieldInfo> fieldsInfo, JIRAService service) {
        Map<String, String> fields = new HashMap<>();

        Iterator<Map.Entry<String, DataField>> fieldsIterator = entity.getAllFields();

        while (fieldsIterator.hasNext()) {
            Map.Entry<String, DataField> field = fieldsIterator.next();
            JIRAFieldInfo  fieldInfo = fieldsInfo.get(field.getKey()); 

            DataField dataField = field.getValue();
            if(dataField == null){
            	fields.put(field.getKey(), null);
            	continue;
            }
            
            if(fieldInfo.getType().equals(JIRAConstants.KEY_FIELD_TYPE_ARRAY) && fieldInfo.getItems().equals(JIRAConstants.KEY_FIELD_TYPE_USER)){
                User user = null;
                if (dataField.isList() && dataField.getType().equals(DATA_TYPE.USER)) {
                    // PPM Multi user field
                    List<User> users = (List<User>)dataField.get();
                	JSONArray tempArr = new JSONArray();
                    if (users != null && !users.isEmpty()) {
                        for(int i=0;i<users.size();i++){
                        	String jiraUsername = service.getJiraUsernameFromPpmUser(users.get(i));
                        	if(jiraUsername != null){
                        		try {
									JSONObject obj = new JSONObject();
									obj.put("name", jiraUsername);
									tempArr.put(obj);
								} catch (JSONException e) {
									throw new RuntimeException("Error when retrieve User json", e);
								}
                        	}
                        	
                            fields.put(field.getKey(), tempArr.toString());
                        }
                    } else {
                    	fields.put(field.getKey(), null);
                    }
                } else if(dataField.getType().equals(DATA_TYPE.USER)){
                    // Single user
                    user = (User)dataField.get();
                    if (user == null) {
                        fields.put(field.getKey(), null);
                    } else {
                        // We need to retrieve the right Jira user matching the PPM user's email or username.
                        String jiraUsername = service.getJiraUsernameFromPpmUser(user);
                        fields.put(field.getKey(), jiraUsername == null ? null : jiraUsername);
                    }
                } else if(dataField.getType().equals(DATA_TYPE.STRING) || dataField.getType().equals(DATA_TYPE.MEMO)){
                	JSONArray ary = new JSONArray();
                	JSONObject obj = new JSONObject();
                	String jiraUsername = "";
                	String str = dataField.get().toString();
                	User tempUser= new User();
                	tempUser.setEmail(str);
                	jiraUsername = service.getJiraUsernameFromPpmUser(tempUser);
                	if(jiraUsername != null){
                		try {
							obj.put("name", jiraUsername);
							ary.put(obj);
						} catch (JSONException e) {
							throw new RuntimeException("Error when generating create multi-user JSONArray Payload", e);
						}
                	}
                	fields.put(field.getKey(), ary.toString());
                }
            	
            } else if(fieldInfo.getType().equals(JIRAConstants.KEY_FIELD_TYPE_USER)){
            	String jiraUsername = "";
                if (dataField.isList() && dataField.getType().equals(DATA_TYPE.USER)) {
                    // PPM Multi user field
                    List<User> users = (List<User>)dataField.get();
                    if (users != null && !users.isEmpty()) {
                    	for(User temUser: users){
                    		String existJiraUsername = service.getJiraUsernameFromPpmUser(temUser);
                    		if(existJiraUsername != null){
                    			jiraUsername = existJiraUsername;
                        		break;
                    		}
                    	}
                    }
                } else if(dataField.getType().equals(DATA_TYPE.USER)){ // Single user
                    User user = (User)dataField.get();
                    if (user == null) {
                        fields.put(field.getKey(), null);
                    } else {
                    	jiraUsername = service.getJiraUsernameFromPpmUser(user);
                    }
                } else if(dataField.getType().equals(DATA_TYPE.STRING) || dataField.getType().equals(DATA_TYPE.MEMO)){
                	String str = dataField.get().toString();
                	User user = new User();
                	user.setEmail(str);
                	jiraUsername = service.getJiraUsernameFromPpmUser(user);
                }

                if (jiraUsername != null && jiraUsername.isEmpty()) {
                    fields.put(field.getKey(), null);
                } else {                   
                    fields.put(field.getKey(), jiraUsername == null ? null : JIRA_NAME_PREFIX + jiraUsername);
                }
            } else if(fieldInfo.getType().equals(JIRAConstants.KEY_FIELD_TYPE_OPTION) || fieldInfo.getType().equals(JIRAConstants.KEY_FIELD_TYPE_PRIORITY)) {
            	if(DataField.DATA_TYPE.ListNode.equals(dataField.getType())){
            		ListNodeField listNodeField = (ListNodeField) dataField;
                	JSONObject complexObj = new JSONObject();
                	if(dataField.get() != null){
                		try {
                			if(listNodeField.get().getId() != null && !listNodeField.get().getId().isEmpty()){
        						complexObj.put("id", listNodeField.get().getId());
        						fields.put(field.getKey(), complexObj.toString());
                			}else {
                				//JIRAFieldInfo  fieldInfo = fieldsInfo.get(field.getKey());
                				List<AgileEntityFieldValue> allowdValues = fieldInfo.getAllowedValues();
                				complexObj.put("id", -1);
                				fields.put(field.getKey(), complexObj.toString());
                				for(int i=0;i<allowdValues.size();i++){
                					if(allowdValues.get(i).getName().equalsIgnoreCase(listNodeField.get().getName())){
                						complexObj.put("id", allowdValues.get(i).getId());
                						fields.put(field.getKey(), complexObj.toString());
                					}
                				}
                			}
                			
    					} catch (JSONException e) {
    						throw new RuntimeException("Error when generating create Issue JSON Payload", e);
    					}
                	} else {
                		fields.put(field.getKey(), null);
                	}
            	} else if(DataField.DATA_TYPE.STRING.equals(dataField.getType())){
            		String ppmValue = dataField.get() == null ? null : dataField.get().toString();
            	}
            	
            } else if(fieldInfo.getType().equals(JIRAConstants.KEY_FIELD_TYPE_ARRAY)) {
            	if(dataField != null && DataField.DATA_TYPE.ListNode.equals(dataField.getType())){
            		ListNodeField listNodeField = (ListNodeField) dataField;
            		JSONArray complexObj = new JSONArray();
            		
            		if(dataField.get() != null){
            			String[] nameArr = listNodeField.get().getName().split(JIRAConstants.SPLIT_CHAR);
                        String[] idArr = listNodeField.get().getId().split(JIRAConstants.SPLIT_CHAR);                         
                        
                        JSONArray tempArr = new JSONArray();
                        try {
							if(listNodeField.get().getId().isEmpty()){
								for (int i = 0; i < nameArr.length; i++) {
							        List<AgileEntityFieldValue> allowdValues = fieldInfo.getAllowedValues();
									for(int j=0;j<allowdValues.size();j++){
										if(allowdValues.get(j).getName().equalsIgnoreCase(nameArr[i])){
									        JSONObject tempObj = new JSONObject(); 
											tempObj.put("id", allowdValues.get(j).getId());
											tempArr.put(tempObj);
										}
									}
								}
									
							} else {
								for (int i = 0; i < idArr.length; i++) {
									JSONObject tempObj = new JSONObject(); 
									tempObj.put("id", idArr[i]);
									tempArr.put(tempObj);
								}
							}
							fields.put(field.getKey(), tempArr.toString());
						} catch (JSONException e) {
							throw new RuntimeException("Error when generating create Issue JSON Payload", e);
						}
            		} else {
            			fields.put(field.getKey(), null);
            		}
            	}
            } else if(fieldInfo.getType().equals(JIRAConstants.KEY_FIELD_TYPE_STRING) && DataField.DATA_TYPE.USER.equals(dataField.getType())){
        		String value = "";
            	if(dataField.isList()){
            		List<User> users = (List<User>)dataField.get();
            		for(User user : users){
            			if(value.isEmpty()){
            				value = user.getFullName();
            			} else {
                			value = value + ";" + user.getFullName();
            			}
            		}
            	} else {
            		User user = (User)dataField.get();
            		value = user.getFullName();
            	}
            	fields.put(field.getKey(), value);
            }else {
            	fields.put(field.getKey(), dataField.get() == null ? null : dataField.get().toString());
            }
        }

        return fields;
    }

    @Override public AgileEntity createEntity(String agileProjectValue, String entityType, AgileEntity entity,
            ValueSet instanceConfigurationParameters)
    {
        JIRAService jiraService = JIRAServiceProvider.get(instanceConfigurationParameters);
        
        Map<String, JIRAFieldInfo> fieldsInfo = new HashMap<>();

        if (!StringUtils.isBlank(entityType)) {
        	fieldsInfo = JIRAServiceProvider.get(instanceConfigurationParameters).getFields(agileProjectValue, entityType);
        }
        
        Map<String, String> fields = getFieldsFromAgileEntity(entity, fieldsInfo, jiraService);

        String issueKey = jiraService.createIssue(agileProjectValue, fields, null, entityType);

        return jiraService.getSingleAgileEntityIssue(agileProjectValue, entityType, issueKey);
    }

    @Override public List<AgileEntity> getEntities(String agileProjectValue, String entityType,
            ValueSet instanceConfigurationParameters, Set<String> entityIds, Date modifiedSinceDate)
    {

        if (entityIds == null || entityIds.isEmpty()) {
            return new ArrayList<AgileEntity>();
        }
        
        Map<String, JIRAFieldInfo> fieldsInfo = new HashMap<>();

        if (!StringUtils.isBlank(entityType)) {
        	fieldsInfo = JIRAServiceProvider.get(instanceConfigurationParameters).getFields(agileProjectValue, entityType);
        }

        List<JIRAAgileEntity> jiraEntities = JIRAServiceProvider.get(instanceConfigurationParameters).getAgileEntityIssuesModifiedSince(fieldsInfo, entityIds, modifiedSinceDate);

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

        return JIRAServiceProvider.get(instanceConfigurationParameters).getSingleAgileEntityIssue(agileProjectValue, entityType, entityId);
    }

}
