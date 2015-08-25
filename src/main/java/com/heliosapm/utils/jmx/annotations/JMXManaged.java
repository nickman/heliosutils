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
package com.heliosapm.utils.jmx.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Title: JMXManaged</p>
 * <p>Description: Provides class/instance level meta-data about a published MBean</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.annotations.JMXManaged</code></p>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JMXManaged {
	/** The default description if one is not supplied */
	public static final String DEFAULT_DESCRIPTION = "Management MBean";
	/** A blank default string value */
	public static final String DEFAULT = "";
	
	/**
	 * The MBean description
	 */
	public String description() default DEFAULT_DESCRIPTION;
	
	/**
	 * The MBean interface
	 */
	public Class<?> mbeanInterface() default DefaultStandardMBean.class;

	/**
	 * The MXBean interface
	 */
	public Class<?> mxbeanInterface() default DefaultStandardMXBean.class;
	
	
	
	
}
