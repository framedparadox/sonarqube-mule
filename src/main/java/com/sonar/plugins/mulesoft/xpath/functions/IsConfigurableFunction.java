package com.sonar.plugins.mulesoft.xpath.functions;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jaxen.Navigator;
import org.jaxen.SimpleFunctionContext;
import org.jaxen.XPathFunctionContext;
import org.jaxen.function.StringFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom XPath function for detecting MuleSoft property placeholder syntax.
 *
 * <p>This function identifies configuration values that use MuleSoft's property placeholder
 * pattern {@code ${property.name}}, which indicates configurable parameters that can be
 * externalized for different deployment environments.</p>
 *
 * <h3>Function Signature:</h3>
 * <pre>
 * is-configurable(value) → boolean
 * </pre>
 *
 * <h3>Detection Pattern:</h3>
 * <p>Recognizes the standard MuleSoft property placeholder format:
 * <ul>
 *   <li>Starts with {@code ${}</li>
 *   <li>Contains property name or expression</li>
 *   <li>Ends with {@code }}</li>
 *   <li>Pattern: {@code ^\$\{.*\}$}</li>
 * </ul>
 * </p>
 *
 * <h3>Usage in XPath Rules:</h3>
 * <pre>
 * // Find hardcoded values that should be configurable
 * //mule:http-request/@host[not(is-configurable(.))]
 * 
 * // Ensure database passwords are externalized
 * //db:config/@password[not(is-configurable(.))]
 * 
 * // Validate environment-specific endpoints are configurable
 * //mule:flow/mule:http-request/@url[not(is-configurable(.)) and contains(., 'prod')]  
 * 
 * // Count configurable vs hardcoded properties
 * count(//mule:property[@value[is-configurable(.)]]) vs count(//mule:property[@value[not(is-configurable(.))]])
 * </pre>
 *
 * <h3>Property Placeholder Examples:</h3>
 * <pre>
 * // Valid configurable values that return true:
 * ${database.host}              → true
 * ${api.timeout:5000}          → true (with default value)
 * ${env.type}                  → true
 * 
 * // Non-configurable values that return false:
 * localhost                    → false
 * https://api.example.com      → false
 * 8080                         → false
 * </pre>
 *
 * <h3>Quality Benefits:</h3>
 * <p>This function enables rules that promote:
 * <ul>
 *   <li><strong>Environment Portability:</strong> Detect hardcoded environment-specific values</li>
 *   <li><strong>Configuration Management:</strong> Ensure sensitive values are externalized</li>
 *   <li><strong>Deployment Flexibility:</strong> Validate proper use of property placeholders</li>
 *   <li><strong>Security Compliance:</strong> Flag hardcoded credentials or endpoints</li>
 * </ul>
 * </p>
 *
 * @see MatchesFunction for regex-based pattern matching
 * @see XPathProcessor for XPath evaluation context
 * @see Function for Jaxen function interface
 * @since 1.0.0
 * @author MuleSoft SonarQube Plugin Team
 */
public class IsConfigurableFunction implements Function {

	private static final Logger logger = LoggerFactory.getLogger(IsConfigurableFunction.class);

	private static final Pattern IS_CONFIGURABLE_PATTERN = Pattern.compile("\\$\\{[^}]+\\}");

	public static void registerSelfInSimpleContext() {
		// see http://jaxen.org/extensions.html
		((SimpleFunctionContext) XPathFunctionContext.getInstance()).registerFunction(null, "is-configurable",
				new IsConfigurableFunction());
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object call(Context context, List args) throws FunctionCallException {

		if (args.size() == 1) {
			Navigator navigator = (context != null && context.getContextSupport() != null) 
				? context.getNavigator() : null;
			return evaluate(args.get(0), navigator);
		}

		throw new FunctionCallException("is-configurable() requires 1 argument.");
	}

	public static Boolean evaluate(Object strArg, Navigator nav) {
		if (strArg instanceof List) {
			@SuppressWarnings("unchecked")
			List<Object> objectList = (List<Object>) strArg;
			// Check if it could load any attribute - If Empty then False
			if (objectList.isEmpty()) {
				return Boolean.FALSE;
			}

			for (Iterator<Object> iterator = objectList.iterator(); iterator.hasNext();) {
				Object object = iterator.next();
				String str;
				try {
					str = StringFunction.evaluate(object, nav);
				} catch (Exception e) {
					// Handle null navigator or other issues gracefully
					str = object != null ? object.toString() : "";
				}
				
				// Check if string contains any valid property placeholder
				if (containsValidPlaceholder(str)) {
					return Boolean.TRUE;
				}
			}
			return Boolean.FALSE;
		} else {
			String str;
			try {
				str = StringFunction.evaluate(strArg, nav);
			} catch (Exception e) {
				// Handle null navigator or other issues gracefully
				str = strArg != null ? strArg.toString() : "";
			}
			
			return containsValidPlaceholder(str) ? Boolean.TRUE : Boolean.FALSE;
		}
	}
	
	private static boolean containsValidPlaceholder(String str) {
		if (str == null || str.isEmpty()) {
			return false;
		}
		return IS_CONFIGURABLE_PATTERN.matcher(str).find();
	}

}
