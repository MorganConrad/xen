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
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.List;

public class XenTest extends TestCase {

   static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><dongle xmlns:m=\"myns\" >\n" +
         "  <m:customer someattr=\"ddd\">\n" +
         "    <id>CUSTOMER_03</id>\n" +
         "    <name>Boomer Institute</name>\n" +
         "    <m:address>Montara CA</m:address>\n" +
         "    <email addr=\"address1\" />\n " +
         "    <email addr=\"address2\"/>" +
         "    <memo/>\n" +
         "    <fixed_pool_id/>\n" +
         "    <![CDATA[<sender>John Smith</sender>]]>" +
         "  </m:customer>\n" +
         "  <id>DONGLE_01</id>\n" +
         "</dongle>\n";


   public void testXenParser() throws Exception {
      XenParser handler = new XenParser();
      handler.parseText(XML);
      Xen root = handler.getRoot();
      testXen(root);
   }

   public void testXenDOM() throws Exception {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      ByteArrayInputStream baos = new  ByteArrayInputStream(XML.getBytes());
      Document doc = dBuilder.parse(baos);
      Xen root = Converter.FromDocument.DEFAULT.convert(doc);
      testXen(root);
   }


   private void testXen(Xen root) throws Exception {

      String s = root.toString();
      System.out.println(s);

      assertEquals("dongle", root.name());
      s = root.getText("id");
      assertEquals("DONGLE_01", s);
      Xen customer = root.get("m:customer");
      s = customer.getText("/id");
      assertEquals("DONGLE_01", s);
      assertEquals("m:customer", customer.name());
 //     assertEquals("customer", customer.getLocalName());
 //     assertEquals("m", customer.getPrefix());
      s = customer.getText("/m:customer/name");
      assertEquals("Boomer Institute", s);
      s = customer.getText(".././id");
      assertEquals("DONGLE_01", s);
      s = customer.getAttr("someattr");
      assertEquals("ddd", s);

      s = customer.text();
      assertEquals("<sender>John Smith</sender>", s);

      //  customer.setText("CUSTOMERTEXT");
      // assertEquals("CUSTOMERTEXT", customer.getText());

      customer.putAttributes("foo", "bar");
      assertTrue(customer.attributes().containsKey("foo"));
      assertFalse(customer.attributes().containsKey("name"));
      //assertFalse(root.hasAttributes());

      assertEquals(7, customer.children(Xpath.WILDCARD).size());

      assertNull(root.getText("customer/not there"));

      try {
         customer.get("email");
         fail();
      }
      catch (DOMException dome) {
         assertEquals("Multiple Elements found for <email>", dome.getMessage());
      }
      try {
         customer.getAttr("email", "@addr");
         fail();
      }
      catch (DOMException dome) {
         assertEquals("Multiple Elements found for <email/@addr>", dome.getMessage());
      }

      assertEquals("address1", customer.getAttr("email[1]", "@addr"));
      assertEquals("address2", customer.getAttr("email[0]", "@addr"));
      assertEquals("[address1, address2]", customer.allAttr("email", "@addr").toString());
      assertEquals("<id>CUSTOMER_03<\\id>", customer.get("id").toString());
      assertEquals("bar", root.getAttr("m:customer", "@foo"));
      customer.attributes().remove("foo");
      assertFalse(customer.attributes().containsKey("foo"));
      assertEquals("/m:customer", customer.getAbsolutePath());

      assertEquals(2, customer.children("email").size());

      assertEquals(1, root.all("m:customer[@someattr]").size());
      assertEquals(1, root.all("m:customer[@someattr='ddd']").size());
      assertEquals(0, root.all("m:customer[@someattrnotthere]").size());
      assertEquals(0, root.all("m:customer[@someattr='xxx']").size());
      assertEquals(0, root.allAttr("m:customer/@someattrnotthere").size());
      assertEquals("ddd", root.getAttr("m:customer/@someattr") );
      Converter.ToDocument converter = new Converter.ToDocument(new CoreDocumentImpl());
      Document outDoc = converter.convert(root);
      assertNotNull(outDoc);

      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      StringWriter writer = new StringWriter();
      transformer.transform(new DOMSource(outDoc), new StreamResult(writer));
      String output = writer.getBuffer().toString();
      System.out.println(output);

      List breadth = root.breadthFirst();
      assertEquals("<id>DONGLE_01<\\id>", breadth.get(2).toString());
      List depth = root.depthFirst();
      assertEquals("<id>CUSTOMER_03<\\id>", depth.get(2).toString());

      root.setProperty("foo1", "bar1");
      root.setProperty("foo2", "bar2");
      customer.setProperty("foo1", "barforcustomer");
      assertEquals("barforcustomer", customer.getProperty("foo1"));
      assertEquals("bar2", customer.getProperty("foo2"));

      assertEquals("<sender>John Smith</sender>DONGLE_01CUSTOMER_03Boomer InstituteMontara CA", root.text(true));
   }
}