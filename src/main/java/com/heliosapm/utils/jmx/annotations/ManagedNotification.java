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

import javax.management.Notification;

/**
 * <p>Title: ManagedNotification</p>
 * <p>Description: Annotation to define a notification emitted by an MBean, based on Spring's ManagedNotification</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Rob Harrob (Spring)
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.annotations.ManagedNotification</code></p>
 */

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ManagedNotification {

	/**
	 * The name of the managed notification
	 */
	String name();
	
	/**
	 * The type of the actual notification emitted
	 */
	Class<? extends Notification> type() default Notification.class;

	/**
	 * The description of the managed notification
	 */
	String description() default "";

	/**
	 * The notification types emitted by the annotated bean
	 */
	String[] notificationTypes();

}

