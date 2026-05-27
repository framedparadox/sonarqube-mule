package com.sonar.plugins.mulesoft.sensor;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class CpdDwlTokenizerTest {

    @Test
    void strips_line_and_block_comments() {
        String dwl = """
                %dw 2.0
                // a comment
                output application/json
                /* block
                   comment */
                ---
                { name: payload.name }
                """;
        List<CpdDwlTokenizer.Token> tokens = new CpdDwlTokenizer().tokenize(dwl);
        String joined = tokens.stream().map(CpdDwlTokenizer.Token::text).reduce("", (a, b) -> a + " " + b);
        assertThat(joined).doesNotContain("a comment");
        assertThat(joined).doesNotContain("block");
        assertThat(joined).contains("payload");
        assertThat(joined).contains("output");
    }
}
