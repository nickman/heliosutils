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


/**
 * <p>Title: Evaluation</p>
 * <p>Description: Defines the signature of an evaluation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.events.eval.Evaluation</code></p>
 * @param <T> The type of the object being evaluated
 * @param <V> The type of the value being evaluated against
 * @param <R> The type of the result of the evaluation
 */

public interface Evaluation<T, V, R> {
	/**
	 * Evaluates the state of object <b><code>T</code></b> against the value of <b><code>V</code></b>,
	 * returning a result of type <b><code>E</code></b> 
	 * @param t The object whose state is being evaluated
	 * @param v The value which the evaluation is being made against
	 * @return the result of the evaluation
	 */
	public R evaluate(T t, V v);
}
