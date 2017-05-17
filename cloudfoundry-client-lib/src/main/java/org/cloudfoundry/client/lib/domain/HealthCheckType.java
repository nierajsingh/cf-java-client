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
package org.cloudfoundry.client.lib.domain;

public enum HealthCheckType {
	http, port, process, none;

	/**
	 * 
	 * @param value
	 * @return {@link HealthCheckType} constant for the given value, or null if
	 *         value is null or does not match any of the enum constants
	 */
	public static HealthCheckType from(String value) {
		if (value == null) {
			return null;
		}
		try {
			return valueOf(value);
		} catch (Throwable e) {
			return null;
		}
	}
}
