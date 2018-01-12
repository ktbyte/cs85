package com.ktbyte.cs85.pollution;

import static spark.Spark.post;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.ktbyte.cs85.pollution.sparkutils.LogbackUtils;
import com.ktbyte.cs85.pollution.sparkutils.ResourceLoader;
import com.ktbyte.cs85.pollution.sparkutils.SparkUtils;
import com.ktbyte.cs85.pollution.sparkutils.WebFetcher;

import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Spark;

public class BreezometerRequester {
	public static String getRealIp(Request request) {
		if (request.headers("X-Forwarded-For") != null
				&& (request.ip().equals("127.0.0.1") || request.ip().equals("0:0:0:0:0:0:0:1"))) {
			return request.headers("X-Forwarded-For");
		} else {
			return request.ip();
		}
	}

	static class BreezometerRequesterFilter implements Filter {

		private final transient Logger log = getLogger();

		public static List<String> latestRequests = Collections.synchronizedList(new LinkedList<String>());

		private static final List<String> EXCLUDED_PARAMS_IN_LOG = Arrays.asList("_password", "password",
				"passwordconfirm", "_csrf_token", "g-recaptcha-response", "studentpass");

		protected Logger getLogger() {
			return LoggerFactory.getLogger(BreezometerRequesterFilter.class);
		}

		public void handle(Request request, Response response) {
			Gson gs = new Gson();
			Map<String, Object> requestLog = new LinkedHashMap<>();
			requestLog.put("method", request.requestMethod());
			requestLog.put("path", request.pathInfo());
			requestLog.put("ip", getRealIp(request));

			if ("GET".equals(request.requestMethod()) || "POST".equals(request.requestMethod())) {
				HashMap<String, String> queryParams = new HashMap<>();
				for (String param : request.queryParams()) {
					if (EXCLUDED_PARAMS_IN_LOG.contains(param)) {
						queryParams.put(param, "*****");
					} else {
						queryParams.put(param, request.queryParams(param));
					}
				}
				requestLog.put("params", queryParams);
			}
			log.info("REQUEST=" + gs.toJson(requestLog));

			String reqInfo = new Date().toString() + ": " + getRealIp(request) + " " + request.pathInfo();
			synchronized (latestRequests) {
				if (!reqInfo.contains("0:0:0:0:0:0:0:1")) {
					latestRequests.add(0, reqInfo);
					while (latestRequests.size() > 22)
						latestRequests.remove(latestRequests.size() - 1);
				}
			}

			// permission matching code removed
		}

	}

	public static void main(String[] args) {
		Spark.port(23232);
		LogbackUtils.setLogLevelInfo();
		Logger logger = LoggerFactory.getLogger(BreezometerRequester.class);
		SparkUtils.createServerWithRequestLog(logger);
		Spark.before(new BreezometerRequesterFilter());
		post("/breezometer", (request, response) -> {

			double lat = Double.parseDouble(request.queryParams("lat")),
					lon = Double.parseDouble(request.queryParams("lon"));
			final String apiKey = request.queryParams("apikey");
			String out = WebFetcher
					.fetchURL("https://api.breezometer.com/baqi/?lat=" + lat + "&lon=" + lon + "&key=" + apiKey);
			return out;
		});

		Spark.get("/mapdl", (request, response) -> {
			// Runtime rt = Runtime.getRuntime();
			// Process pr = rt.exec("C:/Program Files/nodejs/node
			// C:/Users/user/AppData/Roaming/npm/map-dl");
//			Process process = new ProcessBuilder("node", "map-dl").directory(new File("/")).start();
			double north = Double.parseDouble(request.queryParams("north"));
			double east = Double.parseDouble(request.queryParams("east"));
			double south = Double.parseDouble(request.queryParams("south"));
			double west = Double.parseDouble(request.queryParams("west"));
			double scale = Double.parseDouble(request.queryParams("scale"));
			
			
			String outDir = isWindows() ? "C:/Users/user/Desktop/mapdlout" : System.getProperty("user.home")+"/mapdlout";
			{
				//delete previous png files
				File directory = new File(outDir);
				File[] files = directory.listFiles();
				for(File file : files) {
					if(file.getName().toLowerCase().endsWith("png")) {
						file.delete();
					}
				}
			}
			
			System.err.println("north: "+north+" east: "+east+" south: "+south+" west: "+west);
			
			try {
				String runPath = isWindows() ? "C:/Users/user/AppData/Roaming/npm/" : System.getProperty("user.home")+"/";
				/*
				 * Location where the Nodejs Project is Present
				 */
				System.out.println(runPath);
				
				
				

				File jsFile = new File(runPath);
				List<String> command = new ArrayList<String>();
				if(isWindows()) {
					String cmdPrompt = "cmd";
					String path = "/c";
					command.add(cmdPrompt);
					command.add(path);
					
					String mapdl = "map-dl \"["+north+","+east+","+south+","+west+"]\" --scale "+scale+" --output "+outDir;
					command.add(mapdl);
				}else {
					//linux
					command.add("/bin/bash");
					
					String mapdl = "/usr/bin/map-dl \"["+north+","+east+","+south+","+west+"]\" --scale "+scale+" --output "+outDir;
					;
					command.add(createTempScript(mapdl).getAbsolutePath());
				}

				runExecution(command, jsFile);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			File directory = new File(outDir);
			File[] files = directory.listFiles();
			for(File file : files) { //will return the first image found
				System.out.println(file);
				String path = file.getAbsolutePath();
				try {
					String type = Files.probeContentType(Paths.get(path));
					if(type == null) {
						if(path.endsWith(".css")) type = "text/css";
						if(path.endsWith(".js")) type = "text/javascript";
						if(path.endsWith(".pde")) type = "text/plain";
						if(path.endsWith(".ogg")) type = "audio/ogg";
						if(path.endsWith(".mp4")) type = "audio/mp4";
						if(path.endsWith(".png")) type = "image/png";
						if(path.endsWith(".ttf")) type = "application/x-font-ttf";
					}
					response.type(type);
				} catch (IOException e) {
					response.status(404);
					return "file could not be read";
				}
				
				
				//Ready binary:
				try {
					byte[] data = ResourceLoader.loadFileBytes(path);
					return data;
				} catch (NullPointerException e) {
					response.status(404);
					return "file contents could not be read";
				}
			}
			
			
			return "";
		});
	}

	public static File createTempScript(String mapdl) throws IOException {
	    File tempScript = File.createTempFile("script", null);

	    Writer streamWriter = new OutputStreamWriter(new FileOutputStream(
	            tempScript));
	    PrintWriter printWriter = new PrintWriter(streamWriter);

	    printWriter.println("#!/bin/bash");
	    printWriter.println(mapdl);

	    printWriter.close();

	    return tempScript;
	}
	
	static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}

	public static String runExecution(List<String> command, File navigatePath) throws IOException, InterruptedException {

		System.out.println(command);

		ProcessBuilder executeProcess = new ProcessBuilder(command);
		executeProcess.directory(navigatePath);
		Process resultExecution = executeProcess.start();

		BufferedReader br = new BufferedReader(new InputStreamReader(resultExecution.getInputStream()));
		StringBuffer sb = new StringBuffer();

		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line + System.getProperty("line.separator"));
		}
		br.close();
		
		BufferedReader br2 = new BufferedReader(new InputStreamReader(resultExecution.getErrorStream()));
		StringBuffer sb2 = new StringBuffer();

		String line2;
		while ((line2 = br2.readLine()) != null) {
			sb.append(line2 + System.getProperty("line.separator"));
		}
		br2.close();
		
		int resultStatust = resultExecution.waitFor();
		System.err.println("out: "+sb);
		System.err.println("err: "+sb2);
		System.out.println("Result of Execution" + (resultStatust == 0 ? "\tSuccess" : "\tFailure"));
		if(resultStatust != 0) return null; //FAiled to execute
		return sb.toString();
	}
}
