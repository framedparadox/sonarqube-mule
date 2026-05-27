package com.sonar.plugins.mulesoft.xpath.functions;

import org.jaxen.Context;
import org.jaxen.FunctionCallException;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DistinctCountFunctionTest {
    private final DistinctCountFunction fn = new DistinctCountFunction();
    private final Context ctx = mock(Context.class);

    @Test
    void counts_distinct_string_values_from_attributes() throws FunctionCallException {
        Attribute a = new Attribute("path", "/a");
        Attribute b = new Attribute("path", "/b");
        Attribute c = new Attribute("path", "/a");
        Object result = fn.call(ctx, List.of(List.of(a, b, c)));
        assertThat(result).isEqualTo(Double.valueOf(2));
    }

    @Test
    void counts_distinct_element_text_values() throws FunctionCallException {
        Element x = new Element("x"); x.setText("hello");
        Element y = new Element("y"); y.setText("world");
        Element z = new Element("z"); z.setText("hello");
        Object result = fn.call(ctx, List.of(List.of(x, y, z)));
        assertThat(result).isEqualTo(Double.valueOf(2));
    }

    @Test
    void empty_nodeset_returns_zero() throws FunctionCallException {
        Object result = fn.call(ctx, List.of(List.of()));
        assertThat(result).isEqualTo(Double.valueOf(0));
    }
}
