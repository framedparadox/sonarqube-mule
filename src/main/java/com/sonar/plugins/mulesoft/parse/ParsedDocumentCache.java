package com.sonar.plugins.mulesoft.parse;

import com.sonar.plugins.mulesoft.util.XmlParserFactory;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Per-analysis cache that parses each Mule XML file exactly once and reuses
 * the resulting JDOM Document across every sensor that needs it.
 */
public class ParsedDocumentCache {

    private static final Logger LOG = LoggerFactory.getLogger(ParsedDocumentCache.class);

    private final SAXBuilder builder;
    private final Map<InputFile, Document> cache = new HashMap<>();
    private final List<ParseError> parseErrors = new ArrayList<>();

    public ParsedDocumentCache() {
        this(XmlParserFactory.createLocatedBuilder());
    }

    public ParsedDocumentCache(SAXBuilder builder) {
        this.builder = builder;
    }

    public Optional<Document> get(InputFile file) {
        Document cached = cache.get(file);
        if (cached != null) {
            return Optional.of(cached);
        }
        try {
            Document doc = builder.build(file.inputStream());
            cache.put(file, doc);
            return Optional.of(doc);
        } catch (JDOMException e) {
            LOG.warn("XML parse failure for {}: {}", file, e.getMessage());
            parseErrors.add(new ParseError(file, e.getMessage()));
            return Optional.empty();
        } catch (IOException e) {
            LOG.warn("IO failure reading {}: {}", file, e.getMessage());
            parseErrors.add(new ParseError(file, e.getMessage()));
            return Optional.empty();
        }
    }

    public List<ParseError> parseErrors() {
        return Collections.unmodifiableList(parseErrors);
    }

    public record ParseError(InputFile file, String message) {}
}
