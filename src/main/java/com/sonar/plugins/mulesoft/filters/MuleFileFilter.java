package com.sonar.plugins.mulesoft.filters;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;

import com.sonar.plugins.mulesoft.util.XmlParserFactory;

/**
 * File predicate for identifying and filtering Mule XML configuration files.
 *
 * <p>This filter implements SonarQube's {@link FilePredicate} interface to determine
 * which files should be analyzed by Mule-specific sensors. It performs dual validation:
 * file extension matching and Mule namespace verification.</p>
 *
 * <h3>Filtering Criteria:</h3>
 * <ul>
 *   <li><strong>Extension Check:</strong> File must have a configured extension (typically {@code .xml})</li>
 *   <li><strong>Namespace Validation:</strong> XML root element must declare the Mule core namespace</li>
 *   <li><strong>Parse Validation:</strong> File must be well-formed XML</li>
 * </ul>
 *
 * <h3>Mule Namespace Detection:</h3>
 * <p>The filter specifically looks for the Mule core namespace:
 * <pre>
 * http://www.mulesoft.org/schema/mule/core
 * </pre>
 * This ensures only genuine Mule configuration files are processed, excluding
 * other XML files that may exist in the project.
 * </p>
 *
 * <h3>Usage in Sensors:</h3>
 * <pre>
 * // Typical usage in BaseMetricSensor
 * FilePredicates p = context.fileSystem().predicates();
 * FilePredicate muleFilter = new MuleFileFilter(language.getFileSuffixes());
 * 
 * for (InputFile file : context.fileSystem().inputFiles(muleFilter)) {
 *     // Process Mule XML file
 * }
 * </pre>
 *
 * <h3>File Extension Configuration:</h3>
 * <p>File extensions are typically configured through {@link MuleLanguage#getFileSuffixes()}
 * and can include multiple patterns like {@code [".xml", ".mule"]}.</p>
 *
 * <h3>Error Handling:</h3>
 * <p>Files that fail XML parsing are logged as warnings and excluded from analysis.
 * This prevents malformed XML files from breaking the analysis pipeline.</p>
 *
 * @see FilePredicate for SonarQube file filtering interface
 * @see MuleLanguage for file extension configuration
 * @see BaseMetricSensor for typical usage context
 * @since 1.0.0
 * @author MuleSoft SonarQube Plugin Team
 */
public class MuleFileFilter implements FilePredicate {

	private static final Logger logger = LoggerFactory.getLogger(MuleFileFilter.class);
	SAXBuilder saxBuilder = XmlParserFactory.createSecureBuilder();
	String muleNamespace = "http://www.mulesoft.org/schema/mule/core";
	String[] fileExtensions;
	/**
	 * Creates a file filter for Mule XML files.
	 * @param aFileSuffixes Array of file suffixes to filter (e.g., ".xml")
	 */
	public MuleFileFilter(String[] aFileSuffixes) {
		super();
		fileExtensions = aFileSuffixes;
	}

	@Override
	public boolean apply(InputFile inputFile) {
		logger.debug("Executing Mule Sensor on file: {}", inputFile.filename());

		for (String fileExtension : fileExtensions) {
			if (inputFile.filename().endsWith(fileExtension)) {
				try (InputStream is = inputFile.inputStream()) {
					Document document = saxBuilder.build(is);
					String namespace = document.getRootElement().getNamespaceURI();
					if (muleNamespace.equals(namespace))
						return true;
				} catch (JDOMException | IOException e) {
					logger.error("Parsing document: {}", inputFile.filename(), e);
				}
			}
		}
		return false;
	}
}
