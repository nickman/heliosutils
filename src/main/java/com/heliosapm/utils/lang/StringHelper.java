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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	@SafeVarargs
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
				b.append("\n\t").append(pad(entry.getKey().toString(), width)).append(" : ").append(entry.getValue().toString());
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


	/**
	 * Acquires and truncates the current thread's StringBuilder.
	 * @return A truncated string builder for use by the current thread.
	 */
	public static StringBuilder getStringBuilder() {
		return new StringBuilder();
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
	 * @return A string representing the stack trace of the passed thread
	 */
	public static String formatStackTrace(Thread t) {
		if(t==null) return "";
		StackTraceElement[] stacks = t.getStackTrace();
		StringBuilder b = new StringBuilder(stacks.length * 50);
		for(StackTraceElement ste: stacks) {
			b.append("\n\t").append(ste.toString());
		}
		return b.toString();
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
	
}
