package com.flyingspaniel.xen;

import org.w3c.dom.*;

import java.util.Map;

/**
 * Interface for Converters for Xens, and three implementations
 *
 * @author Morgan Conrad
 * @see <a href="http://opensource.org/licenses/MIT">This software is released under the MIT License</a>
 * @since Copyright (c) 2014 by Morgan Conrad
 */
public interface Converter<FROM, TO> {

   public TO convert(FROM from);


   public static class FromDocument implements Converter<Document, Xen>  {

      final boolean trimText;

      public FromDocument(boolean trimText) {
         this.trimText = trimText;
      }

      public static final FromDocument DEFAULT = new FromDocument(true);


      /**
       * Usually start here - convert tree from the org.w3c.dom.Document
       * @param doc incoming Document
       * @return root Xen
       */
      public Xen convert(Document doc) {
         Element rootElement = doc.getDocumentElement();
         return convertFromElement(rootElement, null);
      }

      /**
       * Builds a tree of Xens from an org.w3c.dom.Element.
       * @param e        elementXP
       * @param parent   of the new Element
       * @return  new Xen
       */
      public Xen convertFromElement(Element e, Xen parent) {
         Xen xen = new Xen(e.getTagName(), parent);
         NamedNodeMap eAttrs = e.getAttributes();
         int len = eAttrs.getLength();
         if (len > 0) {
            Map<String, String> attrsMap = xen.attributes();
            for (int i = 0; i < len; i++) {
               Node node = eAttrs.item(i);
               attrsMap.put(node.getNodeName(), node.getNodeValue());
            }
         }

         NodeList eChildren = e.getChildNodes();
         for (int i = 0; i < eChildren.getLength(); i++) {
            Node node = eChildren.item(i);
            if (node instanceof Element) {
               Xen child = convertFromElement((Element) node, xen);
               xen.append(child);
            } else if (node instanceof Text) {
               xen.text += node.getNodeValue();
            }
         }

         return xen.trimText(trimText);
      }

   }


   public static class ToDocument implements Converter<Xen, Document>  {

      final Document document;

      public ToDocument( Document document) {
         this.document = document;
      }


      public Document convert(Xen root) {
         Element element = toElement(root);
         document.appendChild(element);
         return document;
      }


      /**
       * Exports this elementXP (which should generally not be root) to an elementXP
       * @return newly created Element
       */
      public Element toElement(Xen xen) {

         Element element = document.createElement(xen.name());
         element.setTextContent(xen.textXP());  // do before we add children

         for (Map.Entry<String,String> attr : xen.attrs.entrySet())
            element.setAttribute(attr.getKey(), attr.getValue());

         for (Xen xenChild : xen.children) {
            Element childElement = toElement(xenChild);
            element.appendChild(childElement);
         }

         return element;
      }


   }


   public static class ToXML implements Converter<Xen, StringBuilder> {

      protected final String indentPerLevel;
      protected final String initialIndent;

      public static final ToXML DEFAULT = new ToXML("  ", "  ");
      /**
       * Constructor
       * @param initialIndent   typically an empty String, must be non-null
       * @param indentPerLevel  typically a two space String "  ", must be non-null;
       */
      public ToXML(String initialIndent, String indentPerLevel) {
         this.initialIndent = initialIndent;
         this.indentPerLevel = indentPerLevel;
      }


      /**
       * Main method to call
       * @param xen to be converted to XML
       * @return StringBuilder
       */
      public StringBuilder convert(Xen xen) {
         StringBuilder sb = new StringBuilder();
         return convertToXML(xen, sb, initialIndent);
      }



      public StringBuilder convertToXML(Xen xen, StringBuilder sb, String indent) {

         if (sb.length() > 0)
            sb.append("\n").append(indent);

         sb.append("<").append(xen.name);
         for (Map.Entry<String,String> me : xen.attrs.entrySet()) {
            sb.append(" ").append(me.getKey()).append("=\"").append(escapeXML(me.getValue())).append("\"");
         }
         sb.append(">");

         if (!xen.children.isEmpty())  {
            String newIndent = indent + indentPerLevel;
            for (Xen child : xen.children)
               convertToXML(child, sb, newIndent);

            if (xen.text.length() > 0)
               sb.append("\n").append(newIndent).append(escapeXML(xen.text));

            sb.append("\n").append(indent);
         }
         else
            sb.append(escapeXML(xen.text));

         sb.append("<\\" + xen.name + ">");

         return sb;
      }

      /**
       * Escapes HTML
       * Taken from <a href="">https://github.com/leveluplunch/levelup-java-examples/blob/master/src/test/java/com/levelup/java/xml/EscapeXMLAttributes.java</a>
       * @param xml String
       * @return escaped String
       */
      public static String escapeXML(String xml) {
         StringBuilder escapedXML = new StringBuilder();
         for (int i = 0; i < xml.length(); i++) {
            char c = xml.charAt(i);
            switch (c) {
               case '<': escapedXML.append("&lt;"); break;
               case '>': escapedXML.append("&gt;"); break;
               case '\"': escapedXML.append("&quot;"); break;
               case '&': escapedXML.append("&amp;"); break;
               case '\'': escapedXML.append("&apos;"); break;
               default:
                  if (c > 0x7e) {
                     escapedXML.append("&#" + ((int) c) + ";");
                  } else
                     escapedXML.append(c);
            }
         }

         return escapedXML.toString();
      }


   }

}
