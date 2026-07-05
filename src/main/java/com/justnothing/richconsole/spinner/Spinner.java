package com.justnothing.richconsole.spinner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.text.Text;

/**
 * An animated spinner display.
 * Ported from rich/spinner.py Spinner class and rich/_spinners.py.
 */
public class Spinner implements RichRenderable {

        // =========================================================================
        // Constants
        // =========================================================================

        private static final double NANOS_PER_SECOND = 1_000_000_000.0;
        private static final double MILLIS_PER_SECOND = 1000.0;

        // =========================================================================
        // Spinner definition record
        // =========================================================================

        /**
         * A spinner definition containing the interval (ms) between frames
         * and the list of frame strings.
         */
        public record SpinnerDef(int interval, List<String> frames) {
        }

        // =========================================================================
        // SPINNERS registry — ported from rich/_spinners.py
        // =========================================================================

        /** Pre-defined spinner frame sequences. */
        private static final Map<String, SpinnerDef> SPINNERS;

        static {
                Map<String, SpinnerDef> m = new LinkedHashMap<>();

                // --- dots family ---
                m.put("dots", new SpinnerDef(80, Arrays.asList(
                                "⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")));
                m.put("dots2", new SpinnerDef(80, Arrays.asList(
                                "⣾", "⣽", "⣻", "⢿", "⡿", "⣟", "⣯", "⣷")));
                m.put("dots3", new SpinnerDef(80, Arrays.asList(
                                "⠋", "⠙", "⠚", "⠞", "⠖", "⠦", "⠴", "⠲", "⠳", "⠓")));
                m.put("dots4", new SpinnerDef(80, Arrays.asList(
                                "⠄", "⠆", "⠇", "⠋", "⠙", "⠸", "⠰", "⠠",
                                "⠰", "⠸", "⠙", "⠋", "⠇", "⠆")));
                m.put("dots5", new SpinnerDef(80, Arrays.asList(
                                "⠋", "⠙", "⠚", "⠒", "⠂", "⠂", "⠒", "⠲",
                                "⠴", "⠦", "⠖", "⠒", "⠐", "⠐", "⠒", "⠓", "⠋")));
                m.put("dots6", new SpinnerDef(80, Arrays.asList(
                                "⠁", "⠉", "⠙", "⠚", "⠒", "⠂", "⠂", "⠒",
                                "⠲", "⠴", "⠤", "⠄", "⠄", "⠤", "⠴", "⠲",
                                "⠒", "⠂", "⠂", "⠒", "⠚", "⠙", "⠉", "⠁")));
                m.put("dots7", new SpinnerDef(80, Arrays.asList(
                                "⠈", "⠉", "⠋", "⠓", "⠒", "⠐", "⠐", "⠒",
                                "⠖", "⠦", "⠤", "⠠", "⠠", "⠤", "⠦", "⠖",
                                "⠒", "⠐", "⠐", "⠒", "⠓", "⠋", "⠉", "⠈")));
                m.put("dots8", new SpinnerDef(80, Arrays.asList(
                                "⠁", "⠁", "⠉", "⠙", "⠚", "⠒", "⠂", "⠂",
                                "⠒", "⠲", "⠴", "⠤", "⠄", "⠄", "⠤", "⠠",
                                "⠠", "⠤", "⠦", "⠖", "⠒", "⠐", "⠐", "⠒",
                                "⠓", "⠋", "⠉", "⠈", "⠈")));
                m.put("dots9", new SpinnerDef(80, Arrays.asList(
                                "⢹", "⢺", "⢼", "⣸", "⣇", "⡧", "⡗", "⡏")));
                m.put("dots10", new SpinnerDef(80, Arrays.asList(
                                "⢄", "⢂", "⢁", "⡁", "⡈", "⡐", "⡠")));
                m.put("dots11", new SpinnerDef(100, Arrays.asList(
                                "⠁", "⠂", "⠄", "⡀", "⢀", "⠠", "⠐", "⠈")));
                m.put("dots12", new SpinnerDef(80, Arrays.asList(
                                "⢀⠀", "⡀⠀", "⠄⠀", "⢂⠀", "⡂⠀", "⠅⠀",
                                "⢃⠀", "⡃⠀", "⠍⠀", "⢋⠀", "⡋⠀", "⠍⠁",
                                "⢋⠁", "⡋⠁", "⠍⠉", "⠋⠉", "⠋⠉", "⠉⠙",
                                "⠉⠙", "⠉⠩", "⠈⢙", "⠈⡙", "⢈⠩", "⡀⢙",
                                "⠄⡙", "⢂⠩", "⡂⢘", "⠅⡘", "⢃⠨", "⡃⢐",
                                "⠍⡐", "⢋⠠", "⡋⢀", "⠍⡁", "⢋⠁", "⡋⠁",
                                "⠍⠉", "⠋⠉", "⠋⠉", "⠉⠙", "⠉⠙", "⠉⠩",
                                "⠈⢙", "⠈⡙", "⠈⠩", "⠀⢙", "⠀⡙", "⠀⠩",
                                "⠀⢘", "⠀⡘", "⠀⠨", "⠀⢐", "⠀⡐", "⠀⠠",
                                "⠀⢀", "⠀⡀")));

                // --- line / pipe ---
                m.put("line", new SpinnerDef(130, Arrays.asList("-", "\\", "|", "/")));
                m.put("line2", new SpinnerDef(100, Arrays.asList("⠂", "-", "–", "—", "–", "-")));
                m.put("pipe", new SpinnerDef(100, Arrays.asList("┤", "┘", "┴", "└", "├", "┌", "┬", "┐")));

                // --- simple dots ---
                m.put("simpleDots", new SpinnerDef(400, Arrays.asList(".  ", ".. ", "...", "   ")));
                m.put("simpleDotsScrolling", new SpinnerDef(200, Arrays.asList(
                                ".  ", ".. ", "...", " ..", "  .", "   ")));

                // --- star ---
                m.put("star", new SpinnerDef(70, Arrays.asList("✶", "✸", "✹", "✺", "✹", "✷")));
                m.put("star2", new SpinnerDef(80, Arrays.asList("+", "x", "*")));

                // --- flip / hamburger ---
                m.put("flip", new SpinnerDef(70, Arrays.asList(
                                "_", "_", "_", "-", "`", "`", "'", "´", "-", "_", "_", "_")));
                m.put("hamburger", new SpinnerDef(100, Arrays.asList("☱", "☲", "☴")));

                // --- grow ---
                m.put("growVertical", new SpinnerDef(120, Arrays.asList(
                                "▁", "▃", "▄", "▅", "▆", "▇", "█", "▇", "▆", "▅", "▄", "▃")));
                m.put("growHorizontal", new SpinnerDef(120, Arrays.asList(
                                "▏", "▎", "▍", "▌", "▋", "▊", "▉", "▊", "▋", "▌", "▍", "▎")));

                // --- balloon ---
                m.put("balloon", new SpinnerDef(140, Arrays.asList(" ", ".", "o", "O", "@", "*", " ")));
                m.put("balloon2", new SpinnerDef(120, Arrays.asList(".", "o", "O", "°", "O", "o", ".")));

                // --- bounce ---
                m.put("bounce", new SpinnerDef(120, Arrays.asList("⠁", "⠂", "⠄", "⡀", "⢀", "⠠", "⠐", "⠈")));
                m.put("bounce2", new SpinnerDef(80, Arrays.asList(
                                "⠁", "⠂", "⠄", "⠂", "⠠", "⠂", "⠄", "⠂")));

                // --- arc / circle ---
                m.put("arc", new SpinnerDef(100, Arrays.asList("◜", "◠", "◝", "◞", "◡", "◟")));
                m.put("circle", new SpinnerDef(120, Arrays.asList("◡", "⊙", "◠")));
                m.put("circleQuarters", new SpinnerDef(120, Arrays.asList("◴", "◷", "◶", "◵")));
                m.put("circleHalves", new SpinnerDef(50, Arrays.asList("◐", "◓", "◑", "◒")));

                // --- square ---
                m.put("squareCorners", new SpinnerDef(180, Arrays.asList("◰", "◳", "◲", "◱")));
                m.put("square", new SpinnerDef(120, Arrays.asList("▖", "▘", "▝", "▗")));

                // --- triangle ---
                m.put("triangle", new SpinnerDef(50, Arrays.asList("◢", "◣", "◤", "◥")));

                // --- aesthetic ---
                m.put("aesthetic", new SpinnerDef(80, Arrays.asList(
                                "▰▱▱▱▱▱▱", "▰▰▱▱▱▱▱", "▰▰▰▱▱▱▱", "▰▰▰▰▱▱▱",
                                "▰▰▰▰▰▱▱", "▰▰▰▰▰▰▱", "▰▰▰▰▰▰▰", "▱▰▰▰▰▰▰",
                                "▱▱▰▰▰▰▰", "▱▱▱▰▰▰▰", "▱▱▱▱▰▰▰", "▱▱▱▱▱▰▰",
                                "▱▱▱▱▱▱▰", "▱▱▱▱▱▱▱")));

                // --- waveform ---
                m.put("waveform", new SpinnerDef(80, Arrays.asList(
                                "▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁", "▁▂▃▄▅▆▇█▇▆▅▄▃▂▁",
                                "▁▂▃▄▅▆▇█▇▆▅▄▃▂▁", "▁▂▃▄▅▆▇█▇▆▅▄▃▂▁")));

                // --- weather ---
                m.put("weather", new SpinnerDef(100, Arrays.asList(
                                "☀️ ", "☀️ ", "☀️ ", "🌤 ", "⛅️ ", "🌥 ",
                                "☁️ ", "🌧 ", "🌨 ", "🌧 ", "🌨 ", "🌧 ",
                                "🌨 ", "⛈ ", "🌨 ", "🌧 ", "🌨 ", "☁️ ",
                                "🌥 ", "⛅️ ", "🌤 ", "☀️ ", "☀️ ")));

                // --- time travel ---
                m.put("timeTravel", new SpinnerDef(100, Arrays.asList(
                                "🕐 ", "🕐 ", "🕑 ", "🕒 ", "🕓 ", "🕔 ",
                                "🕕 ", "🕖 ", "🕗 ", "🕘 ", "🕙 ", "🕚 ")));

                // --- noise ---
                m.put("noise", new SpinnerDef(100, Arrays.asList("▓", "▒", "░")));

                // --- binary ---
                m.put("binary", new SpinnerDef(80, Arrays.asList(
                                "010010", "001100", "100101", "111010", "111101",
                                "010111", "101011", "111000", "110011", "110101")));

                // --- arrow ---
                m.put("arrow", new SpinnerDef(100, Arrays.asList(
                                "←", "↖", "↑", "↗", "→", "↘", "↓", "↙")));

                // --- bouncing ---
                m.put("bouncingBar", new SpinnerDef(80, Arrays.asList(
                                "[    ]", "[=   ]", "[==  ]", "[=== ]", "[====]",
                                "[ ===]", "[  ==]", "[   =]", "[    ]", "[   =]",
                                "[  ==]", "[ ===]", "[====]", "[=== ]", "[==  ]", "[=   ]")));
                m.put("bouncingBall", new SpinnerDef(80, Arrays.asList(
                                "( ●    )", "(  ●   )", "(   ●  )", "(    ● )",
                                "(     ●)", "(    ● )", "(   ●  )", "(  ●   )",
                                "( ●    )", "(●     )")));

                // --- toggles ---
                m.put("toggle", new SpinnerDef(250, Arrays.asList("⊶", "⊷")));
                m.put("toggle2", new SpinnerDef(80, Arrays.asList("▫", "▪")));
                m.put("toggle3", new SpinnerDef(120, Arrays.asList("□", "■")));

                // --- point / layer ---
                m.put("point", new SpinnerDef(125, Arrays.asList("∙∙∙", "●∙∙", "∙●∙", "∙∙●", "∙∙∙")));
                m.put("layer", new SpinnerDef(150, Arrays.asList("-", "=", "≡")));

                // --- betaWave ---
                m.put("betaWave", new SpinnerDef(80, Arrays.asList(
                                "ρββββββ", "βρβββββ", "ββρββββ", "βββρβββ",
                                "ββββρββ", "βββββρβ", "ββββββρ")));

                // --- clock ---
                m.put("clock", new SpinnerDef(100, Arrays.asList(
                                "🕛 ", "🕐 ", "🕑 ", "🕒 ", "🕓 ", "🕔 ",
                                "🕕 ", "🕖 ", "🕗 ", "🕘 ", "🕙 ", "🕚 ")));

                // --- earth ---
                m.put("earth", new SpinnerDef(180, Arrays.asList("🌍 ", "🌎 ", "🌏 ")));

                // --- moon ---
                m.put("moon", new SpinnerDef(80, Arrays.asList(
                                "🌑 ", "🌒 ", "🌓 ", "🌔 ", "🌕 ", "🌖 ", "🌗 ", "🌘 ")));

                // --- dqpb ---
                m.put("dqpb", new SpinnerDef(100, Arrays.asList("d", "q", "p", "b")));

                SPINNERS = Collections.unmodifiableMap(m);
        }

        // =========================================================================
        // Fields
        // =========================================================================

        private final String name;
        private Object text;
        private final List<String> frames;
        private final int interval;
        private Double startTime;
        private Object style;
        private double speed;
        private double frameNoOffset;
        private double updateSpeed;

        // =========================================================================
        // Config
        // =========================================================================

        /**
         * Fluent configuration object for Spinner construction.
         * Usage: {@code Spinner.of(cfg -> cfg.name("dots").text("Loading...").speed(2.0))}
         */
        public static class Config {
                public String name = "dots";
                public Object text;
                public Object style;
                public double speed = 1.0;

                public Config name(String name) { this.name = name; return this; }
                public Config text(Object text) { this.text = text; return this; }
                public Config style(Object style) { this.style = style; return this; }
                public Config speed(double speed) { this.speed = speed; return this; }
        }

        // =========================================================================
        // Factory methods
        // =========================================================================

        /**
         * Create a Spinner with fluent configuration (name defaults to "dots").
         * <pre>{@code
         * Spinner.of(cfg -> cfg.text("Loading...").speed(2.0))
         * }</pre>
         *
         * @param configurer  a consumer that configures the Spinner options
         * @return a new Spinner instance
         */
        public static Spinner of(Consumer<Config> configurer) {
                Config cfg = new Config();
                configurer.accept(cfg);
                return new Spinner(cfg);
        }

        /**
         * Create a Spinner with a specific name and fluent configuration.
         * <pre>{@code
         * Spinner.of("bouncingBar", cfg -> cfg.text("Processing...").speed(1.5))
         * }</pre>
         *
         * @param name        the spinner name (required)
         * @param configurer  a consumer that configures the Spinner options
         * @return a new Spinner instance
         */
        public static Spinner of(String name, Consumer<Config> configurer) {
                Config cfg = new Config();
                cfg.name = name;
                configurer.accept(cfg);
                return new Spinner(cfg);
        }

        // =========================================================================
        // Constructors
        // =========================================================================

        private Spinner(Config cfg) {
                this(cfg.name, cfg.text, cfg.style, cfg.speed);
        }

        /**
         * Full constructor.
         *
         * @param name  spinner name (must be a key in the SPINNERS map)
         * @param text  text to display after the spinner frame (String, Text, or other
         *              renderable)
         * @param style style for the spinner (String or Style), may be null
         * @param speed animation speed multiplier
         * @throws IllegalArgumentException if no spinner with the given name exists
         */
        public Spinner(String name, Object text, Object style, double speed) {
                SpinnerDef def = SPINNERS.get(name);
                if (def == null) {
                        throw new IllegalArgumentException("no spinner called '" + name + "'");
                }
                this.name = name;
                this.text = text instanceof String str ? Text.fromMarkup(str) : text;
                this.frames = new ArrayList<>(def.frames());
                this.interval = def.interval();
                this.startTime = null;
                this.style = style;
                this.speed = speed;
                this.frameNoOffset = 0.0;
                this.updateSpeed = 0.0;
        }

        /**
         * Create a Spinner with text, default style (null) and speed (1.0).
         *
         * @param name spinner name
         * @param text text to display after the spinner frame
         */
        public Spinner(String name, String text) {
                this(name, text, null, 1.0);
        }

        /**
         * Create a Spinner with no text, default style (null) and speed (1.0).
         *
         * @param name spinner name
         */
        public Spinner(String name) {
                this(name, "", null, 1.0);
        }

        // =========================================================================
        // Getters
        // =========================================================================

        public String getName() {
                return name;
        }

        public Object getText() {
                return text;
        }

        public List<String> getFrames() {
                return Collections.unmodifiableList(frames);
        }

        public int getInterval() {
                return interval;
        }

        public Double getStartTime() {
                return startTime;
        }

        public Object getStyle() {
                return style;
        }

        public double getSpeed() {
                return speed;
        }

        public double getFrameNoOffset() {
                return frameNoOffset;
        }

        // =========================================================================
        // Rendering
        // =========================================================================

        /**
         * Render the spinner for a given time.
         *
         * @param time time in seconds
         * @return a Text renderable containing the animation frame
         */
        public Text render(double time) {
                if (startTime == null) {
                        startTime = time;
                }

                double frameNo = ((time - startTime) * speed) / (interval / MILLIS_PER_SECOND) + frameNoOffset;
                Text frame = new Text(frames.get((int) frameNo % frames.size()), style != null ? style : "");

                if (updateSpeed != 0.0) {
                        frameNoOffset = frameNo;
                        startTime = time;
                        speed = updateSpeed;
                        updateSpeed = 0.0;
                }

                if (text == null) {
                        return frame;
                }
                // For other renderables, just assemble with space separator
                return Text.assemble(frame, " ", text);
        }

        /**
         * Rich console rendering method.
         * Uses System.nanoTime() as the time source.
         */
        @Override
        public Iterable<?> richConsole(Console console, ConsoleOptions options) {
                double time = System.nanoTime() / NANOS_PER_SECOND;
                List<Text> result = new ArrayList<>();
                result.add(render(time));
                return result;
        }

        // =========================================================================
        // Update
        // =========================================================================

        /**
         * Update spinner attributes after it has been started.
         *
         * @param text  new text (empty/null means no change)
         * @param style new style (null means no change)
         * @param speed new speed (null means no change)
         */
        public void update(Object text, Object style, Double speed) {
                if (text != null) {
                        String textStr = text instanceof String str ? str : null;
                        this.text = textStr != null && !textStr.isEmpty()
                                        ? Text.fromMarkup(textStr)
                                        : text;
                }
                if (style != null) {
                        this.style = style;
                }
                if (speed != null && speed > 0) {
                        this.updateSpeed = speed;
                }
        }

        // =========================================================================
        // Convenience
        // =========================================================================

        /**
         * Get the current frame character at the current time.
         *
         * @return the current frame string
         */
        public String getCurrentFrame() {
                double time = System.nanoTime() / NANOS_PER_SECOND;
                if (startTime == null) {
                        startTime = time;
                }
                double frameNo = ((time - startTime) * speed) / (interval / MILLIS_PER_SECOND) + frameNoOffset;
                return frames.get((int) Math.abs(frameNo) % frames.size());
        }

        /**
         * Get all available spinner names.
         *
         * @return unmodifiable set of spinner names
         */
        public static Iterable<String> getSpinnerNames() {
                return SPINNERS.keySet();
        }

        /**
         * Get the spinner definition for a given name.
         *
         * @param name the spinner name
         * @return the SpinnerDef, or null if not found
         */
        public static SpinnerDef getSpinnerDef(String name) {
                return SPINNERS.get(name);
        }

        @Override
        public String toString() {
                return "Spinner(name=" + name
                                + ", text=" + text
                                + ", speed=" + speed
                                + ", frameNoOffset=" + frameNoOffset + ")";
        }
}
