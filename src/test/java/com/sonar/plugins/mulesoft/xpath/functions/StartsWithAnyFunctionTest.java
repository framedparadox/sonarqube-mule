package com.sonar.plugins.mulesoft.xpath.functions;

import org.jaxen.FunctionCallException;
import org.jaxen.Context;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class StartsWithAnyFunctionTest {
    private final StartsWithAnyFunction fn = new StartsWithAnyFunction();
    private final Context ctx = mock(Context.class);

    @Test
    void returns_true_when_any_prefix_matches() throws FunctionCallException {
        Object result = fn.call(ctx, List.of("APIKIT:BAD_REQUEST", "APIKIT:,HTTP:"));
        assertThat(result).isEqualTo(Boolean.TRUE);
    }

    @Test
    void returns_false_when_no_prefix_matches() throws FunctionCallException {
        Object result = fn.call(ctx, List.of("db.host", "api.,mule."));
        assertThat(result).isEqualTo(Boolean.FALSE);
    }

    @Test
    void trims_each_prefix() throws FunctionCallException {
        Object result = fn.call(ctx, List.of("HTTP:NOT_FOUND", " HTTP: , APIKIT: "));
        assertThat(result).isEqualTo(Boolean.TRUE);
    }

    @Test
    void empty_csv_is_false() throws FunctionCallException {
        Object result = fn.call(ctx, List.of("anything", ""));
        assertThat(result).isEqualTo(Boolean.FALSE);
    }

    @Test
    void wrong_arg_count_throws() {
        assertThatThrownBy(() -> fn.call(ctx, List.of("only-one")))
                .isInstanceOf(FunctionCallException.class);
    }
}
