package com.flyingspaniel.xen;

import java.net.URLEncoder;
import java.util.Arrays;

/**
 * Geocoder Demo based upon  "Making Java Groovy" Geocoder example
 * Chapter 2, Page 32
 *
 * @author Morgan Conrad
 * @see <a href="http://opensource.org/licenses/MIT">This software is released under the MIT License</a>
 * @since Copyright (c) 2014 by Morgan Conrad
 */
public class GeocoderDemo {

   public static final String BASE = "http://maps.googleapis.com/maps/api/geocode/xml?sensor=false&address=";


   /*
       OPTION 1 - Java / W3C XPath style with 1-based indexing we ask for the 1st result, not the 0th
    */
   public double[] fillInLatLng_V1(String address) throws Exception {
      String url = BASE + URLEncoder.encode(address);
      Xen response = new XenParser().parse(url);

      double[] latLng = new double[2];

      // Various options for getting the result
      // 1. cache intermediate result (most efficient if you had to look up a lot within location)
      Xen location = response.one("result[1]/geometry/location");
      latLng[0] = location.toDouble("lat");
      latLng[1] = location.toDouble("lng");

      // option 2a and 2b, go there directly
      latLng[0] = response.toDouble("result[1]/geometry/location/lat");
      latLng[1] = response.one("result[1]/geometry/location/lng").toDouble();
      return latLng;
   }


   /*
        OPTION 2 - Groovy XMLSlurper style, with dots and 0-based indexing
    */
   public double[] fillInLatLng_V2(String address) throws Exception {
      String url = BASE + URLEncoder.encode(address);
      Xen response = new XenParser().parse(url);

      double[] latLng = new double[2];

      // Various options for getting the result
      // 1. cache intermediate result (most efficient if you had to look up a lot within location)
      Xen location = response.one(".result[0].geometry.location");
      latLng[0] = location.toDouble(".lat");
      latLng[1] = location.toDouble(".lng");

      // option 2a and 2b, go there directly (as in the Making Java Groovy book)
      latLng[0] = response.toDouble(".result[0].geometry.location.lat");
      latLng[1] = response.one(".result[0].geometry.location.lng").toDouble();

      return latLng;
   }



   public static void main(String[] args) throws Exception {
      GeocoderDemo demo = new GeocoderDemo();
      double[] latLng = demo.fillInLatLng_V1("24 Willy Mays Plaza San Francisco CA");
      System.out.println(Arrays.toString(latLng));

      latLng = demo.fillInLatLng_V2("24 Willy Mays Plaza San Francisco CA");
      System.out.println(Arrays.toString(latLng));

   }
}
