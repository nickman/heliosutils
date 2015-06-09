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
package com.heliosapm.utils.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.heliosapm.utils.lang.StringHelper;

/**
 * <p>Title: MetaHelper</p>
 * <p>Description: Helper to perform common relfective lookups against classes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.reflect.MetaHelper</code></p>
 */

public class MetaHelper {

	/**
	 * Finds all the methods in the passed class that are annotated with the passed annotation
	 * and is enabled for any of the passed modifiers
	 * @param target  The class to inspect
	 * @param annotation The annotation that must be on a matching method
	 * @param modifiers The modifiers the method must be enabled for any of
	 * @return an array of matching methods
	 */
	public static Method[] findAnnotatedMethods(final Class<?> target, final Class<? extends Annotation> annotation, final SModifier...modifiers) {
		if(target==null) throw new IllegalArgumentException("The passed target was null");
		if(annotation==null) throw new IllegalArgumentException("The passed annotation was null");
		final Map<String, Method> matchingMethods = new HashMap<String, Method>();
		for(Method m: target.getMethods()) {
			if(m.getAnnotation(annotation)!=null) {
				if(SModifier.isEnabledForAny(m, modifiers)) {
					matchingMethods.put(StringHelper.getMethodDescriptor(m), m);
				}
			}
		}
		for(Method m: target.getDeclaredMethods()) {
			if(m.getAnnotation(annotation)!=null) {
				if(SModifier.isEnabledForAny(m, modifiers)) {
					matchingMethods.put(StringHelper.getMethodDescriptor(m), m);
				}
			}
		}
		return matchingMethods.values().toArray(new Method[matchingMethods.size()]);
	}
	
	
	public MetaHelper() {}

}
