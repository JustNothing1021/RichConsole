package com.justnothing.richconsole.log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.containers.Renderables;
import com.justnothing.richconsole.table.Table;
import com.justnothing.richconsole.text.Text;

/**
 * Renders log output with optional time, level, and path columns.
 * Ported from rich/_log_render.py LogRender class.
 *
 * <p>LogRender creates a Table.grid with columns for time, level,
 * message, and caller path, matching Python rich's log output format.</p>
 */
public class LogRender implements RichRenderable {

    private final boolean showTime;
    private final boolean showLevel;
    private final boolean showPath;
    private final DateTimeFormatter timeFormatter;
    private final boolean omitRepeatedTimes;
    private final int levelWidth;

    private Text lastTime = null;

    /**
     * Create a LogRender instance.
     */
    public LogRender(boolean showTime, boolean showLevel, boolean showPath,
                     String timeFormat, boolean omitRepeatedTimes, int levelWidth) {
        this.showTime = showTime;
        this.showLevel = showLevel;
        this.showPath = showPath;
        this.omitRepeatedTimes = omitRepeatedTimes;
        this.levelWidth = levelWidth;
        DateTimeFormatter formatter;
        try {
            formatter = DateTimeFormatter.ofPattern(
                    timeFormat != null ? timeFormat : "'['yy-MM-dd HH:mm:ss']'");
        } catch (IllegalArgumentException e) {
            formatter = DateTimeFormatter.ofPattern("'['yy-MM-dd HH:mm:ss']'");
        }
        this.timeFormatter = formatter;
    }

    /**
     * Create a LogRender with sensible defaults matching Python rich.
     */
    public LogRender() {
        this(true, false, true, "'['yy-MM-dd HH:mm:ss']'", true, 8);
    }

    /**
     * Build the log table grid from renderables and caller info.
     * Matches Python rich's LogRender.__call__() output format.
     *
     * @param console     the Console instance
     * @param renderables the message renderables
     * @param logTime     the timestamp for this log entry
     * @param level       the log level text
     * @param path        the caller file path
     * @param lineNo      the caller line number
     * @param linkPath    the full path for hyperlink (optional)
     * @return a Table grid containing the log output
     */
    public Table render(Console console, List<Object> renderables,
                        LocalDateTime logTime, String level,
                        String path, Integer lineNo, String linkPath) {
        // Match Python: Table.grid(padding=(0, 1)) with expand=True
        Table output = Table.grid(0, 1, true);

        if (showTime) {
            output.addColumn("", "log.time", null);
        }
        if (showLevel) {
            Table.TableColumn levelCol = output.addColumn("", "log.level", null);
            levelCol.setWidth(levelWidth);
        }
        Table.TableColumn msgCol = output.addColumn("", "log.message", null);
        msgCol.setRatio(1.0);
        msgCol.setOverflow("fold");
        if (showPath && path != null) {
            output.addColumn("", "log.path", null);
        }

        List<Object> row = new ArrayList<>();

        if (showTime) {
            LocalDateTime time = logTime != null ? logTime : LocalDateTime.now();
            String formatted = time.format(timeFormatter);
            Text logTimeDisplay = new Text(formatted);
            if (logTimeDisplay.equals(lastTime) && omitRepeatedTimes) {
                row.add(new Text(" ".repeat(formatted.length())));
            } else {
                row.add(logTimeDisplay);
                lastTime = logTimeDisplay;
            }
        }

        if (showLevel) {
            row.add(level != null ? level : "");
        }

        row.add(new Renderables(renderables));

        if (showPath && path != null) {
            Text pathText = new Text();
            if (linkPath != null) {
                pathText.append(path, "link file://" + linkPath);
            } else {
                pathText.append(path);
            }
            if (lineNo != null) {
                pathText.append(":");
                pathText.append(String.valueOf(lineNo),
                        linkPath != null ? "link file://" + linkPath + "#" + lineNo : null);
            }
            row.add(pathText);
        }

        output.addRow(row.toArray());
        return output;
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        return new ArrayList<>();
    }
}
