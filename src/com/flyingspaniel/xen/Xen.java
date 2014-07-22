package com.flyingspaniel.xen;

import java.io.PrintWriter;
import java.util.*;

import org.w3c.dom.DOMException;
import org.xml.sax.Attributes;


/**
 * <p>
 * A simple and sane XML implementation using Java Collections, Generics, and varargs.
 * A Xen ("XML Element Node") roughly corresponds to a {@link org.w3c.dom.Element} and a groovy.util.Node.
 * <br>There are <i>not the 10 other types of nodes and 25 Subinterfaces</i> from {@link org.w3c.dom.Node}
 * <ul>
 * <li> Attributes are kept in a Map&lt;String,String&gt;
 * <li> text is kept in a single String (always normalized)
 * <li> child Elements (other Xen) are kept in a List&lt;Xen&gt;
 * </ul>
 *
 * If using namespaces, the name of an elementXP is <i>always</i> <i>qualified</i> name.  The URI is forgotten.
 * e.g.  &lt;w:border&gt;  &lt;w:border/&gt; has a name of "w:border".
 *
 * There are no Comments, EntityReferences, etc. etc.  If you need these don't use Xen.
 * However, all Xens have a field props, which is a Map<String, Object>, which might be useful for small additions.
 *
 * <p>
 * Combined with Xpath, Xen allows for <a href="http://www.w3schools.com/xpath/xpath_syntax.asp">"XPath-like"</a> selection and movement.
 *
 *  Note that this XPath-like syntax is <i>very similar</i> to <a href="http://groovy.codehaus.org/api/groovy/util/XmlSlurper.html">Groovy's XMLSlurper</a> syntax if you replace "." with a "/".  e.g.
 *  <br>
 *  Where Groovy might use    <code>root.records.car[0].@make   </code><br>
 *        Xen would use       <code>root.attributeXP("records/car[1]/@make"); //   note 1 based offset</code>
 *
 *
 * @author Morgan Conrad
 * @see <a href="http://opensource.org/licenses/MIT">This software is released under the MIT License</a>
 * @since Copyright (c) 2014 by Morgan Conrad
 */
public class Xen {


   /*
   Since many Elements have no Attributes and no Children, use these to save memory.  Allocate only when needed.
    */
   static final Map<String, String> NO_ATTRS = Collections.emptyMap();
   static final Map<String, Object> NO_PROPS = Collections.emptyMap();
   static final List<Xen> NO_CHILDREN = Collections.emptyList();


   public final String name;
   protected String text;
   protected Xen parent;

   protected volatile Map<String, String> attrs;
   protected volatile List<Xen> children;
   protected volatile Map<String, Object> props;


   /**
    * Constructor
    * @param name    required
    * @param parent  may be null
    * @param text    if null will become ""
    */
   public Xen(String name, Xen parent, String text) {
      this.name = name;
      this.parent = parent;
      attrs = NO_ATTRS;
      children = NO_CHILDREN;
      props = NO_PROPS;
      this.text = text != null ? text : "";
   }

   public Xen(String name, Xen parent) {
      this(name, parent, "");
   }

   /**
    * Use this instead of org.w3c.dom.Node#getNodeName()
    *
    * @return name with qualification prefix
    */

   public String name() {
      return name;
   }


   /**
    * Use this instead of Node.getOwnerDocument().getDocumentElement()
    *
    * @return root elementXP (should never be null)
    */
   public Xen getRootElement() {
      if (parent != null)
         return parent.getRootElement();

      return this;
   }


   public boolean isRootElement() {
      return this.parent == null;
   }



   /**
    * Use this instead of Node.getParentNode()
    * @return null only if we are root
    */
   public Xen parent() {
      return this.parent;
   }


   /**
    * Access a modifiable HashMap of general purpose properties for this Element
    * @return never null
    */
   public synchronized Map<String,Object> getProperties() {
      if (props == NO_PROPS)
         props = new HashMap<String,Object>();

      return props;
   }


   /**
    * Retrieve a general-purpose property for this Element, checking ancestors if needed
    * @param name of the property
    * @return may be null if none found
    */
   public synchronized Object getProperty(String name) {
      if (props.containsKey(name))
         return props.get(name);
      else if (parent != null)
         return parent.getProperty(name);
      else
         return null;
   }

   /**
    * Set a local user-defined name/value property
    * @param name of the property
    * @param value of the property
    * @return  previous value
    */
   public Object setProperty(String name, Object value) {
        return getProperties().put(name, value);
   }


   public synchronized List<Xen> children() {
      if (children == NO_CHILDREN)
         children = new ArrayList<Xen>();

      return children;
   }



   /**
    * Get text from this Xen.  Text is always "normalized" (combined into one single String
    * A loose replacement for org.w3c.dom.Node.getTextContent() and groovy.util.Node.text(), but DOES NOT INCLUDE text from children
    *
    * @return never-null, may be empty
    */
   public String text() { return this.text; }


   /**
    * Get text from this Xen, plus, possibly, all children.  Breadth first.
    * So similar to org.w3c.dom.getTextContent and groovy.util.Node.text().
    * @param includeChildren whether to recursively include children (breadth first)
    * @return never null
    */
   public String text(boolean includeChildren) {
      if (!includeChildren)
         return this.text;

      StringBuilder sb = new StringBuilder();
      sb.append(text);
      for (Xen xen : this.breadthFirst())
         sb.append(xen.text());

      return sb.toString();
   }

   /**
    * Get text from another Xen along an Xpath
    *
    * @param xpath XPath-like
    * @return null if that Xen does not exist
    */
   public String textXP(String... xpath) {
      Xen m = elementXP(xpath);
      return m != null ? m.text : null;
   }


   /**
    * Set text for this elementXP.  Text is always "normalized" (combined into one single String
    * A loose replacement for org.w3c.dom.Node.setTextContent(), but DOES NOT AFFECT children
    *
    * @return this
    */
   public Xen setText(Object text) {
      if (text != null)
         this.text = text.toString();

      return this;
   }

   /**
    * Use like org.w3c.dom.Element.getAttribute() and groovy.util.Node.attribute()
    * @param name of the attribute
    * @return never null
    */
   public String attribute(String name) {
      String s = attrs.get(name);
      return (s != null) ? s : "";
   }


   /**
    * Use like org.w3c.dom.Element.hasAttribute()
    * @param name of the attribute
    * @return true if attribute exists
    */
   public boolean hasAttribute(String name) {
      return attrs.containsKey(name);
   }

   /**
    * Puts (adds or replaces) attributes
    * Use instead of groovy.util.Node.setAttribute(name, value), but allows varargs to support multiple additions
    * @param pairs  0,2,4... name/value pairs, must be an even number
    * @return this
    */
   public Xen putAttributes(Object... pairs) {
      if (pairs.length == 0)
         return this;
      if ((pairs.length & 1) != 0)
         throw new IllegalArgumentException("Must have even number of arguments");

      attributes();  // prepare for modification
      for (int i = 0; i < pairs.length; i += 2) {
         attrs.put(pairs[i].toString(), pairs[i + 1].toString());
      }

      return this;
   }


   /**
    * Clears any old attributesXP and sets them
    * @param inAttrs
    * @return this
    */
   public Xen setAttributes(Attributes inAttrs) {
      this.attrs = NO_ATTRS;  // clear out the old
      if ((inAttrs == null) || inAttrs.getLength() == 0)
         return this;

      attributes();  // prepare for modification
      for (int i = 0; i < inAttrs.getLength(); i++) {
         String name = inAttrs.getLocalName(i);
         if (name.length() == 0)
            name = inAttrs.getQName(i);

         attrs.put(name, inAttrs.getValue(i));
      }

      return this;
   }



   /**
    * Finds attributes along an XPath-like path
    *
    * @param path XPath-like, last part should start with '@'
    * @return empty list if none found
    */
   public List<String> attributesXP(String... path) {
      Xpath xpath = new Xpath(path);
      xpath.makeSureLastIsAttribute();

      List<Xen> matches = xpath.evaluate(this);
      List<String> attributes = new ArrayList<String>(matches.size());
      for (Xen xen : matches)
         attributes.add(xen.text());

      return attributes;
   }


   /**
    * Finds a single attribute along an XPath-like path
    * @param path  XPath-like, last part should start with '@'
    * @return null if none found
    * @throws DOMException if multiple matches
    */
   public String attributeXP(String... path) throws DOMException {
      Xpath xpath = new Xpath(path);
      xpath.makeSureLastIsAttribute();
      List<Xen> matches = xpath.evaluate(this);

      if (matches.isEmpty())
         return null;
      return xpath.thereCanBeOnlyOne(matches).text;
   }



   /**
    * Return all the Attributes as a modifiable Map.  Use to implement other functionality in Node.  For example
    * <pre>
    *     void Node.removeAttribute(String name) { attributes().remove(name); }
    * </pre>
     * @return  Map<String, String>
    */
   public synchronized Map<String, String> attributes() {
      if (attrs == NO_ATTRS)
         attrs = new LinkedHashMap<String, String>();

      return attrs;
   }



   /**
    * Similar to org.w3c.dom.Node.appendChild() and groovy.util.Node.append(), but varargs to allow multiple additions
    * @param childs 0 or more to be appended
    * @return this
    */
   public Xen append(Xen... childs) {
      if (childs.length == 0)
         return this;

      children();  // prepare for modification();
      for (Xen child : childs) {
         child.parent = this;
         this.children.add(child);
      }

      return this;
   }


   /**
    * Similar to groovy.util.Node.remove(), but varargs to allow multiple removals
    * @param childs 0 or more to be removed
    * @return this
    */
   public Xen remove(Xen... childs)  {
      if (childs.length > 0)
         children().removeAll(Arrays.asList(childs));

      return this;
   }


   /**
    * Similar to Element.getElementsByTagName()
    * @param name  wildcard "*" is supported, but not any predicates
    * @return never-null, may be empty
    */
   public  List<Xen> children(String name) {
      List<Xen> matches = new ArrayList<Xen>();

      if (Xpath.WILDCARD.equals(name)) {
         matches.addAll(children);
      }
      else {
         for (Xen child : children)
            if (name.equals(child.name))
               matches.add(child);
      }

      return matches;
   }




   /**
    * Find elements matching the XPath-like search criteria
    * @param xpaths  XPath-like
    * @return never null, may be empty
    */
   public List<Xen> elementsXP(String... xpaths) {
      Xpath xpath = new Xpath(xpaths);
      return xpath.evaluate(this);
   }


   /**
    * Finds a single "Element" matching the XPath-like search criteria
    *
    * @param path  XPath-like
    * @return Xen
    * @throws DOMException if 0 or many are found
    */
   public Xen elementXP(String... path) throws DOMException {
      if (path.length == 0)
         return this;

      Xpath xpath = new Xpath(path);
      List<Xen> list = xpath.evaluate(this);
      return xpath.thereCanBeOnlyOne(list);
   }

   /**
    * Returns an XPath-like String that resolves to this node
    * @return String
    */
   public String getAbsolutePath() {
      if (parent == null)
         return Xpath.ROOT;

      String s = parent.getAbsolutePath();
      if (s.length() > 1)
         s = s + Xpath.DELIM;

      return s + name;
   }



   protected List<Xen> breadthFirst(List<Xen> list) {
      list.addAll(children);
      for (Xen child : children)
         child.breadthFirst(list);

      return list;
   }


   /**
    * Similar to groovy.util.Node.breadthFirst()
    * @return List<Xen>
    */
   public List<Xen> breadthFirst() {
      ArrayList<Xen> list = new ArrayList<Xen>();
      list.add(this);  // get started...

      return  breadthFirst(list);
   }


   protected List<Xen> depthFirst(List<Xen> list) {
      list.add(this);
      for (Xen child : children)
         child.depthFirst(list);

      return list;
   }

   /**
    * Similar to groovy.util.Node.depthFirst();
    * @return List<Xen>
    */
   public List<Xen> depthFirst() {
      return  depthFirst(new ArrayList<Xen>() );
   }


   @Override
   public String toString() {
      return Converter.ToXML.DEFAULT.convert(this).toString();
   }


   /**
    * Similar to groovy.util.Node.print()
    * @param out if null the call is ignored
    */
   public void print(PrintWriter out) {

      if (out != null)
         out.print(this.toString());
   }


   // trims whitespace from ends of text (usually what you want)
   protected Xen trimText(boolean doit) {
      if (doit)
         this.text = text.trim();
      return this;
   }




}
