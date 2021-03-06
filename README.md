xen
======

Need to read or modify a few values from XML?  Xen makes that easy and painless.  It supports

 * "Groovy-like" syntax with dots separating levels (see latitude part of the demo code below)
 * a similar XPath-like syntax using slashes as separators (see longitude code below)
 * direct programmatic navigation with methods like children(), parent(), etc...

#### Demo
This Example Mimics an Example from the book _Making Java Groovy_.  [See GeocoderDemo.java] (https://github.com/MorganConrad/xen/blob/master/test/com/flyingspaniel/xen/GeocoderDemo.java) for complete code.

    // Get the latitude and longitude of the San Francisco Giant's stadium.

    String url = "http://maps.googleapis.com/maps/api/geocode/xml?sensor=false&address=" + URLEncoder.encode("24 Willy Mays Plaza San Francisco CA");
    Xen response = new XenParser().parse(url);

    // show a couple of options for getting at the data
    double latitude  = response.toDouble(".result[0].geometry.location.lat");
    double longitude = response.one("result[1]/geometry/location/lng").toDouble();


#### Javadocs
[JavaDocs are here](http://morganconrad.github.io/xen/javadocs/)



### Navigation API

 1. A Xen object supports basic navigation via `children(String), parent(), and getRootElement().`
 2. It also supports a convenience API for "XPath-like" search: `get(), one(), and all(), getText(), oneText(), allText().`
 
    - get...() returns a single match, throwing a DOMException if there were multiple matches, or null if there were none
    - one()    is like get...(), except it throws a DOMException if none were found
    - all...() returns a list of matches, possibly empty
 3. You can also explicitly create an Xpath to do searching from an Xen.   Details below.

### XPath

This class implements an "XPath-like" search syntax.

#### All Selectors except "//" are supported
 1. /   if at the start, move to the root Xen, else used as a delimiter
 2. .   move to current Xen.  (not very useful since there is an implied "." at the start of any path)
 3. ..  move up to parent Xen.
 4. x   select all children named x
 5. *   select all children
 6. @x  select attributes named x (only allowed at the end)
 7. // is _not supported_.  All children must be direct descendants.

### Predicates supported (most are as-per W3C)
 1. [N] and [last()-N] work as per W3C, with __1 based indexing__.  _Note:_ the `last()` is optional.  e.g. [-2] is same as [last()-2]
 2. [@a]  selects all elements having an attribute named a
 3. [@a='val'] selects all elements having an attribute a with value val.  _Note:_ unlike W3C the single quotes are optional but highly recommended
 4. [.='val'] or [text()='val'] selects elements whose text equals val.
 5. Use ~ instead of = for regular expressions (non-W3C standard)  e.g.  [.~'.*end'] selects all elements whose text ends with "end"

#### If the path starts with a dot and a letter, it will be treated as a "Groovy Dot Style" path to access elements.
You lose a few options ("/", ".", and ".." are not supported) but the notation matches what you'd type in Groovy, including __0 based indexing__.

#### How does this compare to Groovy?

If you use "Groovy Dot Style", things are nearly identical.  You'll need to add a method call like get() or getText().
If you use "W3C XPath Style " style, replace the "." with "/", and adjust  your indices by +1.
**Important**  Unlike Groovy,W3C XPath indexing is 1-based.  The first element is \[1\], *not* \[0\].

    records.car.make[2].@model.text();         // Groovy
    records.get(".car.make[2].@model").text(); // Xen "Groovy style" with 0 based indexes
    records.get("car/make[3]/@model").text();  // Xen "Xpath style", note 1-based indexing!
    

Note:  For more Groovy compatibility, Xen also has a `depthFirst()` and `breadthFirst()` which return a `List<Xen>`.

Converters - convert to or from an Xen
-----

#### GXmlParser inspired by groovy.util.XmlParser

For resding from general input types, use GXmlParser, which is an implementation of a org.xml.sax.ext.DefaultHandler2 that creates a tree of Xens using a SAXParser.  _e.g._

    SAXParserFactory factory = SAXParserFactory.newInstance();
    // play with the factory settings...
    SAXParser saxParser = factory.newSAXParser();
    GXmlParser gxmlParser = new  GXmlParser(saxParser);
    Xen root = gxmlParser.parse(someKindOfInput);
    
or, more simply, accept the defaults and go Groovy Style.
    
    Xen root = new GXmlParser().parse(someKindOfInput);
    

#### Converter.FromDocument

If you already have an existing org.w3c.dom.Document (say, from a DOM parser), use this to convert to a tree of Xens.

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

#### Converter.ToXML

A reasonable conversion to XML text.  (If you want something fancier, use ToDocumentConverter and apply your
preferred Transformer or whatever to the Document.)  Usage:

    Converter.ToXML converter = new Converter.ToXML(initialIndent, indentPerLevel);
    String niceXML = converter.convert(xelent);   // usually rootXen but not necessarily

_Note:_   Xen.toString() uses this with `Converter.ToXML.DEFAULT`, where indent and indentPerLevel both two spaces.


# Philosophy
Xen was inspired by XPath and Groovy's XML Handling, e.g. [XMLParser](http://groovy.codehaus.org/api/groovy/util/XmlParser.html).

Let's face it, the standard Java `org.w3c.dom.*` XML interfaces and implementations are huge, way overly complex for most users,
and do not match up well with modern Java.  If one were writing them for Java today, the APIs and implementations could be greatly simplified
by using Collections, Generics, and Varargs, as well as numerous other Java syntax goodies.

For example, `Node.getChildNodes()` would likely return a `List<Node>`, and `Node.getAttributes()` would return a `Map`.

Anybody who has used (or even just read about) Groovy's [XMLParser](http://groovy.codehaus.org/api/groovy/util/XmlParser.html) and associated classes knows how simple things _could be_.
For example, _Making Java Groovy_ has example code with just a [few lines to parse Google Geocoder data.](https://github.com/kousen/Making-Java-Groovy/blob/6671f959c7ea9fc5e2522b4f85da1413ede71f20/ch02/groovybaseball/src/main/groovy/service/Geocoder.groovy)

This package is an attempt to rewrite XML structures as they "should be written today", with some major simplifications
that hopefully make sense to 90% of users and match up with much of the Groovy capabilities.  If the simplifications don't make sense for you, don't use this module!
It's interesting that my design came out pretty close to Groovy's.  (I then moved even closer to their design).

This package is fairly new (v0.1.0), and may well contain bugs and design flaws.  The API is still subject to change.

#### General Design - similar to a [Groovy Node](http://groovy.codehaus.org/api/groovy/util/Node.html).

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
[JavaDocs are here](http://morganconrad.github.io/xen/javadocs/)
