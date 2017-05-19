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
package org.cloudfoundry.client.lib;

import java.util.List;

import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;

/**
 * Additional REST operations. Although public, this is for internal use only as API is bound to change.
 *
 */
public interface AdditionalRestOperations {

	/**
	 * A relatively fast way to fetch all applications in the active session
	 * Cloud space, that contains basic information about each apps.
	 * <p/>
	 * Information that may be MISSING from the list for each app: service
	 * bindings, mapped URLs, and app instances.
	 * 
	 * @return list of apps with basic information
	 */
	List<CloudApplication> getBasicApplications();

	/**
	 * A relatively fast way to fetch an application in the active session Cloud
	 * space, that contains basic information.
	 * <p/>
	 * Information that may be MISSING from the list for each app: service
	 * bindings, mapped URLs, and app instances.
	 * 
	 * @return list of apps with basic information
	 */
	CloudApplication getBasicApplication(String appName);

	/**
	 * Alternate, and potentially faster, way of stopping an application that
	 * only fetches the basic application to check if it exists before stopping.
	 * 
	 * @param appName
	 */
	void stopApplicationUsingBasicApp(String appName);

	/**
	 * @param application
	 * @return application stats if application exists and stats are available
	 * @throws may
	 *             throw 404 Error if app no longer exists
	 */
	ApplicationStats getApplicationStats(CloudApplication application);

	/**
	 * Returns a Cloud application with more complete information than
	 * {@link #getBasicApplication(String)}, including bound services and mapped
	 * URLs, but without running instances information. Fetching running
	 * instances may make the process longer, so this is yet another way to
	 * fetch an incomplete Cloud application that may be potentially faster than
	 * fetching a full complete application
	 * 
	 * @param appName
	 * @return non-null Cloud Application
	 * @throws CloudFoundryException
	 *             if error occurs or application does not exist anymore
	 */
	CloudApplication getApplicationWithoutInstances(String appName);

	/**
	 * 
	 * @return Non-null list of stacks. Empty if none are available.
	 */
	List<String> getStacks();

}