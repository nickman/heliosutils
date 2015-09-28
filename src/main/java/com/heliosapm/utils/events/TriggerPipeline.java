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

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;

/**
 * <p>Title: TriggerPipeline</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.TriggerPipeline</code></p>
 * @param <E> The pipeline entry type
 */

public class TriggerPipeline<E> implements PipelineContext {
	
	/** The list of triggers in the pipeline */
	protected final LinkedList<Trigger<?,E>> pipeline;
	
	/** The starter trigger */
	protected final Trigger<?, E> starter;
	/** The executor service for async triggers */
	protected final ExecutorService pipelineExecutor;
	
	/**
	 * Creates a new TriggerPipeline
	 * @param pipeline The list of triggers in the pipeline
	 */
	TriggerPipeline(final LinkedList<Trigger<?,E>> pipeline, final ExecutorService pipelineExecutor) {
		this.pipeline = pipeline;
		this.pipelineExecutor = pipelineExecutor;
		starter = pipeline.getFirst();
	}
	
	@Override
	public ExecutorService getPipelineExecutor() {		
		return pipelineExecutor;
	}
	
	public void in(final E e) {
		starter.in(e);
	}

}
