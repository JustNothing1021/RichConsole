package com.justnothing.richconsole.progress;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.measure.Measurement;
import com.justnothing.richconsole.live.Live;
import com.justnothing.richconsole.table.Table;
import com.justnothing.richconsole.table.Table.TableColumn;

/**
 * A progress bar with task tracking.
 * Faithfully ported from rich/progress.py Progress class.
 *
 * <p>The display is composed of a list of {@link ProgressColumn}s; the default
 * layout shows a spinner, the task description, a bar, the percentage, the
 * estimated time remaining and the elapsed time.</p>
 */
public class Progress implements RichRenderable, AutoCloseable {

    // =========================================================================
    // Constants
    // =========================================================================

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

        /** Current time source (seconds), as used by the progress columns. */
        public double getTime() {
            return getTime.getAsDouble();
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
    private final List<ProgressColumn> columns;
    private final boolean autoRefresh;
    private final double refreshPerSecond;
    private final double speedEstimatePeriod;
    private final boolean transientMode;
    private final boolean disable;
    private final boolean expand;
    private final Map<Integer, Task> tasks;
    private int nextTaskId;
    private final Live live;

    // =========================================================================
    // Config inner class
    // =========================================================================

    public static class Config {
        public Console console;
        public List<ProgressColumn> columns;
        public boolean autoRefresh = true;
        public double refreshPerSecond = 10.0;
        public double speedEstimatePeriod = 40.0;
        public boolean transientMode = false;
        public boolean disable = false;
        public boolean expand = false;
    }

    public static Progress of(Consumer<Config> configurer) {
        Config config = new Config();
        configurer.accept(config);
        return new Progress(config);
    }

    private Progress(Config config) {
        this(config.console, config.columns != null ? config.columns : getDefaultColumns(),
                config.autoRefresh, config.refreshPerSecond, config.speedEstimatePeriod,
                config.transientMode, config.disable, config.expand);
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
        this(console, getDefaultColumns(), autoRefresh, refreshPerSecond,
                speedEstimatePeriod, transientMode, disable, expand);
    }

    /**
     * Full constructor with explicit columns.
     *
     * @param console             console to render to
     * @param columns             the progress columns; use {@link #getDefaultColumns()}
     *                            for the standard layout
     * @param autoRefresh         enable auto refresh
     * @param refreshPerSecond    refresh rate (per second)
     * @param speedEstimatePeriod period (seconds) used to estimate speed
     * @param transientMode       clear the progress on exit
     * @param disable             disable display
     * @param expand              expand the tasks table to fill the terminal width
     */
    public Progress(Console console, List<ProgressColumn> columns, boolean autoRefresh,
                    double refreshPerSecond, double speedEstimatePeriod, boolean transientMode,
                    boolean disable, boolean expand) {
        this.console = console;
        this.columns = columns != null ? new ArrayList<>(columns) : getDefaultColumns();
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
    }

    // =========================================================================
    // Default columns
    // =========================================================================

    /**
     * The default columns: spinner, description, bar, percentage,
     * time remaining (elapsed once finished) and elapsed time.
     */
    public static List<ProgressColumn> getDefaultColumns() {
        List<ProgressColumn> columns = new ArrayList<>();
        columns.add(new SpinnerColumn());
        columns.add(new TextColumn("[progress.description]{task.description}[/progress.description]"));
        columns.add(new BarColumn());
        columns.add(new TaskProgressColumn());
        columns.add(new TimeRemainingColumn(false, true));
        columns.add(new TimeElapsedColumn());
        return columns;
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

    public List<ProgressColumn> getColumns() {
        return Collections.unmodifiableList(columns);
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

    private Table buildTable() {
        Table table = Table.grid(1, expand);
        for (ProgressColumn column : columns) {
            TableColumn tableColumn = table.addColumn(null);
            configureColumn(tableColumn, column);
        }
        return table;
    }

    /** Apply per-column table settings, mirroring rich's make_tasks_table. */
    private void configureColumn(TableColumn column, ProgressColumn progressColumn) {
        if (progressColumn instanceof SpinnerColumn) {
            column.setNoWrap(true);
        } else if (progressColumn instanceof BarColumn barColumn) {
            if (expand) {
                column.setRatio(1.0);
            }
            if (barColumn.getWidth() != null) {
                column.setWidth(barColumn.getWidth());
            }
        } else if (progressColumn instanceof TextColumn) {
            column.setNoWrap(true);
        } else if (progressColumn instanceof TimeRemainingColumn) {
            column.setJustify("right");
        } else if (progressColumn instanceof TimeElapsedColumn) {
            column.setNoWrap(true);
            column.setJustify("right");
        } else if (progressColumn instanceof MofNCompleteColumn) {
            column.setNoWrap(true);
        } else if (progressColumn instanceof DownloadColumn
                || progressColumn instanceof FileSizeColumn
                || progressColumn instanceof TotalFileSizeColumn
                || progressColumn instanceof TransferSpeedColumn) {
            if (progressColumn.getWidth() != null) {
                column.setWidth(progressColumn.getWidth());
            }
        }
    }

    @Override
    public Measurement richMeasure(Console console, ConsoleOptions options) {
        // Delegate to the table's richMeasure so the outer context
        // knows the actual content width (not the full terminal width).
        Table table = buildTable();
        return table.richMeasure(console, options);
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        List<Task> taskList;
        lock.lock();
        try {
            taskList = new ArrayList<>(tasks.values());
        } finally {
            lock.unlock();
        }

        Table table = buildTable();

        for (Task task : taskList) {
            if (!task.isVisible()) {
                continue;
            }
            List<Object> row = new ArrayList<>();
            for (ProgressColumn column : columns) {
                row.add(column.call(task));
            }
            table.addRow(row.toArray());
        }

        return table.richConsole(console, options);
    }

    // =========================================================================
    // Convenience: track()
    // =========================================================================

    /**
     * Configuration for the module-level {@link #track(Iterable, Consumer)} helper.
     */
    public static class TrackConfig {
        public Console console;
        public String description = "Working...";
        public Double total;
        public double completed = 0.0;
        public boolean transientMode = false;

        public TrackConfig console(Console console) { this.console = console; return this; }
        public TrackConfig description(String description) { this.description = description; return this; }
        public TrackConfig total(Double total) { this.total = total; return this; }
        public TrackConfig completed(double completed) { this.completed = completed; return this; }
        public TrackConfig transientMode(boolean transientMode) { this.transientMode = transientMode; return this; }
    }

    /**
     * Track progress while iterating over a sequence.
     *
     * <p>When {@code total} is null the total is taken from the sequence size
     * (if it is a {@link Collection}); otherwise the task is indeterminate
     * and renders a pulsing bar.</p>
     *
     * @param sequence    values to iterate over
     * @param description task description
     * @param total       total number of steps, or null to auto-detect
     * @param completed   steps completed so far
     */
    public <T> Iterable<T> track(Iterable<T> sequence, String description, Double total, double completed) {
        Double effectiveTotal = total;
        if (effectiveTotal == null && sequence instanceof Collection<?> collection) {
            effectiveTotal = (double) collection.size();
        }
        int taskId = addTask(description, effectiveTotal, completed);
        Iterator<T> iterator = sequence.iterator();
        return () -> new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                T value = iterator.next();
                advance(taskId);
                return value;
            }
        };
    }

    /**
     * Convenience wrapper that creates a Progress, starts it and returns an
     * {@link Iterable} which stops the progress once the sequence is exhausted.
     *
     * <pre>{@code
     * for (String item : Progress.track(list, cfg -> cfg.description("Processing"))) {
     *     process(item);
     * }
     * }</pre>
     */
    public static <T> Iterable<T> track(Iterable<T> sequence, Consumer<TrackConfig> configurer) {
        TrackConfig cfg = new TrackConfig();
        configurer.accept(cfg);
        Progress progress = Progress.of(p -> {
            p.console = cfg.console != null ? cfg.console : new Console();
            p.transientMode = cfg.transientMode;
        });
        progress.start();
        Iterable<T> iterable = progress.track(sequence, cfg.description, cfg.total, cfg.completed);
        return new AutoStopIterable<>(iterable, progress);
    }

    /**
     * An iterable that stops the associated progress once exhausted (or closed).
     */
    private static final class AutoStopIterable<T> implements Iterable<T>, AutoCloseable {
        private final Iterable<T> delegate;
        private final Progress progress;

        AutoStopIterable(Iterable<T> delegate, Progress progress) {
            this.delegate = delegate;
            this.progress = progress;
        }

        @Override
        public Iterator<T> iterator() {
            Iterator<T> iterator = delegate.iterator();
            return new Iterator<T>() {
                private boolean stopped = false;

                private void stopIfNeeded() {
                    if (!stopped) {
                        stopped = true;
                        progress.stop();
                    }
                }

                @Override
                public boolean hasNext() {
                    boolean hasNext = iterator.hasNext();
                    if (!hasNext) {
                        stopIfNeeded();
                    }
                    return hasNext;
                }

                @Override
                public T next() {
                    return iterator.next();
                }
            };
        }

        @Override
        public void close() {
            progress.stop();
        }
    }

    // =========================================================================
    // Convenience: wrapFile()
    // =========================================================================

    /**
     * Configuration for the module-level {@link #wrapFile(File, Consumer)} helper.
     */
    public static class WrapFileConfig {
        public Console console;
        public String description = "Reading...";
        public boolean transientMode = false;

        public WrapFileConfig console(Console console) { this.console = console; return this; }
        public WrapFileConfig description(String description) { this.description = description; return this; }
        public WrapFileConfig transientMode(boolean transientMode) { this.transientMode = transientMode; return this; }
    }

    /**
     * Wrap an {@link InputStream} so that bytes read advance a progress task.
     * The caller is responsible for starting/stopping this Progress.
     *
     * @param file        the stream to read from
     * @param total       total number of bytes to read
     * @param description task description
     * @return a tracking input stream
     */
    public InputStream wrapFile(InputStream file, long total, String description) {
        int taskId = addTask(description, (double) total, 0.0);
        return new TrackingInputStream(file, this, taskId);
    }

    /**
     * Read a {@link File} while showing progress. The total is taken from the
     * file length, the progress is started automatically and stopped when the
     * returned stream is exhausted or closed.
     *
     * <pre>{@code
     * try (InputStream in = Progress.wrapFile(file, cfg -> cfg.description("Uploading"))) {
     *     ...
     * }
     * }</pre>
     */
    public static InputStream wrapFile(File file, Consumer<WrapFileConfig> configurer) {
        WrapFileConfig cfg = new WrapFileConfig();
        configurer.accept(cfg);
        Progress progress = Progress.of(p -> {
            p.console = cfg.console != null ? cfg.console : new Console();
            p.transientMode = cfg.transientMode;
        });
        progress.start();
        try {
            InputStream in = new FileInputStream(file);
            long total = file.length();
            return new AutoStopInputStream(progress.wrapFile(in, total, cfg.description), progress);
        } catch (IOException e) {
            progress.stop();
            throw new UncheckedIOException(e);
        }
    }

    /** An input stream that advances a progress task for every byte read. */
    private static final class TrackingInputStream extends InputStream {
        private final InputStream delegate;
        private final Progress progress;
        private final int taskId;
        private boolean closed;

        TrackingInputStream(InputStream delegate, Progress progress, int taskId) {
            this.delegate = delegate;
            this.progress = progress;
            this.taskId = taskId;
        }

        @Override
        public int read() throws IOException {
            int b = delegate.read();
            if (b >= 0) {
                progress.advance(taskId, 1.0);
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = delegate.read(b, off, len);
            if (n > 0) {
                progress.advance(taskId, n);
            }
            return n;
        }

        @Override
        public long skip(long n) throws IOException {
            long skipped = delegate.skip(n);
            if (skipped > 0) {
                progress.advance(taskId, skipped);
            }
            return skipped;
        }

        @Override
        public int available() throws IOException {
            return delegate.available();
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                delegate.close();
            }
        }
    }

    /** An input stream that stops the progress on EOF or close. */
    private static final class AutoStopInputStream extends InputStream {
        private final InputStream delegate;
        private final Progress progress;
        private boolean stopped;
        private boolean closed;

        AutoStopInputStream(InputStream delegate, Progress progress) {
            this.delegate = delegate;
            this.progress = progress;
        }

        private void stopIfNeeded() {
            if (!stopped) {
                stopped = true;
                progress.stop();
            }
        }

        @Override
        public int read() throws IOException {
            int b = delegate.read();
            if (b < 0) {
                stopIfNeeded();
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = delegate.read(b, off, len);
            if (n < 0) {
                stopIfNeeded();
            }
            return n;
        }

        @Override
        public long skip(long n) throws IOException {
            return delegate.skip(n);
        }

        @Override
        public int available() throws IOException {
            return delegate.available();
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                stopIfNeeded();
                delegate.close();
            }
        }
    }
}
