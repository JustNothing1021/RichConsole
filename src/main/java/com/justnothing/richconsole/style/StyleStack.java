package com.justnothing.richconsole.style;

import java.util.ArrayList;
import java.util.List;

import com.justnothing.richconsole.errors.StyleStackError;

/**
 * A stack of styles for rendering nested styled text.
 * Ported from rich/style_stack.py.
 */
public class StyleStack {

    private final List<Style> stack;

    public StyleStack(Style defaultStyle) {
        this.stack = new ArrayList<>();
        this.stack.add(defaultStyle != null ? defaultStyle : Style.nullStyle());
    }

    /**
     * Get the current (top-of-stack) style.
     */
    public Style current() {
        if (stack.isEmpty()) {
            return Style.nullStyle();
        }
        return stack.get(stack.size() - 1);
    }

    /**
     * Push a style onto the stack. The pushed style is combined with the
     * current style using {@link Style#add(Style)}.
     */
    public void push(Style style) {
        if (style == null) {
            style = Style.nullStyle();
        }
        stack.add(current().add(style));
    }

    /**
     * Pop a style from the stack and return the new current style.
     *
     * @throws StyleStackError if the stack would become empty
     */
    public Style pop() {
        if (stack.size() <= 1) {
            throw new StyleStackError("Unable to pop style stack: stack is empty");
        }
        stack.remove(stack.size() - 1);
        return current();
    }

    /**
     * Get the number of styles on the stack.
     */
    public int size() {
        return stack.size();
    }
}
