package com.sonar.plugins.mulesoft.xpath.functions;

import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jaxen.function.StringFunction;
import java.util.List;

public class StartsWithAnyFunction implements Function {
    public static final String NAME = "starts-with-any";

    @Override
    public Object call(Context context, List args) throws FunctionCallException {
        if (args.size() != 2) {
            throw new FunctionCallException(NAME + "() requires exactly 2 arguments (string, csv)");
        }
        String input = StringFunction.evaluate(args.get(0), context.getNavigator());
        String csv = StringFunction.evaluate(args.get(1), context.getNavigator());
        if (csv == null || csv.isEmpty()) return Boolean.FALSE;
        for (String prefix : csv.split(",")) {
            String trimmed = prefix.trim();
            if (!trimmed.isEmpty() && input != null && input.startsWith(trimmed)) return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
}
