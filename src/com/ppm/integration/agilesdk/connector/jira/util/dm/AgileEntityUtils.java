package com.ppm.integration.agilesdk.connector.jira.util.dm;

import com.hp.ppm.user.model.User;
import com.ppm.integration.agilesdk.connector.jira.JIRAIntegrationConnector;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAAgileEntity;
import com.ppm.integration.agilesdk.dm.MultiUserField;
import com.ppm.integration.agilesdk.dm.StringField;
import com.ppm.integration.agilesdk.provider.Providers;
import com.ppm.integration.agilesdk.provider.UserProvider;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * This utility class should contain all dm request integration model class to avoid hard dependencies in JIRAServiceProvider class.
 */
public class AgileEntityUtils {

    public static JIRAAgileEntity getAgileEntityFromIssueJSon(JSONObject issueObj, String baseUrl) {

        JIRAAgileEntity entity = new JIRAAgileEntity();

        try {
            JSONObject fieldsObj = issueObj.getJSONObject("fields");

            for (String fieldKey : JSONObject.getNames(fieldsObj)) {

                if (fieldsObj.isNull(fieldKey)) {
                    // Null fields in JIRA are considered empty fields in PPM.
                    entity.addField(fieldKey, new StringField());
                    continue;
                }

                Object fieldContents = fieldsObj.get(fieldKey);

                if (fieldContents instanceof JSONObject) {
                    JSONObject field = (JSONObject)fieldContents;
                    addJSONObjectFieldToEntity(fieldKey, field, entity);

                } else if (fieldContents instanceof JSONArray) {
                    StringField sf = getStringFieldFromJsonArray((JSONArray)fieldContents);
                    entity.addField(fieldKey, sf);

                } else {
                    // If it's not an object nor an array, it's a string
                    StringField sf = new StringField();
                    sf.set(fieldContents.toString());
                    entity.addField(fieldKey, sf);
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
            Long ppmUserId = getPpmUserIdFromJiraUserField(field);
            if (ppmUserId == null) {
                entity.addField(fieldKey, null);
            } else {
                // PPM Only supports Multi User fields for now
                MultiUserField muf = new MultiUserField();
                com.ppm.integration.agilesdk.dm.User user = new com.ppm.integration.agilesdk.dm.User();
                user.setUserId(ppmUserId);
                List<com.ppm.integration.agilesdk.dm.User> users = new ArrayList<>(1);
                users.add(user);
                muf.set(users);
                entity.addField(fieldKey, muf);
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

    private static Long getPpmUserIdFromJiraUserField(JSONObject field) throws JSONException {
        String email = field.getString("emailAddress");

        UserProvider provider = Providers.getUserProvider(JIRAIntegrationConnector.class);
        User user = provider.getByEmail(email);

        if (user != null) {
            return user.getUserId();
        }

        return null;
    }
}
