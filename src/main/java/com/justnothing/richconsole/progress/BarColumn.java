package com.justnothing.richconsole.progress;

import com.justnothing.richconsole.progressbar.ProgressBar;

/**
 * Renders a visual progress bar.
 * Ported from rich/progress.py BarColumn.
 */
public class BarColumn extends ProgressColumn {

    private final Integer barWidth;
    private final Object style;
    private final Object completeStyle;
    private final Object finishedStyle;
    private final Object pulseStyle;

    public BarColumn() {
        this(40, "bar.back", "bar.complete", "bar.finished", "bar.pulse");
    }

    public BarColumn(Integer barWidth, Object style, Object completeStyle,
                     Object finishedStyle, Object pulseStyle) {
        this.barWidth = barWidth;
        this.style = style;
        this.completeStyle = completeStyle;
        this.finishedStyle = finishedStyle;
        this.pulseStyle = pulseStyle;
    }

    @Override
    public Object render(Progress.Task task) {
        Double total = task.getTotal();
        return ProgressBar.of(cfg -> cfg
                .total(total != null ? Math.max(0.0, total) : 0.0)
                .completed(Math.max(0.0, task.getCompleted()))
                .pulse(!task.isStarted())
                .style(style)
                .completeStyle(completeStyle)
                .finishedStyle(finishedStyle)
                .pulseStyle(pulseStyle)
                .animationTime(task.getTime()));
    }

    @Override
    public Integer getWidth() {
        return barWidth;
    }
}
