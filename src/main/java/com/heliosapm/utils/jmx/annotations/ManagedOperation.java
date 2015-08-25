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

import javax.management.MBeanOperationInfo;

/**
 * <p>Title: ManagedOperation</p>
 * <p>Description: Annotation to mark a method as a JMX operation, based on Spring's ManagedOperation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Rob Harrop (Spring)
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.annotation.ManagedOperation</code></p>
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ManagedOperation {
	/**
	 * The operation name, defaulting to the method name when reflected
	 */
	String name() default "";

	/**
	 * The operation description
	 */
	String description() default "";
	
	/**
	 * The impact of the method
	 */
	int impact() default MBeanOperationInfo.UNKNOWN;
	
	/**
	 * The operation's managed parameters
	 */
	ManagedOperationParameter[] parameters() default {};
	
	/**
	 * An array of managed notifications that may be emitted by the annotated managed attribute 
	 */
	ManagedNotification[] notifications() default {};

	

}