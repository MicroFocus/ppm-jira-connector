
package com.ppm.integration.agilesdk.connector.jira.model;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.hp.ppm.integration.model.AgileEntityFieldValue;

public class JIRAFieldInfo {

    private String key;

    private String name;

    private boolean isList = false;

    private String type;

    private String system;

    private String items;

    private List<AgileEntityFieldValue> allowedValues = null;


    public static JIRAFieldInfo fromJSONObject(JSONObject obj, String key) {
        try {
            JIRAFieldInfo fieldInfo = new JIRAFieldInfo();
            fieldInfo.setKey(key);
            fieldInfo.setName(obj.getString("name"));

            if (obj.has("allowedValues")) {
                JSONArray allowedValues = obj.getJSONArray("allowedValues");
                if (allowedValues != null) {
                    fieldInfo.setAllowedValues(new ArrayList<AgileEntityFieldValue>(allowedValues.length()));

                    for (int i = 0 ; i < allowedValues.length() ; i++) {
                        AgileEntityFieldValue listValue = new AgileEntityFieldValue();
                        JSONObject listValueObj = allowedValues.getJSONObject(i);
                        String value = "?";
                        if (listValueObj.has("name")) {
                            value = listValueObj.getString("name");
                        } else if (listValueObj.has("value")) {
                            value = listValueObj.getString("value");
                        }
                        listValue.setId(listValueObj.getString("id"));
                        listValue.setName(value);
                        fieldInfo.getAllowedValues().add(listValue);
                    }
                }
            }

            JSONObject schema = obj.getJSONObject("schema");

                if (schema != null) {
                    if (schema.has("type")) {
                        fieldInfo.setType(schema.getString("type"));
                    }
                    if (schema.has("system")) {
                        fieldInfo.setSystem(schema.getString("system"));
                    }
                    if (schema.has("items")) {
                        fieldInfo.setItems(schema.getString("items"));
                    }
                }

            if (fieldInfo.getAllowedValues() != null || "array".equals(fieldInfo.getType())
                    || "option".equals(fieldInfo.getType()) || "priority".equals(fieldInfo.getType())) {
                fieldInfo.setList(true);
            }

            return fieldInfo;
        } catch (JSONException e) {
            throw new RuntimeException("Error while reading JSon definition of Issue Type", e);
        }
    }

    public static JIRAFieldInfo fromJSONFieldObject(JSONObject field) {
        try {
            JIRAFieldInfo fieldInfo = new JIRAFieldInfo();
            fieldInfo.setKey(field.getString("id"));
            fieldInfo.setName(field.getString("name"));

            if (field.has("allowedValues")) { // This should not be there when reading from /field REST API...
                JSONArray allowedValues = field.getJSONArray("allowedValues");
                if (allowedValues != null) {
                    fieldInfo.setAllowedValues(new ArrayList<AgileEntityFieldValue>(allowedValues.length()));

                    for (int i = 0 ; i < allowedValues.length() ; i++) {
                        AgileEntityFieldValue listValue = new AgileEntityFieldValue();
                        JSONObject listValueObj = allowedValues.getJSONObject(i);
                        String value = "?";
                        if (listValueObj.has("name")) {
                            value = listValueObj.getString("name");
                        } else if (listValueObj.has("value")) {
                            value = listValueObj.getString("value");
                        }
                        listValue.setId(listValueObj.getString("id"));
                        listValue.setName(value);
                        fieldInfo.getAllowedValues().add(listValue);
                    }
                }
            }

            JSONObject schema = field.getJSONObject("schema");

            if (schema != null) {
                if (schema.has("type")) {
                    fieldInfo.setType(schema.getString("type"));
                }
                if (schema.has("system")) {
                    fieldInfo.setSystem(schema.getString("system"));
                }
                if (schema.has("items")) {
                    fieldInfo.setItems(schema.getString("items"));
                }
            }

            if (fieldInfo.getAllowedValues() != null || "array".equals(fieldInfo.getType())
                    || "option".equals(fieldInfo.getType()) || "priority".equals(fieldInfo.getType())) {
                fieldInfo.setList(true);
            }

            return fieldInfo;
        } catch (JSONException e) {
            throw new RuntimeException("Error while reading JSon definition of Field", e);
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isList() {
        return isList;
    }

    public void setList(boolean list) {
        isList = list;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getItems() {
        return items;
    }

    public void setItems(String items) {
        this.items = items;
    }

    public List<AgileEntityFieldValue> getAllowedValues() {
        return allowedValues;
    }

    public void setAllowedValues(List<AgileEntityFieldValue> allowedValues) {
        this.allowedValues = allowedValues;
    }
}
