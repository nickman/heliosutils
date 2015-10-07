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

import java.lang.management.ManagementFactory;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

/**
 * <p>Title: JMXTracing</p>
 * <p>Description: The JMXTracing MBean is a user friendly interface for setting up JMX traces</p>
 * <p>From <href="https://blogs.oracle.com/jmxetc/entry/tracing_jmx_what_s_going">Daniel Fuchs</a> 
 * <p>Company: Helios Development Group LLC</p>
 * @author dfuchs
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.jmx.JMXTracing</code></p>
 */

public class JMXTracing extends StandardMBean  implements JMXTracingMBean, MBeanRegistration { 

    private final static Logger LOG = 
            Logger.getLogger(JMXTracing.class.getName());
    
    private final static ObjectName loggingMBean;
    static {
        try {
            loggingMBean = 
                    ObjectName.getInstance(LogManager.LOGGING_MXBEAN_NAME);
        } catch (MalformedObjectNameException x) {
            throw new UndeclaredThrowableException(x);
        }
    }
    
    public JMXTracing()  throws NotCompliantMBeanException {
        this(true,true);
    }
    
    // autostart is used in postRegister   method
    // autostop  is used in postDeregister method 
    //
    public JMXTracing(boolean autostart, boolean autostop) 
        throws NotCompliantMBeanException {
        super(JMXTracingMBean.class);
        this.autostart = autostart;
        this.autostop  = autostop;
    }
   
    

    // See JMXTracingMBean.java
    /**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.Foo#getLoggerNames()
	 */
    @Override
	public String[] getLoggerNames() {
        final String[] res;
        try {
            res = (String[])
                mbeanServer.getAttribute(loggingMBean,"LoggerNames");
            Arrays.sort(res);
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new UndeclaredThrowableException(x);
        }
        return res;
    }


    // See JMXTracingMBean.java
    /**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.Foo#list(java.lang.String)
	 */
    @Override
	public String[] list(String pattern) {
        
        final String[] names = getLoggerNames();
        final Set<String> list = new TreeSet<String>();
        for (String name : names ) {
            if (name.matches(pattern)) list.add(name);
        }
        return list.toArray(new String[list.size()]);
    }

    // See JMXTracingMBean.java
    /**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.Foo#switchTo(java.lang.String, java.lang.String)
	 */
    @Override
	public String[] switchTo(String pattern, String level) {
        
        final Level l = Level.parse(level);
        final String[] list = list(pattern);
        final String[] signature = { 
            "java.lang.String","java.lang.String"};
        final Set<String> result = new TreeSet<String>();
        
        for (String name:list) {
            try {
                mbeanServer.invoke(loggingMBean,"setLoggerLevel",new Object[] {
                    name, level
                    },signature);
                result.add(name);
            } catch (Exception x) {
                LOG.fine("Failed to switch " + name + " to " + level +": "+x);
                continue;
            }
        }
        return result.toArray(new String[result.size()]);
    }
    
    // See JMXTracingMBean.java
    /**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.Foo#getLoggerLevel(java.lang.String)
	 */
    @Override
	public String getLoggerLevel(String loggerName)
    throws JMException {
        final String[] signature = {
            "java.lang.String"};
        
        try {
            return (String) mbeanServer.invoke(loggingMBean,"getLoggerLevel",
                new Object[] { loggerName },signature);
        } catch (RuntimeException x) {
            throw x;
        } catch (JMException x) {
            throw x;
        }
        
   }
    
    // See JMXTracingMBean.java
   /**
 * {@inheritDoc}
 * @see com.heliosapm.utils.jmx.Foo#switchJMX(java.lang.String, java.lang.String)
 */
@Override
public Map switchJMX(String jmxLevel, String jmxRemoteLevel) {
        final Level l1 = Level.parse(jmxLevel);
        final Level l2 = Level.parse(jmxRemoteLevel);
        final TreeMap tr = new TreeMap();
        
        for (String log : switchTo("javax.management.\\*",jmxLevel)) {
            try {
                tr.put(log,getLoggerLevel(log));
            } catch (Exception x) {
                LOG.fine("Failed to get level for " + log);
            }
        }
        for (String log : switchTo("javax.management.remote.\\*",
                jmxRemoteLevel)) {
            try {
                 tr.put(log,getLoggerLevel(log));            
            } catch (Exception x) {
                LOG.fine("Failed to get level for " + log);
            }
        }
        for (String log : switchTo("com.sun.jmx.\\*",jmxLevel)) {
            try {
                 tr.put(log,getLoggerLevel(log));            
            } catch (Exception x) {
                LOG.fine("Failed to get level for " + log);
            }
        }
        for (String log : switchTo("com.sun.jmx.remote.\\*",
                jmxRemoteLevel)) {
            try {
                 tr.put(log,getLoggerLevel(log));            
            } catch (Exception x) {
                LOG.fine("Failed to get level for " + log);
            }
        }
        return tr;
    }
    

    // See JMXTracingMBean.java
    /**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.Foo#switchAllJMX(java.lang.String)
	 */
    @Override
	public String[] switchAllJMX(String level) {
        final Level l1 = Level.parse(level);
        final Set<String> res = switchJMX(level,level).keySet();
        final String[] result = res.toArray(new String[res.size()]);
        Arrays.sort(result);
        return result;
    }
    

    // See JMXTracingMBean.java
    /**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.Foo#setLogOnConsole(boolean)
	 */
    @Override
	public synchronized void setLogOnConsole(boolean on) {
        if (on && handler == null) {
            handler = new ConsoleHandler();
            handler.setLevel(Level.FINEST);
            Logger.getLogger("").addHandler(handler);
        } else if ((!on) && (handler != null)) {
            try {
                Logger.getLogger("").removeHandler(handler);
            } catch (Exception x) {
                LOG.fine("Failed to remove handler");
            }
            handler = null;
        }
    }
   
    // See JMXTracingMBean.java
    /**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.Foo#isLogOnConsole()
	 */
    @Override
	public synchronized boolean isLogOnConsole() {
        return handler!=null;
    }
    
    // See JMXTracingMBean.java
    /**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.Foo#enableConsoleLogging()
	 */
    @Override
	public void enableConsoleLogging() {
        setLogOnConsole(true);
    }
    
    // See JMXTracingMBean.java
    /**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.Foo#disableConsoleLogging()
	 */
    @Override
	public void disableConsoleLogging() {
        setLogOnConsole(false);
    }
    
    // See JMXTracingMBean.java
    /**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.Foo#isDebugOn()
	 */
    @Override
	public boolean isDebugOn() {
        return Level.FINEST.equals(LOG.getLevel());
    }
    
    // See JMXTracingMBean.java
    /**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.Foo#setDebugOn(boolean)
	 */
    @Override
	public void setDebugOn(boolean on) {
        if (on) LOG.setLevel(Level.FINEST);
        else LOG.setLevel(Level.INFO);
    }
        
    /**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.Foo#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
	 */
    @Override
	public ObjectName preRegister(MBeanServer server, ObjectName name) 
        throws Exception {
        if (name == null) 
            name=new ObjectName(this.getClass().getPackage().getName()+
                    ":type="+
                    this.getClass().getSimpleName());
        objectName = name;
        mbeanServer = server;
        
        if (!server.isRegistered(loggingMBean))
            throw new InstanceNotFoundException(loggingMBean.toString());
        return name;
    }

    /**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.Foo#postRegister(java.lang.Boolean)
	 */
    @Override
	public void postRegister(Boolean registrationDone) {
        //TODO postRegister implementation;
        if (!registrationDone.booleanValue()) return;
        if (autostart) {
            switchJMX(Level.FINEST.toString(),Level.FINER.toString());
            setDebugOn(false);
            enableConsoleLogging();
        }
    }

    /**
	 * {@inheritDoc}
	 * @see com.heliosapm.utils.jmx.Foo#postDeregister()
	 */
    @Override
	public void postDeregister() {
        //TODO postDeregister implementation;
        if (autostop) {
            switchAllJMX(Level.INFO.toString());
            disableConsoleLogging();
        }
    }

 
    private transient MBeanServer mbeanServer;
    private transient ObjectName objectName;
    private transient ConsoleHandler handler = null;
    
    private final boolean autostart;
    private final boolean autostop;
    
    /**
     * Registers a JMXTracing MBean in the platform MBeanServer
     * @throws JMException
     */
    public static void register() throws JMException {
        ManagementFactory.getPlatformMBeanServer().
                createMBean(JMXTracing.class.getName(),null);
    }
    
    
    public static void register(final ObjectName objectName) {
    	try {
    		JMXHelper.registerMBean(new JMXTracing(), objectName);
    	} catch (Exception ex) {
    		throw new RuntimeException(ex);
    	}
    }

}

