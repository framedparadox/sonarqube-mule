package com.sonar.plugins.mulesoft.dataweave;

/**
 * Small utilities for DataWeave text processing.
 */
public final class DataWeaveTextUtils {

	private DataWeaveTextUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Counts the number of lines in a string.
	 *
	 * <p>Uses '\n' as the line separator (treating both LF and CRLF as containing '\n' once decoded).
	 *
	 * @param content text to inspect
	 * @return line count, or 0 for null/empty input
	 */
	public static int countLines(String content) {
		if (content == null || content.isEmpty()) {
			return 0;
		}
		int lines = 1;
		for (int i = 0; i < content.length(); i++) {
			if (content.charAt(i) == '\n') {
				lines++;
			}
		}
		return lines;
	}
}
