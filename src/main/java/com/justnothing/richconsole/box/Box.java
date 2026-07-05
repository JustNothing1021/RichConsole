package com.justnothing.richconsole.box;

import java.util.*;

import com.justnothing.richconsole.console.ConsoleOptions;

/**
 * Defines characters to render boxes.
 *
 * <pre>
 * ┌─┬┐ top
 * │ ││ head
 * ├─┼┤ head_row
 * │ ││ mid
 * ├─┼┤ row
 * ├─┼┤ foot_row
 * │ ││ foot
 * └─┴┘ bottom
 * </pre>
 *
 * Ported from rich/box.py.
 */
public class Box {

    public static final String HEAD = "head";
    public static final String ROW = "row";
    public static final String MID = "mid";
    public static final String FOOT = "foot";

    private final String boxString;
    public final boolean ascii;

    // Parsed box characters
    public final String topLeft;
    public final String top;
    public final String topDivider;
    public final String topRight;

    public final String headLeft;
    public final String headVertical;
    public final String headRight;

    public final String headRowLeft;
    public final String headRowHorizontal;
    public final String headRowCross;
    public final String headRowRight;

    public final String midLeft;
    public final String midVertical;
    public final String midRight;

    public final String rowLeft;
    public final String rowHorizontal;
    public final String rowCross;
    public final String rowRight;

    public final String footRowLeft;
    public final String footRowHorizontal;
    public final String footRowCross;
    public final String footRowRight;

    public final String footLeft;
    public final String footVertical;
    public final String footRight;

    public final String bottomLeft;
    public final String bottom;
    public final String bottomDivider;
    public final String bottomRight;

    /**
     * Construct a Box from 8 lines of box-drawing characters (non-ASCII by default).
     *
     * @param box 8 newline-separated lines of 4 characters each
     */
    public Box(String box) {
        this(box, false);
    }

    /**
     * Construct a Box from 8 lines of box-drawing characters.
     *
     * @param box   8 newline-separated lines of 4 characters each
     * @param ascii true if this box uses only ASCII characters
     */
    public Box(String box, boolean ascii) {
        this.boxString = box;
        this.ascii = ascii;

        String[] lines = box.split("\n");
        if (lines.length != 8) {
            throw new IllegalArgumentException("Box must have exactly 8 lines, got " + lines.length);
        }

        // Line 1: top
        int[] cp1 = codePoints(lines[0]);
        this.topLeft = str(cp1[0]);
        this.top = str(cp1[1]);
        this.topDivider = str(cp1[2]);
        this.topRight = str(cp1[3]);

        // Line 2: head (second char is same as headVertical, used as separator)
        int[] cp2 = codePoints(lines[1]);
        this.headLeft = str(cp2[0]);
        // cp2[1] is head separator (same as headVertical for non-edge)
        this.headVertical = str(cp2[2]);
        this.headRight = str(cp2[3]);

        // Line 3: head_row
        int[] cp3 = codePoints(lines[2]);
        this.headRowLeft = str(cp3[0]);
        this.headRowHorizontal = str(cp3[1]);
        this.headRowCross = str(cp3[2]);
        this.headRowRight = str(cp3[3]);

        // Line 4: mid
        int[] cp4 = codePoints(lines[3]);
        this.midLeft = str(cp4[0]);
        // cp4[1] is mid-separator
        this.midVertical = str(cp4[2]);
        this.midRight = str(cp4[3]);

        // Line 5: row
        int[] cp5 = codePoints(lines[4]);
        this.rowLeft = str(cp5[0]);
        this.rowHorizontal = str(cp5[1]);
        this.rowCross = str(cp5[2]);
        this.rowRight = str(cp5[3]);

        // Line 6: foot_row
        int[] cp6 = codePoints(lines[5]);
        this.footRowLeft = str(cp6[0]);
        this.footRowHorizontal = str(cp6[1]);
        this.footRowCross = str(cp6[2]);
        this.footRowRight = str(cp6[3]);

        // Line 7: foot
        int[] cp7 = codePoints(lines[6]);
        this.footLeft = str(cp7[0]);
        // cp7[1] is foot separator
        this.footVertical = str(cp7[2]);
        this.footRight = str(cp7[3]);

        // Line 8: bottom
        int[] cp8 = codePoints(lines[7]);
        this.bottomLeft = str(cp8[0]);
        this.bottom = str(cp8[1]);
        this.bottomDivider = str(cp8[2]);
        this.bottomRight = str(cp8[3]);
    }

    private static int[] codePoints(String s) {
        int[] cps = s.codePoints().toArray();
        if (cps.length != 4) {
            throw new IllegalArgumentException(
                "Box line must have exactly 4 characters, got " + cps.length + ": '" + s + "'");
        }
        return cps;
    }

    private static String str(int codePoint) {
        return new String(Character.toChars(codePoint));
    }

    /**
     * Get the top border of a simple box.
     *
     * @param widths widths of columns
     * @return a string of box characters
     */
    public String getTop(List<Integer> widths) {
        StringBuilder sb = new StringBuilder();
        sb.append(topLeft);
        for (int i = 0; i < widths.size(); i++) {
            int width = widths.get(i);
            for (int j = 0; j < width; j++) sb.append(top);
            if (i < widths.size() - 1) sb.append(topDivider);
        }
        sb.append(topRight);
        return sb.toString();
    }

    /**
     * Get a row border of a box.
     *
     * @param widths widths of columns
     * @param level  one of "head", "row", "foot", "mid"
     * @param edge   whether to include left and right edge characters
     * @return a string of box characters
     */
    public String getRow(List<Integer> widths, String level, boolean edge) {
        String left;
        String horizontal;
        String cross;
        String right;

        switch (level) {
            case HEAD:
                left = headRowLeft;
                horizontal = headRowHorizontal;
                cross = headRowCross;
                right = headRowRight;
                break;
            case ROW:
                left = rowLeft;
                horizontal = rowHorizontal;
                cross = rowCross;
                right = rowRight;
                break;
            case MID:
                left = midLeft;
                horizontal = " ";
                cross = midVertical;
                right = midRight;
                break;
            case FOOT:
                left = footRowLeft;
                horizontal = footRowHorizontal;
                cross = footRowCross;
                right = footRowRight;
                break;
            default:
                throw new IllegalArgumentException("level must be one of" + Arrays.toString(new String[] {HEAD, ROW, MID, FOOT}));
        }

        StringBuilder sb = new StringBuilder();
        if (edge) {
            sb.append(left);
        }
        for (int i = 0; i < widths.size(); i++) {
            int width = widths.get(i);
            for (int j = 0; j < width; j++) sb.append(horizontal);
            if (i < widths.size() - 1) sb.append(cross);
        }
        if (edge) {
            sb.append(right);
        }
        return sb.toString();
    }

    /**
     * Get the bottom border of a simple box.
     *
     * @param widths widths of columns
     * @return a string of box characters
     */
    public String getBottom(List<Integer> widths) {
        StringBuilder sb = new StringBuilder();
        sb.append(bottomLeft);
        for (int i = 0; i < widths.size(); i++) {
            int width = widths.get(i);
            for (int j = 0; j < width; j++) {
                sb.append(bottom);
            }
            if (i < widths.size() - 1) {
                sb.append(bottomDivider);
            }
        }
        sb.append(bottomRight);
        return sb.toString();
    }

    @Override
    public String toString() {
        return boxString;
    }

    // =========================================================================
    // Static Box instances
    // =========================================================================

    public static final Box ASCII = new Box( 
            """
                +--+
                | ||
                |-+|
                | ||
                |-+|
                |-+|
                | ||
                +--+
                
                """,
        true
    );

    public static final Box ASCII2 = new Box(
        """
            +-++
            | ||
            +-++
            | ||
            +-++
            +-++
            | ||
            +-++
            """,
        true
    );

    public static final Box ASCII_DOUBLE_HEAD = new Box(
        """
            +-++
            | ||
            +=++
            | ||
            +-++
            +-++
            | ||
            +-++
            """,
        true
    );

    public static final Box SQUARE = new Box(
            """
            ┌─┬┐
            │ ││
            ├─┼┤
            │ ││
            ├─┼┤
            ├─┼┤
            │ ││
            └─┴┘
            """
    );

    public static final Box SQUARE_DOUBLE_HEAD = new Box(
            """
            ┌─┬┐
            │ ││
            ╞═╪╡
            │ ││
            ├─┼┤
            ├─┼┤
            │ ││
            └─┴┘
            """
    );

    public static final Box MINIMAL = new Box(
            """
              ╷\s
              │\s
            ╶─┼╴
              │\s
            ╶─┼╴
            ╶─┼╴
              │\s
              ╴\s
            """
    );

    public static final Box MINIMAL_HEAVY_HEAD = new Box(
            """
              ╷\s
              │\s
            ╺━╿╸
              │\s
            ╶─┼╴
            ╶─┼╴
              │\s
              ╴\s
            """
    );

    public static final Box MINIMAL_DOUBLE_HEAD = new Box(
            """
              ╷\s
              │\s
             ═╪\s
              │\s
             ─┼\s
             ─┼\s
              │\s
              ╵\s
            """
    );

    public static final Box SIMPLE = new Box(
            """
               \s
               \s
             ──\s
               \s
               \s
             ──\s
               \s
               \s
            """
    );

    public static final Box SIMPLE_HEAD = new Box(
            """
               \s
               \s
             ──\s
               \s
               \s
               \s
               \s
               \s
            """
    );

    public static final Box SIMPLE_HEAVY = new Box(
            """
               \s
               \s
             ━━\s
               \s
               \s
             ━━\s
               \s
               \s
            """
    );

    public static final Box HORIZONTALS = new Box(
            """
             ──\s
               \s
             ──\s
               \s
             ──\s
             ──\s
               \s
             ──\s
            """
    );

    public static final Box ROUNDED = new Box(
            """
            ╭─┬╮
            │ ││
            ├─┼┤
            │ ││
            ├─┼┤
            ├─┼┤
            │ ││
            ╰─┴╯
            """
    );

    public static final Box HEAVY = new Box(
            """
            ┏━┳┓
            ┃ ┃┃
            ┣━╋┫
            ┃ ┃┃
            ┣━╋┫
            ┣━╋┫
            ┃ ┃┃
            ┗━┻┛
            """
    );

    public static final Box HEAVY_EDGE = new Box(
            """
            ┏━┯┓
            ┃ │┃
            ┠─┼┨
            ┃ │┃
            ┠─┼┨
            ┠─┼┨
            ┃ │┃
            ┗━┷┛
            """
    );

    public static final Box HEAVY_HEAD = new Box(
            """
            ┏━┳┓
            ┃ ┃┃
            ┡━┇┩
            │ ││
            ├─┼┤
            ├─┼┤
            │ ││
            └─┴┘
            """
    );

    public static final Box DOUBLE = new Box(
            """
            ╔═╦╗
            ║ ║║
            ╠═╬╣
            ║ ║║
            ╠═╬╣
            ╠═╬╣
            ║ ║║
            ╚═╩╝
            """
    );

    public static final Box DOUBLE_EDGE = new Box(
            """
            ╔═╤╗
            ║ │║
            ╟─┼╢
            ║ │║
            ╟─┼╢
            ╟─┼╢
            ║ │║
            ╚═╧╝
            """
    );

    public static final Box MARKDOWN = new Box( 
        """
               \s
            | ||
            |-||
            | ||
            |-||
            |-||
            | ||
               \s
            """,
        true
    );

    // =========================================================================
    // Substitution maps
    // =========================================================================

    /**
     * Map Boxes that don't render with raster fonts on Windows to equivalents that do.
     */
    private static final Map<Box, Box> LEGACY_WINDOWS_SUBSTITUTIONS;

    /**
     * Map headed boxes to their headerless equivalents.
     */
    public static final Map<Box, Box> PLAIN_HEADED_SUBSTITUTIONS;

    static {
        Map<Box, Box> lw = new LinkedHashMap<>();
        lw.put(SQUARE, ASCII);
        lw.put(MINIMAL, ASCII);
        lw.put(SIMPLE, ASCII);
        // ROUNDED maps to SQUARE in legacy Windows
        lw.put(ROUNDED, SQUARE);
        lw.put(HEAVY, SQUARE);
        lw.put(HEAVY_HEAD, SQUARE);
        lw.put(DOUBLE, SQUARE);
        LEGACY_WINDOWS_SUBSTITUTIONS = Collections.unmodifiableMap(lw);

        Map<Box, Box> ph = new LinkedHashMap<>();
        ph.put(HEAVY_HEAD, SQUARE);
        ph.put(SQUARE_DOUBLE_HEAD, SQUARE);
        ph.put(MINIMAL_DOUBLE_HEAD, MINIMAL);
        ph.put(MINIMAL_HEAVY_HEAD, MINIMAL);
        ph.put(ASCII_DOUBLE_HEAD, ASCII2);
        PLAIN_HEADED_SUBSTITUTIONS = Collections.unmodifiableMap(ph);
    }

    /**
     * Substitute this box with an appropriate alternative based on console options.
     * If legacy Windows mode is active and safe is true, use Windows-compatible box characters.
     * If ASCII-only mode is active and this box is not ASCII, use the ASCII box.
     */
    public Box substitute(ConsoleOptions options, boolean safe) {
        Box result = this;
        if (options.isLegacyWindows() && safe) {
            Box substituted = LEGACY_WINDOWS_SUBSTITUTIONS.get(this);
            if (substituted != null) {
                result = substituted;
            }
        }
        if (options.isAsciiOnly() && !this.ascii) {
            result = ASCII;
        }
        return result;
    }

    @Override
    public int hashCode() {
        return boxString.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Box)) return false;
        return boxString.equals(((Box) obj).boxString);
    }
}
