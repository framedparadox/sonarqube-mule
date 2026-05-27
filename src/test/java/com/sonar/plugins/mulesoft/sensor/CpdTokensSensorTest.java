package com.sonar.plugins.mulesoft.sensor;

import com.sonar.plugins.mulesoft.parse.ParsedDocumentCache;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;

import java.io.StringReader;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CpdTokensSensorTest {

    @Test
    void saves_cpd_tokens_for_mule_xml_document() throws Exception {
        // Parse a simple XML
        Document doc = new SAXBuilder().build(new StringReader("""
                <mule xmlns="http://www.mulesoft.org/schema/mule/core">
                  <flow name="f1">
                    <logger level="INFO" message="hi"/>
                  </flow>
                </mule>
                """));

        // Mock the cache to return the document
        ParsedDocumentCache cache = mock(ParsedDocumentCache.class);
        InputFile file = mock(InputFile.class);
        when(cache.get(file)).thenReturn(java.util.Optional.of(doc));

        // Mock sensor context
        SensorContext ctx = mock(SensorContext.class);
        var config = mock(org.sonar.api.config.Configuration.class);
        when(ctx.config()).thenReturn(config);
        when(config.getBoolean(any())).thenReturn(java.util.Optional.of(true));
        NewCpdTokens newCpdTokens = mock(NewCpdTokens.class);
        when(ctx.newCpdTokens()).thenReturn(newCpdTokens);
        when(newCpdTokens.onFile(any())).thenReturn(newCpdTokens);
        when(newCpdTokens.addToken(any(), any())).thenReturn(newCpdTokens);

        // Mock file system
        var fs = mock(org.sonar.api.batch.fs.FileSystem.class);
        var pred = mock(org.sonar.api.batch.fs.FilePredicate.class);
        when(ctx.fileSystem()).thenReturn(fs);
        when(fs.predicates()).thenReturn(mock(org.sonar.api.batch.fs.FilePredicates.class));
        when(fs.predicates().hasLanguage(any())).thenReturn(pred);
        when(fs.inputFiles(pred)).thenReturn(List.of(file));
        when(file.filename()).thenReturn("test.xml");
        when(file.selectLine(anyInt())).thenReturn(mock(org.sonar.api.batch.fs.TextRange.class));

        // Execute
        new CpdTokensSensor(cache, true).execute(ctx);

        // Verify tokens were saved
        verify(newCpdTokens, atLeastOnce()).addToken(any(), any());
        verify(newCpdTokens).save();
    }
}
