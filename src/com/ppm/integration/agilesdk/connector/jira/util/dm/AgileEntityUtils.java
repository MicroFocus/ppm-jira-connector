package com.ppm.integration.agilesdk.connector.jira.util.dm;

import com.hp.ppm.user.model.User;
import com.ppm.integration.agilesdk.connector.jira.JIRAConstants;
import com.ppm.integration.agilesdk.connector.jira.JIRAIntegrationConnector;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAAgileEntity;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAFieldInfo;
import com.ppm.integration.agilesdk.dm.ListNode;
import com.ppm.integration.agilesdk.dm.ListNodeField;
import com.ppm.integration.agilesdk.dm.MultiUserField;
import com.ppm.integration.agilesdk.dm.StringField;
import com.ppm.integration.agilesdk.dm.UserField;
import com.ppm.integration.agilesdk.provider.Providers;
import com.ppm.integration.agilesdk.provider.UserProvider;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This utility class should contain all dm request integration model class to avoid hard dependencies in JIRAServiceProvider class.
 */
public class AgileEntityUtils {

    public static JIRAAgileEntity getAgileEntityFromIssueJSon(Map<String, JIRAFieldInfo> fieldsInfo, JSONObject issueObj, String baseUrl) {

        JIRAAgileEntity entity = new JIRAAgileEntity();

        try {
            JSONObject fieldsObj = issueObj.getJSONObject("fields");

            for (String fieldKey : JSONObject.getNames(fieldsObj)) {

                Object fieldContents = fieldsObj.get(fieldKey);
                
                JIRAFieldInfo fieldInfo = fieldsInfo.get(fieldKey);
                
				if (fieldInfo != null && (fieldInfo.getType().equalsIgnoreCase(JIRAConstants.KEY_FIELD_TYPE_OPTION) || fieldInfo.getType().equalsIgnoreCase(JIRAConstants.KEY_FIELD_TYPE_PRIORITY))) {
					if (fieldContents != JSONObject.NULL) {
						JSONObject field = (JSONObject) fieldContents;
						ListNode listNode = new ListNode();
						listNode.setId(field.has("id") ? field.getString("id") : null);
						if(fieldInfo.getType().equalsIgnoreCase(JIRAConstants.KEY_FIELD_TYPE_PRIORITY)){
							listNode.setName(field.has("name") ? field.getString("name") : null);
						} else {
							listNode.setName(field.has("value") ? field.getString("value") : null);
						}
						ListNodeField listNodeField = new ListNodeField();
						listNodeField.set(listNode);
						entity.addField(fieldKey, listNodeField);
					} else {
						entity.addField(fieldKey, null);
					}

				} else if (fieldInfo != null
						&& fieldInfo.getType().equalsIgnoreCase(JIRAConstants.KEY_FIELD_TYPE_ARRAY) && fieldInfo.getItems().equalsIgnoreCase(JIRAConstants.KEY_FIELD_TYPE_USER)) {
					if (fieldContents != JSONObject.NULL) {
						JSONArray fieldList = (JSONArray) fieldContents;
						MultiUserField muf = new MultiUserField();
		                List<com.ppm.integration.agilesdk.dm.User> users = new ArrayList<>();
						for (int i = 0; i < fieldList.length(); i++) {
							JSONObject field = (JSONObject) fieldList.get(i);
							User ppmUser = getPpmUserIdFromJiraUserField(field);
							if(ppmUser != null){
								 com.ppm.integration.agilesdk.dm.User user = new com.ppm.integration.agilesdk.dm.User();
					                user.setUserId(ppmUser.getUserId());
					                user.setUsername(ppmUser.getUserName());
					                user.setFullName(ppmUser.getFullName());
					                users.add(user);
					                
							} else {
								com.ppm.integration.agilesdk.dm.User user = new com.ppm.integration.agilesdk.dm.User();
				                user.setFullName(field.has("displayName") ? field.getString("displayName") : "");
				                users.add(user);
							}						
						}
						if(users.size() > 0 ){
							muf.set(users);
			                entity.addField(fieldKey, muf);
						} else {
							entity.addField(fieldKey, null);
						}
					} 
					else {
						entity.addField(fieldKey, null);
					}
					
					
				}else if (fieldInfo != null
						&& fieldInfo.getType().equalsIgnoreCase(JIRAConstants.KEY_FIELD_TYPE_ARRAY)) {
					if (fieldContents != JSONObject.NULL) {
						JSONArray fieldList = (JSONArray) fieldContents;
						String ids = "";
						String names = "";
						for (int i = 0; i < fieldList.length(); i++) {
						    //Fix QCCR1L68982,if an element in the fieldlist which is NOT a JSONObject,it will be ignored 
							if(!(fieldList.get(i) instanceof JSONObject)) {				    					    	
				    			continue;
				    		}
							JSONObject field = (JSONObject) fieldList.get(i);
							String id = field.has("id") ? field.getString("id") : null;
							String value = field.has("value") ? field.getString("value") : null;
							if (id != null && value != null) {
								if(ids == ""){
									ids = id;
									names = value;
								} else {
									ids = ids + JIRAConstants.SPLIT_CHAR + id;
									names = names + JIRAConstants.SPLIT_CHAR + value;
								}
							}
						}
						if (ids != null && !ids.isEmpty()) {
							ListNode listNode = new ListNode();
							listNode.setId(ids);
							listNode.setName(names);

							ListNodeField listNodeField = new ListNodeField();
							listNodeField.set(listNode);

							entity.addField(fieldKey, listNodeField);
						}
					} else {
						entity.addField(fieldKey, null);
					}

				} else if (fieldContents instanceof JSONObject) {					
					if (fieldContents != JSONObject.NULL) {
						JSONObject field = (JSONObject) fieldContents;
						addJSONObjectFieldToEntity(fieldKey, field, entity);
					} else {
						entity.addField(fieldKey, null);
					}

				} else if (fieldContents instanceof JSONArray) {
					if (fieldContents != JSONObject.NULL) {
                    StringField sf = getStringFieldFromJsonArray((JSONArray)fieldContents);
                    entity.addField(fieldKey, sf);
					} else {
						entity.addField(fieldKey, null);
					}

                } else {
                	if (fieldContents != JSONObject.NULL) {
                    // If it's not an object nor an array, it's a string
                    StringField sf = new StringField();
                    sf.set(fieldContents.toString());
                    entity.addField(fieldKey, sf);
                	} else {
						entity.addField(fieldKey, null);
					}
                }
            }

            if (fieldsObj.has("updated") && !fieldsObj.isNull("updated")) {
                String updated = fieldsObj.getString("updated");


                if (!StringUtils.isBlank(updated)) {

                    // JIRA will return dates with timezone offset not including colon (for example: +0800. However, XML Spec requires a colon, so let's add it.
                    if (updated.length() == 28) {
                        updated = updated.substring(0, 26) + ":" + updated.substring(26);
                    }

                    entity.setLastUpdateTime(javax.xml.bind.DatatypeConverter.parseDateTime(updated).getTime());
                }
            }

            if (issueObj.has("key") && !issueObj.isNull("key")) {
                entity.setId(issueObj.getString("key"));
            }

            entity.setEntityUrl(baseUrl + "/browse/"+entity.getId());

        } catch (JSONException e) {
            throw new RuntimeException("Error while parsing Issue JSon", e);
        }

        return entity;

    }

    private static StringField getStringFieldFromJsonArray(JSONArray jsonArray) throws JSONException {


        List<String> values = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            Object arrayValue = jsonArray.get(i);
            if (arrayValue instanceof JSONObject) {
                values.add(getValueFromJsonObject((JSONObject)arrayValue));
            } else if (arrayValue instanceof String) {
                values.add((String)arrayValue);
            }
            // We don't support arrays in arrays.
        }

        StringField sf = new StringField();
        sf.set(StringUtils.join(values, ";"));

        return sf;
    }

    private static String getValueFromJsonObject(JSONObject jsonObject) throws JSONException {
        return jsonObject.has("name") ? jsonObject.getString("name"):"";
    }

    private static void addJSONObjectFieldToEntity(String fieldKey, JSONObject field, JIRAAgileEntity entity) throws JSONException {

        if (isUserField(field)) {
        	User ppmUser = getPpmUserIdFromJiraUserField(field);
        	com.ppm.integration.agilesdk.dm.User user = new com.ppm.integration.agilesdk.dm.User();
        	UserField userField = new UserField();
            if (ppmUser == null) {
            	user.setFullName(field.has("displayName")?field.getString("displayName"):"");
            	userField.set(user);
                entity.addField(fieldKey, userField);
            } else {                
                user.setUserId(ppmUser.getUserId());
                user.setUsername(ppmUser.getUserName());
                user.setFullName(ppmUser.getFullName());
                userField.set(user);
                entity.addField(fieldKey, userField);
            }

        } else {
            // Standard field.
            String name = field.has("name") ? field.getString("name") : "";
            StringField sf = new StringField();
            // Since only strings are supported, we only set the Name, not the key. That will be for when CodeMeaning will be supported.
            sf.set(name);
            entity.addField(fieldKey, sf);
        }
    }

    private static boolean isUserField(JSONObject field) throws JSONException {
        return field != null && field.has("self") && field.has("emailAddress") && field.getString("self").contains("/user?");
    }

    private static User getPpmUserIdFromJiraUserField(JSONObject field) throws JSONException {
        String email = field.getString("emailAddress");

        UserProvider provider = Providers.getUserProvider(JIRAIntegrationConnector.class);
        User user = provider.getByEmail(email);

        if (user != null) {
            return user;
        }

        return null;
    }
}
