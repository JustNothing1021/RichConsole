package com.justnothing.richconsole.progressbar;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.color.Color;
import com.justnothing.richconsole.color.ColorSystem;
import com.justnothing.richconsole.color.ColorTriplet;
import com.justnothing.richconsole.color.ColorUtils;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.style.Style;

/**
 * A progress bar widget.
 * Ported from rich/progress_bar.py.
 */
public class ProgressBar implements RichRenderable {

    // =========================================================================
    // Constants
    // =========================================================================

    private static final int PULSE_SIZE = 20;
    private static final int PULSE_ANIMATION_SPEED = 15;

    // Bar characters
    private static final String UNICODE_BAR = "━";
    private static final String ASCII_BAR = "-";
    private static final String UNICODE_HALF_BAR_RIGHT = "╸";
    private static final String UNICODE_HALF_BAR_LEFT = "╺";
    private static final String ASCII_HALF_BAR_RIGHT = " ";
    private static final String ASCII_HALF_BAR_LEFT = " ";
    private static final String NO_COLOR_BAR_PLACEHOLDER = " ";

    // Default style names
    private static final String DEFAULT_BAR_BACK_STYLE = "bar.back";
    private static final String DEFAULT_BAR_COMPLETE_STYLE = "bar.complete";
    private static final String DEFAULT_BAR_FINISHED_STYLE = "bar.finished";
    private static final String DEFAULT_BAR_PULSE_STYLE = "bar.pulse";

    // Default color triplets
    private static final ColorTriplet DEFAULT_FORE_COLOR = new ColorTriplet(255, 0, 255);
    private static final ColorTriplet DEFAULT_BACK_COLOR = new ColorTriplet(0, 0, 0);

    // Nanos per second
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private Double total;
    private double completed;
    private final Integer width;
    private final boolean pulse;
    private final Object style;
    private final Object completeStyle;
    private final Object finishedStyle;
    private final Object pulseStyle;
    private final Double animationTime;
    private List<Segment> pulseSegments;

    // =========================================================================
    // Config — fluent configuration for ProgressBar construction
    // =========================================================================

    /**
     * Fluent configuration object for ProgressBar construction.
     * Usage: {@code ProgressBar.of(cfg -> cfg.total(200).completed(50).pulse(true))}
     */
    public static class Config {
        public double total = 100.0;
        public double completed = 0.0;
        public Integer width;
        public boolean pulse;
        public Object style = DEFAULT_BAR_BACK_STYLE;
        public Object completeStyle = DEFAULT_BAR_COMPLETE_STYLE;
        public Object finishedStyle = DEFAULT_BAR_FINISHED_STYLE;
        public Object pulseStyle = DEFAULT_BAR_PULSE_STYLE;
        public Double animationTime;

        public Config total(double total) { this.total = total; return this; }
        public Config completed(double completed) { this.completed = completed; return this; }
        public Config width(int width) { this.width = width; return this; }
        public Config pulse(boolean pulse) { this.pulse = pulse; return this; }
        public Config style(Object style) { this.style = style; return this; }
        public Config completeStyle(Object completeStyle) { this.completeStyle = completeStyle; return this; }
        public Config finishedStyle(Object finishedStyle) { this.finishedStyle = finishedStyle; return this; }
        public Config pulseStyle(Object pulseStyle) { this.pulseStyle = pulseStyle; return this; }
        public Config animationTime(double animationTime) { this.animationTime = animationTime; return this; }
    }

    /**
     * Create a ProgressBar with a Config consumer for fluent configuration.
     * <pre>{@code
     * ProgressBar.of(cfg -> cfg.total(200).completed(50).pulse(true))
     * }</pre>
     */
    public static ProgressBar of(Consumer<Config> configurer) {
        Config cfg = new Config();
        configurer.accept(cfg);
        return new ProgressBar(cfg);
    }

    private ProgressBar(Config cfg) {
        this(cfg.total, cfg.completed, cfg.width, cfg.pulse,
             cfg.style, cfg.completeStyle, cfg.finishedStyle,
             cfg.pulseStyle, cfg.animationTime);
    }

    public ProgressBar() {
        this(100.0, 0, null, false,
             DEFAULT_BAR_BACK_STYLE, DEFAULT_BAR_COMPLETE_STYLE,
             DEFAULT_BAR_FINISHED_STYLE, DEFAULT_BAR_PULSE_STYLE, null);
    }

    public ProgressBar(double total, double completed, Integer width, boolean pulse,
                       Object style, Object completeStyle, Object finishedStyle,
                       Object pulseStyle, Double animationTime) {
        this.total = total;
        this.completed = completed;
        this.width = width;
        this.pulse = pulse;
        this.style = style;
        this.completeStyle = completeStyle;
        this.finishedStyle = finishedStyle;
        this.pulseStyle = pulseStyle;
        this.animationTime = animationTime;
        this.pulseSegments = null;
    }

    public Double percentageCompleted() {
        if (total == null) {
            return null;
        }
        double completed = (this.completed / total) * 100.0;
        completed = Math.min(100, Math.max(0.0, completed));
        return completed;
    }

    public void update(double completed, Double total) {
        this.completed = completed;
        if (total != null) {
            this.total = total;
        }
    }

    private List<Segment> getPulseSegments(Style foreStyle, Style backStyle,
                                           ColorSystem colorSystem, boolean noColor, boolean ascii) {
        if (pulseSegments != null) {
            return pulseSegments;
        }

        String bar = ascii ? ASCII_BAR : UNICODE_BAR;
        List<Segment> segments = new ArrayList<>();

        if (!(colorSystem == ColorSystem.STANDARD
                || colorSystem == ColorSystem.EIGHT_BIT
                || colorSystem == ColorSystem.TRUECOLOR) || noColor) {
            int foreCount = PULSE_SIZE / 2;
            int backCount = PULSE_SIZE - foreCount;
            Segment foreSeg = new Segment(bar, foreStyle);
            Segment backSeg = new Segment(noColor ? NO_COLOR_BAR_PLACEHOLDER : bar, backStyle);
            for (int i = 0; i < foreCount; i++) {
                segments.add(foreSeg);
            }
            for (int i = 0; i < backCount; i++) {
                segments.add(backSeg);
            }
            pulseSegments = segments;
            return segments;
        }

        // Blend colors for smooth pulse effect
        // Matches Python rich: uses color (foreground) of both styles
        Color foreColor = foreStyle != null ? foreStyle.getColor() : null;
        ColorTriplet foreTriplet;
        if (foreColor != null && !foreColor.isDefault()) {
            foreTriplet = foreColor.getTruecolor(null, true);
        } else {
            foreTriplet = DEFAULT_FORE_COLOR;
        }

        Color backColor = backStyle != null ? backStyle.getColor() : null;
        ColorTriplet backTriplet;
        if (backColor != null && !backColor.isDefault()) {
            backTriplet = backColor.getTruecolor(null, true);
        } else {
            backTriplet = DEFAULT_BACK_COLOR;
        }

        for (int index = 0; index < PULSE_SIZE; index++) {
            double position = (double) index / PULSE_SIZE;
            float fade = (float) (0.5 + Math.cos(position * Math.PI * 2) / 2.0);
            ColorTriplet color = ColorUtils.blendRgb(foreTriplet, backTriplet, fade);
            segments.add(new Segment(bar, Style.builder().color(Color.fromTriplet(color)).build()));
        }

        pulseSegments = segments;
        return segments;
    }

    private Iterable<Segment> renderPulse(Console console, int width, boolean ascii) {
        Style foreStyle = console.getStyle(pulseStyle, "white");
        Style backStyle = console.getStyle(style, "black");
        ColorSystem colorSystem = console.getColorSystemEnum();
        boolean noColor = console.isNoColor();

        List<Segment> pulseSegs = getPulseSegments(foreStyle, backStyle, colorSystem, noColor, ascii);
        int segmentCount = pulseSegs.size();

        double currentTime = animationTime != null
                ? animationTime
                : System.nanoTime() / NANOS_PER_SECOND;

        int repeatCount = (width / segmentCount) + 2;
        List<Segment> allSegments = new ArrayList<>(repeatCount * segmentCount);
        for (int i = 0; i < repeatCount; i++) {
            allSegments.addAll(pulseSegs);
        }

        int offset = (int) (-currentTime * PULSE_ANIMATION_SPEED) % segmentCount;
        if (offset < 0) {
            offset += segmentCount;
        }

        int end = Math.min(offset + width, allSegments.size());
        return new ArrayList<>(allSegments.subList(offset, end));
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        int width = Math.min(this.width != null ? this.width : options.getMaxWidth(),
                             options.getMaxWidth());
        boolean ascii = options.isLegacyWindows() || options.isAsciiOnly();
        boolean shouldPulse = pulse || total == null || total <= 0.0;

        if (shouldPulse) {
            return renderPulse(console, width, ascii);
        }

        double completed = Math.min(total, Math.max(0, this.completed));
        String bar = ascii ? ASCII_BAR : UNICODE_BAR;
        String halfBarRight = ascii ? ASCII_HALF_BAR_RIGHT : UNICODE_HALF_BAR_RIGHT;
        String halfBarLeft = ascii ? ASCII_HALF_BAR_LEFT : UNICODE_HALF_BAR_LEFT;

        int completeHalves;
        if (total != 0) {
            completeHalves = (int) (width * 2 * completed / total);
        } else {
            completeHalves = width * 2;
        }

        int barCount = completeHalves / 2;
        int halfBarCount = completeHalves % 2;

        Style styleObj = console.getStyle(style);
        boolean isFinished = total == null || this.completed >= total;
        Object completeStyleKey = isFinished ? finishedStyle : completeStyle;
        Style completeStyleObj = console.getStyle(completeStyleKey);

        List<Segment> result = new ArrayList<>();

        if (barCount > 0) {
            StringBuilder sb = new StringBuilder(barCount);
            for (int i = 0; i < barCount; i++) {
                sb.append(bar);
            }
            result.add(new Segment(sb.toString(), completeStyleObj));
        }

        if (halfBarCount > 0) {
            result.add(new Segment(halfBarRight, completeStyleObj));
        }

        if (!console.isNoColor()) {
            int remainingBars = width - barCount - halfBarCount;
            if (remainingBars > 0 && console.getColorSystemEnum() != null) {
                if (halfBarCount == 0 && barCount > 0) {
                    result.add(new Segment(halfBarLeft, styleObj));
                    remainingBars -= 1;
                }
                if (remainingBars > 0) {
                    StringBuilder sb = new StringBuilder(remainingBars);
                    for (int i = 0; i < remainingBars; i++) {
                        sb.append(bar);
                    }
                    result.add(new Segment(sb.toString(), styleObj));
                }
            }
        }

        return result;
    }
}
