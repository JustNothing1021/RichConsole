package com.justnothing.richconsole.segment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.justnothing.richconsole.cells.Cells;
import com.justnothing.richconsole.style.Style;

/**
 * A piece of text with optional style and/or control codes.
 * This is the core rendering atom, ported from rich/segment.py Segment.
 */
public class Segment {

    private final String text;
    private final Style style;
    private final List<ControlCode> control;

    public Segment(String text) {
        this(text, null, null);
    }

    public Segment(String text, Style style) {
        this(text, style, null);
    }

    public Segment(String text, Style style, List<ControlCode> control) {
        this.text = text != null ? text : "";
        this.style = style;
        this.control = control;
    }

    public String getText() {
        return text;
    }

    public Style getStyle() {
        return style;
    }

    public List<ControlCode> getControl() {
        return control;
    }

    /**
     * Get the cell length of this segment's text.
     */
    public int cellLength() {
        return Cells.cellLen(text);
    }

    /**
     * Check if this segment is a control segment.
     */
    public boolean isControl() {
        return control != null && !control.isEmpty();
    }

    /**
     * Create a new-line segment.
     */
    public static Segment line() {
        return new Segment("\n");
    }

    /**
     * Split this segment at the given cell position.
     * Returns a two-element array: [left, right]. Either may be null
     * if the split results in an empty segment on that side.
     */
    public Segment[] splitCells(int cut) {
        if (isControl()) {
            if (cut == 0) {
                return new Segment[]{null, this};
            }
            return new Segment[]{this, null};
        }
        String[] split = Cells.splitText(text, cut);
        String leftText = split[0];
        String rightText = split[1];
        Segment left = leftText.isEmpty() ? null : new Segment(leftText, style);
        Segment right = rightText.isEmpty() ? null : new Segment(rightText, style);
        return new Segment[]{left, right};
    }

    /**
     * Apply a style to a sequence of segments.
     *
     * @param segments  the segments to style
     * @param style     the style to apply (may be null)
     * @param postStyle a style to apply after (may be null)
     * @return an iterable of styled segments
     */
    public static Iterable<Segment> applyStyle(Iterable<Segment> segments, Style style, Style postStyle) {
        List<Segment> result = new ArrayList<>();
        for (Segment segment : segments) {
            Style segmentStyle = segment.getStyle();
            if (segmentStyle == null) {
                segmentStyle = style;
            } else if (style != null) {
                segmentStyle = segmentStyle.add(style);
            }
            if (postStyle != null) {
                if (segmentStyle == null) {
                    segmentStyle = postStyle;
                } else {
                    segmentStyle = segmentStyle.add(postStyle);
                }
            }
            result.add(new Segment(segment.getText(), segmentStyle, segment.getControl()));
        }
        return result;
    }

    /**
     * Filter segments to include or exclude control segments.
     *
     * @param segments  the segments to filter
     * @param isControl true to include only control segments, false to exclude them
     * @return filtered iterable of segments
     */
    public static Iterable<Segment> filterControl(Iterable<Segment> segments, boolean isControl) {
        List<Segment> result = new ArrayList<>();
        for (Segment segment : segments) {
            if (segment.isControl() == isControl) {
                result.add(segment);
            }
        }
        return result;
    }

    /**
     * Split segments into lines (lists of segments separated by new-line segments).
     * Ported from rich/segment.py split_lines.
     */
    public static Iterable<List<Segment>> splitLines(Iterable<Segment> segments) {
        List<List<Segment>> lines = new ArrayList<>();
        List<Segment> line = new ArrayList<>();

        for (Segment segment : segments) {
            String text = segment.getText();
            Style style = segment.getStyle();
            boolean isControl = segment.isControl();

            // Check if newline is in the text AND it's not a control segment
            if (text.contains("\n") && !isControl) {
                // Split on newlines within this segment
                int start = 0;
                while (start < text.length()) {
                    int newlinePos = text.indexOf('\n', start);
                    if (newlinePos == -1) {
                        // No more newlines, add remaining text
                        String remaining = text.substring(start);
                        if (!remaining.isEmpty()) {
                            line.add(new Segment(remaining, style));
                        }
                        break;
                    }
                    // Add text before newline
                    String beforeNewline = text.substring(start, newlinePos);
                    if (!beforeNewline.isEmpty()) {
                        line.add(new Segment(beforeNewline, style));
                    }
                    // Yield current line and start new line
                    lines.add(line);
                    line = new ArrayList<>();
                    start = newlinePos + 1;
                }
            } else {
                line.add(segment);
            }
        }
        // Yield remaining line if not empty
        if (!line.isEmpty()) {
            lines.add(line);
        }
        return lines;
    }

    /**
     * Split segments into lines and crop/pad to the given length.
     * Ported from Python rich's split_and_crop_lines.
     *
     * <p>When includeNewLines is true, a newline segment is appended to each line
     * that was terminated by a {@code \n} in the original content. The LAST line
     * (which may or may not end with a newline) is handled according to the
     * original content: if the content ended with {@code \n}, the last line gets
     * a newline segment; otherwise it does not.</p>
     *
     * @param segments       the segments to split
     * @param length         the desired line length in cells
     * @param style          style for padding
     * @param pad            whether to pad lines shorter than length
     * @param includeNewLines whether to include new-line segments at end of each line
     * @return iterable of lists of segments, one per line
     */
    public static Iterable<List<Segment>> splitAndCropLines(Iterable<Segment> segments,
                                                            int length, Style style,
                                                            boolean pad, boolean includeNewLines) {
        List<List<Segment>> lines = new ArrayList<>();
        List<Segment> line = new ArrayList<>();

        for (Segment segment : segments) {
            String text = segment.getText();
            Style segStyle = segment.getStyle();
            boolean isControl = segment.isControl();

            if (text.contains("\n") && !isControl) {
                int start = 0;
                while (start < text.length()) {
                    int newlinePos = text.indexOf('\n', start);
                    if (newlinePos == -1) {
                        String remaining = text.substring(start);
                        if (!remaining.isEmpty()) {
                            line.add(new Segment(remaining, segStyle));
                        }
                        break;
                    }
                    String beforeNewline = text.substring(start, newlinePos);
                    if (!beforeNewline.isEmpty()) {
                        line.add(new Segment(beforeNewline, segStyle));
                    }
                    List<Segment> adjusted = adjustLineLength(line, length, style, pad);
                    if (includeNewLines) {
                        adjusted.add(line());
                    }
                    lines.add(adjusted);
                    line = new ArrayList<>();
                    start = newlinePos + 1;
                }
            } else {
                line.add(segment);
            }
        }
        // Last line (not terminated by \n) — no newline segment appended
        if (!line.isEmpty()) {
            List<Segment> adjusted = adjustLineLength(line, length, style, pad);
            lines.add(adjusted);
        }
        return lines;
    }

    /**
     * Adjust the length of a line of segments by cropping or padding.
     *
     * @param line   the line of segments
     * @param length the desired length in cells
     * @param style  style for padding
     * @param pad    whether to pad shorter lines
     * @return adjusted list of segments
     */
    public static List<Segment> adjustLineLength(List<Segment> line, int length, Style style, boolean pad) {
        int lineLength = getLineLength(line);
        if (lineLength < length) {
            if (pad) {
                int padLength = length - lineLength;
                List<Segment> result = new ArrayList<>(line);
                result.add(new Segment(spaces(padLength), style));
                return result;
            }
            return new ArrayList<>(line);
        }
        if (lineLength > length) {
            List<Segment> result = new ArrayList<>();
            int remaining = length;
            for (Segment segment : line) {
                if (segment.isControl()) {
                    // Always include control segments regardless of remaining space
                    result.add(segment);
                    continue;
                }
                if (remaining <= 0) {
                    break;
                }
                int segLen = segment.cellLength();
                if (segLen <= remaining) {
                    result.add(segment);
                    remaining -= segLen;
                } else {
                    Segment[] split = segment.splitCells(remaining);
                    if (split[0] != null) {
                        result.add(split[0]);
                    }
                    remaining = 0;
                }
            }
            return result;
        }
        return new ArrayList<>(line);
    }

    /**
     * Get the cell length of a line of segments.
     * Control segments are not counted (they have zero visual width).
     */
    public static int getLineLength(List<Segment> line) {
        int length = 0;
        for (Segment segment : line) {
            if (!segment.isControl()) {
                length += segment.cellLength();
            }
        }
        return length;
    }

    /**
     * Get the shape (width, height) of a list of segment lines.
     *
     * @param lines the lines of segments
     * @return an int array [width, height]
     */
    public static int[] getShape(List<List<Segment>> lines) {
        int width = 0;
        for (List<Segment> line : lines) {
            int lineWidth = getLineLength(line);
            if (lineWidth > width) {
                width = lineWidth;
            }
        }
        return new int[]{width, lines.size()};
    }

    /**
     * Set the shape of segment lines by padding/cropping.
     *
     * @param lines    the lines of segments
     * @param width    the desired width
     * @param height   the desired height (null to keep current)
     * @param style    style for padding
     * @param newLines whether to add new-line segments at the end of each line
     * @return reshaped list of lines
     */
    public static List<List<Segment>> setShape(List<List<Segment>> lines, int width,
                                               Integer height, Style style, boolean newLines) {
        List<List<Segment>> result = new ArrayList<>();
        int count = 0;
        for (List<Segment> line : lines) {
            if (height != null && count >= height) {
                break;
            }
            List<Segment> adjusted = adjustLineLength(line, width, style, true);
            if (newLines) {
                adjusted.add(line());
            }
            result.add(adjusted);
            count++;
        }
        // Pad with empty lines if needed
        if (height != null) {
            while (result.size() < height) {
                List<Segment> emptyLine = new ArrayList<>();
                emptyLine.add(new Segment(spaces(width), style));
                if (newLines) {
                    emptyLine.add(line());
                }
                result.add(emptyLine);
            }
        }
        return result;
    }

    /**
     * Align segment lines to the top within a given height.
     */
    public static List<List<Segment>> alignTop(List<List<Segment>> lines, int width,
                                               Integer height, Style style, boolean newLines) {
        return setShape(lines, width, height, style, newLines);
    }

    /**
     * Align segment lines to the bottom within a given height.
     */
    public static List<List<Segment>> alignBottom(List<List<Segment>> lines, int width,
                                                  Integer height, Style style, boolean newLines) {
        if (height == null) {
            return setShape(lines, width, null, style, newLines);
        }
        int excess = height - lines.size();
        List<List<Segment>> result = new ArrayList<>();
        // Add blank lines at top
        for (int i = 0; i < excess; i++) {
            List<Segment> blankLine = new ArrayList<>();
            blankLine.add(new Segment(spaces(width), style));
            if (newLines) {
                blankLine.add(line());
            }
            result.add(blankLine);
        }
        // Add content lines
        for (List<Segment> line : lines) {
            List<Segment> adjusted = adjustLineLength(line, width, style, true);
            if (newLines) {
                adjusted.add(line());
            }
            result.add(adjusted);
        }
        return result;
    }

    /**
     * Align segment lines to the middle within a given height.
     */
    public static List<List<Segment>> alignMiddle(List<List<Segment>> lines, int width,
                                                  Integer height, Style style, boolean newLines) {
        if (height == null) {
            return setShape(lines, width, null, style, newLines);
        }
        int excess = height - lines.size();
        int topPad = excess / 2;
        int bottomPad = excess - topPad;
        List<List<Segment>> result = new ArrayList<>();
        // Top padding
        for (int i = 0; i < topPad; i++) {
            List<Segment> blankLine = new ArrayList<>();
            blankLine.add(new Segment(spaces(width), style));
            if (newLines) {
                blankLine.add(line());
            }
            result.add(blankLine);
        }
        // Content lines
        for (List<Segment> line : lines) {
            List<Segment> adjusted = adjustLineLength(line, width, style, true);
            if (newLines) {
                adjusted.add(line());
            }
            result.add(adjusted);
        }
        // Bottom padding
        for (int i = 0; i < bottomPad; i++) {
            List<Segment> blankLine = new ArrayList<>();
            blankLine.add(new Segment(spaces(width), style));
            if (newLines) {
                blankLine.add(line());
            }
            result.add(blankLine);
        }
        return result;
    }

    /**
     * Simplify segments by combining adjacent segments with the same style.
     */
    public static Iterable<Segment> simplify(Iterable<Segment> segments) {
        List<Segment> result = new ArrayList<>();
        Segment current = null;
        for (Segment segment : segments) {
            if (current == null) {
                current = segment;
                continue;
            }
            if (current.isControl() || segment.isControl()) {
                result.add(current);
                current = segment;
                continue;
            }
            // Combine if styles match
            if (equals(current.getStyle(), segment.getStyle())) {
                current = new Segment(current.getText() + segment.getText(), current.getStyle());
            } else {
                result.add(current);
                current = segment;
            }
        }
        if (current != null) {
            result.add(current);
        }
        return result;
    }

    /**
     * Strip links from segments.
     */
    public static Iterable<Segment> stripLinks(Iterable<Segment> segments) {
        List<Segment> result = new ArrayList<>();
        for (Segment segment : segments) {
            Style segStyle = segment.getStyle();
            if (segStyle != null && segStyle.getLink() != null) {
                Style newStyle = segStyle.updateLink(null);
                result.add(new Segment(segment.getText(), newStyle, segment.getControl()));
            } else {
                result.add(segment);
            }
        }
        return result;
    }

    /**
     * Strip all styles from segments.
     */
    public static Iterable<Segment> stripStyles(Iterable<Segment> segments) {
        List<Segment> result = new ArrayList<>();
        for (Segment segment : segments) {
            if (segment.getStyle() == null) {
                result.add(segment);
            } else {
                result.add(new Segment(segment.getText(), null, segment.getControl()));
            }
        }
        return result;
    }

    /**
     * Remove color from segments, preserving other style attributes.
     */
    public static Iterable<Segment> removeColor(Iterable<Segment> segments) {
        List<Segment> result = new ArrayList<>();
        for (Segment segment : segments) {
            Style segStyle = segment.getStyle();
            if (segStyle == null) {
                result.add(segment);
            } else {
                Style noColor = segStyle.withoutColor();
                result.add(new Segment(segment.getText(), noColor, segment.getControl()));
            }
        }
        return result;
    }

    /**
     * Divide segments at the given cell positions.
     *
     * @param segments the segments to divide
     * @param cuts     the cell positions at which to divide
     * @return a list of lists of segments, one more than the number of cuts
     */
    public static List<List<Segment>> divide(Iterable<Segment> segments, Iterable<Integer> cuts) {
        List<List<Segment>> result = new ArrayList<>();
        List<Segment> currentLine = new ArrayList<>();
        int cellOffset = 0;
        Iterator<Integer> cutIter = cuts.iterator();
        int nextCut = cutIter.hasNext() ? cutIter.next() : Integer.MAX_VALUE;

        for (Segment segment : segments) {
            if (segment.isControl()) {
                currentLine.add(segment);
                continue;
            }
            int segLen = segment.cellLength();
            while (cellOffset + segLen > nextCut) {
                int cutInSegment = nextCut - cellOffset;
                Segment[] split = segment.splitCells(cutInSegment);
                if (split[0] != null) {
                    currentLine.add(split[0]);
                }
                result.add(currentLine);
                currentLine = new ArrayList<>();
                cellOffset = nextCut;
                segment = split[1];
                if (segment == null) {
                    segLen = 0;
                    break;
                }
                segLen = segment.cellLength();
                nextCut = cutIter.hasNext() ? cutIter.next() : Integer.MAX_VALUE;
            }
            if (segment != null) {
                currentLine.add(segment);
                cellOffset += segLen;
            }
        }
        result.add(currentLine);
        return result;
    }

    // --- Utility methods ---

    private static String spaces(int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static boolean equals(Object a, Object b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    @Override
    public String toString() {
        if (control != null && !control.isEmpty()) {
            return "Segment(control=" + control + ")";
        }
        if (style != null) {
            return "Segment(" + text + ", " + style + ")";
        }
        return "Segment(" + text + ")";
    }
}
