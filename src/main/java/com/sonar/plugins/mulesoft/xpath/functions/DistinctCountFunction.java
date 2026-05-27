package com.sonar.plugins.mulesoft.xpath.functions;

import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jdom2.Attribute;
import org.jdom2.Element;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DistinctCountFunction implements Function {
    public static final String NAME = "distinct-count";

    @Override
    public Object call(Context context, List args) throws FunctionCallException {
        if (args.size() != 1) throw new FunctionCallException(NAME + "() requires exactly 1 argument");
        Object arg = args.get(0);
        if (!(arg instanceof List<?> nodes)) return Double.valueOf(0);
        Set<String> seen = new HashSet<>();
        for (Object node : nodes) {
            seen.add(nodeStringValue(node));
        }
        return Double.valueOf(seen.size());
    }

    private static String nodeStringValue(Object node) {
        if (node instanceof Attribute a) return a.getValue();
        if (node instanceof Element e) return e.getValue();
        return node.toString();
    }
}
