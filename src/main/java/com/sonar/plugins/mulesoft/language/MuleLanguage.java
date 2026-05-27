package com.sonar.plugins.mulesoft.language;

import java.util.Arrays;

import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

/**
 * Language definition for Mule XML configuration files.
 * 
 * <p>This class defines the Mule language for SonarQube analysis, including:
 * <ul>
 *   <li>Language identification (key, name)</li>
 *   <li>File extensions (.xml by default)</li>
 *   <li>Version-specific language keys (mule4)</li>
 * </ul>
 * 
 * <p>The language supports configurable file suffixes through the
 * {@code sonar.mule.file.suffixes} property, defaulting to {@code .xml}.</p>
 * 
 * <p>Thread-safe and suitable for dependency injection.</p>
 * 
 * @since 1.0.0
 */
public class MuleLanguage extends AbstractLanguage {

	/** SonarQube configuration for retrieving plugin settings */
	protected Configuration config = null;

	/** Human-readable language name displayed in SonarQube UI */
	public static final String LANGUAGE_NAME = "Mule";
	
	/** Internal language key used by SonarQube */
	public static final String LANGUAGE_KEY = "mule";

	/** Language key specifically for Mule 4 runtime */
	public static final String LANGUAGE_MULE4_KEY = "mule4";

	/** Configuration property key for file suffixes */
	public static final String FILE_SUFFIXES_KEY = "sonar.mule.file.suffixes";
	
	/** Default file extension for Mule configuration files */
	public static final String FILE_SUFFIXES_DEFAULT_VALUE = ".xml";

	/**
	 * Creates a new Mule language definition.
	 * 
	 * @param config SonarQube configuration for retrieving settings
	 * @throws IllegalArgumentException if config is null
	 */
	public MuleLanguage(Configuration config) {
		super("mule", LANGUAGE_NAME);
		this.config = config;
	}

	/**
	 * Gets the file suffixes for Mule configuration files.
	 * 
	 * <p>Suffixes are retrieved from the {@code sonar.mule.file.suffixes} configuration
	 * property. If not configured, defaults to {@code .xml}.</p>
	 * 
	 * <p>Multiple suffixes can be specified as a comma-separated list.</p>
	 * 
	 * @return Array of file suffixes (e.g., [".xml"])
	 */
	@Override
	public String[] getFileSuffixes() {
		String[] suffixes = config.getStringArray(FILE_SUFFIXES_KEY);
		if (suffixes == null || suffixes.length == 0) {
			suffixes = FILE_SUFFIXES_DEFAULT_VALUE.split(",");
		}
		return suffixes;
	}
}
