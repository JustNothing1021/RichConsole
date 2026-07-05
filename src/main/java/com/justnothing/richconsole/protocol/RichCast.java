package com.justnothing.richconsole.protocol;

/**
 * The RichCast protocol interface.
 * Ported from rich/protocol.py __rich__ protocol.
 *
 * <p>Objects implementing this interface can return a rich representation
 * of themselves for rendering.</p>
 */
public interface RichCast {
    /**
     * Return a rich representation of this object.
     *
     * @return a renderable object
     */
    Object rich();
}
