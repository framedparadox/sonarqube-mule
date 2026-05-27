package com.sonar.plugins.mulesoft.metrics;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CoreMetricsFeederTest {

    @Test
    void counts_xml_ncloc_lines_comments() {
        String xml = """
                <?xml version="1.0"?>
                <!-- a comment -->
                <mule>
                  <flow name="f"/>
                  <!-- another
                       comment -->
                </mule>
                """;
        CoreMetricsFeeder.Counts c = CoreMetricsFeeder.countXml(xml);
        assertThat(c.lines()).isEqualTo(7);
        assertThat(c.commentLines()).isEqualTo(3);
        assertThat(c.ncloc()).isEqualTo(4); // <?xml?>, <mule>, <flow/>, </mule>
    }

    @Test
    void counts_dwl_ncloc_lines_comments() {
        String dwl = """
                %dw 2.0
                // single line comment
                output application/json
                /* block
                   comment */
                ---
                { name: payload.name }
                """;
        CoreMetricsFeeder.Counts c = CoreMetricsFeeder.countDwl(dwl);
        assertThat(c.lines()).isEqualTo(7);
        assertThat(c.commentLines()).isEqualTo(3);
        assertThat(c.ncloc()).isEqualTo(4);
    }

    @Test
    void empty_file_is_zero() {
        CoreMetricsFeeder.Counts c = CoreMetricsFeeder.countXml("");
        assertThat(c.lines()).isZero();
        assertThat(c.ncloc()).isZero();
        assertThat(c.commentLines()).isZero();
    }
}
