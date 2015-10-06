/**
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */
package com.heliosapm.utils.events;

import javax.management.ObjectName;

import org.json.JSONObject;

import com.heliosapm.utils.enums.BitMasked;

/**
 * <p>Title: EventNotificationEnricher</p>
 * <p>Description: Defines a class that can enrich the JSON emitted on a sunk event notification</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.EventNotificationEnricher</code></p>
 */

public interface EventNotificationEnricher<E extends Enum<E> & BitMasked> {
	/**
	 * Passes a sunk event and the current notification JSON
	 * @param objectName The object name of the pipeline
	 * @param event The sunk event
	 * @param notification The notification JSON to enrich
	 */
	public void onSunkEvent(final ObjectName objectName, final E event, final JSONObject notification);

}
