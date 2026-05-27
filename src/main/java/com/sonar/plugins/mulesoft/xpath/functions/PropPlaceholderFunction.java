package com.sonar.plugins.mulesoft.xpath.functions;

import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jaxen.function.StringFunction;
import java.util.List;
import java.util.regex.Pattern;

public class PropPlaceholderFunction implements Function {
    public static final String NAME = "prop-placeholder";
    private static final Pattern PATTERN = Pattern.compile("^\\$\\{\\S+\\}$");

    @Override
    public Object call(Context context, List args) throws FunctionCallException {
        if (args.size() != 1) throw new FunctionCallException(NAME + "() requires exactly 1 argument");
        String input = StringFunction.evaluate(args.get(0), context.getNavigator());
        if (input == null) return Boolean.FALSE;
        return PATTERN.matcher(input).matches();
    }
}
