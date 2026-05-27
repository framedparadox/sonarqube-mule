package com.sonar.plugins.mulesoft.check;

import org.jdom2.Document;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;

import java.util.List;
import java.util.Map;

public interface MuleCheckContext {
    InputFile inputFile();
    Document document();
    List<Document> allDocuments();
    Map<String, Object> parameters();
    Object projectModel();
    void reportIssue(TextRange range, String message);
    void reportFileIssue(String message);
    void reportProjectIssue(String message);
}
