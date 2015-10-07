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

import java.util.concurrent.ExecutorService;

import javax.management.ObjectName;

import org.json.JSONObject;

/**
 * <p>Title: PipelineContext</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.PipelineContext</code></p>
 */

public interface PipelineContext<E> {
	public ExecutorService getPipelineExecutor();
	public void eventSunk(final int triggerId, final E event);
	public ObjectName getObjectName();
	public E getState();
	/**
	 * Sets an advisory message
	 * @param an advisory message
	 */
	public void setAdvisory(final String message);
	public void onStateChange(final Trigger<?,E> sender, final E e);
	public Trigger<Void, E> getSink();
	
	/**
	 * Allows a context caller to start the pipeline if it's not started when it receives the first input
	 */
	public void start();
	
	/**
	 * Passes the event message back to the context for possible enrichment 
	 * @param event The sunk event
	 * @param eventMessage the event message
	 */
	public void enrichEventMessage(final Object event, JSONObject eventMessage);
	
}
