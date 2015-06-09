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
package com.heliosapm.utils.xml;

import static javax.xml.xpath.XPathConstants.NODE;
import static javax.xml.xpath.XPathConstants.NODESET;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * <p>Title: XMLHelper</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.utils.xml.XMLHelper</code></p>
 */

public class XMLHelper {
	static {
		String xmlParser = System.getProperty("org.xml.sax.parser");
		if(xmlParser == null) {
			System.setProperty("org.xml.sax.parser", "com.sun.org.apache.xerces.internal.parsers.SAXParser");
		}		
	}
	
	/** The standard auto-generated XML header */
	public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

	  /**
	   * Searches throgh the passed NamedNodeMap for an attribute and returns it if it is found.
	   * If it is not found, returns a null.
	   * @param nnm NamedNodeMap
	   * @param name String
	   * @return String
	   */
	  public static String getAttributeValueByName(NamedNodeMap nnm, String name) {
	    for(int i = 0; i < nnm.getLength(); i++) {
	      Attr attr = (Attr)nnm.item(i);
	      if(attr.getName().equalsIgnoreCase(name)) {
	        return attr.getValue();
	      }
	    }
	    return null;
	  }
	  
	/**
	 * Returns the attribute value for the passed name in the passed node.
	 * @param node the node to get the attribute from
	 * @param name the name of the attribute
	 * @param defaultValue the value to return if the node did not contain the attribute
	 * @return The attribute value or the default value if it is not found.
	 */
	public static String getAttributeByName(Node node, String name, String defaultValue) {
		try {
			String val = getAttributeValueByName(node, name);
			if(val!=null && !val.trim().isEmpty()) return val.trim();
		} catch (Exception e) {}
		return defaultValue;		
	}
	
	/**
	 * Returns the long attribute value for the passed name in the passed node.
	 * @param node the node to get the attribute from
	 * @param name the name of the attribute
	 * @param defaultValue the value to return if the node did not contain the attribute
	 * @return The attribute value or the default value if it is not found.
	 */
	public static long getLongAttributeByName(Node node, String name, long defaultValue) {
		String s = getAttributeByName(node, name, null);
		if(s==null) return defaultValue;
		try {
			return Long.parseLong(s.trim());
		} catch (Exception e) {
			return defaultValue;
		}
	}
	  
	/**
	 * Determines if the passed node has a node with the passed name
	 * @param node The node to search in
	 * @param name The name of the node to search for
	 * @param caseSensitive true if the name matching should be case sensitive, false otherwise
	 * @return true if the child node was found, false otherwise
	 */
	public static boolean hasChildNodeByName(final Node node, final String name, final boolean caseSensitive) {		
		return XMLHelper.getChildNodeByName(node, name, caseSensitive) != null;
	}

	/**
	 * Determines if the passed node has a node with the passed name with no case sensitivity
	 * @param node The node to search in
	 * @param name The name of the node to search for
	 * @return true if the child node was found, false otherwise
	 */
	public static boolean hasChildNodeByName(final Node node, final String name) {		
		return hasChildNodeByName(node, name, false);
	}
	
	/**
	 * Returns the attribute value for the passed name in the passed node.
	 * @param node the node to get the attribute from
	 * @param name the name of the attribute
	 * @return The attribute value or null if it is not found.
	 */
	public static String getAttributeValueByName(Node node, String name) {
		  return getAttributeValueByName(node.getAttributes(), name);
	  }

	  /**
	   * Searches throgh the passed NamedNodeMap for an attribute. If it is found, it will try to convert it to a boolean.
	   * @param nnm NamedNodeMap
	   * @param name String
	   * @throws RuntimeException on any failure to parse a boolean
	   * @return boolean
	   */
	  public static boolean getAttributeBooleanByName(NamedNodeMap nnm, String name) throws RuntimeException {
	    for(int i = 0; i < nnm.getLength(); i++) {
	      Attr attr = (Attr)nnm.item(i);
	      if(attr.getName().equalsIgnoreCase(name)) {
	        String tmp =  attr.getValue().toLowerCase();
	        if(tmp.equalsIgnoreCase("true")) return true;
	        if(tmp.equalsIgnoreCase("false")) return false;
	        throw new RuntimeException("Attribute " + name + " value not boolean:" + tmp);
	      }
	    }
	    throw new RuntimeException("Attribute " + name + " not found.");
	  }
	  
    /**
     * Returns the value of a named node attribute in the form of a boolean
	 * @param node The node to retrieve the attribute from
	 * @param name The name of the attribute
	 * @param defaultValue The default value if the attribute cannot be located or converted.
	 * @return true or false
	 */
	public static boolean getAttributeByName(Node node, String name, boolean defaultValue) {
		  if(node==null || name==null) return defaultValue;
		  try {
			  return getAttributeBooleanByName(node.getAttributes(), name);
		  } catch (Exception e) {
			  return defaultValue;
		  }
	  }
	
    /**
     * Returns the value of a named node attribute in the form of an int
	 * @param node The node to retrieve the attribute from
	 * @param name The name of the attribute
	 * @param defaultValue The default value if the attribute cannot be located or converted.
	 * @return an int
	 */
	public static int getAttributeByName(Node node, String name, int defaultValue) {
		  if(node==null || name==null) return defaultValue;
		  try {
			  return new Double(getAttributeValueByName(node, name).trim()).intValue();
		  } catch (Exception e) {
			  return defaultValue;
		  }
	  }
	
    /**
     * Returns the value of a named node attribute in the form of a long
	 * @param node The node to retrieve the attribute from
	 * @param name The name of the attribute
	 * @param defaultValue The default value if the attribute cannot be located or converted.
	 * @return a long
	 */
	public static long getAttributeByName(Node node, String name, long defaultValue) {
		  if(node==null || name==null) return defaultValue;
		  try {
			  return new Double(getAttributeValueByName(node, name).trim()).longValue();
		  } catch (Exception e) {
			  return defaultValue;
		  }
	  }


	  /**
	   * Helper Method. Searches through the child nodes of a node and returns the first node with a matching name.
	   * @param xnode the node to get the child node from
	   * @param name the name of the child node to search for
	   * @param caseSensitive boolean true for case sensitive name matching, false otherwise
	   * @return the located node or null if one was not found
	   */

	  public static Node getChildNodeByName(final Node xnode, final String name, final boolean caseSensitive) {
	    NodeList list = xnode.getChildNodes();
	    for(int i = 0; i < list.getLength(); i++) {
	      Node node = list.item(i);
	      if(caseSensitive) {
	        if(node.getNodeName().equals(name)) return node;
	      } else {
	        if(node.getNodeName().equalsIgnoreCase(name)) return node;
	      }
	    }
	    return null;
	  }

	  /**
	   * Helper Method. Searches through the child nodes of a node and returns the first node with a matching name, case insensitive.
	   * @param xnode the node to get the child node from
	   * @param name the name of the child node to search for
	   * @return the located node or null if one was not found
	   */

	  public static Node getChildNodeByName(final Node xnode, final String name) {
	  	return getChildNodeByName(xnode, name);
	  }



	  /**
	   * Helper Method. Searches through the child nodes of an element and returns an array of the matching nodes.
	   * @param element Element
	   * @param name String
	   * @param caseSensitive boolean
	   * @return ArrayList
	   */
	  public static List<Node> getChildNodesByName(Node element, String name, boolean caseSensitive) {
	    ArrayList<Node> nodes = new ArrayList<Node>();
	    NodeList list = element.getChildNodes();
	    for (int i = 0; i < list.getLength(); i++) {
	      Node node = list.item(i);
	      if (caseSensitive) {
	        if (node.getNodeName().equals(name)) nodes.add(node);
	      }
	      else {
	        if (node.getNodeName().equalsIgnoreCase(name)) nodes.add(node);
	      }
	    }
	    return nodes;
	  }
	  
	/**
	 * Parses an input source and generates an XML document.
	 * @param is An input source to an XML source.
	 * @return An XML doucument.
	 */
	public static Document parseXML(InputSource is) {
		  try {
			  Document doc = null;
			  DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			  doc = documentBuilder.parse(is);		  
		  return doc;
		  } catch (Exception e) {
			  throw new RuntimeException("Failed to parse XML source", e);
		  }
	}
	  
	  
	/**
	 * Parses an input stream and generates an XML document.
	 * @param is An input stream to an XML source.
	 * @return An XML doucument.
	 */
	public static Document parseXML(InputStream is) {
		return parseXML(new InputSource(is));
	}
	
	/**
	 * Parses a file and generates an XML document.
	 * @param file The file to parse
	 * @return An XML doucument.
	 */
	public static Document parseXML(File file) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			return parseXML(fis);
		} catch (Exception e) {
			throw new RuntimeException("Failed to open XML file:" + file, e);
		} finally {
			try { fis.close(); } catch (Exception e) {}
		}		
	}
	
	/**
	 * Parses the input stream of a URL and generates an XML document.
	 * @param xmlUrl The URL of the XML document.
	 * @return The parsed document.
	 */
	public static Document parseXML(URL xmlUrl) {
		InputStream is = null;
		BufferedInputStream bis = null;
		try {
			is = xmlUrl.openConnection().getInputStream();
			bis = new BufferedInputStream(is);
			return parseXML(bis);
		} catch (Exception e) {
			throw new RuntimeException("Failed to read XML URL:" + xmlUrl, e);
		} finally {
			try {bis.close();} catch (Exception e) {}
			try {is.close();} catch (Exception e) {}
		}
	}
	
	/**
	 * Parses an XML string and generates an XML document.
	 * @param xml The XML to parse
	 * @return An XML doucument.
	 */
	public static Document parseXML(CharSequence xml) {
		StringReader sr = new StringReader(xml.toString());
		return parseXML(new InputSource(sr));
	}
	
	/**
	 * Renders an XML node to a string
	 * @param node The xml node to render
	 * @param stripHeader If true, strips the auto-generated XML header from the generated string
	 * @return the rendered string or null if it failed conversion
	 */
	public static String renderNode(final Node node, final boolean stripHeader) {
		if(node==null) return null;
		try {
			StringWriter writer = new StringWriter();
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.transform(new DOMSource(node), new StreamResult(writer));
			final String s = writer.toString();
			if(stripHeader) {
				return s.replace(XML_HEADER, "");
			}
			return s;
		} catch (Throwable e) {
			return null;
		}		
	}
	
	/**
	 * Renders an XML node to a string
	 * @param node The xml node to render
	 * @return the rendered string or null if it failed conversion
	 */
	public static String renderNode(final Node node) {
		return renderNode(node, false);
	}

	
	/**
	 * Writes an element out to a file.
	 * @param element The XML element to write out.
	 * @param fileName The file name to write to. Existing file is overwriten.
	 */
	public static void writeElement(Element element, String fileName) {
		File file = new File(fileName);
		file.delete();
		DOMSource domSource = new DOMSource(element); 
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			StreamResult result = new StreamResult(fos);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.transform(domSource, result);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			throw new RuntimeException("Failed to write XML element to:" + fileName, e);
		} finally {
			try { fos.flush(); } catch (Exception e) {}
			try { fos.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Locates the attribute defined by the XPath expression in the XML file and replaces it with the passed value.
	 * @param fileName The XML file to update.
	 * @param xPathExpression An XPath expression that locates the attribute to update.
	 * @param attributeName The name of the attribute to update.
	 * @param value The value to update the attribute to.
	 */
	public static void updateAttributeInXMLFile(String fileName, String xPathExpression, String attributeName, String value) {
		try {
			Document document = parseXML(new File(fileName));			
			XPath xpath = XPathFactory.newInstance().newXPath();
			XPathExpression xpathExpression = xpath.compile(xPathExpression);
			Element element = (Element)xpathExpression.evaluate(document, NODE);
			
			element.getAttributeNode(attributeName).setValue(value);
			writeElement(document.getDocumentElement(), fileName);
			
		} catch (Exception e) {
			throw new RuntimeException("Failed to extract element from:" + fileName, e);
		}				
	}
	
	/**
	 * Returns the text inside the passed XML node.
	 * @param node The node to extract from
	 * @param defaultValue The default value if no text is found
	 * @return the extracted text
	 */
	public static String getNodeTextValue(final Node node, final String defaultValue) {
		try {
			String s = node.getFirstChild().getNodeValue();
			return s==null ? defaultValue : s;
		} catch (Exception ex) {
			return defaultValue;
		}
	}	
	
	/**
	 * Determines if the passed node has an attribute with the passed name
	 * @param node The node to inspect
	 * @param name The name to match
	 * @return true if the named attribute was found, false otherwise
	 */
	public static boolean hasAttribute(final Node node, final String name) {
		return getAttributeByName(node, name, null)!=null;
	}

	/**
	 * Returns the text inside the passed XML node.
	 * @param node The node to extract from
	 * @return the extracted text or null if no text was found
	 */
	public static String getNodeTextValue(final Node node) {
		return getNodeTextValue(node, null);
	}
	
	/**
	 * Returns the long hash code for the content in the passed node
	 * @param node the node to compute the long hash code for 
	 * @return the long hash code
	 */
	public static long longHashCode(final Node node) {
		final String s = getNodeTextValue(node);
		return longHashCode(s);
	}
	
	/**
	 * Pretty crap long hashcode algo. FIXME
	 * @param s The string to get the long hash code for
	 * @return the long has code
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
	 * Locates the element defined by the XPath expression in the XML file and replaces the child text with the passed value.
	 * @param fileName The XML file to update.
	 * @param xPathExpression An XPath expression that locates the element to update.
	 * @param value The value to update the attribute to.
	 */
	public static void updateElementValueInXMLFile(String fileName, String xPathExpression, String value) {
		try {
			Document document = parseXML(new File(fileName));			
			XPath xpath = XPathFactory.newInstance().newXPath();
			XPathExpression xpathExpression = xpath.compile(xPathExpression);
			Element element = (Element)xpathExpression.evaluate(document, NODE);
			element.getFirstChild().setNodeValue(value);		
			writeElement(document.getDocumentElement(), fileName);			
		} catch (Exception e) {
			throw new RuntimeException("Failed to extract element from:" + fileName, e);
		}				
	}	
	
	/**
	 * Returns the string value of the named attribute in an XML file at the coordinates specified by the passed XPath expression 
	 * @param fileName The file name
	 * @param xPathExpression The XPath expression
	 * @param attributeName The attribute name
	 * @return the attribute valye
	 */
	public static String getAttribute(String fileName, String xPathExpression, String attributeName) {
		try {
			Document document = parseXML(new File(fileName));			
			XPath xpath = XPathFactory.newInstance().newXPath();
			XPathExpression xpathExpression = xpath.compile(xPathExpression);
			Node node = (Node)xpathExpression.evaluate(document, NODE);
			return getAttributeValueByName(node, attributeName);
		} catch (Exception e) {
			throw new RuntimeException("Failed to extract element from:" + fileName, e);
		}		
	}
	
//	public static void main(String[] args) {
//		log("XMLHelper");
//		if(args.length < 1) return;
//		if(args[0].equalsIgnoreCase("updateAttributeInXMLFile")) {
//			if(args.length != 5) {
//				System.err.println("Invalid Argument Count. Usage: updateAttributeInXMLFile <fileName> <xpath> <attributeName> <newValue>");
//			}
//			updateAttributeInXMLFile(args[1], args[2], args[3], args[4]);
//		} else if(args[0].equalsIgnoreCase("updateElementValueInXMLFile")) {
//			if(args.length != 4) {
//				System.err.println("Invalid Argument Count. Usage: updateElementValueInXMLFile <fileName> <xpath> <newValue>");
//			}
//			updateElementValueInXMLFile(args[1], args[2], args[3]);			
//		}
//		
//	}
	
//	public static void log(Object message) {
//		System.out.println(message);
//	}
	
	/**
	 * Uses the passed XPath expression to locate a set of nodes in the passed element.
	 * @param targetNode The node to search.
	 * @param expression The XPath expression.
	 * @return A list of located nodes.
	 */
	public static List<Node> xGetNodes(Node targetNode, String expression) {
		List<Node> nodes = new ArrayList<Node>();		
		XPath xpath = null;
		try {
			xpath = XPathFactory.newInstance().newXPath();
			XPathExpression xpathExpression = xpath.compile(expression);
			NodeList nodeList = (NodeList)xpathExpression.evaluate(targetNode, NODESET);
			if(nodeList!=null) {
				for(int i = 0; i < nodeList.getLength(); i++) {
					nodes.add(nodeList.item(i));
				}
			}
			return nodes;
		} catch (Exception e) {
			throw new RuntimeException("XPath:Failed to locate the nodes:" + expression, e);
		}		
	}
	
	/**
	 * Uses the passed XPath expression to locate a single node in the passed element.
	 * @param targetNode The node to search.
	 * @param expression The XPath expression.
	 * @return The located node or null if one is not found.
	 */
	public static Node xGetNode(Node targetNode, String expression) {
		Node node = null;		
		XPath xpath = null;
		try {
			xpath = XPathFactory.newInstance().newXPath();
			XPathExpression xpathExpression = xpath.compile(expression);
			node = (Node)xpathExpression.evaluate(targetNode, NODE);
			return node;
		} catch (Exception e) {
			throw new RuntimeException("XPath:Failed to locate the node:" + expression, e);
		}		
	}
	
	/**
	 * Converts a node to a string.
	 * @param node The node to convert.
	 * @return A string representation of the node.
	 */
	public static String getStringFromNode(Node node) {
		DOMSource domSource = new DOMSource(node);
		ByteArrayOutputStream baos = null;
		try {
			baos = new ByteArrayOutputStream();
			StreamResult result = new StreamResult(baos);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.transform(domSource, result);
			baos.flush();
			return new String(baos.toByteArray());
		} catch (Exception e) {
			throw new RuntimeException("Failed to stream node to string", e);
		} finally {
			try { baos.close(); } catch (Exception e) {}
		}
		
		
	}
	

}
