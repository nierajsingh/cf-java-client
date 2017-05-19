/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cloudfoundry.client.lib.rest;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.AdditionalRestOperations;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.InstanceStats;
import org.cloudfoundry.client.lib.util.CloudEntityResourceMapper;
import org.cloudfoundry.client.lib.util.JsonUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

public class AdditionalRestOperationsImpl implements AdditionalRestOperations {

	private CloudEntityResourceMapper resourceMapper = new CloudEntityResourceMapper();

	private final RestTemplate restTemplate;

	private final CloudSpace sessionSpace;

	private final URL cloudControllerUrl;

	public AdditionalRestOperationsImpl(RestTemplate restTemplate, CloudSpace sessionSpace, URL cloudControllerUrl) {
		this.restTemplate = restTemplate;
		this.sessionSpace = sessionSpace;
		this.cloudControllerUrl = cloudControllerUrl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudfoundry.client.lib.rest.AdditionalRestOperations#
	 * getBasicApplications()
	 */
	@Override
	public List<CloudApplication> getBasicApplications() {
		List<CloudApplication> apps = new ArrayList<CloudApplication>();

		if (sessionSpace != null) {
			Map<String, Object> urlVars = new HashMap<String, Object>();
			String urlPath = "/v2";
			urlVars.put("space", sessionSpace.getMeta().getGuid());
			urlPath = urlPath + "/spaces/{space}";
			urlPath = urlPath + "/apps?inline-relations-depth=1";
			List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
			for (Map<String, Object> resource : resourceList) {
				CloudApplication app = mapBasicCloudApplication(resource);
				if (app != null) {
					apps.add(app);
				}
			}
		}
		return apps;
	}
	
	@Override
	public CloudApplication getApplicationWithoutInstances(String appName) {
		Map<String, Object> resource = getBasicApplicationResource(appName);
		if (resource == null) {
			throw new CloudFoundryException(HttpStatus.NOT_FOUND, "Not Found", "Application not found");
		}
		fillApplicationServiceBindings(resource);

		CloudApplication app = mapBasicCloudApplication(resource);
		fillApplicationUris(app);
		return app;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudfoundry.client.lib.rest.AdditionalRestOperations#
	 * getBasicApplication(java.lang.String)
	 */
	@Override
	public CloudApplication getBasicApplication(String appName) {
		return mapBasicCloudApplication(appName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.cloudfoundry.client.lib.rest.AdditionalRestOperations#stopApplication
	 * (java.lang.String)
	 */
	@Override
	public void stopApplicationUsingBasicApp(String appName) {
		CloudApplication app = mapBasicCloudApplication(appName);
		if (app.getState() != CloudApplication.AppState.STOPPED) {
			HashMap<String, Object> appRequest = new HashMap<String, Object>();
			appRequest.put("state", CloudApplication.AppState.STOPPED);
			restTemplate.put(getUrl("/v2/apps/{guid}"), appRequest, app.getMeta().getGuid());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cloudfoundry.client.lib.rest.AdditionalRestOperations#
	 * getApplicationStats(org.cloudfoundry.client.lib.domain.CloudApplication)
	 */
	@Override
	public ApplicationStats getApplicationStats(CloudApplication application) {
		return doGetApplicationStats(application.getMeta().getGuid(), application.getState());
	}
	
	@Override
	public List<String> getStacks() {
		List<String> stacks = new ArrayList<String>();
		String json = restTemplate.getForObject(getUrl("/v2/stacks"), String.class);
		if (json != null) {
			Map<String, Object> resource = JsonUtil.convertJsonToMap(json);
			if (resource != null) {
				List<Map<String, Object>> newResources = (List<Map<String, Object>>) resource.get("resources");
				if (newResources != null) {
					for (Map<String, Object> res : newResources) {
						String name = CloudEntityResourceMapper.getEntityAttribute(res, "name", String.class);
						if (name != null) {
							stacks.add(name);
						}
					}
				}
			}
		}
		return stacks;
	}

	//
	// HELPER METHODS
	//

	/*
	 * Map application information into a CloudApplication type
	 */

	protected CloudApplication mapBasicCloudApplication(String appName) {
		Map<String, Object> resource = getBasicApplicationResource(appName);
		return mapBasicCloudApplication(resource);
	}

	protected CloudApplication mapBasicCloudApplication(Map<String, Object> resource) {
		CloudApplication cloudApp = null;
		if (resource != null) {
			cloudApp = resourceMapper.mapResource(resource, CloudApplication.class);
		}
		return cloudApp;
	}

	protected Map<String, Object> getBasicApplicationResource(String appName) {
		Map<String, Object> urlVars = new HashMap<String, Object>();
		String urlPath = "/v2";

		if (sessionSpace != null) {
			urlVars.put("space", sessionSpace.getMeta().getGuid());
			urlPath = urlPath + "/spaces/{space}";
		}
		urlVars.put("q", "name:" + appName);
		urlPath = urlPath + "/apps?inline-relations-depth=1&q={q}";

		List<Map<String, Object>> allResources = getAllResources(urlPath, urlVars);
		if (!allResources.isEmpty()) {
			Map<String, Object> foundResource = allResources.get(0);
			fillApplicationStack(foundResource);
			return foundResource;
		}
		return null;
	}

	/*
	 * Fill in additional information for the application
	 */

	protected Map<String, Object> fillApplicationServiceBindings(Map<String, Object> resource) {
		fillInEmbeddedResource(resource, "service_bindings", "service_instance");
		return resource;
	}

	protected Map<String, Object> fillApplicationStack(Map<String, Object> resource) {
		fillInEmbeddedResource(resource, "stack");
		return resource;
	}

	protected List<String> findApplicationUris(UUID appGuid) {
		String urlPath = "/v2/apps/{app}/routes?inline-relations-depth=1";
		Map<String, Object> urlVars = new HashMap<String, Object>();
		urlVars.put("app", appGuid);
		List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
		List<String> uris = new ArrayList<String>();
		for (Map<String, Object> resource : resourceList) {
			Map<String, Object> domainResource = CloudEntityResourceMapper.getEmbeddedResource(resource, "domain");
			String host = CloudEntityResourceMapper.getEntityAttribute(resource, "host", String.class);
			String domain = CloudEntityResourceMapper.getEntityAttribute(domainResource, "name", String.class);
			if (host != null && host.length() > 0)
				uris.add(host + "." + domain);
			else
				uris.add(domain);
		}
		return uris;
	}

	protected CloudApplication fillApplicationUris(CloudApplication app) {
		if (app != null) {
			app.setUris(findApplicationUris(app.getMeta().getGuid()));
		}
		return app;
	}

	@SuppressWarnings("unchecked")
	protected void fillInEmbeddedResource(Map<String, Object> resource, String... resourcePath) {
		if (resourcePath.length == 0) {
			return;
		}
		Map<String, Object> entity = (Map<String, Object>) resource.get("entity");

		String headKey = resourcePath[0];
		String[] tailPath = Arrays.copyOfRange(resourcePath, 1, resourcePath.length);

		if (!entity.containsKey(headKey)) {
			String pathUrl = entity.get(headKey + "_url").toString();
			Object response = restTemplate.getForObject(getUrl(pathUrl), Object.class);
			if (response instanceof Map) {
				Map<String, Object> responseMap = (Map<String, Object>) response;
				if (responseMap.containsKey("resources")) {
					response = responseMap.get("resources");
				}
			}
			entity.put(headKey, response);
		}
		Object embeddedResource = entity.get(headKey);

		if (embeddedResource instanceof Map) {
			Map<String, Object> embeddedResourceMap = (Map<String, Object>) embeddedResource;
			fillInEmbeddedResource(embeddedResourceMap, tailPath);
		} else if (embeddedResource instanceof List) {
			List<Object> embeddedResourcesList = (List<Object>) embeddedResource;
			for (Object r : embeddedResourcesList) {
				fillInEmbeddedResource((Map<String, Object>) r, tailPath);
			}
		} else {
			// no way to proceed
			return;
		}
	}

	/*
	 * Additional common helper methods
	 */

	@SuppressWarnings("unchecked")
	protected List<Map<String, Object>> getAllResources(String urlPath, Map<String, Object> urlVars) {
		List<Map<String, Object>> allResources = new ArrayList<Map<String, Object>>();
		String resp;
		if (urlVars != null) {
			resp = restTemplate.getForObject(getUrl(urlPath), String.class, urlVars);
		} else {
			resp = restTemplate.getForObject(getUrl(urlPath), String.class);
		}
		Map<String, Object> respMap = JsonUtil.convertJsonToMap(resp);
		List<Map<String, Object>> newResources = (List<Map<String, Object>>) respMap.get("resources");
		if (newResources != null && newResources.size() > 0) {
			allResources.addAll(newResources);
		}
		String nextUrl = (String) respMap.get("next_url");
		while (nextUrl != null && nextUrl.length() > 0) {
			nextUrl = addPageOfResources(nextUrl, allResources);
		}
		return allResources;
	}

	@SuppressWarnings("unchecked")
	protected String addPageOfResources(String nextUrl, List<Map<String, Object>> allResources) {
		String resp = restTemplate.getForObject(getUrl(nextUrl), String.class);
		Map<String, Object> respMap = JsonUtil.convertJsonToMap(resp);
		List<Map<String, Object>> newResources = (List<Map<String, Object>>) respMap.get("resources");
		if (newResources != null && newResources.size() > 0) {
			allResources.addAll(newResources);
		}
		return (String) respMap.get("next_url");
	}

	@SuppressWarnings("unchecked")
	private ApplicationStats doGetApplicationStats(UUID appId, CloudApplication.AppState appState) {
		List<InstanceStats> instanceList = new ArrayList<InstanceStats>();
		if (appState.equals(CloudApplication.AppState.STARTED)) {
			Map<String, Object> respMap = getInstanceInfoForApp(appId, "stats");
			for (String instanceId : respMap.keySet()) {
				InstanceStats instanceStats = new InstanceStats(instanceId,
						(Map<String, Object>) respMap.get(instanceId));
				instanceList.add(instanceStats);
			}
		}
		return new ApplicationStats(instanceList);
	}

	private Map<String, Object> getInstanceInfoForApp(UUID appId, String path) {
		String url = getUrl("/v2/apps/{guid}/" + path);
		Map<String, Object> urlVars = new HashMap<String, Object>();
		urlVars.put("guid", appId);
		String resp = restTemplate.getForObject(url, String.class, urlVars);
		return JsonUtil.convertJsonToMap(resp);
	}

	protected String getUrl(String path) {
		return cloudControllerUrl + (path.startsWith("/") ? path : "/" + path);
	}
}
