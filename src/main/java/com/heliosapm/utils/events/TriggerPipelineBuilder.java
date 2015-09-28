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

import java.lang.management.ManagementFactory;
import java.util.LinkedList;

import javax.management.ObjectName;

import org.json.JSONObject;

import com.heliosapm.utils.jmx.JMXHelper;
import com.heliosapm.utils.jmx.JMXManagedThreadPool;

/**
 * <p>Title: TriggerPipelineBuilder</p>
 * <p>Description: Builder for a {@link TriggerPipeline} that strings together a series of triggers into one pipeline.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.TriggerPipelineBuilder</code></p>
 * @param <R> The output type of the pipeline
 * @param <E> The input type of the pipeline
 */

public class TriggerPipelineBuilder<R, E> {
	/**  */
	protected final LinkedList<Trigger<R,E>> pipeline = new LinkedList<Trigger<R,E>>();
	/** The pipeline input type */
	protected final Class<E> inputType;
	/** The pipeline output type */
	protected final Class<R> outputType;
	/** The number of cores */
	protected static final int CORES = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	/** The pipeline executor */
	protected static final JMXManagedThreadPool threadPool = new JMXManagedThreadPool(JMXHelper.objectName("com.heliosapm.events:service=PipelineExecutor"), "PipelineExecutor", CORES, CORES, 128, 120, 1024, 99);

	/** Indicates the pipeline builder has a starter */
	protected boolean hasStarter = false;
	/** Indicates the pipeline builder has a sink */
	protected boolean hasSink = false;
	
	protected final ObjectName objectName;
	
	
	/**
	 * Creates a new pipeline builder
	 * @param objectName The JMX ObjectName for this pipeline
	 * @param inputType The pipeline input type
	 * @param outputType The pipeline output type
	 * @return the builder
	 */
	public static <R, E> TriggerPipelineBuilder<R, E> builder(final ObjectName objectName, final Class<E> inputType, final Class<R> outputType) {
		return new TriggerPipelineBuilder<R, E>(objectName, inputType, outputType);
	}
	
//	public static <R, E> TriggerPipeline<R,E> fromJSON(final JSONObject jsonConfig) {
//		return null;  // TODO
//	}
	
	/**
	 * Creates a new TriggerPipelineBuilder
	 * @param objectName The JMX ObjectName for this pipeline
	 * @param inputType The pipeline input type
	 * @param outputType The pipeline output type
	 */
	private TriggerPipelineBuilder(final ObjectName objectName, final Class<E> inputType, final Class<R> outputType) {
		this.objectName = objectName;
		this.inputType = inputType;
		this.outputType = outputType;
	}
	
	public TriggerPipeline<R, E> build() {
		return new TriggerPipeline<R, E>(objectName, pipeline, threadPool);
	}
	
	/**
	 * Adds the pipeline starter trigger
	 * @param trigger a starter trigger
	 */
	@SuppressWarnings("unchecked")
	public TriggerPipelineBuilder<R,E> addStarter(final Trigger<?, E> trigger) {
		if(!pipeline.isEmpty()) throw new IllegalStateException("The pipeline already has a starter");
		if(!trigger.getInputType().isAssignableFrom(inputType)) {
			throw new IllegalArgumentException("Pipeline expects starter input type of [" + inputType.getName() 
					+ "] but trigger accepts input type of [" + trigger.getInputType().getName() + "]");
		}
		pipeline.addLast((Trigger<R,E>)trigger);
		hasStarter = true;
		return this;
	}

	/**
	 * Adds a post starter trigger
	 * @param trigger a post starter trigger
	 */
	@SuppressWarnings("unchecked")
	public <T> TriggerPipelineBuilder<R,E> addTrigger(final Trigger<?, T> trigger) {
		if(!hasStarter) throw new IllegalStateException("The pipeline has no starter");
		final Trigger<T, ?> prior = (Trigger<T, ?>) pipeline.getLast();
		if(!trigger.getInputType().isAssignableFrom(prior.getReturnType())) {
			throw new IllegalArgumentException("Prior trigger provides output type of [" + prior.getReturnType().getName() 
					+ "] but passed trigger accepts input type of [" + trigger.getInputType().getName() + "]");
		}
		pipeline.addLast((Trigger<R,E>)trigger);
		return this;
	}
	
	/**
	 * Adds a sink which is the last trigger in the pipeline
	 * @param sink The pipeline sink trigger
	 */
	@SuppressWarnings("unchecked")
	public TriggerPipelineBuilder<R,E> addSink(final Trigger<Void, R> sink) {
		if(pipeline.isEmpty()) throw new IllegalStateException("The pipeline has no starter");
		final Trigger<R, ?> prior = (Trigger<R, ?>) pipeline.getLast();
		if(!sink.getInputType().isAssignableFrom(prior.getReturnType())) {
			throw new IllegalArgumentException("Prior trigger provides output type of [" + prior.getReturnType().getName() 
					+ "] but passed sink accepts input type of [" + sink.getInputType().getName() + "]");
		}
		pipeline.addLast((Trigger<R,E>)sink);
		hasSink = true;
		return this;
	}
	
	

}
