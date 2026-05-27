package com.sonar.plugins.mulesoft.xpath.functions;

import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jdom2.Element;
import java.util.List;

public class ChildCountFunction implements Function {
    public static final String NAME = "child-count";

    @Override
    public Object call(Context context, List args) throws FunctionCallException {
        if (args.size() != 1) throw new FunctionCallException(NAME + "() requires exactly 1 argument");
        Object arg = args.get(0);
        Element target;
        if (arg instanceof List<?> list) {
            if (list.isEmpty()) return Double.valueOf(0);
            Object first = list.get(0);
            if (!(first instanceof Element el)) return Double.valueOf(0);
            target = el;
        } else if (arg instanceof Element el) {
            target = el;
        } else {
            return Double.valueOf(0);
        }
        return Double.valueOf(countDescendants(target));
    }

    private static int countDescendants(Element el) {
        int count = 0;
        for (Element child : el.getChildren()) {
            count++;
            count += countDescendants(child);
        }
        return count;
    }
}
