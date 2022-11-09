/**
 * 
 */
package globalPortsExtractor;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.distance.DistanceOp;

/**
 * @author Juan Carlos Ballesteros (tecnificados.com)
 *
 */
public class Extractor {

	private static String portsFilePath = "data\\portsWSG84.geojson";
	private static String bigWaterBodiesPath = "data\\bigWaterBodyWSG84.geojson";
	private static String jsonOutputPath;
	
	static
	{
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
		jsonOutputPath="output\\"+LocalDate.now().format(formatter)+"_ports.json";
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		List<Geometry> geometryPortsList = new ArrayList<Geometry>();
		JSONArray portsList = new JSONArray();
		JSONArray portsListInsideBodies = new JSONArray();

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
			portsList.add(customPoint);

			
			System.out.println(customPoint.toJSONString());
			Geometry actualGeometry = GeoToolUtils.generateGeometryFromGeojson(customPoint.toJSONString());
			geometryPortsList.add(actualGeometry);

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

		Geometry geoWater = GeoToolUtils.generateGeometryFromGeojson(new File(bigWaterBodiesPath));

		int counter = 0;
		for (int i = 0; i < geometryPortsList.size(); i++) {
			Geometry portGeometry = geometryPortsList.get(i);
			double distance3 = geoWater.distance(portGeometry);
			Coordinate[] nearestPoints = DistanceOp.nearestPoints(geoWater, portGeometry);
			Coordinate c1 = nearestPoints[0];
			Coordinate c2 = nearestPoints[1];

			double distanceInMeters = GeoToolUtils.distanceFromTwoPoint(c1.x, c1.y, c2.x, c2.y);

			if (distanceInMeters == GeoToolUtils.wrongDistance) {
				throw new Exception("Wrong distance calculation");
			}
			double distanceInKM = distanceInMeters / 1000;

			if (distanceInKM <= 5) {				
				counter++;
				portsListInsideBodies.add(portsList.get(i));
			}
		}
		System.out.println("There are "+counter+" ports in big water bodies");
		
		
		writeJSON(portsListInsideBodies.toJSONString(), jsonOutputPath);
		
		//TODO generate GeoJSON
		
		System.out.println("End");
	}

	private static void writeJSON(String content, String filePath) throws IOException {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();		
		JsonElement je = JsonParser.parseString(content);
		String prettyJsonString = gson.toJson(je);		
		FileWriter writer = new FileWriter(filePath);
		writer.write(prettyJsonString);
		writer.close();
	}

}
