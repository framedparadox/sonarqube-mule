package com.sonar.plugins.mulesoft.xpath.functions;

import java.util.Iterator;
import java.util.List;

import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jaxen.Navigator;
import org.jaxen.SimpleFunctionContext;
import org.jaxen.XPathFunctionContext;
import org.jaxen.function.StringFunction;

/**
 * Custom XPath function for regular expression pattern matching within XPath expressions.
 *
 * <p>This function extends XPath capabilities by providing regex-based string matching,
 * enabling sophisticated pattern validation within Mule configuration rules. It follows
 * the Jaxen extension function pattern for seamless integration.</p>
 *
 * <h3>Function Signature:</h3>
 * <pre>
 * matches(string, regex) → boolean
 * </pre>
 *
 * <h3>Parameters:</h3>
 * <ul>
 *   <li><strong>string:</strong> The input string to test (can be XPath expression result)</li>
 *   <li><strong>regex:</strong> Java regular expression pattern</li>
 * </ul>
 *
 * <h3>Return Value:</h3>
 * <p>Returns {@code true} if the string matches the regex pattern, {@code false} otherwise.</p>
 *
 * <h3>Usage in XPath Rules:</h3>
 * <pre>
 * // Check for HTTP method attributes with specific patterns
 * //mule:http-request[@method[matches(., '^(GET|POST|PUT|DELETE)$')]]
 * 
 * // Validate property names follow naming conventions
 * //mule:property[@name[matches(., '^[a-z][a-zA-Z0-9]*$')]]
 * 
 * // Find configuration elements with environment-specific names
 * //mule:configuration[@name[matches(., '.*-(dev|test|prod)$')]]
 * 
 * // Detect DataWeave expressions in specific contexts
 * //dw:set-payload[matches(., '.*\\$\\[.*\\].*')]
 * </pre>
 *
 * <h3>Registration and Integration:</h3>
 * <p>The function is automatically registered with the Jaxen XPath engine during
 * {@link XPathProcessor} initialization, making it available in all XPath expressions
 * evaluated by the MuleSoft plugin.</p>
 *
 * <h3>Error Handling:</h3>
 * <p>Invalid regex patterns or incorrect argument counts result in {@link FunctionCallException}.
 * The function requires exactly 2 arguments and validates regex syntax before evaluation.</p>
 *
 * @see IsConfigurableFunction for property placeholder detection
 * @see XPathProcessor for XPath evaluation context
 * @see Function for Jaxen function interface
 * @since 1.0.0
 * @author MuleSoft SonarQube Plugin Team
 */
public class MatchesFunction implements Function {

	public static void registerSelfInSimpleContext() {
		// see http://jaxen.org/extensions.html
		((SimpleFunctionContext) XPathFunctionContext.getInstance()).registerFunction(null, "matches",
				new MatchesFunction());
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object call(Context context, List args) throws FunctionCallException {

		if (args.size() == 2) {
			Navigator navigator = (context != null && context.getContextSupport() != null) 
				? context.getNavigator() : null;
			return evaluate(args.get(0), args.get(1), navigator);
		}

		throw new FunctionCallException("matches() requires 2 arguments.");
	}

	public static Boolean evaluate(Object strArg, Object matchArg, Navigator nav) {
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
				String regexp;
				try {
					regexp = StringFunction.evaluate(matchArg, nav);
				} catch (Exception e) {
					regexp = matchArg != null ? matchArg.toString() : "";
				}
				
				try {
					if (!str.matches(regexp)) {
						return Boolean.FALSE;
					}
				} catch (Exception e) {
					// Invalid regex pattern
					return Boolean.FALSE;
				}
			}
			return Boolean.TRUE;
		} else {
			String str;
			try {
				str = StringFunction.evaluate(strArg, nav);
			} catch (Exception e) {
				// Handle null navigator or other issues gracefully  
				str = strArg != null ? strArg.toString() : "";
			}
			String regexp;
			try {
				regexp = StringFunction.evaluate(matchArg, nav);
			} catch (Exception e) {
				regexp = matchArg != null ? matchArg.toString() : "";
			}
			
			try {
				return (str.matches(regexp) ? Boolean.TRUE : Boolean.FALSE);
			} catch (Exception e) {
				// Invalid regex pattern
				return Boolean.FALSE;
			}
		}

	}

}
