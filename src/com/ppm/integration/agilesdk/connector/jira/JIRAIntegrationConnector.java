package com.ppm.integration.agilesdk.connector.jira;


import com.ppm.integration.agilesdk.FunctionIntegration;
import com.ppm.integration.agilesdk.IntegrationConnector;
import com.ppm.integration.agilesdk.ui.*;

import java.util.Arrays;
import java.util.List;

/**
 * @author baijuy
 *         The connector provides the integration for ppm with Jira. The field
 *         InstanceName and BaseUrl are required and field Proxy is optional.
 *         Once the connector is saved, it can be used to integrate with
 *         workplan or timesheet.
 */
public class JIRAIntegrationConnector extends IntegrationConnector {

    @Override public String getExternalApplicationName() {
        return "Atlassian JIRA";
    }

    @Override public String getExternalApplicationVersionIndication() {
        return "7.2.6+";
    }

    @Override public String getConnectorVersion() {
        return "1.0";
    }

    @Override public String getTargetApplicationIcon() {
        return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAApBJREFUeNqkUs9PE0EUftPdttvttqVVW6gEf4ClBpuKGmM8EI1GOTQSPfoXGOViohdvJt5JMHrxoIkXbwRU8GgkGMPBiJVGKEqElgplodt2d9vu7K4z25baM5N8+97Oe983b948ZJom7Gex1if2DgDZAGwM+UOdgFCCOOcJuht5GYJ5ME2SaP4FQyfGAJxM1AX+W8O8i7137nR3f6T3YEhwO920wHK5Ki/9yl/6+j07oqjaM5L3oa2CJjng4x7dSsQGDd0QNnN52GzFvJFjAW/v0UDX1Myib7egQFOkIWB28RwzOjJ8crAolQRJKsL7p7G20h6OpSErOoRrl/sGp6Z/jKoVfYFs52xW1NAT8VNd/cViSZBlBWhjx1+nLMzM5qyU6BEH0FhVVYVIb6CfcvYqMHV8IdwphKRCce/EyU+yRRiKixDkNyC9UoBUSgKGZcDn84coh6S9qF/BwN2MDXhd1wFjHaRiGRS1AmejHNy+7ofUqgITHxWwsU6gj65pmKecVg8MjDDGSK1ikEoVMAwEiGHh8Z0eK/zk1SawTmGvOgOxiHJaAjrOiDuqItcYgXG4gWkk3njwx7IO3t/WUK1SUyiH+o0m4vn1NXGL473g4DwWEBmsuZdnLFC/uU9REEt5ymkJ6HhyfXk1TWqT7ZwX7E4PsA6+Na7Ep3sUWAN5ez2zTDmtK5hGrqYq48nZWd/A0NWYy9PhZjkBroz+tsKeQ8ctq0i78tKXuSSuKONk7HPtk2ga00o+i769fXM/HI2fCPYNHPAFI646cUfdWlkUN34upLVadQxx3mlodArRoWEOPyfX0MBUC2BW5DAZjJskdpGgpyG/RvCZPM0E4twbyNVB+HbQs3frAvtZ/wQYAIlLIa+ciRrSAAAAAElFTkSuQmCC";
    }

    @Override public List<Field> getDriverConfigurationFields() {
        return Arrays.asList(new Field[] {
                new PlainText(JIRAConstants.KEY_BASE_URL, "BASE_URL", "https://marchhead.atlassian.net", true),
                new PlainText(JIRAConstants.KEY_PROXY_HOST, "PROXY_HOST", "", false),
                new PlainText(JIRAConstants.KEY_PROXY_PORT, "PROXY_PORT", "", false),

        });
    }

    @Override public List<FunctionIntegration> getIntegrations() {
        return Arrays.asList(new FunctionIntegration[] {new JIRAWorkPlanIntegration(), new JIRATimeSheetIntegration()});
    }
}
