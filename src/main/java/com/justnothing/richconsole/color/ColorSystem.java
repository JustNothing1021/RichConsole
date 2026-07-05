package com.justnothing.richconsole.color;

/**
 * One of the color systems supported by terminals.
 * Ported from rich/color.py ColorSystem IntEnum.
 */
public enum ColorSystem {
    STANDARD(1),
    EIGHT_BIT(2),
    TRUECOLOR(3),
    WINDOWS(4);

    private final int value;

    ColorSystem(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ColorSystem." + name();
    }
}
