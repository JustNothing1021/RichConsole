package com.justnothing.richconsole.traceback;

import java.lang.Thread.UncaughtExceptionHandler;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.traceback.Traceback.Trace;

/**
 * Install a rich traceback handler as the default uncaught exception handler.
 * Ported from rich/traceback.py install() function.
 *
 * <p>Once installed, any uncaught exceptions will be printed with syntax
 * highlighting and rich formatting instead of the default Java stack trace.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * Install.install();
 * // Now any uncaught exception will be rendered with Rich formatting
 * </pre>
 */
public final class Install {

    private Install() {
    }

    /**
     * Install a rich traceback handler as the default uncaught exception handler.
     *
     * @param console     the Console to write exception output to (null for default)
     * @param width       width of the traceback in characters (0 for auto)
     * @param codeWidth   code width in characters (0 for auto)
     * @param extraLines  extra lines of source code around the error line
     * @param theme       syntax highlighting theme name
     * @param wordWrap    enable word wrapping of long lines
     * @param showLocals  enable display of local variables (via reflection)
     * @param maxFrames   maximum number of frames to show (0 for unlimited)
     * @return the previous uncaught exception handler
     */
    public static UncaughtExceptionHandler install(
            Console console,
            int width,
            int codeWidth,
            int extraLines,
            String theme,
            boolean wordWrap,
            boolean showLocals,
            int maxFrames) {

        Console tbConsole = console != null ? console : Console.of(cfg -> {});

        UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                Trace trace = Traceback.fromThrowable(throwable, showLocals);
                Traceback traceback = new Traceback(
                        trace, width, codeWidth, extraLines,
                        theme, wordWrap, showLocals, maxFrames, null);
                tbConsole.print(traceback);
            } catch (Exception e) {
                // Fallback to default printing if rich rendering fails
                throwable.printStackTrace(System.err);
            }

            // Call the previous handler if present
            if (previous != null) {
                previous.uncaughtException(thread, throwable);
            }
        });

        return previous;
    }

    /**
     * Install with default settings.
     *
     * @return the previous uncaught exception handler
     */
    public static UncaughtExceptionHandler install() {
        return install(null, 100, 88, 3, "monokai", true, false, 100);
    }

    /**
     * Install with a specific Console.
     *
     * @param console the Console to write exception output to
     * @return the previous uncaught exception handler
     */
    public static UncaughtExceptionHandler install(Console console) {
        return install(console, 100, 88, 3, "monokai", true, false, 100);
    }
}
