import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

//retrieve weather data from API - this backend logic will fetch the latest 
//weather data from the external API and return it.
//The GUI will display the data to the user
public class WeatherApp {
	//fetch weather data for given location
	public static JSONObject getWeatherData(String locationName) {
		//get location coordinates using the geolocation API
		JSONArray locationData = getLocationData(locationName);
		
		//extract latitude and longitude data
		JSONObject location = (JSONObject) locationData.get(0);
		double latitude = (double) location.get("latitude");
		double longitude = (double) location.get("longitude");
		
		//build API request URL with location coordinates
		String urlString = "https://api.open-meteo.com/v1/dwd-icon?"
				+ "latitude=" + latitude +"&longitude=" + longitude +"&hourly=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m";
		
		try {
			//call api and get response
			HttpURLConnection conn = fetchApiResponse(urlString);
			
			//check for response status
			//200 means that the connection was a success
			if(conn.getResponseCode() != 200) {
				System.out.println("Error: Could not connect to API");
				return null;
			}
			
			//store resulting json data
			StringBuilder resultJson = new StringBuilder();
			Scanner scanner = new Scanner(conn.getInputStream());
			while(scanner.hasNext()) {
				//read and store into the string builder
				resultJson.append(scanner.nextLine());
			}
			
			//close scanner
			scanner.close();
			
			//close url connection
			conn.disconnect();
			
			//parse our data
			JSONParser parser = new JSONParser();
			JSONObject resultJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));
			
			//retrieve hourly data
			JSONObject hourly = (JSONObject) resultJsonObj.get("hourly");
			
			//we only want the current hour's data
			//so we need to get the index of the current hour
			JSONArray time = (JSONArray) hourly.get("time");
			int index = findIndexOfCurrentTime(time);
			
			//get temperature
			JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
			double temperature = (double) temperatureData.get(index);
			
			//get weather code
			JSONArray weathercode = (JSONArray) hourly.get("weather_code");
			String weatherCondition = convertWeatherCode((long) weathercode.get(index));
			
			//get humidity
			JSONArray relativeHumidity = (JSONArray) hourly.get("relative_humidity_2m");
			long humidity = (long) relativeHumidity.get(index);
			
			//get windspeed
			JSONArray windspeedData = (JSONArray) hourly.get("wind_speed_10m");
			double windspeed = (double) windspeedData.get(index);
			
			//build the weather json data object that we are going to access in our front end
			//we will return this object so we can access and display it in the gui
			JSONObject weatherData = new JSONObject();
			weatherData.put("temperature", temperature);
			weatherData.put("weather_condition", weatherCondition);
			weatherData.put("humidity", humidity);
			weatherData.put("windspeed", windspeed);
			
			return weatherData;
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	//retrieves geographic coordinates for given location
	public static JSONArray getLocationData(String locationName) {
		//replace any whitespaces with + to adhere to API's format
		locationName = locationName.replaceAll(" ", "+");
		
		//build API url with location parameter
		String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" +
				locationName + "&count=10&language=en&format=json";
		
		try {
			//call api and get a response
			HttpURLConnection conn = fetchApiResponse(urlString);
			
			//check response status
			//200 means successful connection
			if(conn.getResponseCode() != 200){
				System.out.println("Error: Could not connect to API");
				return null;
			}else {
				//store the API results
				StringBuilder resultJson = new StringBuilder();
				//use a scanner to read json data that is returned from our API
				Scanner scanner = new Scanner(conn.getInputStream());
				
				//read and store 
				while(scanner.hasNext()) {
					resultJson.append(scanner.nextLine());
				}
				
				//close scanner
				scanner.close();
				
				//close url connection
				conn.disconnect();
				
				//parse the json string into a json obj
				JSONParser parser = new JSONParser();
                JSONObject resultsJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));
				
				//get the list of location data the API generated from the location name
				JSONArray locationData = (JSONArray) resultsJsonObj.get("results");

				return locationData;
			}
			
			
		}catch(Exception e) {
			e.printStackTrace();

		}
		return null;
	}
	
	private static HttpURLConnection fetchApiResponse(String urlString) {
		try {
			//attempt to create connecton
			URL url = new URL(urlString);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			
			//set request method to get
			conn.setRequestMethod("GET");
			
			//connect to API
			conn.connect();
			return conn;
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		//cound not make connection
		return null;
	}
	
	private static int findIndexOfCurrentTime(JSONArray timeList) {
		String currentTime = getCurrentTime();
		
		//iterate through the time list and see which one matches our current time
		for(int i = 0; i < timeList.size(); i++) {
			String time = (String) timeList.get(i);
			if(time.equalsIgnoreCase(currentTime)) {
				//return the index
				return i;
			}
		}
		
		return 0;
	}
	
	public static String getCurrentTime() {
		//get current date and time
		LocalDateTime currentDateTime = LocalDateTime.now();
		
		//format date to be 2024-02-0100:00 (This is how it is read in the API)
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'");
		
		//format and print the current date and time
		String formattedDateTime = currentDateTime.format(formatter);
		
		return formattedDateTime;
	}
	
	private static String convertWeatherCode(long weathercode) {
		String weatherCondition = "";
		if(weathercode == 0L) {
			//clear
			weatherCondition = "Clear";
		}else if(weathercode <= 3L && weathercode > 0L) {
			//cloudy
			weatherCondition = "Cloudy";
		}else if((weathercode >= 5L && weathercode <= 67L)
				|| (weathercode >= 80L && weathercode <= 99L)) {
			//rain
			weatherCondition = "Rain";
		}else if(weathercode >= 71L && weathercode <= 77L) {
			//snow
			weatherCondition = "Snow";
		}
		
		return weatherCondition;
	}
}






