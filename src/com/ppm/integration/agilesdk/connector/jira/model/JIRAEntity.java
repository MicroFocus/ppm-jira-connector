
package com.ppm.integration.agilesdk.connector.jira.model;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.ppm.integration.agilesdk.connector.jira.JIRAConstants.NULL_VALUE;

public abstract class JIRAEntity extends JIRABase {

    private String name;

    private String key;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public static <T extends JIRAEntity> T generateFromJSonObject(JSONObject obj, Class<T> clazz) {

        T entity = null;

        try {
            entity = (T)clazz.newInstance();
            Map<String, Method> methods = new HashMap<>();
            for (Method m : clazz.getDeclaredMethods()) {
                String methodName = m.getName();
                if (!methodName.startsWith("set") || methodName.length() < 4) {
                    // We only need setters
                    continue;
                }
                String key = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);

                Object value = null;
                String parameterTypeName = m.getParameterTypes()[0].getSimpleName();

                if ("String".equals(parameterTypeName)) {
                    value = (obj.has(key) && !obj.isNull(key)) ? obj.getString(key) : null;
                }

                if ("boolean".equalsIgnoreCase(parameterTypeName)) {
                    value = (obj.has(key) && !obj.isNull(key)) ? obj.getBoolean(key) : null;
                }

                if (value != null) {
                    m.invoke(entity, value);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error when reading Version information", e);
        }

        return entity;
    }

}
