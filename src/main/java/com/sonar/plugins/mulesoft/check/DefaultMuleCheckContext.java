package com.sonar.plugins.mulesoft.check;

import org.jdom2.Document;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

import java.util.List;
import java.util.Map;

public class DefaultMuleCheckContext implements MuleCheckContext {

    private final SensorContext sensorContext;
    private final ActiveRule activeRule;
    private final InputFile inputFile;
    private final Document document;
    private final List<Document> allDocuments;
    private final Map<String, Object> parameters;
    private final InputFile projectAnchorFile;

    public DefaultMuleCheckContext(
            SensorContext sensorContext,
            ActiveRule activeRule,
            InputFile inputFile,
            Document document,
            List<Document> allDocuments,
            Map<String, Object> parameters,
            InputFile projectAnchorFile) {
        this.sensorContext = sensorContext;
        this.activeRule = activeRule;
        this.inputFile = inputFile;
        this.document = document;
        this.allDocuments = allDocuments;
        this.parameters = parameters;
        this.projectAnchorFile = projectAnchorFile;
    }

    @Override public InputFile inputFile() { return inputFile; }
    @Override public Document document() { return document; }
    @Override public List<Document> allDocuments() { return allDocuments; }
    @Override public Map<String, Object> parameters() { return parameters; }
    @Override public Object projectModel() { return null; }

    @Override
    public void reportIssue(TextRange range, String message) {
        if (inputFile == null) {
            reportProjectIssue(message);
            return;
        }
        NewIssue issue = sensorContext.newIssue().forRule(activeRule.ruleKey());
        NewIssueLocation loc = issue.newLocation().on(inputFile).at(range).message(message);
        issue.at(loc).save();
    }

    @Override
    public void reportFileIssue(String message) {
        InputFile target = inputFile != null ? inputFile : projectAnchorFile;
        if (target == null) return;
        NewIssue issue = sensorContext.newIssue().forRule(activeRule.ruleKey());
        NewIssueLocation loc = issue.newLocation().on(target).message(message);
        issue.at(loc).save();
    }

    @Override
    public void reportProjectIssue(String message) {
        if (projectAnchorFile == null) return;
        NewIssue issue = sensorContext.newIssue().forRule(activeRule.ruleKey());
        NewIssueLocation loc = issue.newLocation().on(projectAnchorFile).message(message);
        issue.at(loc).save();
    }
}
