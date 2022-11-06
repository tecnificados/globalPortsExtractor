/**
 * 
 */
package globalPortsExtractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.measure.MetricPrefix;
import javax.measure.Quantity;
import javax.measure.quantity.Length;

import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.bedatadriven.jackson.datatype.jts.parsers.GenericGeometryParser;
import com.bedatadriven.jackson.datatype.jts.parsers.GeometryParser;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.operation.distance.DistanceOp;

import si.uom.SI;
import tech.units.indriya.ComparableQuantity;
import tech.units.indriya.quantity.Quantities;

/**
 * @author Juan Carlos Ballesteros (tecnificados.com)
 *
 */
public class Extractor {

	private static String portsFilePath = "D:\\datos\\globalPorts\\portsWSG84.geojson";

	private static String bigWaterBodiesPath = "D:\\datos\\waterBodies\\body3.geojson";

	/**
	 * @param args
	 * @throws FactoryException 
	 * @throws NoSuchAuthorityCodeException 
	 */
	public static void main(String[] args) throws NoSuchAuthorityCodeException, FactoryException {

		List<Geometry> portsList = new ArrayList<Geometry>();
		List<Geometry> spainPortsList = new ArrayList<Geometry>();

		JSONArray ports = new JSONArray();
		File dataFile = new File(portsFilePath);

		System.out.println("Data file found: " + dataFile.exists());

		JSONParser jsonParser = new JSONParser();

		try {
			FileReader reader = new FileReader(portsFilePath);
			Object obj = jsonParser.parse(reader);
			JSONObject featuresData = (JSONObject) obj;
			ports = (JSONArray) featuresData.get("features");
		} catch (Exception e) {
			System.out.println("Error procesing file");
		}

		System.out.println("There are " + ports.size() + " ports");

		Map<String, Integer> portTypes = new HashMap<String, Integer>();
		for (int i = 0; i < ports.size(); i++) {
			JSONObject actualPort = (JSONObject) ports.get(i);
			String portType = (String) ((JSONObject) actualPort.get("properties")).get("prttype");
			JSONObject geometry = (JSONObject) actualPort.get("geometry");
			JSONArray coordinates = (JSONArray) geometry.get("coordinates");
			JSONObject properties = (JSONObject) actualPort.get("properties");
			JSONObject customPoint = new JSONObject();
			customPoint.put("type", "Point");
			customPoint.put("coordinates", coordinates);
			customPoint.put("properties", properties);

			Geometry actualGeometry = generateGeometryFromGeojson(customPoint.toJSONString());
			portsList.add(actualGeometry);

			if (properties.containsKey("country")) {
				String country = (String) properties.get("country");
				String portname = (String) properties.get("portname");

				if (country != null && country.equalsIgnoreCase("spain")) {
						spainPortsList.add(actualGeometry);
				}
			}

			if (portTypes.containsKey(portType)) {
				Integer v = portTypes.get(portType);
				v++;
				portTypes.put(portType, v);
			} else {
				portTypes.put(portType, Integer.valueOf(0));
			}
		}

		System.out.println("There are " + portTypes.size() + " port types");
		System.out.println(portTypes);

		Geometry geoWater = generateGeometryFromGeojson(new File(bigWaterBodiesPath));
		Geometry p1 = spainPortsList.get(0);
		Geometry p2 = spainPortsList.get(1);
		System.out.println(p1);
		System.out.println(p2);
		System.out.printf("Distance 1:  %f\n", p1.distance(p2));

		String EPSG4326 = "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]";
	    CoordinateReferenceSystem crs = CRS.parseWKT(EPSG4326);
		GeodeticCalculator calculator = new GeodeticCalculator(crs);
		calculator.setStartingGeographicPoint(p1.getCentroid().getX(), p1.getCentroid().getY());
		calculator.setDestinationGeographicPoint(p2.getCentroid().getX(), p2.getCentroid().getY());		
		double distanceInMeters = calculator.getOrthodromicDistance();	
		
	
		for (int i = 0; i < spainPortsList.size(); i++) {
			Geometry portGeometry = portsList.get(i);
			double distance3 = geoWater.distance(portGeometry);
			System.out.println(distance3);
			
			Coordinate[] nearestPoints = DistanceOp.nearestPoints(geoWater, portGeometry);
					
			System.out.println(nearestPoints);
			
			Coordinate c1 = nearestPoints[0];
			Coordinate c2 = nearestPoints[1];
			
			
			calculator.setStartingGeographicPoint(c1.x, c1.y);
			calculator.setDestinationGeographicPoint(c2.x, c2.y);		
			double distanceInMeters2 = calculator.getOrthodromicDistance();	
			
			System.out.println(distanceInMeters2+" metros");
			
		}
		System.out.println("End");
	}

	private static Geometry generateGeometryFromGeojson(String content) {
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

	private static Geometry generateGeometryFromGeojson(File fileToLoad) {
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

}
