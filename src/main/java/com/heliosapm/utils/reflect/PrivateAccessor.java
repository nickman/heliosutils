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

import java.io.PrintStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.heliosapm.utils.ref.ReferenceService;

/**
 * <p>Title: PrivateAccessor</p>
 * <p>Description: A helper class to wrap invocations to and fields in objects defined as protected or private.
 * All methods throw RuntimeExceptions only. The located accessible objects (methods, fields, constructors) are cached for performance.
 * The cache is implemented as a SoftReference map so cache entries will be garbage collected if memory is required.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.reflect.PrivateAccessor</code></p>
 */

public class PrivateAccessor {
	/** The accessible object cache */
	protected static Map<Integer, WeakReference<AccessibleObject>> accessibleObjects = new ConcurrentHashMap<Integer, WeakReference<AccessibleObject>>();
	/** Logging DEBUG flag */
	protected static AtomicBoolean DEBUG = new AtomicBoolean(false);
	/** Debug Stream */
	protected static PrintStream out = System.out;
	/** Error Stream */
	protected static PrintStream err = System.err;
	
	
	/**
	 * Looks up the passed key in the accessibleObject cache.
	 * @param key The accessibleObject cache key
	 * @return The cached accessible object or null if not found, or gc'ed. 
	 */
	protected static AccessibleObject cacheLookup(int key) {
		WeakReference<AccessibleObject> sr = accessibleObjects.get(key);
		if(sr==null) return null;
		AccessibleObject ao = sr.get();
		if(ao==null) {
			accessibleObjects.remove(key);
		}
		return ao;
	}
	
	/**
	 * Adds an accessibleObject to cache.
	 * @param key The accessibleObject cache key.
	 * @param ao The accessibleObject to cache.
	 */
	protected static void addToCache(int key, AccessibleObject ao) {
		if(ao==null) throw new IllegalArgumentException("AccessibleObject to cache was null");
		WeakReference<AccessibleObject> sr = ReferenceService.getInstance().newWeakReference(ao, null);
		accessibleObjects.put(key, sr);
	}
	
	/**
	 * Attempts to create a new instance of the passed class
	 * @param clazz The class to instantiate an object from
	 * @param arguments The ctor arguments
	 * @param signature The ctor signature
	 * @return the instantiated object
	 * @param <T> the class [type]
	 */
	public static <T> T createNewInstance(final Class<T> clazz, final Object[] arguments, final  Class<?>...signature) {
		if(clazz==null) throw new IllegalArgumentException("The passed class was null");
		try {
			Constructor<T> ctor = findConstructorFromClass(clazz, signature);
			if(ctor==null) throw new Exception("No ctor found for [" + clazz.getName() + "]");
			return ctor.newInstance(arguments);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create an instance of [" + clazz.getName() + "]", ex);
		}
	}
	
	/**
	 * Invokes a method against an instance of an object.
	 * @param targetObject The object to invoke against.
	 * @param methodName The name of the method to invoke.
	 * @param arguments The arguments to pass to the method.
	 * @param signature The class types of the method parameters.
	 * @return The return value of the method invocation.
	 */
	public static Object invoke(Object targetObject, String methodName, Object[] arguments, Class<?>...signature) {
		if(targetObject==null) throw new IllegalArgumentException("Target Object Was Null");	
		Class<?> clazz = targetObject.getClass();
		if(isDebug()) log("PrivateAccessor Invoking [" , clazz.getName() , "." , methodName , argsToString(arguments) , "]");
		int key = invocationKey(clazz, methodName, signature);
		Method method = (Method)cacheLookup(key);
		if(method==null) {
			method = findMethodFromClass(clazz, methodName, signature);
			if(method==null) {
				elog(null, "The method [" , methodName , "] was not found in the class [" , clazz.getName() , "]");
				throw new RuntimeException("The method [" + methodName + "] was not found in the class [" + clazz.getName() + "]");
			}
			method.setAccessible(true);
			addToCache(key, method);
		}
		try {
			if(isDebug()) log("[PrivateAccessor] Invoking [" , method , argsToString(arguments) , "].");
			if(Modifier.isStatic(method.getModifiers())) {
				return method.invoke(null, arguments);
			}
			return method.invoke(targetObject, arguments);
		} catch (Exception e) {
			elog(e, "Invocation Exception calling method [" , method.toGenericString() , "] in the class [" , clazz.getName() , "] with arguments:" , argsToIndentedString(arguments));
			throw new RuntimeException("Invocation Exception calling method [" + method.toGenericString() + "] in the class [" + clazz.getName() + "] with arguments:" + argsToIndentedString(arguments), e);
		}
	}
	
	/**
	 * Invokes a method against an instance of an object.
	 * This method should only be used if there are no arguments 
	 * or the signature of the constructor can be derrived accurately from <code>argument.getClass()</code>.  
	 * @param targetObject The object to invoke against.
	 * @param methodName The name of the method to invoke.
	 * @param arguments The arguments to pass to the method.
	 * @return The return value of the method invocation.
	 */
	public static Object invoke(Object targetObject, String methodName, Object...arguments) {
		return invoke(targetObject, methodName, arguments, getSignatureForArguments(arguments));
	}
	
	
	
	
	/**
	 * Invokes a static method against a class.
	 * @param targetClass The class on which the static method will be invoked.
	 * @param methodName The name of the method to invoke.
	 * @param arguments The arguments to pass to the method.
	 * @param signature The class types of the method parameters.
	 * @return The return value of the method invocation.
	 */
	public static Object invokeStatic(Class<?> targetClass, String methodName, Object[] arguments, Class<?>[] signature) {
		if(targetClass==null) throw new IllegalArgumentException("Target Class Was Null");
		if(isDebug()) log("PrivateAccessor Invoking Static [" , targetClass.getName() , "." , methodName , argsToString(arguments) , "]");
		int key = invocationKey(targetClass, methodName, signature);
		Method method = (Method)cacheLookup(key);
		if(method==null) {
			method = findMethodFromClass(targetClass, methodName, signature);
			if(method==null) {
				elog(null, "The method [" , methodName , "] was not found in the class [" , targetClass.getName() , "]");
				throw new RuntimeException("The method [" + methodName + "] was not found in the class [" + targetClass.getName() + "]");
			} else {
				method.setAccessible(true);
				addToCache(key, method);
			}
		}
		try {			
			return method.invoke(null, arguments);
		} catch (Exception e) {
			elog(e, "Invocation Exception calling method [" , methodName , "] in the class [" , targetClass.getName() , "]");
			throw new RuntimeException("Invocation Exception calling method [" + methodName + "] in the class [" + targetClass.getName() + "]", e);
		}
	}
	
	/**
	 * Invokes a static method against a class.
	 * @param className The name of the class to invoke on.
	 * @param methodName The name of the method to invoke.
	 * @param arguments The arguments to pass to the method.
	 * @param signature The class types of the method parameters.
	 * @return The return value of the method invocation.
	 */
	public static Object invokeStatic(String className, String methodName, Object[] arguments, Class<?>[] signature) {
		Class<?> clazz = loadClass(className);
		return invokeStatic(clazz, methodName, arguments, signature);
	}
	
	
	/**
	 * Invokes a static method against a class.
	 * This method should only be used if there are no arguments 
	 * or the signature of the constructor can be derrived accurately from <code>argument.getClass()</code>. 
	 * @param targetClass The class on which the static method will be invoked.
	 * @param methodName The name of the method to invoke.
	 * @param arguments The arguments to pass to the method.
	 * @return The return value of the method invocation.
	 */
	public static Object invokeStatic(Class<?> targetClass, String methodName, Object...arguments) {
		return invokeStatic(targetClass, methodName, arguments, getSignatureForArguments(arguments));
	}
	
	/**
	 * Invokes a static method against a class.
	 * This method should only be used if there are no arguments 
	 * or the signature of the constructor can be derrived accurately from <code>argument.getClass()</code>. 
	 * @param className The name of the class to invoke on.
	 * @param methodName The name of the method to invoke.
	 * @param arguments The arguments to pass to the method.
	 * @return The return value of the method invocation.
	 */
	public static Object invokeStatic(String className, String methodName, Object...arguments) {
		Class<?> clazz = loadClass(className);
		return invokeStatic(clazz, methodName, arguments);

	}
	
	/**
	 * Sets the value in a field of an object
	 * @param targetObject The target object to set the value in
	 * @param field The field to set the value in
	 * @param value The value to set
	 */
	public static void setFieldValue(final Object targetObject, final Field field, final Object value) {
		if(targetObject==null) throw new IllegalArgumentException("Target Object Was Null");
		if(field==null) throw new IllegalArgumentException("Field Was Null");
		if(!field.isAccessible()) {
			field.setAccessible(true);
		}
		try {
			field.set(targetObject, value);
		} catch (Exception e) {
			throw new RuntimeException("Exception setting field [" + field.getName() + "] in an instance of class [" + field.getDeclaringClass().getName() + "]", e);
		}
	}
	
	/**
	 * Sets the value of a field in an object.
	 * @param targetObject The target object to set the value in
	 * @param fieldName The name of the field to set the value in
	 * @param value The value to set
	 */
	public static void setFieldValue(Object targetObject, String fieldName, Object value) {
		if(targetObject==null) throw new IllegalArgumentException("Target Object Was Null");		
		if(fieldName==null || fieldName.trim().isEmpty()) throw new IllegalArgumentException("Field name was null or empty");
		Class<?> clazz = targetObject.getClass();
		if(isDebug()) log("PrivateAccessor Accessing Field [" , fieldName , "] in instance of [" , clazz.getName() , "]");
		int key = invocationKey(clazz, fieldName);
		Field field = (Field)cacheLookup(key);
		if(field==null) {
			field = getFieldFromClass(clazz, fieldName);
			if(field==null) {
				elog(null, "The field [" , fieldName , "] was not found in instance of the class [" , clazz.getName() , "]");
				throw new RuntimeException("The field [" + fieldName + "] was not found in instance of the class [" + clazz.getName() + "]");
			} else {
				field.setAccessible(true);
				addToCache(key, field);
			}
		}
		setFieldValue(targetObject, field, value);
	}
	
	/**
	 * Retrieves the value from the given field
	 * @param field The field
	 * @param targetObject A target object instance required only if the field is not static
	 * @return The value in the field
	 */
	public static Object getFieldValue(final Field field, final Object targetObject) {
		if(field==null) throw new IllegalArgumentException("Field Was Null", new Throwable());
		field.setAccessible(true);
		final boolean staticField = Modifier.isStatic(field.getModifiers());
		if(!staticField && targetObject==null) throw new IllegalArgumentException("Field [" + field.getDeclaringClass().getName() + "." + field.getName() + "] is not static and target was null", new Throwable());
		return AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                try {
                	if(staticField) {
                		return field.get(null);
                	}
                	return field.get(targetObject);
				} catch (Exception e) {
					throw new RuntimeException("Failed to get value from field [" + field.getDeclaringClass().getName() + "." + field.getName() + "]", e);
				}
            }
        }); 
	}
	
	/**
	 * Retrieves the result of the given method
	 * @param method the method
	 * @param targetObject A target object instance required only if the method is not static
	 * @param args The arguments to the method
	 * @return the result of the given method
	 */	
	public static Object getMethodResult(final Method method, final Object targetObject, final Object...args) {
		if(method==null) throw new IllegalArgumentException("Method Was Null", new Throwable());
		method.setAccessible(true);
		final boolean staticMethod = Modifier.isStatic(method.getModifiers());
		if(!staticMethod && targetObject==null) throw new IllegalArgumentException("Method [" + method.getDeclaringClass().getName() + "." + method.toGenericString() + "] is not static and target was null", new Throwable());
		return AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                try {
					return method.invoke(staticMethod ? null : targetObject, args);
				} catch (Exception e) {
					throw new RuntimeException("Failed to get value from method [" + method.getDeclaringClass().getName() + "." + method.toGenericString() + "]", e);
				}
            }
        }); 
	}
	
	
	/**
	 * Reads the value from a field in an object.
	 * @param targetObject The target object from which to read the field value.
	 * @param fieldName The name of the field to read.
	 * @return The value of the field.
	 */
	public static Object getFieldValue(Object targetObject, String fieldName) {
		if(targetObject==null) throw new IllegalArgumentException("Target Object Was Null");		
		Class<?> clazz = targetObject.getClass();
		if(isDebug()) log("PrivateAccessor Accessing Field [" , fieldName , "] in instance of [" , clazz.getName() , "]");
		int key = invocationKey(clazz, fieldName);
		Field field = (Field)cacheLookup(key);
		if(field==null) {
			field = getFieldFromClass(clazz, fieldName);
			if(field==null) {
				elog(null, "The field [" , fieldName , "] was not found in instance of the class [" , clazz.getName() , "]");
				throw new RuntimeException("The field [" + fieldName + "] was not found in instance of the class [" + clazz.getName() + "]");
			} else {
				field.setAccessible(true);
				addToCache(key, field);
			}
		}
		try {			
			return field.get(targetObject);
		} catch (Exception e) {
			elog(e, "Exception accessing field [" , fieldName , "] in instance of the class [" , clazz.getName() , "]");
			throw new RuntimeException("Exception accessing field [" + fieldName + "] in the class [" + clazz.getName() + "]", e);
		}		
	}
	
    /**
     * Retrieves the instance of the field from the passed object
     * @param targetObject the object instance to get the field from
     * @param fieldName the name of the field
     * @return the located field.
     */
    public static Field getField(Object targetObject, String fieldName) {
		if(targetObject==null) throw new IllegalArgumentException("Target Object Was Null");		
		Class<?> clazz = targetObject.getClass();
		if(isDebug()) log("PrivateAccessor Accessing Field [" , fieldName , "] in instance of [" , clazz.getName() , "]");
		int key = invocationKey(clazz, fieldName);
		Field field = (Field)cacheLookup(key);
		if(field==null) {
			field = getFieldFromClass(clazz, fieldName);
			if(field==null) {
				elog(null, "The field [" , fieldName , "] was not found in instance of the class [" , clazz.getName() , "]");
				throw new RuntimeException("The field [" + fieldName + "] was not found in instance of the class [" + clazz.getName() + "]");
			}
			field.setAccessible(true);
			addToCache(key, field);
		}
		return field;
    }
    
	/**
	 * Find the named field from the passed class or the classes parents.
	 * @param targetClass The class to get the field from.
	 * @param fieldName The name of the field.
	 * @return The named field.
	 */
	public static Field getFieldFromClass(final Class<?> targetClass, final String fieldName) {
		if(targetClass==null) throw new IllegalArgumentException("Target Class Was Null");
		if(fieldName==null || fieldName.trim().isEmpty()) throw new IllegalArgumentException("Field Name Was Null Or Empty");
		final String _fieldName = fieldName.trim();
		final int key = invocationKey(targetClass, _fieldName);
		Field field = (Field)cacheLookup(key);
		Class<?> clazz = targetClass;
		while(field==null) {		
			try {
				// try declared field
				field = clazz.getDeclaredField(fieldName);
			} catch (NoSuchFieldException e) {
				// try field
				try {
					field = clazz.getField(fieldName);
				} catch (NoSuchFieldException e2) {/* No Op */}
			}
			if(field!=null) break;
			clazz = clazz.getSuperclass();
			if(clazz==null || java.lang.Object.class.equals(clazz)) {
				break;
			}					
		}
		if(field==null) throw new RuntimeException("The field [" + _fieldName + "] was not found in the class [" + targetClass.getName() + "]");
		field.setAccessible(true);
		addToCache(key, field);
		return field;	
	}
    
	
	
	/**
	 * Reads the value from a static field in a class.
	 * @param targetClass The class from which read the static field.
	 * @param fieldName The name of the field to read.
	 * @return The value of the field.
	 */
	public static Object getStaticFieldValue(Class<?> targetClass, String fieldName) {		
		if(targetClass==null) throw new IllegalArgumentException("Target Class Was Null");
		if(isDebug()) log("PrivateAccessor Accessing Static Field [" , fieldName , "] in class [" , targetClass.getName() , "]");
		final int key = invocationKey(targetClass, fieldName);
		Field field = (Field)cacheLookup(key);
		if(field==null) {
			field = getFieldFromClass(targetClass, fieldName);
			if(field==null) {
				elog(null, "The field [" , fieldName , "] was not found in the class [" , targetClass.getName() , "]");
				throw new RuntimeException("The field [" + fieldName + "] was not found in the class [" + targetClass.getName() + "]");
			}
			field.setAccessible(true);
			addToCache(key, field);
		}
		try {			
			return field.get(null);
		} catch (Exception e) {
			elog(e, "Exception accessing field [" , fieldName , "] in the class [" , targetClass.getName() , "]");
			throw new RuntimeException("Exception accessing field [" + fieldName + "] in the class [" + targetClass.getName() + "]", e);
		}		
	}
	
	/**
	 * Sets the value of a field in a static class.
	 * @param targetClass The class to set the field in.
	 * @param fieldName The name of the field.
	 * @param value The value to set the field to.
	 */
	public static void setStaticFieldValue(Class<?> targetClass, String fieldName, Object value) {
		if(targetClass==null) throw new IllegalArgumentException("Target Class Was Null");
		if(isDebug()) log("PrivateAccessor Accessing Static Field [" , fieldName , "] in class [" , targetClass.getName() , "]");
		int key = invocationKey(targetClass, fieldName);
		Field field = (Field)cacheLookup(key);
		if(field==null) {
			field = getFieldFromClass(targetClass, fieldName);
			if(field==null) {
				elog(null, "The field [" , fieldName , "] was not found in the class [" , targetClass.getName() , "]");
				throw new RuntimeException("The field [" + fieldName + "] was not found in the class [" + targetClass.getName() + "]");
			} else {
				field.setAccessible(true);
				addToCache(key, field);
			}
		}
		try {			
			field.set(null, value);
		} catch (Exception e) {
			elog(e, "Exception accessing field [" , fieldName , "] in the class [" , targetClass.getName() , "]");
			throw new RuntimeException("Exception accessing field [" + fieldName + "] in the class [" + targetClass.getName() + "]", e);
		}		
		
	}
	
	/**
	 * Creates a new instance of the provided class name.
	 * @param className The fully qualified class name.
	 * @param arguments The arguments to the constructor.
	 * @param signature The types of the constructor's parameters.
	 * @param loaders An optional array of classloaders to attempt. If none are supplied, the current thread's contextual class loader is used.
	 * @return An instance of the passed class.
	 */
	public static Object getObjectInstance(String className, Object[] arguments, Class<?>[] signature, ClassLoader...loaders) {
		return getObjectInstance(loadClass(className, loaders), arguments, signature);
	}
	
	/**
	 * Creates a new instance of the provided class.
	 * @param clazz The class to create an instance of.
	 * @param arguments The arguments to the constructor.
	 * @param signature The types of the constructor's parameters.
	 * @return An instance of the passed class.
	 */
	public static Object getObjectInstance(Class<?> clazz, Object[] arguments, Class<?>[] signature) {
		if(clazz==null) throw new IllegalArgumentException("Target Class Was Null");
		if(isDebug()) log("PrivateAccessor Creating Instance of class [" , clazz.getName() , "] with arguments [" , argsToString(arguments) , "]");
		int key = invocationKey(clazz, "CTOR", signature);
		Method staticCtor = null;
		Constructor<?> constructor = (Constructor<?>)cacheLookup(key);
		if(constructor==null) {
			constructor = findConstructorFromClass(clazz, signature);
			if(constructor==null) {
				
				try {
					staticCtor = clazz.getDeclaredMethod("getInstance", signature);
				} catch (NoSuchMethodException nsme) {
					try {
						staticCtor = clazz.getMethod("getInstance", signature);
					} catch (NoSuchMethodException nsme2) {
						elog(null, "No constructor or static getInstance was not found in the class [" , clazz.getName() , "]");
						throw new RuntimeException("No constructor or static getInstance was not found in the class [" + clazz.getName() + "]");						
					}
				}
				staticCtor.setAccessible(true);
			} else {
				
				constructor.setAccessible(true);
				addToCache(key, constructor);
			}
		}
		try {			
			if(constructor==null) {
				return staticCtor.invoke(null, arguments);
			}
			return constructor.newInstance(arguments);
		} catch (Exception e) {
			elog(e, "Exception invoking constructor for the class [" , clazz.getName() , "]", "with arguments:" , argsToIndentedString(arguments));
			throw new RuntimeException("Exception invoking constructor for the class [" + clazz.getName() + "]", e);
		}				
	}
	
	/**
	 * Creates a new instance of the provided class.
	 * This method should only be used if there are no arguments 
	 * or the signature of the constructor can be derrived accurately from <code>argument.getClass()</code>.
	 * @param clazz The class to create an instance of.
	 * @param arguments The arguments to the constructor.
	 * @return An instance of the passed class.
	 */
	public static Object getObjectInstance(Class<?> clazz, Object...arguments) {
		return getObjectInstance(clazz, arguments, getSignatureForArguments(arguments));
	}

	/**
	 * Generates a signature from the passed objects.
	 * @param arguments An array of passed invocation argument objects.
	 * @return An array of classes representing the types of the passed arguments.
	 */
	protected static Class<?>[] getSignatureForArguments(Object...arguments) {
		Class<?>[] signature = new Class<?>[arguments==null ? 0 : arguments.length];
		if(arguments!=null) {
			for(int i = 0; i < arguments.length; i++) {
				signature[i] = arguments[i].getClass();
			}
		}
		return signature;
	}
	
	
	/**
	 * Classloads the class for the passed class name.
	 * @param className The fully qualified name of the class.
	 * @param loaders An optional array of classloaders to attempt. The first one found will be returned.
	 * @return The loaded class.
	 */
	public static Class<?> loadClass(String className, ClassLoader... loaders)  {
		Class<?> clazz = null;
		if(loaders==null || loaders.length<1) {
			try {
				clazz = Class.forName(className);
			} catch (Exception e) {
				elog(e, "Target ClassName [" , className , "] Could Not Be Found");
				throw new IllegalArgumentException("Target ClassName [" + className + "] Could Not Be Found", e);
			}
		} else {
			Exception lastEx = null;
			for(ClassLoader cl: loaders) {
				try {
					clazz = Class.forName(className, true, cl);
					break;
				} catch (Exception e) {
					lastEx = e;
				}
			}
			elog(lastEx, "Target ClassName [" , className , "] Could Not Be Found");
			if(clazz==null) throw new IllegalArgumentException("Target ClassName [" + className + "] Could Not Be Found", lastEx);
		}
		return clazz;
	}
	
	

	/**
	 * Find the named method from the passed class or the classes parents.
	 * @param targetClass The class to get the method from.
	 * @param methodName The name of the method.
	 * @param signature The types of the method's parameters.
	 * @return The located method.
	 */
	public static Method findMethodFromClass(Class<?> targetClass,
			String methodName, Class<?>... signature) {
		Method method = null;
		Class<?> clazz = targetClass;
		while(method==null) {
			try {
				// try declared method
				method = clazz.getDeclaredMethod(methodName, signature);
			} catch (NoSuchMethodException e) {
				// try method
				try {
					method = clazz.getMethod(methodName, signature);
				} catch (NoSuchMethodException e2) {}
			}
			if(method!=null) break;
			else {
				clazz = clazz.getSuperclass();
				if(clazz==null || java.lang.Object.class.equals(clazz)) {
					break;
				}
			}
		}
		return method;
	}
	
//	/**
//	 * Acquires the constructor with the passed signature for the passed target class
//	 * @param targetClass The target class
//	 * @param signature The constructor signature
//	 * @return the constructor
//	 */
//	public static <T> Constructor<T> findConstructorFromClass(Class<T> targetClass, Class<?>... signature) {
//		try {
//			final Constructor<T> ctor = targetClass.getConstructor(signature);
//			ctor.setAccessible(true);
//			return ctor;
//		} catch (Exception ex) {
//			throw new RuntimeException(ex);
//		}
//	}

	
	
	/**
	 * Finds a constructor from the passed class or its parents.
	 * @param targetClass The class to get the constructor for.
	 * @param signature The types of the consructors parameters.
	 * @return The located constructor.
	 */
	public static <T> Constructor<T> findConstructorFromClass(Class<T> targetClass, Class<?>...signature) {
		int key = invocationKey(targetClass, "CTOR", signature);
		Constructor<T> ctor = (Constructor<T>)cacheLookup(key);
		if(ctor==null) {
			Class<?> clazz = targetClass;
			while(ctor==null) {				
				try {
					// try declared constructor
					ctor = (Constructor<T>) clazz.getDeclaredConstructor(signature);
				} catch (NoSuchMethodException e) {
					// try constructor
					try {
						ctor = (Constructor<T>) clazz.getConstructor(signature);
					} catch (NoSuchMethodException e2) {}
				}
				if(ctor!=null) break;
				clazz = clazz.getSuperclass();
				if(clazz==null || java.lang.Object.class.equals(clazz)) {
					break;
				}
			}			
		}
		if(ctor!=null) {
			ctor.setAccessible(true);
			addToCache(key, ctor);
		}
		
		return ctor;	
	}
	
	/**
	 * Determines which class called the current code block when the method is called.
	 * The presence of the implementation of the underlying method is not certain.
	 * Returns null in the event of an error.
	 * @return The caller class.
	 */
	public static Class<?> getCallerClass() {
		try {
			return (Class<?>)invokeStatic("sun.reflect.Reflection", "getCallerClass", new Object[]{4}, new Class[]{int.class});
		} catch (Throwable t) {
			return null;
		}
	}
	
		
	
	
	/**
	 * Generates a unique cache key for the passed method or field.
	 * @param clazz The class to which the accessibleObject is defined in.
	 * @param objectName The name of the method or field, or "CTOR" if the accessibleObject is a constructor.
	 * @param optionalSignature The types of the constructor's or method's parameters.
	 * @return The accessibleObject cache key.
	 */
	protected static int invocationKey(Class<?> clazz, String objectName, Class<?>...optionalSignature) {
		StringBuilder b = new StringBuilder(clazz.getName());
		b.append(objectName);
		if(optionalSignature != null) {
			for (Class<?> sigClazz : optionalSignature) {
				b.append(sigClazz.getName());
			}
		}
		return b.toString().hashCode();
	}
	
	/**
	 * Determines if the passed class is in the passed array of classes.
	 * @param clazz The class to test for.
	 * @param types The array of classes to search in.
	 * @return true if clazz is in types.
	 */
	protected static boolean isIn(Class<?> clazz, Class<?>[] types) {
		if(clazz==null || types==null || types.length < 1) return false;
		for(Class<?> type: types) {
			if(type.equals(clazz)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Flattens an array of objects to a single string for display.
	 * @param args The array of objects
	 * @return A string.
	 */
	private static String argsToString(Object...args) {
		if(args==null || args.length < 1) return "()";
		StringBuilder b = new StringBuilder("(");
		for(Object obj: args) {
			b.append(obj.toString()).append(",");
		}
		b.deleteCharAt(b.length()-1);
		b.append(")");
		return b.toString();
	}
	
	/**
	 * Flattens an array of objects to an indented format string for display.
	 * @param args The array of objects
	 * @return A string.
	 */
	private static String argsToIndentedString(Object...args) {
		if(args==null || args.length < 1) return "";
		StringBuilder b = new StringBuilder("");
		for(Object obj: args) {
			b.append("\n\t").append(obj.toString());
		}		
		b.append("\n");
		return b.toString();
	}
	
	
	/**
	 * Debug Logger
	 * @param objs
	 */
	protected static void log(Object...objs) {
		if(isDebug() && objs!=null && objs.length > 0) {
			StringBuilder b = new StringBuilder("[PrivateAccessor Debug]");
			for(Object obj: objs) {
				b.append(obj.toString());
			}
			System.out.println(b.toString());
		}
	}
	
	/**
	 * Error Logger
	 * @param objs
	 */
	protected static void elog(Throwable t, Object...objs) {
		if(err !=null && isDebug() && objs!=null && objs.length > 0) {
			StringBuilder b = new StringBuilder("[PrivateAccessor Error]");
			for(Object obj: objs) {
				b.append(obj.toString());
			}
			System.err.println(b.toString());
			if(t!=null) {
				b.append("\n\tStack Trace Follows:\n");
				t.printStackTrace(System.err);
			}
			
		}
	}
	

	/**
	 * Retrieves the logging debug state.
	 * @return true if debug is enabled.
	 */
	public static boolean isDebug() {
		return DEBUG.get();
	}

	/**
	 * Sets the logging debug state
	 * @param debug if true, debug logging will be issued to System.out.
	 */
	public static void setDebug(boolean debug) {
		DEBUG.set(debug);
	}

	/**
	 * Returns the debug output stream.
	 * @return the debug output
	 */
	public synchronized static PrintStream getOut() {
		return out;
	}

	/**
	 * Sets the debug output stream.
	 * @param out The output stream where debug logging will go.
	 */
	public synchronized static void setOut(PrintStream out) {
		if(out!=null) {
			PrivateAccessor.out = out;
		}
	}

	/**
	 * Returns the error output stream.
	 * @return the error output
	 */	
	public synchronized static PrintStream getErr() {
		return err;
	}

	/**
	 * Sets the error output stream. Set to null to turn off error logging.
	 * @param err The output stream where error logging will go.
	 */
	public synchronized static void setErr(PrintStream err) {
		PrivateAccessor.err = err;
	}

}
