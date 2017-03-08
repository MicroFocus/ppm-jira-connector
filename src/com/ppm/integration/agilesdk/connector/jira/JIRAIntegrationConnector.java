
package com.ppm.integration.agilesdk.connector.jira;

import java.util.Arrays;
import java.util.List;

import com.ppm.integration.agilesdk.FunctionIntegration;
import com.ppm.integration.agilesdk.IntegrationConnector;
import com.ppm.integration.agilesdk.ui.Field;
import com.ppm.integration.agilesdk.ui.PlainText;

/**
 * @author baijuy The connector provides the integration for ppm with Jira. The
 *         field InstanceName and BaseUrl are required and field Proxy is
 *         optional. Once the connector is saved, it can be used to integrate
 *         with workplan or timesheet.
 */
public class JIRAIntegrationConnector extends IntegrationConnector {

    @Override
    public String getExternalApplicationName() {
        return "Atlassian JIRA";
    }

    @Override
    public String getExternalApplicationVersionIndication() {
        return "7.2.6+";
    }

    @Override
    public String getConnectorVersion() {
        return "1.0";
    }

    @Override
    public String getTargetApplicationIcon() {
        return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsQAAA7EAZUrDhsAAAFtSURBVDhPY/wPBAwUACYoTTbAasCfv3/BdGDFXAaPghlg9p9//8A0OsAw4MSVhwysLJYMT998ZPA112BwM1Fl+P7zNwMrsyXD1mPXoaqQACgM0MGK3eegLARYsQdTDARQXFA3axsDs1UhAxsTI5i/cvc5htV7L4DZgpzsDFyOZQzF3WvAfDiAGgQHDKpJ/xm008BsCc+a/xzWxWA2g076fwaVRDAbGWAYUNC/7j+nXcl/XocyqMj//yKulf+5gfyElmVQEQTAmg7Uw9sYXrz9xKAiJczAxcnGcPHuCwZBbg6GhxvroSoQAGdCkvVtYPj26zcDCzMTgyg/N8OV5RVQGVSAMyE93tzAwA7U/PvPXwZTbTmoKBYAcgE6uHjn6X8GyUgwm9Ei/z+DYfb/IxfvgvnoAKsLSidvYmBSlGSYu+EYw75pOQzMwHAom7wZKosGoAahAAaTnP8MMtFQHpAPZDNopkJ5qGCgcyMDAwAu2lvvmsJuBwAAAABJRU5ErkJggg==";
    }

    @Override
    public List<Field> getDriverConfigurationFields() {
        return Arrays.asList(new Field[] {new PlainText(JIRAConstants.KEY_BASE_URL, "BASE_URL", "", true),
                new PlainText(JIRAConstants.KEY_PROXY_HOST, "PROXY_HOST", "", false),
                new PlainText(JIRAConstants.KEY_PROXY_PORT, "PROXY_PORT", "", false),

        });
    }

    @Override
    public List<FunctionIntegration> getIntegrations() {
        return Arrays.asList(new FunctionIntegration[] {new JIRAWorkPlanIntegration(), new JIRATimeSheetIntegration()});
    }

}
