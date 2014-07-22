package com.flyingspaniel.xen;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;

/**
 * Mimics Groovy's XmlParser
 *
 * DefaultHandler for use with a SaxParser to create a tree of Xens.
 * Or use the parse() or parseText() shortcuts.   Example use:
 * <pre><code>
    GXmlParser xmlParser = new GXmlParser();
    Xen root = xmlParser.parse(someKindOfInput, handler);
 * </code> </pre>
 *
 * @author Morgan Conrad
 * @see <a href="http://opensource.org/licenses/MIT">This software is released under the MIT License</a>
 * @since Copyright (c) 2014 by Morgan Conrad
 */
public class XenParser extends DefaultHandler2 {

   Xen current = null;
   Xen root = null;
   boolean trimWhitespace = true;

   final SAXParser saxParser;


   /**
    * Full constructor
    * @param validating
    * @param namespaceAware
    */
   public XenParser(boolean validating, boolean namespaceAware) throws ParserConfigurationException, SAXException {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating(validating);
      factory.setNamespaceAware(namespaceAware);

      saxParser = factory.newSAXParser();
   }


   public XenParser() throws ParserConfigurationException, SAXException {
      this(false, false);
   }

   /**
    * Should you want complete control to provide your own Parser
    * @param saxParser  non-null
    */
   public XenParser(SAXParser saxParser) {
      this.saxParser = saxParser;
   }


   public XenParser setTrimWhitespace(boolean trimWhitespace) {
       this.trimWhitespace = trimWhitespace;
      return this;
   }


   public Xen parse(File file) throws IOException, SAXException {
      current = root = null;
      saxParser.parse(file, this);
      return root;
   }

   public Xen parse(InputSource input) throws IOException, SAXException {
      current = root = null;
      saxParser.parse(input, this);
      return root;
   }

   public Xen parse(InputStream input) throws IOException, SAXException {
      current = root = null;
      saxParser.parse(input, this);
      return root;
   }

   public Xen parse(String uri) throws IOException, SAXException {
      current = root = null;
      saxParser.parse(uri, this);
      return root;
   }

   public Xen parseText(String text) throws IOException, SAXException {
      return this.parse(new ByteArrayInputStream(text.getBytes()));
   }




   @Override
   public void startElement(String namespaceURI, String localName, String qualifiedName, Attributes attrs) throws SAXException {

      String name = qualifiedName.length() > 0 ? qualifiedName : localName;   // prefer qualifiedName

      Xen child = new Xen(name, current);
      child.setAttributes(attrs);

      if (current == null)
         root = child;
      else
         current.append(child);

      current = child; // go down...
   }

   @Override
   public void endElement(String uri, String sName, String qName) throws SAXException {
      current.trimText(this.trimWhitespace);
      current = current.parent;  // move up
   }

   @Override
   public void characters(char[] ch, int start, int length) throws SAXException {
      String s = new String(ch, start, length);
      current.text += s;
   }

   /**
    * Call when done to get root Xen
    * @return  root Xen
    */
   public Xen getRoot() {
      return root;
   }

}


