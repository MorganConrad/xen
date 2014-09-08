package com.flyingspaniel.xen;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Support for XPath-like predicates
 *
 * @author Morgan Conrad
 * @see <a href="http://opensource.org/licenses/MIT">This software is released under the MIT License</a>
 * @since Copyright (c) 2013 by Morgan Conrad
 */
public interface XenPredicate {

   /**
    * Apply the predicate to a list of possible matches.
    * @param inList
    * @return may be the same as inList, may be a new list.
    */
   public List<Xen> apply(List<Xen> inList);

   /**
    * ALL returns everything, same as "no predicate"
    */
   public static XenPredicate ALL = new XenPredicate() {
      public List<Xen> apply(List<Xen> inList) {
         return inList;
      }
   };

   public static XenPredicate LAST = new Index(-1);


   /**
    * 0-based index, negatives values are treated as last() + index
    */
   public static class Index implements XenPredicate {

      final int index;

      public Index(int index) {
         this.index = index;
      }

      public List<Xen> apply(List<Xen> inList) {
         List<Xen> outList = new ArrayList<Xen>();
         Xen one = (index >= 0) ? inList.get(index) : inList.get(inList.size() + index);
         outList.add(one);
         return outList;
      }
   }


   /**
    * True if the Attribute exists (has a value > "")
    */
   public static class AttributeExists implements XenPredicate {

      final String name;

      public AttributeExists(String name) {
         this.name = name;
      }

      public List<Xen> apply(List<Xen> inList) {
         List<Xen> outList = new ArrayList<Xen>();
         for (Xen in : inList) {
            if (in.attribute(name).length() > 0)
               outList.add(in);
         }
         return outList;
      }

   }



   static abstract class Matches implements XenPredicate {
      final String value;
      final Pattern pattern;

      Matches(String value, boolean isRegex) {
         this.value = value;
         if (isRegex) { // regex
            pattern = Pattern.compile(value);
         }
         else {
            pattern = null;
         }
      }

      boolean isMatch(String input) {
         return (pattern != null) ? pattern.matcher(input).matches() : value.equals(input);
      }
   }


   /**
    * True if the Attribute matches value.  Currently just supports equals(), no regex...
    */
   public static class AttributeMatches extends Matches {

      final String name;

      public AttributeMatches(String name, String value, boolean isRegex) {
         super(value, isRegex);
         this.name = name;
      }

      public List<Xen> apply(List<Xen> inList) {
         List<Xen> outList = new ArrayList<Xen>();
         for (Xen in : inList) {
            if (isMatch(in.attribute(name)))
               outList.add(in);
         }
         return outList;
      }

   }


   /**
    * True if the Text matches value.  Currently just supports equals(), no regex...
    */
   public static class TextMatches extends Matches {

      public TextMatches(String value, boolean isRegex) {
         super(value, isRegex);
      }

      public List<Xen> apply(List<Xen> inList) {
         List<Xen> outList = new ArrayList<Xen>();
         for (Xen in : inList) {
            if (isMatch(in.text))
               outList.add(in);
         }
         return outList;
      }

   }



}
