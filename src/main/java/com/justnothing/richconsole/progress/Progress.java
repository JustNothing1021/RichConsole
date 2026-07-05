package com.justnothing.richconsole.progress;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.live.Live;
import com.justnothing.richconsole.progressbar.ProgressBar;
import com.justnothing.richconsole.spinner.Spinner;
import com.justnothing.richconsole.table.Table;
import com.justnothing.richconsole.table.Table.TableColumn;
import com.justnothing.richconsole.text.Text;

/**
 * A progress bar with task tracking.
 * Faithfully ported from rich/progress.py Progress class.
 */
public class Progress implements RichRenderable, AutoCloseable {

    // =========================================================================
    // Constants
    // =========================================================================

    private static final String DEFAULT_BAR_BACK_STYLE = "bar.back";
    private static final String DEFAULT_BAR_COMPLETE_STYLE = "bar.complete";
    private static final String DEFAULT_BAR_FINISHED_STYLE = "bar.finished";
    private static final String DEFAULT_BAR_PULSE_STYLE = "bar.pulse";
    private static final String UNKNOWN_PERCENTAGE_TEXT = "  --%";
    private static final String UNKNOWN_TIME_REMAINING_TEXT = "-:--:--";
    private static final String STYLE_PROGRESS_PERCENTAGE = "progress.percentage";
    private static final String STYLE_PROGRESS_REMAINING = "progress.remaining";
    private static final String STYLE_PROGRESS_ELAPSED = "progress.elapsed";
    private static final String STYLE_PROGRESS_SPINNER = "progress.spinner";
    private static final String STYLE_PROGRESS_DESCRIPTION = "progress.description";
    private static final double DEFAULT_TASK_TOTAL = 100.0;
    private static final int MAX_PROGRESS_SAMPLES = 1000;
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    // =========================================================================
    // TaskID inner class
    // =========================================================================

    public static class TaskID {
        private final int id;

        public TaskID(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof TaskID)) return false;
            return id == ((TaskID) obj).id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public String toString() {
            return "TaskID(" + id + ")";
        }
    }

    // =========================================================================
    // ProgressSample inner record
    // =========================================================================

    public static class ProgressSample {
        private final double timestamp;
        private final double completed;

        public ProgressSample(double timestamp, double completed) {
            this.timestamp = timestamp;
            this.completed = completed;
        }

        public double getTimestamp() {
            return timestamp;
        }

        public double getCompleted() {
            return completed;
        }
    }

    // =========================================================================
    // Task inner class
    // =========================================================================

    public class Task {
        private final int id;
        private String description;
        private Double total;
        private double completed;
        private Double startTime;
        private Double stopTime;
        private Double finishedTime;
        private Double finishedSpeed;
        private boolean visible;
        private final Map<String, Object> fields;
        private final Deque<ProgressSample> progress;
        private final DoubleSupplier getTime;

        Task(int id, String description, Double total, double completed,
             boolean visible, Map<String, Object> fields, DoubleSupplier getTime) {
            this.id = id;
            this.description = description;
            this.total = total;
            this.completed = completed;
            this.visible = visible;
            this.fields = fields != null ? new HashMap<>(fields) : new HashMap<>();
            this.progress = new ArrayDeque<>();
            this.getTime = getTime;
        }

        public int getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Double getTotal() {
            return total;
        }

        public void setTotal(Double total) {
            this.total = total;
        }

        public double getCompleted() {
            return completed;
        }

        public void setCompleted(double completed) {
            this.completed = completed;
        }

        public Double getStartTime() {
            return startTime;
        }

        public void setStartTime(Double startTime) {
            this.startTime = startTime;
        }

        public Double getStopTime() {
            return stopTime;
        }

        public void setStopTime(Double stopTime) {
            this.stopTime = stopTime;
        }

        public Double getFinishedTime() {
            return finishedTime;
        }

        public Double getFinishedSpeed() {
            return finishedSpeed;
        }

        public boolean isVisible() {
            return visible;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public Map<String, Object> getFields() {
            return fields;
        }

        /**
         * 返回 progress 采样数据的快照副本。
         * 不暴露内部 Deque 引用，防止外部修改导致 CME。
         */
        public List<ProgressSample> getProgressSnapshot() {
            synchronized (progress) {
                return new ArrayList<>(progress);
            }
        }

        /**
         * 清理过期的采样数据，并添加新的采样。
         * 所有操作在 synchronized(progress) 内完成，避免 CME。
         */
        public void pruneAndAddSample(double oldSampleTime, double now, double updateCompleted) {
            synchronized (progress) {
                while (!progress.isEmpty()
                        && progress.peekFirst().getTimestamp() < oldSampleTime) {
                    progress.pollFirst();
                }
                while (progress.size() > MAX_PROGRESS_SAMPLES) {
                    progress.pollFirst();
                }
                if (updateCompleted > 0) {
                    progress.addLast(new ProgressSample(now, updateCompleted));
                }
            }
        }

        public boolean isStarted() {
            return startTime != null;
        }

        public Double getRemaining() {
            if (total == null || total <= 0.0) {
                return null;
            }
            return Math.max(0.0, total - completed);
        }

        public Double getElapsed() {
            if (startTime == null) {
                return null;
            }
            if (finishedTime != null) {
                return finishedTime;
            }
            if (stopTime != null) {
                return stopTime - startTime;
            }
            return getTime.getAsDouble() - startTime;
        }

        public boolean isFinished() {
            return total != null && total > 0.0 && completed >= total;
        }

        public Double getPercentage() {
            if (total == null || total == 0.0) {
                return null;
            }
            return Math.min(100.0, (completed * 100.0) / total);
        }

        public Double getSpeed() {
            if (finishedSpeed != null) {
                return finishedSpeed;
            }
            if (!isStarted()) {
                return null;
            }
            Double elapsed = getElapsed();
            if (elapsed == null || elapsed == 0.0) {
                return null;
            }
            List<ProgressSample> snapshot = getProgressSnapshot();
            double totalCompleted = 0.0;
            for (ProgressSample sample : snapshot) {
                totalCompleted += sample.getCompleted();
            }
            if (totalCompleted <= 0.0) {
                return null;
            }
            double elapsedSinceFirstSample = elapsed;
            if (!snapshot.isEmpty()) {
                elapsedSinceFirstSample = getTime.getAsDouble() - snapshot.get(0).getTimestamp();
            }
            if (elapsedSinceFirstSample <= 0.0) {
                return null;
            }
            return totalCompleted / elapsedSinceFirstSample;
        }

        public Double getTimeRemaining() {
            Double speed = getSpeed();
            Double remaining = getRemaining();
            if (speed == null || speed <= 0.0 || remaining == null) {
                return null;
            }
            return remaining / speed;
        }

        public void reset() {
            synchronized (progress) {
                progress.clear();
            }
            finishedTime = null;
            finishedSpeed = null;
        }
    }

    // =========================================================================
    // Progress fields
    // =========================================================================

    private final ReentrantLock lock = new ReentrantLock();
    private final Console console;
    private final boolean autoRefresh;
    private final double refreshPerSecond;
    private final double speedEstimatePeriod;
    private final boolean transientMode;
    private final boolean disable;
    private final boolean expand;
    private final Map<Integer, Task> tasks;
    private int nextTaskId;
    private final Live live;
    private final Spinner spinner;

    // =========================================================================
    // Config inner class
    // =========================================================================

    public static class Config {
        public Console console;
        public boolean autoRefresh = true;
        public double refreshPerSecond = 10.0;
        public double speedEstimatePeriod = 40.0;
        public boolean transientMode = false;
        public boolean disable = false;
        public boolean expand = true;
    }

    public static Progress of(Consumer<Config> configurer) {
        Config config = new Config();
        configurer.accept(config);
        return new Progress(config);
    }

    private Progress(Config config) {
        this(config.console, config.autoRefresh, config.refreshPerSecond,
                config.speedEstimatePeriod, config.transientMode, config.disable, config.expand);
    }

    // =========================================================================
    // Constructors
    // =========================================================================

    public Progress() {
        this(Console.of(cfg -> {}), true, 10.0, 30.0, false, false, false);
    }

    public Progress(Console console) {
        this(console, true, 10.0, 30.0, false, false, false);
    }

    public Progress(Console console, boolean autoRefresh, double refreshPerSecond,
                    double speedEstimatePeriod, boolean transientMode,
                    boolean disable, boolean expand) {
        this.console = console;
        this.autoRefresh = autoRefresh;
        this.refreshPerSecond = refreshPerSecond;
        this.speedEstimatePeriod = speedEstimatePeriod;
        this.transientMode = transientMode;
        this.disable = disable;
        this.expand = expand;
        this.tasks = new LinkedHashMap<>();
        this.nextTaskId = 0;
        this.live = console != null
                ? new Live(this, console, refreshPerSecond, transientMode, autoRefresh)
                : null;
        this.spinner = new Spinner("dots", null, STYLE_PROGRESS_SPINNER, 1.0);
    }

    // =========================================================================
    // Static time helper
    // =========================================================================

    public static double currentTime() {
        return System.nanoTime() / NANOS_PER_SECOND;
    }

    // =========================================================================
    // Properties
    // =========================================================================

    public Console getConsole() {
        return console;
    }

    public List<Task> getTasks() {
        lock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(tasks.values()));
        } finally {
            lock.unlock();
        }
    }

    public boolean isFinished() {
        lock.lock();
        try {
            if (tasks.isEmpty()) {
                return true;
            }
            for (Task task : tasks.values()) {
                if (!task.isFinished()) {
                    return false;
                }
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    public Live getLive() {
        return live;
    }

    // =========================================================================
    // Start / Stop (AutoCloseable)
    // =========================================================================

    public void start() {
        if (live != null) {
            live.start();
        }
    }

    public void stop() {
        if (live != null) {
            live.stop();
        }
    }

    @Override
    public void close() {
        stop();
    }

    // =========================================================================
    // Task management
    // =========================================================================

    public int addTask(String description) {
        return addTask(description, true, DEFAULT_TASK_TOTAL, 0.0, true, null);
    }

    public int addTask(String description, double total) {
        return addTask(description, true, total, 0.0, true, null);
    }

    /**
     * Add a task with nullable total (null for indeterminate progress).
     */
    public int addTask(String description, Double total, double completed) {
        return addTask(description, true, total, completed, true, null);
    }

    public int addTask(String description, boolean start, Double total,
                       double completed, boolean visible, Map<String, Object> fields) {
        lock.lock();
        try {
            Task task = new Task(nextTaskId, description, total, completed,
                    visible, fields, Progress::currentTime);
            tasks.put(nextTaskId, task);
            if (start) {
                startTask(nextTaskId);
            }
            int taskId = nextTaskId;
            nextTaskId++;
            return taskId;
        } finally {
            lock.unlock();
        }
    }

    public void startTask(int taskId) {
        lock.lock();
        try {
            Task task = tasks.get(taskId);
            if (task != null && task.getStartTime() == null) {
                task.setStartTime(currentTime());
            }
        } finally {
            lock.unlock();
        }
    }

    public void stopTask(int taskId) {
        lock.lock();
        try {
            Task task = tasks.get(taskId);
            if (task != null) {
                double now = currentTime();
                if (task.getStartTime() == null) {
                    task.setStartTime(now);
                }
                task.setStopTime(now);
            }
        } finally {
            lock.unlock();
        }
    }

    public void update(int taskId) {
        refresh();
    }

    public void update(int taskId, Double total, Double completed, Double advance,
                       String description, Boolean visible, boolean refresh,
                       Map<String, Object> fields) {
        lock.lock();
        try {
            Task task = tasks.get(taskId);
            if (task == null) {
                return;
            }
            double completedStart = task.getCompleted();

            if (total != null && !total.equals(task.getTotal())) {
                task.setTotal(total);
                task.reset();
            }
            if (advance != null) {
                task.setCompleted(task.getCompleted() + advance);
            }
            if (completed != null) {
                task.setCompleted(completed);
            }
            if (description != null) {
                task.setDescription(description);
            }
            if (visible != null) {
                task.setVisible(visible);
            }
            if (fields != null) {
                task.getFields().putAll(fields);
            }

            double updateCompleted = task.getCompleted() - completedStart;
            double now = currentTime();
            double oldSampleTime = now - speedEstimatePeriod;

            task.pruneAndAddSample(oldSampleTime, now, updateCompleted);

            if (task.getTotal() != null
                    && task.getCompleted() >= task.getTotal()
                    && task.getFinishedTime() == null) {
                task.finishedTime = task.getElapsed();
                task.finishedSpeed = task.getSpeed();
            }
        } finally {
            lock.unlock();
        }

        if (refresh) {
            this.refresh();
        }
    }

    public void advance(int taskId) {
        advance(taskId, 1.0);
    }

    public void advance(int taskId, double step) {
        double now = currentTime();
        lock.lock();
        try {
            Task task = tasks.get(taskId);
            if (task == null) {
                return;
            }
            double completedStart = task.getCompleted();
            task.setCompleted(completedStart + step);

            double updateCompleted = task.getCompleted() - completedStart;
            double oldSampleTime = now - speedEstimatePeriod;

            task.pruneAndAddSample(oldSampleTime, now, updateCompleted);

            if (task.getTotal() != null
                    && task.getCompleted() >= task.getTotal()
                    && task.getFinishedTime() == null) {
                task.finishedTime = task.getElapsed();
                task.finishedSpeed = task.getSpeed();
            }
        } finally {
            lock.unlock();
        }
    }

    public Task getTask(int taskId) {
        lock.lock();
        try {
            return tasks.get(taskId);
        } finally {
            lock.unlock();
        }
    }

    public void removeTask(int taskId) {
        lock.lock();
        try {
            tasks.remove(taskId);
        } finally {
            lock.unlock();
        }
    }

    // =========================================================================
    // Refresh
    // =========================================================================

    public void refresh() {
        if (!disable && live != null && live.isStarted()) {
            live.refresh();
        }
    }

    // =========================================================================
    // Rendering
    // =========================================================================

    public Text makeBar(Task task) {
        Text barText = new Text();
        barText.setEnd("");

        // Description — parse markup if present
        String desc = task.getDescription();
        Text descText = Text.fromMarkup(desc);
        barText.append(descText);
        barText.append(" ");

        // Percentage
        Double pct = task.getPercentage();
        barText.append(" ");
        if (pct == null) {
            barText.append(UNKNOWN_PERCENTAGE_TEXT);
        } else {
            barText.append(String.format("%3.0f%%", pct), STYLE_PROGRESS_PERCENTAGE);
        }

        // Time remaining
        Double timeRemaining = task.getTimeRemaining();
        barText.append(" ");
        if (timeRemaining != null) {
            barText.append(formatTime(timeRemaining), STYLE_PROGRESS_REMAINING);
        } else {
            barText.append(UNKNOWN_TIME_REMAINING_TEXT, STYLE_PROGRESS_REMAINING);
        }

        return barText;
    }

    private static final int BAR_WIDTH = 40;

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        List<Task> taskList;
        lock.lock();
        try {
            taskList = new ArrayList<>(tasks.values());
        } finally {
            lock.unlock();
        }

        Table table = Table.grid(1, expand);

        // Add columns: spinner, description, bar, percentage, remaining, elapsed
        table.addColumn(null); // spinner — no ratio, content-width
        table.addColumn(null); // description — no ratio, content-width (no_wrap=True is implicit in grid)
        TableColumn barCol = table.addColumn(null);
        if (expand) {
            barCol.setRatio(1.0); // bar takes all remaining space when expand=true
        }
        barCol.setWidth(BAR_WIDTH); // minimum width for bar
        table.addColumn(null); // percentage — no ratio, content-width
        table.addColumn(null); // remaining — no ratio, content-width
        table.addColumn(null); // elapsed — no ratio, content-width

        double now = currentTime();
        for (Task task : taskList) {
            if (!task.isVisible()) {
                continue;
            }

            // Spinner
            Text spinnerText;
            if (task.isFinished()) {
                spinnerText = new Text(" ");
            } else {
                spinnerText = spinner.render(now);
            }

            // Description
            Text descText = Text.fromMarkup(task.getDescription());
            descText.setEnd("");

            // Bar — width=null lets ProgressBar auto-adapt to the column width
            Double total = task.getTotal();
            // total=0 is also indeterminate (no meaningful progress)
            boolean isPulse = total == null || total <= 0.0;
            ProgressBar progressBar = ProgressBar.of(cfg -> cfg
                    .total(total != null ? total : 0.0)
                    .completed(task.getCompleted())
                    .pulse(isPulse)
                    .style(DEFAULT_BAR_BACK_STYLE)
                    .completeStyle(DEFAULT_BAR_COMPLETE_STYLE)
                    .finishedStyle(DEFAULT_BAR_FINISHED_STYLE)
                    .pulseStyle(DEFAULT_BAR_PULSE_STYLE)
                    .animationTime(now)
            );

            // Percentage
            Double pct = task.getPercentage();
            Text pctText = new Text();
            pctText.setEnd("");
            if (pct == null) {
                pctText.append(UNKNOWN_PERCENTAGE_TEXT, STYLE_PROGRESS_PERCENTAGE);
            } else {
                pctText.append(String.format("%3.0f%%", pct), STYLE_PROGRESS_PERCENTAGE);
            }

            // Time remaining
            Double timeRemaining = task.getTimeRemaining();
            Text remainingText = new Text();
            remainingText.setEnd("");
            if (timeRemaining != null) {
                remainingText.append(formatTime(timeRemaining), STYLE_PROGRESS_REMAINING);
            } else {
                remainingText.append(UNKNOWN_TIME_REMAINING_TEXT, STYLE_PROGRESS_REMAINING);
            }

            // Elapsed
            Double elapsed = task.getElapsed();
            Text elapsedText = new Text();
            elapsedText.setEnd("");
            if (elapsed != null) {
                elapsedText.append(formatTime(elapsed), STYLE_PROGRESS_ELAPSED);
            } else {
                elapsedText.append(UNKNOWN_TIME_REMAINING_TEXT, STYLE_PROGRESS_ELAPSED);
            }

            table.addRow(spinnerText, descText, progressBar, pctText, remainingText, elapsedText);
        }

        return table.richConsole(console, options);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private static String formatTime(double seconds) {
        if (seconds < 0) {
            return UNKNOWN_TIME_REMAINING_TEXT;
        }
        int totalSeconds = (int) seconds;
        int hrs = totalSeconds / 3600;
        int mins = (totalSeconds % 3600) / 60;
        int secs = totalSeconds % 60;
        // Always show h:mm:ss format (matching Python rich's default)
        return String.format("%d:%02d:%02d", hrs, mins, secs);
    }
}
