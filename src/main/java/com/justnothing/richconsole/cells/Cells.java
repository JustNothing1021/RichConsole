package com.justnothing.richconsole.cells;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cell width calculation utilities.
 * Ported from rich/cells.py.
 */
public final class Cells {

    private Cells() {}

    /**
     * A span representing a single grapheme cluster in a string.
     * start: inclusive index in the string
     * end: exclusive index in the string
     * cellLength: number of terminal cells this grapheme occupies
     */
    public record CellSpan(int start, int end, int cellLength) {}

    // Ranges of unicode ordinals that produce a 1-cell wide character
    // This is non-exhaustive, but covers most common Western characters
    private static final int[][] SINGLE_CELL_UNICODE_RANGES = {
        {0x20, 0x7E},       // Latin (excluding non-printable)
        {0xA0, 0xAC},
        {0xAE, 0x02FF},
        {0x0370, 0x0482},   // Greek / Cyrillic
        {0x2500, 0x25FF},   // Box drawing, block elements, geometric shapes
        {0x2800, 0x28FF},   // Braille
    };

    // A set of characters that are a single cell wide
    private static final Set<Integer> SINGLE_CELLS;

    static {
        Set<Integer> set = new HashSet<>();
        for (int[] range : SINGLE_CELL_UNICODE_RANGES) {
            for (int c = range[0]; c <= range[1]; c++) {
                set.add(c);
            }
        }
        SINGLE_CELLS = Collections.unmodifiableSet(set);
    }

    /**
     * Check if all characters in the text are single-cell width.
     */
    private static boolean isSingleCellWidths(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (!SINGLE_CELLS.contains((int) text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    // Simplified Unicode width table for common double-width/zero-width character ranges.
    // Format: {start, end, width} where width is 2 for double-width, 0 for zero-width
    // IMPORTANT: Must be sorted by start value for binary search to work correctly.
    private static final int[][] UNICODE_WIDTH_TABLE = {
        {0x1100, 0x11FF, 2},   // Hangul Jamo
        {0x200B, 0x200F, 0},   // Zero-width characters
        {0x2028, 0x202F, 0},   // Line/paragraph separators
        {0x2060, 0x206F, 0},   // General punctuation (zero width)
        {0x2460, 0x24FF, 2},   // Enclosed Alphanumerics
        {0x2600, 0x27BF, 2},   // Misc Symbols + Dingbats
        {0x2E80, 0x2EFF, 2},   // CJK Radicals Supplement
        {0x2F00, 0x2FDF, 2},   // Kangxi Radicals
        {0x2FF0, 0x2FFF, 2},   // Ideographic Description Characters
        {0x3000, 0x303F, 2},   // CJK Symbols and Punctuation
        {0x3040, 0x309F, 2},   // Hiragana
        {0x30A0, 0x30FF, 2},   // Katakana
        {0x3100, 0x312F, 2},   // Bopomofo
        {0x3190, 0x319F, 2},   // Kanbun
        {0x31A0, 0x31BF, 2},   // Bopomofo Extended
        {0x3200, 0x33FF, 2},   // Enclosed CJK Letters and Months
        {0x3400, 0x4DBF, 2},   // CJK Extension A
        {0x4E00, 0x9FFF, 2},   // CJK Unified Ideographs
        {0xAC00, 0xD7AF, 2},   // Hangul Syllables
        {0xF900, 0xFAFF, 2},   // CJK Compatibility Ideographs
        {0xFE00, 0xFE0F, 0},   // Variation Selectors
        {0xFE30, 0xFE4F, 2},   // CJK Compatibility Forms
        {0xFEFF, 0xFEFF, 0},   // BOM / zero-width no-break space
        {0xFF00, 0xFFEF, 2},   // Fullwidth Forms
        {0xFFF0, 0xFFF8, 0},   // Unassigned (zero width)
        {0x1F000, 0x1F02F, 2}, // Mahjong Tiles
        {0x1F0A0, 0x1F0FF, 2}, // Playing Cards
        {0x1F300, 0x1F9FF, 2}, // Emoji
        {0x1FA00, 0x1FAFF, 2}, // Emoji (Supplement)
        {0x1FC00, 0x1FCFF, 2}, // Emoji (Supplement)
        {0x20000, 0x2A6DF, 2}, // CJK Extension B
        {0x2A700, 0x2CEAF, 2}, // CJK Extension C
        {0x2CEB0, 0x2EBEF, 2}, // CJK Extension D
        {0x2F800, 0x2FA1F, 2}, // CJK Compatibility Ideographs Supplement
        {0x30000, 0x3134F, 2}, // CJK Extension E-G
        {0xE0000, 0xE007F, 0}, // Tags (zero width)
    };

    // Character cell size cache
    private static final ConcurrentHashMap<Integer, Integer> CHARACTER_CELL_SIZE_CACHE = new ConcurrentHashMap<>();
    // Cached cell length for strings
    private static final ConcurrentHashMap<String, Integer> CACHED_CELL_LEN = new ConcurrentHashMap<>();

    /**
     * Get the cell size of a character.
     *
     * @param character a single character
     * @return number of cells (0, 1 or 2) occupied by that character
     */
    public static int getCharacterCellSize(char character) {
        return getCharacterCellSize((int) character);
    }

    /**
     * Get the cell size of a character by codepoint.
     */
    public static int getCharacterCellSize(int codepoint) {
        Integer cached = CHARACTER_CELL_SIZE_CACHE.get(codepoint);
        if (cached != null) {
            return cached;
        }

        int width;
        if (codepoint > 0 && codepoint < 32 || codepoint >= 0x7F && codepoint < 0xA0) {
            width = 0;
        } else {
            // Check single-cell set first
            if (SINGLE_CELLS.contains(codepoint)) {
                width = 1;
            } else {
                // Binary search in width table
                width = lookupWidth(codepoint);
            }
        }

        CHARACTER_CELL_SIZE_CACHE.put(codepoint, width);
        return width;
    }

    private static int lookupWidth(int codepoint) {
        int lower = 0;
        int upper = UNICODE_WIDTH_TABLE.length - 1;

        while (lower <= upper) {
            int index = (lower + upper) >>> 1;
            int[] entry = UNICODE_WIDTH_TABLE[index];
            int start = entry[0];
            int end = entry[1];
            int width = entry[2];

            if (codepoint < start) {
                upper = index - 1;
            } else if (codepoint > end) {
                lower = index + 1;
            } else {
                return width;
            }
        }
        return 1; // Default: single width
    }

    /**
     * Get the number of cells required to display text (with caching).
     * It is recommended to use cellLen over this method for long strings.
     */
    public static int cachedCellLen(String text) {
        Integer cached = CACHED_CELL_LEN.get(text);
        if (cached != null) {
            return cached;
        }
        int len = computeCellLen(text);
        if (CACHED_CELL_LEN.size() > 4096) {
            CACHED_CELL_LEN.clear();
        }
        CACHED_CELL_LEN.put(text, len);
        return len;
    }

    /**
     * Get the cell length of a string (length as it appears in the terminal).
     */
    public static int cellLen(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        if (text.length() < 512) {
            return cachedCellLen(text);
        }
        return computeCellLen(text);
    }

    private static int computeCellLen(String text) {
        if (text.isEmpty()) {
            return 0;
        }
        if (isSingleCellWidths(text)) {
            return text.length();
        }

        // Check for zero-width joiner and variation selectors
        if (text.indexOf('\u200D') < 0 && text.indexOf('\uFE0F') < 0) {
            // Simple case
            int total = 0;
            for (int i = 0; i < text.length(); i++) {
                total += getCharacterCellSize(text.charAt(i));
            }
            return total;
        }

        // Complex case with ZWJ / variation selectors
        int totalWidth = 0;
        Character lastMeasuredCharacter = null;
        Set<Character> SPECIAL = new HashSet<>(Arrays.asList('\u200D', '\uFE0F'));

        int index = 0;
        int characterCount = text.length();

        while (index < characterCount) {
            char ch = text.charAt(index);
            if (SPECIAL.contains(ch)) {
                if (ch == '\u200D') {
                    index++;
                } else if (lastMeasuredCharacter != null) {
                    // Variation selector 16 could make narrow chars wide
                    // Simplified: don't add extra width for now
                    lastMeasuredCharacter = null;
                }
            } else {
                int charWidth = getCharacterCellSize(ch);
                if (charWidth > 0) {
                    lastMeasuredCharacter = ch;
                    totalWidth += charWidth;
                }
            }
            index++;
        }

        return totalWidth;
    }

    /**
     * Divide text into spans that define a single grapheme, and additionally
     * return the cell length of the whole string.
     *
     * @return an array of two elements: the list of CellSpan and the total width
     */
    public static Object[] splitGraphemes(String text) {
        int codepointCount = text.length();
        int index = 0;

        int totalWidth = 0;
        List<CellSpan> spans = new ArrayList<>();

        while (index < codepointCount) {
            char ch = text.charAt(index);

            if (ch == '\u200D') {
                // Zero width joiner
                if (spans.isEmpty()) {
                    spans.add(new CellSpan(index, index + 1, 0));
                    index++;
                    continue;
                }
                // Join with previous: extend previous span
                int nextIndex = index < (codepointCount - 1) ? index + 2 : index + 1;
                CellSpan prev = spans.get(spans.size() - 1);
                spans.set(spans.size() - 1, new CellSpan(prev.start(), nextIndex, prev.cellLength()));
                index = nextIndex;
                continue;
            }

            if (ch == '\uFE0F') {
                // Variation selector 16
                index++;
                if (spans.isEmpty()) {
                    continue;
                }
                CellSpan prev = spans.get(spans.size() - 1);
                spans.set(spans.size() - 1, new CellSpan(prev.start(), index, prev.cellLength()));
                continue;
            }

            int charWidth = getCharacterCellSize(ch);
            if (charWidth > 0) {
                int nextIndex = index + 1;
                spans.add(new CellSpan(index, nextIndex, charWidth));
                totalWidth += charWidth;
                index = nextIndex;
            } else {
                // Zero width character
                if (!spans.isEmpty()) {
                    CellSpan prev = spans.get(spans.size() - 1);
                    spans.set(spans.size() - 1, new CellSpan(prev.start(), index + 1, prev.cellLength()));
                } else {
                    spans.add(new CellSpan(index, index + 1, 0));
                }
                index++;
            }
        }

        return new Object[]{spans, totalWidth};
    }

    /**
     * Split text by cell position.
     * If the cell position falls within a double width character,
     * it is converted to two spaces.
     *
     * @param text         text to split
     * @param cellPosition offset in cells
     * @return a two-element string array [left, right]
     */
    public static String[] splitText(String text, int cellPosition) {
        if (cellPosition <= 0) {
            return new String[]{"", text};
        }
        if (isSingleCellWidths(text)) {
            if (cellPosition >= text.length()) {
                return new String[]{text, ""};
            }
            return new String[]{text.substring(0, cellPosition), text.substring(cellPosition)};
        }
        return doSplitText(text, cellPosition);
    }

    @SuppressWarnings("unchecked")
    private static String[] doSplitText(String text, int cellPosition) {
        Object[] result = splitGraphemes(text);
        List<CellSpan> spans = (List<CellSpan>) result[0];
        int cellLength = (Integer) result[1];

        if (cellLength <= cellPosition) {
            return new String[]{text, ""};
        }

        // Guess initial offset
        int offset = (int) ((cellPosition / (float) cellLength) * spans.size());
        int leftSize = 0;
        for (int i = 0; i < offset && i < spans.size(); i++) {
            leftSize += spans.get(i).cellLength();
        }

        while (true) {
            if (leftSize == cellPosition) {
                if (offset >= spans.size()) {
                    return new String[]{text, ""};
                }
                int splitIndex = spans.get(offset).start();
                return new String[]{text.substring(0, splitIndex), text.substring(splitIndex)};
            }
            if (leftSize < cellPosition) {
                if (offset >= spans.size()) {
                    return new String[]{text, ""};
                }
                CellSpan span = spans.get(offset);
                if (leftSize + span.cellLength() > cellPosition) {
                    return new String[]{text.substring(0, span.start()) + " ", " " + text.substring(span.end())};
                }
                leftSize += span.cellLength();
                offset++;
            } else {
                // leftSize > cellPosition
                if (offset <= 0) {
                    return new String[]{"", text};
                }
                CellSpan span = spans.get(offset - 1);
                if (leftSize - span.cellLength() < cellPosition) {
                    return new String[]{text.substring(0, span.start()) + " ", " " + text.substring(span.end())};
                }
                leftSize -= span.cellLength();
                offset--;
            }
        }
    }

    /**
     * Adjust a string by cropping or padding with spaces such that it fits
     * within the given number of cells.
     *
     * @param text  string to adjust
     * @param total desired size in cells
     * @return a string with cell size equal to total
     */
    public static String setCellSize(String text, int total) {
        if (isSingleCellWidths(text)) {
            int size = text.length();
            if (size < total) {
                StringBuilder sb = new StringBuilder(text);
                for (int i = size; i < total; i++) sb.append(' ');
                return sb.toString();
            }
            return text.substring(0, total);
        }
        if (total <= 0) return "";
        int cellSize = cellLen(text);
        if (cellSize == total) return text;
        if (cellSize < total) {
            StringBuilder sb = new StringBuilder(text);
            for (int i = cellSize; i < total; i++) sb.append(' ');
            return sb.toString();
        }
        String[] split = doSplitText(text, total);
        return split[0];
    }

    /**
     * Split text into lines such that each line fits within the available (cell) width.
     *
     * @param text  the text to fold
     * @param width the width available (number of cells)
     * @return a list of strings such that each string has cell width &lt;= width
     */
    @SuppressWarnings("unchecked")
    public static List<String> chopCells(String text, int width) {
        if (width <= 0) {
            return Collections.singletonList("");
        }
        if (isSingleCellWidths(text)) {
            List<String> lines = new ArrayList<>();
            for (int i = 0; i < text.length(); i += width) {
                lines.add(text.substring(i, Math.min(i + width, text.length())));
            }
            return lines;
        }

        Object[] result = splitGraphemes(text);
        List<CellSpan> spans = (List<CellSpan>) result[0];

        int lineSize = 0;
        List<String> lines = new ArrayList<>();
        int lineOffset = 0;

        for (CellSpan span : spans) {
            if (lineSize + span.cellLength() > width) {
                lines.add(text.substring(lineOffset, span.start()));
                lineOffset = span.start();
                lineSize = 0;
            }
            lineSize += span.cellLength();
        }
        if (lineSize > 0) {
            lines.add(text.substring(lineOffset));
        }

        return lines;
    }
}
