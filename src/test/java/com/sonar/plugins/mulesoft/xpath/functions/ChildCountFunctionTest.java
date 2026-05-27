package com.sonar.plugins.mulesoft.xpath.functions;

import org.jaxen.Context;
import org.jaxen.FunctionCallException;
import org.jdom2.Element;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ChildCountFunctionTest {
    private final ChildCountFunction fn = new ChildCountFunction();
    private final Context ctx = mock(Context.class);

    @Test
    void counts_all_descendant_elements() throws FunctionCallException {
        Element flow = new Element("flow");
        Element choice = new Element("choice");
        Element when = new Element("when");
        Element otherwise = new Element("otherwise");
        choice.addContent(when);
        choice.addContent(otherwise);
        flow.addContent(choice);
        Object result = fn.call(ctx, List.of(List.of(flow)));
        assertThat(result).isEqualTo(Double.valueOf(3));
    }

    @Test
    void empty_element_returns_zero() throws FunctionCallException {
        Element flow = new Element("flow");
        Object result = fn.call(ctx, List.of(List.of(flow)));
        assertThat(result).isEqualTo(Double.valueOf(0));
    }

    @Test
    void accepts_single_element_not_in_list() throws FunctionCallException {
        Element flow = new Element("flow");
        flow.addContent(new Element("logger"));
        Object result = fn.call(ctx, List.of(flow));
        assertThat(result).isEqualTo(Double.valueOf(1));
    }
}
