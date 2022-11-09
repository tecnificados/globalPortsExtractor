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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
		JsonArray portsList = new JsonArray();
		JsonArray portsListInsideBodies = new JsonArray();

		JsonArray ports = new JsonArray();
		File dataFile = new File(portsFilePath);

		System.out.println("Data file found: " + dataFile.exists());	

		try {
			FileReader reader = new FileReader(portsFilePath);
			Object obj = JsonParser.parseReader(reader);
			JsonObject featuresData = (JsonObject) obj;
			ports = (JsonArray) featuresData.get("features");
		} catch (Exception e) {
			System.out.println("Error procesing file");
		}

		System.out.println("There are " + ports.size() + " ports");

		Map<String, Integer> portTypes = new HashMap<String, Integer>();
		for (int i = 0; i < ports.size(); i++) {
			JsonObject actualPort = (JsonObject) ports.get(i);			
			JsonObject properties = actualPort.getAsJsonObject("properties");
			String portType = properties.get("prttype").toString();			
			JsonObject geometry = (JsonObject) actualPort.get("geometry");
			JsonArray coordinates = (JsonArray) geometry.get("coordinates");			
			JsonObject customPoint = new JsonObject();
			customPoint.addProperty ("type", "Point");
			customPoint.add("coordinates", coordinates);
			customPoint.add("properties", properties);
			portsList.add(customPoint);
			
			Geometry actualGeometry = GeoToolUtils.generateGeometryFromGeojson(customPoint.toString());
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
		
		
		writeJSON(portsListInsideBodies.toString(), jsonOutputPath);
		
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
