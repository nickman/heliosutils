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
package com.heliosapm.utils.jmx;

import java.io.ObjectStreamException;
import java.lang.management.ManagementFactory;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Pattern;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;



import com.heliosapm.utils.lang.StringHelper;

/**
 * <p>Title: ExtendedObjectName</p>
 * <p>Description: An extended {@link ObjectName} extension that provides the following additional functionality:<ul>
 * 	<li></li>
 * 	<li></li>
 * </ul></p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.ExtendedObjectName</code></p>
 */

public class ExtendedObjectName extends ObjectName {
	/** The remapped domain name */
	protected final String extDomain;
	/** The property key the transformed ObjectName's domain goes into */
	protected final String renamedDomainKey;
	
	/** Indicates if this object name is extended */
	protected final boolean extended;
	/** The inverse */
	protected volatile ObjectName inverse = null;
	
	protected static final Pattern range = Pattern.compile("\\[\\d+(?:-\\d+)?\\]"); 
	
	public static void main(String[] args) {
		try {
			ExtendedObjectName on = new ExtendedObjectName(ManagementFactory.COMPILATION_MXBEAN_NAME + ",host=foo,app=bar");
			log("Original Object Name: [" + on + "], extended:" + on.isExtended());
			ExtendedObjectName aon = on.toExtended("alarm", "alarmdomain");
			log("Alarm Object Name: [" + aon + "], extended:" + aon.isExtended());
			ObjectName on2 = aon.fromExtended();
			log("Back to Original Object Name: [" + on2 + "]");
			log("on == on2:" + on.equals(on2));
			
			
			final String original = "alarm:host=foo4,app=bar,type=Compilation,alarmdomain=java.lang";
			final String rangedWildCard = "alarm:host=foo[1-9],app=b*r,type=Compilation,alarmdomain=java.lang";
			log("Match:" + StringHelper.wildmatch(original, rangedWildCard));
			ExtendedObjectName alarmpattern = objectName(rangedWildCard);
			ObjectName pattern = objectName(rangedWildCard);
			ObjectName oo = JMXHelper.objectName(original);
			log("ObjectName Apply:" + pattern.apply(oo));
			log("ExtendedObjectName Apply:" + alarmpattern.apply(oo));
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	public static ExtendedObjectName objectName(final CharSequence stringy) {
		if(stringy==null) throw new IllegalArgumentException("The passed stringy was null");
		try {
			return new ExtendedObjectName(stringy.toString().trim());
		} catch (Exception ex) {
			throw new RuntimeException("Failed to build ExtendedObjectName from [" + stringy + "]", ex);
		}
	}
	
	public static ExtendedObjectName objectName(final ObjectName name) {
		if(name==null) throw new IllegalArgumentException("The passed ObjectName was null");
		try {
			return new ExtendedObjectName(name.toString());
		} catch (Exception ex) {
			throw new RuntimeException("Failed to build ExtendedObjectName from [" + name + "]", ex);
		}
	}
	
	Object writeReplace() throws ObjectStreamException {
		if(extended) return fromExtended();
		return this;
	}
	
	public ExtendedObjectName toExtended(final String extDomain, final String renamedDomainKey) {
		if(extended) return this;
		if(inverse==null) {
			try {
				
				final Hashtable<String, String> p = this.getKeyPropertyList();
				final StringBuilder b = new StringBuilder(extDomain).append(":");
				
				String v = p.remove("host");
				if(v!=null) b.append("host=").append(v).append(",");
				v = p.remove("app");
				if(v!=null) b.append("app=").append(v).append(",");
				if(!p.isEmpty()) {
					for(Map.Entry<String, String> entry: p.entrySet()) {
						b.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
					}
				}
				//props.putAll(p);
				b.append(renamedDomainKey).append("=").append(getDomain());
				
				inverse = new ExtendedObjectName(b.toString(), extDomain, renamedDomainKey);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to convert [" + this + "] to an extended name", ex);
			}
		}
		return (ExtendedObjectName)inverse;
	}
	
	
	/**
	 * Restores and returns the unalarmed rendering of this extended object name
	 * @return the unextended rendering of this extended object name
	 */
	public ObjectName fromExtended() {
		if(!extended) return this;
		if(inverse==null) {
			final Hashtable<String, String> p = this.getKeyPropertyList();
			final String domain = p.remove(renamedDomainKey);
			try {
				inverse = new ObjectName(domain, p);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to revert [" + this + "] to original name", ex);
			}
		}
		return inverse;		
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.ObjectName#apply(javax.management.ObjectName)
	 */
	@Override
	public boolean apply(ObjectName name) {
		if(name==null) throw new IllegalArgumentException("The passed ObjectName was null");
		if(!this.isPattern()) return this.equals(name);
		
		return matchDomains(name) && matchKeys(name);
	}
	
    protected boolean matchDomains(ObjectName name) {
        if (isDomainPattern()) {
            return StringHelper.wildmatch(name.getDomain(),getDomain());
        }
        return getDomain().equals(name.getDomain());
    }

    private final boolean matchKeys(ObjectName name) {
    	final Hashtable<String, String> myprops = getKeyPropertyList();
    	final Hashtable<String, String> hisprops = name.getKeyPropertyList();
    	if(isPropertyValuePattern() && !isPropertyListPattern()
    			&& myprops.size() != hisprops.size()) return false;
    	
    	if(isPropertyListPattern()) {
    		for(String key: myprops.keySet()) {
    			if(!hisprops.containsKey(key)) return false;
    		}
    	}
    	if(isPropertyValuePattern()) {
			for(Map.Entry<String, String> entry: myprops.entrySet()) {
				final String key = entry.getKey();
				if(!StringHelper.wildmatch(hisprops.get(entry.getKey()), entry.getValue())) return false;			
			}
			return true;
    	}
        // If no pattern, then canonical names must be equal
        //
        final String p1 = name.getCanonicalKeyPropertyListString();
        final String p2 = getCanonicalKeyPropertyListString();
        return (p1.equals(p2));
    }
	
	

	/**
	 * Creates a new ExtendedObjectName
	 * @param name A string representation of the object name. 
	 * @throws MalformedObjectNameException
	 */
	public ExtendedObjectName(final String name) throws MalformedObjectNameException {
		this(name, null, null);
	}
	
	/**
	 * Creates a new ExtendedObjectName with a non-extended name
	 * @param name An object name. 
	 * @throws MalformedObjectNameException
	 */
	public ExtendedObjectName(final ObjectName name) throws MalformedObjectNameException {
		this(name.toString(), null, null);
		
	}
	
	/**
	 * Creates a new ExtendedObjectName
	 * @param name An object name. 
	 * @param extDomain The new domain name
	 * @param renamedDomainKey The renamed domain key
	 * @throws MalformedObjectNameException
	 */
	public ExtendedObjectName(final String name, final String extDomain, final String renamedDomainKey) throws MalformedObjectNameException {
		super(extend(name, extDomain, renamedDomainKey));
		if(extDomain!=null && !extDomain.trim().isEmpty() && renamedDomainKey!=null && !renamedDomainKey.trim().isEmpty()) {
			this.extDomain = extDomain.trim();
			this.renamedDomainKey = renamedDomainKey.trim();
			extended = true;
		} else {
			this.extDomain = null;
			this.renamedDomainKey = null;
			extended = false;			
		}		
	}
	

	/**
	 * Creates a new ExtendedObjectName
	 * @param name An object name. 
	 * @param extDomain The new domain name
	 * @param renamedDomainKey The renamed domain key
	 * @throws MalformedObjectNameException
	 */
	public ExtendedObjectName(final ObjectName name, final String extDomain, final String renamedDomainKey) throws MalformedObjectNameException {
		super(extend(name.toString(), extDomain, renamedDomainKey));
		if(extDomain!=null && !extDomain.trim().isEmpty() && renamedDomainKey!=null && !renamedDomainKey.trim().isEmpty()) {
			this.extDomain = extDomain.trim();
			this.renamedDomainKey = renamedDomainKey.trim();
			extended = true;
		} else {
			this.extDomain = null;
			this.renamedDomainKey = null;
			extended = false;			
		}		
	}

	private static String extend(final String name, final String extDomain, final String renamedDomainKey) throws MalformedObjectNameException {
		if(extDomain!=null && !extDomain.trim().isEmpty() && renamedDomainKey!=null && !renamedDomainKey.trim().isEmpty()) {
			if(name.startsWith(extDomain)) return name;
			final ObjectName t = new ObjectName(name);
			final String domain = extDomain.trim();			
			final Hashtable<String, String> props = new Hashtable<String, String>(t.getKeyPropertyList());
			props.put(renamedDomainKey, t.getDomain());
			return new ObjectName(domain, props).toString();
		} else {
			return name;
		}
	}
	
	
	/**
	 * Creates a new ExtendedObjectName with a non-extended name
	 * @param domain The domain part of the object name.
	 * @param table The key properties
	 * @throws MalformedObjectNameException
	 */
	public ExtendedObjectName(final String domain, final Map<String, String> table) throws MalformedObjectNameException {
		this(domain, new Hashtable<String, String>(table), null, null);		
	}
	
	/**
	 * Creates a new ExtendedObjectName 
	 * @param domain The domain part of the object name.
	 * @param table The key properties
	 * @param extDomain The new domain name
	 * @param renamedDomainKey The renamed domain key
	 * @throws MalformedObjectNameException
	 */
	public ExtendedObjectName(final String domain, final Map<String, String> table, final String extDomain, final String renamedDomainKey) throws MalformedObjectNameException {
		super(extend(new StringBuilder(domain).append(":").append(flatten(table)).toString(), extDomain, renamedDomainKey));		
		if(extDomain!=null && !extDomain.trim().isEmpty() && renamedDomainKey!=null && !renamedDomainKey.trim().isEmpty()) {
			this.extDomain = extDomain.trim();
			this.renamedDomainKey = renamedDomainKey.trim();
			extended = true;
		} else {
			this.extDomain = null;
			this.renamedDomainKey = null;
			extended = false;			
		}
	}
	
	private static String flatten(final Map<String, String> table) {
		if(table==null || table.isEmpty()) return "";
		return table.toString().trim().substring(1).replaceAll("\\}$", "").replace(", ", ",");
	}
	

//	/**
//	 * Creates a new ExtendedObjectName with a non-extended name
//	 * @param domain The domain part of the object name.
//	 * @param key The attribute in the key property of the object name.
//	 * @param value The value in the key property of the object name.
//	 * @throws MalformedObjectNameException
//	 */
//	public ExtendedObjectName(final String domain, final String key, final String value) throws MalformedObjectNameException {
//		this(domain, key, value, null, null);
//	}
	
	/**
	 * Creates a new ExtendedObjectName
	 * @param domain The domain part of the object name.
	 * @param key The attribute in the key property of the object name.
	 * @param value The value in the key property of the object name.
	 * @param extDomain The new domain name
	 * @param renamedDomainKey The renamed domain key
	 * @throws MalformedObjectNameException
	 */
	public ExtendedObjectName(final String domain, final String key, final String value, final String extDomain, final String renamedDomainKey) throws MalformedObjectNameException {
		super(extend(new StringBuilder(domain).append(":").append(key).append("=").append(value).toString(), extDomain, renamedDomainKey));
		if(extDomain!=null && !extDomain.trim().isEmpty() && renamedDomainKey!=null && !renamedDomainKey.trim().isEmpty()) {
			this.extDomain = extDomain.trim();
			this.renamedDomainKey = renamedDomainKey.trim();
			extended = true;
		} else {
			this.extDomain = null;
			this.renamedDomainKey = null;
			extended = false;			
		}
	}
	
	
	/**
	 * Indicates if the passed ObjectName has an extended signature
	 * @param objectName The object name to test
	 * @return true if the passed ObjectName has an extended signature, false otherwise
	 */
	public static boolean isExtended(final ObjectName objectName) {
		if(objectName==null) throw new IllegalArgumentException("The passed ObjectName was null");
		if(!(objectName instanceof ExtendedObjectName)) return false;
		final ExtendedObjectName eob = (ExtendedObjectName)objectName;
		return eob.extDomain != null &&
			   eob.renamedDomainKey != null &&			   
			   eob.extDomain.equals(objectName.getDomain()) && 
			   objectName.getKeyProperty(eob.renamedDomainKey) !=null;
	}
	
	/**
	 * Indicates if this object name is extended
	 * @return true if this object name is extended, false otherwise
	 */
	public boolean isExtended() {
		return extended;
	}
	

}
