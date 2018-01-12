package com.ktbyte.cs85.pollution.sparkutils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this to load resources in the class path of the jar file
 */
public class ResourceLoader {
	private static final transient Logger log = LoggerFactory.getLogger(ResourceLoader.class);

	public static InputStream getStream(String path) throws IOException {
		File file = new File(path);
		return FileUtils.openInputStream(file);
	}
	
	public static byte[] loadFileBytes(String file) {
		try {
			log.debug("Loading resource: "+file);
			InputStream stream = getStream(file);
			byte[] out = IOUtils.toByteArray(stream);
			stream.close();
			return out;
		} catch (Exception e) {
			log.error("Failed loading file "+file+". Exception: "+e.toString());
			return null;
		}
	}
	
	public static String loadFileString(String file) {
		try {
			log.debug("Loading resource: "+file);
			InputStream stream = getStream(file);
			String out = IOUtils.toString(stream, StandardCharsets.UTF_8);
			stream.close();
			return out;
		} catch (Exception e) {
			log.error("Failed loading file "+file+". Exception: "+e.toString());
			return null;
		}
	}
}
