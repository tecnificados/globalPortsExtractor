/**
 * 
 */
package globalPortsExtractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.bedatadriven.jackson.datatype.jts.parsers.GenericGeometryParser;
import com.bedatadriven.jackson.datatype.jts.parsers.GeometryParser;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author Juan Carlos Ballesteros (tecnificados.com)
 *
 */
public class GeoToolUtils {
	
	public static double wrongDistance=100000000;
	
	private static String EPSG4326 = "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]";
    
	private static CoordinateReferenceSystem crs;
	
	static {
		try {
			crs = CRS.parseWKT(EPSG4326);
		} catch (FactoryException e) {
			System.out.println("Error preparing WTW parser");
			e.printStackTrace();
		}
	}

	public static Geometry generateGeometryFromGeojson(String content) {
		Geometry geometry = null;
		JsonFactory factory = new JsonFactory();
		com.vividsolutions.jts.geom.GeometryFactory gf = new GeometryFactory();
		try {
			JsonParser lParser = factory.createParser(content.getBytes());
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new JtsModule());
			ObjectNode node = mapper.readTree(lParser);
			GeometryParser<Geometry> gParser = new GenericGeometryParser(gf);
			geometry = gParser.geometryFromJson(node);
		} catch (Exception e) {
			System.out.println("Error reading geometry: " + e.getMessage());
			e.printStackTrace();
		}
		return geometry;
	}

	public static Geometry generateGeometryFromGeojson(File fileToLoad) {
		FileInputStream fileInputStream = null;
		Geometry geometry = null;
		try {
			fileInputStream = new FileInputStream(fileToLoad);
			String content = new String(fileInputStream.readAllBytes());

			geometry = generateGeometryFromGeojson(content);

		} catch (IOException e) {
			System.out.println("Error reading file: " + e.getMessage());
			e.printStackTrace();
		}
		return geometry;
	}
	
	/*Return the distance in meter from two points*/
	public static double distanceFromTwoPoint(double point1X, double point1Y, double point2X, double point2Y) {
		double distanceInMeters = wrongDistance;		
		if (crs!=null) {
			GeodeticCalculator calculator = new GeodeticCalculator(crs);
			calculator.setStartingGeographicPoint(point1X, point1Y);
			calculator.setDestinationGeographicPoint(point2X, point2Y);		
			distanceInMeters = calculator.getOrthodromicDistance();
		}
		return distanceInMeters;
	}
	
}
