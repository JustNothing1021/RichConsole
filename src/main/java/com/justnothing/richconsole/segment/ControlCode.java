package com.justnothing.richconsole.segment;

import java.util.Arrays;

/**
 * Represents a terminal control code with optional parameters.
 * Ported from rich/segment.py ControlCode named tuple.
 */
public record ControlCode(ControlType type, int[] params) {

    public ControlCode(ControlType type) {
        this(type, new int[0]);
    }

    public ControlCode(ControlType type, int param) {
        this(type, new int[]{param});
    }

    public ControlCode(ControlType type, int param1, int param2) {
        this(type, new int[]{param1, param2});
    }

    @Override
    public int[] params() {
        return params.clone();
    }

    @Override
    public String toString() {
        return "ControlCode(" + type + ", " + Arrays.toString(params) + ")";
    }
}
