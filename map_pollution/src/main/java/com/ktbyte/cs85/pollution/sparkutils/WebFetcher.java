package com.ktbyte.cs85.pollution.sparkutils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class WebFetcher {
	private static String USER_AGENT = "ktbyte:com.ktbyte.cs85:v0.0.2";
	private static final transient Logger log = LoggerFactory.getLogger(WebFetcher.class);
	public static String getHTML(URL url) {
		return getHTML(url, 3);
	}
	public static String getHTML(URL url, int followRedircts) {
		if(followRedircts < 0) return null;
		log.info("Fetching: "+url);
	    try {
	    	
	        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
	        urlConnection.addRequestProperty("User-Agent", USER_AGENT);
	        urlConnection.setInstanceFollowRedirects(true);
	        urlConnection.setConnectTimeout(5*1000);
	        urlConnection.setReadTimeout(5*1000);
	        HttpURLConnection.setFollowRedirects(true);
	        
	        boolean redirect = false;
	        
	    	// normally, 3xx is redirect
	    	int status = urlConnection.getResponseCode();
	    	if (status != HttpURLConnection.HTTP_OK) {
	    		if (status == HttpURLConnection.HTTP_MOVED_TEMP
	    			|| status == HttpURLConnection.HTTP_MOVED_PERM
	    				|| status == HttpURLConnection.HTTP_SEE_OTHER)
	    		redirect = true;
	    	}
	     
	    	if (redirect) {
	    		String newUrl = urlConnection.getHeaderField("Location");
	    		return getHTML(new URL(newUrl),followRedircts-1);
	    	}
	        
	        final InputStream inputStream = urlConnection.getInputStream();
	        final String html = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
	        inputStream.close();
	        return html;
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	}
	public static String fetchURL(String url) throws MalformedURLException, IOException {
		return getHTML(new URL(url));
	}
	public static String fetchURL(String url, int wait, int tries) {
		for(int i = 0 ; i < tries; i++) {
			try {
				Thread.sleep(wait);
				return getHTML(new URL(url));
			}catch(Exception e){}
		}
		return null;
	}
	public static String[] fetchJsonStringArray(String url) throws JsonSyntaxException, MalformedURLException, IOException {
		return new Gson().fromJson(fetchURL(url), String[].class);
	}
	@SuppressWarnings("unchecked")
	public static Map<Object,Object> fetchJsonMap(String url) throws JsonSyntaxException, MalformedURLException, IOException {
		return new Gson().fromJson(fetchURL(url), Map.class);
	}
	
	public static String getContentType(String urlname) throws IOException {
		URL url = new URL(urlname);
		HttpURLConnection connection = (HttpURLConnection)  url.openConnection();
		connection.setConnectTimeout(5*1000);
		connection.setReadTimeout(5*1000);
		connection.setRequestMethod("HEAD");
		connection.connect();
		return connection.getContentType();
	}
	public static String fetchSection(String content, String startKeyword, String endKeyword) {
		content = content.toLowerCase();
		int startIndex = content.indexOf(startKeyword.toLowerCase());
		int endIndex = content.lastIndexOf(endKeyword.toLowerCase());
		if(startIndex >= 0 && endIndex >= 0) return content.substring(startIndex,endIndex);
		return "";
	}
	@SuppressWarnings("rawtypes")
	public static List fetchJsonList(String url) throws JsonSyntaxException, MalformedURLException, IOException {
		return new Gson().fromJson(fetchURL(url), List.class);
	}
	
	public static class MyTrustTrategy implements TrustStrategy {
        public boolean isTrusted(X509Certificate[] a, String b) throws CertificateException {
            return true;
        }
    }
	
	public static HttpClient createHttpClient_AcceptsUntrustedCerts() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
	    HttpClientBuilder b = HttpClientBuilder.create();
	 
	    // setup a Trust Strategy that allows all certificates.
	    //
	    SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new MyTrustTrategy()).build();
	    b.setSSLContext( sslContext);
	 
	    // don't check Hostnames, either.
	    //      -- use SSLConnectionSocketFactory.getDefaultHostnameVerifier(), if you don't want to weaken
		NoopHostnameVerifier hostnameVerifier = new NoopHostnameVerifier();
	 
	    // here's the special part:
	    //      -- need to create an SSL Socket Factory, to use our weakened "trust strategy";
	    //      -- and create a Registry, to register it.
	    //
	    SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
	    Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
	            .register("http", PlainConnectionSocketFactory.getSocketFactory())
	            .register("https", sslSocketFactory)
	            .build();
	 
	    // now, we create connection-manager using our Registry.
	    //      -- allows multi-threaded use
	    PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager( socketFactoryRegistry);
	    b.setConnectionManager( connMgr);
	 
	    // finally, build the HttpClient;
	    //      -- done!
	    HttpClient client = b.build();
	    return client;
	}

}
