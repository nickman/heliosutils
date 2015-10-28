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
package com.heliosapm.utils.jmx.bulk;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.QueryExp;

/**
 * <p>Title: BulkJMXService</p>
 * <p>Description: Provides bulk JMX Ops</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.bulk.BulkJMXService</code></p>
 */

public class BulkJMXService implements MBeanRegistration, BulkJMXServiceMBean {
	/** The MBeanServer this service was registered in */
	protected MBeanServer server = null;
	/** The ObjectName this service was registered under */
	protected ObjectName objectName = null;
	/** A cache of mbean attribute names keyed by the ObjectName of the mbean they were extracted from */
	protected final Map<ObjectName, String[]> mbeanAttrNames = Collections.synchronizedMap(new WeakHashMap<ObjectName, String[]>(128));
	
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass().getName());
	
	/** Empty attr map const */
	public static final Map<String, Object> EMPTY_ATTR_MAP = Collections.unmodifiableMap(new HashMap<String, Object>(0));
	/** Empty op result map const */
	public static final Map<ObjectName, Object> EMPTY_OPRES_MAP = Collections.unmodifiableMap(new HashMap<ObjectName, Object>(0));
	
	/** Empty bulk map const */ 
	public static final Map<ObjectName, Map<String, Object>> EMPTY_BULK_MAP = Collections.unmodifiableMap(new HashMap<ObjectName, Map<String, Object>>(0));
	/** Empty string array const */
	public static final String[] EMPTY_STR_ARR = {};
	/** Empty byte array const */
	public static final byte[] EMPTY_BYTE_ARR = {};
	/** Empty object array const */
	public static final Object[] EMPTY_OBJ_ARR = {};
	
	/**
	 * Creates a new BulkJMXService
	 */
	public BulkJMXService() {
		log.info("Created BulkJMXService");
	}
	
	/**
	 * Ping op
	 * @return the installed system's current UTC time
	 */
	@Override
	public long ping() {
		log.info("ping");
		return System.currentTimeMillis();
	}
	
	/**
	 * Bulk attribute lookup
	 * @param lookups A map of attribute names to lookup keyed by the [optionally pattern based] ObjectName of the MBeans to look them up from
	 * @return A map of key/value attributes keyed by the absolute ObjectName of the MBean they were read from
	 */
	@Override
	public Map<ObjectName, Map<String, Object>> getAttributes(final Map<ObjectName, String[]> lookups) {
		if(lookups==null || lookups.isEmpty()) return EMPTY_BULK_MAP;
		final Map<ObjectName, Map<String, Object>> map = new HashMap<ObjectName, Map<String, Object>>();
		for(Map.Entry<ObjectName, String[]> entry: lookups.entrySet()) {
			final ObjectName target = entry.getKey();
			if(target.isPattern()) {
				Map<ObjectName, Map<String, Object>> bulkAttrValues = getPatternAttributes(target, cleanAttrs(entry.getValue()));
				if(!bulkAttrValues.isEmpty()) {
					map.putAll(bulkAttrValues);
				}
			} else {
				final Map<String, Object> attrMap = getAttributes(target, cleanAttrs(entry.getValue()));
				if(!attrMap.isEmpty()) {
					map.put(target, attrMap);
				}
			}
		}			
		return map;
	}
	
	/**
	 * Locates all registered MBeans matching the criteria defined by the passed ObjectName and query
	 * and returns a bulk value map for all through {@link #getAttributes(Map)}.
	 * @param on The object name to match
	 * @param query The query to match
	 * @param attrNames The attribute names of the attributes to retrieve
	 * @return A map of key/value attributes keyed by the absolute ObjectName of the MBean they were read from
	 */
	@Override
	public Map<ObjectName, Map<String, Object>> getAttributes(final ObjectName on, final QueryExp query, final String[] attrNames) {
		if((on==null && query==null) || attrNames==null || attrNames.length==0) return EMPTY_BULK_MAP;
		if(query==null) {
			return getAttributes(Collections.singletonMap(on, attrNames));
		}
		final Set<ObjectName> matches = server.queryNames(on, query);
		final Map<ObjectName, String[]> lookups = new HashMap<ObjectName, String[]>(matches.size());
		for(ObjectName o: matches) {
			lookups.put(o, attrNames);
		}
		return getAttributes(lookups);		
	}
	
	/**
	 * Locates all registered MBeans matching the criteria defined by the passed ObjectName and query
	 * and returns a compressed byte array containing the serialized bulk value map for all through {@link #getCompressedAttributes(Map)}.
	 * @param on The object name to match
	 * @param query The query to match
	 * @param attrNames The attribute names of the attributes to retrieve
	 * @return A map of key/value attributes keyed by the absolute ObjectName of the MBean they were read from
	 * serialized into a compressed byte array
	 */
	@Override
	public byte[] getCompressedAttributes(final ObjectName on, final QueryExp query, final String[] attrNames) {
		return compress(getAttributes(on, query, attrNames));
	}
	
	/**
	 * Invokes the specified operation on all MBeans with ObjectNames matching the passed object name and query.
	 * The results are returned as values in a map keyed by absolute object name if the result was not null.
	 * @param on The object name
	 * @param query The query
	 * @param opName The operation name to invoke
	 * @param signature The signature of the target op
	 * @param args The arguments to pass in the invocation
	 * @return a map of invocation responses keyed by the absolute ObjectName
	 */
	@Override
	public Map<ObjectName, Object> invoke(final ObjectName on, final QueryExp query, final String opName, final String[] signature, final Object...args) {
		if((on==null && query==null) || opName==null || opName.trim().isEmpty()) return EMPTY_OPRES_MAP;
		final Set<ObjectName> matches = server.queryNames(on, query);
		final Map<ObjectName, Object> map = new HashMap<ObjectName, Object>(matches.size());
		for(ObjectName o : matches) {
			try {
				final Object result = server.invoke(o, opName, args==null ? EMPTY_OBJ_ARR : args, signature);
				if(result!=null) map.put(o, result);
			} catch (Exception ex) {
				log.warning("Failed to invoke [" + opName + "] on [" + o + "]");
				map.put(o, ex);
			}
		}
		return map;
	}

	
	/**
	 * Bulk attribute lookup with response compression. Same op as {@link #getAttributes(Map)} and
	 * returns the result serialized into a compressed byte array
	 * @param lookups A map of attribute names to lookup keyed by the [optionally pattern based] ObjectName of the MBeans to look them up from
	 * @return A map of key/value attributes keyed by the absolute ObjectName of the MBean they were read from
	 * serialized into a compressed byte array
	 */
	@Override
	public byte[] getCompressedAttributes(final Map<ObjectName, String[]> lookups) {
		final Map<ObjectName, Map<String, Object>> map = getAttributes(lookups);
		return compress(map);
	}
	
	/**
	 * Compresses the passed map to a byte array
	 * @param map The map to compress
	 * @return a gzipped byte array
	 */
	protected byte[] compress(final Map<ObjectName, Map<String, Object>> map) {
		if(map==null || map.isEmpty()) return EMPTY_BYTE_ARR;
		ObjectOutputStream oos = null;
		GZIPOutputStream gos = null;
		ByteArrayOutputStream baos = null;
		try {
			baos = new ByteArrayOutputStream(2048);
			gos = new GZIPOutputStream(baos, false);
			oos = new ObjectOutputStream(gos);
			oos.writeObject(map);
			oos.flush();
			gos.flush();			
			baos.flush();
			return baos.toByteArray();
		} catch (Exception ex) {
			log.warning("Failed to serialize result map:" + ex);
			return EMPTY_BYTE_ARR;
		} finally {
			if(oos!=null) try { oos.close(); } catch (Exception x) {/* No Op */}
			if(gos!=null) try { gos.close(); } catch (Exception x) {/* No Op */}
			if(baos!=null) try { baos.close(); } catch (Exception x) {/* No Op */}
		}		
	}

	
	/**
	 * Cleans and uniqueifies a string array of attribute names
	 * @param names The names to clean
	 * @return the cleaned array
	 */
	protected String[] cleanAttrs(final String...names) {
		if(names==null || names.length==0) return EMPTY_STR_ARR;
		final Set<String> set = new LinkedHashSet<String>(names.length);
		for(String s: names) {
			if(s!=null) {
				final String _s = s.trim();
				if(!_s.isEmpty()) {
					set.add(_s);
				}
			}
		}
		return set.toArray(new String[set.size()]);
	}
	
	/**
	 * Expands an attribute name array of <b><code>{"*"}</code></b> to the full attribute name array for the target MBean
	 * @param on A non-pattern ObjectName
	 * @param names The array to expand
	 * @return the expanded array, or if not a wildcard array (<b><code>{"*"}</code></b>) returns the array unmodified
	 */
	protected String[] expand(final ObjectName on, final String...names) {
		if(names==null || names.length==0) return EMPTY_STR_ARR;
		if("*".equals(names[0])) {
			return getAttributeNames(on);
		}
		return names;
	}
	
	
	/**
	 * Simple absolute attribute getter
	 * @param on A non pattern ObjectName to read attributes from
	 * @param attrs The attribute names to retrieve
	 * @return A map of values keyed by the attribute name
	 */
	protected Map<String, Object> getAttributes(final ObjectName on, final String[] attrs) {
		if(!server.isRegistered(on) || on.isPattern() || attrs.length==0) return EMPTY_ATTR_MAP;
		final String[] _attrs = expand(on, attrs);
		final Map<String, Object> map = new HashMap<String, Object>();
		if(server.isRegistered(on)) {
			try {
				final AttributeList al = server.getAttributes(on, _attrs);
				for(Attribute attr: al.asList()) {
					final Object obj = attr.getValue();
					if(obj!=null && (obj instanceof Serializable)) {
						map.put(attr.getName(), attr.getValue());
					}
				}
			} catch (Exception ex) {
				log.warning("Failed to getAttributes(" + on + ", " + Arrays.toString(_attrs) + "):" + ex);
				map.put("EX", ex);
			}
		}
		return map;
	}
	
	/**
	 * Resolves the passed pattern ObjectName to a set of absolute ObjectNames and acquires the values from each
	 * @param on The pattern ObjectName
	 * @param attrs @param attrs The attribute names to retrieve
	 * @return A map of values keyed by the attribute name  within a map keyed by the absolute ObjectName
	 */
	protected Map<ObjectName, Map<String, Object>> getPatternAttributes(final ObjectName on, final String[] attrs) {
		if(!server.isRegistered(on) || attrs.length==0) return EMPTY_BULK_MAP;
		final Set<ObjectName> names = server.queryNames(on, null);
		if(names.isEmpty()) return EMPTY_BULK_MAP;
		Map<ObjectName, Map<String, Object>> map = new HashMap<ObjectName, Map<String, Object>>();
		for(ObjectName objName: names) {
			final Map<String, Object> attrValues = getAttributes(objName, attrs);
			if(!attrValues.isEmpty()) {
				map.put(objName, attrValues);
			}
		}
		return map;
	}
	
	/**
	 * Retrieves the attribute names of the MBean registered with the passed ObjectName
	 * @param on The ObjectName of the target MBean
	 * @return An array of attribute names
	 */
	protected String[] getAttributeNames(final ObjectName on) {
		if(on==null || on.isPattern() || !server.isRegistered(on)) return EMPTY_STR_ARR;
		String[] attrNames = mbeanAttrNames.get(on);
		if(attrNames==null) {
			synchronized(mbeanAttrNames) {
				attrNames = mbeanAttrNames.get(on);
				if(attrNames==null) {
					try {
						final MBeanAttributeInfo[] attrInfos = server.getMBeanInfo(on).getAttributes();
						final Set<String> names = new HashSet<String>(attrInfos.length);
						for(MBeanAttributeInfo mai: attrInfos) {
							names.add(mai.getName());
						}
						mbeanAttrNames.put(on, names.toArray(new String[names.size()]));
					} catch (Exception ex) {
						log.warning("Failed to get Attribute Names for [" + on + "]:" + ex);
						return EMPTY_STR_ARR;
					}
				}
			}
		}
		return attrNames;
	}
	
	/**
	 * Returns the number of cached MBean attribute name arrays
	 * @return the number of cached MBean attribute name arrays
	 */
	@Override
	public int getCachedAttributeNames() {
		return mbeanAttrNames.size();
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
	 */
	@Override
	public ObjectName preRegister(final MBeanServer server, final ObjectName name) throws Exception {
		this.server = server;
		this.objectName = name;
		return name;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
	 */
	@Override
	public void postRegister(final Boolean registrationDone) {
		if(registrationDone) {
			log.info(new StringBuilder("\n\t===================================================================\n\tRegistered BulkJMXService\n\t" + objectName + "\n\t===================================================================\n").toString());
		} else {
			log.warning("BulkJMXService not registered");
		}
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#preDeregister()
	 */
	@Override
	public void preDeregister() throws Exception {
		/* No Op */
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#postDeregister()
	 */
	@Override
	public void postDeregister() {
		/* No Op */
	}
	
	
	
	
	
	
	
	
	
	
}
