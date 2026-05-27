package com.sonar.plugins.mulesoft.xpath.functions;

import org.jaxen.Context;
import org.jaxen.FunctionCallException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PropPlaceholderFunctionTest {
    private final PropPlaceholderFunction fn = new PropPlaceholderFunction();
    private final Context ctx = mock(Context.class);

    @Test
    void matches_well_formed_placeholder() throws FunctionCallException {
        assertThat(fn.call(ctx, List.of("${db.host}"))).isEqualTo(Boolean.TRUE);
        assertThat(fn.call(ctx, List.of("${a}"))).isEqualTo(Boolean.TRUE);
    }

    @Test
    void rejects_hardcoded_values() throws FunctionCallException {
        assertThat(fn.call(ctx, List.of("localhost"))).isEqualTo(Boolean.FALSE);
        assertThat(fn.call(ctx, List.of("${}"))).isEqualTo(Boolean.FALSE);
        assertThat(fn.call(ctx, List.of("$db.host"))).isEqualTo(Boolean.FALSE);
        assertThat(fn.call(ctx, List.of(""))).isEqualTo(Boolean.FALSE);
    }

    @Test
    void rejects_embedded_placeholder() throws FunctionCallException {
        assertThat(fn.call(ctx, List.of("prefix-${x}"))).isEqualTo(Boolean.FALSE);
    }

    @Test
    void wrong_arg_count_throws() {
        assertThatThrownBy(() -> fn.call(ctx, List.of("a", "b")))
                .isInstanceOf(FunctionCallException.class);
    }
}
