/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.heliosapm.utils.events.eval;


/**
 * <p>Title: OpenConditionEvaluationException</p>
 * <p>Description: Exception thrown out of an evalution engine when no threshold applies.
 * This is like an open loop rule, meaning the alarm definition is flawed.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.eval.OpenConditionEvaluationException</code></p>
 */
public class OpenConditionEvaluationException extends RuntimeException {


	/**  */
	private static final long serialVersionUID = -3815383919578029735L;

	/**
	 * Creates a new OpenConditionEvaluationException
	 */
	public OpenConditionEvaluationException() {

	}

	/**
	 * Creates a new OpenConditionEvaluationException
	 * @param message The error message
	 */
	public OpenConditionEvaluationException(final String message) {
		super(message);
	}


}
