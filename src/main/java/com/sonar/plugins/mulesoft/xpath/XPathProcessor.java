package com.sonar.plugins.mulesoft.xpath;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.scanner.ScannerSide;

import com.sonar.plugins.mulesoft.xpath.functions.ChildCountFunction;
import com.sonar.plugins.mulesoft.xpath.functions.DistinctCountFunction;
import com.sonar.plugins.mulesoft.xpath.functions.IsConfigurableFunction;
import com.sonar.plugins.mulesoft.xpath.functions.MatchesFunction;
import com.sonar.plugins.mulesoft.xpath.functions.PropPlaceholderFunction;
import com.sonar.plugins.mulesoft.xpath.functions.StartsWithAnyFunction;

/**
 * Advanced XPath processor for evaluating XPath expressions against Mule XML documents.
 *
 * <p>This processor provides namespace-aware XPath evaluation with custom function support
 * specifically designed for MuleSoft configuration analysis. It handles complex XML namespaces
 * and provides extension functions for sophisticated rule validation.</p>
 *
 * <h3>Core Features:</h3>
 * <ul>
 *   <li><strong>Namespace Management:</strong> Automatic loading of Mule namespace mappings from properties</li>
 *   <li><strong>Custom Functions:</strong> Built-in support for {@code matches()} and {@code is-configurable()} functions</li>
 *   <li><strong>Thread Safety:</strong> Designed for concurrent use across multiple sensor threads</li>
 *   <li><strong>Type Safety:</strong> Generic evaluation methods with automatic type conversion</li>
 *   <li><strong>Integration Ready:</strong> Marked with {@code @ScannerSide} for SonarQube injection</li>
 * </ul>
 *
 * <h3>Namespace Configuration:</h3>
 * <p>Namespaces are loaded from properties files (default: {@code mulesoft-namespace.properties}):
 * <pre>
 * # Example namespace configuration
 * mule=http://www.mulesoft.org/schema/mule/core
 * dw=http://www.mulesoft.org/schema/mule/ee/dw  
 * munit=http://www.mulesoft.org/schema/mule/munit
 * http=http://www.mulesoft.org/schema/mule/http
 * </pre>
 * </p>
 *
 * <h3>Custom XPath Functions:</h3>
 * <ul>
 *   <li><strong>matches(string, regex):</strong> Pattern matching with regular expressions</li>
 *   <li><strong>is-configurable(value):</strong> Detects property placeholder syntax {@code ${...}}</li>
 * </ul>
 *
 * <h3>Usage Examples:</h3>
 * <pre>
 * // Basic XPath evaluation
 * XPathProcessor processor = new XPathProcessor();
 * List&lt;Element&gt; flows = processor.evaluateAsList("//mule:flow", rootElement, Element.class);
 * 
 * // Counting with custom functions
 * Double count = processor.evaluate(
 *     "count(//mule:set-property[@propertyName[matches(., 'Content-.*')]])",
 *     rootElement, Double.class
 * );
 * 
 * // Configuration validation
 * Boolean hasConfigurable = processor.evaluate(
 *     "//mule:property/@value[is-configurable(.)]",
 *     rootElement, Boolean.class
 * );
 * </pre>
 *
 * <h3>Error Handling:</h3>
 * <p>XPath evaluation errors are logged and handled gracefully, typically returning
 * empty results or default values rather than throwing exceptions to calling code.</p>
 *
 * @see MatchesFunction for regex-based pattern matching in XPath
 * @see IsConfigurableFunction for property placeholder detection
 * @see MuleValidationSensor for primary usage context
 * @since 1.0.0
 * @author MuleSoft SonarQube Plugin Team
 */
@ScannerSide
public class XPathProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(XPathProcessor.class);
	private static final XPathFactory XPATH_FACTORY = XPathFactory.instance();

	private final List<Namespace> namespaces;

	/**
	 * Creates XPath processor with default Mule namespaces.
	 */
	public XPathProcessor() {
		this("mulesoft-namespace.properties");
	}

	/**
	 * Creates XPath processor with namespaces loaded from specified resource.
	 *
	 * @param namespaceResource Properties file containing namespace mappings
	 */
	public XPathProcessor(String namespaceResource) {
		this.namespaces = loadNamespaces(namespaceResource);
		// Register custom XPath functions once per class
		registerCustomFunctions();
	}

	/**
	 * Creates XPath processor with provided namespaces.
	 *
	 * @param namespaces List of XML namespaces for XPath evaluation
	 */
	public XPathProcessor(List<Namespace> namespaces) {
		this.namespaces = new ArrayList<>(namespaces);
		registerCustomFunctions();
	}

	/**
	 * Registers custom XPath functions (matches, is-configurable).
	 * Called once during initialization.
	 */
	private static synchronized void registerCustomFunctions() {
		// Register custom functions only once
		try {
			MatchesFunction.registerSelfInSimpleContext();
			IsConfigurableFunction.registerSelfInSimpleContext();
			org.jaxen.SimpleFunctionContext functionContext =
				(org.jaxen.SimpleFunctionContext) org.jaxen.XPathFunctionContext.getInstance();
			functionContext.registerFunction(null, StartsWithAnyFunction.NAME, new StartsWithAnyFunction());
			functionContext.registerFunction(null, PropPlaceholderFunction.NAME, new PropPlaceholderFunction());
			functionContext.registerFunction(null, DistinctCountFunction.NAME, new DistinctCountFunction());
			functionContext.registerFunction(null, ChildCountFunction.NAME, new ChildCountFunction());
		} catch (Exception e) {
			LOG.warn("Custom XPath functions may already be registered: {}", e.getMessage());
		}
	}

	/**
	 * Loads namespace mappings from a properties file.
	 *
	 * @param resourceName Properties file in classpath
	 * @return List of Namespace objects
	 */
	private List<Namespace> loadNamespaces(String resourceName) {
		List<Namespace> loadedNamespaces = new ArrayList<>();
		Properties properties = new Properties();

		try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
			if (stream == null) {
				LOG.warn("Namespace resource not found: {}", resourceName);
				return Collections.emptyList();
			}

			properties.load(stream);
			properties.forEach((prefix, uri) ->
				loadedNamespaces.add(Namespace.getNamespace(prefix.toString(), uri.toString()))
			);

			LOG.debug("Loaded {} namespaces from {}", loadedNamespaces.size(), resourceName);
		} catch (IOException e) {
			LOG.error("Failed to load namespaces from {}: {}", resourceName, e.getMessage(), e);
		}

		return loadedNamespaces;
	}

	/**
	 * Evaluates XPath expression against a target element.
	 *
	 * @param xpathExpression XPath expression string
	 * @param target Target element to evaluate against
	 * @param type Expected return type
	 * @return Evaluation result cast to specified type
	 */
	@SuppressWarnings("unchecked")
	public <T> T evaluate(String xpathExpression, Content target, Class<T> type) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Evaluating XPath: {}", xpathExpression);
		}

		try {
			@SuppressWarnings("rawtypes")
			XPathExpression xp = XPATH_FACTORY.compile(
				xpathExpression,
				Filters.fpassthrough(),
				Collections.emptyMap(),
				namespaces
			);

			return (T) xp.evaluateFirst(target);
		} catch (Exception e) {
			LOG.error("XPath evaluation failed for '{}': {}", xpathExpression, e.getMessage());
			// Return null or default value based on type
			return getDefaultValue(type);
		}
	}

	/**
	 * Evaluates XPath expression and returns all matching elements.
	 *
	 * @param xpathExpression XPath expression string
	 * @param target Target element to evaluate against
	 * @return List of matching elements (empty if no matches or error)
	 */
	public List<Element> evaluateList(String xpathExpression, Content target) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Evaluating XPath for list: {}", xpathExpression);
		}

		try {
			XPathExpression<Element> xp = XPATH_FACTORY.compile(
				xpathExpression,
				Filters.element(),
				Collections.emptyMap(),
				namespaces
			);

			return xp.evaluate(target);
		} catch (Exception e) {
			LOG.error("XPath list evaluation failed for '{}': {}", xpathExpression, e.getMessage());
			return Collections.emptyList();
		}
	}

	/**
	 * Returns default value for a type when evaluation fails.
	 */
	@SuppressWarnings("unchecked")
	private <T> T getDefaultValue(Class<T> type) {
		if (type == Boolean.class || type == boolean.class) {
			return (T) Boolean.FALSE;
		}
		return null;
	}

	/**
	 * Returns the configured namespaces.
	 *
	 * @return Unmodifiable list of namespaces
	 */
	public List<Namespace> getNamespaces() {
		return Collections.unmodifiableList(namespaces);
	}
}