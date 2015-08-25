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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Title: ManagedResource</p>
 * <p>Description: Annotation to mark a class as being a JMX managed, or having JMX managed attributes, based on Spring's ManagedResource</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.annotations.ManagedResource</code></p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ManagedResource {


	/**
	 * Specifies the MBean's JMX ObjectName
	 */
	String objectName() default "";

	/**
	 * The MBean description
	 */
	String description() default "";
		
	
	/**
	 * An array of managed notifications emitted from the annotated type 
	 */
	ManagedNotification[] notifications() default {};

	/**
	 * Annotates a type with a name so that it can be auto-managed by a managed object mbean.
	 * Typically ignored except for adding managed ojects at runtime
	 */
	String name() default "";
	
	/**
	 * Indicates if a named managed object is popable
	 */
	boolean popable() default false;
}
