import lombok.AllArgsConstructor;
import lombok.Data;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Genie {
	private String time;
	public String extract() throws Exception {
		OkHttpClient client = new OkHttpClient().newBuilder().build();
		HttpUrl url = new HttpUrl.Builder()
				.scheme("https")
				.host("ecb.europa.eu")
				.addPathSegment("stats")
				.addPathSegment("eurofxref")
				.addPathSegment("eurofxref-daily.xml")
				.build();
		Request request = new Request.Builder()
				.url(url)
				.method("GET", null)
				.build();

		Response response = client.newCall(request).execute();
		String responseBody = Objects.requireNonNull(
				response.body()).string();

		return responseBody;
	}

	public void readXml(String xmlDataString, List<ExchangeObject> xmlDataList, Map<String, Double> destinationCurrencies) throws ParserConfigurationException, IOException, SAXException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(new ByteArrayInputStream(xmlDataString.getBytes(StandardCharsets.UTF_8)));
		Element rootElement = document.getDocumentElement();
		NodeList cubeElements = rootElement.getElementsByTagName("Cube");
		destinationCurrencies.put("EUR", 1.0);
		for (int i = 0; i < cubeElements.getLength(); i++) {
			Element cubeElement = (Element) cubeElements.item(i);
			if (!"".equalsIgnoreCase(cubeElement.getAttribute("time"))) {
				time = cubeElement.getAttribute("time");
			} else if (!"".equalsIgnoreCase(cubeElement.getAttribute("currency"))) {
				String currency = cubeElement.getAttribute("currency");
				String rate = cubeElement.getAttribute("rate");
				double exRate = Double.parseDouble(rate);
				double exSrcToDestEur = 1 / exRate;
				xmlDataList.add(new ExchangeObject(time, "EUR", currency, exRate));
				destinationCurrencies.put(currency, exSrcToDestEur);
			}
		}
	}

	public void writeToCsv(List<ExchangeObject> xmlData, Map<String, Double> destinationCurrencies, File outputFile) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile.getAbsolutePath()));
		writer.write("DATE,SOURCE,TARGET,RATE\n");
		for(Map.Entry<String,Double> entry : destinationCurrencies.entrySet()){
			writer.write(time + "," + entry.getKey() + "," + "EUR" + "," + entry.getValue() + "\n");
			for (ExchangeObject exchangeObject : xmlData) {
				ExchangeObject o = new ExchangeObject(time,entry.getKey(),exchangeObject.getTarget(), entry.getValue() * exchangeObject.getExchangeRate());
				writer.write(o.toString());
				writer.write("\n");
			}
		}
		writer.close();
	}

	public static void main(String[] args) throws Exception {
		Genie genie = new Genie();
		String xmlString = genie.extract();
		List<ExchangeObject> xmlDataList = new ArrayList<>();
		Map<String, Double> destinationCurrencies = new HashMap<>();
		genie.readXml(xmlString, xmlDataList, destinationCurrencies);
		File file = new File("/Users/shubhamdalmia/Downloads/exchangeData.csv");
		genie.writeToCsv(xmlDataList,destinationCurrencies,file);
	}
}

@Data
@AllArgsConstructor
class ExchangeObject{
	private String date;
	private String source;
	private String target;
	private double exchangeRate;

	@Override
	public String toString() {
		return  date + ',' + source + ',' + target + ',' + exchangeRate ;
	}
}

