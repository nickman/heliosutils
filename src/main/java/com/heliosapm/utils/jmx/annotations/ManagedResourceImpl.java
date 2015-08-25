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

import javax.management.ObjectName;
import com.heliosapm.utils.jmx.JMXHelper;
import static com.heliosapm.utils.jmx.Reflector.*;

/**
 * <p>Title: ManagedResourceImpl</p>
 * <p>Description: A concrete bean representing an extracted {@link ManagedResource}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.annotation.ManagedResourceImpl</code></p>
 */

public class ManagedResourceImpl {
	/** Specifies the MBean's JMX ObjectName */
	protected final ObjectName objectName;
	/** The MBean description */
	protected final String description;
	/** A name assigned to instances of this object so they can be auto-managed by a managed object mbean */
	protected final String name;
	/** Indicates if a named managed object is popable */
	protected final boolean popable;
	
	/** An array of managed notifications emitted from the annotated type */
	protected final ManagedNotificationImpl[] notifications;
	
	/** empty const array */
	public static final ManagedResourceImpl[] EMPTY_ARR = {};
	
	/**
	 * Converts an array of ManagedOperations to an array of ManagedResourceImpls
	 * @param resources the array of ManagedOperations to convert
	 * @return a [possibly zero length] array of ManagedResourceImpls
	 */
	public static ManagedResourceImpl[] from(ManagedResource...resources) {
		if(resources==null || resources.length==0) return EMPTY_ARR;
		ManagedResourceImpl[] mopis = new ManagedResourceImpl[resources.length];
		for(int i = 0; i < resources.length; i++) {
			mopis[i] = new ManagedResourceImpl(resources[i]);
		}
		return mopis;		
	}
	
	
	/**
	 * Creates a new ManagedResourceImpl
	 * @param mr The ManagedResource annotation instance to extract from
	 */
	public ManagedResourceImpl(ManagedResource mr) {
		objectName = nws(mr.objectName())==null ? null : JMXHelper.objectName(mr.objectName().trim());
		description = nws(mr.description())==null ? "JMX Managed Resource" : mr.description().trim();
		notifications = ManagedNotificationImpl.from(mr.notifications()); 
		name = nws(mr.name());
		popable = mr.popable();
	}
	
	/**
	 * Creates a new ManagedResourceImpl
	 * @param objectName The object name specification
	 * @param description The mbean description
	 * @param notifications An array of managed notifications
	 */
	ManagedResourceImpl(CharSequence objectName, CharSequence description, ManagedNotification...notifications) {
		this.objectName = objectName==null ? null : JMXHelper.objectName(objectName);
		this.description = nws(description);
		this.notifications = ManagedNotificationImpl.from(notifications);
		name = null;
		popable = false;
	}
	
	/**
	 * Creates a new ManagedResourceImpl
	 * @param objectName The object name specification
	 * @param description The mbean description
	 * @param notifications An array of managed notifications
	 */
	ManagedResourceImpl(ObjectName objectName, CharSequence description, ManagedNotification...notifications) {
		this.objectName = objectName==null ? null : objectName;
		this.description = nws(description);
		this.notifications = ManagedNotificationImpl.from(notifications);
		name = null;
		popable = false;		
	}
	
	/**
	 * Creates a new ManagedResourceImpl
	 * @param objectName The object name specification
	 * @param description The mbean description
	 * @param notifications An array of managed notifications
	 */
	ManagedResourceImpl(CharSequence objectName, CharSequence description, ManagedNotificationImpl...notifications) {
		this.objectName = objectName==null ? null : JMXHelper.objectName(objectName);
		this.description = nws(description);
		this.notifications = notifications;
		name = null;
		popable = false;
	}
	
	/**
	 * Creates a new ManagedResourceImpl
	 * @param objectName The object name specification
	 * @param description The mbean description
	 * @param notifications An array of managed notifications
	 */
	ManagedResourceImpl(ObjectName objectName, CharSequence description, ManagedNotificationImpl...notifications) {
		this.objectName = objectName==null ? null : objectName;
		this.description = nws(description);
		this.notifications = notifications;
		name = null;
		popable = false;
	}
	
	
	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("ManagedResourceImpl [objectName:%s, description:%s, name=%s, popable:%s]", objectName==null ? "none" : objectName.toString(), description==null ? "none" : description, name==null ? "null" : name, popable);
	}
	
	/**
	 * Returns the annotation specfied ObjectName or null if one was not defined
	 * @return the objectName
	 */
	public ObjectName getObjectName() {
		return objectName;
	}
	/**
	 * Returns the 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the array of managed notifications emitted from the annotated type
	 * @return the notifications
	 */
	public ManagedNotificationImpl[] getNotifications() {
		return notifications;
	}

	
	/**
	 * The name assigned to instances of this object so they can be auto-managed by a managed object mbean
	 * @return the managed object name
	 */
	public String getName() {
		return name;
	}


	/**
	 * Indicates if this managed object is popable
	 * @return true if popable, false otherwise
	 */
	public boolean isPopable() {
		return popable;
	}

}
