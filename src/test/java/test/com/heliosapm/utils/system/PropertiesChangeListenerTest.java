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
package test.com.heliosapm.utils.system;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;

import org.junit.Test;

import test.com.heliosutils.BaseTest;

import com.heliosapm.utils.system.ChangeNotifyingProperties;
import com.heliosapm.utils.system.PropertyChangeListener;

/**
 * <p>Title: PropertiesChangeListenerTest</p>
 * <p>Description: Test cases for {@link ChangeNotifyingProperties} and listeners</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.com.heliosapm.utils.system.PropertiesChangeListenerTest</code></p>
 */

public class PropertiesChangeListenerTest extends BaseTest {
	static final Charset UTF8 = Charset.forName("UTF8");
	/**
	 * Test basic change listener
	 * @throws Exception on any error
	 */
	@Test
	public void testChanges() throws Exception {
		final String props = "dana=sculley\nfox=mulder\n";
		Properties base = new Properties();
		base.load(new ByteArrayInputStream(props.getBytes(UTF8)));
		final ChangeNotifyingProperties cnp = new ChangeNotifyingProperties();
		
		ChangeListenerAccumulator cla = new ChangeListenerAccumulator();
		cnp.registerListener(cla);
		cnp.notificationsEnabled(false);
		cnp.putAll(base);		
		cnp.notificationsEnabled(true);
		
		// ============================================
		
		Assert.assertEquals("dana initial value", "sculley", cnp.getProperty("dana"));
		Assert.assertEquals("fox initial value", "mulder", cnp.getProperty("fox"));
		// ============================================
		Assert.assertTrue("Accumulator empty", cla.isEmpty());
		// ============================================
		cnp.setProperty("dana", "mulder");
		cnp.setProperty("fox", "sculley");
		sleep(300);
		// ============================================
		Assert.assertEquals("dana changed value", "mulder", cla.changes.get("dana")[0]);
		Assert.assertEquals("dana changed value", "sculley", cla.changes.get("dana")[1]);
		
		Assert.assertEquals("fox changed value", "sculley", cla.changes.get("fox")[0]);
		Assert.assertEquals("fox changed value", "mulder", cla.changes.get("fox")[1]);
		
		// ============================================
		cnp.setProperty("walter", "skinner");		
		sleep(300);
		Assert.assertEquals("walter new value", "skinner", cla.inserts.get("walter"));

	}
	
	
	class ChangeListenerAccumulator implements PropertyChangeListener {
		final Map<String, String[]> changes = new HashMap<String, String[]>();
		final Map<String, String> inserts = new HashMap<String, String>();
		final Map<String, String> removes = new HashMap<String, String>();
		
		public boolean isEmpty() {
			return changes.isEmpty() && inserts.isEmpty() && removes.isEmpty();
		}
		
		@Override
		public void onChange(final String propertyName, final String newValue, final String oldValue) {
			changes.put(propertyName, new String[]{newValue, oldValue});
		}
		
		@Override
		public void onInsert(final String propertyName, final String value) {
			inserts.put(propertyName, value);
		}
		
		@Override
		public void onRemove(final String propertyName, final String value) {
			removes.put(propertyName, value);
		}
	}
}
