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
package com.heliosapm.utils.events.eval;

import com.heliosapm.utils.enums.BitMasked;

/**
 * <p>Title: StatelessEval</p>
 * <p>Description: Evaluates a condition against a set of possible outcomes (since we're stateless) and returns one of them</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.nash.alarms.threshold.StatelessEval</code></p>
 * @param <T> The type of the value being evaluated
 * @param <V> The type of the value the evaluation is evaluated against 
 * @param <E> The event type
 */

public interface StatelessEval<T, V, E extends Enum<E> & BitMasked> {
	/**
	 * Executes an evaluation
	 * @param t The object being evaluated
	 * @param v The input to the evalutation that the object will be evaluated against
	 * @param def The default event type to return if no event types evaluate to true
	 * @return the event type of the lowest ordinal that evaluated to true, or the supplied default if all event types evaluated false. 
	 * @throws OpenConditionEvaluationException thrown if the evaluation is not conclusive
	 */
	public E eval(T t, V v, E def) throws OpenConditionEvaluationException;
}
