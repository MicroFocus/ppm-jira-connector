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
        return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsQAAA7EAZUrDhsAAARXSURBVFhHrZdrbFVFEMfXYDREqRFsKVJRQLBoi/IIoqYIBhFQSh9QQGl5FMrbByUaCBT4ABgpJE1EAh9UECGYoAg0EZtQiYBVa0tSAqWUx4UiUvARX1Qj/TuzO5eee/bec+4h/JJN7tmds/s/s7MzexVukjNNV9AjcwkShxej4tvj0hucmxagekyCSpsK9Xgh1L0vSW9wPAU0hH6CenQK1H2ZeK10p/QaeFHVfybUgCKopCw0Nf8qI8AHew5D3Z8L9eAE7D9yTHqj4yngzqfmQj1GX8kLdcpEXeNFGQEKSt4nYWOhuuQgNbdEeg1aXL8Zxjuds6U3Op4CVGqBmYS/kr7owPf1MmL44UQIldUn5cnwx1/XoDqOMe+wcJ/t8RSwtfwbqHtehErOQbfRb0mvP9mLNupt48Vfd22dG98gvNbyL7m+SZ7i59yPV9H8y+/yFBtLwKGjjfLr1lNRZR9XS4Bq95ze78FT12DTroO0py0yEpz/rl/Hji++w8gFZVAPTYRqP0J7xoktgIOHA4+j/+GXdfTn0J46Od3UjK9rT6H25HndqurOoKY+JKOGtVv3QyWMhupJc/BR5lORbJ8IS0BW8QaoPvQCCwlHcpdcGTUkDl+o+/ic6/bAeKiUcTJq0HM8QYuG50mfjpRRdiBbAiqr62nCvLYXudEXDC0qFQvgYE2DPv83xsm967Z9KaPAm2W7yHuvRM5Bniglr7ixBDC3DZzVdv7DjeLi0wM1YgFkFL4D1XuyTscdMhZILxC69LM+tjeyJLd+9Ju8FI2oAsoP1UF1HRcpgCckEVd/+1OsgNsHzdF2TlTvfHK9SzzVjeWb9ohFJFEFMGl5K0zwOCdir/SaLBaG1tZW+QW0f3qeKVDOd/pO1+JjEVMAo7pTxXMGErf0aTqi3SSPWGRSt9OWvZaU7ZmQPAUcO30RKpEKjnM/uUmBCtMra6mJB6cNN4qFLXuPiFV0PAUwW/ZRPaCKZk1OIpKeL0b6hJVQj9C+u8e75WH2qm0yS2x8BTAlGz+nYKNz716EY4K3xN1PQffC/DJ525u4BDDz3t5uEo57MXej8z6oYLW85U/cApj8ZXQJIddGXZhbn4JAizOBBDBaBKdf9+J0WjiBBSWwACZzIdWL7lTd3CKo76PyKrGKj0ACPnQcqYwZa+18z8eVKt7fLf+IlT9xCxjzxrtQd4/E0vd2Sw90dbOyZdo0pAS4vsUlgK/cnNH0F1JOcHpCC3Dn/pTx2P1VrVh4E5eAJ6esMdmPJ2cRdB0PX0AaQpdNonJmS8oPdwyeq8f98BXA1yprATpuzvvBxMWb7TpAXqg+fk4sYuMrYP3HFXawUQk+cfaSWBj0PyGnSBLEf1788BUwbNa6Nvdz61uIhGdfldE2hhW57GgbEobYdm58Bdz1zPzI21FqPuastovMhk8qIz3F3qBrmx++AjpznedLRXhiKjSfVdoR3nih2SSn8DbwLZhixRvgf/NC1kbNgmxHAAAAAElFTkSuQmCC";
    }

    @Override public List<Field> getDriverConfigurationFields() {
        return Arrays.asList(new Field[] {
                new PlainText(JIRAConstants.KEY_BASE_URL, "BASE_URL", "", true),
                new PlainText(JIRAConstants.KEY_PROXY_HOST, "PROXY_HOST", "", false),
                new PlainText(JIRAConstants.KEY_PROXY_PORT, "PROXY_PORT", "", false),

        });
    }

    @Override public List<FunctionIntegration> getIntegrations() {
        return Arrays.asList(new FunctionIntegration[] {new JIRAWorkPlanIntegration(), new JIRATimeSheetIntegration()});
    }
}
