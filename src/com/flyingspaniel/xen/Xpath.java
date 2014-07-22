package com.flyingspaniel.xen;

import org.w3c.dom.DOMException;

import java.util.ArrayList;
import java.util.List;

/**
 *
 *  Allows for <a href="http://www.w3schools.com/xpath/xpath_syntax.asp">"XPath-like"</a> selection and movement starting from an Xen.
 *  <b>Note:</b> there is an implied "." at the start of any path
 * <pre>
 *    /   if at the start, move to the root node, else used as a delimiter
 *    .   move to current node (not very useful since there is an implied "." at the start of any path)
 *    ..  move up to parent node
 *    x   selects all children named x
 *    *   selects all children
 *    &#64;x  only allowed at the end, selects the attributeXP named x
 *    //  is NOT supported.
 *
 * </pre>
 *    Three predicates are supported, after x or *.<ul>
 *    <li>[N] and [last()-N] work as per W3C.  However, the last() is optional.  Any non-positive Ns will be treated as last+N.  e.g.
 *    <ul>
 *      <li>If N > 0, will select the Nth child.    (1-based, W3C uses 1-based numbering)
 *      <li>If N == 0, selects the last child.      So book[0] is equivalent to W3C's book[last()]
 *      <li>if N < 0, selects the last+Nth child.  So book[-2] is equivalent to W3C's book[last()-2]
 *    </ul>
 *    <li>[@a] or [@a='val'] work as per W3C.
 *    <ul>
 *       <li>ename[@aname] selects all elementsXP named ename with an attributeXP aname
 *       <li>ename[@aname='avalue'] selects all elementsXP named ename with an attributeXP aname equal to avalue
 *    </ul>
 *     </ul>
 *  <b>Note:</b> Does NOT support //  i.e., all nodes must be <b>direct</b> descendants
 *
 *  Note that this XPath-like syntax is <i>very similar</i> to <a href="http://groovy.codehaus.org/api/groovy/util/XmlSlurper.html">Groovy's XMLSlurper</a> syntax if you replace "." with a "/".  e.g.
 *  <br>
 *  Where Groovy might use    <code>root.records.car[0].@make   </code><br>
 *        Xen would use      <code>root.attributeXP("records/car[1]/@make"); //   note 1 based offset</code>
 *
 * @author Morgan Conrad
 * @see <a href="http://opensource.org/licenses/MIT">This software is released under the MIT License</a>
 * @since Copyright (c) 2014 by Morgan Conrad
 */
public class Xpath {


   public static final String DELIM = "/";
   public static final String ROOT = "/";
   public static final String PARENT = "..";
   public static final String CURRENT = ".";
   public static final String WILDCARD = "*";
   public static final String ATTRIBUTE = "@";

   static final String[] NO_PATH = new String[0];


   private final String pathString;
   private final String[] pathSegments;
   private final String[] predicates;

   public Xpath(String... path) {
      pathString = resolvePath(path);

      String[][] pnp = computePathsAndPredicates(pathString);
      pathSegments = pnp[0];
      predicates = pnp[1];
   }


   public List<Xen> evaluate(Xen xen, int start) {
      List<Xen> matches = new ArrayList<Xen>();

      if (pathSegments.length <= start) {
         matches.add(xen);
         return matches;
      }

      for (int i = start; i < pathSegments.length && (xen != null); i++) {
         String segment = pathSegments[i];

         if (PARENT.equals(segment))     // up one
            xen = xen.parent();
         else if (CURRENT.equals(segment)) // same dir
            continue;
         else if (ROOT.equals(segment))
            xen = xen.getRootElement();
         else if (segment.startsWith(ATTRIBUTE)) {
            if ("@*".equals(segment)) {
               throw new UnsupportedOperationException(pathString); // TODO
            } else {
               String name = segment.substring(1);
               // create a fakey little "elementXP" node...  name will start with "@" for clarity
               if (xen.hasAttribute(name))
                  xen = new Xen(segment, xen, xen.attribute(name));
               else
                  xen = null;
            }
            break; // attributesXP are the end of the line
         } else {
            List<Xen> childList = xen.children(segment);
            xen = null;  // never add
            if (predicates[i] != null)
               childList = applyPredicate(childList, predicates[i]);

            if (i == pathSegments.length-1) {
               matches.addAll(childList);
               return matches;
            }
            else if (childList.size() == 0) {
               // end of the line
               return matches;
            }
            else for (Xen child : childList) {
               matches.addAll(evaluate(child, i + 1));
            }
            break;
         }
      }

      if (matches.size() == 0 && xen != null)
         matches.add(xen);
      return matches;
   }


   public List<Xen> evaluate(Xen xen) {
      return evaluate(xen, 0);
   }


   public List<Xen> applyPredicate(List<Xen> inList, String predicate) {
      List<Xen> outList = new ArrayList<Xen>();

      // support ints and @
      if (predicate.startsWith(ATTRIBUTE)) {
         String[] split = predicate.substring(1).split("="); // keep the @
         String name = split[0];
         String requiredValue = null;
         if (split.length > 1)
            requiredValue = split[1].substring(1, split[1].length() - 1);

         for (Xen in : inList) {
            String actualValue = in.attribute(name);  // never null
            if ( actualValue.equals(requiredValue) ||
                ((requiredValue == null) && (actualValue.length() > 0)) )
               outList.add(in);
         }
      }
      else {
         int idx = Integer.parseInt(predicate) - 1;  //  W3C XPath uses 1-based indexing
         Xen one = (idx >= 0) ? inList.get(idx) : inList.get(inList.size() + idx);
         outList.add(one);
      }

      return outList;
   }

   public boolean makeSureLastIsAttribute() {
      String last = pathSegments[pathSegments.length - 1];
      if (last.startsWith(ATTRIBUTE))
         return true;

      // may eventually throw an exception...
      pathSegments[pathSegments.length - 1] = ATTRIBUTE + last;
      return false;
   }


   public String resolvePath(String... xpaths) {
      if (xpaths == null || xpaths.length == 0)   // check for trivial case
         return "";

      StringBuilder sb = new StringBuilder();
      for (String path : xpaths) {
         if (path.startsWith(DELIM))
            sb.setLength(0);
         else if ((sb.length() > 0) && ('/' != sb.charAt(sb.length() - 1)))
            sb.append(DELIM);

         sb.append(path);
      }

      return sb.toString();
   }


   public Xen thereCanBeOnlyOne(List<Xen> list) {
      if (list.isEmpty())
         throw new DOMException(DOMException.NOT_FOUND_ERR, "No elementsXP found for <" + pathString + ">");
      if (list.size() > 1)
         throw new DOMException(DOMException.TYPE_MISMATCH_ERR, "Multiple Elements found for <" + pathString + ">");

      return list.get(0);
   }


   protected String[][] computePathsAndPredicates(String resolvedPath) {
      String[] paths = NO_PATH;
      String[] predicates = NO_PATH;

      if (resolvedPath.length() > 0) {
         String cleanPath = resolvedPath.replaceAll("//", "/+");  // // is a pain... (and currently unsupported)
         String[] splits = cleanPath.split("/");
         if ("".equals(splits[0]))
            splits[0] = ROOT;

         paths = new String[splits.length];
         predicates = new String[splits.length];
         for (int i = 0; i < splits.length; i++) {
            String s = splits[i];

            int predIndex = s.indexOf('[');
            if (predIndex < 0) {
               paths[i] = s;
            } else {
               paths[i] = s.substring(0, predIndex);
               predicates[i] = s.substring(predIndex + 1, s.length() - 1); // remove []
            }
         }
      }

      return new String[][]{paths, predicates};
   }


}
