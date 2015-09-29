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

import java.util.Date;
import java.util.HashMap;

import javax.management.ObjectName;

/**
 * <p>Title: TriggerPipelineMBean</p>
 * <p>Description: JMX MBean interface for {@link TriggerPipeline} instances </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.TriggerPipelineMBean</code></p>
 */

public interface TriggerPipelineMBean {
	/**
	 * Indicates if the pipeline is started
	 * @return true if the pipeline is started, false otherwise
	 */
	public boolean isStarted();
	/**
	 * Starts the pipeline
	 */
	public void start();
	/**
	 * Stops the pipeline
	 */
	public void stop();
	/**
	 * Stops the pipeline and unregisters the MBean
	 */
	public void unregister();
	/**
	 * Returns the current state
	 * @return the current state
	 */
	public String getStateName();
	/**
	 * Returns the JMX ObjectName of this pipeline
	 * @return the JMX ObjectName of this pipeline
	 */
	public ObjectName getObjectName();
	/**
	 * Returns a string describing the flow of the pipeline
	 * @return a string describing the flow of the pipeline
	 */
	public String getFlow();
	/**
	 * Returns the current advisory
	 * @return the current advisory
	 */
	public String getAdvisory();
	/**
	 * Returns the prior state
	 * @return the prior state
	 */
	public String getPriorStateName();
	/**
	 * Returns the ms. timestamp of the last status change
	 * @return the ms. timestamp of the last status change
	 */
	public long getLastStatusChange();	
	/**
	 * Returns the timestamp of the last change in Date string format
	 * @return the timestamp of the last change in Date string format
	 */
	public Date getLastStatusChangeDate();
	/**
	 * Dumps the state of the pipeline in JSON format
	 * @return the state of the pipeline in JSON format
	 */
	public String dumpState();
	/**
	 * Returns the the countdown to decay.
	 * i.e. if no events are received before this period of time,
	 * the trigger will go into decay state 
	 * @return the decay count down
	 */
	public long getDecaySlope();
	/**
	 * Returns the configured decay period
	 * @return the configured decay period
	 */
	public long getDecay();
	/**
	 * Returns the configured decay period unit
	 * @return the configured decay period unit
	 */
	public String getDecayUnit();
	
	/**
	 * Returns the number of forwards per trigger in the pipeline
	 * @return the number of forwards per trigger in the pipeline
	 */
	public HashMap<String, Long> getForwards();
	
	/**
	 * Returns the number of sinks per trigger in the pipeline
	 * @return the number of sinks per trigger in the pipeline
	 */
	public HashMap<String, Long> getSinks();
	
	/**
	 * Forces a state change in the sink
	 * @param state The state name to force
	 * @see Sink#forceState(String)
	 */
	public void forceState(final String state);
	
	
	
	
}
