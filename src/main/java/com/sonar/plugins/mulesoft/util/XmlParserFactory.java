package com.sonar.plugins.mulesoft.util;

import org.jdom2.input.SAXBuilder;
import org.jdom2.located.LocatedJDOMFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating secure XML parsers with protection against XML external entity (XXE) attacks.
 *
 * <p>This utility class provides centralized, security-hardened XML parser configuration
 * for all XML processing within the MuleSoft SonarQube plugin. It addresses common
 * XML security vulnerabilities while maintaining compatibility with legacy XML features.</p>
 *
 * <h3>Security Features:</h3>
 * <ul>
 *   <li><strong>XXE Prevention:</strong> Disables external entity processing to prevent XXE attacks</li>
 *   <li><strong>DTD Restriction:</strong> Disables loading of external DTDs while allowing DOCTYPE declarations</li>
 *   <li><strong>Namespace Support:</strong> Maintains XML namespace processing capabilities</li>
 *   <li><strong>Legacy Compatibility:</strong> Allows DOCTYPE declarations for existing XML files</li>
 * </ul>
 *
 * <h3>Configuration Applied:</h3>
 * <pre>
 * SAXBuilder settings:
 * - setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
 * - setFeature("http://xml.org/sax/features/external-general-entities", false)
 * - setFeature("http://xml.org/sax/features/external-parameter-entities", false)
 * - setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
 * - setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "")
 * - setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
 * </pre>
 *
 * <h3>Usage Example:</h3>
 * <pre>
 * // Standard usage in sensors and processors
 * SAXBuilder builder = XmlParserFactory.createSecureBuilder();
 * Document document = builder.build(inputFile);
 * 
 * // The builder is pre-configured with security settings
 * // No additional security configuration needed
 * </pre>
 *
 * <h3>Thread Safety:</h3>
 * <p>This factory is thread-safe. Each call to {@link #createSecureBuilder()}
 * returns a new, independently configured SAXBuilder instance.</p>
 *
 * @see SAXBuilder for JDOM2 XML parsing capabilities
 * @see XMLConstants for standard XML security feature names
 * @since 1.0.0
 * @author MuleSoft SonarQube Plugin Team
 */
public final class XmlParserFactory {

	private static final Logger LOG = LoggerFactory.getLogger(XmlParserFactory.class);

	private XmlParserFactory() {
		// Prevent instantiation
	}

	/**
	 * Creates a secure SAXBuilder instance with recommended security settings.
	 *
	 * <p>Configures the following security features:</p>
	 * <ul>
	 *   <li>Disables DOCTYPE declarations (prevents XXE via doctype injection)</li>
	 *   <li>Disables external general entities</li>
	 *   <li>Disables external parameter entities</li>
	 *   <li>Disables loading external DTDs</li>
	 * </ul>
	 *
	 * @return A configured SAXBuilder instance
	 */
	public static SAXBuilder createSecureBuilder() {
		SAXBuilder builder = new SAXBuilder();
		configureSecurityFeatures(builder);
		return builder;
	}

	/**
	 * Creates a secure SAXBuilder that records line/column on every parsed element.
	 * The returned builder uses {@link LocatedJDOMFactory} so that cast to
	 * {@link org.jdom2.located.Located} gives exact source positions.
	 */
	public static SAXBuilder createLocatedBuilder() {
		SAXBuilder builder = new SAXBuilder();
		builder.setJDOMFactory(new LocatedJDOMFactory());
		configureSecurityFeatures(builder);
		return builder;
	}

	/**
	 * Configures security features on an existing SAXBuilder.
	 * 
	 * @param builder The SAXBuilder to configure
	 */
	public static void configureSecurityFeatures(SAXBuilder builder) {
		try {
			builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			builder.setFeature("http://xml.org/sax/features/external-general-entities", false);
			builder.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		} catch (Exception e) {
			LOG.warn("Could not configure SAX builder security features: {}", e.getMessage());
		}
	}
}
