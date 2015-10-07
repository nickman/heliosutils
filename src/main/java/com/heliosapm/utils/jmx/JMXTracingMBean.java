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
package com.heliosapm.utils.jmx;

import java.util.Map;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * <p>Title: JMXTracingMBean</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.JMXTracingMBean</code></p>
 */

public interface JMXTracingMBean {
	// See JMXTracingMBean.java
	String[] getLoggerNames();

	// See JMXTracingMBean.java
	String[] list(String pattern);

	// See JMXTracingMBean.java
	String[] switchTo(String pattern, String level);

	// See JMXTracingMBean.java
	String getLoggerLevel(String loggerName) throws JMException;

	// See JMXTracingMBean.java
	Map switchJMX(String jmxLevel, String jmxRemoteLevel);

	// See JMXTracingMBean.java
	String[] switchAllJMX(String level);

	// See JMXTracingMBean.java
	void setLogOnConsole(boolean on);

	// See JMXTracingMBean.java
	boolean isLogOnConsole();

	// See JMXTracingMBean.java
	void enableConsoleLogging();

	// See JMXTracingMBean.java
	void disableConsoleLogging();

	// See JMXTracingMBean.java
	boolean isDebugOn();

	// See JMXTracingMBean.java
	void setDebugOn(boolean on);


}
