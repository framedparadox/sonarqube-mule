package com.sonar.plugins.mulesoft.metrics.model;

import java.util.ArrayList;

/**
 * Data model for MUnit test coverage information.
 * 
 * <p>This class stores coverage data extracted from MUnit JSON reports, supporting both:
 * <ul>
 *   <li><strong>Line-level coverage:</strong> Specific line numbers that are covered/uncovered</li>
 *   <li><strong>Processor-level coverage:</strong> Count of message processors and coverage ratio</li>
 * </ul>
 * 
 * <p><strong>Coverage Modes:</strong></p>
 * <ul>
 *   <li><strong>Line-based:</strong> When {@code hasLineNumbers() == true}, coverage is tracked per line number</li>
 *   <li><strong>Processor-based:</strong> When {@code hasLineNumbers() == false}, coverage is approximated by processor count</li>
 * </ul>
 * 
 * <h3>Usage in Coverage Import:</h3>
 * <pre>
 * CoverageData data = new CoverageData();
 * data.setHasLineNumbers(true);
 * data.getCoveredLines().add(15);
 * data.getCoveredLines().add(23);
 * data.getNotcoveredLines().add(33);
 * 
 * // Save to SonarQube
 * NewCoverage coverage = context.newCoverage().onFile(file);
 * for (Integer line : data.getCoveredLines()) {
 *   coverage.lineHits(line, 1);
 * }
 * coverage.save();
 * </pre>
 * 
 * @see com.sonar.plugins.mulesoft.sensors.metrics.CoverageImportSensor
 * @since 1.0.0
 */
public class CoverageData {

	/** Total number of message processors in flows */
	private int processors = 0;
	
	/** Number of message processors covered by tests */
	private int coveredProcessors = 0;
	
	/** Whether this coverage data includes line-level information */
	private boolean hasLineNumbers = false;
	
	 /** List of line numbers that are covered by tests */
	private ArrayList<Integer> coveredLines = new ArrayList<Integer>();
	
	/** List of line numbers that are not covered by tests */
	private ArrayList<Integer> notcoveredLines = new ArrayList<Integer>();

	/**
	 * Adds to the total processor count.
	 * 
	 * @param processors Number of processors to add
	 */
	public void addProcessors(int processors) {
		this.processors += processors;
	}

	/**
	 * Adds to the covered processor count.
	 * 
	 * @param processors Number of covered processors to add
	 */
	public void addCoveredProcessors(int processors) {
		this.coveredProcessors += processors;
	}

	/**
	 * Gets the total number of covered processors.
	 * 
	 * @return Number of message processors covered by tests
	 */
	public int getCoveredProcessors() {
		return coveredProcessors;
	}

	/**
	 * Gets the total number of processors.
	 * 
	 * @return Total number of message processors in flows
	 */
	public int getProcessors() {
		return processors;
	}

	public void setCoveredLines(ArrayList<Integer> coveredLines) {
		this.coveredLines = coveredLines;
	}

	/**
	 * Gets the list of covered line numbers.
	 * 
	 * <p>This list is populated only when {@code hasLineNumbers() == true}.
	 * Each integer represents a line number in the source file that is covered by tests.</p>
	 * 
	 * @return Mutable list of covered line numbers
	 */
	public ArrayList<Integer> getCoveredLines() {
		return coveredLines;
	}

	/**
	 * Gets the list of uncovered line numbers.
	 * 
	 * <p>This list is populated only when {@code hasLineNumbers() == true}.
	 * Each integer represents a line number in the source file that is not covered by tests.</p>
	 * 
	 * @return Mutable list of uncovered line numbers
	 */
	public ArrayList<Integer> getNotcoveredLines() {
		return notcoveredLines;
	}

	public void setNotcoveredLines(ArrayList<Integer> notcoveredLines) {
		this.notcoveredLines = notcoveredLines;
	}

	/**
	 * Sets whether this coverage data includes line-level information.
	 * 
	 * <p>When true, coverage is tracked per line number using {@link #getCoveredLines()}
	 * and {@link #getNotcoveredLines()}. When false, coverage is approximated using
	 * processor counts.</p>
	 * 
	 * @param hasLineNumbers true if line-level coverage is available
	 */
	public void setHasLineNumbers(boolean hasLineNumbers) {
		this.hasLineNumbers = hasLineNumbers;
	}

	/**
	 * Determines if line-level coverage information is available.
	 * 
	 * @return true if line numbers are tracked, false if only processor counts are available
	 */
	public boolean hasLineNumbers() {
		return hasLineNumbers;
	}
}
