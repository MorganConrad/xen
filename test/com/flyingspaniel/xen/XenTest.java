package com.flyingspaniel.xen;

import com.sun.org.apache.xerces.internal.dom.CoreDocumentImpl;
import junit.framework.TestCase;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

public class XenTest extends TestCase {

   static final File FILE = new File("test/bookstore.xml");

   public void testXenParser() throws Exception {
      XenParser handler = new XenParser();
      handler.parse(FILE);
      Xen root = handler.getRoot();
      testXen(root);
   }

   public void testXenDOM() throws Exception {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(FILE);
      Xen root = Converter.FromDocument.DEFAULT.convert(doc);
      testXen(root);
   }

   // collects all the other tests...
   private void testXen(Xen root) throws Exception {
      testRoot(root);
      Xen aBook = root.get("m:book");
      testBook0(aBook);
      assertEquals(root, aBook.rootElement());

      testXPathSearch(root);
      testGroovySearch(root);
      testProperties(root);
      testTransformers(root);
      testExceptions(root);

      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      root.print(pw);
      pw.close();
      String s =  sw.toString();
      assertTrue(s.startsWith("<bookstore xmlns:m="));
   }


   private void testRoot(Xen root) {
      assertEquals("bookstore", root.name());
      assertEquals("Acme Book Emporium", root.getText("id"));
      assertNull(root.getText("not there"));
      assertEquals(4, root.all("book").size());
      assertEquals(1, root.all("m:book").size());

      List<Xen> breadth = root.breadthFirst();
      assertEquals("<id>Acme Book Emporium<\\id>", breadth.get(6).toString());
      List<Xen> depth = root.depthFirst();
      assertEquals("Everyday Italian", depth.get(2).text());

      List<String> years = root.allText("book/year");
      Collections.sort(years);
      assertEquals("1990", years.get(0));

      assertNull(root.parent());
      assertEquals(root, root.get());

      assertEquals(7, root.children("*").size());
   }

   private void testBook0(Xen book) {
      assertTrue(book.hasAttribute("category"));
      assertFalse(book.hasAttribute("notthere"));
      assertNull(book.getText("not there"));
      assertNotNull(book.getText("title"));

      book.putAttributes("notthere", "somevalue");
      assertEquals("somevalue", book.attribute("notthere"));

      book.get("title").setText("Exceptional Italian<>&\"\'");  // was "Everyday Italian"  give it lots to escape...
      Xen price = book.get("price");
      book.remove(price);
      String s =  book.getTextContent();
      assertFalse(s.contains("30.00"));
      assertTrue(book.toString().contains("Exceptional Italian&lt;&gt;&amp;&quot;&apos;"));
   }


   private void testXPathSearch(Xen root) {
      Xen title = root.one("book[@category='CHILDREN']/title");
      assertEquals("en", title.attribute("lang"));
      assertEquals("Harry Potter", title.text());
      Xen author = title.one("../author");
      assertEquals("J K. Rowling", author.text());

      title = root.one("/book/title[@lang='es']");
      assertEquals("La tabla de Flandes", title.text());
      testAttributeNode(title.one("@someattr"));
      assertEquals("/book/title", title.absolutePath());

      assertNull(root.get("not", "there"));

      // test wildcard.  Two books have capital X in title
      List<Xen> XMLbooks = root.all(".book.title[.~.*X.*]");
      assertEquals(2, XMLbooks.size());
   }

   private void testGroovySearch(Xen root) {
     Xen book = root.one(".book[2]");
     assertEquals(5, book.children("author").size());
     book = root.one(".book.title[.='Harry Potter']").parent();
     assertEquals("2005", book.getText("year"));
   }

   private void testAttributeNode(Xen attrNode) {
      assertEquals("123", attrNode.text() );
      assertEquals(123.0, attrNode.toDouble() );
      assertEquals(123, attrNode.toInt() );
      try {
         attrNode.setUserProperty("not", "modifiable");
         fail();
      }
      catch (IllegalStateException ise) {
         ;  // expected
      }

      List<Xen> aList = attrNode.rootElement().all("book/title[@someattr]");
      assertEquals(1, aList.size());
      assertEquals("La tabla de Flandes", aList.get(0).getText());
   }

   private void testProperties(Xen root) {
      root.setUserProperty("foo1", "bar1");
      root.setUserProperty("foo2", "bar2");
      Xen aBook = root.get("book[1]");
      aBook.setUserProperty("foo1", "barforbook");
      assertEquals("barforbook", aBook.userProperty("foo1"));
      assertEquals("bar2", aBook.userProperty("foo2"));
      assertNull(aBook.userProperty("not there"));
   }

   private void testTransformers(Xen root) throws Exception {
      Converter.ToDocument converter = new Converter.ToDocument(new CoreDocumentImpl());
      Document outDoc = converter.convert(root);
      assertNotNull(outDoc);
      String output = outDoc.getDocumentElement().getTextContent();
      assertTrue(output.startsWith("Exceptional")); // lame test...

      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      StringWriter writer = new StringWriter();
      transformer.transform(new DOMSource(outDoc), new StreamResult(writer));
      output = writer.getBuffer().toString();
      System.out.println(output);
      assertTrue(output.contains(">Exceptional Italian"));
   }


   private void testExceptions(Xen root) {
      try {
         root.one("book");
         fail();
      }
      catch (DOMException dome) {
         assertEquals("Multiple Elements found for <book>", dome.getMessage());
      }
      try {
         root.one("book", "title", "@lang");
         fail();
      }
      catch (DOMException dome) {
         assertEquals("Multiple Elements found for <book/title/@lang>", dome.getMessage());
      }

      try {
         root.putAttributes("odd number");
         fail();
      }
      catch (IllegalArgumentException expected) {
         ; // ok
      }
   }






}