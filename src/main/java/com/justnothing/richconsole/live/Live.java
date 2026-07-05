package com.justnothing.richconsole.live;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.concurrent.locks.ReentrantLock;

import org.jline.jansi.Ansi;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.RenderHook;
import com.justnothing.richconsole.control.Control;
import com.justnothing.richconsole.segment.Segments;

/**
 * A Live display that auto-updates in the terminal.
 * Ported from rich/live.py Live class.
 *
 * <p>Live renders a renderable and keeps it updated in the terminal.
 * When the display refreshes, it moves the cursor up to overwrite the
 * previous output with new content.</p>
 *
 * <p>Uses JLine's {@link Ansi} builder
 * for generating cursor movement and line erasure sequences.</p>
 */
public class Live implements AutoCloseable, RenderHook {

    // =========================================================================
    // Constants
    // =========================================================================

    private static final String DEFAULT_VERTICAL_OVERFLOW = "ellipsis";
    private static final String STOP_VERTICAL_OVERFLOW = "visible";
    private static final double MILLIS_PER_SECOND = 1000.0;

    // =========================================================================
    // Inner class: RefreshThread
    // =========================================================================

    /**
     * A daemon thread that calls refresh() at regular intervals.
     */
    private static class RefreshThread extends Thread {
        private final Live live;
        private final double refreshPerSecond;
        private volatile boolean done = false;

        RefreshThread(Live live, double refreshPerSecond) {
            this.live = live;
            this.refreshPerSecond = refreshPerSecond;
            setDaemon(true);
        }

        void stopRefresh() {
            done = true;
            interrupt();
        }

        @Override
        public void run() {
            long intervalMs = (long) (MILLIS_PER_SECOND / refreshPerSecond);
            while (!done) {
                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (!done) {
                    live.refresh();
                }
            }
        }
    }

    // =========================================================================
    // Fields
    // =========================================================================

    private Object renderable;
    private final Console console;
    private boolean screen;
    private boolean autoRefresh;
    private double refreshPerSecond;
    private boolean transientMode;
    private String verticalOverflow;
    private boolean started;
    private boolean altScreen;
    private RefreshThread refreshThread;
    private final ReentrantLock lock;
    private Supplier<?> getRenderableCallback;
    private final LiveRender liveRender;

    // =========================================================================
    // Constructors
    // =========================================================================

    /**
     * Full constructor for Live.
     *
     * @param renderable        the initial renderable to display (nullable)
     * @param console           the Console instance to use
     * @param refreshPerSecond  how many times per second to refresh
     * @param transientMode     whether to clear output on stop
     * @param autoRefresh       whether to auto-refresh via background thread
     */
    public Live(Object renderable, Console console, double refreshPerSecond,
                boolean transientMode, boolean autoRefresh) {
        this.renderable = renderable;
        this.console = console;
        this.refreshPerSecond = refreshPerSecond;
        this.transientMode = transientMode;
        this.autoRefresh = autoRefresh;
        this.screen = false;
        this.verticalOverflow = DEFAULT_VERTICAL_OVERFLOW;
        this.started = false;
        this.altScreen = false;
        this.refreshThread = null;
        this.lock = new ReentrantLock();
        this.getRenderableCallback = null;
        this.liveRender = new LiveRender(renderable, null, verticalOverflow);
    }

    /**
     * Create a Live display with a renderable and console.
     *
     * @param renderable the initial renderable to display (nullable)
     * @param console    the Console instance to use
     */
    public Live(Object renderable, Console console) {
        this(renderable, console, 4.0, false, true);
    }

    /**
     * Create a Live display with a renderable and default Console.
     *
     * @param renderable the initial renderable to display (nullable)
     */
    public Live(Object renderable) {
        this(renderable, Console.of(cfg -> {}));
    }

    // =========================================================================
    // Start / Stop
    // =========================================================================

    /**
     * Start the Live display.
     *
     * @param refresh if true, perform an initial refresh
     */
    public void start(boolean refresh) {
        lock.lock();
        try {
            if (started) return;
            started = true;

            // Register with Console
            boolean isTopmost = console.setLive(this);
            if (!isTopmost) {
                // Nested Live — don't start our own refresh thread
                return;
            }

            console.pushRenderHook(this);

            if (screen) {
                altScreen = console.setAltScreen(true);
            }
            console.showCursor(false);
            if (refresh) {
                doRefresh();
            }
            if (autoRefresh) {
                refreshThread = new RefreshThread(this, refreshPerSecond);
                refreshThread.start();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Start the Live display without initial refresh.
     */
    public void start() {
        start(false);
    }

    /**
     * Stop the Live display.
     *
     * <p>Stops the refresh thread, performs a final refresh (if not on alt screen),
     * shows the cursor, and handles transient output cleanup.</p>
     */
    public void stop() {
        lock.lock();
        try {
            if (!started) return;
            started = false;

            // Clear registration with Console
            console.clearLive();
            console.popRenderHook();

            if (autoRefresh && refreshThread != null) {
                refreshThread.stopRefresh();
                refreshThread = null;
            }
            verticalOverflow = STOP_VERTICAL_OVERFLOW;
            try {
                if (!altScreen) {
                    doRefresh();
                }
            } finally {
                console.showCursor(true);
                if (altScreen) {
                    console.setAltScreen(false);
                }
                if (transientMode && !altScreen) {
                    clearRendered();
                }
            }
            if (!transientMode) {
                console.line();
            }
        } finally {
            lock.unlock();
        }
    }

    // =========================================================================
    // AutoCloseable
    // =========================================================================

    @Override
    public void close() {
        stop();
    }

    // =========================================================================
    // Getters
    // =========================================================================

    /**
     * Check if the Live display is started.
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Get the current renderable.
     * If a callback is set, it will be invoked; otherwise the field value is returned.
     */
    public Object getRenderable() {
        if (getRenderableCallback != null) {
            return getRenderableCallback.get();
        }
        return renderable;
    }

    /**
     * Get the Console instance.
     */
    public Console getConsole() {
        return console;
    }

    /**
     * Get the refresh rate per second.
     */
    public double getRefreshPerSecond() {
        return refreshPerSecond;
    }

    /**
     * Check if transient mode is enabled.
     */
    public boolean isTransientMode() {
        return transientMode;
    }

    /**
     * Check if auto-refresh is enabled.
     */
    public boolean isAutoRefresh() {
        return autoRefresh;
    }

    /**
     * Get the vertical overflow mode.
     */
    public String getVerticalOverflow() {
        return verticalOverflow;
    }

    // =========================================================================
    // Setters
    // =========================================================================

    /**
     * Set the callback for obtaining the current renderable.
     */
    public void setGetRenderableCallback(Supplier<?> callback) {
        this.getRenderableCallback = callback;
    }

    /**
     * Set whether to use alternate screen buffer.
     * Must be called before start().
     */
    public void setScreen(boolean screen) {
        this.screen = screen;
        if (screen) {
            this.transientMode = true;
        }
    }

    /**
     * Set whether to auto-refresh.
     */
    public void setAutoRefresh(boolean autoRefresh) {
        this.autoRefresh = autoRefresh;
    }

    /**
     * Set the refresh rate per second.
     */
    public void setRefreshPerSecond(double refreshPerSecond) {
        this.refreshPerSecond = refreshPerSecond;
    }

    /**
     * Set whether to clear output on stop.
     */
    public void setTransientMode(boolean transientMode) {
        this.transientMode = transientMode;
    }

    /**
     * Set the vertical overflow mode ("ellipsis", "visible", "crop").
     */
    public void setVerticalOverflow(String verticalOverflow) {
        this.verticalOverflow = verticalOverflow;
    }

    // =========================================================================
    // Update / Refresh
    // =========================================================================

    /**
     * Update the renderable and optionally refresh the display.
     *
     * @param renderable the new renderable
     * @param refresh    whether to refresh immediately
     */
    public void update(Object renderable, boolean refresh) {
        lock.lock();
        try {
            this.renderable = renderable;
            if (refresh) {
                doRefresh();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Update the renderable without refreshing.
     *
     * @param renderable the new renderable
     */
    public void update(Object renderable) {
        update(renderable, false);
    }

    /**
     * Refresh the display.
     */
    public void refresh() {
        lock.lock();
        try {
            if (!started) return;
            doRefresh();
        } finally {
            lock.unlock();
        }
    }

    // =========================================================================
    // Internal refresh logic
    // =========================================================================

    /**
     * Core refresh implementation. Assumes the lock is already held.
     *
     * <p>Following Python rich's approach: just print an empty Control()
     * through Console.print(). The processRenderables hook will insert
     * position_cursor and liveRender around it, handling all cursor
     * movement and live display rendering.</p>
     */
    private void doRefresh() {
        if (!console.isTerminal() || console.isDumbTerminal()) {
            return;
        }

        // Update liveRender with current renderable
        liveRender.setRenderable(getRenderable());
        liveRender.setVerticalOverflow(verticalOverflow);

        // Just print an empty string — processRenderables does the rest
        // In Python rich, this is console.print(Control()) which is a no-op control
        console.print("");
    }

    /**
     * Clear all previously rendered output.
     * Assumes the lock is already held.
     */
    private void clearRendered() {
        int height = liveRender.getLastRenderHeight();
        if (height > 0) {
            // Print position_cursor to clear the live display area
            console.print(new Segments(liveRender.positionCursor()));
        }
    }

    // =========================================================================
    // RenderHook implementation
    // =========================================================================

    /**
     * Process renderables to restore cursor and display live content.
     *
     * <p>When Console.print() is called during a Live session, this method
     * inserts cursor control sequences to position the print output above
     * the live display, then restores the live display after.</p>
     *
     * <p>Following Python rich's process_renderables exactly:
     * [position_cursor, *renderables, liveRender]</p>
     *
     * @param renderables the list of objects to render
     * @return a new list with cursor control and live renderable inserted
     */
    @Override
    public List<Object> processRenderables(List<Object> renderables) {
        // Note: Do NOT acquire Live.lock here — doRefresh calls console.print()
        // which triggers this method, causing deadlock with different threads.
        // We only read fields, which is safe since they're volatile/set before start.

        if (!console.isTerminal() || console.isDumbTerminal()) {
            // Not interactive — just append the live renderable if finished
            if (!started && !transientMode) {
                List<Object> result = new ArrayList<>(renderables);
                result.add(liveRender);
                return result;
            }
            return renderables;
        }

        // Interactive terminal — insert cursor control and live renderable
        List<Object> result = new ArrayList<>();

        // Update liveRender with current renderable and vertical overflow
        liveRender.setRenderable(getRenderable());
        liveRender.setVerticalOverflow(verticalOverflow);

        // Cursor reset: move to top if alt screen, else position cursor at live top
        if (altScreen) {
            result.add(Control.home());
        } else {
            // Use liveRender's positionCursor to clear previous live display
            result.add(new Segments(liveRender.positionCursor()));
        }

        // Add the user's renderables
        result.addAll(renderables);

        // Add the live renderable to restore display
        result.add(liveRender);

        return result;
    }
}
