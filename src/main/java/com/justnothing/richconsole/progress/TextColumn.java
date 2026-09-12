package com.justnothing.richconsole.progress;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.justnothing.richconsole.text.Text;

/**
 * A column containing text.
 * Ported from rich/progress.py TextColumn.
 *
 * <p>The text format supports {@code {task.<field>}} placeholders (e.g.
 * {@code {task.description}}, {@code {task.percentage}}, {@code {task.completed}},
 * {@code {task.fields.<name>}}), rendered with markup when {@code markup} is true.</p>
 */
public class TextColumn extends ProgressColumn {

    /** Matches custom field placeholders such as {task.fields.file}. */
    private static final Pattern FIELDS_PATTERN = Pattern.compile("\\{task\\.fields\\.([a-zA-Z0-9_]+)\\}");

    private final String textFormat;
    private final Object style;
    private final String justify;
    private final boolean markup;

    /** The raw format template, for use by subclasses. */
    protected String getTextFormat() {
        return textFormat;
    }

    public TextColumn(String textFormat) {
        this(textFormat, "none", "left", true);
    }

    public TextColumn(String textFormat, Object style, String justify, boolean markup) {
        this.textFormat = textFormat;
        this.style = style;
        this.justify = justify;
        this.markup = markup;
    }

    /**
     * Replace the supported {@code {task.*}} placeholders with task values.
     */
    public static String formatTemplate(String template, Progress.Task task) {
        String out = template;
        out = out.replace("{task.description}", String.valueOf(task.getDescription()));
        Double percentage = task.getPercentage();
        out = out.replace("{task.percentage:>3.0f}",
                String.format("%3.0f", percentage != null ? percentage : 0.0));
        out = out.replace("{task.percentage}",
                String.format("%.0f", percentage != null ? percentage : 0.0));
        out = out.replace("{task.completed}", ProgressFormats.formatNumber(task.getCompleted()));
        out = out.replace("{task.total}",
                task.getTotal() != null ? ProgressFormats.formatNumber(task.getTotal()) : "?");
        out = out.replace("{task.elapsed}",
                task.getElapsed() != null ? ProgressFormats.formatTime(task.getElapsed()) : "-:--:--");
        out = out.replace("{task.time_remaining}",
                task.getTimeRemaining() != null ? ProgressFormats.formatTime(task.getTimeRemaining()) : "-:--:--");
        Double speed = task.getSpeed();
        out = out.replace("{task.speed}", speed != null ? ProgressFormats.formatNumber(speed) : "?");
        Matcher matcher = FIELDS_PATTERN.matcher(out);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            Object value = task.getFields().get(matcher.group(1));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(
                    value != null ? String.valueOf(value) : ""));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    @Override
    public Object render(Progress.Task task) {
        return renderFormatted(formatTemplate(textFormat, task), task);
    }

    /** Render pre-formatted text with the column's style/justify/markup settings. */
    protected Text renderFormatted(String text, Progress.Task task) {
        Text rendered = markup ? Text.fromMarkup(text, style) : new Text(text, style);
        rendered.setJustify(justify);
        rendered.setEnd("");
        return rendered;
    }
}
