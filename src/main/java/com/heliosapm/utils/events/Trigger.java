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

/**
 * <p>Title: Trigger</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.Trigger</code></p>
 * @param <R> The return type from the triggered event
 * @param <E> The input type
 */

public interface Trigger<R, E> {
	public R in(final E event);
	public void out(final R result);
	public void reset();
	public void windDown(final E event);
	public void start();	
	public void stop(); 
	public boolean isStarted();
	public void setNextTrigger(Trigger<?, R> nextTrigger);
	public Trigger<?, R> nextTrigger();
	public Class<R> getReturnType();
	public Class<E> getInputType();
	public void setPipelineContext(final PipelineContext context, final int myId);
	

}


/*
	SimpleValue
	Sliding
	Tumbling
	Decay
	Scripted
	Composite
*/

/*
	ThresholdEvals:
	=================
	JMX Query Based, compile into Java ?
	
	Config:
	=======
	AlarmStateMap:
		state:  expression:  count
		
	See: tsdb-csf/csf-core/src/test/resources/configs/jmxcollect/querymanagertests.xml
	And: tsdb-csf/csf-core/src/main/java/com/heliosapm/opentsdb/client/jvmjmx/customx/QueryDecode.java
*/


/*
---metric--->  ThresholdEval --> if returns true --> (alarm state, metric) --> filter for most severe -->  Set Alert Status/Trace Alarm Status as a metric
ObjectName based on metric FQN
Determine if metric is registered for alarm using hashCode(fqn) first, then build object name
Register MBeans in seperate  MBeanServer
Broadcast notification on Alarm State Change
*/