package com.sonar.plugins.mulesoft.metrics;

public final class CoreMetricsFeeder {

    public record Counts(int lines, int ncloc, int commentLines) {}

    private CoreMetricsFeeder() {}

    public static Counts countXml(String content) {
        if (content == null || content.isEmpty()) return new Counts(0, 0, 0);
        String[] rawLines = content.split("\n", -1);
        int effective = rawLines.length;
        if (effective > 0 && rawLines[effective - 1].isEmpty()) effective--;
        int ncloc = 0;
        int commentLines = 0;
        boolean inComment = false;
        for (int i = 0; i < effective; i++) {
            String stripped = rawLines[i].strip();
            boolean lineHasComment = false;
            boolean lineHasCode = false;
            int idx = 0;
            while (idx < stripped.length()) {
                if (inComment) {
                    lineHasComment = true;
                    int end = stripped.indexOf("-->", idx);
                    if (end >= 0) { inComment = false; idx = end + 3; }
                    else { idx = stripped.length(); }
                } else {
                    int start = stripped.indexOf("<!--", idx);
                    if (start < 0) {
                        if (!stripped.substring(idx).isBlank()) lineHasCode = true;
                        break;
                    } else {
                        if (!stripped.substring(idx, start).isBlank()) lineHasCode = true;
                        inComment = true;
                        lineHasComment = true;
                        idx = start + 4;
                    }
                }
            }
            if (lineHasCode) ncloc++;
            if (lineHasComment) commentLines++;
        }
        return new Counts(effective, ncloc, commentLines);
    }

    public static Counts countDwl(String content) {
        if (content == null || content.isEmpty()) return new Counts(0, 0, 0);
        String[] rawLines = content.split("\n", -1);
        int effective = rawLines.length;
        if (effective > 0 && rawLines[effective - 1].isEmpty()) effective--;
        int ncloc = 0;
        int commentLines = 0;
        boolean inBlock = false;
        for (int i = 0; i < effective; i++) {
            String stripped = rawLines[i].strip();
            boolean hasComment = false;
            boolean hasCode = false;
            int idx = 0;
            while (idx < stripped.length()) {
                if (inBlock) {
                    hasComment = true;
                    int end = stripped.indexOf("*/", idx);
                    if (end >= 0) { inBlock = false; idx = end + 2; }
                    else { idx = stripped.length(); }
                } else {
                    int lineC = stripped.indexOf("//", idx);
                    int blockC = stripped.indexOf("/*", idx);
                    int next = -1;
                    boolean isBlock = false;
                    if (lineC >= 0 && (blockC < 0 || lineC < blockC)) { next = lineC; }
                    else if (blockC >= 0) { next = blockC; isBlock = true; }
                    if (next < 0) {
                        if (!stripped.substring(idx).isBlank()) hasCode = true;
                        break;
                    }
                    if (!stripped.substring(idx, next).isBlank()) hasCode = true;
                    hasComment = true;
                    if (isBlock) { inBlock = true; idx = next + 2; }
                    else { break; }
                }
            }
            if (hasCode) ncloc++;
            if (hasComment) commentLines++;
        }
        return new Counts(effective, ncloc, commentLines);
    }
}
