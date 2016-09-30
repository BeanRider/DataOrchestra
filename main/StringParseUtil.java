package main;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class StringParseUtil {
	
	/**
	 * Converts a String with long/lat into a Point2D of (lat/long)
	 * @param centroidLongLat - String
	 * @return Point2D.Double - as lat/long
	 */
	public static Point2D.Double parseCentroidString(String centroidLongLat) {
		List<Double> doubleLongLat = getDoubleFromSet(centroidLongLat);
        double latitude = doubleLongLat.get(1); // flip the given centroid
        double longitude = doubleLongLat.get(0); // flip the given centroid order
        //println("lat = " + Double.toString(latitude) + "; long = " + Double.toString(longitude));
        return new Point2D.Double(latitude, longitude); 
	}
	
	/**
	 * Parses the given set (MUST BE TWO LEVEL DEEP) into sets of Doubles, only one level deeper
	 * @param set- must be surrounded by '[' and ']', and have distinct values separated by ','.
	 * ONLY USE FOR TWO LEVEL DEEP STRING ARRAY
	 */
	public static List<Double> getDoubleFromSet(String set) {
        
        Objects.requireNonNull(set);
        
        List<Double> resultValues = new ArrayList<>();
        
        // 1. Remove '['
        String currentString = set.substring(1);
        
        int startI = 0;
        // 2. Run through "currentString"
        for (int i = 0; i < currentString.length(); ++i) {
            
            char now = currentString.charAt(i);
            // Found a nest, start capturing this nest without going deeper
            if (now == '[') {
                startI = i;
                // finding ending ']'
                for (int s = i; s < currentString.length(); ++s) {
                    if (currentString.charAt(s) == ']') {
                        resultValues.add(Double.parseDouble(currentString.substring(startI, s + 1).trim()));
                        i = s + 2; // set main loop to the index two after the '],'
                        startI = s + 2; // set next startingIndex for substring
                        break;
                    }
                }
            }
            // if current character is a ','; extract
            else if (now == ',') {
                // Add it to the list of doubles
                resultValues.add(Double.parseDouble(currentString.substring(startI, i).trim()));
                startI = i + 1;
            }
            // if current character is a ']'; extract final
            else if (now == ']' && i == currentString.length() - 1) {
                // Last value; excluding the ']'
                String lastValue = currentString.substring(startI, currentString.length() - 1).trim();
                resultValues.add(Double.parseDouble(lastValue));
                break;
            }
        }
        
        //String[] resultArr = resultValues.toArray(new String[resultValues.size()]);
        return resultValues;
	}

	/**
     * Parses the given set (MUST BE TWO LEVEL DEEP) into sets of Strings, only one level deeper
     * @param set- must be surrounded by '[' and ']', and have distinct values separated by ','.
     * ONLY USE FOR TWO LEVEL DEEP STRING ARRAY
     */
    public static List<String> getListFromSet(String set) {
        
        Objects.requireNonNull(set);
        
        List<String> resultValues = new ArrayList<String>();
        String currentString = set;
        
        // 1. Remove '['
        currentString = set.substring(1);
        
        int startI = 0;
        // 2. Run through "currentString"
        for (int i = 0; i < currentString.length(); ++i) {
            
            char now = currentString.charAt(i);
            // Found a nest, start capturing this nest without going deeper
            if (now == '[') {
                startI = i;
                // finding ending ']'
                for (int s = i; s < currentString.length(); ++s) {
                    if (currentString.charAt(s) == ']') {
                        resultValues.add(currentString.substring(startI, s + 1).trim());
                        i = s + 2; // set main loop to the index two after the '],'
                        startI = s + 2; // set next startingIndex for substring
                        break;
                    }
                }
            }
            // if current character is a ','; extract
            else if (now == ',') {
                // Add it to the list of doubles
                resultValues.add(currentString.substring(startI, i).trim());
                startI = i + 1;
            }
            // if current character is a ']'; extract final
            else if (now == ']' && i == currentString.length() - 1) {
                // Last value; excluding the ']'
                String lastValue = currentString.substring(startI, currentString.length() - 1).trim();
                resultValues.add(lastValue);
                break;
            }
        }
        
        //String[] resultArr = resultValues.toArray(new String[resultValues.size()]);
        return resultValues;
    }
}
