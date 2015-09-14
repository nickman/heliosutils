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
package com.heliosapm.utils.ssh.terminal;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ch.ethz.ssh2.KnownHosts;

/**
 * <p>Title: KnownHostsRepo</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.ssh.terminal.KnownHostsRepo</code></p>
 */

public class KnownHostsRepo {
	/** The singleton instance */
	private static volatile KnownHostsRepo instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	private final Map<String, KnownHosts> repo = new ConcurrentHashMap<String, KnownHosts>();
	
	/**
	 * Acquires and returns the KnownHostsRepo singleton
	 * @return the KnownHostsRepo singleton
	 */
	public static KnownHostsRepo getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new KnownHostsRepo();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Acquires the KnownHosts for the passed file name
	 * @param fileName The name of the known hosts file
	 * @return the known hosts
	 */
	public KnownHosts getKnownHosts(final String fileName) {
		if(fileName==null || fileName.trim().isEmpty()) throw new IllegalArgumentException("The passed file name was null or empty");
		final String _name = fileName.trim();
		KnownHosts k = repo.get(_name);
		if(k==null) {
			synchronized(repo) {
				k = repo.get(_name);
				if(k==null) {
					final File f = new File(_name);
					if(!f.exists()) throw new IllegalArgumentException("The file [" + _name + "] does not exist");
					if(f.isDirectory()) throw new IllegalArgumentException("The file [" + _name + "] is a directory");
					if(!f.canRead()) throw new IllegalArgumentException("The file [" + _name + "] cannot be read");
					try {
						k = new KnownHosts(f);
						repo.put(_name, k);
					} catch (Exception e) {
						throw new RuntimeException("Failed to read KnownHosts file [" + _name + "]", e);						
					}
				}
			}
		}
		return k;
	}
	
	/**
	 * Acquires the KnownHosts for the passed file name, returning null if one cannot be created.
	 * @param fileName The name of the known hosts file
	 * @return the known hosts or null
	 */
	public KnownHosts getKnownHostsOrNull(final String fileName) {
		try {
			return getKnownHosts(fileName);
		} catch (Exception ex) {
			return null;
		}
	}


	/**
	 * Creates a new KnownHostsRepo
	 */
	private KnownHostsRepo() {
	}

}
