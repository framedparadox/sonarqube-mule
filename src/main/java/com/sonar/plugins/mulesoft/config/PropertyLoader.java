package com.sonar.plugins.mulesoft.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe property loader and cache for MuleSoft plugin configurations.
 *
 * <p>This utility class manages loading and caching of configuration properties from
 * resource files, providing efficient access to plugin settings across multiple
 * sensors and components. It implements a thread-safe caching strategy to minimize
 * I/O operations and improve performance.</p>
 *
 * <h3>Property File Resolution:</h3>
 * <p>Properties are loaded from classpath resources with language-specific fallback:
 * <ul>
 *   <li><strong>Primary:</strong> {@code <language>.properties} (e.g., {@code mule.properties})</li>
 *   <li><strong>Fallback:</strong> {@code mulesoft.properties} (default configuration)</li>
 * </ul>
 * </p>
 *
 * <h3>Caching Strategy:</h3>
 * <p>Uses {@link ConcurrentHashMap} for thread-safe property caching:
 * <ul>
 *   <li>Properties are loaded once per unique key combination</li>
 *   <li>Subsequent requests return cached instances</li>
 *   <li>Cache persists for the lifetime of the JVM/analysis session</li>
 *   <li>Thread-safe concurrent access without synchronization overhead</li>
 * </ul>
 * </p>
 *
 * <h3>Configuration Examples:</h3>
 * <pre>
 * # mule.properties - XPath expressions for metrics
 * mule.metric.flow=//mule:flow
 * mule.metric.subflow=//mule:sub-flow
 * mule.metric.dw.payload=//dw:transform[dw:set-payload]
 * mule.metric.test=//munit:test
 * 
 * # Coverage report property keys
 * mule.munit.properties.name=name
 * mule.munit.properties.coverage=coverage  
 * mule.munit.properties.lines=lines
 * </pre>
 *
 * <h3>Usage Patterns:</h3>
 * <pre>
 * // Get metric XPath expressions
 * Properties props = PropertyLoader.getProperties("mule");
 * String flowXPath = props.getProperty(MetricKeys.METRIC_FLOW);
 * 
 * // Language-specific configuration access
 * String testXPath = PropertyLoader.getProperties(language)
 *     .getProperty(MetricKeys.METRIC_UNIT_TESTS);
 * </pre>
 *
 * <h3>Error Handling:</h3>
 * <p>Missing property files result in warning logs and empty {@link Properties}
 * objects rather than exceptions, ensuring graceful degradation.</p>
 *
 * @see MetricKeys for standard property key constants
 * @see ConcurrentHashMap for thread-safe caching implementation
 * @since 1.0.0
 * @author MuleSoft SonarQube Plugin Team
 */
public class PropertyLoader {

	// private static final String MULE_PROPERTIES = "mule.properties";
	private static final String MULESOFT_PROPERTIES = "mulesoft.properties";

	private static final Logger logger = LoggerFactory.getLogger(PropertyLoader.class);

	// Thread-safe cache for loaded properties
	private static final Map<String, Properties> props = new ConcurrentHashMap<>();

	private static Properties loadProp(String propName) {
		Properties properties = new Properties();
		try (InputStream input = PropertyLoader.class.getClassLoader().getResourceAsStream(propName)) {
			if (input == null) {
				logger.warn("Property file not found: {}", propName);
				return properties;
			}
			properties.load(input);
		} catch (IOException e) {
			logger.error("Error loading properties from {}: {}", propName, e.getMessage(), e);
		}
		return properties;
	}

	public static Properties getProperties() {
		return props.computeIfAbsent("mulesoft", key -> loadProp(MULESOFT_PROPERTIES));
	}

	/**
	 * @deprecated Use {@link #getProperties()} instead.
	 */
	@Deprecated
	public static Properties getProperties(String language) {
		return getProperties();
	}
}