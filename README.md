xelent
======

Simple and sane Java XML implementation using Collections, Generics and Varargs, inspired by Groovy's XML handling.

Let's face it, the standard Java org.w3c.dom.* XML interfaces and implementations are huge, way overly complex for most users,
and do not match up well with modern Java.  If one were writing them for Java today, the APIs and implementations could be greatly simplified
by using Collections, Generics, and Varargs, as well as numerous other Java syntax goodies.

For example, `Node.getChildNodes()` would likely return a `List<Node>`, and `Node.getAttributes()` would return a `Map`.

Anybody who has used (or even just read about) Groovy's [XMLParser](http://groovy.codehaus.org/api/groovy/util/XmlParser.html) and associated classes knows how simple things _could be_.

This package is an attempt to rewrite XML structures as they "should be written today", with some major simplifications
that hopefully make sense to 90% of users and match up with much of the Groovy capabilities.  If the simplifications don't make sense for you, don't use this module!
It's interesting that my design came out pretty close to Groovy's.  (I then moved even closer to their design).

[JavaDocs are here](http://morganconrad.github.io/xen/javadocs/)

####General Design - similar to a [Groovy Node](http://groovy.codehaus.org/api/groovy/util/Node.html).

 1. All text associated with a node is grouped into a single String.  There are no org.w3c.dom.Text Nodes.
 2. Attributes are kept in a simple `Map<String,String>` under their element.  So there are no org.w3c.dom.Attr nodes.
 3. Since I have never cared about a CData section (and standard parsers just add it to your text), nor comments, nor processing instructions,
 nor an Entity or a Notation, those subclasses of org.w3c.dom.Node are also ignored.  
 4. As noted in the very similar Groovy implemention, "This simple model is sufficient for most simple use cases of processing XML." 
 4. Other than searching via `getElementsBy...`, the only thing I have ever used Document for is to create Nodes and `getDocumentElement()`.
 Since you can search perfectly well from an Element, and there is only one type of Node (Element) to create, there is no need for a org.w3c.dom.Document.  
 If you really need a "root" reference,  there _is_ a single  _rootElement_.
 5. This pretty much eliminates all Nodes other than Element.  The Xen class corresponds roughly to org.w3c.dom.Element and a groovy.util.Node.
 That's why the name is Xen: "XML Element Node".
 6. To allow for some expansion, each Xen does have a `protected Map<String, Object> props`.  So, if you really need to track
  CDATA, comments, etc. you can probably tuck them away in there.  `Xen.getProperty(String name)` works it's way through any
  parents, so you can store "global for this Document information" in the props of the rootElement.

###Navigation API

 1. A Xen object supports basic navigation via `children(String), parent(), and getRootElement().`
 2. It also supports a convenience API for "XPath-like" search: `attributeXP(), attributesXP(), elementXP(), elementsXP().` 
 3. You can also explicitly create an Xpath to do searching from an Xen.   Details below.

###Xpath

This class implements an "XPath-like" search syntax.

####All Selectors except "//" are supported
 1. /   if at the start, move to the root Xen, else used as a delimiter
 2. .   move to current Xen.  (not very useful since there is an implied "." at the start of any path)
 3. ..  move up to parent Xen.
 4. x   select all children named x
 5. *   select all children
 6. @x  select attributes named x (only allowed at the end)
 7. // is _not supported_.  All children must be direct descendants.

 ###Predicates supported (all as-per w3c with one addition)
 1. [N] and [last()-N] work as per W3C.  _Note:_ the `last()` is optional.  e.g. [-2] is same as [last()-2]
 2. [@a]  selects all elements having an attribute named a
 3. [@a='val'] selects all elements having an attribute a with value val.


#### How does this compare to Groovy?

In general, if you replace the "." with "/", and add a method call, many things convert.
**Important**  Unlike Groovy, Xen follows W3C XPath indexing, which is 1-based.  The first element is \[1\], *not* \[0\].

    records.car.make[2].@model.text();               // Groovy
    records.elementXP("car/make[3]/@model").text();  // Xen  note 1-based indexing!
    records.attributeXP("car/make[3]/@model");

Note:  For more Groovy compatibility, Xen also has a `depthFirst()` and `breadthFirst()` which return a `List<Xen>`.

Converters - convert to or from an Xen
-----

#### GXmlParser inspired by groovy.util.XmlParser

An implementation of a org.xml.sax.ext.DefaultHandler2 that creates a tree of Xens using a SAXParser.  _e.g._

    SAXParserFactory factory = SAXParserFactory.newInstance();
    // play with the factory settings...
    SAXParser saxParser = factory.newSAXParser();
    GXmlParser gxmlParser = new  GXmlParser(saxParser);
    Xen root = gxmlParser.parse(someKindOfInput);
    
or, more simply, accept the defaults and go Groovy Style.
    
    Xen root = new GXmlParser().parse(someKindOfInput);
    

#### Converter.FromDocument

Converts an existing org.w3c.dom.Document (say, from a DOM parser) to a tree of Xens.

    DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = dBuilder.parse(someKindOfInput);
    Converter.FromDocument converter = new Converter.FromDocument();
    Xen rootXen = converter.convert(doc);
    
Often you can simplify the last two lines with

    Xen rootXen = Converter.FromDocument.DEFAULT.convert(doc);

#### Converter.ToDocument

Converts the Xen (usually the root but not necessarily) into a org.w3c.dom.Document.  You must provide a blank Document of your preferred type, _e.g._

    Converter.ToDocument converter = new Converter.ToDocument(someDocumentBuilder.newDocument());   // or new CoreDocumentImpl()
    doc = converter.convert(xelent);

####Converter.ToXML

A reasonable conversion to XML text.  (If you want something fancier, use ToDocumentConverter and apply your
preferred Transformer or whatever to the Document.)  Usage:

    Converter.ToXML converter = new Converter.ToXML(initialIndent, indentPerLevel);
    String niceXML = converter.convert(xelent);   // usually rootXen but not necessarily

_Note:_   Xen.toString() uses this with `Converter.ToXML.DEFAULT`, where indent and indentPerLevel both two spaces.

[JavaDocs are here](http://morganconrad.github.io/xen/javadocs/)