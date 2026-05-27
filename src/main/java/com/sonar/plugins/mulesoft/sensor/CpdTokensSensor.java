package com.sonar.plugins.mulesoft.sensor;

import com.sonar.plugins.mulesoft.language.MuleLanguage;
import com.sonar.plugins.mulesoft.parse.ParsedDocumentCache;
import org.jdom2.Document;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;

import java.io.IOException;
import java.util.Optional;

public class CpdTokensSensor implements Sensor {

    public static final String EXCLUDE_DOC_ATTRS_PROPERTY = "sonar.mule.cpd.excludeDocAttributes";

    private final ParsedDocumentCache cache;
    private final boolean defaultExcludeDocAttrs;

    public CpdTokensSensor() {
        this(new ParsedDocumentCache(), true);
    }

    public CpdTokensSensor(ParsedDocumentCache cache, boolean defaultExcludeDocAttrs) {
        this.cache = cache;
        this.defaultExcludeDocAttrs = defaultExcludeDocAttrs;
    }

    @Override
    public void describe(SensorDescriptor d) {
        d.name("Mule CPD Tokens").onlyOnLanguages(MuleLanguage.LANGUAGE_KEY);
    }

    @Override
    public void execute(SensorContext ctx) {
        boolean excl = ctx.config().getBoolean(EXCLUDE_DOC_ATTRS_PROPERTY).orElse(defaultExcludeDocAttrs);
        CpdXmlTokenizer xmlTokenizer = new CpdXmlTokenizer(excl);
        CpdDwlTokenizer dwlTokenizer = new CpdDwlTokenizer();

        for (InputFile file : ctx.fileSystem().inputFiles(
                ctx.fileSystem().predicates().hasLanguage(MuleLanguage.LANGUAGE_KEY))) {
            if (file.filename().endsWith(".dwl")) {
                emitDwl(ctx, file, dwlTokenizer);
            } else {
                emitXml(ctx, file, xmlTokenizer);
            }
        }
    }

    private void emitXml(SensorContext ctx, InputFile file, CpdXmlTokenizer tokenizer) {
        Optional<Document> doc = cache.get(file);
        if (doc.isEmpty()) return;
        NewCpdTokens newTokens = ctx.newCpdTokens().onFile(file);
        for (CpdXmlTokenizer.Token t : tokenizer.tokenize(doc.get())) {
            newTokens.addToken(file.selectLine(t.startLine()), t.text());
        }
        newTokens.save();
    }

    private void emitDwl(SensorContext ctx, InputFile file, CpdDwlTokenizer tokenizer) {
        try {
            String content = new String(file.inputStream().readAllBytes());
            NewCpdTokens newTokens = ctx.newCpdTokens().onFile(file);
            for (CpdDwlTokenizer.Token t : tokenizer.tokenize(content)) {
                newTokens.addToken(file.selectLine(t.startLine()), t.text());
            }
            newTokens.save();
        } catch (IOException e) {
            // skip unparseable files
        }
    }
}
