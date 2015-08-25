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
 * <p>Title: ManagedMetric</p>
 * <p>Description: Annotation to mark a method/attribute as a metric, based on Spring's org.springframework.jmx.export.metadata.ManagedMetric</p> 
 * <p>Company: Helios Development Group LLC</p>
 *  @author Jennifer Hickey (Spring)
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.annotation.ManagedMetric</code></p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ManagedMetric {
	/**
	 * A description of the metric
	 */
	String description();
	/**
	 * The name of the metric as exposed in the MBeanAttributeInfo.
	 */
	String displayName();
	/**
	 * The type of the metric 
	 */
	MetricType metricType() default MetricType.GAUGE;
	/**
	 * The optional unit of the metric
	 */
	String unit() default "";
	/**
	 * The metric category describing the class or package that the metric is grouped into.
	 * The default blamk value indicates that the containing class's 
	 * {@link MetricGroup} annotation should be read for this value.
	 */
	String category() default "";
	/**
	 * An arbitrary content descriptor for this metric which could be JSON, XML or CSV etc.
	 */
	String descriptor() default "";	
	/**
	 * The optional subkeys for this metric
	 */
	String[] subkeys() default {};
	
	/**
	 * An array of managed notifications that may be emitted by the annotated managed metric 
	 */
	ManagedNotification[] notifications() default {};
	
	/**
	 * Indicates if the metric type is rendered as a CompositeType 
	 * and can be enhanced to surface the type's fields as first class mbean attributes 
	 */
	boolean popable() default false;
	
	/**
	 * The window size for GAUGE type metrics
	 */
	int windowSize() default 0;
	
	/**
	 * The initial value for COUNTER type metrics
	 */
	long initialValue() default -1L;


}
