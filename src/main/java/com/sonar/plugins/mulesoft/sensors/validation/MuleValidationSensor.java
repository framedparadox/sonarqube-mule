package com.sonar.plugins.mulesoft.sensors.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.located.Located;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

import java.util.Arrays;
import java.util.Optional;

import com.sonar.plugins.mulesoft.check.DefaultMuleCheckContext;
import com.sonar.plugins.mulesoft.check.MuleCheck;
import com.sonar.plugins.mulesoft.check.MuleCheckContext;
import com.sonar.plugins.mulesoft.check.MuleCheckList;
import com.sonar.plugins.mulesoft.config.MulePluginProperties;
import com.sonar.plugins.mulesoft.filters.MuleFileFilter;
import com.sonar.plugins.mulesoft.language.MuleLanguage;
import com.sonar.plugins.mulesoft.parse.ParsedDocumentCache;
import com.sonar.plugins.mulesoft.rule.MuleRulesDefinition;
import com.sonar.plugins.mulesoft.xpath.XPathProcessor;
import com.sonar.plugins.mulesoft.util.XmlParserFactory;

/**
 * Primary validation sensor for analyzing Mule XML configuration files against custom rules.
 *
 * <p>This sensor processes all Mule XML files in the project and applies validation rules
 * defined in the ruleset configuration. It supports both file-scope and application-scope
 * rule evaluation with precise issue location reporting.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Single Parse Strategy:</strong> Each file is parsed once and cached for multiple rule evaluations</li>
 *   <li><strong>Immediate Issue Reporting:</strong> Issues are reported as soon as they're detected, not batched</li>
 *   <li><strong>Precise Location Tracking:</strong> Uses XPath location hints for accurate line number reporting</li>
 *   <li><strong>Dual Scope Support:</strong> Handles both file-level and application-level rule validation</li>
 *   <li><strong>Security-Hardened XML Parsing:</strong> Uses secure SAXBuilder configuration via {@link XmlParserFactory}</li>
 * </ul>
 *
 * <h3>Rule Evaluation Logic:</h3>
 * <ul>
 *   <li><strong>File-Scope Rules:</strong> Evaluated against each individual file. Failures reported per file.</li>
 *   <li><strong>Application-Scope Rules:</strong> Evaluated across all project files. Issue reported only if
 *       ALL files fail the rule (if any file passes, the rule is satisfied).</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>
 * // File-scope rule in mulesoft-ruleset.yaml:
 * - key: "avoid-dataweave-in-choice"
 *   name: "Avoid DataWeave in Choice Router"
 *   xpath: "//mule:choice//dw:transform"
 *   scope: "FILE"
 *   message: "DataWeave transformation should not be used directly in choice router"
 *
 * // Application-scope rule:
 * - key: "require-global-error-handler" 
 *   name: "Global Error Handler Required"
 *   xpath: "//mule:error-handler[@name='globalErrorHandler']"
 *   scope: "APPLICATION"
 *   message: "Application must define a global error handler"
 * </pre>
 *
 * <p>The sensor integrates with SonarQube's issue tracking system and respects the active
 * rule profile configuration. Only rules that are active in the quality profile will be
 * evaluated during analysis.</p>
 *
 * @see MuleRulesDefinition for rule definition and loading
 * @see XPathProcessor for XPath evaluation with custom functions
 * @see BaseMetricSensor for common sensor functionality
 * @since 1.0.0
 * @author MuleSoft SonarQube Plugin Team
 */
public class MuleValidationSensor implements Sensor {

	private static final Logger LOG = LoggerFactory.getLogger(MuleValidationSensor.class);

	private final SAXBuilder saxBuilder;
	private final XPathProcessor xpathProcessor;
	private final ParsedDocumentCache cache;

	/**
	 * Creates sensor with default configuration.
	 */
	public MuleValidationSensor() {
		this(XmlParserFactory.createLocatedBuilder(), new XPathProcessor());
	}

	/**
	 * Creates sensor with provided dependencies (for testing).
	 *
	 * @param saxBuilder SAX builder for XML parsing
	 * @param xpathProcessor XPath processor for rule evaluation
	 */
	public MuleValidationSensor(SAXBuilder saxBuilder, XPathProcessor xpathProcessor) {
		this(saxBuilder, xpathProcessor, new ParsedDocumentCache(saxBuilder));
	}

	/**
	 * Creates sensor with fully injected dependencies (for testing).
	 *
	 * @param saxBuilder SAX builder for XML parsing
	 * @param xpathProcessor XPath processor for rule evaluation
	 * @param cache pre-built document cache
	 */
	MuleValidationSensor(SAXBuilder saxBuilder, XPathProcessor xpathProcessor, ParsedDocumentCache cache) {
		this.saxBuilder = saxBuilder;
		this.xpathProcessor = xpathProcessor;
		this.cache = cache;
	}

	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.name("Mule XML Analyzer")
				.onlyOnLanguage(MuleLanguage.LANGUAGE_KEY)
				.createIssuesForRuleRepositories(MuleRulesDefinition.MULE_REPOSITORY_KEY);
	}

	@Override
	public void execute(SensorContext context) {
		LOG.info("Executing Mule sensor analysis");

		FileSystem fs = context.fileSystem();
		FilePredicates predicates = fs.predicates();

		MuleLanguage language = new MuleLanguage(context.config());

		// Get all active Mule rules
		Collection<ActiveRule> allActiveRules = context.activeRules()
				.findByRepository(MuleRulesDefinition.MULE_REPOSITORY_KEY);

		// Apply optional category filter (sonar.mule.ruleset.categories)
		Collection<ActiveRule> activeRules = filterByCategory(context, allActiveRules);

		if (activeRules.isEmpty()) {
			LOG.warn("No active rules found for Mule language. No analysis will be performed.");
			LOG.warn("Ensure mulesoft-ruleset.yaml or mulesoft-ruleset.xml exists in extensions/plugins/");
			return;
		}

		LOG.info("Analyzing with {} active rules", activeRules.size());

		// Get all Mule XML files
		Iterable<InputFile> muleFiles = fs.inputFiles(
			predicates.and(
				predicates.hasLanguage(MuleLanguage.LANGUAGE_KEY),
				new MuleFileFilter(language.getFileSuffixes())
			)
		);

		// Separate file-scope and application-scope rules
		List<ActiveRule> fileRules = new ArrayList<>();
		List<ActiveRule> applicationRules = new ArrayList<>();

		for (ActiveRule rule : activeRules) {
			String scope = rule.param(MuleRulesDefinition.PARAMS.SCOPE);
			if ("application".equalsIgnoreCase(scope)) {
				applicationRules.add(rule);
			} else {
				fileRules.add(rule); // Default to file scope
			}
		}

		int fileCount = 0;
		int fileIssueCount = 0;

		// PHASE 1: Analyze file-scope rules (immediate reporting)
		if (!fileRules.isEmpty()) {
			LOG.info("Phase 1: Analyzing {} file-scope rules", fileRules.size());
			for (InputFile file : muleFiles) {
				fileCount++;
				fileIssueCount += analyzeFileScope(context, file, fileRules);
			}
		}

		// PHASE 2: Analyze application-scope rules (deferred reporting)
		int appIssueCount = 0;
		if (!applicationRules.isEmpty()) {
			LOG.info("Phase 2: Analyzing {} application-scope rules across {} files",
					applicationRules.size(), fileCount);
			appIssueCount = analyzeApplicationScope(context, muleFiles, applicationRules);
		}

		LOG.info("Mule sensor analysis complete: {} files, {} file-scope issues, {} application-scope issues",
				fileCount, fileIssueCount, appIssueCount);

		// PHASE 3: Dispatch Java-based MuleCheck implementations
		List<MuleCheck> registeredChecks = MuleCheckList.allChecks();
		if (!registeredChecks.isEmpty()) {
			LOG.info("Phase 3: Running {} Java-based MuleCheck rules", registeredChecks.size());

			List<Document> allDocs = new ArrayList<>();
			List<InputFile> fileList = new ArrayList<>();

			for (InputFile file : fs.inputFiles(
					predicates.and(
						predicates.hasLanguage(MuleLanguage.LANGUAGE_KEY),
						new MuleFileFilter(language.getFileSuffixes())))) {
				fileList.add(file);
				cache.get(file).ifPresent(allDocs::add);
			}

			InputFile anchorFile = fileList.isEmpty() ? null : fileList.get(0);

			for (MuleCheck check : registeredChecks) {
				ActiveRule activeRule = context.activeRules()
					.find(org.sonar.api.rule.RuleKey.of(
						MuleRulesDefinition.MULE_REPOSITORY_KEY, check.ruleKey()));
				if (activeRule == null) {
					LOG.debug("MuleCheck {} is not active — skipping", check.ruleKey());
					continue;
				}

				com.sonar.plugins.mulesoft.util.RuleScope scope = check.scope();
				if (com.sonar.plugins.mulesoft.util.RuleScope.FILE_SCOPE.equals(scope)) {
					for (InputFile file : fileList) {
						Optional<Document> doc = cache.get(file);
						if (doc.isPresent()) {
							MuleCheckContext ctx = new DefaultMuleCheckContext(
								context, activeRule, file, doc.get(), allDocs,
								java.util.Collections.emptyMap(), anchorFile);
							try {
								check.check(ctx);
							} catch (Exception e) {
								LOG.warn("MuleCheck {} failed on {}: {}", check.ruleKey(), file.filename(), e.getMessage());
							}
						}
					}
				} else {
					MuleCheckContext ctx = new DefaultMuleCheckContext(
						context, activeRule, anchorFile, null, allDocs,
						java.util.Collections.emptyMap(), anchorFile);
					try {
						check.check(ctx);
					} catch (Exception e) {
						LOG.warn("MuleCheck {} failed: {}", check.ruleKey(), e.getMessage());
					}
				}
			}
		}
	}

	/**
	 * Filters active rules by category if {@code sonar.mule.ruleset.categories} is configured.
	 *
	 * <p>The property holds a comma-separated list of allowed categories
	 * (e.g. {@code flows,configuration}). If the property is empty or absent,
	 * all rules are returned unfiltered.</p>
	 *
	 * @param context    Sensor context for reading configuration
	 * @param activeRules All active rules for the repository
	 * @return Filtered (or unchanged) collection of active rules
	 */
	private Collection<ActiveRule> filterByCategory(SensorContext context, Collection<ActiveRule> activeRules) {
		Optional<String> categoriesValue = context.config().get(MulePluginProperties.RULESET_CATEGORIES_KEY);
		if (!categoriesValue.isPresent() || categoriesValue.get().trim().isEmpty()) {
			return activeRules;
		}

		List<String> allowedCategories = Arrays.asList(categoriesValue.get().split(","));
		LOG.info("Filtering rules to categories: {}", allowedCategories);

		List<ActiveRule> filtered = new ArrayList<>();
		for (ActiveRule rule : activeRules) {
			String category = rule.param(MuleRulesDefinition.PARAMS.CATEGORY);
			if (category != null && allowedCategories.contains(category.trim())) {
				filtered.add(rule);
			}
		}
		LOG.info("Category filter: {} rules → {} rules", activeRules.size(), filtered.size());
		return filtered;
	}

	/**
	 * Analyzes a single file against file-scope rules.
	 * Issues are reported immediately (not batch collected).
	 *
	 * @param context Sensor context
	 * @param file File to analyze
	 * @param fileRules File-scope validation rules
	 * @return Number of issues found in this file
	 */
	private int analyzeFileScope(SensorContext context, InputFile file, List<ActiveRule> fileRules) {
		LOG.debug("Analyzing file: {}", file.filename());

		Optional<Document> cachedDoc = cache.get(file);
		if (cachedDoc.isEmpty()) {
			LOG.warn("Skipping file {} — XML could not be parsed", file.filename());
			return 0;
		}
		Document document = cachedDoc.get();
		Element rootElement = document.getRootElement();

		int issuesFound = 0;
		for (ActiveRule rule : fileRules) {
			if (evaluateRule(context, file, document, rootElement, rule)) {
				issuesFound++;
			}
		}
		return issuesFound;
	}

	/**
	 * Analyzes all files against application-scope rules.
	 * Application-scope: If ANY file passes the rule, no issue is reported.
	 * Only if ALL files fail is an issue reported on the first failing file.
	 *
	 * @param context Sensor context
	 * @param files All Mule files
	 * @param applicationRules Application-scope validation rules
	 * @return Number of application-scope issues reported
	 */
	private int analyzeApplicationScope(SensorContext context, Iterable<InputFile> files,
										List<ActiveRule> applicationRules) {
		int issuesReported = 0;

		// Track rule evaluation results across all files
		// Map: ruleKey -> (passedAnyFile, firstFailingFile, firstFailingDocument)
		Map<String, ApplicationRuleState> ruleStates = new HashMap<>();

		// Initialize tracking for each application rule
		for (ActiveRule rule : applicationRules) {
			ruleStates.put(rule.ruleKey().toString(), new ApplicationRuleState());
		}

		// Evaluate each file against all application rules
		for (InputFile file : files) {
			Optional<Document> cachedDoc = cache.get(file);
			if (cachedDoc.isEmpty()) {
				LOG.debug("Skipping {} in application-scope analysis — could not be parsed", file.filename());
				continue;
			}
			Document document = cachedDoc.get();
			Element rootElement = document.getRootElement();

			for (ActiveRule rule : applicationRules) {
				String ruleKey = rule.ruleKey().toString();
				ApplicationRuleState state = ruleStates.get(ruleKey);

				if (state.passedAnyFile) {
					continue;
				}

				String xpathExpression = rule.param(MuleRulesDefinition.PARAMS.XPATH);
				if (xpathExpression == null || xpathExpression.trim().isEmpty()) {
					continue;
				}

				String finalXpath = substituteParameters(xpathExpression.trim(), resolveUserParams(rule));
				Boolean isValid = xpathProcessor.evaluate(finalXpath, rootElement, Boolean.class);

				if (Boolean.TRUE.equals(isValid)) {
					state.passedAnyFile = true;
					LOG.debug("Application rule {} PASSED on file {}", ruleKey, file.filename());
				} else if (state.firstFailingFile == null) {
					state.firstFailingFile = file;
					state.firstFailingDocument = document;
					LOG.debug("Application rule {} FAILED on file {} (first failure)", ruleKey, file.filename());
				}
			}
		}

		// Report issues for rules that failed on ALL files
		for (ActiveRule rule : applicationRules) {
			String ruleKey = rule.ruleKey().toString();
			ApplicationRuleState state = ruleStates.get(ruleKey);

			if (!state.passedAnyFile && state.firstFailingFile != null) {
				// Rule failed on all files - report issue on first failing file
				reportIssue(context, state.firstFailingFile, state.firstFailingDocument, rule);
				issuesReported++;
				LOG.info("Application rule {} failed on ALL files, reported issue on {}",
						ruleKey, state.firstFailingFile.filename());
			} else if (state.passedAnyFile) {
				LOG.debug("Application rule {} passed on at least one file - no issue", ruleKey);
			}
		}

		return issuesReported;
	}

	/**
	 * Tracks the state of an application-scope rule evaluation across files.
	 */
	private static class ApplicationRuleState {
		boolean passedAnyFile = false;
		InputFile firstFailingFile = null;
		Document firstFailingDocument = null;
	}

	/**
	 * Evaluates a single rule against a parsed document.
	 * Reports issue immediately if rule is violated.
	 *
	 * @param context Sensor context
	 * @param file Input file
	 * @param document Parsed XML document
	 * @param rootElement Root element of document
	 * @param rule Rule to evaluate
	 * @return true if issue was reported
	 */
	private boolean evaluateRule(SensorContext context, InputFile file, Document document,
								 Element rootElement, ActiveRule rule) {
		try {
			String xpathExpression = rule.param(MuleRulesDefinition.PARAMS.XPATH);
			if (xpathExpression == null || xpathExpression.trim().isEmpty()) {
				LOG.warn("Rule {} has no XPath expression", rule.ruleKey());
				return false;
			}

			// Substitute user-defined parameters into the XPath expression
			String finalXpath = substituteParameters(xpathExpression.trim(), resolveUserParams(rule));

			// Evaluate XPath
			Boolean isValid = xpathProcessor.evaluate(finalXpath, rootElement, Boolean.class);

			if (isValid == null) {
				isValid = false; // Treat null as invalid
			}

			LOG.debug("Rule {} validation result: {} for file {}",
					rule.ruleKey(), isValid, file.filename());

			// Report issue immediately if rule is violated
			if (!isValid) {
				reportIssue(context, file, document, rule);
				return true;
			}

		} catch (Exception e) {
			LOG.warn("Failed to evaluate rule {} for file {}: {}",
					rule.ruleKey(), file.filename(), e.getMessage());
		}

		return false;
	}

	/**
	 * Reports an issue for a rule violation.
	 * Uses XPath location hint if available to pinpoint exact location.
	 *
	 * @param context Sensor context
	 * @param file Input file
	 * @param document Parsed XML document
	 * @param rule Violated rule
	 */
	/**
	 * Reports issues for a rule violation.
	 *
	 * <p>When a locationHint XPath is present, it is evaluated against the same
	 * already-parsed JDOM2 document (which was built with {@link org.jdom2.located.LocatedJDOMFactory}),
	 * yielding one {@link org.jdom2.located.Located} element per violating node.  A separate
	 * SonarQube issue is created for each such element at its exact source line.  If no hint is
	 * configured, or the hint matches nothing, a single file-level issue is raised instead.</p>
	 */
	private void reportIssue(SensorContext context, InputFile file, Document document, ActiveRule rule) {
		String ruleName = rule.param(MuleRulesDefinition.PARAMS.RULE_NAME);
		String message = (ruleName != null && !ruleName.isBlank())
				? ruleName
				: rule.ruleKey().rule();

		List<Element> violatingNodes = findViolatingElements(document, rule);

		if (violatingNodes.isEmpty()) {
			// No precise nodes found — raise one file-level issue
			NewIssue newIssue = context.newIssue().forRule(rule.ruleKey());
			newIssue.at(newIssue.newLocation().on(file).message(message));
			newIssue.save();
			LOG.debug("Rule '{}': reported at file level (no location hint matched)", rule.ruleKey());
		} else {
			// One issue per violating node, each at its exact source line
			for (Element node : violatingNodes) {
				NewIssue newIssue = context.newIssue().forRule(rule.ruleKey());
				NewIssueLocation loc = newIssue.newLocation().on(file).message(message);
				int line = lineOf(node);
				if (line > 0) {
					try {
						loc = newIssue.newLocation().on(file).message(message).at(file.selectLine(line));
					} catch (Exception e) {
						// line out of range — fall back to file level for this node
						loc = newIssue.newLocation().on(file).message(message);
					}
				}
				newIssue.at(loc);
				newIssue.save();
				LOG.debug("Rule '{}': reported at line {}", rule.ruleKey(), line);
			}
		}
		LOG.info("Reported {} issue(s) for rule '{}' in {}", Math.max(1, violatingNodes.size()), rule.ruleKey(), file.filename());
	}

	/**
	 * Evaluates the rule's locationHint XPath against the JDOM2 document to find
	 * the specific elements that violate the rule.
	 *
	 * <p>Uses the same {@link XPathProcessor} (Jaxen + registered namespaces) that was
	 * used for rule evaluation — no second parse, no separate W3C DOM, no namespace
	 * mismatch.</p>
	 *
	 * @return list of violating elements (empty when no hint is configured or hint matches nothing)
	 */
	private List<Element> findViolatingElements(Document document, ActiveRule rule) {
		String locationHint = rule.param(MuleRulesDefinition.PARAMS.XPATH_LOCATION_HINT);
		if (locationHint == null || locationHint.trim().isEmpty()) {
			return List.of();
		}
		try {
			Element root = document.getRootElement();
			List<Element> nodes = xpathProcessor.evaluateList(locationHint.trim(), root);
			return nodes;
		} catch (Exception e) {
			LOG.debug("locationHint XPath failed for rule {}: {}", rule.ruleKey(), e.getMessage());
			return List.of();
		}
	}

	/**
	 * Extracts the source line number from a JDOM2 element built with
	 * {@link org.jdom2.located.LocatedJDOMFactory}.  Returns 0 when location
	 * metadata is unavailable (e.g. in unit tests using plain SAXBuilder).
	 */
	private static int lineOf(Element element) {
		if (element instanceof Located located) {
			return located.getLine();
		}
		return 0;
	}

	/**
	 * Gets the language key for the given context.
	 * Currently always returns Mule 4 language key.
	 *
	 * @param context Sensor context
	 * @return Language key
	 * @deprecated Use MuleLanguage.LANGUAGE_KEY directly
	 */
	public static String getLanguage(SensorContext context) {
		return MuleLanguage.LANGUAGE_MULE4_KEY;
	}

	/**
	 * Substitutes {@code ${key}} placeholders in an XPath expression with values from the
	 * provided parameter map.  Unknown placeholders are left as-is.
	 *
	 * @param xpath   XPath expression possibly containing {@code ${key}} placeholders
	 * @param params  map of parameter name to value
	 * @return XPath expression with all known placeholders replaced
	 */
	public static String substituteParameters(String xpath, java.util.Map<String, String> params) {
		if (xpath == null || params.isEmpty() || !xpath.contains("${")) return xpath;
		String result = xpath;
		for (java.util.Map.Entry<String, String> e : params.entrySet()) {
			result = result.replace("${" + e.getKey() + "}", e.getValue());
		}
		return result;
	}

	/** Internal parameter keys that must not be treated as user-defined rule parameters. */
	private static final java.util.Set<String> INTERNAL_PARAM_KEYS = java.util.Set.of(
		MuleRulesDefinition.PARAMS.CATEGORY,
		MuleRulesDefinition.PARAMS.SCOPE,
		MuleRulesDefinition.PARAMS.XPATH,
		MuleRulesDefinition.PARAMS.XPATH_LOCATION_HINT,
		MuleRulesDefinition.PARAMS.RULE_NAME,
		MuleRulesDefinition.PARAMS.PLUGIN_VERSION
	);

	/**
	 * Collects user-defined parameter values from the active rule, excluding internal params.
	 *
	 * @param rule active rule whose params() map is inspected
	 * @return map of user parameter name → value (defaults used when no override is set)
	 */
	private static java.util.Map<String, String> resolveUserParams(ActiveRule rule) {
		java.util.Map<String, String> resolved = new java.util.HashMap<>();
		for (java.util.Map.Entry<String, String> entry : rule.params().entrySet()) {
			if (!INTERNAL_PARAM_KEYS.contains(entry.getKey())) {
				resolved.put(entry.getKey(), entry.getValue());
			}
		}
		return resolved;
	}
}