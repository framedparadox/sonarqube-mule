package com.sonar.plugins.mulesoft.sensors;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;

import com.sonar.plugins.mulesoft.filters.MuleFileFilter;
import com.sonar.plugins.mulesoft.language.MuleLanguage;

/**
 * Abstract base class for metric collection sensors in Mule applications.
 * 
 * <p>This class provides common functionality for sensors that extract metrics from
 * Mule XML configuration files. It handles:
 * <ul>
 *   <li>File filtering to process only Mule XML files</li>
 *   <li>Iteration over all matching files in the project</li>
 *   <li>Language detection and configuration</li>
 *   <li>Delegation to concrete sensor implementations</li>
 * </ul>
 * 
 * <h3>Usage Pattern:</h3>
 * <pre>
 * public class MyMetricSensor extends BaseMetricSensor {
 *   {@literal @}Override
 *   public void describe(SensorDescriptor descriptor) {
 *     descriptor.name("My Metric Sensor");
 *   }
 * 
 *   {@literal @}Override
 *   protected void process(SensorContext context, InputFile file, String language) {
 *     // Extract metrics from the file and save to context
 *   }
 * }
 * </pre>
 * 
 * <h3>File Processing:</h3>
 * <p>Only files that pass the MuleFileFilter are processed:
 * <ul>
 *   <li>Must have configured file extension (default: .xml)</li>
 *   <li>Must contain Mule namespace declaration</li>
 *   <li>Must be readable and parseable as XML</li>
 * </ul>
 * 
 * @see com.sonar.plugins.mulesoft.filters.MuleFileFilter
 * @see com.sonar.plugins.mulesoft.sensors.metrics
 * @since 1.0.0
 */
public abstract class BaseMetricSensor implements Sensor {

	/**
	 * Executes metric collection across all Mule files in the project.
	 * 
	 * <p>This method orchestrates the metric collection process:
	 * <ol>
	 *   <li>Discovers all files matching Mule file criteria</li>
	 *   <li>Determines the target Mule language version</li>
	 *   <li>Delegates processing to concrete sensor implementations</li>
	 * </ol>
	 * 
	 * <p>Concrete sensors should focus on metric extraction logic in the
	 * {@link #process(SensorContext, InputFile, String)} method.</p>
	 * 
	 * @param context SonarQube sensor context for metric storage and configuration
	 */
	@Override
	public void execute(SensorContext context) {

		FileSystem fs = context.fileSystem();
		// Filter to process only Mule configuration files
		Iterable<InputFile> files = fs.inputFiles(new MuleFileFilter(new MuleLanguage(context.config()).getFileSuffixes()));
		for (InputFile file : files) {
			process(context, file, MuleLanguage.LANGUAGE_KEY);
		}
	}

	/**
	 * Processes a single Mule configuration file to extract metrics.
	 * 
	 * <p>Concrete implementations should:
	 * <ul>
	 *   <li>Parse the XML file (use XmlParserFactory for security)</li>
	 *   <li>Extract relevant metrics using XPath or DOM traversal</li>
	 *   <li>Save metrics to the SensorContext using context.newMeasure()</li>
	 *   <li>Handle parsing errors gracefully with appropriate logging</li>
	 * </ul>
	 * 
	 * @param context SonarQube context for saving metrics and accessing configuration
	 * @param file Input file to process (guaranteed to be a Mule XML file)
	 * @param language Mule language identifier for property lookup
	 */
	protected abstract void process(SensorContext context, InputFile file, String language);

}
