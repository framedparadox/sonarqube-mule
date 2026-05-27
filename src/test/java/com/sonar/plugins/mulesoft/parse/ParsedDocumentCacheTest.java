package com.sonar.plugins.mulesoft.parse;

import org.jdom2.Document;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParsedDocumentCacheTest {

    private static final String SIMPLE_MULE_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <mule xmlns="http://www.mulesoft.org/schema/mule/core">
              <flow name="f1"/>
            </mule>
            """;

    private static InputFile mockFile(String filename, String content) throws IOException {
        InputFile file = mock(InputFile.class);
        when(file.toString()).thenReturn(filename);
        when(file.inputStream()).thenAnswer(inv ->
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        return file;
    }

    @Test
    void parses_once_and_returns_cached_document() throws IOException {
        InputFile file = mockFile("flow.xml", SIMPLE_MULE_XML);

        ParsedDocumentCache cache = new ParsedDocumentCache();
        Optional<Document> first = cache.get(file);
        Optional<Document> second = cache.get(file);

        assertThat(first).isPresent();
        assertThat(second).isPresent();
        assertThat(second.get()).isSameAs(first.get());
        assertThat(cache.parseErrors()).isEmpty();
    }

    @Test
    void records_parse_error_for_malformed_xml() throws IOException {
        InputFile file = mockFile("bad.xml", "<not-xml");

        ParsedDocumentCache cache = new ParsedDocumentCache();
        Optional<Document> result = cache.get(file);

        assertThat(result).isEmpty();
        assertThat(cache.parseErrors()).hasSize(1);
        assertThat(cache.parseErrors().get(0).file()).isEqualTo(file);
        assertThat(cache.parseErrors().get(0).message()).containsIgnoringCase("xml");
    }
}
