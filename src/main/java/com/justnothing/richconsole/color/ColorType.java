package com.justnothing.richconsole.color;

/**
 * Type of color stored in Color class.
 * Ported from rich/color.py ColorType IntEnum.
 */
public enum ColorType {
    DEFAULT(0),
    STANDARD(1),
    EIGHT_BIT(2),
    TRUECOLOR(3),
    WINDOWS(4);

    private final int value;

    ColorType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ColorType." + name();
    }
}
