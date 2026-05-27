package com.sonar.plugins.mulesoft.sensor;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.located.Located;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CpdXmlTokenizer {

    public record Token(int startLine, String text) {}

    private final boolean excludeDocAttrs;

    public CpdXmlTokenizer(boolean excludeDocAttrs) {
        this.excludeDocAttrs = excludeDocAttrs;
    }

    public List<Token> tokenize(Document document) {
        List<Token> tokens = new ArrayList<>();
        Element root = document.getRootElement();
        emit(root, tokens);
        Iterator<Element> it = root.getDescendants(Filters.element());
        while (it.hasNext()) emit(it.next(), tokens);
        return tokens;
    }

    private void emit(Element el, List<Token> out) {
        StringBuilder sb = new StringBuilder();
        String prefix = el.getNamespacePrefix();
        if (prefix != null && !prefix.isEmpty()) sb.append(prefix).append(':');
        sb.append(el.getName());
        List<Attribute> attrs = new ArrayList<>(el.getAttributes());
        attrs.sort((a, b) -> a.getQualifiedName().compareTo(b.getQualifiedName()));
        for (Attribute a : attrs) {
            String qn = a.getQualifiedName();
            if (excludeDocAttrs && (qn.equals("doc:id") || qn.equals("doc:name"))) continue;
            sb.append(' ').append(qn).append('=').append(a.getValue().replace(' ', '_'));
        }
        int line = (el instanceof Located loc) ? Math.max(1, loc.getLine()) : 1;
        out.add(new Token(line, sb.toString()));
    }
}
