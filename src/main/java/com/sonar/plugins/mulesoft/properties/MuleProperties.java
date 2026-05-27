package com.sonar.plugins.mulesoft.properties;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuleProperties {

	// private static final String MULE_PROPERTIES = "mule.properties";
	private static final String MULESOFT_PROPERTIES = "mulesoft.properties";

	static private Logger logger = LoggerFactory.getLogger(MuleProperties.class);

	static private Map<String, Properties> props = new HashMap<String, Properties>();

	private static Properties loadProp(String language, String propName) {
		Properties properties = new Properties();
		try (InputStream input = MuleProperties.class.getClassLoader().getResourceAsStream(propName)) {
			properties.load(input);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		props.put(language, properties);
		return properties;
	}

	public static Properties getProperties(String language) {
		if (props.containsKey(language)) {
			return props.get(language);
		} else {
			return loadProp(language, MULESOFT_PROPERTIES);
		}
	}
}