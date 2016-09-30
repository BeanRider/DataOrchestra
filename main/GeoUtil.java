package main;
import java.awt.geom.Point2D;

import processing.core.PVector;

public class GeoUtil {

	// Returns the horizontal distance between two geo points
	public static double measureX(double lat1, double lon1, double lat2,
			double lon2) {
		double R = 6378.137f; // Radius of earth in KM
		double dLat = (lat2 - lat1) * Math.PI / 180; // degree difference in lat
														// (radians)
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double d = R * c;
		if (lat1 < lat2) {
			return -1 * d * 1000; // meters
		}
		return d * 1000; // meters
	}

	// Returns the vertical distance between two geo points
	public static double measureY(double lat1, double lon1, double lat2,
			double lon2) {
		double R = 6378.137f; // Radius of earth in KM
		double dLon = (lon2 - lon1) * Math.PI / 180;
		double a = Math.cos(lat1 * Math.PI / 180)
				* Math.cos(lat2 * Math.PI / 180) * Math.sin(dLon / 2)
				* Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double d = R * c;
		if (lon1 < lon2) {
			return -1 * d * 1000; // meters
		}
		return d * 1000; // meters
	}

	// HELPER: Given a Point2D.Double (non-screen cords) => converted to PVector (screen cords)
	public static PVector convertCartToScreenCoord(double x, double y,
			int parentW, int parentH) {
		return new PVector((float) (-x + parentW / 2.0),
				(float) (parentH / 2.0 + y));
	}

	/**
	 * Converts a geo point to a point on the screen with respect to the provided origin, width, and height
	 * Kept at Double precision because lat/long needs it.
	 *  
	 * @param latLong	Point2D.Double - the one to convert
	 * @param oLatLong	Point2D.Double - the origin
	 * @param parentW   int			   - the width of the viewport
	 * @param parentH   int			   - the height of the viewport
	 * 
	 * @return PVector 				   - the converted point in terms of the computer screen.
	 */
	public static PVector convertGeoToScreen(Point2D.Double latLong,
			Point2D.Double oLatLong, int parentW, int parentH) {

		double y = GeoUtil.measureX(oLatLong.x, oLatLong.y, latLong.x,
				latLong.y); // Calculate horizontal d. between
		double x = GeoUtil.measureY(oLatLong.x, oLatLong.y, latLong.x,
				latLong.y); // Calculate horizontal d. between
		// println("Non-screen coords: x = " + Double.toString(x) + "; y = " +
		// Double.toString(y));

		return convertCartToScreenCoord(x, y, parentW, parentH);
	}

}
