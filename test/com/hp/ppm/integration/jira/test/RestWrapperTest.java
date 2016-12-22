package com.hp.ppm.integration.jira.test;

import static org.junit.Assert.fail;

import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.Resource;
import org.junit.Before;
import org.junit.Test;

import com.ppm.integration.agilesdk.connector.jira.rest.util.IRestConfig;
import com.ppm.integration.agilesdk.connector.jira.rest.util.JIRARestConfig;
import com.ppm.integration.agilesdk.connector.jira.rest.util.RestWrapper;

public class RestWrapperTest {
	private RestWrapper wrapper;
	private String uri =  "https://marchhead.atlassian.net/rest/api/2/user";
	
	@Before
	public void before() {
		wrapper = new RestWrapper();
	}
	
	
	
	@Test
	public void testRestWrapper() {
		fail("Not yet implemented");
	}

	@Test
	public void testRestWrapperIRestConfig() {
		fail("Not yet implemented");
	}

	@Test
	public void testCreateRestClient() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetResourceString() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetResourceIRestConfigString() {
		IRestConfig config = new JIRARestConfig();
		config.setBasicAuthorizaton("admin", "hpe1990");
		Resource resource = wrapper.getJIRAResource(config, uri);
		ClientResponse response = resource.get();
		System.out.println(response.getStatusCode());
		
	}

	@Test
	public void testGetRestClient() {
		fail("Not yet implemented");
	}

}
