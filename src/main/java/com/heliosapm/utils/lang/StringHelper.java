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
package com.heliosapm.utils.lang;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.bind.DatatypeConverter;

import com.heliosapm.utils.config.ConfigurationHelper;
import com.heliosapm.utils.enums.Primitive;

/**
 * <p>Title: StringHelper</p>
 * <p>Description:Some generic String helper functions</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.lang.StringHelper</code></p>
 */

public class StringHelper {
	/** Regex pattern that defines a range of numbers */
	public static final Pattern intRange = Pattern.compile("([\\d+]+)-([\\d+]+)");
	/** Regex pattern that defines a range of numbers with a wildcard terminator */
	public static final Pattern endRange = Pattern.compile("([\\d+]+)-\\*");
	
	/** The superclass of StringBuilder and StringBuffer */
	public static final Class<?> ABSTRACT_SB_CLAZZ = StringBuilder.class.getSuperclass();
	/** Cleans a string value before conversion to an integral */
	public static final Pattern CLEAN_INTEGRAL = Pattern.compile("\\..*|[\\D&&[^\\-]]");
	/** Text line separator */
	public static final String EOL = System.getProperty("line.separator", "\n");	
	/** End of line splitter */
	public static final Pattern EOLP = Pattern.compile("$");
	/** pattern match for a shebang */
	public static final Pattern SHEBANG = Pattern.compile("#!/.*$");
	
	/** Sys prop substitution pattern */
	public static final Pattern SYS_PROP_PATTERN = Pattern.compile("\\$\\{(.*?)(?::(.*?))??\\}");
	/** Custom substitution pattern */
	public static final String CUSTOM_EXTRACT_PATTERN = "\\$%s\\{(.*?)*?\\}";
	
	/** Typed value substitution pattern */
	public static final Pattern TYPED_PATTERN = Pattern.compile("\\$typed\\{(.*?):(.*)\\}");

	
	/** Indicates if this platform uses backslashes for file separators */
	public static final boolean BACK_SLASH_FILESEP = java.io.File.separator.equals("\\");
	
	
	/** The ThreadMXBean */
	protected static final ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
	
	/** The logging banner line */
	public static final String BANNER = "==========================================================";

	/**
	 * Cleans a string value of all non numerics and anything after (and including) the decimal point
	 * @param number A number text
	 * @return A clean integral string
	 */
	public static String cleanNumber(CharSequence number) {
		return CLEAN_INTEGRAL.matcher(number).replaceAll("");
	}
	
	public static String cleanLines(final CharSequence cs, final Pattern skip) {
		final String[] lines = EOLP.split(cs);
		final StringBuilder b = new StringBuilder(cs.length());
		for(String line: lines) {
			if(!skip.matcher(line).matches()) {
				b.append(line).append(EOL);
			}
		}
		return b.toString();
	}
	
	/**
	 * Replace all sysprop tokens in the passed stringy with the resolved property value.
	 * @param cs The stringy to rewrite
	 * @param props Optional properties to resolve properties from
	 * @return the resolved string
	 */
	public static String resolveTokens(final CharSequence cs, final Properties...props) {
		return resolveTokens(null, cs, props);
	}
	
	public static void main(String[] args) {
//		log("TOK Test");
//		log(replaceNumericTokens("gc.${1}=name=${0},service=GCMonitor", splitString("Scavenge.time", '.')));
		String v = "My home directory is: [${user.home:none}].";
		log(resolveTokens(v));
		v = "My home directory is: [${user.xhome:none}].";
		log(resolveTokens(v));
		
	}
	
//	public static final Pattern BACKSLASH_PATTERN = Pattern.compile("(?!^\\\\)\\\\(?!\\\\)");
//	public static final Pattern BACKSLASH_PATTERN = Pattern.compile("(?<!\\)\\(?!\\)");
	
//	public static final Pattern BACKSLASH_PATTERN = Pattern.compile("\\\\");
	
	/**
	 * Replace all sysprop tokens in the passed stringy with the resolved property value.
	 * @param updateProperties Any resolved properties will added to this properties
	 * @param cs The stringy to rewrite
	 * @param props Optional properties to resolve properties from
	 * @return the resolved string
	 */
	public static String resolveTokens(final Properties updateProperties, final CharSequence cs, final Properties...props) {
		if(cs==null) return null;
		final Matcher m = SYS_PROP_PATTERN.matcher(cs);
		final StringBuffer b = new StringBuffer();
		while(m.find()) {
			final String key = m.group(1);
			final String def = m.group(2);
			try {
				String resolved =  ConfigurationHelper.getSystemThenEnvProperty((key==null ? "" : key), (def==null ? "" : def), props);
				// if BACK_SLASH_FILESEP is true, they will be removed on append replacement, so we need to escape them
				if(resolved!=null && !resolved.trim().isEmpty()) {
					if(BACK_SLASH_FILESEP) {
						resolved = resolved.replace("\\", "/");
					}
					m.appendReplacement(b, resolved);
					if(updateProperties!=null) {
						updateProperties.setProperty(key, resolved);
					}
				}
			} catch (Exception ex) {
				throw new RuntimeException("Resolved failed", ex);
			}			
		}
		m.appendTail(b);
		return b.toString();
	}
	
	
	
	/**
	 * Converts a string describing a string value and a type to the specified type 
	 * @param value The value to convert
	 * @return The converted value or a plain string if could not be converted
	 */
	public static Object convertTyped(final CharSequence value) {
		if(value==null) return null;
		final String _value = value.toString().trim();
		final Matcher m = TYPED_PATTERN.matcher(_value);
		if(m.matches()) {
			final String type = m.group(1);
			final String val = m.group(2);
			if(type==null || type.trim().isEmpty() || val==null || val.trim().isEmpty()) return _value;
		    if(Primitive.ALL_CLASS_NAMES.contains(type.trim())) {
		    	Class<?> clazz = Primitive.PRIMNAME2PRIMCLASS.get(type.trim());
		    	PropertyEditor pe = PropertyEditorManager.findEditor(clazz);
		    	pe.setAsText(val.trim());
		    	return pe.getValue();
		    }
		}
		return _value;
	}
	
	
	/**
	 * Generates a logging string as an indented banner
	 * @param format The format of the banner wrapped message
	 * @param args The token substitutions for the banner wrapped message
	 * @return the banner string
	 */
	public static String banner(CharSequence format, Object...args) {
		return new StringBuilder("\n\t")
			.append(BANNER)
			.append("\n\t")
			.append(String.format(format.toString(), args))
			.append("\n\t")
			.append(BANNER)
			.append("\n")
			.toString();
	}
	
	/**
	 * Optimized version of {@code String#split} that doesn't use regexps.
	 * This function works in O(5n) where n is the length of the string to
	 * split.
	 * @param s The string to split.
	 * @param c The separator to use to split the string.
	 * @return A non-null, non-empty array.
	 * <p>Copied from <a href="http://opentsdb.net">OpenTSDB</a>.
	 */
	public static String[] splitString(final String s, final char c) {
		return splitString(s, c, false);
	}


	/**
	 * Optimized version of {@code String#split} that doesn't use regexps.
	 * This function works in O(5n) where n is the length of the string to
	 * split.
	 * @param s The string to split.
	 * @param c The separator to use to split the string.
	 * @param trimBlanks true to not return any whitespace only array items
	 * @return A non-null, non-empty array.
	 * <p>Copied from <a href="http://opentsdb.net">OpenTSDB</a>.
	 */
	public static String[] splitString(final String s, final char c, final boolean trimBlanks) {
		final char[] chars = s.toCharArray();
		int num_substrings = 1;
		final int last = chars.length-1;
		for(int i = 0; i <= last; i++) {
			char x = chars[i];
			if (x == c) {
				num_substrings++;
			}
		}
		final String[] result = new String[num_substrings];
		final int len = chars.length;
		int start = 0;  // starting index in chars of the current substring.
		int pos = 0;    // current index in chars.
		int i = 0;      // number of the current substring.
		for (; pos < len; pos++) {
			if (chars[pos] == c) {
				result[i++] = new String(chars, start, pos - start);
				start = pos + 1;
			}
		}
		result[i] = new String(chars, start, pos - start);
		if(trimBlanks) {
			int blanks = 0;
			final List<String> strs = new ArrayList<String>(result.length);
			for(int x = 0; x < result.length; x++) {
				if(result[x].trim().isEmpty()) {
					blanks++;
				} else {
					strs.add(result[x]);
				}
			}
			if(blanks==0) return result;
			return strs.toArray(new String[result.length - blanks]);
		}
		return result;
	}

	
  /**
   * Converts an int range expression to an array of integers.
   * The values are comma separated. Each value can be an int or a range in the format <code>x-y</code>.
   * For example, the expresion <b>"1,2,4,7-10"</b> would return an in array <code>{1,2,4,7,8,9,10}</code>.
   * @param valuesStr The range expression.
   * @return An array of ints.
   */
  public static int[] compileRange(final String valuesStr) {
  	final Set<Integer> values = new TreeSet<Integer>();
  	final String[] fragments = valuesStr.split(",");
  	for(String frag: fragments) {
  		frag = frag.trim();
  		if(frag.contains("-")) {
  			Matcher rangeMatcher = intRange.matcher(frag);
  			if(rangeMatcher.matches() && rangeMatcher.groupCount()==2) {
  				rangeMatcher.group();
  				int f1 = Integer.parseInt(rangeMatcher.group(1));
  				int f2 = Integer.parseInt(rangeMatcher.group(2));
  				if(f1==f2) {
  					values.add(f1);
  				} else {
  					int start = f1 > f2 ? f2 : f1;
  					int end = f1 > f2 ? f1 : f2;
  					while(start <= end) {
  						values.add(start);
  						start++;
  					}
  				}
  			}

  		} else {
  			try {
  				if(!frag.endsWith("-*")) {
  					values.add(Integer.parseInt(frag.trim()));
  				}
  			} catch (Exception e) {
  				
  			}
  		}

  	}
  	int[] valuesArr = new int[values.size()];
  	int index = 0;
  	for(Integer i: values) {
  		valuesArr[index] = i;
  		index++;
  	}    	
  	return valuesArr;
  }
  
  public static String buildFromRange(String valuesStr, String delimeter, String...dataCells) {
  	final StringBuilder values = new StringBuilder();
  	final String[] fragments = valuesStr.split(",");
  	for(String frag: fragments) {
  		frag = frag.trim();
  		Matcher rangeMatcher = intRange.matcher(frag);
  		Matcher endMatcher = endRange.matcher(frag);
  		if(rangeMatcher.matches() && rangeMatcher.groupCount()==2) {
  			rangeMatcher.group();
  			int f1 = Integer.parseInt(rangeMatcher.group(1));
  			int f2 = Integer.parseInt(rangeMatcher.group(2));
  			if(f1==f2) {
  				values.append(dataCells[f1]).append(delimeter);
  			} else {
  				int start = f1 > f2 ? f2 : f1;
  				int end = f1 > f2 ? f1 : f2;
  				while(start <= end) {
  					values.append(dataCells[start]).append(delimeter);
  					start++;
  				}
  			}
  		} else if(endMatcher.matches() && endMatcher.groupCount()==1) {
  			endMatcher.group();
  			int f1 = Integer.parseInt(endMatcher.group(1));
  			for(; f1 < dataCells.length; f1++) {
  				values.append(dataCells[f1]).append(delimeter);
  			}
  		} else {
  			try {
  				values.append(dataCells[Integer.parseInt(frag.trim())]).append(delimeter);
  			} catch (Exception e) {}
  		}
  	}
  	values.deleteCharAt(values.length()-1);
  	return values.toString();
  }
  
	
	
	/**
	 * Calculates a low collision hash code for the passed string
	 * @param s The string to calculate the hash code for
	 * @return the long hashcode
	 */
	public static long longHashCode(String s) {
		long h = 0;
        int len = s.length();
    	int off = 0;
    	int hashPrime = s.hashCode();
    	char val[] = s.toCharArray();
        for (int i = 0; i < len; i++) {
            h = (31*h + val[off++] + (hashPrime*h));
        }
        return h;
	}
	
	/**
	 * Returns the longest string value in the passed array
	 * @param objects An array of objects
	 * @return the max length, or zero if array is empty
	 */
	@SafeVarargs
	public static <T> int longest(final T...objects) {
		int max = 0;
		if(objects!=null && objects.length>0) {
			for(T t: objects) {
				if(t==null) continue;
				final int x = t.toString().length();
				if(x>max) max = x;
			}
		}
		return max;
	}
	
	/**
	 * Returns the longest string value in the keyset of the passed map
	 * @param map A T keyed map
	 * @return the max length, or zero if map is empty
	 * @param <T> The assumed type of the return
	 */
	
	public static <T> int longest(final Map<T, ?> map) {
		if(map==null || map.isEmpty()) return 0;
		final T t = map.keySet().iterator().next();
		@SuppressWarnings("unchecked")
		final T[] arr = (T[]) Array.newInstance(t.getClass(), map.size());
		return longest(map.keySet().toArray(arr));
	}
	
	/**
	 * Returns a formated print of the passed map content
	 * @param map The map to print
	 * @return a formated print of the map content
	 */
	public static String printBeanNames(final Map<? extends Object, ? extends Object> map) {
		final StringBuilder b = new StringBuilder();
		final int width = StringHelper.longest(map) + 3;
		for(Entry<? extends Object, ? extends Object> entry: map.entrySet()) {
			try {
				b.append(pad(entry.getKey().toString(), width)).append(" : ").append(entry.getValue().toString());
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
			}
		}
		return b.toString();
	}
	
	/**
	 * Converts the passed stringy, splits out the key value pairs and returns a map of the key/values
	 * @param cs The stringy to split
	 * @param pairDelim The delimeter between the pairs
	 * @param eqDelim The delimeter between the keys and values
	 * @return A map of key value pairs
	 */
	public static Map<String, String> splitKeyValues(final CharSequence cs, final String pairDelim, final String eqDelim) {
		if(cs==null) throw new IllegalArgumentException("The passed CharSequence was null");
		if(pairDelim==null) throw new IllegalArgumentException("The passed pair delimiter was null");
		if(eqDelim==null) throw new IllegalArgumentException("The passed equals delimiter was null");
		final String s = cs.toString().trim();
		if(s.isEmpty()) return Collections.emptyMap();		
		final String[] pairs = s.split(pairDelim);
		final Pattern psplit = Pattern.compile(eqDelim);
		final Map<String, String> map = new LinkedHashMap<String, String>(pairs.length);
		for(String pair: pairs) {
			try {
				String[] kv = psplit.split(pair.trim());
				if(kv.length==2) {
					map.put(kv[0].trim(), kv[1].trim());
				}
			} catch (Exception x) {/* No Op */}
		}
		return map;
	}
	
	/**
	 * Converts the comma separated key value pairs into a key value map
	 * @param cs The stringy to split
	 * @return the map
	 */
	public static Map<String, String> splitKeyValues(final CharSequence cs) {
		return splitKeyValues(cs, ",", "=");
	}
	
	
	/**
	 * Indicates if the passed stringy is an instance of AbstractStringBuilder
	 * @param stringy The stringy to test
	 * @return true if stringy is an instance of AbstractStringBuilder, false otherwise
	 */
	public static boolean isSb(final CharSequence stringy) {
		if(stringy==null) return false;
		return ((stringy instanceof StringBuilder) || ((stringy instanceof StringBuffer)));
	}
	
	/**
	 * Pads the passed stringy with spaces to make it the total width
	 * @param stringy The stringy to pad
	 * @param totalWidth The total width to make the stringy
	 * @return A string made from the stringy plus some white space
	 */
	public static String pad(final CharSequence stringy, final int totalWidth) {
		if(stringy==null) return null;
		if(totalWidth<1) return stringy.toString();
		final int length = stringy.toString().length();
		final int add = totalWidth - length;
		if(add > 0) {
			char[] pads = new char[add];
			Arrays.fill(pads, ' ');
			return stringy.toString() + new String(pads);
		}
		return stringy.toString();		
	}
	
	/**
	 * Calculates a low collision hash code for the passed byte array
	 * @param arr The byte array to calculate the hash code for
	 * @return the long hashcode
	 */
	public static long longHashCode(byte[] arr) {
		long h = 0;
        int len = arr.length;
    	int off = 0;
    	int hashPrime = Arrays.hashCode(arr);
        for (int i = 0; i < len; i++) {
            h = (31*h + arr[off++] + (hashPrime*h));
        }
        return h;
	}
	
	
	/**
	 * Returns the descriptor for the passed member
	 * @param m The class member
	 * @return the member descriptor
	 */
	public static String getMemberDescriptor(final Member m) {
		if(m instanceof Method) {
			return getMethodDescriptor((Method)m);
		} else if(m instanceof Constructor) {
			return getConstructorDescriptor((Constructor<?>)m);
		} else {
			return m.toString();
		}
	}
	
	/**
     * Returns the descriptor corresponding to the given method.
     * @param m a {@link Method Method} object.
     * @return the descriptor of the given method.
     * (Based on getDescriptorfrom ObjectWeb ASM)
     * @author Eric Bruneton  
     * @author Chris Nokleberg
     * @author nwhitehead
     */
    public static String getMethodSignature(final Method m) {
        Class<?>[] parameters = m.getParameterTypes();
        if(parameters.length==0) return "";
        StringBuffer buf = new StringBuffer();        
        for (int i = 0; i < parameters.length; ++i) {
            getDescriptor(buf, parameters[i]);
        }
        return buf.toString();
    }
    
    /**
     * Returns the descriptor corresponding to the given constructor.
     * @param c a {@link Constructor Constructor} object.
     * @return the descriptor of the given constructor.
     * (All credit to ObjectWeb ASM)
     * @author Eric Bruneton  
     * @author Chris Nokleberg
     */
    public static String getConstructorSignature(final Constructor<?> c) {
        Class<?>[] parameters = c.getParameterTypes();
        StringBuffer buf = new StringBuffer();        
        for (int i = 0; i < parameters.length; ++i) {
            getDescriptor(buf, parameters[i]);
        }
        return buf.toString();
    }
	
	
	/**
	 * Returns the signature for the passed member
	 * @param m The class member
	 * @return the member signature
	 */
	@SuppressWarnings("rawtypes")
	public static String getMemberSignature(final Member m) {
		if(m instanceof Method) {
			return getMethodSignature((Method)m);
		} else if(m instanceof Constructor) {
			return getConstructorSignature((Constructor)m);
		} else {
			return m.toString();
		}
	}

	
	
	/**
     * Returns the descriptor corresponding to the given method.
     * @param m a {@link Method Method} object.
     * @return the descriptor of the given method.
     * (All credit to ObjectWeb ASM)
     * @author Eric Bruneton  
     * @author Chris Nokleberg
     */
    public static String getMethodDescriptor(final Method m) {
        Class<?>[] parameters = m.getParameterTypes();
        StringBuilder buf = new StringBuilder();
        buf.append('(');
        for (int i = 0; i < parameters.length; ++i) {
            getDescriptor(buf, parameters[i]);
        }
        buf.append(')');
        getDescriptor(buf, m.getReturnType());
        return buf.toString();
    }
    
    /**
     * Returns the descriptor corresponding to the given constructor.
     * @param c a {@link Constructor Constructor} object.
     * @return the descriptor of the given constructor.
     * (All credit to ObjectWeb ASM)
     * @author Eric Bruneton  
     * @author Chris Nokleberg
     */
    public static String getConstructorDescriptor(final Constructor<?> c) {
        Class<?>[] parameters = c.getParameterTypes();
        StringBuilder buf = new StringBuilder();
        buf.append('(');
        for (int i = 0; i < parameters.length; ++i) {
            getDescriptor(buf, parameters[i]);
        }
        return buf.append(")V").toString();
    }
    
    /**
     * Builds a descriptor from an array of args
     * @param args The arguments
     * @return the descriptor
     */
    public static String getDescriptor(Object...args) {
    	final StringBuilder buf = new StringBuilder("(");
    	for(Object o: args) {
    		if(o==null) throw new RuntimeException("Array of args had a null");
    		getDescriptor(buf, o.getClass());
    	}
    	return buf.append(")").toString();
    }
    
    /**
     * Builds a descriptor from an array of classes
     * @param sig The signature
     * @return the descriptor
     */
    public static String getDescriptor(final Class<?>...sig) {
    	final StringBuilder buf = new StringBuilder("(");
    	for(Class<?> clazz: sig) {
    		if(clazz==null) throw new RuntimeException("Array of sig had a null");
    		getDescriptor(buf, clazz);
    	}
    	return buf.append(")").toString();
    }
    
    /**
     * Returns a deep toStringed string array built from the names of the passed classes
     * @param signature A class array
     * @return the concat value
     */
    public static String concat(Class<?>...signature) {
    	String[] strs = new String[signature.length];
    	for(int i = 0; i < signature.length; i++) {
    		strs[i] = signature[i].getName();
    	}
    	return Arrays.toString(strs);
    }

    /**
     * Appends the descriptor of the given class to the given string buffer.
     * @param buf the string buffer to which the descriptor must be appended.
     * @param c the class whose descriptor must be computed.
     * (All credit to ObjectWeb ASM)
     * @author Eric Bruneton  
     * @author Chris Nokleberg
     */
    public static void getDescriptor(final StringBuilder buf, final Class<?> c) {
        Class<?> d = c;
        while (true) {
            if (d.isPrimitive()) {
                char car;
                if (d == Integer.TYPE) {
                    car = 'I';
                } else if (d == Void.TYPE) {
                    car = 'V';
                } else if (d == Boolean.TYPE) {
                    car = 'Z';
                } else if (d == Byte.TYPE) {
                    car = 'B';
                } else if (d == Character.TYPE) {
                    car = 'C';
                } else if (d == Short.TYPE) {
                    car = 'S';
                } else if (d == Double.TYPE) {
                    car = 'D';
                } else if (d == Float.TYPE) {
                    car = 'F';
                } else /* if (d == Long.TYPE) */{
                    car = 'J';
                }
                buf.append(car);
                return;
            } else if (d.isArray()) {
                buf.append('[');
                d = d.getComponentType();
            } else {
                buf.append('L');
                String name = d.getName();
                int len = name.length();
                for (int i = 0; i < len; ++i) {
                    char car = name.charAt(i);
                    buf.append(car == '.' ? '/' : car);
                }
                buf.append(';');
                return;
            }
        }
    }

	
	/**
	 * Caps the first letter in the passed string
	 * @param cs The string value to initcap
	 * @return the initcapped string
	 */
	public static String initCap(CharSequence cs) {
		char[] chars = cs.toString().trim().toCharArray();
		chars[0] = new String(new char[]{chars[0]}).toUpperCase().charAt(0);
		return new String(chars);
	}

	/**
	 * Returns a formatted string representing the thread identified by the passed id
	 * @param id The id of the thread
	 * @return the formatted message
	 */
	public static String formatThreadName(long id) {
		if(id<1) return "[Nobody]";
		ThreadInfo ti = tmx.getThreadInfo(id);		
		if(ti==null)  return String.format("No Such Thread [%s]", id);
		return String.format("[%s/%s]", ti.getThreadName(), ti.getThreadId());
	}
	
	
	/**
	 * Returns a formatted string presenting the passed elapsed time in the native nanos, microseconds, milliseconds and seconds.
	 * @param title The arbitrary name for the timing
	 * @param nanos The elapsed time in nanos
	 * @return the formatted message
	 */
	public static String reportTimes(String title, long nanos) {
		StringBuilder b = new StringBuilder(title).append(":  ");
		b.append(nanos).append( " ns.  ");
		b.append(TimeUnit.MICROSECONDS.convert(nanos, TimeUnit.NANOSECONDS)).append( " \u00b5s.  ");
		b.append(TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS)).append( " ms.  ");
		b.append(TimeUnit.SECONDS.convert(nanos, TimeUnit.NANOSECONDS)).append( " s.");
		return b.toString();
	}
	
	/**
	 * Returns a formatted string presenting the average elapsed time 
	 * based on the passed time stamp and count of incidents in the native nanos, microseconds, milliseconds and seconds.
	 * @param title The arbitrary name for the timing
	 * @param nanos The elapsed time in nanos
	 * @param count The number of incidents, used for calculating an average
	 * @return the formatted message
	 */
	public static String reportAvgs(String title, long nanos, long count) {
		if(nanos==0 || count==0) return reportTimes(title, 0);
		return reportTimes(title, (nanos/count));
	}
	
	/**
	 * Returns a formatted string presenting the total and average elapsed time 
	 * based on the passed time stamp and count of incidents in the native nanos, microseconds, milliseconds and seconds.
	 * @param title The arbitrary name for the timing
	 * @param nanos The elapsed time in nanos
	 * @param count The number of incidents, used for calculating an average
	 * @return the formatted message
	 */
	public static String reportSummary(String title, long nanos, long count) {
		return reportTimes(title, nanos) + 
				"\n" +
				reportAvgs(title + "  AVGS", nanos, count);
	}

	private static final ThreadLocal<WeakReference<StringBuilder>> stringBuilderCache = new ThreadLocal<WeakReference<StringBuilder>>() {
		@Override
		protected WeakReference<StringBuilder> initialValue() {			
			return new WeakReference<StringBuilder>(new StringBuilder());
		}
	};
	
	private static final ThreadLocal<WeakReference<StringBuffer>> stringBufferCache = new ThreadLocal<WeakReference<StringBuffer>>() {
		@Override
		protected WeakReference<StringBuffer> initialValue() {			
			return new WeakReference<StringBuffer>(new StringBuffer());
		}
	};
	
	
	/**
	 * Acquires and truncates the current thread's StringBuilder.
	 * @return A truncated string builder for use by the current thread.
	 */
	public static StringBuilder getStringBuilder() {
		StringBuilder b = stringBuilderCache.get().get();
		if(b==null) {
			b = new StringBuilder();
			stringBuilderCache.set(new WeakReference<StringBuilder>(b));
			return b;
		}
		b.setLength(0);
		return b;
	}
	
	/**
	 * Acquires and truncates the current thread's StringBuffer.
	 * @return A truncated string buffer for use by the current thread.
	 */
	public static StringBuffer getStringBuffer() {
		StringBuffer b = stringBufferCache.get().get();
		if(b==null) {
			b = new StringBuffer();
			stringBufferCache.set(new WeakReference<StringBuffer>(b));
			return b;
		}
		b.setLength(0);
		return b;
	}	
	
	
	/** The token matcher */
	public static final Pattern NUMERIC_TOKEN = Pattern.compile("\\$\\{(\\d+)?\\}");

	/**
	 * <p>Replaces numeric tokens in the format <b><code>${#}</code></b> using the token decode in the provided decodes
	 * with the index found in the token.</p>
	 * <p>e.g. An expression <b><code>"gc.${1}=name=${0},service=GCMonitor"</code></b> with a set of tokens
	 * <b><code>{"Scavenge", "time"}</code></b> will resolve to  <b><code>"gc.time=name=Scavenge,service=GCMonitor"</code></b></p>
	 * @param expression The expression to resolve
	 * @param decodes The token decodes
	 * @return The replaced expression
	 */
	public static String replaceNumericTokens(final CharSequence expression, final String...decodes) {
		if(expression==null) throw new IllegalArgumentException("The passed expression was null");
		if(decodes.length==0) return expression.toString();
		final Matcher m = NUMERIC_TOKEN.matcher(expression);
		final StringBuffer b = getStringBuffer();
		int index = -1;
		while(m.find()) {
			index = Integer.parseInt(m.group(1));
			if(index < 0 || index-1 > decodes.length) throw new IllegalArgumentException("Invalid token [" + m.group(0) + "] in expression [" + expression + "] with decodes " + Arrays.toString(decodes));			
			m.appendReplacement(b, decodes[index]);
		}
		m.appendTail(b);
		return b.toString();
	}
	
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * Escapes quote characters in the passed string
	 * @param s The string to esacape
	 * @return the escaped string
	 */
	public static String escapeQuotes(CharSequence s) {
		return s.toString().replace("\"", "\\\"");
	}
	
	/**
	 * Escapes json characters in the passed string
	 * @param s The string to esacape
	 * @return the escaped string
	 */
	public static String jsonEscape(CharSequence s) {
		return s.toString().replace("\"", "\\\"").replace("[", "\\[").replace("]", "\\]").replace("{", "\\{").replace("}", "\\}");
	}
	
	
	/**
	 * Acquires and truncates the current thread's StringBuilder.
	 * @param size the inited size of the stringbuilder
	 * @return A truncated string builder for use by the current thread.
	 */
	public static StringBuilder getStringBuilder(int size) {
		return new StringBuilder(size);
	}
	
	/**
	 * Concatenates all the passed strings
	 * @param args The strings to concatentate
	 * @return the concatentated string
	 */
	public static String fastConcat(CharSequence...args) {
		StringBuilder buff = getStringBuilder();
		for(CharSequence s: args) {
			if(s==null) continue;
			buff.append(s);
		}
		return buff.toString();
	}
	
	/**
	 * Accepts an array of strings and returns the array flattened into a single string, optionally delimeted.
	 * @param skipBlanks If true, blank or null items in the passed array will be skipped.
	 * @param delimeter The delimeter to insert between each item.
	 * @param args The string array to flatten
	 * @return the flattened string
	 */
	public static String fastConcatAndDelim(boolean skipBlanks, String delimeter, CharSequence...args) {
		StringBuilder buff = getStringBuilder();
		if(args!=null && args.length > 0) {			
			for(CharSequence s: args) {				
				if(!skipBlanks || (s!=null && s.length()>0)) {
					buff.append(s).append(delimeter);
				}
			}
			if(buff.length()>0) {
				buff.deleteCharAt(buff.length()-1);
			}
		}
		return buff.toString();
	}
	
	/**
	 * Accepts an array of strings and returns the array flattened into a single string, optionally delimeted.
	 * Blank or zero length items in the array will be skipped.
	 * @param delimeter The delimeter to insert between each item.
	 * @param args The string array to flatten
	 * @return the flattened string
	 */
	public static String fastConcatAndDelim(String delimeter, CharSequence...args) {
		return fastConcatAndDelim(true, delimeter, args);
	}
	
	/**
	 * Accepts an array of strings and returns the array flattened into a single string, optionally delimeted.
	 * @param skip Skip this many
	 * @param delimeter The delimeter
	 * @param args The strings to concat
	 * @return the resulting string
	 */
	public static String fastConcatAndDelim(int skip, String delimeter, CharSequence...args) {
		StringBuilder buff = getStringBuilder();
		int cnt = args.length - skip;
		int i = 0;
		for(; i < cnt; i++) {
			if(args[i] != null && args[i].length() > 0) {
				buff.append(args[i]).append(delimeter);
			}
		}
		StringBuilder b = buff.reverse();
		while(b.subSequence(0, delimeter.length()).equals(delimeter)) {
			b.delete(0, delimeter.length());
		}
		return b.reverse().toString();
	}
	
	/**
	 * Formats the stack trace of the passed throwable and generates a formatted string.
	 * @param t The throwable
	 * @return A string representing the stack trace.
	 */
	public static String formatStackTrace(Throwable t) {
		if(t==null) return "";
		StackTraceElement[] stacks = t.getStackTrace();
		StringBuilder b = new StringBuilder(stacks.length * 50);
		for(StackTraceElement ste: stacks) {
			b.append("\n\t").append(ste.toString());
		}
		return b.toString();
	}
	
	/**
	 * Formats the stack trace of the passed thread and generates a formatted string.
	 * @param t The thread
	 * @param full If true, captures the threads monitors and locks in the stack trace
	 * at the cost of a slightly more expensive operation, otherwise creates a simple stack trace.
	 * @return A string representing the stack trace of the passed thread
	 */
	public static String formatStackTrace(final Thread t, final boolean full) {
		if(t==null) return "";
		if(full) return fullThreadStatus(t).toString();
		StackTraceElement[] stacks = t.getStackTrace();
		StringBuilder b = new StringBuilder(stacks.length * 50);
		for(StackTraceElement ste: stacks) {
			b.append("\n\t").append(ste.toString());
		}
		return b.toString();
	}
	
	
	/**
	 * Returns a full thread state dump
	 * @param t The thread to report on
	 * @return the thread state dump
	 */
	public static CharSequence fullThreadStatus(final Thread t) {
		if(t==null) return "";
		final ThreadInfo ti = tmx.getThreadInfo(new long[]{t.getId()}, true, true)[0];		
		final StringBuilder b = new StringBuilder("Thread:").append(t.toString()).append(":");
		b.append("\n\tState:").append(t.getState().name());
		b.append("\n\tDaemon:").append(t.isDaemon());
		b.append("\n\tInterrupted:").append(t.isInterrupted());
		b.append("\n\tPriority:").append(t.getPriority());
		b.append("\n\tIn Native:").append(ti.isInNative());
		final LockInfo li = ti.getLockInfo();
		if(li!=null) {
			b.append("\n\tLocked On:");
			b.append("\n\t\tLock:").append(li.toString());
			b.append("\n\t\tOwned By:").append(ti.getLockOwnerName());
		}
		final LockInfo[] ownableSynchronizers = ti.getLockedSynchronizers();
		if(ownableSynchronizers.length!=0) {
			b.append("\n\tOwnable Synchronizers:");
			for(LockInfo ownedLock : ownableSynchronizers) {
				b.append("\n\t\t").append(ownedLock.toString());
			}
		}
		b.append("\n\tStack:");
		final StackTraceElement[] stack = ti.getStackTrace();		
		final MonitorInfo[] monitors = ti.getLockedMonitors();
		if(monitors.length == 0) {
			for(StackTraceElement element: stack) {
				b.append("\n\t\t").append(element.toString());
			}
		} else {
			//final LinkedList<String> strStack = new LinkedList<String>(Arrays.asList(stack).stream().map(StackTraceElement::toString).collect(Collectors.toList()));
			final LinkedList<String> strStack = new LinkedList<String>();
			for(StackTraceElement element: stack) {
				strStack.add("\n\t\t" + element.toString());
			}
			for(MonitorInfo monitor: monitors) {
				try {
					strStack.add(monitor.getLockedStackDepth(), "\n\t\t  Monitor:" + monitor.toString());
				} catch (Exception x) {/* No Op */}
			}			
			for(String s: strStack) {
				b.append(s);
			}
		}
		return b;
		
	}
	
	/**
	 * Formats the simple stack trace of the passed thread and generates a formatted string.
	 * @param t The thread
	 * @return A string representing the stack trace of the passed thread
	 */
	public static String formatStackTrace(final Thread t) {
		return formatStackTrace(t, false);
	}	

	
    /**
     * Tests whether string s is matched by pattern p.
     * Supports "?", "*", "[", each of which may be escaped with "\";
     * character classes may use "!" for negation and "-" for range.
     * Not yet supported: internationalization; "\" inside brackets.<P>
     * Wildcard matching routine by Karl Heuer.  Public Domain.<P> 
     * @param s The string to test
     * @param p The pattern to test against
     * @return true for a match, false otherwise
     */
    public static boolean wildmatch(final String s, final String p) {
        char c;
        int si = 0, pi = 0;
        int slen = s.length();
        int plen = p.length();

        while (pi < plen) { // While still string
            c = p.charAt(pi++);
            if (c == '?') {
                if (++si > slen)
                    return false;
            } else if (c == '[') { // Start of choice
                if (si >= slen)
                    return false;
                boolean wantit = true;
                boolean seenit = false;
                if (p.charAt(pi) == '!') {
                    wantit = false;
                    ++pi;
                }
                while ((c = p.charAt(pi)) != ']' && ++pi < plen) {
                    if (p.charAt(pi) == '-' &&
                        pi+1 < plen &&
                        p.charAt(pi+1) != ']') {
                        if (s.charAt(si) >= p.charAt(pi-1) &&
                            s.charAt(si) <= p.charAt(pi+1)) {
                            seenit = true;
                        }
                        ++pi;
                    } else {
                        if (c == s.charAt(si)) {
                            seenit = true;
                        }
                    }
                }
                if ((pi >= plen) || (wantit != seenit)) {
                    return false;
                }
                ++pi;
                ++si;
            } else if (c == '*') { // Wildcard
                if (pi >= plen)
                    return true;
                do {
                    if (wildmatch(s.substring(si), p.substring(pi)))
                        return true;
                } while (++si < slen);
                return false;
            } else if (c == '\\') {
                if (pi >= plen || si >= slen ||
                    p.charAt(pi++) != s.charAt(si++))
                    return false;
            } else {
                if (si >= slen || c != s.charAt(si++)) {
                    return false;
                }
            }
        }
        return (si == slen);
    }
    
	/**
	 * Converts the passed bytes to a hex string
	 * @param bytes the bytes to convert
	 * @return the hex string or null if the bytes were null or zero length
	 */
	public static String bytesToHex(final byte[] bytes) {
		return bytes==null ? "" : DatatypeConverter.printHexBinary(bytes);
	}
	
	/**
	 * Converts the passed hex string to a byte array
	 * @param s the hex string to convert
	 * @return the byte array or null if the hex string was null or empty
	 */
	public static byte[] hexToBytes(final String s) {
		if (s == null || s.isEmpty()) {
			return null;
		}
		String id = s;
		if(id.length() % 2 > 0) {
			id = "0" + id;
		}	      
		return DatatypeConverter.parseHexBinary(id);		
	}
    
	
}
