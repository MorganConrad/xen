import com.flyingspaniel.xen.Xen;
import com.flyingspaniel.xen.XenParser;

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


   public double[] fillInLatLng(String address) throws Exception {
      String url = BASE + URLEncoder.encode(address);
      Xen response = new XenParser().parse(url);

      // note that since we are 1-based indexing we ask for the 1st result, not the 0th
      // also note that we are slightly optimized by getting an intermediate result
      Xen location = response.one("result[1]/geometry/location");
      double[] latLng = new double[2];
      latLng[0] = Double.parseDouble(location.getText("lat"));
      latLng[1] = Double.parseDouble(location.getText("lng"));
      return latLng;
   }


   public static void main(String[] args) throws Exception {
      GeocoderDemo demo = new GeocoderDemo();
      double[] latLng = demo.fillInLatLng("24 Willy Mays Plaza San Francisco CA");
      System.out.println(Arrays.toString(latLng));
   }
}
