package com.sonar.plugins.mulesoft.sensor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CpdDwlTokenizer {

    public record Token(int startLine, String text) {}

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "\"[^\"\\n]*\"|'[^'\\n]*'|[A-Za-z_][A-Za-z_0-9]*|[0-9]+(\\.[0-9]+)?|[{}\\[\\]()<>,:;=+\\-*/%!&|]");

    public List<Token> tokenize(String source) {
        String stripped = stripComments(source);
        List<Token> tokens = new ArrayList<>();
        String[] lines = stripped.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            Matcher m = TOKEN_PATTERN.matcher(lines[i]);
            while (m.find()) {
                tokens.add(new Token(i + 1, m.group()));
            }
        }
        return tokens;
    }

    private static String stripComments(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        while (i < source.length()) {
            if (i + 1 < source.length() && source.charAt(i) == '/' && source.charAt(i + 1) == '/') {
                int end = source.indexOf('\n', i);
                if (end < 0) break;
                i = end;
            } else if (i + 1 < source.length() && source.charAt(i) == '/' && source.charAt(i + 1) == '*') {
                int end = source.indexOf("*/", i + 2);
                if (end < 0) break;
                for (int j = i; j < end + 2; j++) {
                    if (source.charAt(j) == '\n') out.append('\n');
                }
                i = end + 2;
            } else {
                out.append(source.charAt(i));
                i++;
            }
        }
        return out.toString();
    }
}
