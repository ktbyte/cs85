package com.ktbyte.cs85.pollution.sparkutils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

/**
 * Allows the changing of log level for orrg.slf4j.Logger via reflection
 */
public class LogbackUtils {

	public static boolean setLogLevelTrace() {
		return setLogLevel(Level.TRACE);
	}
	public static boolean setLogLevelDebug() {
		return setLogLevel(Level.DEBUG);
	}
	public static boolean setLogLevelInfo() {
		return setLogLevel(Level.INFO);
	}
	public static boolean setLogLevelWarn() {
		return setLogLevel(Level.WARN);
	}
	
	
	/**
	 * Dynamically sets the logback log level for the given class to the specified level.
	 *
	 * @param logLevel   One of the supported log levels: TRACE, DEBUG, INFO, WARN, ERROR, FATAL,
	 *                   OFF. {@code null} value is considered as 'OFF'.
	 */
	public static boolean setLogLevel(Level level)
	{
		Logger loggerObtained = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		((ch.qos.logback.classic.Logger)loggerObtained).setLevel(level);
		return true;
	}
}
