package com.justnothing.richconsole.segment;

/**
 * Enumeration of terminal control codes.
 * Ported from rich/segment.py ControlType IntEnum.
 */
public enum ControlType {
    BELL(1),
    CARRIAGE_RETURN(2),
    HOME(3),
    CLEAR(4),
    SHOW_CURSOR(5),
    HIDE_CURSOR(6),
    ENABLE_ALT_SCREEN(7),
    DISABLE_ALT_SCREEN(8),
    CURSOR_UP(9),
    CURSOR_DOWN(10),
    CURSOR_FORWARD(11),
    CURSOR_BACKWARD(12),
    CURSOR_MOVE_TO_COLUMN(13),
    CURSOR_MOVE_TO(14),
    ERASE_IN_LINE(15),
    SET_WINDOW_TITLE(16);

    private final int value;

    ControlType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
