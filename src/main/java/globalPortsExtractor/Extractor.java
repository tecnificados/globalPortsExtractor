/**
 * 
 */
package globalPortsExtractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.bedatadriven.jackson.datatype.jts.parsers.GenericGeometryParser;
import com.bedatadriven.jackson.datatype.jts.parsers.GeometryParser;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.google.common.io.Files;

/**
 * @author Juan Carlos Ballesteros (tecnificados.com)
 *
 */
public class Extractor {

	private static String portsFilePath = "D:\\datos\\globalPorts\\features.json";

	private static String bigWaterBodiesPath = "D:\\datos\\waterBodies\\body3.geojson";

	/**
	 * @param args
	 */
	public static void main(String[] args) {

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
		
		System.out.println(geoWater.getArea());
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

			geometry=generateGeometryFromGeojson(content);

		} catch (IOException e) {
			System.out.println("Error reading file: " + e.getMessage());
			e.printStackTrace();
		}
		return geometry;

	}

}
