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

import static com.heliosapm.utils.jmx.Reflector.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.modelmbean.DescriptorSupport;


/**
 * <p>Title: ManagedNotificationImpl</p>
 * <p>Description: A concrete bean representing an extracted {@link ManagedNotification}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.annotations.ManagedNotificationImpl</code></p>
 */

public class ManagedNotificationImpl {
	/** The name of the managed notification */
	protected final String name;	
	/** The type of the actual notification emitted */
	protected final Class<? extends Notification> type;
	/** The description of the managed notification */
	protected final String description;
	/** The notification types emitted by the annotated bean */
	protected final String[] notificationTypes;
	
	/** A const empty arr */
	public static final ManagedNotificationImpl[] EMPTY_ARR = {};	
	/** A const empty arr */
	public static final MBeanNotificationInfo[] EMPTY_INFO_ARR = {};
	
	/**
	 * Converts an array of ManagedNotifications to an array of ManagedNotificationImpls
	 * @param notifications the array to convert
	 * @return the converted array
	 */
	public static ManagedNotificationImpl[] from(ManagedNotification...notifications) {
		if(notifications.length==0) return EMPTY_ARR;
		ManagedNotificationImpl[] arr = new ManagedNotificationImpl[notifications.length];
		for(int i = 0; i < notifications.length; i++) {
			arr[i] = new ManagedNotificationImpl(notifications[i]);
		}
		return arr;
	}
	
	/**
	 * Generates an array of MBeanNotificationInfos for the passed array of ManagedNotificationImpls
	 * @param notifs The ManagedNotificationImpls to convert
	 * @return a [possibly zero length] array of MBeanNotificationInfos
	 */
	public static MBeanNotificationInfo[] from(ManagedNotificationImpl...notifs) {
		if(notifs==null || notifs.length==0) return EMPTY_INFO_ARR;
		MBeanNotificationInfo[] infos = new MBeanNotificationInfo[notifs.length];
		for(int i = 0; i < infos.length; i++) {
			infos[i] = notifs[i].toMBeanInfo();
		}		
		return infos;		
	}
	
	
	/**
	 * Creates a new ManagedNotificationImpl
	 * @param mn The managed attribute to extract from
	 */
	public ManagedNotificationImpl(ManagedNotification mn) {
		name = nws(nvl(mn, "Managed Notification").name());
		description = nws(mn.description());
		type = mn.type();
		notificationTypes = mn.notificationTypes();
	}
	
	/**
	 * Creates a new ManagedNotificationImpl
	 * @param name The attribute name specification
	 * @param description The attribute description
	 * @param type The type name of the notification object emitted
	 * @param notificationTypes The notification types
	 */
	@SuppressWarnings("unchecked")
	ManagedNotificationImpl(CharSequence name, CharSequence description, CharSequence typeName, String...notificationTypes) {
		this.name = nws(name);
		this.description = nws(description);
		try { type = (Class<? extends Notification>) Class.forName(typeName.toString().trim()); } catch (Exception ex) { throw new RuntimeException(ex); }
		this.notificationTypes = notificationTypes;
	}
	
	/**
	 * Creates a new ManagedNotificationImpl
	 * @param name The notification name specification
	 * @param description The notification description
	 * @param type The type of the notification object emitted
	 * @param notificationTypes The notification types
	 */
	ManagedNotificationImpl(CharSequence name, CharSequence description, Class<? extends Notification> type, String...notificationTypes) {
		this.name = nws(name);
		this.description = nws(description);
		this.type = nvl(type, "Notification Type");
		this.notificationTypes = notificationTypes;
	}

	/**
	 * Returns the notification name
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the notification type
	 * @return the type
	 */
	public Class<? extends Notification> getType() {
		return type;
	}

	/**
	 * Returns the notification description
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the notification types
	 * @return the notificationTypes
	 */
	public String[] getNotificationTypes() {
		return notificationTypes;
	}	
	
	/**
	 * Returns an MBeanNotificationInfo rendered form this ManagedNotificationImpl.
	 * @return a MBeanNotificationInfo rendered form this ManagedNotificationImpl
	 */
	public MBeanNotificationInfo toMBeanInfo() {		
		return new MBeanNotificationInfo(
				notificationTypes,
				type.getName(),
				description,
				toDescriptor()
		);		
	}
	
	
	/**
	 * Generates a mutable MBean descriptor for this ManagedNotificationImpl
	 * @return a MBean descriptor
	 */
	public Descriptor toDescriptor() {
		return toDescriptor(false);
	}
	
	/**
	 * Generates a MBean descriptor for this ManagedNotificationImpl
	 * @param immutable true for an immutable descriptor, false otherwise
	 * @return a MBean descriptor
	 */
	public Descriptor toDescriptor(boolean immutable) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("name", name);
		return !immutable ?  new ImmutableDescriptor(map) : new DescriptorSupport(map.keySet().toArray(new String[map.size()]), map.values().toArray(new Object[map.size()]));	
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("ManagedNotificationImpl [name:%s, description:%s, type:%s, notifTypes:%s]", name==null ? "none" : name, description==null ? "none" : description, type.getName(), Arrays.toString(notificationTypes));
	}	


}
