package com.justnothing.richconsole.terminal;

import java.util.*;

import com.justnothing.richconsole.color.ColorTriplet;
import com.justnothing.richconsole.color.Palette;

/**
 * A color theme used when exporting console content.
 * Ported from rich/terminal_theme.py.
 */
public class TerminalTheme {

    private final ColorTriplet backgroundColor;
    private final ColorTriplet foregroundColor;
    private final Palette ansiColors;

    public TerminalTheme(ColorTriplet backgroundColor, ColorTriplet foregroundColor,
                         List<int[]> normal, List<int[]> bright) {
        this.backgroundColor = backgroundColor;
        this.foregroundColor = foregroundColor;
        List<int[]> combined = new ArrayList<>(normal);
        combined.addAll(Objects.requireNonNullElse(bright, normal));
        this.ansiColors = new Palette(Collections.unmodifiableList(combined));
    }

    public ColorTriplet getBackgroundColor() {
        return backgroundColor;
    }

    public ColorTriplet getForegroundColor() {
        return foregroundColor;
    }

    public Palette getAnsiColors() {
        return ansiColors;
    }

    /**
     * Get an ANSI color by index (0-15).
     */
    public ColorTriplet getAnsiColor(int index) {
        return ansiColors.get(index);
    }

    // Default terminal theme
    public static final TerminalTheme DEFAULT = new TerminalTheme(
        new ColorTriplet(255, 255, 255),
        new ColorTriplet(0, 0, 0),
        Arrays.asList(
            new int[]{0, 0, 0},
            new int[]{128, 0, 0},
            new int[]{0, 128, 0},
            new int[]{128, 128, 0},
            new int[]{0, 0, 128},
            new int[]{128, 0, 128},
            new int[]{0, 128, 128},
            new int[]{192, 192, 192}
        ),
        Arrays.asList(
            new int[]{128, 128, 128},
            new int[]{255, 0, 0},
            new int[]{0, 255, 0},
            new int[]{255, 255, 0},
            new int[]{0, 0, 255},
            new int[]{255, 0, 255},
            new int[]{0, 255, 255},
            new int[]{255, 255, 255}
        )
    );

    public static final TerminalTheme MONOKAI = new TerminalTheme( 
        new ColorTriplet(12, 12, 12),
        new ColorTriplet(217, 217, 217),
        Arrays.asList(
            new int[]{26, 26, 26},
            new int[]{244, 0, 95},
            new int[]{152, 224, 36},
            new int[]{253, 151, 31},
            new int[]{157, 101, 255},
            new int[]{244, 0, 95},
            new int[]{88, 209, 235},
            new int[]{196, 197, 181},
            new int[]{98, 94, 76}
        ),
        Arrays.asList(
            new int[]{244, 0, 95},
            new int[]{152, 224, 36},
            new int[]{224, 213, 97},
            new int[]{157, 101, 255},
            new int[]{244, 0, 95},
            new int[]{88, 209, 235},
            new int[]{246, 246, 239}
        )
    );

    public static final TerminalTheme DIMMED_MONOKAI = new TerminalTheme( 
        new ColorTriplet(25, 25, 25),
        new ColorTriplet(185, 188, 186),
        Arrays.asList(
            new int[]{58, 61, 67},
            new int[]{190, 63, 72},
            new int[]{135, 154, 59},
            new int[]{197, 166, 53},
            new int[]{79, 118, 161},
            new int[]{133, 92, 141},
            new int[]{87, 143, 164},
            new int[]{185, 188, 186},
            new int[]{136, 137, 135}
        ),
        Arrays.asList(
            new int[]{251, 0, 31},
            new int[]{15, 114, 47},
            new int[]{196, 112, 51},
            new int[]{24, 109, 227},
            new int[]{251, 0, 103},
            new int[]{46, 112, 109},
            new int[]{253, 255, 185}
        )
    );

    public static final TerminalTheme NIGHT_OWLISH = new TerminalTheme( 
        new ColorTriplet(255, 255, 255),
        new ColorTriplet(64, 63, 83),
        Arrays.asList(
            new int[]{1, 22, 39},
            new int[]{211, 66, 62},
            new int[]{42, 162, 152},
            new int[]{218, 170, 1},
            new int[]{72, 118, 214},
            new int[]{64, 63, 83},
            new int[]{8, 145, 106},
            new int[]{122, 129, 129},
            new int[]{122, 129, 129}
        ),
        Arrays.asList(
            new int[]{247, 110, 110},
            new int[]{73, 208, 197},
            new int[]{218, 194, 107},
            new int[]{92, 167, 228},
            new int[]{105, 112, 152},
            new int[]{0, 201, 144},
            new int[]{152, 159, 177}
        )
    );

    public static final TerminalTheme SVG_EXPORT = new TerminalTheme( 
        new ColorTriplet(41, 41, 41),
        new ColorTriplet(197, 200, 198),
        Arrays.asList(
            new int[]{75, 78, 85},
            new int[]{204, 85, 90},
            new int[]{152, 168, 75},
            new int[]{208, 179, 68},
            new int[]{96, 138, 177},
            new int[]{152, 114, 159},
            new int[]{104, 160, 179},
            new int[]{197, 200, 198},
            new int[]{154, 155, 153}
        ),
        Arrays.asList(
            new int[]{255, 38, 39},
            new int[]{0, 130, 61},
            new int[]{208, 132, 66},
            new int[]{25, 132, 233},
            new int[]{255, 44, 122},
            new int[]{57, 130, 128},
            new int[]{253, 253, 197}
        )
    );
}
